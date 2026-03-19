package java.sql;

public class SQLRecoverableException extends SQLException {
    private static final long serialVersionUID = -4144386502923131579L;

    public SQLRecoverableException() {
    }

    public SQLRecoverableException(String str) {
        super(str);
    }

    public SQLRecoverableException(String str, String str2) {
        super(str, str2);
    }

    public SQLRecoverableException(String str, String str2, int i) {
        super(str, str2, i);
    }

    public SQLRecoverableException(Throwable th) {
        super(th);
    }

    public SQLRecoverableException(String str, Throwable th) {
        super(str, th);
    }

    public SQLRecoverableException(String str, String str2, Throwable th) {
        super(str, str2, th);
    }

    public SQLRecoverableException(String str, String str2, int i, Throwable th) {
        super(str, str2, i, th);
    }
}
