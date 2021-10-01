package io.github.chrisruffalo.yyall.exception;

public class YyallRuntimeException extends RuntimeException {

    public YyallRuntimeException() {
        super();
    }

    public YyallRuntimeException(String message) {
        super(message);
    }

    public YyallRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public YyallRuntimeException(Throwable cause) {
        super(cause);
    }

    protected YyallRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
