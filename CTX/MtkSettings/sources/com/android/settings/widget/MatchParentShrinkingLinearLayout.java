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
import com.android.settingslib.wifi.AccessPoint;

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

    public MatchParentShrinkingLinearLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public MatchParentShrinkingLinearLayout(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public MatchParentShrinkingLinearLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mBaselineAligned = true;
        this.mBaselineAlignedChildIndex = -1;
        this.mBaselineChildTop = 0;
        this.mGravity = 8388659;
        this.mLayoutDirection = -1;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.LinearLayout, i, i2);
        int i3 = typedArrayObtainStyledAttributes.getInt(1, -1);
        if (i3 >= 0) {
            setOrientation(i3);
        }
        int i4 = typedArrayObtainStyledAttributes.getInt(0, -1);
        if (i4 >= 0) {
            setGravity(i4);
        }
        boolean z = typedArrayObtainStyledAttributes.getBoolean(2, true);
        if (!z) {
            setBaselineAligned(z);
        }
        this.mWeightSum = typedArrayObtainStyledAttributes.getFloat(4, -1.0f);
        this.mBaselineAlignedChildIndex = typedArrayObtainStyledAttributes.getInt(3, -1);
        this.mUseLargestChild = typedArrayObtainStyledAttributes.getBoolean(6, false);
        setDividerDrawable(typedArrayObtainStyledAttributes.getDrawable(5));
        this.mShowDividers = typedArrayObtainStyledAttributes.getInt(7, 0);
        this.mDividerPadding = typedArrayObtainStyledAttributes.getDimensionPixelSize(8, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setShowDividers(int i) {
        if (i != this.mShowDividers) {
            requestLayout();
        }
        this.mShowDividers = i;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public int getShowDividers() {
        return this.mShowDividers;
    }

    public Drawable getDividerDrawable() {
        return this.mDivider;
    }

    public void setDividerDrawable(Drawable drawable) {
        if (drawable == this.mDivider) {
            return;
        }
        this.mDivider = drawable;
        if (drawable != null) {
            this.mDividerWidth = drawable.getIntrinsicWidth();
            this.mDividerHeight = drawable.getIntrinsicHeight();
        } else {
            this.mDividerWidth = 0;
            this.mDividerHeight = 0;
        }
        setWillNotDraw(drawable == null);
        requestLayout();
    }

    public void setDividerPadding(int i) {
        this.mDividerPadding = i;
    }

    public int getDividerPadding() {
        return this.mDividerPadding;
    }

    public int getDividerWidth() {
        return this.mDividerWidth;
    }

    @Override
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
        int virtualChildCount = getVirtualChildCount();
        for (int i = 0; i < virtualChildCount; i++) {
            View virtualChildAt = getVirtualChildAt(i);
            if (virtualChildAt != null && virtualChildAt.getVisibility() != 8 && hasDividerBeforeChildAt(i)) {
                drawHorizontalDivider(canvas, (virtualChildAt.getTop() - ((LayoutParams) virtualChildAt.getLayoutParams()).topMargin) - this.mDividerHeight);
            }
        }
        if (hasDividerBeforeChildAt(virtualChildCount)) {
            View virtualChildAt2 = getVirtualChildAt(virtualChildCount - 1);
            if (virtualChildAt2 == null) {
                bottom = (getHeight() - getPaddingBottom()) - this.mDividerHeight;
            } else {
                bottom = virtualChildAt2.getBottom() + ((LayoutParams) virtualChildAt2.getLayoutParams()).bottomMargin;
            }
            drawHorizontalDivider(canvas, bottom);
        }
    }

    void drawDividersHorizontal(Canvas canvas) {
        int right;
        int left;
        int virtualChildCount = getVirtualChildCount();
        boolean zIsLayoutRtl = isLayoutRtl();
        for (int i = 0; i < virtualChildCount; i++) {
            View virtualChildAt = getVirtualChildAt(i);
            if (virtualChildAt != null && virtualChildAt.getVisibility() != 8 && hasDividerBeforeChildAt(i)) {
                LayoutParams layoutParams = (LayoutParams) virtualChildAt.getLayoutParams();
                if (zIsLayoutRtl) {
                    left = virtualChildAt.getRight() + layoutParams.rightMargin;
                } else {
                    left = (virtualChildAt.getLeft() - layoutParams.leftMargin) - this.mDividerWidth;
                }
                drawVerticalDivider(canvas, left);
            }
        }
        if (hasDividerBeforeChildAt(virtualChildCount)) {
            View virtualChildAt2 = getVirtualChildAt(virtualChildCount - 1);
            if (virtualChildAt2 == null) {
                if (zIsLayoutRtl) {
                    right = getPaddingLeft();
                } else {
                    right = (getWidth() - getPaddingRight()) - this.mDividerWidth;
                }
            } else {
                LayoutParams layoutParams2 = (LayoutParams) virtualChildAt2.getLayoutParams();
                if (zIsLayoutRtl) {
                    right = (virtualChildAt2.getLeft() - layoutParams2.leftMargin) - this.mDividerWidth;
                } else {
                    right = virtualChildAt2.getRight() + layoutParams2.rightMargin;
                }
            }
            drawVerticalDivider(canvas, right);
        }
    }

    void drawHorizontalDivider(Canvas canvas, int i) {
        this.mDivider.setBounds(getPaddingLeft() + this.mDividerPadding, i, (getWidth() - getPaddingRight()) - this.mDividerPadding, this.mDividerHeight + i);
        this.mDivider.draw(canvas);
    }

    void drawVerticalDivider(Canvas canvas, int i) {
        this.mDivider.setBounds(i, getPaddingTop() + this.mDividerPadding, this.mDividerWidth + i, (getHeight() - getPaddingBottom()) - this.mDividerPadding);
        this.mDivider.draw(canvas);
    }

    @RemotableViewMethod
    public void setBaselineAligned(boolean z) {
        this.mBaselineAligned = z;
    }

    @RemotableViewMethod
    public void setMeasureWithLargestChildEnabled(boolean z) {
        this.mUseLargestChild = z;
    }

    @Override
    public int getBaseline() {
        int i;
        if (this.mBaselineAlignedChildIndex < 0) {
            return super.getBaseline();
        }
        if (getChildCount() <= this.mBaselineAlignedChildIndex) {
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout set to an index that is out of bounds.");
        }
        View childAt = getChildAt(this.mBaselineAlignedChildIndex);
        int baseline = childAt.getBaseline();
        if (baseline == -1) {
            if (this.mBaselineAlignedChildIndex == 0) {
                return -1;
            }
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout points to a View that doesn't know how to get its baseline.");
        }
        int i2 = this.mBaselineChildTop;
        if (this.mOrientation == 1 && (i = this.mGravity & 112) != 48) {
            if (i == 16) {
                i2 += ((((this.mBottom - this.mTop) - this.mPaddingTop) - this.mPaddingBottom) - this.mTotalLength) / 2;
            } else if (i == 80) {
                i2 = ((this.mBottom - this.mTop) - this.mPaddingBottom) - this.mTotalLength;
            }
        }
        return i2 + ((LayoutParams) childAt.getLayoutParams()).topMargin + baseline;
    }

    public int getBaselineAlignedChildIndex() {
        return this.mBaselineAlignedChildIndex;
    }

    @RemotableViewMethod
    public void setBaselineAlignedChildIndex(int i) {
        if (i < 0 || i >= getChildCount()) {
            throw new IllegalArgumentException("base aligned child index out of range (0, " + getChildCount() + ")");
        }
        this.mBaselineAlignedChildIndex = i;
    }

    View getVirtualChildAt(int i) {
        return getChildAt(i);
    }

    int getVirtualChildCount() {
        return getChildCount();
    }

    public float getWeightSum() {
        return this.mWeightSum;
    }

    @RemotableViewMethod
    public void setWeightSum(float f) {
        this.mWeightSum = Math.max(0.0f, f);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        if (this.mOrientation == 1) {
            measureVertical(i, i2);
        } else {
            measureHorizontal(i, i2);
        }
    }

    protected boolean hasDividerBeforeChildAt(int i) {
        if (i == 0) {
            return (this.mShowDividers & 1) != 0;
        }
        if (i == getChildCount()) {
            return (this.mShowDividers & 4) != 0;
        }
        if ((this.mShowDividers & 2) == 0) {
            return false;
        }
        for (int i2 = i - 1; i2 >= 0; i2--) {
            if (getChildAt(i2).getVisibility() != 8) {
                return true;
            }
        }
        return false;
    }

    void measureVertical(int i, int i2) {
        int i3;
        int i4;
        int i5;
        int iMax;
        float f;
        int i6;
        int i7;
        boolean z;
        int i8;
        int i9;
        int i10;
        int i11;
        View view;
        int i12;
        int i13;
        int i14;
        LayoutParams layoutParams;
        int i15;
        int iMax2;
        int i16;
        boolean z2;
        int iMax3;
        int i17;
        int i18 = i;
        int i19 = i2;
        int i20 = 0;
        this.mTotalLength = 0;
        int virtualChildCount = getVirtualChildCount();
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int i21 = this.mBaselineAlignedChildIndex;
        boolean z3 = this.mUseLargestChild;
        int i22 = 0;
        int iMax4 = 0;
        int i23 = 0;
        int childrenSkipCount = 0;
        boolean z4 = false;
        boolean z5 = false;
        float f2 = 0.0f;
        boolean z6 = true;
        int i24 = AccessPoint.UNREACHABLE_RSSI;
        while (true) {
            int i25 = 8;
            int i26 = i23;
            if (childrenSkipCount >= virtualChildCount) {
                int i27 = iMax4;
                int i28 = i20;
                int i29 = virtualChildCount;
                int i30 = mode2;
                int i31 = i26;
                int iMax5 = i22;
                int i32 = i24;
                if (this.mTotalLength > 0) {
                    i3 = i29;
                    if (hasDividerBeforeChildAt(i3)) {
                        this.mTotalLength += this.mDividerHeight;
                    }
                } else {
                    i3 = i29;
                }
                if (z3) {
                    i4 = i30;
                    if (i4 == Integer.MIN_VALUE || i4 == 0) {
                        this.mTotalLength = 0;
                        int childrenSkipCount2 = 0;
                        while (childrenSkipCount2 < i3) {
                            View virtualChildAt = getVirtualChildAt(childrenSkipCount2);
                            if (virtualChildAt == null) {
                                this.mTotalLength += measureNullChild(childrenSkipCount2);
                            } else if (virtualChildAt.getVisibility() == i25) {
                                childrenSkipCount2 += getChildrenSkipCount(virtualChildAt, childrenSkipCount2);
                            } else {
                                LayoutParams layoutParams2 = (LayoutParams) virtualChildAt.getLayoutParams();
                                int i33 = this.mTotalLength;
                                this.mTotalLength = Math.max(i33, i33 + i32 + layoutParams2.topMargin + layoutParams2.bottomMargin + getNextLocationOffset(virtualChildAt));
                            }
                            childrenSkipCount2++;
                            i25 = 8;
                        }
                    }
                } else {
                    i4 = i30;
                }
                this.mTotalLength += this.mPaddingTop + this.mPaddingBottom;
                int iResolveSizeAndState = resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumHeight()), i2, 0);
                int i34 = (16777215 & iResolveSizeAndState) - this.mTotalLength;
                if (z4 || (i34 != 0 && f2 > 0.0f)) {
                    if (this.mWeightSum > 0.0f) {
                        f2 = this.mWeightSum;
                    }
                    this.mTotalLength = 0;
                    float f3 = f2;
                    int i35 = 0;
                    int iCombineMeasuredStates = i28;
                    int i36 = i34;
                    while (i35 < i3) {
                        View virtualChildAt2 = getVirtualChildAt(i35);
                        if (virtualChildAt2.getVisibility() == 8) {
                            f = f3;
                        } else {
                            LayoutParams layoutParams3 = (LayoutParams) virtualChildAt2.getLayoutParams();
                            float f4 = layoutParams3.weight;
                            if (f4 <= 0.0f || i36 <= 0) {
                                float f5 = f3;
                                if (i36 < 0) {
                                    f = f5;
                                    if (layoutParams3.height == -1) {
                                        int childMeasureSpec = getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight + layoutParams3.leftMargin + layoutParams3.rightMargin, layoutParams3.width);
                                        int measuredHeight = virtualChildAt2.getMeasuredHeight() + i36;
                                        if (measuredHeight < 0) {
                                            measuredHeight = 0;
                                        }
                                        int measuredHeight2 = i36 - (measuredHeight - virtualChildAt2.getMeasuredHeight());
                                        virtualChildAt2.measure(childMeasureSpec, View.MeasureSpec.makeMeasureSpec(measuredHeight, 1073741824));
                                        iCombineMeasuredStates = combineMeasuredStates(iCombineMeasuredStates, virtualChildAt2.getMeasuredState() & (-256));
                                        i6 = measuredHeight2;
                                    }
                                } else {
                                    f = f5;
                                }
                                i6 = i36;
                            } else {
                                int i37 = (int) ((i36 * f4) / f3);
                                float f6 = f3 - f4;
                                i6 = i36 - i37;
                                int childMeasureSpec2 = getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight + layoutParams3.leftMargin + layoutParams3.rightMargin, layoutParams3.width);
                                if (layoutParams3.height == 0 && i4 == 1073741824) {
                                    if (i37 <= 0) {
                                        i37 = 0;
                                    }
                                    virtualChildAt2.measure(childMeasureSpec2, View.MeasureSpec.makeMeasureSpec(i37, 1073741824));
                                } else {
                                    int measuredHeight3 = virtualChildAt2.getMeasuredHeight() + i37;
                                    if (measuredHeight3 < 0) {
                                        measuredHeight3 = 0;
                                    }
                                    virtualChildAt2.measure(childMeasureSpec2, View.MeasureSpec.makeMeasureSpec(measuredHeight3, 1073741824));
                                }
                                iCombineMeasuredStates = combineMeasuredStates(iCombineMeasuredStates, virtualChildAt2.getMeasuredState() & (-256));
                                f = f6;
                            }
                            int i38 = layoutParams3.leftMargin + layoutParams3.rightMargin;
                            int measuredWidth = virtualChildAt2.getMeasuredWidth() + i38;
                            iMax5 = Math.max(iMax5, measuredWidth);
                            if (mode != 1073741824) {
                                i7 = i38;
                                z = layoutParams3.width == -1;
                                if (z) {
                                    measuredWidth = i7;
                                }
                                int iMax6 = Math.max(i31, measuredWidth);
                                boolean z7 = !z6 && layoutParams3.width == -1;
                                int i39 = this.mTotalLength;
                                this.mTotalLength = Math.max(i39, i39 + virtualChildAt2.getMeasuredHeight() + layoutParams3.topMargin + layoutParams3.bottomMargin + getNextLocationOffset(virtualChildAt2));
                                z6 = z7;
                                i36 = i6;
                                i31 = iMax6;
                            } else {
                                i7 = i38;
                            }
                            if (z) {
                            }
                            int iMax62 = Math.max(i31, measuredWidth);
                            if (z6) {
                            }
                            int i392 = this.mTotalLength;
                            this.mTotalLength = Math.max(i392, i392 + virtualChildAt2.getMeasuredHeight() + layoutParams3.topMargin + layoutParams3.bottomMargin + getNextLocationOffset(virtualChildAt2));
                            z6 = z7;
                            i36 = i6;
                            i31 = iMax62;
                        }
                        i35++;
                        f3 = f;
                    }
                    i5 = i;
                    this.mTotalLength += this.mPaddingTop + this.mPaddingBottom;
                    iMax = i31;
                    i28 = iCombineMeasuredStates;
                } else {
                    iMax = Math.max(i31, i27);
                    if (z3 && i4 != 1073741824) {
                        for (int i40 = 0; i40 < i3; i40++) {
                            View virtualChildAt3 = getVirtualChildAt(i40);
                            if (virtualChildAt3 != null && virtualChildAt3.getVisibility() != 8 && ((LayoutParams) virtualChildAt3.getLayoutParams()).weight > 0.0f) {
                                virtualChildAt3.measure(View.MeasureSpec.makeMeasureSpec(virtualChildAt3.getMeasuredWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(i32, 1073741824));
                            }
                        }
                    }
                    i5 = i;
                }
                if (!z6 && mode != 1073741824) {
                    iMax5 = iMax;
                }
                setMeasuredDimension(resolveSizeAndState(Math.max(iMax5 + this.mPaddingLeft + this.mPaddingRight, getSuggestedMinimumWidth()), i5, i28), iResolveSizeAndState);
                if (z5) {
                    forceUniformWidth(i3, i2);
                    return;
                }
                return;
            }
            View virtualChildAt4 = getVirtualChildAt(childrenSkipCount);
            if (virtualChildAt4 == null) {
                this.mTotalLength += measureNullChild(childrenSkipCount);
                i17 = i20;
                i11 = virtualChildCount;
                i9 = mode2;
                i23 = i26;
            } else {
                int i41 = i22;
                if (virtualChildAt4.getVisibility() == 8) {
                    childrenSkipCount += getChildrenSkipCount(virtualChildAt4, childrenSkipCount);
                    i17 = i20;
                    i11 = virtualChildCount;
                    i9 = mode2;
                    i23 = i26;
                    i22 = i41;
                } else {
                    if (hasDividerBeforeChildAt(childrenSkipCount)) {
                        this.mTotalLength += this.mDividerHeight;
                    }
                    LayoutParams layoutParams4 = (LayoutParams) virtualChildAt4.getLayoutParams();
                    float f7 = f2 + layoutParams4.weight;
                    if (mode2 == 1073741824 && layoutParams4.height == 0 && layoutParams4.weight > 0.0f) {
                        int i42 = this.mTotalLength;
                        iMax2 = i24;
                        this.mTotalLength = Math.max(i42, layoutParams4.topMargin + i42 + layoutParams4.bottomMargin);
                        i12 = iMax4;
                        view = virtualChildAt4;
                        layoutParams = layoutParams4;
                        i15 = i20;
                        i11 = virtualChildCount;
                        i9 = mode2;
                        z4 = true;
                        i13 = i26;
                        i10 = i41;
                        i14 = childrenSkipCount;
                    } else {
                        int i43 = i24;
                        if (layoutParams4.height != 0 || layoutParams4.weight <= 0.0f) {
                            i8 = AccessPoint.UNREACHABLE_RSSI;
                        } else {
                            layoutParams4.height = -2;
                            i8 = 0;
                        }
                        i9 = mode2;
                        i10 = i41;
                        int i44 = i8;
                        int i45 = childrenSkipCount;
                        i11 = virtualChildCount;
                        int i46 = iMax4;
                        int i47 = i18;
                        view = virtualChildAt4;
                        i12 = i46;
                        i13 = i26;
                        i14 = childrenSkipCount;
                        int i48 = i19;
                        layoutParams = layoutParams4;
                        i15 = i20;
                        measureChildBeforeLayout(virtualChildAt4, i45, i47, 0, i48, f7 == 0.0f ? this.mTotalLength : 0);
                        if (i44 != Integer.MIN_VALUE) {
                            layoutParams.height = i44;
                        }
                        int measuredHeight4 = view.getMeasuredHeight();
                        int i49 = this.mTotalLength;
                        this.mTotalLength = Math.max(i49, i49 + measuredHeight4 + layoutParams.topMargin + layoutParams.bottomMargin + getNextLocationOffset(view));
                        iMax2 = z3 ? Math.max(measuredHeight4, i43) : i43;
                    }
                    if (i21 >= 0 && i21 == i14 + 1) {
                        this.mBaselineChildTop = this.mTotalLength;
                    }
                    if (i14 < i21 && layoutParams.weight > 0.0f) {
                        throw new RuntimeException("A child of LinearLayout with index less than mBaselineAlignedChildIndex has weight > 0, which won't work.  Either remove the weight, or don't set mBaselineAlignedChildIndex.");
                    }
                    if (mode != 1073741824) {
                        i16 = -1;
                        if (layoutParams.width == -1) {
                            z2 = true;
                            z5 = true;
                        }
                        int i50 = layoutParams.leftMargin + layoutParams.rightMargin;
                        int measuredWidth2 = view.getMeasuredWidth() + i50;
                        int iMax7 = Math.max(i10, measuredWidth2);
                        int iCombineMeasuredStates2 = combineMeasuredStates(i15, view.getMeasuredState());
                        boolean z8 = !z6 && layoutParams.width == i16;
                        if (layoutParams.weight <= 0.0f) {
                            if (!z2) {
                                i50 = measuredWidth2;
                            }
                            iMax4 = Math.max(i12, i50);
                            iMax3 = i13;
                        } else {
                            int i51 = i12;
                            if (!z2) {
                                i50 = measuredWidth2;
                            }
                            iMax3 = Math.max(i13, i50);
                            iMax4 = i51;
                        }
                        z6 = z8;
                        i17 = iCombineMeasuredStates2;
                        i24 = iMax2;
                        childrenSkipCount = getChildrenSkipCount(view, i14) + i14;
                        i22 = iMax7;
                        i23 = iMax3;
                        f2 = f7;
                        childrenSkipCount++;
                        i20 = i17;
                        mode2 = i9;
                        virtualChildCount = i11;
                        i18 = i;
                        i19 = i2;
                    } else {
                        i16 = -1;
                    }
                    z2 = false;
                    int i502 = layoutParams.leftMargin + layoutParams.rightMargin;
                    int measuredWidth22 = view.getMeasuredWidth() + i502;
                    int iMax72 = Math.max(i10, measuredWidth22);
                    int iCombineMeasuredStates22 = combineMeasuredStates(i15, view.getMeasuredState());
                    if (z6) {
                        if (layoutParams.weight <= 0.0f) {
                        }
                        z6 = z8;
                        i17 = iCombineMeasuredStates22;
                        i24 = iMax2;
                        childrenSkipCount = getChildrenSkipCount(view, i14) + i14;
                        i22 = iMax72;
                        i23 = iMax3;
                        f2 = f7;
                    }
                    childrenSkipCount++;
                    i20 = i17;
                    mode2 = i9;
                    virtualChildCount = i11;
                    i18 = i;
                    i19 = i2;
                }
            }
            childrenSkipCount++;
            i20 = i17;
            mode2 = i9;
            virtualChildCount = i11;
            i18 = i;
            i19 = i2;
        }
    }

    private void forceUniformWidth(int i, int i2) {
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824);
        for (int i3 = 0; i3 < i; i3++) {
            View virtualChildAt = getVirtualChildAt(i3);
            if (virtualChildAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) virtualChildAt.getLayoutParams();
                if (layoutParams.width == -1) {
                    int i4 = layoutParams.height;
                    layoutParams.height = virtualChildAt.getMeasuredHeight();
                    measureChildWithMargins(virtualChildAt, iMakeMeasureSpec, 0, i2, 0);
                    layoutParams.height = i4;
                }
            }
        }
    }

    void measureHorizontal(int i, int i2) {
        throw new IllegalStateException("horizontal mode not supported.");
    }

    int getChildrenSkipCount(View view, int i) {
        return 0;
    }

    int measureNullChild(int i) {
        return 0;
    }

    void measureChildBeforeLayout(View view, int i, int i2, int i3, int i4, int i5) {
        measureChildWithMargins(view, i2, i3, i4, i5);
    }

    int getLocationOffset(View view) {
        return 0;
    }

    int getNextLocationOffset(View view) {
        return 0;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.mOrientation == 1) {
            layoutVertical(i, i2, i3, i4);
        } else {
            layoutHorizontal(i, i2, i3, i4);
        }
    }

    void layoutVertical(int i, int i2, int i3, int i4) {
        int iMeasureNullChild;
        int i5;
        int i6 = this.mPaddingLeft;
        int i7 = i3 - i;
        int i8 = i7 - this.mPaddingRight;
        int i9 = (i7 - i6) - this.mPaddingRight;
        int virtualChildCount = getVirtualChildCount();
        int i10 = this.mGravity & 112;
        int i11 = this.mGravity & 8388615;
        if (i10 == 16) {
            iMeasureNullChild = (((i4 - i2) - this.mTotalLength) / 2) + this.mPaddingTop;
        } else if (i10 == 80) {
            iMeasureNullChild = ((this.mPaddingTop + i4) - i2) - this.mTotalLength;
        } else {
            iMeasureNullChild = this.mPaddingTop;
        }
        int childrenSkipCount = 0;
        while (childrenSkipCount < virtualChildCount) {
            View virtualChildAt = getVirtualChildAt(childrenSkipCount);
            if (virtualChildAt == null) {
                iMeasureNullChild += measureNullChild(childrenSkipCount);
            } else if (virtualChildAt.getVisibility() != 8) {
                int measuredWidth = virtualChildAt.getMeasuredWidth();
                int measuredHeight = virtualChildAt.getMeasuredHeight();
                LayoutParams layoutParams = (LayoutParams) virtualChildAt.getLayoutParams();
                int i12 = layoutParams.gravity;
                if (i12 < 0) {
                    i12 = i11;
                }
                int absoluteGravity = Gravity.getAbsoluteGravity(i12, getLayoutDirection()) & 7;
                if (absoluteGravity == 1) {
                    i5 = ((((i9 - measuredWidth) / 2) + i6) + layoutParams.leftMargin) - layoutParams.rightMargin;
                } else if (absoluteGravity == 5) {
                    i5 = (i8 - measuredWidth) - layoutParams.rightMargin;
                } else {
                    i5 = layoutParams.leftMargin + i6;
                }
                int i13 = i5;
                if (hasDividerBeforeChildAt(childrenSkipCount)) {
                    iMeasureNullChild += this.mDividerHeight;
                }
                int i14 = iMeasureNullChild + layoutParams.topMargin;
                setChildFrame(virtualChildAt, i13, i14 + getLocationOffset(virtualChildAt), measuredWidth, measuredHeight);
                int nextLocationOffset = i14 + measuredHeight + layoutParams.bottomMargin + getNextLocationOffset(virtualChildAt);
                childrenSkipCount += getChildrenSkipCount(virtualChildAt, childrenSkipCount);
                iMeasureNullChild = nextLocationOffset;
            }
            childrenSkipCount++;
        }
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        if (i != this.mLayoutDirection) {
            this.mLayoutDirection = i;
            if (this.mOrientation == 0) {
                requestLayout();
            }
        }
    }

    void layoutHorizontal(int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        boolean z;
        int i10;
        int i11;
        int i12;
        int measuredHeight;
        boolean zIsLayoutRtl = isLayoutRtl();
        int i13 = this.mPaddingTop;
        int i14 = i4 - i2;
        int i15 = i14 - this.mPaddingBottom;
        int i16 = (i14 - i13) - this.mPaddingBottom;
        int virtualChildCount = getVirtualChildCount();
        int i17 = this.mGravity & 8388615;
        int i18 = this.mGravity & 112;
        boolean z2 = this.mBaselineAligned;
        int[] iArr = this.mMaxAscent;
        int[] iArr2 = this.mMaxDescent;
        int absoluteGravity = Gravity.getAbsoluteGravity(i17, getLayoutDirection());
        boolean z3 = true;
        int iMeasureNullChild = absoluteGravity != 1 ? absoluteGravity != 5 ? this.mPaddingLeft : ((this.mPaddingLeft + i3) - i) - this.mTotalLength : (((i3 - i) - this.mTotalLength) / 2) + this.mPaddingLeft;
        if (zIsLayoutRtl) {
            i5 = virtualChildCount - 1;
            i6 = -1;
        } else {
            i5 = 0;
            i6 = 1;
        }
        int childrenSkipCount = 0;
        while (childrenSkipCount < virtualChildCount) {
            int i19 = i5 + (i6 * childrenSkipCount);
            View virtualChildAt = getVirtualChildAt(i19);
            if (virtualChildAt == null) {
                iMeasureNullChild += measureNullChild(i19);
                z = z3;
                i7 = i13;
                i8 = virtualChildCount;
                i9 = i18;
            } else if (virtualChildAt.getVisibility() != 8) {
                int measuredWidth = virtualChildAt.getMeasuredWidth();
                int measuredHeight2 = virtualChildAt.getMeasuredHeight();
                LayoutParams layoutParams = (LayoutParams) virtualChildAt.getLayoutParams();
                if (z2) {
                    i10 = childrenSkipCount;
                    i8 = virtualChildCount;
                    int baseline = layoutParams.height != -1 ? virtualChildAt.getBaseline() : -1;
                    i11 = layoutParams.gravity;
                    if (i11 < 0) {
                        i11 = i18;
                    }
                    i12 = i11 & 112;
                    i9 = i18;
                    if (i12 != 16) {
                        z = true;
                        measuredHeight = ((((i16 - measuredHeight2) / 2) + i13) + layoutParams.topMargin) - layoutParams.bottomMargin;
                    } else if (i12 != 48) {
                        if (i12 != 80) {
                            measuredHeight = i13;
                        } else {
                            int i20 = (i15 - measuredHeight2) - layoutParams.bottomMargin;
                            if (baseline != -1) {
                                measuredHeight = i20 - (iArr2[2] - (virtualChildAt.getMeasuredHeight() - baseline));
                            } else {
                                measuredHeight = i20;
                                z = true;
                            }
                        }
                        z = true;
                    } else {
                        int i21 = layoutParams.topMargin + i13;
                        if (baseline != -1) {
                            z = true;
                            i21 += iArr[1] - baseline;
                        } else {
                            z = true;
                        }
                        measuredHeight = i21;
                    }
                    if (hasDividerBeforeChildAt(i19)) {
                        iMeasureNullChild += this.mDividerWidth;
                    }
                    int i22 = layoutParams.leftMargin + iMeasureNullChild;
                    i7 = i13;
                    setChildFrame(virtualChildAt, i22 + getLocationOffset(virtualChildAt), measuredHeight, measuredWidth, measuredHeight2);
                    int nextLocationOffset = i22 + measuredWidth + layoutParams.rightMargin + getNextLocationOffset(virtualChildAt);
                    childrenSkipCount = i10 + getChildrenSkipCount(virtualChildAt, i19);
                    iMeasureNullChild = nextLocationOffset;
                    childrenSkipCount++;
                    z3 = z;
                    virtualChildCount = i8;
                    i18 = i9;
                    i13 = i7;
                } else {
                    i10 = childrenSkipCount;
                    i8 = virtualChildCount;
                }
                i11 = layoutParams.gravity;
                if (i11 < 0) {
                }
                i12 = i11 & 112;
                i9 = i18;
                if (i12 != 16) {
                }
                if (hasDividerBeforeChildAt(i19)) {
                }
                int i222 = layoutParams.leftMargin + iMeasureNullChild;
                i7 = i13;
                setChildFrame(virtualChildAt, i222 + getLocationOffset(virtualChildAt), measuredHeight, measuredWidth, measuredHeight2);
                int nextLocationOffset2 = i222 + measuredWidth + layoutParams.rightMargin + getNextLocationOffset(virtualChildAt);
                childrenSkipCount = i10 + getChildrenSkipCount(virtualChildAt, i19);
                iMeasureNullChild = nextLocationOffset2;
                childrenSkipCount++;
                z3 = z;
                virtualChildCount = i8;
                i18 = i9;
                i13 = i7;
            } else {
                i7 = i13;
                i8 = virtualChildCount;
                i9 = i18;
                z = true;
            }
            childrenSkipCount++;
            z3 = z;
            virtualChildCount = i8;
            i18 = i9;
            i13 = i7;
        }
    }

    private void setChildFrame(View view, int i, int i2, int i3, int i4) {
        view.layout(i, i2, i3 + i, i4 + i2);
    }

    public void setOrientation(int i) {
        if (this.mOrientation != i) {
            this.mOrientation = i;
            requestLayout();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    @RemotableViewMethod
    public void setGravity(int i) {
        if (this.mGravity != i) {
            if ((8388615 & i) == 0) {
                i |= 8388611;
            }
            if ((i & 112) == 0) {
                i |= 48;
            }
            this.mGravity = i;
            requestLayout();
        }
    }

    @RemotableViewMethod
    public void setHorizontalGravity(int i) {
        int i2 = i & 8388615;
        if ((8388615 & this.mGravity) != i2) {
            this.mGravity = i2 | (this.mGravity & (-8388616));
            requestLayout();
        }
    }

    @RemotableViewMethod
    public void setVerticalGravity(int i) {
        int i2 = i & 112;
        if ((this.mGravity & 112) != i2) {
            this.mGravity = i2 | (this.mGravity & (-113));
            requestLayout();
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        if (this.mOrientation == 0) {
            return new LayoutParams(-2, -2);
        }
        if (this.mOrientation == 1) {
            return new LayoutParams(-1, -2);
        }
        return null;
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return MatchParentShrinkingLinearLayout.class.getName();
    }

    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        viewHierarchyEncoder.addProperty("layout:baselineAligned", this.mBaselineAligned);
        viewHierarchyEncoder.addProperty("layout:baselineAlignedChildIndex", this.mBaselineAlignedChildIndex);
        viewHierarchyEncoder.addProperty("measurement:baselineChildTop", this.mBaselineChildTop);
        viewHierarchyEncoder.addProperty("measurement:orientation", this.mOrientation);
        viewHierarchyEncoder.addProperty("measurement:gravity", this.mGravity);
        viewHierarchyEncoder.addProperty("measurement:totalLength", this.mTotalLength);
        viewHierarchyEncoder.addProperty("layout:totalLength", this.mTotalLength);
        viewHierarchyEncoder.addProperty("layout:useLargestChild", this.mUseLargestChild);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        @ViewDebug.ExportedProperty(category = "layout", mapping = {@ViewDebug.IntToString(from = -1, to = "NONE"), @ViewDebug.IntToString(from = 0, to = "NONE"), @ViewDebug.IntToString(from = 48, to = "TOP"), @ViewDebug.IntToString(from = 80, to = "BOTTOM"), @ViewDebug.IntToString(from = 3, to = "LEFT"), @ViewDebug.IntToString(from = 5, to = "RIGHT"), @ViewDebug.IntToString(from = 8388611, to = "START"), @ViewDebug.IntToString(from = 8388613, to = "END"), @ViewDebug.IntToString(from = 16, to = "CENTER_VERTICAL"), @ViewDebug.IntToString(from = 112, to = "FILL_VERTICAL"), @ViewDebug.IntToString(from = 1, to = "CENTER_HORIZONTAL"), @ViewDebug.IntToString(from = 7, to = "FILL_HORIZONTAL"), @ViewDebug.IntToString(from = 17, to = "CENTER"), @ViewDebug.IntToString(from = 119, to = "FILL")})
        public int gravity;

        @ViewDebug.ExportedProperty(category = "layout")
        public float weight;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.gravity = -1;
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.LinearLayout_Layout);
            this.weight = typedArrayObtainStyledAttributes.getFloat(3, 0.0f);
            this.gravity = typedArrayObtainStyledAttributes.getInt(0, -1);
            typedArrayObtainStyledAttributes.recycle();
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
            this.gravity = -1;
            this.weight = 0.0f;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.gravity = -1;
        }

        public String debug(String str) {
            return str + "MatchParentShrinkingLinearLayout.LayoutParams={width=" + sizeToString(this.width) + ", height=" + sizeToString(this.height) + " weight=" + this.weight + "}";
        }

        protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
            super.encodeProperties(viewHierarchyEncoder);
            viewHierarchyEncoder.addProperty("layout:weight", this.weight);
            viewHierarchyEncoder.addProperty("layout:gravity", this.gravity);
        }
    }
}
