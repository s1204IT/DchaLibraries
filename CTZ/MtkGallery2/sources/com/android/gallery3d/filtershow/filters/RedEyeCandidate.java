package com.android.gallery3d.filtershow.filters;

import android.graphics.RectF;

public class RedEyeCandidate implements FilterPoint {
    RectF mRect = new RectF();
    RectF mBounds = new RectF();

    public RedEyeCandidate(RectF rectF, RectF rectF2) {
        this.mRect.set(rectF);
        this.mBounds.set(rectF2);
    }

    public boolean intersect(RectF rectF) {
        return this.mRect.intersect(rectF);
    }

    public RectF getRect() {
        return this.mRect;
    }
}
