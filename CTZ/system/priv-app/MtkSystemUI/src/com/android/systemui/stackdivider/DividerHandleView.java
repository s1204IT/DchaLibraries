package com.android.systemui.stackdivider;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class DividerHandleView extends View {
    private AnimatorSet mAnimator;
    private final int mCircleDiameter;
    private int mCurrentHeight;
    private int mCurrentWidth;
    private final int mHeight;
    private final Paint mPaint;
    private boolean mTouching;
    private final int mWidth;
    private static final Property<DividerHandleView, Integer> WIDTH_PROPERTY = new Property<DividerHandleView, Integer>(Integer.class, "width") { // from class: com.android.systemui.stackdivider.DividerHandleView.1
        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Integer get(DividerHandleView dividerHandleView) {
            return Integer.valueOf(dividerHandleView.mCurrentWidth);
        }

        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(DividerHandleView dividerHandleView, Integer num) {
            dividerHandleView.mCurrentWidth = num.intValue();
            dividerHandleView.invalidate();
        }
    };
    private static final Property<DividerHandleView, Integer> HEIGHT_PROPERTY = new Property<DividerHandleView, Integer>(Integer.class, "height") { // from class: com.android.systemui.stackdivider.DividerHandleView.2
        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Integer get(DividerHandleView dividerHandleView) {
            return Integer.valueOf(dividerHandleView.mCurrentHeight);
        }

        /* JADX DEBUG: Method merged with bridge method: set(Ljava/lang/Object;Ljava/lang/Object;)V */
        @Override // android.util.Property
        public void set(DividerHandleView dividerHandleView, Integer num) {
            dividerHandleView.mCurrentHeight = num.intValue();
            dividerHandleView.invalidate();
        }
    };

    public DividerHandleView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint();
        this.mPaint.setColor(getResources().getColor(R.color.docked_divider_handle, null));
        this.mPaint.setAntiAlias(true);
        this.mWidth = getResources().getDimensionPixelSize(R.dimen.docked_divider_handle_width);
        this.mHeight = getResources().getDimensionPixelSize(R.dimen.docked_divider_handle_height);
        this.mCurrentWidth = this.mWidth;
        this.mCurrentHeight = this.mHeight;
        this.mCircleDiameter = (this.mWidth + this.mHeight) / 3;
    }

    public void setTouching(boolean z, boolean z2) {
        if (z == this.mTouching) {
            return;
        }
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
            this.mAnimator = null;
        }
        if (!z2) {
            if (z) {
                this.mCurrentWidth = this.mCircleDiameter;
                this.mCurrentHeight = this.mCircleDiameter;
            } else {
                this.mCurrentWidth = this.mWidth;
                this.mCurrentHeight = this.mHeight;
            }
            invalidate();
        } else {
            animateToTarget(z ? this.mCircleDiameter : this.mWidth, z ? this.mCircleDiameter : this.mHeight, z);
        }
        this.mTouching = z;
    }

    private void animateToTarget(int i, int i2, boolean z) {
        long j;
        Interpolator interpolator;
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, WIDTH_PROPERTY, this.mCurrentWidth, i);
        ObjectAnimator objectAnimatorOfInt2 = ObjectAnimator.ofInt(this, HEIGHT_PROPERTY, this.mCurrentHeight, i2);
        this.mAnimator = new AnimatorSet();
        this.mAnimator.playTogether(objectAnimatorOfInt, objectAnimatorOfInt2);
        AnimatorSet animatorSet = this.mAnimator;
        if (z) {
            j = 150;
        } else {
            j = 200;
        }
        animatorSet.setDuration(j);
        AnimatorSet animatorSet2 = this.mAnimator;
        if (z) {
            interpolator = Interpolators.TOUCH_RESPONSE;
        } else {
            interpolator = Interpolators.FAST_OUT_SLOW_IN;
        }
        animatorSet2.setInterpolator(interpolator);
        this.mAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.stackdivider.DividerHandleView.3
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                DividerHandleView.this.mAnimator = null;
            }
        });
        this.mAnimator.start();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int width = (getWidth() / 2) - (this.mCurrentWidth / 2);
        int height = (getHeight() / 2) - (this.mCurrentHeight / 2);
        float fMin = Math.min(this.mCurrentWidth, this.mCurrentHeight) / 2;
        canvas.drawRoundRect(width, height, width + this.mCurrentWidth, height + this.mCurrentHeight, fMin, fMin, this.mPaint);
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
