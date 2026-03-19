package com.mediatek.camera.common.widget;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;

public class RotateLayout extends ViewGroup implements Rotatable {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(RotateLayout.class.getSimpleName());
    protected View mChild;
    private OnSizeChangedListener mListener;
    private int mOrientation;

    public interface OnSizeChangedListener {
        void onSizeChanged(int i, int i2);
    }

    public RotateLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setBackgroundResource(R.color.transparent);
    }

    public RotateLayout(Context context) {
        super(context);
        setBackgroundResource(R.color.transparent);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mChild = getChildAt(0);
        this.mChild.setPivotX(0.0f);
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
        int measuredHeight;
        int measuredWidth = 0;
        if (this.mChild == null) {
            setMeasuredDimension(0, 0);
            return;
        }
        int i3 = this.mOrientation;
        if (i3 == 0) {
            measureChild(this.mChild, i, i2);
            measuredWidth = this.mChild.getMeasuredWidth();
            measuredHeight = this.mChild.getMeasuredHeight();
        } else if (i3 == 90) {
            measureChild(this.mChild, i2, i);
            measuredWidth = this.mChild.getMeasuredHeight();
            measuredHeight = this.mChild.getMeasuredWidth();
        } else if (i3 != 180) {
            if (i3 != 270) {
                measuredHeight = 0;
            }
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
        int i4 = this.mOrientation;
        if (i4 == 0) {
            this.mChild.setTranslationX(0.0f);
            this.mChild.setTranslationY(0.0f);
        } else if (i4 == 90) {
            this.mChild.setTranslationX(0.0f);
            this.mChild.setTranslationY(measuredHeight);
        } else if (i4 == 180) {
            this.mChild.setTranslationX(measuredWidth);
            this.mChild.setTranslationY(measuredHeight);
        } else if (i4 == 270) {
            this.mChild.setTranslationX(measuredWidth);
            this.mChild.setTranslationY(0.0f);
        }
        this.mChild.setRotation(-this.mOrientation);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void setOrientation(int i, boolean z) {
        if (i != 0 && i != 90 && i != 180 && i != 270) {
            LogHelper.w(TAG, "setOrientation : Not support orientation = " + i);
            return;
        }
        int i2 = i % 360;
        if (this.mOrientation == i2) {
            return;
        }
        this.mOrientation = i2;
        requestLayout();
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setOnSizeChangedListener(OnSizeChangedListener onSizeChangedListener) {
        this.mListener = onSizeChangedListener;
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        if (this.mListener != null) {
            this.mListener.onSizeChanged(i, i2);
        }
    }
}
