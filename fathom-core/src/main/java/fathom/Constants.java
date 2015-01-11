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
package fathom;

import com.google.common.base.Preconditions;
import fathom.utils.ClassUtil;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Constants for Fathom.
 *
 * @author James Moger
 */
public class Constants {

    public static final int MIN_BORDER_LENGTH = 68;

    private static final String LOGO = "\n" +
            "    (_)        ______      __  __\n" +
            "   __|__      / ____/___ _/ /_/ /_  ____  ____ ___ \n" +
            "     |       / /_  / __ `/ __/ __ \\/ __ \\/ __ `__ \\  ${description}\n" +
            " \\__/ \\__/  / __/ / /_/ / /_/ / / / /_/ / / / / / /  ${url}\n" +
            "  °-. .-°  /_/    \\__,_/\\__/_/ /_/\\____/_/ /_/ /_/   ${version}\n" +
            "     '";

    public static String getLogo() {
        String FATHOM_PROPERTIES = "fathom/version.properties";
        String version = null;
        String projectUrl = null;
        String description = null;

        URL url = ClassUtil.getResource(FATHOM_PROPERTIES);
        Preconditions.checkNotNull(url, "Failed to find " + FATHOM_PROPERTIES);

        try (InputStream stream = url.openStream()) {
            Properties prop = new Properties();
            prop.load(stream);

            version = prop.getProperty("version");
            projectUrl = prop.getProperty("projectUrl");
            description = prop.getProperty("description");

        } catch (IOException e) {
            LoggerFactory.getLogger(Constants.class).error("Failed to read '{}'", FATHOM_PROPERTIES, e);
        }

        Preconditions.checkNotNull(version, "The Fathom version is null!");
        Preconditions.checkNotNull(projectUrl, "The Fathom project url is null!");
        Preconditions.checkNotNull(description, "The Fathom description is null!");

        String logo = Constants.LOGO.replace("${version}", version).replace("${url}", projectUrl).replace("${description}", description);
        return logo;
    }

    /**
     * Returns the running Fathom version.
     *
     * @return the running Fathom version
     */
    public static String getVersion() {

        String FATHOM_PROPERTIES = "fathom/version.properties";
        String version = null;

        URL url = ClassUtil.getResource(FATHOM_PROPERTIES);
        Preconditions.checkNotNull(url, "Failed to find " + FATHOM_PROPERTIES);

        try (InputStream stream = url.openStream()) {
            Properties prop = new Properties();
            prop.load(stream);

            version = prop.getProperty("version");

        } catch (IOException e) {
            LoggerFactory.getLogger(Constants.class).error("Failed to read '{}'", FATHOM_PROPERTIES, e);
        }

        Preconditions.checkNotNull(version, "The Fathom version is null!");

        return version;
    }

    public static enum Mode {
        DEV, TEST, PROD, MASTER, SLAVE;

        public static Mode byName(String name) {
            for (Mode mode : values()) {
                if (name.equalsIgnoreCase(mode.name())) {
                    return mode;
                }
            }

            throw new IllegalArgumentException("Illegal mode specified \'" + name + "\'. Must be one of " + Mode.values().toString());
        }

    }

}
