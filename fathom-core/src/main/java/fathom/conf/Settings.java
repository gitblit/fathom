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
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
    private final String defaultListenAddress = "0.0.0.0";
    private final String defaultUploadFilesLocation = System.getProperty("java.io.tmpdir");
    private final long defaultUploadFilesMaxSize = -1L;
    private Config config;
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

        // We apply args twice - which at first seems redundant.
        // The first pass sets up state fields of Settings (e.g. runtime mode).
        // The second pass overrides settings in the parsed config of the specified runtime mode.
        // This is a little wasteful, but it doesn't require adding special parsing conditions.
        this.config = ConfigFactory.empty();
        applyArgs(args);

        this.config = loadConfig();
        applyArgs(args);
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

    @Option(name = "--applicationUrl", metaVar = "URL",
            usage = "The URL to advertise to clients.")
    public Settings applicationUrl(String url) {
        this.overrideSetting(Setting.application_url, url);

        return this;
    }

    /**
     * Returns the preferred application url, if no application url is specified use the Fathom url.
     *
     * @return the preferred application url
     */
    public String getApplicationUrl() {
        return getString(Setting.application_url, getFathomUrl());
    }

    /**
     * Returns the url of the Fathom application based on the listen address, port, and context path.
     * This url is intended for integration testing and will return an http url over https even if both
     * are configured.
     *
     * @return the fathom url
     */
    public String getFathomUrl() {
        if (getHttpPort() > 0) {
            int port = getHttpPort();
            if (port == 80) {
                return String.format("http://%s%s", getHttpListenAddress(), getContextPath());
            }
            return String.format("http://%s:%s%s", getHttpListenAddress(), getHttpPort(), getContextPath());
        } else if (getHttpsPort() > 0) {
            int port = getHttpsPort();
            if (port == 443) {
                return String.format("https://%s%s", getHttpsListenAddress(), getContextPath());
            }
            return String.format("https://%s:%s%s", getHttpsListenAddress(), port, getContextPath());
        }
        return null;
    }

    /**
     * Returns the preferred host url (http is favored over https).
     * Generally this url is used for integration testing.
     *
     * @return the preferred host url
     * @deprecated Use getFathomUrl() or getApplicationUrl() instead.
     */
    @Deprecated
    public String getUrl() {
        return getFathomUrl();
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

    /**
     * Returns the listen address of Undertow.
     *
     * @return thre listen address of Undertow
     * @deprecated Use the transport specific method.
     */
    @Deprecated
    public String getHost() {
        if (getHttpPort() > 0) {
            return getHttpListenAddress();
        } else if (getHttpsPort() > 0) {
            return getHttpsListenAddress();
        } else {
            return getAjpListenAddress();
        }
    }

    /**
     * Sets the host interface for binding all transports.
     *
     * @param host
     * @return this
     * @deprecated Please use the transport-specific settings.
     */
    @Deprecated
    public Settings host(String host) {
        throw new FathomException("Setting the host address is no longer supported!  Please use the transport-specific settings.");
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

    public String getHttpListenAddress() {
        return getString(Setting.undertow_httpListenAddress, defaultListenAddress);
    }

    @Option(name = "--httpListenAddress", metaVar = "ADDRESS",
            usage = "Interface to use for serving HTTP.")
    public Settings httpListenAddress(String address) {
        this.overrideSetting(Setting.undertow_httpListenAddress, address);

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

    public String getHttpsListenAddress() {
        return getString(Setting.undertow_httpsListenAddress, defaultListenAddress);
    }

    @Option(name = "--httpsListenAddress", metaVar = "ADDRESS",
            usage = "Interface to use for serving HTTPS.")
    public Settings httpsListenAddress(String address) {
        this.overrideSetting(Setting.undertow_httpsListenAddress, address);

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

    public String getAjpListenAddress() {
        return getString(Setting.undertow_ajpListenAddress, defaultListenAddress);
    }

    @Option(name = "--ajpListenAddress", metaVar = "ADDRESS",
            usage = "Interface to use for serving AJP.")
    public Settings ajpListenAddress(String address) {
        this.overrideSetting(Setting.undertow_ajpListenAddress, address);

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
        return getLong(key.toString(), defaultValue);
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

    private String extractScheme(String url) {
        try {
            return URI.create(url).getScheme();
        } catch (Exception e) {
        }
        return null;
    }

    public String getApplicationScheme() {
        String fathomScheme = extractScheme(getFathomUrl());
        String applicationScheme = extractScheme(getApplicationUrl());
        return Optional.fromNullable(Strings.emptyToNull(applicationScheme)).or(fathomScheme);
    }

    public int getApplicationPort() {
        int fathomPort = extractPort(getFathomUrl());
        int applicationPort = extractPort(getApplicationUrl());
        return applicationPort > 0 ? applicationPort : fathomPort;
    }

    private int extractPort(String url) {
        try {
            URI uri = URI.create(url);
            int port = uri.getPort();
            if (port > 0) {
                return port;
            }
            if ("https".equals(uri.getScheme())) {
                return 443;
            } else if ("http".equals(uri.getScheme())) {
                return 80;
            }
        } catch (Exception e) {
        }
        return 0;
    }

    private String extractHostname(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
        }
        return null;
    }

    public String getApplicationHostname() {
        String fathomHostname = extractHostname(getFathomUrl());
        String applicationHostname = extractHostname(getApplicationUrl());
        return Optional.fromNullable(Strings.emptyToNull(applicationHostname))
                .or(Optional.fromNullable(Strings.emptyToNull(fathomHostname)).or(getLocalHostname()));
    }

    /**
     * Formerly set the application hostname.
     *
     * @param hostname
     * @return this
     * @deprecated does nothing
     */
    @Deprecated
    public Settings hostname(String hostname) {
        throw new FathomException("Setting the application hostname is no longer supported!");
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

    /**
     * Merges the supplied config into the current config.
     *
     * @param config
     */
    public void mergeConfig(Config config) {
        if (this.config == null) {
            this.config = config;
        } else {
            this.config = config.withFallback(this.config);
        }
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
     * Override the settings at runtime with the specified value.
     * This change does not persist.
     *
     * @param settings
     */
    public void overrideSettings(Map<String, Object> settings) {
        mergeConfig(ConfigFactory.parseMap(settings));
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, boolean value) {
        overrideSetting(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, String value) {
        overrideSettings(new HashMap<String, Object>() {{
            put(name, value);
        }});
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, char value) {
        overrideSetting(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, int value) {
        overrideSetting(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, long value) {
        overrideSetting(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, float value) {
        overrideSetting(name, "" + value);
    }

    /**
     * Override the setting at runtime with the specified value.
     * This change does not persist.
     *
     * @param name
     * @param value
     */
    public void overrideSetting(String name, double value) {
        overrideSetting(name, "" + value);
    }

    public Properties toProperties() {
        Properties props = new Properties();
        for (Map.Entry<String, ConfigValue> entry : getConfig().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue().unwrapped();
            props.put(key, value);
        }
        return props;
    }

    public Properties toProperties(String name) {
        Properties props = new Properties();
        for (Map.Entry<String, ConfigValue> entry : getConfig(name).entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue().unwrapped();
            props.put(key, value);
        }
        return props;
    }

    public static enum Setting {
        application_name,
        application_version,
        application_package,
        application_controllersPackage,
        application_uploadLocation,
        application_uploadMaxSize,
        application_url,
        jcache_preferredProvider,
        jmx_port,
        metrics_jvm_enabled,
        metrics_mbeans_enabled,
        undertow_ajpPort,
        undertow_ajpListenAddress,
        undertow_httpPort,
        undertow_httpListenAddress,
        undertow_httpsPort,
        undertow_httpsListenAddress,
        undertow_contextPath,
        undertow_keystoreFile,
        undertow_keystorePassword,
        undertow_truststoreFile,
        undertow_truststorePassword,
        undertow_ioThreads,
        undertow_workerThreads,
        undertow_bufferSize;

        @Override
        public String toString() {
            return name().replace('_', '.');
        }
    }
}
