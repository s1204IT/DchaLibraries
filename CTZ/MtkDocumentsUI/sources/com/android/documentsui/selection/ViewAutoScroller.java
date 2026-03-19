package com.android.documentsui.selection;

import android.graphics.Point;

public final class ViewAutoScroller implements Runnable {
    static final boolean $assertionsDisabled = false;
    private ScrollerCallbacks mCallbacks;
    private ScrollHost mHost;

    public static abstract class ScrollHost {
        public abstract Point getCurrentPosition();

        public abstract int getViewHeight();

        public abstract boolean isActive();
    }

    public ViewAutoScroller(ScrollHost scrollHost, ScrollerCallbacks scrollerCallbacks) {
        this.mHost = scrollHost;
        this.mCallbacks = scrollerCallbacks;
    }

    @Override
    public void run() {
        int viewHeight;
        int viewHeight2 = (int) (this.mHost.getViewHeight() * 0.125f);
        if (this.mHost.getCurrentPosition().y <= viewHeight2) {
            viewHeight = this.mHost.getCurrentPosition().y - viewHeight2;
        } else if (this.mHost.getCurrentPosition().y >= this.mHost.getViewHeight() - viewHeight2) {
            viewHeight = (this.mHost.getCurrentPosition().y - this.mHost.getViewHeight()) + viewHeight2;
        } else {
            viewHeight = 0;
        }
        if (!this.mHost.isActive() || viewHeight == 0) {
            return;
        }
        if (viewHeight <= viewHeight2) {
            viewHeight2 = viewHeight;
        }
        this.mCallbacks.scrollBy(computeScrollDistance(viewHeight2));
        this.mCallbacks.removeCallback(this);
        this.mCallbacks.runAtNextFrame(this);
    }

    public int computeScrollDistance(int i) {
        int iSignum = (int) Math.signum(i);
        int iSmoothOutOfBoundsRatio = (int) (iSignum * 70 * smoothOutOfBoundsRatio(Math.min(1.0f, Math.abs(i) / ((int) (this.mHost.getViewHeight() * 0.125f)))));
        return iSmoothOutOfBoundsRatio != 0 ? iSmoothOutOfBoundsRatio : iSignum;
    }

    private float smoothOutOfBoundsRatio(float f) {
        return (float) Math.pow(f, 10.0d);
    }

    public static abstract class ScrollerCallbacks {
        public void scrollBy(int i) {
        }

        public void runAtNextFrame(Runnable runnable) {
        }

        public void removeCallback(Runnable runnable) {
        }
    }
}
