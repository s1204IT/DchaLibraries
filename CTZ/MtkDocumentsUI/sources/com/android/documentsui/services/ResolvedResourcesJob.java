package com.android.documentsui.services;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import com.android.documentsui.archives.ArchivesProvider;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.clipping.UrisSupplier;
import com.android.documentsui.services.Job;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ResolvedResourcesJob extends Job {
    static final boolean $assertionsDisabled = false;
    final List<Uri> mAcquiredArchivedUris;
    final List<DocumentInfo> mResolvedDocs;

    ResolvedResourcesJob(Context context, Job.Listener listener, String str, int i, DocumentStack documentStack, UrisSupplier urisSupplier, Features features) {
        super(context, listener, str, i, documentStack, urisSupplier, features);
        this.mAcquiredArchivedUris = new ArrayList();
        this.mResolvedDocs = new ArrayList(urisSupplier.getItemCount());
    }

    @Override
    boolean setUp() throws Throwable {
        if (!super.setUp()) {
            return false;
        }
        try {
            for (Uri uri : this.mResourceUris.getUris(this.appContext)) {
                try {
                    if ("com.android.documentsui.archives".equals(uri.getAuthority())) {
                        ArchivesProvider.acquireArchive(getClient(uri), uri);
                        this.mAcquiredArchivedUris.add(uri);
                    }
                } catch (RemoteException e) {
                    Log.e("ResolvedResourcesJob", "Failed to acquire an archive.");
                    return false;
                }
            }
            int iBuildDocumentList = buildDocumentList();
            if (!isCanceled() && iBuildDocumentList < this.mResourceUris.getItemCount()) {
                if (iBuildDocumentList == 0) {
                    Log.e("ResolvedResourcesJob", "Failed to load any documents. Aborting.");
                    return false;
                }
                Log.e("ResolvedResourcesJob", "Failed to load some documents. Processing loaded documents only.");
                return true;
            }
            return true;
        } catch (IOException e2) {
            Log.e("ResolvedResourcesJob", "Failed to read list of target resource Uris. Cannot continue.", e2);
            return false;
        }
    }

    @Override
    void finish() {
        for (Uri uri : this.mAcquiredArchivedUris) {
            try {
                ArchivesProvider.releaseArchive(getClient(uri), uri);
            } catch (RemoteException e) {
                Log.e("ResolvedResourcesJob", "Failed to release an archived document.");
            }
        }
    }

    boolean isEligibleDoc(DocumentInfo documentInfo, RootInfo rootInfo) {
        return true;
    }

    protected int buildDocumentList() throws Throwable {
        ContentResolver contentResolver = this.appContext.getContentResolver();
        int i = 0;
        try {
            for (Uri uri : this.mResourceUris.getUris(this.appContext)) {
                try {
                    DocumentInfo documentInfoFromUri = DocumentInfo.fromUri(contentResolver, uri);
                    if (isEligibleDoc(documentInfoFromUri, this.stack.getRoot())) {
                        this.mResolvedDocs.add(documentInfoFromUri);
                    } else {
                        onFileFailed(documentInfoFromUri);
                    }
                    i++;
                } catch (FileNotFoundException e) {
                    Log.e("ResolvedResourcesJob", "Failed to resolve content from Uri: " + uri + ". Skipping to next resource.", e);
                    onResolveFailed(uri);
                }
                if (isCanceled()) {
                    break;
                }
            }
            return i;
        } catch (IOException e2) {
            Log.e("ResolvedResourcesJob", "Failed to read list of target resource Uris. Cannot continue.", e2);
            this.failureCount = this.mResourceUris.getItemCount();
            return 0;
        }
    }
}
