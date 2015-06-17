package fathom.rest.controller.exceptions;

public class RequiredException extends ValidationException {
    public RequiredException(String message, Object... parameters) {
        super(format(message, parameters));
    }
}