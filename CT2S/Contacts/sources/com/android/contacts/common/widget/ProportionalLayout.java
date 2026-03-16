package com.android.contacts.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.common.R;

public class ProportionalLayout extends ViewGroup {
    private Direction mDirection;
    private float mRatio;

    public enum Direction {
        widthToHeight("widthToHeight"),
        heightToWidth("heightToWidth");

        public final String XmlName;

        Direction(String xmlName) {
            this.XmlName = xmlName;
        }

        public static Direction parse(String value) {
            if (widthToHeight.XmlName.equals(value)) {
                return widthToHeight;
            }
            if (heightToWidth.XmlName.equals(value)) {
                return heightToWidth;
            }
            throw new IllegalStateException("direction must be either " + widthToHeight.XmlName + " or " + heightToWidth.XmlName);
        }
    }

    public ProportionalLayout(Context context) {
        super(context);
    }

    public ProportionalLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs);
    }

    public ProportionalLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initFromAttributes(context, attrs);
    }

    private void initFromAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProportionalLayout);
        this.mDirection = Direction.parse(a.getString(0));
        this.mRatio = a.getFloat(1, 1.0f);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        if (getChildCount() != 1) {
            throw new IllegalStateException("ProportionalLayout requires exactly one child");
        }
        View child = getChildAt(0);
        measureChild(child, widthMeasureSpec, heightMeasureSpec);
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        if (this.mDirection == Direction.heightToWidth) {
            width = Math.round(childHeight * this.mRatio);
            height = childHeight;
        } else {
            width = childWidth;
            height = Math.round(childWidth * this.mRatio);
        }
        measureChild(child, View.MeasureSpec.makeMeasureSpec(width, 1073741824), View.MeasureSpec.makeMeasureSpec(height, 1073741824));
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() != 1) {
            throw new IllegalStateException("ProportionalLayout requires exactly one child");
        }
        View child = getChildAt(0);
        child.layout(0, 0, right - left, bottom - top);
    }
}
