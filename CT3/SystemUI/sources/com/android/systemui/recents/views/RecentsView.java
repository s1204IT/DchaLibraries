package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewPropertyAnimator;
import android.view.WindowInsets;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.HideStackActionButtonEvent;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.ShowStackActionButtonEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEndedEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.RecentsTransitionHelper;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class RecentsView extends FrameLayout {
    private boolean mAwaitingFirstLayout;
    private Drawable mBackgroundScrim;
    private Animator mBackgroundScrimAnimator;
    private int mDividerSize;
    private TextView mEmptyView;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private boolean mLastTaskLaunchedWasFreeform;
    private TaskStack mStack;
    private TextView mStackActionButton;

    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mSystemInsets;
    private TaskStackView mTaskStackView;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "touch_")
    private RecentsViewTouchHandler mTouchHandler;
    private RecentsTransitionHelper mTransitionHelper;

    public RecentsView(Context context) {
        this(context, null);
    }

    public RecentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mAwaitingFirstLayout = true;
        this.mSystemInsets = new Rect();
        this.mBackgroundScrim = new ColorDrawable(Color.argb(84, 0, 0, 0)).mutate();
        setWillNotDraw(false);
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mTransitionHelper = new RecentsTransitionHelper(getContext());
        this.mDividerSize = ssp.getDockedDividerSize(context);
        this.mTouchHandler = new RecentsViewTouchHandler(this);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
        LayoutInflater inflater = LayoutInflater.from(context);
        final float cornerRadius = context.getResources().getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        this.mStackActionButton = (TextView) inflater.inflate(R.layout.recents_stack_action_button, (ViewGroup) this, false);
        this.mStackActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().send(new DismissAllTaskViewsEvent());
            }
        });
        addView(this.mStackActionButton);
        this.mStackActionButton.setClipToOutline(true);
        this.mStackActionButton.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        this.mEmptyView = (TextView) inflater.inflate(R.layout.recents_empty, (ViewGroup) this, false);
        addView(this.mEmptyView);
    }

    public void onReload(boolean isResumingFromVisible, boolean isTaskStackEmpty) {
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();
        if (this.mTaskStackView == null) {
            isResumingFromVisible = false;
            this.mTaskStackView = new TaskStackView(getContext());
            this.mTaskStackView.setSystemInsets(this.mSystemInsets);
            addView(this.mTaskStackView);
        }
        this.mAwaitingFirstLayout = !isResumingFromVisible;
        this.mLastTaskLaunchedWasFreeform = false;
        this.mTaskStackView.onReload(isResumingFromVisible);
        if (isResumingFromVisible) {
            animateBackgroundScrim(1.0f, 200);
        } else if (launchState.launchedViaDockGesture || launchState.launchedFromApp || isTaskStackEmpty) {
            this.mBackgroundScrim.setAlpha(255);
        } else {
            this.mBackgroundScrim.setAlpha(0);
        }
    }

    public void updateStack(TaskStack stack, boolean setStackViewTasks) {
        this.mStack = stack;
        if (setStackViewTasks) {
            this.mTaskStackView.setTasks(stack, true);
        }
        if (stack.getTaskCount() > 0) {
            hideEmptyView();
        } else {
            showEmptyView(R.string.recents_empty_message);
        }
    }

    public TaskStack getStack() {
        return this.mStack;
    }

    public Drawable getBackgroundScrim() {
        return this.mBackgroundScrim;
    }

    public boolean isLastTaskLaunchedFreeform() {
        return this.mLastTaskLaunchedWasFreeform;
    }

    public boolean launchFocusedTask(int logEvent) {
        Task task;
        if (this.mTaskStackView == null || (task = this.mTaskStackView.getFocusedTask()) == null) {
            return false;
        }
        TaskView taskView = this.mTaskStackView.getChildViewForTask(task);
        EventBus.getDefault().send(new LaunchTaskEvent(taskView, task, null, -1, false));
        if (logEvent != 0) {
            MetricsLogger.action(getContext(), logEvent, task.key.getComponent().toString());
            return true;
        }
        return true;
    }

    public boolean launchPreviousTask() {
        if (this.mTaskStackView != null) {
            TaskStack stack = this.mTaskStackView.getStack();
            Task task = stack.getLaunchTarget();
            if (task != null) {
                TaskView taskView = this.mTaskStackView.getChildViewForTask(task);
                EventBus.getDefault().send(new LaunchTaskEvent(taskView, task, null, -1, false));
                return true;
            }
        }
        return false;
    }

    public void showEmptyView(int msgResId) {
        this.mTaskStackView.setVisibility(4);
        this.mEmptyView.setText(msgResId);
        this.mEmptyView.setVisibility(0);
        this.mEmptyView.bringToFront();
        this.mStackActionButton.bringToFront();
    }

    public void hideEmptyView() {
        this.mEmptyView.setVisibility(4);
        this.mTaskStackView.setVisibility(0);
        this.mTaskStackView.bringToFront();
        this.mStackActionButton.bringToFront();
    }

    @Override
    protected void onAttachedToWindow() {
        EventBus.getDefault().register(this, 3);
        EventBus.getDefault().register(this.mTouchHandler, 4);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().unregister(this.mTouchHandler);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        if (this.mTaskStackView.getVisibility() != 8) {
            this.mTaskStackView.measure(widthMeasureSpec, heightMeasureSpec);
        }
        if (this.mEmptyView.getVisibility() != 8) {
            measureChild(this.mEmptyView, View.MeasureSpec.makeMeasureSpec(width, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(height, Integer.MIN_VALUE));
        }
        Rect buttonBounds = this.mTaskStackView.mLayoutAlgorithm.mStackActionButtonRect;
        measureChild(this.mStackActionButton, View.MeasureSpec.makeMeasureSpec(buttonBounds.width(), Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(buttonBounds.height(), Integer.MIN_VALUE));
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mTaskStackView.getVisibility() != 8) {
            this.mTaskStackView.layout(left, top, getMeasuredWidth() + left, getMeasuredHeight() + top);
        }
        if (this.mEmptyView.getVisibility() != 8) {
            int leftRightInsets = this.mSystemInsets.left + this.mSystemInsets.right;
            int topBottomInsets = this.mSystemInsets.top + this.mSystemInsets.bottom;
            int childWidth = this.mEmptyView.getMeasuredWidth();
            int childHeight = this.mEmptyView.getMeasuredHeight();
            int childLeft = this.mSystemInsets.left + left + (Math.max(0, ((right - left) - leftRightInsets) - childWidth) / 2);
            int childTop = this.mSystemInsets.top + top + (Math.max(0, ((bottom - top) - topBottomInsets) - childHeight) / 2);
            this.mEmptyView.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
        Rect buttonBounds = getStackActionButtonBoundsFromStackLayout();
        this.mStackActionButton.layout(buttonBounds.left, buttonBounds.top, buttonBounds.right, buttonBounds.bottom);
        if (!this.mAwaitingFirstLayout) {
            return;
        }
        this.mAwaitingFirstLayout = false;
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (launchState.launchedViaDragGesture) {
            setTranslationY(getMeasuredHeight());
        } else {
            setTranslationY(0.0f);
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mSystemInsets.set(insets.getSystemWindowInsets());
        this.mTaskStackView.setSystemInsets(this.mSystemInsets);
        requestLayout();
        return insets;
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
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        ArrayList<TaskStack.DockState> visDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int i = visDockStates.size() - 1; i >= 0; i--) {
            visDockStates.get(i).viewState.draw(canvas);
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        ArrayList<TaskStack.DockState> visDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int i = visDockStates.size() - 1; i >= 0; i--) {
            Drawable d = visDockStates.get(i).viewState.dockAreaOverlay;
            if (d == who) {
                return true;
            }
        }
        return super.verifyDrawable(who);
    }

    public final void onBusEvent(LaunchTaskEvent event) {
        this.mLastTaskLaunchedWasFreeform = event.task.isFreeformTask();
        this.mTransitionHelper.launchTaskFromRecents(this.mStack, event.task, this.mTaskStackView, event.taskView, event.screenPinningRequested, event.targetTaskBounds, event.targetTaskStack);
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted event) {
        hideStackActionButton(200, false);
        animateBackgroundScrim(0.0f, 200);
    }

    public final void onBusEvent(DragStartEvent event) {
        updateVisibleDockRegions(this.mTouchHandler.getDockStatesForCurrentOrientation(), true, TaskStack.DockState.NONE.viewState.dockAreaAlpha, TaskStack.DockState.NONE.viewState.hintTextAlpha, true, false);
        if (this.mStackActionButton != null) {
            this.mStackActionButton.animate().alpha(0.0f).setDuration(100L).setInterpolator(Interpolators.ALPHA_OUT).start();
        }
    }

    public final void onBusEvent(DragDropTargetChangedEvent event) {
        if (event.dropTarget == null || !(event.dropTarget instanceof TaskStack.DockState)) {
            updateVisibleDockRegions(this.mTouchHandler.getDockStatesForCurrentOrientation(), true, TaskStack.DockState.NONE.viewState.dockAreaAlpha, TaskStack.DockState.NONE.viewState.hintTextAlpha, true, true);
        } else {
            TaskStack.DockState dockState = (TaskStack.DockState) event.dropTarget;
            updateVisibleDockRegions(new TaskStack.DockState[]{dockState}, false, -1, -1, true, true);
        }
        if (this.mStackActionButton == null) {
            return;
        }
        event.addPostAnimationCallback(new Runnable() {
            @Override
            public void run() {
                Rect buttonBounds = RecentsView.this.getStackActionButtonBoundsFromStackLayout();
                RecentsView.this.mStackActionButton.setLeftTopRightBottom(buttonBounds.left, buttonBounds.top, buttonBounds.right, buttonBounds.bottom);
            }
        });
    }

    public final void onBusEvent(final DragEndEvent event) {
        if (event.dropTarget instanceof TaskStack.DockState) {
            TaskStack.DockState dockState = (TaskStack.DockState) event.dropTarget;
            updateVisibleDockRegions(null, false, -1, -1, false, false);
            Utilities.setViewFrameFromTranslation(event.taskView);
            SystemServicesProxy ssp = Recents.getSystemServices();
            if (ssp.startTaskInDockedMode(event.task.key.id, dockState.createMode)) {
                ActivityOptions.OnAnimationStartedListener startedListener = new ActivityOptions.OnAnimationStartedListener() {
                    public void onAnimationStarted() {
                        EventBus.getDefault().send(new DockedFirstAnimationFrameEvent());
                        RecentsView.this.mTaskStackView.getStack().removeTask(event.task, null, true);
                    }
                };
                final Rect taskRect = getTaskRect(event.taskView);
                IAppTransitionAnimationSpecsFuture future = this.mTransitionHelper.getAppTransitionFuture(new RecentsTransitionHelper.AnimationSpecComposer() {
                    @Override
                    public List<AppTransitionAnimationSpec> composeSpecs() {
                        return RecentsView.this.mTransitionHelper.composeDockAnimationSpec(event.taskView, taskRect);
                    }
                });
                ssp.overridePendingAppTransitionMultiThumbFuture(future, this.mTransitionHelper.wrapStartedListener(startedListener), true);
                MetricsLogger.action(this.mContext, 270, event.task.getTopComponent().flattenToShortString());
            } else {
                EventBus.getDefault().send(new DragEndCancelledEvent(this.mStack, event.task, event.taskView));
            }
        } else {
            updateVisibleDockRegions(null, true, -1, -1, true, false);
        }
        if (this.mStackActionButton != null) {
            this.mStackActionButton.animate().alpha(1.0f).setDuration(134L).setInterpolator(Interpolators.ALPHA_IN).start();
        }
    }

    public final void onBusEvent(DragEndCancelledEvent event) {
        updateVisibleDockRegions(null, true, -1, -1, true, false);
    }

    private Rect getTaskRect(TaskView taskView) {
        int[] location = taskView.getLocationOnScreen();
        int viewX = location[0];
        int viewY = location[1];
        return new Rect(viewX, viewY, (int) (viewX + (taskView.getWidth() * taskView.getScaleX())), (int) (viewY + (taskView.getHeight() * taskView.getScaleY())));
    }

    public final void onBusEvent(DraggingInRecentsEvent event) {
        if (this.mTaskStackView.getTaskViews().size() <= 0) {
            return;
        }
        setTranslationY(event.distanceFromTop - this.mTaskStackView.getTaskViews().get(0).getY());
    }

    public final void onBusEvent(DraggingInRecentsEndedEvent event) {
        ViewPropertyAnimator animator = animate();
        if (event.velocity > this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            animator.translationY(getHeight());
            animator.withEndAction(new Runnable() {
                @Override
                public void run() {
                    WindowManagerProxy.getInstance().maximizeDockedStack();
                }
            });
            this.mFlingAnimationUtils.apply(animator, getTranslationY(), getHeight(), event.velocity);
        } else {
            animator.translationY(0.0f);
            animator.setListener(null);
            this.mFlingAnimationUtils.apply(animator, getTranslationY(), 0.0f, event.velocity);
        }
        animator.start();
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent event) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (launchState.launchedViaDockGesture || launchState.launchedFromApp || this.mStack.getTaskCount() <= 0) {
            return;
        }
        animateBackgroundScrim(1.0f, 300);
    }

    public final void onBusEvent(AllTaskViewsDismissedEvent event) {
        hideStackActionButton(100, true);
    }

    public final void onBusEvent(DismissAllTaskViewsEvent event) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.hasDockedTask()) {
            return;
        }
        animateBackgroundScrim(0.0f, 200);
    }

    public final void onBusEvent(ShowStackActionButtonEvent event) {
        showStackActionButton(134, event.translate);
    }

    public final void onBusEvent(HideStackActionButtonEvent event) {
        hideStackActionButton(100, true);
    }

    public final void onBusEvent(MultiWindowStateChangedEvent event) {
        updateStack(event.stack, false);
    }

    private void showStackActionButton(final int duration, final boolean translate) {
        ReferenceCountedTrigger postAnimationTrigger = new ReferenceCountedTrigger();
        if (this.mStackActionButton.getVisibility() == 4) {
            this.mStackActionButton.setVisibility(0);
            this.mStackActionButton.setAlpha(0.0f);
            if (translate) {
                this.mStackActionButton.setTranslationY((-this.mStackActionButton.getMeasuredHeight()) * 0.25f);
            } else {
                this.mStackActionButton.setTranslationY(0.0f);
            }
            postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                @Override
                public void run() {
                    if (translate) {
                        RecentsView.this.mStackActionButton.animate().translationY(0.0f);
                    }
                    RecentsView.this.mStackActionButton.animate().alpha(1.0f).setDuration(duration).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).start();
                }
            });
        }
        postAnimationTrigger.flushLastDecrementRunnables();
    }

    private void hideStackActionButton(int duration, boolean translate) {
        ReferenceCountedTrigger postAnimationTrigger = new ReferenceCountedTrigger();
        hideStackActionButton(duration, translate, postAnimationTrigger);
        postAnimationTrigger.flushLastDecrementRunnables();
    }

    private void hideStackActionButton(int duration, boolean translate, final ReferenceCountedTrigger postAnimationTrigger) {
        if (this.mStackActionButton.getVisibility() == 0) {
            if (translate) {
                this.mStackActionButton.animate().translationY((-this.mStackActionButton.getMeasuredHeight()) * 0.25f);
            }
            this.mStackActionButton.animate().alpha(0.0f).setDuration(duration).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).withEndAction(new Runnable() {
                @Override
                public void run() {
                    RecentsView.this.mStackActionButton.setVisibility(4);
                    postAnimationTrigger.decrement();
                }
            }).start();
            postAnimationTrigger.increment();
        }
    }

    private void updateVisibleDockRegions(TaskStack.DockState[] newDockStates, boolean isDefaultDockState, int overrideAreaAlpha, int overrideHintAlpha, boolean animateAlpha, boolean animateBounds) {
        int areaAlpha;
        int hintAlpha;
        Rect bounds;
        ArraySet<TaskStack.DockState> newDockStatesSet = Utilities.arrayToSet(newDockStates, new ArraySet());
        ArrayList<TaskStack.DockState> visDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int i = visDockStates.size() - 1; i >= 0; i--) {
            TaskStack.DockState dockState = visDockStates.get(i);
            TaskStack.DockState.ViewState viewState = dockState.viewState;
            if (newDockStates == null || !newDockStatesSet.contains(dockState)) {
                viewState.startAnimation(null, 0, 0, 250, Interpolators.FAST_OUT_SLOW_IN, animateAlpha, animateBounds);
            } else {
                if (overrideAreaAlpha != -1) {
                    areaAlpha = overrideAreaAlpha;
                } else {
                    areaAlpha = viewState.dockAreaAlpha;
                }
                if (overrideHintAlpha != -1) {
                    hintAlpha = overrideHintAlpha;
                } else {
                    hintAlpha = viewState.hintTextAlpha;
                }
                if (isDefaultDockState) {
                    bounds = dockState.getPreDockedBounds(getMeasuredWidth(), getMeasuredHeight());
                } else {
                    bounds = dockState.getDockedBounds(getMeasuredWidth(), getMeasuredHeight(), this.mDividerSize, this.mSystemInsets, getResources());
                }
                if (viewState.dockAreaOverlay.getCallback() != this) {
                    viewState.dockAreaOverlay.setCallback(this);
                    viewState.dockAreaOverlay.setBounds(bounds);
                }
                viewState.startAnimation(bounds, areaAlpha, hintAlpha, 250, Interpolators.FAST_OUT_SLOW_IN, animateAlpha, animateBounds);
            }
        }
    }

    private void animateBackgroundScrim(float alpha, int duration) {
        Interpolator interpolator;
        Utilities.cancelAnimationWithoutCallbacks(this.mBackgroundScrimAnimator);
        int fromAlpha = (int) ((this.mBackgroundScrim.getAlpha() / 84.15f) * 255.0f);
        int toAlpha = (int) (alpha * 255.0f);
        this.mBackgroundScrimAnimator = ObjectAnimator.ofInt(this.mBackgroundScrim, Utilities.DRAWABLE_ALPHA, fromAlpha, toAlpha);
        this.mBackgroundScrimAnimator.setDuration(duration);
        Animator animator = this.mBackgroundScrimAnimator;
        if (toAlpha > fromAlpha) {
            interpolator = Interpolators.ALPHA_IN;
        } else {
            interpolator = Interpolators.ALPHA_OUT;
        }
        animator.setInterpolator(interpolator);
        this.mBackgroundScrimAnimator.start();
    }

    public Rect getStackActionButtonBoundsFromStackLayout() {
        int left;
        Rect actionButtonRect = new Rect(this.mTaskStackView.mLayoutAlgorithm.mStackActionButtonRect);
        if (isLayoutRtl()) {
            left = actionButtonRect.left - this.mStackActionButton.getPaddingLeft();
        } else {
            left = (actionButtonRect.right + this.mStackActionButton.getPaddingRight()) - this.mStackActionButton.getMeasuredWidth();
        }
        int top = actionButtonRect.top + ((actionButtonRect.height() - this.mStackActionButton.getMeasuredHeight()) / 2);
        actionButtonRect.set(left, top, this.mStackActionButton.getMeasuredWidth() + left, this.mStackActionButton.getMeasuredHeight() + top);
        return actionButtonRect;
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        String id = Integer.toHexString(System.identityHashCode(this));
        writer.print(prefix);
        writer.print("RecentsView");
        writer.print(" awaitingFirstLayout=");
        writer.print(this.mAwaitingFirstLayout ? "Y" : "N");
        writer.print(" insets=");
        writer.print(Utilities.dumpRect(this.mSystemInsets));
        writer.print(" [0x");
        writer.print(id);
        writer.print("]");
        writer.println();
        if (this.mStack != null) {
            this.mStack.dump(innerPrefix, writer);
        }
        if (this.mTaskStackView == null) {
            return;
        }
        this.mTaskStackView.dump(innerPrefix, writer);
    }
}
