package java.lang;

public class ClassNotFoundException extends ReflectiveOperationException {
    private static final long serialVersionUID = 9176873029745254542L;
    private Throwable ex;

    public ClassNotFoundException() {
        super((Throwable) null);
    }

    public ClassNotFoundException(String str) {
        super(str, null);
    }

    public ClassNotFoundException(String str, Throwable th) {
        super(str, null);
        this.ex = th;
    }

    public Throwable getException() {
        return this.ex;
    }

    @Override
    public Throwable getCause() {
        return this.ex;
    }
}
