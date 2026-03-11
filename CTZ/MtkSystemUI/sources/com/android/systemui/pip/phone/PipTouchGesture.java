package com.android.systemui.pip.phone;

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
