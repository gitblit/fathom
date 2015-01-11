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

import com.google.inject.AbstractModule;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import fathom.conf.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for a Fathom components Module.
 *
 * @author James Moger
 */
public abstract class Module extends AbstractModule {

    private final static Logger log = LoggerFactory.getLogger(Module.class);

    private Services services;

    private Settings settings;

    protected Settings getSettings() {
        return settings;
    }

    void setSettings(Settings settings) {
        this.settings = settings;
    }

    void setServices(Services services) {
        this.services = services;
    }

    /**
     * @see com.google.inject.Binder#bind(Class)
     */
    @Override
    protected <T> AnnotatedBindingBuilder<T> bind(Class<T> clazz) {
        if (Service.class.isAssignableFrom(clazz)) {
            Class<? extends Service> serviceClass = (Class<? extends Service>) clazz;
            services.register(serviceClass);
        }
        return binder().bind(clazz);
    }

    /**
     * Register a Service instance.
     *
     * @param service
     */
    protected void register(Service service) {
        services.register(service);
    }

    protected <T, X extends T> void multibind(Class<T> baseClass, Class<X> implementationClass) {
        Multibinder<T> binderSet = Multibinder.newSetBinder(binder(), baseClass);
        binderSet.addBinding().to(implementationClass);
    }

    /**
     * Install a Fathom Module.
     */
    protected void install(Module module) {
        module.setSettings(settings);
        module.setServices(services);
        binder().install(module);
    }

    /**
     * @see com.google.inject.Binder#install(com.google.inject.Module)
     */
    protected void install(com.google.inject.Module module) {
        binder().install(module);
    }

    @Override
    protected final void configure() {
        log.debug("Setup module '{}'", getClass().getName());
        setup();
    }

    protected abstract void setup();
}