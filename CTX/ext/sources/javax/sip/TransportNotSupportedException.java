package javax.sip;

public class TransportNotSupportedException extends SipException {
    public TransportNotSupportedException() {
    }

    public TransportNotSupportedException(String str) {
        super(str);
    }

    public TransportNotSupportedException(String str, Throwable th) {
        super(str, th);
    }
}
