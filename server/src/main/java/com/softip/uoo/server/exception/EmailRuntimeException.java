package com.softip.uoo.server.exception;

public class EmailRuntimeException extends RuntimeException {
    public EmailRuntimeException() {
        super();
    }

    public EmailRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailRuntimeException(String message) {
        super(message);
    }

    public EmailRuntimeException(Throwable cause) {
        super(cause);
    }
}
