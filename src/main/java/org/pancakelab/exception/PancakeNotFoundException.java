package org.pancakelab.exception;

/**
 * Exception thrown when a pancake is not found
 */
public class PancakeNotFoundException extends PancakeLabException {
    public PancakeNotFoundException(String pancakeId) {
        super("Pancake not found: " + pancakeId, 404);
    }
}
