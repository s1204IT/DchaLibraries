package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class LinearLayoutCompat extends ViewGroup {
    private boolean mBaselineAligned;
    private int mBaselineAlignedChildIndex;
    private int mBaselineChildTop;
    private Drawable mDivider;
    private int mDividerHeight;
    private int mDividerPadding;
    private int mDividerWidth;
    private int mGravity;
    private int[] mMaxAscent;
    private int[] mMaxDescent;
    private int mOrientation;
    private int mShowDividers;
    private int mTotalLength;
    private boolean mUseLargestChild;
    private float mWeightSum;

    public LinearLayoutCompat(Context context) {
        this(context, null);
    }

    public LinearLayoutCompat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinearLayoutCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mBaselineAligned = true;
        this.mBaselineAlignedChildIndex = -1;
        this.mBaselineChildTop = 0;
        this.mGravity = 8388659;
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.LinearLayoutCompat, defStyleAttr, 0);
        int index = a.getInt(R.styleable.LinearLayoutCompat_android_orientation, -1);
        if (index >= 0) {
            setOrientation(index);
        }
        int index2 = a.getInt(R.styleable.LinearLayoutCompat_android_gravity, -1);
        if (index2 >= 0) {
            setGravity(index2);
        }
        boolean baselineAligned = a.getBoolean(R.styleable.LinearLayoutCompat_android_baselineAligned, true);
        if (!baselineAligned) {
            setBaselineAligned(baselineAligned);
        }
        this.mWeightSum = a.getFloat(R.styleable.LinearLayoutCompat_android_weightSum, -1.0f);
        this.mBaselineAlignedChildIndex = a.getInt(R.styleable.LinearLayoutCompat_android_baselineAlignedChildIndex, -1);
        this.mUseLargestChild = a.getBoolean(R.styleable.LinearLayoutCompat_measureWithLargestChild, false);
        setDividerDrawable(a.getDrawable(R.styleable.LinearLayoutCompat_divider));
        this.mShowDividers = a.getInt(R.styleable.LinearLayoutCompat_showDividers, 0);
        this.mDividerPadding = a.getDimensionPixelSize(R.styleable.LinearLayoutCompat_dividerPadding, 0);
        a.recycle();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public Drawable getDividerDrawable() {
        return this.mDivider;
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
        int count = getVirtualChildCount();
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != 8 && hasDividerBeforeChildAt(i)) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int top = (child.getTop() - lp.topMargin) - this.mDividerHeight;
                drawHorizontalDivider(canvas, top);
            }
        }
        if (hasDividerBeforeChildAt(count)) {
            View child2 = getVirtualChildAt(count - 1);
            if (child2 == null) {
                bottom = (getHeight() - getPaddingBottom()) - this.mDividerHeight;
            } else {
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                bottom = child2.getBottom() + lp2.bottomMargin;
            }
            drawHorizontalDivider(canvas, bottom);
        }
    }

    void drawDividersHorizontal(Canvas canvas) {
        int position;
        int position2;
        int count = getVirtualChildCount();
        boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child != null && child.getVisibility() != 8 && hasDividerBeforeChildAt(i)) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (isLayoutRtl) {
                    position2 = child.getRight() + lp.rightMargin;
                } else {
                    int position3 = child.getLeft();
                    position2 = (position3 - lp.leftMargin) - this.mDividerWidth;
                }
                drawVerticalDivider(canvas, position2);
            }
        }
        if (hasDividerBeforeChildAt(count)) {
            View child2 = getVirtualChildAt(count - 1);
            if (child2 == null) {
                if (isLayoutRtl) {
                    position = getPaddingLeft();
                } else {
                    int position4 = getWidth();
                    position = (position4 - getPaddingRight()) - this.mDividerWidth;
                }
            } else {
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                if (isLayoutRtl) {
                    int position5 = (child2.getLeft() - lp2.leftMargin) - this.mDividerWidth;
                    position = position5;
                } else {
                    int position6 = child2.getRight();
                    position = position6 + lp2.rightMargin;
                }
            }
            drawVerticalDivider(canvas, position);
        }
    }

    void drawHorizontalDivider(Canvas canvas, int top) {
        this.mDivider.setBounds(getPaddingLeft() + this.mDividerPadding, top, (getWidth() - getPaddingRight()) - this.mDividerPadding, this.mDividerHeight + top);
        this.mDivider.draw(canvas);
    }

    void drawVerticalDivider(Canvas canvas, int left) {
        this.mDivider.setBounds(left, getPaddingTop() + this.mDividerPadding, this.mDividerWidth + left, (getHeight() - getPaddingBottom()) - this.mDividerPadding);
        this.mDivider.draw(canvas);
    }

    public void setBaselineAligned(boolean baselineAligned) {
        this.mBaselineAligned = baselineAligned;
    }

    @Override
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
            if (majorGravity == 16) {
                childTop += ((((getBottom() - getTop()) - getPaddingTop()) - getPaddingBottom()) - this.mTotalLength) / 2;
            } else if (majorGravity == 80) {
                childTop = ((getBottom() - getTop()) - getPaddingBottom()) - this.mTotalLength;
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

    @Override
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
        }
        if (childIndex == getChildCount()) {
            return (this.mShowDividers & 4) != 0;
        }
        if ((this.mShowDividers & 2) == 0) {
            return false;
        }
        for (int i = childIndex - 1; i >= 0; i--) {
            if (getChildAt(i).getVisibility() != 8) {
                return true;
            }
        }
        return false;
    }

    void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        int count;
        int heightMode;
        int delta;
        float totalWeight;
        int alternativeMaxWidth;
        int largestChildHeight;
        int heightMode2;
        int baselineChildIndex;
        int delta2;
        int margin;
        float totalWeight2;
        int weightedMaxWidth;
        int alternativeMaxWidth2;
        int i;
        int i2;
        boolean skippedMeasure;
        int childState;
        int maxWidth;
        int heightMode3;
        View child;
        int count2;
        int weightedMaxWidth2;
        int alternativeMaxWidth3;
        LayoutParams lp;
        int i3;
        int i4;
        int i5;
        int weightedMaxWidth3;
        int i6 = widthMeasureSpec;
        int i7 = heightMeasureSpec;
        this.mTotalLength = 0;
        int childState2 = 0;
        float totalWeight3 = 0.0f;
        int count3 = getVirtualChildCount();
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode4 = View.MeasureSpec.getMode(heightMeasureSpec);
        boolean skippedMeasure2 = false;
        int baselineChildIndex2 = this.mBaselineAlignedChildIndex;
        boolean useLargestChild = this.mUseLargestChild;
        boolean matchWidth = false;
        int weightedMaxWidth4 = 0;
        int alternativeMaxWidth4 = 0;
        int maxWidth2 = 0;
        int margin2 = 0;
        int weightedMaxWidth5 = 0;
        int largestChildHeight2 = 1;
        while (true) {
            int weightedMaxWidth6 = margin2;
            if (maxWidth2 < count3) {
                View child2 = getVirtualChildAt(maxWidth2);
                if (child2 == null) {
                    int childState3 = childState2;
                    int childState4 = this.mTotalLength;
                    this.mTotalLength = childState4 + measureNullChild(maxWidth2);
                    count2 = count3;
                    heightMode3 = heightMode4;
                    margin2 = weightedMaxWidth6;
                    childState2 = childState3;
                } else {
                    int childState5 = childState2;
                    int childState6 = child2.getVisibility();
                    int maxWidth3 = alternativeMaxWidth4;
                    if (childState6 == 8) {
                        maxWidth2 += getChildrenSkipCount(child2, maxWidth2);
                        count2 = count3;
                        heightMode3 = heightMode4;
                        margin2 = weightedMaxWidth6;
                        childState2 = childState5;
                        alternativeMaxWidth4 = maxWidth3;
                    } else {
                        if (hasDividerBeforeChildAt(maxWidth2)) {
                            this.mTotalLength += this.mDividerHeight;
                        }
                        LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                        float totalWeight4 = totalWeight3 + lp2.weight;
                        if (heightMode4 == 1073741824 && lp2.height == 0 && lp2.weight > 0.0f) {
                            int totalLength = this.mTotalLength;
                            int i8 = lp2.topMargin + totalLength;
                            int i9 = maxWidth2;
                            int i10 = lp2.bottomMargin;
                            this.mTotalLength = Math.max(totalLength, i8 + i10);
                            lp = lp2;
                            child = child2;
                            alternativeMaxWidth3 = weightedMaxWidth4;
                            count2 = count3;
                            heightMode3 = heightMode4;
                            skippedMeasure = true;
                            weightedMaxWidth2 = weightedMaxWidth6;
                            childState = childState5;
                            maxWidth = maxWidth3;
                            i2 = i9;
                        } else {
                            int i11 = maxWidth2;
                            int oldHeight = Integer.MIN_VALUE;
                            if (lp2.height == 0 && lp2.weight > 0.0f) {
                                oldHeight = 0;
                                lp2.height = -2;
                            }
                            int oldHeight2 = oldHeight;
                            i2 = i11;
                            skippedMeasure = skippedMeasure2;
                            childState = childState5;
                            maxWidth = maxWidth3;
                            heightMode3 = heightMode4;
                            int heightMode5 = weightedMaxWidth5;
                            int largestChildHeight3 = i6;
                            child = child2;
                            count2 = count3;
                            weightedMaxWidth2 = weightedMaxWidth6;
                            int oldHeight3 = i7;
                            alternativeMaxWidth3 = weightedMaxWidth4;
                            int alternativeMaxWidth5 = totalWeight4 == 0.0f ? this.mTotalLength : 0;
                            measureChildBeforeLayout(child2, i2, largestChildHeight3, 0, oldHeight3, alternativeMaxWidth5);
                            if (oldHeight2 != Integer.MIN_VALUE) {
                                lp = lp2;
                                lp.height = oldHeight2;
                            } else {
                                lp = lp2;
                            }
                            int childHeight = child.getMeasuredHeight();
                            int totalLength2 = this.mTotalLength;
                            this.mTotalLength = Math.max(totalLength2, totalLength2 + childHeight + lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));
                            weightedMaxWidth5 = useLargestChild ? Math.max(childHeight, heightMode5) : heightMode5;
                        }
                        if (baselineChildIndex2 >= 0) {
                            i3 = i2;
                            if (baselineChildIndex2 == i3 + 1) {
                                this.mBaselineChildTop = this.mTotalLength;
                            }
                        } else {
                            i3 = i2;
                        }
                        if (i3 < baselineChildIndex2 && lp.weight > 0.0f) {
                            throw new RuntimeException("A child of LinearLayout with index less than mBaselineAlignedChildIndex has weight > 0, which won't work.  Either remove the weight, or don't set mBaselineAlignedChildIndex.");
                        }
                        boolean matchWidthLocally = false;
                        if (widthMode != 1073741824) {
                            i4 = -1;
                            if (lp.width == -1) {
                                matchWidth = true;
                                matchWidthLocally = true;
                            }
                        } else {
                            i4 = -1;
                        }
                        int margin3 = lp.leftMargin + lp.rightMargin;
                        int measuredWidth = child.getMeasuredWidth() + margin3;
                        int maxWidth4 = Math.max(maxWidth, measuredWidth);
                        int childState7 = View.combineMeasuredStates(childState, child.getMeasuredState());
                        int i12 = (largestChildHeight2 == 0 || lp.width != i4) ? 0 : 1;
                        if (lp.weight > 0.0f) {
                            i5 = i12;
                            weightedMaxWidth3 = Math.max(weightedMaxWidth2, matchWidthLocally ? margin3 : measuredWidth);
                        } else {
                            i5 = i12;
                            weightedMaxWidth3 = weightedMaxWidth2;
                            alternativeMaxWidth3 = Math.max(alternativeMaxWidth3, matchWidthLocally ? margin3 : measuredWidth);
                        }
                        maxWidth2 = i3 + getChildrenSkipCount(child, i3);
                        margin2 = weightedMaxWidth3;
                        weightedMaxWidth4 = alternativeMaxWidth3;
                        alternativeMaxWidth4 = maxWidth4;
                        childState2 = childState7;
                        totalWeight3 = totalWeight4;
                        skippedMeasure2 = skippedMeasure;
                        largestChildHeight2 = i5;
                    }
                }
                maxWidth2++;
                heightMode4 = heightMode3;
                count3 = count2;
                i6 = widthMeasureSpec;
                i7 = heightMeasureSpec;
            } else {
                int alternativeMaxWidth6 = weightedMaxWidth4;
                int count4 = count3;
                int heightMode6 = heightMode4;
                boolean skippedMeasure3 = skippedMeasure2;
                int weightedMaxWidth7 = weightedMaxWidth6;
                int childState8 = childState2;
                int maxWidth5 = alternativeMaxWidth4;
                int largestChildHeight4 = weightedMaxWidth5;
                int childState9 = this.mTotalLength;
                if (childState9 > 0) {
                    count = count4;
                    if (hasDividerBeforeChildAt(count)) {
                        this.mTotalLength += this.mDividerHeight;
                    }
                } else {
                    count = count4;
                }
                if (useLargestChild) {
                    heightMode = heightMode6;
                    if (heightMode == Integer.MIN_VALUE || heightMode == 0) {
                        this.mTotalLength = 0;
                        int i13 = 0;
                        while (i13 < count) {
                            View child3 = getVirtualChildAt(i13);
                            if (child3 == null) {
                                this.mTotalLength += measureNullChild(i13);
                            } else if (child3.getVisibility() == 8) {
                                i13 += getChildrenSkipCount(child3, i13);
                            } else {
                                LayoutParams lp3 = (LayoutParams) child3.getLayoutParams();
                                int totalLength3 = this.mTotalLength;
                                i = i13;
                                int i14 = lp3.topMargin;
                                this.mTotalLength = Math.max(totalLength3, totalLength3 + largestChildHeight4 + i14 + lp3.bottomMargin + getNextLocationOffset(child3));
                                i13 = i + 1;
                            }
                            i = i13;
                            i13 = i + 1;
                        }
                    }
                } else {
                    heightMode = heightMode6;
                }
                this.mTotalLength += getPaddingTop() + getPaddingBottom();
                int heightSize = this.mTotalLength;
                int heightSizeAndState = View.resolveSizeAndState(Math.max(heightSize, getSuggestedMinimumHeight()), heightMeasureSpec, 0);
                int heightSize2 = heightSizeAndState & 16777215;
                int delta3 = heightSize2 - this.mTotalLength;
                if (skippedMeasure3) {
                    delta = delta3;
                    totalWeight = totalWeight3;
                } else {
                    if (delta3 == 0 || totalWeight3 <= 0.0f) {
                        int alternativeMaxWidth7 = Math.max(alternativeMaxWidth6, weightedMaxWidth7);
                        if (useLargestChild) {
                            if (heightMode != 1073741824) {
                                int i15 = 0;
                                while (true) {
                                    int i16 = i15;
                                    if (i16 >= count) {
                                        break;
                                    }
                                    int delta4 = delta3;
                                    View child4 = getVirtualChildAt(i16);
                                    if (child4 != null) {
                                        totalWeight2 = totalWeight3;
                                        weightedMaxWidth = weightedMaxWidth7;
                                        if (child4.getVisibility() == 8) {
                                            alternativeMaxWidth2 = alternativeMaxWidth7;
                                        } else if (((LayoutParams) child4.getLayoutParams()).weight > 0.0f) {
                                            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(child4.getMeasuredWidth(), 1073741824);
                                            alternativeMaxWidth2 = alternativeMaxWidth7;
                                            int alternativeMaxWidth8 = View.MeasureSpec.makeMeasureSpec(largestChildHeight4, 1073741824);
                                            child4.measure(iMakeMeasureSpec, alternativeMaxWidth8);
                                        } else {
                                            alternativeMaxWidth2 = alternativeMaxWidth7;
                                        }
                                    } else {
                                        totalWeight2 = totalWeight3;
                                        weightedMaxWidth = weightedMaxWidth7;
                                        alternativeMaxWidth2 = alternativeMaxWidth7;
                                    }
                                    i15 = i16 + 1;
                                    delta3 = delta4;
                                    totalWeight3 = totalWeight2;
                                    weightedMaxWidth7 = weightedMaxWidth;
                                    alternativeMaxWidth7 = alternativeMaxWidth2;
                                }
                            }
                            alternativeMaxWidth = alternativeMaxWidth7;
                        } else {
                            alternativeMaxWidth = alternativeMaxWidth7;
                        }
                        largestChildHeight = widthMeasureSpec;
                        if (largestChildHeight2 == 0 && widthMode != 1073741824) {
                            maxWidth5 = alternativeMaxWidth;
                        }
                        setMeasuredDimension(View.resolveSizeAndState(Math.max(maxWidth5 + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth()), largestChildHeight, childState8), heightSizeAndState);
                        if (matchWidth) {
                            return;
                        }
                        forceUniformWidth(count, heightMeasureSpec);
                        return;
                    }
                    delta = delta3;
                    totalWeight = totalWeight3;
                }
                float weightSum = this.mWeightSum > 0.0f ? this.mWeightSum : totalWeight;
                this.mTotalLength = 0;
                int i17 = 0;
                int delta5 = delta;
                while (i17 < count) {
                    View child5 = getVirtualChildAt(i17);
                    boolean useLargestChild2 = useLargestChild;
                    int largestChildHeight5 = largestChildHeight4;
                    if (child5.getVisibility() == 8) {
                        heightMode2 = heightMode;
                        baselineChildIndex = baselineChildIndex2;
                    } else {
                        LayoutParams lp4 = (LayoutParams) child5.getLayoutParams();
                        float childExtra = lp4.weight;
                        if (childExtra > 0.0f) {
                            baselineChildIndex = baselineChildIndex2;
                            int share = (int) ((delta5 * childExtra) / weightSum);
                            float weightSum2 = weightSum - childExtra;
                            delta2 = delta5 - share;
                            int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight() + lp4.leftMargin + lp4.rightMargin, lp4.width);
                            if (lp4.height == 0 && heightMode == 1073741824) {
                                heightMode2 = heightMode;
                                child5.measure(childWidthMeasureSpec, View.MeasureSpec.makeMeasureSpec(share > 0 ? share : 0, 1073741824));
                                childState8 = View.combineMeasuredStates(childState8, child5.getMeasuredState() & (-256));
                                weightSum = weightSum2;
                            } else {
                                heightMode2 = heightMode;
                                int heightMode7 = child5.getMeasuredHeight();
                                int childHeight2 = heightMode7 + share;
                                if (childHeight2 < 0) {
                                    childHeight2 = 0;
                                }
                                child5.measure(childWidthMeasureSpec, View.MeasureSpec.makeMeasureSpec(childHeight2, 1073741824));
                                childState8 = View.combineMeasuredStates(childState8, child5.getMeasuredState() & (-256));
                                weightSum = weightSum2;
                            }
                        } else {
                            heightMode2 = heightMode;
                            baselineChildIndex = baselineChildIndex2;
                            delta2 = delta5;
                        }
                        int heightMode8 = lp4.leftMargin;
                        int margin4 = heightMode8 + lp4.rightMargin;
                        int measuredWidth2 = child5.getMeasuredWidth() + margin4;
                        maxWidth5 = Math.max(maxWidth5, measuredWidth2);
                        if (widthMode != 1073741824) {
                            margin = margin4;
                            int margin5 = lp4.width == -1 ? 1 : 0;
                            alternativeMaxWidth6 = Math.max(alternativeMaxWidth6, margin5 == 0 ? margin : measuredWidth2);
                            int i18 = (largestChildHeight2 == 0 && lp4.width == -1) ? 1 : 0;
                            int totalLength4 = this.mTotalLength;
                            this.mTotalLength = Math.max(totalLength4, totalLength4 + child5.getMeasuredHeight() + lp4.topMargin + lp4.bottomMargin + getNextLocationOffset(child5));
                            largestChildHeight2 = i18;
                            delta5 = delta2;
                            weightSum = weightSum;
                        } else {
                            margin = margin4;
                        }
                        alternativeMaxWidth6 = Math.max(alternativeMaxWidth6, margin5 == 0 ? margin : measuredWidth2);
                        if (largestChildHeight2 == 0) {
                        }
                        int totalLength42 = this.mTotalLength;
                        this.mTotalLength = Math.max(totalLength42, totalLength42 + child5.getMeasuredHeight() + lp4.topMargin + lp4.bottomMargin + getNextLocationOffset(child5));
                        largestChildHeight2 = i18;
                        delta5 = delta2;
                        weightSum = weightSum;
                    }
                    i17++;
                    useLargestChild = useLargestChild2;
                    largestChildHeight4 = largestChildHeight5;
                    baselineChildIndex2 = baselineChildIndex;
                    heightMode = heightMode2;
                }
                largestChildHeight = widthMeasureSpec;
                this.mTotalLength += getPaddingTop() + getPaddingBottom();
                alternativeMaxWidth = alternativeMaxWidth6;
                if (largestChildHeight2 == 0) {
                    maxWidth5 = alternativeMaxWidth;
                }
                setMeasuredDimension(View.resolveSizeAndState(Math.max(maxWidth5 + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth()), largestChildHeight, childState8), heightSizeAndState);
                if (matchWidth) {
                }
            }
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
        int childState;
        int maxHeight;
        int count;
        int maxHeight2;
        int i;
        int alternativeMaxHeight;
        int weightedMaxHeight;
        int count2;
        boolean useLargestChild;
        float weightSum;
        int delta;
        boolean allFillParent;
        int alternativeMaxHeight2;
        int largestChildWidth;
        int alternativeMaxHeight3;
        int maxHeight3;
        int i2;
        int i3;
        int i4;
        int weightedMaxHeight2;
        int alternativeMaxHeight4;
        int maxHeight4;
        boolean baselineAligned;
        int i5;
        LayoutParams lp;
        int i6;
        boolean matchHeightLocally;
        int margin;
        int margin2;
        int weightedMaxHeight3;
        int weightedMaxHeight4;
        int childBaseline;
        int i7;
        int i8 = widthMeasureSpec;
        int i9 = heightMeasureSpec;
        this.mTotalLength = 0;
        float totalWeight = 0.0f;
        int count3 = getVirtualChildCount();
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        if (this.mMaxAscent == null || this.mMaxDescent == null) {
            this.mMaxAscent = new int[4];
            this.mMaxDescent = new int[4];
        }
        int[] maxAscent = this.mMaxAscent;
        int[] maxDescent = this.mMaxDescent;
        maxAscent[3] = -1;
        maxAscent[2] = -1;
        maxAscent[1] = -1;
        maxAscent[0] = -1;
        maxDescent[3] = -1;
        maxDescent[2] = -1;
        maxDescent[1] = -1;
        maxDescent[0] = -1;
        boolean baselineAligned2 = this.mBaselineAligned;
        boolean skippedMeasure = false;
        boolean useLargestChild2 = this.mUseLargestChild;
        boolean isExactly = widthMode == 1073741824;
        int childState2 = 0;
        int largestChildWidth2 = 0;
        int largestChildWidth3 = 0;
        boolean matchHeight = true;
        int childState3 = 0;
        int maxHeight5 = 0;
        int weightedMaxHeight5 = 0;
        int alternativeMaxHeight5 = 0;
        while (maxHeight5 < count3) {
            View child = getVirtualChildAt(maxHeight5);
            if (child == null) {
                int largestChildWidth4 = largestChildWidth2;
                int largestChildWidth5 = this.mTotalLength;
                this.mTotalLength = largestChildWidth5 + measureNullChild(maxHeight5);
                baselineAligned = baselineAligned2;
                largestChildWidth2 = largestChildWidth4;
            } else {
                int largestChildWidth6 = largestChildWidth2;
                int largestChildWidth7 = child.getVisibility();
                int weightedMaxHeight6 = alternativeMaxHeight5;
                if (largestChildWidth7 == 8) {
                    maxHeight5 += getChildrenSkipCount(child, maxHeight5);
                    baselineAligned = baselineAligned2;
                    largestChildWidth2 = largestChildWidth6;
                    alternativeMaxHeight5 = weightedMaxHeight6;
                } else {
                    if (hasDividerBeforeChildAt(maxHeight5)) {
                        this.mTotalLength += this.mDividerWidth;
                    }
                    LayoutParams lp2 = (LayoutParams) child.getLayoutParams();
                    float totalWeight2 = totalWeight + lp2.weight;
                    if (widthMode == 1073741824 && lp2.width == 0 && lp2.weight > 0.0f) {
                        if (isExactly) {
                            int i10 = this.mTotalLength;
                            int i11 = lp2.leftMargin;
                            i7 = maxHeight5;
                            int i12 = lp2.rightMargin;
                            this.mTotalLength = i10 + i11 + i12;
                        } else {
                            i7 = maxHeight5;
                            int i13 = this.mTotalLength;
                            this.mTotalLength = Math.max(i13, lp2.leftMargin + i13 + lp2.rightMargin);
                        }
                        if (baselineAligned2) {
                            int freeSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
                            child.measure(freeSpec, freeSpec);
                            lp = lp2;
                            alternativeMaxHeight4 = weightedMaxHeight5;
                            maxHeight4 = childState3;
                            baselineAligned = baselineAligned2;
                            i6 = largestChildWidth6;
                            weightedMaxHeight2 = weightedMaxHeight6;
                            i4 = i7;
                            i5 = -1;
                            largestChildWidth6 = i6;
                            matchHeightLocally = false;
                            if (heightMode != 1073741824) {
                            }
                            margin = lp.topMargin + lp.bottomMargin;
                            int childHeight = child.getMeasuredHeight() + margin;
                            int childState4 = View.combineMeasuredStates(childState2, child.getMeasuredState());
                            if (baselineAligned) {
                            }
                        } else {
                            skippedMeasure = true;
                            lp = lp2;
                            alternativeMaxHeight4 = weightedMaxHeight5;
                            maxHeight4 = childState3;
                            baselineAligned = baselineAligned2;
                            weightedMaxHeight2 = weightedMaxHeight6;
                            i4 = i7;
                            i5 = -1;
                            matchHeightLocally = false;
                            if (heightMode != 1073741824) {
                            }
                            margin = lp.topMargin + lp.bottomMargin;
                            int childHeight2 = child.getMeasuredHeight() + margin;
                            int childState42 = View.combineMeasuredStates(childState2, child.getMeasuredState());
                            if (baselineAligned) {
                            }
                        }
                    } else {
                        int i14 = maxHeight5;
                        int oldWidth = Integer.MIN_VALUE;
                        if (lp2.width == 0 && lp2.weight > 0.0f) {
                            oldWidth = 0;
                            lp2.width = -2;
                        }
                        int oldWidth2 = oldWidth;
                        i4 = i14;
                        weightedMaxHeight2 = weightedMaxHeight6;
                        alternativeMaxHeight4 = weightedMaxHeight5;
                        int alternativeMaxHeight6 = i8;
                        maxHeight4 = childState3;
                        int maxHeight6 = totalWeight2 == 0.0f ? this.mTotalLength : 0;
                        int oldWidth3 = i9;
                        baselineAligned = baselineAligned2;
                        i5 = -1;
                        measureChildBeforeLayout(child, i4, alternativeMaxHeight6, maxHeight6, oldWidth3, 0);
                        if (oldWidth2 != Integer.MIN_VALUE) {
                            lp = lp2;
                            lp.width = oldWidth2;
                        } else {
                            lp = lp2;
                        }
                        int childWidth = child.getMeasuredWidth();
                        if (isExactly) {
                            this.mTotalLength += lp.leftMargin + childWidth + lp.rightMargin + getNextLocationOffset(child);
                        } else {
                            int totalLength = this.mTotalLength;
                            this.mTotalLength = Math.max(totalLength, totalLength + childWidth + lp.leftMargin + lp.rightMargin + getNextLocationOffset(child));
                        }
                        if (useLargestChild2) {
                            largestChildWidth6 = Math.max(childWidth, largestChildWidth6);
                            matchHeightLocally = false;
                            if (heightMode != 1073741824 && lp.height == i5) {
                                largestChildWidth3 = 1;
                                matchHeightLocally = true;
                            }
                            margin = lp.topMargin + lp.bottomMargin;
                            int childHeight22 = child.getMeasuredHeight() + margin;
                            int childState422 = View.combineMeasuredStates(childState2, child.getMeasuredState());
                            if (baselineAligned || (childBaseline = child.getBaseline()) == i5) {
                                margin2 = margin;
                            } else {
                                int gravity = (lp.gravity < 0 ? this.mGravity : lp.gravity) & 112;
                                int index = ((gravity >> 4) & (-2)) >> 1;
                                maxAscent[index] = Math.max(maxAscent[index], childBaseline);
                                margin2 = margin;
                                maxDescent[index] = Math.max(maxDescent[index], childHeight22 - childBaseline);
                            }
                            int maxHeight7 = Math.max(maxHeight4, childHeight22);
                            boolean allFillParent2 = !matchHeight && lp.height == -1;
                            if (lp.weight <= 0.0f) {
                                weightedMaxHeight3 = Math.max(weightedMaxHeight2, matchHeightLocally ? margin2 : childHeight22);
                                weightedMaxHeight4 = alternativeMaxHeight4;
                            } else {
                                weightedMaxHeight3 = weightedMaxHeight2;
                                weightedMaxHeight4 = Math.max(alternativeMaxHeight4, matchHeightLocally ? margin2 : childHeight22);
                            }
                            int i15 = i4;
                            maxHeight5 = i15 + getChildrenSkipCount(child, i15);
                            childState2 = childState422;
                            matchHeight = allFillParent2;
                            weightedMaxHeight5 = weightedMaxHeight4;
                            totalWeight = totalWeight2;
                            largestChildWidth2 = largestChildWidth6;
                            childState3 = maxHeight7;
                            alternativeMaxHeight5 = weightedMaxHeight3;
                        } else {
                            i6 = largestChildWidth6;
                            largestChildWidth6 = i6;
                            matchHeightLocally = false;
                            if (heightMode != 1073741824) {
                                largestChildWidth3 = 1;
                                matchHeightLocally = true;
                            }
                            margin = lp.topMargin + lp.bottomMargin;
                            int childHeight222 = child.getMeasuredHeight() + margin;
                            int childState4222 = View.combineMeasuredStates(childState2, child.getMeasuredState());
                            if (baselineAligned) {
                                margin2 = margin;
                                int maxHeight72 = Math.max(maxHeight4, childHeight222);
                                if (matchHeight) {
                                    if (lp.weight <= 0.0f) {
                                    }
                                    int i152 = i4;
                                    maxHeight5 = i152 + getChildrenSkipCount(child, i152);
                                    childState2 = childState4222;
                                    matchHeight = allFillParent2;
                                    weightedMaxHeight5 = weightedMaxHeight4;
                                    totalWeight = totalWeight2;
                                    largestChildWidth2 = largestChildWidth6;
                                    childState3 = maxHeight72;
                                    alternativeMaxHeight5 = weightedMaxHeight3;
                                }
                            }
                        }
                    }
                }
            }
            maxHeight5++;
            baselineAligned2 = baselineAligned;
            i8 = widthMeasureSpec;
            i9 = heightMeasureSpec;
        }
        int weightedMaxHeight7 = alternativeMaxHeight5;
        int maxHeight8 = childState3;
        boolean baselineAligned3 = baselineAligned2;
        int childState5 = childState2;
        int largestChildWidth8 = largestChildWidth2;
        if (this.mTotalLength > 0 && hasDividerBeforeChildAt(count3)) {
            this.mTotalLength += this.mDividerWidth;
        }
        if (maxAscent[1] == -1 && maxAscent[0] == -1 && maxAscent[2] == -1 && maxAscent[3] == -1) {
            childState = childState5;
            maxHeight = maxHeight8;
        } else {
            int ascent = Math.max(maxAscent[3], Math.max(maxAscent[0], Math.max(maxAscent[1], maxAscent[2])));
            int i16 = maxDescent[3];
            int i17 = maxDescent[0];
            int i18 = maxDescent[1];
            childState = childState5;
            int childState6 = maxDescent[2];
            int descent = Math.max(i16, Math.max(i17, Math.max(i18, childState6)));
            maxHeight = Math.max(maxHeight8, ascent + descent);
        }
        if (useLargestChild2 && (widthMode == Integer.MIN_VALUE || widthMode == 0)) {
            this.mTotalLength = 0;
            int i19 = 0;
            while (i19 < count3) {
                View child2 = getVirtualChildAt(i19);
                if (child2 == null) {
                    this.mTotalLength += measureNullChild(i19);
                    i2 = i19;
                } else if (child2.getVisibility() == 8) {
                    i3 = i19 + getChildrenSkipCount(child2, i19);
                    i19 = i3 + 1;
                } else {
                    LayoutParams lp3 = (LayoutParams) child2.getLayoutParams();
                    if (isExactly) {
                        int i20 = this.mTotalLength;
                        int i21 = lp3.leftMargin + largestChildWidth8;
                        i2 = i19;
                        int i22 = lp3.rightMargin;
                        this.mTotalLength = i20 + i21 + i22 + getNextLocationOffset(child2);
                    } else {
                        i2 = i19;
                        int i23 = this.mTotalLength;
                        this.mTotalLength = Math.max(i23, i23 + largestChildWidth8 + lp3.leftMargin + lp3.rightMargin + getNextLocationOffset(child2));
                    }
                }
                i3 = i2;
                i19 = i3 + 1;
            }
        }
        int i24 = this.mTotalLength;
        this.mTotalLength = i24 + getPaddingLeft() + getPaddingRight();
        int widthSizeAndState = View.resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumWidth()), widthMeasureSpec, 0);
        int widthSize = widthSizeAndState & 16777215;
        int delta2 = widthSize - this.mTotalLength;
        if (skippedMeasure || (delta2 != 0 && totalWeight > 0.0f)) {
            float weightSum2 = this.mWeightSum > 0.0f ? this.mWeightSum : totalWeight;
            maxAscent[3] = -1;
            maxAscent[2] = -1;
            maxAscent[1] = -1;
            maxAscent[0] = -1;
            maxDescent[3] = -1;
            maxDescent[2] = -1;
            maxDescent[1] = -1;
            maxDescent[0] = -1;
            maxHeight2 = -1;
            this.mTotalLength = 0;
            alternativeMaxHeight = weightedMaxHeight5;
            int childState7 = childState;
            float weightSum3 = weightSum2;
            int i25 = 0;
            while (i25 < count3) {
                float totalWeight3 = totalWeight;
                View child3 = getVirtualChildAt(i25);
                if (child3 != null) {
                    weightedMaxHeight = weightedMaxHeight7;
                    int weightedMaxHeight8 = child3.getVisibility();
                    useLargestChild = useLargestChild2;
                    if (weightedMaxHeight8 == 8) {
                        count2 = count3;
                    } else {
                        LayoutParams lp4 = (LayoutParams) child3.getLayoutParams();
                        float childExtra = lp4.weight;
                        if (childExtra > 0.0f) {
                            int share = (int) ((delta2 * childExtra) / weightSum3);
                            weightSum = weightSum3 - childExtra;
                            delta = delta2 - share;
                            count2 = count3;
                            int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom() + lp4.topMargin + lp4.bottomMargin, lp4.height);
                            if (lp4.width == 0 && widthMode == 1073741824) {
                                child3.measure(View.MeasureSpec.makeMeasureSpec(share > 0 ? share : 0, 1073741824), childHeightMeasureSpec);
                            } else {
                                int childWidth2 = child3.getMeasuredWidth() + share;
                                if (childWidth2 < 0) {
                                    childWidth2 = 0;
                                }
                                child3.measure(View.MeasureSpec.makeMeasureSpec(childWidth2, 1073741824), childHeightMeasureSpec);
                            }
                            childState7 = View.combineMeasuredStates(childState7, child3.getMeasuredState() & (-16777216));
                        } else {
                            count2 = count3;
                            weightSum = weightSum3;
                            delta = delta2;
                        }
                        if (isExactly) {
                            this.mTotalLength += child3.getMeasuredWidth() + lp4.leftMargin + lp4.rightMargin + getNextLocationOffset(child3);
                        } else {
                            int totalLength2 = this.mTotalLength;
                            this.mTotalLength = Math.max(totalLength2, child3.getMeasuredWidth() + totalLength2 + lp4.leftMargin + lp4.rightMargin + getNextLocationOffset(child3));
                        }
                        boolean matchHeightLocally2 = heightMode != 1073741824 && lp4.height == -1;
                        int margin3 = lp4.topMargin + lp4.bottomMargin;
                        int childHeight3 = child3.getMeasuredHeight() + margin3;
                        int maxHeight9 = Math.max(maxHeight2, childHeight3);
                        int alternativeMaxHeight7 = Math.max(alternativeMaxHeight, matchHeightLocally2 ? margin3 : childHeight3);
                        boolean matchHeightLocally3 = matchHeight && lp4.height == -1;
                        if (baselineAligned3) {
                            int childBaseline2 = child3.getBaseline();
                            allFillParent = matchHeightLocally3;
                            if (childBaseline2 != -1) {
                                int gravity2 = (lp4.gravity < 0 ? this.mGravity : lp4.gravity) & 112;
                                int index2 = ((gravity2 >> 4) & (-2)) >> 1;
                                int gravity3 = maxAscent[index2];
                                maxAscent[index2] = Math.max(gravity3, childBaseline2);
                                alternativeMaxHeight2 = alternativeMaxHeight7;
                                int alternativeMaxHeight8 = childHeight3 - childBaseline2;
                                maxDescent[index2] = Math.max(maxDescent[index2], alternativeMaxHeight8);
                            } else {
                                alternativeMaxHeight2 = alternativeMaxHeight7;
                            }
                        } else {
                            allFillParent = matchHeightLocally3;
                            alternativeMaxHeight2 = alternativeMaxHeight7;
                        }
                        maxHeight2 = maxHeight9;
                        weightSum3 = weightSum;
                        delta2 = delta;
                        matchHeight = allFillParent;
                        alternativeMaxHeight = alternativeMaxHeight2;
                    }
                } else {
                    weightedMaxHeight = weightedMaxHeight7;
                    count2 = count3;
                    useLargestChild = useLargestChild2;
                }
                i25++;
                totalWeight = totalWeight3;
                weightedMaxHeight7 = weightedMaxHeight;
                useLargestChild2 = useLargestChild;
                count3 = count2;
            }
            count = count3;
            i = heightMeasureSpec;
            this.mTotalLength += getPaddingLeft() + getPaddingRight();
            if (maxAscent[1] != -1 || maxAscent[0] != -1 || maxAscent[2] != -1 || maxAscent[3] != -1) {
                int ascent2 = Math.max(maxAscent[3], Math.max(maxAscent[0], Math.max(maxAscent[1], maxAscent[2])));
                int descent2 = Math.max(maxDescent[3], Math.max(maxDescent[0], Math.max(maxDescent[1], maxDescent[2])));
                maxHeight2 = Math.max(maxHeight2, ascent2 + descent2);
            }
            childState = childState7;
            if (!matchHeight && heightMode != 1073741824) {
                maxHeight2 = alternativeMaxHeight;
            }
            setMeasuredDimension((childState & (-16777216)) | widthSizeAndState, View.resolveSizeAndState(Math.max(maxHeight2 + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight()), i, childState << 16));
            if (largestChildWidth3 == 0) {
                forceUniformHeight(count, widthMeasureSpec);
                return;
            }
            return;
        }
        int alternativeMaxHeight9 = Math.max(weightedMaxHeight5, weightedMaxHeight7);
        if (useLargestChild2 && widthMode != 1073741824) {
            int i26 = 0;
            while (true) {
                int i27 = i26;
                if (i27 >= count3) {
                    break;
                }
                int widthSize2 = widthSize;
                View child4 = getVirtualChildAt(i27);
                if (child4 != null) {
                    alternativeMaxHeight3 = alternativeMaxHeight9;
                    int alternativeMaxHeight10 = child4.getVisibility();
                    maxHeight3 = maxHeight;
                    if (alternativeMaxHeight10 == 8) {
                        largestChildWidth = largestChildWidth8;
                    } else if (((LayoutParams) child4.getLayoutParams()).weight > 0.0f) {
                        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(largestChildWidth8, 1073741824);
                        largestChildWidth = largestChildWidth8;
                        int largestChildWidth9 = child4.getMeasuredHeight();
                        child4.measure(iMakeMeasureSpec, View.MeasureSpec.makeMeasureSpec(largestChildWidth9, 1073741824));
                    } else {
                        largestChildWidth = largestChildWidth8;
                    }
                } else {
                    largestChildWidth = largestChildWidth8;
                    alternativeMaxHeight3 = alternativeMaxHeight9;
                    maxHeight3 = maxHeight;
                }
                i26 = i27 + 1;
                widthSize = widthSize2;
                alternativeMaxHeight9 = alternativeMaxHeight3;
                maxHeight = maxHeight3;
                largestChildWidth8 = largestChildWidth;
            }
        }
        count = count3;
        alternativeMaxHeight = alternativeMaxHeight9;
        maxHeight2 = maxHeight;
        i = heightMeasureSpec;
        if (!matchHeight) {
            maxHeight2 = alternativeMaxHeight;
        }
        setMeasuredDimension((childState & (-16777216)) | widthSizeAndState, View.resolveSizeAndState(Math.max(maxHeight2 + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight()), i, childState << 16));
        if (largestChildWidth3 == 0) {
        }
    }

    private void forceUniformHeight(int count, int widthMeasureSpec) {
        int uniformMeasureSpec = View.MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 1073741824);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.height == -1) {
                    int oldWidth = lp.width;
                    lp.width = child.getMeasuredWidth();
                    measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0);
                    lp.width = oldWidth;
                }
            }
        }
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (this.mOrientation == 1) {
            layoutVertical(l, t, r, b);
        } else {
            layoutHorizontal(l, t, r, b);
        }
    }

    void layoutVertical(int left, int top, int right, int bottom) {
        int childTop;
        int majorGravity;
        int paddingLeft;
        int childLeft;
        int paddingLeft2 = getPaddingLeft();
        int width = right - left;
        int childRight = width - getPaddingRight();
        int childSpace = (width - paddingLeft2) - getPaddingRight();
        int count = getVirtualChildCount();
        int majorGravity2 = this.mGravity & 112;
        int minorGravity = this.mGravity & 8388615;
        if (majorGravity2 == 16) {
            int childTop2 = getPaddingTop();
            childTop = childTop2 + (((bottom - top) - this.mTotalLength) / 2);
        } else if (majorGravity2 == 80) {
            childTop = ((getPaddingTop() + bottom) - top) - this.mTotalLength;
        } else {
            childTop = getPaddingTop();
        }
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= count) {
                return;
            }
            View child = getVirtualChildAt(i2);
            if (child == null) {
                childTop += measureNullChild(i2);
                majorGravity = majorGravity2;
                paddingLeft = paddingLeft2;
            } else if (child.getVisibility() != 8) {
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int gravity = lp.gravity;
                if (gravity < 0) {
                    gravity = minorGravity;
                }
                int layoutDirection = ViewCompat.getLayoutDirection(this);
                int absoluteGravity = GravityCompat.getAbsoluteGravity(gravity, layoutDirection);
                int gravity2 = absoluteGravity & 7;
                majorGravity = majorGravity2;
                if (gravity2 == 1) {
                    int childLeft2 = childSpace - childWidth;
                    childLeft = (((childLeft2 / 2) + paddingLeft2) + lp.leftMargin) - lp.rightMargin;
                } else if (gravity2 == 5) {
                    childLeft = (childRight - childWidth) - lp.rightMargin;
                } else {
                    childLeft = lp.leftMargin + paddingLeft2;
                }
                if (hasDividerBeforeChildAt(i2)) {
                    childTop += this.mDividerHeight;
                }
                int childTop3 = childTop + lp.topMargin;
                int childTop4 = getLocationOffset(child);
                int i3 = childTop3 + childTop4;
                paddingLeft = paddingLeft2;
                setChildFrame(child, childLeft, i3, childWidth, childHeight);
                int childTop5 = childTop3 + childHeight + lp.bottomMargin + getNextLocationOffset(child);
                i2 += getChildrenSkipCount(child, i2);
                childTop = childTop5;
            } else {
                majorGravity = majorGravity2;
                paddingLeft = paddingLeft2;
            }
            i = i2 + 1;
            majorGravity2 = majorGravity;
            paddingLeft2 = paddingLeft;
        }
    }

    void layoutHorizontal(int left, int top, int right, int bottom) {
        int childLeft;
        int majorGravity;
        int[] maxDescent;
        int[] maxAscent;
        boolean baselineAligned;
        int count;
        boolean isLayoutRtl;
        int childBaseline;
        int gravity;
        int i;
        int childTop;
        boolean isLayoutRtl2 = ViewUtils.isLayoutRtl(this);
        int paddingTop = getPaddingTop();
        int height = bottom - top;
        int childBottom = height - getPaddingBottom();
        int childSpace = (height - paddingTop) - getPaddingBottom();
        int count2 = getVirtualChildCount();
        int majorGravity2 = this.mGravity & 8388615;
        int minorGravity = this.mGravity & 112;
        boolean baselineAligned2 = this.mBaselineAligned;
        int[] maxAscent2 = this.mMaxAscent;
        int[] maxDescent2 = this.mMaxDescent;
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        int absoluteGravity = GravityCompat.getAbsoluteGravity(majorGravity2, layoutDirection);
        if (absoluteGravity == 1) {
            int childLeft2 = getPaddingLeft();
            int layoutDirection2 = this.mTotalLength;
            childLeft = childLeft2 + (((right - left) - layoutDirection2) / 2);
        } else if (absoluteGravity == 5) {
            int childLeft3 = getPaddingLeft();
            childLeft = ((childLeft3 + right) - left) - this.mTotalLength;
        } else {
            childLeft = getPaddingLeft();
        }
        int childLeft4 = childLeft;
        int start = 0;
        int dir = 1;
        if (isLayoutRtl2) {
            start = count2 - 1;
            dir = -1;
        }
        int i2 = 0;
        int childLeft5 = childLeft4;
        while (true) {
            int i3 = i2;
            if (i3 >= count2) {
                return;
            }
            int childIndex = start + (dir * i3);
            View child = getVirtualChildAt(childIndex);
            if (child == null) {
                childLeft5 += measureNullChild(childIndex);
                maxDescent = maxDescent2;
                maxAscent = maxAscent2;
                baselineAligned = baselineAligned2;
                majorGravity = majorGravity2;
                count = count2;
                isLayoutRtl = isLayoutRtl2;
            } else {
                int i4 = child.getVisibility();
                majorGravity = majorGravity2;
                if (i4 == 8) {
                    maxDescent = maxDescent2;
                    maxAscent = maxAscent2;
                    baselineAligned = baselineAligned2;
                    count = count2;
                    isLayoutRtl = isLayoutRtl2;
                    i3 = i3;
                } else {
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    if (!baselineAligned2) {
                        baselineAligned = baselineAligned2;
                    } else {
                        baselineAligned = baselineAligned2;
                        if (lp.height != -1) {
                            childBaseline = child.getBaseline();
                        }
                        gravity = lp.gravity;
                        if (gravity < 0) {
                            gravity = minorGravity;
                        }
                        i = gravity & 112;
                        count = count2;
                        if (i != 16) {
                            int childTop2 = childSpace - childHeight;
                            childTop = (((childTop2 / 2) + paddingTop) + lp.topMargin) - lp.bottomMargin;
                        } else if (i == 48) {
                            int childTop3 = lp.topMargin;
                            childTop = childTop3 + paddingTop;
                            if (childBaseline != -1) {
                                childTop += maxAscent2[1] - childBaseline;
                            }
                        } else if (i == 80) {
                            childTop = (childBottom - childHeight) - lp.bottomMargin;
                            if (childBaseline != -1) {
                                int descent = child.getMeasuredHeight() - childBaseline;
                                childTop -= maxDescent2[2] - descent;
                            }
                        } else {
                            childTop = paddingTop;
                        }
                        if (hasDividerBeforeChildAt(childIndex)) {
                            childLeft5 += this.mDividerWidth;
                        }
                        int childLeft6 = childLeft5 + lp.leftMargin;
                        maxDescent = maxDescent2;
                        maxAscent = maxAscent2;
                        isLayoutRtl = isLayoutRtl2;
                        setChildFrame(child, childLeft6 + getLocationOffset(child), childTop, childWidth, childHeight);
                        childLeft5 = childLeft6 + childWidth + lp.rightMargin + getNextLocationOffset(child);
                        i3 += getChildrenSkipCount(child, childIndex);
                    }
                    childBaseline = -1;
                    gravity = lp.gravity;
                    if (gravity < 0) {
                    }
                    i = gravity & 112;
                    count = count2;
                    if (i != 16) {
                    }
                    if (hasDividerBeforeChildAt(childIndex)) {
                    }
                    int childLeft62 = childLeft5 + lp.leftMargin;
                    maxDescent = maxDescent2;
                    maxAscent = maxAscent2;
                    isLayoutRtl = isLayoutRtl2;
                    setChildFrame(child, childLeft62 + getLocationOffset(child), childTop, childWidth, childHeight);
                    childLeft5 = childLeft62 + childWidth + lp.rightMargin + getNextLocationOffset(child);
                    i3 += getChildrenSkipCount(child, childIndex);
                }
            }
            i2 = i3 + 1;
            majorGravity2 = majorGravity;
            baselineAligned2 = baselineAligned;
            maxDescent2 = maxDescent;
            count2 = count;
            maxAscent2 = maxAscent;
            isLayoutRtl2 = isLayoutRtl;
        }
    }

    private void setChildFrame(View child, int left, int top, int width, int height) {
        child.layout(left, top, left + width, top + height);
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            requestLayout();
        }
    }

    public void setGravity(int gravity) {
        if (this.mGravity != gravity) {
            if ((8388615 & gravity) == 0) {
                gravity |= 8388611;
            }
            if ((gravity & 112) == 0) {
                gravity |= 48;
            }
            this.mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
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
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(LinearLayoutCompat.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(LinearLayoutCompat.class.getName());
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public int gravity;
        public float weight;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.gravity = -1;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.LinearLayoutCompat_Layout);
            this.weight = a.getFloat(R.styleable.LinearLayoutCompat_Layout_android_layout_weight, 0.0f);
            this.gravity = a.getInt(R.styleable.LinearLayoutCompat_Layout_android_layout_gravity, -1);
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
    }
}
