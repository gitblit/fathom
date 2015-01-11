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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import fathom.conf.Fathom;
import fathom.conf.Ftm;
import fathom.conf.Settings;
import fathom.exception.FathomException;
import fathom.utils.ClassUtil;
import fathom.utils.RequireUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

/**
 * Core is responsible for registering all binding all Fathom Modules & Services.
 * <p>
 * Core creates the Guice Injector.
 * </p>
 *
 * @author James Moger
 */
class Core {

    private static final Logger log = LoggerFactory.getLogger(Core.class);

    private static final String COMPONENTS_CLASS = "conf.Components";
    private static final String SERVLETS_CLASS = "conf.Servlets";
    private static final String FATHOM_CLASS = "conf.Fathom";

    private final Settings settings;
    private Services services;
    private Injector injector;

    Core(Settings settings) {
        Preconditions.checkNotNull(settings);

        this.settings = settings;
        this.services = new Services(settings);
    }

    synchronized Injector getInjector() {
        if (injector == null) {
            startup();
        }
        return injector;
    }

    synchronized void startup() {

        Preconditions.checkState(injector == null, "Fathom has already been started!");

        long startTime = System.nanoTime();

        String border = Strings.padEnd("", Constants.MIN_BORDER_LENGTH, '-');
        Optional<String> applicationPackage = Optional.fromNullable(settings.getApplicationPackage());

        log.info(border);
        log.info("Initializing Guice injector");
        log.info(border);
        injector = initializeInjector(applicationPackage);

        Preconditions.checkNotNull(injector, "Failed to initialize Guice injector!");

        log.info(border);
        log.info("Starting services");
        log.info(border);
        services.start(injector);

        Fathom fathom = injector.getInstance(Fathom.class);
        log.info("Starting Fathom '{}'", fathom.getClass().getName());
        initializeMetadata(fathom);
        fathom.onStartup();

        long bootTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        String name = settings.getApplicationName();
        String version = settings.getApplicationVersion();
        log.info("{} {} initialized in {} ms", name, version, bootTime);
    }

    synchronized void shutdown() {

        Preconditions.checkNotNull(injector, "Shutdown not clean => injector already null.");

        Fathom fathom = injector.getInstance(Fathom.class);
        fathom.onShutdown();

        services.stop();

        injector = null;
        services = null;

    }

    private Injector initializeInjector(Optional<String> applicationPackage) {

        try {
            List<AbstractModule> modules = new ArrayList<>();

            log.info("adding Fathom core components module");
            final Class<? extends Fathom> fathomClass = getFathomClass(applicationPackage);
            modules.add(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Fathom.class).to(fathomClass).in(Singleton.class);
                    bind(Services.class).toInstance(services);
                    bind(Settings.class).toInstance(settings);
                }
            });

            // add discovered modules
            ServiceLoader<fathom.Module> discoveredModules = ServiceLoader.load(fathom.Module.class);
            for (fathom.Module discoveredModule : discoveredModules) {
                Class<? extends Module> moduleClass = discoveredModule.getClass();

                if (RequireUtil.allowClass(settings, moduleClass)) {
                    log.info("adding '{}'", moduleClass.getName());
                    discoveredModule.setSettings(settings);
                    discoveredModule.setServices(services);
                    modules.add(discoveredModule);
                }
            }

            // add discovered servlet modules
            ServiceLoader<ServletsModule> discoveredServletModules = ServiceLoader.load(ServletsModule.class);
            for (ServletsModule discoveredModule : discoveredServletModules) {
                Class<? extends ServletsModule> moduleClass = discoveredModule.getClass();

                if (RequireUtil.allowClass(settings, moduleClass)) {
                    log.info("adding '{}'", moduleClass.getName());
                    discoveredModule.setSettings(settings);
                    discoveredModule.setServices(services);
                    modules.add(discoveredModule);
                }
            }

            // add application components module
            AbstractModule componentsModule = initializeModule(applicationPackage, COMPONENTS_CLASS);
            if (componentsModule != null) {
                log.info("adding '{}'", componentsModule.getClass().getName());
                modules.add(componentsModule);
            }

            // add application servlets module
            AbstractModule servletsModule = initializeModule(applicationPackage, SERVLETS_CLASS);
            if (servletsModule != null) {
                log.info("adding '{}'", servletsModule.getClass().getName());
                modules.add(servletsModule);
            }

            // create the Guice injector
            injector = Guice.createInjector(Stage.PRODUCTION, modules);

            return injector;

        } catch (FathomException e) {
            throw e;
        } catch (Exception e) {
            throw new FathomException(e, "Failed to create a Guice injector!");
        }
    }

    private AbstractModule initializeModule(Optional<String> applicationPackage, String className) {
        String fullClassName = ClassUtil.buildClassName(applicationPackage, className);
        if (ClassUtil.doesClassExist(fullClassName)) {

            Class<?> moduleClass = ClassUtil.getClass(fullClassName);
            if (fathom.Module.class.isAssignableFrom(moduleClass)) {

                fathom.Module module = (fathom.Module) ClassUtil.newInstance(moduleClass);
                module.setSettings(settings);
                module.setServices(services);
                return module;

            } else if (ServletsModule.class.isAssignableFrom(moduleClass)) {

                ServletsModule module = (ServletsModule) ClassUtil.newInstance(moduleClass);
                module.setSettings(settings);
                module.setServices(services);
                return module;

            } else {
                throw new FathomException("'{}' must either be a subclass of '{}' or '{}'", moduleClass.getName(),
                        fathom.Module.class.getName(), ServletsModule.class.getName());
            }

        } else if (applicationPackage.isPresent()) {
            log.warn("Module '{}' not found on classpath!", fullClassName);
        }

        return null;
    }

    private Class<? extends Fathom> getFathomClass(Optional<String> applicationPackage) {

        String fathomClassName = ClassUtil.buildClassName(applicationPackage, FATHOM_CLASS);
        final Class<? extends Fathom> fathomClass;
        if (ClassUtil.doesClassExist(fathomClassName)) {
            final Class<?> specifiedClass = ClassUtil.getClass(fathomClassName);
            if (Fathom.class.isAssignableFrom(specifiedClass)) {
                fathomClass = (Class<? extends Fathom>) specifiedClass;
            } else {
                throw new FathomException("Your Fathom class '{}' does not implement '{}'!", fathomClassName,
                        Fathom.class.getName());
            }
        } else {
            // use the default Fathom implementation
            fathomClass = Ftm.class;
        }

        return fathomClass;
    }

    private void initializeMetadata(Fathom fathom) {
        Class<? extends Fathom> fathomClass = fathom.getClass();
        if (fathomClass.isAnnotationPresent(fathom.conf.Application.class)) {
            log.debug("Setting Application name & version from '{}'", fathomClass);
            fathom.conf.Application app = fathomClass.getAnnotation(fathom.conf.Application.class);
            settings.overrideSetting(Settings.Setting.application_name, app.name());
            settings.overrideSetting(Settings.Setting.application_version, app.version());
        }
    }
}
