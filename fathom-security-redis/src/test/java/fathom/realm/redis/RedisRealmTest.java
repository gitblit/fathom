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

package fathom.realm.redis;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import fathom.realm.Account;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests the RedisRealm.
 *
 * @author James Moger
 */
public class RedisRealmTest extends Assert {

    private Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("url", "redis://localhost:6379/8");
        settings.put("passwordMapping", "fathom:${username}:password");
        settings.put("nameMapping", "fathom:${username}:name");
        settings.put("emailMapping", "fathom:${username}:email");
        settings.put("roleMapping", "fathom:${username}:roles");
        settings.put("permissionMapping", "fathom:${username}:permissions");
        settings.put("startScript", "classpath:conf/realm.redis");
        return settings;
    }

    private Config getConfig(Map<String, Object> settings) {
        Config config = ConfigFactory.parseMap(settings);
        return config;
    }

    private Config getConfig() {
        return getConfig(getSettings());
    }

    private RedisRealm getRealm(Config config) {
        RedisRealm realm = new RedisRealm();
        realm.setup(config);
        realm.start();
        return realm;
    }

    private RedisRealm getRealm() {
        return getRealm(getConfig());
    }

    @Test
    public void basicTest() {
        RedisRealm realm = getRealm();
        Account account = null;

        // Phineas
        account = realm.authenticate("phineas", "fail");
        assertNull(account);
        account = realm.authenticate("phineas", "iKnowWhatWereGonnaDoToday");
        assertNotNull(account);
        assertEquals("phineas", account.getUsername());
        assertEquals("Phineas Flynn", account.getName());
        assertTrue(account.getEmailAddresses().contains("phineas.flynn@disney.com"));
        assertTrue(account.hasRole("inventor"));
        assertTrue(account.isPermitted("secure:view"));

        // Ferb
        account = realm.authenticate("ferb", "fail");
        assertNull(account);
        account = realm.authenticate("ferb", "ferb");
        assertNotNull(account);
        assertEquals("ferb", account.getUsername());
        assertEquals("Ferb Fletcher", account.getName());
        assertTrue(account.getEmailAddresses().contains("ferb.fletcher@disney.com"));
        assertTrue(account.hasRole("inventor"));
        assertTrue(account.isPermitted("secure:view"));

        // Candace
        account = realm.authenticate("candace", "fail");
        assertNull(account);
        account = realm.authenticate("candace", "ilovejeremy");
        assertNotNull(account);
        assertEquals("candace", account.getUsername());
        assertEquals("Candace Flynn", account.getName());
        assertTrue(account.getEmailAddresses().contains("candace.flynn@disney.com"));
        assertFalse(account.hasRole("inventor"));
        assertTrue(account.isPermitted("secure:view"));

        // Linda
        account = realm.authenticate("linda", "fail");
        assertNull(account);
        account = realm.authenticate("linda", "imLindanaAndIWannaHaveFun");
        assertNotNull(account);
        assertEquals("linda", account.getUsername());
        assertEquals("Linda Flynn-Fletcher", account.getName());
        assertTrue(account.getEmailAddresses().contains("linda.fletcher@disney.com"));
        assertFalse(account.hasRole("inventor"));
        assertFalse(account.isPermitted("secure:view"));

        // Doofenshmirtz
        account = realm.authenticate("heinz", "CurseYouPerryThePlatypus");
        assertNull(account);

    }
}