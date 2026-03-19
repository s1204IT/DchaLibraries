package javax.sip;

public class TransactionDoesNotExistException extends SipException {
    public TransactionDoesNotExistException() {
    }

    public TransactionDoesNotExistException(String str) {
        super(str);
    }

    public TransactionDoesNotExistException(String str, Throwable th) {
        super(str, th);
    }
}
