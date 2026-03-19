package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.internal.R;

@RemoteViews.RemoteView
@Deprecated
public class AbsoluteLayout extends ViewGroup {
    public AbsoluteLayout(Context context) {
        this(context, null);
    }

    public AbsoluteLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AbsoluteLayout(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AbsoluteLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int childCount = getChildCount();
        measureChildren(i, i2);
        int iMax = 0;
        int iMax2 = 0;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                int measuredWidth = layoutParams.x + childAt.getMeasuredWidth();
                int measuredHeight = layoutParams.y + childAt.getMeasuredHeight();
                iMax = Math.max(iMax, measuredWidth);
                iMax2 = Math.max(iMax2, measuredHeight);
            }
        }
        setMeasuredDimension(resolveSizeAndState(Math.max(iMax + this.mPaddingLeft + this.mPaddingRight, getSuggestedMinimumWidth()), i, 0), resolveSizeAndState(Math.max(iMax2 + this.mPaddingTop + this.mPaddingBottom, getSuggestedMinimumHeight()), i2, 0));
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2, 0, 0);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int childCount = getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = getChildAt(i5);
            if (childAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                int i6 = this.mPaddingLeft + layoutParams.x;
                int i7 = this.mPaddingTop + layoutParams.y;
                childAt.layout(i6, i7, childAt.getMeasuredWidth() + i6, childAt.getMeasuredHeight() + i7);
            }
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        public int x;
        public int y;

        public LayoutParams(int i, int i2, int i3, int i4) {
            super(i, i2);
            this.x = i3;
            this.y = i4;
        }

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.AbsoluteLayout_Layout);
            this.x = typedArrayObtainStyledAttributes.getDimensionPixelOffset(0, 0);
            this.y = typedArrayObtainStyledAttributes.getDimensionPixelOffset(1, 0);
            typedArrayObtainStyledAttributes.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
        }

        @Override
        public String debug(String str) {
            return str + "Absolute.LayoutParams={width=" + sizeToString(this.width) + ", height=" + sizeToString(this.height) + " x=" + this.x + " y=" + this.y + "}";
        }
    }
}
