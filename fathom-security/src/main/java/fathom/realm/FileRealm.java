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

package fathom.realm;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import fathom.authc.AuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * A FileRealm is a MemoryRealm that loads accounts and roles from an external file.
 * The external file is watched and reloaded on modification.
 *
 * @author James Moger
 */
public class FileRealm extends MemoryRealm {

    private static final Logger log = LoggerFactory.getLogger(FileRealm.class);

    private volatile File realmFile;

    private volatile long lastModified;

    @Override
    public void setup(Config config) {
        super.setup(config);

        String file = Strings.emptyToNull(config.getString("file"));
        Preconditions.checkNotNull(file, "The [file] setting must be set!");

        File realmFile = new File(file);
        setFile(realmFile);
    }

    public void setFile(File realmFile) {
        this.realmFile = realmFile;
        readFile();
    }

    @Override
    public void start() {
        log.debug("Realm '{}' configuration:", getRealmName());
        logSetting(log, "file", realmFile);
    }

    /**
     * Force a re-read of the credentials file (if modified) before executing authentication.
     *
     * @param authenticationToken
     * @return the account if authentication is successful
     */
    @Override
    public Account authenticate(AuthenticationToken authenticationToken) {
        readFile();
        return super.authenticate(authenticationToken);
    }

    /**
     * Reads the realm file and rebuilds the in-memory lookup tables.
     */
    protected synchronized void readFile() {
        if (realmFile != null && realmFile.exists() && (realmFile.lastModified() != lastModified)) {
            lastModified = realmFile.lastModified();
            try {
                Preconditions.checkArgument(realmFile.canRead(), "The file '{}' can not be read!", realmFile);
                Config config = ConfigFactory.parseFile(realmFile);
                super.setup(config);
            } catch (Exception e) {
                log.error("Failed to read {}", realmFile, e);
            }
        }
    }

}
