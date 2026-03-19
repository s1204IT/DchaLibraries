package com.android.server.wm;

import android.graphics.Rect;

public interface PinnedStackWindowListener extends StackWindowListener {
    default void updatePictureInPictureModeForPinnedStackAnimation(Rect rect, boolean z) {
    }
}
