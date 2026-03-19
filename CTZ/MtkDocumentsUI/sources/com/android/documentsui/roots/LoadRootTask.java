package com.android.documentsui.roots;

import android.app.Activity;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.AbstractActionHandler;
import com.android.documentsui.AbstractActionHandler.CommonAddons;
import com.android.documentsui.base.PairedTask;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;

public final class LoadRootTask<T extends Activity & AbstractActionHandler.CommonAddons> extends PairedTask<T, Void, RootInfo> {
    private final ProvidersAccess mProviders;
    private final Uri mRootUri;
    private final State mState;

    public LoadRootTask(T t, ProvidersAccess providersAccess, State state, Uri uri) {
        super(t);
        this.mState = state;
        this.mProviders = providersAccess;
        this.mRootUri = uri;
    }

    @Override
    protected RootInfo run(Void... voidArr) {
        if (SharedMinimal.DEBUG) {
            Log.d("LoadRootTask", "Loading root: " + this.mRootUri);
        }
        return this.mProviders.getRootOneshot(this.mRootUri.getAuthority(), DocumentsContract.getRootId(this.mRootUri));
    }

    @Override
    protected void finish(RootInfo rootInfo) {
        if (rootInfo != null) {
            if (SharedMinimal.DEBUG) {
                Log.d("LoadRootTask", "Loaded root: " + rootInfo);
            }
            ((AbstractActionHandler.CommonAddons) this.mOwner).onRootPicked(rootInfo);
            return;
        }
        Log.w("LoadRootTask", "Failed to find root: " + this.mRootUri);
        this.mOwner.finish();
    }
}
