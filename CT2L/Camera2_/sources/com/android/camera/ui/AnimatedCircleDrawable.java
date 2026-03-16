package com.android.camera.ui;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import com.android.camera.util.Gusterpolator;

public class AnimatedCircleDrawable extends Drawable {
    private static final int CIRCLE_ANIM_DURATION_MS = 300;
    private static int DRAWABLE_MAX_LEVEL = 10000;
    private int mCanvasHeight;
    private int mCanvasWidth;
    private int mColor;
    private int mRadius;
    private int mSmallRadiusTarget;
    private int mAlpha = MotionEventCompat.ACTION_MASK;
    private Paint mPaint = new Paint();

    public AnimatedCircleDrawable(int smallRadiusTarget) {
        this.mPaint.setAntiAlias(true);
        this.mSmallRadiusTarget = smallRadiusTarget;
    }

    public void setColor(int color) {
        this.mColor = color;
        updatePaintColor();
    }

    private void updatePaintColor() {
        int paintColor = (this.mAlpha << 24) | (this.mColor & ViewCompat.MEASURED_SIZE_MASK);
        this.mPaint.setColor(paintColor);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void setAlpha(int alpha) {
        this.mAlpha = alpha;
        updatePaintColor();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public boolean onLevelChange(int level) {
        invalidateSelf();
        return true;
    }

    public void animateToSmallRadius() {
        int smallLevel = map(this.mSmallRadiusTarget, 0, diagonalLength(this.mCanvasWidth, this.mCanvasHeight) / 2, 0, DRAWABLE_MAX_LEVEL);
        ValueAnimator animator = ValueAnimator.ofInt(getLevel(), smallLevel);
        animator.setDuration(300L);
        animator.setInterpolator(Gusterpolator.INSTANCE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                AnimatedCircleDrawable.this.setLevel(((Integer) animation.getAnimatedValue()).intValue());
            }
        });
        animator.start();
    }

    public void animateToFullSize() {
        ValueAnimator animator = ValueAnimator.ofInt(getLevel(), DRAWABLE_MAX_LEVEL);
        animator.setDuration(300L);
        animator.setInterpolator(Gusterpolator.INSTANCE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                AnimatedCircleDrawable.this.setLevel(((Integer) animation.getAnimatedValue()).intValue());
            }
        });
        animator.start();
    }

    @Override
    public void draw(Canvas canvas) {
        this.mCanvasWidth = canvas.getWidth();
        this.mCanvasHeight = canvas.getHeight();
        this.mRadius = map(getLevel(), 0, DRAWABLE_MAX_LEVEL, 0, diagonalLength(canvas.getWidth(), canvas.getHeight()) / 2);
        canvas.drawCircle(canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f, this.mRadius, this.mPaint);
    }

    private static int map(int x, int in_min, int in_max, int out_min, int out_max) {
        return (((x - in_min) * (out_max - out_min)) / (in_max - in_min)) + out_min;
    }

    private static int diagonalLength(int w, int h) {
        return (int) Math.sqrt((w * w) + (h * h));
    }
}
