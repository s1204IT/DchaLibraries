package com.android.documentsui.dirlist;

import android.os.Bundle;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.services.FileOperation;

final class DirectoryState {
    private String mConfigKey;
    DocumentInfo mDocument;
    int mLastSortDimensionId = 0;
    int mLastSortDirection;
    FileOperation mPendingOperation;
    private RootInfo mRoot;

    DirectoryState() {
    }

    public void restore(Bundle bundle) {
        this.mRoot = (RootInfo) bundle.getParcelable("root");
        this.mDocument = (DocumentInfo) bundle.getParcelable("document");
        this.mPendingOperation = (FileOperation) bundle.getParcelable("com.android.documentsui.OPERATION");
        this.mLastSortDimensionId = bundle.getInt("sortDimensionId");
        this.mLastSortDirection = bundle.getInt("sortDirection");
    }

    public void save(Bundle bundle) {
        bundle.putParcelable("root", this.mRoot);
        bundle.putParcelable("document", this.mDocument);
        bundle.putParcelable("com.android.documentsui.OPERATION", this.mPendingOperation);
        bundle.putInt("sortDimensionId", this.mLastSortDimensionId);
        bundle.putInt("sortDirection", this.mLastSortDirection);
    }

    public FileOperation claimPendingOperation() {
        FileOperation fileOperation = this.mPendingOperation;
        this.mPendingOperation = null;
        return fileOperation;
    }

    String getConfigKey() {
        if (this.mConfigKey == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.mRoot != null ? this.mRoot.authority : "null");
            sb.append(';');
            sb.append(this.mRoot != null ? this.mRoot.rootId : "null");
            sb.append(';');
            sb.append(this.mDocument != null ? this.mDocument.documentId : "null");
            this.mConfigKey = sb.toString();
        }
        return this.mConfigKey;
    }
}
