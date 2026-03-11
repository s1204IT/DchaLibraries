package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewOutlineProvider;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.ui.DismissTaskViewEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;

public class TaskView extends FixedSizeFrameLayout implements Task.TaskCallbacks, View.OnClickListener, View.OnLongClickListener {
    private float mActionButtonTranslationZ;
    private View mActionButtonView;
    private TaskViewCallbacks mCb;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mClipViewInStack;

    @ViewDebug.ExportedProperty(category = "recents")
    private float mDimAlpha;
    private ObjectAnimator mDimAnimator;
    private Toast mDisabledAppToast;

    @ViewDebug.ExportedProperty(category = "recents")
    private Point mDownTouchPos;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "header_")
    TaskViewHeader mHeaderView;
    private View mIncompatibleAppToastView;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mIsDisabledInSafeMode;
    private ObjectAnimator mOutlineAnimator;
    private final TaskViewTransform mTargetAnimationTransform;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "task_")
    private Task mTask;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "thumbnail_")
    TaskViewThumbnail mThumbnailView;
    private ArrayList<Animator> mTmpAnimators;

    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mTouchExplorationEnabled;
    private AnimatorSet mTransformAnimation;

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "view_bounds_")
    private AnimateableViewBounds mViewBounds;
    public static final Property<TaskView, Float> DIM_ALPHA_WITHOUT_HEADER = new FloatProperty<TaskView>("dimAlphaWithoutHeader") {
        @Override
        public void setValue(TaskView tv, float dimAlpha) {
            tv.setDimAlphaWithoutHeader(dimAlpha);
        }

        @Override
        public Float get(TaskView tv) {
            return Float.valueOf(tv.getDimAlpha());
        }
    };
    public static final Property<TaskView, Float> DIM_ALPHA = new FloatProperty<TaskView>("dimAlpha") {
        @Override
        public void setValue(TaskView tv, float dimAlpha) {
            tv.setDimAlpha(dimAlpha);
        }

        @Override
        public Float get(TaskView tv) {
            return Float.valueOf(tv.getDimAlpha());
        }
    };
    public static final Property<TaskView, Float> VIEW_OUTLINE_ALPHA = new FloatProperty<TaskView>("viewOutlineAlpha") {
        @Override
        public void setValue(TaskView tv, float alpha) {
            tv.getViewBounds().setAlpha(alpha);
        }

        @Override
        public Float get(TaskView tv) {
            return Float.valueOf(tv.getViewBounds().getAlpha());
        }
    };

    interface TaskViewCallbacks {
        void onTaskViewClipStateChanged(TaskView taskView);
    }

    public TaskView(Context context) {
        this(context, null);
    }

    public TaskView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mClipViewInStack = true;
        this.mTargetAnimationTransform = new TaskViewTransform();
        this.mTmpAnimators = new ArrayList<>();
        this.mDownTouchPos = new Point();
        RecentsConfiguration config = Recents.getConfiguration();
        Resources res = context.getResources();
        this.mViewBounds = new AnimateableViewBounds(this, res.getDimensionPixelSize(R.dimen.recents_task_view_shadow_rounded_corners_radius));
        if (config.fakeShadows) {
            setBackground(new FakeShadowDrawable(res, config));
        }
        setOutlineProvider(this.mViewBounds);
        setOnLongClickListener(this);
    }

    void setCallbacks(TaskViewCallbacks cb) {
        this.mCb = cb;
    }

    void onReload(boolean isResumingFromVisible) {
        resetNoUserInteractionState();
        if (isResumingFromVisible) {
            return;
        }
        resetViewProperties();
    }

    public Task getTask() {
        return this.mTask;
    }

    AnimateableViewBounds getViewBounds() {
        return this.mViewBounds;
    }

    @Override
    protected void onFinishInflate() {
        this.mHeaderView = (TaskViewHeader) findViewById(R.id.task_view_bar);
        this.mThumbnailView = (TaskViewThumbnail) findViewById(R.id.task_view_thumbnail);
        this.mThumbnailView.updateClipToTaskBar(this.mHeaderView);
        this.mActionButtonView = findViewById(R.id.lock_to_app_fab);
        this.mActionButtonView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, TaskView.this.mActionButtonView.getWidth(), TaskView.this.mActionButtonView.getHeight());
                outline.setAlpha(0.35f);
            }
        });
        this.mActionButtonView.setOnClickListener(this);
        this.mActionButtonTranslationZ = this.mActionButtonView.getTranslationZ();
    }

    void onConfigurationChanged() {
        this.mHeaderView.onConfigurationChanged();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        this.mHeaderView.onTaskViewSizeChanged(w, h);
        this.mThumbnailView.onTaskViewSizeChanged(w, h);
        this.mActionButtonView.setTranslationX(w - getMeasuredWidth());
        this.mActionButtonView.setTranslationY(h - getMeasuredHeight());
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mDownTouchPos.set((int) (ev.getX() * getScaleX()), (int) (ev.getY() * getScaleY()));
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    protected void measureContents(int width, int height) {
        int widthWithoutPadding = (width - this.mPaddingLeft) - this.mPaddingRight;
        int heightWithoutPadding = (height - this.mPaddingTop) - this.mPaddingBottom;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(heightWithoutPadding, 1073741824);
        measureChildren(widthSpec, heightSpec);
        setMeasuredDimension(width, height);
    }

    void updateViewPropertiesToTaskTransform(TaskViewTransform toTransform, AnimationProps toAnimation, ValueAnimator.AnimatorUpdateListener updateCallback) {
        RecentsConfiguration config = Recents.getConfiguration();
        cancelTransformAnimation();
        this.mTmpAnimators.clear();
        toTransform.applyToTaskView(this, this.mTmpAnimators, toAnimation, !config.fakeShadows);
        if (toAnimation.isImmediate()) {
            if (Float.compare(getDimAlpha(), toTransform.dimAlpha) != 0) {
                setDimAlpha(toTransform.dimAlpha);
            }
            if (Float.compare(this.mViewBounds.getAlpha(), toTransform.viewOutlineAlpha) != 0) {
                this.mViewBounds.setAlpha(toTransform.viewOutlineAlpha);
            }
            if (toAnimation.getListener() != null) {
                toAnimation.getListener().onAnimationEnd(null);
            }
            if (updateCallback == null) {
                return;
            }
            updateCallback.onAnimationUpdate(null);
            return;
        }
        if (Float.compare(getDimAlpha(), toTransform.dimAlpha) != 0) {
            this.mDimAnimator = ObjectAnimator.ofFloat(this, DIM_ALPHA, getDimAlpha(), toTransform.dimAlpha);
            this.mTmpAnimators.add(toAnimation.apply(6, this.mDimAnimator));
        }
        if (Float.compare(this.mViewBounds.getAlpha(), toTransform.viewOutlineAlpha) != 0) {
            this.mOutlineAnimator = ObjectAnimator.ofFloat(this, VIEW_OUTLINE_ALPHA, this.mViewBounds.getAlpha(), toTransform.viewOutlineAlpha);
            this.mTmpAnimators.add(toAnimation.apply(6, this.mOutlineAnimator));
        }
        if (updateCallback != null) {
            ValueAnimator updateCallbackAnim = ValueAnimator.ofInt(0, 1);
            updateCallbackAnim.addUpdateListener(updateCallback);
            this.mTmpAnimators.add(toAnimation.apply(6, updateCallbackAnim));
        }
        this.mTransformAnimation = toAnimation.createAnimator(this.mTmpAnimators);
        this.mTransformAnimation.start();
        this.mTargetAnimationTransform.copyFrom(toTransform);
    }

    void resetViewProperties() {
        cancelTransformAnimation();
        setDimAlpha(0.0f);
        setVisibility(0);
        getViewBounds().reset();
        getHeaderView().reset();
        TaskViewTransform.reset(this);
        this.mActionButtonView.setScaleX(1.0f);
        this.mActionButtonView.setScaleY(1.0f);
        this.mActionButtonView.setAlpha(0.0f);
        this.mActionButtonView.setTranslationX(0.0f);
        this.mActionButtonView.setTranslationY(0.0f);
        this.mActionButtonView.setTranslationZ(this.mActionButtonTranslationZ);
        if (this.mIncompatibleAppToastView == null) {
            return;
        }
        this.mIncompatibleAppToastView.setVisibility(4);
    }

    boolean isAnimatingTo(TaskViewTransform transform) {
        if (this.mTransformAnimation == null || !this.mTransformAnimation.isStarted()) {
            return false;
        }
        return this.mTargetAnimationTransform.isSame(transform);
    }

    public void cancelTransformAnimation() {
        Utilities.cancelAnimationWithoutCallbacks(this.mTransformAnimation);
        Utilities.cancelAnimationWithoutCallbacks(this.mDimAnimator);
        Utilities.cancelAnimationWithoutCallbacks(this.mOutlineAnimator);
    }

    void setTouchEnabled(boolean enabled) {
        setOnClickListener(enabled ? this : null);
    }

    void startNoUserInteractionAnimation() {
        this.mHeaderView.startNoUserInteractionAnimation();
    }

    void setNoUserInteractionState() {
        this.mHeaderView.setNoUserInteractionState();
    }

    void resetNoUserInteractionState() {
        this.mHeaderView.resetNoUserInteractionState();
    }

    void dismissTask() {
        DismissTaskViewEvent dismissEvent = new DismissTaskViewEvent(this);
        dismissEvent.addPostAnimationCallback(new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().send(new TaskViewDismissedEvent(TaskView.this.mTask, this, new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN)));
            }
        });
        EventBus.getDefault().send(dismissEvent);
    }

    boolean shouldClipViewInStack() {
        if (this.mTask.isFreeformTask() || getVisibility() != 0) {
            return false;
        }
        return this.mClipViewInStack;
    }

    void setClipViewInStack(boolean clip) {
        if (clip == this.mClipViewInStack) {
            return;
        }
        this.mClipViewInStack = clip;
        if (this.mCb == null) {
            return;
        }
        this.mCb.onTaskViewClipStateChanged(this);
    }

    public TaskViewHeader getHeaderView() {
        return this.mHeaderView;
    }

    public void setDimAlpha(float dimAlpha) {
        this.mDimAlpha = dimAlpha;
        this.mThumbnailView.setDimAlpha(dimAlpha);
        this.mHeaderView.setDimAlpha(dimAlpha);
    }

    public void setDimAlphaWithoutHeader(float dimAlpha) {
        this.mDimAlpha = dimAlpha;
        this.mThumbnailView.setDimAlpha(dimAlpha);
    }

    public float getDimAlpha() {
        return this.mDimAlpha;
    }

    public void setFocusedState(boolean isFocused, boolean requestViewFocus) {
        if (isFocused) {
            if (!requestViewFocus || isFocused()) {
                return;
            }
            requestFocus();
            return;
        }
        if (!isAccessibilityFocused() || !this.mTouchExplorationEnabled) {
            return;
        }
        clearAccessibilityFocus();
    }

    public void showActionButton(boolean fadeIn, int fadeInDuration) {
        this.mActionButtonView.setVisibility(0);
        if (fadeIn && this.mActionButtonView.getAlpha() < 1.0f) {
            this.mActionButtonView.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(fadeInDuration).setInterpolator(Interpolators.ALPHA_IN).start();
            return;
        }
        this.mActionButtonView.setScaleX(1.0f);
        this.mActionButtonView.setScaleY(1.0f);
        this.mActionButtonView.setAlpha(1.0f);
        this.mActionButtonView.setTranslationZ(this.mActionButtonTranslationZ);
    }

    public void hideActionButton(boolean fadeOut, int fadeOutDuration, boolean scaleDown, final Animator.AnimatorListener animListener) {
        if (fadeOut && this.mActionButtonView.getAlpha() > 0.0f) {
            if (scaleDown) {
                this.mActionButtonView.animate().scaleX(0.9f).scaleY(0.9f);
            }
            this.mActionButtonView.animate().alpha(0.0f).setDuration(fadeOutDuration).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (animListener != null) {
                        animListener.onAnimationEnd(null);
                    }
                    TaskView.this.mActionButtonView.setVisibility(4);
                }
            }).start();
        } else {
            this.mActionButtonView.setAlpha(0.0f);
            this.mActionButtonView.setVisibility(4);
            if (animListener != null) {
                animListener.onAnimationEnd(null);
            }
        }
    }

    public void onPrepareLaunchTargetForEnterAnimation() {
        setDimAlphaWithoutHeader(0.0f);
        this.mActionButtonView.setAlpha(0.0f);
        if (this.mIncompatibleAppToastView == null || this.mIncompatibleAppToastView.getVisibility() != 0) {
            return;
        }
        this.mIncompatibleAppToastView.setAlpha(0.0f);
    }

    public void onStartLaunchTargetEnterAnimation(TaskViewTransform transform, int duration, boolean screenPinningEnabled, ReferenceCountedTrigger postAnimationTrigger) {
        Utilities.cancelAnimationWithoutCallbacks(this.mDimAnimator);
        postAnimationTrigger.increment();
        AnimationProps animation = new AnimationProps(duration, Interpolators.ALPHA_OUT);
        this.mDimAnimator = (ObjectAnimator) animation.apply(7, ObjectAnimator.ofFloat(this, DIM_ALPHA_WITHOUT_HEADER, getDimAlpha(), transform.dimAlpha));
        this.mDimAnimator.addListener(postAnimationTrigger.decrementOnAnimationEnd());
        this.mDimAnimator.start();
        if (screenPinningEnabled) {
            showActionButton(true, duration);
        }
        if (this.mIncompatibleAppToastView == null || this.mIncompatibleAppToastView.getVisibility() != 0) {
            return;
        }
        this.mIncompatibleAppToastView.animate().alpha(1.0f).setDuration(duration).setInterpolator(Interpolators.ALPHA_IN).start();
    }

    public void onStartLaunchTargetLaunchAnimation(int duration, boolean screenPinningRequested, ReferenceCountedTrigger postAnimationTrigger) {
        Utilities.cancelAnimationWithoutCallbacks(this.mDimAnimator);
        AnimationProps animation = new AnimationProps(duration, Interpolators.ALPHA_OUT);
        this.mDimAnimator = (ObjectAnimator) animation.apply(7, ObjectAnimator.ofFloat(this, DIM_ALPHA, getDimAlpha(), 0.0f));
        this.mDimAnimator.start();
        postAnimationTrigger.increment();
        hideActionButton(true, duration, !screenPinningRequested, postAnimationTrigger.decrementOnAnimationEnd());
    }

    public void onStartFrontTaskEnterAnimation(boolean screenPinningEnabled) {
        if (!screenPinningEnabled) {
            return;
        }
        showActionButton(false, 0);
    }

    public void onTaskBound(Task t, boolean touchExplorationEnabled, int displayOrientation, Rect displayRect) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mTouchExplorationEnabled = touchExplorationEnabled;
        this.mTask = t;
        this.mTask.addCallback(this);
        this.mIsDisabledInSafeMode = !this.mTask.isSystemApp ? ssp.isInSafeMode() : false;
        this.mThumbnailView.bindToTask(this.mTask, this.mIsDisabledInSafeMode, displayOrientation, displayRect);
        this.mHeaderView.bindToTask(this.mTask, this.mTouchExplorationEnabled, this.mIsDisabledInSafeMode);
        if (!t.isDockable && ssp.hasDockedTask()) {
            if (this.mIncompatibleAppToastView == null) {
                this.mIncompatibleAppToastView = Utilities.findViewStubById(this, R.id.incompatible_app_toast_stub).inflate();
                TextView msg = (TextView) findViewById(android.R.id.message);
                msg.setText(R.string.recents_incompatible_app_message);
            }
            this.mIncompatibleAppToastView.setVisibility(0);
            return;
        }
        if (this.mIncompatibleAppToastView == null) {
            return;
        }
        this.mIncompatibleAppToastView.setVisibility(4);
    }

    @Override
    public void onTaskDataLoaded(Task task, ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        this.mThumbnailView.onTaskDataLoaded(thumbnailInfo);
        this.mHeaderView.onTaskDataLoaded();
    }

    @Override
    public void onTaskDataUnloaded() {
        this.mTask.removeCallback(this);
        this.mThumbnailView.unbindFromTask();
        this.mHeaderView.unbindFromTask(this.mTouchExplorationEnabled);
    }

    @Override
    public void onTaskStackIdChanged() {
        this.mHeaderView.bindToTask(this.mTask, this.mTouchExplorationEnabled, this.mIsDisabledInSafeMode);
        this.mHeaderView.onTaskDataLoaded();
    }

    @Override
    public void onClick(View v) {
        if (this.mIsDisabledInSafeMode) {
            Context context = getContext();
            String msg = context.getString(R.string.recents_launch_disabled_message, this.mTask.title);
            if (this.mDisabledAppToast != null) {
                this.mDisabledAppToast.cancel();
            }
            this.mDisabledAppToast = Toast.makeText(context, msg, 0);
            this.mDisabledAppToast.show();
            return;
        }
        boolean screenPinningRequested = false;
        if (v == this.mActionButtonView) {
            this.mActionButtonView.setTranslationZ(0.0f);
            screenPinningRequested = true;
        }
        EventBus.getDefault().send(new LaunchTaskEvent(this, this.mTask, null, -1, screenPinningRequested));
        MetricsLogger.action(v.getContext(), 277, this.mTask.key.getComponent().toString());
    }

    @Override
    public boolean onLongClick(View v) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        Rect clipBounds = new Rect(this.mViewBounds.mClipBounds);
        clipBounds.scale(getScaleX());
        boolean inBounds = clipBounds.contains(this.mDownTouchPos.x, this.mDownTouchPos.y);
        if (v != this || !inBounds || ssp.hasDockedTask()) {
            return false;
        }
        setClipViewInStack(false);
        this.mDownTouchPos.x = (int) (r3.x + (((1.0f - getScaleX()) * getWidth()) / 2.0f));
        this.mDownTouchPos.y = (int) (r3.y + (((1.0f - getScaleY()) * getHeight()) / 2.0f));
        EventBus.getDefault().register(this, 3);
        EventBus.getDefault().send(new DragStartEvent(this.mTask, this, this.mDownTouchPos));
        return true;
    }

    public final void onBusEvent(DragEndEvent event) {
        if (!(event.dropTarget instanceof TaskStack.DockState)) {
            event.addPostAnimationCallback(new Runnable() {
                @Override
                public void run() {
                    TaskView.this.m1086com_android_systemui_recents_views_TaskView_lambda$1();
                }
            });
        }
        EventBus.getDefault().unregister(this);
    }

    void m1086com_android_systemui_recents_views_TaskView_lambda$1() {
        setClipViewInStack(true);
    }

    public final void onBusEvent(DragEndCancelledEvent event) {
        event.addPostAnimationCallback(new Runnable() {
            @Override
            public void run() {
                TaskView.this.m1087com_android_systemui_recents_views_TaskView_lambda$2();
            }
        });
    }

    void m1087com_android_systemui_recents_views_TaskView_lambda$2() {
        setClipViewInStack(true);
    }
}
