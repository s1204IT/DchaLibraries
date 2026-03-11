package com.android.systemui.recents.views;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.IntDef;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.MutableBoolean;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.RecentsDebugFlags;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.EnterRecentsTaskStackAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.HideStackActionButtonEvent;
import com.android.systemui.recents.events.activity.IterateRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.activity.LaunchTaskStartedEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.PackagesChangedEvent;
import com.android.systemui.recents.events.activity.ShowStackActionButtonEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.DismissTaskViewEvent;
import com.android.systemui.recents.events.ui.RecentsGrowingEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.events.ui.UpdateFreeformTaskViewVisibilityEvent;
import com.android.systemui.recents.events.ui.UserInteractionEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartInitializeDropTargetsEvent;
import com.android.systemui.recents.events.ui.focus.DismissFocusedTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusNextTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusPreviousTaskViewEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackViewScroller;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.recents.views.ViewPool;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class TaskStackView extends FrameLayout implements TaskStack.TaskStackCallbacks, TaskView.TaskViewCallbacks, TaskStackViewScroller.TaskStackViewScrollerCallbacks, TaskStackLayoutAlgorithm.TaskStackLayoutAlgorithmCallbacks, ViewPool.ViewPoolConsumer<TaskView, Task> {
    private TaskStackAnimationHelper mAnimationHelper;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mAwaitingFirstLayout;
    private ArrayList<TaskViewTransform> mCurrentTaskTransforms;
    private AnimationProps mDeferredTaskViewLayoutAnimation;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mDisplayOrientation;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mDisplayRect;
    private int mDividerSize;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mEnterAnimationComplete;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "focused_task_")
    private Task mFocusedTask;
    private GradientDrawable mFreeformWorkspaceBackground;
    private ObjectAnimator mFreeformWorkspaceBackgroundAnimator;
    private DropTarget mFreeformWorkspaceDropTarget;
    private ArraySet<Task.TaskKey> mIgnoreTasks;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mInMeasureLayout;
    private LayoutInflater mInflater;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialState;
    private int mLastHeight;
    private int mLastWidth;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "layout_")
    TaskStackLayoutAlgorithm mLayoutAlgorithm;
    private ValueAnimator.AnimatorUpdateListener mRequestUpdateClippingListener;
    private boolean mResetToInitialStateWhenResized;

    @ViewDebug.ExportedProperty(category = "recents")
    boolean mScreenPinningEnabled;
    private TaskStackLayoutAlgorithm mStableLayoutAlgorithm;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStableStackBounds;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStableWindowRect;
    private TaskStack mStack;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStackBounds;
    private DropTarget mStackDropTarget;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "scroller_")
    private TaskStackViewScroller mStackScroller;
    private int mStartTimerIndicatorDuration;
    private int mTaskCornerRadiusPx;
    private ArrayList<TaskView> mTaskViews;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mTaskViewsClipDirty;
    private int[] mTmpIntPair;
    private Rect mTmpRect;
    private ArrayMap<Task.TaskKey, TaskView> mTmpTaskViewMap;
    private List<TaskView> mTmpTaskViews;
    private TaskViewTransform mTmpTransform;

    @ViewDebug.ExportedProperty(category = "recents")
    boolean mTouchExplorationEnabled;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "touch_")
    private TaskStackViewTouchHandler mTouchHandler;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "doze_")
    private DozeTrigger mUIDozeTrigger;
    private ViewPool<TaskView, Task> mViewPool;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mWindowRect;

    @IntDef({0, 1, 2})
    @Retention(RetentionPolicy.SOURCE)
    public @interface InitialStateAction {
    }

    public TaskStackView(Context context) {
        super(context);
        this.mStack = new TaskStack();
        this.mTaskViews = new ArrayList<>();
        this.mCurrentTaskTransforms = new ArrayList<>();
        this.mIgnoreTasks = new ArraySet<>();
        this.mDeferredTaskViewLayoutAnimation = null;
        this.mTaskViewsClipDirty = true;
        this.mAwaitingFirstLayout = true;
        this.mInitialState = 1;
        this.mInMeasureLayout = false;
        this.mEnterAnimationComplete = false;
        this.mStableStackBounds = new Rect();
        this.mStackBounds = new Rect();
        this.mStableWindowRect = new Rect();
        this.mWindowRect = new Rect();
        this.mDisplayRect = new Rect();
        this.mDisplayOrientation = 0;
        this.mTmpRect = new Rect();
        this.mTmpTaskViewMap = new ArrayMap<>();
        this.mTmpTaskViews = new ArrayList();
        this.mTmpTransform = new TaskViewTransform();
        this.mTmpIntPair = new int[2];
        this.mRequestUpdateClippingListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (TaskStackView.this.mTaskViewsClipDirty) {
                    return;
                }
                TaskStackView.this.mTaskViewsClipDirty = true;
                TaskStackView.this.invalidate();
            }
        };
        this.mFreeformWorkspaceDropTarget = new DropTarget() {
            @Override
            public boolean acceptsDrop(int x, int y, int width, int height, boolean isCurrentTarget) {
                if (!isCurrentTarget) {
                    return TaskStackView.this.mLayoutAlgorithm.mFreeformRect.contains(x, y);
                }
                return false;
            }
        };
        this.mStackDropTarget = new DropTarget() {
            @Override
            public boolean acceptsDrop(int x, int y, int width, int height, boolean isCurrentTarget) {
                if (!isCurrentTarget) {
                    return TaskStackView.this.mLayoutAlgorithm.mStackRect.contains(x, y);
                }
                return false;
            }
        };
        SystemServicesProxy ssp = Recents.getSystemServices();
        Resources res = context.getResources();
        this.mStack.setCallbacks(this);
        this.mViewPool = new ViewPool<>(context, this);
        this.mInflater = LayoutInflater.from(context);
        this.mLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, this);
        this.mStableLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, null);
        this.mStackScroller = new TaskStackViewScroller(context, this, this.mLayoutAlgorithm);
        this.mTouchHandler = new TaskStackViewTouchHandler(context, this, this.mStackScroller);
        this.mAnimationHelper = new TaskStackAnimationHelper(context, this);
        this.mTaskCornerRadiusPx = res.getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        this.mDividerSize = ssp.getDockedDividerSize(context);
        this.mDisplayOrientation = Utilities.getAppConfiguration(this.mContext).orientation;
        this.mDisplayRect = ssp.getDisplayRect();
        int taskBarDismissDozeDelaySeconds = getResources().getInteger(R.integer.recents_task_bar_dismiss_delay_seconds);
        this.mUIDozeTrigger = new DozeTrigger(taskBarDismissDozeDelaySeconds, new Runnable() {
            @Override
            public void run() {
                List<TaskView> taskViews = TaskStackView.this.getTaskViews();
                int taskViewCount = taskViews.size();
                for (int i = 0; i < taskViewCount; i++) {
                    TaskView tv = taskViews.get(i);
                    tv.startNoUserInteractionAnimation();
                }
            }
        });
        setImportantForAccessibility(1);
        this.mFreeformWorkspaceBackground = (GradientDrawable) getContext().getDrawable(R.drawable.recents_freeform_workspace_bg);
        this.mFreeformWorkspaceBackground.setCallback(this);
        if (!ssp.hasFreeformWorkspaceSupport()) {
            return;
        }
        this.mFreeformWorkspaceBackground.setColor(getContext().getColor(R.color.recents_freeform_workspace_bg_color));
    }

    @Override
    protected void onAttachedToWindow() {
        EventBus.getDefault().register(this, 3);
        super.onAttachedToWindow();
        readSystemFlags();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    void onReload(boolean isResumingFromVisible) {
        if (!isResumingFromVisible) {
            resetFocusedTask(getFocusedTask());
        }
        List<TaskView> taskViews = new ArrayList<>();
        taskViews.addAll(getTaskViews());
        taskViews.addAll(this.mViewPool.getViews());
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            taskViews.get(i).onReload(isResumingFromVisible);
        }
        readSystemFlags();
        this.mTaskViewsClipDirty = true;
        this.mEnterAnimationComplete = false;
        this.mUIDozeTrigger.stopDozing();
        if (isResumingFromVisible) {
            int ffBgAlpha = this.mLayoutAlgorithm.getStackState().freeformBackgroundAlpha;
            animateFreeformWorkspaceBackgroundAlpha(ffBgAlpha, new AnimationProps(150, Interpolators.FAST_OUT_SLOW_IN));
        } else {
            this.mStackScroller.reset();
            this.mStableLayoutAlgorithm.reset();
            this.mLayoutAlgorithm.reset();
        }
        this.mAwaitingFirstLayout = true;
        this.mInitialState = 1;
        requestLayout();
    }

    public void setTasks(TaskStack stack, boolean allowNotifyStackChanges) {
        boolean isInitialized = this.mLayoutAlgorithm.isInitialized();
        TaskStack taskStack = this.mStack;
        Context context = getContext();
        ArrayList<Task> arrayListComputeAllTasksList = stack.computeAllTasksList();
        if (!allowNotifyStackChanges) {
            isInitialized = false;
        }
        taskStack.setTasks(context, arrayListComputeAllTasksList, isInitialized);
    }

    public TaskStack getStack() {
        return this.mStack;
    }

    public void updateToInitialState() {
        this.mStackScroller.setStackScrollToInitialState();
        this.mLayoutAlgorithm.setTaskOverridesForInitialState(this.mStack, false);
    }

    void updateTaskViewsList() {
        this.mTaskViews.clear();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            if (v instanceof TaskView) {
                this.mTaskViews.add((TaskView) v);
            }
        }
    }

    List<TaskView> getTaskViews() {
        return this.mTaskViews;
    }

    private TaskView getFrontMostTaskView(boolean stackTasksOnly) {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = taskViewCount - 1; i >= 0; i--) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!stackTasksOnly || !task.isFreeformTask()) {
                return tv;
            }
        }
        return null;
    }

    public TaskView getChildViewForTask(Task t) {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            if (tv.getTask() == t) {
                return tv;
            }
        }
        return null;
    }

    public TaskStackLayoutAlgorithm getStackAlgorithm() {
        return this.mLayoutAlgorithm;
    }

    public TaskStackViewTouchHandler getTouchHandler() {
        return this.mTouchHandler;
    }

    void addIgnoreTask(Task task) {
        this.mIgnoreTasks.add(task.key);
    }

    void removeIgnoreTask(Task task) {
        this.mIgnoreTasks.remove(task.key);
    }

    boolean isIgnoredTask(Task task) {
        return this.mIgnoreTasks.contains(task.key);
    }

    int[] computeVisibleTaskTransforms(ArrayList<TaskViewTransform> taskTransforms, ArrayList<Task> tasks, float curStackScroll, float targetStackScroll, ArraySet<Task.TaskKey> ignoreTasksSet, boolean ignoreTaskOverrides) {
        int taskCount = tasks.size();
        int[] visibleTaskRange = this.mTmpIntPair;
        visibleTaskRange[0] = -1;
        visibleTaskRange[1] = -1;
        boolean useTargetStackScroll = Float.compare(curStackScroll, targetStackScroll) != 0;
        Utilities.matchTaskListSize(tasks, taskTransforms);
        TaskViewTransform frontTransform = null;
        TaskViewTransform frontTransformAtTarget = null;
        TaskViewTransform transformAtTarget = null;
        for (int i = taskCount - 1; i >= 0; i--) {
            Task task = tasks.get(i);
            TaskViewTransform transform = this.mLayoutAlgorithm.getStackTransform(task, curStackScroll, taskTransforms.get(i), frontTransform, ignoreTaskOverrides);
            if (useTargetStackScroll && !transform.visible) {
                transformAtTarget = this.mLayoutAlgorithm.getStackTransform(task, targetStackScroll, new TaskViewTransform(), frontTransformAtTarget);
                if (transformAtTarget.visible) {
                    transform.copyFrom(transformAtTarget);
                }
            }
            if (!ignoreTasksSet.contains(task.key) && !task.isFreeformTask()) {
                frontTransform = transform;
                frontTransformAtTarget = transformAtTarget;
                if (transform.visible) {
                    if (visibleTaskRange[0] < 0) {
                        visibleTaskRange[0] = i;
                    }
                    visibleTaskRange[1] = i;
                }
            }
        }
        return visibleTaskRange;
    }

    void bindVisibleTaskViews(float targetStackScroll) {
        bindVisibleTaskViews(targetStackScroll, false);
    }

    void bindVisibleTaskViews(float targetStackScroll, boolean ignoreTaskOverrides) {
        int newFocusedTaskIndex;
        ArrayList<Task> tasks = this.mStack.getStackTasks();
        int[] visibleTaskRange = computeVisibleTaskTransforms(this.mCurrentTaskTransforms, tasks, this.mStackScroller.getStackScroll(), targetStackScroll, this.mIgnoreTasks, ignoreTaskOverrides);
        this.mTmpTaskViewMap.clear();
        List<TaskView> taskViews = getTaskViews();
        int lastFocusedTaskIndex = -1;
        int taskViewCount = taskViews.size();
        for (int i = taskViewCount - 1; i >= 0; i--) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!this.mIgnoreTasks.contains(task.key)) {
                int taskIndex = this.mStack.indexOfStackTask(task);
                TaskViewTransform transform = null;
                if (taskIndex != -1) {
                    transform = this.mCurrentTaskTransforms.get(taskIndex);
                }
                if (task.isFreeformTask() || (transform != null && transform.visible)) {
                    this.mTmpTaskViewMap.put(task.key, tv);
                } else {
                    if (this.mTouchExplorationEnabled && Utilities.isDescendentAccessibilityFocused(tv)) {
                        lastFocusedTaskIndex = taskIndex;
                        resetFocusedTask(task);
                    }
                    this.mViewPool.returnViewToPool(tv);
                }
            }
        }
        for (int i2 = tasks.size() - 1; i2 >= 0; i2--) {
            Task task2 = tasks.get(i2);
            TaskViewTransform transform2 = this.mCurrentTaskTransforms.get(i2);
            if (!this.mIgnoreTasks.contains(task2.key) && (task2.isFreeformTask() || transform2.visible)) {
                TaskView tv2 = this.mTmpTaskViewMap.get(task2.key);
                if (tv2 == null) {
                    TaskView tv3 = this.mViewPool.pickUpViewFromPool(task2, task2);
                    if (task2.isFreeformTask()) {
                        updateTaskViewToTransform(tv3, transform2, AnimationProps.IMMEDIATE);
                    } else if (transform2.rect.top <= this.mLayoutAlgorithm.mStackRect.top) {
                        updateTaskViewToTransform(tv3, this.mLayoutAlgorithm.getBackOfStackTransform(), AnimationProps.IMMEDIATE);
                    } else {
                        updateTaskViewToTransform(tv3, this.mLayoutAlgorithm.getFrontOfStackTransform(), AnimationProps.IMMEDIATE);
                    }
                } else {
                    int insertIndex = findTaskViewInsertIndex(task2, this.mStack.indexOfStackTask(task2));
                    if (insertIndex != getTaskViews().indexOf(tv2)) {
                        detachViewFromParent(tv2);
                        attachViewToParent(tv2, insertIndex, tv2.getLayoutParams());
                        updateTaskViewsList();
                    }
                }
            }
        }
        if (lastFocusedTaskIndex == -1) {
            return;
        }
        if (lastFocusedTaskIndex < visibleTaskRange[1]) {
            newFocusedTaskIndex = visibleTaskRange[1];
        } else {
            newFocusedTaskIndex = visibleTaskRange[0];
        }
        setFocusedTask(newFocusedTaskIndex, false, true);
        TaskView focusedTaskView = getChildViewForTask(this.mFocusedTask);
        if (focusedTaskView == null) {
            return;
        }
        focusedTaskView.requestAccessibilityFocus();
    }

    public void relayoutTaskViews(AnimationProps animation) {
        relayoutTaskViews(animation, null, false);
    }

    private void relayoutTaskViews(AnimationProps animation, ArrayMap<Task, AnimationProps> animationOverrides, boolean ignoreTaskOverrides) {
        cancelDeferredTaskViewLayoutAnimation();
        bindVisibleTaskViews(this.mStackScroller.getStackScroll(), ignoreTaskOverrides);
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            int taskIndex = this.mStack.indexOfStackTask(task);
            if (taskIndex != -1) {
                TaskViewTransform transform = this.mCurrentTaskTransforms.get(taskIndex);
                if (!this.mIgnoreTasks.contains(task.key)) {
                    if (animationOverrides != null && animationOverrides.containsKey(task)) {
                        AnimationProps animation2 = animationOverrides.get(task);
                        animation = animation2;
                    }
                    updateTaskViewToTransform(tv, transform, animation);
                }
            }
        }
    }

    void relayoutTaskViewsOnNextFrame(AnimationProps animation) {
        this.mDeferredTaskViewLayoutAnimation = animation;
        invalidate();
    }

    public void updateTaskViewToTransform(TaskView taskView, TaskViewTransform transform, AnimationProps animation) {
        if (taskView.isAnimatingTo(transform)) {
            return;
        }
        taskView.cancelTransformAnimation();
        taskView.updateViewPropertiesToTaskTransform(transform, animation, this.mRequestUpdateClippingListener);
    }

    public void getCurrentTaskTransforms(ArrayList<Task> tasks, ArrayList<TaskViewTransform> transformsOut) {
        Utilities.matchTaskListSize(tasks, transformsOut);
        int focusState = this.mLayoutAlgorithm.getFocusState();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task task = tasks.get(i);
            TaskViewTransform transform = transformsOut.get(i);
            TaskView tv = getChildViewForTask(task);
            if (tv != null) {
                transform.fillIn(tv);
            } else {
                this.mLayoutAlgorithm.getStackTransform(task, this.mStackScroller.getStackScroll(), focusState, transform, null, true, false);
            }
            transform.visible = true;
        }
    }

    public void getLayoutTaskTransforms(float stackScroll, int focusState, ArrayList<Task> tasks, boolean ignoreTaskOverrides, ArrayList<TaskViewTransform> transformsOut) {
        Utilities.matchTaskListSize(tasks, transformsOut);
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task task = tasks.get(i);
            TaskViewTransform transform = transformsOut.get(i);
            this.mLayoutAlgorithm.getStackTransform(task, stackScroll, focusState, transform, null, true, ignoreTaskOverrides);
            transform.visible = true;
        }
    }

    void cancelDeferredTaskViewLayoutAnimation() {
        this.mDeferredTaskViewLayoutAnimation = null;
    }

    void cancelAllTaskViewAnimations() {
        List<TaskView> taskViews = getTaskViews();
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            TaskView tv = taskViews.get(i);
            if (!this.mIgnoreTasks.contains(tv.getTask().key)) {
                tv.cancelTransformAnimation();
            }
        }
    }

    private void clipTaskViews() {
        List<TaskView> taskViews = getTaskViews();
        TaskView prevVisibleTv = null;
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            TaskView frontTv = null;
            int clipBottom = 0;
            if (isIgnoredTask(tv.getTask()) && prevVisibleTv != null) {
                tv.setTranslationZ(Math.max(tv.getTranslationZ(), prevVisibleTv.getTranslationZ() + 0.1f));
            }
            if (i < taskViewCount - 1 && tv.shouldClipViewInStack()) {
                int j = i + 1;
                while (true) {
                    if (j >= taskViewCount) {
                        break;
                    }
                    TaskView tmpTv = taskViews.get(j);
                    if (!tmpTv.shouldClipViewInStack()) {
                        j++;
                    } else {
                        frontTv = tmpTv;
                        break;
                    }
                }
                if (frontTv != null) {
                    float taskBottom = tv.getBottom();
                    float frontTaskTop = frontTv.getTop();
                    if (frontTaskTop < taskBottom) {
                        clipBottom = ((int) (taskBottom - frontTaskTop)) - this.mTaskCornerRadiusPx;
                    }
                }
            }
            tv.getViewBounds().setClipBottom(clipBottom);
            tv.mThumbnailView.updateThumbnailVisibility(clipBottom - tv.getPaddingBottom());
            prevVisibleTv = tv;
        }
        this.mTaskViewsClipDirty = false;
    }

    public void updateLayoutAlgorithm(boolean boundScrollToNewMinMax) {
        this.mLayoutAlgorithm.update(this.mStack, this.mIgnoreTasks);
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.hasFreeformWorkspaceSupport()) {
            this.mTmpRect.set(this.mLayoutAlgorithm.mFreeformRect);
            this.mFreeformWorkspaceBackground.setBounds(this.mTmpRect);
        }
        if (!boundScrollToNewMinMax) {
            return;
        }
        this.mStackScroller.boundScroll();
    }

    private void updateLayoutToStableBounds() {
        this.mWindowRect.set(this.mStableWindowRect);
        this.mStackBounds.set(this.mStableStackBounds);
        this.mLayoutAlgorithm.setSystemInsets(this.mStableLayoutAlgorithm.mSystemInsets);
        this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(this.mStack));
        updateLayoutAlgorithm(true);
    }

    public TaskStackViewScroller getScroller() {
        return this.mStackScroller;
    }

    public boolean setFocusedTask(int taskIndex, boolean scrollToTask, boolean requestViewFocus) {
        return setFocusedTask(taskIndex, scrollToTask, requestViewFocus, 0);
    }

    private boolean setFocusedTask(int focusTaskIndex, boolean scrollToTask, boolean requestViewFocus, int timerIndicatorDuration) {
        TaskView tv;
        int newFocusedTaskIndex = this.mStack.getTaskCount() > 0 ? Utilities.clamp(focusTaskIndex, 0, this.mStack.getTaskCount() - 1) : -1;
        Task task = newFocusedTaskIndex != -1 ? this.mStack.getStackTasks().get(newFocusedTaskIndex) : null;
        if (this.mFocusedTask != null) {
            if (timerIndicatorDuration > 0 && (tv = getChildViewForTask(this.mFocusedTask)) != null) {
                tv.getHeaderView().cancelFocusTimerIndicator();
            }
            resetFocusedTask(this.mFocusedTask);
        }
        this.mFocusedTask = task;
        if (task == null) {
            return false;
        }
        if (timerIndicatorDuration > 0) {
            TaskView tv2 = getChildViewForTask(this.mFocusedTask);
            if (tv2 != null) {
                tv2.getHeaderView().startFocusTimerIndicator(timerIndicatorDuration);
            } else {
                this.mStartTimerIndicatorDuration = timerIndicatorDuration;
            }
        }
        if (scrollToTask) {
            if (!this.mEnterAnimationComplete) {
                cancelAllTaskViewAnimations();
            }
            this.mLayoutAlgorithm.clearUnfocusedTaskOverrides();
            boolean willScroll = this.mAnimationHelper.startScrollToFocusedTaskAnimation(task, requestViewFocus);
            return willScroll;
        }
        TaskView newFocusedTaskView = getChildViewForTask(task);
        if (newFocusedTaskView == null) {
            return false;
        }
        newFocusedTaskView.setFocusedState(true, requestViewFocus);
        return false;
    }

    public void setRelativeFocusedTask(boolean forward, boolean stackTasksOnly, boolean animated) {
        setRelativeFocusedTask(forward, stackTasksOnly, animated, false, 0);
    }

    public void setRelativeFocusedTask(boolean forward, boolean stackTasksOnly, boolean animated, boolean cancelWindowAnimations, int timerIndicatorDuration) {
        Task focusedTask = getFocusedTask();
        int newIndex = this.mStack.indexOfStackTask(focusedTask);
        if (focusedTask != null) {
            if (stackTasksOnly) {
                List<Task> tasks = this.mStack.getStackTasks();
                if (focusedTask.isFreeformTask()) {
                    TaskView tv = getFrontMostTaskView(stackTasksOnly);
                    if (tv != null) {
                        newIndex = this.mStack.indexOfStackTask(tv.getTask());
                    }
                } else {
                    int tmpNewIndex = newIndex + (forward ? -1 : 1);
                    if (tmpNewIndex >= 0 && tmpNewIndex < tasks.size()) {
                        Task t = tasks.get(tmpNewIndex);
                        if (!t.isFreeformTask()) {
                            newIndex = tmpNewIndex;
                        }
                    }
                }
            } else {
                int taskCount = this.mStack.getTaskCount();
                newIndex = (((forward ? -1 : 1) + newIndex) + taskCount) % taskCount;
            }
        } else {
            float stackScroll = this.mStackScroller.getStackScroll();
            ArrayList<Task> tasks2 = this.mStack.getStackTasks();
            int taskCount2 = tasks2.size();
            if (forward) {
                newIndex = taskCount2 - 1;
                while (newIndex >= 0) {
                    float taskP = this.mLayoutAlgorithm.getStackScrollForTask(tasks2.get(newIndex));
                    if (Float.compare(taskP, stackScroll) <= 0) {
                        break;
                    } else {
                        newIndex--;
                    }
                }
            } else {
                newIndex = 0;
                while (newIndex < taskCount2) {
                    float taskP2 = this.mLayoutAlgorithm.getStackScrollForTask(tasks2.get(newIndex));
                    if (Float.compare(taskP2, stackScroll) >= 0) {
                        break;
                    } else {
                        newIndex++;
                    }
                }
            }
        }
        if (newIndex == -1) {
            return;
        }
        boolean willScroll = setFocusedTask(newIndex, true, true, timerIndicatorDuration);
        if (!willScroll || !cancelWindowAnimations) {
            return;
        }
        EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(null));
    }

    void resetFocusedTask(Task task) {
        TaskView tv;
        if (task != null && (tv = getChildViewForTask(task)) != null) {
            tv.setFocusedState(false, false);
        }
        this.mFocusedTask = null;
    }

    Task getFocusedTask() {
        return this.mFocusedTask;
    }

    Task getAccessibilityFocusedTask() {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            if (Utilities.isDescendentAccessibilityFocused(tv)) {
                return tv.getTask();
            }
        }
        TaskView frontTv = getFrontMostTaskView(true);
        if (frontTv != null) {
            return frontTv.getTask();
        }
        return null;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        if (taskViewCount > 0) {
            TaskView backMostTask = taskViews.get(0);
            TaskView frontMostTask = taskViews.get(taskViewCount - 1);
            event.setFromIndex(this.mStack.indexOfStackTask(backMostTask.getTask()));
            event.setToIndex(this.mStack.indexOfStackTask(frontMostTask.getTask()));
            event.setContentDescription(frontMostTask.getTask().title);
        }
        event.setItemCount(this.mStack.getTaskCount());
        int stackHeight = this.mLayoutAlgorithm.mStackRect.height();
        event.setScrollY((int) (this.mStackScroller.getStackScroll() * stackHeight));
        event.setMaxScrollY((int) (this.mLayoutAlgorithm.mMaxScrollP * stackHeight));
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        if (taskViewCount <= 1) {
            return;
        }
        Task focusedTask = getAccessibilityFocusedTask();
        info.setScrollable(true);
        int focusedTaskIndex = this.mStack.indexOfStackTask(focusedTask);
        if (focusedTaskIndex > 0) {
            info.addAction(8192);
        }
        if (focusedTaskIndex < 0 || focusedTaskIndex >= this.mStack.getTaskCount() - 1) {
            return;
        }
        info.addAction(4096);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ScrollView.class.getName();
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }
        Task focusedTask = getAccessibilityFocusedTask();
        int taskIndex = this.mStack.indexOfStackTask(focusedTask);
        if (taskIndex >= 0 && taskIndex < this.mStack.getTaskCount()) {
            switch (action) {
                case 4096:
                    setFocusedTask(taskIndex + 1, true, true, 0);
                    return true;
                case 8192:
                    setFocusedTask(taskIndex - 1, true, true, 0);
                    return true;
            }
        }
        return false;
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
        if (this.mStackScroller.computeScroll()) {
            sendAccessibilityEvent(4096);
        }
        if (this.mDeferredTaskViewLayoutAnimation != null) {
            relayoutTaskViews(this.mDeferredTaskViewLayoutAnimation);
            this.mTaskViewsClipDirty = true;
            this.mDeferredTaskViewLayoutAnimation = null;
        }
        if (!this.mTaskViewsClipDirty) {
            return;
        }
        clipTaskViews();
    }

    public TaskStackLayoutAlgorithm.VisibilityReport computeStackVisibilityReport() {
        return this.mLayoutAlgorithm.computeStackVisibilityReport(this.mStack.getStackTasks());
    }

    public void setSystemInsets(Rect systemInsets) {
        boolean changed = this.mStableLayoutAlgorithm.setSystemInsets(systemInsets);
        if (!(changed | this.mLayoutAlgorithm.setSystemInsets(systemInsets))) {
            return;
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean resetToInitialState;
        this.mInMeasureLayout = true;
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        this.mLayoutAlgorithm.getTaskStackBounds(this.mDisplayRect, new Rect(0, 0, width, height), this.mLayoutAlgorithm.mSystemInsets.top, this.mLayoutAlgorithm.mSystemInsets.right, this.mTmpRect);
        if (!this.mTmpRect.equals(this.mStableStackBounds)) {
            this.mStableStackBounds.set(this.mTmpRect);
            this.mStackBounds.set(this.mTmpRect);
            this.mStableWindowRect.set(0, 0, width, height);
            this.mWindowRect.set(0, 0, width, height);
        }
        this.mStableLayoutAlgorithm.initialize(this.mDisplayRect, this.mStableWindowRect, this.mStableStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(this.mStack));
        this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(this.mStack));
        updateLayoutAlgorithm(false);
        if (width == this.mLastWidth && height == this.mLastHeight) {
            resetToInitialState = false;
        } else {
            resetToInitialState = this.mResetToInitialStateWhenResized;
        }
        if (this.mAwaitingFirstLayout || this.mInitialState != 0 || resetToInitialState) {
            if (this.mInitialState != 2 || resetToInitialState) {
                updateToInitialState();
                this.mResetToInitialStateWhenResized = false;
            }
            if (!this.mAwaitingFirstLayout) {
                this.mInitialState = 0;
            }
        }
        bindVisibleTaskViews(this.mStackScroller.getStackScroll(), false);
        this.mTmpTaskViews.clear();
        this.mTmpTaskViews.addAll(getTaskViews());
        this.mTmpTaskViews.addAll(this.mViewPool.getViews());
        int taskViewCount = this.mTmpTaskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            measureTaskView(this.mTmpTaskViews.get(i));
        }
        setMeasuredDimension(width, height);
        this.mLastWidth = width;
        this.mLastHeight = height;
        this.mInMeasureLayout = false;
    }

    private void measureTaskView(TaskView tv) {
        Rect padding = new Rect();
        if (tv.getBackground() != null) {
            tv.getBackground().getPadding(padding);
        }
        this.mTmpRect.set(this.mStableLayoutAlgorithm.mTaskRect);
        this.mTmpRect.union(this.mLayoutAlgorithm.mTaskRect);
        tv.measure(View.MeasureSpec.makeMeasureSpec(this.mTmpRect.width() + padding.left + padding.right, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mTmpRect.height() + padding.top + padding.bottom, 1073741824));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mTmpTaskViews.clear();
        this.mTmpTaskViews.addAll(getTaskViews());
        this.mTmpTaskViews.addAll(this.mViewPool.getViews());
        int taskViewCount = this.mTmpTaskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            layoutTaskView(changed, this.mTmpTaskViews.get(i));
        }
        if (changed && this.mStackScroller.isScrollOutOfBounds()) {
            this.mStackScroller.boundScroll();
        }
        relayoutTaskViews(AnimationProps.IMMEDIATE);
        clipTaskViews();
        if (!this.mAwaitingFirstLayout && this.mEnterAnimationComplete) {
            return;
        }
        this.mAwaitingFirstLayout = false;
        this.mInitialState = 0;
        onFirstLayout();
    }

    private void layoutTaskView(boolean changed, TaskView tv) {
        if (changed) {
            Rect padding = new Rect();
            if (tv.getBackground() != null) {
                tv.getBackground().getPadding(padding);
            }
            this.mTmpRect.set(this.mStableLayoutAlgorithm.mTaskRect);
            this.mTmpRect.union(this.mLayoutAlgorithm.mTaskRect);
            tv.cancelTransformAnimation();
            tv.layout(this.mTmpRect.left - padding.left, this.mTmpRect.top - padding.top, this.mTmpRect.right + padding.right, this.mTmpRect.bottom + padding.bottom);
            return;
        }
        tv.layout(tv.getLeft(), tv.getTop(), tv.getRight(), tv.getBottom());
    }

    void onFirstLayout() {
        this.mAnimationHelper.prepareForEnterAnimation();
        int ffBgAlpha = this.mLayoutAlgorithm.getStackState().freeformBackgroundAlpha;
        animateFreeformWorkspaceBackgroundAlpha(ffBgAlpha, new AnimationProps(150, Interpolators.FAST_OUT_SLOW_IN));
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        int focusedTaskIndex = launchState.getInitialFocusTaskIndex(this.mStack.getTaskCount());
        if (focusedTaskIndex != -1) {
            setFocusedTask(focusedTaskIndex, false, false);
        }
        if (this.mStackScroller.getStackScroll() < 0.3f && this.mStack.getTaskCount() > 0) {
            EventBus.getDefault().send(new ShowStackActionButtonEvent(false));
        } else {
            EventBus.getDefault().send(new HideStackActionButtonEvent());
        }
    }

    public boolean isTouchPointInView(float x, float y, TaskView tv) {
        this.mTmpRect.set(tv.getLeft(), tv.getTop(), tv.getRight(), tv.getBottom());
        this.mTmpRect.offset((int) tv.getTranslationX(), (int) tv.getTranslationY());
        return this.mTmpRect.contains((int) x, (int) y);
    }

    public Task findAnchorTask(List<Task> tasks, MutableBoolean isFrontMostTask) {
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task task = tasks.get(i);
            if (isIgnoredTask(task)) {
                if (i == tasks.size() - 1) {
                    isFrontMostTask.value = true;
                }
            } else {
                return task;
            }
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (!ssp.hasFreeformWorkspaceSupport() || this.mFreeformWorkspaceBackground.getAlpha() <= 0) {
            return;
        }
        this.mFreeformWorkspaceBackground.draw(canvas);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        if (who == this.mFreeformWorkspaceBackground) {
            return true;
        }
        return super.verifyDrawable(who);
    }

    public boolean launchFreeformTasks() {
        Task frontTask;
        ArrayList<Task> tasks = this.mStack.getFreeformTasks();
        if (tasks.isEmpty() || (frontTask = tasks.get(tasks.size() - 1)) == null || !frontTask.isFreeformTask()) {
            return false;
        }
        EventBus.getDefault().send(new LaunchTaskEvent(getChildViewForTask(frontTask), frontTask, null, -1, false));
        return true;
    }

    @Override
    public void onStackTaskAdded(TaskStack stack, Task newTask) {
        AnimationProps animationProps;
        updateLayoutAlgorithm(true);
        if (this.mAwaitingFirstLayout) {
            animationProps = AnimationProps.IMMEDIATE;
        } else {
            animationProps = new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN);
        }
        relayoutTaskViews(animationProps);
    }

    @Override
    public void onStackTaskRemoved(TaskStack stack, Task removedTask, Task newFrontMostTask, AnimationProps animation, boolean fromDockGesture) {
        int i;
        TaskView frontTv;
        if (this.mFocusedTask == removedTask) {
            resetFocusedTask(removedTask);
        }
        TaskView tv = getChildViewForTask(removedTask);
        if (tv != null) {
            this.mViewPool.returnViewToPool(tv);
        }
        removeIgnoreTask(removedTask);
        if (animation != null) {
            updateLayoutAlgorithm(true);
            relayoutTaskViews(animation);
        }
        if (this.mScreenPinningEnabled && newFrontMostTask != null && (frontTv = getChildViewForTask(newFrontMostTask)) != null) {
            frontTv.showActionButton(true, 200);
        }
        if (this.mStack.getTaskCount() != 0) {
            return;
        }
        EventBus eventBus = EventBus.getDefault();
        if (fromDockGesture) {
            i = R.string.recents_empty_message;
        } else {
            i = R.string.recents_empty_message_dismissed_all;
        }
        eventBus.send(new AllTaskViewsDismissedEvent(i));
    }

    @Override
    public void onStackTasksRemoved(TaskStack stack) {
        resetFocusedTask(getFocusedTask());
        List<TaskView> taskViews = new ArrayList<>();
        taskViews.addAll(getTaskViews());
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            this.mViewPool.returnViewToPool(taskViews.get(i));
        }
        this.mIgnoreTasks.clear();
        EventBus.getDefault().send(new AllTaskViewsDismissedEvent(R.string.recents_empty_message_dismissed_all));
    }

    @Override
    public void onStackTasksUpdated(TaskStack stack) {
        updateLayoutAlgorithm(false);
        relayoutTaskViews(AnimationProps.IMMEDIATE);
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            bindTaskView(tv, tv.getTask());
        }
    }

    @Override
    public TaskView createView(Context context) {
        return (TaskView) this.mInflater.inflate(R.layout.recents_task_view, (ViewGroup) this, false);
    }

    @Override
    public void onReturnViewToPool(TaskView tv) {
        Task task = tv.getTask();
        unbindTaskView(tv, task);
        tv.clearAccessibilityFocus();
        tv.resetViewProperties();
        tv.setFocusedState(false, false);
        tv.setClipViewInStack(false);
        if (this.mScreenPinningEnabled) {
            tv.hideActionButton(false, 0, false, null);
        }
        detachViewFromParent(tv);
        updateTaskViewsList();
    }

    @Override
    public void onPickUpViewFromPool(TaskView tv, Task task, boolean isNewView) {
        int taskIndex = this.mStack.indexOfStackTask(task);
        int insertIndex = findTaskViewInsertIndex(task, taskIndex);
        if (isNewView) {
            if (this.mInMeasureLayout) {
                addView(tv, insertIndex);
            } else {
                ViewGroup.LayoutParams params = tv.getLayoutParams();
                if (params == null) {
                    params = generateDefaultLayoutParams();
                }
                addViewInLayout(tv, insertIndex, params, true);
                measureTaskView(tv);
                layoutTaskView(true, tv);
            }
        } else {
            attachViewToParent(tv, insertIndex, tv.getLayoutParams());
        }
        updateTaskViewsList();
        bindTaskView(tv, task);
        if (this.mUIDozeTrigger.isAsleep()) {
            tv.setNoUserInteractionState();
        }
        tv.setCallbacks(this);
        tv.setTouchEnabled(true);
        tv.setClipViewInStack(true);
        if (this.mFocusedTask == task) {
            tv.setFocusedState(true, false);
            if (this.mStartTimerIndicatorDuration > 0) {
                tv.getHeaderView().startFocusTimerIndicator(this.mStartTimerIndicatorDuration);
                this.mStartTimerIndicatorDuration = 0;
            }
        }
        if (!this.mScreenPinningEnabled || tv.getTask() != this.mStack.getStackFrontMostTask(false)) {
            return;
        }
        tv.showActionButton(false, 0);
    }

    @Override
    public boolean hasPreferredData(TaskView tv, Task preferredData) {
        return tv.getTask() == preferredData;
    }

    private void bindTaskView(TaskView tv, Task task) {
        tv.onTaskBound(task, this.mTouchExplorationEnabled, this.mDisplayOrientation, this.mDisplayRect);
        Recents.getTaskLoader().loadTaskData(task);
    }

    private void unbindTaskView(TaskView tv, Task task) {
        Recents.getTaskLoader().unloadTaskData(task);
    }

    @Override
    public void onTaskViewClipStateChanged(TaskView tv) {
        if (this.mTaskViewsClipDirty) {
            return;
        }
        this.mTaskViewsClipDirty = true;
        invalidate();
    }

    @Override
    public void onFocusStateChanged(int prevFocusState, int curFocusState) {
        if (this.mDeferredTaskViewLayoutAnimation != null) {
            return;
        }
        this.mUIDozeTrigger.poke();
        relayoutTaskViewsOnNextFrame(AnimationProps.IMMEDIATE);
    }

    @Override
    public void onStackScrollChanged(float prevScroll, float curScroll, AnimationProps animation) {
        this.mUIDozeTrigger.poke();
        if (animation != null) {
            relayoutTaskViewsOnNextFrame(animation);
        }
        if (!this.mEnterAnimationComplete) {
            return;
        }
        if (prevScroll > 0.3f && curScroll <= 0.3f && this.mStack.getTaskCount() > 0) {
            EventBus.getDefault().send(new ShowStackActionButtonEvent(true));
        } else {
            if (prevScroll >= 0.3f || curScroll < 0.3f) {
                return;
            }
            EventBus.getDefault().send(new HideStackActionButtonEvent());
        }
    }

    public final void onBusEvent(PackagesChangedEvent event) {
        ArraySet<ComponentName> removedComponents = this.mStack.computeComponentsRemoved(event.packageName, event.userId);
        ArrayList<Task> tasks = this.mStack.getStackTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task t = tasks.get(i);
            if (removedComponents.contains(t.key.getComponent())) {
                TaskView tv = getChildViewForTask(t);
                if (tv != null) {
                    tv.dismissTask();
                } else {
                    this.mStack.removeTask(t, AnimationProps.IMMEDIATE, false);
                }
            }
        }
    }

    public final void onBusEvent(LaunchTaskEvent event) {
        this.mUIDozeTrigger.stopDozing();
    }

    public final void onBusEvent(LaunchNextTaskRequestEvent event) {
        int launchTaskIndex;
        int launchTaskIndex2 = this.mStack.indexOfStackTask(this.mStack.getLaunchTarget());
        if (launchTaskIndex2 != -1) {
            launchTaskIndex = Math.max(0, launchTaskIndex2 - 1);
        } else {
            launchTaskIndex = this.mStack.getTaskCount() - 1;
        }
        if (launchTaskIndex != -1) {
            cancelAllTaskViewAnimations();
            final Task launchTask = this.mStack.getStackTasks().get(launchTaskIndex);
            float curScroll = this.mStackScroller.getStackScroll();
            float targetScroll = this.mLayoutAlgorithm.getStackScrollForTaskAtInitialOffset(launchTask);
            float absScrollDiff = Math.abs(targetScroll - curScroll);
            if (getChildViewForTask(launchTask) == null || absScrollDiff > 0.35f) {
                int duration = (int) ((32.0f * absScrollDiff) + 216.0f);
                this.mStackScroller.animateScroll(targetScroll, duration, new Runnable() {
                    @Override
                    public void run() {
                        EventBus.getDefault().send(new LaunchTaskEvent(TaskStackView.this.getChildViewForTask(launchTask), launchTask, null, -1, false));
                    }
                });
            } else {
                EventBus.getDefault().send(new LaunchTaskEvent(getChildViewForTask(launchTask), launchTask, null, -1, false));
            }
            MetricsLogger.action(getContext(), 318, launchTask.key.getComponent().toString());
            return;
        }
        if (this.mStack.getTaskCount() != 0) {
            return;
        }
        EventBus.getDefault().send(new HideRecentsEvent(false, true));
    }

    public final void onBusEvent(LaunchTaskStartedEvent event) {
        this.mAnimationHelper.startLaunchTaskAnimation(event.taskView, event.screenPinningRequested, event.getAnimationTrigger());
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted event) {
        this.mTouchHandler.cancelNonDismissTaskAnimations();
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        cancelDeferredTaskViewLayoutAnimation();
        this.mAnimationHelper.startExitToHomeAnimation(event.animated, event.getAnimationTrigger());
        animateFreeformWorkspaceBackgroundAlpha(0, new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN));
    }

    public final void onBusEvent(DismissFocusedTaskViewEvent event) {
        if (this.mFocusedTask == null) {
            return;
        }
        TaskView tv = getChildViewForTask(this.mFocusedTask);
        if (tv != null) {
            tv.dismissTask();
        }
        resetFocusedTask(this.mFocusedTask);
    }

    public final void onBusEvent(DismissTaskViewEvent event) {
        this.mAnimationHelper.startDeleteTaskAnimation(event.taskView, event.getAnimationTrigger());
    }

    public final void onBusEvent(DismissAllTaskViewsEvent event) {
        final ArrayList<Task> tasks = new ArrayList<>(this.mStack.getStackTasks());
        this.mAnimationHelper.startDeleteAllTasksAnimation(getTaskViews(), event.getAnimationTrigger());
        event.addPostAnimationCallback(new Runnable() {
            @Override
            public void run() {
                TaskStackView.this.announceForAccessibility(TaskStackView.this.getContext().getString(R.string.accessibility_recents_all_items_dismissed));
                TaskStackView.this.mStack.removeAllTasks();
                for (int i = tasks.size() - 1; i >= 0; i--) {
                    EventBus.getDefault().send(new DeleteTaskDataEvent((Task) tasks.get(i)));
                }
                MetricsLogger.action(TaskStackView.this.getContext(), 357);
            }
        });
    }

    public final void onBusEvent(TaskViewDismissedEvent event) {
        announceForAccessibility(getContext().getString(R.string.accessibility_recents_item_dismissed, event.task.title));
        this.mStack.removeTask(event.task, event.animation, false);
        EventBus.getDefault().send(new DeleteTaskDataEvent(event.task));
        MetricsLogger.action(getContext(), 289, event.task.key.getComponent().toString());
    }

    public final void onBusEvent(FocusNextTaskViewEvent event) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        setRelativeFocusedTask(true, false, true, false, event.timerIndicatorDuration);
    }

    public final void onBusEvent(FocusPreviousTaskViewEvent event) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        setRelativeFocusedTask(false, false, true);
    }

    public final void onBusEvent(UserInteractionEvent event) {
        TaskView tv;
        this.mUIDozeTrigger.poke();
        RecentsDebugFlags debugFlags = Recents.getDebugFlags();
        if (!debugFlags.isFastToggleRecentsEnabled() || this.mFocusedTask == null || (tv = getChildViewForTask(this.mFocusedTask)) == null) {
            return;
        }
        tv.getHeaderView().cancelFocusTimerIndicator();
    }

    public final void onBusEvent(DragStartEvent event) {
        addIgnoreTask(event.task);
        if (event.task.isFreeformTask()) {
            this.mStackScroller.animateScroll(this.mLayoutAlgorithm.mInitialScrollP, null);
        }
        float finalScale = event.taskView.getScaleX() * 1.05f;
        this.mLayoutAlgorithm.getStackTransform(event.task, getScroller().getStackScroll(), this.mTmpTransform, null);
        this.mTmpTransform.scale = finalScale;
        this.mTmpTransform.translationZ = this.mLayoutAlgorithm.mMaxTranslationZ + 1;
        this.mTmpTransform.dimAlpha = 0.0f;
        updateTaskViewToTransform(event.taskView, this.mTmpTransform, new AnimationProps(175, Interpolators.FAST_OUT_SLOW_IN));
    }

    public final void onBusEvent(DragStartInitializeDropTargetsEvent event) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (!ssp.hasFreeformWorkspaceSupport()) {
            return;
        }
        event.handler.registerDropTargetForCurrentDrag(this.mStackDropTarget);
        event.handler.registerDropTargetForCurrentDrag(this.mFreeformWorkspaceDropTarget);
    }

    public final void onBusEvent(DragDropTargetChangedEvent event) {
        AnimationProps animation = new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN);
        boolean ignoreTaskOverrides = false;
        if (event.dropTarget instanceof TaskStack.DockState) {
            TaskStack.DockState dockState = (TaskStack.DockState) event.dropTarget;
            Rect systemInsets = new Rect(this.mStableLayoutAlgorithm.mSystemInsets);
            int height = getMeasuredHeight();
            int height2 = height - systemInsets.bottom;
            systemInsets.bottom = 0;
            this.mStackBounds.set(dockState.getDockedTaskStackBounds(this.mDisplayRect, getMeasuredWidth(), height2, this.mDividerSize, systemInsets, this.mLayoutAlgorithm, getResources(), this.mWindowRect));
            this.mLayoutAlgorithm.setSystemInsets(systemInsets);
            this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(this.mStack));
            updateLayoutAlgorithm(true);
            ignoreTaskOverrides = true;
        } else {
            removeIgnoreTask(event.task);
            updateLayoutToStableBounds();
            addIgnoreTask(event.task);
        }
        relayoutTaskViews(animation, null, ignoreTaskOverrides);
    }

    public final void onBusEvent(final DragEndEvent event) {
        boolean hasChangedStacks = false;
        if (event.dropTarget instanceof TaskStack.DockState) {
            this.mLayoutAlgorithm.clearUnfocusedTaskOverrides();
            return;
        }
        boolean isFreeformTask = event.task.isFreeformTask();
        if (!isFreeformTask && event.dropTarget == this.mFreeformWorkspaceDropTarget) {
            hasChangedStacks = true;
        } else if (isFreeformTask && event.dropTarget == this.mStackDropTarget) {
            hasChangedStacks = true;
        }
        if (hasChangedStacks) {
            if (event.dropTarget == this.mFreeformWorkspaceDropTarget) {
                this.mStack.moveTaskToStack(event.task, 2);
            } else if (event.dropTarget == this.mStackDropTarget) {
                this.mStack.moveTaskToStack(event.task, 1);
            }
            updateLayoutAlgorithm(true);
            event.addPostAnimationCallback(new Runnable() {
                @Override
                public void run() {
                    SystemServicesProxy ssp = Recents.getSystemServices();
                    ssp.moveTaskToStack(event.task.key.id, event.task.key.stackId);
                }
            });
        }
        removeIgnoreTask(event.task);
        Utilities.setViewFrameFromTranslation(event.taskView);
        ArrayMap<Task, AnimationProps> animationOverrides = new ArrayMap<>();
        animationOverrides.put(event.task, new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN, event.getAnimationTrigger().decrementOnAnimationEnd()));
        relayoutTaskViews(new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN));
        event.getAnimationTrigger().increment();
    }

    public final void onBusEvent(DragEndCancelledEvent event) {
        removeIgnoreTask(event.task);
        updateLayoutToStableBounds();
        Utilities.setViewFrameFromTranslation(event.taskView);
        ArrayMap<Task, AnimationProps> animationOverrides = new ArrayMap<>();
        animationOverrides.put(event.task, new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN, event.getAnimationTrigger().decrementOnAnimationEnd()));
        relayoutTaskViews(new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN));
        event.getAnimationTrigger().increment();
    }

    public final void onBusEvent(IterateRecentsEvent event) {
        if (this.mEnterAnimationComplete) {
            return;
        }
        EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(null));
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent event) {
        this.mEnterAnimationComplete = true;
        if (this.mStack.getTaskCount() <= 0) {
            return;
        }
        this.mAnimationHelper.startEnterAnimation(event.getAnimationTrigger());
        event.addPostAnimationCallback(new Runnable() {
            @Override
            public void run() {
                TaskStackView.this.mUIDozeTrigger.startDozing();
                if (TaskStackView.this.mFocusedTask != null) {
                    RecentsConfiguration config = Recents.getConfiguration();
                    RecentsActivityLaunchState launchState = config.getLaunchState();
                    TaskStackView.this.setFocusedTask(TaskStackView.this.mStack.indexOfStackTask(TaskStackView.this.mFocusedTask), false, launchState.launchedWithAltTab);
                    TaskView focusedTaskView = TaskStackView.this.getChildViewForTask(TaskStackView.this.mFocusedTask);
                    if (TaskStackView.this.mTouchExplorationEnabled && focusedTaskView != null) {
                        focusedTaskView.requestAccessibilityFocus();
                    }
                }
                EventBus.getDefault().send(new EnterRecentsTaskStackAnimationCompletedEvent());
            }
        });
    }

    public final void onBusEvent(UpdateFreeformTaskViewVisibilityEvent event) {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (task.isFreeformTask()) {
                tv.setVisibility(event.visible ? 0 : 4);
            }
        }
    }

    public final void onBusEvent(final MultiWindowStateChangedEvent event) {
        if (event.inMultiWindow || !event.showDeferredAnimation) {
            setTasks(event.stack, true);
            return;
        }
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        launchState.reset();
        event.getAnimationTrigger().increment();
        post(new Runnable() {
            @Override
            public void run() {
                TaskStackView.this.mAnimationHelper.startNewStackScrollAnimation(event.stack, event.getAnimationTrigger());
                event.getAnimationTrigger().decrement();
            }
        });
    }

    public final void onBusEvent(ConfigurationChangedEvent event) {
        if (event.fromDeviceOrientationChange) {
            this.mDisplayOrientation = Utilities.getAppConfiguration(this.mContext).orientation;
            this.mDisplayRect = Recents.getSystemServices().getDisplayRect();
            this.mStackScroller.stopScroller();
        }
        reloadOnConfigurationChange();
        if (!event.fromMultiWindow) {
            this.mTmpTaskViews.clear();
            this.mTmpTaskViews.addAll(getTaskViews());
            this.mTmpTaskViews.addAll(this.mViewPool.getViews());
            int taskViewCount = this.mTmpTaskViews.size();
            for (int i = 0; i < taskViewCount; i++) {
                this.mTmpTaskViews.get(i).onConfigurationChanged();
            }
        }
        if (event.fromMultiWindow) {
            this.mInitialState = 2;
            requestLayout();
        } else {
            if (!event.fromDeviceOrientationChange) {
                return;
            }
            this.mInitialState = 1;
            requestLayout();
        }
    }

    public final void onBusEvent(RecentsGrowingEvent event) {
        this.mResetToInitialStateWhenResized = true;
    }

    public void reloadOnConfigurationChange() {
        this.mStableLayoutAlgorithm.reloadOnConfigurationChange(getContext());
        this.mLayoutAlgorithm.reloadOnConfigurationChange(getContext());
    }

    private void animateFreeformWorkspaceBackgroundAlpha(int targetAlpha, AnimationProps animation) {
        if (this.mFreeformWorkspaceBackground.getAlpha() == targetAlpha) {
            return;
        }
        Utilities.cancelAnimationWithoutCallbacks(this.mFreeformWorkspaceBackgroundAnimator);
        this.mFreeformWorkspaceBackgroundAnimator = ObjectAnimator.ofInt(this.mFreeformWorkspaceBackground, (Property<GradientDrawable, Integer>) Utilities.DRAWABLE_ALPHA, this.mFreeformWorkspaceBackground.getAlpha(), targetAlpha);
        this.mFreeformWorkspaceBackgroundAnimator.setStartDelay(animation.getDuration(4));
        this.mFreeformWorkspaceBackgroundAnimator.setDuration(animation.getDuration(4));
        this.mFreeformWorkspaceBackgroundAnimator.setInterpolator(animation.getInterpolator(4));
        this.mFreeformWorkspaceBackgroundAnimator.start();
    }

    private int findTaskViewInsertIndex(Task task, int taskIndex) {
        if (taskIndex != -1) {
            List<TaskView> taskViews = getTaskViews();
            boolean foundTaskView = false;
            int taskViewCount = taskViews.size();
            for (int i = 0; i < taskViewCount; i++) {
                Task tvTask = taskViews.get(i).getTask();
                if (tvTask == task) {
                    foundTaskView = true;
                } else if (taskIndex < this.mStack.indexOfStackTask(tvTask)) {
                    if (foundTaskView) {
                        return i - 1;
                    }
                    return i;
                }
            }
        }
        return -1;
    }

    private void readSystemFlags() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mTouchExplorationEnabled = ssp.isTouchExplorationEnabled();
        this.mScreenPinningEnabled = ssp.getSystemSetting(getContext(), "lock_to_app_enabled") != 0;
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        String id = Integer.toHexString(System.identityHashCode(this));
        writer.print(prefix);
        writer.print("TaskStackView");
        writer.print(" hasDefRelayout=");
        writer.print(this.mDeferredTaskViewLayoutAnimation != null ? "Y" : "N");
        writer.print(" clipDirty=");
        writer.print(this.mTaskViewsClipDirty ? "Y" : "N");
        writer.print(" awaitingFirstLayout=");
        writer.print(this.mAwaitingFirstLayout ? "Y" : "N");
        writer.print(" initialState=");
        writer.print(this.mInitialState);
        writer.print(" inMeasureLayout=");
        writer.print(this.mInMeasureLayout ? "Y" : "N");
        writer.print(" enterAnimCompleted=");
        writer.print(this.mEnterAnimationComplete ? "Y" : "N");
        writer.print(" touchExplorationOn=");
        writer.print(this.mTouchExplorationEnabled ? "Y" : "N");
        writer.print(" screenPinningOn=");
        writer.print(this.mScreenPinningEnabled ? "Y" : "N");
        writer.print(" numIgnoreTasks=");
        writer.print(this.mIgnoreTasks.size());
        writer.print(" numViewPool=");
        writer.print(this.mViewPool.getViews().size());
        writer.print(" stableStackBounds=");
        writer.print(Utilities.dumpRect(this.mStableStackBounds));
        writer.print(" stackBounds=");
        writer.print(Utilities.dumpRect(this.mStackBounds));
        writer.print(" stableWindow=");
        writer.print(Utilities.dumpRect(this.mStableWindowRect));
        writer.print(" window=");
        writer.print(Utilities.dumpRect(this.mWindowRect));
        writer.print(" display=");
        writer.print(Utilities.dumpRect(this.mDisplayRect));
        writer.print(" orientation=");
        writer.print(this.mDisplayOrientation);
        writer.print(" [0x");
        writer.print(id);
        writer.print("]");
        writer.println();
        if (this.mFocusedTask != null) {
            writer.print(innerPrefix);
            writer.print("Focused task: ");
            this.mFocusedTask.dump("", writer);
        }
        this.mLayoutAlgorithm.dump(innerPrefix, writer);
        this.mStackScroller.dump(innerPrefix, writer);
    }
}
