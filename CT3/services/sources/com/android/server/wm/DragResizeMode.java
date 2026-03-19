package com.android.server.wm;

class DragResizeMode {
    static final int DRAG_RESIZE_MODE_DOCKED_DIVIDER = 1;
    static final int DRAG_RESIZE_MODE_FREEFORM = 0;

    DragResizeMode() {
    }

    static boolean isModeAllowedForStack(int stackId, int mode) {
        switch (mode) {
            case 0:
                return stackId == 2;
            case 1:
                return stackId == 3 || stackId == 1 || stackId == 0;
            default:
                return false;
        }
    }
}
