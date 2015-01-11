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

package fathom.rest.route;

import com.google.common.base.Strings;
import fathom.rest.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.Languages;
import ro.pippo.core.route.RouteHandler;

import java.util.Locale;

/**
 * The LanguageHandler determines the appropriate language, binds the lang
 * and locale Response models, and continues the handler chain.
 *
 * @author James Moger
 */
public class LanguageHandler implements RouteHandler<Context> {

    public static class Parameter {
        public static final String LANG = "lang";
        public static final String LOCALE = "locale";
        public static final String LANGUAGES = "languages";
    }

    private static final Logger log = LoggerFactory.getLogger(LanguageHandler.class);

    protected final Languages languages;
    protected final boolean enableQueryParameter;
    protected final boolean setCookie;

    /**
     * Create the language filter with optional support for accepting the
     * language specification from a query parameter (e.g. "?lang=LN")
     *
     * @param languages
     * @param enableQueryParameter
     */
    public LanguageHandler(Languages languages, boolean enableQueryParameter, boolean setCookie) {
        this.languages = languages;
        this.enableQueryParameter = enableQueryParameter;
        this.setCookie = setCookie;
    }

    @Override
    public void handle(Context context) {
        String language = enableQueryParameter ? context.getParameter(Parameter.LANG).toString() : null;

        if (Strings.isNullOrEmpty(language)) {
            language = languages.getLanguageOrDefault(context);
        }
        Locale locale = languages.getLocaleOrDefault(language);

        context.setLocal(Parameter.LANG, language);
        context.setLocal(Parameter.LOCALE, locale);
        context.setLocal(Parameter.LANGUAGES, languages.getRegisteredLanguages());

        if (setCookie) {
            if (context.getResponse().isCommitted()) {
                log.debug("LANG cookie NOT set, Response already committed!");
            } else {
                languages.setLanguageCookie(language, context);
            }
        }

        context.next();
    }

}

