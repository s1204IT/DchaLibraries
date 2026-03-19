package android.service.autofill;

import android.os.RemoteException;

public final class FillCallback {
    private final IFillCallback mCallback;
    private boolean mCalled;
    private final int mRequestId;

    public FillCallback(IFillCallback iFillCallback, int i) {
        this.mCallback = iFillCallback;
        this.mRequestId = i;
    }

    public void onSuccess(FillResponse fillResponse) {
        assertNotCalled();
        this.mCalled = true;
        if (fillResponse != null) {
            fillResponse.setRequestId(this.mRequestId);
        }
        try {
            this.mCallback.onSuccess(fillResponse);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    public void onFailure(CharSequence charSequence) {
        assertNotCalled();
        this.mCalled = true;
        try {
            this.mCallback.onFailure(this.mRequestId, charSequence);
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
