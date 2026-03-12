package com.android.keyguard;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IRotationWatcher;
import android.view.IWindowManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardMessageArea;

public class KeyguardFaceUnlockView extends LinearLayout implements KeyguardSecurityView {
    private BiometricSensorUnlock mBiometricUnlock;
    private Drawable mBouncerFrame;
    private ImageButton mCancelButton;
    private View mEcaView;
    private View mFaceUnlockAreaView;
    private boolean mIsBouncerVisibleToUser;
    private final Object mIsBouncerVisibleToUserLock;
    private KeyguardSecurityCallback mKeyguardSecurityCallback;
    private int mLastRotation;
    private LockPatternUtils mLockPatternUtils;
    private final IRotationWatcher mRotationWatcher;
    private SecurityMessageDisplay mSecurityMessageDisplay;
    KeyguardUpdateMonitorCallback mUpdateCallback;
    private boolean mWatchingRotation;
    private final IWindowManager mWindowManager;

    public KeyguardFaceUnlockView(Context context) {
        this(context, null);
    }

    public KeyguardFaceUnlockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsBouncerVisibleToUser = false;
        this.mIsBouncerVisibleToUserLock = new Object();
        this.mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mRotationWatcher = new IRotationWatcher.Stub() {
            public void onRotationChanged(int rotation) {
                if (Math.abs(rotation - KeyguardFaceUnlockView.this.mLastRotation) == 2 && KeyguardFaceUnlockView.this.mBiometricUnlock != null) {
                    KeyguardFaceUnlockView.this.mBiometricUnlock.stop();
                    KeyguardFaceUnlockView.this.maybeStartBiometricUnlock();
                }
                KeyguardFaceUnlockView.this.mLastRotation = rotation;
            }
        };
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onPhoneStateChanged(int phoneState) {
                if (phoneState == 1 && KeyguardFaceUnlockView.this.mBiometricUnlock != null) {
                    KeyguardFaceUnlockView.this.mBiometricUnlock.stopAndShowBackup();
                }
            }

            @Override
            public void onUserSwitching(int userId) {
                if (KeyguardFaceUnlockView.this.mBiometricUnlock != null) {
                    KeyguardFaceUnlockView.this.mBiometricUnlock.stop();
                }
            }

            @Override
            public void onUserSwitchComplete(int userId) {
                if (KeyguardFaceUnlockView.this.mBiometricUnlock != null) {
                    KeyguardFaceUnlockView.this.maybeStartBiometricUnlock();
                }
            }

            @Override
            public void onKeyguardVisibilityChanged(boolean showing) {
                KeyguardFaceUnlockView.this.handleBouncerUserVisibilityChanged();
            }

            @Override
            public void onKeyguardBouncerChanged(boolean bouncer) {
                KeyguardFaceUnlockView.this.handleBouncerUserVisibilityChanged();
            }

            @Override
            public void onScreenTurnedOn() {
                KeyguardFaceUnlockView.this.handleBouncerUserVisibilityChanged();
            }

            @Override
            public void onScreenTurnedOff(int why) {
                KeyguardFaceUnlockView.this.handleBouncerUserVisibilityChanged();
            }

            @Override
            public void onEmergencyCallAction() {
                if (KeyguardFaceUnlockView.this.mBiometricUnlock != null) {
                    KeyguardFaceUnlockView.this.mBiometricUnlock.stop();
                }
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initializeBiometricUnlockView();
        this.mSecurityMessageDisplay = new KeyguardMessageArea.Helper(this);
        this.mEcaView = findViewById(R.id.keyguard_selector_fade_container);
        View bouncerFrameView = findViewById(R.id.keyguard_bouncer_frame);
        if (bouncerFrameView != null) {
            this.mBouncerFrame = bouncerFrameView.getBackground();
        }
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mKeyguardSecurityCallback = callback;
        ((FaceUnlock) this.mBiometricUnlock).setKeyguardCallback(callback);
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    @Override
    public void onDetachedFromWindow() {
        if (this.mBiometricUnlock != null) {
            this.mBiometricUnlock.stop();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateCallback);
        if (this.mWatchingRotation) {
            try {
                this.mWindowManager.removeRotationWatcher(this.mRotationWatcher);
                this.mWatchingRotation = false;
            } catch (RemoteException e) {
                Log.e("FULKeyguardFaceUnlockView", "Remote exception when removing rotation watcher");
            }
        }
    }

    @Override
    public void onPause() {
        if (this.mBiometricUnlock != null) {
            this.mBiometricUnlock.stop();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateCallback);
        if (this.mWatchingRotation) {
            try {
                this.mWindowManager.removeRotationWatcher(this.mRotationWatcher);
                this.mWatchingRotation = false;
            } catch (RemoteException e) {
                Log.e("FULKeyguardFaceUnlockView", "Remote exception when removing rotation watcher");
            }
        }
    }

    @Override
    public void onResume(int reason) {
        synchronized (this.mIsBouncerVisibleToUserLock) {
            this.mIsBouncerVisibleToUser = isBouncerVisibleToUser();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateCallback);
        if (!this.mWatchingRotation) {
            try {
                this.mLastRotation = this.mWindowManager.watchRotation(this.mRotationWatcher);
                this.mWatchingRotation = true;
            } catch (RemoteException e) {
                Log.e("FULKeyguardFaceUnlockView", "Remote exception when adding rotation watcher");
            }
        }
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.mBiometricUnlock.initializeView(this.mFaceUnlockAreaView);
    }

    private void initializeBiometricUnlockView() {
        this.mFaceUnlockAreaView = findViewById(R.id.face_unlock_area_view);
        if (this.mFaceUnlockAreaView != null) {
            this.mBiometricUnlock = new FaceUnlock(this.mContext);
            this.mCancelButton = (ImageButton) findViewById(R.id.face_unlock_cancel_button);
            this.mCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    KeyguardFaceUnlockView.this.mBiometricUnlock.stopAndShowBackup();
                }
            });
            return;
        }
        Log.w("FULKeyguardFaceUnlockView", "Couldn't find biometric unlock view");
    }

    public void maybeStartBiometricUnlock() {
        boolean isBouncerVisibleToUser;
        if (this.mBiometricUnlock != null) {
            KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
            boolean backupIsTimedOut = monitor.getFailedUnlockAttempts() >= 5;
            synchronized (this.mIsBouncerVisibleToUserLock) {
                isBouncerVisibleToUser = this.mIsBouncerVisibleToUser;
            }
            if (!isBouncerVisibleToUser) {
                this.mBiometricUnlock.stop();
                return;
            }
            if (monitor.getPhoneState() == 0 && monitor.isAlternateUnlockEnabled() && !monitor.getMaxBiometricUnlockAttemptsReached() && !backupIsTimedOut) {
                this.mBiometricUnlock.start();
            } else {
                this.mBiometricUnlock.stopAndShowBackup();
            }
        }
    }

    private boolean isBouncerVisibleToUser() {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        return updateMonitor.isKeyguardBouncer() && updateMonitor.isKeyguardVisible() && updateMonitor.isScreenOn();
    }

    public void handleBouncerUserVisibilityChanged() {
        boolean wasBouncerVisibleToUser;
        synchronized (this.mIsBouncerVisibleToUserLock) {
            wasBouncerVisibleToUser = this.mIsBouncerVisibleToUser;
            this.mIsBouncerVisibleToUser = isBouncerVisibleToUser();
        }
        if (this.mBiometricUnlock != null) {
            if (wasBouncerVisibleToUser && !this.mIsBouncerVisibleToUser) {
                this.mBiometricUnlock.stop();
            } else if (!wasBouncerVisibleToUser && this.mIsBouncerVisibleToUser) {
                maybeStartBiometricUnlock();
            }
        }
    }

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public void showBouncer(int duration) {
        KeyguardSecurityViewHelper.showBouncer(this.mSecurityMessageDisplay, this.mEcaView, this.mBouncerFrame, duration);
    }

    @Override
    public void hideBouncer(int duration) {
        KeyguardSecurityViewHelper.hideBouncer(this.mSecurityMessageDisplay, this.mEcaView, this.mBouncerFrame, duration);
    }

    @Override
    public void startAppearAnimation() {
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }
}
