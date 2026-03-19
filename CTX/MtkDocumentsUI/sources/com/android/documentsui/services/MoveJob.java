package com.android.documentsui.services;

import android.app.Notification;
import android.content.Context;
import android.net.Uri;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.DocumentsContract;
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

final class MoveJob extends CopyJob {
    private DocumentInfo mSrcParent;
    private final Uri mSrcParentUri;

    MoveJob(Context context, Job.Listener listener, String str, DocumentStack documentStack, UrisSupplier urisSupplier, Uri uri, Messenger messenger, Features features) {
        super(context, listener, str, 4, documentStack, urisSupplier, messenger, features);
        this.mSrcParentUri = uri;
    }

    @Override
    Notification.Builder createProgressBuilder() {
        return super.createProgressBuilder(this.service.getString(R.string.move_notification_title), R.drawable.ic_menu_copy, this.service.getString(android.R.string.cancel), R.drawable.ic_cab_cancel);
    }

    @Override
    public Notification getSetupNotification() {
        return getSetupNotification(this.service.getString(R.string.move_preparing));
    }

    @Override
    public Notification getProgressNotification() {
        return getProgressNotification(R.string.copy_remaining);
    }

    @Override
    Notification getFailureNotification() {
        return getFailureNotification(R.plurals.move_error_notification_title, R.drawable.ic_menu_copy);
    }

    @Override
    public boolean setUp() {
        if (this.mSrcParentUri != null) {
            try {
                this.mSrcParent = DocumentInfo.fromUri(this.appContext.getContentResolver(), this.mSrcParentUri);
            } catch (FileNotFoundException e) {
                Log.e("MoveJob", "Failed to create srcParent.", e);
                this.failureCount = this.mResourceUris.getItemCount();
                return false;
            }
        }
        return super.setUp();
    }

    @Override
    boolean checkSpace() {
        long jCalculateFileSizesRecursively = 0;
        for (DocumentInfo documentInfo : this.mResolvedDocs) {
            if (!documentInfo.authority.equals(this.stack.getRoot().authority)) {
                if (documentInfo.isDirectory()) {
                    try {
                        jCalculateFileSizesRecursively += calculateFileSizesRecursively(getClient(documentInfo), documentInfo.derivedUri);
                    } catch (RemoteException | ResourceException e) {
                        Log.w("MoveJob", "Failed to obtain client for %s" + documentInfo.derivedUri + ".", e);
                        return true;
                    }
                } else {
                    jCalculateFileSizesRecursively += documentInfo.size;
                }
            }
        }
        return verifySpaceAvailable(jCalculateFileSizesRecursively);
    }

    @Override
    void processDocument(DocumentInfo documentInfo, DocumentInfo documentInfo2, DocumentInfo documentInfo3) throws ResourceException {
        if (documentInfo.authority.equals(documentInfo3.authority) && ((documentInfo2 != null || this.mSrcParent != null) && (documentInfo.flags & 256) != 0)) {
            try {
                if (DocumentsContract.moveDocument(getClient(documentInfo), documentInfo.derivedUri, documentInfo2 != null ? documentInfo2.derivedUri : this.mSrcParent.derivedUri, documentInfo3.derivedUri) != null) {
                    Metrics.logFileOperated(this.appContext, this.operationType, 1);
                    return;
                }
            } catch (RemoteException | RuntimeException e) {
                Metrics.logFileOperationFailure(this.appContext, 9, documentInfo.derivedUri);
                Log.e("MoveJob", "Provider side move failed for: " + documentInfo.derivedUri + " due to an exception: ", e);
            }
            if (SharedMinimal.DEBUG) {
                Log.d("MoveJob", "Fallback to byte-by-byte move for: " + documentInfo.derivedUri);
            }
        }
        if (documentInfo.isVirtual()) {
            throw new ResourceException("Cannot move virtual file %s byte by byte.", documentInfo.derivedUri);
        }
        byteCopyDocument(documentInfo, documentInfo3);
        if (!isCanceled()) {
            deleteDocument(documentInfo, documentInfo2);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MoveJob");
        sb.append("{");
        sb.append("id=" + this.id);
        sb.append(", uris=" + this.mResourceUris);
        sb.append(", docs=" + this.mResolvedDocs);
        sb.append(", srcParent=" + this.mSrcParent);
        sb.append(", destination=" + this.stack);
        sb.append("}");
        return sb.toString();
    }
}
