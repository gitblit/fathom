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

package fathom.rest.controller;

import fathom.rest.Context;
import ro.pippo.core.util.StringUtils;

import java.lang.reflect.Method;

/**
 * Represents a RESTful response to a controller request.
 *
 * @author James Moger
 */
public class Result implements ControllerResult {

    private int statusCode;

    private String message;

    private Object value;

    protected Result(int statusCode) {
        this.statusCode = statusCode;
    }

    public static Result status(int statusCode) {
        return new Result(statusCode);
    }

    public static Result ok() {
        return status(200);
    }

    public static Result ok(String message, Object... args) {
        return ok().message(message, args);
    }

    public static Result ok(Object value) {
        return ok().value(value);
    }

    public static Result created() {
        return status(201);
    }

    public static Result created(String message, Object... args) {
        return created().message(message, args);
    }

    public static Result created(Object value) {
        return created().value(value);
    }

    public static Result accepted() {
        return status(202);
    }

    public static Result accepted(String message, Object... args) {
        return accepted().message(message, args);
    }

    public static Result accepted(Object value) {
        return accepted().value(value);
    }

    public static Result badRequest() {
        return status(400);
    }

    public static Result badRequest(String message, Object... args) {
        return badRequest().message(message, args);
    }

    public static Result badRequest(Object value) {
        return badRequest().value(value);
    }

    public static Result unauthorized() {
        return status(401);
    }

    public static Result unauthorized(String message, Object... args) {
        return unauthorized().message(message, args);
    }

    public static Result unauthorized(Object value) {
        return unauthorized().value(value);
    }

    public static Result paymentRequired() {
        return status(402);
    }

    public static Result paymentRequired(String message, Object... args) {
        return paymentRequired().message(message, args);
    }

    public static Result paymentRequired(Object value) {
        return paymentRequired().value(value);
    }

    public static Result forbidden() {
        return status(403);
    }

    public static Result forbidden(String message, Object... args) {
        return forbidden().message(message, args);
    }

    public static Result forbidden(Object value) {
        return forbidden().value(value);
    }

    public static Result notFound() {
        return status(404);
    }

    public static Result notFound(String message, Object... args) {
        return notFound().message(message, args);
    }

    public static Result notFound(Object value) {
        return notFound().value(value);
    }

    public static Result notAllowed() {
        return status(405);
    }

    public static Result notAllowed(String message, Object... args) {
        return notAllowed().message(message, args);
    }

    public static Result notAllowed(Object value) {
        return notAllowed().value(value);
    }

    public static Result conflict() {
        return status(409);
    }

    public static Result conflict(String message, Object... args) {
        return conflict().message(message, args);
    }

    public static Result conflict(Object value) {
        return conflict().value(value);
    }

    public static Result gone() {
        return status(410);
    }

    public static Result gone(String message, Object... args) {
        return gone().message(message, args);
    }

    public static Result gone(Object value) {
        return gone().value(value);
    }

    public static Result internalError() {
        return status(500);
    }

    public static Result internalError(String message, Object... args) {
        return internalError().message(message, args);
    }

    public static Result internalError(Object value) {
        return internalError().value(value);
    }

    public static Result notImplemented() {
        return status(501);
    }

    public static Result notImplemented(String message, Object... args) {
        return notImplemented().message(message, args);
    }

    public static Result notImplemented(Object value) {
        return notImplemented().value(value);
    }

    public static Result overloaded() {
        return status(502);
    }

    public static Result overloaded(String message, Object... args) {
        return overloaded().message(message, args);
    }

    public static Result overloaded(Object value) {
        return overloaded().value(value);
    }

    public static Result unavailable() {
        return status(503);
    }

    public static Result unavailable(String message, Object... args) {
        return unavailable().message(message, args);
    }

    public static Result unavailable(Object value) {
        return unavailable().value(value);
    }

    public boolean hasMessage() {
        return message != null;
    }

    public boolean hasValue() {
        return value != null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void process(Context context, Method controllerMethod) {
        context.negotiateContentType().status(getStatusCode());
        if (hasMessage()) {
            context.send(getMessage());
        } else if (hasValue()) {
            context.send(getValue());
        }
    }

    public Result value(Object value) {
        this.value = value;

        return this;
    }

    public Result message(String message, Object... args) {
        if (args == null || args.length == 0) {
            this.message = message;
        } else {
            this.message = StringUtils.format(message, args);
        }

        return this;
    }
}
