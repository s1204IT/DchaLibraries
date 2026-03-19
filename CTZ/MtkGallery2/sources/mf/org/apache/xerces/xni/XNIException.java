package mf.org.apache.xerces.xni;

public class XNIException extends RuntimeException {
    static final long serialVersionUID = 9019819772686063775L;
    private Exception fException;

    public XNIException(String message) {
        super(message);
        this.fException = this;
    }

    public XNIException(Exception exception) {
        super(exception.getMessage());
        this.fException = this;
        this.fException = exception;
    }

    public XNIException(String message, Exception exception) {
        super(message);
        this.fException = this;
        this.fException = exception;
    }

    public Exception getException() {
        if (this.fException != this) {
            return this.fException;
        }
        return null;
    }

    @Override
    public synchronized Throwable initCause(Throwable throwable) {
        if (this.fException != this) {
            throw new IllegalStateException();
        }
        if (throwable == this) {
            throw new IllegalArgumentException();
        }
        this.fException = (Exception) throwable;
        return this;
    }

    @Override
    public Throwable getCause() {
        return getException();
    }
}
