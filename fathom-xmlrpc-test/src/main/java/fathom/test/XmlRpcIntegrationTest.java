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

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.URL;

/**
 * Base class for exercising XMLRPC integration tests.
 * Each unit test starts an instance of our Fathom app in TEST mode.
 */
public abstract class XmlRpcIntegrationTest extends FathomIntegrationTest {

    protected <X> X callAnon(String methodName, Object... args) {
        return call(null, null, "/RPC2", methodName, args);
    }

    protected <X> X call(String username, String password, String path, String methodName, Object... args) {
        try {
            URL url = new URL(getTestBoot().getSettings().getUrl() + StringUtils.removeStart(path, "/"));
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setBasicUserName(username);
            config.setBasicPassword(password);
            config.setServerURL(url);
            XmlRpcClient xmlrpc = new XmlRpcClient();
            xmlrpc.setConfig(config);
            Object x = xmlrpc.execute(methodName, args);
            return (X) x;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}