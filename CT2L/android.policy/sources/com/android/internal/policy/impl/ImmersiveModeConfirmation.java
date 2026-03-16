package com.android.internal.policy.impl;

import android.R;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;

public class ImmersiveModeConfirmation {
    private static final String CONFIRMED = "confirmed";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SHOW_EVERY_TIME = false;
    private static final String TAG = "ImmersiveModeConfirmation";
    private ClingWindowView mClingWindow;
    private boolean mConfirmed;
    private final Context mContext;
    private int mCurrentUserId;
    private final long mPanicThresholdMs;
    private long mPanicTime;
    private WindowManager mWindowManager;
    private final SparseBooleanArray mUserPanicResets = new SparseBooleanArray();
    private final Runnable mConfirm = new Runnable() {
        @Override
        public void run() {
            if (!ImmersiveModeConfirmation.this.mConfirmed) {
                ImmersiveModeConfirmation.this.mConfirmed = true;
                ImmersiveModeConfirmation.this.saveSetting();
            }
            ImmersiveModeConfirmation.this.handleHide();
        }
    };
    private final H mHandler = new H();
    private final long mShowDelayMs = getNavBarExitDuration() * 3;

    public ImmersiveModeConfirmation(Context context) {
        this.mContext = context;
        this.mPanicThresholdMs = context.getResources().getInteger(R.integer.config_displayWhiteBalanceColorTemperatureDefault);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
    }

    private long getNavBarExitDuration() {
        Animation exit = AnimationUtils.loadAnimation(this.mContext, R.anim.btn_checkbox_to_unchecked_box_inner_merged_animation);
        if (exit != null) {
            return exit.getDuration();
        }
        return 0L;
    }

    public void loadSetting(int currentUserId) {
        this.mConfirmed = false;
        this.mCurrentUserId = currentUserId;
        String value = null;
        try {
            value = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "immersive_mode_confirmations", -2);
            this.mConfirmed = CONFIRMED.equals(value);
        } catch (Throwable t) {
            Slog.w(TAG, "Error loading confirmations, value=" + value, t);
        }
    }

    private void saveSetting() {
        try {
            String value = this.mConfirmed ? CONFIRMED : null;
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "immersive_mode_confirmations", value, -2);
        } catch (Throwable t) {
            Slog.w(TAG, "Error saving confirmations, mConfirmed=" + this.mConfirmed, t);
        }
    }

    public void immersiveModeChanged(String pkg, boolean isImmersiveMode, boolean userSetupComplete) {
        this.mHandler.removeMessages(1);
        if (isImmersiveMode) {
            boolean disabled = PolicyControl.disableImmersiveConfirmation(pkg);
            if (!disabled && !this.mConfirmed && userSetupComplete) {
                this.mHandler.sendEmptyMessageDelayed(1, this.mShowDelayMs);
                return;
            }
            return;
        }
        this.mHandler.sendEmptyMessage(2);
    }

    public boolean onPowerKeyDown(boolean isScreenOn, long time, boolean inImmersiveMode) {
        if (!isScreenOn && time - this.mPanicTime < this.mPanicThresholdMs) {
            this.mHandler.sendEmptyMessage(3);
            return this.mClingWindow == null;
        }
        if (isScreenOn && inImmersiveMode) {
            this.mPanicTime = time;
            return false;
        }
        this.mPanicTime = 0L;
        return false;
    }

    public void confirmCurrentPrompt() {
        if (this.mClingWindow != null) {
            this.mHandler.post(this.mConfirm);
        }
    }

    private void handlePanic() {
        if (!this.mUserPanicResets.get(this.mCurrentUserId, false)) {
            this.mUserPanicResets.put(this.mCurrentUserId, true);
            this.mConfirmed = false;
            saveSetting();
        }
    }

    private void handleHide() {
        if (this.mClingWindow != null) {
            this.mWindowManager.removeView(this.mClingWindow);
            this.mClingWindow = null;
        }
    }

    public WindowManager.LayoutParams getClingWindowLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2005, 16777480, -3);
        lp.privateFlags |= 16;
        lp.setTitle(TAG);
        lp.windowAnimations = R.style.AccessibilityAutoclickPanelButtonLayoutStyle;
        lp.gravity = 119;
        return lp;
    }

    public FrameLayout.LayoutParams getBubbleLayoutParams() {
        return new FrameLayout.LayoutParams(this.mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_4), -2, 49);
    }

    private class ClingWindowView extends FrameLayout {
        private static final int BGCOLOR = Integer.MIN_VALUE;
        private static final int OFFSET_DP = 48;
        private ViewGroup mClingLayout;
        private final ColorDrawable mColor;
        private ValueAnimator mColorAnim;
        private final Runnable mConfirm;
        private BroadcastReceiver mReceiver;
        private Runnable mUpdateLayoutRunnable;

        public ClingWindowView(Context context, Runnable confirm) {
            super(context);
            this.mColor = new ColorDrawable(0);
            this.mUpdateLayoutRunnable = new Runnable() {
                @Override
                public void run() {
                    if (ClingWindowView.this.mClingLayout != null && ClingWindowView.this.mClingLayout.getParent() != null) {
                        ClingWindowView.this.mClingLayout.setLayoutParams(ImmersiveModeConfirmation.this.getBubbleLayoutParams());
                    }
                }
            };
            this.mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    if (intent.getAction().equals("android.intent.action.CONFIGURATION_CHANGED")) {
                        ClingWindowView.this.post(ClingWindowView.this.mUpdateLayoutRunnable);
                    }
                }
            };
            this.mConfirm = confirm;
            setClickable(true);
            setBackground(this.mColor);
        }

        @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            DisplayMetrics metrics = new DisplayMetrics();
            ImmersiveModeConfirmation.this.mWindowManager.getDefaultDisplay().getMetrics(metrics);
            float density = metrics.density;
            this.mClingLayout = (ViewGroup) View.inflate(getContext(), R.layout.choose_account_row, null);
            Button ok = (Button) this.mClingLayout.findViewById(R.id.find_next);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClingWindowView.this.mConfirm.run();
                }
            });
            addView(this.mClingLayout, ImmersiveModeConfirmation.this.getBubbleLayoutParams());
            if (ActivityManager.isHighEndGfx()) {
                View bubble = this.mClingLayout.findViewById(R.id.KEYCODE_3);
                bubble.setAlpha(0.0f);
                bubble.setTranslationY((-48.0f) * density);
                bubble.animate().alpha(1.0f).translationY(0.0f).setDuration(300L).setInterpolator(new DecelerateInterpolator()).start();
                ok.setAlpha(0.0f);
                ok.setTranslationY((-48.0f) * density);
                ok.animate().alpha(1.0f).translationY(0.0f).setDuration(300L).setStartDelay(200L).setInterpolator(new DecelerateInterpolator()).start();
                this.mColorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), 0, Integer.valueOf(BGCOLOR));
                this.mColorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int c = ((Integer) animation.getAnimatedValue()).intValue();
                        ClingWindowView.this.mColor.setColor(c);
                    }
                });
                this.mColorAnim.setDuration(1000L);
                this.mColorAnim.start();
            } else {
                this.mColor.setColor(BGCOLOR);
            }
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
        }

        @Override
        public void onDetachedFromWindow() {
            this.mContext.unregisterReceiver(this.mReceiver);
        }

        @Override
        public boolean onTouchEvent(MotionEvent motion) {
            return true;
        }
    }

    private void handleShow() {
        this.mClingWindow = new ClingWindowView(this.mContext, this.mConfirm);
        this.mClingWindow.setSystemUiVisibility(768);
        WindowManager.LayoutParams lp = getClingWindowLayoutParams();
        this.mWindowManager.addView(this.mClingWindow, lp);
    }

    private final class H extends Handler {
        private static final int HIDE = 2;
        private static final int PANIC = 3;
        private static final int SHOW = 1;

        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ImmersiveModeConfirmation.this.handleShow();
                    break;
                case 2:
                    ImmersiveModeConfirmation.this.handleHide();
                    break;
                case 3:
                    ImmersiveModeConfirmation.this.handlePanic();
                    break;
            }
        }
    }
}
