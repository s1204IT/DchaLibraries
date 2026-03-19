package android.net.lowpan;

public class JoinFailedException extends LowpanException {
    public JoinFailedException() {
    }

    public JoinFailedException(String str) {
        super(str);
    }

    public JoinFailedException(String str, Throwable th) {
        super(str, th);
    }

    protected JoinFailedException(Exception exc) {
        super(exc);
    }
}
