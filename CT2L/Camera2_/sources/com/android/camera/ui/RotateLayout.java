package com.android.camera.ui;

import android.R;
import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.camera.debug.Log;

public class RotateLayout extends ViewGroup implements Rotatable {
    private static final Log.Tag TAG = new Log.Tag("RotateLayout");
    protected View mChild;
    private Matrix mMatrix;
    private int mOrientation;

    public RotateLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMatrix = new Matrix();
        setBackgroundResource(R.color.transparent);
    }

    @Override
    protected void onFinishInflate() {
        this.mChild = getChildAt(0);
        this.mChild.setPivotX(0.0f);
        this.mChild.setPivotY(0.0f);
    }

    @Override
    protected void onLayout(boolean change, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        switch (this.mOrientation) {
            case 0:
            case 180:
                this.mChild.layout(0, 0, width, height);
                break;
            case 90:
            case 270:
                this.mChild.layout(0, 0, height, width);
                break;
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = 0;
        int h = 0;
        switch (this.mOrientation) {
            case 0:
            case 180:
                measureChild(this.mChild, widthSpec, heightSpec);
                w = this.mChild.getMeasuredWidth();
                h = this.mChild.getMeasuredHeight();
                break;
            case 90:
            case 270:
                measureChild(this.mChild, heightSpec, widthSpec);
                w = this.mChild.getMeasuredHeight();
                h = this.mChild.getMeasuredWidth();
                break;
        }
        setMeasuredDimension(w, h);
        switch (this.mOrientation) {
            case 0:
                this.mChild.setTranslationX(0.0f);
                this.mChild.setTranslationY(0.0f);
                break;
            case 90:
                this.mChild.setTranslationX(0.0f);
                this.mChild.setTranslationY(h);
                break;
            case 180:
                this.mChild.setTranslationX(w);
                this.mChild.setTranslationY(h);
                break;
            case 270:
                this.mChild.setTranslationX(w);
                this.mChild.setTranslationY(0.0f);
                break;
        }
        this.mChild.setRotation(-this.mOrientation);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void setOrientation(int orientation, boolean animation) {
        int orientation2 = orientation % 360;
        if (this.mOrientation != orientation2) {
            this.mOrientation = orientation2;
            requestLayout();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }
}
