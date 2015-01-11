/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package fathom.authz;

import org.junit.Assert;
import org.junit.Test;

/**
 * This class was adapted from Apache Shiro.
 */
public class PermissionTest extends Assert {

    @Test(expected = IllegalArgumentException.class)
    public void testNull() {
        new Permission(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmpty() {
        new Permission("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlank() {
        new Permission("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnlyDelimiters() {
        new Permission("::,,::,:");
    }

    @Test
    public void testNamed() {
        Permission p1, p2;

        // Case insensitive, same
        p1 = new Permission("something");
        p2 = new Permission("something");
        assertTrue(p1.implies(p2));
        assertTrue(p2.implies(p1));

        // Case insensitive, different case
        p1 = new Permission("something");
        p2 = new Permission("SOMETHING");
        assertTrue(p1.implies(p2));
        assertTrue(p2.implies(p1));

        // Case insensitive, different word
        p1 = new Permission("something");
        p2 = new Permission("else");
        assertFalse(p1.implies(p2));
        assertFalse(p2.implies(p1));

        // Case sensitive same
        p1 = new Permission("BLAHBLAH", false);
        p2 = new Permission("BLAHBLAH", false);
        assertTrue(p1.implies(p2));
        assertTrue(p2.implies(p1));

        // Case sensitive, different case
        p1 = new Permission("BLAHBLAH", false);
        p2 = new Permission("bLAHBLAH", false);
        assertTrue(p1.implies(p2));
        assertTrue(p2.implies(p1));

        // Case sensitive, different word
        p1 = new Permission("BLAHBLAH", false);
        p2 = new Permission("whatwhat", false);
        assertFalse(p1.implies(p2));
        assertFalse(p2.implies(p1));

    }

    @Test
    public void testLists() {
        Permission p1, p2, p3;

        p1 = new Permission("one,two");
        p2 = new Permission("one");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        p1 = new Permission("one,two,three");
        p2 = new Permission("one,three");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        p1 = new Permission("one,two:one,two,three");
        p2 = new Permission("one:three");
        p3 = new Permission("one:two,three");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));
        assertTrue(p1.implies(p3));
        assertFalse(p2.implies(p3));
        assertTrue(p3.implies(p2));

        p1 = new Permission("one,two,three:one,two,three:one,two");
        p2 = new Permission("one:three:two");
        assertTrue(p1.implies(p2));
        assertFalse(p2.implies(p1));

        p1 = new Permission("one");
        p2 = new Permission("one:two,three,four");
        p3 = new Permission("one:two,three,four:five:six:seven");
        assertTrue(p1.implies(p2));
        assertTrue(p1.implies(p3));
        assertFalse(p2.implies(p1));
        assertFalse(p3.implies(p1));
        assertTrue(p2.implies(p3));

    }

    @Test
    public void testWildcards() {
        Permission p1, p2, p3, p4, p5, p6, p7, p8;

        p1 = new Permission("*");
        p2 = new Permission("one");
        p3 = new Permission("one:two");
        p4 = new Permission("one,two:three,four");
        p5 = new Permission("one,two:three,four,five:six:seven,eight");
        assertTrue(p1.implies(p2));
        assertTrue(p1.implies(p3));
        assertTrue(p1.implies(p4));
        assertTrue(p1.implies(p5));

        p1 = new Permission("newsletter:*");
        p2 = new Permission("newsletter:read");
        p3 = new Permission("newsletter:read,write");
        p4 = new Permission("newsletter:*");
        p5 = new Permission("newsletter:*:*");
        p6 = new Permission("newsletter:*:read");
        p7 = new Permission("newsletter:write:*");
        p8 = new Permission("newsletter:read,write:*");
        assertTrue(p1.implies(p2));
        assertTrue(p1.implies(p3));
        assertTrue(p1.implies(p4));
        assertTrue(p1.implies(p5));
        assertTrue(p1.implies(p6));
        assertTrue(p1.implies(p7));
        assertTrue(p1.implies(p8));


        p1 = new Permission("newsletter:*:*");
        assertTrue(p1.implies(p2));
        assertTrue(p1.implies(p3));
        assertTrue(p1.implies(p4));
        assertTrue(p1.implies(p5));
        assertTrue(p1.implies(p6));
        assertTrue(p1.implies(p7));
        assertTrue(p1.implies(p8));

        p1 = new Permission("newsletter:*:*:*");
        assertTrue(p1.implies(p2));
        assertTrue(p1.implies(p3));
        assertTrue(p1.implies(p4));
        assertTrue(p1.implies(p5));
        assertTrue(p1.implies(p6));
        assertTrue(p1.implies(p7));
        assertTrue(p1.implies(p8));

        p1 = new Permission("newsletter:*:read");
        p2 = new Permission("newsletter:123:read");
        p3 = new Permission("newsletter:123,456:read,write");
        p4 = new Permission("newsletter:read");
        p5 = new Permission("newsletter:read,write");
        p6 = new Permission("newsletter:123:read:write");
        assertTrue(p1.implies(p2));
        assertFalse(p1.implies(p3));
        assertFalse(p1.implies(p4));
        assertFalse(p1.implies(p5));
        assertTrue(p1.implies(p6));

        p1 = new Permission("newsletter:*:read:*");
        assertTrue(p1.implies(p2));
        assertTrue(p1.implies(p6));

    }

}