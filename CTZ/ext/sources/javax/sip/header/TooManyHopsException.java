package javax.sip.header;

public class TooManyHopsException extends Exception {
    public TooManyHopsException() {
    }

    public TooManyHopsException(String str) {
        super(str);
    }

    public TooManyHopsException(String str, Throwable th) {
        super(str, th);
    }
}
