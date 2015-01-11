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

package fathom.rest;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.multibindings.Multibinder;
import fathom.ServletsModule;
import ro.pippo.core.Application;
import ro.pippo.core.ContentTypeEngines;
import ro.pippo.core.ErrorHandler;
import ro.pippo.core.Languages;
import ro.pippo.core.Messages;
import ro.pippo.core.PippoSettings;
import ro.pippo.core.RuntimeMode;
import ro.pippo.core.SessionFactory;
import ro.pippo.core.TemplateEngine;
import ro.pippo.core.route.Router;
import ro.pippo.core.util.HttpCacheToolkit;
import ro.pippo.core.util.MimeTypes;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * @author James Moger
 */
public class RestModule extends ServletsModule {

    @Override
    protected void setup() {
        String basePath = Strings.nullToEmpty(getSettings().getString(RestServlet.SETTING_URL, null)).trim();
        serve(basePath + "/*").with(RestServlet.class);

        RuntimeMode runtimeMode = RuntimeMode.PROD;
        for (RuntimeMode mode : RuntimeMode.values()) {
            if (mode.name().equalsIgnoreCase(getSettings().getMode().toString())) {
                runtimeMode = mode;
            }
        }

        final Properties properties = getSettings().toProperties();

        final PippoSettings pippoSettings = new PippoSettings(runtimeMode);
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            String propertyValue;
            if (value instanceof Collection) {
                // strip brackets from collection [en,de,ru]
                propertyValue = Optional.fromNullable(value).or("").toString();
                propertyValue = propertyValue.substring(1, propertyValue.length() - 1);
            } else {
                // Object.toString()
                propertyValue = Optional.fromNullable(value).or("").toString();
            }
            String propertyName = entry.getKey().toString();
            pippoSettings.overrideSetting(propertyName, propertyValue);
        }

        final Application application = new Application(pippoSettings);

        // must set context path before starting application
        application.getRouter().setContextPath(getSettings().getContextPath());

        // start the application which sets up all initializers
        application.init();

        bind(Application.class).toInstance(application);
        bind(Router.class).toInstance(application.getRouter());
        bind(Messages.class).toInstance(application.getMessages());
        bind(Languages.class).toInstance(application.getLanguages());
        bind(MimeTypes.class).toInstance(application.getMimeTypes());
        bind(ErrorHandler.class).toInstance(application.getErrorHandler());
        bind(TemplateEngine.class).toInstance(application.getTemplateEngine());
        bind(SessionFactory.class).toInstance(application.getSessionFactory());
        bind(HttpCacheToolkit.class).toInstance(application.getHttpCacheToolkit());
        bind(ContentTypeEngines.class).toInstance(application.getContentTypeEngines());

        bind(RestService.class);

    }

}
