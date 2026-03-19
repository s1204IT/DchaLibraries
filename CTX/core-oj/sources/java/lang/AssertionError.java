package java.lang;

public class AssertionError extends Error {
    private static final long serialVersionUID = -5013299493970297370L;

    public AssertionError() {
    }

    private AssertionError(String str) {
        super(str);
    }

    public AssertionError(Object obj) {
        this(String.valueOf(obj));
        if (obj instanceof Throwable) {
            initCause((Throwable) obj);
        }
    }

    public AssertionError(boolean z) {
        this(String.valueOf(z));
    }

    public AssertionError(char c) {
        this(String.valueOf(c));
    }

    public AssertionError(int i) {
        this(String.valueOf(i));
    }

    public AssertionError(long j) {
        this(String.valueOf(j));
    }

    public AssertionError(float f) {
        this(String.valueOf(f));
    }

    public AssertionError(double d) {
        this(String.valueOf(d));
    }

    public AssertionError(String str, Throwable th) {
        super(str, th);
    }
}
