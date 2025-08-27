package com.google.android.flexbox;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.flexbox.FlexboxHelper;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class FlexboxLayout extends ViewGroup implements FlexContainer {
    private int mAlignContent;
    private int mAlignItems;
    private Drawable mDividerDrawableHorizontal;
    private Drawable mDividerDrawableVertical;
    private int mDividerHorizontalHeight;
    private int mDividerVerticalWidth;
    private int mFlexDirection;
    private List<FlexLine> mFlexLines;
    private FlexboxHelper.FlexLinesResult mFlexLinesResult;
    private int mFlexWrap;
    private FlexboxHelper mFlexboxHelper;
    private int mJustifyContent;
    private SparseIntArray mOrderCache;
    private int[] mReorderedIndices;
    private int mShowDividerHorizontal;
    private int mShowDividerVertical;

    public FlexboxLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlexboxLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mFlexboxHelper = new FlexboxHelper(this);
        this.mFlexLines = new ArrayList();
        this.mFlexLinesResult = new FlexboxHelper.FlexLinesResult();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlexboxLayout, defStyleAttr, 0);
        this.mFlexDirection = a.getInt(R.styleable.FlexboxLayout_flexDirection, 0);
        this.mFlexWrap = a.getInt(R.styleable.FlexboxLayout_flexWrap, 0);
        this.mJustifyContent = a.getInt(R.styleable.FlexboxLayout_justifyContent, 0);
        this.mAlignItems = a.getInt(R.styleable.FlexboxLayout_alignItems, 4);
        this.mAlignContent = a.getInt(R.styleable.FlexboxLayout_alignContent, 5);
        Drawable drawable = a.getDrawable(R.styleable.FlexboxLayout_dividerDrawable);
        if (drawable != null) {
            setDividerDrawableHorizontal(drawable);
            setDividerDrawableVertical(drawable);
        }
        Drawable drawableHorizontal = a.getDrawable(R.styleable.FlexboxLayout_dividerDrawableHorizontal);
        if (drawableHorizontal != null) {
            setDividerDrawableHorizontal(drawableHorizontal);
        }
        Drawable drawableVertical = a.getDrawable(R.styleable.FlexboxLayout_dividerDrawableVertical);
        if (drawableVertical != null) {
            setDividerDrawableVertical(drawableVertical);
        }
        int dividerMode = a.getInt(R.styleable.FlexboxLayout_showDivider, 0);
        if (dividerMode != 0) {
            this.mShowDividerVertical = dividerMode;
            this.mShowDividerHorizontal = dividerMode;
        }
        int dividerModeVertical = a.getInt(R.styleable.FlexboxLayout_showDividerVertical, 0);
        if (dividerModeVertical != 0) {
            this.mShowDividerVertical = dividerModeVertical;
        }
        int dividerModeHorizontal = a.getInt(R.styleable.FlexboxLayout_showDividerHorizontal, 0);
        if (dividerModeHorizontal != 0) {
            this.mShowDividerHorizontal = dividerModeHorizontal;
        }
        a.recycle();
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mOrderCache == null) {
            this.mOrderCache = new SparseIntArray(getChildCount());
        }
        if (this.mFlexboxHelper.isOrderChangedFromLastMeasurement(this.mOrderCache)) {
            this.mReorderedIndices = this.mFlexboxHelper.createReorderedIndices(this.mOrderCache);
        }
        switch (this.mFlexDirection) {
            case 0:
            case 1:
                measureHorizontal(widthMeasureSpec, heightMeasureSpec);
                return;
            case 2:
            case 3:
                measureVertical(widthMeasureSpec, heightMeasureSpec);
                return;
            default:
                throw new IllegalStateException("Invalid value for the flex direction is set: " + this.mFlexDirection);
        }
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getFlexItemCount() {
        return getChildCount();
    }

    @Override // com.google.android.flexbox.FlexContainer
    public View getFlexItemAt(int index) {
        return getChildAt(index);
    }

    public View getReorderedChildAt(int index) {
        if (index < 0 || index >= this.mReorderedIndices.length) {
            return null;
        }
        return getChildAt(this.mReorderedIndices[index]);
    }

    @Override // com.google.android.flexbox.FlexContainer
    public View getReorderedFlexItemAt(int index) {
        return getReorderedChildAt(index);
    }

    @Override // android.view.ViewGroup
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (this.mOrderCache == null) {
            this.mOrderCache = new SparseIntArray(getChildCount());
        }
        this.mReorderedIndices = this.mFlexboxHelper.createReorderedIndices(child, index, params, this.mOrderCache);
        super.addView(child, index, params);
    }

    private void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        this.mFlexLines.clear();
        this.mFlexLinesResult.reset();
        this.mFlexboxHelper.calculateHorizontalFlexLines(this.mFlexLinesResult, widthMeasureSpec, heightMeasureSpec);
        this.mFlexLines = this.mFlexLinesResult.mFlexLines;
        this.mFlexboxHelper.determineMainSize(widthMeasureSpec, heightMeasureSpec);
        if (this.mAlignItems == 3) {
            for (FlexLine flexLine : this.mFlexLines) {
                int largestHeightInLine = Integer.MIN_VALUE;
                for (int i = 0; i < flexLine.mItemCount; i++) {
                    int viewIndex = flexLine.mFirstIndex + i;
                    View child = getReorderedChildAt(viewIndex);
                    if (child != null && child.getVisibility() != 8) {
                        LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        if (this.mFlexWrap != 2) {
                            int marginTop = flexLine.mMaxBaseline - child.getBaseline();
                            largestHeightInLine = Math.max(largestHeightInLine, child.getMeasuredHeight() + Math.max(marginTop, lp.topMargin) + lp.bottomMargin);
                        } else {
                            int marginBottom = (flexLine.mMaxBaseline - child.getMeasuredHeight()) + child.getBaseline();
                            largestHeightInLine = Math.max(largestHeightInLine, child.getMeasuredHeight() + lp.topMargin + Math.max(marginBottom, lp.bottomMargin));
                        }
                    }
                }
                flexLine.mCrossSize = largestHeightInLine;
            }
        }
        this.mFlexboxHelper.determineCrossSize(widthMeasureSpec, heightMeasureSpec, getPaddingTop() + getPaddingBottom());
        this.mFlexboxHelper.stretchViews();
        setMeasuredDimensionForFlex(this.mFlexDirection, widthMeasureSpec, heightMeasureSpec, this.mFlexLinesResult.mChildState);
    }

    private void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        this.mFlexLines.clear();
        this.mFlexLinesResult.reset();
        this.mFlexboxHelper.calculateVerticalFlexLines(this.mFlexLinesResult, widthMeasureSpec, heightMeasureSpec);
        this.mFlexLines = this.mFlexLinesResult.mFlexLines;
        this.mFlexboxHelper.determineMainSize(widthMeasureSpec, heightMeasureSpec);
        this.mFlexboxHelper.determineCrossSize(widthMeasureSpec, heightMeasureSpec, getPaddingLeft() + getPaddingRight());
        this.mFlexboxHelper.stretchViews();
        setMeasuredDimensionForFlex(this.mFlexDirection, widthMeasureSpec, heightMeasureSpec, this.mFlexLinesResult.mChildState);
    }

    private void setMeasuredDimensionForFlex(int flexDirection, int widthMeasureSpec, int heightMeasureSpec, int childState) {
        int calculatedMaxHeight;
        int calculatedMaxWidth;
        int widthSizeAndState;
        int heightSizeAndState;
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        switch (flexDirection) {
            case 0:
            case 1:
                int calculatedMaxHeight2 = getSumOfCrossSize();
                calculatedMaxHeight = calculatedMaxHeight2 + getPaddingTop() + getPaddingBottom();
                calculatedMaxWidth = getLargestMainSize();
                break;
            case 2:
            case 3:
                calculatedMaxHeight = getLargestMainSize();
                calculatedMaxWidth = getSumOfCrossSize() + getPaddingLeft() + getPaddingRight();
                break;
            default:
                throw new IllegalArgumentException("Invalid flex direction: " + flexDirection);
        }
        if (widthMode == Integer.MIN_VALUE) {
            if (widthSize < calculatedMaxWidth) {
                childState = View.combineMeasuredStates(childState, 16777216);
            } else {
                widthSize = calculatedMaxWidth;
            }
            widthSizeAndState = View.resolveSizeAndState(widthSize, widthMeasureSpec, childState);
        } else if (widthMode == 0) {
            widthSizeAndState = View.resolveSizeAndState(calculatedMaxWidth, widthMeasureSpec, childState);
        } else if (widthMode == 1073741824) {
            if (widthSize < calculatedMaxWidth) {
                childState = View.combineMeasuredStates(childState, 16777216);
            }
            widthSizeAndState = View.resolveSizeAndState(widthSize, widthMeasureSpec, childState);
        } else {
            throw new IllegalStateException("Unknown width mode is set: " + widthMode);
        }
        if (heightMode == Integer.MIN_VALUE) {
            if (heightSize < calculatedMaxHeight) {
                childState = View.combineMeasuredStates(childState, 256);
            } else {
                heightSize = calculatedMaxHeight;
            }
            heightSizeAndState = View.resolveSizeAndState(heightSize, heightMeasureSpec, childState);
        } else if (heightMode == 0) {
            heightSizeAndState = View.resolveSizeAndState(calculatedMaxHeight, heightMeasureSpec, childState);
        } else if (heightMode == 1073741824) {
            if (heightSize < calculatedMaxHeight) {
                childState = View.combineMeasuredStates(childState, 256);
            }
            heightSizeAndState = View.resolveSizeAndState(heightSize, heightMeasureSpec, childState);
        } else {
            throw new IllegalStateException("Unknown height mode is set: " + heightMode);
        }
        setMeasuredDimension(widthSizeAndState, heightSizeAndState);
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getLargestMainSize() {
        int largestSize = Integer.MIN_VALUE;
        for (FlexLine flexLine : this.mFlexLines) {
            largestSize = Math.max(largestSize, flexLine.mMainSize);
        }
        return largestSize;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getSumOfCrossSize() {
        int sum = 0;
        int size = this.mFlexLines.size();
        for (int i = 0; i < size; i++) {
            FlexLine flexLine = this.mFlexLines.get(i);
            if (hasDividerBeforeFlexLine(i)) {
                if (isMainAxisDirectionHorizontal()) {
                    sum += this.mDividerHorizontalHeight;
                } else {
                    sum += this.mDividerVerticalWidth;
                }
            }
            if (hasEndDividerAfterFlexLine(i)) {
                if (isMainAxisDirectionHorizontal()) {
                    sum += this.mDividerHorizontalHeight;
                } else {
                    sum += this.mDividerVerticalWidth;
                }
            }
            sum += flexLine.mCrossSize;
        }
        return sum;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public boolean isMainAxisDirectionHorizontal() {
        return this.mFlexDirection == 0 || this.mFlexDirection == 1;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        boolean isRtl;
        boolean isRtl2;
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        switch (this.mFlexDirection) {
            case 0:
                isRtl = layoutDirection == 1;
                layoutHorizontal(isRtl, left, top, right, bottom);
                isRtl2 = isRtl;
                return;
            case 1:
                isRtl = layoutDirection != 1;
                layoutHorizontal(isRtl, left, top, right, bottom);
                isRtl2 = isRtl;
                return;
            case 2:
                boolean isRtl3 = layoutDirection == 1;
                if (this.mFlexWrap == 2) {
                    isRtl3 = isRtl3 ? false : true;
                }
                isRtl2 = isRtl3;
                layoutVertical(isRtl2, false, left, top, right, bottom);
                return;
            case 3:
                boolean isRtl4 = layoutDirection == 1;
                if (this.mFlexWrap == 2) {
                    isRtl4 = isRtl4 ? false : true;
                }
                isRtl2 = isRtl4;
                layoutVertical(isRtl2, true, left, top, right, bottom);
                return;
            default:
                throw new IllegalStateException("Invalid flex direction is set: " + this.mFlexDirection);
        }
    }

    private void layoutHorizontal(boolean isRtl, int left, int top, int right, int bottom) {
        int height;
        float childLeft;
        float childRight;
        int paddingLeft;
        int width;
        int size;
        int size2;
        int endDividerLength;
        int paddingLeft2 = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int height2 = bottom - top;
        int width2 = right - left;
        int childBottom = height2 - getPaddingBottom();
        int childTop = getPaddingTop();
        int i = 0;
        int size3 = this.mFlexLines.size();
        while (i < size3) {
            FlexLine flexLine = this.mFlexLines.get(i);
            if (hasDividerBeforeFlexLine(i)) {
                childBottom -= this.mDividerHorizontalHeight;
                childTop += this.mDividerHorizontalHeight;
            }
            float spaceBetweenItem = 0.0f;
            switch (this.mJustifyContent) {
                case 0:
                    height = height2;
                    childLeft = paddingLeft2;
                    childRight = width2 - paddingRight;
                    break;
                case 1:
                    height = height2;
                    childLeft = (width2 - flexLine.mMainSize) + paddingRight;
                    childRight = flexLine.mMainSize - paddingLeft2;
                    break;
                case 2:
                    height = height2;
                    childLeft = ((width2 - flexLine.mMainSize) / 2.0f) + paddingLeft2;
                    childRight = (width2 - paddingRight) - ((width2 - flexLine.mMainSize) / 2.0f);
                    break;
                case 3:
                    height = height2;
                    childLeft = paddingLeft2;
                    int visibleItem = flexLine.getItemCountNotGone();
                    float denominator = visibleItem != 1 ? visibleItem - 1 : 1.0f;
                    spaceBetweenItem = (width2 - flexLine.mMainSize) / denominator;
                    float childRight2 = width2 - paddingRight;
                    childRight = childRight2;
                    break;
                case 4:
                    int visibleCount = flexLine.getItemCountNotGone();
                    if (visibleCount != 0) {
                        height = height2;
                        spaceBetweenItem = (width2 - flexLine.mMainSize) / visibleCount;
                    } else {
                        height = height2;
                    }
                    childLeft = paddingLeft2 + (spaceBetweenItem / 2.0f);
                    childRight = (width2 - paddingRight) - (spaceBetweenItem / 2.0f);
                    break;
                default:
                    throw new IllegalStateException("Invalid justifyContent is set: " + this.mJustifyContent);
            }
            float childLeft2 = childLeft;
            float childRight3 = childRight;
            float spaceBetweenItem2 = Math.max(spaceBetweenItem, 0.0f);
            int j = 0;
            while (true) {
                paddingLeft = paddingLeft2;
                int paddingLeft3 = flexLine.mItemCount;
                if (j < paddingLeft3) {
                    int index = flexLine.mFirstIndex + j;
                    int paddingRight2 = paddingRight;
                    View child = getReorderedChildAt(index);
                    if (child == null) {
                        width = width2;
                        size = size3;
                        size2 = j;
                    } else if (child.getVisibility() == 8) {
                        width = width2;
                        size = size3;
                        size2 = j;
                    } else {
                        LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        float childLeft3 = childLeft2 + lp.leftMargin;
                        float childRight4 = childRight3 - lp.rightMargin;
                        int beforeDividerLength = 0;
                        if (hasDividerBeforeChildAtAlongMainAxis(index, j)) {
                            beforeDividerLength = this.mDividerVerticalWidth;
                            childLeft3 += beforeDividerLength;
                            childRight4 -= beforeDividerLength;
                        }
                        if (j == flexLine.mItemCount - 1 && (this.mShowDividerVertical & 4) > 0) {
                            endDividerLength = this.mDividerVerticalWidth;
                        } else {
                            endDividerLength = 0;
                        }
                        width = width2;
                        int width3 = this.mFlexWrap;
                        size = size3;
                        if (width3 == 2) {
                            if (isRtl) {
                                size2 = j;
                                this.mFlexboxHelper.layoutSingleChildHorizontal(child, flexLine, Math.round(childRight4) - child.getMeasuredWidth(), childBottom - child.getMeasuredHeight(), Math.round(childRight4), childBottom);
                            } else {
                                size2 = j;
                                this.mFlexboxHelper.layoutSingleChildHorizontal(child, flexLine, Math.round(childLeft3), childBottom - child.getMeasuredHeight(), Math.round(childLeft3) + child.getMeasuredWidth(), childBottom);
                            }
                        } else {
                            size2 = j;
                            if (isRtl) {
                                this.mFlexboxHelper.layoutSingleChildHorizontal(child, flexLine, Math.round(childRight4) - child.getMeasuredWidth(), childTop, Math.round(childRight4), childTop + child.getMeasuredHeight());
                            } else {
                                this.mFlexboxHelper.layoutSingleChildHorizontal(child, flexLine, Math.round(childLeft3), childTop, Math.round(childLeft3) + child.getMeasuredWidth(), childTop + child.getMeasuredHeight());
                            }
                        }
                        childLeft2 = childLeft3 + child.getMeasuredWidth() + spaceBetweenItem2 + lp.rightMargin;
                        childRight3 = childRight4 - ((child.getMeasuredWidth() + spaceBetweenItem2) + lp.leftMargin);
                        if (isRtl) {
                            flexLine.updatePositionFromView(child, endDividerLength, 0, beforeDividerLength, 0);
                        } else {
                            flexLine.updatePositionFromView(child, beforeDividerLength, 0, endDividerLength, 0);
                        }
                    }
                    j = size2 + 1;
                    paddingLeft2 = paddingLeft;
                    paddingRight = paddingRight2;
                    width2 = width;
                    size3 = size;
                }
            }
            childTop += flexLine.mCrossSize;
            childBottom -= flexLine.mCrossSize;
            i++;
            height2 = height;
            paddingLeft2 = paddingLeft;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:28:0x00d5  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void layoutVertical(boolean isRtl, boolean fromBottomToTop, int left, int top, int right, int bottom) {
        int paddingRight;
        float childTop;
        float childBottom;
        float spaceBetweenItem;
        int j;
        int paddingBottom;
        int paddingBottom2;
        int height;
        int size;
        int endDividerLength;
        int paddingTop = getPaddingTop();
        int paddingBottom3 = getPaddingBottom();
        int paddingRight2 = getPaddingRight();
        int childLeft = getPaddingLeft();
        int width = right - left;
        int height2 = bottom - top;
        int childRight = width - paddingRight2;
        int i = 0;
        int size2 = this.mFlexLines.size();
        while (i < size2) {
            FlexLine flexLine = this.mFlexLines.get(i);
            if (hasDividerBeforeFlexLine(i)) {
                paddingRight = paddingRight2;
                childLeft += this.mDividerVerticalWidth;
                childRight -= this.mDividerVerticalWidth;
            } else {
                paddingRight = paddingRight2;
            }
            float spaceBetweenItem2 = 0.0f;
            switch (this.mJustifyContent) {
                case 0.0f:
                    childTop = paddingTop;
                    childBottom = height2 - paddingBottom3;
                    spaceBetweenItem = spaceBetweenItem2;
                    int paddingTop2 = paddingTop;
                    float spaceBetweenItem3 = Math.max(spaceBetweenItem, 0.0f);
                    j = 0;
                    while (true) {
                        paddingBottom = paddingBottom3;
                        paddingBottom2 = flexLine.mItemCount;
                        if (j < paddingBottom2) {
                            int index = flexLine.mFirstIndex + j;
                            int width2 = width;
                            View child = getReorderedChildAt(index);
                            if (child == null) {
                                height = height2;
                                size = size2;
                            } else if (child.getVisibility() == 8) {
                                height = height2;
                                size = size2;
                            } else {
                                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                                float childTop2 = childTop + lp.topMargin;
                                float childBottom2 = childBottom - lp.bottomMargin;
                                int beforeDividerLength = 0;
                                if (hasDividerBeforeChildAtAlongMainAxis(index, j)) {
                                    beforeDividerLength = this.mDividerHorizontalHeight;
                                    childTop2 += beforeDividerLength;
                                    childBottom2 -= beforeDividerLength;
                                }
                                if (j == flexLine.mItemCount - 1 && (this.mShowDividerHorizontal & 4) > 0) {
                                    endDividerLength = this.mDividerHorizontalHeight;
                                } else {
                                    endDividerLength = 0;
                                }
                                if (isRtl) {
                                    if (fromBottomToTop) {
                                        height = height2;
                                        this.mFlexboxHelper.layoutSingleChildVertical(child, flexLine, true, childRight - child.getMeasuredWidth(), Math.round(childBottom2) - child.getMeasuredHeight(), childRight, Math.round(childBottom2));
                                    } else {
                                        height = height2;
                                        this.mFlexboxHelper.layoutSingleChildVertical(child, flexLine, true, childRight - child.getMeasuredWidth(), Math.round(childTop2), childRight, Math.round(childTop2) + child.getMeasuredHeight());
                                    }
                                } else {
                                    height = height2;
                                    if (fromBottomToTop) {
                                        this.mFlexboxHelper.layoutSingleChildVertical(child, flexLine, false, childLeft, Math.round(childBottom2) - child.getMeasuredHeight(), childLeft + child.getMeasuredWidth(), Math.round(childBottom2));
                                    } else {
                                        this.mFlexboxHelper.layoutSingleChildVertical(child, flexLine, false, childLeft, Math.round(childTop2), childLeft + child.getMeasuredWidth(), Math.round(childTop2) + child.getMeasuredHeight());
                                    }
                                }
                                size = size2;
                                int size3 = lp.bottomMargin;
                                childTop = childTop2 + child.getMeasuredHeight() + spaceBetweenItem3 + size3;
                                childBottom = childBottom2 - ((child.getMeasuredHeight() + spaceBetweenItem3) + lp.topMargin);
                                if (fromBottomToTop) {
                                    flexLine.updatePositionFromView(child, 0, endDividerLength, 0, beforeDividerLength);
                                } else {
                                    flexLine.updatePositionFromView(child, 0, beforeDividerLength, 0, endDividerLength);
                                }
                            }
                            j++;
                            paddingBottom3 = paddingBottom;
                            width = width2;
                            height2 = height;
                            size2 = size;
                        }
                    }
                    childLeft += flexLine.mCrossSize;
                    childRight -= flexLine.mCrossSize;
                    i++;
                    paddingRight2 = paddingRight;
                    paddingTop = paddingTop2;
                    paddingBottom3 = paddingBottom;
                    break;
                case Float.MIN_VALUE:
                    childTop = (height2 - flexLine.mMainSize) + paddingBottom3;
                    childBottom = flexLine.mMainSize - paddingTop;
                    spaceBetweenItem = spaceBetweenItem2;
                    int paddingTop22 = paddingTop;
                    float spaceBetweenItem32 = Math.max(spaceBetweenItem, 0.0f);
                    j = 0;
                    while (true) {
                        paddingBottom = paddingBottom3;
                        paddingBottom2 = flexLine.mItemCount;
                        if (j < paddingBottom2) {
                            break;
                        }
                        j++;
                        paddingBottom3 = paddingBottom;
                        width = width2;
                        height2 = height;
                        size2 = size;
                    }
                    childLeft += flexLine.mCrossSize;
                    childRight -= flexLine.mCrossSize;
                    i++;
                    paddingRight2 = paddingRight;
                    paddingTop = paddingTop22;
                    paddingBottom3 = paddingBottom;
                    break;
                case 2.8E-45f:
                    float childBottom3 = paddingTop;
                    childTop = ((height2 - flexLine.mMainSize) / 2.0f) + childBottom3;
                    childBottom = (height2 - paddingBottom3) - ((height2 - flexLine.mMainSize) / 2.0f);
                    spaceBetweenItem = spaceBetweenItem2;
                    int paddingTop222 = paddingTop;
                    float spaceBetweenItem322 = Math.max(spaceBetweenItem, 0.0f);
                    j = 0;
                    while (true) {
                        paddingBottom = paddingBottom3;
                        paddingBottom2 = flexLine.mItemCount;
                        if (j < paddingBottom2) {
                        }
                        j++;
                        paddingBottom3 = paddingBottom;
                        width = width2;
                        height2 = height;
                        size2 = size;
                    }
                    childLeft += flexLine.mCrossSize;
                    childRight -= flexLine.mCrossSize;
                    i++;
                    paddingRight2 = paddingRight;
                    paddingTop = paddingTop222;
                    paddingBottom3 = paddingBottom;
                    break;
                case 4.2E-45f:
                    childTop = paddingTop;
                    int visibleItem = flexLine.getItemCountNotGone();
                    float denominator = visibleItem != 1 ? visibleItem - 1 : 1.0f;
                    float spaceBetweenItem4 = (height2 - flexLine.mMainSize) / denominator;
                    float childBottom4 = height2 - paddingBottom3;
                    childBottom = childBottom4;
                    spaceBetweenItem = spaceBetweenItem4;
                    int paddingTop2222 = paddingTop;
                    float spaceBetweenItem3222 = Math.max(spaceBetweenItem, 0.0f);
                    j = 0;
                    while (true) {
                        paddingBottom = paddingBottom3;
                        paddingBottom2 = flexLine.mItemCount;
                        if (j < paddingBottom2) {
                            break;
                        }
                        j++;
                        paddingBottom3 = paddingBottom;
                        width = width2;
                        height2 = height;
                        size2 = size;
                    }
                    childLeft += flexLine.mCrossSize;
                    childRight -= flexLine.mCrossSize;
                    i++;
                    paddingRight2 = paddingRight;
                    paddingTop = paddingTop2222;
                    paddingBottom3 = paddingBottom;
                    break;
                case 5.6E-45f:
                    int visibleCount = flexLine.getItemCountNotGone();
                    if (visibleCount != 0) {
                        spaceBetweenItem2 = (height2 - flexLine.mMainSize) / visibleCount;
                    }
                    childTop = paddingTop + (spaceBetweenItem2 / 2.0f);
                    childBottom = (height2 - paddingBottom3) - (spaceBetweenItem2 / 2.0f);
                    spaceBetweenItem = spaceBetweenItem2;
                    int paddingTop22222 = paddingTop;
                    float spaceBetweenItem32222 = Math.max(spaceBetweenItem, 0.0f);
                    j = 0;
                    while (true) {
                        paddingBottom = paddingBottom3;
                        paddingBottom2 = flexLine.mItemCount;
                        if (j < paddingBottom2) {
                        }
                        j++;
                        paddingBottom3 = paddingBottom;
                        width = width2;
                        height2 = height;
                        size2 = size;
                    }
                    childLeft += flexLine.mCrossSize;
                    childRight -= flexLine.mCrossSize;
                    i++;
                    paddingRight2 = paddingRight;
                    paddingTop = paddingTop22222;
                    paddingBottom3 = paddingBottom;
                    break;
                default:
                    throw new IllegalStateException("Invalid justifyContent is set: " + this.mJustifyContent);
            }
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.mDividerDrawableVertical == null && this.mDividerDrawableHorizontal == null) {
        }
        if (this.mShowDividerHorizontal == 0 && this.mShowDividerVertical == 0) {
            return;
        }
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        boolean fromBottomToTop = false;
        switch (this.mFlexDirection) {
            case 0:
                boolean isRtl = layoutDirection == 1;
                if (this.mFlexWrap == 2) {
                    fromBottomToTop = true;
                }
                drawDividersHorizontal(canvas, isRtl, fromBottomToTop);
                break;
            case 1:
                boolean isRtl2 = layoutDirection != 1;
                if (this.mFlexWrap == 2) {
                    fromBottomToTop = true;
                }
                drawDividersHorizontal(canvas, isRtl2, fromBottomToTop);
                break;
            case 2:
                boolean isRtl3 = layoutDirection == 1;
                if (this.mFlexWrap == 2) {
                    isRtl3 = isRtl3 ? false : true;
                }
                drawDividersVertical(canvas, isRtl3, false);
                break;
            case 3:
                boolean isRtl4 = layoutDirection == 1;
                if (this.mFlexWrap == 2) {
                    isRtl4 = isRtl4 ? false : true;
                }
                drawDividersVertical(canvas, isRtl4, true);
                break;
        }
    }

    private void drawDividersHorizontal(Canvas canvas, boolean isRtl, boolean fromBottomToTop) {
        int horizontalDividerTop;
        int horizontalDividerTop2;
        int paddingRight;
        int dividerLeft;
        int dividerLeft2;
        int paddingLeft = getPaddingLeft();
        int paddingRight2 = getPaddingRight();
        int i = 0;
        int horizontalDividerLength = Math.max(0, (getWidth() - paddingRight2) - paddingLeft);
        int i2 = 0;
        int size = this.mFlexLines.size();
        while (i2 < size) {
            FlexLine flexLine = this.mFlexLines.get(i2);
            int j = i;
            while (j < flexLine.mItemCount) {
                int viewIndex = flexLine.mFirstIndex + j;
                View view = getReorderedChildAt(viewIndex);
                if (view == null) {
                    paddingRight = paddingRight2;
                } else if (view.getVisibility() == 8) {
                    paddingRight = paddingRight2;
                } else {
                    LayoutParams lp = (LayoutParams) view.getLayoutParams();
                    if (hasDividerBeforeChildAtAlongMainAxis(viewIndex, j)) {
                        if (isRtl) {
                            dividerLeft2 = view.getRight() + lp.rightMargin;
                        } else {
                            dividerLeft2 = (view.getLeft() - lp.leftMargin) - this.mDividerVerticalWidth;
                        }
                        int dividerLeft3 = dividerLeft2;
                        int dividerLeft4 = flexLine.mTop;
                        paddingRight = paddingRight2;
                        drawVerticalDivider(canvas, dividerLeft3, dividerLeft4, flexLine.mCrossSize);
                    } else {
                        paddingRight = paddingRight2;
                    }
                    if (j == flexLine.mItemCount - 1 && (this.mShowDividerVertical & 4) > 0) {
                        if (isRtl) {
                            dividerLeft = (view.getLeft() - lp.leftMargin) - this.mDividerVerticalWidth;
                        } else {
                            int dividerLeft5 = view.getRight();
                            dividerLeft = dividerLeft5 + lp.rightMargin;
                        }
                        drawVerticalDivider(canvas, dividerLeft, flexLine.mTop, flexLine.mCrossSize);
                    }
                }
                j++;
                paddingRight2 = paddingRight;
            }
            int paddingRight3 = paddingRight2;
            if (hasDividerBeforeFlexLine(i2)) {
                if (fromBottomToTop) {
                    horizontalDividerTop2 = flexLine.mBottom;
                } else {
                    int horizontalDividerTop3 = flexLine.mTop;
                    horizontalDividerTop2 = horizontalDividerTop3 - this.mDividerHorizontalHeight;
                }
                drawHorizontalDivider(canvas, paddingLeft, horizontalDividerTop2, horizontalDividerLength);
            }
            if (hasEndDividerAfterFlexLine(i2) && (this.mShowDividerHorizontal & 4) > 0) {
                if (fromBottomToTop) {
                    horizontalDividerTop = flexLine.mTop - this.mDividerHorizontalHeight;
                } else {
                    horizontalDividerTop = flexLine.mBottom;
                }
                drawHorizontalDivider(canvas, paddingLeft, horizontalDividerTop, horizontalDividerLength);
            }
            i2++;
            paddingRight2 = paddingRight3;
            i = 0;
        }
    }

    private void drawDividersVertical(Canvas canvas, boolean isRtl, boolean fromBottomToTop) {
        int verticalDividerLeft;
        int verticalDividerLeft2;
        int paddingBottom;
        int dividerTop;
        int dividerTop2;
        int paddingTop = getPaddingTop();
        int paddingBottom2 = getPaddingBottom();
        int i = 0;
        int verticalDividerLength = Math.max(0, (getHeight() - paddingBottom2) - paddingTop);
        int i2 = 0;
        int size = this.mFlexLines.size();
        while (i2 < size) {
            FlexLine flexLine = this.mFlexLines.get(i2);
            int j = i;
            while (j < flexLine.mItemCount) {
                int viewIndex = flexLine.mFirstIndex + j;
                View view = getReorderedChildAt(viewIndex);
                if (view == null) {
                    paddingBottom = paddingBottom2;
                } else if (view.getVisibility() == 8) {
                    paddingBottom = paddingBottom2;
                } else {
                    LayoutParams lp = (LayoutParams) view.getLayoutParams();
                    if (hasDividerBeforeChildAtAlongMainAxis(viewIndex, j)) {
                        if (fromBottomToTop) {
                            dividerTop2 = view.getBottom() + lp.bottomMargin;
                        } else {
                            dividerTop2 = (view.getTop() - lp.topMargin) - this.mDividerHorizontalHeight;
                        }
                        int dividerTop3 = dividerTop2;
                        int dividerTop4 = flexLine.mLeft;
                        paddingBottom = paddingBottom2;
                        drawHorizontalDivider(canvas, dividerTop4, dividerTop3, flexLine.mCrossSize);
                    } else {
                        paddingBottom = paddingBottom2;
                    }
                    if (j == flexLine.mItemCount - 1 && (this.mShowDividerHorizontal & 4) > 0) {
                        if (fromBottomToTop) {
                            dividerTop = (view.getTop() - lp.topMargin) - this.mDividerHorizontalHeight;
                        } else {
                            int dividerTop5 = view.getBottom();
                            dividerTop = dividerTop5 + lp.bottomMargin;
                        }
                        drawHorizontalDivider(canvas, flexLine.mLeft, dividerTop, flexLine.mCrossSize);
                    }
                }
                j++;
                paddingBottom2 = paddingBottom;
            }
            int paddingBottom3 = paddingBottom2;
            if (hasDividerBeforeFlexLine(i2)) {
                if (isRtl) {
                    verticalDividerLeft2 = flexLine.mRight;
                } else {
                    int verticalDividerLeft3 = flexLine.mLeft;
                    verticalDividerLeft2 = verticalDividerLeft3 - this.mDividerVerticalWidth;
                }
                drawVerticalDivider(canvas, verticalDividerLeft2, paddingTop, verticalDividerLength);
            }
            if (hasEndDividerAfterFlexLine(i2) && (this.mShowDividerVertical & 4) > 0) {
                if (isRtl) {
                    verticalDividerLeft = flexLine.mLeft - this.mDividerVerticalWidth;
                } else {
                    verticalDividerLeft = flexLine.mRight;
                }
                drawVerticalDivider(canvas, verticalDividerLeft, paddingTop, verticalDividerLength);
            }
            i2++;
            paddingBottom2 = paddingBottom3;
            i = 0;
        }
    }

    private void drawVerticalDivider(Canvas canvas, int left, int top, int length) {
        if (this.mDividerDrawableVertical == null) {
            return;
        }
        this.mDividerDrawableVertical.setBounds(left, top, this.mDividerVerticalWidth + left, top + length);
        this.mDividerDrawableVertical.draw(canvas);
    }

    private void drawHorizontalDivider(Canvas canvas, int left, int top, int length) {
        if (this.mDividerDrawableHorizontal == null) {
            return;
        }
        this.mDividerDrawableHorizontal.setBounds(left, top, left + length, this.mDividerHorizontalHeight + top);
        this.mDividerDrawableHorizontal.draw(canvas);
    }

    @Override // android.view.ViewGroup
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /* JADX DEBUG: Method merged with bridge method: generateLayoutParams(Landroid/util/AttributeSet;)Landroid/view/ViewGroup$LayoutParams; */
    @Override // android.view.ViewGroup
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override // android.view.ViewGroup
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        }
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            return new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getFlexDirection() {
        return this.mFlexDirection;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getFlexWrap() {
        return this.mFlexWrap;
    }

    public void setFlexWrap(int flexWrap) {
        if (this.mFlexWrap != flexWrap) {
            this.mFlexWrap = flexWrap;
            requestLayout();
        }
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getAlignItems() {
        return this.mAlignItems;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getAlignContent() {
        return this.mAlignContent;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getDecorationLengthMainAxis(View view, int index, int indexInFlexLine) {
        int decorationLength;
        if (isMainAxisDirectionHorizontal()) {
            decorationLength = hasDividerBeforeChildAtAlongMainAxis(index, indexInFlexLine) ? 0 + this.mDividerVerticalWidth : 0;
            if ((this.mShowDividerVertical & 4) > 0) {
                return decorationLength + this.mDividerVerticalWidth;
            }
            return decorationLength;
        }
        decorationLength = hasDividerBeforeChildAtAlongMainAxis(index, indexInFlexLine) ? 0 + this.mDividerHorizontalHeight : 0;
        if ((this.mShowDividerHorizontal & 4) > 0) {
            return decorationLength + this.mDividerHorizontalHeight;
        }
        return decorationLength;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getDecorationLengthCrossAxis(View view) {
        return 0;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public void onNewFlexLineAdded(FlexLine flexLine) {
        if (isMainAxisDirectionHorizontal()) {
            if ((this.mShowDividerVertical & 4) > 0) {
                flexLine.mMainSize += this.mDividerVerticalWidth;
                flexLine.mDividerLengthInMainSize += this.mDividerVerticalWidth;
                return;
            }
            return;
        }
        if ((this.mShowDividerHorizontal & 4) > 0) {
            flexLine.mMainSize += this.mDividerHorizontalHeight;
            flexLine.mDividerLengthInMainSize += this.mDividerHorizontalHeight;
        }
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getChildWidthMeasureSpec(int widthSpec, int padding, int childDimension) {
        return getChildMeasureSpec(widthSpec, padding, childDimension);
    }

    @Override // com.google.android.flexbox.FlexContainer
    public int getChildHeightMeasureSpec(int heightSpec, int padding, int childDimension) {
        return getChildMeasureSpec(heightSpec, padding, childDimension);
    }

    @Override // com.google.android.flexbox.FlexContainer
    public void onNewFlexItemAdded(View view, int index, int indexInFlexLine, FlexLine flexLine) {
        if (hasDividerBeforeChildAtAlongMainAxis(index, indexInFlexLine)) {
            if (isMainAxisDirectionHorizontal()) {
                flexLine.mMainSize += this.mDividerVerticalWidth;
                flexLine.mDividerLengthInMainSize += this.mDividerVerticalWidth;
            } else {
                flexLine.mMainSize += this.mDividerHorizontalHeight;
                flexLine.mDividerLengthInMainSize += this.mDividerHorizontalHeight;
            }
        }
    }

    @Override // com.google.android.flexbox.FlexContainer
    public void setFlexLines(List<FlexLine> flexLines) {
        this.mFlexLines = flexLines;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public List<FlexLine> getFlexLinesInternal() {
        return this.mFlexLines;
    }

    @Override // com.google.android.flexbox.FlexContainer
    public void updateViewCache(int position, View view) {
    }

    public void setDividerDrawable(Drawable divider) {
        setDividerDrawableHorizontal(divider);
        setDividerDrawableVertical(divider);
    }

    public void setDividerDrawableHorizontal(Drawable divider) {
        if (divider == this.mDividerDrawableHorizontal) {
            return;
        }
        this.mDividerDrawableHorizontal = divider;
        if (divider != null) {
            this.mDividerHorizontalHeight = divider.getIntrinsicHeight();
        } else {
            this.mDividerHorizontalHeight = 0;
        }
        setWillNotDrawFlag();
        requestLayout();
    }

    public void setDividerDrawableVertical(Drawable divider) {
        if (divider == this.mDividerDrawableVertical) {
            return;
        }
        this.mDividerDrawableVertical = divider;
        if (divider != null) {
            this.mDividerVerticalWidth = divider.getIntrinsicWidth();
        } else {
            this.mDividerVerticalWidth = 0;
        }
        setWillNotDrawFlag();
        requestLayout();
    }

    public void setShowDivider(int dividerMode) {
        setShowDividerVertical(dividerMode);
        setShowDividerHorizontal(dividerMode);
    }

    public void setShowDividerVertical(int dividerMode) {
        if (dividerMode != this.mShowDividerVertical) {
            this.mShowDividerVertical = dividerMode;
            requestLayout();
        }
    }

    public void setShowDividerHorizontal(int dividerMode) {
        if (dividerMode != this.mShowDividerHorizontal) {
            this.mShowDividerHorizontal = dividerMode;
            requestLayout();
        }
    }

    private void setWillNotDrawFlag() {
        if (this.mDividerDrawableHorizontal == null && this.mDividerDrawableVertical == null) {
            setWillNotDraw(true);
        } else {
            setWillNotDraw(false);
        }
    }

    private boolean hasDividerBeforeChildAtAlongMainAxis(int index, int indexInFlexLine) {
        return allViewsAreGoneBefore(index, indexInFlexLine) ? isMainAxisDirectionHorizontal() ? (this.mShowDividerVertical & 1) != 0 : (this.mShowDividerHorizontal & 1) != 0 : isMainAxisDirectionHorizontal() ? (this.mShowDividerVertical & 2) != 0 : (this.mShowDividerHorizontal & 2) != 0;
    }

    private boolean allViewsAreGoneBefore(int index, int indexInFlexLine) {
        for (int i = 1; i <= indexInFlexLine; i++) {
            View view = getReorderedChildAt(index - i);
            if (view != null && view.getVisibility() != 8) {
                return false;
            }
        }
        return true;
    }

    private boolean hasDividerBeforeFlexLine(int flexLineIndex) {
        if (flexLineIndex < 0 || flexLineIndex >= this.mFlexLines.size()) {
            return false;
        }
        return allFlexLinesAreDummyBefore(flexLineIndex) ? isMainAxisDirectionHorizontal() ? (this.mShowDividerHorizontal & 1) != 0 : (this.mShowDividerVertical & 1) != 0 : isMainAxisDirectionHorizontal() ? (this.mShowDividerHorizontal & 2) != 0 : (this.mShowDividerVertical & 2) != 0;
    }

    private boolean allFlexLinesAreDummyBefore(int flexLineIndex) {
        for (int i = 0; i < flexLineIndex; i++) {
            if (this.mFlexLines.get(i).getItemCountNotGone() > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean hasEndDividerAfterFlexLine(int flexLineIndex) {
        if (flexLineIndex < 0 || flexLineIndex >= this.mFlexLines.size()) {
            return false;
        }
        for (int i = flexLineIndex + 1; i < this.mFlexLines.size(); i++) {
            if (this.mFlexLines.get(i).getItemCountNotGone() > 0) {
                return false;
            }
        }
        return isMainAxisDirectionHorizontal() ? (this.mShowDividerHorizontal & 4) != 0 : (this.mShowDividerVertical & 4) != 0;
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams implements FlexItem {
        public static final Parcelable.Creator<LayoutParams> CREATOR = new Parcelable.Creator<LayoutParams>() { // from class: com.google.android.flexbox.FlexboxLayout.LayoutParams.1
            /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public LayoutParams createFromParcel(Parcel source) {
                return new LayoutParams(source);
            }

            /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public LayoutParams[] newArray(int size) {
                return new LayoutParams[size];
            }
        };
        private int mAlignSelf;
        private float mFlexBasisPercent;
        private float mFlexGrow;
        private float mFlexShrink;
        private int mMaxHeight;
        private int mMaxWidth;
        private int mMinHeight;
        private int mMinWidth;
        private int mOrder;
        private boolean mWrapBefore;

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.mOrder = 1;
            this.mFlexGrow = 0.0f;
            this.mFlexShrink = 1.0f;
            this.mAlignSelf = -1;
            this.mFlexBasisPercent = -1.0f;
            this.mMaxWidth = 16777215;
            this.mMaxHeight = 16777215;
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlexboxLayout_Layout);
            this.mOrder = a.getInt(R.styleable.FlexboxLayout_Layout_layout_order, 1);
            this.mFlexGrow = a.getFloat(R.styleable.FlexboxLayout_Layout_layout_flexGrow, 0.0f);
            this.mFlexShrink = a.getFloat(R.styleable.FlexboxLayout_Layout_layout_flexShrink, 1.0f);
            this.mAlignSelf = a.getInt(R.styleable.FlexboxLayout_Layout_layout_alignSelf, -1);
            this.mFlexBasisPercent = a.getFraction(R.styleable.FlexboxLayout_Layout_layout_flexBasisPercent, 1, 1, -1.0f);
            this.mMinWidth = a.getDimensionPixelSize(R.styleable.FlexboxLayout_Layout_layout_minWidth, 0);
            this.mMinHeight = a.getDimensionPixelSize(R.styleable.FlexboxLayout_Layout_layout_minHeight, 0);
            this.mMaxWidth = a.getDimensionPixelSize(R.styleable.FlexboxLayout_Layout_layout_maxWidth, 16777215);
            this.mMaxHeight = a.getDimensionPixelSize(R.styleable.FlexboxLayout_Layout_layout_maxHeight, 16777215);
            this.mWrapBefore = a.getBoolean(R.styleable.FlexboxLayout_Layout_layout_wrapBefore, false);
            a.recycle();
        }

        public LayoutParams(LayoutParams source) {
            super((ViewGroup.MarginLayoutParams) source);
            this.mOrder = 1;
            this.mFlexGrow = 0.0f;
            this.mFlexShrink = 1.0f;
            this.mAlignSelf = -1;
            this.mFlexBasisPercent = -1.0f;
            this.mMaxWidth = 16777215;
            this.mMaxHeight = 16777215;
            this.mOrder = source.mOrder;
            this.mFlexGrow = source.mFlexGrow;
            this.mFlexShrink = source.mFlexShrink;
            this.mAlignSelf = source.mAlignSelf;
            this.mFlexBasisPercent = source.mFlexBasisPercent;
            this.mMinWidth = source.mMinWidth;
            this.mMinHeight = source.mMinHeight;
            this.mMaxWidth = source.mMaxWidth;
            this.mMaxHeight = source.mMaxHeight;
            this.mWrapBefore = source.mWrapBefore;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            this.mOrder = 1;
            this.mFlexGrow = 0.0f;
            this.mFlexShrink = 1.0f;
            this.mAlignSelf = -1;
            this.mFlexBasisPercent = -1.0f;
            this.mMaxWidth = 16777215;
            this.mMaxHeight = 16777215;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            this.mOrder = 1;
            this.mFlexGrow = 0.0f;
            this.mFlexShrink = 1.0f;
            this.mAlignSelf = -1;
            this.mFlexBasisPercent = -1.0f;
            this.mMaxWidth = 16777215;
            this.mMaxHeight = 16777215;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getWidth() {
            return this.width;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getHeight() {
            return this.height;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getOrder() {
            return this.mOrder;
        }

        @Override // com.google.android.flexbox.FlexItem
        public float getFlexGrow() {
            return this.mFlexGrow;
        }

        @Override // com.google.android.flexbox.FlexItem
        public float getFlexShrink() {
            return this.mFlexShrink;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getAlignSelf() {
            return this.mAlignSelf;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMinWidth() {
            return this.mMinWidth;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMinHeight() {
            return this.mMinHeight;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMaxWidth() {
            return this.mMaxWidth;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMaxHeight() {
            return this.mMaxHeight;
        }

        @Override // com.google.android.flexbox.FlexItem
        public boolean isWrapBefore() {
            return this.mWrapBefore;
        }

        @Override // com.google.android.flexbox.FlexItem
        public float getFlexBasisPercent() {
            return this.mFlexBasisPercent;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMarginLeft() {
            return this.leftMargin;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMarginTop() {
            return this.topMargin;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMarginRight() {
            return this.rightMargin;
        }

        @Override // com.google.android.flexbox.FlexItem
        public int getMarginBottom() {
            return this.bottomMargin;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mOrder);
            parcel.writeFloat(this.mFlexGrow);
            parcel.writeFloat(this.mFlexShrink);
            parcel.writeInt(this.mAlignSelf);
            parcel.writeFloat(this.mFlexBasisPercent);
            parcel.writeInt(this.mMinWidth);
            parcel.writeInt(this.mMinHeight);
            parcel.writeInt(this.mMaxWidth);
            parcel.writeInt(this.mMaxHeight);
            parcel.writeByte(this.mWrapBefore ? (byte) 1 : (byte) 0);
            parcel.writeInt(this.bottomMargin);
            parcel.writeInt(this.leftMargin);
            parcel.writeInt(this.rightMargin);
            parcel.writeInt(this.topMargin);
            parcel.writeInt(this.height);
            parcel.writeInt(this.width);
        }

        protected LayoutParams(Parcel in) {
            super(0, 0);
            this.mOrder = 1;
            this.mFlexGrow = 0.0f;
            this.mFlexShrink = 1.0f;
            this.mAlignSelf = -1;
            this.mFlexBasisPercent = -1.0f;
            this.mMaxWidth = 16777215;
            this.mMaxHeight = 16777215;
            this.mOrder = in.readInt();
            this.mFlexGrow = in.readFloat();
            this.mFlexShrink = in.readFloat();
            this.mAlignSelf = in.readInt();
            this.mFlexBasisPercent = in.readFloat();
            this.mMinWidth = in.readInt();
            this.mMinHeight = in.readInt();
            this.mMaxWidth = in.readInt();
            this.mMaxHeight = in.readInt();
            this.mWrapBefore = in.readByte() != 0;
            this.bottomMargin = in.readInt();
            this.leftMargin = in.readInt();
            this.rightMargin = in.readInt();
            this.topMargin = in.readInt();
            this.height = in.readInt();
            this.width = in.readInt();
        }
    }
}
