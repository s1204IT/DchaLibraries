package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

public class KeyguardPINView extends KeyguardPinBasedInputView {
    private final AppearAnimationUtils mAppearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private int mDisappearYTranslation;
    private View mDivider;
    private ViewGroup mKeyguardBouncerFrame;
    private ViewGroup mRow0;
    private ViewGroup mRow1;
    private ViewGroup mRow2;
    private ViewGroup mRow3;
    private View[][] mViews;

    public KeyguardPINView(Context context) {
        this(context, null);
    }

    public KeyguardPINView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAppearAnimationUtils = new AppearAnimationUtils(context);
        this.mDisappearAnimationUtils = new DisappearAnimationUtils(context, 125L, 0.6f, 0.6f, AnimationUtils.loadInterpolator(this.mContext, android.R.interpolator.fast_out_linear_in));
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R.dimen.disappear_y_translation);
    }

    @Override
    protected void resetState() {
        super.resetState();
        if (KeyguardUpdateMonitor.getInstance(this.mContext).getMaxBiometricUnlockAttemptsReached()) {
            this.mSecurityMessageDisplay.setMessage(R.string.faceunlock_multiple_failures, true);
        } else {
            this.mSecurityMessageDisplay.setMessage(R.string.kg_pin_instructions, false);
        }
    }

    @Override
    protected int getPasswordTextViewId() {
        return R.id.pinEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mKeyguardBouncerFrame = (ViewGroup) findViewById(R.id.keyguard_bouncer_frame);
        this.mRow0 = (ViewGroup) findViewById(R.id.row0);
        this.mRow1 = (ViewGroup) findViewById(R.id.row1);
        this.mRow2 = (ViewGroup) findViewById(R.id.row2);
        this.mRow3 = (ViewGroup) findViewById(R.id.row3);
        this.mDivider = findViewById(R.id.divider);
        this.mViews = new View[][]{new View[]{this.mRow0, null, null}, new View[]{findViewById(R.id.key1), findViewById(R.id.key2), findViewById(R.id.key3)}, new View[]{findViewById(R.id.key4), findViewById(R.id.key5), findViewById(R.id.key6)}, new View[]{findViewById(R.id.key7), findViewById(R.id.key8), findViewById(R.id.key9)}, new View[]{null, findViewById(R.id.key0), findViewById(R.id.key_enter)}, new View[]{null, this.mEcaView, null}};
    }

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public int getWrongPasswordStringId() {
        return R.string.kg_wrong_pin;
    }

    @Override
    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        animate().setDuration(500L).setInterpolator(this.mAppearAnimationUtils.getInterpolator()).translationY(0.0f);
        this.mAppearAnimationUtils.startAnimation(this.mViews, new Runnable() {
            @Override
            public void run() {
                KeyguardPINView.this.enableClipping(true);
            }
        });
    }

    @Override
    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        enableClipping(false);
        setTranslationY(0.0f);
        animate().setDuration(280L).setInterpolator(this.mDisappearAnimationUtils.getInterpolator()).translationY(this.mDisappearYTranslation);
        this.mDisappearAnimationUtils.startAnimation(this.mViews, new Runnable() {
            @Override
            public void run() {
                KeyguardPINView.this.enableClipping(true);
                if (finishRunnable != null) {
                    finishRunnable.run();
                }
            }
        });
        return true;
    }

    private void enableClipping(boolean enable) {
        this.mKeyguardBouncerFrame.setClipToPadding(enable);
        this.mKeyguardBouncerFrame.setClipChildren(enable);
        this.mRow1.setClipToPadding(enable);
        this.mRow2.setClipToPadding(enable);
        this.mRow3.setClipToPadding(enable);
        setClipChildren(enable);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
