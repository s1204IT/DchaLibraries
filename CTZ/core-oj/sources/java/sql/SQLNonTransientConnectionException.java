package java.sql;

public class SQLNonTransientConnectionException extends SQLNonTransientException {
    private static final long serialVersionUID = -5852318857474782892L;

    public SQLNonTransientConnectionException() {
    }

    public SQLNonTransientConnectionException(String str) {
        super(str);
    }

    public SQLNonTransientConnectionException(String str, String str2) {
        super(str, str2);
    }

    public SQLNonTransientConnectionException(String str, String str2, int i) {
        super(str, str2, i);
    }

    public SQLNonTransientConnectionException(Throwable th) {
        super(th);
    }

    public SQLNonTransientConnectionException(String str, Throwable th) {
        super(str, th);
    }

    public SQLNonTransientConnectionException(String str, String str2, Throwable th) {
        super(str, str2, th);
    }

    public SQLNonTransientConnectionException(String str, String str2, int i, Throwable th) {
        super(str, str2, i, th);
    }
}
