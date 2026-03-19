package javax.xml.parsers;

public class FactoryConfigurationError extends Error {
    private Exception exception;

    public FactoryConfigurationError() {
        this.exception = null;
    }

    public FactoryConfigurationError(String str) {
        super(str);
        this.exception = null;
    }

    public FactoryConfigurationError(Exception exc) {
        super(exc.toString());
        this.exception = exc;
    }

    public FactoryConfigurationError(Exception exc, String str) {
        super(str);
        this.exception = exc;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (message == null && this.exception != null) {
            return this.exception.getMessage();
        }
        return message;
    }

    public Exception getException() {
        return this.exception;
    }
}
