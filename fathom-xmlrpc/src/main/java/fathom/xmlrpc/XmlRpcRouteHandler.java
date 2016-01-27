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

package fathom.xmlrpc;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import fathom.authc.StandardCredentials;
import fathom.authc.TokenCredentials;
import fathom.exception.StatusCodeException;
import fathom.realm.Account;
import fathom.rest.Context;
import fathom.rest.security.AuthConstants;
import fathom.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author James Moger.
 */
public abstract class XmlRpcRouteHandler implements RouteHandler<Context> {

    private final static String TEXT_XML = "text/xml";

    private final Logger log = LoggerFactory.getLogger(XmlRpcRouteHandler.class);

    private final XmlRpcMethodRegistrar xmlRpcMethodRegistrar;

    @Inject
    private SecurityManager securityManager;

    @Inject
    public XmlRpcRouteHandler(XmlRpcMethodRegistrar xmlRpcMethodRegistrar) {
        this.xmlRpcMethodRegistrar = xmlRpcMethodRegistrar;
    }

    @Override
    public void handle(Context context) {
        if ("POST".equals(context.getRequestMethod())) {
            if (!context.getContentTypes().contains(TEXT_XML)) {
                log.warn("{} request from {} did not specify {}, ignoring",
                        context.getRequestUri(), context.getRequest().getClientIp(), TEXT_XML);
                context.next();
                return;
            }

            try {
                authenticate(context);

                byte[] result;
                try (InputStream is = context.getRequest().getHttpServletRequest().getInputStream()) {
                    XmlRpcRequest request = new XmlRpcRequest();
                    request.parse(is);

                    XmlRpcResponse response = new XmlRpcResponse(xmlRpcMethodRegistrar);
                    result = response.process(request);
                }

                context.getResponse().ok().contentType(TEXT_XML).contentLength(result.length);
                try (OutputStream output = context.getResponse().getOutputStream()) {
                    output.write(result);
                    output.flush();
                }

            } catch (Exception e) {
                log.error("Failed to handle XML-RPC request", e);
                context.getResponse().internalError().commit();
            }
        } else {
            throw new StatusCodeException(405, "Only POST is supported!");
        }
    }

    protected void authenticate(Context context) {
        Account session = context.getSession(AuthConstants.ACCOUNT_ATTRIBUTE);
        Account local = context.getLocal(AuthConstants.ACCOUNT_ATTRIBUTE);
        Account account = Optional.fromNullable(session).or(Optional.fromNullable(local).or(Account.GUEST));

        if (account.isGuest()) {
            String authorization = context.getRequest().getHeader("Authorization");
            if (!Strings.isNullOrEmpty(authorization)) {
                if (authorization.toLowerCase().startsWith("token")) {
                    String packet = authorization.substring("token".length()).trim();
                    TokenCredentials credentials = new TokenCredentials(packet);
                    account = securityManager.authenticate(credentials);
                } else if (authorization.toLowerCase().startsWith("basic")) {
                    String packet = authorization.substring("basic".length()).trim();
                    String credentials1 = new String(Base64.getDecoder().decode(packet), StandardCharsets.UTF_8);
                    String[] values1 = credentials1.split(":", 2);
                    String username = values1[0];
                    String password = values1[1];
                    StandardCredentials authenticationToken = new StandardCredentials(username, password);
                    account = securityManager.authenticate(authenticationToken);
                }
            }
        }

        account = Optional.fromNullable(account).or(Account.GUEST);
        context.setLocal(AuthConstants.ACCOUNT_ATTRIBUTE, account);
    }
}
