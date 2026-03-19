package com.android.server.policy;

import android.R;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.provider.Settings;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;

public class ImmersiveModeConfirmation {
    private static final String CONFIRMED = "confirmed";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_SHOW_EVERY_TIME = false;
    private static final String TAG = "ImmersiveModeConfirmation";
    private ClingWindowView mClingWindow;
    private boolean mConfirmed;
    private int mCurrentUserId;
    private final long mPanicThresholdMs;
    private long mPanicTime;
    private final IBinder mWindowToken = new Binder();
    boolean mVrModeEnabled = false;
    private int mLockTaskState = 0;
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
    private final IVrStateCallbacks mVrStateCallbacks = new IVrStateCallbacks.Stub() {
        public void onVrStateChanged(boolean z) throws RemoteException {
            ImmersiveModeConfirmation.this.mVrModeEnabled = z;
            if (ImmersiveModeConfirmation.this.mVrModeEnabled) {
                ImmersiveModeConfirmation.this.mHandler.removeMessages(1);
                ImmersiveModeConfirmation.this.mHandler.sendEmptyMessage(2);
            }
        }
    };
    private final Context mContext = ActivityThread.currentActivityThread().getSystemUiContext();
    private final H mHandler = new H();
    private final long mShowDelayMs = getNavBarExitDuration() * 3;
    private WindowManager mWindowManager = (WindowManager) this.mContext.getSystemService("window");

    public ImmersiveModeConfirmation(Context context) {
        this.mPanicThresholdMs = context.getResources().getInteger(R.integer.config_criticalBatteryWarningLevel);
    }

    private long getNavBarExitDuration() {
        Animation animationLoadAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.dream_activity_open_enter);
        if (animationLoadAnimation != null) {
            return animationLoadAnimation.getDuration();
        }
        return 0L;
    }

    public void loadSetting(int i) {
        this.mConfirmed = false;
        this.mCurrentUserId = i;
        String str = null;
        try {
            String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "immersive_mode_confirmations", -2);
            try {
                this.mConfirmed = CONFIRMED.equals(stringForUser);
            } catch (Throwable th) {
                th = th;
                str = stringForUser;
                Slog.w(TAG, "Error loading confirmations, value=" + str, th);
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void saveSetting() {
        try {
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "immersive_mode_confirmations", this.mConfirmed ? CONFIRMED : null, -2);
        } catch (Throwable th) {
            Slog.w(TAG, "Error saving confirmations, mConfirmed=" + this.mConfirmed, th);
        }
    }

    void systemReady() {
        IVrManager iVrManagerAsInterface = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        if (iVrManagerAsInterface != null) {
            try {
                iVrManagerAsInterface.registerListener(this.mVrStateCallbacks);
                this.mVrModeEnabled = iVrManagerAsInterface.getVrModeState();
            } catch (RemoteException e) {
            }
        }
    }

    public void immersiveModeChangedLw(String str, boolean z, boolean z2, boolean z3) {
        this.mHandler.removeMessages(1);
        if (z) {
            if (!PolicyControl.disableImmersiveConfirmation(str) && !this.mConfirmed && z2 && !this.mVrModeEnabled && !z3 && !UserManager.isDeviceInDemoMode(this.mContext) && this.mLockTaskState != 1) {
                this.mHandler.sendEmptyMessageDelayed(1, this.mShowDelayMs);
                return;
            }
            return;
        }
        this.mHandler.sendEmptyMessage(2);
    }

    public boolean onPowerKeyDown(boolean z, long j, boolean z2, boolean z3) {
        if (!z && j - this.mPanicTime < this.mPanicThresholdMs) {
            return this.mClingWindow == null;
        }
        if (z && z2 && !z3) {
            this.mPanicTime = j;
        } else {
            this.mPanicTime = 0L;
        }
        return false;
    }

    public void confirmCurrentPrompt() {
        if (this.mClingWindow != null) {
            this.mHandler.post(this.mConfirm);
        }
    }

    private void handleHide() {
        if (this.mClingWindow != null) {
            this.mWindowManager.removeView(this.mClingWindow);
            this.mClingWindow = null;
        }
    }

    public WindowManager.LayoutParams getClingWindowLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2014, 16777504, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.setTitle(TAG);
        layoutParams.windowAnimations = R.style.AccessibilityDialogTitle;
        layoutParams.token = getWindowToken();
        return layoutParams;
    }

    public FrameLayout.LayoutParams getBubbleLayoutParams() {
        return new FrameLayout.LayoutParams(this.mContext.getResources().getDimensionPixelSize(R.dimen.car_switch_thumb_size), -2, 49);
    }

    public IBinder getWindowToken() {
        return this.mWindowToken;
    }

    private class ClingWindowView extends FrameLayout {
        private static final int ANIMATION_DURATION = 250;
        private static final int BGCOLOR = Integer.MIN_VALUE;
        private static final int OFFSET_DP = 96;
        private ViewGroup mClingLayout;
        private final ColorDrawable mColor;
        private ValueAnimator mColorAnim;
        private final Runnable mConfirm;
        private ViewTreeObserver.OnComputeInternalInsetsListener mInsetsListener;
        private final Interpolator mInterpolator;
        private BroadcastReceiver mReceiver;
        private Runnable mUpdateLayoutRunnable;

        public ClingWindowView(Context context, Runnable runnable) {
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
            this.mInsetsListener = new ViewTreeObserver.OnComputeInternalInsetsListener() {
                private final int[] mTmpInt2 = new int[2];

                public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
                    ClingWindowView.this.mClingLayout.getLocationInWindow(this.mTmpInt2);
                    internalInsetsInfo.setTouchableInsets(3);
                    internalInsetsInfo.touchableRegion.set(this.mTmpInt2[0], this.mTmpInt2[1], this.mTmpInt2[0] + ClingWindowView.this.mClingLayout.getWidth(), this.mTmpInt2[1] + ClingWindowView.this.mClingLayout.getHeight());
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
            this.mConfirm = runnable;
            setBackground(this.mColor);
            setImportantForAccessibility(2);
            this.mInterpolator = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.linear_out_slow_in);
        }

        @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ImmersiveModeConfirmation.this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
            float f = displayMetrics.density;
            getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsListener);
            this.mClingLayout = (ViewGroup) View.inflate(getContext(), R.layout.date_picker_view_animator_material, null);
            ((Button) this.mClingLayout.findViewById(R.id.hard_keyboard_switch)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClingWindowView.this.mConfirm.run();
                }
            });
            addView(this.mClingLayout, ImmersiveModeConfirmation.this.getBubbleLayoutParams());
            if (ActivityManager.isHighEndGfx()) {
                final ViewGroup viewGroup = this.mClingLayout;
                viewGroup.setAlpha(0.0f);
                viewGroup.setTranslationY((-96.0f) * f);
                postOnAnimation(new Runnable() {
                    @Override
                    public void run() {
                        viewGroup.animate().alpha(1.0f).translationY(0.0f).setDuration(250L).setInterpolator(ClingWindowView.this.mInterpolator).withLayer().start();
                        ClingWindowView.this.mColorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), 0, Integer.MIN_VALUE);
                        ClingWindowView.this.mColorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                ClingWindowView.this.mColor.setColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
                            }
                        });
                        ClingWindowView.this.mColorAnim.setDuration(250L);
                        ClingWindowView.this.mColorAnim.setInterpolator(ClingWindowView.this.mInterpolator);
                        ClingWindowView.this.mColorAnim.start();
                    }
                });
            } else {
                this.mColor.setColor(Integer.MIN_VALUE);
            }
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
        }

        @Override
        public void onDetachedFromWindow() {
            this.mContext.unregisterReceiver(this.mReceiver);
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            return true;
        }
    }

    private void handleShow() {
        this.mClingWindow = new ClingWindowView(this.mContext, this.mConfirm);
        this.mClingWindow.setSystemUiVisibility(768);
        this.mWindowManager.addView(this.mClingWindow, getClingWindowLayoutParams());
    }

    private final class H extends Handler {
        private static final int HIDE = 2;
        private static final int SHOW = 1;

        private H() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    ImmersiveModeConfirmation.this.handleShow();
                    break;
                case 2:
                    ImmersiveModeConfirmation.this.handleHide();
                    break;
            }
        }
    }

    void onLockTaskModeChangedLw(int i) {
        this.mLockTaskState = i;
    }
}
