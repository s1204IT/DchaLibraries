package com.android.documentsui.roots;

import android.app.Activity;
import android.util.Log;
import com.android.documentsui.DocumentsAccess;
import com.android.documentsui.TimeoutTask;
import com.android.documentsui.base.CheckedTask;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.RootInfo;
import java.util.Objects;
import java.util.function.Consumer;

public class GetRootDocumentTask extends TimeoutTask<Void, DocumentInfo> {
    private final Consumer<DocumentInfo> mCallback;
    private final DocumentsAccess mDocs;
    private final RootInfo mRootInfo;

    public GetRootDocumentTask(RootInfo rootInfo, final Activity activity, long j, DocumentsAccess documentsAccess, Consumer<DocumentInfo> consumer) {
        super(new CheckedTask.Check() {
            @Override
            public final boolean stop() {
                return activity.isDestroyed();
            }
        }, j);
        Objects.requireNonNull(activity);
        this.mRootInfo = rootInfo;
        this.mDocs = documentsAccess;
        this.mCallback = consumer;
    }

    @Override
    public DocumentInfo run(Void... voidArr) {
        return this.mDocs.getRootDocument(this.mRootInfo);
    }

    @Override
    public void finish(DocumentInfo documentInfo) {
        if (documentInfo == null) {
            Log.e("GetRootDocumentTask", "Cannot find document info for root: " + this.mRootInfo);
        }
        this.mCallback.accept(documentInfo);
    }
}
