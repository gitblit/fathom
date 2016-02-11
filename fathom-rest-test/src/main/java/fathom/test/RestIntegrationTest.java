/*
 * Copyright (C) 2016 the original author or authors.
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
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.mapper.ObjectMapperDeserializationContext;
import com.jayway.restassured.mapper.ObjectMapperSerializationContext;
import fathom.exception.FathomException;
import org.junit.Before;
import ro.pippo.core.ContentTypeEngine;
import ro.pippo.core.ContentTypeEngines;

/**
 * @author James Moger
 */
public class RestIntegrationTest extends FathomIntegrationTest {

    @Before
    public void setupObjectMapper() {
        RestAssured.objectMapper(getObjectMapper());
    }

    /**
     * Creates an ObjectMapper for RestAssured using the Pippo ContentTypeEngines class.
     *
     * @return an ObjectMapper
     */
    protected ObjectMapper getObjectMapper() {
        final ContentTypeEngines contentTypeEngines = getInstance(ContentTypeEngines.class);
        return new ObjectMapper() {
            @Override
            public Object deserialize(ObjectMapperDeserializationContext context) {
                ContentTypeEngine contentTypeEngine = contentTypeEngines.getContentTypeEngine(context.getContentType());
                if (contentTypeEngine == null) {
                    throw new FathomException("No ContentTypeEngine registered for {}", context.getContentType());
                }
                return contentTypeEngine.fromString(context.getDataToDeserialize().asString(), context.getType());
            }

            @Override
            public Object serialize(ObjectMapperSerializationContext context) {
                ContentTypeEngine contentTypeEngine = contentTypeEngines.getContentTypeEngine(context.getContentType());
                if (contentTypeEngine == null) {
                    throw new FathomException("No ContentTypeEngine registered for {}", context.getContentType());
                }
                return contentTypeEngine.toString(context.getObjectToSerialize());
            }
        };
    }

}