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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import fathom.conf.Settings;
import fathom.exception.FathomException;
import fathom.utils.Util;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Boot starts/stops Fathom and can be optionally used as a Commons-Daemon Service.
 * <p>
 * http://commons.apache.org/proper/commons-daemon
 *
 * @author James Moger
 */
public class Boot implements Daemon {

    public final static String LOGBACK_CONFIGURATION_FILE_PROPERTY = "logback.configurationFile";

    private static final Logger log = LoggerFactory.getLogger(Boot.class);

    private static Object SERVICE = new Object();

    private static Boot boot;

    private final Settings settings;

    private Server server;

    public Boot() {
        this(new Settings());
    }

    public Boot(String[] args) {
        settings = new Settings(args);
        init();
    }

    public Boot(Constants.Mode mode) {
        this(mode == null ? new Settings() : new Settings(mode));
    }

    public Boot(Settings settings) {
        this.settings = settings;
        init();
    }

    /**
     * Called by prunsrv (Windows) to start the service.
     *
     * @param args
     */
    public static void start(String args[]) {
        log.debug("windowsStart called");
        boot = new Boot();
        boot.start();

        // block until interrupted by stop() or the undertow dies
        while (boot.getServer().isRunning()) {
            synchronized (SERVICE) {
                try {
                    SERVICE.wait(30000);  // wait 30 seconds and check if stopped
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    /**
     * Called by prunsrv (Windows) to stop the service.
     *
     * @param args
     */
    public static void stop(String args[]) {
        log.debug("windowsStop called");
        boot.stop();

        synchronized (SERVICE) {
            // notify the thread synchronized on SERVER in start()
            SERVICE.notify();
        }
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

    /**
     * Setup the Boot instance.
     */
    protected void init() {
        System.setProperty("java.awt.headless", "true");
        setupLogback();
    }

    public Settings getSettings() {
        return settings;
    }

    public Server getServer() {
        if (server == null) {
            server = new Server();
            server.setSettings(settings);
        }

        return server;
    }

    /**
     * Add a JVM shutdown hook.
     */
    public Boot addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                Boot.this.stop();
            }

        });

        return this;
    }

    /**
     * Called by prunsrv (Windows) or jsvc (UNIX) before start().
     *
     * @param context
     * @throws Exception
     */
    @Override
    public void init(DaemonContext context) throws Exception {
        log.debug("Fathom Daemon initialized");
        settings.applyArgs(context.getArguments());
    }

    /**
     * Starts Fathom synchronously.
     */
    @Override
    public synchronized void start() {
        Preconditions.checkNotNull(getServer());

        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        log.info("Bootstrapping {} ({})", settings.getApplicationName(), settings.getApplicationVersion());
        Util.logSetting(log, "Fathom", Constants.getVersion());
        Util.logSetting(log, "Mode", settings.getMode().toString());
        Util.logSetting(log, "Operating System", String.format("%s (%s)", osName, osVersion));
        Util.logSetting(log, "Available processors", Runtime.getRuntime().availableProcessors());
        Util.logSetting(log, "Available heap", (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");

        SimpleDateFormat df = new SimpleDateFormat("z Z");
        df.setTimeZone(TimeZone.getDefault());
        String offset = df.format(new Date());
        Util.logSetting(log, "JVM timezone", String.format("%s (%s)", TimeZone.getDefault().getID(), offset));
        Util.logSetting(log, "JVM locale", Locale.getDefault());

        long startTime = System.nanoTime();
        getServer().start();

        String hostname = settings.getHost();
        String contextPath = settings.getContextPath();

        if (settings.getHttpsPort() > 0) {
            log.info("https://{}:{}{}", hostname, settings.getHttpsPort(), contextPath);
        }
        if (settings.getHttpPort() > 0) {
            log.info("http://{}:{}{}", hostname, settings.getHttpPort(), contextPath);
        }
        if (settings.getAjpPort() > 0) {
            log.info("ajp://{}:{}{}", hostname, settings.getAjpPort(), contextPath);
        }

        long delta = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        String duration;
        if (delta < 1000L) {
            duration = String.format("%s ms", delta);
        } else {
            duration = String.format("%.1f seconds", (delta / 1000f));
        }
        log.info("Fathom bootstrapped {} mode in {}", settings.getMode().toString(), duration);
        log.info("READY.");

    }

    /**
     * Stops Fathom synchronously.
     */
    @Override
    public synchronized void stop() {
        Preconditions.checkNotNull(getServer());
        if (getServer().isRunning()) {
            try {
                log.info("Stopping...");
                getServer().stop();
                log.info("STOPPED.");
            } catch (Exception e) {
                Throwable t = Throwables.getRootCause(e);
                log.error("Fathom failed on shutdown!", t);
            }
        }
    }

    /**
     * Called by prunsrv (Windows) or jsrv (UNIX) after stop().
     */
    @Override
    public void destroy() {
        log.debug("Destroyed Fathom");
    }

    /**
     * Setup Logback logging by optionally reloading the configuration file.
     */
    protected void setupLogback() {

        // Check for Logback config file System Property
        // http://logback.qos.ch/manual/configuration.html
        //     -Dlogback.configurationFile=logback_prod.xml
        if (System.getProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY) != null) {
            // Logback already configured
            return;
        }

        // Check for a logback configuration file declared in Fathom settings
        URL configFileUrl = settings.getFileUrl(LOGBACK_CONFIGURATION_FILE_PROPERTY, "classpath:conf/logback.xml");

        if (configFileUrl == null) {
            throw new FathomException("Failed to find Logback config file '{}'",
                    settings.getString(LOGBACK_CONFIGURATION_FILE_PROPERTY, "classpath:conf/logback.xml"));
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try (InputStream is = configFileUrl.openStream()) {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(is);
            log.info("Configured Logback from '{}'", configFileUrl);
        } catch (IOException | JoranException je) {
            // StatusPrinter will handle this
        }

        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }
}
