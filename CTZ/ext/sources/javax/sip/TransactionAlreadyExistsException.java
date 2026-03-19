package javax.sip;

public class TransactionAlreadyExistsException extends SipException {
    public TransactionAlreadyExistsException() {
    }

    public TransactionAlreadyExistsException(String str) {
        super(str);
    }

    public TransactionAlreadyExistsException(String str, Throwable th) {
        super(str, th);
    }
}
