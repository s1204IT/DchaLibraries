package javax.sip;

public class PeerUnavailableException extends SipException {
    public PeerUnavailableException() {
    }

    public PeerUnavailableException(String str) {
        super(str);
    }

    public PeerUnavailableException(String str, Throwable th) {
        super(str, th);
    }
}
