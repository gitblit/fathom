/*
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fathom.realm.redis;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import fathom.authc.StandardCredentials;
import fathom.exception.FathomException;
import fathom.realm.Account;
import fathom.realm.CachingRealm;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * @author James Moger
 */
public class RedisRealm extends CachingRealm {

    private static Logger log = LoggerFactory.getLogger(RedisRealm.class);
    private JedisPool pool;
    private String redisUrl;
    private String redisPassword;
    private String passwordMapping;
    private String nameMapping;
    private String emailMapping;
    private String roleMapping;
    private String permissionMapping;
    private String startScript;
    private String stopScript;

    public RedisRealm() {
        super();
    }

    @Override
    public void setup(Config config) {
        super.setup(config);

        redisUrl = Strings.emptyToNull(config.getString("url"));
        Preconditions.checkNotNull(redisUrl, "Url must be specified!");

        if (config.hasPath("password")) {
            redisPassword = Strings.emptyToNull(config.getString("password"));
        }

        passwordMapping = "${username}:password";
        if (config.hasPath("passwordMapping")) {
            passwordMapping = Strings.emptyToNull(config.getString("passwordMapping"));
        }
        Preconditions.checkNotNull(passwordMapping, "You must specify 'passwordMapping'");

        if (config.hasPath("nameMapping")) {
            nameMapping = Strings.emptyToNull(config.getString("nameMapping"));
        }

        if (config.hasPath("emailMapping")) {
            emailMapping = Strings.emptyToNull(config.getString("emailMapping"));
        }

        if (config.hasPath("roleMapping")) {
            roleMapping = Strings.emptyToNull(config.getString("roleMapping"));
        }

        if (config.hasPath("permissionMapping")) {
            permissionMapping = Strings.emptyToNull(config.getString("permissionMapping"));
        }

        if (config.hasPath("startScript")) {
            startScript = Strings.emptyToNull(config.getString("startScript"));
        }

        if (config.hasPath("stopScript")) {
            stopScript = Strings.emptyToNull(config.getString("stopScript"));
        }

    }

    @Override
    public void start() {

        log.debug("Realm '{}' configuration:", getRealmName());
        Util.logSetting(log, "url", redisUrl);
        Util.logSetting(log, "password", redisPassword);
        Util.logSetting(log, "passwordMapping", passwordMapping);
        Util.logSetting(log, "nameMapping", nameMapping);
        Util.logSetting(log, "emailMapping", emailMapping);
        Util.logSetting(log, "roleMapping", roleMapping);
        super.logCacheSettings(log);

        try {
            this.pool = new JedisPool(redisUrl);
        } catch (JedisException e) {
            throw new FathomException("Failed to create a Redis pool!", e);
        }

        if (!Strings.isNullOrEmpty(startScript)) {
            executeScript(startScript);
        }
    }

    @Override
    public void stop() {
        if (this.pool != null) {

            if (!Strings.isNullOrEmpty(stopScript)) {
                executeScript(stopScript);
            }

            this.pool.close();
        }
    }

    @Override
    public Account authenticate(StandardCredentials requestCredentials) {

        final String username = requestCredentials.getUsername();
        final String password = requestCredentials.getPassword();

        if (hasAccount(username)) {
            // account is cached, authenticate against the cache
            return super.authenticate(requestCredentials);
        }

        return authenticate(username, password);
    }

    @Override
    public Account authenticate(final String username, final String password) {
        Jedis jedis = pool.getResource();
        try {
            String storedPassword = asNull(jedis.get(key(username, passwordMapping)));
            if (Strings.isNullOrEmpty(storedPassword)) {
                log.debug("Account '{}' in '{}' has no password and may not be used for authentication",
                        username, getRealmName());
                return null;
            }

            StandardCredentials requestCredentials = new StandardCredentials(username, password);
            StandardCredentials storedCredentials = new StandardCredentials(username, storedPassword);
            if (validatePassword(requestCredentials, storedCredentials)) {
                log.debug("Authentication succeeded for '{}' against '{}'", username, getRealmName());

                String name = null;
                if (!Strings.isNullOrEmpty(nameMapping)) {
                    name = asNull(jedis.get(key(username, nameMapping)));
                }

                Account account = new Account(name, new StandardCredentials(username, password));

                if (!Strings.isNullOrEmpty(emailMapping)) {
                    account.addEmailAddresses(jedis.lrange(key(username, emailMapping), 0, -1));
                }

                if (!Strings.isNullOrEmpty(roleMapping)) {
                    account.getAuthorizations().addRoles(asArray(jedis.lrange(key(username, roleMapping), 0, -1)));
                }

                if (!Strings.isNullOrEmpty(permissionMapping)) {
                    account.getAuthorizations().addPermissions(asArray(jedis.lrange(key(username, permissionMapping), 0, -1)));
                }

                cacheAccount(account);

                return account;
            } else {
                log.debug("Authentication failed for '{}' against '{}'", username, getRealmName());
            }
        } catch (JedisException e) {
            log.debug("Authentication failed for '{}' against '{}'", username, getRealmName());
            pool.returnBrokenResource(jedis);
            jedis = null;
        } finally {
            if (jedis != null) {
                pool.returnResource(jedis);
            }
        }
        return null;
    }

    /**
     * Constructs a key for use with a key-value data store.
     *
     * @param username
     * @param pattern
     * @return a key
     */
    private String key(String username, String pattern) {
        return pattern.replace("${username}", username);
    }

    private String asNull(String value) {
        return "nil".equals(value) ? null : value;
    }

    private String[] asArray(Collection<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

    private String unwrapQuotes(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }
        if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Execute a script located either in the classpath or on the filesystem.
     *
     * @param scriptPath
     */
    protected void executeScript(String scriptPath) {
        URL scriptUrl = null;
        try {
            if (scriptPath.startsWith("classpath:")) {
                String script = scriptPath.substring("classpath:".length());
                scriptUrl = ClassUtil.getResource(script);
                if (scriptUrl == null) {
                    log.warn("Script '{}' not found!", scriptPath);
                }
            } else {
                File file = new File(scriptPath);
                if (file.exists()) {
                    scriptUrl = file.toURI().toURL();
                } else {
                    log.warn("Script '{}' not found!", scriptPath);
                }
            }

            // execute the script
            if (scriptUrl != null) {
                executeScript(scriptUrl);
            }

        } catch (Exception e) {
            log.error("Failed to parse '{}' as a url", scriptPath);
        }
    }

    /**
     * Execute a script.
     *
     * @param scriptUrl
     */
    protected void executeScript(URL scriptUrl) {
        log.debug("Executing script '{}'", scriptUrl);
        final Jedis jedis = pool.getResource();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(scriptUrl.openStream(), StandardCharsets.UTF_8))) {
            reader.lines()
                    .filter(line -> !Strings.isNullOrEmpty(line) && line.charAt(0) != '#' && !line.startsWith("//"))
                    .forEach(line -> {
                        try {
                            log.trace("execute: {}", line);
                            String[] args = line.split(" ", 3);
                            String command = null;
                            if (args.length == 1) {
                                command = String.format("return redis.call('%s')", args[0]);
                            } else if (args.length == 2) {
                                command = String.format("return redis.call('%s', '%s')", args[0], args[1]);
                            } else if (args.length == 3) {
                                command = String.format("return redis.call('%s', '%s', '%s')", args[0], args[1], unwrapQuotes(args[2]));
                            } else {
                                log.error("Unexpected number of script arguments {}", args.length);
                            }
                            log.trace("command: {}", command);
                            Object result = jedis.eval(command);
                        } catch (Exception e) {
                            log.error("Failed to execute '{}'", line, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to execute script '{}'", scriptUrl, e);
            pool.returnBrokenResource(jedis);
//            jedis = null;
        } finally {
            if (jedis != null) {
                pool.returnResource(jedis);
            }
        }
    }
}
