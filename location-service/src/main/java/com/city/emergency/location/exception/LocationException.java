package com.city.emergency.location.exception;

public class LocationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LocationException(String message) {
        super(message);
    }

    public LocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
