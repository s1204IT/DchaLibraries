package com.android.keyguard;

import android.content.Context;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyButton;

public abstract class KeyguardAbsKeyInputView extends LinearLayout implements KeyguardSecurityView, EmergencyButton.EmergencyButtonCallback {
    protected KeyguardSecurityCallback mCallback;
    private boolean mDismissing;
    protected View mEcaView;
    protected boolean mEnableHaptics;
    protected LockPatternUtils mLockPatternUtils;
    protected AsyncTask<?, ?, ?> mPendingLockCheck;
    protected SecurityMessageDisplay mSecurityMessageDisplay;

    protected abstract String getPasswordText();

    protected abstract int getPasswordTextViewId();

    protected abstract int getPromtReasonStringRes(int i);

    protected abstract void resetPasswordText(boolean z, boolean z2);

    protected abstract void resetState();

    protected abstract void setPasswordEntryEnabled(boolean z);

    protected abstract void setPasswordEntryInputEnabled(boolean z);

    public KeyguardAbsKeyInputView(Context context) {
        this(context, null);
    }

    public KeyguardAbsKeyInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
        this.mEnableHaptics = this.mLockPatternUtils.isTactileFeedbackEnabled();
    }

    public void reset() {
        this.mDismissing = false;
        resetPasswordText(false, false);
        long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        if (shouldLockout(deadline)) {
            handleAttemptLockout(deadline);
        } else {
            resetState();
        }
    }

    protected boolean shouldLockout(long deadline) {
        return deadline != 0;
    }

    @Override
    protected void onFinishInflate() {
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
        this.mEcaView = findViewById(R$id.keyguard_selector_fade_container);
        EmergencyButton button = (EmergencyButton) findViewById(R$id.emergency_call_button);
        if (button == null) {
            return;
        }
        button.setCallback(this);
    }

    @Override
    public void onEmergencyButtonClickedWhenInCall() {
        this.mCallback.reset();
    }

    protected int getWrongPasswordStringId() {
        return R$string.kg_wrong_password;
    }

    protected void verifyPasswordAndUnlock() {
        if (this.mDismissing) {
            return;
        }
        String entry = getPasswordText();
        setPasswordEntryInputEnabled(false);
        if (this.mPendingLockCheck != null) {
            this.mPendingLockCheck.cancel(false);
        }
        final int userId = KeyguardUpdateMonitor.getCurrentUser();
        if (entry.length() <= 3) {
            setPasswordEntryInputEnabled(true);
            onPasswordChecked(userId, false, 0, false);
        } else {
            this.mPendingLockCheck = LockPatternChecker.checkPassword(this.mLockPatternUtils, entry, userId, new LockPatternChecker.OnCheckCallback() {
                public void onChecked(boolean matched, int timeoutMs) {
                    KeyguardAbsKeyInputView.this.setPasswordEntryInputEnabled(true);
                    KeyguardAbsKeyInputView.this.mPendingLockCheck = null;
                    KeyguardAbsKeyInputView.this.onPasswordChecked(userId, matched, timeoutMs, true);
                }
            });
        }
    }

    public void onPasswordChecked(int userId, boolean matched, int timeoutMs, boolean isValidPassword) {
        boolean dismissKeyguard = KeyguardUpdateMonitor.getCurrentUser() == userId;
        if (matched) {
            this.mCallback.reportUnlockAttempt(userId, true, 0);
            if (dismissKeyguard) {
                this.mDismissing = true;
                this.mCallback.dismiss(true);
            }
        } else {
            if (isValidPassword) {
                this.mCallback.reportUnlockAttempt(userId, false, timeoutMs);
                if (timeoutMs > 0) {
                    long deadline = this.mLockPatternUtils.setLockoutAttemptDeadline(userId, timeoutMs);
                    handleAttemptLockout(deadline);
                }
            }
            if (timeoutMs == 0) {
                this.mSecurityMessageDisplay.setMessage(getWrongPasswordStringId(), true);
            }
        }
        resetPasswordText(true, matched ? false : true);
    }

    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        setPasswordEntryEnabled(false);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                KeyguardAbsKeyInputView.this.mSecurityMessageDisplay.setMessage(R$string.kg_too_many_failed_attempts_countdown, true, Integer.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                KeyguardAbsKeyInputView.this.mSecurityMessageDisplay.setMessage((CharSequence) "", false);
                KeyguardAbsKeyInputView.this.resetState();
            }
        }.start();
    }

    protected void onUserInput() {
        if (this.mCallback != null) {
            this.mCallback.userActivity();
        }
        this.mSecurityMessageDisplay.setMessage((CharSequence) "", false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        onUserInput();
        return false;
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
        if (this.mPendingLockCheck == null) {
            return;
        }
        this.mPendingLockCheck.cancel(false);
        this.mPendingLockCheck = null;
    }

    @Override
    public void onResume(int reason) {
        reset();
    }

    @Override
    public void showPromptReason(int reason) {
        int promtReasonStringRes;
        if (reason == 0 || (promtReasonStringRes = getPromtReasonStringRes(reason)) == 0) {
            return;
        }
        this.mSecurityMessageDisplay.setMessage(promtReasonStringRes, true);
    }

    @Override
    public void showMessage(String message, int color) {
        this.mSecurityMessageDisplay.setNextMessageColor(color);
        this.mSecurityMessageDisplay.setMessage((CharSequence) message, true);
    }

    public void doHapticKeyClick() {
        if (!this.mEnableHaptics) {
            return;
        }
        performHapticFeedback(1, 3);
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }
}
