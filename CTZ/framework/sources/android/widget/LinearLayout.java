package android.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.TtmlUtils;
import android.security.keystore.KeyProperties;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewHierarchyEncoder;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RemoteViews.RemoteView
public class LinearLayout extends ViewGroup {
    public static final int HORIZONTAL = 0;
    private static final int INDEX_BOTTOM = 2;
    private static final int INDEX_CENTER_VERTICAL = 0;
    private static final int INDEX_FILL = 3;
    private static final int INDEX_TOP = 1;
    public static final int SHOW_DIVIDER_BEGINNING = 1;
    public static final int SHOW_DIVIDER_END = 4;
    public static final int SHOW_DIVIDER_MIDDLE = 2;
    public static final int SHOW_DIVIDER_NONE = 0;
    public static final int VERTICAL = 1;
    private static final int VERTICAL_GRAVITY_COUNT = 4;
    private static boolean sCompatibilityDone = false;
    private static boolean sRemeasureWeightedChildren = true;
    private final boolean mAllowInconsistentMeasurement;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    private boolean mBaselineAligned;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    private int mBaselineAlignedChildIndex;

    @ViewDebug.ExportedProperty(category = "measurement")
    private int mBaselineChildTop;
    private Drawable mDivider;
    private int mDividerHeight;
    private int mDividerPadding;
    private int mDividerWidth;

    @ViewDebug.ExportedProperty(category = "measurement", flagMapping = {@ViewDebug.FlagToString(equals = -1, mask = -1, name = KeyProperties.DIGEST_NONE), @ViewDebug.FlagToString(equals = 0, mask = 0, name = KeyProperties.DIGEST_NONE), @ViewDebug.FlagToString(equals = 48, mask = 48, name = "TOP"), @ViewDebug.FlagToString(equals = 80, mask = 80, name = "BOTTOM"), @ViewDebug.FlagToString(equals = 3, mask = 3, name = "LEFT"), @ViewDebug.FlagToString(equals = 5, mask = 5, name = "RIGHT"), @ViewDebug.FlagToString(equals = Gravity.START, mask = Gravity.START, name = "START"), @ViewDebug.FlagToString(equals = Gravity.END, mask = Gravity.END, name = "END"), @ViewDebug.FlagToString(equals = 16, mask = 16, name = "CENTER_VERTICAL"), @ViewDebug.FlagToString(equals = 112, mask = 112, name = "FILL_VERTICAL"), @ViewDebug.FlagToString(equals = 1, mask = 1, name = "CENTER_HORIZONTAL"), @ViewDebug.FlagToString(equals = 7, mask = 7, name = "FILL_HORIZONTAL"), @ViewDebug.FlagToString(equals = 17, mask = 17, name = "CENTER"), @ViewDebug.FlagToString(equals = 119, mask = 119, name = "FILL"), @ViewDebug.FlagToString(equals = 8388608, mask = 8388608, name = "RELATIVE")}, formatToHexString = true)
    private int mGravity;
    private int mLayoutDirection;
    private int[] mMaxAscent;
    private int[] mMaxDescent;

    @ViewDebug.ExportedProperty(category = "measurement")
    private int mOrientation;
    private int mShowDividers;

    @ViewDebug.ExportedProperty(category = "measurement")
    private int mTotalLength;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    private boolean mUseLargestChild;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    private float mWeightSum;

    @Retention(RetentionPolicy.SOURCE)
    public @interface DividerMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface OrientationMode {
    }

    public LinearLayout(Context context) {
        this(context, null);
    }

    public LinearLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public LinearLayout(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public LinearLayout(Context context, AttributeSet attributeSet, int i, int i2) {
        boolean z;
        super(context, attributeSet, i, i2);
        this.mBaselineAligned = true;
        this.mBaselineAlignedChildIndex = -1;
        this.mBaselineChildTop = 0;
        this.mGravity = 8388659;
        this.mLayoutDirection = -1;
        if (!sCompatibilityDone && context != null) {
            if (context.getApplicationInfo().targetSdkVersion >= 28) {
                z = true;
            } else {
                z = false;
            }
            sRemeasureWeightedChildren = z;
            sCompatibilityDone = true;
        }
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.LinearLayout, i, i2);
        int i3 = typedArrayObtainStyledAttributes.getInt(1, -1);
        if (i3 >= 0) {
            setOrientation(i3);
        }
        int i4 = typedArrayObtainStyledAttributes.getInt(0, -1);
        if (i4 >= 0) {
            setGravity(i4);
        }
        boolean z2 = typedArrayObtainStyledAttributes.getBoolean(2, true);
        if (!z2) {
            setBaselineAligned(z2);
        }
        this.mWeightSum = typedArrayObtainStyledAttributes.getFloat(4, -1.0f);
        this.mBaselineAlignedChildIndex = typedArrayObtainStyledAttributes.getInt(3, -1);
        this.mUseLargestChild = typedArrayObtainStyledAttributes.getBoolean(6, false);
        this.mShowDividers = typedArrayObtainStyledAttributes.getInt(7, 0);
        this.mDividerPadding = typedArrayObtainStyledAttributes.getDimensionPixelSize(8, 0);
        setDividerDrawable(typedArrayObtainStyledAttributes.getDrawable(5));
        this.mAllowInconsistentMeasurement = context.getApplicationInfo().targetSdkVersion <= 23;
        typedArrayObtainStyledAttributes.recycle();
    }

    private boolean isShowingDividers() {
        return (this.mShowDividers == 0 || this.mDivider == null) ? false : true;
    }

    public void setShowDividers(int i) {
        if (i == this.mShowDividers) {
            return;
        }
        this.mShowDividers = i;
        setWillNotDraw(!isShowingDividers());
        requestLayout();
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
        setWillNotDraw(!isShowingDividers());
        requestLayout();
    }

    public void setDividerPadding(int i) {
        if (i == this.mDividerPadding) {
            return;
        }
        this.mDividerPadding = i;
        if (isShowingDividers()) {
            requestLayout();
            invalidate();
        }
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
            View lastNonGoneChild = getLastNonGoneChild();
            if (lastNonGoneChild == null) {
                bottom = (getHeight() - getPaddingBottom()) - this.mDividerHeight;
            } else {
                bottom = lastNonGoneChild.getBottom() + ((LayoutParams) lastNonGoneChild.getLayoutParams()).bottomMargin;
            }
            drawHorizontalDivider(canvas, bottom);
        }
    }

    private View getLastNonGoneChild() {
        for (int virtualChildCount = getVirtualChildCount() - 1; virtualChildCount >= 0; virtualChildCount--) {
            View virtualChildAt = getVirtualChildAt(virtualChildCount);
            if (virtualChildAt != null && virtualChildAt.getVisibility() != 8) {
                return virtualChildAt;
            }
        }
        return null;
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
            View lastNonGoneChild = getLastNonGoneChild();
            if (lastNonGoneChild == null) {
                if (zIsLayoutRtl) {
                    right = getPaddingLeft();
                } else {
                    right = (getWidth() - getPaddingRight()) - this.mDividerWidth;
                }
            } else {
                LayoutParams layoutParams2 = (LayoutParams) lastNonGoneChild.getLayoutParams();
                if (zIsLayoutRtl) {
                    right = (lastNonGoneChild.getLeft() - layoutParams2.leftMargin) - this.mDividerWidth;
                } else {
                    right = lastNonGoneChild.getRight() + layoutParams2.rightMargin;
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

    public boolean isBaselineAligned() {
        return this.mBaselineAligned;
    }

    @RemotableViewMethod
    public void setBaselineAligned(boolean z) {
        this.mBaselineAligned = z;
    }

    public boolean isMeasureWithLargestChildEnabled() {
        return this.mUseLargestChild;
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
        return i == getVirtualChildCount() ? (this.mShowDividers & 4) != 0 : allViewsAreGoneBefore(i) ? (this.mShowDividers & 1) != 0 : (this.mShowDividers & 2) != 0;
    }

    private boolean allViewsAreGoneBefore(int i) {
        for (int i2 = i - 1; i2 >= 0; i2--) {
            View virtualChildAt = getVirtualChildAt(i2);
            if (virtualChildAt != null && virtualChildAt.getVisibility() != 8) {
                return false;
            }
        }
        return true;
    }

    void measureVertical(int i, int i2) {
        int i3;
        int iCombineMeasuredStates;
        int i4;
        int iMax;
        int i5;
        int i6;
        int i7;
        int i8;
        boolean z;
        int i9;
        int i10;
        int i11;
        View view;
        int i12;
        int i13;
        LayoutParams layoutParams;
        int i14;
        boolean z2;
        int i15 = i;
        int i16 = i2;
        this.mTotalLength = 0;
        int virtualChildCount = getVirtualChildCount();
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int i17 = this.mBaselineAlignedChildIndex;
        boolean z3 = this.mUseLargestChild;
        int i18 = 0;
        int iMax2 = 0;
        int iMax3 = 0;
        int childrenSkipCount = 0;
        int i19 = 0;
        boolean z4 = false;
        int i20 = 0;
        int i21 = 0;
        boolean z5 = false;
        float f = 0.0f;
        boolean z6 = true;
        int i22 = Integer.MIN_VALUE;
        while (true) {
            int i23 = 8;
            if (childrenSkipCount < virtualChildCount) {
                int iMax4 = i22;
                View virtualChildAt = getVirtualChildAt(childrenSkipCount);
                if (virtualChildAt == null) {
                    this.mTotalLength += measureNullChild(childrenSkipCount);
                } else if (virtualChildAt.getVisibility() == 8) {
                    childrenSkipCount += getChildrenSkipCount(virtualChildAt, childrenSkipCount);
                } else {
                    int i24 = i18 + 1;
                    if (hasDividerBeforeChildAt(childrenSkipCount)) {
                        this.mTotalLength += this.mDividerHeight;
                    }
                    LayoutParams layoutParams2 = (LayoutParams) virtualChildAt.getLayoutParams();
                    float f2 = f + layoutParams2.weight;
                    boolean z7 = layoutParams2.height == 0 && layoutParams2.weight > 0.0f;
                    if (mode2 == 1073741824 && z7) {
                        int i25 = this.mTotalLength;
                        this.mTotalLength = Math.max(i25, layoutParams2.topMargin + i25 + layoutParams2.bottomMargin);
                        view = virtualChildAt;
                        i13 = childrenSkipCount;
                        layoutParams = layoutParams2;
                        i10 = i24;
                        i12 = mode2;
                        z4 = true;
                        i9 = iMax2;
                        i11 = iMax3;
                    } else {
                        int i26 = iMax2;
                        if (z7) {
                            layoutParams2.height = -2;
                        }
                        i9 = i26;
                        int i27 = childrenSkipCount;
                        i10 = i24;
                        i11 = iMax3;
                        int i28 = i15;
                        view = virtualChildAt;
                        i12 = mode2;
                        i13 = childrenSkipCount;
                        int i29 = i16;
                        layoutParams = layoutParams2;
                        measureChildBeforeLayout(virtualChildAt, i27, i28, 0, i29, f2 == 0.0f ? this.mTotalLength : 0);
                        int measuredHeight = view.getMeasuredHeight();
                        if (z7) {
                            layoutParams.height = 0;
                            i19 += measuredHeight;
                        }
                        int i30 = this.mTotalLength;
                        this.mTotalLength = Math.max(i30, i30 + measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin + getNextLocationOffset(view));
                        iMax4 = z3 ? Math.max(measuredHeight, iMax4) : iMax4;
                    }
                    if (i17 >= 0) {
                        i14 = i13;
                        if (i17 == i14 + 1) {
                            this.mBaselineChildTop = this.mTotalLength;
                        }
                    } else {
                        i14 = i13;
                    }
                    if (i14 < i17 && layoutParams.weight > 0.0f) {
                        throw new RuntimeException("A child of LinearLayout with index less than mBaselineAlignedChildIndex has weight > 0, which won't work.  Either remove the weight, or don't set mBaselineAlignedChildIndex.");
                    }
                    if (mode == 1073741824 || layoutParams.width != -1) {
                        z2 = false;
                    } else {
                        z2 = true;
                        z5 = true;
                    }
                    int i31 = layoutParams.leftMargin + layoutParams.rightMargin;
                    int measuredWidth = view.getMeasuredWidth() + i31;
                    int iMax5 = Math.max(i20, measuredWidth);
                    int iCombineMeasuredStates2 = combineMeasuredStates(i21, view.getMeasuredState());
                    boolean z8 = z6 && layoutParams.width == -1;
                    if (layoutParams.weight > 0.0f) {
                        if (!z2) {
                            i31 = measuredWidth;
                        }
                        iMax2 = Math.max(i9, i31);
                        iMax3 = i11;
                    } else {
                        int i32 = i9;
                        if (!z2) {
                            i31 = measuredWidth;
                        }
                        iMax3 = Math.max(i11, i31);
                        iMax2 = i32;
                    }
                    i20 = iMax5;
                    i21 = iCombineMeasuredStates2;
                    z6 = z8;
                    i22 = iMax4;
                    f = f2;
                    childrenSkipCount = i14 + getChildrenSkipCount(view, i14);
                    i18 = i10;
                    childrenSkipCount++;
                    mode2 = i12;
                    i15 = i;
                    i16 = i2;
                }
                i12 = mode2;
                i22 = iMax4;
                childrenSkipCount++;
                mode2 = i12;
                i15 = i;
                i16 = i2;
            } else {
                int i33 = iMax2;
                int i34 = iMax3;
                int i35 = mode2;
                int i36 = i22;
                int iMax6 = i20;
                if (i18 > 0 && hasDividerBeforeChildAt(virtualChildCount)) {
                    this.mTotalLength += this.mDividerHeight;
                }
                if (z3) {
                    i3 = i35;
                    if (i3 == Integer.MIN_VALUE || i3 == 0) {
                        this.mTotalLength = 0;
                        int childrenSkipCount2 = 0;
                        while (childrenSkipCount2 < virtualChildCount) {
                            View virtualChildAt2 = getVirtualChildAt(childrenSkipCount2);
                            if (virtualChildAt2 == null) {
                                this.mTotalLength += measureNullChild(childrenSkipCount2);
                            } else if (virtualChildAt2.getVisibility() == i23) {
                                childrenSkipCount2 += getChildrenSkipCount(virtualChildAt2, childrenSkipCount2);
                            } else {
                                LayoutParams layoutParams3 = (LayoutParams) virtualChildAt2.getLayoutParams();
                                int i37 = this.mTotalLength;
                                this.mTotalLength = Math.max(i37, i37 + i36 + layoutParams3.topMargin + layoutParams3.bottomMargin + getNextLocationOffset(virtualChildAt2));
                            }
                            childrenSkipCount2++;
                            i23 = 8;
                        }
                    }
                } else {
                    i3 = i35;
                }
                this.mTotalLength += this.mPaddingTop + this.mPaddingBottom;
                int iResolveSizeAndState = resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumHeight()), i2, 0);
                int i38 = (16777215 & iResolveSizeAndState) - this.mTotalLength;
                if (this.mAllowInconsistentMeasurement) {
                    i19 = 0;
                }
                int i39 = i38 + i19;
                if (z4 || ((sRemeasureWeightedChildren || i39 != 0) && f > 0.0f)) {
                    if (this.mWeightSum > 0.0f) {
                        f = this.mWeightSum;
                    }
                    this.mTotalLength = 0;
                    float f3 = f;
                    int i40 = i39;
                    iCombineMeasuredStates = i21;
                    int i41 = 0;
                    while (i41 < virtualChildCount) {
                        View virtualChildAt3 = getVirtualChildAt(i41);
                        if (virtualChildAt3 == null || virtualChildAt3.getVisibility() == 8) {
                            i5 = i3;
                            i6 = i36;
                            i7 = i40;
                        } else {
                            LayoutParams layoutParams4 = (LayoutParams) virtualChildAt3.getLayoutParams();
                            float f4 = layoutParams4.weight;
                            if (f4 > 0.0f) {
                                i6 = i36;
                                int measuredHeight2 = (int) ((i40 * f4) / f3);
                                int i42 = i40 - measuredHeight2;
                                f3 -= f4;
                                if (!this.mUseLargestChild || i3 == 1073741824) {
                                    if (layoutParams4.height != 0 || (this.mAllowInconsistentMeasurement && i3 != 1073741824)) {
                                        measuredHeight2 += virtualChildAt3.getMeasuredHeight();
                                    }
                                } else {
                                    measuredHeight2 = i6;
                                }
                                i5 = i3;
                                i7 = i42;
                                virtualChildAt3.measure(getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight + layoutParams4.leftMargin + layoutParams4.rightMargin, layoutParams4.width), View.MeasureSpec.makeMeasureSpec(Math.max(0, measuredHeight2), 1073741824));
                                iCombineMeasuredStates = combineMeasuredStates(iCombineMeasuredStates, virtualChildAt3.getMeasuredState() & (-256));
                            } else {
                                i5 = i3;
                                i6 = i36;
                                i7 = i40;
                            }
                            int i43 = layoutParams4.leftMargin + layoutParams4.rightMargin;
                            int measuredWidth2 = virtualChildAt3.getMeasuredWidth() + i43;
                            iMax6 = Math.max(iMax6, measuredWidth2);
                            if (mode == 1073741824) {
                                i8 = i43;
                            } else {
                                i8 = i43;
                                z = layoutParams4.width == -1;
                                if (z) {
                                    measuredWidth2 = i8;
                                }
                                int iMax7 = Math.max(i34, measuredWidth2);
                                boolean z9 = !z6 && layoutParams4.width == -1;
                                int i44 = this.mTotalLength;
                                this.mTotalLength = Math.max(i44, i44 + virtualChildAt3.getMeasuredHeight() + layoutParams4.topMargin + layoutParams4.bottomMargin + getNextLocationOffset(virtualChildAt3));
                                z6 = z9;
                                i34 = iMax7;
                            }
                            if (z) {
                            }
                            int iMax72 = Math.max(i34, measuredWidth2);
                            if (z6) {
                            }
                            int i442 = this.mTotalLength;
                            this.mTotalLength = Math.max(i442, i442 + virtualChildAt3.getMeasuredHeight() + layoutParams4.topMargin + layoutParams4.bottomMargin + getNextLocationOffset(virtualChildAt3));
                            z6 = z9;
                            i34 = iMax72;
                        }
                        i41++;
                        i36 = i6;
                        i3 = i5;
                        i40 = i7;
                    }
                    i4 = i;
                    this.mTotalLength += this.mPaddingTop + this.mPaddingBottom;
                    iMax = i34;
                } else {
                    iMax = Math.max(i34, i33);
                    if (z3 && i3 != 1073741824) {
                        for (int i45 = 0; i45 < virtualChildCount; i45++) {
                            View virtualChildAt4 = getVirtualChildAt(i45);
                            if (virtualChildAt4 != null && virtualChildAt4.getVisibility() != 8 && ((LayoutParams) virtualChildAt4.getLayoutParams()).weight > 0.0f) {
                                virtualChildAt4.measure(View.MeasureSpec.makeMeasureSpec(virtualChildAt4.getMeasuredWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(i36, 1073741824));
                            }
                        }
                    }
                    iCombineMeasuredStates = i21;
                    i4 = i;
                }
                if (z6 || mode == 1073741824) {
                    iMax = iMax6;
                }
                setMeasuredDimension(resolveSizeAndState(Math.max(iMax + this.mPaddingLeft + this.mPaddingRight, getSuggestedMinimumWidth()), i4, iCombineMeasuredStates), iResolveSizeAndState);
                if (z5) {
                    forceUniformWidth(virtualChildCount, i2);
                    return;
                }
                return;
            }
        }
    }

    private void forceUniformWidth(int i, int i2) {
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824);
        for (int i3 = 0; i3 < i; i3++) {
            View virtualChildAt = getVirtualChildAt(i3);
            if (virtualChildAt != null && virtualChildAt.getVisibility() != 8) {
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
        int[] iArr;
        int iMax;
        int i3;
        int i4;
        int i5;
        int baseline;
        int i6;
        int i7;
        boolean z;
        boolean z2;
        LayoutParams layoutParams;
        View view;
        boolean z3;
        int baseline2;
        int i8 = i;
        boolean z4 = false;
        this.mTotalLength = 0;
        int virtualChildCount = getVirtualChildCount();
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        if (this.mMaxAscent == null || this.mMaxDescent == null) {
            this.mMaxAscent = new int[4];
            this.mMaxDescent = new int[4];
        }
        int[] iArr2 = this.mMaxAscent;
        int[] iArr3 = this.mMaxDescent;
        iArr2[3] = -1;
        iArr2[2] = -1;
        iArr2[1] = -1;
        iArr2[0] = -1;
        iArr3[3] = -1;
        iArr3[2] = -1;
        iArr3[1] = -1;
        iArr3[0] = -1;
        boolean z5 = this.mBaselineAligned;
        boolean z6 = this.mUseLargestChild;
        int i9 = 1073741824;
        boolean z7 = mode == 1073741824;
        int childrenSkipCount = 0;
        int i10 = 0;
        int i11 = 0;
        int i12 = 0;
        boolean z8 = false;
        int iMax2 = 0;
        int iMax3 = 0;
        int i13 = 0;
        boolean z9 = false;
        boolean z10 = true;
        float f = 0.0f;
        int iMax4 = Integer.MIN_VALUE;
        while (true) {
            iArr = iArr3;
            if (childrenSkipCount >= virtualChildCount) {
                break;
            }
            View virtualChildAt = getVirtualChildAt(childrenSkipCount);
            if (virtualChildAt == null) {
                this.mTotalLength += measureNullChild(childrenSkipCount);
            } else if (virtualChildAt.getVisibility() == 8) {
                childrenSkipCount += getChildrenSkipCount(virtualChildAt, childrenSkipCount);
            } else {
                i10++;
                if (hasDividerBeforeChildAt(childrenSkipCount)) {
                    this.mTotalLength += this.mDividerWidth;
                }
                LayoutParams layoutParams2 = (LayoutParams) virtualChildAt.getLayoutParams();
                f += layoutParams2.weight;
                boolean z11 = (layoutParams2.width != 0 || layoutParams2.weight <= 0.0f) ? z4 : true;
                if (mode == i9 && z11) {
                    if (z7) {
                        this.mTotalLength += layoutParams2.leftMargin + layoutParams2.rightMargin;
                    } else {
                        int i14 = this.mTotalLength;
                        this.mTotalLength = Math.max(i14, layoutParams2.leftMargin + i14 + layoutParams2.rightMargin);
                    }
                    if (z5) {
                        virtualChildAt.measure(View.MeasureSpec.makeSafeMeasureSpec(View.MeasureSpec.getSize(i), 0), View.MeasureSpec.makeSafeMeasureSpec(View.MeasureSpec.getSize(i2), 0));
                        i7 = childrenSkipCount;
                        z = z6;
                        z2 = z5;
                        layoutParams = layoutParams2;
                        view = virtualChildAt;
                    } else {
                        i7 = childrenSkipCount;
                        z = z6;
                        z2 = z5;
                        layoutParams = layoutParams2;
                        view = virtualChildAt;
                        z8 = true;
                    }
                } else {
                    if (z11) {
                        layoutParams2.width = -2;
                    }
                    int i15 = childrenSkipCount;
                    i7 = i15;
                    z = z6;
                    z2 = z5;
                    layoutParams = layoutParams2;
                    view = virtualChildAt;
                    measureChildBeforeLayout(virtualChildAt, i15, i8, f == 0.0f ? this.mTotalLength : 0, i2, 0);
                    int measuredWidth = view.getMeasuredWidth();
                    if (z11) {
                        layoutParams.width = 0;
                        i12 += measuredWidth;
                    }
                    if (z7) {
                        this.mTotalLength += layoutParams.leftMargin + measuredWidth + layoutParams.rightMargin + getNextLocationOffset(view);
                    } else {
                        int i16 = this.mTotalLength;
                        this.mTotalLength = Math.max(i16, i16 + measuredWidth + layoutParams.leftMargin + layoutParams.rightMargin + getNextLocationOffset(view));
                    }
                    if (z) {
                        iMax4 = Math.max(measuredWidth, iMax4);
                    }
                }
                if (mode2 == 1073741824 || layoutParams.height != -1) {
                    z3 = false;
                } else {
                    z3 = true;
                    z9 = true;
                }
                int i17 = layoutParams.topMargin + layoutParams.bottomMargin;
                int measuredHeight = view.getMeasuredHeight() + i17;
                int iCombineMeasuredStates = combineMeasuredStates(i13, view.getMeasuredState());
                if (z2 && (baseline2 = view.getBaseline()) != -1) {
                    int i18 = ((((layoutParams.gravity < 0 ? this.mGravity : layoutParams.gravity) & 112) >> 4) & (-2)) >> 1;
                    iArr2[i18] = Math.max(iArr2[i18], baseline2);
                    iArr[i18] = Math.max(iArr[i18], measuredHeight - baseline2);
                }
                int iMax5 = Math.max(i11, measuredHeight);
                boolean z12 = z10 && layoutParams.height == -1;
                if (layoutParams.weight > 0.0f) {
                    if (!z3) {
                        i17 = measuredHeight;
                    }
                    iMax3 = Math.max(iMax3, i17);
                } else {
                    int i19 = iMax3;
                    if (!z3) {
                        i17 = measuredHeight;
                    }
                    iMax2 = Math.max(iMax2, i17);
                    iMax3 = i19;
                }
                int i20 = i7;
                childrenSkipCount = getChildrenSkipCount(view, i20) + i20;
                i13 = iCombineMeasuredStates;
                i11 = iMax5;
                z10 = z12;
                childrenSkipCount++;
                iArr3 = iArr;
                z6 = z;
                z5 = z2;
                i9 = 1073741824;
                i8 = i;
                z4 = false;
            }
            z = z6;
            z2 = z5;
            childrenSkipCount++;
            iArr3 = iArr;
            z6 = z;
            z5 = z2;
            i9 = 1073741824;
            i8 = i;
            z4 = false;
        }
        boolean z13 = z6;
        boolean z14 = z5;
        int iMax6 = i11;
        int i21 = iMax2;
        int i22 = iMax3;
        int i23 = i13;
        if (i10 > 0 && hasDividerBeforeChildAt(virtualChildCount)) {
            this.mTotalLength += this.mDividerWidth;
        }
        if (iArr2[1] != -1 || iArr2[0] != -1 || iArr2[2] != -1 || iArr2[3] != -1) {
            iMax6 = Math.max(iMax6, Math.max(iArr2[3], Math.max(iArr2[0], Math.max(iArr2[1], iArr2[2]))) + Math.max(iArr[3], Math.max(iArr[0], Math.max(iArr[1], iArr[2]))));
        }
        if (z13 && (mode == Integer.MIN_VALUE || mode == 0)) {
            this.mTotalLength = 0;
            int childrenSkipCount2 = 0;
            while (childrenSkipCount2 < virtualChildCount) {
                View virtualChildAt2 = getVirtualChildAt(childrenSkipCount2);
                if (virtualChildAt2 == null) {
                    this.mTotalLength += measureNullChild(childrenSkipCount2);
                } else if (virtualChildAt2.getVisibility() == 8) {
                    childrenSkipCount2 += getChildrenSkipCount(virtualChildAt2, childrenSkipCount2);
                } else {
                    LayoutParams layoutParams3 = (LayoutParams) virtualChildAt2.getLayoutParams();
                    if (z7) {
                        this.mTotalLength += layoutParams3.leftMargin + iMax4 + layoutParams3.rightMargin + getNextLocationOffset(virtualChildAt2);
                    } else {
                        int i24 = this.mTotalLength;
                        i6 = childrenSkipCount2;
                        this.mTotalLength = Math.max(i24, i24 + iMax4 + layoutParams3.leftMargin + layoutParams3.rightMargin + getNextLocationOffset(virtualChildAt2));
                        childrenSkipCount2 = i6 + 1;
                    }
                }
                i6 = childrenSkipCount2;
                childrenSkipCount2 = i6 + 1;
            }
        }
        this.mTotalLength += this.mPaddingLeft + this.mPaddingRight;
        int iResolveSizeAndState = resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumWidth()), i, 0);
        int i25 = (16777215 & iResolveSizeAndState) - this.mTotalLength;
        if (this.mAllowInconsistentMeasurement) {
            i12 = 0;
        }
        int i26 = i25 + i12;
        if (z8 || ((sRemeasureWeightedChildren || i26 != 0) && f > 0.0f)) {
            float f2 = this.mWeightSum > 0.0f ? this.mWeightSum : f;
            iArr2[3] = -1;
            iArr2[2] = -1;
            iArr2[1] = -1;
            iArr2[0] = -1;
            iArr[3] = -1;
            iArr[2] = -1;
            iArr[1] = -1;
            iArr[0] = -1;
            this.mTotalLength = 0;
            int i27 = i21;
            int iCombineMeasuredStates2 = i23;
            int iMax7 = -1;
            int i28 = 0;
            while (i28 < virtualChildCount) {
                View virtualChildAt3 = getVirtualChildAt(i28);
                if (virtualChildAt3 != null) {
                    i3 = iMax4;
                    if (virtualChildAt3.getVisibility() != 8) {
                        LayoutParams layoutParams4 = (LayoutParams) virtualChildAt3.getLayoutParams();
                        float f3 = layoutParams4.weight;
                        if (f3 > 0.0f) {
                            int i29 = (int) ((i26 * f3) / f2);
                            int i30 = i26 - i29;
                            f2 -= f3;
                            virtualChildAt3.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, (!this.mUseLargestChild || mode == 1073741824) ? (layoutParams4.width != 0 || (this.mAllowInconsistentMeasurement && mode != 1073741824)) ? virtualChildAt3.getMeasuredWidth() + i29 : i29 : i3), 1073741824), getChildMeasureSpec(i2, this.mPaddingTop + this.mPaddingBottom + layoutParams4.topMargin + layoutParams4.bottomMargin, layoutParams4.height));
                            iCombineMeasuredStates2 = combineMeasuredStates(iCombineMeasuredStates2, virtualChildAt3.getMeasuredState() & (-16777216));
                            i26 = i30;
                        }
                        if (z7) {
                            i4 = i26;
                            this.mTotalLength += virtualChildAt3.getMeasuredWidth() + layoutParams4.leftMargin + layoutParams4.rightMargin + getNextLocationOffset(virtualChildAt3);
                        } else {
                            i4 = i26;
                            int i31 = this.mTotalLength;
                            this.mTotalLength = Math.max(i31, virtualChildAt3.getMeasuredWidth() + i31 + layoutParams4.leftMargin + layoutParams4.rightMargin + getNextLocationOffset(virtualChildAt3));
                        }
                        boolean z15 = mode2 != 1073741824 && layoutParams4.height == -1;
                        int i32 = layoutParams4.topMargin + layoutParams4.bottomMargin;
                        int measuredHeight2 = virtualChildAt3.getMeasuredHeight() + i32;
                        iMax7 = Math.max(iMax7, measuredHeight2);
                        if (!z15) {
                            i32 = measuredHeight2;
                        }
                        int iMax8 = Math.max(i27, i32);
                        if (z10) {
                            i5 = iMax8;
                            boolean z16 = layoutParams4.height == -1;
                            if (z14 && (baseline = virtualChildAt3.getBaseline()) != -1) {
                                int i33 = ((((layoutParams4.gravity >= 0 ? this.mGravity : layoutParams4.gravity) & 112) >> 4) & (-2)) >> 1;
                                iArr2[i33] = Math.max(iArr2[i33], baseline);
                                iArr[i33] = Math.max(iArr[i33], measuredHeight2 - baseline);
                            }
                            z10 = z16;
                            i26 = i4;
                            i27 = i5;
                        } else {
                            i5 = iMax8;
                        }
                        if (z14) {
                            int i332 = ((((layoutParams4.gravity >= 0 ? this.mGravity : layoutParams4.gravity) & 112) >> 4) & (-2)) >> 1;
                            iArr2[i332] = Math.max(iArr2[i332], baseline);
                            iArr[i332] = Math.max(iArr[i332], measuredHeight2 - baseline);
                        }
                        z10 = z16;
                        i26 = i4;
                        i27 = i5;
                    }
                    i28++;
                    iMax4 = i3;
                } else {
                    i3 = iMax4;
                }
                i27 = i27;
                i28++;
                iMax4 = i3;
            }
            int i34 = i27;
            this.mTotalLength += this.mPaddingLeft + this.mPaddingRight;
            iMax6 = (iArr2[1] == -1 && iArr2[0] == -1 && iArr2[2] == -1 && iArr2[3] == -1) ? iMax7 : Math.max(iMax7, Math.max(iArr2[3], Math.max(iArr2[0], Math.max(iArr2[1], iArr2[2]))) + Math.max(iArr[3], Math.max(iArr[0], Math.max(iArr[1], iArr[2]))));
            iMax = i34;
            i23 = iCombineMeasuredStates2;
        } else {
            iMax = Math.max(i21, i22);
            if (z13 && mode != 1073741824) {
                for (int i35 = 0; i35 < virtualChildCount; i35++) {
                    View virtualChildAt4 = getVirtualChildAt(i35);
                    if (virtualChildAt4 != null && virtualChildAt4.getVisibility() != 8 && ((LayoutParams) virtualChildAt4.getLayoutParams()).weight > 0.0f) {
                        virtualChildAt4.measure(View.MeasureSpec.makeMeasureSpec(iMax4, 1073741824), View.MeasureSpec.makeMeasureSpec(virtualChildAt4.getMeasuredHeight(), 1073741824));
                    }
                }
            }
        }
        if (z10 || mode2 == 1073741824) {
            iMax = iMax6;
        }
        setMeasuredDimension(iResolveSizeAndState | ((-16777216) & i23), resolveSizeAndState(Math.max(iMax + this.mPaddingTop + this.mPaddingBottom, getSuggestedMinimumHeight()), i2, i23 << 16));
        if (z9) {
            forceUniformHeight(virtualChildCount, i);
        }
    }

    private void forceUniformHeight(int i, int i2) {
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 1073741824);
        for (int i3 = 0; i3 < i; i3++) {
            View virtualChildAt = getVirtualChildAt(i3);
            if (virtualChildAt != null && virtualChildAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) virtualChildAt.getLayoutParams();
                if (layoutParams.height == -1) {
                    int i4 = layoutParams.width;
                    layoutParams.width = virtualChildAt.getMeasuredWidth();
                    measureChildWithMargins(virtualChildAt, i2, 0, iMakeMeasureSpec, 0);
                    layoutParams.width = i4;
                }
            }
        }
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
        int i11 = this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
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
        int i17 = this.mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
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
                i |= Gravity.START;
            }
            if ((i & 112) == 0) {
                i |= 48;
            }
            this.mGravity = i;
            requestLayout();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    @RemotableViewMethod
    public void setHorizontalGravity(int i) {
        int i2 = i & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if ((8388615 & this.mGravity) != i2) {
            this.mGravity = i2 | (this.mGravity & (-8388616));
            requestLayout();
        }
    }

    @RemotableViewMethod
    public void setVerticalGravity(int i) {
        int i2 = i & 112;
        if ((this.mGravity & 112) != i2) {
            this.mGravity = i2 | (this.mGravity & PackageManager.INSTALL_FAILED_NO_MATCHING_ABIS);
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
        if (sPreserveMarginParamsInLayoutParamConversion) {
            if (layoutParams instanceof LayoutParams) {
                return new LayoutParams((LayoutParams) layoutParams);
            }
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                return new LayoutParams((ViewGroup.MarginLayoutParams) layoutParams);
            }
        }
        return new LayoutParams(layoutParams);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return LinearLayout.class.getName();
    }

    @Override
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

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT, mapping = {@ViewDebug.IntToString(from = -1, to = KeyProperties.DIGEST_NONE), @ViewDebug.IntToString(from = 0, to = KeyProperties.DIGEST_NONE), @ViewDebug.IntToString(from = 48, to = "TOP"), @ViewDebug.IntToString(from = 80, to = "BOTTOM"), @ViewDebug.IntToString(from = 3, to = "LEFT"), @ViewDebug.IntToString(from = 5, to = "RIGHT"), @ViewDebug.IntToString(from = Gravity.START, to = "START"), @ViewDebug.IntToString(from = Gravity.END, to = "END"), @ViewDebug.IntToString(from = 16, to = "CENTER_VERTICAL"), @ViewDebug.IntToString(from = 112, to = "FILL_VERTICAL"), @ViewDebug.IntToString(from = 1, to = "CENTER_HORIZONTAL"), @ViewDebug.IntToString(from = 7, to = "FILL_HORIZONTAL"), @ViewDebug.IntToString(from = 17, to = "CENTER"), @ViewDebug.IntToString(from = 119, to = "FILL")})
        public int gravity;

        @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
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

        public LayoutParams(int i, int i2, float f) {
            super(i, i2);
            this.gravity = -1;
            this.weight = f;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.gravity = -1;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            this.gravity = -1;
        }

        public LayoutParams(LayoutParams layoutParams) {
            super((ViewGroup.MarginLayoutParams) layoutParams);
            this.gravity = -1;
            this.weight = layoutParams.weight;
            this.gravity = layoutParams.gravity;
        }

        @Override
        public String debug(String str) {
            return str + "LinearLayout.LayoutParams={width=" + sizeToString(this.width) + ", height=" + sizeToString(this.height) + " weight=" + this.weight + "}";
        }

        @Override
        protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
            super.encodeProperties(viewHierarchyEncoder);
            viewHierarchyEncoder.addProperty("layout:weight", this.weight);
            viewHierarchyEncoder.addProperty("layout:gravity", this.gravity);
        }
    }
}
