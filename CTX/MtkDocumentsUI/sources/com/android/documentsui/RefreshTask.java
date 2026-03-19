package com.android.documentsui;

import android.content.ContentProviderClient;
import android.content.Context;
import android.os.CancellationSignal;
import android.util.Log;
import com.android.documentsui.base.BooleanConsumer;
import com.android.documentsui.base.CheckedTask;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.base.State;

public class RefreshTask extends TimeoutTask<Void, Boolean> {
    private final BooleanConsumer mCallback;
    private final Context mContext;
    private final DocumentInfo mDoc;
    private final Features mFeatures;
    private final CancellationSignal mSignal;
    private final State mState;

    public RefreshTask(Features features, State state, DocumentInfo documentInfo, long j, Context context, CheckedTask.Check check, BooleanConsumer booleanConsumer) {
        super(check, j);
        this.mFeatures = features;
        this.mState = state;
        this.mDoc = documentInfo;
        this.mContext = context;
        this.mCallback = booleanConsumer;
        this.mSignal = new CancellationSignal();
    }

    @Override
    public Boolean run(Void... voidArr) throws Throwable {
        ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow;
        boolean z = false;
        if (this.mDoc == null) {
            Log.w("RefreshTask", "Ignoring attempt to refresh due to null DocumentInfo.");
            return false;
        }
        if (this.mState.stack.isEmpty()) {
            Log.w("RefreshTask", "Ignoring attempt to refresh due to empty stack.");
            return false;
        }
        if (!this.mDoc.derivedUri.equals(this.mState.stack.peek().derivedUri)) {
            Log.w("RefreshTask", "Ignoring attempt to refresh on a non-top-level uri.");
            return false;
        }
        if (!this.mFeatures.isContentRefreshEnabled()) {
            Log.w("RefreshTask", "Ignoring attempt to call Refresh on an older Android platform.");
            return false;
        }
        ContentProviderClient contentProviderClient = null;
        try {
            try {
                contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(this.mContext.getContentResolver(), this.mDoc.authority);
            } catch (Throwable th) {
                th = th;
                contentProviderClientAcquireUnstableProviderOrThrow = contentProviderClient;
            }
        } catch (Exception e) {
            e = e;
        }
        try {
            boolean zRefresh = contentProviderClientAcquireUnstableProviderOrThrow.refresh(this.mDoc.derivedUri, null, this.mSignal);
            ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
            z = zRefresh;
        } catch (Exception e2) {
            e = e2;
            contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
            Log.w("RefreshTask", "Failed to refresh", e);
            ContentProviderClient.releaseQuietly(contentProviderClient);
        } catch (Throwable th2) {
            th = th2;
            ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
            throw th;
        }
        return Boolean.valueOf(z);
    }

    @Override
    protected void onTimeout() {
        this.mSignal.cancel();
        Log.w("RefreshTask", "Provider taking too long to respond. Cancelling.");
    }

    @Override
    public void finish(Boolean bool) {
        if (SharedMinimal.DEBUG) {
            if (Boolean.TRUE.equals(bool)) {
                Log.v("RefreshTask", "Provider supports refresh and has refreshed");
            } else {
                Log.v("RefreshTask", "Provider does not support refresh and did not refresh");
            }
        }
        BooleanConsumer booleanConsumer = this.mCallback;
        if (bool == null) {
            bool = Boolean.FALSE;
        }
        booleanConsumer.accept(bool.booleanValue());
    }
}
