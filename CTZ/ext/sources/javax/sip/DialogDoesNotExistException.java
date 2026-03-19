package javax.sip;

public class DialogDoesNotExistException extends SipException {
    public DialogDoesNotExistException() {
    }

    public DialogDoesNotExistException(String str) {
        super(str);
    }

    public DialogDoesNotExistException(String str, Throwable th) {
        super(str, th);
    }
}
