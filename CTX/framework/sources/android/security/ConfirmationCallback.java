package android.security;

public abstract class ConfirmationCallback {
    public void onConfirmed(byte[] bArr) {
    }

    public void onDismissed() {
    }

    public void onCanceled() {
    }

    public void onError(Throwable th) {
    }
}
