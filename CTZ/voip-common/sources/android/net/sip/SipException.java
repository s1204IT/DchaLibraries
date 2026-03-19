package android.net.sip;

public class SipException extends Exception {
    public SipException() {
    }

    public SipException(String str) {
        super(str);
    }

    public SipException(String str, Throwable th) {
        if ((th instanceof javax.sip.SipException) && th.getCause() != null) {
            th = th.getCause();
        }
        super(str, th);
    }
}
