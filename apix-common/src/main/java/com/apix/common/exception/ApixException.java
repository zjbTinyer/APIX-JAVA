package com.apix.common.exception;

/**
 * Base exception for all Apix exceptions.
 */
public class ApixException extends RuntimeException {

    public ApixException(String message) {
        super(message);
    }

    public ApixException(String message, Throwable cause) {
        super(message, cause);
    }
}
