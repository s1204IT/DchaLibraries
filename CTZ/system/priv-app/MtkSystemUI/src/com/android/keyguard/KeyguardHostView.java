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
import java.lang.reflect.InvocationTargetException;

/* loaded from: classes.dex */
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
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() { // from class: com.android.keyguard.KeyguardHostView.1
            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onUserSwitchComplete(int i) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                KeyguardHostView.this.getSecurityContainer().showPrimarySecurityScreen(false);
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
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

    @Override // android.view.ViewGroup, android.view.View
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

    @Override // android.view.View
    protected void onFinishInflate() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.mSecurityContainer = (KeyguardSecurityContainer) findViewById(com.android.systemui.R.id.keyguard_security_container);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mSecurityContainer.setLockPatternUtils(this.mLockPatternUtils);
        this.mSecurityContainer.setSecurityCallback(this);
        this.mSecurityContainer.showPrimarySecurityScreen(false);
    }

    public void showPrimarySecurityScreen() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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

    @Override // com.android.keyguard.KeyguardSecurityContainer.SecurityCallback
    public boolean dismiss(boolean z, int i) {
        return this.mSecurityContainer.showNextSecurityScreenOrFinish(z, i);
    }

    @Override // com.android.keyguard.KeyguardSecurityContainer.SecurityCallback
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

    @Override // com.android.keyguard.KeyguardSecurityContainer.SecurityCallback
    public void reset() {
        this.mViewMediatorCallback.resetKeyguard();
    }

    public void resetSecurityContainer() {
        this.mSecurityContainer.reset();
    }

    @Override // com.android.keyguard.KeyguardSecurityContainer.SecurityCallback
    public void onSecurityModeChanged(KeyguardSecurityModel.SecurityMode securityMode, boolean z) {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.setNeedsInput(z);
        }
    }

    public CharSequence getAccessibilityTitleForCurrentMode() {
        return this.mSecurityContainer.getTitle();
    }

    @Override // com.android.keyguard.KeyguardSecurityContainer.SecurityCallback
    public void userActivity() {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.userActivity();
        }
    }

    public void onPause() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (interceptMediaKey(keyEvent)) {
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:13:0x0026  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean interceptMediaKey(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (keyEvent.getAction() == 0) {
            if (keyCode != 79 && keyCode != 130) {
                if (keyCode != 164) {
                    if (keyCode != 222) {
                        switch (keyCode) {
                            case 24:
                            case 25:
                                break;
                            default:
                                switch (keyCode) {
                                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_radioButtonStyle /* 85 */:
                                        if (this.mTelephonyManager == null) {
                                            this.mTelephonyManager = (TelephonyManager) getContext().getSystemService("phone");
                                        }
                                        if (this.mTelephonyManager != null && this.mTelephonyManager.getCallState() != 0) {
                                            return true;
                                        }
                                        break;
                                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_ratingBarStyle /* 86 */:
                                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_ratingBarStyleIndicator /* 87 */:
                                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_ratingBarStyleSmall /* 88 */:
                                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_searchViewStyle /* 89 */:
                                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_seekBarStyle /* 90 */:
                                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_selectableItemBackground /* 91 */:
                                        break;
                                    default:
                                        switch (keyCode) {
                                        }
                                }
                        }
                    }
                }
                return false;
            }
            handleMediaKeyEvent(keyEvent);
            return true;
        }
        if (keyEvent.getAction() == 1) {
            if (keyCode != 79 && keyCode != 130 && keyCode != 222) {
                switch (keyCode) {
                    default:
                        switch (keyCode) {
                        }
                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_radioButtonStyle /* 85 */:
                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_ratingBarStyle /* 86 */:
                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_ratingBarStyleIndicator /* 87 */:
                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_ratingBarStyleSmall /* 88 */:
                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_searchViewStyle /* 89 */:
                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_seekBarStyle /* 90 */:
                    case com.android.systemui.plugins.R.styleable.AppCompatTheme_selectableItemBackground /* 91 */:
                        handleMediaKeyEvent(keyEvent);
                        return true;
                }
            }
            handleMediaKeyEvent(keyEvent);
            return true;
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

    @Override // android.view.ViewGroup, android.view.View
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

    @Override // com.android.keyguard.KeyguardSecurityContainer.SecurityCallback
    public void updateNavbarStatus() {
        this.mViewMediatorCallback.updateNavbarStatus();
    }
}
