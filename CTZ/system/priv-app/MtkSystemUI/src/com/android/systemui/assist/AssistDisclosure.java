package com.android.systemui.assist;

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
import com.android.systemui.Interpolators;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class AssistDisclosure {
    private final Context mContext;
    private final Handler mHandler;
    private Runnable mShowRunnable = new Runnable() { // from class: com.android.systemui.assist.AssistDisclosure.1
        @Override // java.lang.Runnable
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

    private void show() {
        if (this.mView == null) {
            this.mView = new AssistDisclosureView(this.mContext);
        }
        if (!this.mViewAdded) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(2015, 525576, -3);
            layoutParams.setTitle("AssistDisclosure");
            this.mWm.addView(this.mView, layoutParams);
            this.mViewAdded = true;
        }
    }

    private void hide() {
        if (this.mViewAdded) {
            this.mWm.removeView(this.mView);
            this.mViewAdded = false;
        }
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

        public AssistDisclosureView(Context context) {
            super(context);
            this.mPaint = new Paint();
            this.mShadowPaint = new Paint();
            this.mAlpha = 0;
            this.mAlphaInAnimator = ValueAnimator.ofInt(0, 222).setDuration(400L);
            this.mAlphaInAnimator.addUpdateListener(this);
            this.mAlphaInAnimator.setInterpolator(Interpolators.CUSTOM_40_40);
            this.mAlphaOutAnimator = ValueAnimator.ofInt(222, 0).setDuration(300L);
            this.mAlphaOutAnimator.addUpdateListener(this);
            this.mAlphaOutAnimator.setInterpolator(Interpolators.CUSTOM_40_40);
            this.mAnimator = new AnimatorSet();
            this.mAnimator.play(this.mAlphaInAnimator).before(this.mAlphaOutAnimator);
            this.mAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.assist.AssistDisclosure.AssistDisclosureView.1
                boolean mCancelled;

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator animator) {
                    this.mCancelled = false;
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animator) {
                    this.mCancelled = true;
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    if (!this.mCancelled) {
                        AssistDisclosure.this.hide();
                    }
                }
            });
            PorterDuffXfermode porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
            this.mPaint.setColor(-1);
            this.mPaint.setXfermode(porterDuffXfermode);
            this.mShadowPaint.setColor(-12303292);
            this.mShadowPaint.setXfermode(porterDuffXfermode);
            this.mThickness = getResources().getDimension(R.dimen.assist_disclosure_thickness);
            this.mShadowThickness = getResources().getDimension(R.dimen.assist_disclosure_shadow_thickness);
        }

        @Override // android.view.View
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startAnimation();
            sendAccessibilityEvent(16777216);
        }

        @Override // android.view.View
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            this.mAnimator.cancel();
            this.mAlpha = 0;
        }

        private void startAnimation() {
            this.mAnimator.cancel();
            this.mAnimator.start();
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            this.mPaint.setAlpha(this.mAlpha);
            this.mShadowPaint.setAlpha(this.mAlpha / 4);
            drawGeometry(canvas, this.mShadowPaint, this.mShadowThickness);
            drawGeometry(canvas, this.mPaint, 0.0f);
        }

        private void drawGeometry(Canvas canvas, Paint paint, float f) {
            int width = getWidth();
            int height = getHeight();
            float f2 = this.mThickness;
            float f3 = height;
            float f4 = f3 - f2;
            float f5 = width;
            drawBeam(canvas, 0.0f, f4, f5, f3, paint, f);
            drawBeam(canvas, 0.0f, 0.0f, f2, f4, paint, f);
            float f6 = f5 - f2;
            drawBeam(canvas, f6, 0.0f, f5, f4, paint, f);
            drawBeam(canvas, f2, 0.0f, f6, f2, paint, f);
        }

        private void drawBeam(Canvas canvas, float f, float f2, float f3, float f4, Paint paint, float f5) {
            canvas.drawRect(f - f5, f2 - f5, f3 + f5, f4 + f5, paint);
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (valueAnimator == this.mAlphaOutAnimator) {
                this.mAlpha = ((Integer) this.mAlphaOutAnimator.getAnimatedValue()).intValue();
            } else if (valueAnimator == this.mAlphaInAnimator) {
                this.mAlpha = ((Integer) this.mAlphaInAnimator.getAnimatedValue()).intValue();
            }
            invalidate();
        }
    }
}
