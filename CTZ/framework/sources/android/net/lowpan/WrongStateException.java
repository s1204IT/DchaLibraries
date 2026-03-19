package android.net.lowpan;

public class WrongStateException extends LowpanException {
    public WrongStateException() {
    }

    public WrongStateException(String str) {
        super(str);
    }

    public WrongStateException(String str, Throwable th) {
        super(str, th);
    }

    protected WrongStateException(Exception exc) {
        super(exc);
    }
}
