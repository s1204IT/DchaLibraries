package com.android.documentsui.services;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.android.documentsui.Metrics;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.clipping.UrisSupplier;
import com.android.documentsui.services.Job;
import java.io.FileNotFoundException;

final class DeleteJob extends ResolvedResourcesJob {
    private volatile int mDocsProcessed;
    private final Uri mParentUri;

    DeleteJob(Context context, Job.Listener listener, String str, DocumentStack documentStack, UrisSupplier urisSupplier, Uri uri, Features features) {
        super(context, listener, str, 5, documentStack, urisSupplier, features);
        this.mDocsProcessed = 0;
        this.mParentUri = uri;
    }

    @Override
    Notification.Builder createProgressBuilder() {
        return super.createProgressBuilder(this.service.getString(R.string.delete_notification_title), R.drawable.ic_menu_delete, this.service.getString(android.R.string.cancel), R.drawable.ic_cab_cancel);
    }

    @Override
    public Notification getSetupNotification() {
        return getSetupNotification(this.service.getString(R.string.delete_preparing));
    }

    @Override
    public Notification getProgressNotification() {
        this.mProgressBuilder.setProgress(this.mResourceUris.getItemCount(), this.mDocsProcessed, false);
        this.mProgressBuilder.setSubText(String.format(this.service.getString(R.string.delete_progress), Integer.valueOf(this.mDocsProcessed), Integer.valueOf(this.mResourceUris.getItemCount())));
        this.mProgressBuilder.setContentText(null);
        return this.mProgressBuilder.build();
    }

    @Override
    Notification getFailureNotification() {
        return getFailureNotification(R.plurals.delete_error_notification_title, R.drawable.ic_menu_delete);
    }

    @Override
    Notification getWarningNotification() {
        throw new UnsupportedOperationException();
    }

    @Override
    void start() throws Throwable {
        DocumentInfo documentInfoFromUri;
        ContentResolver contentResolver = this.appContext.getContentResolver();
        try {
            if (this.mParentUri != null) {
                documentInfoFromUri = DocumentInfo.fromUri(contentResolver, this.mParentUri);
            } else {
                documentInfoFromUri = null;
            }
            for (DocumentInfo documentInfo : this.mResolvedDocs) {
                if (SharedMinimal.DEBUG) {
                    Log.d("DeleteJob", "Deleting document @ " + documentInfo.derivedUri);
                }
                try {
                    deleteDocument(documentInfo, documentInfoFromUri);
                } catch (ResourceException e) {
                    Metrics.logFileOperationFailure(this.appContext, 7, documentInfo.derivedUri);
                    if (SharedMinimal.DEBUG) {
                        Log.e("DeleteJob", "Failed to delete document @ " + documentInfo.derivedUri, e);
                    }
                    onFileFailed(documentInfo);
                }
                this.mDocsProcessed++;
                if (isCanceled()) {
                    return;
                }
            }
            Metrics.logFileOperation(this.service, this.operationType, this.mResolvedDocs, null);
        } catch (FileNotFoundException e2) {
            Log.e("DeleteJob", "Failed to resolve parent from Uri: " + this.mParentUri + ". Cannot continue.", e2);
            this.failureCount = this.failureCount + this.mResourceUris.getItemCount();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DeleteJob");
        sb.append("{");
        sb.append("id=" + this.id);
        sb.append(", uris=" + this.mResourceUris);
        sb.append(", docs=" + this.mResolvedDocs);
        sb.append(", srcParent=" + this.mParentUri);
        sb.append(", location=" + this.stack);
        sb.append("}");
        return sb.toString();
    }
}
