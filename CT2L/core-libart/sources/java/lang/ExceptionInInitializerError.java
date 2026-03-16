package java.lang;

public class ExceptionInInitializerError extends LinkageError {
    private static final long serialVersionUID = 1521711792217232256L;
    private Throwable exception;

    public ExceptionInInitializerError() {
        initCause(null);
    }

    public ExceptionInInitializerError(String detailMessage) {
        super(detailMessage);
        initCause(null);
    }

    public ExceptionInInitializerError(Throwable exception) {
        this.exception = exception;
        initCause(exception);
    }

    public Throwable getException() {
        return this.exception;
    }

    @Override
    public Throwable getCause() {
        return this.exception;
    }
}
