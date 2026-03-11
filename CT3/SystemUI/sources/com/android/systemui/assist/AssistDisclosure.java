package com.android.systemui.assist;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import com.android.systemui.Interpolators;

public class AssistDisclosure {
    private final Context mContext;
    private final Handler mHandler;
    private Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            AssistDisclosure.this.show();
        }
    };
    private AssistDisclosureView mView;
    private boolean mViewAdded;
    private final WindowManager mWm;

    public AssistDisclosure(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mWm = (WindowManager) this.mContext.getSystemService(WindowManager.class);
    }

    public void postShow() {
        this.mHandler.removeCallbacks(this.mShowRunnable);
        this.mHandler.post(this.mShowRunnable);
    }

    public void show() {
        if (this.mView == null) {
            this.mView = new AssistDisclosureView(this.mContext);
        }
        if (this.mViewAdded) {
            return;
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(2015, R.drawable.ic_media_route_connecting_light_05_mtrl, -3);
        lp.setTitle("AssistDisclosure");
        this.mWm.addView(this.mView, lp);
        this.mViewAdded = true;
    }

    public void hide() {
        if (!this.mViewAdded) {
            return;
        }
        this.mWm.removeView(this.mView);
        this.mViewAdded = false;
    }

    private class AssistDisclosureView extends View implements ValueAnimator.AnimatorUpdateListener {
        private int mAlpha;
        private final ValueAnimator mAlphaInAnimator;
        private final ValueAnimator mAlphaOutAnimator;
        private final AnimatorSet mAnimator;
        private final Paint mPaint;
        private final Paint mShadowPaint;
        private float mShadowThickness;
        private float mThickness;
        private final ValueAnimator mTracingAnimator;
        private float mTracingProgress;

        public AssistDisclosureView(Context context) {
            super(context);
            this.mPaint = new Paint();
            this.mShadowPaint = new Paint();
            this.mTracingProgress = 0.0f;
            this.mAlpha = 0;
            this.mTracingAnimator = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(600L);
            this.mTracingAnimator.addUpdateListener(this);
            this.mTracingAnimator.setInterpolator(AnimationUtils.loadInterpolator(this.mContext, com.android.systemui.R.interpolator.assist_disclosure_trace));
            this.mAlphaInAnimator = ValueAnimator.ofInt(0, 255).setDuration(450L);
            this.mAlphaInAnimator.addUpdateListener(this);
            this.mAlphaInAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            this.mAlphaOutAnimator = ValueAnimator.ofInt(255, 0).setDuration(400L);
            this.mAlphaOutAnimator.addUpdateListener(this);
            this.mAlphaOutAnimator.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
            this.mAnimator = new AnimatorSet();
            this.mAnimator.play(this.mAlphaInAnimator).with(this.mTracingAnimator);
            this.mAnimator.play(this.mAlphaInAnimator).before(this.mAlphaOutAnimator);
            this.mAnimator.addListener(new AnimatorListenerAdapter() {
                boolean mCancelled;

                @Override
                public void onAnimationStart(Animator animation) {
                    this.mCancelled = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (this.mCancelled) {
                        return;
                    }
                    AssistDisclosure.this.hide();
                }
            });
            PorterDuffXfermode srcMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
            this.mPaint.setColor(-1);
            this.mPaint.setXfermode(srcMode);
            this.mShadowPaint.setColor(-12303292);
            this.mShadowPaint.setXfermode(srcMode);
            this.mThickness = getResources().getDimension(com.android.systemui.R.dimen.assist_disclosure_thickness);
            this.mShadowThickness = getResources().getDimension(com.android.systemui.R.dimen.assist_disclosure_shadow_thickness);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startAnimation();
            sendAccessibilityEvent(16777216);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            this.mAnimator.cancel();
            this.mTracingProgress = 0.0f;
            this.mAlpha = 0;
        }

        private void startAnimation() {
            this.mAnimator.cancel();
            this.mAnimator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            this.mPaint.setAlpha(this.mAlpha);
            this.mShadowPaint.setAlpha(this.mAlpha / 4);
            drawGeometry(canvas, this.mShadowPaint, this.mShadowThickness);
            drawGeometry(canvas, this.mPaint, 0.0f);
        }

        private void drawGeometry(Canvas canvas, Paint paint, float padding) {
            int width = getWidth();
            int height = getHeight();
            float thickness = this.mThickness;
            float pixelProgress = this.mTracingProgress * ((width + height) - (2.0f * thickness));
            float bottomProgress = Math.min(pixelProgress, width / 2.0f);
            if (bottomProgress > 0.0f) {
                drawBeam(canvas, (width / 2.0f) - bottomProgress, height - thickness, (width / 2.0f) + bottomProgress, height, paint, padding);
            }
            float sideProgress = Math.min(pixelProgress - bottomProgress, height - thickness);
            if (sideProgress > 0.0f) {
                drawBeam(canvas, 0.0f, (height - thickness) - sideProgress, thickness, height - thickness, paint, padding);
                drawBeam(canvas, width - thickness, (height - thickness) - sideProgress, width, height - thickness, paint, padding);
            }
            float topProgress = Math.min((pixelProgress - bottomProgress) - sideProgress, (width / 2) - thickness);
            if (sideProgress <= 0.0f || topProgress <= 0.0f) {
                return;
            }
            drawBeam(canvas, thickness, 0.0f, thickness + topProgress, thickness, paint, padding);
            drawBeam(canvas, (width - thickness) - topProgress, 0.0f, width - thickness, thickness, paint, padding);
        }

        private void drawBeam(Canvas canvas, float left, float top, float right, float bottom, Paint paint, float padding) {
            canvas.drawRect(left - padding, top - padding, right + padding, bottom + padding, paint);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (animation == this.mAlphaOutAnimator) {
                this.mAlpha = ((Integer) this.mAlphaOutAnimator.getAnimatedValue()).intValue();
            } else if (animation == this.mAlphaInAnimator) {
                this.mAlpha = ((Integer) this.mAlphaInAnimator.getAnimatedValue()).intValue();
            } else if (animation == this.mTracingAnimator) {
                this.mTracingProgress = ((Float) this.mTracingAnimator.getAnimatedValue()).floatValue();
            }
            invalidate();
        }
    }
}
