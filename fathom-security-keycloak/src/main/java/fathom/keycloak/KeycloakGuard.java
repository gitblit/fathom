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

package fathom.keycloak;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.realm.Account;
import fathom.realm.keycloak.KeycloakConfig;
import fathom.realm.keycloak.KeycloakRealm;
import fathom.realm.keycloak.KeycloakToken;
import fathom.rest.Context;
import fathom.rest.security.AuthConstants;
import fathom.security.SecurityManager;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.servlet.FilterRequestAuthenticator;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore;
import org.keycloak.adapters.servlet.OIDCServletHttpFacade;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.UserSessionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * A guard that delegates authentication to a Keycloak server.
 *
 * @author James Moger
 */
public final class KeycloakGuard implements RouteHandler<Context> {

    private final Logger log = LoggerFactory.getLogger(KeycloakGuard.class);

    private final SecurityManager securityManager;
    private final AdapterDeploymentContext deploymentContext;
    private final SessionIdMapper idMapper = new InMemorySessionIdMapper();
    private final NodesRegistrationManagement nodesRegistrationManagement;

    @Inject
    public KeycloakGuard(SecurityManager securityManager) {
        this("", "", securityManager);
    }

    public KeycloakGuard(String realm, String resource, SecurityManager securityManager) {
        this.securityManager = securityManager;

        KeycloakConfig keycloakConfig = new KeycloakConfig();
        for (KeycloakRealm keycloakRealm : securityManager.getRealms(KeycloakRealm.class)) {
            if (Strings.isNullOrEmpty(realm) && Strings.isNullOrEmpty(resource)) {
                // take first KeycloakRealm hit
                keycloakConfig = keycloakRealm.getKeycloakConfig();
                break;
            } else if (realm.equals(keycloakConfig.getRealm()) && resource.equals(keycloakConfig.getResource())) {
                // match the realm & resource to the KeycloakRealm
                keycloakConfig = keycloakRealm.getKeycloakConfig();
                break;
            }
        }

        KeycloakDeployment keycloakDeployment;
        if (Strings.isNullOrEmpty(keycloakConfig.getRealm()) || Strings.isNullOrEmpty(keycloakConfig.getRealmKey())) {
            keycloakDeployment = new KeycloakDeployment();
        } else {
            keycloakDeployment = KeycloakDeploymentBuilder.build(keycloakConfig);
        }

        this.deploymentContext = new AdapterDeploymentContext(keycloakDeployment);
        this.nodesRegistrationManagement = new NodesRegistrationManagement();
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

        PreAuthActionsHandler preActions = new PreAuthActionsHandler(new UserSessionManagement() {
            @Override
            public void logoutAll() {
                if (idMapper != null) {
                    idMapper.clear();
                }
            }

            @Override
            public void logoutHttpSessions(List<String> ids) {
                log.trace("**************** logoutHttpSessions");
                for (String id : ids) {
                    log.trace("removed idMapper: " + id);
                    idMapper.removeSession(id);
                }

            }
        }, deploymentContext, facade);

        if (preActions.handleRequest()) {
            return;
        }

        nodesRegistrationManagement.tryRegister(deployment);
        OIDCFilterSessionStore tokenStore = new OIDCFilterSessionStore(request, facade, 100000, deployment, idMapper);
        tokenStore.checkCurrentToken();

        FilterRequestAuthenticator authenticator = new FilterRequestAuthenticator(deployment, tokenStore, facade, request, 8443);
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            log.trace("Keycloak authenticated request");
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) context.getRequest()
                    .getHttpServletRequest().getAttribute(KeycloakSecurityContext.class.getName());

            // configure Session with Fathom Security Account
            if (request.getSession().getAttribute(AuthConstants.ACCOUNT_ATTRIBUTE) == null) {
                Account account = securityManager.authenticate(new KeycloakToken(securityContext.getToken()));
                request.getSession().setAttribute(AuthConstants.ACCOUNT_ATTRIBUTE, account);
                log.info("{} logged in via Keycloak", account.getUsername());
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
