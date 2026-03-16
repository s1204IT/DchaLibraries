package com.android.gallery3d.ui;

import android.content.Context;
import android.view.ViewConfiguration;
import com.android.gallery3d.common.OverScroller;
import com.android.gallery3d.common.Utils;

public class ScrollerHelper {
    private int mOverflingDistance;
    private boolean mOverflingEnabled;
    private OverScroller mScroller;

    public ScrollerHelper(Context context) {
        this.mScroller = new OverScroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mOverflingDistance = configuration.getScaledOverflingDistance();
    }

    public boolean advanceAnimation(long currentTimeMillis) {
        return this.mScroller.computeScrollOffset();
    }

    public boolean isFinished() {
        return this.mScroller.isFinished();
    }

    public void forceFinished() {
        this.mScroller.forceFinished(true);
    }

    public int getPosition() {
        return this.mScroller.getCurrX();
    }

    public float getCurrVelocity() {
        return this.mScroller.getCurrVelocity();
    }

    public void setPosition(int position) {
        this.mScroller.startScroll(position, 0, 0, 0, 0);
        this.mScroller.abortAnimation();
    }

    public void fling(int velocity, int min, int max) {
        int currX = getPosition();
        this.mScroller.fling(currX, 0, velocity, 0, min, max, 0, 0, this.mOverflingEnabled ? this.mOverflingDistance : 0, 0);
    }

    public int startScroll(int distance, int min, int max) {
        int currPosition = this.mScroller.getCurrX();
        int finalPosition = this.mScroller.isFinished() ? currPosition : this.mScroller.getFinalX();
        int newPosition = Utils.clamp(finalPosition + distance, min, max);
        if (newPosition != currPosition) {
            this.mScroller.startScroll(currPosition, 0, newPosition - currPosition, 0, 0);
        }
        return (finalPosition + distance) - newPosition;
    }
}
