package com.android.documentsui;

import android.app.Activity;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.PairedTask;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.roots.ProvidersAccess;

public class LoadDocStackTask extends PairedTask<Activity, Uri, DocumentStack> {
    private final LoadDocStackCallback mCallback;
    private final DocumentsAccess mDocs;
    private final ProvidersAccess mProviders;

    @FunctionalInterface
    public interface LoadDocStackCallback {
        void onDocumentStackLoaded(DocumentStack documentStack);
    }

    public LoadDocStackTask(Activity activity, ProvidersAccess providersAccess, DocumentsAccess documentsAccess, LoadDocStackCallback loadDocStackCallback) {
        super(activity);
        this.mProviders = providersAccess;
        this.mDocs = documentsAccess;
        this.mCallback = loadDocStackCallback;
    }

    @Override
    public DocumentStack run(Uri... uriArr) {
        Uri uriBuildDocumentUri;
        if (this.mDocs.isDocumentUri(uriArr[0])) {
            if (DocumentsContract.isTreeUri(uriArr[0])) {
                uriBuildDocumentUri = DocumentsContract.buildDocumentUri(uriArr[0].getAuthority(), DocumentsContract.getDocumentId(uriArr[0]));
            } else {
                uriBuildDocumentUri = uriArr[0];
            }
            try {
                DocumentsContract.Path pathFindDocumentPath = this.mDocs.findDocumentPath(uriBuildDocumentUri);
                if (pathFindDocumentPath != null) {
                    return buildStack(uriBuildDocumentUri.getAuthority(), pathFindDocumentPath);
                }
                Log.i("LoadDocStackTask", "Remote provider doesn't support findDocumentPath.");
                return null;
            } catch (Exception e) {
                Log.e("LoadDocStackTask", "Failed to build document stack for uri: " + uriBuildDocumentUri, e);
                return null;
            }
        }
        return null;
    }

    @Override
    public void finish(DocumentStack documentStack) {
        this.mCallback.onDocumentStackLoaded(documentStack);
    }

    private DocumentStack buildStack(String str, DocumentsContract.Path path) throws Exception {
        if (path.getRootId() == null) {
            throw new IllegalStateException("Provider doesn't provider root id.");
        }
        RootInfo rootOneshot = this.mProviders.getRootOneshot(str, path.getRootId());
        if (rootOneshot == null) {
            throw new IllegalStateException("Failed to load root for authority: " + str + " and root ID: " + path.getRootId() + ".");
        }
        return new DocumentStack(rootOneshot, this.mDocs.getDocuments(str, path.getPath()));
    }
}
