package android.service.autofill;

import android.content.IntentSender;
import android.os.RemoteException;
import com.android.internal.util.Preconditions;

public final class SaveCallback {
    private final ISaveCallback mCallback;
    private boolean mCalled;

    SaveCallback(ISaveCallback iSaveCallback) {
        this.mCallback = iSaveCallback;
    }

    public void onSuccess() {
        onSuccessInternal(null);
    }

    public void onSuccess(IntentSender intentSender) {
        onSuccessInternal((IntentSender) Preconditions.checkNotNull(intentSender));
    }

    private void onSuccessInternal(IntentSender intentSender) {
        assertNotCalled();
        this.mCalled = true;
        try {
            this.mCallback.onSuccess(intentSender);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    public void onFailure(CharSequence charSequence) {
        assertNotCalled();
        this.mCalled = true;
        try {
            this.mCallback.onFailure(charSequence);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    private void assertNotCalled() {
        if (this.mCalled) {
            throw new IllegalStateException("Already called");
        }
    }
}
