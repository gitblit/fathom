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

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import fathom.exception.StatusCodeException;
import fathom.rest.Context;
import fathom.rest.controller.HttpMethod;
import fathom.utils.CryptoUtil;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteHandler;

import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * Base class for generating and validating a CSRF token.
 * <p>
 * An attacker can coerce a victims browser to make the following types of requests:
 * <p>
 * GET requests
 * POST requests with a "Content-Type" of "application/x-www-form-urlencoded", "multipart/form-data", and "text/plain".
 * <p>
 * An attacker can not:
 * <p>
 * Coerce the browser to use other request methods such as PUT and DELETE.
 * Coerce the browser to post other content types, such as "application/json".
 * Coerce the browser to send new cookies, other than those that the server has already set.
 * Coerce the browser to set arbitrary headers, other than the normal headers the browser adds to requests.
 * <p>
 * Since GET requests are not meant to be mutative, there is no danger to an application that follows this
 * best practice.
 * <p>
 * Rules:
 * <p>
 * Permit POST if the "Content-Type" is not a guarded type (see above).
 * Permit POST if the "Csrf-Token" header is "nocheck".
 * Permit POST if the "_csrf_token" query parameter or form field matches the session csrf token.
 *
 * @author James Moger
 */
@Singleton
public class CSRFHandler implements RouteHandler<Context> {

    public static final String HEADER = "Csrf-Token";

    public static final String PARAMETER = "_csrf_token";

    public static final String BINDING = "csrfToken";

    private static final Logger log = LoggerFactory.getLogger(CSRFHandler.class);

    private final Set<String> guardedTypes = Sets.newHashSet("application/x-www-form-urlencoded", "multipart/form-data", "text/plain");

    private final String secretKey;

    private final String algorithm;

    @Inject
    public CSRFHandler() {
        this(CryptoUtil.generateSecretKey());
    }

    public CSRFHandler(String secretKey) {
        this(secretKey, CryptoUtil.HMAC_SHA256);
    }

    public CSRFHandler(String secretKey, String algorithm) {
        this.secretKey = secretKey;
        this.algorithm = algorithm;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    protected String getSessionCsrfToken(Context context) {
        return context.getSession(PARAMETER);
    }

    protected void setSessionCsrfToken(Context context, String token) {
        context.setSession(PARAMETER, token);
    }

    protected String getTokenId(Context context) {
        return context.getSession().getId().toString();
    }

    @Override
    public void handle(Context context) {

        String httpSerlvetRequestMethod = context.getRequest().getHttpServletRequest().getMethod();
        if (HttpMethod.POST.equals(httpSerlvetRequestMethod)) {

            // Verify the content-type is guarded
            String contentType = Util.getPreSubstring(context.getRequest().getHeader("Content-Type").toLowerCase(), ';');
            if (!guardedTypes.contains(contentType)) {
                log.debug("Ignoring '{}' request for {} '{}'", contentType, context.getRequestMethod(),
                        context.getRequestUri());
                return;
            }

            // Permit "nocheck" Csrf-Token headers
            String requestToken = context.getRequest().getHeader(HEADER);
            if ("nocheck".equals(requestToken)) {
                log.debug("Ignoring 'nocheck' request for {} '{}'", context.getRequestMethod(), context.getRequestUri());
                return;
            }

            if (Strings.isNullOrEmpty(requestToken)) {
                requestToken = context.getParameter(PARAMETER).toString();
            }

            if (Strings.isNullOrEmpty(requestToken)) {
                throw new StatusCodeException(HttpServletResponse.SC_FORBIDDEN, "Illegal request, no '{}'!", PARAMETER);
            }

            // Validate the request token against the session token
            String sessionToken = getSessionCsrfToken(context);
            if (!requestToken.equals(sessionToken)) {
                throw new StatusCodeException(HttpServletResponse.SC_FORBIDDEN, "Illegal request, invalid '{}'!", PARAMETER);
            }

            log.debug("Validated '{}' for {} '{}'", PARAMETER, context.getRequestMethod(), context.getRequestUri());

            context.setLocal(BINDING, sessionToken);

        } else if (HttpMethod.GET.equals(httpSerlvetRequestMethod)) {

            // Generate a CSRF session token on reads
            if (getSessionCsrfToken(context) == null) {
                String sessionId = getTokenId(context);
                String token = CryptoUtil.hmacDigest(sessionId, secretKey, algorithm);
                setSessionCsrfToken(context, token);
                log.debug("Generated '{}' for {} '{}'", PARAMETER, httpSerlvetRequestMethod, context.getRequestUri());
            }

            String token = getSessionCsrfToken(context);
            context.setLocal(BINDING, token);
        }

        context.next();
    }

}

