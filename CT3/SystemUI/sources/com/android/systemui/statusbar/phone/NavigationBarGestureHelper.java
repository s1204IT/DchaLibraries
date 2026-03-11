package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.DividerView;
import com.android.systemui.tuner.TunerService;

public class NavigationBarGestureHelper extends GestureDetector.SimpleOnGestureListener implements TunerService.Tunable {
    private Context mContext;
    private Divider mDivider;
    private boolean mDockWindowEnabled;
    private boolean mDockWindowTouchSlopExceeded;
    private boolean mDownOnRecents;
    private int mDragMode;
    private boolean mIsRTL;
    private boolean mIsVertical;
    private final int mMinFlingVelocity;
    private NavigationBarView mNavigationBarView;
    private RecentsComponent mRecentsComponent;
    private final int mScrollTouchSlop;
    private final GestureDetector mTaskSwitcherDetector;
    private int mTouchDownX;
    private int mTouchDownY;
    private VelocityTracker mVelocityTracker;

    public NavigationBarGestureHelper(Context context) {
        this.mContext = context;
        ViewConfiguration configuration = ViewConfiguration.get(context);
        Resources r = context.getResources();
        this.mScrollTouchSlop = r.getDimensionPixelSize(R.dimen.navigation_bar_min_swipe_distance);
        this.mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mTaskSwitcherDetector = new GestureDetector(context, this);
        TunerService.get(context).addTunable(this, "overview_nav_bar_gesture");
    }

    public void setComponents(RecentsComponent recentsComponent, Divider divider, NavigationBarView navigationBarView) {
        this.mRecentsComponent = recentsComponent;
        this.mDivider = divider;
        this.mNavigationBarView = navigationBarView;
    }

    public void setBarState(boolean isVertical, boolean isRTL) {
        this.mIsVertical = isVertical;
        this.mIsRTL = isRTL;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean exceededTouchSlop;
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
                if (!this.mIsVertical) {
                    exceededTouchSlop = xDiff > this.mScrollTouchSlop && xDiff > yDiff;
                } else {
                    exceededTouchSlop = yDiff > this.mScrollTouchSlop && yDiff > xDiff;
                }
                if (exceededTouchSlop) {
                    return true;
                }
                break;
        }
        if (this.mDockWindowEnabled) {
            return interceptDockWindowEvent(event);
        }
        return false;
    }

    private boolean interceptDockWindowEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case 0:
                handleDragActionDownEvent(event);
                return false;
            case 1:
            case 3:
                handleDragActionUpEvent(event);
                return false;
            case 2:
                return handleDragActionMoveEvent(event);
            default:
                return false;
        }
    }

    private boolean handleDockWindowEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case 0:
                handleDragActionDownEvent(event);
                break;
            case 1:
            case 3:
                handleDragActionUpEvent(event);
                break;
            case 2:
                handleDragActionMoveEvent(event);
                break;
        }
        return true;
    }

    private void handleDragActionDownEvent(MotionEvent event) {
        boolean z = false;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mVelocityTracker.addMovement(event);
        this.mDockWindowTouchSlopExceeded = false;
        this.mTouchDownX = (int) event.getX();
        this.mTouchDownY = (int) event.getY();
        if (this.mNavigationBarView == null) {
            return;
        }
        View recentsButton = this.mNavigationBarView.getRecentsButton().getCurrentView();
        if (recentsButton != null) {
            if (this.mTouchDownX >= recentsButton.getLeft() && this.mTouchDownX <= recentsButton.getRight() && this.mTouchDownY >= recentsButton.getTop() && this.mTouchDownY <= recentsButton.getBottom()) {
                z = true;
            }
            this.mDownOnRecents = z;
            return;
        }
        this.mDownOnRecents = false;
    }

    private boolean handleDragActionMoveEvent(MotionEvent event) {
        boolean touchSlopExceeded;
        int rawY;
        int i;
        this.mVelocityTracker.addMovement(event);
        int x = (int) event.getX();
        int y = (int) event.getY();
        int xDiff = Math.abs(x - this.mTouchDownX);
        int yDiff = Math.abs(y - this.mTouchDownY);
        if (this.mDivider == null || this.mRecentsComponent == null) {
            return false;
        }
        if (!this.mDockWindowTouchSlopExceeded) {
            if (!this.mIsVertical) {
                touchSlopExceeded = yDiff > this.mScrollTouchSlop && yDiff > xDiff;
            } else {
                touchSlopExceeded = xDiff > this.mScrollTouchSlop && xDiff > yDiff;
            }
            if (this.mDownOnRecents && touchSlopExceeded && this.mDivider.getView().getWindowManagerProxy().getDockSide() == -1) {
                Rect initialBounds = null;
                int dragMode = calculateDragMode();
                int createMode = 0;
                if (dragMode == 1) {
                    initialBounds = new Rect();
                    DividerView view = this.mDivider.getView();
                    if (this.mIsVertical) {
                        rawY = (int) event.getRawX();
                    } else {
                        rawY = (int) event.getRawY();
                    }
                    if (this.mDivider.getView().isHorizontalDivision()) {
                        i = 2;
                    } else {
                        i = 1;
                    }
                    view.calculateBoundsForPosition(rawY, i, initialBounds);
                } else if (dragMode == 0 && this.mTouchDownX < this.mContext.getResources().getDisplayMetrics().widthPixels / 2) {
                    createMode = 1;
                }
                boolean docked = this.mRecentsComponent.dockTopTask(dragMode, createMode, initialBounds, 272);
                if (docked) {
                    this.mDragMode = dragMode;
                    if (this.mDragMode == 1) {
                        this.mDivider.getView().startDragging(false, true);
                    }
                    this.mDockWindowTouchSlopExceeded = true;
                    return true;
                }
                return false;
            }
            return false;
        }
        if (this.mDragMode == 1) {
            int position = (int) (!this.mIsVertical ? event.getRawY() : event.getRawX());
            DividerSnapAlgorithm.SnapTarget snapTarget = this.mDivider.getView().getSnapAlgorithm().calculateSnapTarget(position, 0.0f, false);
            this.mDivider.getView().resizeStack(position, snapTarget.position, snapTarget);
            return false;
        }
        if (this.mDragMode == 0) {
            this.mRecentsComponent.onDraggingInRecents(event.getRawY());
            return false;
        }
        return false;
    }

    private void handleDragActionUpEvent(MotionEvent event) {
        int rawY;
        float yVelocity;
        this.mVelocityTracker.addMovement(event);
        this.mVelocityTracker.computeCurrentVelocity(1000);
        if (this.mDockWindowTouchSlopExceeded && this.mDivider != null && this.mRecentsComponent != null) {
            if (this.mDragMode == 1) {
                DividerView view = this.mDivider.getView();
                if (this.mIsVertical) {
                    rawY = (int) event.getRawX();
                } else {
                    rawY = (int) event.getRawY();
                }
                if (this.mIsVertical) {
                    yVelocity = this.mVelocityTracker.getXVelocity();
                } else {
                    yVelocity = this.mVelocityTracker.getYVelocity();
                }
                view.stopDragging(rawY, yVelocity, true, false);
            } else if (this.mDragMode == 0) {
                this.mRecentsComponent.onDraggingInRecentsEnded(this.mVelocityTracker.getYVelocity());
            }
        }
        this.mVelocityTracker.recycle();
        this.mVelocityTracker = null;
    }

    private int calculateDragMode() {
        if (!this.mIsVertical || this.mDivider.getView().isHorizontalDivision()) {
            return (this.mIsVertical || !this.mDivider.getView().isHorizontalDivision()) ? 0 : 1;
        }
        return 1;
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean result = this.mTaskSwitcherDetector.onTouchEvent(event);
        if (this.mDockWindowEnabled) {
            return result | handleDockWindowEvent(event);
        }
        return result;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean showNext;
        boolean isValidFling = false;
        float absVelX = Math.abs(velocityX);
        float absVelY = Math.abs(velocityY);
        if (absVelX <= this.mMinFlingVelocity || !this.mIsVertical ? absVelX > absVelY : absVelY > absVelX) {
            isValidFling = true;
        }
        if (isValidFling && this.mRecentsComponent != null) {
            if (!this.mIsRTL) {
                showNext = this.mIsVertical ? false : false;
            } else {
                showNext = this.mIsVertical ? false : false;
            }
            if (showNext) {
                this.mRecentsComponent.showNextAffiliatedTask();
            } else {
                this.mRecentsComponent.showPrevAffiliatedTask();
            }
        }
        return true;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        boolean z = false;
        if (!key.equals("overview_nav_bar_gesture")) {
            return;
        }
        if (newValue != null && Integer.parseInt(newValue) != 0) {
            z = true;
        }
        this.mDockWindowEnabled = z;
    }
}
