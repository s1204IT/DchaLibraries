package com.android.documentsui;

import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.State;

public abstract class ActivityConfig {
    public boolean canSelectType(String str, int i, State state) {
        return true;
    }

    public boolean isDocumentEnabled(String str, int i, State state) {
        return true;
    }

    public boolean managedModeEnabled(DocumentStack documentStack) {
        return false;
    }

    public boolean dragAndDropEnabled() {
        return false;
    }
}
