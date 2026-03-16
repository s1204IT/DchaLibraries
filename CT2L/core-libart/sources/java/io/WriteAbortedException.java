package java.io;

public class WriteAbortedException extends ObjectStreamException {
    private static final long serialVersionUID = -3326426625597282442L;
    public Exception detail;

    public WriteAbortedException(String detailMessage, Exception rootCause) {
        super(detailMessage);
        this.detail = rootCause;
        initCause(rootCause);
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (this.detail != null) {
            return msg + "; " + this.detail.toString();
        }
        return msg;
    }

    @Override
    public Throwable getCause() {
        return this.detail;
    }
}
