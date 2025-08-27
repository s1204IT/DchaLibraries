package com.android.systemui.recents.views;

import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.drawable.GradientDrawable;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.Utils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.ExitRecentsWindowFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.HideStackActionButtonEvent;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskStartedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskSucceededEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.ShowEmptyViewEvent;
import com.android.systemui.recents.events.activity.ShowStackActionButtonEvent;
import com.android.systemui.recents.events.component.ExpandPipEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.component.SetWaitingForTransitionStartEvent;
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
import com.android.systemui.recents.views.DockState;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.TaskStack;
import com.android.systemui.shared.recents.utilities.Utilities;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecCompat;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture;
import com.android.systemui.shared.recents.view.RecentsTransition;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class RecentsView extends FrameLayout {
    private boolean mAwaitingFirstLayout;
    private GradientDrawable mBackgroundScrim;
    private ValueAnimator mBackgroundScrimAnimator;
    private float mBusynessFactor;
    private int mDividerSize;
    private TextView mEmptyView;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private Handler mHandler;
    private ColorDrawable mMultiWindowBackgroundScrim;
    private TextView mStackActionButton;
    private final int mStackButtonShadowColor;
    private final PointF mStackButtonShadowDistance;
    private final float mStackButtonShadowRadius;

    @ViewDebug.ExportedProperty(category = "recents")
    Rect mSystemInsets;
    private TaskStackView mTaskStackView;
    private Point mTmpDisplaySize;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "touch_")
    private RecentsViewTouchHandler mTouchHandler;
    private RecentsTransitionComposer mTransitionHelper;
    private final ValueAnimator.AnimatorUpdateListener mUpdateBackgroundScrimAlpha;

    public static /* synthetic */ void lambda$new$0(RecentsView recentsView, ValueAnimator valueAnimator) {
        int iIntValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
        recentsView.mBackgroundScrim.setAlpha(iIntValue);
        recentsView.mMultiWindowBackgroundScrim.setAlpha(iIntValue);
    }

    public RecentsView(Context context) {
        this(context, null);
    }

    public RecentsView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RecentsView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RecentsView(Context context, AttributeSet attributeSet, int i, int i2) {
        int i3;
        super(context, attributeSet, i, i2);
        this.mAwaitingFirstLayout = true;
        this.mSystemInsets = new Rect();
        this.mTmpDisplaySize = new Point();
        this.mUpdateBackgroundScrimAlpha = new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.recents.views.-$$Lambda$RecentsView$6rfoH9yP_J2fW6JDlOW4RINdzy4
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                RecentsView.lambda$new$0(this.f$0, valueAnimator);
            }
        };
        setWillNotDraw(false);
        SystemServicesProxy systemServices = Recents.getSystemServices();
        this.mHandler = new Handler();
        this.mTransitionHelper = new RecentsTransitionComposer(getContext());
        this.mDividerSize = systemServices.getDockedDividerSize(context);
        this.mTouchHandler = new RecentsViewTouchHandler(this);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
        this.mBackgroundScrim = new GradientDrawable(context);
        this.mMultiWindowBackgroundScrim = new ColorDrawable();
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(context);
        this.mEmptyView = (TextView) layoutInflaterFrom.inflate(R.layout.recents_empty, (ViewGroup) this, false);
        addView(this.mEmptyView);
        if (this.mStackActionButton != null) {
            removeView(this.mStackActionButton);
        }
        if (Recents.getConfiguration().isLowRamDevice) {
            i3 = R.layout.recents_low_ram_stack_action_button;
        } else {
            i3 = R.layout.recents_stack_action_button;
        }
        this.mStackActionButton = (TextView) layoutInflaterFrom.inflate(i3, (ViewGroup) this, false);
        this.mStackButtonShadowRadius = this.mStackActionButton.getShadowRadius();
        this.mStackButtonShadowDistance = new PointF(this.mStackActionButton.getShadowDx(), this.mStackActionButton.getShadowDy());
        this.mStackButtonShadowColor = this.mStackActionButton.getShadowColor();
        addView(this.mStackActionButton);
        reevaluateStyles();
    }

    public void reevaluateStyles() {
        int colorAttr = Utils.getColorAttr(this.mContext, R.attr.wallpaperTextColor);
        boolean z = Color.luminance(colorAttr) < 0.5f;
        this.mEmptyView.setTextColor(colorAttr);
        this.mEmptyView.setCompoundDrawableTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_enabled}}, new int[]{colorAttr}));
        if (this.mStackActionButton != null) {
            this.mStackActionButton.setTextColor(colorAttr);
            if (z) {
                this.mStackActionButton.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
            } else {
                this.mStackActionButton.setShadowLayer(this.mStackButtonShadowRadius, this.mStackButtonShadowDistance.x, this.mStackButtonShadowDistance.y, this.mStackButtonShadowColor);
            }
        }
        setSystemUiVisibility(1792 | (z ? 8208 : 0));
    }

    public void onReload(TaskStack taskStack, boolean z) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        boolean z2 = taskStack.getTaskCount() == 0;
        if (this.mTaskStackView == null) {
            this.mTaskStackView = new TaskStackView(getContext());
            this.mTaskStackView.setSystemInsets(this.mSystemInsets);
            addView(this.mTaskStackView);
            z = false;
        }
        this.mAwaitingFirstLayout = !z;
        this.mTaskStackView.onReload(z);
        updateStack(taskStack, true);
        updateBusyness();
        if (z) {
            animateBackgroundScrim(getOpaqueScrimAlpha(), 200);
            return;
        }
        if (launchState.launchedViaDockGesture || launchState.launchedFromApp || z2) {
            this.mBackgroundScrim.setAlpha((int) (getOpaqueScrimAlpha() * 255.0f));
        } else {
            this.mBackgroundScrim.setAlpha(0);
        }
        this.mMultiWindowBackgroundScrim.setAlpha(this.mBackgroundScrim.getAlpha());
    }

    public void updateStack(TaskStack taskStack, boolean z) {
        if (z) {
            this.mTaskStackView.setTasks(taskStack, true);
        }
        if (taskStack.getTaskCount() > 0) {
            hideEmptyView();
        } else {
            showEmptyView(R.string.recents_empty_message);
        }
    }

    public void updateScrimOpacity() {
        if (updateBusyness()) {
            animateBackgroundScrim(getOpaqueScrimAlpha(), 200);
        }
    }

    private boolean updateBusyness() {
        float fMin = Math.min(this.mTaskStackView.getStack().getTaskCount(), 3) / 3.0f;
        if (this.mBusynessFactor == fMin) {
            return false;
        }
        this.mBusynessFactor = fMin;
        return true;
    }

    public TaskStack getStack() {
        return this.mTaskStackView.getStack();
    }

    public void updateBackgroundScrim(Window window, boolean z) {
        if (z) {
            this.mBackgroundScrim.setCallback((Drawable.Callback) null);
            window.setBackgroundDrawable(this.mMultiWindowBackgroundScrim);
        } else {
            this.mMultiWindowBackgroundScrim.setCallback(null);
            window.setBackgroundDrawable(this.mBackgroundScrim);
        }
    }

    public boolean launchFocusedTask(int i) {
        Task focusedTask;
        if (this.mTaskStackView == null || (focusedTask = this.mTaskStackView.getFocusedTask()) == null) {
            return false;
        }
        EventBus.getDefault().send(new LaunchTaskEvent(this.mTaskStackView.getChildViewForTask(focusedTask), focusedTask, null, false));
        if (i != 0) {
            MetricsLogger.action(getContext(), i, focusedTask.key.getComponent().toString());
            return true;
        }
        return true;
    }

    public boolean launchPreviousTask() {
        Task launchTarget;
        if (Recents.getConfiguration().getLaunchState().launchedFromPipApp) {
            EventBus.getDefault().send(new ExpandPipEvent());
            return true;
        }
        if (this.mTaskStackView == null || (launchTarget = getStack().getLaunchTarget()) == null) {
            return false;
        }
        EventBus.getDefault().send(new LaunchTaskEvent(this.mTaskStackView.getChildViewForTask(launchTarget), launchTarget, null, false));
        return true;
    }

    public void showEmptyView(int i) {
        this.mTaskStackView.setVisibility(4);
        this.mEmptyView.setText(i);
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

    public void setScrimColors(ColorExtractor.GradientColors gradientColors, boolean z) {
        this.mBackgroundScrim.setColors(gradientColors, z);
        int alpha = this.mMultiWindowBackgroundScrim.getAlpha();
        this.mMultiWindowBackgroundScrim.setColor(gradientColors.getMainColor());
        this.mMultiWindowBackgroundScrim.setAlpha(alpha);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() throws NoSuchMethodException, SecurityException {
        EventBus.getDefault().register(this, 3);
        EventBus.getDefault().register(this.mTouchHandler, 4);
        super.onAttachedToWindow();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().unregister(this.mTouchHandler);
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        if (this.mTaskStackView.getVisibility() != 8) {
            this.mTaskStackView.measure(i, i2);
        }
        if (this.mEmptyView.getVisibility() != 8) {
            measureChild(this.mEmptyView, View.MeasureSpec.makeMeasureSpec(size, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(size2, Integer.MIN_VALUE));
        }
        Rect stackActionButtonRect = this.mTaskStackView.mLayoutAlgorithm.getStackActionButtonRect();
        measureChild(this.mStackActionButton, View.MeasureSpec.makeMeasureSpec(stackActionButtonRect.width(), Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(stackActionButtonRect.height(), Integer.MIN_VALUE));
        setMeasuredDimension(size, size2);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.mTaskStackView.getVisibility() != 8) {
            this.mTaskStackView.layout(i, i2, getMeasuredWidth() + i, getMeasuredHeight() + i2);
        }
        if (this.mEmptyView.getVisibility() != 8) {
            int i5 = this.mSystemInsets.left + this.mSystemInsets.right;
            int i6 = this.mSystemInsets.top + this.mSystemInsets.bottom;
            int measuredWidth = this.mEmptyView.getMeasuredWidth();
            int measuredHeight = this.mEmptyView.getMeasuredHeight();
            int iMax = this.mSystemInsets.left + i + (Math.max(0, ((i3 - i) - i5) - measuredWidth) / 2);
            int iMax2 = this.mSystemInsets.top + i2 + (Math.max(0, ((i4 - i2) - i6) - measuredHeight) / 2);
            this.mEmptyView.layout(iMax, iMax2, measuredWidth + iMax, measuredHeight + iMax2);
        }
        this.mContext.getDisplay().getRealSize(this.mTmpDisplaySize);
        this.mBackgroundScrim.setScreenSize(this.mTmpDisplaySize.x, this.mTmpDisplaySize.y);
        this.mBackgroundScrim.setBounds(i, i2, i3, i4);
        this.mMultiWindowBackgroundScrim.setBounds(0, 0, this.mTmpDisplaySize.x, this.mTmpDisplaySize.y);
        Rect stackActionButtonBoundsFromStackLayout = getStackActionButtonBoundsFromStackLayout();
        this.mStackActionButton.layout(stackActionButtonBoundsFromStackLayout.left, stackActionButtonBoundsFromStackLayout.top, stackActionButtonBoundsFromStackLayout.right, stackActionButtonBoundsFromStackLayout.bottom);
        if (this.mAwaitingFirstLayout) {
            this.mAwaitingFirstLayout = false;
            if (Recents.getConfiguration().getLaunchState().launchedViaDragGesture) {
                setTranslationY(getMeasuredHeight());
            } else {
                setTranslationY(0.0f);
            }
            if (Recents.getConfiguration().isLowRamDevice && this.mEmptyView.getVisibility() == 0) {
                animateEmptyView(true, null);
            }
        }
    }

    @Override // android.view.View
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mSystemInsets.set(windowInsets.getSystemWindowInsets());
        this.mTaskStackView.setSystemInsets(this.mSystemInsets);
        requestLayout();
        return windowInsets;
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onInterceptTouchEvent(motionEvent);
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onTouchEvent(motionEvent);
    }

    @Override // android.view.View
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        ArrayList<DockState> visibleDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int size = visibleDockStates.size() - 1; size >= 0; size--) {
            visibleDockStates.get(size).viewState.draw(canvas);
        }
    }

    @Override // android.view.View
    protected boolean verifyDrawable(Drawable drawable) {
        ArrayList<DockState> visibleDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int size = visibleDockStates.size() - 1; size >= 0; size--) {
            if (visibleDockStates.get(size).viewState.dockAreaOverlay == drawable) {
                return true;
            }
        }
        return super.verifyDrawable(drawable);
    }

    public final void onBusEvent(LaunchTaskEvent launchTaskEvent) {
        launchTaskFromRecents(getStack(), launchTaskEvent.task, this.mTaskStackView, launchTaskEvent.taskView, launchTaskEvent.screenPinningRequested, launchTaskEvent.targetWindowingMode, launchTaskEvent.targetActivityType);
        if (Recents.getConfiguration().isLowRamDevice) {
            EventBus.getDefault().send(new HideStackActionButtonEvent(false));
        }
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted dismissRecentsToHomeAnimationStarted) {
        EventBus.getDefault().send(new HideStackActionButtonEvent());
        animateBackgroundScrim(0.0f, 200);
        if (Recents.getConfiguration().isLowRamDevice) {
            animateEmptyView(false, dismissRecentsToHomeAnimationStarted.getAnimationTrigger());
        }
    }

    public final void onBusEvent(DragStartEvent dragStartEvent) {
        updateVisibleDockRegions(Recents.getConfiguration().getDockStatesForCurrentOrientation(), true, DockState.NONE.viewState.dockAreaAlpha, DockState.NONE.viewState.hintTextAlpha, true, false);
        if (this.mStackActionButton != null) {
            this.mStackActionButton.animate().alpha(0.0f).setDuration(100L).setInterpolator(Interpolators.ALPHA_OUT).start();
        }
    }

    public final void onBusEvent(DragDropTargetChangedEvent dragDropTargetChangedEvent) {
        if (dragDropTargetChangedEvent.dropTarget == null || !(dragDropTargetChangedEvent.dropTarget instanceof DockState)) {
            updateVisibleDockRegions(Recents.getConfiguration().getDockStatesForCurrentOrientation(), true, DockState.NONE.viewState.dockAreaAlpha, DockState.NONE.viewState.hintTextAlpha, true, true);
        } else {
            updateVisibleDockRegions(new DockState[]{(DockState) dragDropTargetChangedEvent.dropTarget}, false, -1, -1, true, true);
        }
        if (this.mStackActionButton != null) {
            dragDropTargetChangedEvent.addPostAnimationCallback(new Runnable() { // from class: com.android.systemui.recents.views.RecentsView.1
                AnonymousClass1() {
                }

                @Override // java.lang.Runnable
                public void run() {
                    Rect stackActionButtonBoundsFromStackLayout = RecentsView.this.getStackActionButtonBoundsFromStackLayout();
                    RecentsView.this.mStackActionButton.setLeftTopRightBottom(stackActionButtonBoundsFromStackLayout.left, stackActionButtonBoundsFromStackLayout.top, stackActionButtonBoundsFromStackLayout.right, stackActionButtonBoundsFromStackLayout.bottom);
                }
            });
        }
    }

    /* renamed from: com.android.systemui.recents.views.RecentsView$1 */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            Rect stackActionButtonBoundsFromStackLayout = RecentsView.this.getStackActionButtonBoundsFromStackLayout();
            RecentsView.this.mStackActionButton.setLeftTopRightBottom(stackActionButtonBoundsFromStackLayout.left, stackActionButtonBoundsFromStackLayout.top, stackActionButtonBoundsFromStackLayout.right, stackActionButtonBoundsFromStackLayout.bottom);
        }
    }

    public final void onBusEvent(final DragEndEvent dragEndEvent) {
        if (dragEndEvent.dropTarget instanceof DockState) {
            DockState dockState = (DockState) dragEndEvent.dropTarget;
            updateVisibleDockRegions(null, false, -1, -1, false, false);
            Utilities.setViewFrameFromTranslation(dragEndEvent.taskView);
            if (ActivityManagerWrapper.getInstance().startActivityFromRecents(dragEndEvent.task.key.id, ActivityOptionsCompat.makeSplitScreenOptions(dockState.createMode == 0))) {
                WindowManagerWrapper.getInstance().overridePendingAppTransitionMultiThumbFuture(new AppTransitionAnimationSpecsFuture(getHandler()) { // from class: com.android.systemui.recents.views.RecentsView.2
                    final /* synthetic */ DragEndEvent val$event;
                    final /* synthetic */ Rect val$taskRect;

                    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                    AnonymousClass2(Handler handler, final DragEndEvent dragEndEvent2, Rect rect) {
                        super(handler);
                        dragEndEvent = dragEndEvent2;
                        rect = rect;
                    }

                    @Override // com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture
                    public List<AppTransitionAnimationSpecCompat> composeSpecs() {
                        return RecentsView.this.mTransitionHelper.composeDockAnimationSpec(dragEndEvent.taskView, rect);
                    }
                }, new Runnable() { // from class: com.android.systemui.recents.views.-$$Lambda$RecentsView$RRL6yVNHxRLA7npjCgaGmNF62Mc
                    @Override // java.lang.Runnable
                    public final void run() {
                        RecentsView.lambda$onBusEvent$1(this.f$0, dragEndEvent2);
                    }
                }, getHandler(), true);
                MetricsLogger.action(this.mContext, 270, dragEndEvent2.task.getTopComponent().flattenToShortString());
            } else {
                EventBus.getDefault().send(new DragEndCancelledEvent(getStack(), dragEndEvent2.task, dragEndEvent2.taskView));
            }
        } else {
            updateVisibleDockRegions(null, true, -1, -1, true, false);
        }
        if (this.mStackActionButton != null) {
            this.mStackActionButton.animate().alpha(1.0f).setDuration(134L).setInterpolator(Interpolators.ALPHA_IN).start();
        }
    }

    public static /* synthetic */ void lambda$onBusEvent$1(RecentsView recentsView, DragEndEvent dragEndEvent) {
        EventBus.getDefault().send(new DockedFirstAnimationFrameEvent());
        recentsView.getStack().removeTask(dragEndEvent.task, null, true);
    }

    /* renamed from: com.android.systemui.recents.views.RecentsView$2 */
    class AnonymousClass2 extends AppTransitionAnimationSpecsFuture {
        final /* synthetic */ DragEndEvent val$event;
        final /* synthetic */ Rect val$taskRect;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass2(Handler handler, final DragEndEvent dragEndEvent2, Rect rect) {
            super(handler);
            dragEndEvent = dragEndEvent2;
            rect = rect;
        }

        @Override // com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture
        public List<AppTransitionAnimationSpecCompat> composeSpecs() {
            return RecentsView.this.mTransitionHelper.composeDockAnimationSpec(dragEndEvent.taskView, rect);
        }
    }

    public final void onBusEvent(DragEndCancelledEvent dragEndCancelledEvent) {
        updateVisibleDockRegions(null, true, -1, -1, true, false);
    }

    private Rect getTaskRect(TaskView taskView) {
        int[] locationOnScreen = taskView.getLocationOnScreen();
        int i = locationOnScreen[0];
        int i2 = locationOnScreen[1];
        return new Rect(i, i2, (int) (i + (taskView.getWidth() * taskView.getScaleX())), (int) (i2 + (taskView.getHeight() * taskView.getScaleY())));
    }

    public final void onBusEvent(DraggingInRecentsEvent draggingInRecentsEvent) {
        if (this.mTaskStackView.getTaskViews().size() > 0) {
            setTranslationY(draggingInRecentsEvent.distanceFromTop - this.mTaskStackView.getTaskViews().get(0).getY());
        }
    }

    public final void onBusEvent(DraggingInRecentsEndedEvent draggingInRecentsEndedEvent) {
        ViewPropertyAnimator viewPropertyAnimatorAnimate = animate();
        if (draggingInRecentsEndedEvent.velocity > this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            viewPropertyAnimatorAnimate.translationY(getHeight());
            viewPropertyAnimatorAnimate.withEndAction(new Runnable() { // from class: com.android.systemui.recents.views.RecentsView.3
                AnonymousClass3() {
                }

                @Override // java.lang.Runnable
                public void run() {
                    WindowManagerProxy.getInstance().maximizeDockedStack();
                }
            });
            this.mFlingAnimationUtils.apply(viewPropertyAnimatorAnimate, getTranslationY(), getHeight(), draggingInRecentsEndedEvent.velocity);
        } else {
            viewPropertyAnimatorAnimate.translationY(0.0f);
            viewPropertyAnimatorAnimate.setListener(null);
            this.mFlingAnimationUtils.apply(viewPropertyAnimatorAnimate, getTranslationY(), 0.0f, draggingInRecentsEndedEvent.velocity);
        }
        viewPropertyAnimatorAnimate.start();
    }

    /* renamed from: com.android.systemui.recents.views.RecentsView$3 */
    class AnonymousClass3 implements Runnable {
        AnonymousClass3() {
        }

        @Override // java.lang.Runnable
        public void run() {
            WindowManagerProxy.getInstance().maximizeDockedStack();
        }
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent enterRecentsWindowAnimationCompletedEvent) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (!launchState.launchedViaDockGesture && !launchState.launchedFromApp && getStack().getTaskCount() > 0) {
            animateBackgroundScrim(getOpaqueScrimAlpha(), 300);
        }
    }

    public final void onBusEvent(AllTaskViewsDismissedEvent allTaskViewsDismissedEvent) {
        EventBus.getDefault().send(new HideStackActionButtonEvent());
    }

    public final void onBusEvent(DismissAllTaskViewsEvent dismissAllTaskViewsEvent) {
        if (!Recents.getSystemServices().hasDockedTask()) {
            animateBackgroundScrim(0.0f, 200);
        }
    }

    public final void onBusEvent(ShowStackActionButtonEvent showStackActionButtonEvent) {
        showStackActionButton(134, showStackActionButtonEvent.translate);
    }

    public final void onBusEvent(HideStackActionButtonEvent hideStackActionButtonEvent) {
        hideStackActionButton(100, true);
    }

    public final void onBusEvent(MultiWindowStateChangedEvent multiWindowStateChangedEvent) {
        updateStack(multiWindowStateChangedEvent.stack, false);
    }

    public final void onBusEvent(ShowEmptyViewEvent showEmptyViewEvent) {
        showEmptyView(R.string.recents_empty_message);
    }

    private void showStackActionButton(int i, boolean z) {
        ReferenceCountedTrigger referenceCountedTrigger = new ReferenceCountedTrigger();
        if (this.mStackActionButton.getVisibility() == 4) {
            this.mStackActionButton.setVisibility(0);
            this.mStackActionButton.setAlpha(0.0f);
            if (z) {
                this.mStackActionButton.setTranslationY(this.mStackActionButton.getMeasuredHeight() * (Recents.getConfiguration().isLowRamDevice ? 1.0f : -0.25f));
            } else {
                this.mStackActionButton.setTranslationY(0.0f);
            }
            referenceCountedTrigger.addLastDecrementRunnable(new Runnable() { // from class: com.android.systemui.recents.views.RecentsView.4
                final /* synthetic */ int val$duration;
                final /* synthetic */ boolean val$translate;

                AnonymousClass4(boolean z2, int i2) {
                    z = z2;
                    i = i2;
                }

                @Override // java.lang.Runnable
                public void run() {
                    if (z) {
                        RecentsView.this.mStackActionButton.animate().translationY(0.0f);
                    }
                    RecentsView.this.mStackActionButton.animate().alpha(1.0f).setDuration(i).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).start();
                }
            });
        }
        referenceCountedTrigger.flushLastDecrementRunnables();
    }

    /* renamed from: com.android.systemui.recents.views.RecentsView$4 */
    class AnonymousClass4 implements Runnable {
        final /* synthetic */ int val$duration;
        final /* synthetic */ boolean val$translate;

        AnonymousClass4(boolean z2, int i2) {
            z = z2;
            i = i2;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (z) {
                RecentsView.this.mStackActionButton.animate().translationY(0.0f);
            }
            RecentsView.this.mStackActionButton.animate().alpha(1.0f).setDuration(i).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).start();
        }
    }

    private void hideStackActionButton(int i, boolean z) {
        ReferenceCountedTrigger referenceCountedTrigger = new ReferenceCountedTrigger();
        hideStackActionButton(i, z, referenceCountedTrigger);
        referenceCountedTrigger.flushLastDecrementRunnables();
    }

    private void hideStackActionButton(int i, boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        if (this.mStackActionButton.getVisibility() == 0) {
            if (z) {
                this.mStackActionButton.animate().translationY(this.mStackActionButton.getMeasuredHeight() * (Recents.getConfiguration().isLowRamDevice ? 1.0f : -0.25f));
            }
            this.mStackActionButton.animate().alpha(0.0f).setDuration(i).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).withEndAction(new Runnable() { // from class: com.android.systemui.recents.views.RecentsView.5
                final /* synthetic */ ReferenceCountedTrigger val$postAnimationTrigger;

                AnonymousClass5(ReferenceCountedTrigger referenceCountedTrigger2) {
                    referenceCountedTrigger = referenceCountedTrigger2;
                }

                @Override // java.lang.Runnable
                public void run() {
                    RecentsView.this.mStackActionButton.setVisibility(4);
                    referenceCountedTrigger.decrement();
                }
            }).start();
            referenceCountedTrigger2.increment();
        }
    }

    /* renamed from: com.android.systemui.recents.views.RecentsView$5 */
    class AnonymousClass5 implements Runnable {
        final /* synthetic */ ReferenceCountedTrigger val$postAnimationTrigger;

        AnonymousClass5(ReferenceCountedTrigger referenceCountedTrigger2) {
            referenceCountedTrigger = referenceCountedTrigger2;
        }

        @Override // java.lang.Runnable
        public void run() {
            RecentsView.this.mStackActionButton.setVisibility(4);
            referenceCountedTrigger.decrement();
        }
    }

    private void animateEmptyView(boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        float fHeight = this.mTaskStackView.getStackAlgorithm().getTaskRect().height() / 4;
        this.mEmptyView.setTranslationY(z ? fHeight : 0.0f);
        this.mEmptyView.setAlpha(z ? 0.0f : 1.0f);
        ViewPropertyAnimator interpolator = this.mEmptyView.animate().setDuration(150L).setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        if (z) {
            fHeight = 0.0f;
        }
        ViewPropertyAnimator viewPropertyAnimatorAlpha = interpolator.translationY(fHeight).alpha(z ? 1.0f : 0.0f);
        if (referenceCountedTrigger != null) {
            viewPropertyAnimatorAlpha.setListener(referenceCountedTrigger.decrementOnAnimationEnd());
            referenceCountedTrigger.increment();
        }
        viewPropertyAnimatorAlpha.start();
    }

    private void updateVisibleDockRegions(DockState[] dockStateArr, boolean z, int i, int i2, boolean z2, boolean z3) {
        int i3;
        int i4;
        Rect dockedBounds;
        ArraySet arraySetArrayToSet = Utilities.arrayToSet(dockStateArr, new ArraySet());
        ArrayList<DockState> visibleDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int size = visibleDockStates.size() - 1; size >= 0; size--) {
            DockState dockState = visibleDockStates.get(size);
            DockState.ViewState viewState = dockState.viewState;
            if (dockStateArr == null || !arraySetArrayToSet.contains(dockState)) {
                viewState.startAnimation(null, 0, 0, 250, Interpolators.FAST_OUT_SLOW_IN, z2, z3);
            } else {
                if (i == -1) {
                    i3 = viewState.dockAreaAlpha;
                } else {
                    i3 = i;
                }
                if (i2 == -1) {
                    i4 = viewState.hintTextAlpha;
                } else {
                    i4 = i2;
                }
                if (z) {
                    dockedBounds = dockState.getPreDockedBounds(getMeasuredWidth(), getMeasuredHeight(), this.mSystemInsets);
                } else {
                    dockedBounds = dockState.getDockedBounds(getMeasuredWidth(), getMeasuredHeight(), this.mDividerSize, this.mSystemInsets, getResources());
                }
                Rect rect = dockedBounds;
                if (viewState.dockAreaOverlay.getCallback() != this) {
                    viewState.dockAreaOverlay.setCallback(this);
                    viewState.dockAreaOverlay.setBounds(rect);
                }
                viewState.startAnimation(rect, i3, i4, 250, Interpolators.FAST_OUT_SLOW_IN, z2, z3);
            }
        }
    }

    private float getOpaqueScrimAlpha() {
        return MathUtils.map(0.0f, 1.0f, 0.45f, 0.7f, this.mBusynessFactor);
    }

    private void animateBackgroundScrim(float f, int i) {
        Interpolator interpolator;
        Utilities.cancelAnimationWithoutCallbacks(this.mBackgroundScrimAnimator);
        int alpha = this.mBackgroundScrim.getAlpha();
        int i2 = (int) (f * 255.0f);
        this.mBackgroundScrimAnimator = ValueAnimator.ofInt(alpha, i2);
        this.mBackgroundScrimAnimator.setDuration(i);
        ValueAnimator valueAnimator = this.mBackgroundScrimAnimator;
        if (i2 > alpha) {
            interpolator = Interpolators.ALPHA_IN;
        } else {
            interpolator = Interpolators.ALPHA_OUT;
        }
        valueAnimator.setInterpolator(interpolator);
        this.mBackgroundScrimAnimator.addUpdateListener(this.mUpdateBackgroundScrimAlpha);
        this.mBackgroundScrimAnimator.start();
    }

    Rect getStackActionButtonBoundsFromStackLayout() {
        int paddingRight;
        int iWidth;
        int iHeight;
        Rect rect = new Rect(this.mTaskStackView.mLayoutAlgorithm.getStackActionButtonRect());
        if (Recents.getConfiguration().isLowRamDevice) {
            Rect windowRect = Recents.getSystemServices().getWindowRect();
            iWidth = ((((windowRect.width() - this.mSystemInsets.left) - this.mSystemInsets.right) - this.mStackActionButton.getMeasuredWidth()) / 2) + this.mSystemInsets.left;
            iHeight = windowRect.height() - ((this.mStackActionButton.getMeasuredHeight() + this.mSystemInsets.bottom) + (this.mStackActionButton.getPaddingBottom() / 2));
        } else {
            if (isLayoutRtl()) {
                paddingRight = rect.left - this.mStackActionButton.getPaddingLeft();
            } else {
                paddingRight = (rect.right + this.mStackActionButton.getPaddingRight()) - this.mStackActionButton.getMeasuredWidth();
            }
            iWidth = paddingRight;
            iHeight = rect.top + ((rect.height() - this.mStackActionButton.getMeasuredHeight()) / 2);
        }
        rect.set(iWidth, iHeight, this.mStackActionButton.getMeasuredWidth() + iWidth, this.mStackActionButton.getMeasuredHeight() + iHeight);
        return rect;
    }

    View getStackActionButton() {
        return this.mStackActionButton;
    }

    public void launchTaskFromRecents(TaskStack taskStack, Task task, TaskStackView taskStackView, TaskView taskView, boolean z, int i, int i2) {
        Runnable anonymousClass8;
        AnonymousClass6 anonymousClass6;
        AnonymousClass6 anonymousClass62 = null;
        if (taskView != null) {
            AnonymousClass6 anonymousClass63 = new AppTransitionAnimationSpecsFuture(taskStackView.getHandler()) { // from class: com.android.systemui.recents.views.RecentsView.6
                final /* synthetic */ int val$activityType;
                final /* synthetic */ TaskStackView val$stackView;
                final /* synthetic */ Task val$task;
                final /* synthetic */ Rect val$windowRect;
                final /* synthetic */ int val$windowingMode;

                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                AnonymousClass6(Handler handler, Task task2, TaskStackView taskStackView2, int i3, int i22, Rect rect) {
                    super(handler);
                    task = task2;
                    taskStackView = taskStackView2;
                    i = i3;
                    i = i22;
                    rect = rect;
                }

                @Override // com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture
                public List<AppTransitionAnimationSpecCompat> composeSpecs() {
                    return RecentsView.this.mTransitionHelper.composeAnimationSpecs(task, taskStackView, i, i, rect);
                }
            };
            anonymousClass8 = new AnonymousClass7(task2, taskStackView2, z);
            anonymousClass6 = anonymousClass63;
        } else {
            anonymousClass8 = new Runnable() { // from class: com.android.systemui.recents.views.RecentsView.8
                private boolean mHandled;
                final /* synthetic */ TaskStackView val$stackView;
                final /* synthetic */ Task val$task;

                AnonymousClass8(Task task2, TaskStackView taskStackView2) {
                    task = task2;
                    taskStackView = taskStackView2;
                }

                @Override // java.lang.Runnable
                public void run() {
                    if (this.mHandled) {
                        return;
                    }
                    this.mHandled = true;
                    EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(task));
                    EventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
                    taskStackView.cancelAllTaskViewAnimations();
                    if (!Recents.getConfiguration().isLowRamDevice) {
                        EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
                    }
                }
            };
            anonymousClass6 = null;
        }
        EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(true));
        Context context = this.mContext;
        Handler handler = this.mHandler;
        if (anonymousClass6 != null) {
            anonymousClass62 = anonymousClass6;
        }
        ActivityOptions activityOptionsCreateAspectScaleAnimation = RecentsTransition.createAspectScaleAnimation(context, handler, true, anonymousClass62, anonymousClass8);
        if (taskView == null) {
            startTaskActivity(taskStack, task2, taskView, activityOptionsCreateAspectScaleAnimation, anonymousClass6, i3, i22);
        } else {
            EventBus.getDefault().send(new LaunchTaskStartedEvent(taskView, z));
            startTaskActivity(taskStack, task2, taskView, activityOptionsCreateAspectScaleAnimation, anonymousClass6, i3, i22);
        }
        ActivityManagerWrapper.getInstance().closeSystemWindows("recentapps");
    }

    /* renamed from: com.android.systemui.recents.views.RecentsView$6 */
    class AnonymousClass6 extends AppTransitionAnimationSpecsFuture {
        final /* synthetic */ int val$activityType;
        final /* synthetic */ TaskStackView val$stackView;
        final /* synthetic */ Task val$task;
        final /* synthetic */ Rect val$windowRect;
        final /* synthetic */ int val$windowingMode;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass6(Handler handler, Task task2, TaskStackView taskStackView2, int i3, int i22, Rect rect) {
            super(handler);
            task = task2;
            taskStackView = taskStackView2;
            i = i3;
            i = i22;
            rect = rect;
        }

        @Override // com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture
        public List<AppTransitionAnimationSpecCompat> composeSpecs() {
            return RecentsView.this.mTransitionHelper.composeAnimationSpecs(task, taskStackView, i, i, rect);
        }
    }

    /* renamed from: com.android.systemui.recents.views.RecentsView$7 */
    class AnonymousClass7 implements Runnable {
        private boolean mHandled;
        final /* synthetic */ boolean val$screenPinningRequested;
        final /* synthetic */ TaskStackView val$stackView;
        final /* synthetic */ Task val$task;

        AnonymousClass7(Task task, TaskStackView taskStackView, boolean z) {
            this.val$task = task;
            this.val$stackView = taskStackView;
            this.val$screenPinningRequested = z;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (this.mHandled) {
                return;
            }
            this.mHandled = true;
            EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(this.val$task));
            EventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
            this.val$stackView.cancelAllTaskViewAnimations();
            if (this.val$screenPinningRequested) {
                Handler handler = RecentsView.this.mHandler;
                final Task task = this.val$task;
                handler.postDelayed(new Runnable() { // from class: com.android.systemui.recents.views.-$$Lambda$RecentsView$7$6nm4p5eX5UvPb6uaSaO8OVjwZdU
                    @Override // java.lang.Runnable
                    public final void run() {
                        EventBus.getDefault().send(new ScreenPinningRequestEvent(RecentsView.this.mContext, task.key.id));
                    }
                }, 350L);
            }
            if (!Recents.getConfiguration().isLowRamDevice) {
                EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
            }
        }
    }

    /* renamed from: com.android.systemui.recents.views.RecentsView$8 */
    class AnonymousClass8 implements Runnable {
        private boolean mHandled;
        final /* synthetic */ TaskStackView val$stackView;
        final /* synthetic */ Task val$task;

        AnonymousClass8(Task task2, TaskStackView taskStackView2) {
            task = task2;
            taskStackView = taskStackView2;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (this.mHandled) {
                return;
            }
            this.mHandled = true;
            EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(task));
            EventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
            taskStackView.cancelAllTaskViewAnimations();
            if (!Recents.getConfiguration().isLowRamDevice) {
                EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
            }
        }
    }

    private void startTaskActivity(final TaskStack taskStack, final Task task, final TaskView taskView, ActivityOptions activityOptions, final AppTransitionAnimationSpecsFuture appTransitionAnimationSpecsFuture, int i, int i2) {
        ActivityManagerWrapper.getInstance().startActivityFromRecentsAsync(task.key, activityOptions, i, i2, new Consumer() { // from class: com.android.systemui.recents.views.-$$Lambda$RecentsView$Izp2qzQsBKZyynkPkjmVQGpzmLA
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RecentsView.lambda$startTaskActivity$2(this.f$0, taskStack, task, taskView, (Boolean) obj);
            }
        }, getHandler());
        if (appTransitionAnimationSpecsFuture != null) {
            Handler handler = this.mHandler;
            Objects.requireNonNull(appTransitionAnimationSpecsFuture);
            handler.post(new Runnable() { // from class: com.android.systemui.recents.views.-$$Lambda$2_yYbS189Yb53TwKAnkQBhUWOR4
                @Override // java.lang.Runnable
                public final void run() {
                    appTransitionAnimationSpecsFuture.composeSpecsSynchronous();
                }
            });
        }
    }

    public static /* synthetic */ void lambda$startTaskActivity$2(RecentsView recentsView, TaskStack taskStack, Task task, TaskView taskView, Boolean bool) {
        if (bool.booleanValue()) {
            EventBus.getDefault().send(new LaunchTaskSucceededEvent(taskStack.indexOfTask(task) > -1 ? (taskStack.getTaskCount() - r5) - 1 : 0));
        } else {
            Log.e("RecentsView", recentsView.mContext.getString(R.string.recents_launch_error_message, task.title));
            if (taskView != null) {
                taskView.dismissTask();
            }
            EventBus.getDefault().send(new LaunchTaskFailedEvent());
        }
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public void requestDisallowInterceptTouchEvent(boolean z) {
        super.requestDisallowInterceptTouchEvent(z);
        this.mTouchHandler.cancelStackActionButtonClick();
    }

    public void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        String hexString = Integer.toHexString(System.identityHashCode(this));
        printWriter.print(str);
        printWriter.print("RecentsView");
        printWriter.print(" awaitingFirstLayout=");
        printWriter.print(this.mAwaitingFirstLayout ? "Y" : "N");
        printWriter.print(" insets=");
        printWriter.print(Utilities.dumpRect(this.mSystemInsets));
        printWriter.print(" [0x");
        printWriter.print(hexString);
        printWriter.print("]");
        printWriter.println();
        if (getStack() != null) {
            getStack().dump(str2, printWriter);
        }
        if (this.mTaskStackView != null) {
            this.mTaskStackView.dump(str2, printWriter);
        }
    }
}
