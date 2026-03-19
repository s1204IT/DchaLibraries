package com.mediatek.camera.ui.modepicker;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.widget.Rotatable;

public class RotateLayout extends ViewGroup implements Rotatable {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(RotateLayout.class.getSimpleName());
    private long mAnimationEndTime;
    private long mAnimationStartTime;
    private View mChild;
    private boolean mClockwise;
    private int mCurrentDegree;
    private boolean mEnableAnimation;
    private OnSizeChangedListener mListener;
    private int mOrientation;
    private int mStartDegree;
    private int mTargetDegree;

    public interface OnSizeChangedListener {
        void onSizeChanged(int i, int i2);
    }

    public RotateLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentDegree = 0;
        this.mStartDegree = 0;
        this.mTargetDegree = 0;
        this.mClockwise = false;
        this.mEnableAnimation = true;
        this.mAnimationStartTime = 0L;
        this.mAnimationEndTime = 0L;
        setBackgroundResource(R.color.transparent);
    }

    public RotateLayout(Context context) {
        super(context);
        this.mCurrentDegree = 0;
        this.mStartDegree = 0;
        this.mTargetDegree = 0;
        this.mClockwise = false;
        this.mEnableAnimation = true;
        this.mAnimationStartTime = 0L;
        this.mAnimationEndTime = 0L;
        setBackgroundResource(R.color.transparent);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mChild = getChildAt(0);
        this.mChild.setPivotY(0.0f);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.mChild == null) {
            return;
        }
        int i5 = i3 - i;
        int i6 = i4 - i2;
        int i7 = this.mOrientation;
        if (i7 != 0) {
            if (i7 != 90) {
                if (i7 != 180) {
                    if (i7 != 270) {
                        return;
                    }
                }
            }
            this.mChild.layout(0, 0, i6, i5);
            return;
        }
        this.mChild.layout(0, 0, i5, i6);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        measureChild(this.mChild, i, i2);
        int iMax = Math.max(this.mChild.getMeasuredWidth(), this.mChild.getMeasuredHeight());
        setMeasuredDimension(iMax, iMax);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void setOrientation(int i, boolean z) {
        if (i != 0 && i != 90 && i != 180 && i != 270) {
            LogHelper.w(TAG, "setOrientation : Not support degree = " + i);
            return;
        }
        this.mEnableAnimation = z;
        int i2 = i >= 0 ? i % 360 : (i % 360) + 360;
        if (i2 == this.mTargetDegree) {
            return;
        }
        this.mTargetDegree = i2;
        this.mOrientation = i2;
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
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        if (this.mListener != null) {
            this.mListener.onSizeChanged(i, i2);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
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
        super.onDraw(canvas);
        canvas.restoreToCount(saveCount);
    }
}
