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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <code>Authorizations</code> represents an Account's Roles & Permissions.
 * <p>
 * This class was adapted from Apache Shiro.
 * </p>
 *
 * @see fathom.authz.Role
 * @see fathom.authz.Permission
 * @see fathom.realm.Account
 */
public final class Authorizations implements Serializable, Cloneable {

    private static final long serialVersionUID = 8777587779337470374L;

    protected final Set<Role> roles;

    protected final Set<Permission> permissions;

    protected transient Collection<Permission> aggregatePermissions;

    public Authorizations() {
        this(new LinkedHashSet<>());
    }

    /**
     * Creates a new instance with the specified roles and no permissions.
     *
     * @param roles the roles assigned to the Account.
     */
    public Authorizations(Set<String> roles) {
        this(new LinkedHashSet<>(), new LinkedHashSet<>());

        for (String role : roles) {
            addRole(role);
        }
    }

    /**
     * Creates a new instance with the specified Roles and Permissions.
     *
     * @param roles       the roles assigned to the Account.
     * @param permissions the permissions assigned to the Account.
     */
    public Authorizations(Set<Role> roles, Set<Permission> permissions) {
        this.roles = roles;
        this.permissions = permissions;
        this.aggregatePermissions = null;
    }

    /**
     * Returns the Roles assigned to the corresponding Account.
     *
     * @return the Roles assigned to the corresponding Account.
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     * Sets the roles assigned to the Account.
     *
     * @param roles the roles assigned to the Account.
     */
    public Authorizations setRoles(Set<Role> roles) {
        this.roles.clear();
        this.aggregatePermissions = null;
        addRoles(roles);

        return this;
    }

    /**
     * Adds roles to the Account Authorizations.
     *
     * @param roles the roles to add.
     */
    public Authorizations addRoles(String... roles) {
        for (String role : roles) {
            addRole(new Role(role));
        }

        return this;
    }

    /**
     * Adds (assigns) multiple roles to those associated with the Account.
     *
     * @param roles the roles to add to those associated with the Account.
     */
    public Authorizations addRoles(Collection<Role> roles) {
        this.roles.addAll(roles);
        this.aggregatePermissions = null;

        return this;
    }

    /**
     * Adds (assigns) multiple roles to those associated with the Account.
     *
     * @param role the role to add to those associated with the Account.
     */
    public Authorizations addRole(String role) {
        this.addRole(new Role(role));

        return this;
    }


    /**
     * Adds (assigns) multiple roles to those associated with the Account.
     *
     * @param role the role to add to those associated with the Account.
     */
    public Authorizations addRole(Role role) {
        this.roles.add(role);
        this.aggregatePermissions = null;

        return this;
    }

    /**
     * Adds (assigns) a permission to those directly associated with the Account.  If the Account doesn't yet have any
     * direct permissions, a new permission collection (a Set&lt;String&gt;) will be created automatically.
     *
     * @param permissions the permissions to add to those directly assigned to the Account.
     */
    public Authorizations addPermissions(String... permissions) {
        for (String permission : permissions) {
            addPermission(permission);
        }

        return this;
    }

    /**
     * Adds (assigns) a permission to those directly associated with the Account.  If the Account doesn't yet have any
     * direct permissions, a new permission collection (a Set&lt;String&gt;) will be created automatically.
     *
     * @param permission the permission to add to those directly assigned to the Account.
     */
    public Authorizations addPermission(String permission) {
        this.permissions.add(new Permission(permission));
        this.aggregatePermissions = null;

        return this;
    }

    /**
     * Adds (assigns) a permission to those directly associated with the Account.  If the Account doesn't yet have any
     * direct permissions, a new permission collection (a Set&lt;String&gt;) will be created automatically.
     *
     * @param permissions the permissions to add to those directly assigned to the Account.
     */
    public Authorizations addPermissions(Collection<Permission> permissions) {
        this.permissions.addAll(permissions);
        this.aggregatePermissions = null;

        return this;
    }

    /**
     * Returns all type-safe {@link Permission Permission}s assigned to the corresponding Account.  The permissions
     * returned from this method plus any returned from {@link #getPermissions() getPermissions()}
     * represent the total set of permissions.  The aggregate set is used to perform a permission authorization check.
     *
     * @return all type-safe {@link Permission Permission}s assigned to the corresponding Account.
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }

    /**
     * Sets the string-based permissions assigned directly to the Account.  The permissions set here, in addition to any
     * {@link #getPermissions() object permissions} constitute the total permissions assigned directly to the
     * Account.
     *
     * @param permissions the string-based permissions assigned directly to the Account.
     */
    public void setPermissions(Collection<Permission> permissions) {
        this.permissions.clear();
        this.aggregatePermissions = null;
        this.addPermissions(permissions);
    }

    /**
     * Returns {@code true} if this Account is permitted to perform an action or access a resource summarized by the
     * specified permission string.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link Permission Permission} variant.
     * Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permission the String representation of a Permission that is being checked.
     * @return true if this Account is permitted, false otherwise.
     */
    public boolean isPermitted(String permission) {
        return isPermitted(new Permission(permission));
    }

    /**
     * Checks if this Account implies the given permission strings and returns a boolean array indicating which
     * permissions are implied.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link Permission Permission} variant.
     * Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permissions the String representations of the Permissions that are being checked.
     * @return a boolean array where indices correspond to the index of the
     * permissions in the given list.  A true value at an index indicates this Account is permitted for
     * for the associated {@code Permission} string in the list.  A false value at an index
     * indicates otherwise.
     */
    public boolean[] isPermitted(String... permissions) {
        boolean[] rights = new boolean[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            Permission permission = new Permission(permissions[i]);
            rights[i] = isPermitted(permission);
        }
        return rights;
    }

    /**
     * Returns {@code true} if this Account implies all of the specified permission strings, {@code false} otherwise.
     * <p>
     * This is an overloaded method for the corresponding type-safe {@link fathom.authz.Permission Permission}
     * variant.  Please see the class-level JavaDoc for more information on these String-based permission methods.
     *
     * @param permissions the String representations of the Permissions that are being checked.
     * @return true if this Account has all of the specified permissions, false otherwise.
     */
    public boolean isPermittedAll(String... permissions) {
        for (boolean permitted : isPermitted(permissions)) {
            if (!permitted) {
                return false;
            }
        }
        return true;
    }

    protected boolean isPermitted(Permission permission) {
        for (Permission perm : getAggregatePermissions()) {
            if (perm.implies(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this Account has the specified role, {@code false} otherwise.
     *
     * @param roleIdentifier the application-specific role identifier (usually a role id or role name).
     * @return {@code true} if this Account has the specified role, {@code false} otherwise.
     */
    public boolean hasRole(String roleIdentifier) {
        return roles.contains(new Role(roleIdentifier));
    }

    /**
     * Returns {@code true} if this Account has the specified roles, {@code false} otherwise.
     *
     * @param roleIdentifiers the application-specific role identifiers to check (usually role ids or role names).
     * @return {@code true} if this Account has all the specified roles, {@code false} otherwise.
     */
    public boolean hasRoles(String... roleIdentifiers) {
        List<Role> requiredRoles = new ArrayList<>();
        for (String roleIdentifier : roleIdentifiers) {
            requiredRoles.add(new Role(roleIdentifier));
        }
        return roles.containsAll(requiredRoles);
    }

    /**
     * Gets the collection of permissions including the role permissions and discrete permissions.
     *
     * @return a collection of aggregate permissions
     */
    public Collection<Permission> getAggregatePermissions() {
        if (aggregatePermissions == null) {
            Set<Permission> perms = new LinkedHashSet<>();
            perms.addAll(permissions);
            for (Role role : roles) {
                perms.addAll(role.getPermissions());
            }

            if (perms.isEmpty()) {
                aggregatePermissions = Collections.emptySet();
            } else {
                aggregatePermissions = Collections.unmodifiableSet(perms);
            }
        }

        return aggregatePermissions;
    }

    @Override
    public Authorizations clone() {
        Authorizations clone = new Authorizations(roles, permissions);
        return clone;
    }

}
