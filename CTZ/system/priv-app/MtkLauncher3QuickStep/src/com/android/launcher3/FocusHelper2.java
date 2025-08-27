package com.android.launcher3;

import android.view.KeyEvent;
import android.view.View;

/* compiled from: FocusHelper.java */
/* renamed from: com.android.launcher3.FullscreenKeyEventListener, reason: use source file name */
/* loaded from: classes.dex */
class FocusHelper2 implements View.OnKeyListener {
    FocusHelper2() {
    }

    @Override // android.view.View.OnKeyListener
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (i == 21 || i == 22 || i == 93 || i == 92) {
            return FocusHelper.handleIconKeyEvent(view, i, keyEvent);
        }
        return false;
    }
}
