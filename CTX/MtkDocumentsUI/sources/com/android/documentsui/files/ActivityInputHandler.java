package com.android.documentsui.files;

import android.view.KeyEvent;

final class ActivityInputHandler {
    private final Runnable mDeleteHandler;

    ActivityInputHandler(Runnable runnable) {
        this.mDeleteHandler = runnable;
    }

    boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i != 67) {
            if (i != 112) {
                return false;
            }
            this.mDeleteHandler.run();
            return true;
        }
        if (!keyEvent.isAltPressed()) {
            return false;
        }
        this.mDeleteHandler.run();
        return true;
    }
}
