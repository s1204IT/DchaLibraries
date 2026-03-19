package com.mediatek.camera.common.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class RotateImageView extends AppCompatImageView implements Rotatable {
    private long mAnimationEndTime;
    private long mAnimationStartTime;
    private boolean mClockwise;
    private int mCurrentDegree;
    private Bitmap mDrawableBitmap;
    private boolean mEnableAnimation;
    private int mStartDegree;
    private int mTargetDegree;

    public RotateImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentDegree = 0;
        this.mStartDegree = 0;
        this.mTargetDegree = 0;
        this.mClockwise = false;
        this.mEnableAnimation = true;
        this.mAnimationStartTime = 0L;
        this.mAnimationEndTime = 0L;
    }

    public RotateImageView(Context context) {
        this(context, null);
    }

    @Override
    public void setOrientation(int i, boolean z) {
        this.mEnableAnimation = z;
        int i2 = i >= 0 ? i % 360 : (i % 360) + 360;
        if (i2 == this.mTargetDegree) {
            return;
        }
        this.mTargetDegree = i2;
        if (this.mEnableAnimation) {
            this.mStartDegree = this.mCurrentDegree;
            this.mAnimationStartTime = AnimationUtils.currentAnimationTimeMillis();
            int i3 = this.mTargetDegree - this.mCurrentDegree;
            if (i3 < 0) {
                i3 += 360;
            }
            if (i3 > 180) {
                i3 -= 360;
            }
            this.mClockwise = i3 >= 0;
            this.mAnimationEndTime = this.mAnimationStartTime + ((long) ((Math.abs(i3) * 1000) / 270));
        } else {
            this.mCurrentDegree = this.mTargetDegree;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        ?? drawable = getDrawable();
        if (drawable == 0) {
            return;
        }
        Rect bounds = drawable.getBounds();
        int i = bounds.right - bounds.left;
        int i2 = bounds.bottom - bounds.top;
        if (i == 0 || i2 == 0) {
            return;
        }
        if (Build.VERSION.SDK_INT == 23 && (drawable instanceof VectorDrawable)) {
            this.mDrawableBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas2 = new Canvas(this.mDrawableBitmap);
            drawable.setBounds(0, 0, canvas2.getWidth(), canvas2.getHeight());
            drawable.draw(canvas2);
        }
        if (this.mCurrentDegree != this.mTargetDegree) {
            long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis();
            if (jCurrentAnimationTimeMillis < this.mAnimationEndTime) {
                int i3 = (int) (jCurrentAnimationTimeMillis - this.mAnimationStartTime);
                int i4 = this.mStartDegree;
                if (!this.mClockwise) {
                    i3 = -i3;
                }
                int i5 = i4 + ((270 * i3) / 1000);
                this.mCurrentDegree = i5 >= 0 ? i5 % 360 : (i5 % 360) + 360;
                invalidate();
            } else {
                this.mCurrentDegree = this.mTargetDegree;
            }
        }
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int width = (getWidth() - paddingLeft) - paddingRight;
        int height = (getHeight() - paddingTop) - paddingBottom;
        int saveCount = canvas.getSaveCount();
        if (getScaleType() == ImageView.ScaleType.FIT_CENTER && (width < i || height < i2)) {
            float f = width;
            float f2 = height;
            float fMin = Math.min(f / i, f2 / i2);
            canvas.scale(fMin, fMin, f / 2.0f, f2 / 2.0f);
        }
        canvas.translate(paddingLeft + (width / 2), paddingTop + (height / 2));
        canvas.rotate(-this.mCurrentDegree);
        canvas.translate((-i) / 2, (-i2) / 2);
        if (this.mDrawableBitmap != null) {
            canvas.drawBitmap(this.mDrawableBitmap, 0.0f, 0.0f, (Paint) null);
        } else {
            drawable.draw(canvas);
        }
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mDrawableBitmap != null) {
            this.mDrawableBitmap.recycle();
            this.mDrawableBitmap = null;
        }
    }
}
