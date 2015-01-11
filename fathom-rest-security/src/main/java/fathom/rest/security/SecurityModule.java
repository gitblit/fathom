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

package fathom.rest.security;

import fathom.Module;
import fathom.rest.security.aop.RequireAdministrator;
import fathom.rest.security.aop.RequireAdministratorInterceptor;
import fathom.rest.security.aop.RequireAuthenticated;
import fathom.rest.security.aop.RequireAuthenticatedInterceptor;
import fathom.rest.security.aop.RequireGuest;
import fathom.rest.security.aop.RequireGuestInterceptor;
import fathom.rest.security.aop.RequirePermission;
import fathom.rest.security.aop.RequirePermissionInterceptor;
import fathom.rest.security.aop.RequirePermissions;
import fathom.rest.security.aop.RequirePermissionsInterceptor;
import fathom.rest.security.aop.RequireRole;
import fathom.rest.security.aop.RequireRoleInterceptor;
import fathom.rest.security.aop.RequireRoles;
import fathom.rest.security.aop.RequireRolesInterceptor;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

/**
 * @author James Moger
 */
public class SecurityModule extends Module {

    @Override
    protected void setup() {

        bind(BasicAuthenticationHandler.class);
        bind(FormAuthenticationHandler.class);

        RequireGuestInterceptor guestInterceptor = new RequireGuestInterceptor();
        bindInterceptor(any(), annotatedWith(RequireGuest.class), guestInterceptor);

        RequireAuthenticatedInterceptor authenticatedInterceptor = new RequireAuthenticatedInterceptor();
        bindInterceptor(any(), annotatedWith(RequireAuthenticated.class), authenticatedInterceptor);

        RequireAdministratorInterceptor administratorInterceptor = new RequireAdministratorInterceptor();
        bindInterceptor(any(), annotatedWith(RequireAdministrator.class), administratorInterceptor);

        RequireRoleInterceptor roleInterceptor = new RequireRoleInterceptor();
        bindInterceptor(any(), annotatedWith(RequireRole.class), roleInterceptor);

        RequirePermissionInterceptor permissionInterceptor = new RequirePermissionInterceptor();
        bindInterceptor(any(), annotatedWith(RequirePermission.class), permissionInterceptor);

        RequireRolesInterceptor rolesInterceptor = new RequireRolesInterceptor();
        bindInterceptor(any(), annotatedWith(RequireRoles.class), rolesInterceptor);

        RequirePermissionsInterceptor permissionsInterceptor = new RequirePermissionsInterceptor();
        bindInterceptor(any(), annotatedWith(RequirePermissions.class), permissionsInterceptor);
    }

}
