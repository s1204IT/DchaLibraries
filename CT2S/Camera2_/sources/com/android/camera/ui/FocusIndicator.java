package com.android.camera.ui;

public interface FocusIndicator {
    void clear();

    void showFail(boolean z);

    void showStart();

    void showSuccess(boolean z);
}
