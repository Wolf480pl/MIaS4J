package com.github.wolf480pl.sandbox;

public class RewriteAbortException extends Exception {
    private static final long serialVersionUID = 72081484204385874L;

    public RewriteAbortException() {
    }

    public RewriteAbortException(String message) {
        super(message);
    }

    public RewriteAbortException(Throwable cause) {
        super(cause);
    }

    public RewriteAbortException(String message, Throwable cause) {
        super(message, cause);
    }
}
