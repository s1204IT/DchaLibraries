package com.android.gallery3d.filtershow.filters;

import android.graphics.RectF;

public class RedEyeCandidate implements FilterPoint {
    RectF mRect = new RectF();
    RectF mBounds = new RectF();

    public RedEyeCandidate(RectF rect, RectF bounds) {
        this.mRect.set(rect);
        this.mBounds.set(bounds);
    }

    public boolean intersect(RectF rect) {
        return this.mRect.intersect(rect);
    }

    public RectF getRect() {
        return this.mRect;
    }
}
