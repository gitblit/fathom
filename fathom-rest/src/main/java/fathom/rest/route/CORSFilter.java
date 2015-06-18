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

package fathom.rest.route;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import fathom.rest.Context;
import fathom.rest.controller.HttpMethod;
import fathom.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.pippo.core.route.RouteHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * CORSFilter should only be applied to Routes that will be called from another domain by a browser.
 *
 * If you are designing a webapp whose API is accessed by a browser from content served from the
 * same webapp (i.e. same-origin) then you don't need a CORS filter.
 *
 * If you are using your API from both a same-origin-browser client and a non-browser client,
 * then you don't need a CORS filter.
 *
 * If you are using your API from a browser on multiple domains, then you do need a CORS filter.
 *
 * For example, I am browsing the page http://mydomain.com/example.html and from that page I make
 * an AJAX request to http://otherdomain.com/api/something, then I need a CORS filter running on
 * http://otherdomain.com which accepts requests originating from http://mydomain.com.
 *
 * A CORS filter relaxes the default same-origin policy of the browser.
 * https://en.wikipedia.org/wiki/Same-origin_policy
 * http://www.html5rocks.com/en/tutorials/cors/
 *
 * @author James Moger
 */
public class CORSFilter implements RouteHandler<Context> {

    public final static String HEADER_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public final static String HEADER_ALLOW_METHODS = "Access-Control-Allow-Methods";

    public final static String HEADER_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    public final static String HEADER_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    public final static String HEADER_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    public final static String HEADER_MAX_AGE = "Access-Control-Max-Age";

    public final static String HEADER_ORIGIN = "Origin";

    public final static String HEADER_REQUEST_METHOD = "Access-Control-Request-Method";

    public final static String HEADER_REQUEST_HEADERS = "Access-Control-Request-Headers";

    private final Logger log = LoggerFactory.getLogger(CORSFilter.class);

    protected Set<String> allowOriginSet = new HashSet<>();

    protected Set<String> allowMethodsSet = new HashSet<>();

    protected Set<String> allowHeadersSet = new HashSet<>();

    protected Set<String> exposeHeadersSet = new HashSet<>();

    protected String allowOrigin;

    protected String allowMethods;

    protected String allowHeaders;

    protected Integer maxAge;

    protected String exposeHeaders;

    protected Boolean allowCredentials;

    protected int corsErrorStatus = 200;

    /**
     * Set the list of request origins that are permitted to access the protected routes.
     *
     * Multiple origins may be specified and they must be complete scheme, domain, & port
     * specifications.  Alternatively, you can specify the wildcard origin "*" to accept
     * from all origins.
     *
     * setAllowOrigin("*");
     * setAllowOrigin("http://mydomain.com:8080", http://myotherdomain.com:8080");
     *
     * @param origin
     */
    public void setAllowOrigin(String... origin) {
        allowOriginSet.clear();
        allowOriginSet.addAll(Arrays.asList(origin));
        allowOrigin = Joiner.on(",").join(origin);
    }

    /**
     * Set the list of request methods that may be sent by the browser for a CORS request.
     *
     * setAllowMethods("GET", "PUT", "PATCH");
     *
     * @param methods
     */
    public void setAllowMethods(String... methods) {
        allowMethodsSet.clear();
        for (String method : methods) {
            allowMethodsSet.add(method.toUpperCase());
        }
        allowMethods = Joiner.on(",").join(allowMethodsSet);
    }

    /**
     * Set the list of headers that may be sent by the browser for a CORS request.
     *
     * setAllowHeaders("Content-Type", "api_key", "Csrf-Token");
     *
     * @param headers
     */
    public void setAllowHeaders(String... headers) {
        allowHeadersSet.clear();
        for (String header : headers) {
            allowHeadersSet.add(header.toLowerCase());
        }
        allowHeaders = Joiner.on(",").join(headers);
    }

    /**
     * During a CORS request, the browser can only access simple response headers such as:
     *
     * <ul>
     * <li>Cache-Control</li>
     * <li>Content-Language</li>
     * <li>Content-Type</li>
     * <li>Expires</li>
     * <li>Last-Modified</li>
     * <li>Pragma</li>
     * </ul>
     *
     * If you want clients to be able to access other headers, you have to Expose these headers.
     * @param header
     */
    public void setExposeHeaders(String... header) {
        exposeHeadersSet.addAll(Arrays.asList(header));
        exposeHeaders = Joiner.on(",").join(header);
    }

    /**
     * Permit the browser to send cookies with a CORS request. By default the browser will not send cookies.
     *
     * @param value
     */
    public void setAllowCredentials(boolean value) {
        allowCredentials = value;
    }

    /**
     * Set the number of seconds that a browser may cache the results of a preflight request.
     *
     * @param seconds
     */
    public void setPreflightMaxAge(int seconds) {
        this.maxAge = seconds;
    }

    /**
     * Set the response status code to send to the client in the event that the request is invalid.
     * The default status code is 200 which is an acceptable status code for a failed CORS request.
     *
     * @param status
     */
    public void setCorsErrorStatus(int status) {
        this.corsErrorStatus = status;
    }

    @Override
    public void handle(Context context) {

        // A valid CORS request *always* contains an Origin header
        // A same-origin request may or may not contain an Origin, it depends on the browser
        final String origin = Strings.emptyToNull(context.getHeader(HEADER_ORIGIN));
        final String preflightMethod = Strings.emptyToNull(context.getHeader(HEADER_REQUEST_METHOD));

        if (HttpMethod.OPTIONS.equals(context.getRequestMethod()) && preflightMethod != null) {

            // Preflight request
            Set<String> preflightHeaders = null;
            String headers = Strings.emptyToNull(context.getHeader(HEADER_REQUEST_HEADERS));
            if (headers != null) {
                preflightHeaders = Util.splitToSet(headers.toLowerCase(), ",");
                preflightHeaders.remove("accept");
                preflightHeaders.remove("accept-language");
                preflightHeaders.remove("content-language");
                preflightHeaders.remove("content-type");
            }

            if (isValidRequest(context, origin, preflightMethod, preflightHeaders)) {
                // Valid Preflight request
                setHeader(context, HEADER_ALLOW_ORIGIN, Optional.fromNullable(origin).or(allowOrigin));
                setHeader(context, HEADER_ALLOW_METHODS, allowMethods);
                setHeader(context, HEADER_ALLOW_HEADERS, allowHeaders);
                setHeader(context, HEADER_ALLOW_CREDENTIALS, allowCredentials);
                setHeader(context, HEADER_MAX_AGE, maxAge);

                // Set OK & break the chain
                context.status(200).getResponse().commit();
            } else {
                // Invalid Preflight request, set the error status & break the chain
                context.status(corsErrorStatus).getResponse().commit();
            }

        } else {

            // Standard request
            String method = context.getRequestMethod();
            if (isValidRequest(context, origin, method, null)) {

                // valid CORS request
                setHeader(context, HEADER_ALLOW_ORIGIN, Optional.fromNullable(origin).or(allowOrigin));
                setHeader(context, HEADER_ALLOW_CREDENTIALS, allowCredentials);
                setHeader(context, HEADER_EXPOSE_HEADERS, exposeHeaders);

                // next handler in chain
                context.next();

            } else {

                // invalid CORS request, set the error status & break the chain
                context.status(corsErrorStatus).getResponse().commit();

            }
        }
    }

    protected void setHeader(Context context, String header, Object value) {
        if (value != null) {
            context.setHeader(header, value);
        }
    }

    protected boolean isValidRequest(Context context, String origin, String method, Set<String> headers) {

        // an actual CORS request *always* specify an Origin header
        // same-origin requests may or may not specify an Origin header, it depends on the browser
        // non-browser clients won't specify an Origin header either
        if (!"*".equals(allowOrigin)) {
            if (origin != null && !allowOriginSet.contains(origin)) {
                log.debug("Prohibited origin {} for {} {}", origin, method, context.getRequestUri());
                return false;
            }
        }

        // validate CORS request method
        if (method != null && !allowMethodsSet.contains(method)) {
            log.debug("Prohibited request method {} for {}", method, context.getRequestUri());
            return false;
        }

        // validate CORS request headers
        if (headers != null && !allowHeadersSet.containsAll(headers)) {
            log.debug("Unexpected request headers {} for {}", headers.toString(), context.getRequestUri());
            return false;
        }
        return true;
    }

}

