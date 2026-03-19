package org.apache.commons.logging;

@Deprecated
public class LogConfigurationException extends RuntimeException {
    protected Throwable cause;

    public LogConfigurationException() {
        this.cause = null;
    }

    public LogConfigurationException(String message) {
        super(message);
        this.cause = null;
    }

    public LogConfigurationException(Throwable cause) {
        this(cause != null ? cause.toString() : null, cause);
    }

    public LogConfigurationException(String message, Throwable cause) {
        super(message + " (Caused by " + cause + ")");
        this.cause = null;
        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
