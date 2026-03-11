package com.android.keyguard;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityContainer;
import com.android.keyguard.KeyguardSecurityModel;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManager;
import java.io.File;

public class KeyguardHostView extends FrameLayout implements KeyguardSecurityContainer.SecurityCallback {
    private AudioManager mAudioManager;
    private Runnable mCancelAction;
    private OnDismissAction mDismissAction;
    protected LockPatternUtils mLockPatternUtils;
    private KeyguardSecurityContainer mSecurityContainer;
    private TelephonyManager mTelephonyManager;
    private final KeyguardUpdateMonitorCallback mUpdateCallback;
    protected ViewMediatorCallback mViewMediatorCallback;

    public interface OnDismissAction {
        boolean onDismiss();
    }

    public KeyguardHostView(Context context) {
        this(context, null);
    }

    public KeyguardHostView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTelephonyManager = null;
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onUserSwitchComplete(int userId) {
                KeyguardHostView.this.getSecurityContainer().showPrimarySecurityScreen(false);
            }

            @Override
            public void onTrustGrantedWithFlags(int flags, int userId) {
                if (userId == KeyguardUpdateMonitor.getCurrentUser() && KeyguardHostView.this.isAttachedToWindow()) {
                    boolean bouncerVisible = KeyguardHostView.this.isVisibleToUser();
                    boolean initiatedByUser = (flags & 1) != 0;
                    boolean dismissKeyguard = (flags & 2) != 0;
                    if (!initiatedByUser && !dismissKeyguard) {
                        return;
                    }
                    if (KeyguardHostView.this.mViewMediatorCallback.isScreenOn() && (bouncerVisible || dismissKeyguard)) {
                        if (!bouncerVisible) {
                            Log.i("KeyguardViewBase", "TrustAgent dismissed Keyguard.");
                        }
                        KeyguardHostView.this.dismiss(false);
                        return;
                    }
                    KeyguardHostView.this.mViewMediatorCallback.playTrustedSound();
                }
            }
        };
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateCallback);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mViewMediatorCallback == null) {
            return;
        }
        this.mViewMediatorCallback.keyguardDoneDrawing();
    }

    public void setOnDismissAction(OnDismissAction action, Runnable cancelAction) {
        if (this.mCancelAction != null) {
            this.mCancelAction.run();
            this.mCancelAction = null;
        }
        this.mDismissAction = action;
        this.mCancelAction = cancelAction;
    }

    public void cancelDismissAction() {
        setOnDismissAction(null, null);
    }

    @Override
    protected void onFinishInflate() {
        this.mSecurityContainer = (KeyguardSecurityContainer) findViewById(R$id.keyguard_security_container);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mSecurityContainer.setLockPatternUtils(this.mLockPatternUtils);
        this.mSecurityContainer.setSecurityCallback(this);
        this.mSecurityContainer.showPrimarySecurityScreen(false);
    }

    public void showPrimarySecurityScreen() {
        Log.d("KeyguardViewBase", "show()");
        this.mSecurityContainer.showPrimarySecurityScreen(false);
    }

    public void showPromptReason(int reason) {
        this.mSecurityContainer.showPromptReason(reason);
    }

    public void showMessage(String message, int color) {
        this.mSecurityContainer.showMessage(message, color);
    }

    public boolean dismiss() {
        if (AntiTheftManager.isDismissable()) {
            return dismiss(false);
        }
        return false;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == 32) {
            event.getText().add(this.mSecurityContainer.getCurrentSecurityModeContentDescription());
            return true;
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    protected KeyguardSecurityContainer getSecurityContainer() {
        return this.mSecurityContainer;
    }

    @Override
    public boolean dismiss(boolean authenticated) {
        return this.mSecurityContainer.showNextSecurityScreenOrFinish(authenticated);
    }

    @Override
    public void updateNavbarStatus() {
        this.mViewMediatorCallback.updateNavbarStatus();
    }

    public void setOnDismissAction(OnDismissAction action) {
        this.mDismissAction = action;
    }

    @Override
    public void finish(boolean strongAuth) {
        boolean deferKeyguardDone = false;
        if (this.mDismissAction != null) {
            deferKeyguardDone = this.mDismissAction.onDismiss();
            this.mDismissAction = null;
            this.mCancelAction = null;
        } else if (VoiceWakeupManager.getInstance().isDismissAndLaunchApp()) {
            Log.d("KeyguardViewBase", "finish() - call VoiceWakeupManager.getInstance().onDismiss().");
            VoiceWakeupManager.getInstance().onDismiss();
            deferKeyguardDone = false;
        }
        if (this.mViewMediatorCallback == null) {
            return;
        }
        if (deferKeyguardDone) {
            this.mViewMediatorCallback.keyguardDonePending(strongAuth);
        } else {
            this.mViewMediatorCallback.keyguardDone(strongAuth);
        }
    }

    @Override
    public void reset() {
        this.mViewMediatorCallback.resetKeyguard();
    }

    @Override
    public void onSecurityModeChanged(KeyguardSecurityModel.SecurityMode securityMode, boolean needsInput) {
        if (this.mViewMediatorCallback == null) {
            return;
        }
        this.mViewMediatorCallback.setNeedsInput(needsInput);
    }

    @Override
    public void userActivity() {
        if (this.mViewMediatorCallback == null) {
            return;
        }
        this.mViewMediatorCallback.userActivity();
    }

    public void onPause() {
        Log.d("KeyguardViewBase", String.format("screen off, instance %s at %s", Integer.toHexString(hashCode()), Long.valueOf(SystemClock.uptimeMillis())));
        KeyguardUpdateMonitor.getInstance(this.mContext).setAlternateUnlockEnabled(true);
        this.mSecurityContainer.showPrimarySecurityScreen(true);
        this.mSecurityContainer.onPause();
        clearFocus();
    }

    public void onResume() {
        Log.d("KeyguardViewBase", "screen on, instance " + Integer.toHexString(hashCode()));
        this.mSecurityContainer.onResume(1);
        requestFocus();
    }

    public void startAppearAnimation() {
        this.mSecurityContainer.startAppearAnimation();
    }

    public void startDisappearAnimation(Runnable finishRunnable) {
        if (this.mSecurityContainer.startDisappearAnimation(finishRunnable) || finishRunnable == null) {
            return;
        }
        finishRunnable.run();
    }

    public void cleanUp() {
        getSecurityContainer().onPause();
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
        if (event.getAction() == 1) {
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
            }
        }
        return false;
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
        if (this.mContext instanceof Activity) {
            return;
        }
        setSystemUiVisibility(4194304);
    }

    public boolean shouldEnableMenuKey() {
        Resources res = getResources();
        boolean configDisabled = res.getBoolean(R$bool.config_disableMenuKeyInLockScreen);
        boolean isTestHarness = ActivityManager.isRunningInTestHarness();
        boolean fileOverride = new File("/data/local/enable_menu_key").exists();
        if (!configDisabled || isTestHarness) {
            return true;
        }
        return fileOverride;
    }

    public void setViewMediatorCallback(ViewMediatorCallback viewMediatorCallback) {
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mViewMediatorCallback.setNeedsInput(this.mSecurityContainer.needsInput());
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

    public void setNotificationPanelView(ViewGroup notificationPanelView) {
        this.mSecurityContainer.setNotificationPanelView(notificationPanelView);
    }

    public void onScreenTurnedOff() {
        this.mSecurityContainer.onScreenTurnedOff();
    }
}
