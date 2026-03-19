package com.android.documentsui.picker;

import android.app.Activity;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.PairedTask;

class SetLastAccessedStackTask extends PairedTask<Activity, Void, Void> {
    private final Runnable mCallback;
    private final LastAccessedStorage mLastAccessed;
    private final DocumentStack mStack;

    SetLastAccessedStackTask(Activity activity, LastAccessedStorage lastAccessedStorage, DocumentStack documentStack, Runnable runnable) {
        super(activity);
        this.mLastAccessed = lastAccessedStorage;
        this.mStack = documentStack;
        this.mCallback = runnable;
    }

    @Override
    protected Void run(Void... voidArr) {
        this.mLastAccessed.setLastAccessed(this.mOwner, this.mStack);
        return null;
    }

    @Override
    protected void finish(Void r1) {
        this.mCallback.run();
    }
}
