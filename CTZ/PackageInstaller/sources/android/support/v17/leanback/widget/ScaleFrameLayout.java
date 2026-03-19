package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class ScaleFrameLayout extends FrameLayout {
    private float mChildScale;
    private float mLayoutScaleX;
    private float mLayoutScaleY;

    public ScaleFrameLayout(Context context) {
        this(context, null);
    }

    public ScaleFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLayoutScaleX = 1.0f;
        this.mLayoutScaleY = 1.0f;
        this.mChildScale = 1.0f;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        child.setScaleX(this.mChildScale);
        child.setScaleY(this.mChildScale);
    }

    @Override
    protected boolean addViewInLayout(View child, int index, ViewGroup.LayoutParams params, boolean preventRequestLayout) {
        boolean ret = super.addViewInLayout(child, index, params, preventRequestLayout);
        if (ret) {
            child.setScaleX(this.mChildScale);
            child.setScaleY(this.mChildScale);
        }
        return ret;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        float pivotX;
        int parentLeft;
        int parentRight;
        int parentTop;
        int parentBottom;
        int childLeft;
        int childTop;
        ScaleFrameLayout scaleFrameLayout = this;
        int count = getChildCount();
        int layoutDirection = getLayoutDirection();
        if (layoutDirection == 1) {
            pivotX = getWidth() - getPivotX();
        } else {
            pivotX = getPivotX();
        }
        if (scaleFrameLayout.mLayoutScaleX != 1.0f) {
            parentLeft = getPaddingLeft() + ((int) ((pivotX - (pivotX / scaleFrameLayout.mLayoutScaleX)) + 0.5f));
            parentRight = ((int) (((((right - left) - pivotX) / scaleFrameLayout.mLayoutScaleX) + pivotX) + 0.5f)) - getPaddingRight();
        } else {
            parentLeft = getPaddingLeft();
            parentRight = (right - left) - getPaddingRight();
        }
        float pivotY = getPivotY();
        if (scaleFrameLayout.mLayoutScaleY != 1.0f) {
            parentTop = getPaddingTop() + ((int) ((pivotY - (pivotY / scaleFrameLayout.mLayoutScaleY)) + 0.5f));
            parentBottom = ((int) (((((bottom - top) - pivotY) / scaleFrameLayout.mLayoutScaleY) + pivotY) + 0.5f)) - getPaddingBottom();
        } else {
            parentTop = getPaddingTop();
            parentBottom = (bottom - top) - getPaddingBottom();
        }
        int i = 0;
        while (i < count) {
            View child = scaleFrameLayout.getChildAt(i);
            if (child.getVisibility() != 8) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = 8388659;
                }
                int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                int verticalGravity = gravity & 112;
                int gravity2 = absoluteGravity & 7;
                if (gravity2 == 1) {
                    int childLeft2 = parentRight - parentLeft;
                    childLeft = ((((childLeft2 - width) / 2) + parentLeft) + lp.leftMargin) - lp.rightMargin;
                } else if (gravity2 == 5) {
                    childLeft = (parentRight - width) - lp.rightMargin;
                } else {
                    childLeft = lp.leftMargin + parentLeft;
                }
                if (verticalGravity == 16) {
                    childTop = (((((parentBottom - parentTop) - height) / 2) + parentTop) + lp.topMargin) - lp.bottomMargin;
                } else if (verticalGravity != 48 && verticalGravity == 80) {
                    childTop = (parentBottom - height) - lp.bottomMargin;
                } else {
                    childTop = lp.topMargin + parentTop;
                }
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
                child.setPivotX(pivotX - childLeft);
                child.setPivotY(pivotY - childTop);
            }
            i++;
            scaleFrameLayout = this;
        }
    }

    private static int getScaledMeasureSpec(int measureSpec, float scale) {
        return scale == 1.0f ? measureSpec : View.MeasureSpec.makeMeasureSpec((int) ((View.MeasureSpec.getSize(measureSpec) / scale) + 0.5f), View.MeasureSpec.getMode(measureSpec));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mLayoutScaleX != 1.0f || this.mLayoutScaleY != 1.0f) {
            int scaledWidthMeasureSpec = getScaledMeasureSpec(widthMeasureSpec, this.mLayoutScaleX);
            int scaledHeightMeasureSpec = getScaledMeasureSpec(heightMeasureSpec, this.mLayoutScaleY);
            super.onMeasure(scaledWidthMeasureSpec, scaledHeightMeasureSpec);
            setMeasuredDimension((int) ((getMeasuredWidth() * this.mLayoutScaleX) + 0.5f), (int) ((getMeasuredHeight() * this.mLayoutScaleY) + 0.5f));
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setForeground(Drawable d) {
        throw new UnsupportedOperationException();
    }
}
