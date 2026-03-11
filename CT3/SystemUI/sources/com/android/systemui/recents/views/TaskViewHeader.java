package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.CountDownTimer;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.util.Log;
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
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.mediatek.multiwindow.MultiWindowManager;

public class TaskViewHeader extends FrameLayout implements View.OnClickListener, View.OnLongClickListener {
    ImageView mAppIconView;
    ImageView mAppInfoView;
    FrameLayout mAppOverlayView;
    TextView mAppTitleView;
    private HighlightColorDrawable mBackground;
    int mCornerRadius;
    Drawable mDarkDismissDrawable;
    Drawable mDarkFreeformIcon;
    Drawable mDarkFullscreenIcon;
    Drawable mDarkInfoIcon;

    @ViewDebug.ExportedProperty(category = "recents")
    float mDimAlpha;
    private Paint mDimLayerPaint;
    int mDisabledTaskBarBackgroundColor;
    ImageView mDismissButton;
    private CountDownTimer mFocusTimerCountDown;
    ProgressBar mFocusTimerIndicator;
    int mHeaderBarHeight;
    int mHeaderButtonPadding;
    int mHighlightHeight;
    ImageView mIconView;
    Drawable mLightDismissDrawable;
    Drawable mLightFreeformIcon;
    Drawable mLightFullscreenIcon;
    Drawable mLightInfoIcon;
    ImageView mMoveTaskButton;
    int mMoveTaskTargetStackId;
    private HighlightColorDrawable mOverlayBackground;
    Task mTask;
    int mTaskBarViewDarkTextColor;
    int mTaskBarViewLightTextColor;

    @ViewDebug.ExportedProperty(category = "recents")
    Rect mTaskViewRect;
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

        public void setColorAndDim(int color, float dimAlpha) {
            if (this.mColor == color && Float.compare(this.mDimAlpha, dimAlpha) == 0) {
                return;
            }
            this.mColor = color;
            this.mDimAlpha = dimAlpha;
            this.mBackgroundPaint.setColor(color);
            ColorUtils.colorToHSL(color, TaskViewHeader.this.mTmpHSL);
            TaskViewHeader.this.mTmpHSL[2] = Math.min(1.0f, TaskViewHeader.this.mTmpHSL[2] + ((1.0f - dimAlpha) * 0.075f));
            this.mHighlightPaint.setColor(ColorUtils.HSLToColor(TaskViewHeader.this.mTmpHSL));
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawRoundRect(0.0f, 0.0f, TaskViewHeader.this.mTaskViewRect.width(), Math.max(TaskViewHeader.this.mHighlightHeight, TaskViewHeader.this.mCornerRadius) * 2, TaskViewHeader.this.mCornerRadius, TaskViewHeader.this.mCornerRadius, this.mHighlightPaint);
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

    public TaskViewHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTaskViewRect = new Rect();
        this.mMoveTaskTargetStackId = -1;
        this.mTmpHSL = new float[3];
        this.mDimLayerPaint = new Paint();
        setWillNotDraw(false);
        Resources res = context.getResources();
        this.mLightDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_light);
        this.mDarkDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_dark);
        this.mCornerRadius = res.getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        this.mHighlightHeight = res.getDimensionPixelSize(R.dimen.recents_task_view_highlight);
        this.mTaskBarViewLightTextColor = context.getColor(R.color.recents_task_bar_light_text_color);
        this.mTaskBarViewDarkTextColor = context.getColor(R.color.recents_task_bar_dark_text_color);
        this.mLightFreeformIcon = context.getDrawable(R.drawable.recents_move_task_freeform_light);
        this.mDarkFreeformIcon = context.getDrawable(R.drawable.recents_move_task_freeform_dark);
        this.mLightFullscreenIcon = context.getDrawable(R.drawable.recents_move_task_fullscreen_light);
        this.mDarkFullscreenIcon = context.getDrawable(R.drawable.recents_move_task_fullscreen_dark);
        this.mLightInfoIcon = context.getDrawable(R.drawable.recents_info_light);
        this.mDarkInfoIcon = context.getDrawable(R.drawable.recents_info_dark);
        this.mDisabledTaskBarBackgroundColor = context.getColor(R.color.recents_task_bar_disabled_background_color);
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
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mIconView = (ImageView) findViewById(R.id.icon);
        this.mIconView.setOnLongClickListener(this);
        this.mTitleView = (TextView) findViewById(R.id.title);
        this.mDismissButton = (ImageView) findViewById(R.id.dismiss_task);
        if (ssp.hasFreeformWorkspaceSupport()) {
            this.mMoveTaskButton = (ImageView) findViewById(R.id.move_task);
            if (MultiWindowManager.isSupported()) {
                if (MultiWindowManager.DEBUG) {
                    Log.d("BMW", "onFinishInflate, ssp.hasDockedTask() = " + ssp.hasDockedTask());
                }
                if (ssp.hasDockedTask()) {
                    this.mMoveTaskButton.setVisibility(4);
                } else {
                    this.mMoveTaskButton.setVisibility(0);
                }
            }
        }
        onConfigurationChanged();
    }

    private void updateLayoutParams(View icon, View title, View secondaryButton, View button) {
        int i;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, this.mHeaderBarHeight, 48);
        setLayoutParams(lp);
        FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(this.mHeaderBarHeight, this.mHeaderBarHeight, 8388611);
        icon.setLayoutParams(lp2);
        FrameLayout.LayoutParams lp3 = new FrameLayout.LayoutParams(-1, -2, 8388627);
        lp3.setMarginStart(this.mHeaderBarHeight);
        if (this.mMoveTaskButton != null) {
            i = this.mHeaderBarHeight * 2;
        } else {
            i = this.mHeaderBarHeight;
        }
        lp3.setMarginEnd(i);
        title.setLayoutParams(lp3);
        if (secondaryButton != null) {
            FrameLayout.LayoutParams lp4 = new FrameLayout.LayoutParams(this.mHeaderBarHeight, this.mHeaderBarHeight, 8388613);
            lp4.setMarginEnd(this.mHeaderBarHeight);
            secondaryButton.setLayoutParams(lp4);
            secondaryButton.setPadding(this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding);
        }
        FrameLayout.LayoutParams lp5 = new FrameLayout.LayoutParams(this.mHeaderBarHeight, this.mHeaderBarHeight, 8388613);
        button.setLayoutParams(lp5);
        button.setPadding(this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding);
    }

    public void onConfigurationChanged() {
        getResources();
        int headerBarHeight = TaskStackLayoutAlgorithm.getDimensionForDevice(getContext(), R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land);
        int headerButtonPadding = TaskStackLayoutAlgorithm.getDimensionForDevice(getContext(), R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding_tablet_land, R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding_tablet_land);
        if (headerBarHeight == this.mHeaderBarHeight && headerButtonPadding == this.mHeaderButtonPadding) {
            return;
        }
        this.mHeaderBarHeight = headerBarHeight;
        this.mHeaderButtonPadding = headerButtonPadding;
        updateLayoutParams(this.mIconView, this.mTitleView, this.mMoveTaskButton, this.mDismissButton);
        if (this.mAppOverlayView == null) {
            return;
        }
        updateLayoutParams(this.mAppIconView, this.mAppTitleView, null, this.mAppInfoView);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        onTaskViewSizeChanged(this.mTaskViewRect.width(), this.mTaskViewRect.height());
    }

    public void onTaskViewSizeChanged(int width, int height) {
        int moveTaskWidth;
        this.mTaskViewRect.set(0, 0, width, height);
        boolean showTitle = true;
        boolean showMoveIcon = true;
        boolean showDismissIcon = true;
        int rightInset = width - getMeasuredWidth();
        if (this.mTask != null && this.mTask.isFreeformTask()) {
            int appIconWidth = this.mIconView.getMeasuredWidth();
            int titleWidth = (int) this.mTitleView.getPaint().measureText(this.mTask.title);
            int dismissWidth = this.mDismissButton.getMeasuredWidth();
            if (this.mMoveTaskButton != null) {
                moveTaskWidth = this.mMoveTaskButton.getMeasuredWidth();
            } else {
                moveTaskWidth = 0;
            }
            showTitle = width >= ((appIconWidth + dismissWidth) + moveTaskWidth) + titleWidth;
            showMoveIcon = width >= (appIconWidth + dismissWidth) + moveTaskWidth;
            showDismissIcon = width >= appIconWidth + dismissWidth;
        }
        this.mTitleView.setVisibility(showTitle ? 0 : 4);
        if (this.mMoveTaskButton != null) {
            if (MultiWindowManager.isSupported()) {
                SystemServicesProxy ssp = Recents.getSystemServices();
                if (MultiWindowManager.DEBUG) {
                    Log.d("BMW", "onFinishInflate, ssp.hasDockedTask() = " + ssp.hasDockedTask());
                }
                if (ssp.hasDockedTask() && showMoveIcon) {
                    showMoveIcon = false;
                }
            }
            this.mMoveTaskButton.setVisibility(showMoveIcon ? 0 : 4);
            this.mMoveTaskButton.setTranslationX(rightInset);
        }
        this.mDismissButton.setVisibility(showDismissIcon ? 0 : 4);
        this.mDismissButton.setTranslationX(rightInset);
        setLeftTopRightBottom(0, 0, width, getMeasuredHeight());
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        canvas.drawRoundRect(0.0f, 0.0f, this.mTaskViewRect.width(), getHeight() + this.mCornerRadius, this.mCornerRadius, this.mCornerRadius, this.mDimLayerPaint);
    }

    public void startFocusTimerIndicator(int duration) {
        if (this.mFocusTimerIndicator == null) {
            return;
        }
        this.mFocusTimerIndicator.setVisibility(0);
        this.mFocusTimerIndicator.setMax(duration);
        this.mFocusTimerIndicator.setProgress(duration);
        if (this.mFocusTimerCountDown != null) {
            this.mFocusTimerCountDown.cancel();
        }
        this.mFocusTimerCountDown = new CountDownTimer(duration, 30L) {
            @Override
            public void onTick(long millisUntilFinished) {
                TaskViewHeader.this.mFocusTimerIndicator.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
            }
        }.start();
    }

    public void cancelFocusTimerIndicator() {
        if (this.mFocusTimerIndicator == null || this.mFocusTimerCountDown == null) {
            return;
        }
        this.mFocusTimerCountDown.cancel();
        this.mFocusTimerIndicator.setProgress(0);
        this.mFocusTimerIndicator.setVisibility(4);
    }

    public ImageView getIconView() {
        return this.mIconView;
    }

    int getSecondaryColor(int primaryColor, boolean useLightOverlayColor) {
        int overlayColor = useLightOverlayColor ? -1 : -16777216;
        return Utilities.getColorWithOverlay(primaryColor, overlayColor, 0.8f);
    }

    public void setDimAlpha(float dimAlpha) {
        if (Float.compare(this.mDimAlpha, dimAlpha) == 0) {
            return;
        }
        this.mDimAlpha = dimAlpha;
        this.mTitleView.setAlpha(1.0f - dimAlpha);
        updateBackgroundColor(this.mBackground.getColor(), dimAlpha);
    }

    private void updateBackgroundColor(int color, float dimAlpha) {
        if (this.mTask == null) {
            return;
        }
        this.mBackground.setColorAndDim(color, dimAlpha);
        ColorUtils.colorToHSL(color, this.mTmpHSL);
        this.mTmpHSL[2] = Math.min(1.0f, this.mTmpHSL[2] + ((1.0f - dimAlpha) * (-0.0625f)));
        this.mOverlayBackground.setColorAndDim(ColorUtils.HSLToColor(this.mTmpHSL), dimAlpha);
        this.mDimLayerPaint.setAlpha((int) (255.0f * dimAlpha));
        invalidate();
    }

    public void bindToTask(Task t, boolean touchExplorationEnabled, boolean disabledInSafeMode) {
        int primaryColor;
        Drawable drawable;
        Drawable drawable2;
        this.mTask = t;
        if (disabledInSafeMode) {
            primaryColor = this.mDisabledTaskBarBackgroundColor;
        } else {
            primaryColor = t.colorPrimary;
        }
        if (this.mBackground.getColor() != primaryColor) {
            updateBackgroundColor(primaryColor, this.mDimAlpha);
        }
        if (!this.mTitleView.getText().toString().equals(t.title)) {
            this.mTitleView.setText(t.title);
        }
        this.mTitleView.setContentDescription(t.titleDescription);
        this.mTitleView.setTextColor(t.useLightOnPrimaryColor ? this.mTaskBarViewLightTextColor : this.mTaskBarViewDarkTextColor);
        this.mDismissButton.setImageDrawable(t.useLightOnPrimaryColor ? this.mLightDismissDrawable : this.mDarkDismissDrawable);
        this.mDismissButton.setContentDescription(t.dismissDescription);
        this.mDismissButton.setOnClickListener(this);
        this.mDismissButton.setClickable(false);
        ((RippleDrawable) this.mDismissButton.getBackground()).setForceSoftware(true);
        if (this.mMoveTaskButton != null) {
            if (t.isFreeformTask()) {
                this.mMoveTaskTargetStackId = 1;
                ImageView imageView = this.mMoveTaskButton;
                if (t.useLightOnPrimaryColor) {
                    drawable2 = this.mLightFullscreenIcon;
                } else {
                    drawable2 = this.mDarkFullscreenIcon;
                }
                imageView.setImageDrawable(drawable2);
            } else {
                this.mMoveTaskTargetStackId = 2;
                ImageView imageView2 = this.mMoveTaskButton;
                if (t.useLightOnPrimaryColor) {
                    drawable = this.mLightFreeformIcon;
                } else {
                    drawable = this.mDarkFreeformIcon;
                }
                imageView2.setImageDrawable(drawable);
            }
            this.mMoveTaskButton.setOnClickListener(this);
            this.mMoveTaskButton.setClickable(false);
            ((RippleDrawable) this.mMoveTaskButton.getBackground()).setForceSoftware(true);
        }
        if (Recents.getDebugFlags().isFastToggleRecentsEnabled()) {
            if (this.mFocusTimerIndicator == null) {
                this.mFocusTimerIndicator = (ProgressBar) Utilities.findViewStubById(this, R.id.focus_timer_indicator_stub).inflate();
            }
            this.mFocusTimerIndicator.getProgressDrawable().setColorFilter(getSecondaryColor(t.colorPrimary, t.useLightOnPrimaryColor), PorterDuff.Mode.SRC_IN);
        }
        if (!touchExplorationEnabled) {
            return;
        }
        this.mIconView.setContentDescription(t.appInfoDescription);
        this.mIconView.setOnClickListener(this);
        this.mIconView.setClickable(true);
    }

    public void onTaskDataLoaded() {
        if (this.mTask.icon == null) {
            return;
        }
        this.mIconView.setImageDrawable(this.mTask.icon);
    }

    void unbindFromTask(boolean touchExplorationEnabled) {
        this.mTask = null;
        this.mIconView.setImageDrawable(null);
        if (!touchExplorationEnabled) {
            return;
        }
        this.mIconView.setClickable(false);
    }

    void startNoUserInteractionAnimation() {
        int duration = getResources().getInteger(R.integer.recents_task_enter_from_app_duration);
        this.mDismissButton.setVisibility(0);
        this.mDismissButton.setClickable(true);
        if (this.mDismissButton.getVisibility() == 0) {
            this.mDismissButton.animate().alpha(1.0f).setInterpolator(Interpolators.FAST_OUT_LINEAR_IN).setDuration(duration).start();
        } else {
            this.mDismissButton.setAlpha(1.0f);
        }
        if (this.mMoveTaskButton == null) {
            return;
        }
        if (this.mMoveTaskButton.getVisibility() == 0) {
            this.mMoveTaskButton.setVisibility(0);
            this.mMoveTaskButton.setClickable(true);
            this.mMoveTaskButton.animate().alpha(1.0f).setInterpolator(Interpolators.FAST_OUT_LINEAR_IN).setDuration(duration).start();
            return;
        }
        this.mMoveTaskButton.setAlpha(1.0f);
    }

    void setNoUserInteractionState() {
        this.mDismissButton.setVisibility(0);
        this.mDismissButton.animate().cancel();
        this.mDismissButton.setAlpha(1.0f);
        this.mDismissButton.setClickable(true);
        if (this.mMoveTaskButton == null) {
            return;
        }
        if (MultiWindowManager.isSupported() && Recents.getSystemServices().hasDockedTask()) {
            return;
        }
        this.mMoveTaskButton.setVisibility(0);
        this.mMoveTaskButton.animate().cancel();
        this.mMoveTaskButton.setAlpha(1.0f);
        this.mMoveTaskButton.setClickable(true);
    }

    void resetNoUserInteractionState() {
        this.mDismissButton.setVisibility(4);
        this.mDismissButton.setAlpha(0.0f);
        this.mDismissButton.setClickable(false);
        if (this.mMoveTaskButton == null) {
            return;
        }
        this.mMoveTaskButton.setVisibility(4);
        this.mMoveTaskButton.setAlpha(0.0f);
        this.mMoveTaskButton.setClickable(false);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        return new int[0];
    }

    @Override
    public void onClick(View v) {
        Rect bounds;
        if (v == this.mIconView) {
            EventBus.getDefault().send(new ShowApplicationInfoEvent(this.mTask));
            return;
        }
        if (v == this.mDismissButton) {
            TaskView tv = (TaskView) Utilities.findParent(this, TaskView.class);
            tv.dismissTask();
            MetricsLogger.histogram(getContext(), "overview_task_dismissed_source", 2);
        } else {
            if (v == this.mMoveTaskButton) {
                TaskView tv2 = (TaskView) Utilities.findParent(this, TaskView.class);
                if (this.mMoveTaskTargetStackId == 2) {
                    bounds = new Rect(this.mTaskViewRect);
                } else {
                    bounds = new Rect();
                }
                EventBus.getDefault().send(new LaunchTaskEvent(tv2, this.mTask, bounds, this.mMoveTaskTargetStackId, false));
                return;
            }
            if (v == this.mAppInfoView) {
                EventBus.getDefault().send(new ShowApplicationInfoEvent(this.mTask));
            } else {
                if (v != this.mAppIconView) {
                    return;
                }
                hideAppOverlay(false);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == this.mIconView) {
            showAppOverlay();
            return true;
        }
        if (v != this.mAppIconView) {
            return false;
        }
        hideAppOverlay(false);
        return true;
    }

    private void showAppOverlay() {
        Drawable drawable;
        SystemServicesProxy ssp = Recents.getSystemServices();
        ComponentName cn = this.mTask.key.getComponent();
        int userId = this.mTask.key.userId;
        ActivityInfo activityInfo = ssp.getActivityInfo(cn, userId);
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
        this.mAppTitleView.setText(ssp.getBadgedApplicationLabel(activityInfo.applicationInfo, userId));
        this.mAppTitleView.setTextColor(this.mTask.useLightOnPrimaryColor ? this.mTaskBarViewLightTextColor : this.mTaskBarViewDarkTextColor);
        this.mAppIconView.setImageDrawable(ssp.getBadgedApplicationIcon(activityInfo.applicationInfo, userId));
        ImageView imageView = this.mAppInfoView;
        if (this.mTask.useLightOnPrimaryColor) {
            drawable = this.mLightInfoIcon;
        } else {
            drawable = this.mDarkInfoIcon;
        }
        imageView.setImageDrawable(drawable);
        this.mAppOverlayView.setVisibility(0);
        int x = this.mIconView.getLeft() + (this.mIconView.getWidth() / 2);
        int y = this.mIconView.getTop() + (this.mIconView.getHeight() / 2);
        Animator revealAnim = ViewAnimationUtils.createCircularReveal(this.mAppOverlayView, x, y, 0.0f, getWidth());
        revealAnim.setDuration(250L);
        revealAnim.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        revealAnim.start();
    }

    private void hideAppOverlay(boolean immediate) {
        if (this.mAppOverlayView == null) {
            return;
        }
        if (immediate) {
            this.mAppOverlayView.setVisibility(8);
            return;
        }
        int x = this.mIconView.getLeft() + (this.mIconView.getWidth() / 2);
        int y = this.mIconView.getTop() + (this.mIconView.getHeight() / 2);
        Animator revealAnim = ViewAnimationUtils.createCircularReveal(this.mAppOverlayView, x, y, getWidth(), 0.0f);
        revealAnim.setDuration(250L);
        revealAnim.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        revealAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                TaskViewHeader.this.mAppOverlayView.setVisibility(8);
            }
        });
        revealAnim.start();
    }
}
