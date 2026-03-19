package java.sql;

public class SQLTransactionRollbackException extends SQLTransientException {
    private static final long serialVersionUID = 5246680841170837229L;

    public SQLTransactionRollbackException() {
    }

    public SQLTransactionRollbackException(String str) {
        super(str);
    }

    public SQLTransactionRollbackException(String str, String str2) {
        super(str, str2);
    }

    public SQLTransactionRollbackException(String str, String str2, int i) {
        super(str, str2, i);
    }

    public SQLTransactionRollbackException(Throwable th) {
        super(th);
    }

    public SQLTransactionRollbackException(String str, Throwable th) {
        super(str, th);
    }

    public SQLTransactionRollbackException(String str, String str2, Throwable th) {
        super(str, str2, th);
    }

    public SQLTransactionRollbackException(String str, String str2, int i, Throwable th) {
        super(str, str2, i, th);
    }
}
