package fathom.rest.controller;

import fathom.rest.controller.RouteInterceptor;
import fathom.rest.security.PassiveBasicTokenAuthenticationHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Passively authenticates a request using http BASIC or TOKEN auth.
 * i.e. authentication is not explicitly required.
 *
 * @author James Moger
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@RouteInterceptor(PassiveBasicTokenAuthenticationHandler.class)
public @interface PassiveBasicTokenAuth {
}
