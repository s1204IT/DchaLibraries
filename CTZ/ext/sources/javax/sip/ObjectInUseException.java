package javax.sip;

public class ObjectInUseException extends SipException {
    public ObjectInUseException() {
    }

    public ObjectInUseException(String str) {
        super(str);
    }

    public ObjectInUseException(String str, Throwable th) {
        super(str, th);
    }
}
