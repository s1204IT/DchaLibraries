package com.android.documentsui.files;

import com.android.documentsui.ActivityConfig;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.RootInfo;

public final class Config extends ActivityConfig {
    @Override
    public boolean managedModeEnabled(DocumentStack documentStack) {
        RootInfo root = documentStack.getRoot();
        return root != null && root.isDownloads() && documentStack.size() == 1;
    }

    @Override
    public boolean dragAndDropEnabled() {
        return true;
    }
}
