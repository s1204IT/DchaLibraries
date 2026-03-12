package com.android.systemui.statusbar;

import android.content.res.Resources;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import com.android.systemui.R;

public class DelegateViewHelper {
    private BaseStatusBar mBar;
    private View mDelegateView;
    private boolean mDisabled;
    private boolean mPanelShowing;
    private View mSourceView;
    private boolean mStarted;
    private float mTriggerThreshhold;
    private int[] mTempPoint = new int[2];
    private float[] mDownPoint = new float[2];
    RectF mInitialTouch = new RectF();
    private boolean mSwapXY = false;

    public DelegateViewHelper(View sourceView) {
        setSourceView(sourceView);
    }

    public void setDelegateView(View view) {
        this.mDelegateView = view;
    }

    public void setBar(BaseStatusBar phoneStatusBar) {
        this.mBar = phoneStatusBar;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mSourceView == null || this.mDelegateView == null || this.mBar.shouldDisableNavbarGestures()) {
            return false;
        }
        this.mSourceView.getLocationOnScreen(this.mTempPoint);
        float sourceX = this.mTempPoint[0];
        float sourceY = this.mTempPoint[1];
        int action = event.getAction();
        switch (action) {
            case 0:
                this.mPanelShowing = this.mDelegateView.getVisibility() == 0;
                this.mDownPoint[0] = event.getX();
                this.mDownPoint[1] = event.getY();
                this.mStarted = this.mInitialTouch.contains(this.mDownPoint[0] + sourceX, this.mDownPoint[1] + sourceY);
                break;
        }
        if (!this.mStarted) {
            return false;
        }
        if (!this.mDisabled && !this.mPanelShowing && action == 2) {
            int historySize = event.getHistorySize();
            int k = 0;
            while (true) {
                if (k < historySize + 1) {
                    float x = k < historySize ? event.getHistoricalX(k) : event.getX();
                    float y = k < historySize ? event.getHistoricalY(k) : event.getY();
                    float distance = this.mSwapXY ? this.mDownPoint[0] - x : this.mDownPoint[1] - y;
                    if (distance <= this.mTriggerThreshhold) {
                        k++;
                    } else {
                        this.mBar.showSearchPanel();
                        this.mPanelShowing = true;
                    }
                }
            }
        }
        if (action == 0) {
            this.mBar.setInteracting(2, true);
        } else if (action == 1 || action == 3) {
            this.mBar.setInteracting(2, false);
        }
        this.mDelegateView.getLocationOnScreen(this.mTempPoint);
        float delegateX = this.mTempPoint[0];
        float delegateY = this.mTempPoint[1];
        float deltaX = sourceX - delegateX;
        float deltaY = sourceY - delegateY;
        event.offsetLocation(deltaX, deltaY);
        this.mDelegateView.dispatchTouchEvent(event);
        event.offsetLocation(-deltaX, -deltaY);
        return this.mPanelShowing;
    }

    public void setSourceView(View view) {
        this.mSourceView = view;
        if (this.mSourceView != null) {
            Resources r = this.mSourceView.getContext().getResources();
            this.mTriggerThreshhold = r.getDimensionPixelSize(R.dimen.navigation_bar_min_swipe_distance);
        }
    }

    public void setInitialTouchRegion(View... views) {
        RectF bounds = new RectF();
        int[] p = new int[2];
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            if (view != null) {
                view.getLocationOnScreen(p);
                if (i == 0) {
                    bounds.set(p[0], p[1], p[0] + view.getWidth(), p[1] + view.getHeight());
                } else {
                    bounds.union(p[0], p[1], p[0] + view.getWidth(), p[1] + view.getHeight());
                }
            }
        }
        this.mInitialTouch.set(bounds);
    }

    public void setSwapXY(boolean swap) {
        this.mSwapXY = swap;
    }
}
