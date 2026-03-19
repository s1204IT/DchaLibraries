package java.sql;

public class SQLTransientException extends SQLException {
    private static final long serialVersionUID = -9042733978262274539L;

    public SQLTransientException() {
    }

    public SQLTransientException(String str) {
        super(str);
    }

    public SQLTransientException(String str, String str2) {
        super(str, str2);
    }

    public SQLTransientException(String str, String str2, int i) {
        super(str, str2, i);
    }

    public SQLTransientException(Throwable th) {
        super(th);
    }

    public SQLTransientException(String str, Throwable th) {
        super(str, th);
    }

    public SQLTransientException(String str, String str2, Throwable th) {
        super(str, str2, th);
    }

    public SQLTransientException(String str, String str2, int i, Throwable th) {
        super(str, str2, i, th);
    }
}
