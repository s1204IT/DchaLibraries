package android.net.lowpan;

public class JoinFailedAtAuthException extends JoinFailedException {
    public JoinFailedAtAuthException() {
    }

    public JoinFailedAtAuthException(String str) {
        super(str);
    }

    public JoinFailedAtAuthException(String str, Throwable th) {
        super(str, th);
    }

    public JoinFailedAtAuthException(Exception exc) {
        super(exc);
    }
}
