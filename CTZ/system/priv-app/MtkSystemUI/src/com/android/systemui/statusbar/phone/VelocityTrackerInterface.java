package com.android.systemui.statusbar.phone;

import android.view.MotionEvent;

/* loaded from: classes.dex */
public interface VelocityTrackerInterface {
    void addMovement(MotionEvent motionEvent);

    void computeCurrentVelocity(int i);

    float getXVelocity();

    float getYVelocity();

    void recycle();
}
