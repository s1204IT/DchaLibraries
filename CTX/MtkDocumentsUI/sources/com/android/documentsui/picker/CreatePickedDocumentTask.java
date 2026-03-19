package com.android.documentsui.picker;

import android.app.Activity;
import android.net.Uri;
import com.android.documentsui.DocumentsAccess;
import com.android.documentsui.R;
import com.android.documentsui.base.BooleanConsumer;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.PairedTask;
import com.android.documentsui.ui.Snackbars;
import java.util.function.Consumer;

class CreatePickedDocumentTask extends PairedTask<Activity, Void, Uri> {
    private final Consumer<Uri> mCallback;
    private final String mDisplayName;
    private final DocumentsAccess mDocs;
    private final BooleanConsumer mInProgressStateListener;
    private final LastAccessedStorage mLastAccessed;
    private final String mMimeType;
    private final DocumentStack mStack;

    CreatePickedDocumentTask(Activity activity, DocumentsAccess documentsAccess, LastAccessedStorage lastAccessedStorage, DocumentStack documentStack, String str, String str2, BooleanConsumer booleanConsumer, Consumer<Uri> consumer) {
        super(activity);
        this.mLastAccessed = lastAccessedStorage;
        this.mDocs = documentsAccess;
        this.mStack = documentStack;
        this.mMimeType = str;
        this.mDisplayName = str2;
        this.mInProgressStateListener = booleanConsumer;
        this.mCallback = consumer;
    }

    @Override
    protected void prepare() {
        this.mInProgressStateListener.accept(true);
    }

    @Override
    protected Uri run(Void... voidArr) {
        Uri uriCreateDocument = this.mDocs.createDocument(this.mStack.peek(), this.mMimeType, this.mDisplayName);
        if (uriCreateDocument != null) {
            this.mLastAccessed.setLastAccessed(this.mOwner, this.mStack);
        }
        return uriCreateDocument;
    }

    @Override
    protected void finish(Uri uri) {
        if (uri != null) {
            this.mCallback.accept(uri);
        } else {
            Snackbars.makeSnackbar(this.mOwner, R.string.save_error, -1).show();
        }
        this.mInProgressStateListener.accept(false);
    }
}
