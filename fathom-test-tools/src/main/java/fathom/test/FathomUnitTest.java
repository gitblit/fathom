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

import com.jayway.restassured.RestAssured;
import org.junit.After;
import org.junit.Before;

/**
 * Fathom Test marries Fathom, JUnit, & RestAssured to provide a convenient
 * way to perform integration tests of your Fathom application.
 * <p>
 * FathomTest will start your Fathom application on a dynamically assigned port
 * in TEST mode for execution of your unit tests.
 * </p>
 * <p>
 * FathomUnitTest starts up an instance of Fathom per unit test.
 * </p>
 * @author James Moger
 */
public abstract class FathomUnitTest extends FathomTest {

    protected final TestBoot testBoot;

    public FathomUnitTest() {
        testBoot = new TestBoot();
    }

    @Override
    protected TestBoot getTestBoot() {
        return testBoot;
    }

    /**
     * Starts Fathom in TEST mode and configures RestAssured with the base URL of the test instance.
     */
    @Before
    public void startFathom() {
        testBoot.start();
        String url = testBoot.getSettings().getFathomUrl();
        RestAssured.baseURI = url;
    }

    /**
     * Stops the Fathom test instance.
     */
    @After
    public void stopFathom() {
        testBoot.stop();
    }
}
