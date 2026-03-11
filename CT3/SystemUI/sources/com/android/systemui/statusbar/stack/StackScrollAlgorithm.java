package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StackScrollAlgorithm {
    private StackIndentationFunctor mBottomStackIndentationFunctor;
    private int mBottomStackPeekSize;
    private int mBottomStackSlowDownLength;
    private int mCollapsedSize;
    private int mIncreasedPaddingBetweenElements;
    private boolean mIsExpanded;
    private int mPaddingBetweenElements;
    private StackScrollAlgorithmState mTempAlgorithmState = new StackScrollAlgorithmState();
    private int mZBasicHeight;
    private int mZDistanceBetweenElements;

    public StackScrollAlgorithm(Context context) {
        initView(context);
    }

    public void initView(Context context) {
        initConstants(context);
    }

    public int getBottomStackSlowDownLength() {
        return this.mBottomStackSlowDownLength + this.mPaddingBetweenElements;
    }

    private void initConstants(Context context) {
        this.mPaddingBetweenElements = Math.max(1, context.getResources().getDimensionPixelSize(R.dimen.notification_divider_height));
        this.mIncreasedPaddingBetweenElements = context.getResources().getDimensionPixelSize(R.dimen.notification_divider_height_increased);
        this.mCollapsedSize = context.getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        this.mBottomStackPeekSize = context.getResources().getDimensionPixelSize(R.dimen.bottom_stack_peek_amount);
        this.mZDistanceBetweenElements = Math.max(1, context.getResources().getDimensionPixelSize(R.dimen.z_distance_between_notifications));
        this.mZBasicHeight = this.mZDistanceBetweenElements * 4;
        this.mBottomStackSlowDownLength = context.getResources().getDimensionPixelSize(R.dimen.bottom_stack_slow_down_length);
        this.mBottomStackIndentationFunctor = new PiecewiseLinearIndentationFunctor(3, this.mBottomStackPeekSize, getBottomStackSlowDownLength(), 0.5f);
    }

    public void getStackScrollState(AmbientState ambientState, StackScrollState resultState) {
        StackScrollAlgorithmState algorithmState = this.mTempAlgorithmState;
        resultState.resetViewStates();
        initAlgorithmState(resultState, algorithmState, ambientState);
        updatePositionsForState(resultState, algorithmState, ambientState);
        updateZValuesForState(resultState, algorithmState, ambientState);
        updateHeadsUpStates(resultState, algorithmState, ambientState);
        handleDraggedViews(ambientState, resultState, algorithmState);
        updateDimmedActivatedHideSensitive(ambientState, resultState, algorithmState);
        updateClipping(resultState, algorithmState, ambientState);
        updateSpeedBumpState(resultState, algorithmState, ambientState.getSpeedBumpIndex());
        getNotificationChildrenStates(resultState, algorithmState);
    }

    private void getNotificationChildrenStates(StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            ExpandableView v = algorithmState.visibleChildren.get(i);
            if (v instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                row.getChildrenStates(resultState);
            }
        }
    }

    private void updateSpeedBumpState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, int speedBumpIndex) {
        int childCount = algorithmState.visibleChildren.size();
        int i = 0;
        while (i < childCount) {
            View child = algorithmState.visibleChildren.get(i);
            StackViewState childViewState = resultState.getViewStateForView(child);
            childViewState.belowSpeedBump = speedBumpIndex != -1 && i >= speedBumpIndex;
            i++;
        }
    }

    private void updateClipping(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        boolean zIsPinned;
        float drawStart = ambientState.getTopPadding() + ambientState.getStackTranslation();
        float previousNotificationEnd = 0.0f;
        float previousNotificationStart = 0.0f;
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = algorithmState.visibleChildren.get(i);
            StackViewState state = resultState.getViewStateForView(child);
            if (!child.mustStayOnScreen()) {
                previousNotificationEnd = Math.max(drawStart, previousNotificationEnd);
                previousNotificationStart = Math.max(drawStart, previousNotificationStart);
            }
            float newYTranslation = state.yTranslation;
            float newHeight = state.height;
            float newNotificationEnd = newYTranslation + newHeight;
            if (!(child instanceof ExpandableNotificationRow)) {
                zIsPinned = false;
            } else {
                zIsPinned = ((ExpandableNotificationRow) child).isPinned();
            }
            if (newYTranslation < previousNotificationEnd && (!zIsPinned || ambientState.isShadeExpanded())) {
                float overlapAmount = previousNotificationEnd - newYTranslation;
                state.clipTopAmount = (int) overlapAmount;
            } else {
                state.clipTopAmount = 0;
            }
            if (!child.isTransparent()) {
                previousNotificationEnd = newNotificationEnd;
                previousNotificationStart = newYTranslation;
            }
        }
    }

    public static boolean canChildBeDismissed(View v) {
        if (!(v instanceof ExpandableNotificationRow)) {
            return false;
        }
        ExpandableNotificationRow row = (ExpandableNotificationRow) v;
        if (row.areGutsExposed()) {
            return false;
        }
        return row.canViewBeDismissed();
    }

    private void updateDimmedActivatedHideSensitive(AmbientState ambientState, StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        boolean dimmed = ambientState.isDimmed();
        boolean dark = ambientState.isDark();
        boolean hideSensitive = ambientState.isHideSensitive();
        View activatedChild = ambientState.getActivatedChild();
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            View child = algorithmState.visibleChildren.get(i);
            StackViewState childViewState = resultState.getViewStateForView(child);
            childViewState.dimmed = dimmed;
            childViewState.dark = dark;
            childViewState.hideSensitive = hideSensitive;
            boolean isActivatedChild = activatedChild == child;
            if (dimmed && isActivatedChild) {
                childViewState.zTranslation += this.mZDistanceBetweenElements * 2.0f;
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
                    StackViewState viewState = resultState.getViewStateForView(nextChild);
                    if (ambientState.isShadeExpanded()) {
                        viewState.shadowAlpha = 1.0f;
                        viewState.hidden = false;
                    }
                }
                resultState.getViewStateForView(draggedView).alpha = draggedView.getAlpha();
            }
        }
    }

    private void initAlgorithmState(StackScrollState resultState, StackScrollAlgorithmState state, AmbientState ambientState) {
        float newValue;
        state.itemsInBottomStack = 0.0f;
        state.partialInBottom = 0.0f;
        float bottomOverScroll = ambientState.getOverScrollAmount(false);
        int scrollY = ambientState.getScrollY();
        state.scrollY = (int) (Math.max(0, scrollY) + bottomOverScroll);
        ViewGroup hostView = resultState.getHostView();
        int childCount = hostView.getChildCount();
        state.visibleChildren.clear();
        state.visibleChildren.ensureCapacity(childCount);
        state.increasedPaddingMap.clear();
        int notGoneIndex = 0;
        ExpandableView lastView = null;
        for (int i = 0; i < childCount; i++) {
            ExpandableView v = (ExpandableView) hostView.getChildAt(i);
            if (v.getVisibility() != 8) {
                notGoneIndex = updateNotGoneIndex(resultState, state, notGoneIndex, v);
                float increasedPadding = v.getIncreasedPaddingAmount();
                if (increasedPadding != 0.0f) {
                    state.increasedPaddingMap.put(v, Float.valueOf(increasedPadding));
                    if (lastView != null) {
                        Float prevValue = state.increasedPaddingMap.get(lastView);
                        if (prevValue != null) {
                            newValue = Math.max(prevValue.floatValue(), increasedPadding);
                        } else {
                            newValue = increasedPadding;
                        }
                        state.increasedPaddingMap.put(lastView, Float.valueOf(newValue));
                    }
                }
                if (v instanceof ExpandableNotificationRow) {
                    ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                    List<ExpandableNotificationRow> children = row.getNotificationChildren();
                    if (row.isSummaryWithChildren() && children != null) {
                        for (ExpandableNotificationRow childRow : children) {
                            if (childRow.getVisibility() != 8) {
                                StackViewState childState = resultState.getViewStateForView(childRow);
                                childState.notGoneIndex = notGoneIndex;
                                notGoneIndex++;
                            }
                        }
                    }
                }
                lastView = v;
            }
        }
    }

    private int updateNotGoneIndex(StackScrollState resultState, StackScrollAlgorithmState state, int notGoneIndex, ExpandableView v) {
        StackViewState viewState = resultState.getViewStateForView(v);
        viewState.notGoneIndex = notGoneIndex;
        state.visibleChildren.add(v);
        return notGoneIndex + 1;
    }

    private void updatePositionsForState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        float bottomPeekStart = ambientState.getInnerHeight() - this.mBottomStackPeekSize;
        float bottomStackStart = bottomPeekStart - this.mBottomStackSlowDownLength;
        float currentYPosition = -algorithmState.scrollY;
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = algorithmState.visibleChildren.get(i);
            StackViewState childViewState = resultState.getViewStateForView(child);
            childViewState.location = 0;
            int paddingAfterChild = getPaddingAfterChild(algorithmState, child);
            int childHeight = getMaxAllowedChildHeight(child);
            int collapsedHeight = child.getCollapsedHeight();
            childViewState.yTranslation = currentYPosition;
            if (i == 0) {
                updateFirstChildHeight(child, childViewState, childHeight, ambientState);
            }
            float nextYPosition = childHeight + currentYPosition + paddingAfterChild;
            if (nextYPosition < bottomStackStart) {
                childViewState.location = 4;
                clampPositionToBottomStackStart(childViewState, childViewState.height, childHeight, ambientState);
            } else if (currentYPosition >= bottomStackStart) {
                updateStateForChildFullyInBottomStack(algorithmState, bottomStackStart, childViewState, collapsedHeight, ambientState, child);
            } else {
                updateStateForChildTransitioningInBottom(algorithmState, bottomStackStart, child, currentYPosition, childViewState, childHeight);
            }
            if (i == 0 && ambientState.getScrollY() <= 0) {
                childViewState.yTranslation = Math.max(0.0f, childViewState.yTranslation);
            }
            currentYPosition = childViewState.yTranslation + childHeight + paddingAfterChild;
            if (currentYPosition <= 0.0f) {
                childViewState.location = 2;
            }
            if (childViewState.location == 0) {
                Log.wtf("StackScrollAlgorithm", "Failed to assign location for child " + i);
            }
            childViewState.yTranslation += ambientState.getTopPadding() + ambientState.getStackTranslation();
        }
    }

    private int getPaddingAfterChild(StackScrollAlgorithmState algorithmState, ExpandableView child) {
        Float paddingValue = algorithmState.increasedPaddingMap.get(child);
        if (paddingValue == null) {
            return this.mPaddingBetweenElements;
        }
        return (int) NotificationUtils.interpolate(this.mPaddingBetweenElements, this.mIncreasedPaddingBetweenElements, paddingValue.floatValue());
    }

    private void updateHeadsUpStates(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        int childCount = algorithmState.visibleChildren.size();
        ExpandableNotificationRow topHeadsUpEntry = null;
        for (int i = 0; i < childCount; i++) {
            View child = algorithmState.visibleChildren.get(i);
            if (!(child instanceof ExpandableNotificationRow)) {
                return;
            }
            ExpandableNotificationRow row = (ExpandableNotificationRow) child;
            if (!row.isHeadsUp()) {
                return;
            }
            StackViewState childState = resultState.getViewStateForView(row);
            if (topHeadsUpEntry == null) {
                topHeadsUpEntry = row;
                childState.location = 1;
            }
            boolean isTopEntry = topHeadsUpEntry == row;
            float unmodifiedEndLocation = childState.yTranslation + childState.height;
            if (this.mIsExpanded) {
                clampHunToTop(ambientState, row, childState);
                clampHunToMaxTranslation(ambientState, row, childState);
            }
            if (row.isPinned()) {
                childState.yTranslation = Math.max(childState.yTranslation, 0.0f);
                childState.height = Math.max(row.getIntrinsicHeight(), childState.height);
                StackViewState topState = resultState.getViewStateForView(topHeadsUpEntry);
                if (!isTopEntry && (!this.mIsExpanded || unmodifiedEndLocation < topState.yTranslation + topState.height)) {
                    childState.height = row.getIntrinsicHeight();
                    childState.yTranslation = (topState.yTranslation + topState.height) - childState.height;
                }
            }
        }
    }

    private void clampHunToTop(AmbientState ambientState, ExpandableNotificationRow row, StackViewState childState) {
        float newTranslation = Math.max(ambientState.getTopPadding() + ambientState.getStackTranslation(), childState.yTranslation);
        childState.height = (int) Math.max(childState.height - (newTranslation - childState.yTranslation), row.getCollapsedHeight());
        childState.yTranslation = newTranslation;
    }

    private void clampHunToMaxTranslation(AmbientState ambientState, ExpandableNotificationRow row, StackViewState childState) {
        float bottomPosition = ambientState.getMaxHeadsUpTranslation() - row.getCollapsedHeight();
        float newTranslation = Math.min(childState.yTranslation, bottomPosition);
        childState.height = (int) Math.max(childState.height - (childState.yTranslation - newTranslation), row.getCollapsedHeight());
        childState.yTranslation = newTranslation;
    }

    private void clampPositionToBottomStackStart(StackViewState childViewState, int childHeight, int minHeight, AmbientState ambientState) {
        int bottomStackStart = (ambientState.getInnerHeight() - this.mBottomStackPeekSize) - this.mBottomStackSlowDownLength;
        int childStart = bottomStackStart - childHeight;
        if (childStart >= childViewState.yTranslation) {
            return;
        }
        float newHeight = bottomStackStart - childViewState.yTranslation;
        if (newHeight < minHeight) {
            newHeight = minHeight;
            childViewState.yTranslation = bottomStackStart - minHeight;
        }
        childViewState.height = (int) newHeight;
    }

    private int getMaxAllowedChildHeight(View child) {
        if (!(child instanceof ExpandableView)) {
            return child == null ? this.mCollapsedSize : child.getHeight();
        }
        ExpandableView expandableView = (ExpandableView) child;
        return expandableView.getIntrinsicHeight();
    }

    private void updateStateForChildTransitioningInBottom(StackScrollAlgorithmState algorithmState, float transitioningPositionStart, ExpandableView child, float currentYPosition, StackViewState childViewState, int childHeight) {
        algorithmState.partialInBottom = 1.0f - ((transitioningPositionStart - currentYPosition) / (getPaddingAfterChild(algorithmState, child) + childHeight));
        float offset = this.mBottomStackIndentationFunctor.getValue(algorithmState.partialInBottom);
        algorithmState.itemsInBottomStack += algorithmState.partialInBottom;
        int newHeight = childHeight;
        if (childHeight > child.getCollapsedHeight()) {
            newHeight = (int) Math.max(Math.min(((transitioningPositionStart + offset) - getPaddingAfterChild(algorithmState, child)) - currentYPosition, childHeight), child.getCollapsedHeight());
            childViewState.height = newHeight;
        }
        childViewState.yTranslation = ((transitioningPositionStart + offset) - newHeight) - getPaddingAfterChild(algorithmState, child);
        childViewState.location = 4;
    }

    private void updateStateForChildFullyInBottomStack(StackScrollAlgorithmState algorithmState, float transitioningPositionStart, StackViewState childViewState, int collapsedHeight, AmbientState ambientState, ExpandableView child) {
        float currentYPosition;
        algorithmState.itemsInBottomStack += 1.0f;
        if (algorithmState.itemsInBottomStack < 3.0f) {
            currentYPosition = (this.mBottomStackIndentationFunctor.getValue(algorithmState.itemsInBottomStack) + transitioningPositionStart) - getPaddingAfterChild(algorithmState, child);
            childViewState.location = 8;
        } else {
            if (algorithmState.itemsInBottomStack > 5.0f) {
                childViewState.hidden = true;
                childViewState.shadowAlpha = 0.0f;
            } else if (algorithmState.itemsInBottomStack > 4.0f) {
                childViewState.shadowAlpha = 1.0f - algorithmState.partialInBottom;
            }
            childViewState.location = 16;
            currentYPosition = ambientState.getInnerHeight();
        }
        childViewState.height = collapsedHeight;
        childViewState.yTranslation = currentYPosition - collapsedHeight;
    }

    private void updateFirstChildHeight(ExpandableView child, StackViewState childViewState, int childHeight, AmbientState ambientState) {
        int bottomPeekStart = ((ambientState.getInnerHeight() - this.mBottomStackPeekSize) - this.mBottomStackSlowDownLength) + ambientState.getScrollY();
        childViewState.height = (int) Math.max(Math.min(bottomPeekStart, childHeight), child.getCollapsedHeight());
    }

    private void updateZValuesForState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        float zSubtraction;
        int childCount = algorithmState.visibleChildren.size();
        float childrenOnTop = 0.0f;
        for (int i = childCount - 1; i >= 0; i--) {
            ExpandableView child = algorithmState.visibleChildren.get(i);
            StackViewState childViewState = resultState.getViewStateForView(child);
            if (i > (childCount - 1) - algorithmState.itemsInBottomStack) {
                float numItemsAbove = i - ((childCount - 1) - algorithmState.itemsInBottomStack);
                if (numItemsAbove > 1.0f) {
                    zSubtraction = numItemsAbove * this.mZDistanceBetweenElements;
                } else if (numItemsAbove <= 0.2f) {
                    zSubtraction = 0.1f * numItemsAbove * 5.0f;
                } else {
                    zSubtraction = 0.1f + ((numItemsAbove - 0.2f) * (1.0f / 0.8f) * (this.mZDistanceBetweenElements - 0.1f));
                }
                childViewState.zTranslation = this.mZBasicHeight - zSubtraction;
            } else if (child.mustStayOnScreen() && childViewState.yTranslation < ambientState.getTopPadding() + ambientState.getStackTranslation()) {
                if (childrenOnTop != 0.0f) {
                    childrenOnTop += 1.0f;
                } else {
                    float overlap = (ambientState.getTopPadding() + ambientState.getStackTranslation()) - childViewState.yTranslation;
                    childrenOnTop += Math.min(1.0f, overlap / childViewState.height);
                }
                childViewState.zTranslation = this.mZBasicHeight + (this.mZDistanceBetweenElements * childrenOnTop);
            } else {
                childViewState.zTranslation = this.mZBasicHeight;
            }
        }
    }

    public void setIsExpanded(boolean isExpanded) {
        this.mIsExpanded = isExpanded;
    }

    class StackScrollAlgorithmState {
        public float itemsInBottomStack;
        public float partialInBottom;
        public int scrollY;
        public final ArrayList<ExpandableView> visibleChildren = new ArrayList<>();
        public final HashMap<ExpandableView, Float> increasedPaddingMap = new HashMap<>();

        StackScrollAlgorithmState() {
        }
    }
}
