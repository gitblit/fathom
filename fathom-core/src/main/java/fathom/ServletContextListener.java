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

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import fathom.conf.Settings;

import javax.servlet.ServletContextEvent;

/**
 * ServletContextListener is an extension of the GuiceServlet API and
 * is the entry point from the Servlet API into your Fathom application.
 * <p>
 * It creates the Fathom Core instance which, in turn, creates the Guice
 * Injector for the Fathom application.
 * </p>
 */
class ServletContextListener extends GuiceServletContextListener {

    private final Core core;

    public ServletContextListener(Settings settings) {
        super();
        this.core = new Core(settings);
    }

    @Override
    public synchronized void contextInitialized(ServletContextEvent servletContextEvent) {
        super.contextInitialized(servletContextEvent);
    }

    @Override
    public synchronized void contextDestroyed(ServletContextEvent servletContextEvent) {
        core.shutdown();
        super.contextDestroyed(servletContextEvent);
    }

    @Override
    public synchronized Injector getInjector() {
        return core.getInjector();
    }

}
