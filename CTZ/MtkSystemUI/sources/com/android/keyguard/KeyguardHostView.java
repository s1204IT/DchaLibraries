package com.android.keyguard;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityContainer;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.settingslib.Utils;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManagerProxy;
import java.io.File;

public class KeyguardHostView extends FrameLayout implements KeyguardSecurityContainer.SecurityCallback {
    public static final boolean DEBUG = KeyguardConstants.DEBUG;
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

    public KeyguardHostView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTelephonyManager = null;
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onUserSwitchComplete(int i) {
                KeyguardHostView.this.getSecurityContainer().showPrimarySecurityScreen(false);
            }

            @Override
            public void onTrustGrantedWithFlags(int i, int i2) {
                if (i2 == KeyguardUpdateMonitor.getCurrentUser() && KeyguardHostView.this.isAttachedToWindow()) {
                    boolean zIsVisibleToUser = KeyguardHostView.this.isVisibleToUser();
                    boolean z = (i & 1) != 0;
                    boolean z2 = (i & 2) != 0;
                    if (z || z2) {
                        if (KeyguardHostView.this.mViewMediatorCallback.isScreenOn() && (zIsVisibleToUser || z2)) {
                            if (!zIsVisibleToUser) {
                                Log.i("KeyguardViewBase", "TrustAgent dismissed Keyguard.");
                            }
                            KeyguardHostView.this.dismiss(false, i2);
                            return;
                        }
                        KeyguardHostView.this.mViewMediatorCallback.playTrustedSound();
                    }
                }
            }
        };
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateCallback);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.keyguardDoneDrawing();
        }
    }

    public void setOnDismissAction(OnDismissAction onDismissAction, Runnable runnable) {
        if (this.mCancelAction != null) {
            this.mCancelAction.run();
            this.mCancelAction = null;
        }
        this.mDismissAction = onDismissAction;
        this.mCancelAction = runnable;
    }

    public boolean hasDismissActions() {
        return (this.mDismissAction == null && this.mCancelAction == null) ? false : true;
    }

    public void cancelDismissAction() {
        setOnDismissAction(null, null);
    }

    @Override
    protected void onFinishInflate() {
        this.mSecurityContainer = (KeyguardSecurityContainer) findViewById(com.android.systemui.R.id.keyguard_security_container);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mSecurityContainer.setLockPatternUtils(this.mLockPatternUtils);
        this.mSecurityContainer.setSecurityCallback(this);
        this.mSecurityContainer.showPrimarySecurityScreen(false);
    }

    public void showPrimarySecurityScreen() {
        if (DEBUG) {
            Log.d("KeyguardViewBase", "show()");
        }
        this.mSecurityContainer.showPrimarySecurityScreen(false);
    }

    public void showPromptReason(int i) {
        this.mSecurityContainer.showPromptReason(i);
    }

    public void showMessage(CharSequence charSequence, int i) {
        this.mSecurityContainer.showMessage(charSequence, i);
    }

    public void showErrorMessage(CharSequence charSequence) {
        showMessage(charSequence, Utils.getColorError(this.mContext));
    }

    public boolean dismiss(int i) {
        if (AntiTheftManager.isDismissable()) {
            return dismiss(false, i);
        }
        return false;
    }

    protected KeyguardSecurityContainer getSecurityContainer() {
        return this.mSecurityContainer;
    }

    @Override
    public boolean dismiss(boolean z, int i) {
        return this.mSecurityContainer.showNextSecurityScreenOrFinish(z, i);
    }

    @Override
    public void finish(boolean z, int i) {
        boolean zOnDismiss = false;
        if (this.mDismissAction != null) {
            zOnDismiss = this.mDismissAction.onDismiss();
            this.mDismissAction = null;
            this.mCancelAction = null;
        } else if (VoiceWakeupManagerProxy.getInstance().isDismissAndLaunchApp()) {
            if (DEBUG) {
                Log.d("KeyguardViewBase", "finish() - call VoiceWakeupManager.getInstance().onDismiss().");
            }
            VoiceWakeupManagerProxy.getInstance().onDismiss();
        }
        if (this.mViewMediatorCallback != null) {
            if (zOnDismiss) {
                this.mViewMediatorCallback.keyguardDonePending(z, i);
            } else {
                this.mViewMediatorCallback.keyguardDone(z, i);
            }
        }
    }

    @Override
    public void reset() {
        this.mViewMediatorCallback.resetKeyguard();
    }

    public void resetSecurityContainer() {
        this.mSecurityContainer.reset();
    }

    @Override
    public void onSecurityModeChanged(KeyguardSecurityModel.SecurityMode securityMode, boolean z) {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.setNeedsInput(z);
        }
    }

    public CharSequence getAccessibilityTitleForCurrentMode() {
        return this.mSecurityContainer.getTitle();
    }

    @Override
    public void userActivity() {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.userActivity();
        }
    }

    public void onPause() {
        if (DEBUG) {
            Log.d("KeyguardViewBase", String.format("screen off, instance %s at %s", Integer.toHexString(hashCode()), Long.valueOf(SystemClock.uptimeMillis())));
        }
        this.mSecurityContainer.showPrimarySecurityScreen(true);
        this.mSecurityContainer.onPause();
        clearFocus();
    }

    public void onResume() {
        if (DEBUG) {
            Log.d("KeyguardViewBase", "screen on, instance " + Integer.toHexString(hashCode()));
        }
        this.mSecurityContainer.onResume(1);
        requestFocus();
    }

    public void startAppearAnimation() {
        this.mSecurityContainer.startAppearAnimation();
    }

    public void startDisappearAnimation(Runnable runnable) {
        if (!this.mSecurityContainer.startDisappearAnimation(runnable) && runnable != null) {
            runnable.run();
        }
    }

    public void cleanUp() {
        getSecurityContainer().onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (interceptMediaKey(keyEvent)) {
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    public boolean interceptMediaKey(android.view.KeyEvent r8) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardHostView.interceptMediaKey(android.view.KeyEvent):boolean");
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
    public void dispatchSystemUiVisibilityChanged(int i) {
        super.dispatchSystemUiVisibilityChanged(i);
        if (!(this.mContext instanceof Activity)) {
            setSystemUiVisibility(4194304);
        }
    }

    public boolean shouldEnableMenuKey() {
        return !getResources().getBoolean(com.android.systemui.R.bool.config_disableMenuKeyInLockScreen) || ActivityManager.isRunningInTestHarness() || new File("/data/local/enable_menu_key").exists();
    }

    public void setViewMediatorCallback(ViewMediatorCallback viewMediatorCallback) {
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mViewMediatorCallback.setNeedsInput(this.mSecurityContainer.needsInput());
    }

    public void setLockPatternUtils(LockPatternUtils lockPatternUtils) {
        this.mLockPatternUtils = lockPatternUtils;
        this.mSecurityContainer.setLockPatternUtils(lockPatternUtils);
    }

    public KeyguardSecurityModel.SecurityMode getSecurityMode() {
        return this.mSecurityContainer.getSecurityMode();
    }

    public KeyguardSecurityModel.SecurityMode getCurrentSecurityMode() {
        return this.mSecurityContainer.getCurrentSecurityMode();
    }

    @Override
    public void updateNavbarStatus() {
        this.mViewMediatorCallback.updateNavbarStatus();
    }
}
