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

/**
 * A Permission represents the ability to perform an action or access a resource.  A Permission is the most
 * granular, or atomic, unit in a system's security policy and is the cornerstone upon which fine-grained security
 * models are built.
 * <p/>
 * It is important to understand a Permission instance only represents functionality or access - it does not grant it.
 * Granting access to an application functionality or a particular resource is done by the application's security
 * configuration, typically by assigning Permissions to users, roles and/or groups.
 * <p/>
 * Most typical systems are what the Shiro team calls <em>role-based</em> in nature, where a role represents
 * common behavior for certain user types.  For example, a system might have an <em>Aministrator</em> role, a
 * <em>User</em> or <em>Guest</em> roles, etc.
 * <p/>
 * But if you have a dynamic security model, where roles can be created and deleted at runtime, you can't hard-code
 * role names in your code.  In this environment, roles themselves aren't aren't very useful.  What matters is what
 * <em>permissions</em> are assigned to these roles.
 * <p/>
 * Under this paradigm, permissions are immutable and reflect an application's raw functionality
 * (opening files, accessing a web URL, creating users, etc).  This is what allows a system's security policy
 * to be dynamic: because Permissions represent raw functionality and only change when the application's
 * source code changes, they are immutable at runtime - they represent 'what' the system can do.  Roles, users, and
 * groups are the 'who' of the application.  Determining 'who' can do 'what' then becomes a simple exercise of
 * associating Permissions to roles, users, and groups in some way.
 * <p/>
 * Most applications do this by associating a named role with permissions (i.e. a role 'has a' collection of
 * Permissions) and then associate users with roles (i.e. a user 'has a' collection of roles) so that by transitive
 * association, the user 'has' the permissions in their roles.  There are numerous variations on this theme
 * (permissions assigned directly to users, or assigned to groups, and users added to groups and these groups in turn
 * have roles, etc, etc).  When employing a permission-based security model instead of a role-based one, users, roles,
 * and groups can all be created, configured and/or deleted at runtime.  This enables  an extremely powerful security
 * model.
 * <p/>
 * A benefit to Shiro is that, although it assumes most systems are based on these types of static role or
 * dynamic role w/ permission schemes, it does not require a system to model their security data this way - all
 * Permission checks are relegated to {@link fathom.rest.realm.Realm} implementations, and only those
 * implementations really determine how a user 'has' a permission or not.  The Realm could use the semantics described
 * here, or it could utilize some other mechanism entirely - it is always up to the application developer.
 * <p/>
 * Shiro provides a very powerful default implementation of this interface in the form of the
 * {@link fathom.rest.authz.WildcardPermission WildcardPermission}.  We highly recommend that you
 * investigate this class before trying to implement your own <code>Permission</code>s.
 *
 * @see fathom.rest.authz.WildcardPermission WildcardPermission
 * @since 0.2
 */

import com.google.common.base.Joiner;
import fathom.utils.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A <code>WildcardPermission</code> is a very flexible permission construct supporting multiple levels of
 * permission matching. However, most people will probably follow some standard conventions as explained below.
 * <p>
 * <h3>Simple Usage</h3>
 * <p>
 * In the simplest form, <code>WildcardPermission</code> can be used as a simple permission string. You could grant a
 * user an &quot;editNewsletter&quot; permission and then check to see if the user has the editNewsletter
 * permission by calling
 * <p>
 * <code>subject.isPermitted(&quot;editNewsletter&quot;)</code>
 * <p>
 * This is (mostly) equivalent to
 * <p>
 * <code>subject.isPermitted( new WildcardPermission(&quot;editNewsletter&quot;) )</code>
 * <p>
 * but more on that later.
 * <p>
 * The simple permission string may work for simple applications, but it requires you to have permissions like
 * <code>&quot;viewNewsletter&quot;</code>, <code>&quot;deleteNewsletter&quot;</code>,
 * <code>&quot;createNewsletter&quot;</code>, etc. You can also grant a user <code>&quot;*&quot;</code> permissions
 * using the wildcard character (giving this class its name), which means they have <em>all</em> permissions. But
 * using this approach there's no way to just say a user has &quot;all newsletter permissions&quot;.
 * <p>
 * For this reason, <code>WildcardPermission</code> supports multiple <em>levels</em> of permissioning.
 * <p>
 * <h3>Multiple Levels</h3>
 * <p>
 * WildcardPermission</code> also supports the concept of multiple <em>levels</em>.  For example, you could
 * restructure the previous simple example by granting a user the permission <code>&quot;newsletter:edit&quot;</code>.
 * The colon in this example is a special character used by the <code>WildcardPermission</code> that delimits the
 * next token in the permission.
 * <p>
 * In this example, the first token is the <em>domain</em> that is being operated on
 * and the second token is the <em>action</em> being performed. Each level can contain multiple values.  So you
 * could simply grant a user the permission <code>&quot;newsletter:view,edit,create&quot;</code> which gives them
 * access to perform <code>view</code>, <code>edit</code>, and <code>create</code> actions in the <code>newsletter</code>
 * <em>domain</em>. Then you could check to see if the user has the <code>&quot;newsletter:create&quot;</code>
 * permission by calling
 * <p>
 * <code>subject.isPermitted(&quot;newsletter:create&quot;)</code>
 * <p>
 * (which would return true).
 * <p>
 * In addition to granting multiple permissions via a single string, you can grant all permission for a particular
 * level. So if you wanted to grant a user all actions in the <code>newsletter</code> domain, you could simply give
 * them <code>&quot;newsletter:*&quot;</code>. Now, any permission check for <code>&quot;newsletter:XXX&quot;</code>
 * will return <code>true</code>. It is also possible to use the wildcard token at the domain level (or both): so you
 * could grant a user the <code>&quot;view&quot;</code> action across all domains <code>&quot;*:view&quot;</code>.
 * <p>
 * <h3>Instance-level Access Control</h3>
 * <p>
 * Another common usage of the <code>WildcardPermission</code> is to model instance-level Access Control Lists.
 * In this scenario you use three tokens - the first is the <em>domain</em>, the second is the <em>action</em>, and
 * the third is the <em>instance</em> you are acting on.
 * <p>
 * So for example you could grant a user <code>&quot;newsletter:edit:12,13,18&quot;</code>.  In this example, assume
 * that the third token is the system's ID of the newsletter. That would allow the user to edit newsletters
 * <code>12</code>, <code>13</code>, and <code>18</code>. This is an extremely powerful way to express permissions,
 * since you can now say things like <code>&quot;newsletter:*:13&quot;</code> (grant a user all actions for newsletter
 * <code>13</code>), <code>&quot;newsletter:view,create,edit:*&quot;</code> (allow the user to
 * <code>view</code>, <code>create</code>, or <code>edit</code> <em>any</em> newsletter), or
 * <code>&quot;newsletter:*:*</code> (allow the user to perform <em>any</em> action on <em>any</em> newsletter).
 * <p>
 * To perform checks against these instance-level permissions, the application should include the instance ID in the
 * permission check like so:
 * <p>
 * <code>subject.isPermitted( &quot;newsletter:edit:13&quot; )</code>
 * <p>
 * There is no limit to the number of tokens that can be used, so it is up to your imagination in terms of ways that
 * this could be used in your application.  However, the Shiro team likes to standardize some common usages shown
 * above to help people get started and provide consistency in the Shiro community.
 * <p>
 * This class was adapted from Apache Shiro.
 * </p>
 */
public class Permission implements Serializable {

    protected static final String WILDCARD_TOKEN = "*";
    protected static final String PART_DIVIDER_TOKEN = ":";
    protected static final String SUBPART_DIVIDER_TOKEN = ",";
    protected static final boolean DEFAULT_CASE_SENSITIVE = false;

    private List<Set<String>> parts;

    /**
     * Default no-arg constructor for subclasses only - end-user developers instantiating Permission instances must
     * provide a wildcard string at a minimum, since Permission instances are immutable once instantiated.
     * <p>
     * Note that the WildcardPermission class is very robust and typically subclasses are not necessary unless you
     * wish to create type-safe Permission objects that would be used in your application, such as perhaps a
     * {@code UserPermission}, {@code SystemPermission}, {@code PrinterPermission}, etc.  If you want such type-safe
     * permission usage, consider subclassing the {@link DomainPermission DomainPermission} class for your needs.
     */
    protected Permission() {
    }

    public Permission(String wildcardString) {
        this(wildcardString, DEFAULT_CASE_SENSITIVE);
    }

    public Permission(String wildcardString, boolean caseSensitive) {
        setParts(wildcardString, caseSensitive);
    }

    protected void setParts(String wildcardString, boolean caseSensitive) {
        if (wildcardString == null || wildcardString.trim().length() == 0) {
            throw new IllegalArgumentException("Wildcard string cannot be null or empty. Make sure permission strings are properly formatted.");
        }

        wildcardString = wildcardString.trim();

        List<String> parts = Arrays.asList(wildcardString.split(PART_DIVIDER_TOKEN));

        this.parts = new ArrayList<>();
        for (String part : parts) {
            Set<String> subparts = Util.splitToSet(part, SUBPART_DIVIDER_TOKEN);
            if (!caseSensitive) {
                subparts = lowercase(subparts);
            }
            if (subparts.isEmpty()) {
                throw new IllegalArgumentException("Wildcard string cannot contain parts with only dividers. Make sure permission strings are properly formatted.");
            }
            this.parts.add(subparts);
        }

        if (this.parts.isEmpty()) {
            throw new IllegalArgumentException("Wildcard string cannot contain only dividers. Make sure permission strings are properly formatted.");
        }
    }

    private Set<String> lowercase(Set<String> subparts) {
        Set<String> lowerCasedSubparts = new LinkedHashSet<String>(subparts.size());
        for (String subpart : subparts) {
            lowerCasedSubparts.add(subpart.toLowerCase());
        }
        return lowerCasedSubparts;
    }

    protected List<Set<String>> getParts() {
        return this.parts;
    }

    protected void setParts(String wildcardString) {
        setParts(wildcardString, DEFAULT_CASE_SENSITIVE);
    }

    /**
     * Returns {@code true} if this current instance <em>implies</em> all the functionality and/or resource access
     * described by the specified {@code Permission} argument, {@code false} otherwise.
     * <p>
     * <p>That is, this current instance must be exactly equal to or a <em>superset</em> of the functionalty
     * and/or resource access described by the given {@code Permission} argument.  Yet another way of saying this
     * would be:
     * <p>
     * <p>If &quot;permission1 implies permission2&quot;, i.e. <code>permission1.implies(permission2)</code> ,
     * then any Subject granted {@code permission1} would have ability greater than or equal to that defined by
     * {@code permission2}.
     *
     * @param p the permission to check for behavior/functionality comparison.
     * @return {@code true} if this current instance <em>implies</em> all the functionality and/or resource access
     * described by the specified {@code Permission} argument, {@code false} otherwise.
     */
    public boolean implies(Permission p) {
        // By default only supports comparisons with other WildcardPermissions
        List<Set<String>> otherParts = p.getParts();

        int i = 0;
        for (Set<String> otherPart : otherParts) {
            // If this permission has less parts than the other permission, everything after the number of parts contained
            // in this permission is automatically implied, so return true
            if (getParts().size() - 1 < i) {
                return true;
            } else {
                Set<String> part = getParts().get(i);
                if (!part.contains(WILDCARD_TOKEN) && !part.containsAll(otherPart)) {
                    return false;
                }
                i++;
            }
        }

        // If this permission has more parts than the other parts, only imply it if all of the other parts are wildcards
        for (; i < getParts().size(); i++) {
            Set<String> part = getParts().get(i);
            if (!part.contains(WILDCARD_TOKEN)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return Joiner.on(':').join(parts);
    }

    public boolean equals(Object o) {
        if (o instanceof Permission) {
            Permission p = (Permission) o;
            return parts.equals(p.parts);
        }
        return false;
    }

    public int hashCode() {
        return parts.hashCode();
    }

}
