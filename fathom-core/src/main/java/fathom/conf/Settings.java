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
package fathom.conf;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import fathom.Constants;
import fathom.exception.FathomException;
import fathom.utils.ClassUtil;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Settings {

    private static Logger log = LoggerFactory.getLogger(Settings.class);
    private final int defaultJmxPort = 7091;
    private final int defaultHttpPort = 8080;
    private final int defaultHttpsPort = 0;
    private final int defaultAjpPort = 0;
    private final String defaultContextPath = "/";
    private final String defaultHost = "0.0.0.0";
    private final String defaultUploadFilesLocation = System.getProperty("java.io.tmpdir");
    private final long defaultUploadFilesMaxSize = -1L;
    private final Config config;
    private final Properties overrides;
    private String profile = "default";
    private Constants.Mode mode;

    public Settings() {
        this(Constants.Mode.PROD);
    }

    public Settings(Constants.Mode mode) {
        this(mode, new String[0]);
    }

    public Settings(String[] args) {
        this(Constants.Mode.PROD, args);
    }

    public Settings(Constants.Mode mode, String[] args) {
        this.mode = mode;
        this.overrides = new Properties();

        applyArgs(args);
        this.config = loadConfig();
    }

    private Config loadConfig() {
        log.info("Runtime profile is '{}'", profile);

        // start with an empty config
        Config runtimeConfig = ConfigFactory.empty();

        // merge the classpath config file
        URL configFileUrl = ClassUtil.getResource(String.format("conf/%s.conf", profile));
        if (configFileUrl == null) {
            log.warn("Failed to find Fathom config 'classpath:conf/{}.conf'", profile);
        } else {
            Config classpathConfig = loadConfig(configFileUrl);
            if (classpathConfig.isEmpty()) {
                log.warn("Empty config '{}'", configFileUrl);
            } else {
                runtimeConfig = classpathConfig.withFallback(runtimeConfig);
                log.debug("Loaded config '{}'", configFileUrl);
            }
        }

        // merge an external config file
        try {
            Path path = Paths.get(System.getProperty("user.dir"), String.format("%s.conf", profile));
            if (path.toFile().exists()) {
                URL externalUrl = path.toUri().toURL();
                Config externalConfig = loadConfig(externalUrl);
                if (externalConfig != null) {
                    runtimeConfig = externalConfig.withFallback(runtimeConfig);
                    log.debug("Loaded config '{}'", externalUrl);
                }
            } else {
                log.debug("Failed to find working directory config file '{}'", path);
            }
        } catch (MalformedURLException e) {
            log.error("Failed to parse working directory config file!", e);
        }

        // filter the merged runtime config by mode
        String modeName = mode.name().toLowerCase();
        if (runtimeConfig.hasPath(modeName)) {
            Config runtimeModeConfig = runtimeConfig
                    .getConfig(modeName)
                    .withFallback(runtimeConfig);
            return runtimeModeConfig.resolve();
        }

        // resolve all substitutions
        return runtimeConfig.resolve();
    }

    private Config loadConfig(URL url) {
        if (url == null) {
            return ConfigFactory.empty();
        }

        Config config = null;
        try (InputStreamReader reader = new InputStreamReader(url.openStream())) {
            config = ConfigFactory.parseReader(reader);
        } catch (IOException e) {
            throw new FathomException(e, "Failed to parse config file '{}'", url);
        }
        return config;
    }


    public boolean isDev() {
        return Constants.Mode.DEV == mode;
    }

    public boolean isTest() {
        return Constants.Mode.TEST == mode;
    }

    public boolean isProd() {
        return Constants.Mode.PROD == mode;
    }

    public String getApplicationName() {
        return Optional.fromNullable(Strings.emptyToNull(getString(Setting.application_name, null))).or("<APPLICATION>");
    }

    public String getApplicationVersion() {
        return Optional.fromNullable(Strings.emptyToNull(getString(Setting.application_version, null))).or("<VERSION>");
    }

    public String getApplicationPackage() {
        return Strings.emptyToNull(getString(Setting.application_package, ""));
    }

    public String getApplicationHostname() {
        return getString(Setting.application_hostname, defaultHost);
    }

    @Option(name = "--hostname", metaVar = "HOSTNAME",
            usage = "Hostname to use for within the application\n" +
                    "e.g. 'my.application.com'")
    public Settings hostname(String hostname) {
        this.overrideSetting(Setting.application_hostname, hostname);

        return this;
    }

    public URL getFileUrl(String name, String defaultValue) {
        String file = getString(name, defaultValue);
        if (Strings.isNullOrEmpty(file)) {
            return null;
        }

        try {
            if (file.startsWith("classpath:")) {
                return ClassUtil.getResource(file.substring("classpath:".length()));
            } else if (file.startsWith("url:")) {
                return new URL(file.substring("url:".length()));
            } else if (file.startsWith("file:")) {
                return new URL(file.substring("file:".length()));
            } else {
                return new URL(file);
            }
        } catch (IOException e) {
            throw new FathomException(e, "Failed to create '{}' URL from '{}'", name, file);
        }
    }

    /**
     * Returns the preferred application url (https is favored over http).
     *
     * @return the preferred application url
     */
    public String getApplicationUrl() {
        if (getHttpsPort() > 0) {
            int port = getHttpsPort();
            if (port == 443) {
                return String.format("https://%s%s", getApplicationHostname(), getContextPath());
            }
            return String.format("https://%s:%s%s", getApplicationHostname(), port, getContextPath());
        } else if (getHttpPort() > 0) {
            int port = getHttpPort();
            if (port == 80) {
                return String.format("http://%s%s", getApplicationHostname(), getContextPath());
            }
            return String.format("http://%s:%s%s", getApplicationHostname(), getHttpPort(), getContextPath());
        }
        return null;
    }

    /**
     * Returns the preferred host url (http is favored over https).
     * Generally this url is used for integration testing.
     *
     * @return the preferred host url
     */
    public String getUrl() {

        if (getHttpPort() > 0) {
            return String.format("http://%s:%s%s", getHost(), getHttpPort(), getContextPath());
        } else if (getHttpsPort() > 0) {
            return String.format("https://%s:%s%s", getHost(), getHttpsPort(), getContextPath());
        }
        return null;
    }

    public void applyArgs(final String... args) {
        if (args == null || args.length == 0) {
            return;
        }

        final CmdLineParser parser = new CmdLineParser(this);
        try {

            parser.parseArgument(args);

        } catch (CmdLineException e) {
            // handling of wrong arguments
            StringWriter sw = new StringWriter();
            sw.append(Constants.getLogo());
            sw.append("\n");
            sw.append(String.format("Arguments:%n    %s%n%n", Arrays.toString(args)));
            sw.append(String.format("Error:%n    %s%n%n", e.getMessage()));
            sw.append("Options:\n\n");
            parser.printUsage(sw, null);
            log.error(sw.toString());
            throw new FathomException("Failed to parse args: {}", args);
        }
    }

    public String getProfile() {
        return profile;
    }

    @Option(name = "--profile", metaVar = "PROFILE",
            usage = "Specify the settings profile name\n" +
                    "e.g. 'application' to load the 'application.conf' settings\n" +
                    "     'master' to load the 'master.conf' settings")
    public Settings profile(String profile) {
        this.profile = profile;

        return this;
    }

    public Constants.Mode getMode() {
        return mode;
    }

    // careful changing this flag (see getMode(String... args);
    @Option(name = "--mode", metaVar = "MODE",
            usage = "Fathom runtime mode.\n" +
                    "PROD = production mode\n" +
                    "TEST = test mode\n" +
                    "DEV = development mode")
    public Settings mode(Constants.Mode mode) {
        this.mode = mode;

        return this;
    }

    public String getHost() {
        return getString(Setting.undertow_host, defaultHost);
    }

    @Option(name = "--host", metaVar = "HOST",
            usage = "Host interface for binding transports\n" +
                    "e.g. '0.0.0.0' to serve on all interfaces to all clients\n" +
                    "     'localhost' to serve only to this machine")
    public Settings host(String host) {
        this.overrideSetting(Setting.undertow_host, host);

        return this;
    }

    public int getJmxPort() {
        return getInteger(Setting.jmx_port, defaultJmxPort);
    }

    @Option(name = "--jmxPort", metaVar = "PORT",
            usage = "Port for serving the JMX registry & data.\nPORT <= 0 will disable this transport.")
    public Settings jmxPort(int port) {
        this.overrideSetting(Setting.jmx_port, port);

        return this;
    }


    public int getHttpPort() {
        return getInteger(Setting.undertow_httpPort, defaultHttpPort);
    }

    @Option(name = "--httpPort", metaVar = "PORT",
            usage = "Port for serving HTTP.\nPORT <= 0 will disable this transport.")
    public Settings httpPort(int port) {
        this.overrideSetting(Setting.undertow_httpPort, port);

        return this;
    }

    public int getHttpsPort() {
        return getInteger(Setting.undertow_httpsPort, defaultHttpsPort);
    }

    @Option(name = "--httpsPort", metaVar = "PORT",
            usage = "Port for serving HTTPS.\nPORT <= 0 will disable this transport.")
    public Settings httpsPort(int port) {
        this.overrideSetting(Setting.undertow_httpsPort, port);

        return this;
    }

    public int getAjpPort() {
        return getInteger(Setting.undertow_ajpPort, defaultAjpPort);
    }

    @Option(name = "--ajpPort", metaVar = "PORT",
            usage = "Port for serving AJP.\nPORT <= 0 will disable this transport.")
    public Settings ajpPort(int port) {
        this.overrideSetting(Setting.undertow_ajpPort, port);

        return this;
    }

    public String getContextPath() {
        return getString(Setting.undertow_contextPath, defaultContextPath);
    }

    @Option(name = "--context", metaVar = "PATH",
            usage = "Sets the context path")
    public Settings contextPath(String contextPath) {
        this.overrideSetting(Setting.undertow_contextPath, contextPath/*StringUtils.addStart(contextPath, "/")*/);

        return this;
    }

    public String getUploadFilesLocation() {
        return getString(Setting.application_uploadLocation, defaultUploadFilesLocation);
    }

    public Settings uploadFilesLocation(String uploadFilesLocation) {
        this.overrideSetting(Setting.application_uploadLocation, uploadFilesLocation);

        return this;
    }

    public long getUploadFilesMaxSize() {
        return getLong(Setting.application_uploadMaxSize, defaultUploadFilesMaxSize);
    }

    public Settings uploadFilesMaxSize(long uploadFilesMaxSize) {
        this.overrideSetting(Setting.application_uploadMaxSize, uploadFilesMaxSize);

        return this;
    }

    public String getKeystoreFile() {
        return getString(Setting.undertow_keystoreFile, null);
    }

    public Settings keystoreFile(String keystoreFile) {
        this.overrideSetting(Setting.undertow_keystoreFile, keystoreFile);

        return this;
    }

    public String getKeystorePassword() {
        return getString(Setting.undertow_keystorePassword, null);
    }

    public Settings keystorePassword(String keystorePassword) {
        this.overrideSetting(Setting.undertow_keystorePassword, keystorePassword);

        return this;
    }

    public String getTruststoreFile() {
        return getString(Setting.undertow_truststoreFile, null);
    }

    public Settings truststoreFile(String truststoreFile) {
        this.overrideSetting(Setting.undertow_truststoreFile, truststoreFile);

        return this;
    }

    public String getTruststorePassword() {
        return getString(Setting.undertow_truststorePassword, null);
    }

    public Settings truststorePassword(String truststorePassword) {
        this.overrideSetting(Setting.undertow_truststorePassword, truststorePassword);

        return this;
    }

    public String getRequiredString(Enum<?> key) {
        return getRequiredString(key.toString());
    }

    public String getString(Enum<?> key, String defaultValue) {
        return getString(key.toString(), defaultValue);
    }

    public int getInteger(Enum<?> key, int defaultValue) {
        return getInteger(key.toString(), defaultValue);
    }

    public long getLong(Enum<?> key, long defaultValue) {
        return defaultValue;//getLong(key.toString(), defaultValue);
    }

    public boolean getBoolean(Enum<?> key, boolean defaultValue) {
        return getBoolean(key.toString(), defaultValue);
    }

    public long getBytes(Enum<?> key, String defaultValue) {
        return getBytes(key.toString(), defaultValue);
    }

    public void overrideSetting(Enum<?> key, String defaultValue) {
        overrideSetting(key.toString(), defaultValue);
    }

    public void overrideSetting(Enum<?> key, int defaultValue) {
        overrideSetting(key.toString(), defaultValue);
    }

    public void overrideSetting(Enum<?> key, long defaultValue) {
        overrideSetting(key.toString(), defaultValue);
    }

    public void overrideSetting(Enum<?> key, boolean defaultValue) {
        overrideSetting(key.toString(), defaultValue);
    }

    public String getLocalHostname() {
        // try InetAddress.LocalHost first;
        // NOTE -- InetAddress.getLocalHost().getHostName() will not work in
        // certain environments.
        try {
            String result = InetAddress.getLocalHost().getHostName();
            if (!Strings.isNullOrEmpty(result))
                return result;
        } catch (UnknownHostException e) {
            // failed; try alternate means.
        }

        // try environment properties.
        //
        String host = System.getenv("COMPUTERNAME");
        if (host != null)
            return host;
        host = System.getenv("HOSTNAME");
        if (host != null)
            return host;

        // undetermined.
        return "Fathom";
    }

    public Config getConfig() {
        return config;
    }

    public Config getConfig(String name) {
        return config.getConfig(name);
    }

    public String getNonEmptyString(String name, String defaultValue) {
        String value = Optional.fromNullable(Strings.emptyToNull(getString(name, null))).or(defaultValue);
        return value;
    }

    /**
     * Returns the string value for the specified name. If the name does not exist
     * or the value for the name can not be interpreted as a string, the
     * defaultValue is returned.
     *
     * @param name
     * @param defaultValue
     * @return name value or defaultValue
     */
    public String getString(String name, String defaultValue) {
        String value = defaultValue;
        if (getConfig().hasPath(name)) {
            value = getConfig().getString(name);
        }
        value = overrides.getProperty(name, value);

        return value;
    }

    /**
     * Returns the boolean value for the specified name. If the name does not
     * exist or the value for the name can not be interpreted as a boolean, the
     * defaultValue is returned.
     *
     * @param name
     * @param defaultValue
     * @return name value or defaultValue
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        String value = getString(name, null);
        if (!Strings.isNullOrEmpty(value)) {
            return Boolean.parseBoolean(value.trim());
        }

        return defaultValue;
    }

    /**
     * Returns the integer value for the specified name. If the name does not
     * exist or the value for the name can not be interpreted as an integer, the
     * defaultValue is returned.
     *
     * @param name
     * @param defaultValue
     * @return name value or defaultValue
     */
    public int getInteger(String name, int defaultValue) {
        try {
            String value = getString(name, null);
            if (!Strings.isNullOrEmpty(value)) {
                return Integer.parseInt(value.trim().split(" ")[0]);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer for " + name + " using default of "
                    + defaultValue);
        }

        return defaultValue;
    }

    /**
     * Returns the long value for the specified name. If the name does not
     * exist or the value for the name can not be interpreted as an long, the
     * defaultValue is returned.
     *
     * @param name
     * @param defaultValue
     * @return name value or defaultValue
     */
    public long getLong(String name, long defaultValue) {
        try {
            String value = getString(name, null);
            if (!Strings.isNullOrEmpty(value)) {
                return Long.parseLong(value.trim().split(" ")[0]);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long for " + name + " using default of "
                    + defaultValue);
        }

        return defaultValue;
    }

    /**
     * Returns the bytes value for the specified name. If the name does not
     * exist or the value for the name can not be interpreted as a size expression, the
     * defaultValue is returned.
     *
     * @param name
     * @param defaultValue
     * @return value or defaultValue
     */
    public long getBytes(String name, String defaultValue) {
        if (config.hasPath(name)) {
            try {
                long value = config.getBytes(name);
                return value;
            } catch (Exception e) {
                log.warn("Failed to parse bytes for {} using default of {}", name, defaultValue);
            }
        }

        if (!Strings.isNullOrEmpty(defaultValue)) {
            try {
                Config temp = ConfigFactory.parseString(String.format("%s=%s", name, defaultValue));
                long value = temp.getBytes(name);
                return value;
            } catch (Exception e) {
                log.warn("Failed to parse default bytes expression {} for {}", defaultValue, name);
            }
        }
        return 0;
    }

    /**
     * Returns the float value for the specified name. If the name does not
     * exist or the value for the name can not be interpreted as a float, the
     * defaultValue is returned.
     *
     * @param name
     * @param defaultValue
     * @return name value or defaultValue
     */
    public float getFloat(String name, float defaultValue) {
        try {
            String value = getString(name, null);
            if (!Strings.isNullOrEmpty(value)) {
                return Float.parseFloat(value.trim().split(" ")[0]);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse float for " + name + " using default of "
                    + defaultValue);
        }

        return defaultValue;
    }

    /**
     * Returns the double value for the specified name. If the name does not
     * exist or the value for the name can not be interpreted as a double, the
     * defaultValue is returned.
     *
     * @param name
     * @param defaultValue
     * @return name value or defaultValue
     */
    public double getDouble(String name, double defaultValue) {
        try {
            String value = getString(name, null);
            if (!Strings.isNullOrEmpty(value)) {
                return Double.parseDouble(value.trim().split(" ")[0]);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse double for " + name + " using default of "
                    + defaultValue);
        }

        return defaultValue;
    }

    /**
     * Returns the char value for the specified name. If the name does not exist
     * or the value for the name can not be interpreted as a char, the
     * defaultValue is returned.
     *
     * @param name
     * @param defaultValue
     * @return name value or defaultValue
     */
    public char getChar(String name, char defaultValue) {
        String value = getString(name, null);
        if (!Strings.isNullOrEmpty(value)) {
            return value.trim().charAt(0);
        }

        return defaultValue;
    }

    /**
     * Returns the string value for the specified name.  If the name does not
     * exist an exception is thrown.
     *
     * @param name
     * @return name value
     */
    public String getRequiredString(String name) {
        String value = getString(name, null);
        if (value != null) {
            return value.trim();
        }
        throw new FathomException("Setting '{}' has not been configured!", name);
    }

    /**
     * Returns a list of comma-delimited strings from the specified name.
     *
     * @param name
     * @return list of strings
     */
    public List<String> getStrings(String name) {
        if (!getConfig().hasPath(name)) {
            return Collections.emptyList();
        }
        List<String> stringList = getConfig().getStringList(name);
        return stringList;
    }

    /**
     * Returns a list of comma-delimited integers from the specified name.
     *
     * @param name
     * @return list of integers
     */
    public List<Integer> getIntegers(String name) {
        if (!getConfig().hasPath(name)) {
            return Collections.emptyList();
        }
        List<Integer> ints = getConfig().getIntList(name);
        return ints;
    }

    /**
     * Returns a list of comma-delimited longs from the specified name.
     *
     * @param name
     * @return list of longs
     */
    public List<Long> getLongs(String name) {
        if (!getConfig().hasPath(name)) {
            return Collections.emptyList();
        }
        List<Long> longs = getConfig().getLongList(name);
        return longs;
    }

    /**
     * Gets the duration setting and converts it to milliseconds.
     * <p>
     * The setting must be use one of the following conventions:
     * <ul>
     * <li> n MILLISECONDS
     * <li> n SECONDS
     * <li> n MINUTES
     * <li> n HOURS
     * <li> n DAYS
     * </ul>
     *
     * @param name
     * @return milliseconds
     */
    public long getDuration(String name, TimeUnit timeUnit, long defaultValue) {
        if (!getConfig().hasPath(name)) {
            return defaultValue;
        }
        long duration = getConfig().getDuration(name, timeUnit);
        return duration;
    }

    /**
     * Tests for the existence of a setting.
     *
     * @param name
     * @return true if the setting exists
     */
    public boolean hasSetting(String name) {
        return getConfig().hasPath(name) && getConfig().getValue(name).unwrapped() != null;
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, boolean value) {
        overrides.put(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, String value) {
        overrides.put(name, value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, char value) {
        overrides.put(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, int value) {
        overrides.put(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, long value) {
        overrides.put(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, float value) {
        overrides.put(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, double value) {
        overrides.put(name, "" + value);
    }

    public Properties toProperties() {
        Properties props = new Properties();
        for (Map.Entry<String, ConfigValue> entry : getConfig().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue().unwrapped();
            props.put(key, value);
        }
        props.putAll(overrides);
        return props;
    }

    public Properties toProperties(String name) {
        Properties props = new Properties();
        for (Map.Entry<String, ConfigValue> entry : getConfig(name).entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue().unwrapped();
            props.put(key, value);
        }
        String nameGroup = name + ".";
        for (String override : overrides.stringPropertyNames()) {
            if (override.startsWith(nameGroup)) {
                props.put(override, overrides.get(override));
            }
        }
        return props;
    }

    public static enum Setting {
        application_name,
        application_version,
        application_package,
        application_hostname,
        application_controllersPackage,
        application_uploadLocation,
        application_uploadMaxSize,
        jcache_preferredProvider,
        jmx_port,
        metrics_jvm_enabled,
        metrics_mbeans_enabled,
        undertow_ajpPort,
        undertow_httpPort,
        undertow_httpsPort,
        undertow_host,
        undertow_contextPath,
        undertow_keystoreFile,
        undertow_keystorePassword,
        undertow_truststoreFile,
        undertow_truststorePassword,
        undertow_ioThreads,
        undertow_workerThreads,
        undertow_bufferSize,
        undertow_buffersPerRegion;

        @Override
        public String toString() {
            return name().replace('_', '.');
        }
    }
}
