package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;

public class TaskViewHeader extends FrameLayout {
    static Paint sHighlightPaint;
    TextView mActivityDescription;
    ImageView mApplicationIcon;
    RippleDrawable mBackground;
    int mBackgroundColor;
    GradientDrawable mBackgroundColorDrawable;
    RecentsConfiguration mConfig;
    int mCurrentPrimaryColor;
    boolean mCurrentPrimaryColorIsDark;
    Drawable mDarkDismissDrawable;
    PorterDuffColorFilter mDimColorFilter;
    Paint mDimLayerPaint;
    ImageView mDismissButton;
    String mDismissContentDescription;
    AnimatorSet mFocusAnimator;
    Drawable mLightDismissDrawable;

    public TaskViewHeader(Context context) {
        this(context, null);
    }

    public TaskViewHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mDimLayerPaint = new Paint();
        this.mDimColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);
        this.mConfig = RecentsConfiguration.getInstance();
        setWillNotDraw(false);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRect(0, 0, TaskViewHeader.this.getMeasuredWidth(), TaskViewHeader.this.getMeasuredHeight());
            }
        });
        Resources res = context.getResources();
        this.mLightDismissDrawable = res.getDrawable(R.drawable.recents_dismiss_light);
        this.mDarkDismissDrawable = res.getDrawable(R.drawable.recents_dismiss_dark);
        this.mDismissContentDescription = res.getString(R.string.accessibility_recents_item_will_be_dismissed);
        if (sHighlightPaint == null) {
            sHighlightPaint = new Paint();
            sHighlightPaint.setStyle(Paint.Style.STROKE);
            sHighlightPaint.setStrokeWidth(this.mConfig.taskViewHighlightPx);
            sHighlightPaint.setColor(this.mConfig.taskBarViewHighlightColor);
            sHighlightPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            sHighlightPaint.setAntiAlias(true);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    protected void onFinishInflate() {
        this.mApplicationIcon = (ImageView) findViewById(R.id.application_icon);
        this.mActivityDescription = (TextView) findViewById(R.id.activity_description);
        this.mDismissButton = (ImageView) findViewById(R.id.dismiss_task);
        if (this.mApplicationIcon.getBackground() instanceof RippleDrawable) {
            this.mApplicationIcon.setBackground(null);
        }
        this.mBackgroundColorDrawable = (GradientDrawable) getContext().getDrawable(R.drawable.recents_task_view_header_bg_color);
        this.mBackground = (RippleDrawable) getContext().getDrawable(R.drawable.recents_task_view_header_bg);
        this.mBackground = (RippleDrawable) this.mBackground.mutate().getConstantState().newDrawable();
        this.mBackground.setColor(ColorStateList.valueOf(0));
        this.mBackground.setDrawableByLayerId(this.mBackground.getId(0), this.mBackgroundColorDrawable);
        setBackground(this.mBackground);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float offset = (float) Math.ceil(this.mConfig.taskViewHighlightPx / 2.0f);
        float radius = this.mConfig.taskViewRoundedCornerRadiusPx;
        int count = canvas.save(2);
        canvas.clipRect(0, 0, getMeasuredWidth(), getMeasuredHeight());
        canvas.drawRoundRect(-offset, 0.0f, getMeasuredWidth() + offset, getMeasuredHeight() + radius, radius, radius, sHighlightPaint);
        canvas.restoreToCount(count);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    void setDimAlpha(int alpha) {
        this.mDimColorFilter.setColor(Color.argb(alpha, 0, 0, 0));
        this.mDimLayerPaint.setColorFilter(this.mDimColorFilter);
        setLayerType(2, this.mDimLayerPaint);
    }

    int getSecondaryColor(int primaryColor, boolean useLightOverlayColor) {
        int overlayColor = useLightOverlayColor ? -1 : -16777216;
        return Utilities.getColorWithOverlay(primaryColor, overlayColor, 0.8f);
    }

    public void rebindToTask(Task t) {
        if (t.activityIcon != null) {
            this.mApplicationIcon.setImageDrawable(t.activityIcon);
        } else if (t.applicationIcon != null) {
            this.mApplicationIcon.setImageDrawable(t.applicationIcon);
        }
        this.mApplicationIcon.setContentDescription(t.activityLabel);
        if (!this.mActivityDescription.getText().toString().equals(t.activityLabel)) {
            this.mActivityDescription.setText(t.activityLabel);
        }
        int existingBgColor = getBackground() instanceof ColorDrawable ? ((ColorDrawable) getBackground()).getColor() : 0;
        if (existingBgColor != t.colorPrimary) {
            this.mBackgroundColorDrawable.setColor(t.colorPrimary);
            this.mBackgroundColor = t.colorPrimary;
        }
        this.mCurrentPrimaryColor = t.colorPrimary;
        this.mCurrentPrimaryColorIsDark = t.useLightOnPrimaryColor;
        this.mActivityDescription.setTextColor(t.useLightOnPrimaryColor ? this.mConfig.taskBarViewLightTextColor : this.mConfig.taskBarViewDarkTextColor);
        this.mDismissButton.setImageDrawable(t.useLightOnPrimaryColor ? this.mLightDismissDrawable : this.mDarkDismissDrawable);
        this.mDismissButton.setContentDescription(String.format(this.mDismissContentDescription, t.activityLabel));
    }

    void unbindFromTask() {
        this.mApplicationIcon.setImageDrawable(null);
    }

    void startLaunchTaskDismissAnimation() {
        if (this.mDismissButton.getVisibility() == 0) {
            this.mDismissButton.animate().cancel();
            this.mDismissButton.animate().alpha(0.0f).setStartDelay(0L).setInterpolator(this.mConfig.fastOutSlowInInterpolator).setDuration(this.mConfig.taskViewExitToAppDuration).withLayer().start();
        }
    }

    void startNoUserInteractionAnimation() {
        if (this.mDismissButton.getVisibility() != 0) {
            this.mDismissButton.setVisibility(0);
            this.mDismissButton.setAlpha(0.0f);
            this.mDismissButton.animate().alpha(1.0f).setStartDelay(0L).setInterpolator(this.mConfig.fastOutLinearInInterpolator).setDuration(this.mConfig.taskViewEnterFromAppDuration).withLayer().start();
        }
    }

    void setNoUserInteractionState() {
        if (this.mDismissButton.getVisibility() != 0) {
            this.mDismissButton.animate().cancel();
            this.mDismissButton.setVisibility(0);
            this.mDismissButton.setAlpha(1.0f);
        }
    }

    void resetNoUserInteractionState() {
        this.mDismissButton.setVisibility(4);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        return new int[0];
    }

    void onTaskViewFocusChanged(boolean focused, boolean animateFocusedState) {
        if (animateFocusedState) {
            boolean isRunning = false;
            if (this.mFocusAnimator != null) {
                isRunning = this.mFocusAnimator.isRunning();
                Utilities.cancelAnimationWithoutCallbacks(this.mFocusAnimator);
            }
            if (focused) {
                int secondaryColor = getSecondaryColor(this.mCurrentPrimaryColor, this.mCurrentPrimaryColorIsDark);
                int[][] states = {new int[]{android.R.attr.state_enabled}, new int[]{android.R.attr.state_pressed}};
                int[] newStates = {android.R.attr.state_enabled, android.R.attr.state_pressed};
                int[] colors = {secondaryColor, secondaryColor};
                this.mBackground.setColor(new ColorStateList(states, colors));
                this.mBackground.setState(newStates);
                int currentColor = this.mBackgroundColor;
                int lightPrimaryColor = getSecondaryColor(this.mCurrentPrimaryColor, this.mCurrentPrimaryColorIsDark);
                ValueAnimator backgroundColor = ValueAnimator.ofObject(new ArgbEvaluator(), Integer.valueOf(currentColor), Integer.valueOf(lightPrimaryColor));
                backgroundColor.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        TaskViewHeader.this.mBackground.setState(new int[0]);
                    }
                });
                backgroundColor.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int color = ((Integer) animation.getAnimatedValue()).intValue();
                        TaskViewHeader.this.mBackgroundColorDrawable.setColor(color);
                        TaskViewHeader.this.mBackgroundColor = color;
                    }
                });
                backgroundColor.setRepeatCount(-1);
                backgroundColor.setRepeatMode(2);
                ObjectAnimator translation = ObjectAnimator.ofFloat(this, "translationZ", 15.0f);
                translation.setRepeatCount(-1);
                translation.setRepeatMode(2);
                this.mFocusAnimator = new AnimatorSet();
                this.mFocusAnimator.playTogether(backgroundColor, translation);
                this.mFocusAnimator.setStartDelay(750L);
                this.mFocusAnimator.setDuration(750L);
                this.mFocusAnimator.start();
                return;
            }
            if (isRunning) {
                int currentColor2 = this.mBackgroundColor;
                ValueAnimator backgroundColor2 = ValueAnimator.ofObject(new ArgbEvaluator(), Integer.valueOf(currentColor2), Integer.valueOf(this.mCurrentPrimaryColor));
                backgroundColor2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int color = ((Integer) animation.getAnimatedValue()).intValue();
                        TaskViewHeader.this.mBackgroundColorDrawable.setColor(color);
                        TaskViewHeader.this.mBackgroundColor = color;
                    }
                });
                ObjectAnimator translation2 = ObjectAnimator.ofFloat(this, "translationZ", 0.0f);
                this.mFocusAnimator = new AnimatorSet();
                this.mFocusAnimator.playTogether(backgroundColor2, translation2);
                this.mFocusAnimator.setDuration(150L);
                this.mFocusAnimator.start();
                return;
            }
            this.mBackground.setState(new int[0]);
            setTranslationZ(0.0f);
        }
    }
}
