package com.android.server.wm;

class DragResizeMode {
    static final int DRAG_RESIZE_MODE_DOCKED_DIVIDER = 1;
    static final int DRAG_RESIZE_MODE_FREEFORM = 0;

    DragResizeMode() {
    }

    static boolean isModeAllowedForStack(TaskStack taskStack, int i) {
        switch (i) {
            case 0:
                return taskStack.getWindowingMode() == 5;
            case 1:
                return taskStack.inSplitScreenWindowingMode();
            default:
                return false;
        }
    }
}
