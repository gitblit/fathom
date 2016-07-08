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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import fathom.realm.Account;
import fathom.realm.keycloak.KeycloakRealm;
import fathom.realm.keycloak.KeycloakToken;
import fathom.rest.Context;
import fathom.rest.security.AuthConstants;
import fathom.security.SecurityManager;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.servlet.FilterRequestAuthenticator;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore;
import org.keycloak.adapters.servlet.OIDCServletHttpFacade;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A guard that delegates authentication to a Keycloak server.
 *
 * @author James Moger
 */
public final class KeycloakGuard implements RouteHandler<Context> {

    private final Logger log = LoggerFactory.getLogger(KeycloakGuard.class);

    private final SecurityManager securityManager;
    private final KeycloakRealm keycloakRealm;
    private final AdapterDeploymentContext deploymentContext;

    @Inject
    public KeycloakGuard(SecurityManager securityManager) {
        this("", "", securityManager);
    }

    public KeycloakGuard(String realm, String resource, SecurityManager securityManager) {
        this.securityManager = securityManager;

        KeycloakRealm kRealm = null;
        for (KeycloakRealm keycloakRealm : securityManager.getRealms(KeycloakRealm.class)) {
            if (Strings.isNullOrEmpty(realm) && Strings.isNullOrEmpty(resource)) {
                // take first KeycloakRealm hit
                kRealm = keycloakRealm;
                break;
            } else if (realm.equals(keycloakRealm.getKeycloakConfig().getRealm())
                    && resource.equals(keycloakRealm.getKeycloakConfig().getResource())) {
                // match the realm & resource to the KeycloakRealm
                kRealm = keycloakRealm;
                break;
            }
        }

        this.keycloakRealm = kRealm;

        Preconditions.checkArgument(keycloakRealm != null, "Please specify a KeycloakRealm in realms.conf!");
        Preconditions.checkArgument(keycloakRealm.getKeycloakDeployment().isConfigured(), "Keycloak is not properly configured!");

        this.deploymentContext = new AdapterDeploymentContext(keycloakRealm.getKeycloakDeployment());
    }

    @Override
    public void handle(Context context) {
        HttpServletRequest request = context.getRequest().getHttpServletRequest();
        HttpServletResponse response = context.getResponse().getHttpServletResponse();
        OIDCServletHttpFacade facade = new OIDCServletHttpFacade(request, response);

        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            context.getResponse().forbidden();
            log.warn("Keycloak is not properly configured");
            return;
        }

        PreAuthActionsHandler preActions = new PreAuthActionsHandler(keycloakRealm, deploymentContext, facade);
        if (preActions.handleRequest()) {
            return;
        }

        keycloakRealm.registerKeycloakDeployment(deployment);

        OIDCFilterSessionStore tokenStore = new OIDCFilterSessionStore(request, facade, 100000, deployment, keycloakRealm.getSessionIdMapper());
        tokenStore.checkCurrentToken();

        FilterRequestAuthenticator authenticator = new FilterRequestAuthenticator(deployment, tokenStore, facade, request, 8443);

        final AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            log.trace("Keycloak authenticated request");
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) context.getRequest()
                    .getHttpServletRequest().getAttribute(KeycloakSecurityContext.class.getName());

            // configure Context and conditionally the Session with Fathom Security Account
            if (keycloakRealm.getKeycloakConfig().isAlwaysRefreshToken()
                    || context.getSession(AuthConstants.ACCOUNT_ATTRIBUTE) == null) {
                Account account = securityManager.authenticate(new KeycloakToken(securityContext.getToken()));
                context.setLocal(AuthConstants.ACCOUNT_ATTRIBUTE, account);
                if (context.hasSession()) {
                    context.setSession(AuthConstants.ACCOUNT_ATTRIBUTE, account);
                }
                log.trace("{} logged in via Keycloak", account.getUsername());
            }

            if (facade.isEnded()) {
                // Set Context status to match the underlying servlet response status.
                // This is necessary for the Pippo request dispatcher.
                context.status(response.getStatus());
                return;
            }

            AuthenticatedActionsHandler actions = new AuthenticatedActionsHandler(deployment, facade);
            if (actions.handledRequest()) {
                return;
            }

//            HttpServletRequestWrapper requestWrapper = tokenStore.buildWrapper();
//            ClassUtil.setField(context.getRequest(), "httpServletRequest", requestWrapper);
            context.next();
            return;
        }

        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            log.trace("Redirecting to Keycloak");
            challenge.challenge(facade);
            // Set Context status to match the underlying servlet response status.
            // This is necessary for the Pippo request dispatcher.
            context.status(response.getStatus());
            return;
        }

        // Request is forbidden
        context.getResponse().forbidden();
    }

}
