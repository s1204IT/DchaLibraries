package com.android.documentsui.selection;

import android.graphics.Point;
import android.view.MotionEvent;

final class MotionEvents {
    static boolean isMouseEvent(MotionEvent motionEvent) {
        return motionEvent.getToolType(0) == 3;
    }

    static boolean isActionMove(MotionEvent motionEvent) {
        return motionEvent.getActionMasked() == 2;
    }

    static boolean isActionUp(MotionEvent motionEvent) {
        return motionEvent.getActionMasked() == 1;
    }

    static boolean isActionPointerUp(MotionEvent motionEvent) {
        return motionEvent.getActionMasked() == 6;
    }

    static boolean isActionCancel(MotionEvent motionEvent) {
        return motionEvent.getActionMasked() == 3;
    }

    static Point getOrigin(MotionEvent motionEvent) {
        return new Point((int) motionEvent.getX(), (int) motionEvent.getY());
    }

    static boolean isPrimaryButtonPressed(MotionEvent motionEvent) {
        return motionEvent.isButtonPressed(1);
    }

    public static boolean isSecondaryButtonPressed(MotionEvent motionEvent) {
        return motionEvent.isButtonPressed(2);
    }

    public static boolean isTertiaryButtonPressed(MotionEvent motionEvent) {
        return motionEvent.isButtonPressed(4);
    }

    static boolean isShiftKeyPressed(MotionEvent motionEvent) {
        return hasBit(motionEvent.getMetaState(), 1);
    }

    static boolean isCtrlKeyPressed(MotionEvent motionEvent) {
        return hasBit(motionEvent.getMetaState(), 4096);
    }

    static boolean isAltKeyPressed(MotionEvent motionEvent) {
        return hasBit(motionEvent.getMetaState(), 2);
    }

    public static boolean isTouchpadScroll(MotionEvent motionEvent) {
        return isMouseEvent(motionEvent) && isActionMove(motionEvent) && motionEvent.getButtonState() == 0;
    }

    private static boolean hasBit(int i, int i2) {
        return (i & i2) != 0;
    }
}
