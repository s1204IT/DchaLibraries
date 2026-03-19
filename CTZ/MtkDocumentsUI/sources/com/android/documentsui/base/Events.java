package com.android.documentsui.base;

import android.view.MotionEvent;

public final class Events {
    public static boolean isMouseEvent(MotionEvent motionEvent) {
        return motionEvent.getToolType(0) == 3;
    }

    public static boolean isActionMove(MotionEvent motionEvent) {
        return motionEvent.getActionMasked() == 2;
    }

    public static boolean isActionDown(MotionEvent motionEvent) {
        return motionEvent.getActionMasked() == 0;
    }

    public static boolean isActionUp(MotionEvent motionEvent) {
        return motionEvent.getActionMasked() == 1;
    }

    public static boolean isPrimaryButtonPressed(MotionEvent motionEvent) {
        return motionEvent.isButtonPressed(1);
    }

    public static boolean isCtrlKeyPressed(MotionEvent motionEvent) {
        return hasBit(motionEvent.getMetaState(), 4096);
    }

    private static boolean hasBit(int i, int i2) {
        return (i & i2) != 0;
    }

    public static boolean isNavigationKeyCode(int i) {
        switch (i) {
            case 19:
            case 20:
            case 21:
            case 22:
            case 92:
            case 93:
            case 122:
            case 123:
                return true;
            default:
                return false;
        }
    }

    public static boolean isMouseDragEvent(MotionEvent motionEvent) {
        return isMouseEvent(motionEvent) && isActionMove(motionEvent) && isPrimaryButtonPressed(motionEvent);
    }
}
