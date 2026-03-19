package android.os.strictmode;

public final class LeakedClosableViolation extends Violation {
    public LeakedClosableViolation(String str, Throwable th) {
        super(str);
        initCause(th);
    }
}
