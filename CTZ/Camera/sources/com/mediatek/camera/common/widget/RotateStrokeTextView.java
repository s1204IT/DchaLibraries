package com.mediatek.camera.common.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import java.lang.reflect.Field;

public class RotateStrokeTextView extends TwoStateTextView implements Rotatable {
    private long mAnimationEndTime;
    private long mAnimationStartTime;
    private boolean mClockwise;
    private int mCurrentDegree;
    private boolean mEnableAnimation;
    private int mStartDegree;
    private float mStrokeWidth;
    private int mTargetDegree;
    private TextPaint mTextPaint;

    public RotateStrokeTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentDegree = 0;
        this.mStartDegree = 0;
        this.mTargetDegree = 0;
        this.mClockwise = false;
        this.mEnableAnimation = true;
        this.mAnimationStartTime = 0L;
        this.mAnimationEndTime = 0L;
        this.mStrokeWidth = dip2Px(context, 0.5f);
        this.mTextPaint = getPaint();
        setGravity(17);
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
        if (getText() == null) {
            return;
        }
        if (this.mCurrentDegree != this.mTargetDegree) {
            long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis();
            if (jCurrentAnimationTimeMillis < this.mAnimationEndTime) {
                int i = (int) (jCurrentAnimationTimeMillis - this.mAnimationStartTime);
                int i2 = this.mStartDegree;
                if (!this.mClockwise) {
                    i = -i;
                }
                int i3 = i2 + ((270 * i) / 1000);
                this.mCurrentDegree = i3 >= 0 ? i3 % 360 : (i3 % 360) + 360;
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
        canvas.translate(paddingLeft + (width / 2), paddingTop + (height / 2));
        canvas.rotate(-this.mCurrentDegree);
        canvas.translate((-getWidth()) / 2, (-getHeight()) / 2);
        setTextColorUseReflection(-16777216);
        this.mTextPaint.setStyle(Paint.Style.STROKE);
        this.mTextPaint.setAlpha(127);
        this.mTextPaint.setStrokeWidth(this.mStrokeWidth);
        super.onDraw(canvas);
        setTextColorUseReflection(-1);
        this.mTextPaint.setStrokeWidth(0.0f);
        this.mTextPaint.setStyle(Paint.Style.FILL);
        this.mTextPaint.setAlpha(255);
        super.onDraw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int iMax = Math.max(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(iMax, iMax);
    }

    private void setTextColorUseReflection(int i) {
        try {
            Field declaredField = TextView.class.getDeclaredField("mCurTextColor");
            declaredField.setAccessible(true);
            declaredField.set(this, Integer.valueOf(i));
            declaredField.setAccessible(false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
        } catch (NoSuchFieldException e3) {
            e3.printStackTrace();
        }
        this.mTextPaint.setColor(i);
    }

    private static float dip2Px(Context context, float f) {
        return f * context.getResources().getDisplayMetrics().density;
    }
}
