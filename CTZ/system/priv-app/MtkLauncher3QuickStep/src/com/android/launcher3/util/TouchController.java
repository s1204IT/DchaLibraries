package com.android.launcher3.util;

import android.view.MotionEvent;

/* loaded from: classes.dex */
public interface TouchController {
    boolean onControllerInterceptTouchEvent(MotionEvent motionEvent);

    boolean onControllerTouchEvent(MotionEvent motionEvent);
}
