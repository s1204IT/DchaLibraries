package com.android.keyguard;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardSecurityContainer;
import com.android.keyguard.KeyguardSecurityModel;
import java.io.File;

public abstract class KeyguardViewBase extends FrameLayout implements KeyguardSecurityContainer.SecurityCallback {
    private final KeyguardActivityLauncher mActivityLauncher;
    private AudioManager mAudioManager;
    private KeyguardHostView.OnDismissAction mDismissAction;
    protected LockPatternUtils mLockPatternUtils;
    private KeyguardSecurityContainer mSecurityContainer;
    private TelephonyManager mTelephonyManager;
    protected ViewMediatorCallback mViewMediatorCallback;

    public abstract void cleanUp();

    public abstract long getUserActivityTimeout();

    public KeyguardViewBase(Context context) {
        this(context, null);
    }

    public KeyguardViewBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTelephonyManager = null;
        this.mActivityLauncher = new KeyguardActivityLauncher() {
            @Override
            Context getContext() {
                return KeyguardViewBase.this.mContext;
            }

            @Override
            void setOnDismissAction(KeyguardHostView.OnDismissAction action) {
                KeyguardViewBase.this.setOnDismissAction(action);
            }

            @Override
            LockPatternUtils getLockPatternUtils() {
                return KeyguardViewBase.this.mLockPatternUtils;
            }

            @Override
            void requestDismissKeyguard() {
                KeyguardViewBase.this.dismiss(false);
            }
        };
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.keyguardDoneDrawing();
        }
    }

    public void setOnDismissAction(KeyguardHostView.OnDismissAction action) {
        this.mDismissAction = action;
    }

    @Override
    protected void onFinishInflate() {
        this.mSecurityContainer = (KeyguardSecurityContainer) findViewById(R.id.keyguard_security_container);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mSecurityContainer.setLockPatternUtils(this.mLockPatternUtils);
        this.mSecurityContainer.setSecurityCallback(this);
        this.mSecurityContainer.showPrimarySecurityScreen(false);
    }

    public void showPrimarySecurityScreen() {
        this.mSecurityContainer.showPrimarySecurityScreen(false);
    }

    public boolean dismiss() {
        return dismiss(false);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != 32) {
            return super.dispatchPopulateAccessibilityEvent(event);
        }
        event.getText().add(this.mSecurityContainer.getCurrentSecurityModeContentDescription());
        return true;
    }

    protected KeyguardSecurityContainer getSecurityContainer() {
        return this.mSecurityContainer;
    }

    public boolean dismiss(boolean authenticated) {
        return this.mSecurityContainer.showNextSecurityScreenOrFinish(authenticated);
    }

    @Override
    public void finish() {
        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(true);
        boolean deferKeyguardDone = false;
        if (this.mDismissAction != null) {
            deferKeyguardDone = this.mDismissAction.onDismiss();
            this.mDismissAction = null;
        }
        if (this.mViewMediatorCallback != null) {
            if (deferKeyguardDone) {
                this.mViewMediatorCallback.keyguardDonePending();
            } else {
                this.mViewMediatorCallback.keyguardDone(true);
            }
        }
    }

    @Override
    public void onSecurityModeChanged(KeyguardSecurityModel.SecurityMode securityMode, boolean needsInput) {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.setNeedsInput(needsInput);
        }
    }

    public void userActivity() {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.userActivity();
        }
    }

    protected void onUserActivityTimeoutChanged() {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.onUserActivityTimeoutChanged();
        }
    }

    public void onPause() {
        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(true);
        this.mSecurityContainer.showPrimarySecurityScreen(true);
        this.mSecurityContainer.onPause();
        clearFocus();
    }

    public void onResume() {
        this.mSecurityContainer.onResume(1);
        requestFocus();
    }

    public void startAppearAnimation() {
        this.mSecurityContainer.startAppearAnimation();
    }

    public void startDisappearAnimation(Runnable finishRunnable) {
        if (!this.mSecurityContainer.startDisappearAnimation(finishRunnable) && finishRunnable != null) {
            finishRunnable.run();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (interceptMediaKey(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public boolean interceptMediaKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == 0) {
            switch (keyCode) {
                case 24:
                case 25:
                case 164:
                default:
                    return false;
                case 79:
                case 86:
                case 87:
                case 88:
                case 89:
                case 90:
                case 91:
                case 130:
                case 222:
                    break;
                case 85:
                case 126:
                case 127:
                    if (this.mTelephonyManager == null) {
                        this.mTelephonyManager = (TelephonyManager) getContext().getSystemService("phone");
                    }
                    if (this.mTelephonyManager != null && this.mTelephonyManager.getCallState() != 0) {
                        return true;
                    }
                    break;
            }
            handleMediaKeyEvent(event);
            return true;
        }
        if (event.getAction() != 1) {
            return false;
        }
        switch (keyCode) {
            case 79:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 126:
            case 127:
            case 130:
            case 222:
                handleMediaKeyEvent(event);
                return true;
            default:
                return false;
        }
    }

    private void handleMediaKeyEvent(KeyEvent keyEvent) {
        synchronized (this) {
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
            }
        }
        this.mAudioManager.dispatchMediaKeyEvent(keyEvent);
    }

    @Override
    public void dispatchSystemUiVisibilityChanged(int visibility) {
        super.dispatchSystemUiVisibilityChanged(visibility);
        if (!(this.mContext instanceof Activity)) {
            setSystemUiVisibility(4194304);
        }
    }

    private boolean shouldEnableMenuKey() {
        Resources res = getResources();
        boolean configDisabled = res.getBoolean(R.bool.config_disableMenuKeyInLockScreen);
        boolean isTestHarness = ActivityManager.isRunningInTestHarness();
        boolean fileOverride = new File("/data/local/enable_menu_key").exists();
        return !configDisabled || isTestHarness || fileOverride;
    }

    public boolean handleMenuKey() {
        if (!shouldEnableMenuKey()) {
            return false;
        }
        dismiss();
        return true;
    }

    public void setViewMediatorCallback(ViewMediatorCallback viewMediatorCallback) {
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mViewMediatorCallback.setNeedsInput(this.mSecurityContainer.needsInput());
    }

    protected KeyguardActivityLauncher getActivityLauncher() {
        return this.mActivityLauncher;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
        this.mSecurityContainer.setLockPatternUtils(utils);
    }

    public KeyguardSecurityModel.SecurityMode getSecurityMode() {
        return this.mSecurityContainer.getSecurityMode();
    }

    public KeyguardSecurityModel.SecurityMode getCurrentSecurityMode() {
        return this.mSecurityContainer.getCurrentSecurityMode();
    }
}
