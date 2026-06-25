package com.apix.common.exception;

/**
 * LLM provider not found.
 * 对标 Python: ProviderNotFound
 */
public class ProviderNotFoundException extends ApixException {

    public ProviderNotFoundException(String message) {
        super(message);
    }

    public ProviderNotFoundException(String message, String provider) {
        super(message + " [provider=" + provider + "]");
    }
}
