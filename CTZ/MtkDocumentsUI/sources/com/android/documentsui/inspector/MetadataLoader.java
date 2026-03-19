package com.android.documentsui.inspector;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import java.io.FileNotFoundException;

final class MetadataLoader extends AsyncTaskLoader<Bundle> {
    private final Context mContext;
    private Bundle mMetadata;
    private final Uri mUri;

    MetadataLoader(Context context, Uri uri) {
        super(context);
        this.mContext = context;
        this.mUri = uri;
    }

    @Override
    public Bundle loadInBackground() {
        try {
            return DocumentsContract.getDocumentMetadata(this.mContext.getContentResolver(), this.mUri);
        } catch (FileNotFoundException e) {
            Log.e("MetadataLoader", "Failed to load metadata for doc: " + this.mUri, e);
            return null;
        }
    }

    @Override
    protected void onStartLoading() {
        if (this.mMetadata != null) {
            deliverResult(this.mMetadata);
        }
        if (takeContentChanged() || this.mMetadata == null) {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(Bundle bundle) {
        if (isReset()) {
            return;
        }
        this.mMetadata = bundle;
        if (isStarted()) {
            super.deliverResult(bundle);
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        this.mMetadata = null;
    }
}
