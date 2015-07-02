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

package fathom.test;

import com.google.inject.Injector;
import com.jayway.restassured.RestAssured;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * Fathom Test marries Fathom, JUnit, & RestAssured to provide a convenient
 * way to perform integration tests of your Fathom application.
 * <p>
 * FathomTest will start your Fathom application on a dynamically assigned port
 * in TEST mode for execution of your unit tests.
 * </p>
 *
 * @author James Moger
 */
abstract class FathomTest extends Assert {

    protected static String XML = "application/xml";

    protected static String JSON = "application/json";

    protected abstract TestBoot getTestBoot();

    /**
     * Returns the complete URL of the specified path for the running Fathom TEST instance.
     *
     * @param path
     * @return an url
     */
    protected String urlFor(String path) {
        String url = StringUtils.removeEnd(RestAssured.baseURI, "/") + StringUtils.prependIfMissing(path, "/");
        return url;
    }

    /**
     * Returns the Guice injector of the Fathom TEST instance.
     * This allows you to retrieve any object bound by your application.
     *
     * @return the Guice injector
     */
    protected Injector getInjector() {
        return getTestBoot().getServer().getInjector();
    }

    /**
     * Use the Guice injector of the Fathom TEST instance to retrieve a bound object.
     *
     * @param classOfT
     * @param <T>
     * @return an object of type T
     */
    protected <T> T getInstance(Class<T> classOfT) {
        T t = getInjector().getInstance(classOfT);
        return t;
    }

}
