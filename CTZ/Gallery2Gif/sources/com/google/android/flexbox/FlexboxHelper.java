package com.google.android.flexbox;

import android.support.v4.view.MarginLayoutParamsCompat;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.gallerybasic.base.Generator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class FlexboxHelper {
    static final boolean $assertionsDisabled = false;
    private boolean[] mChildrenFrozen;
    private final FlexContainer mFlexContainer;
    int[] mIndexToFlexLine;
    long[] mMeasureSpecCache;
    private long[] mMeasuredSizeCache;

    int[] createReorderedIndices(View viewBeforeAdded, int indexForViewBeforeAdded, ViewGroup.LayoutParams layoutParams, SparseIntArray orderCache) {
        int childCount = this.mFlexContainer.getFlexItemCount();
        List<Order> orders = createOrders(childCount);
        Order orderForViewToBeAdded = new Order();
        if (viewBeforeAdded != null && (layoutParams instanceof FlexItem)) {
            orderForViewToBeAdded.order = ((FlexItem) layoutParams).getOrder();
        } else {
            orderForViewToBeAdded.order = 1;
        }
        if (indexForViewBeforeAdded != -1 && indexForViewBeforeAdded != childCount && indexForViewBeforeAdded < this.mFlexContainer.getFlexItemCount()) {
            orderForViewToBeAdded.index = indexForViewBeforeAdded;
            for (int i = indexForViewBeforeAdded; i < childCount; i++) {
                orders.get(i).index++;
            }
        } else {
            orderForViewToBeAdded.index = childCount;
        }
        orders.add(orderForViewToBeAdded);
        return sortOrdersIntoReorderedIndices(childCount + 1, orders, orderCache);
    }

    int[] createReorderedIndices(SparseIntArray orderCache) {
        int childCount = this.mFlexContainer.getFlexItemCount();
        List<Order> orders = createOrders(childCount);
        return sortOrdersIntoReorderedIndices(childCount, orders, orderCache);
    }

    private List<Order> createOrders(int childCount) {
        List<Order> orders = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            View child = this.mFlexContainer.getFlexItemAt(i);
            FlexItem flexItem = (FlexItem) child.getLayoutParams();
            Order order = new Order();
            order.order = flexItem.getOrder();
            order.index = i;
            orders.add(order);
        }
        return orders;
    }

    boolean isOrderChangedFromLastMeasurement(SparseIntArray orderCache) {
        int childCount = this.mFlexContainer.getFlexItemCount();
        if (orderCache.size() != childCount) {
            return true;
        }
        for (int i = 0; i < childCount; i++) {
            View view = this.mFlexContainer.getFlexItemAt(i);
            if (view != null) {
                FlexItem flexItem = (FlexItem) view.getLayoutParams();
                if (flexItem.getOrder() != orderCache.get(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int[] sortOrdersIntoReorderedIndices(int childCount, List<Order> orders, SparseIntArray orderCache) {
        Collections.sort(orders);
        orderCache.clear();
        int[] reorderedIndices = new int[childCount];
        int i = 0;
        for (Order order : orders) {
            reorderedIndices[i] = order.index;
            orderCache.append(order.index, order.order);
            i++;
        }
        return reorderedIndices;
    }

    void calculateHorizontalFlexLines(FlexLinesResult result, int widthMeasureSpec, int heightMeasureSpec) {
        calculateFlexLines(result, widthMeasureSpec, heightMeasureSpec, Integer.MAX_VALUE, 0, -1, null);
    }

    void calculateHorizontalFlexLines(FlexLinesResult result, int widthMeasureSpec, int heightMeasureSpec, int needsCalcAmount, int fromIndex, List<FlexLine> existingLines) {
        calculateFlexLines(result, widthMeasureSpec, heightMeasureSpec, needsCalcAmount, fromIndex, -1, existingLines);
    }

    void calculateHorizontalFlexLinesToIndex(FlexLinesResult result, int widthMeasureSpec, int heightMeasureSpec, int needsCalcAmount, int toIndex, List<FlexLine> existingLines) {
        calculateFlexLines(result, widthMeasureSpec, heightMeasureSpec, needsCalcAmount, 0, toIndex, existingLines);
    }

    void calculateVerticalFlexLines(FlexLinesResult result, int widthMeasureSpec, int heightMeasureSpec) {
        calculateFlexLines(result, heightMeasureSpec, widthMeasureSpec, Integer.MAX_VALUE, 0, -1, null);
    }

    void calculateVerticalFlexLines(FlexLinesResult result, int widthMeasureSpec, int heightMeasureSpec, int needsCalcAmount, int fromIndex, List<FlexLine> existingLines) {
        calculateFlexLines(result, heightMeasureSpec, widthMeasureSpec, needsCalcAmount, fromIndex, -1, existingLines);
    }

    void calculateVerticalFlexLinesToIndex(FlexLinesResult result, int widthMeasureSpec, int heightMeasureSpec, int needsCalcAmount, int toIndex, List<FlexLine> existingLines) {
        calculateFlexLines(result, heightMeasureSpec, widthMeasureSpec, needsCalcAmount, 0, toIndex, existingLines);
    }

    void calculateFlexLines(FlexLinesResult result, int mainMeasureSpec, int crossMeasureSpec, int needsCalcAmount, int fromIndex, int toIndex, List<FlexLine> existingLines) {
        List<FlexLine> flexLines;
        List<FlexLine> flexLines2;
        int mainSize;
        int mainMode;
        int i;
        int childMainMeasureSpec;
        int childCrossMeasureSpec;
        List<FlexLine> flexLines3;
        int mainSize2;
        View child;
        int i2;
        int indexInFlexLine;
        int sumCrossSize;
        int i3;
        int sumCrossSize2;
        int sumCrossSize3;
        int childCrossMeasureSpec2;
        int sumCrossSize4 = mainMeasureSpec;
        int i4 = toIndex;
        boolean isMainHorizontal = this.mFlexContainer.isMainAxisDirectionHorizontal();
        int mainMode2 = View.MeasureSpec.getMode(mainMeasureSpec);
        int mainSize3 = View.MeasureSpec.getSize(mainMeasureSpec);
        if (existingLines == null) {
            flexLines = new ArrayList<>();
        } else {
            flexLines = existingLines;
        }
        List<FlexLine> flexLines4 = flexLines;
        result.mFlexLines = flexLines4;
        boolean reachedToIndex = i4 == -1;
        int mainPaddingStart = getPaddingStartMain(isMainHorizontal);
        int mainPaddingEnd = getPaddingEndMain(isMainHorizontal);
        int crossPaddingStart = getPaddingStartCross(isMainHorizontal);
        int crossPaddingEnd = getPaddingEndCross(isMainHorizontal);
        int childCount = 0;
        int indexInFlexLine2 = 0;
        FlexLine flexLine = new FlexLine();
        flexLine.mFirstIndex = fromIndex;
        flexLine.mMainSize = mainPaddingStart + mainPaddingEnd;
        int childCount2 = this.mFlexContainer.getFlexItemCount();
        boolean reachedToIndex2 = reachedToIndex;
        int largestSizeInCross = Integer.MIN_VALUE;
        int largestSizeInCross2 = 0;
        int childState = fromIndex;
        while (true) {
            int largestSizeInCross3 = childState;
            if (largestSizeInCross3 >= childCount2) {
                break;
            }
            View child2 = this.mFlexContainer.getReorderedFlexItemAt(largestSizeInCross3);
            if (child2 == null) {
                if (isLastFlexItem(largestSizeInCross3, childCount2, flexLine)) {
                    addFlexLine(flexLines4, flexLine, largestSizeInCross3, childCount);
                }
            } else if (child2.getVisibility() == 8) {
                flexLine.mGoneItemCount++;
                flexLine.mItemCount++;
                if (isLastFlexItem(largestSizeInCross3, childCount2, flexLine)) {
                    addFlexLine(flexLines4, flexLine, largestSizeInCross3, childCount);
                }
            } else {
                FlexItem flexItem = (FlexItem) child2.getLayoutParams();
                int childCount3 = childCount2;
                if (flexItem.getAlignSelf() == 4) {
                    flexLine.mIndicesAlignSelfStretch.add(Integer.valueOf(largestSizeInCross3));
                }
                int childMainSize = getFlexItemSizeMain(flexItem, isMainHorizontal);
                if (flexItem.getFlexBasisPercent() != -1.0f && mainMode2 == 1073741824) {
                    childMainSize = Math.round(mainSize3 * flexItem.getFlexBasisPercent());
                }
                int childMainSize2 = childMainSize;
                if (isMainHorizontal) {
                    flexLines2 = flexLines4;
                    childMainMeasureSpec = this.mFlexContainer.getChildWidthMeasureSpec(sumCrossSize4, mainPaddingStart + mainPaddingEnd + getFlexItemMarginStartMain(flexItem, true) + getFlexItemMarginEndMain(flexItem, true), childMainSize2);
                    FlexContainer flexContainer = this.mFlexContainer;
                    mainSize = mainSize3;
                    int mainSize4 = crossPaddingStart + crossPaddingEnd + getFlexItemMarginStartCross(flexItem, true) + getFlexItemMarginEndCross(flexItem, true) + childCount;
                    mainMode = mainMode2;
                    int mainMode3 = getFlexItemSizeCross(flexItem, true);
                    int childCrossMeasureSpec3 = flexContainer.getChildHeightMeasureSpec(crossMeasureSpec, mainSize4, mainMode3);
                    child2.measure(childMainMeasureSpec, childCrossMeasureSpec3);
                    updateMeasureCache(largestSizeInCross3, childMainMeasureSpec, childCrossMeasureSpec3, child2);
                    childCrossMeasureSpec = childCrossMeasureSpec3;
                    i = 0;
                } else {
                    flexLines2 = flexLines4;
                    mainSize = mainSize3;
                    mainMode = mainMode2;
                    i = 0;
                    int childCrossMeasureSpec4 = this.mFlexContainer.getChildWidthMeasureSpec(crossMeasureSpec, crossPaddingStart + crossPaddingEnd + getFlexItemMarginStartCross(flexItem, false) + getFlexItemMarginEndCross(flexItem, false) + childCount, getFlexItemSizeCross(flexItem, false));
                    childMainMeasureSpec = this.mFlexContainer.getChildHeightMeasureSpec(sumCrossSize4, mainPaddingStart + mainPaddingEnd + getFlexItemMarginStartMain(flexItem, false) + getFlexItemMarginEndMain(flexItem, false), childMainSize2);
                    child2.measure(childCrossMeasureSpec4, childMainMeasureSpec);
                    updateMeasureCache(largestSizeInCross3, childCrossMeasureSpec4, childMainMeasureSpec, child2);
                    childCrossMeasureSpec = childCrossMeasureSpec4;
                }
                int childMainMeasureSpec2 = childMainMeasureSpec;
                this.mFlexContainer.updateViewCache(largestSizeInCross3, child2);
                checkSizeConstraints(child2, largestSizeInCross3);
                int childState2 = View.combineMeasuredStates(largestSizeInCross2, child2.getMeasuredState());
                int i5 = mainMode;
                int sumCrossSize5 = childCount;
                int sumCrossSize6 = mainSize;
                int childCrossMeasureSpec5 = childCrossMeasureSpec;
                int i6 = i;
                FlexLine flexLine2 = flexLine;
                flexLines3 = flexLines2;
                mainSize2 = mainSize;
                if (isWrapRequired(child2, i5, sumCrossSize6, flexLine.mMainSize, getViewMeasuredSizeMain(child2, isMainHorizontal) + getFlexItemMarginStartMain(flexItem, isMainHorizontal) + getFlexItemMarginEndMain(flexItem, isMainHorizontal), flexItem, largestSizeInCross3, indexInFlexLine2)) {
                    if (flexLine2.getItemCountNotGone() > 0) {
                        i2 = largestSizeInCross3;
                        addFlexLine(flexLines3, flexLine2, i2 > 0 ? i2 - 1 : i6, sumCrossSize5);
                        sumCrossSize3 = sumCrossSize5 + flexLine2.mCrossSize;
                    } else {
                        i2 = largestSizeInCross3;
                        sumCrossSize3 = sumCrossSize5;
                    }
                    if (isMainHorizontal) {
                        if (flexItem.getHeight() == -1) {
                            childCrossMeasureSpec2 = this.mFlexContainer.getChildHeightMeasureSpec(crossMeasureSpec, this.mFlexContainer.getPaddingTop() + this.mFlexContainer.getPaddingBottom() + flexItem.getMarginTop() + flexItem.getMarginBottom() + sumCrossSize3, flexItem.getHeight());
                            child = child2;
                            child.measure(childMainMeasureSpec2, childCrossMeasureSpec2);
                            checkSizeConstraints(child, i2);
                            flexLine = new FlexLine();
                            flexLine.mItemCount = 1;
                            flexLine.mMainSize = mainPaddingStart + mainPaddingEnd;
                            flexLine.mFirstIndex = i2;
                            indexInFlexLine = 0;
                            largestSizeInCross = Integer.MIN_VALUE;
                            sumCrossSize5 = sumCrossSize3;
                        } else {
                            child = child2;
                            childCrossMeasureSpec2 = childCrossMeasureSpec5;
                            flexLine = new FlexLine();
                            flexLine.mItemCount = 1;
                            flexLine.mMainSize = mainPaddingStart + mainPaddingEnd;
                            flexLine.mFirstIndex = i2;
                            indexInFlexLine = 0;
                            largestSizeInCross = Integer.MIN_VALUE;
                            sumCrossSize5 = sumCrossSize3;
                        }
                    } else {
                        child = child2;
                        if (flexItem.getWidth() == -1) {
                            childCrossMeasureSpec2 = this.mFlexContainer.getChildWidthMeasureSpec(crossMeasureSpec, this.mFlexContainer.getPaddingLeft() + this.mFlexContainer.getPaddingRight() + flexItem.getMarginLeft() + flexItem.getMarginRight() + sumCrossSize3, flexItem.getWidth());
                            child.measure(childCrossMeasureSpec2, childMainMeasureSpec2);
                            checkSizeConstraints(child, i2);
                        } else {
                            childCrossMeasureSpec2 = childCrossMeasureSpec5;
                        }
                        flexLine = new FlexLine();
                        flexLine.mItemCount = 1;
                        flexLine.mMainSize = mainPaddingStart + mainPaddingEnd;
                        flexLine.mFirstIndex = i2;
                        indexInFlexLine = 0;
                        largestSizeInCross = Integer.MIN_VALUE;
                        sumCrossSize5 = sumCrossSize3;
                    }
                } else {
                    child = child2;
                    i2 = largestSizeInCross3;
                    flexLine2.mItemCount++;
                    flexLine = flexLine2;
                    indexInFlexLine = indexInFlexLine2 + 1;
                }
                int largestSizeInCross4 = largestSizeInCross;
                if (this.mIndexToFlexLine != null) {
                    this.mIndexToFlexLine[i2] = flexLines3.size();
                }
                flexLine.mMainSize += getViewMeasuredSizeMain(child, isMainHorizontal) + getFlexItemMarginStartMain(flexItem, isMainHorizontal) + getFlexItemMarginEndMain(flexItem, isMainHorizontal);
                flexLine.mTotalFlexGrow += flexItem.getFlexGrow();
                flexLine.mTotalFlexShrink += flexItem.getFlexShrink();
                this.mFlexContainer.onNewFlexItemAdded(child, i2, indexInFlexLine, flexLine);
                int largestSizeInCross5 = Math.max(largestSizeInCross4, getViewMeasuredSizeCross(child, isMainHorizontal) + getFlexItemMarginStartCross(flexItem, isMainHorizontal) + getFlexItemMarginEndCross(flexItem, isMainHorizontal) + this.mFlexContainer.getDecorationLengthCrossAxis(child));
                flexLine.mCrossSize = Math.max(flexLine.mCrossSize, largestSizeInCross5);
                if (isMainHorizontal) {
                    if (this.mFlexContainer.getFlexWrap() != 2) {
                        flexLine.mMaxBaseline = Math.max(flexLine.mMaxBaseline, child.getBaseline() + flexItem.getMarginTop());
                    } else {
                        flexLine.mMaxBaseline = Math.max(flexLine.mMaxBaseline, (child.getMeasuredHeight() - child.getBaseline()) + flexItem.getMarginBottom());
                    }
                }
                sumCrossSize = childCount3;
                if (isLastFlexItem(i2, sumCrossSize, flexLine)) {
                    addFlexLine(flexLines3, flexLine, i2, sumCrossSize5);
                    sumCrossSize5 += flexLine.mCrossSize;
                }
                i3 = toIndex;
                if (i3 != -1 && flexLines3.size() > 0) {
                    if (flexLines3.get(flexLines3.size() - 1).mLastIndex >= i3 && i2 >= i3 && !reachedToIndex2) {
                        sumCrossSize2 = -flexLine.getCrossSize();
                        reachedToIndex2 = true;
                    }
                    if (sumCrossSize2 <= needsCalcAmount && reachedToIndex2) {
                        largestSizeInCross2 = childState2;
                        break;
                    }
                    largestSizeInCross = largestSizeInCross5;
                    indexInFlexLine2 = indexInFlexLine;
                    largestSizeInCross2 = childState2;
                    childState = i2 + 1;
                    childCount2 = sumCrossSize;
                    i4 = i3;
                    childCount = sumCrossSize2;
                    flexLines4 = flexLines3;
                    mainSize3 = mainSize2;
                    mainMode2 = mainMode;
                    sumCrossSize4 = mainMeasureSpec;
                }
                sumCrossSize2 = sumCrossSize5;
                if (sumCrossSize2 <= needsCalcAmount) {
                }
                largestSizeInCross = largestSizeInCross5;
                indexInFlexLine2 = indexInFlexLine;
                largestSizeInCross2 = childState2;
                childState = i2 + 1;
                childCount2 = sumCrossSize;
                i4 = i3;
                childCount = sumCrossSize2;
                flexLines4 = flexLines3;
                mainSize3 = mainSize2;
                mainMode2 = mainMode;
                sumCrossSize4 = mainMeasureSpec;
            }
            i2 = largestSizeInCross3;
            sumCrossSize2 = childCount;
            sumCrossSize = childCount2;
            mainSize2 = mainSize3;
            mainMode = mainMode2;
            flexLines3 = flexLines4;
            i3 = i4;
            childState = i2 + 1;
            childCount2 = sumCrossSize;
            i4 = i3;
            childCount = sumCrossSize2;
            flexLines4 = flexLines3;
            mainSize3 = mainSize2;
            mainMode2 = mainMode;
            sumCrossSize4 = mainMeasureSpec;
        }
        result.mChildState = largestSizeInCross2;
    }

    private int getPaddingStartMain(boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return this.mFlexContainer.getPaddingStart();
        }
        return this.mFlexContainer.getPaddingTop();
    }

    private int getPaddingEndMain(boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return this.mFlexContainer.getPaddingEnd();
        }
        return this.mFlexContainer.getPaddingBottom();
    }

    private int getPaddingStartCross(boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return this.mFlexContainer.getPaddingTop();
        }
        return this.mFlexContainer.getPaddingStart();
    }

    private int getPaddingEndCross(boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return this.mFlexContainer.getPaddingBottom();
        }
        return this.mFlexContainer.getPaddingEnd();
    }

    private int getViewMeasuredSizeMain(View view, boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return view.getMeasuredWidth();
        }
        return view.getMeasuredHeight();
    }

    private int getViewMeasuredSizeCross(View view, boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return view.getMeasuredHeight();
        }
        return view.getMeasuredWidth();
    }

    private int getFlexItemSizeMain(FlexItem flexItem, boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return flexItem.getWidth();
        }
        return flexItem.getHeight();
    }

    private int getFlexItemSizeCross(FlexItem flexItem, boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return flexItem.getHeight();
        }
        return flexItem.getWidth();
    }

    private int getFlexItemMarginStartMain(FlexItem flexItem, boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return flexItem.getMarginLeft();
        }
        return flexItem.getMarginTop();
    }

    private int getFlexItemMarginEndMain(FlexItem flexItem, boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return flexItem.getMarginRight();
        }
        return flexItem.getMarginBottom();
    }

    private int getFlexItemMarginStartCross(FlexItem flexItem, boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return flexItem.getMarginTop();
        }
        return flexItem.getMarginLeft();
    }

    private int getFlexItemMarginEndCross(FlexItem flexItem, boolean isMainHorizontal) {
        if (isMainHorizontal) {
            return flexItem.getMarginBottom();
        }
        return flexItem.getMarginRight();
    }

    private boolean isWrapRequired(View view, int mode, int maxSize, int currentLength, int childLength, FlexItem flexItem, int index, int indexInFlexLine) {
        if (this.mFlexContainer.getFlexWrap() == 0) {
            return false;
        }
        if (flexItem.isWrapBefore()) {
            return true;
        }
        if (mode == 0) {
            return false;
        }
        int decorationLength = this.mFlexContainer.getDecorationLengthMainAxis(view, index, indexInFlexLine);
        if (decorationLength > 0) {
            childLength += decorationLength;
        }
        return maxSize < currentLength + childLength;
    }

    private boolean isLastFlexItem(int childIndex, int childCount, FlexLine flexLine) {
        return childIndex == childCount + (-1) && flexLine.getItemCountNotGone() != 0;
    }

    private void addFlexLine(List<FlexLine> flexLines, FlexLine flexLine, int viewIndex, int usedCrossSizeSoFar) {
        flexLine.mSumCrossSizeBefore = usedCrossSizeSoFar;
        this.mFlexContainer.onNewFlexLineAdded(flexLine);
        flexLine.mLastIndex = viewIndex;
        flexLines.add(flexLine);
    }

    private void checkSizeConstraints(View view, int index) {
        boolean needsMeasure = false;
        FlexItem flexItem = (FlexItem) view.getLayoutParams();
        int childWidth = view.getMeasuredWidth();
        int childHeight = view.getMeasuredHeight();
        if (childWidth < flexItem.getMinWidth()) {
            needsMeasure = true;
            childWidth = flexItem.getMinWidth();
        } else if (childWidth > flexItem.getMaxWidth()) {
            needsMeasure = true;
            childWidth = flexItem.getMaxWidth();
        }
        if (childHeight < flexItem.getMinHeight()) {
            needsMeasure = true;
            childHeight = flexItem.getMinHeight();
        } else if (childHeight > flexItem.getMaxHeight()) {
            needsMeasure = true;
            childHeight = flexItem.getMaxHeight();
        }
        if (needsMeasure) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(childWidth, 1073741824);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(childHeight, 1073741824);
            view.measure(widthSpec, heightSpec);
            updateMeasureCache(index, widthSpec, heightSpec, view);
            this.mFlexContainer.updateViewCache(index, view);
        }
    }

    void determineMainSize(int widthMeasureSpec, int heightMeasureSpec) {
        determineMainSize(widthMeasureSpec, heightMeasureSpec, 0);
    }

    void determineMainSize(int widthMeasureSpec, int heightMeasureSpec, int fromIndex) {
        int mainSize;
        int paddingAlongMainAxis;
        int size;
        int i;
        List<FlexLine> flexLines;
        int mainSize2;
        ensureChildrenFrozen(this.mFlexContainer.getFlexItemCount());
        if (fromIndex >= this.mFlexContainer.getFlexItemCount()) {
            return;
        }
        int flexDirection = this.mFlexContainer.getFlexDirection();
        switch (this.mFlexContainer.getFlexDirection()) {
            case 0:
            case 1:
                int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
                int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
                if (widthMode == 1073741824) {
                    mainSize = widthSize;
                } else {
                    mainSize = this.mFlexContainer.getLargestMainSize();
                }
                paddingAlongMainAxis = this.mFlexContainer.getPaddingLeft() + this.mFlexContainer.getPaddingRight();
                break;
            case 2:
            case Generator.STATE_GENERATED_FAIL:
                int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
                int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
                if (heightMode == 1073741824) {
                    mainSize = heightSize;
                } else {
                    mainSize = this.mFlexContainer.getLargestMainSize();
                }
                paddingAlongMainAxis = this.mFlexContainer.getPaddingTop() + this.mFlexContainer.getPaddingBottom();
                break;
            default:
                throw new IllegalArgumentException("Invalid flex direction: " + flexDirection);
        }
        int paddingAlongMainAxis2 = paddingAlongMainAxis;
        int mainSize3 = mainSize;
        int flexLineIndex = 0;
        if (this.mIndexToFlexLine != null) {
            flexLineIndex = this.mIndexToFlexLine[fromIndex];
        }
        List<FlexLine> flexLines2 = this.mFlexContainer.getFlexLinesInternal();
        int size2 = flexLines2.size();
        int i2 = flexLineIndex;
        while (true) {
            int size3 = size2;
            if (i2 >= size3) {
                return;
            }
            FlexLine flexLine = flexLines2.get(i2);
            if (flexLine.mMainSize < mainSize3) {
                expandFlexItems(widthMeasureSpec, heightMeasureSpec, flexLine, mainSize3, paddingAlongMainAxis2, false);
                size = size3;
                i = i2;
                flexLines = flexLines2;
                mainSize2 = mainSize3;
            } else {
                size = size3;
                i = i2;
                int i3 = mainSize3;
                flexLines = flexLines2;
                mainSize2 = mainSize3;
                shrinkFlexItems(widthMeasureSpec, heightMeasureSpec, flexLine, i3, paddingAlongMainAxis2, false);
            }
            i2 = i + 1;
            size2 = size;
            flexLines2 = flexLines;
            mainSize3 = mainSize2;
        }
    }

    private void ensureChildrenFrozen(int size) {
        if (this.mChildrenFrozen != null) {
            if (this.mChildrenFrozen.length < size) {
                int newCapacity = this.mChildrenFrozen.length * 2;
                this.mChildrenFrozen = new boolean[newCapacity >= size ? newCapacity : size];
                return;
            } else {
                Arrays.fill(this.mChildrenFrozen, false);
                return;
            }
        }
        this.mChildrenFrozen = new boolean[size >= 10 ? size : 10];
    }

    private void expandFlexItems(int widthMeasureSpec, int heightMeasureSpec, FlexLine flexLine, int maxMainSize, int paddingAlongMainAxis, boolean calledRecursively) {
        int sizeBeforeExpand;
        int largestCrossSize;
        int childMeasuredHeight;
        int flexDirection;
        int childMeasuredHeight2;
        int childMeasuredWidth;
        float accumulatedRoundError;
        if (flexLine.mTotalFlexGrow <= 0.0f || maxMainSize < flexLine.mMainSize) {
            return;
        }
        int sizeBeforeExpand2 = flexLine.mMainSize;
        float unitSpace = (maxMainSize - flexLine.mMainSize) / flexLine.mTotalFlexGrow;
        flexLine.mMainSize = paddingAlongMainAxis + flexLine.mDividerLengthInMainSize;
        if (!calledRecursively) {
            flexLine.mCrossSize = Integer.MIN_VALUE;
        }
        int i = 0;
        boolean needsReexpand = false;
        int largestCrossSize2 = 0;
        float accumulatedRoundError2 = 0.0f;
        while (true) {
            int i2 = i;
            if (i2 >= flexLine.mItemCount) {
                break;
            }
            int index = flexLine.mFirstIndex + i2;
            View child = this.mFlexContainer.getReorderedFlexItemAt(index);
            if (child == null) {
                sizeBeforeExpand = sizeBeforeExpand2;
            } else if (child.getVisibility() == 8) {
                sizeBeforeExpand = sizeBeforeExpand2;
            } else {
                FlexItem flexItem = (FlexItem) child.getLayoutParams();
                int flexDirection2 = this.mFlexContainer.getFlexDirection();
                if (flexDirection2 == 0 || flexDirection2 == 1) {
                    int childMeasuredWidth2 = child.getMeasuredWidth();
                    if (this.mMeasuredSizeCache != null) {
                        childMeasuredWidth2 = extractLowerInt(this.mMeasuredSizeCache[index]);
                    }
                    int childMeasuredHeight3 = child.getMeasuredHeight();
                    if (this.mMeasuredSizeCache != null) {
                        int childMeasuredHeight4 = extractHigherInt(this.mMeasuredSizeCache[index]);
                        childMeasuredHeight = childMeasuredHeight4;
                    } else {
                        childMeasuredHeight = childMeasuredHeight3;
                    }
                    if (this.mChildrenFrozen[index] || flexItem.getFlexGrow() <= 0.0f) {
                        sizeBeforeExpand = sizeBeforeExpand2;
                    } else {
                        float rawCalculatedWidth = childMeasuredWidth2 + (flexItem.getFlexGrow() * unitSpace);
                        if (i2 == flexLine.mItemCount - 1) {
                            rawCalculatedWidth += accumulatedRoundError2;
                            accumulatedRoundError2 = 0.0f;
                        }
                        int newWidth = Math.round(rawCalculatedWidth);
                        if (newWidth > flexItem.getMaxWidth()) {
                            newWidth = flexItem.getMaxWidth();
                            this.mChildrenFrozen[index] = true;
                            flexLine.mTotalFlexGrow -= flexItem.getFlexGrow();
                            needsReexpand = true;
                            sizeBeforeExpand = sizeBeforeExpand2;
                        } else {
                            float accumulatedRoundError3 = accumulatedRoundError2 + (rawCalculatedWidth - newWidth);
                            sizeBeforeExpand = sizeBeforeExpand2;
                            if (accumulatedRoundError3 > 1.0d) {
                                newWidth++;
                                accumulatedRoundError3 = (float) (((double) accumulatedRoundError3) - 1.0d);
                            } else if (accumulatedRoundError3 < -1.0d) {
                                newWidth--;
                                accumulatedRoundError3 = (float) (((double) accumulatedRoundError3) + 1.0d);
                            }
                            accumulatedRoundError2 = accumulatedRoundError3;
                        }
                        int childHeightMeasureSpec = getChildHeightMeasureSpecInternal(heightMeasureSpec, flexItem, flexLine.mSumCrossSizeBefore);
                        int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(newWidth, 1073741824);
                        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                        int childMeasuredWidth3 = child.getMeasuredWidth();
                        childMeasuredHeight = child.getMeasuredHeight();
                        updateMeasureCache(index, childWidthMeasureSpec, childHeightMeasureSpec, child);
                        this.mFlexContainer.updateViewCache(index, child);
                        childMeasuredWidth2 = childMeasuredWidth3;
                    }
                    int largestCrossSize3 = Math.max(largestCrossSize2, childMeasuredHeight + flexItem.getMarginTop() + flexItem.getMarginBottom() + this.mFlexContainer.getDecorationLengthCrossAxis(child));
                    flexLine.mMainSize += flexItem.getMarginLeft() + childMeasuredWidth2 + flexItem.getMarginRight();
                    largestCrossSize = largestCrossSize3;
                    int largestCrossSize4 = flexLine.mCrossSize;
                    flexLine.mCrossSize = Math.max(largestCrossSize4, largestCrossSize);
                    largestCrossSize2 = largestCrossSize;
                } else {
                    int childMeasuredHeight5 = child.getMeasuredHeight();
                    if (this.mMeasuredSizeCache != null) {
                        int childMeasuredHeight6 = extractHigherInt(this.mMeasuredSizeCache[index]);
                        flexDirection = childMeasuredHeight6;
                    } else {
                        flexDirection = childMeasuredHeight5;
                    }
                    int childMeasuredWidth4 = child.getMeasuredWidth();
                    if (this.mMeasuredSizeCache != null) {
                        childMeasuredWidth4 = extractLowerInt(this.mMeasuredSizeCache[index]);
                    }
                    if (this.mChildrenFrozen[index] || flexItem.getFlexGrow() <= 0.0f) {
                        childMeasuredHeight2 = flexDirection;
                        childMeasuredWidth = childMeasuredWidth4;
                    } else {
                        float rawCalculatedHeight = flexDirection + (flexItem.getFlexGrow() * unitSpace);
                        if (i2 == flexLine.mItemCount - 1) {
                            rawCalculatedHeight += accumulatedRoundError2;
                            accumulatedRoundError2 = 0.0f;
                        }
                        int newHeight = Math.round(rawCalculatedHeight);
                        if (newHeight > flexItem.getMaxHeight()) {
                            newHeight = flexItem.getMaxHeight();
                            this.mChildrenFrozen[index] = true;
                            flexLine.mTotalFlexGrow -= flexItem.getFlexGrow();
                            needsReexpand = true;
                        } else {
                            float accumulatedRoundError4 = accumulatedRoundError2 + (rawCalculatedHeight - newHeight);
                            if (accumulatedRoundError4 > 1.0d) {
                                newHeight++;
                                accumulatedRoundError = (float) (((double) accumulatedRoundError4) - 1.0d);
                            } else if (accumulatedRoundError4 < -1.0d) {
                                newHeight--;
                                accumulatedRoundError = (float) (((double) accumulatedRoundError4) + 1.0d);
                            } else {
                                accumulatedRoundError2 = accumulatedRoundError4;
                            }
                            accumulatedRoundError2 = accumulatedRoundError;
                        }
                        int childWidthMeasureSpec2 = getChildWidthMeasureSpecInternal(widthMeasureSpec, flexItem, flexLine.mSumCrossSizeBefore);
                        int childHeightMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(newHeight, 1073741824);
                        child.measure(childWidthMeasureSpec2, childHeightMeasureSpec2);
                        int childMeasuredWidth5 = child.getMeasuredWidth();
                        int childMeasuredHeight7 = child.getMeasuredHeight();
                        updateMeasureCache(index, childWidthMeasureSpec2, childHeightMeasureSpec2, child);
                        this.mFlexContainer.updateViewCache(index, child);
                        childMeasuredWidth = childMeasuredWidth5;
                        childMeasuredHeight2 = childMeasuredHeight7;
                    }
                    largestCrossSize = Math.max(largestCrossSize2, childMeasuredWidth + flexItem.getMarginLeft() + flexItem.getMarginRight() + this.mFlexContainer.getDecorationLengthCrossAxis(child));
                    flexLine.mMainSize += childMeasuredHeight2 + flexItem.getMarginTop() + flexItem.getMarginBottom();
                    sizeBeforeExpand = sizeBeforeExpand2;
                    int largestCrossSize42 = flexLine.mCrossSize;
                    flexLine.mCrossSize = Math.max(largestCrossSize42, largestCrossSize);
                    largestCrossSize2 = largestCrossSize;
                }
            }
            i = i2 + 1;
            sizeBeforeExpand2 = sizeBeforeExpand;
        }
        int sizeBeforeExpand3 = sizeBeforeExpand2;
        if (!needsReexpand || sizeBeforeExpand3 == flexLine.mMainSize) {
            return;
        }
        expandFlexItems(widthMeasureSpec, heightMeasureSpec, flexLine, maxMainSize, paddingAlongMainAxis, true);
    }

    private void shrinkFlexItems(int widthMeasureSpec, int heightMeasureSpec, FlexLine flexLine, int maxMainSize, int paddingAlongMainAxis, boolean calledRecursively) {
        int sizeBeforeShrink;
        float unitShrink;
        int largestCrossSize;
        int childMeasuredWidth;
        int childMeasuredHeight;
        int childMeasuredWidth2;
        int childMeasuredHeight2;
        int sizeBeforeShrink2 = flexLine.mMainSize;
        if (flexLine.mTotalFlexShrink > 0.0f && maxMainSize <= flexLine.mMainSize) {
            float unitShrink2 = (flexLine.mMainSize - maxMainSize) / flexLine.mTotalFlexShrink;
            flexLine.mMainSize = paddingAlongMainAxis + flexLine.mDividerLengthInMainSize;
            if (!calledRecursively) {
                flexLine.mCrossSize = Integer.MIN_VALUE;
            }
            int i = 0;
            boolean needsReshrink = false;
            float accumulatedRoundError = 0.0f;
            int largestCrossSize2 = 0;
            while (true) {
                int i2 = i;
                if (i2 >= flexLine.mItemCount) {
                    break;
                }
                int index = flexLine.mFirstIndex + i2;
                View child = this.mFlexContainer.getReorderedFlexItemAt(index);
                if (child == null) {
                    sizeBeforeShrink = sizeBeforeShrink2;
                    unitShrink = unitShrink2;
                } else if (child.getVisibility() == 8) {
                    sizeBeforeShrink = sizeBeforeShrink2;
                    unitShrink = unitShrink2;
                } else {
                    FlexItem flexItem = (FlexItem) child.getLayoutParams();
                    int flexDirection = this.mFlexContainer.getFlexDirection();
                    if (flexDirection == 0 || flexDirection == 1) {
                        sizeBeforeShrink = sizeBeforeShrink2;
                        int childMeasuredWidth3 = child.getMeasuredWidth();
                        if (this.mMeasuredSizeCache != null) {
                            childMeasuredWidth3 = extractLowerInt(this.mMeasuredSizeCache[index]);
                        }
                        int childMeasuredHeight3 = child.getMeasuredHeight();
                        if (this.mMeasuredSizeCache != null) {
                            childMeasuredHeight3 = extractHigherInt(this.mMeasuredSizeCache[index]);
                        }
                        if (!this.mChildrenFrozen[index] && flexItem.getFlexShrink() > 0.0f) {
                            float rawCalculatedWidth = childMeasuredWidth3 - (flexItem.getFlexShrink() * unitShrink2);
                            if (i2 == flexLine.mItemCount - 1) {
                                rawCalculatedWidth += accumulatedRoundError;
                                accumulatedRoundError = 0.0f;
                            }
                            int newWidth = Math.round(rawCalculatedWidth);
                            int childMeasuredWidth4 = flexItem.getMinWidth();
                            if (newWidth < childMeasuredWidth4) {
                                newWidth = flexItem.getMinWidth();
                                this.mChildrenFrozen[index] = true;
                                flexLine.mTotalFlexShrink -= flexItem.getFlexShrink();
                                needsReshrink = true;
                                unitShrink = unitShrink2;
                            } else {
                                accumulatedRoundError += rawCalculatedWidth - newWidth;
                                unitShrink = unitShrink2;
                                if (accumulatedRoundError > 1.0d) {
                                    newWidth++;
                                    accumulatedRoundError -= 1.0f;
                                } else if (accumulatedRoundError < -1.0d) {
                                    newWidth--;
                                    accumulatedRoundError += 1.0f;
                                }
                            }
                            int childHeightMeasureSpec = getChildHeightMeasureSpecInternal(heightMeasureSpec, flexItem, flexLine.mSumCrossSizeBefore);
                            int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(newWidth, 1073741824);
                            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                            childMeasuredWidth = child.getMeasuredWidth();
                            childMeasuredHeight = child.getMeasuredHeight();
                            updateMeasureCache(index, childWidthMeasureSpec, childHeightMeasureSpec, child);
                            this.mFlexContainer.updateViewCache(index, child);
                        } else {
                            unitShrink = unitShrink2;
                            childMeasuredWidth = childMeasuredWidth3;
                            childMeasuredHeight = childMeasuredHeight3;
                        }
                        largestCrossSize = Math.max(largestCrossSize2, childMeasuredHeight + flexItem.getMarginTop() + flexItem.getMarginBottom() + this.mFlexContainer.getDecorationLengthCrossAxis(child));
                        int largestCrossSize3 = flexLine.mMainSize;
                        flexLine.mMainSize = largestCrossSize3 + childMeasuredWidth + flexItem.getMarginLeft() + flexItem.getMarginRight();
                        flexLine.mCrossSize = Math.max(flexLine.mCrossSize, largestCrossSize);
                        largestCrossSize2 = largestCrossSize;
                    } else {
                        int childMeasuredHeight4 = child.getMeasuredHeight();
                        if (this.mMeasuredSizeCache != null) {
                            childMeasuredHeight4 = extractHigherInt(this.mMeasuredSizeCache[index]);
                        }
                        int childMeasuredHeight5 = childMeasuredHeight4;
                        int childMeasuredWidth5 = child.getMeasuredWidth();
                        if (this.mMeasuredSizeCache != null) {
                            int childMeasuredWidth6 = extractLowerInt(this.mMeasuredSizeCache[index]);
                            childMeasuredWidth2 = childMeasuredWidth6;
                        } else {
                            childMeasuredWidth2 = childMeasuredWidth5;
                        }
                        if (this.mChildrenFrozen[index] || flexItem.getFlexShrink() <= 0.0f) {
                            childMeasuredHeight2 = childMeasuredHeight5;
                            sizeBeforeShrink = sizeBeforeShrink2;
                        } else {
                            float rawCalculatedHeight = childMeasuredHeight5 - (flexItem.getFlexShrink() * unitShrink2);
                            if (i2 == flexLine.mItemCount - 1) {
                                rawCalculatedHeight += accumulatedRoundError;
                                accumulatedRoundError = 0.0f;
                            }
                            int newHeight = Math.round(rawCalculatedHeight);
                            int childMeasuredHeight6 = flexItem.getMinHeight();
                            if (newHeight < childMeasuredHeight6) {
                                newHeight = flexItem.getMinHeight();
                                this.mChildrenFrozen[index] = true;
                                flexLine.mTotalFlexShrink -= flexItem.getFlexShrink();
                                needsReshrink = true;
                                sizeBeforeShrink = sizeBeforeShrink2;
                            } else {
                                accumulatedRoundError += rawCalculatedHeight - newHeight;
                                sizeBeforeShrink = sizeBeforeShrink2;
                                if (accumulatedRoundError > 1.0d) {
                                    newHeight++;
                                    accumulatedRoundError -= 1.0f;
                                } else if (accumulatedRoundError < -1.0d) {
                                    newHeight--;
                                    accumulatedRoundError += 1.0f;
                                }
                            }
                            int childWidthMeasureSpec2 = getChildWidthMeasureSpecInternal(widthMeasureSpec, flexItem, flexLine.mSumCrossSizeBefore);
                            int childHeightMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(newHeight, 1073741824);
                            child.measure(childWidthMeasureSpec2, childHeightMeasureSpec2);
                            childMeasuredWidth2 = child.getMeasuredWidth();
                            int childMeasuredHeight7 = child.getMeasuredHeight();
                            updateMeasureCache(index, childWidthMeasureSpec2, childHeightMeasureSpec2, child);
                            this.mFlexContainer.updateViewCache(index, child);
                            childMeasuredHeight2 = childMeasuredHeight7;
                        }
                        int childMeasuredHeight8 = flexItem.getMarginLeft();
                        largestCrossSize = Math.max(largestCrossSize2, childMeasuredWidth2 + childMeasuredHeight8 + flexItem.getMarginRight() + this.mFlexContainer.getDecorationLengthCrossAxis(child));
                        int largestCrossSize4 = flexLine.mMainSize;
                        flexLine.mMainSize = largestCrossSize4 + childMeasuredHeight2 + flexItem.getMarginTop() + flexItem.getMarginBottom();
                        unitShrink = unitShrink2;
                        flexLine.mCrossSize = Math.max(flexLine.mCrossSize, largestCrossSize);
                        largestCrossSize2 = largestCrossSize;
                    }
                }
                i = i2 + 1;
                sizeBeforeShrink2 = sizeBeforeShrink;
                unitShrink2 = unitShrink;
            }
            int sizeBeforeShrink3 = sizeBeforeShrink2;
            if (needsReshrink && sizeBeforeShrink3 != flexLine.mMainSize) {
                shrinkFlexItems(widthMeasureSpec, heightMeasureSpec, flexLine, maxMainSize, paddingAlongMainAxis, true);
            }
        }
    }

    private int getChildWidthMeasureSpecInternal(int widthMeasureSpec, FlexItem flexItem, int padding) {
        int childWidthMeasureSpec = this.mFlexContainer.getChildWidthMeasureSpec(widthMeasureSpec, this.mFlexContainer.getPaddingLeft() + this.mFlexContainer.getPaddingRight() + flexItem.getMarginLeft() + flexItem.getMarginRight() + padding, flexItem.getWidth());
        int childWidth = View.MeasureSpec.getSize(childWidthMeasureSpec);
        if (childWidth > flexItem.getMaxWidth()) {
            return View.MeasureSpec.makeMeasureSpec(flexItem.getMaxWidth(), View.MeasureSpec.getMode(childWidthMeasureSpec));
        }
        if (childWidth < flexItem.getMinWidth()) {
            return View.MeasureSpec.makeMeasureSpec(flexItem.getMinWidth(), View.MeasureSpec.getMode(childWidthMeasureSpec));
        }
        return childWidthMeasureSpec;
    }

    private int getChildHeightMeasureSpecInternal(int heightMeasureSpec, FlexItem flexItem, int padding) {
        int childHeightMeasureSpec = this.mFlexContainer.getChildHeightMeasureSpec(heightMeasureSpec, this.mFlexContainer.getPaddingTop() + this.mFlexContainer.getPaddingBottom() + flexItem.getMarginTop() + flexItem.getMarginBottom() + padding, flexItem.getHeight());
        int childHeight = View.MeasureSpec.getSize(childHeightMeasureSpec);
        if (childHeight > flexItem.getMaxHeight()) {
            return View.MeasureSpec.makeMeasureSpec(flexItem.getMaxHeight(), View.MeasureSpec.getMode(childHeightMeasureSpec));
        }
        if (childHeight < flexItem.getMinHeight()) {
            return View.MeasureSpec.makeMeasureSpec(flexItem.getMinHeight(), View.MeasureSpec.getMode(childHeightMeasureSpec));
        }
        return childHeightMeasureSpec;
    }

    void determineCrossSize(int widthMeasureSpec, int heightMeasureSpec, int paddingAlongCrossAxis) {
        int mode;
        int size;
        char c;
        int flexDirection = this.mFlexContainer.getFlexDirection();
        switch (flexDirection) {
            case 0:
            case 1:
                mode = View.MeasureSpec.getMode(heightMeasureSpec);
                size = View.MeasureSpec.getSize(heightMeasureSpec);
                break;
            case 2:
            case Generator.STATE_GENERATED_FAIL:
                mode = View.MeasureSpec.getMode(widthMeasureSpec);
                size = View.MeasureSpec.getSize(widthMeasureSpec);
                break;
            default:
                throw new IllegalArgumentException("Invalid flex direction: " + flexDirection);
        }
        List<FlexLine> flexLines = this.mFlexContainer.getFlexLinesInternal();
        if (mode == 1073741824) {
            int totalCrossSize = this.mFlexContainer.getSumOfCrossSize() + paddingAlongCrossAxis;
            if (flexLines.size() == 1) {
                flexLines.get(0).mCrossSize = size - paddingAlongCrossAxis;
                return;
            }
            if (flexLines.size() >= 2) {
                switch (this.mFlexContainer.getAlignContent()) {
                    case 1:
                        int spaceTop = size - totalCrossSize;
                        FlexLine dummySpaceFlexLine = new FlexLine();
                        dummySpaceFlexLine.mCrossSize = spaceTop;
                        flexLines.add(0, dummySpaceFlexLine);
                        return;
                    case 2:
                        this.mFlexContainer.setFlexLines(constructFlexLinesForAlignContentCenter(flexLines, size, totalCrossSize));
                        return;
                    case Generator.STATE_GENERATED_FAIL:
                        if (totalCrossSize < size) {
                            int numberOfSpaces = flexLines.size() - 1;
                            float spaceBetweenFlexLine = (size - totalCrossSize) / numberOfSpaces;
                            float accumulatedError = 0.0f;
                            List<FlexLine> newFlexLines = new ArrayList<>();
                            int flexLineSize = flexLines.size();
                            for (int i = 0; i < flexLineSize; i++) {
                                FlexLine flexLine = flexLines.get(i);
                                newFlexLines.add(flexLine);
                                if (i == flexLines.size() - 1) {
                                    c = 0;
                                } else {
                                    FlexLine dummySpaceFlexLine2 = new FlexLine();
                                    if (i == flexLines.size() - 2) {
                                        dummySpaceFlexLine2.mCrossSize = Math.round(spaceBetweenFlexLine + accumulatedError);
                                        accumulatedError = 0.0f;
                                    } else {
                                        dummySpaceFlexLine2.mCrossSize = Math.round(spaceBetweenFlexLine);
                                    }
                                    accumulatedError += spaceBetweenFlexLine - dummySpaceFlexLine2.mCrossSize;
                                    c = 0;
                                    if (accumulatedError > 1.0f) {
                                        dummySpaceFlexLine2.mCrossSize++;
                                        accumulatedError -= 1.0f;
                                    } else if (accumulatedError < -1.0f) {
                                        dummySpaceFlexLine2.mCrossSize--;
                                        accumulatedError += 1.0f;
                                    }
                                    newFlexLines.add(dummySpaceFlexLine2);
                                }
                            }
                            this.mFlexContainer.setFlexLines(newFlexLines);
                            return;
                        }
                        return;
                    case 4:
                        if (totalCrossSize >= size) {
                            this.mFlexContainer.setFlexLines(constructFlexLinesForAlignContentCenter(flexLines, size, totalCrossSize));
                            return;
                        }
                        int spaceTopAndBottom = size - totalCrossSize;
                        int numberOfSpaces2 = flexLines.size() * 2;
                        List<FlexLine> newFlexLines2 = new ArrayList<>();
                        FlexLine dummySpaceFlexLine3 = new FlexLine();
                        dummySpaceFlexLine3.mCrossSize = spaceTopAndBottom / numberOfSpaces2;
                        for (FlexLine flexLine2 : flexLines) {
                            newFlexLines2.add(dummySpaceFlexLine3);
                            newFlexLines2.add(flexLine2);
                            newFlexLines2.add(dummySpaceFlexLine3);
                        }
                        this.mFlexContainer.setFlexLines(newFlexLines2);
                        return;
                    case 5:
                        if (totalCrossSize < size) {
                            float freeSpaceUnit = (size - totalCrossSize) / flexLines.size();
                            float accumulatedError2 = 0.0f;
                            int flexLinesSize = flexLines.size();
                            for (int i2 = 0; i2 < flexLinesSize; i2++) {
                                FlexLine flexLine3 = flexLines.get(i2);
                                float newCrossSizeAsFloat = flexLine3.mCrossSize + freeSpaceUnit;
                                if (i2 == flexLines.size() - 1) {
                                    newCrossSizeAsFloat += accumulatedError2;
                                    accumulatedError2 = 0.0f;
                                }
                                int newCrossSize = Math.round(newCrossSizeAsFloat);
                                accumulatedError2 += newCrossSizeAsFloat - newCrossSize;
                                if (accumulatedError2 > 1.0f) {
                                    newCrossSize++;
                                    accumulatedError2 -= 1.0f;
                                } else if (accumulatedError2 < -1.0f) {
                                    newCrossSize--;
                                    accumulatedError2 += 1.0f;
                                }
                                flexLine3.mCrossSize = newCrossSize;
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        }
    }

    private List<FlexLine> constructFlexLinesForAlignContentCenter(List<FlexLine> flexLines, int size, int totalCrossSize) {
        int spaceAboveAndBottom = size - totalCrossSize;
        List<FlexLine> newFlexLines = new ArrayList<>();
        FlexLine dummySpaceFlexLine = new FlexLine();
        dummySpaceFlexLine.mCrossSize = spaceAboveAndBottom / 2;
        int flexLineSize = flexLines.size();
        for (int i = 0; i < flexLineSize; i++) {
            if (i == 0) {
                newFlexLines.add(dummySpaceFlexLine);
            }
            FlexLine flexLine = flexLines.get(i);
            newFlexLines.add(flexLine);
            if (i == flexLines.size() - 1) {
                newFlexLines.add(dummySpaceFlexLine);
            }
        }
        return newFlexLines;
    }

    void stretchViews() {
        stretchViews(0);
    }

    void stretchViews(int fromIndex) {
        View view;
        if (fromIndex >= this.mFlexContainer.getFlexItemCount()) {
            return;
        }
        int flexDirection = this.mFlexContainer.getFlexDirection();
        if (this.mFlexContainer.getAlignItems() == 4) {
            int flexLineIndex = 0;
            if (this.mIndexToFlexLine != null) {
                flexLineIndex = this.mIndexToFlexLine[fromIndex];
            }
            List<FlexLine> flexLines = this.mFlexContainer.getFlexLinesInternal();
            int size = flexLines.size();
            for (int i = flexLineIndex; i < size; i++) {
                FlexLine flexLine = flexLines.get(i);
                int itemCount = flexLine.mItemCount;
                for (int j = 0; j < itemCount; j++) {
                    int viewIndex = flexLine.mFirstIndex + j;
                    if (j < this.mFlexContainer.getFlexItemCount() && (view = this.mFlexContainer.getReorderedFlexItemAt(viewIndex)) != null && view.getVisibility() != 8) {
                        FlexItem flexItem = (FlexItem) view.getLayoutParams();
                        if (flexItem.getAlignSelf() == -1 || flexItem.getAlignSelf() == 4) {
                            switch (flexDirection) {
                                case 0:
                                case 1:
                                    stretchViewVertically(view, flexLine.mCrossSize, viewIndex);
                                    break;
                                case 2:
                                case Generator.STATE_GENERATED_FAIL:
                                    stretchViewHorizontally(view, flexLine.mCrossSize, viewIndex);
                                    break;
                                default:
                                    throw new IllegalArgumentException("Invalid flex direction: " + flexDirection);
                            }
                        }
                    }
                }
            }
            return;
        }
        for (FlexLine flexLine2 : this.mFlexContainer.getFlexLinesInternal()) {
            for (Integer index : flexLine2.mIndicesAlignSelfStretch) {
                View view2 = this.mFlexContainer.getReorderedFlexItemAt(index.intValue());
                switch (flexDirection) {
                    case 0:
                    case 1:
                        stretchViewVertically(view2, flexLine2.mCrossSize, index.intValue());
                        break;
                    case 2:
                    case Generator.STATE_GENERATED_FAIL:
                        stretchViewHorizontally(view2, flexLine2.mCrossSize, index.intValue());
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid flex direction: " + flexDirection);
                }
            }
        }
    }

    private void stretchViewVertically(View view, int crossSize, int index) {
        int measuredWidth;
        FlexItem flexItem = (FlexItem) view.getLayoutParams();
        int newHeight = ((crossSize - flexItem.getMarginTop()) - flexItem.getMarginBottom()) - this.mFlexContainer.getDecorationLengthCrossAxis(view);
        int newHeight2 = Math.min(Math.max(newHeight, flexItem.getMinHeight()), flexItem.getMaxHeight());
        if (this.mMeasuredSizeCache != null) {
            measuredWidth = extractLowerInt(this.mMeasuredSizeCache[index]);
        } else {
            measuredWidth = view.getMeasuredWidth();
        }
        int childWidthSpec = View.MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824);
        int childHeightSpec = View.MeasureSpec.makeMeasureSpec(newHeight2, 1073741824);
        view.measure(childWidthSpec, childHeightSpec);
        updateMeasureCache(index, childWidthSpec, childHeightSpec, view);
        this.mFlexContainer.updateViewCache(index, view);
    }

    private void stretchViewHorizontally(View view, int crossSize, int index) {
        int measuredHeight;
        FlexItem flexItem = (FlexItem) view.getLayoutParams();
        int newWidth = ((crossSize - flexItem.getMarginLeft()) - flexItem.getMarginRight()) - this.mFlexContainer.getDecorationLengthCrossAxis(view);
        int newWidth2 = Math.min(Math.max(newWidth, flexItem.getMinWidth()), flexItem.getMaxWidth());
        if (this.mMeasuredSizeCache != null) {
            measuredHeight = extractHigherInt(this.mMeasuredSizeCache[index]);
        } else {
            measuredHeight = view.getMeasuredHeight();
        }
        int childHeightSpec = View.MeasureSpec.makeMeasureSpec(measuredHeight, 1073741824);
        int childWidthSpec = View.MeasureSpec.makeMeasureSpec(newWidth2, 1073741824);
        view.measure(childWidthSpec, childHeightSpec);
        updateMeasureCache(index, childWidthSpec, childHeightSpec, view);
        this.mFlexContainer.updateViewCache(index, view);
    }

    void layoutSingleChildHorizontal(View view, FlexLine flexLine, int left, int top, int right, int bottom) {
        FlexItem flexItem = (FlexItem) view.getLayoutParams();
        int alignItems = this.mFlexContainer.getAlignItems();
        if (flexItem.getAlignSelf() != -1) {
            alignItems = flexItem.getAlignSelf();
        }
        int crossSize = flexLine.mCrossSize;
        switch (alignItems) {
            case 0:
            case 4:
                if (this.mFlexContainer.getFlexWrap() != 2) {
                    view.layout(left, flexItem.getMarginTop() + top, right, flexItem.getMarginTop() + bottom);
                } else {
                    view.layout(left, top - flexItem.getMarginBottom(), right, bottom - flexItem.getMarginBottom());
                }
                break;
            case 1:
                if (this.mFlexContainer.getFlexWrap() != 2) {
                    view.layout(left, ((top + crossSize) - view.getMeasuredHeight()) - flexItem.getMarginBottom(), right, (top + crossSize) - flexItem.getMarginBottom());
                } else {
                    view.layout(left, (top - crossSize) + view.getMeasuredHeight() + flexItem.getMarginTop(), right, (bottom - crossSize) + view.getMeasuredHeight() + flexItem.getMarginTop());
                }
                break;
            case 2:
                int topFromCrossAxis = (((crossSize - view.getMeasuredHeight()) + flexItem.getMarginTop()) - flexItem.getMarginBottom()) / 2;
                if (this.mFlexContainer.getFlexWrap() != 2) {
                    view.layout(left, top + topFromCrossAxis, right, top + topFromCrossAxis + view.getMeasuredHeight());
                } else {
                    view.layout(left, top - topFromCrossAxis, right, (top - topFromCrossAxis) + view.getMeasuredHeight());
                }
                break;
            case Generator.STATE_GENERATED_FAIL:
                if (this.mFlexContainer.getFlexWrap() != 2) {
                    int marginTop = Math.max(flexLine.mMaxBaseline - view.getBaseline(), flexItem.getMarginTop());
                    view.layout(left, top + marginTop, right, bottom + marginTop);
                } else {
                    int marginBottom = Math.max((flexLine.mMaxBaseline - view.getMeasuredHeight()) + view.getBaseline(), flexItem.getMarginBottom());
                    view.layout(left, top - marginBottom, right, bottom - marginBottom);
                }
                break;
        }
    }

    void layoutSingleChildVertical(View view, FlexLine flexLine, boolean isRtl, int left, int top, int right, int bottom) {
        FlexItem flexItem = (FlexItem) view.getLayoutParams();
        int alignItems = this.mFlexContainer.getAlignItems();
        if (flexItem.getAlignSelf() != -1) {
            alignItems = flexItem.getAlignSelf();
        }
        int crossSize = flexLine.mCrossSize;
        switch (alignItems) {
            case 0:
            case Generator.STATE_GENERATED_FAIL:
            case 4:
                if (!isRtl) {
                    view.layout(flexItem.getMarginLeft() + left, top, flexItem.getMarginLeft() + right, bottom);
                } else {
                    view.layout(left - flexItem.getMarginRight(), top, right - flexItem.getMarginRight(), bottom);
                }
                break;
            case 1:
                if (!isRtl) {
                    view.layout(((left + crossSize) - view.getMeasuredWidth()) - flexItem.getMarginRight(), top, ((right + crossSize) - view.getMeasuredWidth()) - flexItem.getMarginRight(), bottom);
                } else {
                    view.layout((left - crossSize) + view.getMeasuredWidth() + flexItem.getMarginLeft(), top, (right - crossSize) + view.getMeasuredWidth() + flexItem.getMarginLeft(), bottom);
                }
                break;
            case 2:
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                int leftFromCrossAxis = (((crossSize - view.getMeasuredWidth()) + MarginLayoutParamsCompat.getMarginStart(lp)) - MarginLayoutParamsCompat.getMarginEnd(lp)) / 2;
                if (!isRtl) {
                    view.layout(left + leftFromCrossAxis, top, right + leftFromCrossAxis, bottom);
                } else {
                    view.layout(left - leftFromCrossAxis, top, right - leftFromCrossAxis, bottom);
                }
                break;
        }
    }

    void ensureMeasuredSizeCache(int size) {
        if (this.mMeasuredSizeCache != null) {
            if (this.mMeasuredSizeCache.length < size) {
                int newCapacity = this.mMeasuredSizeCache.length * 2;
                this.mMeasuredSizeCache = Arrays.copyOf(this.mMeasuredSizeCache, newCapacity >= size ? newCapacity : size);
                return;
            }
            return;
        }
        this.mMeasuredSizeCache = new long[size >= 10 ? size : 10];
    }

    void ensureMeasureSpecCache(int size) {
        if (this.mMeasureSpecCache != null) {
            if (this.mMeasureSpecCache.length < size) {
                int newCapacity = this.mMeasureSpecCache.length * 2;
                this.mMeasureSpecCache = Arrays.copyOf(this.mMeasureSpecCache, newCapacity >= size ? newCapacity : size);
                return;
            }
            return;
        }
        this.mMeasureSpecCache = new long[size >= 10 ? size : 10];
    }

    int extractLowerInt(long longValue) {
        return (int) longValue;
    }

    int extractHigherInt(long longValue) {
        return (int) (longValue >> 32);
    }

    long makeCombinedLong(int widthMeasureSpec, int heightMeasureSpec) {
        return (((long) heightMeasureSpec) << 32) | (((long) widthMeasureSpec) & 4294967295L);
    }

    private void updateMeasureCache(int index, int widthMeasureSpec, int heightMeasureSpec, View view) {
        if (this.mMeasureSpecCache != null) {
            this.mMeasureSpecCache[index] = makeCombinedLong(widthMeasureSpec, heightMeasureSpec);
        }
        if (this.mMeasuredSizeCache != null) {
            this.mMeasuredSizeCache[index] = makeCombinedLong(view.getMeasuredWidth(), view.getMeasuredHeight());
        }
    }

    void ensureIndexToFlexLine(int size) {
        if (this.mIndexToFlexLine != null) {
            if (this.mIndexToFlexLine.length < size) {
                int newCapacity = this.mIndexToFlexLine.length * 2;
                this.mIndexToFlexLine = Arrays.copyOf(this.mIndexToFlexLine, newCapacity >= size ? newCapacity : size);
                return;
            }
            return;
        }
        this.mIndexToFlexLine = new int[size >= 10 ? size : 10];
    }

    void clearFlexLines(List<FlexLine> flexLines, int fromFlexItem) {
        int fromFlexLine = this.mIndexToFlexLine[fromFlexItem];
        if (fromFlexLine == -1) {
            fromFlexLine = 0;
        }
        for (int i = flexLines.size() - 1; i >= fromFlexLine; i--) {
            flexLines.remove(i);
        }
        int fillTo = this.mIndexToFlexLine.length - 1;
        if (fromFlexItem > fillTo) {
            Arrays.fill(this.mIndexToFlexLine, -1);
        } else {
            Arrays.fill(this.mIndexToFlexLine, fromFlexItem, fillTo, -1);
        }
        int fillTo2 = this.mMeasureSpecCache.length - 1;
        if (fromFlexItem > fillTo2) {
            Arrays.fill(this.mMeasureSpecCache, 0L);
        } else {
            Arrays.fill(this.mMeasureSpecCache, fromFlexItem, fillTo2, 0L);
        }
    }

    private static class Order implements Comparable<Order> {
        int index;
        int order;

        private Order() {
        }

        @Override
        public int compareTo(Order another) {
            if (this.order != another.order) {
                return this.order - another.order;
            }
            return this.index - another.index;
        }

        public String toString() {
            return "Order{order=" + this.order + ", index=" + this.index + '}';
        }
    }

    static class FlexLinesResult {
        int mChildState;
        List<FlexLine> mFlexLines;

        void reset() {
            this.mFlexLines = null;
            this.mChildState = 0;
        }
    }
}
