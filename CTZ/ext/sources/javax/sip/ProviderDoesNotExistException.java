package javax.sip;

public class ProviderDoesNotExistException extends SipException {
    public ProviderDoesNotExistException() {
    }

    public ProviderDoesNotExistException(String str) {
        super(str);
    }

    public ProviderDoesNotExistException(String str, Throwable th) {
        super(str, th);
    }
}
