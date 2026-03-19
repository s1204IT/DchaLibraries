package java.lang.invoke;

public class WrongMethodTypeException extends RuntimeException {
    private static final long serialVersionUID = 292;

    public WrongMethodTypeException() {
    }

    public WrongMethodTypeException(String str) {
        super(str);
    }

    WrongMethodTypeException(String str, Throwable th) {
        super(str, th);
    }

    WrongMethodTypeException(Throwable th) {
        super(th);
    }
}
