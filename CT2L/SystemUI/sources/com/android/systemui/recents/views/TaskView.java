package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.ViewAnimation;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class TaskView extends FrameLayout implements View.OnClickListener, View.OnLongClickListener, Task.TaskCallbacks {
    float mActionButtonTranslationZ;
    View mActionButtonView;
    TaskViewCallbacks mCb;
    boolean mClipViewInStack;
    RecentsConfiguration mConfig;
    View mContent;
    int mDimAlpha;
    PorterDuffColorFilter mDimColorFilter;
    AccelerateInterpolator mDimInterpolator;
    Paint mDimLayerPaint;
    boolean mFocusAnimationsEnabled;
    TaskViewHeader mHeaderView;
    boolean mIsFocused;
    float mMaxDimScale;
    Task mTask;
    boolean mTaskDataLoaded;
    float mTaskProgress;
    ObjectAnimator mTaskProgressAnimator;
    TaskViewThumbnail mThumbnailView;
    ValueAnimator.AnimatorUpdateListener mUpdateDimListener;
    AnimateableViewBounds mViewBounds;

    interface TaskViewCallbacks {
        void onTaskViewAppInfoClicked(TaskView taskView);

        void onTaskViewClicked(TaskView taskView, Task task, boolean z);

        void onTaskViewClipStateChanged(TaskView taskView);

        void onTaskViewDismissed(TaskView taskView);

        void onTaskViewFocusChanged(TaskView taskView, boolean z);
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
        this.mDimInterpolator = new AccelerateInterpolator(1.0f);
        this.mDimColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);
        this.mDimLayerPaint = new Paint();
        this.mUpdateDimListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                TaskView.this.setTaskProgress(((Float) animation.getAnimatedValue()).floatValue());
            }
        };
        this.mConfig = RecentsConfiguration.getInstance();
        this.mMaxDimScale = this.mConfig.taskStackMaxDim / 255.0f;
        this.mClipViewInStack = true;
        this.mViewBounds = new AnimateableViewBounds(this, this.mConfig.taskViewRoundedCornerRadiusPx);
        setTaskProgress(getTaskProgress());
        setDim(getDim());
        if (this.mConfig.fakeShadows) {
            setBackground(new FakeShadowDrawable(context.getResources(), this.mConfig));
        }
        setOutlineProvider(this.mViewBounds);
    }

    void setCallbacks(TaskViewCallbacks cb) {
        this.mCb = cb;
    }

    void reset() {
        resetViewProperties();
        resetNoUserInteractionState();
        setClipViewInStack(false);
        setCallbacks(null);
    }

    Task getTask() {
        return this.mTask;
    }

    AnimateableViewBounds getViewBounds() {
        return this.mViewBounds;
    }

    @Override
    protected void onFinishInflate() {
        this.mContent = findViewById(R.id.task_view_content);
        this.mHeaderView = (TaskViewHeader) findViewById(R.id.task_view_bar);
        this.mThumbnailView = (TaskViewThumbnail) findViewById(R.id.task_view_thumbnail);
        this.mThumbnailView.updateClipToTaskBar(this.mHeaderView);
        this.mActionButtonView = findViewById(R.id.lock_to_app_fab);
        this.mActionButtonView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, TaskView.this.mActionButtonView.getWidth(), TaskView.this.mActionButtonView.getHeight());
            }
        });
        this.mActionButtonTranslationZ = this.mActionButtonView.getTranslationZ();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthWithoutPadding = (width - this.mPaddingLeft) - this.mPaddingRight;
        int heightWithoutPadding = (height - this.mPaddingTop) - this.mPaddingBottom;
        this.mContent.measure(View.MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824), View.MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824));
        this.mHeaderView.measure(View.MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mConfig.taskBarHeight, 1073741824));
        this.mActionButtonView.measure(View.MeasureSpec.makeMeasureSpec(widthWithoutPadding, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(heightWithoutPadding, Integer.MIN_VALUE));
        this.mThumbnailView.measure(View.MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824), View.MeasureSpec.makeMeasureSpec(widthWithoutPadding, 1073741824));
        setMeasuredDimension(width, height);
        invalidateOutline();
    }

    void updateViewPropertiesToTaskTransform(TaskViewTransform toTransform, int duration) {
        updateViewPropertiesToTaskTransform(toTransform, duration, null);
    }

    void updateViewPropertiesToTaskTransform(TaskViewTransform toTransform, int duration, ValueAnimator.AnimatorUpdateListener updateCallback) {
        toTransform.applyToTaskView(this, duration, this.mConfig.fastOutSlowInInterpolator, false, !this.mConfig.fakeShadows, updateCallback);
        Utilities.cancelAnimationWithoutCallbacks(this.mTaskProgressAnimator);
        if (duration <= 0) {
            setTaskProgress(toTransform.p);
            return;
        }
        this.mTaskProgressAnimator = ObjectAnimator.ofFloat(this, "taskProgress", toTransform.p);
        this.mTaskProgressAnimator.setDuration(duration);
        this.mTaskProgressAnimator.addUpdateListener(this.mUpdateDimListener);
        this.mTaskProgressAnimator.start();
    }

    void resetViewProperties() {
        setDim(0);
        setLayerType(0, null);
        TaskViewTransform.reset(this);
        if (this.mActionButtonView != null) {
            this.mActionButtonView.setScaleX(1.0f);
            this.mActionButtonView.setScaleY(1.0f);
            this.mActionButtonView.setAlpha(1.0f);
            this.mActionButtonView.setTranslationZ(this.mActionButtonTranslationZ);
        }
    }

    void prepareEnterRecentsAnimation(boolean isTaskViewLaunchTargetTask, boolean occludesLaunchTarget, int offscreenY) {
        int initialDim = getDim();
        if (!this.mConfig.launchedHasConfigurationChanged) {
            if (this.mConfig.launchedFromAppWithThumbnail) {
                if (isTaskViewLaunchTargetTask) {
                    initialDim = 0;
                    this.mActionButtonView.setAlpha(0.0f);
                } else if (occludesLaunchTarget) {
                    setTranslationY(offscreenY);
                }
            } else if (this.mConfig.launchedFromHome) {
                setTranslationY(offscreenY);
                setTranslationZ(0.0f);
                setScaleX(1.0f);
                setScaleY(1.0f);
            }
        }
        setDim(initialDim);
        this.mThumbnailView.prepareEnterRecentsAnimation(isTaskViewLaunchTargetTask);
    }

    void startEnterRecentsAnimation(final ViewAnimation.TaskViewEnterContext ctx) {
        TaskViewTransform transform = ctx.currentTaskTransform;
        int startDelay = 0;
        if (this.mConfig.launchedFromAppWithThumbnail) {
            if (this.mTask.isLaunchTarget) {
                animateDimToProgress(this.mConfig.transitionEnterFromAppDelay, this.mConfig.taskViewEnterFromAppDuration, ctx.postAnimationTrigger.decrementOnAnimationEnd());
                ctx.postAnimationTrigger.increment();
                fadeInActionButton(this.mConfig.transitionEnterFromAppDelay, this.mConfig.taskViewEnterFromAppDuration);
            } else if (ctx.currentTaskOccludesLaunchTarget) {
                setTranslationY(transform.translationY + this.mConfig.taskViewAffiliateGroupEnterOffsetPx);
                setAlpha(0.0f);
                animate().alpha(1.0f).translationY(transform.translationY).setStartDelay(this.mConfig.transitionEnterFromAppDelay).setUpdateListener(null).setInterpolator(this.mConfig.fastOutSlowInInterpolator).setDuration(this.mConfig.taskViewEnterFromHomeDuration).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        ctx.postAnimationTrigger.decrement();
                    }
                }).start();
                ctx.postAnimationTrigger.increment();
            }
            startDelay = this.mConfig.transitionEnterFromAppDelay;
        } else if (this.mConfig.launchedFromHome) {
            int frontIndex = (ctx.currentStackViewCount - ctx.currentStackViewIndex) - 1;
            int delay = this.mConfig.transitionEnterFromHomeDelay + (this.mConfig.taskViewEnterFromHomeStaggerDelay * frontIndex);
            setScaleX(transform.scale);
            setScaleY(transform.scale);
            if (!this.mConfig.fakeShadows) {
                animate().translationZ(transform.translationZ);
            }
            animate().translationY(transform.translationY).setStartDelay(delay).setUpdateListener(ctx.updateListener).setInterpolator(this.mConfig.quintOutInterpolator).setDuration(this.mConfig.taskViewEnterFromHomeDuration + (this.mConfig.taskViewEnterFromHomeStaggerDelay * frontIndex)).withEndAction(new Runnable() {
                @Override
                public void run() {
                    ctx.postAnimationTrigger.decrement();
                }
            }).start();
            ctx.postAnimationTrigger.increment();
            startDelay = delay;
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                TaskView.this.enableFocusAnimations();
            }
        }, startDelay);
    }

    public void fadeInActionButton(int delay, int duration) {
        this.mActionButtonView.setAlpha(0.0f);
        this.mActionButtonView.animate().alpha(1.0f).setStartDelay(delay).setDuration(duration).setInterpolator(PhoneStatusBar.ALPHA_IN).withLayer().start();
    }

    void startExitToHomeAnimation(ViewAnimation.TaskViewExitContext ctx) {
        animate().translationY(ctx.offscreenTranslationY).setStartDelay(0L).setUpdateListener(null).setInterpolator(this.mConfig.fastOutLinearInInterpolator).setDuration(this.mConfig.taskViewExitToHomeDuration).withEndAction(ctx.postAnimationTrigger.decrementAsRunnable()).start();
        ctx.postAnimationTrigger.increment();
    }

    void startLaunchTaskAnimation(Runnable postAnimRunnable, boolean isLaunchingTask, boolean occludesLaunchTarget, boolean lockToTask) {
        if (isLaunchingTask) {
            this.mThumbnailView.startLaunchTaskAnimation(postAnimRunnable);
            if (this.mDimAlpha > 0) {
                ObjectAnimator anim = ObjectAnimator.ofInt(this, "dim", 0);
                anim.setDuration(this.mConfig.taskViewExitToAppDuration);
                anim.setInterpolator(this.mConfig.fastOutLinearInInterpolator);
                anim.start();
            }
            if (!lockToTask) {
                this.mActionButtonView.animate().scaleX(0.9f).scaleY(0.9f);
            }
            this.mActionButtonView.animate().alpha(0.0f).setStartDelay(0L).setDuration(this.mConfig.taskViewExitToAppDuration).setInterpolator(this.mConfig.fastOutLinearInInterpolator).withLayer().start();
            return;
        }
        this.mHeaderView.startLaunchTaskDismissAnimation();
        if (occludesLaunchTarget) {
            animate().alpha(0.0f).translationY(getTranslationY() + this.mConfig.taskViewAffiliateGroupEnterOffsetPx).setStartDelay(0L).setUpdateListener(null).setInterpolator(this.mConfig.fastOutLinearInInterpolator).setDuration(this.mConfig.taskViewExitToAppDuration).start();
        }
    }

    void startDeleteTaskAnimation(final Runnable r) {
        setClipViewInStack(false);
        animate().translationX(this.mConfig.taskViewRemoveAnimTranslationXPx).alpha(0.0f).setStartDelay(0L).setUpdateListener(null).setInterpolator(this.mConfig.fastOutSlowInInterpolator).setDuration(this.mConfig.taskViewRemoveAnimDuration).withEndAction(new Runnable() {
            @Override
            public void run() {
                r.run();
                TaskView.this.setClipViewInStack(true);
            }
        }).start();
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
        startDeleteTaskAnimation(new Runnable() {
            @Override
            public void run() {
                if (TaskView.this.mCb != null) {
                    TaskView.this.mCb.onTaskViewDismissed(this);
                }
            }
        });
    }

    boolean shouldClipViewInStack() {
        return this.mClipViewInStack && getVisibility() == 0;
    }

    void setClipViewInStack(boolean clip) {
        if (clip != this.mClipViewInStack) {
            this.mClipViewInStack = clip;
            if (this.mCb != null) {
                this.mCb.onTaskViewClipStateChanged(this);
            }
        }
    }

    public void setTaskProgress(float p) {
        this.mTaskProgress = p;
        this.mViewBounds.setAlpha(p);
        updateDimFromTaskProgress();
    }

    public float getTaskProgress() {
        return this.mTaskProgress;
    }

    public void setDim(int dim) {
        this.mDimAlpha = dim;
        if (this.mConfig.useHardwareLayers) {
            if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
                this.mDimColorFilter.setColor(Color.argb(this.mDimAlpha, 0, 0, 0));
                this.mDimLayerPaint.setColorFilter(this.mDimColorFilter);
                this.mContent.setLayerType(2, this.mDimLayerPaint);
                return;
            }
            return;
        }
        float dimAlpha = this.mDimAlpha / 255.0f;
        if (this.mThumbnailView != null) {
            this.mThumbnailView.setDimAlpha(dimAlpha);
        }
        if (this.mHeaderView != null) {
            this.mHeaderView.setDimAlpha(dim);
        }
    }

    public int getDim() {
        return this.mDimAlpha;
    }

    void animateDimToProgress(int delay, int duration, Animator.AnimatorListener postAnimRunnable) {
        int toDim = getDimFromTaskProgress();
        if (toDim != getDim()) {
            ObjectAnimator anim = ObjectAnimator.ofInt(this, "dim", toDim);
            anim.setStartDelay(delay);
            anim.setDuration(duration);
            if (postAnimRunnable != null) {
                anim.addListener(postAnimRunnable);
            }
            anim.start();
        }
    }

    int getDimFromTaskProgress() {
        float dim = this.mMaxDimScale * this.mDimInterpolator.getInterpolation(1.0f - this.mTaskProgress);
        return (int) (255.0f * dim);
    }

    void updateDimFromTaskProgress() {
        setDim(getDimFromTaskProgress());
    }

    public void setFocusedTask(boolean animateFocusedState) {
        this.mIsFocused = true;
        if (this.mFocusAnimationsEnabled) {
            this.mHeaderView.onTaskViewFocusChanged(true, animateFocusedState);
        }
        this.mThumbnailView.onFocusChanged(true);
        if (this.mCb != null) {
            this.mCb.onTaskViewFocusChanged(this, true);
        }
        setFocusableInTouchMode(true);
        requestFocus();
        setFocusableInTouchMode(false);
        invalidate();
    }

    void unsetFocusedTask() {
        this.mIsFocused = false;
        if (this.mFocusAnimationsEnabled) {
            this.mHeaderView.onTaskViewFocusChanged(false, true);
        }
        this.mThumbnailView.onFocusChanged(false);
        if (this.mCb != null) {
            this.mCb.onTaskViewFocusChanged(this, false);
        }
        invalidate();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            unsetFocusedTask();
        }
    }

    public boolean isFocusedTask() {
        return this.mIsFocused || isFocused();
    }

    void enableFocusAnimations() {
        boolean wasFocusAnimationsEnabled = this.mFocusAnimationsEnabled;
        this.mFocusAnimationsEnabled = true;
        if (this.mIsFocused && !wasFocusAnimationsEnabled) {
            this.mHeaderView.onTaskViewFocusChanged(true, true);
        }
    }

    public void onTaskBound(Task t) {
        this.mTask = t;
        this.mTask.setCallbacks(this);
        int lockButtonVisibility = (t.lockToTaskEnabled && t.lockToThisTask) ? 0 : 8;
        if (this.mActionButtonView.getVisibility() != lockButtonVisibility) {
            this.mActionButtonView.setVisibility(lockButtonVisibility);
            requestLayout();
        }
    }

    @Override
    public void onTaskDataLoaded() {
        if (this.mThumbnailView != null && this.mHeaderView != null) {
            this.mThumbnailView.rebindToTask(this.mTask);
            this.mHeaderView.rebindToTask(this.mTask);
            this.mHeaderView.mApplicationIcon.setOnClickListener(this);
            this.mHeaderView.mDismissButton.setOnClickListener(this);
            this.mActionButtonView.setOnClickListener(this);
            if (this.mConfig.developerOptionsEnabled) {
                this.mHeaderView.mApplicationIcon.setOnLongClickListener(this);
            }
        }
        this.mTaskDataLoaded = true;
    }

    @Override
    public void onTaskDataUnloaded() {
        if (this.mThumbnailView != null && this.mHeaderView != null) {
            this.mTask.setCallbacks(null);
            this.mThumbnailView.unbindFromTask();
            this.mHeaderView.unbindFromTask();
            this.mHeaderView.mApplicationIcon.setOnClickListener(null);
            this.mHeaderView.mDismissButton.setOnClickListener(null);
            this.mActionButtonView.setOnClickListener(null);
            this.mHeaderView.mApplicationIcon.setOnLongClickListener(null);
        }
        this.mTaskDataLoaded = false;
    }

    void setTouchEnabled(boolean enabled) {
        setOnClickListener(enabled ? this : null);
    }

    @Override
    public void onClick(final View v) {
        boolean delayViewClick = (v == this || v == this.mActionButtonView) ? false : true;
        if (delayViewClick) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (v == TaskView.this.mHeaderView.mDismissButton) {
                        TaskView.this.dismissTask();
                    }
                }
            }, 125L);
            return;
        }
        if (v == this.mActionButtonView) {
            this.mActionButtonView.setTranslationZ(0.0f);
        }
        if (this.mCb != null) {
            this.mCb.onTaskViewClicked(this, getTask(), v == this.mActionButtonView);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v != this.mHeaderView.mApplicationIcon || this.mCb == null) {
            return false;
        }
        this.mCb.onTaskViewAppInfoClicked(this);
        return true;
    }
}
