package com.android.keyguard;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardMessageArea;

public abstract class KeyguardAbsKeyInputView extends LinearLayout implements KeyguardSecurityView {
    private Drawable mBouncerFrame;
    protected KeyguardSecurityCallback mCallback;
    protected View mEcaView;
    protected boolean mEnableHaptics;
    protected LockPatternUtils mLockPatternUtils;
    protected SecurityMessageDisplay mSecurityMessageDisplay;

    protected abstract String getPasswordText();

    protected abstract int getPasswordTextViewId();

    protected abstract void resetPasswordText(boolean z);

    protected abstract void resetState();

    protected abstract void setPasswordEntryEnabled(boolean z);

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
        resetPasswordText(false);
        long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline();
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
        this.mSecurityMessageDisplay = new KeyguardMessageArea.Helper(this);
        this.mEcaView = findViewById(R.id.keyguard_selector_fade_container);
        View bouncerFrameView = findViewById(R.id.keyguard_bouncer_frame);
        if (bouncerFrameView != null) {
            this.mBouncerFrame = bouncerFrameView.getBackground();
        }
    }

    protected int getWrongPasswordStringId() {
        return R.string.kg_wrong_password;
    }

    protected void verifyPasswordAndUnlock() {
        String entry = getPasswordText();
        if (this.mLockPatternUtils.checkPassword(entry)) {
            this.mCallback.reportUnlockAttempt(true);
            this.mCallback.dismiss(true);
        } else {
            if (entry.length() > 3) {
                this.mCallback.reportUnlockAttempt(false);
                int attempts = KeyguardUpdateMonitor.getInstance(this.mContext).getFailedUnlockAttempts();
                if (attempts % 5 == 0) {
                    long deadline = this.mLockPatternUtils.setLockoutAttemptDeadline();
                    handleAttemptLockout(deadline);
                }
            }
            this.mSecurityMessageDisplay.setMessage(getWrongPasswordStringId(), true);
        }
        resetPasswordText(true);
    }

    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        setPasswordEntryEnabled(false);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                KeyguardAbsKeyInputView.this.mSecurityMessageDisplay.setMessage(R.string.kg_too_many_failed_attempts_countdown, true, Integer.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                KeyguardAbsKeyInputView.this.mSecurityMessageDisplay.setMessage((CharSequence) "", false);
                KeyguardAbsKeyInputView.this.resetState();
            }
        }.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        this.mCallback.userActivity();
        return false;
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume(int reason) {
        reset();
    }

    public void doHapticKeyClick() {
        if (this.mEnableHaptics) {
            performHapticFeedback(1, 3);
        }
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
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }
}
