package com.android.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.android.camera.debug.Log;
import com.android.camera.ui.PreviewStatusListener;

public class CaptureAnimationOverlay extends View implements PreviewStatusListener.PreviewAreaChangedListener {
    private static final int FLASH_COLOR = -1;
    private static final long FLASH_DECREASE_DURATION_MS = 150;
    private static final long FLASH_FULL_DURATION_MS = 65;
    private static final float FLASH_MAX_ALPHA = 0.85f;
    private static final long SHORT_FLASH_DECREASE_DURATION_MS = 100;
    private static final long SHORT_FLASH_FULL_DURATION_MS = 34;
    private static final float SHORT_FLASH_MAX_ALPHA = 0.75f;
    private static final Log.Tag TAG = new Log.Tag("CaptureAnimOverlay");
    private final Interpolator mFlashAnimInterpolator;
    private final Animator.AnimatorListener mFlashAnimListener;
    private final ValueAnimator.AnimatorUpdateListener mFlashAnimUpdateListener;
    private AnimatorSet mFlashAnimation;
    private final Paint mPaint;
    private RectF mPreviewArea;

    public CaptureAnimationOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPreviewArea = new RectF();
        this.mPaint = new Paint();
        this.mPaint.setColor(-1);
        this.mFlashAnimInterpolator = new LinearInterpolator();
        this.mFlashAnimUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                CaptureAnimationOverlay.this.setAlpha(((Float) animation.getAnimatedValue()).floatValue());
                CaptureAnimationOverlay.this.invalidate();
            }
        };
        this.mFlashAnimListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                CaptureAnimationOverlay.this.setVisibility(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                CaptureAnimationOverlay.this.mFlashAnimation = null;
                CaptureAnimationOverlay.this.setVisibility(4);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        };
    }

    public void startFlashAnimation(boolean shortFlash) {
        float maxAlpha;
        if (this.mFlashAnimation != null && this.mFlashAnimation.isRunning()) {
            this.mFlashAnimation.cancel();
        }
        if (shortFlash) {
            maxAlpha = SHORT_FLASH_MAX_ALPHA;
        } else {
            maxAlpha = FLASH_MAX_ALPHA;
        }
        ValueAnimator flashAnim1 = ValueAnimator.ofFloat(maxAlpha, maxAlpha);
        ValueAnimator flashAnim2 = ValueAnimator.ofFloat(maxAlpha, 0.0f);
        if (shortFlash) {
            flashAnim1.setDuration(SHORT_FLASH_FULL_DURATION_MS);
            flashAnim2.setDuration(SHORT_FLASH_DECREASE_DURATION_MS);
        } else {
            flashAnim1.setDuration(FLASH_FULL_DURATION_MS);
            flashAnim2.setDuration(FLASH_DECREASE_DURATION_MS);
        }
        flashAnim1.addUpdateListener(this.mFlashAnimUpdateListener);
        flashAnim2.addUpdateListener(this.mFlashAnimUpdateListener);
        flashAnim1.setInterpolator(this.mFlashAnimInterpolator);
        flashAnim2.setInterpolator(this.mFlashAnimInterpolator);
        this.mFlashAnimation = new AnimatorSet();
        this.mFlashAnimation.play(flashAnim1).before(flashAnim2);
        this.mFlashAnimation.addListener(this.mFlashAnimListener);
        this.mFlashAnimation.start();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.mFlashAnimation != null && this.mFlashAnimation.isRunning()) {
            canvas.drawRect(this.mPreviewArea, this.mPaint);
        }
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        this.mPreviewArea.set(previewArea);
    }
}
