package java.sql;

public class SQLIntegrityConstraintViolationException extends SQLNonTransientException {
    private static final long serialVersionUID = 8033405298774849169L;

    public SQLIntegrityConstraintViolationException() {
    }

    public SQLIntegrityConstraintViolationException(String str) {
        super(str);
    }

    public SQLIntegrityConstraintViolationException(String str, String str2) {
        super(str, str2);
    }

    public SQLIntegrityConstraintViolationException(String str, String str2, int i) {
        super(str, str2, i);
    }

    public SQLIntegrityConstraintViolationException(Throwable th) {
        super(th);
    }

    public SQLIntegrityConstraintViolationException(String str, Throwable th) {
        super(str, th);
    }

    public SQLIntegrityConstraintViolationException(String str, String str2, Throwable th) {
        super(str, str2, th);
    }

    public SQLIntegrityConstraintViolationException(String str, String str2, int i, Throwable th) {
        super(str, str2, i, th);
    }
}
