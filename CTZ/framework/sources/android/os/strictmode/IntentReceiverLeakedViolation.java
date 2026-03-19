package android.os.strictmode;

public final class IntentReceiverLeakedViolation extends Violation {
    public IntentReceiverLeakedViolation(Throwable th) {
        super(null);
        setStackTrace(th.getStackTrace());
    }
}
