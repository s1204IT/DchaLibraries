package com.android.systemui.recents.views;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.views.SwipeHelper;

class TaskStackViewTouchHandler implements SwipeHelper.Callback {
    static int INACTIVE_POINTER_ID = -1;
    int mActivePointerId = INACTIVE_POINTER_ID;
    TaskView mActiveTaskView = null;
    RecentsConfiguration mConfig;
    int mInitialMotionX;
    int mInitialMotionY;
    float mInitialP;
    boolean mInterceptedBySwipeHelper;
    boolean mIsScrolling;
    int mLastMotionX;
    int mLastMotionY;
    float mLastP;
    int mMaximumVelocity;
    int mMinimumVelocity;
    float mPagingTouchSlop;
    int mScrollTouchSlop;
    TaskStackViewScroller mScroller;
    TaskStackView mSv;
    SwipeHelper mSwipeHelper;
    float mTotalPMotion;
    VelocityTracker mVelocityTracker;

    public TaskStackViewTouchHandler(Context context, TaskStackView sv, RecentsConfiguration config, TaskStackViewScroller scroller) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mScrollTouchSlop = configuration.getScaledTouchSlop();
        this.mPagingTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mSv = sv;
        this.mScroller = scroller;
        this.mConfig = config;
        float densityScale = context.getResources().getDisplayMetrics().density;
        this.mSwipeHelper = new SwipeHelper(0, this, densityScale, this.mPagingTouchSlop);
        this.mSwipeHelper.setMinAlpha(1.0f);
    }

    void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    TaskView findViewAtPoint(int x, int y) {
        int childCount = this.mSv.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            TaskView tv = (TaskView) this.mSv.getChildAt(i);
            if (tv.getVisibility() == 0 && this.mSv.isTransformedTouchPointInView(x, y, tv)) {
                return tv;
            }
        }
        return null;
    }

    MotionEvent createMotionEventForStackScroll(MotionEvent ev) {
        MotionEvent pev = MotionEvent.obtainNoHistory(ev);
        pev.setLocation(0.0f, this.mScroller.progressToScrollRange(this.mScroller.getStackScroll()));
        return pev;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean hasChildren = this.mSv.getChildCount() > 0;
        if (!hasChildren) {
            return false;
        }
        this.mInterceptedBySwipeHelper = this.mSwipeHelper.onInterceptTouchEvent(ev);
        if (this.mInterceptedBySwipeHelper) {
            return true;
        }
        boolean wasScrolling = this.mScroller.isScrolling() || (this.mScroller.mScrollAnimator != null && this.mScroller.mScrollAnimator.isRunning());
        int action = ev.getAction();
        switch (action & 255) {
            case 0:
                int x = (int) ev.getX();
                this.mLastMotionX = x;
                this.mInitialMotionX = x;
                int y = (int) ev.getY();
                this.mLastMotionY = y;
                this.mInitialMotionY = y;
                float fScreenYToCurveProgress = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                this.mLastP = fScreenYToCurveProgress;
                this.mInitialP = fScreenYToCurveProgress;
                this.mActivePointerId = ev.getPointerId(0);
                this.mActiveTaskView = findViewAtPoint(this.mLastMotionX, this.mLastMotionY);
                this.mScroller.stopScroller();
                this.mScroller.stopBoundScrollAnimation();
                initOrResetVelocityTracker();
                this.mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                break;
            case 1:
            case 3:
                this.mScroller.animateBoundScroll();
                this.mIsScrolling = false;
                this.mActivePointerId = INACTIVE_POINTER_ID;
                this.mActiveTaskView = null;
                this.mTotalPMotion = 0.0f;
                recycleVelocityTracker();
                break;
            case 2:
                if (this.mActivePointerId != INACTIVE_POINTER_ID) {
                    initVelocityTrackerIfNotExists();
                    this.mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                    int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
                    int y2 = (int) ev.getY(activePointerIndex);
                    int x2 = (int) ev.getX(activePointerIndex);
                    if (Math.abs(y2 - this.mInitialMotionY) > this.mScrollTouchSlop) {
                        this.mIsScrolling = true;
                        ViewParent parent = this.mSv.getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    this.mLastMotionX = x2;
                    this.mLastMotionY = y2;
                    this.mLastP = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                }
                break;
        }
        return wasScrolling || this.mIsScrolling;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean hasChildren = this.mSv.getChildCount() > 0;
        if (!hasChildren) {
            return false;
        }
        if (this.mInterceptedBySwipeHelper && this.mSwipeHelper.onTouchEvent(ev)) {
            return true;
        }
        initVelocityTrackerIfNotExists();
        int action = ev.getAction();
        switch (action & 255) {
            case 0:
                int x = (int) ev.getX();
                this.mLastMotionX = x;
                this.mInitialMotionX = x;
                int y = (int) ev.getY();
                this.mLastMotionY = y;
                this.mInitialMotionY = y;
                float fScreenYToCurveProgress = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                this.mLastP = fScreenYToCurveProgress;
                this.mInitialP = fScreenYToCurveProgress;
                this.mActivePointerId = ev.getPointerId(0);
                this.mActiveTaskView = findViewAtPoint(this.mLastMotionX, this.mLastMotionY);
                this.mScroller.stopScroller();
                this.mScroller.stopBoundScrollAnimation();
                initOrResetVelocityTracker();
                this.mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                ViewParent parent = this.mSv.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case 1:
                this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                int velocity = (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                if (this.mIsScrolling && Math.abs(velocity) > this.mMinimumVelocity) {
                    float overscrollRangePct = Math.abs(velocity / this.mMaximumVelocity);
                    int overscrollRange = (int) (Math.min(1.0f, overscrollRangePct) * 96.0f);
                    this.mScroller.mScroller.fling(0, this.mScroller.progressToScrollRange(this.mScroller.getStackScroll()), 0, velocity, 0, 0, this.mScroller.progressToScrollRange(this.mSv.mLayoutAlgorithm.mMinScrollP), this.mScroller.progressToScrollRange(this.mSv.mLayoutAlgorithm.mMaxScrollP), 0, overscrollRange + 32);
                    this.mSv.invalidate();
                } else if (this.mScroller.isScrollOutOfBounds()) {
                    this.mScroller.animateBoundScroll();
                }
                this.mActivePointerId = INACTIVE_POINTER_ID;
                this.mIsScrolling = false;
                this.mTotalPMotion = 0.0f;
                recycleVelocityTracker();
                return true;
            case 2:
                if (this.mActivePointerId != INACTIVE_POINTER_ID) {
                    this.mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                    int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
                    int x2 = (int) ev.getX(activePointerIndex);
                    int y2 = (int) ev.getY(activePointerIndex);
                    int yTotal = Math.abs(y2 - this.mInitialMotionY);
                    float curP = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(y2);
                    float deltaP = this.mLastP - curP;
                    if (!this.mIsScrolling && yTotal > this.mScrollTouchSlop) {
                        this.mIsScrolling = true;
                        ViewParent parent2 = this.mSv.getParent();
                        if (parent2 != null) {
                            parent2.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    if (this.mIsScrolling) {
                        float curStackScroll = this.mScroller.getStackScroll();
                        float overScrollAmount = this.mScroller.getScrollAmountOutOfBounds(curStackScroll + deltaP);
                        if (Float.compare(overScrollAmount, 0.0f) != 0) {
                            float maxOverScroll = this.mConfig.taskStackOverscrollPct;
                            deltaP *= 1.0f - (Math.min(maxOverScroll, overScrollAmount) / maxOverScroll);
                        }
                        this.mScroller.setStackScroll(curStackScroll + deltaP);
                    }
                    this.mLastMotionX = x2;
                    this.mLastMotionY = y2;
                    this.mLastP = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                    this.mTotalPMotion += Math.abs(deltaP);
                }
                return true;
            case 3:
                if (this.mScroller.isScrollOutOfBounds()) {
                    this.mScroller.animateBoundScroll();
                }
                this.mActivePointerId = INACTIVE_POINTER_ID;
                this.mIsScrolling = false;
                this.mTotalPMotion = 0.0f;
                recycleVelocityTracker();
                return true;
            case 4:
            default:
                return true;
            case 5:
                int index = ev.getActionIndex();
                this.mActivePointerId = ev.getPointerId(index);
                this.mLastMotionX = (int) ev.getX(index);
                this.mLastMotionY = (int) ev.getY(index);
                this.mLastP = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                return true;
            case 6:
                int pointerIndex = ev.getActionIndex();
                int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == this.mActivePointerId) {
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    this.mActivePointerId = ev.getPointerId(newPointerIndex);
                    this.mLastMotionX = (int) ev.getX(newPointerIndex);
                    this.mLastMotionY = (int) ev.getY(newPointerIndex);
                    this.mLastP = this.mSv.mLayoutAlgorithm.screenYToCurveProgress(this.mLastMotionY);
                    this.mVelocityTracker.clear();
                }
                return true;
        }
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        if ((ev.getSource() & 2) == 2) {
            int action = ev.getAction();
            switch (action & 255) {
                case 8:
                    float vScroll = ev.getAxisValue(9);
                    if (vScroll > 0.0f) {
                        if (!this.mSv.ensureFocusedTask()) {
                            return true;
                        }
                        this.mSv.focusNextTask(true, false);
                        return true;
                    }
                    if (!this.mSv.ensureFocusedTask()) {
                        return true;
                    }
                    this.mSv.focusNextTask(false, false);
                    return true;
            }
        }
        return false;
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        return findViewAtPoint((int) ev.getX(), (int) ev.getY());
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        return true;
    }

    @Override
    public void onBeginDrag(View v) {
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(false);
        tv.setTouchEnabled(false);
        ViewParent parent = this.mSv.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    public void onSwipeChanged(View v, float delta) {
    }

    @Override
    public void onChildDismissed(View v) {
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(true);
        tv.setTouchEnabled(true);
        this.mSv.onTaskViewDismissed(tv);
    }

    @Override
    public void onSnapBackCompleted(View v) {
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(true);
        tv.setTouchEnabled(true);
    }

    @Override
    public void onDragCancelled(View v) {
    }
}
