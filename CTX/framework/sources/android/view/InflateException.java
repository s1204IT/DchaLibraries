package android.view;

public class InflateException extends RuntimeException {
    public InflateException() {
    }

    public InflateException(String str, Throwable th) {
        super(str, th);
    }

    public InflateException(String str) {
        super(str);
    }

    public InflateException(Throwable th) {
        super(th);
    }
}
