package com.android.documentsui.files;

import com.android.documentsui.services.FileOperations;
import com.android.documentsui.ui.DialogController;

public final class $$Lambda$n6sAx1oP7V7e9pd6SZfCi6Mvs implements FileOperations.Callback {
    private final DialogController f$0;

    @Override
    public final void onOperationResult(int i, int i2, int i3) {
        this.f$0.showFileOperationStatus(i, i2, i3);
    }
}
