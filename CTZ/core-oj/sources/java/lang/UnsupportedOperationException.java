package java.lang;

public class UnsupportedOperationException extends RuntimeException {
    static final long serialVersionUID = -1242599979055084673L;

    public UnsupportedOperationException() {
    }

    public UnsupportedOperationException(String str) {
        super(str);
    }

    public UnsupportedOperationException(String str, Throwable th) {
        super(str, th);
    }

    public UnsupportedOperationException(Throwable th) {
        super(th);
    }
}
