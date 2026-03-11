package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.Log;
import android.util.MutableBoolean;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.SwipeHelper;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.ui.StackViewScrolledEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.misc.FreePathInterpolator;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.util.ArrayList;
import java.util.List;

class TaskStackViewTouchHandler implements SwipeHelper.Callback {
    private static final Interpolator OVERSCROLL_INTERP;
    Context mContext;
    float mDownScrollP;
    int mDownX;
    int mDownY;
    FlingAnimationUtils mFlingAnimUtils;
    boolean mInterceptedBySwipeHelper;

    @ViewDebug.ExportedProperty(category = "recents")
    boolean mIsScrolling;
    int mLastY;
    int mMaximumVelocity;
    int mMinimumVelocity;
    int mOverscrollSize;
    ValueAnimator mScrollFlingAnimator;
    int mScrollTouchSlop;
    TaskStackViewScroller mScroller;
    TaskStackView mSv;
    SwipeHelper mSwipeHelper;
    private float mTargetStackScroll;
    VelocityTracker mVelocityTracker;
    final int mWindowTouchSlop;
    int mActivePointerId = -1;
    TaskView mActiveTaskView = null;
    private final StackViewScrolledEvent mStackViewScrolledEvent = new StackViewScrolledEvent();
    private ArrayList<Task> mCurrentTasks = new ArrayList<>();
    private ArrayList<TaskViewTransform> mCurrentTaskTransforms = new ArrayList<>();
    private ArrayList<TaskViewTransform> mFinalTaskTransforms = new ArrayList<>();
    private ArrayMap<View, Animator> mSwipeHelperAnimations = new ArrayMap<>();
    private TaskViewTransform mTmpTransform = new TaskViewTransform();

    static {
        Path OVERSCROLL_PATH = new Path();
        OVERSCROLL_PATH.moveTo(0.0f, 0.0f);
        OVERSCROLL_PATH.cubicTo(0.2f, 0.175f, 0.25f, 0.3f, 1.0f, 0.3f);
        OVERSCROLL_INTERP = new FreePathInterpolator(OVERSCROLL_PATH);
    }

    public TaskStackViewTouchHandler(Context context, TaskStackView sv, TaskStackViewScroller scroller) {
        Resources res = context.getResources();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mContext = context;
        this.mSv = sv;
        this.mScroller = scroller;
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mScrollTouchSlop = configuration.getScaledTouchSlop();
        this.mWindowTouchSlop = configuration.getScaledWindowTouchSlop();
        this.mFlingAnimUtils = new FlingAnimationUtils(context, 0.2f);
        this.mOverscrollSize = res.getDimensionPixelSize(R.dimen.recents_fling_overscroll_distance);
        this.mSwipeHelper = new SwipeHelper(0, this, context) {
            @Override
            public float getSize(View v) {
                return TaskStackViewTouchHandler.this.getScaledDismissSize();
            }

            @Override
            protected void prepareDismissAnimation(View v, Animator anim) {
                TaskStackViewTouchHandler.this.mSwipeHelperAnimations.put(v, anim);
            }

            @Override
            protected void prepareSnapBackAnimation(View v, Animator anim) {
                anim.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                TaskStackViewTouchHandler.this.mSwipeHelperAnimations.put(v, anim);
            }

            @Override
            protected float getUnscaledEscapeVelocity() {
                return 800.0f;
            }

            @Override
            protected long getMaxEscapeAnimDuration() {
                return 700L;
            }
        };
        this.mSwipeHelper.setDisableHardwareLayers(true);
    }

    void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    void recycleVelocityTracker() {
        if (this.mVelocityTracker == null) {
            return;
        }
        this.mVelocityTracker.recycle();
        this.mVelocityTracker = null;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        this.mInterceptedBySwipeHelper = this.mSwipeHelper.onInterceptTouchEvent(ev);
        if (this.mInterceptedBySwipeHelper) {
            return true;
        }
        return handleTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mInterceptedBySwipeHelper && this.mSwipeHelper.onTouchEvent(ev)) {
            return true;
        }
        handleTouchEvent(ev);
        return true;
    }

    public void cancelNonDismissTaskAnimations() {
        Utilities.cancelAnimationWithoutCallbacks(this.mScrollFlingAnimator);
        if (!this.mSwipeHelperAnimations.isEmpty()) {
            List<TaskView> taskViews = this.mSv.getTaskViews();
            for (int i = taskViews.size() - 1; i >= 0; i--) {
                TaskView tv = taskViews.get(i);
                if (!this.mSv.isIgnoredTask(tv.getTask())) {
                    tv.cancelTransformAnimation();
                    this.mSv.getStackAlgorithm().addUnfocusedTaskOverride(tv, this.mTargetStackScroll);
                }
            }
            this.mSv.getStackAlgorithm().setFocusState(0);
            this.mSv.getScroller().setStackScroll(this.mTargetStackScroll, null);
            this.mSwipeHelperAnimations.clear();
        }
        this.mActiveTaskView = null;
    }

    private boolean handleTouchEvent(MotionEvent ev) {
        if (this.mSv.getTaskViews().size() == 0) {
            return false;
        }
        TaskStackLayoutAlgorithm layoutAlgorithm = this.mSv.mLayoutAlgorithm;
        int action = ev.getAction();
        switch (action & 255) {
            case 0:
                this.mScroller.stopScroller();
                this.mScroller.stopBoundScrollAnimation();
                this.mScroller.resetDeltaScroll();
                cancelNonDismissTaskAnimations();
                this.mSv.cancelDeferredTaskViewLayoutAnimation();
                this.mDownX = (int) ev.getX();
                this.mDownY = (int) ev.getY();
                this.mLastY = this.mDownY;
                this.mDownScrollP = this.mScroller.getStackScroll();
                this.mActivePointerId = ev.getPointerId(0);
                this.mActiveTaskView = findViewAtPoint(this.mDownX, this.mDownY);
                initOrResetVelocityTracker();
                this.mVelocityTracker.addMovement(ev);
                break;
            case 1:
                this.mVelocityTracker.addMovement(ev);
                this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                int y = (int) ev.getY(ev.findPointerIndex(this.mActivePointerId));
                int velocity = (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                if (this.mIsScrolling) {
                    if (this.mScroller.isScrollOutOfBounds()) {
                        this.mScroller.animateBoundScroll();
                    } else if (Math.abs(velocity) > this.mMinimumVelocity) {
                        float minY = this.mDownY + layoutAlgorithm.getYForDeltaP(this.mDownScrollP, layoutAlgorithm.mMaxScrollP);
                        float maxY = this.mDownY + layoutAlgorithm.getYForDeltaP(this.mDownScrollP, layoutAlgorithm.mMinScrollP);
                        this.mScroller.fling(this.mDownScrollP, this.mDownY, y, velocity, (int) minY, (int) maxY, this.mOverscrollSize);
                        this.mSv.invalidate();
                    }
                    if (!this.mSv.mTouchExplorationEnabled) {
                        this.mSv.resetFocusedTask(this.mSv.getFocusedTask());
                    }
                } else if (this.mActiveTaskView == null) {
                    maybeHideRecentsFromBackgroundTap((int) ev.getX(), (int) ev.getY());
                }
                this.mActivePointerId = -1;
                this.mIsScrolling = false;
                recycleVelocityTracker();
                break;
            case 2:
                int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
                if (activePointerIndex < 0) {
                    Log.d("TaskStackViewTouchHandler", "findPointerIndex failed");
                    this.mActivePointerId = -1;
                } else {
                    int y2 = (int) ev.getY(activePointerIndex);
                    int x = (int) ev.getX(activePointerIndex);
                    if (!this.mIsScrolling) {
                        int yDiff = Math.abs(y2 - this.mDownY);
                        int xDiff = Math.abs(x - this.mDownX);
                        if (Math.abs(y2 - this.mDownY) > this.mScrollTouchSlop && yDiff > xDiff) {
                            this.mIsScrolling = true;
                            float stackScroll = this.mScroller.getStackScroll();
                            List<TaskView> taskViews = this.mSv.getTaskViews();
                            for (int i = taskViews.size() - 1; i >= 0; i--) {
                                layoutAlgorithm.addUnfocusedTaskOverride(taskViews.get(i).getTask(), stackScroll);
                            }
                            layoutAlgorithm.setFocusState(0);
                            ViewParent parent = this.mSv.getParent();
                            if (parent != null) {
                                parent.requestDisallowInterceptTouchEvent(true);
                            }
                            MetricsLogger.action(this.mSv.getContext(), 287);
                        }
                    }
                    if (this.mIsScrolling) {
                        float deltaP = layoutAlgorithm.getDeltaPForY(this.mDownY, y2);
                        float minScrollP = layoutAlgorithm.mMinScrollP;
                        float maxScrollP = layoutAlgorithm.mMaxScrollP;
                        float curScrollP = this.mDownScrollP + deltaP;
                        if (curScrollP < minScrollP || curScrollP > maxScrollP) {
                            float clampedScrollP = Utilities.clamp(curScrollP, minScrollP, maxScrollP);
                            float overscrollP = curScrollP - clampedScrollP;
                            float overscrollX = Math.abs(overscrollP) / 2.3333333f;
                            float interpX = OVERSCROLL_INTERP.getInterpolation(overscrollX);
                            curScrollP = clampedScrollP + (Math.signum(overscrollP) * 2.3333333f * interpX);
                        }
                        this.mDownScrollP += this.mScroller.setDeltaStackScroll(this.mDownScrollP, curScrollP - this.mDownScrollP);
                        this.mStackViewScrolledEvent.updateY(y2 - this.mLastY);
                        EventBus.getDefault().send(this.mStackViewScrolledEvent);
                    }
                    this.mLastY = y2;
                    this.mVelocityTracker.addMovement(ev);
                }
                break;
            case 3:
                this.mActivePointerId = -1;
                this.mIsScrolling = false;
                recycleVelocityTracker();
                break;
            case 5:
                int index = ev.getActionIndex();
                this.mActivePointerId = ev.getPointerId(index);
                this.mDownX = (int) ev.getX(index);
                this.mDownY = (int) ev.getY(index);
                this.mLastY = this.mDownY;
                this.mDownScrollP = this.mScroller.getStackScroll();
                this.mScroller.resetDeltaScroll();
                this.mVelocityTracker.addMovement(ev);
                break;
            case 6:
                int pointerIndex = ev.getActionIndex();
                int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == this.mActivePointerId) {
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    this.mActivePointerId = ev.getPointerId(newPointerIndex);
                    this.mDownX = (int) ev.getX(pointerIndex);
                    this.mDownY = (int) ev.getY(pointerIndex);
                    this.mLastY = this.mDownY;
                    this.mDownScrollP = this.mScroller.getStackScroll();
                }
                this.mVelocityTracker.addMovement(ev);
                break;
        }
        return this.mIsScrolling;
    }

    void maybeHideRecentsFromBackgroundTap(int x, int y) {
        int shiftedX;
        int dx = Math.abs(this.mDownX - x);
        int dy = Math.abs(this.mDownY - y);
        if (dx > this.mScrollTouchSlop || dy > this.mScrollTouchSlop) {
            return;
        }
        if (x > (this.mSv.getRight() - this.mSv.getLeft()) / 2) {
            shiftedX = x - this.mWindowTouchSlop;
        } else {
            shiftedX = x + this.mWindowTouchSlop;
        }
        if (findViewAtPoint(shiftedX, y) != null) {
            return;
        }
        if (x > this.mSv.mLayoutAlgorithm.mStackRect.left && x < this.mSv.mLayoutAlgorithm.mStackRect.right) {
            return;
        }
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.hasFreeformWorkspaceSupport()) {
            Rect freeformRect = this.mSv.mLayoutAlgorithm.mFreeformRect;
            if (freeformRect.top <= y && y <= freeformRect.bottom && this.mSv.launchFreeformTasks()) {
                return;
            }
        }
        EventBus.getDefault().send(new HideRecentsEvent(false, true));
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        if ((ev.getSource() & 2) == 2) {
            int action = ev.getAction();
            switch (action & 255) {
                case 8:
                    float vScroll = ev.getAxisValue(9);
                    if (vScroll > 0.0f) {
                        this.mSv.setRelativeFocusedTask(true, true, false);
                    } else {
                        this.mSv.setRelativeFocusedTask(false, true, false);
                    }
                    return true;
            }
        }
        return false;
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        TaskView tv = findViewAtPoint((int) ev.getX(), (int) ev.getY());
        if (tv == null || !canChildBeDismissed(tv)) {
            return null;
        }
        return tv;
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        TaskView tv = (TaskView) v;
        Task task = tv.getTask();
        return (this.mSwipeHelperAnimations.containsKey(v) || this.mSv.getStack().indexOfStackTask(task) == -1) ? false : true;
    }

    public void onBeginManualDrag(TaskView v) {
        this.mActiveTaskView = v;
        this.mSwipeHelperAnimations.put(v, null);
        onBeginDrag(v);
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
        this.mSv.addIgnoreTask(tv.getTask());
        this.mCurrentTasks = new ArrayList<>(this.mSv.getStack().getStackTasks());
        MutableBoolean isFrontMostTask = new MutableBoolean(false);
        Task anchorTask = this.mSv.findAnchorTask(this.mCurrentTasks, isFrontMostTask);
        TaskStackLayoutAlgorithm layoutAlgorithm = this.mSv.getStackAlgorithm();
        TaskStackViewScroller stackScroller = this.mSv.getScroller();
        if (anchorTask == null) {
            return;
        }
        this.mSv.getCurrentTaskTransforms(this.mCurrentTasks, this.mCurrentTaskTransforms);
        float prevAnchorTaskScroll = 0.0f;
        boolean pullStackForward = this.mCurrentTasks.size() > 0;
        if (pullStackForward) {
            prevAnchorTaskScroll = layoutAlgorithm.getStackScrollForTask(anchorTask);
        }
        this.mSv.updateLayoutAlgorithm(false);
        float newStackScroll = stackScroller.getStackScroll();
        if (isFrontMostTask.value) {
            newStackScroll = stackScroller.getBoundedStackScroll(newStackScroll);
        } else if (pullStackForward) {
            float anchorTaskScroll = layoutAlgorithm.getStackScrollForTaskIgnoreOverrides(anchorTask);
            float stackScrollOffset = anchorTaskScroll - prevAnchorTaskScroll;
            if (layoutAlgorithm.getFocusState() != 1) {
                stackScrollOffset *= 0.75f;
            }
            newStackScroll = stackScroller.getBoundedStackScroll(stackScroller.getStackScroll() + stackScrollOffset);
        }
        this.mSv.bindVisibleTaskViews(newStackScroll, true);
        this.mSv.getLayoutTaskTransforms(newStackScroll, 0, this.mCurrentTasks, true, this.mFinalTaskTransforms);
        this.mTargetStackScroll = newStackScroll;
    }

    @Override
    public boolean updateSwipeProgress(View v, boolean dismissable, float swipeProgress) {
        if (this.mActiveTaskView == v || this.mSwipeHelperAnimations.containsKey(v)) {
            updateTaskViewTransforms(Interpolators.FAST_OUT_SLOW_IN.getInterpolation(swipeProgress));
            return true;
        }
        return true;
    }

    @Override
    public void onChildDismissed(View v) {
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(true);
        tv.setTouchEnabled(true);
        EventBus.getDefault().send(new TaskViewDismissedEvent(tv.getTask(), tv, this.mSwipeHelperAnimations.containsKey(v) ? new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN) : null));
        if (this.mSwipeHelperAnimations.containsKey(v)) {
            this.mSv.getScroller().setStackScroll(this.mTargetStackScroll, null);
            this.mSv.getStackAlgorithm().setFocusState(0);
            this.mSv.getStackAlgorithm().clearUnfocusedTaskOverrides();
            this.mSwipeHelperAnimations.remove(v);
        }
        MetricsLogger.histogram(tv.getContext(), "overview_task_dismissed_source", 1);
    }

    @Override
    public void onChildSnappedBack(View v, float targetLeft) {
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(true);
        tv.setTouchEnabled(true);
        this.mSv.removeIgnoreTask(tv.getTask());
        this.mSv.updateLayoutAlgorithm(false);
        this.mSv.relayoutTaskViews(AnimationProps.IMMEDIATE);
        this.mSwipeHelperAnimations.remove(v);
    }

    @Override
    public void onDragCancelled(View v) {
    }

    @Override
    public boolean isAntiFalsingNeeded() {
        return false;
    }

    @Override
    public float getFalsingThresholdFactor() {
        return 0.0f;
    }

    private void updateTaskViewTransforms(float dismissFraction) {
        int taskIndex;
        List<TaskView> taskViews = this.mSv.getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!this.mSv.isIgnoredTask(task) && (taskIndex = this.mCurrentTasks.indexOf(task)) != -1) {
                TaskViewTransform fromTransform = this.mCurrentTaskTransforms.get(taskIndex);
                TaskViewTransform toTransform = this.mFinalTaskTransforms.get(taskIndex);
                this.mTmpTransform.copyFrom(fromTransform);
                this.mTmpTransform.rect.set(Utilities.RECTF_EVALUATOR.evaluate(dismissFraction, fromTransform.rect, toTransform.rect));
                this.mTmpTransform.dimAlpha = fromTransform.dimAlpha + ((toTransform.dimAlpha - fromTransform.dimAlpha) * dismissFraction);
                this.mTmpTransform.viewOutlineAlpha = fromTransform.viewOutlineAlpha + ((toTransform.viewOutlineAlpha - fromTransform.viewOutlineAlpha) * dismissFraction);
                this.mTmpTransform.translationZ = fromTransform.translationZ + ((toTransform.translationZ - fromTransform.translationZ) * dismissFraction);
                this.mSv.updateTaskViewToTransform(tv, this.mTmpTransform, AnimationProps.IMMEDIATE);
            }
        }
    }

    private TaskView findViewAtPoint(int x, int y) {
        List<Task> tasks = this.mSv.getStack().getStackTasks();
        int taskCount = tasks.size();
        for (int i = taskCount - 1; i >= 0; i--) {
            TaskView tv = this.mSv.getChildViewForTask(tasks.get(i));
            if (tv != null && tv.getVisibility() == 0 && this.mSv.isTouchPointInView(x, y, tv)) {
                return tv;
            }
        }
        return null;
    }

    public float getScaledDismissSize() {
        return Math.max(this.mSv.getWidth(), this.mSv.getHeight()) * 1.5f;
    }
}
