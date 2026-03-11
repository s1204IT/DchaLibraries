package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Path;
import android.util.ArrayMap;
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
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.utilities.AnimationProps;
import com.android.systemui.shared.recents.utilities.Utilities;
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
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(0.2f, 0.175f, 0.25f, 0.3f, 1.0f, 0.3f);
        OVERSCROLL_INTERP = new FreePathInterpolator(path);
    }

    public TaskStackViewTouchHandler(Context context, TaskStackView taskStackView, TaskStackViewScroller taskStackViewScroller) {
        Resources resources = context.getResources();
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mContext = context;
        this.mSv = taskStackView;
        this.mScroller = taskStackViewScroller;
        this.mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mScrollTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mWindowTouchSlop = viewConfiguration.getScaledWindowTouchSlop();
        this.mFlingAnimUtils = new FlingAnimationUtils(context, 0.2f);
        this.mOverscrollSize = resources.getDimensionPixelSize(R.dimen.recents_fling_overscroll_distance);
        this.mSwipeHelper = new SwipeHelper(0, this, context) {
            @Override
            protected float getSize(View view) {
                return TaskStackViewTouchHandler.this.getScaledDismissSize();
            }

            @Override
            protected void prepareDismissAnimation(View view, Animator animator) {
                TaskStackViewTouchHandler.this.mSwipeHelperAnimations.put(view, animator);
            }

            @Override
            protected void prepareSnapBackAnimation(View view, Animator animator) {
                animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                TaskStackViewTouchHandler.this.mSwipeHelperAnimations.put(view, animator);
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
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        this.mInterceptedBySwipeHelper = isSwipingEnabled() && this.mSwipeHelper.onInterceptTouchEvent(motionEvent);
        if (this.mInterceptedBySwipeHelper) {
            return true;
        }
        return handleTouchEvent(motionEvent);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mInterceptedBySwipeHelper && this.mSwipeHelper.onTouchEvent(motionEvent)) {
            return true;
        }
        handleTouchEvent(motionEvent);
        return true;
    }

    public void cancelNonDismissTaskAnimations() {
        Utilities.cancelAnimationWithoutCallbacks(this.mScrollFlingAnimator);
        if (!this.mSwipeHelperAnimations.isEmpty()) {
            List<TaskView> taskViews = this.mSv.getTaskViews();
            for (int size = taskViews.size() - 1; size >= 0; size--) {
                TaskView taskView = taskViews.get(size);
                if (!this.mSv.isIgnoredTask(taskView.getTask())) {
                    taskView.cancelTransformAnimation();
                    this.mSv.getStackAlgorithm().addUnfocusedTaskOverride(taskView, this.mTargetStackScroll);
                }
            }
            this.mSv.getStackAlgorithm().setFocusState(0);
            this.mSv.getScroller().setStackScroll(this.mTargetStackScroll, null);
            this.mSwipeHelperAnimations.clear();
        }
        this.mActiveTaskView = null;
    }

    private boolean handleTouchEvent(MotionEvent motionEvent) {
        float maxOverscroll;
        if (this.mSv.getTaskViews().size() == 0) {
            return false;
        }
        TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = this.mSv.mLayoutAlgorithm;
        switch (motionEvent.getAction() & 255) {
            case 0:
                this.mScroller.stopScroller();
                this.mScroller.stopBoundScrollAnimation();
                this.mScroller.resetDeltaScroll();
                cancelNonDismissTaskAnimations();
                this.mSv.cancelDeferredTaskViewLayoutAnimation();
                this.mDownX = (int) motionEvent.getX();
                this.mDownY = (int) motionEvent.getY();
                this.mLastY = this.mDownY;
                this.mDownScrollP = this.mScroller.getStackScroll();
                this.mActivePointerId = motionEvent.getPointerId(0);
                this.mActiveTaskView = findViewAtPoint(this.mDownX, this.mDownY);
                initOrResetVelocityTracker();
                this.mVelocityTracker.addMovement(motionEvent);
                break;
            case 1:
                this.mVelocityTracker.addMovement(motionEvent);
                this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                int y = (int) motionEvent.getY(motionEvent.findPointerIndex(this.mActivePointerId));
                int yVelocity = (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                if (this.mIsScrolling) {
                    if (this.mScroller.isScrollOutOfBounds()) {
                        this.mScroller.animateBoundScroll();
                    } else if (Math.abs(yVelocity) > this.mMinimumVelocity && !Recents.getConfiguration().isLowRamDevice) {
                        this.mScroller.fling(this.mDownScrollP, this.mDownY, y, yVelocity, this.mDownY + taskStackLayoutAlgorithm.getYForDeltaP(this.mDownScrollP, taskStackLayoutAlgorithm.mMaxScrollP), this.mDownY + taskStackLayoutAlgorithm.getYForDeltaP(this.mDownScrollP, taskStackLayoutAlgorithm.mMinScrollP), this.mOverscrollSize);
                        this.mSv.invalidate();
                    }
                    if (!this.mSv.mTouchExplorationEnabled && !this.mSv.useGridLayout()) {
                        if (Recents.getConfiguration().isLowRamDevice) {
                            this.mScroller.scrollToClosestTask(yVelocity);
                        } else {
                            this.mSv.resetFocusedTask(this.mSv.getFocusedTask());
                        }
                    }
                } else if (this.mActiveTaskView == null) {
                    maybeHideRecentsFromBackgroundTap((int) motionEvent.getX(), (int) motionEvent.getY());
                }
                this.mActivePointerId = -1;
                this.mIsScrolling = false;
                recycleVelocityTracker();
                break;
            case 2:
                int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                if (iFindPointerIndex != -1) {
                    int y2 = (int) motionEvent.getY(iFindPointerIndex);
                    int x = (int) motionEvent.getX(iFindPointerIndex);
                    if (!this.mIsScrolling) {
                        int iAbs = Math.abs(y2 - this.mDownY);
                        int iAbs2 = Math.abs(x - this.mDownX);
                        if (Math.abs(y2 - this.mDownY) > this.mScrollTouchSlop && iAbs > iAbs2) {
                            this.mIsScrolling = true;
                            float stackScroll = this.mScroller.getStackScroll();
                            List<TaskView> taskViews = this.mSv.getTaskViews();
                            for (int size = taskViews.size() - 1; size >= 0; size--) {
                                taskStackLayoutAlgorithm.addUnfocusedTaskOverride(taskViews.get(size).getTask(), stackScroll);
                            }
                            taskStackLayoutAlgorithm.setFocusState(0);
                            ViewParent parent = this.mSv.getParent();
                            if (parent != null) {
                                parent.requestDisallowInterceptTouchEvent(true);
                            }
                            MetricsLogger.action(this.mSv.getContext(), 287);
                            this.mDownY = y2;
                            this.mLastY = y2;
                        }
                    }
                    if (this.mIsScrolling) {
                        float deltaPForY = taskStackLayoutAlgorithm.getDeltaPForY(this.mDownY, y2);
                        float f = taskStackLayoutAlgorithm.mMinScrollP;
                        float f2 = taskStackLayoutAlgorithm.mMaxScrollP;
                        float fSignum = this.mDownScrollP + deltaPForY;
                        if (fSignum < f || fSignum > f2) {
                            float fClamp = Utilities.clamp(fSignum, f, f2);
                            float f3 = fSignum - fClamp;
                            if (Recents.getConfiguration().isLowRamDevice) {
                                maxOverscroll = taskStackLayoutAlgorithm.mTaskStackLowRamLayoutAlgorithm.getMaxOverscroll();
                            } else {
                                maxOverscroll = 2.3333333f;
                            }
                            fSignum = fClamp + (Math.signum(f3) * OVERSCROLL_INTERP.getInterpolation(Math.abs(f3) / maxOverscroll) * maxOverscroll);
                        }
                        this.mDownScrollP += this.mScroller.setDeltaStackScroll(this.mDownScrollP, fSignum - this.mDownScrollP);
                        this.mStackViewScrolledEvent.updateY(y2 - this.mLastY);
                        EventBus.getDefault().send(this.mStackViewScrolledEvent);
                    }
                    this.mLastY = y2;
                    this.mVelocityTracker.addMovement(motionEvent);
                }
                break;
            case 3:
                this.mActivePointerId = -1;
                this.mIsScrolling = false;
                recycleVelocityTracker();
                break;
            case 5:
                int actionIndex = motionEvent.getActionIndex();
                this.mActivePointerId = motionEvent.getPointerId(actionIndex);
                this.mDownX = (int) motionEvent.getX(actionIndex);
                this.mDownY = (int) motionEvent.getY(actionIndex);
                this.mLastY = this.mDownY;
                this.mDownScrollP = this.mScroller.getStackScroll();
                this.mScroller.resetDeltaScroll();
                this.mVelocityTracker.addMovement(motionEvent);
                break;
            case 6:
                int actionIndex2 = motionEvent.getActionIndex();
                if (motionEvent.getPointerId(actionIndex2) == this.mActivePointerId) {
                    this.mActivePointerId = motionEvent.getPointerId(actionIndex2 == 0 ? 1 : 0);
                    this.mDownX = (int) motionEvent.getX(actionIndex2);
                    this.mDownY = (int) motionEvent.getY(actionIndex2);
                    this.mLastY = this.mDownY;
                    this.mDownScrollP = this.mScroller.getStackScroll();
                }
                this.mVelocityTracker.addMovement(motionEvent);
                break;
        }
        return this.mIsScrolling;
    }

    void maybeHideRecentsFromBackgroundTap(int i, int i2) {
        int i3;
        int iAbs = Math.abs(this.mDownX - i);
        int iAbs2 = Math.abs(this.mDownY - i2);
        if (iAbs > this.mScrollTouchSlop || iAbs2 > this.mScrollTouchSlop) {
            return;
        }
        if (i > (this.mSv.getRight() - this.mSv.getLeft()) / 2) {
            i3 = i - this.mWindowTouchSlop;
        } else {
            i3 = this.mWindowTouchSlop + i;
        }
        if (findViewAtPoint(i3, i2) != null) {
            return;
        }
        if (i > this.mSv.mLayoutAlgorithm.mStackRect.left && i < this.mSv.mLayoutAlgorithm.mStackRect.right) {
            return;
        }
        EventBus.getDefault().send(new HideRecentsEvent(false, true));
    }

    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if ((motionEvent.getSource() & 2) != 2 || (motionEvent.getAction() & 255) != 8) {
            return false;
        }
        if (motionEvent.getAxisValue(9) > 0.0f) {
            this.mSv.setRelativeFocusedTask(true, true, false);
        } else {
            this.mSv.setRelativeFocusedTask(false, true, false);
        }
        return true;
    }

    @Override
    public View getChildAtPosition(MotionEvent motionEvent) {
        TaskView taskViewFindViewAtPoint = findViewAtPoint((int) motionEvent.getX(), (int) motionEvent.getY());
        if (taskViewFindViewAtPoint != null && canChildBeDismissed(taskViewFindViewAtPoint)) {
            return taskViewFindViewAtPoint;
        }
        return null;
    }

    @Override
    public boolean canChildBeDismissed(View view) {
        return (this.mSwipeHelperAnimations.containsKey(view) || this.mSv.getStack().indexOfTask(((TaskView) view).getTask()) == -1) ? false : true;
    }

    public void onBeginManualDrag(TaskView taskView) {
        this.mActiveTaskView = taskView;
        this.mSwipeHelperAnimations.put(taskView, null);
        onBeginDrag(taskView);
    }

    @Override
    public void onBeginDrag(View view) {
        TaskView taskView = (TaskView) view;
        taskView.setClipViewInStack(false);
        taskView.setTouchEnabled(false);
        ViewParent parent = this.mSv.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        this.mSv.addIgnoreTask(taskView.getTask());
        this.mCurrentTasks = new ArrayList<>(this.mSv.getStack().getTasks());
        MutableBoolean mutableBoolean = new MutableBoolean(false);
        Task taskFindAnchorTask = this.mSv.findAnchorTask(this.mCurrentTasks, mutableBoolean);
        TaskStackLayoutAlgorithm stackAlgorithm = this.mSv.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mSv.getScroller();
        if (taskFindAnchorTask != null) {
            this.mSv.getCurrentTaskTransforms(this.mCurrentTasks, this.mCurrentTaskTransforms);
            float stackScrollForTask = 0.0f;
            boolean z = this.mCurrentTasks.size() > 0;
            if (z) {
                if (Recents.getConfiguration().isLowRamDevice) {
                    stackScrollForTask = this.mSv.getStackAlgorithm().mTaskStackLowRamLayoutAlgorithm.getScrollPForTask((int) stackAlgorithm.getStackScrollForTask(taskFindAnchorTask));
                } else {
                    stackScrollForTask = stackAlgorithm.getStackScrollForTask(taskFindAnchorTask);
                }
            }
            this.mSv.updateLayoutAlgorithm(false);
            float stackScroll = scroller.getStackScroll();
            if (mutableBoolean.value) {
                stackScroll = scroller.getBoundedStackScroll(stackScroll);
            } else if (z) {
                float stackScrollForTaskIgnoreOverrides = stackAlgorithm.getStackScrollForTaskIgnoreOverrides(taskFindAnchorTask);
                if (Recents.getConfiguration().isLowRamDevice) {
                    stackScrollForTaskIgnoreOverrides = this.mSv.getStackAlgorithm().mTaskStackLowRamLayoutAlgorithm.getScrollPForTask((int) stackAlgorithm.getStackScrollForTask(taskFindAnchorTask));
                }
                float f = stackScrollForTaskIgnoreOverrides - stackScrollForTask;
                if (stackAlgorithm.getFocusState() != 1 && !Recents.getConfiguration().isLowRamDevice) {
                    f *= 0.75f;
                }
                stackScroll = scroller.getBoundedStackScroll(scroller.getStackScroll() + f);
            }
            this.mSv.bindVisibleTaskViews(stackScroll, true);
            this.mSv.getLayoutTaskTransforms(stackScroll, 0, this.mCurrentTasks, true, this.mFinalTaskTransforms);
            this.mTargetStackScroll = stackScroll;
        }
    }

    @Override
    public boolean updateSwipeProgress(View view, boolean z, float f) {
        if ((this.mActiveTaskView == view || this.mSwipeHelperAnimations.containsKey(view)) && !Recents.getConfiguration().isLowRamDevice) {
            updateTaskViewTransforms(Interpolators.FAST_OUT_SLOW_IN.getInterpolation(f));
            return true;
        }
        return true;
    }

    @Override
    public void onChildDismissed(View view) {
        TaskView taskView = (TaskView) view;
        taskView.setClipViewInStack(true);
        taskView.setTouchEnabled(true);
        if (this.mSwipeHelperAnimations.containsKey(view)) {
            this.mSv.getScroller().setStackScroll(this.mTargetStackScroll, null);
        }
        EventBus.getDefault().send(new TaskViewDismissedEvent(taskView.getTask(), taskView, this.mSwipeHelperAnimations.containsKey(view) ? new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN) : null));
        if (this.mSwipeHelperAnimations.containsKey(view)) {
            this.mSv.getStackAlgorithm().setFocusState(0);
            this.mSv.getStackAlgorithm().clearUnfocusedTaskOverrides();
            this.mSwipeHelperAnimations.remove(view);
        }
        MetricsLogger.histogram(taskView.getContext(), "overview_task_dismissed_source", 1);
    }

    @Override
    public void onChildSnappedBack(View view, float f) {
        TaskView taskView = (TaskView) view;
        taskView.setClipViewInStack(true);
        taskView.setTouchEnabled(true);
        this.mSv.removeIgnoreTask(taskView.getTask());
        this.mSv.updateLayoutAlgorithm(false);
        this.mSv.relayoutTaskViews(AnimationProps.IMMEDIATE);
        this.mSwipeHelperAnimations.remove(view);
    }

    @Override
    public void onDragCancelled(View view) {
    }

    @Override
    public boolean isAntiFalsingNeeded() {
        return false;
    }

    @Override
    public float getFalsingThresholdFactor() {
        return 0.0f;
    }

    private void updateTaskViewTransforms(float f) {
        int iIndexOf;
        List<TaskView> taskViews = this.mSv.getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            Task task = taskView.getTask();
            if (!this.mSv.isIgnoredTask(task) && (iIndexOf = this.mCurrentTasks.indexOf(task)) != -1) {
                TaskViewTransform taskViewTransform = this.mCurrentTaskTransforms.get(iIndexOf);
                TaskViewTransform taskViewTransform2 = this.mFinalTaskTransforms.get(iIndexOf);
                this.mTmpTransform.copyFrom(taskViewTransform);
                this.mTmpTransform.rect.set(Utilities.RECTF_EVALUATOR.evaluate(f, taskViewTransform.rect, taskViewTransform2.rect));
                this.mTmpTransform.dimAlpha = taskViewTransform.dimAlpha + ((taskViewTransform2.dimAlpha - taskViewTransform.dimAlpha) * f);
                this.mTmpTransform.viewOutlineAlpha = taskViewTransform.viewOutlineAlpha + ((taskViewTransform2.viewOutlineAlpha - taskViewTransform.viewOutlineAlpha) * f);
                this.mTmpTransform.translationZ = taskViewTransform.translationZ + ((taskViewTransform2.translationZ - taskViewTransform.translationZ) * f);
                this.mSv.updateTaskViewToTransform(taskView, this.mTmpTransform, AnimationProps.IMMEDIATE);
            }
        }
    }

    private TaskView findViewAtPoint(int i, int i2) {
        ArrayList<Task> tasks = this.mSv.getStack().getTasks();
        for (int size = tasks.size() - 1; size >= 0; size--) {
            TaskView childViewForTask = this.mSv.getChildViewForTask(tasks.get(size));
            if (childViewForTask != null && childViewForTask.getVisibility() == 0 && this.mSv.isTouchPointInView(i, i2, childViewForTask)) {
                return childViewForTask;
            }
        }
        return null;
    }

    public float getScaledDismissSize() {
        return 1.5f * Math.max(this.mSv.getWidth(), this.mSv.getHeight());
    }

    private boolean isSwipingEnabled() {
        return !this.mSv.useGridLayout();
    }
}
