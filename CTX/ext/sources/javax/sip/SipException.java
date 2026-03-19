package javax.sip;

public class SipException extends Exception {
    public SipException() {
    }

    public SipException(String str) {
        super(str);
    }

    public SipException(String str, Throwable th) {
        super(str, th);
    }
}
