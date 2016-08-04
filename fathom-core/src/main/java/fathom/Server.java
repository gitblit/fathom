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
import com.google.common.base.Strings;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import fathom.conf.Settings;
import fathom.exception.FathomException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Fathom's development and deployment engine is Undertow.
 *
 * @author James Moger
 * @see <a href="http://undertow.io">Undertow</a>
 */
public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private Settings settings;

    private volatile boolean started;

    private Undertow server;

    private DeploymentManager fathomDeploymentManager;

    public final Settings getSettings() {
        return settings;
    }

    public final void setSettings(Settings settings) {
        Preconditions.checkState(started == false, "Can not reassign settings after Undertow has been started!");
        this.settings = settings;
    }

    public final void start() {
        startImpl();
        started = true;
    }

    public final void stop() {
        stopImpl();
        started = false;
    }

    public boolean isRunning() {
        return started;
    }

    protected void startImpl() {
        try {
            fathomDeploymentManager = createFathomDeploymentManager();
            HttpHandler fathomHandler = fathomDeploymentManager.start();

            String contextPath = settings.getContextPath();

            // create a handler than redirects non-context requests to the context
            PathHandler contextHandler = Handlers.path(Handlers.redirect(contextPath));

            // add the handler with the context prefix
            contextHandler.addPrefixPath(contextPath, fathomHandler);

            GracefulShutdownHandler rootHandler = new GracefulShutdownHandler(contextHandler);
            server = createServer(rootHandler);

            String version = server.getClass().getPackage().getImplementationVersion();
            log.info("Starting Undertow {}", version);

            server.start();
        } catch (Exception e) {
            throw new FathomException(e);
        }
    }

    public Injector getInjector() {
        for (ListenerInfo listener : fathomDeploymentManager.getDeployment().getDeploymentInfo().getListeners()) {
            if (ServletContextListener.class == listener.getListenerClass()) {
                try {
                    ServletContextListener servletContextListener = (ServletContextListener) listener
                            .getInstanceFactory().createInstance().getInstance();
                    Injector injector = servletContextListener.getInjector();
                    return injector;
                } catch (Exception e) {
                    log.error("Failed to get Guice injector!", e);
                    return null;
                }
            }
        }

        return null;
    }

    protected void stopImpl() {
        if (server != null) {
            try {
                String version = server.getClass().getPackage().getImplementationVersion();
                log.info("Stopping Undertow {}", version);
                server.stop();

                fathomDeploymentManager.undeploy();
            } catch (Exception e) {
                throw new FathomException(e);
            }
        }
    }

    protected io.undertow.Undertow createServer(HttpHandler contextHandler) {

        int ports = settings.getHttpPort() + settings.getHttpsPort() + settings.getAjpPort();
        Preconditions.checkState(ports > 0, "No port specified! Please review your server port settings!");

        log.debug("Configuring Undertow engine");

        Builder builder = io.undertow.Undertow.builder();

        if (settings.getHttpPort() > 0) {
            // HTTP
            builder.addHttpListener(settings.getHttpPort(), settings.getHttpListenAddress());
            logSetting(Settings.Setting.undertow_httpPort, settings.getHttpPort());
        }

        if (settings.getHttpsPort() > 0) {
            // HTTPS
            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
            try {
                KeyStore keyStore = loadKeyStore(settings.getKeystoreFile(), settings.getKeystorePassword());
                KeyStore trustStore = loadKeyStore(settings.getTruststoreFile(), settings.getTruststorePassword());
                SSLContext sslContext = createSSLContext(keyStore, trustStore);
                builder.addHttpsListener(settings.getHttpsPort(), settings.getHttpsListenAddress(), sslContext);
                logSetting(Settings.Setting.undertow_httpsPort, settings.getHttpsPort());
            } catch (Exception e) {
                throw new FathomException(e, "Failed to setup an Undertow SSL listener!");
            }
        }

        if (settings.getAjpPort() > 0) {
            // AJP
            builder.addAjpListener(settings.getAjpPort(), settings.getAjpListenAddress());
            logSetting(Settings.Setting.undertow_ajpPort, settings.getAjpPort());
        }

        int ioThreads = settings.getInteger(Settings.Setting.undertow_ioThreads, 0);
        if (ioThreads > 0) {
            builder.setIoThreads(ioThreads);
            logSetting(Settings.Setting.undertow_ioThreads, ioThreads);
        }

        int workerThreads = settings.getInteger(Settings.Setting.undertow_workerThreads, 0);
        if (workerThreads > 0) {
            builder.setWorkerThreads(workerThreads);
            logSetting(Settings.Setting.undertow_workerThreads, workerThreads);
        }

        long bufferSize = settings.getBytes(Settings.Setting.undertow_bufferSize, null);
        if (bufferSize > 0) {
            builder.setBufferSize((int) bufferSize);
            logSetting(Settings.Setting.undertow_bufferSize, bufferSize);
        }

        builder.setHandler(contextHandler);
        io.undertow.Undertow server = builder.build();
        return server;
    }

    protected void logSetting(Settings.Setting setting, Object value) {
        log.debug("{}{}", Strings.padEnd(setting.toString(), 32, '.') , value);
    }

    protected DeploymentManager createFathomDeploymentManager() throws ServletException {

        DeploymentInfo info = Servlets.deployment();
        info.setDeploymentName("Fathom");
        info.setClassLoader(this.getClass().getClassLoader());
        info.setContextPath(settings.getContextPath());
        info.setIgnoreFlush(true);
        info.setDefaultEncoding("UTF-8");

        FilterInfo guiceFilter = new FilterInfo("GuiceFilter", GuiceFilter.class);
        guiceFilter.setAsyncSupported(true);

        info.addFilterUrlMapping("GuiceFilter", "/*", DispatcherType.REQUEST);
        info.addFilters(guiceFilter);

        ServletInfo defaultServlet = new ServletInfo("DefaultServlet", DefaultServlet.class);
        defaultServlet.setAsyncSupported(true);
        defaultServlet.addMapping("/");

        ServletContextListener fathomListener = new ServletContextListener(settings);

        info.addListeners(new ListenerInfo(ServletContextListener.class, new ImmediateInstanceFactory<>(fathomListener)));

        MultipartConfigElement multipartConfig = new MultipartConfigElement(settings.getUploadFilesLocation(), settings.getUploadFilesMaxSize(), -1L, 0);
        defaultServlet.setMultipartConfig(multipartConfig);
        info.addServlets(defaultServlet);

        DeploymentManager deploymentManager = Servlets.defaultContainer().addDeployment(info);
        deploymentManager.deploy();

        return deploymentManager;
    }

    private SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, settings.getKeystorePassword().toCharArray());
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

    private KeyStore loadKeyStore(String filename, String password) throws Exception {
        KeyStore loadedKeystore = KeyStore.getInstance("JKS");
        File file = new File(filename);
        if (file.exists()) {
            try (InputStream stream = new FileInputStream(file)) {
                loadedKeystore.load(stream, password.toCharArray());
            }
        } else {
            log.error("Failed to find keystore '{}'!", filename);
        }
        return loadedKeystore;
    }
}
