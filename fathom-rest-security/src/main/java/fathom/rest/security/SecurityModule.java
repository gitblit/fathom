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

import com.google.inject.matcher.Matcher;
import fathom.Module;
import fathom.rest.controller.Controller;
import fathom.rest.security.aop.ControllerInterceptor;
import fathom.rest.security.aop.RequireAdministrator;
import fathom.rest.security.aop.RequireAdministratorInterceptor;
import fathom.rest.security.aop.RequireAuthenticated;
import fathom.rest.security.aop.RequireAuthenticatedInterceptor;
import fathom.rest.security.aop.RequirePermission;
import fathom.rest.security.aop.RequirePermissionInterceptor;
import fathom.rest.security.aop.RequirePermissions;
import fathom.rest.security.aop.RequirePermissionsInterceptor;
import fathom.rest.security.aop.RequireRole;
import fathom.rest.security.aop.RequireRoleInterceptor;
import fathom.rest.security.aop.RequireRoles;
import fathom.rest.security.aop.RequireRolesInterceptor;
import fathom.security.SecurityManager;

import static com.google.inject.matcher.Matchers.*;

/**
 * @author James Moger
 */
public class SecurityModule extends Module {

    @Override
    protected void setup() {

        bind(BasicAuthenticationHandler.class);
        bind(FormAuthenticationHandler.class);

        Matcher<Class> controllers = subclassesOf(Controller.class);
        Matcher<Class> notControllers = not(controllers);

        /*
         * The grand ControllerInterceptor.
         */
        ControllerInterceptor controllerInterceptor = new ControllerInterceptor(getProvider(SecurityManager.class));
        bindInterceptor(controllers, any(), controllerInterceptor);

        /*
         * Individual method interceptors for annotating non-controllers.
         */
        RequireAuthenticatedInterceptor authenticatedInterceptor = new RequireAuthenticatedInterceptor();
        bindInterceptor(notControllers, annotatedWith(RequireAuthenticated.class), authenticatedInterceptor);

        RequireAdministratorInterceptor administratorInterceptor = new RequireAdministratorInterceptor();
        bindInterceptor(notControllers, annotatedWith(RequireAdministrator.class), administratorInterceptor);

        RequireRoleInterceptor roleInterceptor = new RequireRoleInterceptor();
        bindInterceptor(notControllers, annotatedWith(RequireRole.class), roleInterceptor);

        RequirePermissionInterceptor permissionInterceptor = new RequirePermissionInterceptor();
        bindInterceptor(notControllers, annotatedWith(RequirePermission.class), permissionInterceptor);

        RequireRolesInterceptor rolesInterceptor = new RequireRolesInterceptor();
        bindInterceptor(notControllers, annotatedWith(RequireRoles.class), rolesInterceptor);

        RequirePermissionsInterceptor permissionsInterceptor = new RequirePermissionsInterceptor();
        bindInterceptor(notControllers, annotatedWith(RequirePermissions.class), permissionsInterceptor);
    }

}
