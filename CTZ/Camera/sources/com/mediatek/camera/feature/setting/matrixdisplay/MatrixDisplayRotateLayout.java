package com.mediatek.camera.feature.setting.matrixdisplay;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.widget.Rotatable;

public class MatrixDisplayRotateLayout extends ViewGroup implements Rotatable {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MatrixDisplayRotateLayout.class.getSimpleName());
    private View mChild;
    private int mOrientation;

    public MatrixDisplayRotateLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setBackgroundResource(R.color.transparent);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mChild = getChildAt(0);
        this.mChild.setPivotX(0.0f);
        this.mChild.setPivotY(0.0f);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        View view = this.mChild;
        view.layout(0, 0, i4 - i2, i3 - i);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        measureChild(this.mChild, i2, i);
        int measuredHeight = this.mChild.getMeasuredHeight();
        setMeasuredDimension(measuredHeight, this.mChild.getMeasuredWidth());
        this.mChild.setTranslationX(measuredHeight);
        this.mChild.setTranslationY(0.0f);
        this.mChild.setRotation(-270.0f);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void setOrientation(int i, boolean z) {
        LogHelper.d(TAG, "[setOrientation]orientation = " + i + ",mOrientation = " + this.mOrientation + ",animation=" + z);
        int i2 = ((i + (-270)) + 360) % 360;
        if (this.mOrientation != i2) {
            this.mOrientation = i2;
            setOrientation(this.mChild, this.mOrientation, z);
            requestLayout();
        }
    }

    private static void setOrientation(View view, int i, boolean z) {
        if (view == 0) {
            LogHelper.d(TAG, "[setOrientation]view is null,return.");
            return;
        }
        if (view instanceof Rotatable) {
            ((Rotatable) view).setOrientation(i, z);
            return;
        }
        if (view instanceof ViewGroup) {
            int childCount = view.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                setOrientation(view.getChildAt(i2), i, z);
            }
        }
    }
}
