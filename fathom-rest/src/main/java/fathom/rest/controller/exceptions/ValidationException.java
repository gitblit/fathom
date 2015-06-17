package fathom.rest.controller.exceptions;

import fathom.exception.FathomException;

/**
 * @author James Moger
 */
public class ValidationException extends FathomException {

    public ValidationException() {
        this("");
    }

    public ValidationException(String message, Object... parameters) {
        super(format(message, parameters));
    }

}
