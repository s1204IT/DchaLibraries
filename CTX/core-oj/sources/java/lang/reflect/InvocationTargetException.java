package java.lang.reflect;

public class InvocationTargetException extends ReflectiveOperationException {
    private static final long serialVersionUID = 4085088731926701167L;
    private Throwable target;

    protected InvocationTargetException() {
        super((Throwable) null);
    }

    public InvocationTargetException(Throwable th) {
        super((Throwable) null);
        this.target = th;
    }

    public InvocationTargetException(Throwable th, String str) {
        super(str, null);
        this.target = th;
    }

    public Throwable getTargetException() {
        return this.target;
    }

    @Override
    public Throwable getCause() {
        return this.target;
    }
}
