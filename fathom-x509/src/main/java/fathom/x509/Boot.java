/*
 * Copyright (C) 2016 the original author or authors.
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

package fathom.x509;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import fathom.Constants;
import fathom.conf.Settings;

import java.io.File;

/**
 * Implementation of Boot which will auto-generate a complete X509 certificate infrastructure for your microservice.
 */
public class Boot extends fathom.Boot {

    public Boot() {
        super();
    }

    public Boot(String[] args) {
        super(args);
    }

    public Boot(Constants.Mode mode) {
        super(mode);
    }

    public Boot(Settings settings) {
        super(settings);
    }

    /**
     * The Java entry point.
     *
     * @param args
     */
    public static void main(String... args) {
        try {
            final Boot boot = new Boot(args);
            boot.addShutdownHook().start();
        } catch (Exception e) {
            Exception root = (Exception) Throwables.getRootCause(e);
            root.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    protected void init() {
        super.init();
        setupX509Infrastructure();
    }

    protected void setupX509Infrastructure() {

        final String keystorePassword = Optional.fromNullable(Strings.emptyToNull(getSettings().getKeystorePassword())).or("fathom");
        final String truststorePassword = Optional.fromNullable(Strings.emptyToNull(getSettings().getTruststorePassword())).or(keystorePassword);

        Preconditions.checkArgument(keystorePassword.equals(truststorePassword),
                "Keystore password and truststore password must be the same");

        String keystore = Optional.fromNullable(Strings.emptyToNull(getSettings().getKeystoreFile())).or(X509Utils.SERVER_KEY_STORE);
        String truststore = Optional.fromNullable(Strings.emptyToNull(getSettings().getTruststoreFile())).or(X509Utils.SERVER_TRUST_STORE);

        File serverKeyStore = new File(keystore);
        File serverTrustStore = new File(truststore);

        String hostname = Optional.fromNullable(Strings.emptyToNull(getSettings().getApplicationHostname())).or("localhost");
        int validityDuration = getSettings().getInteger("undertow.certificateValidityDuration", 10);

        if (!serverKeyStore.exists()) {
            X509Utils.X509Metadata metadata = new X509Utils.X509Metadata(hostname, keystorePassword, validityDuration);
            X509Utils.prepareX509Infrastructure(metadata, serverKeyStore, serverTrustStore);
        }

        // Update Fathom runtime settings
        getSettings().overrideSetting(Settings.Setting.undertow_keystoreFile, serverKeyStore.getAbsolutePath());
        getSettings().overrideSetting(Settings.Setting.undertow_keystorePassword, keystorePassword);
        getSettings().overrideSetting(Settings.Setting.undertow_truststoreFile, serverTrustStore.getAbsolutePath());
        getSettings().overrideSetting(Settings.Setting.undertow_truststorePassword, truststorePassword);
    }
}
