package android.os.strictmode;

public final class ServiceConnectionLeakedViolation extends Violation {
    public ServiceConnectionLeakedViolation(Throwable th) {
        super(null);
        setStackTrace(th.getStackTrace());
    }
}
