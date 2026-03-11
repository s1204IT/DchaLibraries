package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
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
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.keyguard.KeyguardMessageArea;
import java.util.List;

public class KeyguardPatternView extends LinearLayout implements AppearAnimationCreator<LockPatternView.CellState>, KeyguardSecurityView {
    private final AppearAnimationUtils mAppearAnimationUtils;
    private Drawable mBouncerFrame;
    private KeyguardSecurityCallback mCallback;
    private Runnable mCancelPatternRunnable;
    private CountDownTimer mCountdownTimer;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private int mDisappearYTranslation;
    private View mEcaView;
    private KeyguardMessageArea mHelpMessage;
    private ViewGroup mKeyguardBouncerFrame;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private long mLastPokeTime;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternView mLockPatternView;
    private SecurityMessageDisplay mSecurityMessageDisplay;
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
        this.mAppearAnimationUtils = new AppearAnimationUtils(context, 220L, 1.5f, 2.0f, AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.linear_out_slow_in));
        this.mDisappearAnimationUtils = new DisappearAnimationUtils(context, 125L, 1.2f, 0.8f, AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_linear_in));
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R.dimen.disappear_y_translation);
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
        super.onFinishInflate();
        this.mLockPatternUtils = this.mLockPatternUtils == null ? new LockPatternUtils(this.mContext) : this.mLockPatternUtils;
        this.mLockPatternView = findViewById(R.id.lockPatternView);
        this.mLockPatternView.setSaveEnabled(false);
        this.mLockPatternView.setFocusable(false);
        this.mLockPatternView.setOnPatternListener(new UnlockPatternListener());
        this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled());
        this.mLockPatternView.setTactileFeedbackEnabled(this.mLockPatternUtils.isTactileFeedbackEnabled());
        setFocusableInTouchMode(true);
        this.mSecurityMessageDisplay = new KeyguardMessageArea.Helper(this);
        this.mEcaView = findViewById(R.id.keyguard_selector_fade_container);
        View bouncerFrameView = findViewById(R.id.keyguard_bouncer_frame);
        if (bouncerFrameView != null) {
            this.mBouncerFrame = bouncerFrameView.getBackground();
        }
        this.mKeyguardBouncerFrame = (ViewGroup) findViewById(R.id.keyguard_bouncer_frame);
        this.mHelpMessage = (KeyguardMessageArea) findViewById(R.id.keyguard_message_area);
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
        boolean result2 = this.mLockPatternView.dispatchTouchEvent(ev) || result;
        ev.offsetLocation(-this.mTempRect.left, -this.mTempRect.top);
        return result2;
    }

    public void reset() {
        this.mLockPatternView.enableInput();
        this.mLockPatternView.setEnabled(true);
        this.mLockPatternView.clearPattern();
        long deadline = this.mLockPatternUtils.getLockoutAttemptDeadline();
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else {
            displayDefaultSecurityMessage();
        }
    }

    public void displayDefaultSecurityMessage() {
        if (this.mKeyguardUpdateMonitor.getMaxBiometricUnlockAttemptsReached()) {
            this.mSecurityMessageDisplay.setMessage(R.string.faceunlock_multiple_failures, true);
        } else {
            this.mSecurityMessageDisplay.setMessage(R.string.kg_pattern_instructions, false);
        }
    }

    @Override
    public void showUsabilityHint() {
    }

    private class UnlockPatternListener implements LockPatternView.OnPatternListener {
        private UnlockPatternListener() {
        }

        public void onPatternStart() {
            KeyguardPatternView.this.mLockPatternView.removeCallbacks(KeyguardPatternView.this.mCancelPatternRunnable);
        }

        public void onPatternCleared() {
        }

        public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
            KeyguardPatternView.this.mCallback.userActivity();
        }

        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            if (KeyguardPatternView.this.mLockPatternUtils.checkPattern(pattern)) {
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(true);
                KeyguardPatternView.this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                KeyguardPatternView.this.mCallback.dismiss(true);
                return;
            }
            if (pattern.size() > 2) {
                KeyguardPatternView.this.mCallback.userActivity();
            }
            KeyguardPatternView.this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            boolean registeredAttempt = pattern.size() >= 4;
            if (registeredAttempt) {
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(false);
            }
            int attempts = KeyguardPatternView.this.mKeyguardUpdateMonitor.getFailedUnlockAttempts();
            if (!registeredAttempt || attempts % 5 != 0) {
                KeyguardPatternView.this.mSecurityMessageDisplay.setMessage(R.string.kg_wrong_pattern, true);
                KeyguardPatternView.this.mLockPatternView.postDelayed(KeyguardPatternView.this.mCancelPatternRunnable, 2000L);
            } else {
                long deadline = KeyguardPatternView.this.mLockPatternUtils.setLockoutAttemptDeadline();
                KeyguardPatternView.this.handleAttemptLockout(deadline);
            }
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
                KeyguardPatternView.this.mSecurityMessageDisplay.setMessage(R.string.kg_too_many_failed_attempts_countdown, true, Integer.valueOf(secondsRemaining));
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
    }

    @Override
    public void onResume(int reason) {
        reset();
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
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        animate().setDuration(500L).setInterpolator(this.mAppearAnimationUtils.getInterpolator()).translationY(0.0f);
        this.mAppearAnimationUtils.startAnimation((Object[][]) this.mLockPatternView.getCellStates(), new Runnable() {
            @Override
            public void run() {
                KeyguardPatternView.this.enableClipping(true);
            }
        }, (AppearAnimationCreator) this);
        if (!TextUtils.isEmpty(this.mHelpMessage.getText())) {
            this.mAppearAnimationUtils.createAnimation((View) this.mHelpMessage, 0L, 220L, this.mAppearAnimationUtils.getStartTranslation(), true, this.mAppearAnimationUtils.getInterpolator(), (Runnable) null);
        }
    }

    @Override
    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        this.mLockPatternView.clearPattern();
        enableClipping(false);
        setTranslationY(0.0f);
        animate().setDuration(300L).setInterpolator(this.mDisappearAnimationUtils.getInterpolator()).translationY(-this.mDisappearAnimationUtils.getStartTranslation());
        this.mDisappearAnimationUtils.startAnimation((Object[][]) this.mLockPatternView.getCellStates(), new Runnable() {
            @Override
            public void run() {
                KeyguardPatternView.this.enableClipping(true);
                if (finishRunnable != null) {
                    finishRunnable.run();
                }
            }
        }, (AppearAnimationCreator) this);
        if (!TextUtils.isEmpty(this.mHelpMessage.getText())) {
            this.mDisappearAnimationUtils.createAnimation((View) this.mHelpMessage, 0L, 200L, (-this.mDisappearAnimationUtils.getStartTranslation()) * 3.0f, false, this.mDisappearAnimationUtils.getInterpolator(), (Runnable) null);
            return true;
        }
        return true;
    }

    public void enableClipping(boolean enable) {
        setClipChildren(enable);
        this.mKeyguardBouncerFrame.setClipToPadding(enable);
        this.mKeyguardBouncerFrame.setClipChildren(enable);
    }

    @Override
    public void createAnimation(final LockPatternView.CellState animatedCell, long delay, long duration, float translationY, final boolean appearing, Interpolator interpolator, final Runnable finishListener) {
        if (appearing) {
            animatedCell.scale = 0.0f;
            animatedCell.alpha = 1.0f;
        }
        animatedCell.translateY = appearing ? translationY : 0.0f;
        float[] fArr = new float[2];
        fArr[0] = animatedCell.translateY;
        fArr[1] = appearing ? 0.0f : translationY;
        ValueAnimator animator = ValueAnimator.ofFloat(fArr);
        animator.setInterpolator(interpolator);
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = animation.getAnimatedFraction();
                if (appearing) {
                    animatedCell.scale = animatedFraction;
                } else {
                    animatedCell.alpha = 1.0f - animatedFraction;
                }
                animatedCell.translateY = ((Float) animation.getAnimatedValue()).floatValue();
                KeyguardPatternView.this.mLockPatternView.invalidate();
            }
        });
        if (finishListener != null) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    finishListener.run();
                }
            });
            this.mAppearAnimationUtils.createAnimation(this.mEcaView, delay, duration, translationY, appearing, interpolator, (Runnable) null);
        }
        animator.start();
        this.mLockPatternView.invalidate();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
