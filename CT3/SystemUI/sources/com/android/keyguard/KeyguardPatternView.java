package com.android.keyguard;

import android.R;
import android.content.Context;
import android.graphics.Rect;
import android.media.AudioSystem;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.keyguard.EmergencyButton;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import java.util.List;

public class KeyguardPatternView extends LinearLayout implements KeyguardSecurityView, AppearAnimationCreator<LockPatternView.CellState>, EmergencyButton.EmergencyButtonCallback {
    private final AppearAnimationUtils mAppearAnimationUtils;
    private KeyguardSecurityCallback mCallback;
    private Runnable mCancelPatternRunnable;
    private ViewGroup mContainer;
    private CountDownTimer mCountdownTimer;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private int mDisappearYTranslation;
    private View mEcaView;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private long mLastPokeTime;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternView mLockPatternView;
    private AsyncTask<?, ?, ?> mPendingLockCheck;
    private KeyguardMessageArea mSecurityMessageDisplay;
    private Rect mTempRect;

    public KeyguardPatternView(Context context) {
        this(context, null);
    }

    public KeyguardPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCountdownTimer = null;
        this.mLastPokeTime = -7000L;
        this.mCancelPatternRunnable = new Runnable() {
            @Override
            public void run() {
                KeyguardPatternView.this.mLockPatternView.clearPattern();
            }
        };
        this.mTempRect = new Rect();
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mAppearAnimationUtils = new AppearAnimationUtils(context, 220L, 1.5f, 2.0f, AnimationUtils.loadInterpolator(this.mContext, R.interpolator.linear_out_slow_in));
        this.mDisappearAnimationUtils = new DisappearAnimationUtils(context, 125L, 1.2f, 0.6f, AnimationUtils.loadInterpolator(this.mContext, R.interpolator.fast_out_linear_in));
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    @Override
    protected void onFinishInflate() {
        UnlockPatternListener unlockPatternListener = null;
        super.onFinishInflate();
        this.mLockPatternUtils = this.mLockPatternUtils == null ? new LockPatternUtils(this.mContext) : this.mLockPatternUtils;
        this.mLockPatternView = findViewById(R$id.lockPatternView);
        this.mLockPatternView.setSaveEnabled(false);
        this.mLockPatternView.setOnPatternListener(new UnlockPatternListener(this, unlockPatternListener));
        this.mLockPatternView.setTactileFeedbackEnabled(this.mLockPatternUtils.isTactileFeedbackEnabled());
        this.mSecurityMessageDisplay = (KeyguardMessageArea) KeyguardMessageArea.findSecurityMessageDisplay(this);
        this.mEcaView = findViewById(R$id.keyguard_selector_fade_container);
        this.mContainer = (ViewGroup) findViewById(R$id.container);
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

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        long elapsed = SystemClock.elapsedRealtime() - this.mLastPokeTime;
        if (result && elapsed > 6900) {
            this.mLastPokeTime = SystemClock.elapsedRealtime();
        }
        this.mTempRect.set(0, 0, 0, 0);
        offsetRectIntoDescendantCoords(this.mLockPatternView, this.mTempRect);
        ev.offsetLocation(this.mTempRect.left, this.mTempRect.top);
        if (this.mLockPatternView.dispatchTouchEvent(ev)) {
            result = true;
        }
        ev.offsetLocation(-this.mTempRect.left, -this.mTempRect.top);
        return result;
    }

    public void reset() {
        this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled(KeyguardUpdateMonitor.getCurrentUser()));
        this.mLockPatternView.enableInput();
        this.mLockPatternView.setEnabled(true);
        this.mLockPatternView.clearPattern();
        long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else {
            displayDefaultSecurityMessage();
        }
    }

    public void displayDefaultSecurityMessage() {
        if (this.mKeyguardUpdateMonitor.getMaxBiometricUnlockAttemptsReached()) {
            LockPatternUtils lockPatternUtils = this.mLockPatternUtils;
            KeyguardUpdateMonitor keyguardUpdateMonitor = this.mKeyguardUpdateMonitor;
            if (!lockPatternUtils.usingVoiceWeak(KeyguardUpdateMonitor.getCurrentUser())) {
                return;
            }
            this.mSecurityMessageDisplay.setMessage(R$string.voiceunlock_multiple_failures, true);
            this.mKeyguardUpdateMonitor.setAlternateUnlockEnabled(false);
            return;
        }
        this.mSecurityMessageDisplay.setMessage(R$string.kg_pattern_instructions, false);
    }

    private class UnlockPatternListener implements LockPatternView.OnPatternListener {
        UnlockPatternListener(KeyguardPatternView this$0, UnlockPatternListener unlockPatternListener) {
            this();
        }

        private UnlockPatternListener() {
        }

        public void onPatternStart() {
            KeyguardPatternView.this.mLockPatternView.removeCallbacks(KeyguardPatternView.this.mCancelPatternRunnable);
            KeyguardPatternView.this.mSecurityMessageDisplay.setMessage((CharSequence) "", false);
        }

        public void onPatternCleared() {
        }

        public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
            KeyguardPatternView.this.mCallback.userActivity();
        }

        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            KeyguardPatternView.this.mLockPatternView.disableInput();
            if (KeyguardPatternView.this.mPendingLockCheck != null) {
                KeyguardPatternView.this.mPendingLockCheck.cancel(false);
            }
            final int userId = KeyguardUpdateMonitor.getCurrentUser();
            if (pattern.size() < 4) {
                KeyguardPatternView.this.mLockPatternView.enableInput();
                onPatternChecked(userId, false, 0, false);
                return;
            }
            KeyguardPatternView.this.mPendingLockCheck = LockPatternChecker.checkPattern(KeyguardPatternView.this.mLockPatternUtils, pattern, userId, new LockPatternChecker.OnCheckCallback() {
                public void onChecked(boolean matched, int timeoutMs) {
                    KeyguardPatternView.this.mLockPatternView.enableInput();
                    KeyguardPatternView.this.mPendingLockCheck = null;
                    UnlockPatternListener.this.onPatternChecked(userId, matched, timeoutMs, true);
                }
            });
            if (pattern.size() <= 2) {
                return;
            }
            KeyguardPatternView.this.mCallback.userActivity();
        }

        public void onPatternChecked(int userId, boolean matched, int timeoutMs, boolean isValidPattern) {
            boolean dismissKeyguard = KeyguardUpdateMonitor.getCurrentUser() == userId;
            if (matched) {
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(userId, true, 0);
                if (!dismissKeyguard) {
                    return;
                }
                KeyguardPatternView.this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                KeyguardPatternView.this.mCallback.dismiss(true);
                return;
            }
            KeyguardPatternView.this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            if (isValidPattern) {
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(userId, false, timeoutMs);
                if (timeoutMs > 0) {
                    long deadline = KeyguardPatternView.this.mLockPatternUtils.setLockoutAttemptDeadline(userId, timeoutMs);
                    KeyguardPatternView.this.handleAttemptLockout(deadline);
                }
            }
            if (timeoutMs != 0) {
                return;
            }
            KeyguardPatternView.this.mSecurityMessageDisplay.setMessage(R$string.kg_wrong_pattern, true);
            KeyguardPatternView.this.mLockPatternView.postDelayed(KeyguardPatternView.this.mCancelPatternRunnable, 2000L);
        }
    }

    public void handleAttemptLockout(long elapsedRealtimeDeadline) {
        this.mLockPatternView.clearPattern();
        this.mLockPatternView.setEnabled(false);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        this.mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                KeyguardPatternView.this.mSecurityMessageDisplay.setMessage(R$string.kg_too_many_failed_attempts_countdown, true, Integer.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                KeyguardPatternView.this.mLockPatternView.setEnabled(true);
                KeyguardPatternView.this.displayDefaultSecurityMessage();
            }
        }.start();
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
        if (this.mCountdownTimer != null) {
            this.mCountdownTimer.cancel();
            this.mCountdownTimer = null;
        }
        if (this.mPendingLockCheck == null) {
            return;
        }
        this.mPendingLockCheck.cancel(false);
        this.mPendingLockCheck = null;
    }

    @Override
    public void onResume(int reason) {
        reset();
        boolean mediaPlaying = AudioSystem.isStreamActive(3, 0);
        if (!this.mLockPatternUtils.usingVoiceWeak() || !mediaPlaying) {
            return;
        }
        this.mSecurityMessageDisplay.setMessage(R$string.voice_unlock_media_playing, true);
    }

    @Override
    public void showPromptReason(int reason) {
        switch (reason) {
            case 0:
                break;
            case 1:
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_restart_pattern, true);
                break;
            case 2:
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_timeout_pattern, true);
                break;
            case 3:
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_device_admin, true);
                break;
            case 4:
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_user_request, true);
                break;
            default:
                this.mSecurityMessageDisplay.setMessage(R$string.kg_prompt_reason_timeout_pattern, true);
                break;
        }
    }

    @Override
    public void showMessage(String message, int color) {
        this.mSecurityMessageDisplay.setNextMessageColor(color);
        this.mSecurityMessageDisplay.setMessage((CharSequence) message, true);
    }

    @Override
    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        AppearAnimationUtils.startTranslationYAnimation(this, 0L, 500L, 0.0f, this.mAppearAnimationUtils.getInterpolator());
        this.mAppearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() {
            @Override
            public void run() {
                KeyguardPatternView.this.enableClipping(true);
            }
        }, this);
        if (TextUtils.isEmpty(this.mSecurityMessageDisplay.getText())) {
            return;
        }
        this.mAppearAnimationUtils.createAnimation((View) this.mSecurityMessageDisplay, 0L, 220L, this.mAppearAnimationUtils.getStartTranslation(), true, this.mAppearAnimationUtils.getInterpolator(), (Runnable) null);
    }

    @Override
    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        this.mLockPatternView.clearPattern();
        enableClipping(false);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0L, 300L, -this.mDisappearAnimationUtils.getStartTranslation(), this.mDisappearAnimationUtils.getInterpolator());
        this.mDisappearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() {
            @Override
            public void run() {
                KeyguardPatternView.this.enableClipping(true);
                if (finishRunnable == null) {
                    return;
                }
                finishRunnable.run();
            }
        }, this);
        if (!TextUtils.isEmpty(this.mSecurityMessageDisplay.getText())) {
            this.mDisappearAnimationUtils.createAnimation((View) this.mSecurityMessageDisplay, 0L, 200L, (-this.mDisappearAnimationUtils.getStartTranslation()) * 3.0f, false, this.mDisappearAnimationUtils.getInterpolator(), (Runnable) null);
            return true;
        }
        return true;
    }

    public void enableClipping(boolean enable) {
        setClipChildren(enable);
        this.mContainer.setClipToPadding(enable);
        this.mContainer.setClipChildren(enable);
    }

    @Override
    public void createAnimation(LockPatternView.CellState animatedCell, long delay, long duration, float translationY, boolean appearing, Interpolator interpolator, Runnable finishListener) {
        this.mLockPatternView.startCellStateAnimation(animatedCell, 1.0f, appearing ? 1.0f : 0.0f, appearing ? translationY : 0.0f, appearing ? 0.0f : translationY, appearing ? 0.0f : 1.0f, 1.0f, delay, duration, interpolator, finishListener);
        if (finishListener == null) {
            return;
        }
        this.mAppearAnimationUtils.createAnimation(this.mEcaView, delay, duration, translationY, appearing, interpolator, (Runnable) null);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
