package com.android.systemui.statusbar.phone;

import android.R;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import com.android.keyguard.AlphaOptimizedImageButton;
import com.android.systemui.Interpolators;

/* loaded from: classes.dex */
public class SettingsButton extends AlphaOptimizedImageButton {
    private ObjectAnimator mAnimator;
    private final Runnable mLongPressCallback;
    private float mSlop;
    private boolean mUpToSpeed;

    public SettingsButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLongPressCallback = new Runnable() { // from class: com.android.systemui.statusbar.phone.SettingsButton.3
            @Override // java.lang.Runnable
            public void run() {
                SettingsButton.this.startAccelSpin();
            }
        };
        this.mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public boolean isTunerClick() {
        return this.mUpToSpeed;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 1:
                if (this.mUpToSpeed) {
                    startExitAnimation();
                    break;
                } else {
                    cancelLongClick();
                    break;
                }
            case 2:
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                if (x < (-this.mSlop) || y < (-this.mSlop) || x > getWidth() + this.mSlop || y > getHeight() + this.mSlop) {
                    cancelLongClick();
                    break;
                }
                break;
            case 3:
                cancelLongClick();
                break;
        }
        return super.onTouchEvent(motionEvent);
    }

    private void cancelLongClick() {
        cancelAnimation();
        this.mUpToSpeed = false;
        removeCallbacks(this.mLongPressCallback);
    }

    private void cancelAnimation() {
        if (this.mAnimator != null) {
            this.mAnimator.removeAllListeners();
            this.mAnimator.cancel();
            this.mAnimator = null;
        }
    }

    private void startExitAnimation() {
        animate().translationX(((View) getParent().getParent()).getWidth() - getX()).alpha(0.0f).setDuration(350L).setInterpolator(AnimationUtils.loadInterpolator(this.mContext, R.interpolator.accelerate_cubic)).setListener(new Animator.AnimatorListener() { // from class: com.android.systemui.statusbar.phone.SettingsButton.1
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                SettingsButton.this.setAlpha(1.0f);
                SettingsButton.this.setTranslationX(0.0f);
                SettingsButton.this.cancelLongClick();
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
            }
        }).start();
    }

    protected void startAccelSpin() {
        cancelAnimation();
        this.mAnimator = ObjectAnimator.ofFloat(this, (Property<SettingsButton, Float>) View.ROTATION, 0.0f, 360.0f);
        this.mAnimator.setInterpolator(AnimationUtils.loadInterpolator(this.mContext, R.interpolator.accelerate_quad));
        this.mAnimator.setDuration(750L);
        this.mAnimator.addListener(new Animator.AnimatorListener() { // from class: com.android.systemui.statusbar.phone.SettingsButton.2
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                SettingsButton.this.startContinuousSpin();
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
            }
        });
        this.mAnimator.start();
    }

    protected void startContinuousSpin() {
        cancelAnimation();
        performHapticFeedback(0);
        this.mUpToSpeed = true;
        this.mAnimator = ObjectAnimator.ofFloat(this, (Property<SettingsButton, Float>) View.ROTATION, 0.0f, 360.0f);
        this.mAnimator.setInterpolator(Interpolators.LINEAR);
        this.mAnimator.setDuration(375L);
        this.mAnimator.setRepeatCount(-1);
        this.mAnimator.start();
    }
}
