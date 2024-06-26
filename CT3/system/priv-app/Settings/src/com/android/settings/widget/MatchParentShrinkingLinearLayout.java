package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import com.android.internal.R;
/* loaded from: classes.dex */
public class MatchParentShrinkingLinearLayout extends ViewGroup {
    @ViewDebug.ExportedProperty(category = "layout")
    private boolean mBaselineAligned;
    @ViewDebug.ExportedProperty(category = "layout")
    private int mBaselineAlignedChildIndex;
    @ViewDebug.ExportedProperty(category = "measurement")
    private int mBaselineChildTop;
    private Drawable mDivider;
    private int mDividerHeight;
    private int mDividerPadding;
    private int mDividerWidth;
    @ViewDebug.ExportedProperty(category = "measurement", flagMapping = {@ViewDebug.FlagToString(equals = -1, mask = -1, name = "NONE"), @ViewDebug.FlagToString(equals = 0, mask = 0, name = "NONE"), @ViewDebug.FlagToString(equals = 48, mask = 48, name = "TOP"), @ViewDebug.FlagToString(equals = 80, mask = 80, name = "BOTTOM"), @ViewDebug.FlagToString(equals = 3, mask = 3, name = "LEFT"), @ViewDebug.FlagToString(equals = 5, mask = 5, name = "RIGHT"), @ViewDebug.FlagToString(equals = 8388611, mask = 8388611, name = "START"), @ViewDebug.FlagToString(equals = 8388613, mask = 8388613, name = "END"), @ViewDebug.FlagToString(equals = 16, mask = 16, name = "CENTER_VERTICAL"), @ViewDebug.FlagToString(equals = 112, mask = 112, name = "FILL_VERTICAL"), @ViewDebug.FlagToString(equals = 1, mask = 1, name = "CENTER_HORIZONTAL"), @ViewDebug.FlagToString(equals = 7, mask = 7, name = "FILL_HORIZONTAL"), @ViewDebug.FlagToString(equals = 17, mask = 17, name = "CENTER"), @ViewDebug.FlagToString(equals = 119, mask = 119, name = "FILL"), @ViewDebug.FlagToString(equals = 8388608, mask = 8388608, name = "RELATIVE")}, formatToHexString = true)
    private int mGravity;
    private int mLayoutDirection;
    private int[] mMaxAscent;
    private int[] mMaxDescent;
    @ViewDebug.ExportedProperty(category = "measurement")
    private int mOrientation;
    private int mShowDividers;
    @ViewDebug.ExportedProperty(category = "measurement")
    private int mTotalLength;
    @ViewDebug.ExportedProperty(category = "layout")
    private boolean mUseLargestChild;
    @ViewDebug.ExportedProperty(category = "layout")
    private float mWeightSum;

    public MatchParentShrinkingLinearLayout(Context context) {
        this(context, null);
    }

    public MatchParentShrinkingLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MatchParentShrinkingLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MatchParentShrinkingLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mBaselineAligned = true;
        this.mBaselineAlignedChildIndex = -1;
        this.mBaselineChildTop = 0;
        this.mGravity = 8388659;
        this.mLayoutDirection = -1;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LinearLayout, defStyleAttr, defStyleRes);
        int index = a.getInt(1, -1);
        if (index >= 0) {
            setOrientation(index);
        }
        int index2 = a.getInt(0, -1);
        if (index2 >= 0) {
            setGravity(index2);
        }
        boolean baselineAligned = a.getBoolean(2, true);
        if (!baselineAligned) {
            setBaselineAligned(baselineAligned);
        }
        this.mWeightSum = a.getFloat(4, -1.0f);
        this.mBaselineAlignedChildIndex = a.getInt(3, -1);
        this.mUseLargestChild = a.getBoolean(6, false);
        setDividerDrawable(a.getDrawable(5));
        this.mShowDividers = a.getInt(7, 0);
        this.mDividerPadding = a.getDimensionPixelSize(8, 0);
        a.recycle();
    }

    @Override // android.view.ViewGroup
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void setDividerDrawable(Drawable divider) {
        if (divider == this.mDivider) {
            return;
        }
        this.mDivider = divider;
        if (divider != null) {
            this.mDividerWidth = divider.getIntrinsicWidth();
            this.mDividerHeight = divider.getIntrinsicHeight();
        } else {
            this.mDividerWidth = 0;
            this.mDividerHeight = 0;
        }
        setWillNotDraw(divider == null);
        requestLayout();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.mDivider == null) {
            return;
        }
        if (this.mOrientation == 1) {
            drawDividersVertical(canvas);
        } else {
            drawDividersHorizontal(canvas);
        }
    }

    void drawDividersVertical(Canvas canvas) {
        int bottom;
        int count = getVirtualChildCount();
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != 8 && hasDividerBeforeChildAt(i)) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int top = (child.getTop() - lp.topMargin) - this.mDividerHeight;
                drawHorizontalDivider(canvas, top);
            }
        }
        if (!hasDividerBeforeChildAt(count)) {
            return;
        }
        View child2 = getVirtualChildAt(count - 1);
        if (child2 == null) {
            bottom = (getHeight() - getPaddingBottom()) - this.mDividerHeight;
        } else {
            LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
            bottom = child2.getBottom() + lp2.bottomMargin;
        }
        drawHorizontalDivider(canvas, bottom);
    }

    void drawDividersHorizontal(Canvas canvas) {
        int position;
        int position2;
        int count = getVirtualChildCount();
        boolean isLayoutRtl = isLayoutRtl();
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != 8 && hasDividerBeforeChildAt(i)) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (isLayoutRtl) {
                    position2 = child.getRight() + lp.rightMargin;
                } else {
                    position2 = (child.getLeft() - lp.leftMargin) - this.mDividerWidth;
                }
                drawVerticalDivider(canvas, position2);
            }
        }
        if (!hasDividerBeforeChildAt(count)) {
            return;
        }
        View child2 = getVirtualChildAt(count - 1);
        if (child2 == null) {
            if (isLayoutRtl) {
                position = getPaddingLeft();
            } else {
                position = (getWidth() - getPaddingRight()) - this.mDividerWidth;
            }
        } else {
            LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
            if (isLayoutRtl) {
                position = (child2.getLeft() - lp2.leftMargin) - this.mDividerWidth;
            } else {
                position = child2.getRight() + lp2.rightMargin;
            }
        }
        drawVerticalDivider(canvas, position);
    }

    void drawHorizontalDivider(Canvas canvas, int top) {
        this.mDivider.setBounds(getPaddingLeft() + this.mDividerPadding, top, (getWidth() - getPaddingRight()) - this.mDividerPadding, this.mDividerHeight + top);
        this.mDivider.draw(canvas);
    }

    void drawVerticalDivider(Canvas canvas, int left) {
        this.mDivider.setBounds(left, getPaddingTop() + this.mDividerPadding, this.mDividerWidth + left, (getHeight() - getPaddingBottom()) - this.mDividerPadding);
        this.mDivider.draw(canvas);
    }

    @RemotableViewMethod
    public void setBaselineAligned(boolean baselineAligned) {
        this.mBaselineAligned = baselineAligned;
    }

    @Override // android.view.View
    public int getBaseline() {
        int majorGravity;
        if (this.mBaselineAlignedChildIndex < 0) {
            return super.getBaseline();
        }
        if (getChildCount() <= this.mBaselineAlignedChildIndex) {
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout set to an index that is out of bounds.");
        }
        View child = getChildAt(this.mBaselineAlignedChildIndex);
        int childBaseline = child.getBaseline();
        if (childBaseline == -1) {
            if (this.mBaselineAlignedChildIndex == 0) {
                return -1;
            }
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout points to a View that doesn't know how to get its baseline.");
        }
        int childTop = this.mBaselineChildTop;
        if (this.mOrientation == 1 && (majorGravity = this.mGravity & 112) != 48) {
            switch (majorGravity) {
                case 16:
                    childTop += ((((this.mBottom - this.mTop) - this.mPaddingTop) - this.mPaddingBottom) - this.mTotalLength) / 2;
                    break;
                case 80:
                    childTop = ((this.mBottom - this.mTop) - this.mPaddingBottom) - this.mTotalLength;
                    break;
            }
        }
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        return lp.topMargin + childTop + childBaseline;
    }

    View getVirtualChildAt(int index) {
        return getChildAt(index);
    }

    int getVirtualChildCount() {
        return getChildCount();
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mOrientation == 1) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    protected boolean hasDividerBeforeChildAt(int childIndex) {
        if (childIndex == 0) {
            return (this.mShowDividers & 1) != 0;
        } else if (childIndex == getChildCount()) {
            return (this.mShowDividers & 4) != 0;
        } else if ((this.mShowDividers & 2) != 0) {
            for (int i = childIndex - 1; i >= 0; i--) {
                if (getChildAt(i).getVisibility() != 8) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        this.mTotalLength = 0;
        int maxWidth = 0;
        int childState = 0;
        int alternativeMaxWidth = 0;
        int weightedMaxWidth = 0;
        boolean allFillParent = true;
        float totalWeight = 0.0f;
        int count = getVirtualChildCount();
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        boolean matchWidth = false;
        boolean skippedMeasure = false;
        int baselineChildIndex = this.mBaselineAlignedChildIndex;
        boolean useLargestChild = this.mUseLargestChild;
        int largestChildHeight = Integer.MIN_VALUE;
        int i = 0;
        while (i < count) {
            View child = getVirtualChildAt(i);
            if (child == null) {
                this.mTotalLength += measureNullChild(i);
            } else if (child.getVisibility() == 8) {
                i += getChildrenSkipCount(child, i);
            } else {
                if (hasDividerBeforeChildAt(i)) {
                    this.mTotalLength += this.mDividerHeight;
                }
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                totalWeight += lp.weight;
                if (heightMode == 1073741824 && lp.height == 0 && lp.weight > 0.0f) {
                    int totalLength = this.mTotalLength;
                    this.mTotalLength = Math.max(totalLength, lp.topMargin + totalLength + lp.bottomMargin);
                    skippedMeasure = true;
                } else {
                    int oldHeight = Integer.MIN_VALUE;
                    if (lp.height == 0 && lp.weight > 0.0f) {
                        oldHeight = 0;
                        lp.height = -2;
                    }
                    measureChildBeforeLayout(child, i, widthMeasureSpec, 0, heightMeasureSpec, totalWeight == 0.0f ? this.mTotalLength : 0);
                    if (oldHeight != Integer.MIN_VALUE) {
                        lp.height = oldHeight;
                    }
                    int childHeight = child.getMeasuredHeight();
                    int totalLength2 = this.mTotalLength;
                    this.mTotalLength = Math.max(totalLength2, totalLength2 + childHeight + lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));
                    if (useLargestChild) {
                        largestChildHeight = Math.max(childHeight, largestChildHeight);
                    }
                }
                if (baselineChildIndex >= 0 && baselineChildIndex == i + 1) {
                    this.mBaselineChildTop = this.mTotalLength;
                }
                if (i < baselineChildIndex && lp.weight > 0.0f) {
                    throw new RuntimeException("A child of LinearLayout with index less than mBaselineAlignedChildIndex has weight > 0, which won't work.  Either remove the weight, or don't set mBaselineAlignedChildIndex.");
                }
                boolean matchWidthLocally = false;
                if (widthMode != 1073741824 && lp.width == -1) {
                    matchWidth = true;
                    matchWidthLocally = true;
                }
                int margin = lp.leftMargin + lp.rightMargin;
                int measuredWidth = child.getMeasuredWidth() + margin;
                maxWidth = Math.max(maxWidth, measuredWidth);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
                allFillParent = allFillParent && lp.width == -1;
                if (lp.weight > 0.0f) {
                    if (!matchWidthLocally) {
                        margin = measuredWidth;
                    }
                    weightedMaxWidth = Math.max(weightedMaxWidth, margin);
                } else {
                    if (!matchWidthLocally) {
                        margin = measuredWidth;
                    }
                    alternativeMaxWidth = Math.max(alternativeMaxWidth, margin);
                }
                i += getChildrenSkipCount(child, i);
            }
            i++;
        }
        if (this.mTotalLength > 0 && hasDividerBeforeChildAt(count)) {
            this.mTotalLength += this.mDividerHeight;
        }
        if (useLargestChild && (heightMode == Integer.MIN_VALUE || heightMode == 0)) {
            this.mTotalLength = 0;
            int i2 = 0;
            while (i2 < count) {
                View child2 = getVirtualChildAt(i2);
                if (child2 == null) {
                    this.mTotalLength += measureNullChild(i2);
                } else if (child2.getVisibility() == 8) {
                    i2 += getChildrenSkipCount(child2, i2);
                } else {
                    LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                    int totalLength3 = this.mTotalLength;
                    this.mTotalLength = Math.max(totalLength3, totalLength3 + largestChildHeight + lp2.topMargin + lp2.bottomMargin + getNextLocationOffset(child2));
                }
                i2++;
            }
        }
        this.mTotalLength += this.mPaddingTop + this.mPaddingBottom;
        int heightSize = this.mTotalLength;
        int heightSizeAndState = resolveSizeAndState(Math.max(heightSize, getSuggestedMinimumHeight()), heightMeasureSpec, 0);
        int heightSize2 = heightSizeAndState & 16777215;
        int delta = heightSize2 - this.mTotalLength;
        if (skippedMeasure || (delta != 0 && totalWeight > 0.0f)) {
            float weightSum = this.mWeightSum > 0.0f ? this.mWeightSum : totalWeight;
            this.mTotalLength = 0;
            for (int i3 = 0; i3 < count; i3++) {
                View child3 = getVirtualChildAt(i3);
                if (child3.getVisibility() != 8) {
                    LayoutParams lp3 = (LayoutParams) child3.getLayoutParams();
                    float childExtra = lp3.weight;
                    if (childExtra > 0.0f && delta > 0) {
                        int share = (int) ((delta * childExtra) / weightSum);
                        weightSum -= childExtra;
                        delta -= share;
                        int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, this.mPaddingLeft + this.mPaddingRight + lp3.leftMargin + lp3.rightMargin, lp3.width);
                        if (lp3.height == 0 && heightMode == 1073741824) {
                            if (share <= 0) {
                                share = 0;
                            }
                            child3.measure(childWidthMeasureSpec, View.MeasureSpec.makeMeasureSpec(share, 1073741824));
                        } else {
                            int childHeight2 = child3.getMeasuredHeight() + share;
                            if (childHeight2 < 0) {
                                childHeight2 = 0;
                            }
                            child3.measure(childWidthMeasureSpec, View.MeasureSpec.makeMeasureSpec(childHeight2, 1073741824));
                        }
                        childState = combineMeasuredStates(childState, child3.getMeasuredState() & (-256));
                    } else if (delta < 0 && lp3.height == -1) {
                        int childWidthMeasureSpec2 = getChildMeasureSpec(widthMeasureSpec, this.mPaddingLeft + this.mPaddingRight + lp3.leftMargin + lp3.rightMargin, lp3.width);
                        int childHeight3 = child3.getMeasuredHeight() + delta;
                        if (childHeight3 < 0) {
                            childHeight3 = 0;
                        }
                        delta -= childHeight3 - child3.getMeasuredHeight();
                        child3.measure(childWidthMeasureSpec2, View.MeasureSpec.makeMeasureSpec(childHeight3, 1073741824));
                        childState = combineMeasuredStates(childState, child3.getMeasuredState() & (-256));
                    }
                    int margin2 = lp3.leftMargin + lp3.rightMargin;
                    int measuredWidth2 = child3.getMeasuredWidth() + margin2;
                    maxWidth = Math.max(maxWidth, measuredWidth2);
                    if (!(widthMode != 1073741824 ? lp3.width == -1 : false)) {
                        margin2 = measuredWidth2;
                    }
                    alternativeMaxWidth = Math.max(alternativeMaxWidth, margin2);
                    allFillParent = allFillParent && lp3.width == -1;
                    int totalLength4 = this.mTotalLength;
                    this.mTotalLength = Math.max(totalLength4, child3.getMeasuredHeight() + totalLength4 + lp3.topMargin + lp3.bottomMargin + getNextLocationOffset(child3));
                }
            }
            this.mTotalLength += this.mPaddingTop + this.mPaddingBottom;
        } else {
            alternativeMaxWidth = Math.max(alternativeMaxWidth, weightedMaxWidth);
            if (useLargestChild && heightMode != 1073741824) {
                for (int i4 = 0; i4 < count; i4++) {
                    View child4 = getVirtualChildAt(i4);
                    if (child4 != null && child4.getVisibility() != 8 && ((LayoutParams) child4.getLayoutParams()).weight > 0.0f) {
                        child4.measure(View.MeasureSpec.makeMeasureSpec(child4.getMeasuredWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(largestChildHeight, 1073741824));
                    }
                }
            }
        }
        if (!allFillParent && widthMode != 1073741824) {
            maxWidth = alternativeMaxWidth;
        }
        setMeasuredDimension(resolveSizeAndState(Math.max(maxWidth + this.mPaddingLeft + this.mPaddingRight, getSuggestedMinimumWidth()), widthMeasureSpec, childState), heightSizeAndState);
        if (matchWidth) {
            forceUniformWidth(count, heightMeasureSpec);
        }
    }

    private void forceUniformWidth(int count, int heightMeasureSpec) {
        int uniformMeasureSpec = View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.width == -1) {
                    int oldHeight = lp.height;
                    lp.height = child.getMeasuredHeight();
                    measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasureSpec, 0);
                    lp.height = oldHeight;
                }
            }
        }
    }

    void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        throw new IllegalStateException("horizontal mode not supported.");
    }

    int getChildrenSkipCount(View child, int index) {
        return 0;
    }

    int measureNullChild(int childIndex) {
        return 0;
    }

    void measureChildBeforeLayout(View child, int childIndex, int widthMeasureSpec, int totalWidth, int heightMeasureSpec, int totalHeight) {
        measureChildWithMargins(child, widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
    }

    int getLocationOffset(View child) {
        return 0;
    }

    int getNextLocationOffset(View child) {
        return 0;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (this.mOrientation == 1) {
            layoutVertical(l, t, r, b);
        } else {
            layoutHorizontal(l, t, r, b);
        }
    }

    void layoutVertical(int left, int top, int right, int bottom) {
        int childTop;
        int childLeft;
        int paddingLeft = this.mPaddingLeft;
        int width = right - left;
        int childRight = width - this.mPaddingRight;
        int childSpace = (width - paddingLeft) - this.mPaddingRight;
        int count = getVirtualChildCount();
        int majorGravity = this.mGravity & 112;
        int minorGravity = this.mGravity & 8388615;
        switch (majorGravity) {
            case 16:
                childTop = this.mPaddingTop + (((bottom - top) - this.mTotalLength) / 2);
                break;
            case 80:
                childTop = ((this.mPaddingTop + bottom) - top) - this.mTotalLength;
                break;
            default:
                childTop = this.mPaddingTop;
                break;
        }
        int i = 0;
        while (i < count) {
            View child = getVirtualChildAt(i);
            if (child == null) {
                childTop += measureNullChild(i);
            } else if (child.getVisibility() != 8) {
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int gravity = lp.gravity;
                if (gravity < 0) {
                    gravity = minorGravity;
                }
                int layoutDirection = getLayoutDirection();
                int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                switch (absoluteGravity & 7) {
                    case 1:
                        childLeft = ((((childSpace - childWidth) / 2) + paddingLeft) + lp.leftMargin) - lp.rightMargin;
                        break;
                    case 5:
                        childLeft = (childRight - childWidth) - lp.rightMargin;
                        break;
                    default:
                        childLeft = paddingLeft + lp.leftMargin;
                        break;
                }
                if (hasDividerBeforeChildAt(i)) {
                    childTop += this.mDividerHeight;
                }
                int childTop2 = childTop + lp.topMargin;
                setChildFrame(child, childLeft, childTop2 + getLocationOffset(child), childWidth, childHeight);
                childTop = childTop2 + lp.bottomMargin + childHeight + getNextLocationOffset(child);
                i += getChildrenSkipCount(child, i);
            }
            i++;
        }
    }

    @Override // android.view.View
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        if (layoutDirection == this.mLayoutDirection) {
            return;
        }
        this.mLayoutDirection = layoutDirection;
        if (this.mOrientation != 0) {
            return;
        }
        requestLayout();
    }

    void layoutHorizontal(int left, int top, int right, int bottom) {
        int childLeft;
        int childTop;
        boolean isLayoutRtl = isLayoutRtl();
        int paddingTop = this.mPaddingTop;
        int height = bottom - top;
        int childBottom = height - this.mPaddingBottom;
        int childSpace = (height - paddingTop) - this.mPaddingBottom;
        int count = getVirtualChildCount();
        int majorGravity = this.mGravity & 8388615;
        int minorGravity = this.mGravity & 112;
        boolean baselineAligned = this.mBaselineAligned;
        int[] maxAscent = this.mMaxAscent;
        int[] maxDescent = this.mMaxDescent;
        int layoutDirection = getLayoutDirection();
        switch (Gravity.getAbsoluteGravity(majorGravity, layoutDirection)) {
            case 1:
                childLeft = this.mPaddingLeft + (((right - left) - this.mTotalLength) / 2);
                break;
            case 5:
                childLeft = ((this.mPaddingLeft + right) - left) - this.mTotalLength;
                break;
            default:
                childLeft = this.mPaddingLeft;
                break;
        }
        int start = 0;
        int dir = 1;
        if (isLayoutRtl) {
            start = count - 1;
            dir = -1;
        }
        int i = 0;
        while (i < count) {
            int childIndex = start + (dir * i);
            View child = getVirtualChildAt(childIndex);
            if (child == null) {
                childLeft += measureNullChild(childIndex);
            } else if (child.getVisibility() != 8) {
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                int childBaseline = -1;
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (baselineAligned && lp.height != -1) {
                    childBaseline = child.getBaseline();
                }
                int gravity = lp.gravity;
                if (gravity < 0) {
                    gravity = minorGravity;
                }
                switch (gravity & 112) {
                    case 16:
                        childTop = ((((childSpace - childHeight) / 2) + paddingTop) + lp.topMargin) - lp.bottomMargin;
                        break;
                    case 48:
                        childTop = paddingTop + lp.topMargin;
                        if (childBaseline != -1) {
                            childTop += maxAscent[1] - childBaseline;
                            break;
                        }
                        break;
                    case 80:
                        childTop = (childBottom - childHeight) - lp.bottomMargin;
                        if (childBaseline != -1) {
                            int descent = child.getMeasuredHeight() - childBaseline;
                            childTop -= maxDescent[2] - descent;
                            break;
                        }
                        break;
                    default:
                        childTop = paddingTop;
                        break;
                }
                if (hasDividerBeforeChildAt(childIndex)) {
                    childLeft += this.mDividerWidth;
                }
                int childLeft2 = childLeft + lp.leftMargin;
                setChildFrame(child, childLeft2 + getLocationOffset(child), childTop, childWidth, childHeight);
                childLeft = childLeft2 + lp.rightMargin + childWidth + getNextLocationOffset(child);
                i += getChildrenSkipCount(child, childIndex);
            }
            i++;
        }
    }

    private void setChildFrame(View child, int left, int top, int width, int height) {
        child.layout(left, top, left + width, top + height);
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation == orientation) {
            return;
        }
        this.mOrientation = orientation;
        requestLayout();
    }

    @RemotableViewMethod
    public void setGravity(int gravity) {
        if (this.mGravity == gravity) {
            return;
        }
        if ((8388615 & gravity) == 0) {
            gravity |= 8388611;
        }
        if ((gravity & 112) == 0) {
            gravity |= 48;
        }
        this.mGravity = gravity;
        requestLayout();
    }

    @Override // android.view.ViewGroup
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.ViewGroup
    public LayoutParams generateDefaultLayoutParams() {
        if (this.mOrientation == 0) {
            return new LayoutParams(-2, -2);
        }
        if (this.mOrientation == 1) {
            return new LayoutParams(-1, -2);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // android.view.ViewGroup
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override // android.view.ViewGroup
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override // android.view.ViewGroup, android.view.View
    public CharSequence getAccessibilityClassName() {
        return MatchParentShrinkingLinearLayout.class.getName();
    }

    protected void encodeProperties(ViewHierarchyEncoder encoder) {
        super.encodeProperties(encoder);
        encoder.addProperty("layout:baselineAligned", this.mBaselineAligned);
        encoder.addProperty("layout:baselineAlignedChildIndex", this.mBaselineAlignedChildIndex);
        encoder.addProperty("measurement:baselineChildTop", this.mBaselineChildTop);
        encoder.addProperty("measurement:orientation", this.mOrientation);
        encoder.addProperty("measurement:gravity", this.mGravity);
        encoder.addProperty("measurement:totalLength", this.mTotalLength);
        encoder.addProperty("layout:totalLength", this.mTotalLength);
        encoder.addProperty("layout:useLargestChild", this.mUseLargestChild);
    }

    /* loaded from: classes.dex */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        @ViewDebug.ExportedProperty(category = "layout", mapping = {@ViewDebug.IntToString(from = -1, to = "NONE"), @ViewDebug.IntToString(from = 0, to = "NONE"), @ViewDebug.IntToString(from = 48, to = "TOP"), @ViewDebug.IntToString(from = 80, to = "BOTTOM"), @ViewDebug.IntToString(from = 3, to = "LEFT"), @ViewDebug.IntToString(from = 5, to = "RIGHT"), @ViewDebug.IntToString(from = 8388611, to = "START"), @ViewDebug.IntToString(from = 8388613, to = "END"), @ViewDebug.IntToString(from = 16, to = "CENTER_VERTICAL"), @ViewDebug.IntToString(from = 112, to = "FILL_VERTICAL"), @ViewDebug.IntToString(from = 1, to = "CENTER_HORIZONTAL"), @ViewDebug.IntToString(from = 7, to = "FILL_HORIZONTAL"), @ViewDebug.IntToString(from = 17, to = "CENTER"), @ViewDebug.IntToString(from = 119, to = "FILL")})
        public int gravity;
        @ViewDebug.ExportedProperty(category = "layout")
        public float weight;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.gravity = -1;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.LinearLayout_Layout);
            this.weight = a.getFloat(3, 0.0f);
            this.gravity = a.getInt(0, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = -1;
            this.weight = 0.0f;
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
            this.gravity = -1;
        }

        public String debug(String output) {
            return output + "MatchParentShrinkingLinearLayout.LayoutParams={width=" + sizeToString(this.width) + ", height=" + sizeToString(this.height) + " weight=" + this.weight + "}";
        }

        protected void encodeProperties(ViewHierarchyEncoder encoder) {
            super.encodeProperties(encoder);
            encoder.addProperty("layout:weight", this.weight);
            encoder.addProperty("layout:gravity", this.gravity);
        }
    }
}
