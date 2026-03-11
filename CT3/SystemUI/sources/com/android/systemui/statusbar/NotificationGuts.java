package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.INotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.Utils;
import com.android.systemui.Interpolators;
import com.android.systemui.tuner.TunerService;

public class NotificationGuts extends LinearLayout implements TunerService.Tunable {
    private float mActiveSliderAlpha;
    private ColorStateList mActiveSliderTint;
    private int mActualHeight;
    private boolean mAuto;
    private ImageView mAutoButton;
    private Drawable mBackground;
    private RadioButton mBlock;
    private int mClipTopAmount;
    private boolean mExposed;
    private Runnable mFalsingCheck;
    private Handler mHandler;
    private INotificationManager mINotificationManager;
    private TextView mImportanceSummary;
    private TextView mImportanceTitle;
    private float mInactiveSliderAlpha;
    private ColorStateList mInactiveSliderTint;
    private OnGutsClosedListener mListener;
    private boolean mNeedsFalsingProtection;
    private int mNotificationImportance;
    private RadioButton mReset;
    private SeekBar mSeekBar;
    private boolean mShowSlider;
    private RadioButton mSilent;
    private int mStartingUserImportance;

    public interface OnGutsClosedListener {
        void onGutsClosed(NotificationGuts notificationGuts);
    }

    public NotificationGuts(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mActiveSliderAlpha = 1.0f;
        setWillNotDraw(false);
        this.mHandler = new Handler();
        this.mFalsingCheck = new Runnable() {
            @Override
            public void run() {
                if (!NotificationGuts.this.mNeedsFalsingProtection || !NotificationGuts.this.mExposed) {
                    return;
                }
                NotificationGuts.this.closeControls(-1, -1, true);
            }
        };
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.Theme, 0, 0);
        this.mInactiveSliderAlpha = ta.getFloat(3, 0.5f);
        ta.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        TunerService.get(this.mContext).addTunable(this, "show_importance_slider");
    }

    @Override
    protected void onDetachedFromWindow() {
        TunerService.get(this.mContext).removeTunable(this);
        super.onDetachedFromWindow();
    }

    public void resetFalsingCheck() {
        this.mHandler.removeCallbacks(this.mFalsingCheck);
        if (!this.mNeedsFalsingProtection || !this.mExposed) {
            return;
        }
        this.mHandler.postDelayed(this.mFalsingCheck, 8000L);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        draw(canvas, this.mBackground);
    }

    private void draw(Canvas canvas, Drawable drawable) {
        if (drawable == null) {
            return;
        }
        drawable.setBounds(0, this.mClipTopAmount, getWidth(), this.mActualHeight);
        drawable.draw(canvas);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mBackground = this.mContext.getDrawable(com.android.systemui.R.drawable.notification_guts_bg);
        if (this.mBackground == null) {
            return;
        }
        this.mBackground.setCallback(this);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mBackground;
    }

    @Override
    protected void drawableStateChanged() {
        drawableStateChanged(this.mBackground);
    }

    private void drawableStateChanged(Drawable d) {
        if (d == null || !d.isStateful()) {
            return;
        }
        d.setState(getDrawableState());
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        if (this.mBackground == null) {
            return;
        }
        this.mBackground.setHotspot(x, y);
    }

    void bindImportance(PackageManager pm, StatusBarNotification sbn, int importance) {
        this.mINotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        this.mStartingUserImportance = -1000;
        try {
            this.mStartingUserImportance = this.mINotificationManager.getImportance(sbn.getPackageName(), sbn.getUid());
        } catch (RemoteException e) {
        }
        this.mNotificationImportance = importance;
        boolean systemApp = false;
        try {
            PackageInfo info = pm.getPackageInfo(sbn.getPackageName(), 64);
            systemApp = Utils.isSystemPackage(pm, info);
        } catch (PackageManager.NameNotFoundException e2) {
        }
        View importanceSlider = findViewById(com.android.systemui.R.id.importance_slider);
        View importanceButtons = findViewById(com.android.systemui.R.id.importance_buttons);
        if (this.mShowSlider) {
            bindSlider(importanceSlider, systemApp);
            importanceSlider.setVisibility(0);
            importanceButtons.setVisibility(8);
        } else {
            bindToggles(importanceButtons, this.mStartingUserImportance, systemApp);
            importanceButtons.setVisibility(0);
            importanceSlider.setVisibility(8);
        }
    }

    public boolean hasImportanceChanged() {
        return this.mStartingUserImportance != getSelectedImportance();
    }

    void saveImportance(StatusBarNotification sbn) {
        int progress = getSelectedImportance();
        MetricsLogger.action(this.mContext, 291, progress - this.mStartingUserImportance);
        try {
            this.mINotificationManager.setImportance(sbn.getPackageName(), sbn.getUid(), progress);
        } catch (RemoteException e) {
        }
    }

    private int getSelectedImportance() {
        if (this.mSeekBar != null && this.mSeekBar.isShown()) {
            if (this.mSeekBar.isEnabled()) {
                return this.mSeekBar.getProgress();
            }
            return -1000;
        }
        if (this.mBlock.isChecked()) {
            return 0;
        }
        return this.mSilent.isChecked() ? 2 : -1000;
    }

    private void bindToggles(View importanceButtons, int importance, boolean systemApp) {
        ((RadioGroup) importanceButtons).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                NotificationGuts.this.resetFalsingCheck();
            }
        });
        this.mBlock = (RadioButton) importanceButtons.findViewById(com.android.systemui.R.id.block_importance);
        this.mSilent = (RadioButton) importanceButtons.findViewById(com.android.systemui.R.id.silent_importance);
        this.mReset = (RadioButton) importanceButtons.findViewById(com.android.systemui.R.id.reset_importance);
        if (systemApp) {
            this.mBlock.setVisibility(8);
            this.mReset.setText(this.mContext.getString(com.android.systemui.R.string.do_not_silence));
        } else {
            this.mReset.setText(this.mContext.getString(com.android.systemui.R.string.do_not_silence_block));
        }
        this.mBlock.setText(this.mContext.getString(com.android.systemui.R.string.block));
        this.mSilent.setText(this.mContext.getString(com.android.systemui.R.string.show_silently));
        if (importance == 2) {
            this.mSilent.setChecked(true);
        } else {
            this.mReset.setChecked(true);
        }
    }

    private void bindSlider(View importanceSlider, boolean systemApp) {
        final int minProgress;
        this.mActiveSliderTint = loadColorStateList(com.android.systemui.R.color.notification_guts_slider_color);
        this.mInactiveSliderTint = loadColorStateList(com.android.systemui.R.color.notification_guts_disabled_slider_color);
        this.mImportanceSummary = (TextView) importanceSlider.findViewById(com.android.systemui.R.id.summary);
        this.mImportanceTitle = (TextView) importanceSlider.findViewById(com.android.systemui.R.id.title);
        this.mSeekBar = (SeekBar) importanceSlider.findViewById(com.android.systemui.R.id.seekbar);
        if (systemApp) {
            minProgress = 1;
        } else {
            minProgress = 0;
        }
        this.mSeekBar.setMax(5);
        this.mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                NotificationGuts.this.resetFalsingCheck();
                if (progress < minProgress) {
                    seekBar.setProgress(minProgress);
                    progress = minProgress;
                }
                NotificationGuts.this.updateTitleAndSummary(progress);
                if (!fromUser) {
                    return;
                }
                MetricsLogger.action(NotificationGuts.this.mContext, 290);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                NotificationGuts.this.resetFalsingCheck();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        this.mSeekBar.setProgress(this.mNotificationImportance);
        this.mAutoButton = (ImageView) importanceSlider.findViewById(com.android.systemui.R.id.auto_importance);
        this.mAutoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationGuts.this.mAuto = !NotificationGuts.this.mAuto;
                NotificationGuts.this.applyAuto();
            }
        });
        this.mAuto = this.mStartingUserImportance == -1000;
        applyAuto();
    }

    public void applyAuto() {
        this.mSeekBar.setEnabled(!this.mAuto);
        ColorStateList starTint = this.mAuto ? this.mActiveSliderTint : this.mInactiveSliderTint;
        float alpha = this.mAuto ? this.mInactiveSliderAlpha : this.mActiveSliderAlpha;
        Drawable icon = this.mAutoButton.getDrawable().mutate();
        icon.setTintList(starTint);
        this.mAutoButton.setImageDrawable(icon);
        this.mSeekBar.setAlpha(alpha);
        if (this.mAuto) {
            this.mSeekBar.setProgress(this.mNotificationImportance);
            this.mImportanceSummary.setText(this.mContext.getString(com.android.systemui.R.string.notification_importance_user_unspecified));
            this.mImportanceTitle.setText(this.mContext.getString(com.android.systemui.R.string.user_unspecified_importance));
            return;
        }
        updateTitleAndSummary(this.mSeekBar.getProgress());
    }

    public void updateTitleAndSummary(int progress) {
        switch (progress) {
            case 0:
                this.mImportanceSummary.setText(this.mContext.getString(com.android.systemui.R.string.notification_importance_blocked));
                this.mImportanceTitle.setText(this.mContext.getString(com.android.systemui.R.string.blocked_importance));
                break;
            case 1:
                this.mImportanceSummary.setText(this.mContext.getString(com.android.systemui.R.string.notification_importance_min));
                this.mImportanceTitle.setText(this.mContext.getString(com.android.systemui.R.string.min_importance));
                break;
            case 2:
                this.mImportanceSummary.setText(this.mContext.getString(com.android.systemui.R.string.notification_importance_low));
                this.mImportanceTitle.setText(this.mContext.getString(com.android.systemui.R.string.low_importance));
                break;
            case 3:
                this.mImportanceSummary.setText(this.mContext.getString(com.android.systemui.R.string.notification_importance_default));
                this.mImportanceTitle.setText(this.mContext.getString(com.android.systemui.R.string.default_importance));
                break;
            case 4:
                this.mImportanceSummary.setText(this.mContext.getString(com.android.systemui.R.string.notification_importance_high));
                this.mImportanceTitle.setText(this.mContext.getString(com.android.systemui.R.string.high_importance));
                break;
            case 5:
                this.mImportanceSummary.setText(this.mContext.getString(com.android.systemui.R.string.notification_importance_max));
                this.mImportanceTitle.setText(this.mContext.getString(com.android.systemui.R.string.max_importance));
                break;
        }
    }

    private ColorStateList loadColorStateList(int colorResId) {
        return ColorStateList.valueOf(this.mContext.getColor(colorResId));
    }

    public void closeControls(int x, int y, boolean notify) {
        if (getWindowToken() == null) {
            if (notify && this.mListener != null) {
                this.mListener.onGutsClosed(this);
                return;
            }
            return;
        }
        if (x == -1 || y == -1) {
            x = (getLeft() + getRight()) / 2;
            y = getTop() + (getHeight() / 2);
        }
        double horz = Math.max(getWidth() - x, x);
        double vert = Math.max(getHeight() - y, y);
        float r = (float) Math.hypot(horz, vert);
        Animator a = ViewAnimationUtils.createCircularReveal(this, x, y, r, 0.0f);
        a.setDuration(360L);
        a.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                NotificationGuts.this.setVisibility(8);
            }
        });
        a.start();
        setExposed(false, this.mNeedsFalsingProtection);
        if (!notify || this.mListener == null) {
            return;
        }
        this.mListener.onGutsClosed(this);
    }

    public void setActualHeight(int actualHeight) {
        this.mActualHeight = actualHeight;
        invalidate();
    }

    public int getActualHeight() {
        return this.mActualHeight;
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
        invalidate();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setClosedListener(OnGutsClosedListener listener) {
        this.mListener = listener;
    }

    public void setExposed(boolean exposed, boolean needsFalsingProtection) {
        this.mExposed = exposed;
        this.mNeedsFalsingProtection = needsFalsingProtection;
        if (this.mExposed && this.mNeedsFalsingProtection) {
            resetFalsingCheck();
        } else {
            this.mHandler.removeCallbacks(this.mFalsingCheck);
        }
    }

    public boolean areGutsExposed() {
        return this.mExposed;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        boolean z = false;
        if (!"show_importance_slider".equals(key)) {
            return;
        }
        if (newValue != null && Integer.parseInt(newValue) != 0) {
            z = true;
        }
        this.mShowSlider = z;
    }
}
