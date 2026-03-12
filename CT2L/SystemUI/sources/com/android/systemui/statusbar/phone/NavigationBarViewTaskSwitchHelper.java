package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

public class NavigationBarViewTaskSwitchHelper extends GestureDetector.SimpleOnGestureListener {
    private BaseStatusBar mBar;
    private boolean mIsRTL;
    private boolean mIsVertical;
    private final int mMinFlingVelocity;
    private final int mScrollTouchSlop;
    private final GestureDetector mTaskSwitcherDetector;
    private int mTouchDownX;
    private int mTouchDownY;

    public NavigationBarViewTaskSwitchHelper(Context context) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        Resources r = context.getResources();
        this.mScrollTouchSlop = r.getDimensionPixelSize(R.dimen.navigation_bar_min_swipe_distance);
        this.mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mTaskSwitcherDetector = new GestureDetector(context, this);
    }

    public void setBar(BaseStatusBar phoneStatusBar) {
        this.mBar = phoneStatusBar;
    }

    public void setBarState(boolean isVertical, boolean isRTL) {
        this.mIsVertical = isVertical;
        this.mIsRTL = isRTL;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean exceededTouchSlop = false;
        this.mTaskSwitcherDetector.onTouchEvent(event);
        int action = event.getAction();
        switch (action & 255) {
            case 0:
                this.mTouchDownX = (int) event.getX();
                this.mTouchDownY = (int) event.getY();
                break;
            case 2:
                int x = (int) event.getX();
                int y = (int) event.getY();
                int xDiff = Math.abs(x - this.mTouchDownX);
                int yDiff = Math.abs(y - this.mTouchDownY);
                if (this.mIsVertical) {
                    if (yDiff > this.mScrollTouchSlop && yDiff > xDiff) {
                        exceededTouchSlop = true;
                    }
                } else if (xDiff > this.mScrollTouchSlop && xDiff > yDiff) {
                    exceededTouchSlop = true;
                }
                if (exceededTouchSlop) {
                }
                break;
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return this.mTaskSwitcherDetector.onTouchEvent(event);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean showNext = false;
        float absVelX = Math.abs(velocityX);
        float absVelY = Math.abs(velocityY);
        boolean isValidFling = (absVelX <= ((float) this.mMinFlingVelocity) || !this.mIsVertical) ? absVelX > absVelY : absVelY > absVelX;
        if (isValidFling) {
            if (!this.mIsRTL) {
                if (this.mIsVertical) {
                    if (velocityY < 0.0f) {
                        showNext = true;
                    }
                } else if (velocityX < 0.0f) {
                    showNext = true;
                }
            } else if (this.mIsVertical) {
                if (velocityY < 0.0f) {
                    showNext = true;
                }
            } else if (velocityX > 0.0f) {
                showNext = true;
            }
            if (showNext) {
                this.mBar.showNextAffiliatedTask();
            } else {
                this.mBar.showPreviousAffiliatedTask();
            }
        }
        return true;
    }
}
