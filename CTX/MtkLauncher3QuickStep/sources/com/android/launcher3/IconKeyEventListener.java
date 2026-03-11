package com.android.launcher3;

import android.view.KeyEvent;
import android.view.View;

class IconKeyEventListener implements View.OnKeyListener {
    IconKeyEventListener() {
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        return FocusHelper.handleIconKeyEvent(view, i, keyEvent);
    }
}
