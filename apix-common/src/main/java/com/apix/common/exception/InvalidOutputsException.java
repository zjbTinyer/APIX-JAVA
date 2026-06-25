package com.apix.common.exception;

/**
 * Invalid LLM outputs detected.
 * 对标 Python: InvalidOutputsError
 */
public class InvalidOutputsException extends ApixException {

    public InvalidOutputsException(String message) {
        super(message);
    }
}
