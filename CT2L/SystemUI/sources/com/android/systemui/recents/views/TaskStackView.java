package com.android.systemui.recents.views;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.RecentsPackageMonitor;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskStackViewLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackViewScroller;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.recents.views.ViewAnimation;
import com.android.systemui.recents.views.ViewPool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class TaskStackView extends FrameLayout implements RecentsPackageMonitor.PackageCallbacks, TaskStack.TaskStackCallbacks, TaskStackViewScroller.TaskStackViewScrollerCallbacks, TaskView.TaskViewCallbacks, ViewPool.ViewPoolConsumer<TaskView, Task> {
    boolean mAwaitingFirstLayout;
    TaskStackViewCallbacks mCb;
    RecentsConfiguration mConfig;
    ArrayList<TaskViewTransform> mCurrentTaskTransforms;
    DebugOverlayView mDebugOverlay;
    TaskStackViewFilterAlgorithm mFilterAlgorithm;
    int mFocusedTaskIndex;
    LayoutInflater mInflater;
    TaskStackViewLayoutAlgorithm mLayoutAlgorithm;
    int mPrevAccessibilityFocusedIndex;
    ValueAnimator.AnimatorUpdateListener mRequestUpdateClippingListener;
    TaskStack mStack;
    TaskStackViewScroller mStackScroller;
    int mStackViewsAnimationDuration;
    boolean mStackViewsClipDirty;
    boolean mStackViewsDirty;
    boolean mStartEnterAnimationCompleted;
    ViewAnimation.TaskViewEnterContext mStartEnterAnimationContext;
    boolean mStartEnterAnimationRequestedAfterLayout;
    Rect mTaskStackBounds;
    float[] mTmpCoord;
    Matrix mTmpMatrix;
    Rect mTmpRect;
    HashMap<Task, TaskView> mTmpTaskViewMap;
    TaskViewTransform mTmpTransform;
    int[] mTmpVisibleRange;
    TaskStackViewTouchHandler mTouchHandler;
    DozeTrigger mUIDozeTrigger;
    ViewPool<TaskView, Task> mViewPool;

    interface TaskStackViewCallbacks {
        void onAllTaskViewsDismissed();

        void onTaskViewAppInfoClicked(Task task);

        void onTaskViewClicked(TaskStackView taskStackView, TaskView taskView, TaskStack taskStack, Task task, boolean z);

        void onTaskViewDismissed(Task task);
    }

    public TaskStackView(Context context, TaskStack stack) {
        super(context);
        this.mCurrentTaskTransforms = new ArrayList<>();
        this.mTaskStackBounds = new Rect();
        this.mFocusedTaskIndex = -1;
        this.mPrevAccessibilityFocusedIndex = -1;
        this.mStackViewsDirty = true;
        this.mStackViewsClipDirty = true;
        this.mAwaitingFirstLayout = true;
        this.mTmpVisibleRange = new int[2];
        this.mTmpCoord = new float[2];
        this.mTmpMatrix = new Matrix();
        this.mTmpRect = new Rect();
        this.mTmpTransform = new TaskViewTransform();
        this.mTmpTaskViewMap = new HashMap<>();
        this.mRequestUpdateClippingListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                TaskStackView.this.requestUpdateStackViewsClip();
            }
        };
        setStack(stack);
        this.mConfig = RecentsConfiguration.getInstance();
        this.mViewPool = new ViewPool<>(context, this);
        this.mInflater = LayoutInflater.from(context);
        this.mLayoutAlgorithm = new TaskStackViewLayoutAlgorithm(this.mConfig);
        this.mFilterAlgorithm = new TaskStackViewFilterAlgorithm(this.mConfig, this, this.mViewPool);
        this.mStackScroller = new TaskStackViewScroller(context, this.mConfig, this.mLayoutAlgorithm);
        this.mStackScroller.setCallbacks(this);
        this.mTouchHandler = new TaskStackViewTouchHandler(context, this, this.mConfig, this.mStackScroller);
        this.mUIDozeTrigger = new DozeTrigger(this.mConfig.taskBarDismissDozeDelaySeconds, new Runnable() {
            @Override
            public void run() {
                int childCount = TaskStackView.this.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    TaskView tv = (TaskView) TaskStackView.this.getChildAt(i);
                    tv.startNoUserInteractionAnimation();
                }
            }
        });
    }

    void setCallbacks(TaskStackViewCallbacks cb) {
        this.mCb = cb;
    }

    void setStack(TaskStack stack) {
        this.mStack = stack;
        if (this.mStack != null) {
            this.mStack.setCallbacks(this);
        }
        requestLayout();
    }

    public void setDebugOverlay(DebugOverlayView overlay) {
        this.mDebugOverlay = overlay;
    }

    void reset() {
        Iterator<TaskView> iter;
        resetFocusedTask();
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            TaskView tv = (TaskView) getChildAt(i);
            this.mViewPool.returnViewToPool(tv);
        }
        if (this.mViewPool != null && (iter = this.mViewPool.poolViewIterator()) != null) {
            while (iter.hasNext()) {
                TaskView tv2 = iter.next();
                tv2.reset();
            }
        }
        this.mStack.reset();
        this.mStackViewsDirty = true;
        this.mStackViewsClipDirty = true;
        this.mAwaitingFirstLayout = true;
        this.mPrevAccessibilityFocusedIndex = -1;
        if (this.mUIDozeTrigger != null) {
            this.mUIDozeTrigger.stopDozing();
            this.mUIDozeTrigger.resetTrigger();
        }
        this.mStackScroller.reset();
    }

    void requestSynchronizeStackViewsWithModel() {
        requestSynchronizeStackViewsWithModel(0);
    }

    void requestSynchronizeStackViewsWithModel(int duration) {
        if (!this.mStackViewsDirty) {
            invalidate();
            this.mStackViewsDirty = true;
        }
        if (this.mAwaitingFirstLayout) {
            this.mStackViewsAnimationDuration = 0;
        } else {
            this.mStackViewsAnimationDuration = Math.max(this.mStackViewsAnimationDuration, duration);
        }
    }

    void requestUpdateStackViewsClip() {
        if (!this.mStackViewsClipDirty) {
            invalidate();
            this.mStackViewsClipDirty = true;
        }
    }

    public TaskView getChildViewForTask(Task t) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            TaskView tv = (TaskView) getChildAt(i);
            if (tv.getTask() == t) {
                return tv;
            }
        }
        return null;
    }

    public TaskStackViewLayoutAlgorithm getStackAlgorithm() {
        return this.mLayoutAlgorithm;
    }

    private boolean updateStackTransforms(ArrayList<TaskViewTransform> taskTransforms, ArrayList<Task> tasks, float stackScroll, int[] visibleRangeOut, boolean boundTranslationsToRect) {
        int taskTransformCount = taskTransforms.size();
        int taskCount = tasks.size();
        int frontMostVisibleIndex = -1;
        int backMostVisibleIndex = -1;
        if (taskTransformCount < taskCount) {
            for (int i = taskTransformCount; i < taskCount; i++) {
                taskTransforms.add(new TaskViewTransform());
            }
        } else if (taskTransformCount > taskCount) {
            taskTransforms.subList(0, taskCount);
        }
        TaskViewTransform prevTransform = null;
        int i2 = taskCount - 1;
        while (true) {
            if (i2 < 0) {
                break;
            }
            TaskViewTransform transform = this.mLayoutAlgorithm.getStackTransform(tasks.get(i2), stackScroll, taskTransforms.get(i2), prevTransform);
            if (transform.visible) {
                if (frontMostVisibleIndex < 0) {
                    frontMostVisibleIndex = i2;
                }
                backMostVisibleIndex = i2;
            } else if (backMostVisibleIndex != -1) {
                while (i2 >= 0) {
                    taskTransforms.get(i2).reset();
                    i2--;
                }
            }
            if (boundTranslationsToRect) {
                transform.translationY = Math.min(transform.translationY, this.mLayoutAlgorithm.mViewRect.bottom);
            }
            prevTransform = transform;
            i2--;
        }
        if (visibleRangeOut != null) {
            visibleRangeOut[0] = frontMostVisibleIndex;
            visibleRangeOut[1] = backMostVisibleIndex;
        }
        return (frontMostVisibleIndex == -1 || backMostVisibleIndex == -1) ? false : true;
    }

    boolean synchronizeStackViewsWithModel() {
        if (!this.mStackViewsDirty) {
            return false;
        }
        RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
        SystemServicesProxy ssp = loader.getSystemServicesProxy();
        ArrayList<Task> tasks = this.mStack.getTasks();
        float stackScroll = this.mStackScroller.getStackScroll();
        int[] visibleRange = this.mTmpVisibleRange;
        boolean isValidVisibleRange = updateStackTransforms(this.mCurrentTaskTransforms, tasks, stackScroll, visibleRange, false);
        if (this.mDebugOverlay != null) {
            this.mDebugOverlay.setText("vis[" + visibleRange[1] + "-" + visibleRange[0] + "]");
        }
        this.mTmpTaskViewMap.clear();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            TaskView tv = (TaskView) getChildAt(i);
            Task task = tv.getTask();
            int taskIndex = this.mStack.indexOfTask(task);
            if (visibleRange[1] <= taskIndex && taskIndex <= visibleRange[0]) {
                this.mTmpTaskViewMap.put(task, tv);
            } else {
                this.mViewPool.returnViewToPool(tv);
            }
        }
        for (int i2 = visibleRange[0]; isValidVisibleRange && i2 >= visibleRange[1]; i2--) {
            Task task2 = tasks.get(i2);
            TaskViewTransform transform = this.mCurrentTaskTransforms.get(i2);
            TaskView tv2 = this.mTmpTaskViewMap.get(task2);
            int taskIndex2 = this.mStack.indexOfTask(task2);
            if (tv2 == null) {
                tv2 = this.mViewPool.pickUpViewFromPool(task2, task2);
                if (this.mStackViewsAnimationDuration > 0) {
                    if (Float.compare(transform.p, 0.0f) <= 0) {
                        this.mLayoutAlgorithm.getStackTransform(0.0f, 0.0f, this.mTmpTransform, (TaskViewTransform) null);
                    } else {
                        this.mLayoutAlgorithm.getStackTransform(1.0f, 0.0f, this.mTmpTransform, (TaskViewTransform) null);
                    }
                    tv2.updateViewPropertiesToTaskTransform(this.mTmpTransform, 0);
                }
            }
            tv2.updateViewPropertiesToTaskTransform(this.mCurrentTaskTransforms.get(taskIndex2), this.mStackViewsAnimationDuration, this.mRequestUpdateClippingListener);
            int childCount = getChildCount();
            if (childCount > 0 && ssp.isTouchExplorationEnabled()) {
                TaskView atv = (TaskView) getChildAt(childCount - 1);
                int indexOfTask = this.mStack.indexOfTask(atv.getTask());
                if (this.mPrevAccessibilityFocusedIndex != indexOfTask) {
                    tv2.requestAccessibilityFocus();
                    this.mPrevAccessibilityFocusedIndex = indexOfTask;
                }
            }
        }
        this.mStackViewsAnimationDuration = 0;
        this.mStackViewsDirty = false;
        this.mStackViewsClipDirty = true;
        return true;
    }

    void clipTaskViews() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            TaskView tv = (TaskView) getChildAt(i);
            TaskView nextTv = null;
            int clipBottom = 0;
            if (tv.shouldClipViewInStack()) {
                int nextIndex = i;
                while (true) {
                    if (nextIndex >= getChildCount()) {
                        break;
                    }
                    nextIndex++;
                    TaskView tmpTv = (TaskView) getChildAt(nextIndex);
                    if (tmpTv != null && tmpTv.shouldClipViewInStack()) {
                        nextTv = tmpTv;
                        break;
                    }
                }
                if (nextTv != null) {
                    float[] fArr = this.mTmpCoord;
                    this.mTmpCoord[1] = 0.0f;
                    fArr[0] = 0.0f;
                    Utilities.mapCoordInDescendentToSelf(nextTv, this, this.mTmpCoord, false);
                    Utilities.mapCoordInSelfToDescendent(tv, this, this.mTmpCoord, this.mTmpMatrix);
                    clipBottom = (int) Math.floor(((tv.getMeasuredHeight() - this.mTmpCoord[1]) - nextTv.getPaddingTop()) - 1.0f);
                }
            }
            tv.getViewBounds().setClipBottom(clipBottom);
        }
        if (getChildCount() > 0) {
            ((TaskView) getChildAt(getChildCount() - 1)).getViewBounds().setClipBottom(0);
        }
        this.mStackViewsClipDirty = false;
    }

    public void setStackInsetRect(Rect r) {
        this.mTaskStackBounds.set(r);
    }

    void updateMinMaxScroll(boolean boundScrollToNewMinMax, boolean launchedWithAltTab, boolean launchedFromHome) {
        this.mLayoutAlgorithm.computeMinMaxScroll(this.mStack.getTasks(), launchedWithAltTab, launchedFromHome);
        if (boundScrollToNewMinMax) {
            this.mStackScroller.boundScroll();
        }
    }

    public TaskStackViewScroller getScroller() {
        return this.mStackScroller;
    }

    void focusTask(int taskIndex, boolean scrollToNewPosition, final boolean animateFocusedState) {
        if (taskIndex != this.mFocusedTaskIndex && taskIndex >= 0 && taskIndex < this.mStack.getTaskCount()) {
            this.mFocusedTaskIndex = taskIndex;
            Task t = this.mStack.getTasks().get(taskIndex);
            TaskView tv = getChildViewForTask(t);
            Runnable postScrollRunnable = null;
            if (tv != null) {
                tv.setFocusedTask(animateFocusedState);
            } else {
                postScrollRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Task t2 = TaskStackView.this.mStack.getTasks().get(TaskStackView.this.mFocusedTaskIndex);
                        TaskView tv2 = TaskStackView.this.getChildViewForTask(t2);
                        if (tv2 != null) {
                            tv2.setFocusedTask(animateFocusedState);
                        }
                    }
                };
            }
            if (scrollToNewPosition) {
                float newScroll = this.mLayoutAlgorithm.getStackScrollForTask(t) - 0.5f;
                this.mStackScroller.animateScroll(this.mStackScroller.getStackScroll(), this.mStackScroller.getBoundedStackScroll(newScroll), postScrollRunnable);
            } else if (postScrollRunnable != null) {
                postScrollRunnable.run();
            }
        }
    }

    public boolean ensureFocusedTask() {
        if (this.mFocusedTaskIndex < 0) {
            int x = this.mLayoutAlgorithm.mStackVisibleRect.centerX();
            int y = this.mLayoutAlgorithm.mStackVisibleRect.centerY();
            int childCount = getChildCount();
            int i = childCount - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                TaskView tv = (TaskView) getChildAt(i);
                tv.getHitRect(this.mTmpRect);
                if (!this.mTmpRect.contains(x, y)) {
                    i--;
                } else {
                    this.mFocusedTaskIndex = this.mStack.indexOfTask(tv.getTask());
                    break;
                }
            }
            if (this.mFocusedTaskIndex < 0 && childCount > 0) {
                this.mFocusedTaskIndex = childCount - 1;
            }
        }
        return this.mFocusedTaskIndex >= 0;
    }

    public void focusNextTask(boolean forward, boolean animateFocusedState) {
        int numTasks = this.mStack.getTaskCount();
        if (numTasks != 0) {
            int direction = forward ? -1 : 1;
            int newIndex = this.mFocusedTaskIndex + direction;
            if (newIndex >= 0 && newIndex <= numTasks - 1) {
                focusTask(Math.max(0, Math.min(numTasks - 1, newIndex)), true, animateFocusedState);
            }
        }
    }

    public void dismissFocusedTask() {
        if (this.mFocusedTaskIndex < 0 || this.mFocusedTaskIndex >= this.mStack.getTaskCount()) {
            this.mFocusedTaskIndex = -1;
            return;
        }
        Task t = this.mStack.getTasks().get(this.mFocusedTaskIndex);
        TaskView tv = getChildViewForTask(t);
        tv.dismissTask();
    }

    void resetFocusedTask() {
        if (this.mFocusedTaskIndex >= 0 && this.mFocusedTaskIndex < this.mStack.getTaskCount()) {
            Task t = this.mStack.getTasks().get(this.mFocusedTaskIndex);
            TaskView tv = getChildViewForTask(t);
            if (tv != null) {
                tv.unsetFocusedTask();
            }
        }
        this.mFocusedTaskIndex = -1;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        int childCount = getChildCount();
        if (childCount > 0) {
            TaskView backMostTask = (TaskView) getChildAt(0);
            TaskView frontMostTask = (TaskView) getChildAt(childCount - 1);
            event.setFromIndex(this.mStack.indexOfTask(backMostTask.getTask()));
            event.setToIndex(this.mStack.indexOfTask(frontMostTask.getTask()));
            event.setContentDescription(frontMostTask.getTask().activityLabel);
        }
        event.setItemCount(this.mStack.getTaskCount());
        event.setScrollY(this.mStackScroller.mScroller.getCurrY());
        event.setMaxScrollY(this.mStackScroller.progressToScrollRange(this.mLayoutAlgorithm.mMaxScrollP));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mTouchHandler.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return this.mTouchHandler.onTouchEvent(ev);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        return this.mTouchHandler.onGenericMotionEvent(ev);
    }

    @Override
    public void computeScroll() {
        this.mStackScroller.computeScroll();
        synchronizeStackViewsWithModel();
        clipTaskViews();
        sendAccessibilityEvent(4096);
    }

    public void computeRects(int windowWidth, int windowHeight, Rect taskStackBounds, boolean launchedWithAltTab, boolean launchedFromHome) {
        this.mLayoutAlgorithm.computeRects(windowWidth, windowHeight, taskStackBounds);
        updateMinMaxScroll(false, launchedWithAltTab, launchedFromHome);
    }

    public void updateMinMaxScrollForStack(TaskStack stack, boolean launchedWithAltTab, boolean launchedFromHome) {
        this.mStack = stack;
        updateMinMaxScroll(false, launchedWithAltTab, launchedFromHome);
    }

    public TaskStackViewLayoutAlgorithm.VisibilityReport computeStackVisibilityReport() {
        return this.mLayoutAlgorithm.computeStackVisibilityReport(this.mStack.getTasks());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        Rect taskStackBounds = new Rect(this.mTaskStackBounds);
        taskStackBounds.bottom -= this.mConfig.systemInsets.bottom;
        computeRects(width, height, taskStackBounds, this.mConfig.launchedWithAltTab, this.mConfig.launchedFromHome);
        if (this.mAwaitingFirstLayout) {
            this.mStackScroller.setStackScrollToInitialState();
            requestSynchronizeStackViewsWithModel();
            synchronizeStackViewsWithModel();
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            TaskView tv = (TaskView) getChildAt(i);
            if (tv.getBackground() != null) {
                tv.getBackground().getPadding(this.mTmpRect);
            } else {
                this.mTmpRect.setEmpty();
            }
            tv.measure(View.MeasureSpec.makeMeasureSpec(this.mLayoutAlgorithm.mTaskRect.width() + this.mTmpRect.left + this.mTmpRect.right, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mLayoutAlgorithm.mTaskRect.height() + this.mTmpRect.top + this.mTmpRect.bottom, 1073741824));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            TaskView tv = (TaskView) getChildAt(i);
            if (tv.getBackground() != null) {
                tv.getBackground().getPadding(this.mTmpRect);
            } else {
                this.mTmpRect.setEmpty();
            }
            tv.layout(this.mLayoutAlgorithm.mTaskRect.left - this.mTmpRect.left, this.mLayoutAlgorithm.mTaskRect.top - this.mTmpRect.top, this.mLayoutAlgorithm.mTaskRect.right + this.mTmpRect.right, this.mLayoutAlgorithm.mTaskRect.bottom + this.mTmpRect.bottom);
        }
        if (this.mAwaitingFirstLayout) {
            this.mAwaitingFirstLayout = false;
            onFirstLayout();
        }
    }

    void onFirstLayout() {
        int offscreenY = this.mLayoutAlgorithm.mViewRect.bottom - (this.mLayoutAlgorithm.mTaskRect.top - this.mLayoutAlgorithm.mViewRect.top);
        Task launchTargetTask = null;
        int childCount = getChildCount();
        int i = childCount - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            Task task = ((TaskView) getChildAt(i)).getTask();
            if (task.isLaunchTarget) {
                launchTargetTask = task;
                break;
            }
            i--;
        }
        for (int i2 = childCount - 1; i2 >= 0; i2--) {
            TaskView tv = (TaskView) getChildAt(i2);
            Task task2 = tv.getTask();
            boolean occludesLaunchTarget = launchTargetTask != null && launchTargetTask.group.isTaskAboveTask(task2, launchTargetTask);
            tv.prepareEnterRecentsAnimation(task2.isLaunchTarget, occludesLaunchTarget, offscreenY);
        }
        if (this.mStartEnterAnimationRequestedAfterLayout) {
            startEnterRecentsAnimation(this.mStartEnterAnimationContext);
            this.mStartEnterAnimationRequestedAfterLayout = false;
            this.mStartEnterAnimationContext = null;
        }
        if (this.mConfig.launchedWithAltTab) {
            if (this.mConfig.launchedFromAppWithThumbnail) {
                focusTask(Math.max(0, this.mStack.getTaskCount() - 2), false, this.mConfig.launchedHasConfigurationChanged);
            } else {
                focusTask(Math.max(0, this.mStack.getTaskCount() - 1), false, this.mConfig.launchedHasConfigurationChanged);
            }
        }
        this.mUIDozeTrigger.startDozing();
    }

    public void startEnterRecentsAnimation(ViewAnimation.TaskViewEnterContext ctx) {
        if (this.mAwaitingFirstLayout) {
            this.mStartEnterAnimationRequestedAfterLayout = true;
            this.mStartEnterAnimationContext = ctx;
            return;
        }
        if (this.mStack.getTaskCount() > 0) {
            Task launchTargetTask = null;
            int childCount = getChildCount();
            int i = childCount - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                Task task = ((TaskView) getChildAt(i)).getTask();
                if (task.isLaunchTarget) {
                    launchTargetTask = task;
                    break;
                }
                i--;
            }
            for (int i2 = childCount - 1; i2 >= 0; i2--) {
                TaskView tv = (TaskView) getChildAt(i2);
                Task task2 = tv.getTask();
                ctx.currentTaskTransform = new TaskViewTransform();
                ctx.currentStackViewIndex = i2;
                ctx.currentStackViewCount = childCount;
                ctx.currentTaskRect = this.mLayoutAlgorithm.mTaskRect;
                ctx.currentTaskOccludesLaunchTarget = launchTargetTask != null && launchTargetTask.group.isTaskAboveTask(task2, launchTargetTask);
                ctx.updateListener = this.mRequestUpdateClippingListener;
                this.mLayoutAlgorithm.getStackTransform(task2, this.mStackScroller.getStackScroll(), ctx.currentTaskTransform, (TaskViewTransform) null);
                tv.startEnterRecentsAnimation(ctx);
            }
            ctx.postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                @Override
                public void run() {
                    View tv2;
                    TaskStackView.this.mStartEnterAnimationCompleted = true;
                    TaskStackView.this.mUIDozeTrigger.poke();
                    RecentsTaskLoader loader = RecentsTaskLoader.getInstance();
                    SystemServicesProxy ssp = loader.getSystemServicesProxy();
                    int childCount2 = TaskStackView.this.getChildCount();
                    if (childCount2 > 0 && ssp.isTouchExplorationEnabled()) {
                        TaskView tv3 = (TaskView) TaskStackView.this.getChildAt(childCount2 - 1);
                        tv3.requestAccessibilityFocus();
                        TaskStackView.this.mPrevAccessibilityFocusedIndex = TaskStackView.this.mStack.indexOfTask(tv3.getTask());
                    }
                    if (TaskStackView.this.mConfig.launchedWithAltTab && !TaskStackView.this.mConfig.launchedHasConfigurationChanged && (tv2 = TaskStackView.this.getChildAt(TaskStackView.this.mFocusedTaskIndex)) != null) {
                        ((TaskView) tv2).setFocusedTask(true);
                    }
                }
            });
        }
    }

    public void startExitToHomeAnimation(ViewAnimation.TaskViewExitContext ctx) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        ctx.offscreenTranslationY = this.mLayoutAlgorithm.mViewRect.bottom - (this.mLayoutAlgorithm.mTaskRect.top - this.mLayoutAlgorithm.mViewRect.top);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            TaskView tv = (TaskView) getChildAt(i);
            tv.startExitToHomeAnimation(ctx);
        }
    }

    public void startLaunchTaskAnimation(TaskView tv, Runnable r, boolean lockToTask) {
        Task launchTargetTask = tv.getTask();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            TaskView t = (TaskView) getChildAt(i);
            if (t == tv) {
                t.setClipViewInStack(false);
                t.startLaunchTaskAnimation(r, true, true, lockToTask);
            } else {
                boolean occludesLaunchTarget = launchTargetTask.group.isTaskAboveTask(t.getTask(), launchTargetTask);
                t.startLaunchTaskAnimation(null, false, occludesLaunchTarget, lockToTask);
            }
        }
    }

    void onRecentsHidden() {
        reset();
    }

    public boolean isTransformedTouchPointInView(float x, float y, View child) {
        return isTransformedTouchPointInView(x, y, child, null);
    }

    void onUserInteraction() {
        this.mUIDozeTrigger.poke();
    }

    @Override
    public void onStackTaskAdded(TaskStack stack, Task t) {
        requestSynchronizeStackViewsWithModel();
    }

    @Override
    public void onStackTaskRemoved(TaskStack stack, Task removedTask, Task newFrontMostTask) {
        TaskView frontTv;
        TaskView tv = getChildViewForTask(removedTask);
        if (tv != null) {
            this.mViewPool.returnViewToPool(tv);
        }
        this.mCb.onTaskViewDismissed(removedTask);
        Task anchorTask = null;
        float prevAnchorTaskScroll = 0.0f;
        boolean pullStackForward = stack.getTaskCount() > 0;
        if (pullStackForward) {
            anchorTask = this.mStack.getFrontMostTask();
            prevAnchorTaskScroll = this.mLayoutAlgorithm.getStackScrollForTask(anchorTask);
        }
        updateMinMaxScroll(true, this.mConfig.launchedWithAltTab, this.mConfig.launchedFromHome);
        if (pullStackForward) {
            float anchorTaskScroll = this.mLayoutAlgorithm.getStackScrollForTask(anchorTask);
            this.mStackScroller.setStackScroll(this.mStackScroller.getStackScroll() + (anchorTaskScroll - prevAnchorTaskScroll));
            this.mStackScroller.boundScroll();
        }
        requestSynchronizeStackViewsWithModel(200);
        if (newFrontMostTask != null && (frontTv = getChildViewForTask(newFrontMostTask)) != null) {
            frontTv.onTaskBound(newFrontMostTask);
            frontTv.fadeInActionButton(0, this.mConfig.taskViewEnterFromAppDuration);
        }
        if (this.mStack.getTaskCount() == 0) {
            boolean shouldFinishActivity = true;
            if (this.mStack.hasFilteredTasks()) {
                this.mStack.unfilterTasks();
                shouldFinishActivity = this.mStack.getTaskCount() == 0;
            }
            if (shouldFinishActivity) {
                this.mCb.onAllTaskViewsDismissed();
            }
        }
    }

    @Override
    public void onStackUnfiltered(TaskStack newStack, ArrayList<Task> curTasks) {
    }

    @Override
    public TaskView createView(Context context) {
        return (TaskView) this.mInflater.inflate(R.layout.recents_task_view, (ViewGroup) this, false);
    }

    @Override
    public void prepareViewToEnterPool(TaskView tv) {
        Task task = tv.getTask();
        if (tv.isAccessibilityFocused()) {
            tv.clearAccessibilityFocus();
        }
        RecentsTaskLoader.getInstance().unloadTaskData(task);
        detachViewFromParent(tv);
        tv.resetViewProperties();
        tv.setClipViewInStack(false);
    }

    @Override
    public void prepareViewToLeavePool(TaskView tv, Task task, boolean isNewView) {
        boolean requiresRelayout = tv.getWidth() <= 0 && !isNewView;
        tv.onTaskBound(task);
        RecentsTaskLoader.getInstance().loadTaskData(task);
        if (this.mUIDozeTrigger.hasTriggered()) {
            tv.setNoUserInteractionState();
        }
        if (this.mStartEnterAnimationCompleted) {
            tv.enableFocusAnimations();
        }
        int insertIndex = -1;
        int taskIndex = this.mStack.indexOfTask(task);
        if (taskIndex != -1) {
            int childCount = getChildCount();
            int i = 0;
            while (true) {
                if (i >= childCount) {
                    break;
                }
                Task tvTask = ((TaskView) getChildAt(i)).getTask();
                if (taskIndex >= this.mStack.indexOfTask(tvTask)) {
                    i++;
                } else {
                    insertIndex = i;
                    break;
                }
            }
        }
        if (isNewView) {
            addView(tv, insertIndex);
        } else {
            attachViewToParent(tv, insertIndex, tv.getLayoutParams());
            if (requiresRelayout) {
                tv.requestLayout();
            }
        }
        tv.setCallbacks(this);
        tv.setTouchEnabled(true);
        tv.setClipViewInStack(true);
    }

    @Override
    public boolean hasPreferredData(TaskView tv, Task preferredData) {
        return tv.getTask() == preferredData;
    }

    @Override
    public void onTaskViewAppInfoClicked(TaskView tv) {
        if (this.mCb != null) {
            this.mCb.onTaskViewAppInfoClicked(tv.getTask());
        }
    }

    @Override
    public void onTaskViewClicked(TaskView tv, Task task, boolean lockToTask) {
        this.mUIDozeTrigger.stopDozing();
        if (this.mCb != null) {
            this.mCb.onTaskViewClicked(this, tv, this.mStack, task, lockToTask);
        }
    }

    @Override
    public void onTaskViewDismissed(TaskView tv) {
        Task task = tv.getTask();
        int taskIndex = this.mStack.indexOfTask(task);
        boolean taskWasFocused = tv.isFocusedTask();
        tv.announceForAccessibility(getContext().getString(R.string.accessibility_recents_item_dismissed, tv.getTask().activityLabel));
        this.mStack.removeTask(task);
        if (taskWasFocused) {
            ArrayList<Task> tasks = this.mStack.getTasks();
            int nextTaskIndex = Math.min(tasks.size() - 1, taskIndex - 1);
            if (nextTaskIndex >= 0) {
                Task nextTask = tasks.get(nextTaskIndex);
                TaskView nextTv = getChildViewForTask(nextTask);
                if (nextTv != null) {
                    nextTv.setFocusedTask(this.mConfig.launchedWithAltTab);
                }
            }
        }
    }

    @Override
    public void onTaskViewClipStateChanged(TaskView tv) {
        if (!this.mStackViewsDirty) {
            invalidate();
        }
    }

    @Override
    public void onTaskViewFocusChanged(TaskView tv, boolean focused) {
        if (focused) {
            this.mFocusedTaskIndex = this.mStack.indexOfTask(tv.getTask());
        }
    }

    @Override
    public void onScrollChanged(float p) {
        this.mUIDozeTrigger.poke();
        requestSynchronizeStackViewsWithModel();
        postInvalidateOnAnimation();
    }

    @Override
    public void onPackagesChanged(RecentsPackageMonitor monitor, String packageName, int userId) {
        HashSet<ComponentName> removedComponents = monitor.computeComponentsRemoved(this.mStack.getTaskKeys(), packageName, userId);
        ArrayList<Task> tasks = this.mStack.getTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            final Task t = tasks.get(i);
            if (removedComponents.contains(t.key.baseIntent.getComponent())) {
                TaskView tv = getChildViewForTask(t);
                if (tv != null) {
                    tv.startDeleteTaskAnimation(new Runnable() {
                        @Override
                        public void run() {
                            TaskStackView.this.mStack.removeTask(t);
                        }
                    });
                } else {
                    this.mStack.removeTask(t);
                }
            }
        }
    }
}
