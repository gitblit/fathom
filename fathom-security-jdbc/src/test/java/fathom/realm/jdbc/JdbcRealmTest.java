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

package fathom.realm.jdbc;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import fathom.realm.Account;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests the Fathom JdbcRealm.
 *
 * @author James Moger
 */
public class JdbcRealmTest extends Assert {

    private Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("url", "jdbc:h2:mem:fathom");
        settings.put("username", "");
        settings.put("password", "");

        // define our initial script to create our test tables & data
        settings.put("startScript", "classpath:conf/realm.sql");

        settings.put("accountQuery", "select * from accounts where username=?");
        settings.put("passwordMapping", "password");
        settings.put("nameMapping", "name");
        settings.put("emailMapping", "email");

        settings.put("accountRolesQuery", "select role from account_roles where username=?");
        settings.put("accountPermissionsQuery", "select permission from account_permissions where username=?");
        settings.put("definedRolesQuery", "select role, definition from defined_roles");

        settings.put("hikariCP.connectionTimeout", "5000");
        settings.put("hikariCP.registerMbeans", "true");

        return settings;
    }

    private Config getConfig(Map<String, Object> settings) {
        Config config = ConfigFactory.parseMap(settings);
        return config;
    }

    private Config getConfig() {
        return getConfig(getSettings());
    }

    private JdbcRealm getRealm(Config config) {
        JdbcRealm realm = new JdbcRealm();
        realm.setup(config);
        realm.start();
        return realm;
    }

    private JdbcRealm getRealm() {
        return getRealm(getConfig());
    }

    @Test
    public void basicTest() {
        JdbcRealm realm = getRealm();

        Account account = realm.authenticate("gjetson", "astro1");
        assertNull("Authentication succeeded with wrong password", account);

        // George Jetson
        account = realm.authenticate("gjetson", "astro");
        assertNotNull("Authentication failed", account);
        assertEquals("Name mappingfailed", "George Jetson", account.getName());
        assertTrue("Email address mapping failed", account.getEmailAddresses().contains("george@spacelyspacesprockets.com"));
        assertTrue("Role lookup failed", account.hasRole("buttonpusher"));
        assertTrue("Permission lookup failed", account.isPermitted("powers:sleeping"));
        assertFalse("Defined role lookup failed", account.isPermitted("secure:write"));
        assertFalse("Role lookup failed", account.hasRole("admin"));

        // Jane Jetson
        account = realm.authenticate("jjetson", "george");
        assertNotNull("Authentication failed", account);
        assertEquals("Name mappingfailed", "Jane Jetson", account.getName());
        assertTrue("Email address mapping failed", account.getEmailAddresses().contains("jane@spacelyspacesprockets.com"));
        assertTrue("Permission lookup failed", account.isPermitted("powers:*"));
        assertTrue("Defined role lookup failed", account.isPermitted("secure:*"));
        assertTrue("Role lookup failed", account.hasRole("admin"));
    }
}