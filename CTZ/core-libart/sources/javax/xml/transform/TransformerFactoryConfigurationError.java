package javax.xml.transform;

public class TransformerFactoryConfigurationError extends Error {
    private Exception exception;

    public TransformerFactoryConfigurationError() {
        this.exception = null;
    }

    public TransformerFactoryConfigurationError(String str) {
        super(str);
        this.exception = null;
    }

    public TransformerFactoryConfigurationError(Exception exc) {
        super(exc.toString());
        this.exception = exc;
    }

    public TransformerFactoryConfigurationError(Exception exc, String str) {
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
