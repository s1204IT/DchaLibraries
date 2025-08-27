package com.android.settings.fingerprint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.settings.R;
import com.android.settings.Utils;

/* loaded from: classes.dex */
public class FingerprintLocationAnimationView extends View implements FingerprintFindSensorAnimation {
    private ValueAnimator mAlphaAnimator;
    private final Paint mDotPaint;
    private final int mDotRadius;
    private final Interpolator mFastOutSlowInInterpolator;
    private final float mFractionCenterX;
    private final float mFractionCenterY;
    private final Interpolator mLinearOutSlowInInterpolator;
    private final int mMaxPulseRadius;
    private final Paint mPulsePaint;
    private float mPulseRadius;
    private ValueAnimator mRadiusAnimator;
    private final Runnable mStartPhaseRunnable;

    public FingerprintLocationAnimationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDotPaint = new Paint();
        this.mPulsePaint = new Paint();
        this.mStartPhaseRunnable = new Runnable() { // from class: com.android.settings.fingerprint.FingerprintLocationAnimationView.5
            @Override // java.lang.Runnable
            public void run() {
                FingerprintLocationAnimationView.this.startPhase();
            }
        };
        this.mDotRadius = getResources().getDimensionPixelSize(R.dimen.fingerprint_dot_radius);
        this.mMaxPulseRadius = getResources().getDimensionPixelSize(R.dimen.fingerprint_pulse_radius);
        this.mFractionCenterX = getResources().getFraction(R.fraction.fingerprint_sensor_location_fraction_x, 1, 1);
        this.mFractionCenterY = getResources().getFraction(R.fraction.fingerprint_sensor_location_fraction_y, 1, 1);
        int colorAccent = Utils.getColorAccent(context);
        this.mDotPaint.setAntiAlias(true);
        this.mPulsePaint.setAntiAlias(true);
        this.mDotPaint.setColor(colorAccent);
        this.mPulsePaint.setColor(colorAccent);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        drawPulse(canvas);
        drawDot(canvas);
    }

    private void drawDot(Canvas canvas) {
        canvas.drawCircle(getCenterX(), getCenterY(), this.mDotRadius, this.mDotPaint);
    }

    private void drawPulse(Canvas canvas) {
        canvas.drawCircle(getCenterX(), getCenterY(), this.mPulseRadius, this.mPulsePaint);
    }

    private float getCenterX() {
        return getWidth() * this.mFractionCenterX;
    }

    private float getCenterY() {
        return getHeight() * this.mFractionCenterY;
    }

    @Override // com.android.settings.fingerprint.FingerprintFindSensorAnimation
    public void startAnimation() {
        startPhase();
    }

    @Override // com.android.settings.fingerprint.FingerprintFindSensorAnimation
    public void stopAnimation() {
        removeCallbacks(this.mStartPhaseRunnable);
        if (this.mRadiusAnimator != null) {
            this.mRadiusAnimator.cancel();
        }
        if (this.mAlphaAnimator != null) {
            this.mAlphaAnimator.cancel();
        }
    }

    @Override // com.android.settings.fingerprint.FingerprintFindSensorAnimation
    public void pauseAnimation() {
        stopAnimation();
    }

    private void startPhase() {
        startRadiusAnimation();
        startAlphaAnimation();
    }

    private void startRadiusAnimation() {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, this.mMaxPulseRadius);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.settings.fingerprint.FingerprintLocationAnimationView.1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                FingerprintLocationAnimationView.this.mPulseRadius = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                FingerprintLocationAnimationView.this.invalidate();
            }
        });
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.settings.fingerprint.FingerprintLocationAnimationView.2
            boolean mCancelled;

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                FingerprintLocationAnimationView.this.mRadiusAnimator = null;
                if (!this.mCancelled) {
                    FingerprintLocationAnimationView.this.postDelayed(FingerprintLocationAnimationView.this.mStartPhaseRunnable, 1000L);
                }
            }
        });
        valueAnimatorOfFloat.setDuration(1000L);
        valueAnimatorOfFloat.setInterpolator(this.mLinearOutSlowInInterpolator);
        valueAnimatorOfFloat.start();
        this.mRadiusAnimator = valueAnimatorOfFloat;
    }

    private void startAlphaAnimation() {
        this.mPulsePaint.setAlpha(38);
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.15f, 0.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.settings.fingerprint.FingerprintLocationAnimationView.3
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                FingerprintLocationAnimationView.this.mPulsePaint.setAlpha((int) (255.0f * ((Float) valueAnimator.getAnimatedValue()).floatValue()));
                FingerprintLocationAnimationView.this.invalidate();
            }
        });
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.settings.fingerprint.FingerprintLocationAnimationView.4
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                FingerprintLocationAnimationView.this.mAlphaAnimator = null;
            }
        });
        valueAnimatorOfFloat.setDuration(750L);
        valueAnimatorOfFloat.setInterpolator(this.mFastOutSlowInInterpolator);
        valueAnimatorOfFloat.setStartDelay(250L);
        valueAnimatorOfFloat.start();
        this.mAlphaAnimator = valueAnimatorOfFloat;
    }
}
