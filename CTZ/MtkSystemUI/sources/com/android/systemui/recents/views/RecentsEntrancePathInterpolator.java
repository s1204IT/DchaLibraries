package com.android.systemui.recents.views;

import android.view.animation.PathInterpolator;

public class RecentsEntrancePathInterpolator extends PathInterpolator {
    final float mStartOffsetFraction;

    public RecentsEntrancePathInterpolator(float f, float f2, float f3, float f4, float f5) {
        super(f, f2, f3, f4);
        this.mStartOffsetFraction = f5;
    }

    @Override
    public float getInterpolation(float f) {
        return super.getInterpolation(f + this.mStartOffsetFraction);
    }
}
