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

import com.google.common.base.Joiner;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A simple representation of a security role that has a name and a collection of permissions.  This object can be
 * used internally by Realms to maintain authorization state.
 * <p>
 * This class was adapted from Apache Shiro.
 * </p>
 */
public class Role implements Serializable {

    protected final String name;
    protected final Set<Permission> permissions;

    public Role(String name) {
        this(name, new String[0]);
    }

    public Role(String name, String... permissions) {
        this.name = name.trim();
        this.permissions = new LinkedHashSet<>();
        addPermissions(permissions);
    }

    public Role(String name, Set<Permission> permissions) {
        this.name = name.trim();
        this.permissions = new LinkedHashSet<>();
        addPermissions(permissions);
    }

    public String getName() {
        return name;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions.clear();
        this.permissions.addAll(permissions);
    }

    public Role addPermission(String permission) {
        permissions.add(new Permission(permission));

        return this;
    }

    public Role addPermission(Permission permission) {
        permissions.add(permission);

        return this;
    }

    public Role addPermissions(String... perms) {
        for (String permission : perms) {
            permissions.add(new Permission(permission));
        }

        return this;
    }

    public Role addPermissions(Collection<Permission> perms) {
        if (perms != null && !perms.isEmpty()) {
            permissions.addAll(perms);
        }

        return this;
    }

    public boolean isPermitted(String permission) {
        return isPermitted(new Permission(permission));
    }

    public boolean isPermitted(Permission permission) {
        for (Permission perm : permissions) {
            if (perm.implies(permission)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return (getName() != null ? getName().hashCode() : 0);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Role) {
            Role sr = (Role) o;
            //only check name, since role names should be unique across an entire application:
            return (getName() != null ? getName().equals(sr.getName()) : sr.getName() == null);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Role{name='" + getName()
                + "', permissions=" + Joiner.on(',').join(permissions)
                + "}";
    }
}
