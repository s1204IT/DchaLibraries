package com.android.gallery3d.ui;

import android.content.Context;
import android.view.ViewConfiguration;
import com.android.gallery3d.common.OverScroller;
import com.android.gallery3d.common.Utils;

public class ScrollerHelper {
    private int mOverflingDistance;
    private boolean mOverflingEnabled;
    private int mScrollMax = -1;
    private int mScrollMin = -1;
    private OverScroller mScroller;

    public ScrollerHelper(Context context) {
        this.mScroller = new OverScroller(context);
        this.mOverflingDistance = ViewConfiguration.get(context).getScaledOverflingDistance();
    }

    public void setOverfling(boolean z) {
        this.mOverflingEnabled = z;
    }

    public boolean advanceAnimation(long j) {
        return this.mScroller.computeScrollOffset();
    }

    public boolean isFinished() {
        return this.mScroller.isFinished();
    }

    public void forceFinished() {
        this.mScroller.forceFinished(true);
    }

    public int getPosition() {
        if (this.mScrollMax != -1 && this.mScrollMin != -1) {
            return Utils.clamp(this.mScroller.getCurrX(), this.mScrollMin, this.mScrollMax);
        }
        return this.mScroller.getCurrX();
    }

    public float getCurrVelocity() {
        return this.mScroller.getCurrVelocity();
    }

    public void setPosition(int i) {
        this.mScroller.startScroll(i, 0, 0, 0, 0);
        this.mScroller.abortAnimation();
    }

    public void fling(int i, int i2, int i3) {
        this.mScrollMax = i3;
        this.mScrollMin = i2;
        this.mScroller.setMaxScrollLength(i3);
        this.mScroller.setMinScrollLength(i2);
        this.mScroller.fling(getPosition(), 0, i, 0, i2, i3, 0, 0, this.mOverflingEnabled ? this.mOverflingDistance : 0, 0);
    }

    public int startScroll(int i, int i2, int i3) {
        int finalX;
        this.mScrollMax = i3;
        this.mScrollMin = i2;
        this.mScroller.setMaxScrollLength(i3);
        this.mScroller.setMinScrollLength(i2);
        int currX = this.mScroller.getCurrX();
        if (!this.mScroller.isFinished()) {
            finalX = this.mScroller.getFinalX();
        } else {
            finalX = currX;
        }
        int i4 = finalX + i;
        int iClamp = Utils.clamp(i4, i2, i3);
        if (iClamp != currX) {
            this.mScroller.startScroll(currX, 0, iClamp - currX, 0, 0);
        }
        return i4 - iClamp;
    }
}
