package javax.sip;

public class InvalidArgumentException extends SipException {
    public InvalidArgumentException() {
    }

    public InvalidArgumentException(String str) {
        super(str);
    }

    public InvalidArgumentException(String str, Throwable th) {
        super(str, th);
    }
}
