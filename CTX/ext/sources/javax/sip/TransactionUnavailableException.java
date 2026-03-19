package javax.sip;

public class TransactionUnavailableException extends SipException {
    public TransactionUnavailableException() {
    }

    public TransactionUnavailableException(String str) {
        super(str);
    }

    public TransactionUnavailableException(String str, Throwable th) {
        super(str, th);
    }
}
