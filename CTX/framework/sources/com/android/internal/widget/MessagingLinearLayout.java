package com.android.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.internal.R;

@RemoteViews.RemoteView
public class MessagingLinearLayout extends ViewGroup {
    private int mMaxDisplayedLines;
    private MessagingLayout mMessagingLayout;
    private int mSpacing;

    public MessagingLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMaxDisplayedLines = Integer.MAX_VALUE;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.MessagingLinearLayout, 0, 0);
        int indexCount = typedArrayObtainStyledAttributes.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            if (typedArrayObtainStyledAttributes.getIndex(i) == 0) {
                this.mSpacing = typedArrayObtainStyledAttributes.getDimensionPixelSize(i, 0);
            }
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int measuredType;
        int size = View.MeasureSpec.getSize(i2);
        if (View.MeasureSpec.getMode(i2) == 0) {
            size = Integer.MAX_VALUE;
        }
        int i3 = size;
        int i4 = this.mPaddingLeft + this.mPaddingRight;
        int childCount = getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            ((LayoutParams) getChildAt(i5).getLayoutParams()).hide = true;
        }
        int iMax = i4;
        int i6 = this.mPaddingTop + this.mPaddingBottom;
        int consumedLines = this.mMaxDisplayedLines;
        boolean z = true;
        for (int i7 = childCount - 1; i7 >= 0 && i6 < i3; i7--) {
            if (getChildAt(i7).getVisibility() != 8) {
                View childAt = getChildAt(i7);
                LayoutParams layoutParams = (LayoutParams) getChildAt(i7).getLayoutParams();
                MessagingChild messagingChild = null;
                int extraSpacing = this.mSpacing;
                if (childAt instanceof MessagingChild) {
                    messagingChild = (MessagingChild) childAt;
                    messagingChild.setMaxDisplayedLines(consumedLines);
                    extraSpacing += messagingChild.getExtraSpacing();
                }
                MessagingChild messagingChild2 = messagingChild;
                int i8 = z ? 0 : extraSpacing;
                measureChildWithMargins(childAt, i, 0, i2, ((i6 - this.mPaddingTop) - this.mPaddingBottom) + i8);
                int iMax2 = Math.max(i6, childAt.getMeasuredHeight() + i6 + layoutParams.topMargin + layoutParams.bottomMargin + i8);
                if (messagingChild2 != null) {
                    measuredType = messagingChild2.getMeasuredType();
                    consumedLines -= messagingChild2.getConsumedLines();
                } else {
                    measuredType = 0;
                }
                boolean z2 = measuredType == 1;
                boolean z3 = measuredType == 2;
                if (iMax2 > i3 || z3) {
                    break;
                }
                iMax = Math.max(iMax, childAt.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin + this.mPaddingLeft + this.mPaddingRight);
                layoutParams.hide = false;
                if (!z2 && consumedLines > 0) {
                    i6 = iMax2;
                    z = false;
                } else {
                    i6 = iMax2;
                    break;
                }
            }
        }
        setMeasuredDimension(resolveSize(Math.max(getSuggestedMinimumWidth(), iMax), i), Math.max(getSuggestedMinimumHeight(), i6));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        int i6 = this.mPaddingLeft;
        int i7 = (i3 - i) - this.mPaddingRight;
        int layoutDirection = getLayoutDirection();
        int childCount = getChildCount();
        int i8 = this.mPaddingTop;
        boolean zIsShown = isShown();
        int i9 = 1;
        int i10 = i8;
        boolean z2 = true;
        int i11 = 0;
        while (i11 < childCount) {
            View childAt = getChildAt(i11);
            if (childAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                MessagingChild messagingChild = (MessagingChild) childAt;
                int measuredWidth = childAt.getMeasuredWidth();
                int measuredHeight = childAt.getMeasuredHeight();
                int i12 = layoutDirection == i9 ? (i7 - measuredWidth) - layoutParams.rightMargin : i6 + layoutParams.leftMargin;
                if (layoutParams.hide) {
                    if (zIsShown && layoutParams.visibleBefore) {
                        childAt.layout(i12, i10, measuredWidth + i12, layoutParams.lastVisibleHeight + i10);
                        messagingChild.hideAnimated();
                    }
                    layoutParams.visibleBefore = false;
                } else {
                    i5 = 1;
                    layoutParams.visibleBefore = true;
                    layoutParams.lastVisibleHeight = measuredHeight;
                    if (!z2) {
                        i10 += this.mSpacing;
                    }
                    int i13 = i10 + layoutParams.topMargin;
                    childAt.layout(i12, i13, measuredWidth + i12, i13 + measuredHeight);
                    i10 = i13 + measuredHeight + layoutParams.bottomMargin;
                    z2 = false;
                    i11++;
                    i9 = i5;
                }
            }
            i5 = 1;
            i11++;
            i9 = i5;
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View view, long j) {
        if (((LayoutParams) view.getLayoutParams()).hide && !((MessagingChild) view).isHidingAnimated()) {
            return true;
        }
        return super.drawChild(canvas, view, j);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(this.mContext, attributeSet);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -2);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        LayoutParams layoutParams2 = new LayoutParams(layoutParams.width, layoutParams.height);
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            layoutParams2.copyMarginsFrom((ViewGroup.MarginLayoutParams) layoutParams);
        }
        return layoutParams2;
    }

    public static boolean isGone(View view) {
        if (view.getVisibility() == 8) {
            return true;
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        return (layoutParams instanceof LayoutParams) && ((LayoutParams) layoutParams).hide;
    }

    @RemotableViewMethod
    public void setMaxDisplayedLines(int i) {
        this.mMaxDisplayedLines = i;
    }

    public void setMessagingLayout(MessagingLayout messagingLayout) {
        this.mMessagingLayout = messagingLayout;
    }

    public MessagingLayout getMessagingLayout() {
        return this.mMessagingLayout;
    }

    public interface MessagingChild {
        public static final int MEASURED_NORMAL = 0;
        public static final int MEASURED_SHORTENED = 1;
        public static final int MEASURED_TOO_SMALL = 2;

        int getConsumedLines();

        int getMeasuredType();

        void hideAnimated();

        boolean isHidingAnimated();

        void setMaxDisplayedLines(int i);

        default int getExtraSpacing() {
            return 0;
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public boolean hide;
        public int lastVisibleHeight;
        public boolean visibleBefore;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.hide = false;
            this.visibleBefore = false;
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
            this.hide = false;
            this.visibleBefore = false;
        }
    }
}
