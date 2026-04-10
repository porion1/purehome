package com.purehome.uicore.exception;

/**
 * Exception thrown when a version is not found
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
public class VersionNotFoundException extends RuntimeException {

    public VersionNotFoundException(String message) {
        super(message);
    }

    public VersionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}