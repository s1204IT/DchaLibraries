package java.sql;

public class SQLNonTransientException extends SQLException {
    private static final long serialVersionUID = -9104382843534716547L;

    public SQLNonTransientException() {
    }

    public SQLNonTransientException(String str) {
        super(str);
    }

    public SQLNonTransientException(String str, String str2) {
        super(str, str2);
    }

    public SQLNonTransientException(String str, String str2, int i) {
        super(str, str2, i);
    }

    public SQLNonTransientException(Throwable th) {
        super(th);
    }

    public SQLNonTransientException(String str, Throwable th) {
        super(str, th);
    }

    public SQLNonTransientException(String str, String str2, Throwable th) {
        super(str, str2, th);
    }

    public SQLNonTransientException(String str, String str2, int i, Throwable th) {
        super(str, str2, i, th);
    }
}
