package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class ViewInvertHelper {
    private final long mFadeDuration;
    private final Interpolator mLinearOutSlowInInterpolator;
    private final View mTarget;
    private final Paint mDarkPaint = new Paint();
    private final ColorMatrix mMatrix = new ColorMatrix();
    private final ColorMatrix mGrayscaleMatrix = new ColorMatrix();

    public ViewInvertHelper(View target, long fadeDuration) {
        this.mTarget = target;
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mTarget.getContext(), android.R.interpolator.linear_out_slow_in);
        this.mFadeDuration = fadeDuration;
    }

    public void fade(final boolean invert, long delay) {
        float startIntensity = invert ? 0.0f : 1.0f;
        float endIntensity = invert ? 1.0f : 0.0f;
        ValueAnimator animator = ValueAnimator.ofFloat(startIntensity, endIntensity);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewInvertHelper.this.updateInvertPaint(((Float) animation.getAnimatedValue()).floatValue());
                ViewInvertHelper.this.mTarget.setLayerType(2, ViewInvertHelper.this.mDarkPaint);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!invert) {
                    ViewInvertHelper.this.mTarget.setLayerType(0, null);
                }
            }
        });
        animator.setDuration(this.mFadeDuration);
        animator.setInterpolator(this.mLinearOutSlowInInterpolator);
        animator.setStartDelay(delay);
        animator.start();
    }

    public void update(boolean invert) {
        if (invert) {
            updateInvertPaint(1.0f);
            this.mTarget.setLayerType(2, this.mDarkPaint);
        } else {
            this.mTarget.setLayerType(0, null);
        }
    }

    private void updateInvertPaint(float intensity) {
        float components = 1.0f - (2.0f * intensity);
        float[] invert = {components, 0.0f, 0.0f, 0.0f, 255.0f * intensity, 0.0f, components, 0.0f, 0.0f, 255.0f * intensity, 0.0f, 0.0f, components, 0.0f, 255.0f * intensity, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f};
        this.mMatrix.set(invert);
        this.mGrayscaleMatrix.setSaturation(1.0f - intensity);
        this.mMatrix.preConcat(this.mGrayscaleMatrix);
        this.mDarkPaint.setColorFilter(new ColorMatrixColorFilter(this.mMatrix));
    }
}
