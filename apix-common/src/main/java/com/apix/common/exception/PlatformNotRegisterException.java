package com.apix.common.exception;

/**
 * Platform not registered.
 * 对标 Python: PlatformNotRegister
 */
public class PlatformNotRegisterException extends ApixException {

    public PlatformNotRegisterException(String message) {
        super(message);
    }

    public PlatformNotRegisterException(String message, String platform) {
        super(message + " [platform=" + platform + "]");
    }
}
