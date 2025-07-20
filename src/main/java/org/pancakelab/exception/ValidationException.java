package org.pancakelab.exception;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends PancakeLabException {
    public ValidationException(String message) {
        super(message, 400);
    }

    public ValidationException(String field, String reason) {
        super("Validation failed for field '" + field + "': " + reason, 400);
    }
}
