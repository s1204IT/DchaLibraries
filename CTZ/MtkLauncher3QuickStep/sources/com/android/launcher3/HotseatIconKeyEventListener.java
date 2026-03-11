package com.android.launcher3;

import android.view.KeyEvent;
import android.view.View;

class HotseatIconKeyEventListener implements View.OnKeyListener {
    HotseatIconKeyEventListener() {
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        return FocusHelper.handleHotseatButtonKeyEvent(view, i, keyEvent);
    }
}
