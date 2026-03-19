package java.sql;

public class SQLInvalidAuthorizationSpecException extends SQLNonTransientException {
    private static final long serialVersionUID = -64105250450891498L;

    public SQLInvalidAuthorizationSpecException() {
    }

    public SQLInvalidAuthorizationSpecException(String str) {
        super(str);
    }

    public SQLInvalidAuthorizationSpecException(String str, String str2) {
        super(str, str2);
    }

    public SQLInvalidAuthorizationSpecException(String str, String str2, int i) {
        super(str, str2, i);
    }

    public SQLInvalidAuthorizationSpecException(Throwable th) {
        super(th);
    }

    public SQLInvalidAuthorizationSpecException(String str, Throwable th) {
        super(str, th);
    }

    public SQLInvalidAuthorizationSpecException(String str, String str2, Throwable th) {
        super(str, str2, th);
    }

    public SQLInvalidAuthorizationSpecException(String str, String str2, int i, Throwable th) {
        super(str, str2, i, th);
    }
}
