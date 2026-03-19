package android.net.lowpan;

public class JoinFailedAtScanException extends JoinFailedException {
    public JoinFailedAtScanException() {
    }

    public JoinFailedAtScanException(String str) {
        super(str);
    }

    public JoinFailedAtScanException(String str, Throwable th) {
        super(str, th);
    }

    public JoinFailedAtScanException(Exception exc) {
        super(exc);
    }
}
