package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.CountDownTimer;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.util.IconDrawableFactory;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewDebug;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.ui.ShowApplicationInfoEvent;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.utilities.Utilities;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.PackageManagerWrapper;

public class TaskViewHeader extends FrameLayout implements View.OnClickListener, View.OnLongClickListener {
    private static IconDrawableFactory sDrawableFactory;
    ImageView mAppIconView;
    String mAppInfoDescFormat;
    ImageView mAppInfoView;
    FrameLayout mAppOverlayView;
    TextView mAppTitleView;
    private HighlightColorDrawable mBackground;
    int mCornerRadius;
    Drawable mDarkDismissDrawable;
    Drawable mDarkFullscreenIcon;
    Drawable mDarkInfoIcon;

    @ViewDebug.ExportedProperty(category = "recents")
    float mDimAlpha;
    private Paint mDimLayerPaint;
    int mDisabledTaskBarBackgroundColor;
    ImageView mDismissButton;
    String mDismissDescFormat;
    private CountDownTimer mFocusTimerCountDown;
    ProgressBar mFocusTimerIndicator;
    int mHeaderBarHeight;
    int mHeaderButtonPadding;
    int mHighlightHeight;
    ImageView mIconView;
    Drawable mLightDismissDrawable;
    Drawable mLightFullscreenIcon;
    Drawable mLightInfoIcon;
    ImageView mMoveTaskButton;
    private HighlightColorDrawable mOverlayBackground;
    private boolean mShouldDarkenBackgroundColor;
    Task mTask;
    int mTaskBarViewDarkTextColor;
    int mTaskBarViewLightTextColor;

    @ViewDebug.ExportedProperty(category = "recents")
    Rect mTaskViewRect;
    int mTaskWindowingMode;
    TextView mTitleView;
    private float[] mTmpHSL;

    private class HighlightColorDrawable extends Drawable {
        private int mColor;
        private float mDimAlpha;
        private Paint mHighlightPaint = new Paint();
        private Paint mBackgroundPaint = new Paint();

        public HighlightColorDrawable() {
            this.mBackgroundPaint.setColor(Color.argb(255, 0, 0, 0));
            this.mBackgroundPaint.setAntiAlias(true);
            this.mHighlightPaint.setColor(Color.argb(255, 255, 255, 255));
            this.mHighlightPaint.setAntiAlias(true);
        }

        public void setColorAndDim(int i, float f) {
            if (this.mColor != i || Float.compare(this.mDimAlpha, f) != 0) {
                this.mColor = i;
                this.mDimAlpha = f;
                if (TaskViewHeader.this.mShouldDarkenBackgroundColor) {
                    i = TaskViewHeader.this.getSecondaryColor(i, false);
                }
                this.mBackgroundPaint.setColor(i);
                ColorUtils.colorToHSL(i, TaskViewHeader.this.mTmpHSL);
                TaskViewHeader.this.mTmpHSL[2] = Math.min(1.0f, TaskViewHeader.this.mTmpHSL[2] + (0.075f * (1.0f - f)));
                this.mHighlightPaint.setColor(ColorUtils.HSLToColor(TaskViewHeader.this.mTmpHSL));
                invalidateSelf();
            }
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public void setAlpha(int i) {
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawRoundRect(0.0f, 0.0f, TaskViewHeader.this.mTaskViewRect.width(), 2 * Math.max(TaskViewHeader.this.mHighlightHeight, TaskViewHeader.this.mCornerRadius), TaskViewHeader.this.mCornerRadius, TaskViewHeader.this.mCornerRadius, this.mHighlightPaint);
            canvas.drawRoundRect(0.0f, TaskViewHeader.this.mHighlightHeight, TaskViewHeader.this.mTaskViewRect.width(), TaskViewHeader.this.getHeight() + TaskViewHeader.this.mCornerRadius, TaskViewHeader.this.mCornerRadius, TaskViewHeader.this.mCornerRadius, this.mBackgroundPaint);
        }

        @Override
        public int getOpacity() {
            return -1;
        }

        public int getColor() {
            return this.mColor;
        }
    }

    public TaskViewHeader(Context context) {
        this(context, null);
    }

    public TaskViewHeader(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attributeSet, int i, int i2) {
        int dimensionPixelSize;
        super(context, attributeSet, i, i2);
        this.mTaskViewRect = new Rect();
        this.mTaskWindowingMode = 0;
        this.mTmpHSL = new float[3];
        this.mDimLayerPaint = new Paint();
        this.mShouldDarkenBackgroundColor = false;
        setWillNotDraw(false);
        Resources resources = context.getResources();
        this.mLightDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_light);
        this.mDarkDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_dark);
        if (Recents.getConfiguration().isGridEnabled) {
            dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.recents_grid_task_view_rounded_corners_radius);
        } else {
            dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        }
        this.mCornerRadius = dimensionPixelSize;
        this.mHighlightHeight = resources.getDimensionPixelSize(R.dimen.recents_task_view_highlight);
        this.mTaskBarViewLightTextColor = context.getColor(R.color.recents_task_bar_light_text_color);
        this.mTaskBarViewDarkTextColor = context.getColor(R.color.recents_task_bar_dark_text_color);
        this.mLightFullscreenIcon = context.getDrawable(R.drawable.recents_move_task_fullscreen_light);
        this.mDarkFullscreenIcon = context.getDrawable(R.drawable.recents_move_task_fullscreen_dark);
        this.mLightInfoIcon = context.getDrawable(R.drawable.recents_info_light);
        this.mDarkInfoIcon = context.getDrawable(R.drawable.recents_info_dark);
        this.mDisabledTaskBarBackgroundColor = context.getColor(R.color.recents_task_bar_disabled_background_color);
        this.mDismissDescFormat = this.mContext.getString(R.string.accessibility_recents_item_will_be_dismissed);
        this.mAppInfoDescFormat = this.mContext.getString(R.string.accessibility_recents_item_open_app_info);
        this.mBackground = new HighlightColorDrawable();
        this.mBackground.setColorAndDim(Color.argb(255, 0, 0, 0), 0.0f);
        setBackground(this.mBackground);
        this.mOverlayBackground = new HighlightColorDrawable();
        this.mDimLayerPaint.setColor(Color.argb(255, 0, 0, 0));
        this.mDimLayerPaint.setAntiAlias(true);
    }

    public void reset() {
        hideAppOverlay(true);
    }

    @Override
    protected void onFinishInflate() {
        Recents.getSystemServices();
        this.mIconView = (ImageView) findViewById(R.id.icon);
        this.mIconView.setOnLongClickListener(this);
        this.mTitleView = (TextView) findViewById(R.id.title);
        this.mDismissButton = (ImageView) findViewById(R.id.dismiss_task);
        onConfigurationChanged();
    }

    private void updateLayoutParams(View view, View view2, View view3, View view4) {
        int i;
        setLayoutParams(new FrameLayout.LayoutParams(-1, this.mHeaderBarHeight, 48));
        view.setLayoutParams(new FrameLayout.LayoutParams(this.mHeaderBarHeight, this.mHeaderBarHeight, 8388611));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -2, 8388627);
        layoutParams.setMarginStart(this.mHeaderBarHeight);
        if (this.mMoveTaskButton != null) {
            i = 2 * this.mHeaderBarHeight;
        } else {
            i = this.mHeaderBarHeight;
        }
        layoutParams.setMarginEnd(i);
        view2.setLayoutParams(layoutParams);
        if (view3 != null) {
            FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(this.mHeaderBarHeight, this.mHeaderBarHeight, 8388613);
            layoutParams2.setMarginEnd(this.mHeaderBarHeight);
            view3.setLayoutParams(layoutParams2);
            view3.setPadding(this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding);
        }
        view4.setLayoutParams(new FrameLayout.LayoutParams(this.mHeaderBarHeight, this.mHeaderBarHeight, 8388613));
        view4.setPadding(this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding);
    }

    public void onConfigurationChanged() {
        getResources();
        int dimensionForDevice = TaskStackLayoutAlgorithm.getDimensionForDevice(getContext(), R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land, R.dimen.recents_grid_task_view_header_height);
        int dimensionForDevice2 = TaskStackLayoutAlgorithm.getDimensionForDevice(getContext(), R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding_tablet_land, R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding_tablet_land, R.dimen.recents_grid_task_view_header_button_padding);
        if (dimensionForDevice != this.mHeaderBarHeight || dimensionForDevice2 != this.mHeaderButtonPadding) {
            this.mHeaderBarHeight = dimensionForDevice;
            this.mHeaderButtonPadding = dimensionForDevice2;
            updateLayoutParams(this.mIconView, this.mTitleView, this.mMoveTaskButton, this.mDismissButton);
            if (this.mAppOverlayView != null) {
                updateLayoutParams(this.mAppIconView, this.mAppTitleView, null, this.mAppInfoView);
            }
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        onTaskViewSizeChanged(this.mTaskViewRect.width(), this.mTaskViewRect.height());
    }

    public void onTaskViewSizeChanged(int i, int i2) {
        this.mTaskViewRect.set(0, 0, i, i2);
        int measuredWidth = i - getMeasuredWidth();
        this.mTitleView.setVisibility(0);
        if (this.mMoveTaskButton != null) {
            this.mMoveTaskButton.setVisibility(0);
            this.mMoveTaskButton.setTranslationX(measuredWidth);
        }
        this.mDismissButton.setVisibility(0);
        this.mDismissButton.setTranslationX(measuredWidth);
        setLeftTopRightBottom(0, 0, i, getMeasuredHeight());
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        canvas.drawRoundRect(0.0f, 0.0f, this.mTaskViewRect.width(), getHeight() + this.mCornerRadius, this.mCornerRadius, this.mCornerRadius, this.mDimLayerPaint);
    }

    public void startFocusTimerIndicator(int i) {
        if (this.mFocusTimerIndicator == null) {
            return;
        }
        this.mFocusTimerIndicator.setVisibility(0);
        this.mFocusTimerIndicator.setMax(i);
        this.mFocusTimerIndicator.setProgress(i);
        if (this.mFocusTimerCountDown != null) {
            this.mFocusTimerCountDown.cancel();
        }
        this.mFocusTimerCountDown = new CountDownTimer(i, 30L) {
            @Override
            public void onTick(long j) {
                TaskViewHeader.this.mFocusTimerIndicator.setProgress((int) j);
            }

            @Override
            public void onFinish() {
            }
        }.start();
    }

    public void cancelFocusTimerIndicator() {
        if (this.mFocusTimerIndicator != null && this.mFocusTimerCountDown != null) {
            this.mFocusTimerCountDown.cancel();
            this.mFocusTimerIndicator.setProgress(0);
            this.mFocusTimerIndicator.setVisibility(4);
        }
    }

    public ImageView getIconView() {
        return this.mIconView;
    }

    int getSecondaryColor(int i, boolean z) {
        return Utilities.getColorWithOverlay(i, z ? -1 : -16777216, 0.8f);
    }

    public void setDimAlpha(float f) {
        if (Float.compare(this.mDimAlpha, f) != 0) {
            this.mDimAlpha = f;
            this.mTitleView.setAlpha(1.0f - f);
            updateBackgroundColor(this.mBackground.getColor(), f);
        }
    }

    private void updateBackgroundColor(int i, float f) {
        if (this.mTask != null) {
            this.mBackground.setColorAndDim(i, f);
            ColorUtils.colorToHSL(i, this.mTmpHSL);
            this.mTmpHSL[2] = Math.min(1.0f, this.mTmpHSL[2] + ((-0.0625f) * (1.0f - f)));
            this.mOverlayBackground.setColorAndDim(ColorUtils.HSLToColor(this.mTmpHSL), f);
            this.mDimLayerPaint.setAlpha((int) (f * 255.0f));
            invalidate();
        }
    }

    public void setShouldDarkenBackgroundColor(boolean z) {
        this.mShouldDarkenBackgroundColor = z;
    }

    public void bindToTask(Task task, boolean z, boolean z2) {
        int i;
        this.mTask = task;
        if (z2) {
            i = this.mDisabledTaskBarBackgroundColor;
        } else {
            i = task.colorPrimary;
        }
        if (this.mBackground.getColor() != i) {
            updateBackgroundColor(i, this.mDimAlpha);
        }
        if (!this.mTitleView.getText().toString().equals(task.title)) {
            this.mTitleView.setText(task.title);
        }
        this.mTitleView.setContentDescription(task.titleDescription);
        this.mTitleView.setTextColor(task.useLightOnPrimaryColor ? this.mTaskBarViewLightTextColor : this.mTaskBarViewDarkTextColor);
        this.mDismissButton.setImageDrawable(task.useLightOnPrimaryColor ? this.mLightDismissDrawable : this.mDarkDismissDrawable);
        this.mDismissButton.setContentDescription(String.format(this.mDismissDescFormat, task.titleDescription));
        this.mDismissButton.setOnClickListener(this);
        this.mDismissButton.setClickable(false);
        ((RippleDrawable) this.mDismissButton.getBackground()).setForceSoftware(true);
        if (z) {
            this.mIconView.setContentDescription(String.format(this.mAppInfoDescFormat, task.titleDescription));
            this.mIconView.setOnClickListener(this);
            this.mIconView.setClickable(true);
        }
    }

    public void onTaskDataLoaded() {
        if (this.mTask != null && this.mTask.icon != null) {
            this.mIconView.setImageDrawable(this.mTask.icon);
        }
    }

    void unbindFromTask(boolean z) {
        this.mTask = null;
        this.mIconView.setImageDrawable(null);
        if (z) {
            this.mIconView.setClickable(false);
        }
    }

    void startNoUserInteractionAnimation() {
        int integer = getResources().getInteger(R.integer.recents_task_enter_from_app_duration);
        this.mDismissButton.setVisibility(0);
        this.mDismissButton.setClickable(true);
        if (this.mDismissButton.getVisibility() == 0) {
            this.mDismissButton.animate().alpha(1.0f).setInterpolator(Interpolators.FAST_OUT_LINEAR_IN).setDuration(integer).start();
        } else {
            this.mDismissButton.setAlpha(1.0f);
        }
        if (this.mMoveTaskButton != null) {
            if (this.mMoveTaskButton.getVisibility() == 0) {
                this.mMoveTaskButton.setVisibility(0);
                this.mMoveTaskButton.setClickable(true);
                this.mMoveTaskButton.animate().alpha(1.0f).setInterpolator(Interpolators.FAST_OUT_LINEAR_IN).setDuration(integer).start();
                return;
            }
            this.mMoveTaskButton.setAlpha(1.0f);
        }
    }

    public void setNoUserInteractionState() {
        this.mDismissButton.setVisibility(0);
        this.mDismissButton.animate().cancel();
        this.mDismissButton.setAlpha(1.0f);
        this.mDismissButton.setClickable(true);
        if (this.mMoveTaskButton != null) {
            this.mMoveTaskButton.setVisibility(0);
            this.mMoveTaskButton.animate().cancel();
            this.mMoveTaskButton.setAlpha(1.0f);
            this.mMoveTaskButton.setClickable(true);
        }
    }

    void resetNoUserInteractionState() {
        this.mDismissButton.setVisibility(4);
        this.mDismissButton.setAlpha(0.0f);
        this.mDismissButton.setClickable(false);
        if (this.mMoveTaskButton != null) {
            this.mMoveTaskButton.setVisibility(4);
            this.mMoveTaskButton.setAlpha(0.0f);
            this.mMoveTaskButton.setClickable(false);
        }
    }

    @Override
    protected int[] onCreateDrawableState(int i) {
        return new int[0];
    }

    @Override
    public void onClick(View view) {
        if (view == this.mIconView) {
            EventBus.getDefault().send(new ShowApplicationInfoEvent(this.mTask));
            return;
        }
        if (view == this.mDismissButton) {
            ((TaskView) Utilities.findParent(this, TaskView.class)).dismissTask();
            MetricsLogger.histogram(getContext(), "overview_task_dismissed_source", 2);
        } else if (view == this.mMoveTaskButton) {
            EventBus.getDefault().send(new LaunchTaskEvent((TaskView) Utilities.findParent(this, TaskView.class), this.mTask, null, false, this.mTaskWindowingMode, 0));
        } else if (view == this.mAppInfoView) {
            EventBus.getDefault().send(new ShowApplicationInfoEvent(this.mTask));
        } else if (view == this.mAppIconView) {
            hideAppOverlay(false);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == this.mIconView) {
            showAppOverlay();
            return true;
        }
        if (view != this.mAppIconView) {
            return false;
        }
        hideAppOverlay(false);
        return true;
    }

    private void showAppOverlay() {
        Drawable drawable;
        Recents.getSystemServices();
        ComponentName component = this.mTask.key.getComponent();
        int i = this.mTask.key.userId;
        ActivityInfo activityInfo = PackageManagerWrapper.getInstance().getActivityInfo(component, i);
        if (activityInfo == null) {
            return;
        }
        if (this.mAppOverlayView == null) {
            this.mAppOverlayView = (FrameLayout) Utilities.findViewStubById(this, R.id.app_overlay_stub).inflate();
            this.mAppOverlayView.setBackground(this.mOverlayBackground);
            this.mAppIconView = (ImageView) this.mAppOverlayView.findViewById(R.id.app_icon);
            this.mAppIconView.setOnClickListener(this);
            this.mAppIconView.setOnLongClickListener(this);
            this.mAppInfoView = (ImageView) this.mAppOverlayView.findViewById(R.id.app_info);
            this.mAppInfoView.setOnClickListener(this);
            this.mAppTitleView = (TextView) this.mAppOverlayView.findViewById(R.id.app_title);
            updateLayoutParams(this.mAppIconView, this.mAppTitleView, null, this.mAppInfoView);
        }
        this.mAppTitleView.setText(ActivityManagerWrapper.getInstance().getBadgedApplicationLabel(activityInfo.applicationInfo, i));
        this.mAppTitleView.setTextColor(this.mTask.useLightOnPrimaryColor ? this.mTaskBarViewLightTextColor : this.mTaskBarViewDarkTextColor);
        this.mAppIconView.setImageDrawable(getIconDrawableFactory().getBadgedIcon(activityInfo.applicationInfo, i));
        ImageView imageView = this.mAppInfoView;
        if (this.mTask.useLightOnPrimaryColor) {
            drawable = this.mLightInfoIcon;
        } else {
            drawable = this.mDarkInfoIcon;
        }
        imageView.setImageDrawable(drawable);
        this.mAppOverlayView.setVisibility(0);
        Animator animatorCreateCircularReveal = ViewAnimationUtils.createCircularReveal(this.mAppOverlayView, this.mIconView.getLeft() + (this.mIconView.getWidth() / 2), this.mIconView.getTop() + (this.mIconView.getHeight() / 2), 0.0f, getWidth());
        animatorCreateCircularReveal.setDuration(250L);
        animatorCreateCircularReveal.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        animatorCreateCircularReveal.start();
    }

    private void hideAppOverlay(boolean z) {
        if (this.mAppOverlayView == null) {
            return;
        }
        if (z) {
            this.mAppOverlayView.setVisibility(8);
            return;
        }
        Animator animatorCreateCircularReveal = ViewAnimationUtils.createCircularReveal(this.mAppOverlayView, this.mIconView.getLeft() + (this.mIconView.getWidth() / 2), this.mIconView.getTop() + (this.mIconView.getHeight() / 2), getWidth(), 0.0f);
        animatorCreateCircularReveal.setDuration(250L);
        animatorCreateCircularReveal.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        animatorCreateCircularReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                TaskViewHeader.this.mAppOverlayView.setVisibility(8);
            }
        });
        animatorCreateCircularReveal.start();
    }

    private static IconDrawableFactory getIconDrawableFactory() {
        if (sDrawableFactory == null) {
            sDrawableFactory = IconDrawableFactory.newInstance(AppGlobals.getInitialApplication());
        }
        return sDrawableFactory;
    }
}
