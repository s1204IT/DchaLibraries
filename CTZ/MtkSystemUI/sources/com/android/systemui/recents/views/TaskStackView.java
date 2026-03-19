package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.MutableBoolean;
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
import com.android.systemui.recents.RecentsImpl;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.HideStackActionButtonEvent;
import com.android.systemui.recents.events.activity.LaunchMostRecentTaskRequestEvent;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.activity.LaunchTaskStartedEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.PackagesChangedEvent;
import com.android.systemui.recents.events.activity.ShowEmptyViewEvent;
import com.android.systemui.recents.events.activity.ShowStackActionButtonEvent;
import com.android.systemui.recents.events.component.ActivityPinnedEvent;
import com.android.systemui.recents.events.component.ExpandPipEvent;
import com.android.systemui.recents.events.component.HidePipMenuEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.DismissTaskViewEvent;
import com.android.systemui.recents.events.ui.RecentsGrowingEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.events.ui.UserInteractionEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.events.ui.focus.DismissFocusedTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusNextTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusPreviousTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackViewScroller;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.recents.views.ViewPool;
import com.android.systemui.recents.views.grid.GridTaskView;
import com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm;
import com.android.systemui.recents.views.grid.TaskViewFocusFrame;
import com.android.systemui.shared.recents.model.HighResThumbnailLoader;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.TaskStack;
import com.android.systemui.shared.recents.utilities.AnimationProps;
import com.android.systemui.shared.recents.utilities.Utilities;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TaskStackView extends FrameLayout implements TaskStackLayoutAlgorithm.TaskStackLayoutAlgorithmCallbacks, TaskStackViewScroller.TaskStackViewScrollerCallbacks, TaskView.TaskViewCallbacks, ViewPool.ViewPoolConsumer<TaskView, Task>, TaskStack.TaskStackCallbacks {
    private TaskStackAnimationHelper mAnimationHelper;
    private ArrayList<TaskViewTransform> mCurrentTaskTransforms;
    private AnimationProps mDeferredTaskViewLayoutAnimation;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mDisplayOrientation;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mDisplayRect;
    private int mDividerSize;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mEnterAnimationComplete;
    private final float mFastFlingVelocity;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mFinishedLayoutAfterStackReload;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "focused_task_")
    private Task mFocusedTask;
    private ArraySet<Task.TaskKey> mIgnoreTasks;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mInMeasureLayout;
    private LayoutInflater mInflater;

    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialState;
    private int mLastHeight;
    private float mLastScrollPPercent;
    private int mLastWidth;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mLaunchNextAfterFirstMeasure;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "layout_")
    TaskStackLayoutAlgorithm mLayoutAlgorithm;
    private Task mPrefetchingTask;
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
    private boolean mStackActionButtonVisible;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStackBounds;
    private DropTarget mStackDropTarget;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mStackReloaded;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "scroller_")
    private TaskStackViewScroller mStackScroller;
    private int mStartTimerIndicatorDuration;
    private int mTaskCornerRadiusPx;
    private TaskViewFocusFrame mTaskViewFocusFrame;
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

    public TaskStackView(Context context) {
        int dimensionPixelSize;
        super(context);
        this.mStack = new TaskStack();
        this.mTaskViews = new ArrayList<>();
        this.mCurrentTaskTransforms = new ArrayList<>();
        this.mIgnoreTasks = new ArraySet<>();
        this.mDeferredTaskViewLayoutAnimation = null;
        this.mTaskViewsClipDirty = true;
        this.mEnterAnimationComplete = false;
        this.mStackReloaded = false;
        this.mFinishedLayoutAfterStackReload = false;
        this.mLaunchNextAfterFirstMeasure = false;
        this.mInitialState = 1;
        this.mInMeasureLayout = false;
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
        this.mLastScrollPPercent = -1.0f;
        this.mRequestUpdateClippingListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (!TaskStackView.this.mTaskViewsClipDirty) {
                    TaskStackView.this.mTaskViewsClipDirty = true;
                    TaskStackView.this.invalidate();
                }
            }
        };
        this.mStackDropTarget = new DropTarget() {
            @Override
            public boolean acceptsDrop(int i, int i2, int i3, int i4, Rect rect, boolean z) {
                if (!z) {
                    return TaskStackView.this.mLayoutAlgorithm.mStackRect.contains(i, i2);
                }
                return false;
            }
        };
        SystemServicesProxy systemServices = Recents.getSystemServices();
        Resources resources = context.getResources();
        this.mStack.setCallbacks(this);
        this.mViewPool = new ViewPool<>(context, this);
        this.mInflater = LayoutInflater.from(context);
        this.mLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, this);
        this.mStableLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, null);
        this.mStackScroller = new TaskStackViewScroller(context, this, this.mLayoutAlgorithm);
        this.mTouchHandler = new TaskStackViewTouchHandler(context, this, this.mStackScroller);
        this.mAnimationHelper = new TaskStackAnimationHelper(context, this);
        if (Recents.getConfiguration().isGridEnabled) {
            dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.recents_grid_task_view_rounded_corners_radius);
        } else {
            dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        }
        this.mTaskCornerRadiusPx = dimensionPixelSize;
        this.mFastFlingVelocity = resources.getDimensionPixelSize(R.dimen.recents_fast_fling_velocity);
        this.mDividerSize = systemServices.getDockedDividerSize(context);
        this.mDisplayOrientation = Utilities.getAppConfiguration(this.mContext).orientation;
        this.mDisplayRect = systemServices.getDisplayRect();
        this.mStackActionButtonVisible = false;
        if (Recents.getConfiguration().isGridEnabled) {
            this.mTaskViewFocusFrame = new TaskViewFocusFrame(this.mContext, this, this.mLayoutAlgorithm.mTaskGridLayoutAlgorithm);
            addView(this.mTaskViewFocusFrame);
            getViewTreeObserver().addOnGlobalFocusChangeListener(this.mTaskViewFocusFrame);
        }
        this.mUIDozeTrigger = new DozeTrigger(getResources().getInteger(R.integer.recents_task_bar_dismiss_delay_seconds), new Runnable() {
            @Override
            public void run() {
                List<TaskView> taskViews = TaskStackView.this.getTaskViews();
                int size = taskViews.size();
                for (int i = 0; i < size; i++) {
                    taskViews.get(i).startNoUserInteractionAnimation();
                }
            }
        });
        setImportantForAccessibility(1);
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

    void onReload(boolean z) {
        if (!z) {
            resetFocusedTask(getFocusedTask());
        }
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(getTaskViews());
        arrayList.addAll(this.mViewPool.getViews());
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            ((TaskView) arrayList.get(size)).onReload(z);
        }
        readSystemFlags();
        this.mTaskViewsClipDirty = true;
        this.mUIDozeTrigger.stopDozing();
        if (!z) {
            this.mStackScroller.reset();
            this.mStableLayoutAlgorithm.reset();
            this.mLayoutAlgorithm.reset();
            this.mLastScrollPPercent = -1.0f;
        }
        this.mStackReloaded = true;
        this.mFinishedLayoutAfterStackReload = false;
        this.mLaunchNextAfterFirstMeasure = false;
        this.mInitialState = 1;
        requestLayout();
    }

    public void setTasks(TaskStack taskStack, boolean z) {
        this.mStack.setTasks(taskStack, z && this.mLayoutAlgorithm.isInitialized());
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
            View childAt = getChildAt(i);
            if (childAt instanceof TaskView) {
                this.mTaskViews.add((TaskView) childAt);
            }
        }
    }

    List<TaskView> getTaskViews() {
        return this.mTaskViews;
    }

    private TaskView getFrontMostTaskView() {
        List<TaskView> taskViews = getTaskViews();
        if (taskViews.isEmpty()) {
            return null;
        }
        return taskViews.get(taskViews.size() - 1);
    }

    public TaskView getChildViewForTask(Task task) {
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            if (taskView.getTask() == task) {
                return taskView;
            }
        }
        return null;
    }

    public TaskStackLayoutAlgorithm getStackAlgorithm() {
        return this.mLayoutAlgorithm;
    }

    public TaskGridLayoutAlgorithm getGridAlgorithm() {
        return this.mLayoutAlgorithm.mTaskGridLayoutAlgorithm;
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

    int[] computeVisibleTaskTransforms(ArrayList<TaskViewTransform> arrayList, ArrayList<Task> arrayList2, float f, float f2, ArraySet<Task.TaskKey> arraySet, boolean z) {
        int size = arrayList2.size();
        int[] iArr = this.mTmpIntPair;
        iArr[0] = -1;
        iArr[1] = -1;
        boolean z2 = Float.compare(f, f2) != 0;
        matchTaskListSize(arrayList2, arrayList);
        TaskViewTransform taskViewTransform = null;
        TaskViewTransform taskViewTransform2 = null;
        TaskViewTransform taskViewTransform3 = null;
        for (int i = size - 1; i >= 0; i--) {
            Task task = arrayList2.get(i);
            TaskViewTransform stackTransform = this.mLayoutAlgorithm.getStackTransform(task, f, arrayList.get(i), taskViewTransform, z);
            if (z2 && !stackTransform.visible) {
                TaskViewTransform stackTransform2 = this.mLayoutAlgorithm.getStackTransform(task, f2, new TaskViewTransform(), taskViewTransform2);
                if (stackTransform2.visible) {
                    stackTransform.copyFrom(stackTransform2);
                }
                taskViewTransform3 = stackTransform2;
            }
            if (!arraySet.contains(task.key)) {
                if (stackTransform.visible) {
                    if (iArr[0] < 0) {
                        iArr[0] = i;
                    }
                    iArr[1] = i;
                }
                taskViewTransform = stackTransform;
                taskViewTransform2 = taskViewTransform3;
            }
        }
        return iArr;
    }

    void bindVisibleTaskViews(float f) {
        bindVisibleTaskViews(f, false);
    }

    void bindVisibleTaskViews(float f, boolean z) {
        int i;
        ArrayList<Task> tasks = this.mStack.getTasks();
        int[] iArrComputeVisibleTaskTransforms = computeVisibleTaskTransforms(this.mCurrentTaskTransforms, tasks, this.mStackScroller.getStackScroll(), f, this.mIgnoreTasks, z);
        this.mTmpTaskViewMap.clear();
        List<TaskView> taskViews = getTaskViews();
        int i2 = -1;
        for (int size = taskViews.size() - 1; size >= 0; size--) {
            TaskView taskView = taskViews.get(size);
            Task task = taskView.getTask();
            if (!this.mIgnoreTasks.contains(task.key)) {
                int iIndexOfTask = this.mStack.indexOfTask(task);
                TaskViewTransform taskViewTransform = null;
                if (iIndexOfTask != -1) {
                    taskViewTransform = this.mCurrentTaskTransforms.get(iIndexOfTask);
                }
                if (taskViewTransform != null && taskViewTransform.visible) {
                    this.mTmpTaskViewMap.put(task.key, taskView);
                } else {
                    if (this.mTouchExplorationEnabled && Utilities.isDescendentAccessibilityFocused(taskView)) {
                        resetFocusedTask(task);
                        i2 = iIndexOfTask;
                    }
                    this.mViewPool.returnViewToPool(taskView);
                }
            }
        }
        for (int size2 = tasks.size() - 1; size2 >= 0; size2--) {
            Task task2 = tasks.get(size2);
            TaskViewTransform taskViewTransform2 = this.mCurrentTaskTransforms.get(size2);
            if (!this.mIgnoreTasks.contains(task2.key) && taskViewTransform2.visible) {
                TaskView taskView2 = this.mTmpTaskViewMap.get(task2.key);
                if (taskView2 == null) {
                    TaskView taskViewPickUpViewFromPool = this.mViewPool.pickUpViewFromPool(task2, task2);
                    if (taskViewTransform2.rect.top <= this.mLayoutAlgorithm.mStackRect.top) {
                        updateTaskViewToTransform(taskViewPickUpViewFromPool, this.mLayoutAlgorithm.getBackOfStackTransform(), AnimationProps.IMMEDIATE);
                    } else {
                        updateTaskViewToTransform(taskViewPickUpViewFromPool, this.mLayoutAlgorithm.getFrontOfStackTransform(), AnimationProps.IMMEDIATE);
                    }
                } else {
                    int iFindTaskViewInsertIndex = findTaskViewInsertIndex(task2, this.mStack.indexOfTask(task2));
                    if (iFindTaskViewInsertIndex != getTaskViews().indexOf(taskView2)) {
                        detachViewFromParent(taskView2);
                        attachViewToParent(taskView2, iFindTaskViewInsertIndex, taskView2.getLayoutParams());
                        updateTaskViewsList();
                    }
                }
            }
        }
        updatePrefetchingTask(tasks, iArrComputeVisibleTaskTransforms[0], iArrComputeVisibleTaskTransforms[1]);
        if (i2 != -1) {
            if (i2 < iArrComputeVisibleTaskTransforms[1]) {
                i = iArrComputeVisibleTaskTransforms[1];
            } else {
                i = iArrComputeVisibleTaskTransforms[0];
            }
            setFocusedTask(i, false, true);
            TaskView childViewForTask = getChildViewForTask(this.mFocusedTask);
            if (childViewForTask != null) {
                childViewForTask.requestAccessibilityFocus();
            }
        }
    }

    public void relayoutTaskViews(AnimationProps animationProps) {
        relayoutTaskViews(animationProps, null, false);
    }

    private void relayoutTaskViews(AnimationProps animationProps, ArrayMap<Task, AnimationProps> arrayMap, boolean z) {
        cancelDeferredTaskViewLayoutAnimation();
        bindVisibleTaskViews(this.mStackScroller.getStackScroll(), z);
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            Task task = taskView.getTask();
            if (!this.mIgnoreTasks.contains(task.key)) {
                int iIndexOfTask = this.mStack.indexOfTask(task);
                if (iIndexOfTask == -1) {
                    Log.w("TaskStackView", "relayoutTaskViews() task index = -1");
                } else {
                    TaskViewTransform taskViewTransform = this.mCurrentTaskTransforms.get(iIndexOfTask);
                    if (arrayMap != null && arrayMap.containsKey(task)) {
                        animationProps = arrayMap.get(task);
                    }
                    updateTaskViewToTransform(taskView, taskViewTransform, animationProps);
                }
            }
        }
    }

    void relayoutTaskViewsOnNextFrame(AnimationProps animationProps) {
        this.mDeferredTaskViewLayoutAnimation = animationProps;
        invalidate();
    }

    public void updateTaskViewToTransform(TaskView taskView, TaskViewTransform taskViewTransform, AnimationProps animationProps) {
        if (taskView.isAnimatingTo(taskViewTransform)) {
            return;
        }
        taskView.cancelTransformAnimation();
        taskView.updateViewPropertiesToTaskTransform(taskViewTransform, animationProps, this.mRequestUpdateClippingListener);
    }

    public void getCurrentTaskTransforms(ArrayList<Task> arrayList, ArrayList<TaskViewTransform> arrayList2) {
        matchTaskListSize(arrayList, arrayList2);
        int focusState = this.mLayoutAlgorithm.getFocusState();
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            Task task = arrayList.get(size);
            TaskViewTransform taskViewTransform = arrayList2.get(size);
            TaskView childViewForTask = getChildViewForTask(task);
            if (childViewForTask != null) {
                taskViewTransform.fillIn(childViewForTask);
            } else {
                this.mLayoutAlgorithm.getStackTransform(task, this.mStackScroller.getStackScroll(), focusState, taskViewTransform, null, true, false);
            }
            taskViewTransform.visible = true;
        }
    }

    public void getLayoutTaskTransforms(float f, int i, ArrayList<Task> arrayList, boolean z, ArrayList<TaskViewTransform> arrayList2) {
        matchTaskListSize(arrayList, arrayList2);
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            Task task = arrayList.get(size);
            TaskViewTransform taskViewTransform = arrayList2.get(size);
            this.mLayoutAlgorithm.getStackTransform(task, f, i, taskViewTransform, null, true, z);
            taskViewTransform.visible = true;
        }
    }

    void cancelDeferredTaskViewLayoutAnimation() {
        this.mDeferredTaskViewLayoutAnimation = null;
    }

    void cancelAllTaskViewAnimations() {
        List<TaskView> taskViews = getTaskViews();
        for (int size = taskViews.size() - 1; size >= 0; size--) {
            TaskView taskView = taskViews.get(size);
            if (!this.mIgnoreTasks.contains(taskView.getTask().key)) {
                taskView.cancelTransformAnimation();
            }
        }
    }

    private void clipTaskViews() {
        int i;
        TaskView taskView;
        if (Recents.getConfiguration().isGridEnabled) {
            return;
        }
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        TaskView taskView2 = null;
        int i2 = 0;
        while (i2 < size) {
            TaskView taskView3 = taskViews.get(i2);
            if (isIgnoredTask(taskView3.getTask()) && taskView2 != null) {
                taskView3.setTranslationZ(Math.max(taskView3.getTranslationZ(), taskView2.getTranslationZ() + 0.1f));
            }
            if (i2 < size - 1 && taskView3.shouldClipViewInStack()) {
                int i3 = i2 + 1;
                while (true) {
                    if (i3 < size) {
                        taskView = taskViews.get(i3);
                        if (taskView.shouldClipViewInStack()) {
                            break;
                        } else {
                            i3++;
                        }
                    } else {
                        taskView = null;
                        break;
                    }
                }
                if (taskView != null) {
                    float bottom = taskView3.getBottom();
                    float top = taskView.getTop();
                    if (top < bottom) {
                        i = ((int) (bottom - top)) - this.mTaskCornerRadiusPx;
                    }
                }
            } else {
                i = 0;
            }
            taskView3.getViewBounds().setClipBottom(i);
            taskView3.mThumbnailView.updateThumbnailVisibility(i - taskView3.getPaddingBottom());
            i2++;
            taskView2 = taskView3;
        }
        this.mTaskViewsClipDirty = false;
    }

    public void updateLayoutAlgorithm(boolean z) {
        updateLayoutAlgorithm(z, Recents.getConfiguration().getLaunchState());
    }

    public void updateLayoutAlgorithm(boolean z, RecentsActivityLaunchState recentsActivityLaunchState) {
        this.mLayoutAlgorithm.update(this.mStack, this.mIgnoreTasks, recentsActivityLaunchState, this.mLastScrollPPercent);
        if (z) {
            this.mStackScroller.boundScroll();
        }
    }

    private void updateLayoutToStableBounds() {
        this.mWindowRect.set(this.mStableWindowRect);
        this.mStackBounds.set(this.mStableStackBounds);
        this.mLayoutAlgorithm.setSystemInsets(this.mStableLayoutAlgorithm.mSystemInsets);
        this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds);
        updateLayoutAlgorithm(true);
    }

    public TaskStackViewScroller getScroller() {
        return this.mStackScroller;
    }

    public boolean setFocusedTask(int i, boolean z, boolean z2) {
        return setFocusedTask(i, z, z2, 0);
    }

    public boolean setFocusedTask(int i, boolean z, boolean z2, int i2) {
        int iClamp;
        TaskView childViewForTask;
        boolean z3 = false;
        if (this.mStack.getTaskCount() > 0) {
            iClamp = Utilities.clamp(i, 0, this.mStack.getTaskCount() - 1);
        } else {
            iClamp = -1;
        }
        Task task = iClamp != -1 ? this.mStack.getTasks().get(iClamp) : null;
        if (this.mFocusedTask != null) {
            if (i2 > 0 && (childViewForTask = getChildViewForTask(this.mFocusedTask)) != null) {
                childViewForTask.getHeaderView().cancelFocusTimerIndicator();
            }
            resetFocusedTask(this.mFocusedTask);
        }
        this.mFocusedTask = task;
        if (task != null) {
            if (i2 > 0) {
                TaskView childViewForTask2 = getChildViewForTask(this.mFocusedTask);
                if (childViewForTask2 != null) {
                    childViewForTask2.getHeaderView().startFocusTimerIndicator(i2);
                } else {
                    this.mStartTimerIndicatorDuration = i2;
                }
            }
            if (z) {
                if (!this.mEnterAnimationComplete) {
                    cancelAllTaskViewAnimations();
                }
                this.mLayoutAlgorithm.clearUnfocusedTaskOverrides();
                boolean zStartScrollToFocusedTaskAnimation = this.mAnimationHelper.startScrollToFocusedTaskAnimation(task, z2);
                if (zStartScrollToFocusedTaskAnimation) {
                    sendAccessibilityEvent(4096);
                }
                z3 = zStartScrollToFocusedTaskAnimation;
            } else {
                TaskView childViewForTask3 = getChildViewForTask(task);
                if (childViewForTask3 != null) {
                    childViewForTask3.setFocusedState(true, z2);
                }
            }
            if (this.mTaskViewFocusFrame != null) {
                this.mTaskViewFocusFrame.moveGridTaskViewFocus(getChildViewForTask(task));
            }
        }
        return z3;
    }

    public void setRelativeFocusedTask(boolean z, boolean z2, boolean z3) {
        setRelativeFocusedTask(z, z2, z3, false, 0);
    }

    public void setRelativeFocusedTask(boolean z, boolean z2, boolean z3, boolean z4, int i) {
        int i2;
        Task focusedTask = getFocusedTask();
        int iIndexOfTask = this.mStack.indexOfTask(focusedTask);
        if (focusedTask != null) {
            if (z2) {
                ArrayList<Task> tasks = this.mStack.getTasks();
                i2 = (z ? -1 : 1) + iIndexOfTask;
                if (i2 < 0 || i2 >= tasks.size()) {
                    i2 = iIndexOfTask;
                }
            } else {
                int taskCount = this.mStack.getTaskCount();
                i2 = ((iIndexOfTask + (z ? -1 : 1)) + taskCount) % taskCount;
            }
        } else {
            float stackScroll = this.mStackScroller.getStackScroll();
            ArrayList<Task> tasks2 = this.mStack.getTasks();
            int size = tasks2.size();
            if (useGridLayout()) {
                i2 = size - 1;
            } else if (z) {
                i2 = size - 1;
                while (i2 >= 0 && Float.compare(this.mLayoutAlgorithm.getStackScrollForTask(tasks2.get(i2)), stackScroll) > 0) {
                    i2--;
                }
            } else {
                i2 = 0;
                while (i2 < size && Float.compare(this.mLayoutAlgorithm.getStackScrollForTask(tasks2.get(i2)), stackScroll) < 0) {
                    i2++;
                }
            }
        }
        if (i2 != -1 && setFocusedTask(i2, true, true, i) && z4) {
            EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(null));
        }
    }

    public void resetFocusedTask(Task task) {
        TaskView childViewForTask;
        if (task != null && (childViewForTask = getChildViewForTask(task)) != null) {
            childViewForTask.setFocusedState(false, false);
        }
        if (this.mTaskViewFocusFrame != null) {
            this.mTaskViewFocusFrame.moveGridTaskViewFocus(null);
        }
        this.mFocusedTask = null;
    }

    public Task getFocusedTask() {
        return this.mFocusedTask;
    }

    Task getAccessibilityFocusedTask() {
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            if (Utilities.isDescendentAccessibilityFocused(taskView)) {
                return taskView.getTask();
            }
        }
        TaskView frontMostTaskView = getFrontMostTaskView();
        if (frontMostTaskView != null) {
            return frontMostTaskView.getTask();
        }
        return null;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        if (size > 0) {
            TaskView taskView = taskViews.get(0);
            TaskView taskView2 = taskViews.get(size - 1);
            accessibilityEvent.setFromIndex(this.mStack.indexOfTask(taskView.getTask()));
            accessibilityEvent.setToIndex(this.mStack.indexOfTask(taskView2.getTask()));
            accessibilityEvent.setContentDescription(taskView2.getTask().title);
        }
        accessibilityEvent.setItemCount(this.mStack.getTaskCount());
        float fHeight = this.mLayoutAlgorithm.mStackRect.height();
        accessibilityEvent.setScrollY((int) (this.mStackScroller.getStackScroll() * fHeight));
        accessibilityEvent.setMaxScrollY((int) (this.mLayoutAlgorithm.mMaxScrollP * fHeight));
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (getTaskViews().size() > 1) {
            Task accessibilityFocusedTask = getAccessibilityFocusedTask();
            accessibilityNodeInfo.setScrollable(true);
            int iIndexOfTask = this.mStack.indexOfTask(accessibilityFocusedTask);
            if (iIndexOfTask > 0 || !this.mStackActionButtonVisible) {
                accessibilityNodeInfo.addAction(8192);
            }
            if (iIndexOfTask >= 0 && iIndexOfTask < this.mStack.getTaskCount() - 1) {
                accessibilityNodeInfo.addAction(4096);
            }
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ScrollView.class.getName();
    }

    @Override
    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (super.performAccessibilityAction(i, bundle)) {
            return true;
        }
        int iIndexOfTask = this.mStack.indexOfTask(getAccessibilityFocusedTask());
        if (iIndexOfTask >= 0 && iIndexOfTask < this.mStack.getTaskCount()) {
            if (i == 4096) {
                setFocusedTask(iIndexOfTask + 1, true, true, 0);
                return true;
            }
            if (i == 8192) {
                setFocusedTask(iIndexOfTask - 1, true, true, 0);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onInterceptTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onGenericMotionEvent(motionEvent);
    }

    @Override
    public void computeScroll() {
        boolean z;
        if (this.mStackScroller.computeScroll()) {
            sendAccessibilityEvent(4096);
            HighResThumbnailLoader highResThumbnailLoader = Recents.getTaskLoader().getHighResThumbnailLoader();
            if (this.mStackScroller.getScrollVelocity() > this.mFastFlingVelocity) {
                z = true;
            } else {
                z = false;
            }
            highResThumbnailLoader.setFlingingFast(z);
        }
        if (this.mDeferredTaskViewLayoutAnimation != null) {
            relayoutTaskViews(this.mDeferredTaskViewLayoutAnimation);
            this.mTaskViewsClipDirty = true;
            this.mDeferredTaskViewLayoutAnimation = null;
        }
        if (this.mTaskViewsClipDirty) {
            clipTaskViews();
        }
        this.mLastScrollPPercent = Utilities.clamp(Utilities.unmapRange(this.mStackScroller.getStackScroll(), this.mLayoutAlgorithm.mMinScrollP, this.mLayoutAlgorithm.mMaxScrollP), 0.0f, 1.0f);
    }

    public TaskStackLayoutAlgorithm.VisibilityReport computeStackVisibilityReport() {
        return this.mLayoutAlgorithm.computeStackVisibilityReport(this.mStack.getTasks());
    }

    public void setSystemInsets(Rect rect) {
        if (this.mLayoutAlgorithm.setSystemInsets(rect) | this.mStableLayoutAlgorithm.setSystemInsets(rect) | false) {
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        boolean z = true;
        this.mInMeasureLayout = true;
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        this.mLayoutAlgorithm.getTaskStackBounds(this.mDisplayRect, new Rect(0, 0, size, size2), this.mLayoutAlgorithm.mSystemInsets.top, this.mLayoutAlgorithm.mSystemInsets.left, this.mLayoutAlgorithm.mSystemInsets.right, this.mTmpRect);
        if (!this.mTmpRect.equals(this.mStableStackBounds)) {
            this.mStableStackBounds.set(this.mTmpRect);
            this.mStackBounds.set(this.mTmpRect);
            this.mStableWindowRect.set(0, 0, size, size2);
            this.mWindowRect.set(0, 0, size, size2);
        }
        this.mStableLayoutAlgorithm.initialize(this.mDisplayRect, this.mStableWindowRect, this.mStableStackBounds);
        this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds);
        updateLayoutAlgorithm(false);
        if ((size == this.mLastWidth && size2 == this.mLastHeight) || !this.mResetToInitialStateWhenResized) {
            z = false;
        }
        if (!this.mFinishedLayoutAfterStackReload || this.mInitialState != 0 || z) {
            if (this.mInitialState != 2 || z) {
                updateToInitialState();
                this.mResetToInitialStateWhenResized = false;
            }
            if (this.mFinishedLayoutAfterStackReload) {
                this.mInitialState = 0;
            }
        }
        if (this.mLaunchNextAfterFirstMeasure) {
            this.mLaunchNextAfterFirstMeasure = false;
            EventBus.getDefault().post(new LaunchNextTaskRequestEvent());
        }
        bindVisibleTaskViews(this.mStackScroller.getStackScroll(), false);
        this.mTmpTaskViews.clear();
        this.mTmpTaskViews.addAll(getTaskViews());
        this.mTmpTaskViews.addAll(this.mViewPool.getViews());
        int size3 = this.mTmpTaskViews.size();
        for (int i3 = 0; i3 < size3; i3++) {
            measureTaskView(this.mTmpTaskViews.get(i3));
        }
        if (this.mTaskViewFocusFrame != null) {
            this.mTaskViewFocusFrame.measure();
        }
        setMeasuredDimension(size, size2);
        this.mLastWidth = size;
        this.mLastHeight = size2;
        this.mInMeasureLayout = false;
    }

    private void measureTaskView(TaskView taskView) {
        Rect rect = new Rect();
        if (taskView.getBackground() != null) {
            taskView.getBackground().getPadding(rect);
        }
        this.mTmpRect.set(this.mStableLayoutAlgorithm.getTaskRect());
        this.mTmpRect.union(this.mLayoutAlgorithm.getTaskRect());
        taskView.measure(View.MeasureSpec.makeMeasureSpec(this.mTmpRect.width() + rect.left + rect.right, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mTmpRect.height() + rect.top + rect.bottom, 1073741824));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mTmpTaskViews.clear();
        this.mTmpTaskViews.addAll(getTaskViews());
        this.mTmpTaskViews.addAll(this.mViewPool.getViews());
        int size = this.mTmpTaskViews.size();
        for (int i5 = 0; i5 < size; i5++) {
            layoutTaskView(z, this.mTmpTaskViews.get(i5));
        }
        if (this.mTaskViewFocusFrame != null) {
            this.mTaskViewFocusFrame.layout();
        }
        if (z && this.mStackScroller.isScrollOutOfBounds()) {
            this.mStackScroller.boundScroll();
        }
        relayoutTaskViews(AnimationProps.IMMEDIATE);
        clipTaskViews();
        if (!this.mFinishedLayoutAfterStackReload) {
            this.mInitialState = 0;
            onFirstLayout();
            if (this.mStackReloaded) {
                this.mFinishedLayoutAfterStackReload = true;
                tryStartEnterAnimation();
            }
        }
    }

    private void layoutTaskView(boolean z, TaskView taskView) {
        if (z) {
            Rect rect = new Rect();
            if (taskView.getBackground() != null) {
                taskView.getBackground().getPadding(rect);
            }
            this.mTmpRect.set(this.mStableLayoutAlgorithm.getTaskRect());
            this.mTmpRect.union(this.mLayoutAlgorithm.getTaskRect());
            taskView.cancelTransformAnimation();
            taskView.layout(this.mTmpRect.left - rect.left, this.mTmpRect.top - rect.top, this.mTmpRect.right + rect.right, this.mTmpRect.bottom + rect.bottom);
            return;
        }
        taskView.layout(taskView.getLeft(), taskView.getTop(), taskView.getRight(), taskView.getBottom());
    }

    void onFirstLayout() {
        boolean z;
        int initialFocusTaskIndex;
        this.mAnimationHelper.prepareForEnterAnimation();
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (!useGridLayout() || launchState.launchedWithAltTab) {
            z = true;
        } else {
            z = false;
        }
        if (z && (initialFocusTaskIndex = getInitialFocusTaskIndex(launchState, this.mStack.getTaskCount(), useGridLayout())) != -1) {
            setFocusedTask(initialFocusTaskIndex, false, false);
        }
        updateStackActionButtonVisibility();
    }

    public boolean isTouchPointInView(float f, float f2, TaskView taskView) {
        this.mTmpRect.set(taskView.getLeft(), taskView.getTop(), taskView.getRight(), taskView.getBottom());
        this.mTmpRect.offset((int) taskView.getTranslationX(), (int) taskView.getTranslationY());
        return this.mTmpRect.contains((int) f, (int) f2);
    }

    public Task findAnchorTask(List<Task> list, MutableBoolean mutableBoolean) {
        for (int size = list.size() - 1; size >= 0; size--) {
            Task task = list.get(size);
            if (isIgnoredTask(task)) {
                if (size == list.size() - 1) {
                    mutableBoolean.value = true;
                }
            } else {
                return task;
            }
        }
        return null;
    }

    @Override
    public void onStackTaskAdded(TaskStack taskStack, Task task) {
        AnimationProps animationProps;
        updateLayoutAlgorithm(true);
        if (!this.mFinishedLayoutAfterStackReload) {
            animationProps = AnimationProps.IMMEDIATE;
        } else {
            animationProps = new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN);
        }
        relayoutTaskViews(animationProps);
    }

    @Override
    public void onStackTaskRemoved(TaskStack taskStack, Task task, Task task2, AnimationProps animationProps, boolean z, boolean z2) {
        int i;
        TaskView childViewForTask;
        if (this.mFocusedTask == task) {
            resetFocusedTask(task);
        }
        TaskView childViewForTask2 = getChildViewForTask(task);
        if (childViewForTask2 != null) {
            this.mViewPool.returnViewToPool(childViewForTask2);
        }
        removeIgnoreTask(task);
        if (animationProps != null) {
            updateLayoutAlgorithm(true);
            relayoutTaskViews(animationProps);
        }
        if (this.mScreenPinningEnabled && task2 != null && (childViewForTask = getChildViewForTask(task2)) != null) {
            childViewForTask.showActionButton(true, 200);
        }
        if (this.mStack.getTaskCount() == 0) {
            if (z2) {
                EventBus eventBus = EventBus.getDefault();
                if (z) {
                    i = R.string.recents_empty_message;
                } else {
                    i = R.string.recents_empty_message_dismissed_all;
                }
                eventBus.send(new AllTaskViewsDismissedEvent(i));
                return;
            }
            EventBus.getDefault().send(new ShowEmptyViewEvent());
        }
    }

    @Override
    public void onStackTasksRemoved(TaskStack taskStack) {
        resetFocusedTask(getFocusedTask());
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(getTaskViews());
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            this.mViewPool.returnViewToPool((TaskView) arrayList.get(size));
        }
        this.mIgnoreTasks.clear();
        EventBus.getDefault().send(new AllTaskViewsDismissedEvent(R.string.recents_empty_message_dismissed_all));
    }

    @Override
    public void onStackTasksUpdated(TaskStack taskStack) {
        if (!this.mFinishedLayoutAfterStackReload) {
            return;
        }
        updateLayoutAlgorithm(false);
        relayoutTaskViews(AnimationProps.IMMEDIATE);
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            bindTaskView(taskView, taskView.getTask());
        }
    }

    @Override
    public TaskView createView(Context context) {
        if (Recents.getConfiguration().isGridEnabled) {
            return (GridTaskView) this.mInflater.inflate(R.layout.recents_grid_task_view, (ViewGroup) this, false);
        }
        return (TaskView) this.mInflater.inflate(R.layout.recents_task_view, (ViewGroup) this, false);
    }

    @Override
    public void onReturnViewToPool(TaskView taskView) {
        unbindTaskView(taskView, taskView.getTask());
        taskView.clearAccessibilityFocus();
        taskView.resetViewProperties();
        taskView.setFocusedState(false, false);
        taskView.setClipViewInStack(false);
        if (this.mScreenPinningEnabled) {
            taskView.hideActionButton(false, 0, false, null);
        }
        detachViewFromParent(taskView);
        updateTaskViewsList();
    }

    @Override
    public void onPickUpViewFromPool(TaskView taskView, Task task, boolean z) {
        int iFindTaskViewInsertIndex = findTaskViewInsertIndex(task, this.mStack.indexOfTask(task));
        if (z) {
            if (this.mInMeasureLayout) {
                addView(taskView, iFindTaskViewInsertIndex);
            } else {
                ViewGroup.LayoutParams layoutParams = taskView.getLayoutParams();
                if (layoutParams == null) {
                    layoutParams = generateDefaultLayoutParams();
                }
                addViewInLayout(taskView, iFindTaskViewInsertIndex, layoutParams, true);
                measureTaskView(taskView);
                layoutTaskView(true, taskView);
            }
        } else {
            attachViewToParent(taskView, iFindTaskViewInsertIndex, taskView.getLayoutParams());
        }
        updateTaskViewsList();
        bindTaskView(taskView, task);
        taskView.setCallbacks(this);
        taskView.setTouchEnabled(true);
        taskView.setClipViewInStack(true);
        if (this.mFocusedTask == task) {
            taskView.setFocusedState(true, false);
            if (this.mStartTimerIndicatorDuration > 0) {
                taskView.getHeaderView().startFocusTimerIndicator(this.mStartTimerIndicatorDuration);
                this.mStartTimerIndicatorDuration = 0;
            }
        }
        if (this.mScreenPinningEnabled && taskView.getTask() == this.mStack.getFrontMostTask()) {
            taskView.showActionButton(false, 0);
        }
    }

    @Override
    public boolean hasPreferredData(TaskView taskView, Task task) {
        return taskView.getTask() == task;
    }

    private void bindTaskView(TaskView taskView, Task task) {
        taskView.onTaskBound(task, this.mTouchExplorationEnabled, this.mDisplayOrientation, this.mDisplayRect);
        if (this.mUIDozeTrigger.isAsleep() || useGridLayout() || Recents.getConfiguration().isLowRamDevice) {
            taskView.setNoUserInteractionState();
        }
        if (task == this.mPrefetchingTask) {
            task.notifyTaskDataLoaded(task.thumbnail, task.icon);
        } else {
            Recents.getTaskLoader().loadTaskData(task);
        }
        Recents.getTaskLoader().getHighResThumbnailLoader().onTaskVisible(task);
    }

    private void unbindTaskView(TaskView taskView, Task task) {
        if (task != this.mPrefetchingTask) {
            Recents.getTaskLoader().unloadTaskData(task);
        }
        Recents.getTaskLoader().getHighResThumbnailLoader().onTaskInvisible(task);
    }

    private void updatePrefetchingTask(ArrayList<Task> arrayList, int i, int i2) {
        boolean z;
        Task task;
        int iIndexOf;
        if (i == -1 || i2 == -1) {
            z = false;
        } else {
            z = true;
        }
        if (z && i < arrayList.size() - 1) {
            task = arrayList.get(i + 1);
        } else {
            task = null;
        }
        if (this.mPrefetchingTask != task) {
            if (this.mPrefetchingTask != null && ((iIndexOf = arrayList.indexOf(this.mPrefetchingTask)) < i2 || iIndexOf > i)) {
                Recents.getTaskLoader().unloadTaskData(this.mPrefetchingTask);
            }
            this.mPrefetchingTask = task;
            if (task != null) {
                Recents.getTaskLoader().loadTaskData(task);
            }
        }
    }

    private void clearPrefetchingTask() {
        if (this.mPrefetchingTask != null) {
            Recents.getTaskLoader().unloadTaskData(this.mPrefetchingTask);
        }
        this.mPrefetchingTask = null;
    }

    @Override
    public void onTaskViewClipStateChanged(TaskView taskView) {
        if (!this.mTaskViewsClipDirty) {
            this.mTaskViewsClipDirty = true;
            invalidate();
        }
    }

    @Override
    public void onFocusStateChanged(int i, int i2) {
        if (this.mDeferredTaskViewLayoutAnimation == null) {
            this.mUIDozeTrigger.poke();
            relayoutTaskViewsOnNextFrame(AnimationProps.IMMEDIATE);
        }
    }

    @Override
    public void onStackScrollChanged(float f, float f2, AnimationProps animationProps) {
        this.mUIDozeTrigger.poke();
        if (animationProps != null) {
            relayoutTaskViewsOnNextFrame(animationProps);
        }
        if (this.mEnterAnimationComplete && !useGridLayout()) {
            if (Recents.getConfiguration().isLowRamDevice) {
                if (this.mStack.getTaskCount() > 0 && !this.mStackActionButtonVisible && this.mTouchHandler.mIsScrolling && f2 - f < 0.0f) {
                    EventBus.getDefault().send(new ShowStackActionButtonEvent(true));
                    return;
                }
                return;
            }
            if (f > 0.3f && f2 <= 0.3f && this.mStack.getTaskCount() > 0) {
                EventBus.getDefault().send(new ShowStackActionButtonEvent(true));
            } else if (f < 0.3f && f2 >= 0.3f) {
                EventBus.getDefault().send(new HideStackActionButtonEvent());
            }
        }
    }

    public final void onBusEvent(PackagesChangedEvent packagesChangedEvent) {
        ArraySet<ComponentName> arraySetComputeComponentsRemoved = this.mStack.computeComponentsRemoved(packagesChangedEvent.packageName, packagesChangedEvent.userId);
        ArrayList<Task> tasks = this.mStack.getTasks();
        for (int size = tasks.size() - 1; size >= 0; size--) {
            Task task = tasks.get(size);
            if (arraySetComputeComponentsRemoved.contains(task.key.getComponent())) {
                TaskView childViewForTask = getChildViewForTask(task);
                if (childViewForTask != null) {
                    childViewForTask.dismissTask();
                } else {
                    this.mStack.removeTask(task, AnimationProps.IMMEDIATE, false);
                }
            }
        }
    }

    public final void onBusEvent(LaunchTaskEvent launchTaskEvent) {
        this.mUIDozeTrigger.stopDozing();
    }

    public final void onBusEvent(LaunchMostRecentTaskRequestEvent launchMostRecentTaskRequestEvent) {
        if (this.mStack.getTaskCount() > 0) {
            launchTask(this.mStack.getFrontMostTask());
        }
    }

    public final void onBusEvent(ShowStackActionButtonEvent showStackActionButtonEvent) {
        this.mStackActionButtonVisible = true;
    }

    public final void onBusEvent(HideStackActionButtonEvent hideStackActionButtonEvent) {
        this.mStackActionButtonVisible = false;
    }

    public final void onBusEvent(LaunchNextTaskRequestEvent launchNextTaskRequestEvent) {
        if (!this.mFinishedLayoutAfterStackReload) {
            this.mLaunchNextAfterFirstMeasure = true;
            return;
        }
        if (this.mStack.getTaskCount() == 0) {
            if (RecentsImpl.getLastPipTime() != -1) {
                EventBus.getDefault().send(new ExpandPipEvent());
                MetricsLogger.action(getContext(), 318, "pip");
                return;
            } else {
                EventBus.getDefault().send(new HideRecentsEvent(false, true));
                return;
            }
        }
        if (!Recents.getConfiguration().getLaunchState().launchedFromPipApp && this.mStack.isNextLaunchTargetPip(RecentsImpl.getLastPipTime())) {
            EventBus.getDefault().send(new ExpandPipEvent());
            MetricsLogger.action(getContext(), 318, "pip");
            return;
        }
        final Task nextLaunchTarget = this.mStack.getNextLaunchTarget();
        if (nextLaunchTarget != null) {
            HidePipMenuEvent hidePipMenuEvent = new HidePipMenuEvent();
            hidePipMenuEvent.addPostAnimationCallback(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.launchTask(nextLaunchTarget);
                }
            });
            EventBus.getDefault().send(hidePipMenuEvent);
            MetricsLogger.action(getContext(), 318, nextLaunchTarget.key.getComponent().toString());
        }
    }

    public final void onBusEvent(LaunchTaskStartedEvent launchTaskStartedEvent) {
        this.mAnimationHelper.startLaunchTaskAnimation(launchTaskStartedEvent.taskView, launchTaskStartedEvent.screenPinningRequested, launchTaskStartedEvent.getAnimationTrigger());
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted dismissRecentsToHomeAnimationStarted) {
        this.mTouchHandler.cancelNonDismissTaskAnimations();
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        cancelDeferredTaskViewLayoutAnimation();
        this.mAnimationHelper.startExitToHomeAnimation(dismissRecentsToHomeAnimationStarted.animated, dismissRecentsToHomeAnimationStarted.getAnimationTrigger());
        if (this.mTaskViewFocusFrame != null) {
            this.mTaskViewFocusFrame.moveGridTaskViewFocus(null);
        }
    }

    public final void onBusEvent(DismissFocusedTaskViewEvent dismissFocusedTaskViewEvent) {
        if (this.mFocusedTask != null) {
            if (this.mTaskViewFocusFrame != null) {
                this.mTaskViewFocusFrame.moveGridTaskViewFocus(null);
            }
            TaskView childViewForTask = getChildViewForTask(this.mFocusedTask);
            if (childViewForTask != null) {
                childViewForTask.dismissTask();
            }
            resetFocusedTask(this.mFocusedTask);
        }
    }

    public final void onBusEvent(DismissTaskViewEvent dismissTaskViewEvent) {
        this.mAnimationHelper.startDeleteTaskAnimation(dismissTaskViewEvent.taskView, useGridLayout(), dismissTaskViewEvent.getAnimationTrigger());
    }

    public final void onBusEvent(DismissAllTaskViewsEvent dismissAllTaskViewsEvent) {
        final ArrayList arrayList = new ArrayList(this.mStack.getTasks());
        this.mAnimationHelper.startDeleteAllTasksAnimation(getTaskViews(), useGridLayout(), dismissAllTaskViewsEvent.getAnimationTrigger());
        dismissAllTaskViewsEvent.addPostAnimationCallback(new Runnable() {
            @Override
            public void run() {
                TaskStackView.this.announceForAccessibility(TaskStackView.this.getContext().getString(R.string.accessibility_recents_all_items_dismissed));
                TaskStackView.this.mStack.removeAllTasks(true);
                for (int size = arrayList.size() - 1; size >= 0; size--) {
                    EventBus.getDefault().send(new DeleteTaskDataEvent((Task) arrayList.get(size)));
                }
                MetricsLogger.action(TaskStackView.this.getContext(), 357);
            }
        });
    }

    public final void onBusEvent(TaskViewDismissedEvent taskViewDismissedEvent) {
        announceForAccessibility(getContext().getString(R.string.accessibility_recents_item_dismissed, taskViewDismissedEvent.task.title));
        if (useGridLayout() && taskViewDismissedEvent.animation != null) {
            taskViewDismissedEvent.animation.setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    if (TaskStackView.this.mTaskViewFocusFrame != null) {
                        TaskStackView.this.mTaskViewFocusFrame.resize();
                    }
                }
            });
        }
        this.mStack.removeTask(taskViewDismissedEvent.task, taskViewDismissedEvent.animation, false);
        EventBus.getDefault().send(new DeleteTaskDataEvent(taskViewDismissedEvent.task));
        if (this.mStack.getTaskCount() > 0 && Recents.getConfiguration().isLowRamDevice) {
            EventBus.getDefault().send(new ShowStackActionButtonEvent(false));
        }
        MetricsLogger.action(getContext(), 289, taskViewDismissedEvent.task.key.getComponent().toString());
    }

    public final void onBusEvent(FocusNextTaskViewEvent focusNextTaskViewEvent) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        setRelativeFocusedTask(true, false, true, false, 0);
    }

    public final void onBusEvent(FocusPreviousTaskViewEvent focusPreviousTaskViewEvent) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        setRelativeFocusedTask(false, false, true);
    }

    public final void onBusEvent(NavigateTaskViewEvent navigateTaskViewEvent) {
        if (useGridLayout()) {
            setFocusedTask(this.mLayoutAlgorithm.mTaskGridLayoutAlgorithm.navigateFocus(this.mStack.getTaskCount(), this.mStack.indexOfTask(getFocusedTask()), navigateTaskViewEvent.direction), false, true);
        } else {
            switch (navigateTaskViewEvent.direction) {
                case UP:
                    EventBus.getDefault().send(new FocusPreviousTaskViewEvent());
                    break;
                case DOWN:
                    EventBus.getDefault().send(new FocusNextTaskViewEvent());
                    break;
            }
        }
    }

    public final void onBusEvent(UserInteractionEvent userInteractionEvent) {
        TaskView childViewForTask;
        this.mUIDozeTrigger.poke();
        Recents.getDebugFlags();
        if (this.mFocusedTask != null && (childViewForTask = getChildViewForTask(this.mFocusedTask)) != null) {
            childViewForTask.getHeaderView().cancelFocusTimerIndicator();
        }
    }

    public final void onBusEvent(DragStartEvent dragStartEvent) {
        addIgnoreTask(dragStartEvent.task);
        float scaleX = dragStartEvent.taskView.getScaleX() * 1.05f;
        this.mLayoutAlgorithm.getStackTransform(dragStartEvent.task, getScroller().getStackScroll(), this.mTmpTransform, null);
        this.mTmpTransform.scale = scaleX;
        this.mTmpTransform.translationZ = this.mLayoutAlgorithm.mMaxTranslationZ + 1;
        this.mTmpTransform.dimAlpha = 0.0f;
        updateTaskViewToTransform(dragStartEvent.taskView, this.mTmpTransform, new AnimationProps(175, Interpolators.FAST_OUT_SLOW_IN));
    }

    public final void onBusEvent(DragDropTargetChangedEvent dragDropTargetChangedEvent) {
        AnimationProps animationProps = new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN);
        boolean z = true;
        if (dragDropTargetChangedEvent.dropTarget instanceof DockState) {
            DockState dockState = (DockState) dragDropTargetChangedEvent.dropTarget;
            Rect rect = new Rect(this.mStableLayoutAlgorithm.mSystemInsets);
            int measuredHeight = getMeasuredHeight() - rect.bottom;
            rect.bottom = 0;
            this.mStackBounds.set(dockState.getDockedTaskStackBounds(this.mDisplayRect, getMeasuredWidth(), measuredHeight, this.mDividerSize, rect, this.mLayoutAlgorithm, getResources(), this.mWindowRect));
            this.mLayoutAlgorithm.setSystemInsets(rect);
            this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds);
            updateLayoutAlgorithm(true);
        } else {
            removeIgnoreTask(dragDropTargetChangedEvent.task);
            updateLayoutToStableBounds();
            addIgnoreTask(dragDropTargetChangedEvent.task);
            z = false;
        }
        relayoutTaskViews(animationProps, null, z);
    }

    public final void onBusEvent(DragEndEvent dragEndEvent) {
        if (dragEndEvent.dropTarget instanceof DockState) {
            this.mLayoutAlgorithm.clearUnfocusedTaskOverrides();
            return;
        }
        removeIgnoreTask(dragEndEvent.task);
        Utilities.setViewFrameFromTranslation(dragEndEvent.taskView);
        new ArrayMap().put(dragEndEvent.task, new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN, dragEndEvent.getAnimationTrigger().decrementOnAnimationEnd()));
        relayoutTaskViews(new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN));
        dragEndEvent.getAnimationTrigger().increment();
    }

    public final void onBusEvent(DragEndCancelledEvent dragEndCancelledEvent) {
        removeIgnoreTask(dragEndCancelledEvent.task);
        updateLayoutToStableBounds();
        Utilities.setViewFrameFromTranslation(dragEndCancelledEvent.taskView);
        new ArrayMap().put(dragEndCancelledEvent.task, new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN, dragEndCancelledEvent.getAnimationTrigger().decrementOnAnimationEnd()));
        relayoutTaskViews(new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN));
        dragEndCancelledEvent.getAnimationTrigger().increment();
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent enterRecentsWindowAnimationCompletedEvent) {
        this.mEnterAnimationComplete = true;
        tryStartEnterAnimation();
    }

    private void tryStartEnterAnimation() {
        if (!this.mStackReloaded || !this.mFinishedLayoutAfterStackReload || !this.mEnterAnimationComplete) {
            return;
        }
        if (this.mStack.getTaskCount() > 0) {
            ReferenceCountedTrigger referenceCountedTrigger = new ReferenceCountedTrigger();
            this.mAnimationHelper.startEnterAnimation(referenceCountedTrigger);
            referenceCountedTrigger.addLastDecrementRunnable(new Runnable() {
                @Override
                public final void run() {
                    TaskStackView.lambda$tryStartEnterAnimation$1(this.f$0);
                }
            });
        }
        this.mStackReloaded = false;
    }

    public static void lambda$tryStartEnterAnimation$1(TaskStackView taskStackView) {
        taskStackView.mUIDozeTrigger.startDozing();
        if (taskStackView.mFocusedTask != null) {
            taskStackView.setFocusedTask(taskStackView.mStack.indexOfTask(taskStackView.mFocusedTask), false, Recents.getConfiguration().getLaunchState().launchedWithAltTab);
            TaskView childViewForTask = taskStackView.getChildViewForTask(taskStackView.mFocusedTask);
            if (taskStackView.mTouchExplorationEnabled && childViewForTask != null) {
                childViewForTask.requestAccessibilityFocus();
            }
        }
    }

    public final void onBusEvent(final MultiWindowStateChangedEvent multiWindowStateChangedEvent) {
        if (multiWindowStateChangedEvent.inMultiWindow || !multiWindowStateChangedEvent.showDeferredAnimation) {
            setTasks(multiWindowStateChangedEvent.stack, true);
            return;
        }
        Recents.getConfiguration().getLaunchState().reset();
        multiWindowStateChangedEvent.getAnimationTrigger().increment();
        post(new Runnable() {
            @Override
            public void run() {
                TaskStackView.this.mAnimationHelper.startNewStackScrollAnimation(multiWindowStateChangedEvent.stack, multiWindowStateChangedEvent.getAnimationTrigger());
                multiWindowStateChangedEvent.getAnimationTrigger().decrement();
            }
        });
    }

    public final void onBusEvent(ConfigurationChangedEvent configurationChangedEvent) {
        if (configurationChangedEvent.fromDeviceOrientationChange) {
            this.mDisplayOrientation = Utilities.getAppConfiguration(this.mContext).orientation;
            this.mDisplayRect = Recents.getSystemServices().getDisplayRect();
            this.mStackScroller.stopScroller();
        }
        reloadOnConfigurationChange();
        if (!configurationChangedEvent.fromMultiWindow) {
            this.mTmpTaskViews.clear();
            this.mTmpTaskViews.addAll(getTaskViews());
            this.mTmpTaskViews.addAll(this.mViewPool.getViews());
            int size = this.mTmpTaskViews.size();
            for (int i = 0; i < size; i++) {
                this.mTmpTaskViews.get(i).onConfigurationChanged();
            }
        }
        updateStackActionButtonVisibility();
        if (configurationChangedEvent.fromMultiWindow && this.mInitialState == 0) {
            this.mInitialState = 2;
            requestLayout();
        } else if (configurationChangedEvent.fromDeviceOrientationChange) {
            this.mInitialState = 1;
            requestLayout();
        }
    }

    public final void onBusEvent(RecentsGrowingEvent recentsGrowingEvent) {
        this.mResetToInitialStateWhenResized = true;
    }

    public final void onBusEvent(RecentsVisibilityChangedEvent recentsVisibilityChangedEvent) {
        if (!recentsVisibilityChangedEvent.visible) {
            if (this.mTaskViewFocusFrame != null) {
                this.mTaskViewFocusFrame.moveGridTaskViewFocus(null);
            }
            ArrayList arrayList = new ArrayList(getTaskViews());
            for (int i = 0; i < arrayList.size(); i++) {
                this.mViewPool.returnViewToPool((TaskView) arrayList.get(i));
            }
            clearPrefetchingTask();
            this.mEnterAnimationComplete = false;
        }
    }

    public final void onBusEvent(ActivityPinnedEvent activityPinnedEvent) {
        Task taskFindTaskWithId = this.mStack.findTaskWithId(activityPinnedEvent.taskId);
        if (taskFindTaskWithId != null) {
            this.mStack.removeTask(taskFindTaskWithId, AnimationProps.IMMEDIATE, false, false);
        }
        updateLayoutAlgorithm(false);
        updateToInitialState();
    }

    public void reloadOnConfigurationChange() {
        this.mStableLayoutAlgorithm.reloadOnConfigurationChange(getContext());
        this.mLayoutAlgorithm.reloadOnConfigurationChange(getContext());
    }

    private int findTaskViewInsertIndex(Task task, int i) {
        if (i != -1) {
            List<TaskView> taskViews = getTaskViews();
            int size = taskViews.size();
            boolean z = false;
            for (int i2 = 0; i2 < size; i2++) {
                Task task2 = taskViews.get(i2).getTask();
                if (task2 == task) {
                    z = true;
                } else if (i < this.mStack.indexOfTask(task2)) {
                    if (z) {
                        return i2 - 1;
                    }
                    return i2;
                }
            }
        }
        return -1;
    }

    private void launchTask(final Task task) {
        cancelAllTaskViewAnimations();
        float stackScroll = this.mStackScroller.getStackScroll();
        float stackScrollForTaskAtInitialOffset = this.mLayoutAlgorithm.getStackScrollForTaskAtInitialOffset(task);
        float fAbs = Math.abs(stackScrollForTaskAtInitialOffset - stackScroll);
        if (getChildViewForTask(task) == null || fAbs > 0.35f) {
            this.mStackScroller.animateScroll(stackScrollForTaskAtInitialOffset, (int) (216.0f + (fAbs * 32.0f)), new Runnable() {
                @Override
                public void run() {
                    EventBus.getDefault().send(new LaunchTaskEvent(TaskStackView.this.getChildViewForTask(task), task, null, false));
                }
            });
        } else {
            EventBus.getDefault().send(new LaunchTaskEvent(getChildViewForTask(task), task, null, false));
        }
    }

    public boolean useGridLayout() {
        return this.mLayoutAlgorithm.useGridLayout();
    }

    private void readSystemFlags() {
        this.mTouchExplorationEnabled = Recents.getSystemServices().isTouchExplorationEnabled();
        this.mScreenPinningEnabled = ActivityManagerWrapper.getInstance().isScreenPinningEnabled() && !ActivityManagerWrapper.getInstance().isLockToAppActive();
    }

    private void updateStackActionButtonVisibility() {
        if (Recents.getConfiguration().isLowRamDevice) {
            return;
        }
        if (useGridLayout() || (this.mStackScroller.getStackScroll() < 0.3f && this.mStack.getTaskCount() > 0)) {
            EventBus.getDefault().send(new ShowStackActionButtonEvent(false));
        } else {
            EventBus.getDefault().send(new HideStackActionButtonEvent());
        }
    }

    private int getInitialFocusTaskIndex(RecentsActivityLaunchState recentsActivityLaunchState, int i, boolean z) {
        if (recentsActivityLaunchState.launchedFromApp) {
            if (z) {
                return i - 1;
            }
            return Math.max(0, i - 2);
        }
        return i - 1;
    }

    private void matchTaskListSize(List<Task> list, List<TaskViewTransform> list2) {
        int size = list2.size();
        int size2 = list.size();
        if (size < size2) {
            while (size < size2) {
                list2.add(new TaskViewTransform());
                size++;
            }
        } else if (size > size2) {
            list2.subList(size2, size).clear();
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        String hexString = Integer.toHexString(System.identityHashCode(this));
        printWriter.print(str);
        printWriter.print("TaskStackView");
        printWriter.print(" hasDefRelayout=");
        printWriter.print(this.mDeferredTaskViewLayoutAnimation != null ? "Y" : "N");
        printWriter.print(" clipDirty=");
        printWriter.print(this.mTaskViewsClipDirty ? "Y" : "N");
        printWriter.print(" awaitingStackReload=");
        printWriter.print(this.mFinishedLayoutAfterStackReload ? "Y" : "N");
        printWriter.print(" initialState=");
        printWriter.print(this.mInitialState);
        printWriter.print(" inMeasureLayout=");
        printWriter.print(this.mInMeasureLayout ? "Y" : "N");
        printWriter.print(" enterAnimCompleted=");
        printWriter.print(this.mEnterAnimationComplete ? "Y" : "N");
        printWriter.print(" touchExplorationOn=");
        printWriter.print(this.mTouchExplorationEnabled ? "Y" : "N");
        printWriter.print(" screenPinningOn=");
        printWriter.print(this.mScreenPinningEnabled ? "Y" : "N");
        printWriter.print(" numIgnoreTasks=");
        printWriter.print(this.mIgnoreTasks.size());
        printWriter.print(" numViewPool=");
        printWriter.print(this.mViewPool.getViews().size());
        printWriter.print(" stableStackBounds=");
        printWriter.print(Utilities.dumpRect(this.mStableStackBounds));
        printWriter.print(" stackBounds=");
        printWriter.print(Utilities.dumpRect(this.mStackBounds));
        printWriter.print(" stableWindow=");
        printWriter.print(Utilities.dumpRect(this.mStableWindowRect));
        printWriter.print(" window=");
        printWriter.print(Utilities.dumpRect(this.mWindowRect));
        printWriter.print(" display=");
        printWriter.print(Utilities.dumpRect(this.mDisplayRect));
        printWriter.print(" orientation=");
        printWriter.print(this.mDisplayOrientation);
        printWriter.print(" [0x");
        printWriter.print(hexString);
        printWriter.print("]");
        printWriter.println();
        if (this.mFocusedTask != null) {
            printWriter.print(str2);
            printWriter.print("Focused task: ");
            this.mFocusedTask.dump("", printWriter);
        }
        int size = this.mTaskViews.size();
        for (int i = 0; i < size; i++) {
            this.mTaskViews.get(i).dump(str2, printWriter);
        }
        this.mLayoutAlgorithm.dump(str2, printWriter);
        this.mStackScroller.dump(str2, printWriter);
    }
}
