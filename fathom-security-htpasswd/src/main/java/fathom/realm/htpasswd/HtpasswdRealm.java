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
package fathom.realm.htpasswd;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import fathom.authc.AuthenticationToken;
import fathom.authc.StandardCredentials;
import fathom.realm.Account;
import fathom.realm.MemoryRealm;
import fathom.utils.ClassUtil;
import fathom.utils.Util;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of a user service using an Apache htpasswd file for authentication.
 * <p>
 * This realm reads a file created by the 'htpasswd' program of an Apache web server.
 * All possible output options of the 'htpasswd' program version 2.2 are supported:
 * <ul>
 * <li>plain text (only on Windows and Netware)</li>
 * <li>glibc crypt() (not on Windows and NetWare)</li>
 * <li>Apache MD5 (apr1)</li>
 * <li>unsalted SHA-1</li>
 * </ul>
 * <p>
 * Configuration options:
 * <ul>
 * <li><i>file</i> - The text file with the htpasswd entries to be used for authentication.</li>
 * <li><i>allowClearPasswords</i> - Boolean flag for controlling clear/crypt passwords.
 * The file is formatted using one or the other, but not both.</li>
 * </ul>
 *
 * @author Florian Zschocke
 * @author James Moger
 */
public class HtpasswdRealm extends MemoryRealm {

    private final static Logger log = LoggerFactory.getLogger(HtpasswdRealm.class);

    private final Map<String, String> credentialsMap;

    private String file;

    private volatile File realmFile;

    private volatile long lastModified;

    private boolean isAllowClearTextPasswords;

    public HtpasswdRealm() {
        this.credentialsMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean canAuthenticate(AuthenticationToken authenticationToken) {
        return authenticationToken instanceof StandardCredentials;
    }

    @Override
    public void setup(Config config) {
        super.setup(config);

        // Default to Apache htpasswd specificiations
        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("windows") || os.startsWith("netware")) {
            isAllowClearTextPasswords = true;
        } else {
            isAllowClearTextPasswords = false;
        }

        // Allow settings override
        if (config.hasPath("allowClearPasswords")) {
            isAllowClearTextPasswords = config.getBoolean("allowClearPasswords");
        }

        file = config.getString("file");
        Preconditions.checkNotNull(file, "You must specify an htpasswd 'file'!");

        if (file.startsWith("classpath:")) {
            // one-time read of the credentials file
            URL url = ClassUtil.getResource(file.substring("classpath:".length()));
            credentialsMap.putAll(readCredentialsURL(url));
            log.debug("Read {} standard credentials from '{}'", credentialsMap.size(), url);
        } else {
            // keep a credentials file reference so it may auto-reload
            setFile(new File(file));
            log.debug("Read {} standard credentials from '{}'", credentialsMap.size(), file);
        }

    }

    @Override
    public void start() {
        log.debug("Realm '{}' configuration:", getRealmName());
        Util.logSetting(log, "file", file);
        Util.logSetting(log, "allowClearPasswords", isAllowClearTextPasswords);
    }

    @Override
    public void stop() {
    }

    public boolean isAllowClearTextPasswords() {
        return isAllowClearTextPasswords;
    }

    public void setAllowClearTextPasswords(boolean value) {
        this.isAllowClearTextPasswords = value;
    }

    public synchronized void setFile(File realmFile) {
        Preconditions.checkNotNull(realmFile, "File is null!");
        Preconditions.checkArgument(realmFile.exists(), "{} does not exist!", realmFile);

        this.realmFile = realmFile;
        readCredentialsFile();
    }

    /**
     * Returns true if the username is in the htpasswd file.  If the username is not
     * in the htpasswd file but is in the MemoryRealm cache, then we still return false.
     * The htpasswd file is the boss.
     *
     * @param username
     * @return true if the htpasswd file has the username
     */
    @Override
    public boolean hasAccount(String username) {
        return credentialsMap.containsKey(username);
    }

    @Override
    public Account getAccount(String username) {
        // if we do not have a defined account (e.g. defined in realms.conf)
        // then we create an empty placeholder
        Account account = super.getAccount(username);
        if (account == null) {
            account = addAccount(username, credentialsMap.get(username));
        }

        return account;
    }

    /**
     * Force a re-read of the credentials file (if modified) before executing authentication.
     *
     * @param authenticationToken
     * @return the account if authentication is successful
     */
    @Override
    public Account authenticate(AuthenticationToken authenticationToken) {
        readCredentialsFile();
        return super.authenticate(authenticationToken);
    }

    /**
     * htpasswd supports a few other password encryption schemes than the StandardCredentialsRealm.
     *
     * @param requestCredentials
     * @param storedCredentials
     * @return true if the request password validates against the stored password
     */
    @Override
    protected boolean validatePassword(StandardCredentials requestCredentials, StandardCredentials storedCredentials) {
        final String storedPassword = storedCredentials.getPassword();
        final String username = requestCredentials.getUsername();
        final String password = requestCredentials.getPassword();
        boolean authenticated = false;

        // test Apache MD5 variant encrypted password
        if (storedPassword.startsWith("$apr1$")) {
            if (storedPassword.equals(Md5Crypt.apr1Crypt(password, storedPassword))) {
                log.trace("Apache MD5 encoded password matched for user '{}'", username);
                authenticated = true;
            }
        }
        // test Unsalted SHA password
        else if (storedPassword.startsWith("{SHA}")) {
            String password64 = Base64.encodeBase64String(DigestUtils.sha1(password));
            if (storedPassword.substring("{SHA}".length()).equals(password64)) {
                log.trace("Unsalted SHA-1 encoded password matched for user '{}'", username);
                authenticated = true;
            }
        }
        // test Libc Crypt password
        else if (!isAllowClearTextPasswords() && storedPassword.equals(Crypt.crypt(password, storedPassword))) {
            log.trace("Libc crypt encoded password matched for user '{}'", username);
            authenticated = true;
        }
        // test Clear Text password
        else if (isAllowClearTextPasswords() && storedPassword.equals(password)) {
            log.trace("Clear text password matched for user '{}'", username);
            authenticated = true;
        }

        return authenticated;
    }

    /**
     * Reads the credentials file and rebuilds the in-memory lookup tables.
     */
    protected synchronized void readCredentialsFile() {
        if (realmFile != null && realmFile.exists() && (realmFile.lastModified() != lastModified)) {
            lastModified = realmFile.lastModified();
            try {
                Map<String, String> credentials = readCredentialsURL(realmFile.toURI().toURL());
                credentialsMap.clear();
                credentialsMap.putAll(credentials);
            } catch (Exception e) {
                log.error("Failed to read {}", realmFile, e);
            }
        }
    }

    /**
     * Reads the credentials url.
     */
    protected Map<String, String> readCredentialsURL(URL url) {
        Map<String, String> credentials = new HashMap<>();
        Pattern entry = Pattern.compile("^([^:]+):(.+)");
        try (Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    Matcher m = entry.matcher(line);
                    if (m.matches()) {
                        String username = m.group(1);
                        String password = m.group(2);
                        if (Strings.isNullOrEmpty(username)) {
                            log.warn("Skipping line because the username is blank!");
                            continue;
                        }
                        if (Strings.isNullOrEmpty(password)) {
                            log.warn("Skipping '{}' account because the password is blank!", username);
                            continue;
                        }

                        credentials.put(username.trim(), password.trim());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to read {}", url, e);
        }
        return credentials;
    }
}
