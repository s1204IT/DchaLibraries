package com.android.documentsui.picker;

import android.app.Activity;
import com.android.documentsui.AbstractActionHandler;
import com.android.documentsui.AbstractActionHandler.CommonAddons;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.PairedTask;
import com.android.documentsui.base.State;
import com.android.documentsui.roots.ProvidersAccess;
import java.util.function.Consumer;

final class LoadLastAccessedStackTask<T extends Activity & AbstractActionHandler.CommonAddons> extends PairedTask<T, Void, DocumentStack> {
    private final Consumer<DocumentStack> mCallback;
    private final LastAccessedStorage mLastAccessed;
    private final ProvidersAccess mProviders;
    private final State mState;

    LoadLastAccessedStackTask(T t, LastAccessedStorage lastAccessedStorage, State state, ProvidersAccess providersAccess, Consumer<DocumentStack> consumer) {
        super(t);
        this.mLastAccessed = lastAccessedStorage;
        this.mProviders = providersAccess;
        this.mState = state;
        this.mCallback = consumer;
    }

    @Override
    protected DocumentStack run(Void... voidArr) {
        return this.mLastAccessed.getLastAccessed(this.mOwner, this.mProviders, this.mState);
    }

    @Override
    protected void finish(DocumentStack documentStack) {
        this.mCallback.accept(documentStack);
    }
}
