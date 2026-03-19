package com.android.documentsui.sidebar;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.base.BooleanConsumer;

public final class EjectRootTask extends AsyncTask<Void, Void, Boolean> {
    private final String TAG = "EjectRootTask";
    private final String mAuthority;
    private final BooleanConsumer mCallback;
    private final ContentResolver mResolver;
    private final String mRootId;

    public EjectRootTask(ContentResolver contentResolver, String str, String str2, BooleanConsumer booleanConsumer) {
        this.mResolver = contentResolver;
        this.mAuthority = str;
        this.mRootId = str2;
        this.mCallback = booleanConsumer;
    }

    @Override
    protected Boolean doInBackground(Void... voidArr) throws Throwable {
        ContentProviderClient contentProviderClientAcquireUnstableProviderOrThrow;
        Uri uriBuildRootUri = DocumentsContract.buildRootUri(this.mAuthority, this.mRootId);
        ContentProviderClient contentProviderClient = null;
        try {
            try {
                contentProviderClientAcquireUnstableProviderOrThrow = DocumentsApplication.acquireUnstableProviderOrThrow(this.mResolver, this.mAuthority);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IllegalStateException e) {
            e = e;
        } catch (Exception e2) {
            e = e2;
        }
        try {
            DocumentsContract.ejectRoot(contentProviderClientAcquireUnstableProviderOrThrow, uriBuildRootUri);
            ContentProviderClient.releaseQuietly(contentProviderClientAcquireUnstableProviderOrThrow);
            return true;
        } catch (IllegalStateException e3) {
            e = e3;
            contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
            Log.w("EjectRootTask", "Failed to eject root.", e);
            ContentProviderClient.releaseQuietly(contentProviderClient);
            return false;
        } catch (Exception e4) {
            e = e4;
            contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
            Log.w("EjectRootTask", "Binder call failed.", e);
            ContentProviderClient.releaseQuietly(contentProviderClient);
            return false;
        } catch (Throwable th2) {
            th = th2;
            contentProviderClient = contentProviderClientAcquireUnstableProviderOrThrow;
            ContentProviderClient.releaseQuietly(contentProviderClient);
            throw th;
        }
    }

    @Override
    protected void onPostExecute(Boolean bool) {
        this.mCallback.accept(bool.booleanValue());
    }
}
