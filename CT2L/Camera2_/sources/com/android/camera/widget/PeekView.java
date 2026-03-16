package com.android.camera.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import com.android.camera.util.CameraUtil;

public class PeekView extends ImageView {
    private static final float FILMSTRIP_SCALE = 0.7f;
    private static final long PEEK_IN_DURATION_MS = 200;
    private static final long PEEK_OUT_DURATION_MS = 200;
    private static final long PEEK_STAY_DURATION_MS = 100;
    private static final float ROTATE_ANGLE = -7.0f;
    private boolean mAnimationCanceled;
    private Rect mDrawableBound;
    private Drawable mImageDrawable;
    private AnimatorSet mPeekAnimator;
    private float mPeekRotateAngle;
    private float mRotateScale;
    private Point mRotationPivot;

    public PeekView(Context context) {
        super(context);
        init();
    }

    public PeekView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PeekView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.mRotationPivot = new Point();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (this.mImageDrawable != null) {
            c.save();
            c.rotate(this.mPeekRotateAngle, this.mRotationPivot.x, this.mRotationPivot.y);
            this.mImageDrawable.setBounds(this.mDrawableBound);
            this.mImageDrawable.draw(c);
            c.restore();
        }
    }

    public void startPeekAnimation(Bitmap bitmap, boolean strong, String accessibilityString) {
        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                PeekView.this.mPeekRotateAngle = ((Float) valueAnimator.getAnimatedValue()).floatValue() * PeekView.this.mRotateScale;
                PeekView.this.invalidate();
            }
        };
        ValueAnimator peekAnimateIn = ValueAnimator.ofFloat(0.0f, ROTATE_ANGLE);
        ValueAnimator peekAnimateStay = ValueAnimator.ofFloat(ROTATE_ANGLE, ROTATE_ANGLE);
        ValueAnimator peekAnimateOut = ValueAnimator.ofFloat(ROTATE_ANGLE, 0.0f);
        peekAnimateIn.addUpdateListener(updateListener);
        peekAnimateOut.addUpdateListener(updateListener);
        peekAnimateIn.setDuration(200L);
        peekAnimateStay.setDuration(PEEK_STAY_DURATION_MS);
        peekAnimateOut.setDuration(200L);
        peekAnimateIn.setInterpolator(new DecelerateInterpolator());
        peekAnimateOut.setInterpolator(new AccelerateInterpolator());
        this.mPeekAnimator = new AnimatorSet();
        this.mPeekAnimator.playSequentially(peekAnimateIn, peekAnimateStay, peekAnimateOut);
        this.mPeekAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                PeekView.this.setVisibility(0);
                PeekView.this.mAnimationCanceled = false;
                PeekView.this.invalidate();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (!PeekView.this.mAnimationCanceled) {
                    PeekView.this.clear();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                PeekView.this.mAnimationCanceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mRotateScale = strong ? 1.0f : 0.5f;
        this.mImageDrawable = new BitmapDrawable(getResources(), bitmap);
        Point drawDim = CameraUtil.resizeToFill(this.mImageDrawable.getIntrinsicWidth(), this.mImageDrawable.getIntrinsicHeight(), 0, (int) (getWidth() * FILMSTRIP_SCALE), (int) (getHeight() * FILMSTRIP_SCALE));
        int x = getMeasuredWidth();
        int y = (getMeasuredHeight() - drawDim.y) / 2;
        this.mDrawableBound = new Rect(x, y, drawDim.x + x, drawDim.y + y);
        this.mRotationPivot.set(x, (int) (((double) y) + (((double) drawDim.y) * 1.1d)));
        this.mPeekAnimator.start();
        announceForAccessibility(accessibilityString);
    }

    public boolean isPeekAnimationRunning() {
        return this.mPeekAnimator.isRunning();
    }

    public void stopPeekAnimation() {
        if (isPeekAnimationRunning()) {
            this.mPeekAnimator.end();
        } else {
            clear();
        }
    }

    public void cancelPeekAnimation() {
        if (isPeekAnimationRunning()) {
            this.mPeekAnimator.cancel();
        } else {
            clear();
        }
    }

    private void clear() {
        setVisibility(4);
        setImageDrawable(null);
    }
}
