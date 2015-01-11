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
package fathom.realm.htpasswd;

import fathom.realm.Account;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Test the Htpasswd Realm.
 */
public class HtpasswdRealmTest extends Assert {

    HtpasswdRealm htpasswd;

    @Before
    public void setUp() {
        htpasswd = new HtpasswdRealm();
        htpasswd.setFile(new File(getClass().getResource("/conf/realm.htpasswd").getFile()));
    }

    @Test
    public void testAuthenticate() {

        htpasswd.setAllowClearTextPasswords(true);

        Account account = htpasswd.authenticate("user1", "pass1");
        assertNotNull(account);
        assertEquals("user1", account.getUsername());

        account = htpasswd.authenticate("user2", "pass2");
        assertNotNull(account);
        assertEquals("user2", account.getUsername());

        // Test different encryptions
        account = htpasswd.authenticate("plain", "passWord");
        assertNotNull(account);
        assertEquals("plain", account.getUsername());

        htpasswd.setAllowClearTextPasswords(false);
        account = htpasswd.authenticate("crypt", "password");
        assertNotNull(account);
        assertEquals("crypt", account.getUsername());

        account = htpasswd.authenticate("md5", "password");
        assertNotNull(account);
        assertEquals("md5", account.getUsername());

        account = htpasswd.authenticate("sha", "password");
        assertNotNull(account);
        assertEquals("sha", account.getUsername());


        // Test leading and trailing whitespace
        account = htpasswd.authenticate("trailing", "whitespace");
        assertNotNull(account);
        assertEquals("trailing", account.getUsername());

        account = htpasswd.authenticate("tabbed", "frontAndBack");
        assertNotNull(account);
        assertEquals("tabbed", account.getUsername());

        account = htpasswd.authenticate("leading", "whitespace");
        assertNotNull(account);
        assertEquals("leading", account.getUsername());
    }

    @Test
    public void testAuthenticateDenied() {
        Account account = null;

        htpasswd.setAllowClearTextPasswords(true);

        account = htpasswd.authenticate("user1", "");
        assertNull("User 'user1' falsely authenticated.", account);

        account = htpasswd.authenticate("user1", "pass2");
        assertNull("User 'user1' falsely authenticated.", account);

        account = htpasswd.authenticate("user2", "lalala");
        assertNull("User 'user2' falsely authenticated.", account);


        account = htpasswd.authenticate("user3", "disabled");
        assertNull("User 'user3' falsely authenticated.", account);

        account = htpasswd.authenticate("user4", "disabled");
        assertNull("User 'user4' falsely authenticated.", account);

        account = htpasswd.authenticate("plain", "text");
        assertNull("User 'plain' falsely authenticated.", account);

        htpasswd.setAllowClearTextPasswords(false);

        account = htpasswd.authenticate("crypt", "");
        assertNull("User 'cyrpt' falsely authenticated.", account);

        account = htpasswd.authenticate("crypt", "passwd");
        assertNull("User 'crypt' falsely authenticated.", account);

        account = htpasswd.authenticate("md5", "");
        assertNull("User 'md5' falsely authenticated.", account);

        account = htpasswd.authenticate("md5", "pwd");
        assertNull("User 'md5' falsely authenticated.", account);

        account = htpasswd.authenticate("sha", "");
        assertNull("User 'sha' falsely authenticated.", account);

        account = htpasswd.authenticate("sha", "letmein");
        assertNull("User 'sha' falsely authenticated.", account);
    }

    @Test
    public void testCleartextIntrusion() {

        htpasswd.setAllowClearTextPasswords(true);

        assertNull(htpasswd.authenticate("md5", "$apr1$qAGGNfli$sAn14mn.WKId/3EQS7KSX0"));
        assertNull(htpasswd.authenticate("sha", "{SHA}W6ph5Mm5Pz8GgiULbPgzG37mj9g="));

        assertNull(htpasswd.authenticate("user1", "#externalAccount"));

        htpasswd.setAllowClearTextPasswords(false);

        assertNull(htpasswd.authenticate("md5", "$apr1$qAGGNfli$sAn14mn.WKId/3EQS7KSX0"));
        assertNull(htpasswd.authenticate("sha", "{SHA}W6ph5Mm5Pz8GgiULbPgzG37mj9g="));

        assertNull(htpasswd.authenticate("user1", "#externalAccount"));
    }

    @Test
    public void testCryptVsPlaintext1() {

        htpasswd.setAllowClearTextPasswords(false);
        assertNull(htpasswd.authenticate("crypt", "6TmlbxqZ2kBIA"));
        assertNotNull(htpasswd.authenticate("crypt", "password"));

    }

    @Test
    public void testCryptVsPlaintext2() {

        htpasswd.setAllowClearTextPasswords(true);
        assertNotNull(htpasswd.authenticate("crypt", "6TmlbxqZ2kBIA"));
        assertNull(htpasswd.authenticate("crypt", "password"));
    }

}
