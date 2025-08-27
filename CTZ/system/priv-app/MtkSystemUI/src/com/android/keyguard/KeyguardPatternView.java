package com.android.keyguard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
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
import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.keyguard.EmergencyButton;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import java.util.List;

/* loaded from: classes.dex */
public class KeyguardPatternView extends LinearLayout implements EmergencyButton.EmergencyButtonCallback, KeyguardSecurityView, AppearAnimationCreator<LockPatternView.CellState> {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private final AppearAnimationUtils mAppearAnimationUtils;
    private KeyguardSecurityCallback mCallback;
    private Runnable mCancelPatternRunnable;
    private ViewGroup mContainer;
    private CountDownTimer mCountdownTimer;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtilsLocked;
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

    public KeyguardPatternView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCountdownTimer = null;
        this.mLastPokeTime = -7000L;
        this.mCancelPatternRunnable = new Runnable() { // from class: com.android.keyguard.KeyguardPatternView.1
            @Override // java.lang.Runnable
            public void run() {
                KeyguardPatternView.this.mLockPatternView.clearPattern();
            }
        };
        this.mTempRect = new Rect();
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mAppearAnimationUtils = new AppearAnimationUtils(context, 220L, 1.5f, 2.0f, AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.linear_out_slow_in));
        this.mDisappearAnimationUtils = new DisappearAnimationUtils(context, 125L, 1.2f, 0.6f, AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_linear_in));
        this.mDisappearAnimationUtilsLocked = new DisappearAnimationUtils(context, 187L, 1.2f, 0.6f, AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_linear_in));
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(com.android.systemui.R.dimen.disappear_y_translation);
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void setKeyguardCallback(KeyguardSecurityCallback keyguardSecurityCallback) {
        this.mCallback = keyguardSecurityCallback;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void setLockPatternUtils(LockPatternUtils lockPatternUtils) {
        this.mLockPatternUtils = lockPatternUtils;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = this.mLockPatternUtils == null ? new LockPatternUtils(this.mContext) : this.mLockPatternUtils;
        this.mLockPatternView = findViewById(com.android.systemui.R.id.lockPatternView);
        this.mLockPatternView.setSaveEnabled(false);
        this.mLockPatternView.setOnPatternListener(new UnlockPatternListener());
        this.mLockPatternView.setTactileFeedbackEnabled(this.mLockPatternUtils.isTactileFeedbackEnabled());
        this.mSecurityMessageDisplay = (KeyguardMessageArea) KeyguardMessageArea.findSecurityMessageDisplay(this);
        this.mEcaView = findViewById(com.android.systemui.R.id.keyguard_selector_fade_container);
        this.mContainer = (ViewGroup) findViewById(com.android.systemui.R.id.container);
        EmergencyButton emergencyButton = (EmergencyButton) findViewById(com.android.systemui.R.id.emergency_call_button);
        if (emergencyButton != null) {
            emergencyButton.setCallback(this);
        }
        View viewFindViewById = findViewById(com.android.systemui.R.id.cancel_button);
        if (viewFindViewById != null) {
            viewFindViewById.setOnClickListener(new View.OnClickListener() { // from class: com.android.keyguard.-$$Lambda$KeyguardPatternView$N-2kmt4uZ3ZvQBB4SmVDuZJ_Wqw
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.mCallback.reset();
                }
            });
        }
    }

    @Override // com.android.keyguard.EmergencyButton.EmergencyButtonCallback
    public void onEmergencyButtonClickedWhenInCall() {
        this.mCallback.reset();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
        long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mLastPokeTime;
        if (zOnTouchEvent && jElapsedRealtime > 6900) {
            this.mLastPokeTime = SystemClock.elapsedRealtime();
        }
        this.mTempRect.set(0, 0, 0, 0);
        offsetRectIntoDescendantCoords(this.mLockPatternView, this.mTempRect);
        motionEvent.offsetLocation(this.mTempRect.left, this.mTempRect.top);
        boolean z = this.mLockPatternView.dispatchTouchEvent(motionEvent) || zOnTouchEvent;
        motionEvent.offsetLocation(-this.mTempRect.left, -this.mTempRect.top);
        return z;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void reset() {
        this.mLockPatternView.setInStealthMode(!this.mLockPatternUtils.isVisiblePatternEnabled(KeyguardUpdateMonitor.getCurrentUser()));
        this.mLockPatternView.enableInput();
        this.mLockPatternView.setEnabled(true);
        this.mLockPatternView.clearPattern();
        long lockoutAttemptDeadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        if (lockoutAttemptDeadline != 0) {
            handleAttemptLockout(lockoutAttemptDeadline);
        } else {
            displayDefaultSecurityMessage();
        }
    }

    private void displayDefaultSecurityMessage() {
        this.mSecurityMessageDisplay.setMessage("");
    }

    private class UnlockPatternListener implements LockPatternView.OnPatternListener {
        private UnlockPatternListener() {
        }

        public void onPatternStart() {
            KeyguardPatternView.this.mLockPatternView.removeCallbacks(KeyguardPatternView.this.mCancelPatternRunnable);
            KeyguardPatternView.this.mSecurityMessageDisplay.setMessage("");
        }

        public void onPatternCleared() {
        }

        public void onPatternCellAdded(List<LockPatternView.Cell> list) {
            KeyguardPatternView.this.mCallback.userActivity();
        }

        public void onPatternDetected(List<LockPatternView.Cell> list) throws Resources.NotFoundException {
            KeyguardPatternView.this.mLockPatternView.disableInput();
            if (KeyguardPatternView.this.mPendingLockCheck != null) {
                KeyguardPatternView.this.mPendingLockCheck.cancel(false);
            }
            final int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            if (list.size() < 4) {
                KeyguardPatternView.this.mLockPatternView.enableInput();
                onPatternChecked(currentUser, false, 0, false);
                return;
            }
            if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionStart(3);
                LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionStart(4);
            }
            KeyguardPatternView.this.mPendingLockCheck = LockPatternChecker.checkPattern(KeyguardPatternView.this.mLockPatternUtils, list, currentUser, new LockPatternChecker.OnCheckCallback() { // from class: com.android.keyguard.KeyguardPatternView.UnlockPatternListener.1
                public void onEarlyMatched() throws Resources.NotFoundException {
                    if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionEnd(3);
                    }
                    UnlockPatternListener.this.onPatternChecked(currentUser, true, 0, true);
                }

                public void onChecked(boolean z, int i) throws Resources.NotFoundException {
                    if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionEnd(4);
                    }
                    KeyguardPatternView.this.mLockPatternView.enableInput();
                    KeyguardPatternView.this.mPendingLockCheck = null;
                    if (!z) {
                        UnlockPatternListener.this.onPatternChecked(currentUser, false, i, true);
                    }
                }

                public void onCancelled() {
                    if (LatencyTracker.isEnabled(KeyguardPatternView.this.mContext)) {
                        LatencyTracker.getInstance(KeyguardPatternView.this.mContext).onActionEnd(4);
                    }
                }
            });
            if (list.size() > 2) {
                KeyguardPatternView.this.mCallback.userActivity();
            }
        }

        private void onPatternChecked(int i, boolean z, int i2, boolean z2) throws Resources.NotFoundException {
            boolean z3 = KeyguardUpdateMonitor.getCurrentUser() == i;
            if (z) {
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(i, true, 0);
                if (z3) {
                    KeyguardPatternView.this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                    KeyguardPatternView.this.mCallback.dismiss(true, i);
                    return;
                }
                return;
            }
            KeyguardPatternView.this.mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            if (z2) {
                KeyguardPatternView.this.mCallback.reportUnlockAttempt(i, false, i2);
                if (i2 > 0) {
                    KeyguardPatternView.this.handleAttemptLockout(KeyguardPatternView.this.mLockPatternUtils.setLockoutAttemptDeadline(i, i2));
                }
            }
            if (i2 == 0) {
                KeyguardPatternView.this.mSecurityMessageDisplay.setMessage(com.android.systemui.R.string.kg_wrong_pattern);
                KeyguardPatternView.this.mLockPatternView.postDelayed(KeyguardPatternView.this.mCancelPatternRunnable, 2000L);
            }
        }
    }

    /* JADX WARN: Type inference failed for: r0v5, types: [com.android.keyguard.KeyguardPatternView$2] */
    private void handleAttemptLockout(long j) {
        this.mLockPatternView.clearPattern();
        this.mLockPatternView.setEnabled(false);
        long jCeil = (long) Math.ceil((j - SystemClock.elapsedRealtime()) / 1000.0d);
        if (this.mCountdownTimer != null) {
            this.mCountdownTimer.cancel();
        }
        this.mCountdownTimer = new CountDownTimer(jCeil * 1000, 1000L) { // from class: com.android.keyguard.KeyguardPatternView.2
            @Override // android.os.CountDownTimer
            public void onTick(long j2) {
                int iRound = (int) Math.round(j2 / 1000.0d);
                KeyguardPatternView.this.mSecurityMessageDisplay.setMessage(KeyguardPatternView.this.mContext.getResources().getQuantityString(com.android.systemui.R.plurals.kg_too_many_failed_attempts_countdown, iRound, Integer.valueOf(iRound)));
            }

            @Override // android.os.CountDownTimer
            public void onFinish() {
                KeyguardPatternView.this.mLockPatternView.setEnabled(true);
                KeyguardPatternView.this.displayDefaultSecurityMessage();
            }
        }.start();
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public boolean needsInput() {
        return false;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void onPause() {
        if (this.mCountdownTimer != null) {
            this.mCountdownTimer.cancel();
            this.mCountdownTimer = null;
        }
        if (this.mPendingLockCheck != null) {
            this.mPendingLockCheck.cancel(false);
            this.mPendingLockCheck = null;
        }
        reset();
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void onResume(int i) {
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void showPromptReason(int i) throws Resources.NotFoundException {
        switch (i) {
            case 0:
                break;
            case 1:
                this.mSecurityMessageDisplay.setMessage(com.android.systemui.R.string.kg_prompt_reason_restart_pattern);
                break;
            case 2:
                this.mSecurityMessageDisplay.setMessage(com.android.systemui.R.string.kg_prompt_reason_timeout_pattern);
                break;
            case 3:
                this.mSecurityMessageDisplay.setMessage(com.android.systemui.R.string.kg_prompt_reason_device_admin);
                break;
            case 4:
                this.mSecurityMessageDisplay.setMessage(com.android.systemui.R.string.kg_prompt_reason_user_request);
                break;
            default:
                this.mSecurityMessageDisplay.setMessage(com.android.systemui.R.string.kg_prompt_reason_timeout_pattern);
                break;
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void showMessage(CharSequence charSequence, int i) {
        this.mSecurityMessageDisplay.setNextMessageColor(i);
        this.mSecurityMessageDisplay.setMessage(charSequence);
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        AppearAnimationUtils.startTranslationYAnimation(this, 0L, 500L, 0.0f, this.mAppearAnimationUtils.getInterpolator());
        this.mAppearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() { // from class: com.android.keyguard.KeyguardPatternView.3
            @Override // java.lang.Runnable
            public void run() {
                KeyguardPatternView.this.enableClipping(true);
            }
        }, this);
        if (!TextUtils.isEmpty(this.mSecurityMessageDisplay.getText())) {
            this.mAppearAnimationUtils.createAnimation((View) this.mSecurityMessageDisplay, 0L, 220L, this.mAppearAnimationUtils.getStartTranslation(), true, this.mAppearAnimationUtils.getInterpolator(), (Runnable) null);
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public boolean startDisappearAnimation(final Runnable runnable) {
        float f;
        DisappearAnimationUtils disappearAnimationUtils;
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            f = 1.5f;
        } else {
            f = 1.0f;
        }
        this.mLockPatternView.clearPattern();
        enableClipping(false);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0L, (long) (300.0f * f), -this.mDisappearAnimationUtils.getStartTranslation(), this.mDisappearAnimationUtils.getInterpolator());
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            disappearAnimationUtils = this.mDisappearAnimationUtilsLocked;
        } else {
            disappearAnimationUtils = this.mDisappearAnimationUtils;
        }
        disappearAnimationUtils.startAnimation2d(this.mLockPatternView.getCellStates(), new Runnable() { // from class: com.android.keyguard.-$$Lambda$KeyguardPatternView$i51b4f44m8j5rvWUlLMM4eRNauI
            @Override // java.lang.Runnable
            public final void run() {
                KeyguardPatternView.lambda$startDisappearAnimation$1(this.f$0, runnable);
            }
        }, this);
        if (!TextUtils.isEmpty(this.mSecurityMessageDisplay.getText())) {
            this.mDisappearAnimationUtils.createAnimation((View) this.mSecurityMessageDisplay, 0L, (long) (200.0f * f), (-this.mDisappearAnimationUtils.getStartTranslation()) * 3.0f, false, this.mDisappearAnimationUtils.getInterpolator(), (Runnable) null);
            return true;
        }
        return true;
    }

    public static /* synthetic */ void lambda$startDisappearAnimation$1(KeyguardPatternView keyguardPatternView, Runnable runnable) {
        keyguardPatternView.enableClipping(true);
        if (runnable != null) {
            runnable.run();
        }
    }

    private void enableClipping(boolean z) {
        setClipChildren(z);
        this.mContainer.setClipToPadding(z);
        this.mContainer.setClipChildren(z);
    }

    /* JADX DEBUG: Method merged with bridge method: createAnimation(Ljava/lang/Object;JJFZLandroid/view/animation/Interpolator;Ljava/lang/Runnable;)V */
    @Override // com.android.settingslib.animation.AppearAnimationCreator
    public void createAnimation(LockPatternView.CellState cellState, long j, long j2, float f, boolean z, Interpolator interpolator, Runnable runnable) {
        this.mLockPatternView.startCellStateAnimation(cellState, 1.0f, z ? 1.0f : 0.0f, z ? f : 0.0f, z ? 0.0f : f, z ? 0.0f : 1.0f, 1.0f, j, j2, interpolator, runnable);
        if (runnable != null) {
            this.mAppearAnimationUtils.createAnimation(this.mEcaView, j, j2, f, z, interpolator, (Runnable) null);
        }
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public CharSequence getTitle() {
        return getContext().getString(android.R.string.config_systemGameService);
    }
}
