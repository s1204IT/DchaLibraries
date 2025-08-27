package com.android.launcher3;

import android.view.KeyEvent;
import android.view.View;

/* compiled from: FocusHelper.java */
/* renamed from: com.android.launcher3.IconKeyEventListener, reason: use source file name */
/* loaded from: classes.dex */
class FocusHelper4 implements View.OnKeyListener {
    FocusHelper4() {
    }

    @Override // android.view.View.OnKeyListener
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        return FocusHelper.handleIconKeyEvent(view, i, keyEvent);
    }
}
