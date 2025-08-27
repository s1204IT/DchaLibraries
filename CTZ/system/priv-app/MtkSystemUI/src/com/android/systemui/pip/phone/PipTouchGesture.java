package com.android.systemui.pip.phone;

/* loaded from: classes.dex */
public abstract class PipTouchGesture {
    void onDown(PipTouchState pipTouchState) {
    }

    boolean onMove(PipTouchState pipTouchState) {
        return false;
    }

    boolean onUp(PipTouchState pipTouchState) {
        return false;
    }
}
