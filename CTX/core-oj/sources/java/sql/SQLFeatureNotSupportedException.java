package java.sql;

public class SQLFeatureNotSupportedException extends SQLNonTransientException {
    private static final long serialVersionUID = -1026510870282316051L;

    public SQLFeatureNotSupportedException() {
    }

    public SQLFeatureNotSupportedException(String str) {
        super(str);
    }

    public SQLFeatureNotSupportedException(String str, String str2) {
        super(str, str2);
    }

    public SQLFeatureNotSupportedException(String str, String str2, int i) {
        super(str, str2, i);
    }

    public SQLFeatureNotSupportedException(Throwable th) {
        super(th);
    }

    public SQLFeatureNotSupportedException(String str, Throwable th) {
        super(str, th);
    }

    public SQLFeatureNotSupportedException(String str, String str2, Throwable th) {
        super(str, str2, th);
    }

    public SQLFeatureNotSupportedException(String str, String str2, int i, Throwable th) {
        super(str, str2, i, th);
    }
}
