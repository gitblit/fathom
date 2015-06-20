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
package fathom.rest.security.aop;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author James Moger
 */
public class SecurityUtil {

    public static List<String> collectPermissions(Method method) {
        List<String> permissions = new ArrayList<>();
        permissions.addAll(collectPermissions(method.getAnnotation(RequirePermissions.class)));
        if (method.isAnnotationPresent(RequirePermission.class)) {
            permissions.add(method.getAnnotation(RequirePermission.class).value());
        }

        permissions.addAll(collectPermissions(method.getDeclaringClass().getAnnotation(RequirePermissions.class)));
        if (method.getDeclaringClass().isAnnotationPresent(RequirePermission.class)) {
            permissions.add(method.getDeclaringClass().getAnnotation(RequirePermission.class).value());
        }

        return permissions;
    }

    private static Collection<String> collectPermissions(RequirePermissions requirePermissions) {
        if (requirePermissions == null) {
            return Collections.emptyList();
        }

        List<String> permissions = new ArrayList<>();
        for (RequirePermission permission : requirePermissions.value()) {
            permissions.add(permission.value());
        }
        return permissions;
    }

    public static List<String> collectRoles(Method method) {
        List<String> roles = new ArrayList<>();
        roles.addAll(collectRoles(method.getAnnotation(RequireRoles.class)));
        if (method.isAnnotationPresent(RequireRole.class)) {
            roles.add(method.getAnnotation(RequireRole.class).value());
        }

        roles.addAll(collectRoles(method.getDeclaringClass().getAnnotation(RequireRoles.class)));
        if (method.getDeclaringClass().isAnnotationPresent(RequireRole.class)) {
            roles.add(method.getDeclaringClass().getAnnotation(RequireRole.class).value());
        }
        return roles;
    }

    private static Collection<String> collectRoles(RequireRoles requireRoles) {
        if (requireRoles == null) {
            return Collections.emptyList();
        }

        List<String> roles = new ArrayList<>();
        for (RequireRole role : requireRoles.value()) {
            roles.add(role.value());
        }
        return roles;
    }
}
