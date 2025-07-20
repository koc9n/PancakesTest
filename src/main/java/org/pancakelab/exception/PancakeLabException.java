package org.pancakelab.exception;

/**
 * Base exception for all PancakeLab specific exceptions
 */
public class PancakeLabException extends Exception {
    private final int statusCode;

    public PancakeLabException(String message) {
        this(message, 500);
    }

    public PancakeLabException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public PancakeLabException(String message, Throwable cause) {
        this(message, cause, 500);
    }

    public PancakeLabException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
