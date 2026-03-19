package com.android.documentsui.services;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.R;
import com.android.documentsui.archives.ArchivesProvider;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.clipping.UrisSupplier;
import com.android.documentsui.services.Job;
import java.io.FileNotFoundException;

final class CompressJob extends CopyJob {
    CompressJob(Context context, Job.Listener listener, String str, DocumentStack documentStack, UrisSupplier urisSupplier, Messenger messenger, Features features) {
        super(context, listener, str, 4, documentStack, urisSupplier, messenger, features);
    }

    @Override
    Notification.Builder createProgressBuilder() {
        return super.createProgressBuilder(this.service.getString(R.string.compress_notification_title), R.drawable.ic_menu_compress, this.service.getString(android.R.string.cancel), R.drawable.ic_cab_cancel);
    }

    @Override
    public Notification getSetupNotification() {
        return getSetupNotification(this.service.getString(R.string.compress_preparing));
    }

    @Override
    public Notification getProgressNotification() {
        return getProgressNotification(R.string.copy_remaining);
    }

    @Override
    Notification getFailureNotification() {
        return getFailureNotification(R.plurals.compress_error_notification_title, R.drawable.ic_menu_compress);
    }

    @Override
    public boolean setUp() {
        String string;
        Uri uriCreateDocument;
        if (!super.setUp()) {
            return false;
        }
        ContentResolver contentResolver = this.appContext.getContentResolver();
        if (this.mResolvedDocs.size() == 1) {
            string = this.mResolvedDocs.get(0).displayName + ".zip";
        } else {
            string = this.service.getString(R.string.new_archive_file_name, ".zip");
        }
        try {
            uriCreateDocument = DocumentsContract.createDocument(contentResolver, this.mDstInfo.derivedUri, "application/zip", string);
        } catch (Exception e) {
            uriCreateDocument = null;
        }
        try {
            this.mDstInfo = DocumentInfo.fromUri(contentResolver, ArchivesProvider.buildUriForArchive(uriCreateDocument, 536870912));
            ArchivesProvider.acquireArchive(getClient(this.mDstInfo), this.mDstInfo.derivedUri);
            return true;
        } catch (RemoteException e2) {
            Log.e("CompressJob", "Failed to acquire the archive.", e2);
            this.failureCount = this.mResourceUris.getItemCount();
            return false;
        } catch (FileNotFoundException e3) {
            Log.e("CompressJob", "Failed to create dstInfo.", e3);
            this.failureCount = this.mResourceUris.getItemCount();
            return false;
        }
    }

    @Override
    void finish() {
        try {
            ArchivesProvider.releaseArchive(getClient(this.mDstInfo), this.mDstInfo.derivedUri);
        } catch (RemoteException e) {
            Log.e("CompressJob", "Failed to release the archive.");
        }
        super.finish();
    }

    @Override
    boolean checkSpace() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CompressJob");
        sb.append("{");
        sb.append("id=" + this.id);
        sb.append(", uris=" + this.mResourceUris);
        sb.append(", docs=" + this.mResolvedDocs);
        sb.append(", destination=" + this.stack);
        sb.append("}");
        return sb.toString();
    }
}
