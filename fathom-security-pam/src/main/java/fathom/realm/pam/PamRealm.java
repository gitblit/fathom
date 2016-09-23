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

package fathom.realm.pam;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import fathom.authc.StandardCredentials;
import fathom.realm.Account;
import fathom.realm.CachingRealm;
import fathom.utils.Util;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author James Moger
 */
public class PamRealm extends CachingRealm {

    private static Logger log = LoggerFactory.getLogger(PamRealm.class);

    private String serviceName;

    public PamRealm() {
        super();
    }

    @Override
    public void setup(Config config) {
        super.setup(config);

        String os = System.getProperty("os.name").toLowerCase();
        Preconditions.checkState(!os.startsWith("windows"), "PAM authentication is not supported on '{0}'", os);

        // Try to identify the passwd database
        String[] files = {"/etc/shadow", "/etc/master.passwd"};
        File passwdFile = null;
        for (String name : files) {
            File f = new File(name);
            if (f.exists()) {
                passwdFile = f;
                break;
            }
        }
        if (passwdFile == null) {
            log.warn("Could not find a passwd database!");
        } else if (!passwdFile.canRead()) {
            log.warn("Can not read passwd database {}! PAM authentications may fail!", passwdFile);
        }

        serviceName = "system-auth";
        if (config.hasPath("serviceName")) {
            serviceName = config.getString("serviceName");
        }
    }

    @Override
    public void start() {
        log.debug("Realm '{}' configuration:", getRealmName());
        Util.logSetting(log, "serviceName", serviceName);
        super.logCacheSettings(log);
    }

    @Override
    public void stop() {
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
        PAM pam = null;
        try {
            pam = new PAM(serviceName);
            pam.authenticate(username, password);
            log.debug("Authentication succeeded for '{}' against '{}'", username, getRealmName());

            Account account = new Account(null, new StandardCredentials(username, password));
            cacheAccount(account);

            return account;
        } catch (PAMException e) {
            log.debug("Authentication failed for '{}' against '{}'", username, getRealmName());
            log.error(e.getMessage());
        } finally {
            if (pam != null) {
                pam.dispose();
            }
        }

        return null;
    }

}
