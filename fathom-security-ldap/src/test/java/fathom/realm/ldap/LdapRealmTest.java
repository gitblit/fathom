/*
 * Copyright 2012 John Crygier
 * Copyright 2012 gitblit.com
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
package fathom.realm.ldap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldif.LDIFReader;
import fathom.realm.Account;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * An Integration test for LDAP that tests going against an in-memory UnboundID
 * LDAP server.
 *
 * @author John Crygier
 * @author James Moger
 */
public class LdapRealmTest extends Assert {

    static int ldapPort = 1389;
    private static InMemoryDirectoryServer ds;
    private LdapRealm ldap;

    @BeforeClass
    public static void createInMemoryLdapServer() throws Exception {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=MyDomain");
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", ldapPort));
        config.setSchema(null);

        ds = new InMemoryDirectoryServer(config);
        ds.startListening();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        ds.shutDown(true);
    }

    @Before
    public void init() throws Exception {
        ds.clear();
        ds.importFromLDIF(true, new LDIFReader(getClass().getResourceAsStream("/conf/realm.ldif")));

        Map<String, Object> settings = getSettings();
        ldap = newRealm(settings);
    }

    private Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("url", "ldap://localhost:" + ldapPort);
        settings.put("username", "cn=Directory Manager");
        settings.put("password", "password");
        settings.put("accountBase", "OU=Users,OU=UserControl,OU=MyOrganization,DC=MyDomain");
        settings.put("accountPattern", "(&(objectClass=person)(sAMAccountName=${username}))");
        settings.put("groupBase", "OU=Groups,OU=UserControl,OU=MyOrganization,DC=MyDomain");
        settings.put("groupMemberPattern", "(&(objectClass=group)(member=${dn}))");
        settings.put("adminGroups", Arrays.asList("@UserThree", "Git_Admins", "Git Admins"));
        settings.put("nameMapping", "displayName");
        settings.put("emailMapping", "email");
        return settings;
    }

    private LdapRealm newRealm(Map<String, Object> settings) {
        Config config = ConfigFactory.parseMap(settings);
        LdapRealm ldap = new LdapRealm();
        ldap.setup(config);
        return ldap;
    }

    @Test
    public void testAuthenticate() {
        Account userOne = ldap.authenticate("UserOne", "userOnePassword");
        assertNotNull(userOne);
        assertTrue("UserOne is missing the 'Git_Admins' role!", userOne.hasRole("Git_Admins"));
        assertTrue("UserOne is missing the 'Git_Users' role!", userOne.hasRole("Git_Users"));
        assertTrue("UserOne should be an administrator!", userOne.isAdministrator());

        Account userOneAuthenticationFailed = ldap.authenticate("UserOne", "userTwoPassword");
        assertNull(userOneAuthenticationFailed);

        Account userTwo = ldap.authenticate("UserTwo", "userTwoPassword");
        assertNotNull(userTwo);
        assertTrue("UserTwo is missing the 'Git_Users' role!", userTwo.hasRole("Git_Users"));
        assertFalse("UserTwo has the 'Git_Admins' role!", userTwo.hasRole("Git_Admins"));
        assertTrue("UserTwo is missing the 'Git Admins' role!", userTwo.hasRole("Git Admins"));
        assertTrue("UserTwo should be an administrator!", userTwo.isAdministrator());

        Account userThree = ldap.authenticate("UserThree", "userThreePassword");
        assertNotNull(userThree);
        assertTrue("UserThree is missing the 'Git_Users' role!", userThree.hasRole("Git_Users"));
        assertFalse("UserThree has the 'Git_Admins role!", userThree.hasRole("Git_Admins"));
        assertTrue("UserThree should be an administrator!", userThree.isAdministrator());
    }

    @Test
    public void testSimpleName() {
        Account userOne = ldap.authenticate("UserOne", "userOnePassword");
        assertNotNull(userOne);
        assertEquals("User One", userOne.getName());
    }

    @Test
    public void testComplexName() {
        Map<String, Object> settings = getSettings();
        settings.put("nameMapping", "${personalTitle}. ${givenName} ${surname}");
        ldap = newRealm(settings);

        Account userOne = ldap.authenticate("UserOne", "userOnePassword");
        assertNotNull(userOne);
        assertEquals("Mr. User One", userOne.getName());
    }

    @Test
    public void testSimpleEmail() {
        Account userOne = ldap.authenticate("UserOne", "userOnePassword");
        assertNotNull(userOne);
        assertTrue(userOne.getEmailAddresses().contains("userone@gitblit.com"));
    }

    @Test
    public void testComplexEmail() {
        Map<String, Object> settings = getSettings();
        settings.put("emailMapping", "${givenName}.${surname}@gitblit.com");
        ldap = newRealm(settings);

        Account userOne = ldap.authenticate("UserOne", "userOnePassword");
        assertNotNull(userOne);
        assertTrue(userOne.getEmailAddresses().contains("User.One@gitblit.com"));
    }

    @Test
    public void testLdapInjection() {
        // Inject so
        //   "(&(objectClass=person)(sAMAccountName=${username}))"
        // becomes
        //   "(&(objectClass=person)(sAMAccountName=*)(userPassword=userOnePassword))"
        // Thus searching by password
        Account userOneModel = ldap.authenticate("*)(userPassword=userOnePassword", "userOnePassword");
        assertNull(userOneModel);
    }

    @Test
    public void testBindWithUser() {
        Map<String, Object> settings = getSettings();
        settings.put("bindPattern", "CN=${username},OU=US,OU=Users,OU=UserControl,OU=MyOrganization,DC=MyDomain");
        settings.put("username", "");
        settings.put("password", "");

        ldap = newRealm(settings);

        Account userOne = ldap.authenticate("UserOne", "userOnePassword");
        assertNotNull(userOne);

        Account userOneFailedAuth = ldap.authenticate("UserOne", "userTwoPassword");
        assertNull(userOneFailedAuth);
    }

}
