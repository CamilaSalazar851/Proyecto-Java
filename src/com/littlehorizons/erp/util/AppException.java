package com.littlehorizons.erp.util;

/**
 * Unchecked exception used to carry a user-friendly message from the
 * data/business layers up to the view layer.
 */
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
