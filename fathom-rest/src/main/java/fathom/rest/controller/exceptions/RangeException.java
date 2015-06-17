package fathom.rest.controller.exceptions;

public class RangeException extends ValidationException {

    public RangeException() {
        this("");
    }

    public RangeException(String message, Object... parameters) {
        super(format(message, parameters));
    }
}