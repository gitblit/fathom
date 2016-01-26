package fathom.rest.controller;

import fathom.rest.controller.RouteInterceptor;
import fathom.rest.security.BasicAuthenticationHandler;
import fathom.rest.security.BasicTokenAuthenticationHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authenticates a request using http BASIC or TOKEN auth.
 *
 * @author James Moger
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@RouteInterceptor(BasicTokenAuthenticationHandler.class)
public @interface BasicTokenAuth {
}
