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

package fathom.rest;

import com.google.common.base.MoreObjects;
import fathom.Constants;
import fathom.metrics.Counted;
import fathom.metrics.Metered;
import fathom.metrics.Timed;
import ro.pippo.core.route.RouteHandler;
import ro.pippo.core.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.EnumSet;

/**
 * @author James Moger
 */
public class RouteRegistration {

    private String requestMethod;
    private String uriPattern;
    private RouteHandler routeHandler;
    private boolean runAsFinally;
    private String name;

    private Class<? extends Annotation> metricClass;
    private String metricName;
    private EnumSet<Constants.Mode> modes;

    public RouteRegistration(String requestMethod, String uriPattern, RouteHandler routeHandler) {
        this.requestMethod = requestMethod;
        this.uriPattern = uriPattern;
        this.routeHandler = routeHandler;
    }

    public String getUriPattern() {
        return uriPattern;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public RouteHandler getRouteHandler() {
        return routeHandler;
    }

    public boolean isRunAsFinally() {
        return runAsFinally;
    }

    /**
     * Mark this route to be invoked even when exceptions were raised in previous routes.
     * This flag make sense only for an after filter.
     */
    public void runAsFinally() {
        runAsFinally = true;
    }

    public String getName() {
        return name;
    }

    public RouteRegistration named(String name) {
        this.name = name;

        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RouteRegistration metered() {
        return meteredAs(getName());
    }

    public RouteRegistration meteredAs(String name) {
        setMetricClass(Metered.class, name);

        return this;
    }

    public boolean isMetered() {
        return Metered.class == metricClass;
    }

    public RouteRegistration timed() {
        return timedAs(getName());
    }

    public RouteRegistration timedAs(String name) {
        setMetricClass(Timed.class, name);

        return this;
    }

    public boolean isTimed() {
        return Timed.class == metricClass;
    }

    public RouteRegistration counted() {
        return countedAs(getName());
    }

    public RouteRegistration countedAs(String name) {
        setMetricClass(Counted.class, name);

        return this;
    }

    public boolean isCounted() {
        return Timed.class == metricClass;
    }

    public String getMetricName() {
        return metricName;
    }

    private void setMetricClass(Class<? extends Annotation> metricClass, String name) {
        this.metricClass = metricClass;
        this.metricName = name == null ? (getRequestMethod() + "." + StringUtils.removeStart(getUriPattern(), "/")) : name;
    }

    public RouteRegistration modes(Constants.Mode mode, Constants.Mode... modes) {
        this.modes = EnumSet.of(mode, modes);

        return this;
    }

    public EnumSet<Constants.Mode> getModes() {
        return modes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteRegistration routeRegistration = (RouteRegistration) o;

        if (!requestMethod.equals(routeRegistration.requestMethod)) return false;
        if (!uriPattern.equals(routeRegistration.uriPattern)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uriPattern.hashCode();
        result = 31 * result + requestMethod.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("requestMethod", getRequestMethod())
                .add("uriPattern", getUriPattern())
                .add("name", getName())
                .toString();
    }
}
