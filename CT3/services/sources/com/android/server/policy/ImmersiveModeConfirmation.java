package com.android.server.policy;

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
import android.os.RemoteException;
import android.os.ServiceManager;
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
import com.android.server.vr.VrManagerService;

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
    boolean mVrModeEnabled = false;
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
        public void onVrStateChanged(boolean enabled) throws RemoteException {
            ImmersiveModeConfirmation.this.mVrModeEnabled = enabled;
            if (!ImmersiveModeConfirmation.this.mVrModeEnabled) {
                return;
            }
            ImmersiveModeConfirmation.this.mHandler.removeMessages(1);
            ImmersiveModeConfirmation.this.mHandler.sendEmptyMessage(2);
        }
    };
    private final H mHandler = new H(this, null);
    private final long mShowDelayMs = getNavBarExitDuration() * 3;

    public ImmersiveModeConfirmation(Context context) {
        this.mContext = context;
        this.mPanicThresholdMs = context.getResources().getInteger(R.integer.config_doublelineClockDefault);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
    }

    private long getNavBarExitDuration() {
        Animation exit = AnimationUtils.loadAnimation(this.mContext, R.anim.dialog_enter);
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
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "immersive_mode_confirmations", this.mConfirmed ? CONFIRMED : null, -2);
        } catch (Throwable t) {
            Slog.w(TAG, "Error saving confirmations, mConfirmed=" + this.mConfirmed, t);
        }
    }

    void systemReady() {
        IVrManager vrManager = IVrManager.Stub.asInterface(ServiceManager.getService(VrManagerService.VR_MANAGER_BINDER_SERVICE));
        if (vrManager == null) {
            return;
        }
        try {
            vrManager.registerListener(this.mVrStateCallbacks);
            this.mVrModeEnabled = vrManager.getVrModeState();
        } catch (RemoteException e) {
        }
    }

    public void immersiveModeChangedLw(String pkg, boolean isImmersiveMode, boolean userSetupComplete) {
        this.mHandler.removeMessages(1);
        if (isImmersiveMode) {
            boolean disabled = PolicyControl.disableImmersiveConfirmation(pkg);
            if (disabled || this.mConfirmed || !userSetupComplete || this.mVrModeEnabled) {
                return;
            }
            this.mHandler.sendEmptyMessageDelayed(1, this.mShowDelayMs);
            return;
        }
        this.mHandler.sendEmptyMessage(2);
    }

    public boolean onPowerKeyDown(boolean isScreenOn, long time, boolean inImmersiveMode) {
        if (!isScreenOn && time - this.mPanicTime < this.mPanicThresholdMs) {
            return this.mClingWindow == null;
        }
        if (isScreenOn && inImmersiveMode) {
            this.mPanicTime = time;
        } else {
            this.mPanicTime = 0L;
        }
        return false;
    }

    public void confirmCurrentPrompt() {
        if (this.mClingWindow == null) {
            return;
        }
        this.mHandler.post(this.mConfirm);
    }

    private void handleHide() {
        if (this.mClingWindow == null) {
            return;
        }
        this.mWindowManager.removeView(this.mClingWindow);
        this.mClingWindow = null;
    }

    public WindowManager.LayoutParams getClingWindowLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2014, 16777480, -3);
        lp.privateFlags |= 16;
        lp.setTitle(TAG);
        lp.windowAnimations = R.style.AlertDialog.DeviceDefault;
        return lp;
    }

    public FrameLayout.LayoutParams getBubbleLayoutParams() {
        return new FrameLayout.LayoutParams(this.mContext.getResources().getDimensionPixelSize(R.dimen.car_large_avatar_badge_size), -2, 49);
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

        public ClingWindowView(Context context, Runnable confirm) {
            super(context);
            this.mColor = new ColorDrawable(0);
            this.mUpdateLayoutRunnable = new Runnable() {
                @Override
                public void run() {
                    if (ClingWindowView.this.mClingLayout == null || ClingWindowView.this.mClingLayout.getParent() == null) {
                        return;
                    }
                    ClingWindowView.this.mClingLayout.setLayoutParams(ImmersiveModeConfirmation.this.getBubbleLayoutParams());
                }
            };
            this.mInsetsListener = new ViewTreeObserver.OnComputeInternalInsetsListener() {
                private final int[] mTmpInt2 = new int[2];

                public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo inoutInfo) {
                    ClingWindowView.this.mClingLayout.getLocationInWindow(this.mTmpInt2);
                    inoutInfo.setTouchableInsets(3);
                    inoutInfo.touchableRegion.set(this.mTmpInt2[0], this.mTmpInt2[1], this.mTmpInt2[0] + ClingWindowView.this.mClingLayout.getWidth(), this.mTmpInt2[1] + ClingWindowView.this.mClingLayout.getHeight());
                }
            };
            this.mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    if (!intent.getAction().equals("android.intent.action.CONFIGURATION_CHANGED")) {
                        return;
                    }
                    ClingWindowView.this.post(ClingWindowView.this.mUpdateLayoutRunnable);
                }
            };
            this.mConfirm = confirm;
            setBackground(this.mColor);
            setImportantForAccessibility(2);
            this.mInterpolator = AnimationUtils.loadInterpolator(this.mContext, R.interpolator.linear_out_slow_in);
        }

        @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            DisplayMetrics metrics = new DisplayMetrics();
            ImmersiveModeConfirmation.this.mWindowManager.getDefaultDisplay().getMetrics(metrics);
            float density = metrics.density;
            getViewTreeObserver().addOnComputeInternalInsetsListener(this.mInsetsListener);
            this.mClingLayout = (ViewGroup) View.inflate(getContext(), R.layout.date_picker_dialog, null);
            Button ok = (Button) this.mClingLayout.findViewById(R.id.inherit);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClingWindowView.this.mConfirm.run();
                }
            });
            addView(this.mClingLayout, ImmersiveModeConfirmation.this.getBubbleLayoutParams());
            if (ActivityManager.isHighEndGfx()) {
                final View cling = this.mClingLayout;
                cling.setAlpha(0.0f);
                cling.setTranslationY((-96.0f) * density);
                postOnAnimation(new Runnable() {
                    @Override
                    public void run() {
                        cling.animate().alpha(1.0f).translationY(0.0f).setDuration(250L).setInterpolator(ClingWindowView.this.mInterpolator).withLayer().start();
                        ClingWindowView.this.mColorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), 0, Integer.MIN_VALUE);
                        ClingWindowView.this.mColorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                int c = ((Integer) animation.getAnimatedValue()).intValue();
                                ClingWindowView.this.mColor.setColor(c);
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
        private static final int SHOW = 1;

        H(ImmersiveModeConfirmation this$0, H h) {
            this();
        }

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
            }
        }
    }
}
