package com.android.keyguard;

import android.R;
import android.content.Context;
import android.media.AudioSystem;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

public class KeyguardPINView extends KeyguardPinBasedInputView {
    private final AppearAnimationUtils mAppearAnimationUtils;
    private ViewGroup mContainer;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private int mDisappearYTranslation;
    private View mDivider;
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
        this.mDisappearAnimationUtils = new DisappearAnimationUtils(context, 125L, 0.6f, 0.45f, AnimationUtils.loadInterpolator(this.mContext, R.interpolator.fast_out_linear_in));
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
    }

    @Override
    protected void resetState() {
        super.resetState();
        if (KeyguardUpdateMonitor.getInstance(this.mContext).getMaxBiometricUnlockAttemptsReached()) {
            if (!this.mLockPatternUtils.usingVoiceWeak()) {
                return;
            }
            this.mSecurityMessageDisplay.setMessage(R$string.voiceunlock_multiple_failures, true);
            return;
        }
        this.mSecurityMessageDisplay.setMessage(R$string.kg_pin_instructions, true);
    }

    @Override
    protected int getPasswordTextViewId() {
        return R$id.pinEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContainer = (ViewGroup) findViewById(R$id.container);
        this.mRow0 = (ViewGroup) findViewById(R$id.row0);
        this.mRow1 = (ViewGroup) findViewById(R$id.row1);
        this.mRow2 = (ViewGroup) findViewById(R$id.row2);
        this.mRow3 = (ViewGroup) findViewById(R$id.row3);
        this.mDivider = findViewById(R$id.divider);
        this.mViews = new View[][]{new View[]{this.mRow0, null, null}, new View[]{findViewById(R$id.key1), findViewById(R$id.key2), findViewById(R$id.key3)}, new View[]{findViewById(R$id.key4), findViewById(R$id.key5), findViewById(R$id.key6)}, new View[]{findViewById(R$id.key7), findViewById(R$id.key8), findViewById(R$id.key9)}, new View[]{null, findViewById(R$id.key0), findViewById(R$id.key_enter)}, new View[]{null, this.mEcaView, null}};
    }

    @Override
    public int getWrongPasswordStringId() {
        return R$string.kg_wrong_pin;
    }

    @Override
    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        AppearAnimationUtils.startTranslationYAnimation(this, 0L, 500L, 0.0f, this.mAppearAnimationUtils.getInterpolator());
        this.mAppearAnimationUtils.startAnimation2d(this.mViews, new Runnable() {
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
        AppearAnimationUtils.startTranslationYAnimation(this, 0L, 280L, this.mDisappearYTranslation, this.mDisappearAnimationUtils.getInterpolator());
        this.mDisappearAnimationUtils.startAnimation2d(this.mViews, new Runnable() {
            @Override
            public void run() {
                KeyguardPINView.this.enableClipping(true);
                if (finishRunnable == null) {
                    return;
                }
                finishRunnable.run();
            }
        });
        return true;
    }

    public void enableClipping(boolean enable) {
        this.mContainer.setClipToPadding(enable);
        this.mContainer.setClipChildren(enable);
        this.mRow1.setClipToPadding(enable);
        this.mRow2.setClipToPadding(enable);
        this.mRow3.setClipToPadding(enable);
        setClipChildren(enable);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onResume(int reason) {
        super.onResume(reason);
        boolean mediaPlaying = AudioSystem.isStreamActive(3, 0);
        if (!this.mLockPatternUtils.usingVoiceWeak() || !mediaPlaying) {
            return;
        }
        this.mSecurityMessageDisplay.setMessage(R$string.voice_unlock_media_playing, true);
    }
}
