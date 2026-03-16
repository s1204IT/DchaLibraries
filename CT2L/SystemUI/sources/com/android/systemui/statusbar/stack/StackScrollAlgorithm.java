package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.stack.StackScrollState;
import java.util.ArrayList;

public class StackScrollAlgorithm {
    private StackIndentationFunctor mBottomStackIndentationFunctor;
    private int mBottomStackPeekSize;
    private int mBottomStackSlowDownLength;
    private int mCollapseSecondCardPadding;
    private int mCollapsedSize;
    private boolean mExpandedOnStart;
    private int mFirstChildMaxHeight;
    private ExpandableView mFirstChildWhileExpanding;
    private int mInnerHeight;
    private boolean mIsExpanded;
    private boolean mIsExpansionChanging;
    private boolean mIsSmallScreen;
    private int mLayoutHeight;
    private int mMaxNotificationHeight;
    private int mPaddingBetweenElements;
    private int mPaddingBetweenElementsDimmed;
    private int mPaddingBetweenElementsNormal;
    private int mRoundedRectCornerRadius;
    private boolean mScaleDimmed;
    private StackScrollAlgorithmState mTempAlgorithmState = new StackScrollAlgorithmState();
    private int mTopPadding;
    private StackIndentationFunctor mTopStackIndentationFunctor;
    private int mTopStackPeekSize;
    private int mTopStackSlowDownLength;
    private int mTopStackTotalSize;
    private int mZBasicHeight;
    private int mZDistanceBetweenElements;

    public StackScrollAlgorithm(Context context) {
        initConstants(context);
        updatePadding(false);
    }

    private void updatePadding(boolean dimmed) {
        this.mPaddingBetweenElements = (dimmed && this.mScaleDimmed) ? this.mPaddingBetweenElementsDimmed : this.mPaddingBetweenElementsNormal;
        this.mTopStackTotalSize = this.mTopStackSlowDownLength + this.mPaddingBetweenElements + this.mTopStackPeekSize;
        this.mTopStackIndentationFunctor = new PiecewiseLinearIndentationFunctor(3, this.mTopStackPeekSize, this.mTopStackTotalSize - this.mTopStackPeekSize, 0.5f);
        this.mBottomStackIndentationFunctor = new PiecewiseLinearIndentationFunctor(3, this.mBottomStackPeekSize, getBottomStackSlowDownLength(), 0.5f);
    }

    public int getBottomStackSlowDownLength() {
        return this.mBottomStackSlowDownLength + this.mPaddingBetweenElements;
    }

    private void initConstants(Context context) {
        this.mPaddingBetweenElementsDimmed = context.getResources().getDimensionPixelSize(R.dimen.notification_padding_dimmed);
        this.mPaddingBetweenElementsNormal = context.getResources().getDimensionPixelSize(R.dimen.notification_padding);
        this.mCollapsedSize = context.getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        this.mMaxNotificationHeight = context.getResources().getDimensionPixelSize(R.dimen.notification_max_height);
        this.mTopStackPeekSize = context.getResources().getDimensionPixelSize(R.dimen.top_stack_peek_amount);
        this.mBottomStackPeekSize = context.getResources().getDimensionPixelSize(R.dimen.bottom_stack_peek_amount);
        this.mZDistanceBetweenElements = context.getResources().getDimensionPixelSize(R.dimen.z_distance_between_notifications);
        this.mZBasicHeight = this.mZDistanceBetweenElements * 4;
        this.mBottomStackSlowDownLength = context.getResources().getDimensionPixelSize(R.dimen.bottom_stack_slow_down_length);
        this.mTopStackSlowDownLength = context.getResources().getDimensionPixelSize(R.dimen.top_stack_slow_down_length);
        this.mRoundedRectCornerRadius = context.getResources().getDimensionPixelSize(R.dimen.notification_material_rounded_rect_radius);
        this.mCollapseSecondCardPadding = context.getResources().getDimensionPixelSize(R.dimen.notification_collapse_second_card_padding);
        this.mScaleDimmed = context.getResources().getDisplayMetrics().densityDpi >= 480;
    }

    public boolean shouldScaleDimmed() {
        return this.mScaleDimmed;
    }

    public void getStackScrollState(AmbientState ambientState, StackScrollState resultState) {
        StackScrollAlgorithmState algorithmState = this.mTempAlgorithmState;
        resultState.resetViewStates();
        algorithmState.itemsInTopStack = 0.0f;
        algorithmState.partialInTop = 0.0f;
        algorithmState.lastTopStackIndex = 0;
        algorithmState.scrolledPixelsTop = 0.0f;
        algorithmState.itemsInBottomStack = 0.0f;
        algorithmState.partialInBottom = 0.0f;
        float bottomOverScroll = ambientState.getOverScrollAmount(false);
        int scrollY = ambientState.getScrollY();
        algorithmState.scrollY = (int) (this.mCollapsedSize + Math.max(0, scrollY) + bottomOverScroll);
        updateVisibleChildren(resultState, algorithmState);
        findNumberOfItemsInTopStackAndUpdateState(resultState, algorithmState);
        updatePositionsForState(resultState, algorithmState);
        updateZValuesForState(resultState, algorithmState);
        handleDraggedViews(ambientState, resultState, algorithmState);
        updateDimmedActivatedHideSensitive(ambientState, resultState, algorithmState);
        updateClipping(resultState, algorithmState);
        updateSpeedBumpState(resultState, algorithmState, ambientState.getSpeedBumpIndex());
    }

    private void updateSpeedBumpState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, int speedBumpIndex) {
        int childCount = algorithmState.visibleChildren.size();
        int i = 0;
        while (i < childCount) {
            View child = algorithmState.visibleChildren.get(i);
            StackScrollState.ViewState childViewState = resultState.getViewStateForView(child);
            childViewState.belowSpeedBump = speedBumpIndex != -1 && i >= speedBumpIndex;
            i++;
        }
    }

    private void updateClipping(StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        float clipHeight;
        float previousNotificationEnd = 0.0f;
        float previousNotificationStart = 0.0f;
        boolean previousNotificationIsSwiped = false;
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = algorithmState.visibleChildren.get(i);
            StackScrollState.ViewState state = resultState.getViewStateForView(child);
            float newYTranslation = state.yTranslation + ((state.height * (1.0f - state.scale)) / 2.0f);
            float newHeight = state.height * state.scale;
            float newNotificationEnd = newYTranslation + newHeight;
            if (previousNotificationIsSwiped) {
                clipHeight = newHeight;
            } else {
                clipHeight = Math.max(0.0f, newNotificationEnd - previousNotificationEnd);
                if (clipHeight != 0.0f) {
                    float clippingCorrection = state.dimmed ? 0.0f : this.mRoundedRectCornerRadius * state.scale;
                    clipHeight += clippingCorrection;
                }
            }
            updateChildClippingAndBackground(state, newHeight, clipHeight, newHeight - (previousNotificationStart - newYTranslation));
            if (!child.isTransparent()) {
                previousNotificationStart = newYTranslation + (state.clipTopAmount * state.scale);
                previousNotificationEnd = newNotificationEnd;
                previousNotificationIsSwiped = child.getTranslationX() != 0.0f;
            }
        }
    }

    private void updateChildClippingAndBackground(StackScrollState.ViewState state, float realHeight, float clipHeight, float backgroundHeight) {
        if (realHeight > clipHeight) {
            state.topOverLap = (int) Math.floor((realHeight - clipHeight) / state.scale);
        } else {
            state.topOverLap = 0;
        }
        if (realHeight > backgroundHeight) {
            state.clipTopAmount = (int) Math.floor((realHeight - backgroundHeight) / state.scale);
        } else {
            state.clipTopAmount = 0;
        }
    }

    private void updateDimmedActivatedHideSensitive(AmbientState ambientState, StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        boolean dimmed = ambientState.isDimmed();
        boolean dark = ambientState.isDark();
        boolean hideSensitive = ambientState.isHideSensitive();
        View activatedChild = ambientState.getActivatedChild();
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            View child = algorithmState.visibleChildren.get(i);
            StackScrollState.ViewState childViewState = resultState.getViewStateForView(child);
            childViewState.dimmed = dimmed;
            childViewState.dark = dark;
            childViewState.hideSensitive = hideSensitive;
            boolean isActivatedChild = activatedChild == child;
            childViewState.scale = (this.mScaleDimmed && dimmed && !isActivatedChild) ? 0.95f : 1.0f;
            if (dimmed && isActivatedChild) {
                childViewState.zTranslation += 2.0f * this.mZDistanceBetweenElements;
            }
        }
    }

    private void handleDraggedViews(AmbientState ambientState, StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        ArrayList<View> draggedViews = ambientState.getDraggedViews();
        for (View draggedView : draggedViews) {
            int childIndex = algorithmState.visibleChildren.indexOf(draggedView);
            if (childIndex >= 0 && childIndex < algorithmState.visibleChildren.size() - 1) {
                View nextChild = algorithmState.visibleChildren.get(childIndex + 1);
                if (!draggedViews.contains(nextChild)) {
                    StackScrollState.ViewState viewState = resultState.getViewStateForView(nextChild);
                    viewState.alpha = 1.0f;
                }
                StackScrollState.ViewState viewState2 = resultState.getViewStateForView(draggedView);
                viewState2.alpha = draggedView.getAlpha();
            }
        }
    }

    private void updateVisibleChildren(StackScrollState resultState, StackScrollAlgorithmState state) {
        ViewGroup hostView = resultState.getHostView();
        int childCount = hostView.getChildCount();
        state.visibleChildren.clear();
        state.visibleChildren.ensureCapacity(childCount);
        for (int i = 0; i < childCount; i++) {
            ExpandableView v = (ExpandableView) hostView.getChildAt(i);
            if (v.getVisibility() != 8) {
                StackScrollState.ViewState viewState = resultState.getViewStateForView(v);
                viewState.notGoneIndex = state.visibleChildren.size();
                state.visibleChildren.add(v);
            }
        }
    }

    private void updatePositionsForState(StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        float bottomPeekStart = this.mInnerHeight - this.mBottomStackPeekSize;
        float bottomStackStart = bottomPeekStart - this.mBottomStackSlowDownLength;
        float currentYPosition = 0.0f;
        float yPositionInScrollView = 0.0f;
        int childCount = algorithmState.visibleChildren.size();
        int numberOfElementsCompletelyIn = (int) algorithmState.itemsInTopStack;
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = algorithmState.visibleChildren.get(i);
            StackScrollState.ViewState childViewState = resultState.getViewStateForView(child);
            childViewState.location = 0;
            int childHeight = getMaxAllowedChildHeight(child);
            float yPositionInScrollViewAfterElement = childHeight + yPositionInScrollView + this.mPaddingBetweenElements;
            float scrollOffset = (yPositionInScrollView - algorithmState.scrollY) + this.mCollapsedSize;
            if (i == algorithmState.lastTopStackIndex + 1) {
                currentYPosition = Math.min(scrollOffset, bottomStackStart);
            }
            childViewState.yTranslation = currentYPosition;
            float nextYPosition = childHeight + currentYPosition + this.mPaddingBetweenElements;
            if (i <= algorithmState.lastTopStackIndex) {
                updateStateForTopStackChild(algorithmState, numberOfElementsCompletelyIn, i, childHeight, childViewState, scrollOffset);
                clampPositionToTopStackEnd(childViewState, childHeight);
                if (childViewState.yTranslation + childHeight + this.mPaddingBetweenElements >= bottomStackStart && !this.mIsExpansionChanging && i != 0 && this.mIsSmallScreen) {
                    int newSize = (int) Math.max((bottomStackStart - this.mPaddingBetweenElements) - childViewState.yTranslation, this.mCollapsedSize);
                    childViewState.height = newSize;
                    float currentYPosition2 = childViewState.yTranslation;
                    updateStateForChildTransitioningInBottom(algorithmState, bottomStackStart, bottomPeekStart, currentYPosition2, childViewState, childHeight);
                }
                clampPositionToBottomStackStart(childViewState, childViewState.height);
            } else if (nextYPosition < bottomStackStart) {
                childViewState.location = 8;
                clampYTranslation(childViewState, childHeight);
            } else if (currentYPosition >= bottomStackStart) {
                updateStateForChildFullyInBottomStack(algorithmState, bottomStackStart, childViewState, childHeight);
            } else {
                updateStateForChildTransitioningInBottom(algorithmState, bottomStackStart, bottomPeekStart, currentYPosition, childViewState, childHeight);
            }
            if (i == 0) {
                childViewState.alpha = 1.0f;
                childViewState.yTranslation = Math.max(this.mCollapsedSize - algorithmState.scrollY, 0);
                if (childViewState.yTranslation + childViewState.height > bottomPeekStart - this.mCollapseSecondCardPadding) {
                    childViewState.height = (int) Math.max((bottomPeekStart - this.mCollapseSecondCardPadding) - childViewState.yTranslation, this.mCollapsedSize);
                }
                childViewState.location = 1;
            }
            if (childViewState.location == 0) {
                Log.wtf("StackScrollAlgorithm", "Failed to assign location for child " + i);
            }
            currentYPosition = childViewState.yTranslation + childHeight + this.mPaddingBetweenElements;
            yPositionInScrollView = yPositionInScrollViewAfterElement;
            childViewState.yTranslation += this.mTopPadding;
        }
    }

    private void clampYTranslation(StackScrollState.ViewState childViewState, int childHeight) {
        clampPositionToBottomStackStart(childViewState, childHeight);
        clampPositionToTopStackEnd(childViewState, childHeight);
    }

    private void clampPositionToBottomStackStart(StackScrollState.ViewState childViewState, int childHeight) {
        childViewState.yTranslation = Math.min(childViewState.yTranslation, ((this.mInnerHeight - this.mBottomStackPeekSize) - this.mCollapseSecondCardPadding) - childHeight);
    }

    private void clampPositionToTopStackEnd(StackScrollState.ViewState childViewState, int childHeight) {
        childViewState.yTranslation = Math.max(childViewState.yTranslation, this.mCollapsedSize - childHeight);
    }

    private int getMaxAllowedChildHeight(View child) {
        if (child instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) child;
            return row.getIntrinsicHeight();
        }
        if (!(child instanceof ExpandableView)) {
            return child == null ? this.mCollapsedSize : child.getHeight();
        }
        ExpandableView expandableView = (ExpandableView) child;
        return expandableView.getActualHeight();
    }

    private void updateStateForChildTransitioningInBottom(StackScrollAlgorithmState algorithmState, float transitioningPositionStart, float bottomPeakStart, float currentYPosition, StackScrollState.ViewState childViewState, int childHeight) {
        algorithmState.partialInBottom = 1.0f - ((transitioningPositionStart - currentYPosition) / (this.mPaddingBetweenElements + childHeight));
        float offset = this.mBottomStackIndentationFunctor.getValue(algorithmState.partialInBottom);
        algorithmState.itemsInBottomStack += algorithmState.partialInBottom;
        int newHeight = childHeight;
        if (childHeight > this.mCollapsedSize && this.mIsSmallScreen) {
            newHeight = (int) Math.max(Math.min(((transitioningPositionStart + offset) - this.mPaddingBetweenElements) - currentYPosition, childHeight), this.mCollapsedSize);
            childViewState.height = newHeight;
        }
        childViewState.yTranslation = ((transitioningPositionStart + offset) - newHeight) - this.mPaddingBetweenElements;
        clampPositionToTopStackEnd(childViewState, newHeight);
        childViewState.location = 8;
    }

    private void updateStateForChildFullyInBottomStack(StackScrollAlgorithmState algorithmState, float transitioningPositionStart, StackScrollState.ViewState childViewState, int childHeight) {
        float currentYPosition;
        algorithmState.itemsInBottomStack += 1.0f;
        if (algorithmState.itemsInBottomStack < 3.0f) {
            currentYPosition = (this.mBottomStackIndentationFunctor.getValue(algorithmState.itemsInBottomStack) + transitioningPositionStart) - this.mPaddingBetweenElements;
            childViewState.location = 16;
        } else {
            if (algorithmState.itemsInBottomStack > 5.0f) {
                childViewState.alpha = 0.0f;
            } else if (algorithmState.itemsInBottomStack > 4.0f) {
                childViewState.alpha = 1.0f - algorithmState.partialInBottom;
            }
            childViewState.location = 32;
            currentYPosition = this.mInnerHeight;
        }
        childViewState.yTranslation = currentYPosition - childHeight;
        clampPositionToTopStackEnd(childViewState, childHeight);
    }

    private void updateStateForTopStackChild(StackScrollAlgorithmState algorithmState, int numberOfElementsCompletelyIn, int i, int childHeight, StackScrollState.ViewState childViewState, float scrollOffset) {
        float numItemsBefore;
        int paddedIndex = (i - 1) - Math.max(numberOfElementsCompletelyIn - 3, 0);
        if (paddedIndex >= 0) {
            float distanceToStack = (this.mPaddingBetweenElements + childHeight) - algorithmState.scrolledPixelsTop;
            if (i == algorithmState.lastTopStackIndex && distanceToStack > this.mTopStackTotalSize + this.mPaddingBetweenElements) {
                childViewState.yTranslation = scrollOffset;
            } else {
                if (i == algorithmState.lastTopStackIndex) {
                    numItemsBefore = 1.0f - (distanceToStack / (this.mTopStackTotalSize + this.mPaddingBetweenElements));
                } else {
                    numItemsBefore = algorithmState.itemsInTopStack - i;
                }
                float currentChildEndY = (this.mCollapsedSize + this.mTopStackTotalSize) - this.mTopStackIndentationFunctor.getValue(numItemsBefore);
                childViewState.yTranslation = currentChildEndY - childHeight;
            }
            childViewState.location = 4;
            return;
        }
        if (paddedIndex == -1) {
            childViewState.alpha = 1.0f - algorithmState.partialInTop;
        } else {
            childViewState.alpha = 0.0f;
        }
        childViewState.yTranslation = this.mCollapsedSize - childHeight;
        childViewState.location = 2;
    }

    private void findNumberOfItemsInTopStackAndUpdateState(StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        float yPositionInScrollView = 0.0f;
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = algorithmState.visibleChildren.get(i);
            StackScrollState.ViewState childViewState = resultState.getViewStateForView(child);
            int childHeight = getMaxAllowedChildHeight(child);
            float yPositionInScrollViewAfterElement = childHeight + yPositionInScrollView + this.mPaddingBetweenElements;
            if (yPositionInScrollView < algorithmState.scrollY) {
                if (i == 0 && algorithmState.scrollY <= this.mCollapsedSize) {
                    int bottomPeekStart = (this.mInnerHeight - this.mBottomStackPeekSize) - this.mCollapseSecondCardPadding;
                    float maxHeight = (this.mIsExpansionChanging && child == this.mFirstChildWhileExpanding) ? this.mFirstChildMaxHeight : childHeight;
                    childViewState.height = (int) Math.max(Math.min(bottomPeekStart, maxHeight), this.mCollapsedSize);
                    algorithmState.itemsInTopStack = 1.0f;
                } else if (yPositionInScrollViewAfterElement < algorithmState.scrollY) {
                    algorithmState.itemsInTopStack += 1.0f;
                    if (i == 0) {
                        childViewState.height = this.mCollapsedSize;
                    }
                } else {
                    algorithmState.scrolledPixelsTop = algorithmState.scrollY - yPositionInScrollView;
                    algorithmState.partialInTop = algorithmState.scrolledPixelsTop / (this.mPaddingBetweenElements + childHeight);
                    algorithmState.partialInTop = Math.max(0.0f, algorithmState.partialInTop);
                    algorithmState.itemsInTopStack += algorithmState.partialInTop;
                    if (i == 0) {
                        float newSize = ((yPositionInScrollViewAfterElement - this.mPaddingBetweenElements) - algorithmState.scrollY) + this.mCollapsedSize;
                        float newSize2 = Math.max(this.mCollapsedSize, newSize);
                        algorithmState.itemsInTopStack = 1.0f;
                        childViewState.height = (int) newSize2;
                    }
                    algorithmState.lastTopStackIndex = i;
                    return;
                }
                yPositionInScrollView = yPositionInScrollViewAfterElement;
            } else {
                algorithmState.lastTopStackIndex = i - 1;
                return;
            }
        }
    }

    private void updateZValuesForState(StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        int childCount = algorithmState.visibleChildren.size();
        int i = 0;
        while (i < childCount) {
            View child = algorithmState.visibleChildren.get(i);
            StackScrollState.ViewState childViewState = resultState.getViewStateForView(child);
            if (i < algorithmState.itemsInTopStack) {
                float stackIndex = algorithmState.itemsInTopStack - i;
                float max = 3.0f + (i == 0 ? 2.5f : 2.0f);
                float stackIndex2 = Math.min(stackIndex, max);
                if (i == 0 && algorithmState.itemsInTopStack < 2.0f) {
                    stackIndex2 -= 1.0f;
                    if (algorithmState.scrollY > this.mCollapsedSize) {
                        stackIndex2 = 0.1f + (1.9f * stackIndex2);
                    }
                }
                childViewState.zTranslation = this.mZBasicHeight + (this.mZDistanceBetweenElements * stackIndex2);
            } else if (i > (childCount - 1) - algorithmState.itemsInBottomStack) {
                float numItemsAbove = i - ((childCount - 1) - algorithmState.itemsInBottomStack);
                float translationZ = this.mZBasicHeight - (this.mZDistanceBetweenElements * numItemsAbove);
                childViewState.zTranslation = translationZ;
            } else {
                childViewState.zTranslation = this.mZBasicHeight;
            }
            i++;
        }
    }

    public void setLayoutHeight(int layoutHeight) {
        this.mLayoutHeight = layoutHeight;
        updateInnerHeight();
    }

    public void setTopPadding(int topPadding) {
        this.mTopPadding = topPadding;
        updateInnerHeight();
    }

    private void updateInnerHeight() {
        this.mInnerHeight = this.mLayoutHeight - this.mTopPadding;
    }

    public void updateIsSmallScreen(int panelHeight) {
        this.mIsSmallScreen = panelHeight < ((this.mCollapsedSize + this.mBottomStackSlowDownLength) + this.mBottomStackPeekSize) + this.mMaxNotificationHeight;
    }

    public void onExpansionStarted(StackScrollState currentState) {
        this.mIsExpansionChanging = true;
        this.mExpandedOnStart = this.mIsExpanded;
        ViewGroup hostView = currentState.getHostView();
        updateFirstChildHeightWhileExpanding(hostView);
    }

    private void updateFirstChildHeightWhileExpanding(ViewGroup hostView) {
        this.mFirstChildWhileExpanding = (ExpandableView) findFirstVisibleChild(hostView);
        if (this.mFirstChildWhileExpanding != null) {
            if (this.mExpandedOnStart) {
                this.mFirstChildMaxHeight = StackStateAnimator.getFinalActualHeight(this.mFirstChildWhileExpanding);
                return;
            } else {
                updateFirstChildMaxSizeToMaxHeight();
                return;
            }
        }
        this.mFirstChildMaxHeight = 0;
    }

    private void updateFirstChildMaxSizeToMaxHeight() {
        if (!isMaxSizeInitialized(this.mFirstChildWhileExpanding)) {
            this.mFirstChildWhileExpanding.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (StackScrollAlgorithm.this.mFirstChildWhileExpanding == null) {
                        StackScrollAlgorithm.this.mFirstChildMaxHeight = 0;
                    } else {
                        StackScrollAlgorithm.this.mFirstChildMaxHeight = StackScrollAlgorithm.this.getMaxAllowedChildHeight(StackScrollAlgorithm.this.mFirstChildWhileExpanding);
                    }
                    v.removeOnLayoutChangeListener(this);
                }
            });
        } else {
            this.mFirstChildMaxHeight = getMaxAllowedChildHeight(this.mFirstChildWhileExpanding);
        }
    }

    private boolean isMaxSizeInitialized(ExpandableView child) {
        if (!(child instanceof ExpandableNotificationRow)) {
            return child == null || child.getWidth() != 0;
        }
        ExpandableNotificationRow row = (ExpandableNotificationRow) child;
        return row.isMaxExpandHeightInitialized();
    }

    private View findFirstVisibleChild(ViewGroup container) {
        int childCount = container.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return null;
    }

    public void onExpansionStopped() {
        this.mIsExpansionChanging = false;
        this.mFirstChildWhileExpanding = null;
    }

    public void setIsExpanded(boolean isExpanded) {
        this.mIsExpanded = isExpanded;
    }

    public void notifyChildrenChanged(final ViewGroup hostView) {
        if (this.mIsExpansionChanging) {
            hostView.post(new Runnable() {
                @Override
                public void run() {
                    StackScrollAlgorithm.this.updateFirstChildHeightWhileExpanding(hostView);
                }
            });
        }
    }

    public void setDimmed(boolean dimmed) {
        updatePadding(dimmed);
    }

    public void onReset(ExpandableView view) {
        if (view.equals(this.mFirstChildWhileExpanding)) {
            updateFirstChildMaxSizeToMaxHeight();
        }
    }

    class StackScrollAlgorithmState {
        public float itemsInBottomStack;
        public float itemsInTopStack;
        public int lastTopStackIndex;
        public float partialInBottom;
        public float partialInTop;
        public int scrollY;
        public float scrolledPixelsTop;
        public final ArrayList<ExpandableView> visibleChildren = new ArrayList<>();

        StackScrollAlgorithmState() {
        }
    }
}
