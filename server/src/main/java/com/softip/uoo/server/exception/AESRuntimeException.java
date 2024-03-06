package com.softip.uoo.server.exception;

public class AESRuntimeException extends RuntimeException {
    public AESRuntimeException() {
        super();
    }

    public AESRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public AESRuntimeException(String message) {
        super(message);
    }

    public AESRuntimeException(Throwable cause) {
        super(cause);
    }
}
