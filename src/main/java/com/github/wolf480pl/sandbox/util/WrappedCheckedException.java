package com.github.wolf480pl.sandbox.util;

public class WrappedCheckedException extends RuntimeException {
    private static final long serialVersionUID = 7520666984508345582L;

    public WrappedCheckedException(Throwable cause) {
        super(cause);
    }

    public WrappedCheckedException(String message, Throwable cause) {
        super(message, cause);
    }
}
