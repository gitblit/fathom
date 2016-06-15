/*
 * Copyright (C) 2016 the original author or authors.
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

package fathom.keycloak;

import fathom.rest.Context;
import fathom.rest.security.LogoutHandler;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore;
import org.keycloak.adapters.spi.KeycloakAccount;

import static org.keycloak.adapters.servlet.FilterSessionStore.*;

/**
 * @author James Moger
 */
public class KeycloakLogoutHandler extends LogoutHandler {

    @Override
    public void handle(Context context) {
        OIDCFilterSessionStore.SerializableKeycloakAccount account = context.removeSession(KeycloakAccount.class.getName());
        if (account != null) {
            // Logout of the Keycloak server
            KeycloakDeployment deployment = account.getKeycloakSecurityContext().getDeployment();
            account.getKeycloakSecurityContext().logout(deployment);
        }

        // Cleanup the session of Keycloak metadata
        context.removeSession(KeycloakSecurityContext.class.getName());
        context.removeSession(REDIRECT_URI);
        context.removeSession(SAVED_METHOD);
        context.removeSession(SAVED_HEADERS);
        context.removeSession(SAVED_BODY);

        super.handle(context);
    }
}
