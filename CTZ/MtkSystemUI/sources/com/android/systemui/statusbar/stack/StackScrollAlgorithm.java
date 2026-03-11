package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FooterView;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.notification.NotificationUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StackScrollAlgorithm {
    private boolean mClipNotificationScrollToTop;
    private int mCollapsedSize;
    private float mHeadsUpInset;
    private int mIncreasedPaddingBetweenElements;
    private boolean mIsExpanded;
    private int mPaddingBetweenElements;
    private int mPinnedZTranslationExtra;
    private int mStatusBarHeight;
    private StackScrollAlgorithmState mTempAlgorithmState = new StackScrollAlgorithmState();

    public StackScrollAlgorithm(Context context) {
        initView(context);
    }

    public void initView(Context context) {
        initConstants(context);
    }

    private void initConstants(Context context) {
        Resources resources = context.getResources();
        this.mPaddingBetweenElements = resources.getDimensionPixelSize(R.dimen.notification_divider_height);
        this.mIncreasedPaddingBetweenElements = resources.getDimensionPixelSize(R.dimen.notification_divider_height_increased);
        this.mCollapsedSize = resources.getDimensionPixelSize(R.dimen.notification_min_height);
        this.mStatusBarHeight = resources.getDimensionPixelSize(R.dimen.status_bar_height);
        this.mClipNotificationScrollToTop = resources.getBoolean(R.bool.config_clipNotificationScrollToTop);
        this.mHeadsUpInset = this.mStatusBarHeight + resources.getDimensionPixelSize(R.dimen.heads_up_status_bar_padding);
        this.mPinnedZTranslationExtra = resources.getDimensionPixelSize(R.dimen.heads_up_pinned_elevation);
    }

    public void getStackScrollState(AmbientState ambientState, StackScrollState stackScrollState) {
        StackScrollAlgorithmState stackScrollAlgorithmState = this.mTempAlgorithmState;
        stackScrollState.resetViewStates();
        initAlgorithmState(stackScrollState, stackScrollAlgorithmState, ambientState);
        updatePositionsForState(stackScrollState, stackScrollAlgorithmState, ambientState);
        updateZValuesForState(stackScrollState, stackScrollAlgorithmState, ambientState);
        updateHeadsUpStates(stackScrollState, stackScrollAlgorithmState, ambientState);
        handleDraggedViews(ambientState, stackScrollState, stackScrollAlgorithmState);
        updateDimmedActivatedHideSensitive(ambientState, stackScrollState, stackScrollAlgorithmState);
        updateClipping(stackScrollState, stackScrollAlgorithmState, ambientState);
        updateSpeedBumpState(stackScrollState, stackScrollAlgorithmState, ambientState);
        updateShelfState(stackScrollState, ambientState);
        getNotificationChildrenStates(stackScrollState, stackScrollAlgorithmState, ambientState);
    }

    private void getNotificationChildrenStates(StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, AmbientState ambientState) {
        int size = stackScrollAlgorithmState.visibleChildren.size();
        for (int i = 0; i < size; i++) {
            ExpandableView expandableView = stackScrollAlgorithmState.visibleChildren.get(i);
            if (expandableView instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) expandableView).getChildrenStates(stackScrollState, ambientState);
            }
        }
    }

    private void updateSpeedBumpState(StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, AmbientState ambientState) {
        int size = stackScrollAlgorithmState.visibleChildren.size();
        int speedBumpIndex = ambientState.getSpeedBumpIndex();
        int i = 0;
        while (i < size) {
            stackScrollState.getViewStateForView(stackScrollAlgorithmState.visibleChildren.get(i)).belowSpeedBump = i >= speedBumpIndex;
            i++;
        }
    }

    private void updateShelfState(StackScrollState stackScrollState, AmbientState ambientState) {
        NotificationShelf shelf = ambientState.getShelf();
        if (shelf != null) {
            shelf.updateState(stackScrollState, ambientState);
        }
    }

    private void updateClipping(StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, AmbientState ambientState) {
        float topPadding;
        if (!ambientState.isOnKeyguard()) {
            topPadding = ambientState.getTopPadding() + ambientState.getStackTranslation() + ambientState.getExpandAnimationTopChange();
        } else {
            topPadding = 0.0f;
        }
        int size = stackScrollAlgorithmState.visibleChildren.size();
        float fMax = 0.0f;
        float fMax2 = 0.0f;
        for (int i = 0; i < size; i++) {
            ExpandableView expandableView = stackScrollAlgorithmState.visibleChildren.get(i);
            ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(expandableView);
            if (!expandableView.mustStayOnScreen() || viewStateForView.headsUpIsVisible) {
                fMax = Math.max(topPadding, fMax);
                fMax2 = Math.max(topPadding, fMax2);
            }
            float f = viewStateForView.yTranslation;
            float f2 = viewStateForView.height + f;
            boolean z = (expandableView instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) expandableView).isPinned();
            if (this.mClipNotificationScrollToTop && !viewStateForView.inShelf && f < fMax && (!z || ambientState.isShadeExpanded())) {
                viewStateForView.clipTopAmount = (int) (fMax - f);
            } else {
                viewStateForView.clipTopAmount = 0;
            }
            if (!expandableView.isTransparent()) {
                fMax2 = f;
                fMax = f2;
            }
        }
    }

    public static boolean canChildBeDismissed(View view) {
        if (!(view instanceof ExpandableNotificationRow)) {
            return false;
        }
        ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
        if (expandableNotificationRow.areGutsExposed()) {
            return false;
        }
        return expandableNotificationRow.canViewBeDismissed();
    }

    private void updateDimmedActivatedHideSensitive(AmbientState ambientState, StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState) {
        boolean zIsDimmed = ambientState.isDimmed();
        boolean zIsFullyDark = ambientState.isFullyDark();
        boolean zIsHideSensitive = ambientState.isHideSensitive();
        ActivatableNotificationView activatedChild = ambientState.getActivatedChild();
        int size = stackScrollAlgorithmState.visibleChildren.size();
        for (int i = 0; i < size; i++) {
            ExpandableView expandableView = stackScrollAlgorithmState.visibleChildren.get(i);
            ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(expandableView);
            viewStateForView.dimmed = zIsDimmed;
            viewStateForView.dark = zIsFullyDark;
            viewStateForView.hideSensitive = zIsHideSensitive;
            boolean z = activatedChild == expandableView;
            if (zIsDimmed && z) {
                viewStateForView.zTranslation += 2.0f * ambientState.getZDistanceBetweenElements();
            }
        }
    }

    private void handleDraggedViews(AmbientState ambientState, StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState) {
        ArrayList<View> draggedViews = ambientState.getDraggedViews();
        for (View view : draggedViews) {
            int iIndexOf = stackScrollAlgorithmState.visibleChildren.indexOf(view);
            if (iIndexOf >= 0 && iIndexOf < stackScrollAlgorithmState.visibleChildren.size() - 1) {
                ExpandableView expandableView = stackScrollAlgorithmState.visibleChildren.get(iIndexOf + 1);
                if (!draggedViews.contains(expandableView)) {
                    ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(expandableView);
                    if (ambientState.isShadeExpanded()) {
                        viewStateForView.shadowAlpha = 1.0f;
                        viewStateForView.hidden = false;
                    }
                }
                stackScrollState.getViewStateForView(view).alpha = view.getAlpha();
            }
        }
    }

    private void initAlgorithmState(StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, AmbientState ambientState) {
        stackScrollAlgorithmState.scrollY = (int) (Math.max(0, ambientState.getScrollY()) + ambientState.getOverScrollAmount(false));
        ViewGroup hostView = stackScrollState.getHostView();
        int childCount = hostView.getChildCount();
        stackScrollAlgorithmState.visibleChildren.clear();
        stackScrollAlgorithmState.visibleChildren.ensureCapacity(childCount);
        stackScrollAlgorithmState.paddingMap.clear();
        int i = ambientState.isDark() ? ambientState.hasPulsingNotifications() ? 1 : 0 : childCount;
        int iUpdateNotGoneIndex = 0;
        ExpandableView expandableView = null;
        for (int i2 = 0; i2 < childCount; i2++) {
            ExpandableView expandableView2 = (ExpandableView) hostView.getChildAt(i2);
            if (expandableView2.getVisibility() != 8 && expandableView2 != ambientState.getShelf()) {
                if (i2 >= i) {
                    expandableView = null;
                }
                iUpdateNotGoneIndex = updateNotGoneIndex(stackScrollState, stackScrollAlgorithmState, iUpdateNotGoneIndex, expandableView2);
                float increasedPaddingAmount = expandableView2.getIncreasedPaddingAmount();
                if (increasedPaddingAmount != 0.0f) {
                    stackScrollAlgorithmState.paddingMap.put(expandableView2, Float.valueOf(increasedPaddingAmount));
                    if (expandableView != null) {
                        Float f = stackScrollAlgorithmState.paddingMap.get(expandableView);
                        float paddingForValue = getPaddingForValue(Float.valueOf(increasedPaddingAmount));
                        if (f != null) {
                            float paddingForValue2 = getPaddingForValue(f);
                            if (increasedPaddingAmount > 0.0f) {
                                paddingForValue = NotificationUtils.interpolate(paddingForValue2, paddingForValue, increasedPaddingAmount);
                            } else if (f.floatValue() > 0.0f) {
                                paddingForValue = NotificationUtils.interpolate(paddingForValue, paddingForValue2, f.floatValue());
                            }
                        }
                        stackScrollAlgorithmState.paddingMap.put(expandableView, Float.valueOf(paddingForValue));
                    }
                } else if (expandableView != null) {
                    stackScrollAlgorithmState.paddingMap.put(expandableView, Float.valueOf(getPaddingForValue(stackScrollAlgorithmState.paddingMap.get(expandableView))));
                }
                if (expandableView2 instanceof ExpandableNotificationRow) {
                    ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView2;
                    List<ExpandableNotificationRow> notificationChildren = expandableNotificationRow.getNotificationChildren();
                    if (expandableNotificationRow.isSummaryWithChildren() && notificationChildren != null) {
                        for (ExpandableNotificationRow expandableNotificationRow2 : notificationChildren) {
                            if (expandableNotificationRow2.getVisibility() != 8) {
                                stackScrollState.getViewStateForView(expandableNotificationRow2).notGoneIndex = iUpdateNotGoneIndex;
                                iUpdateNotGoneIndex++;
                            }
                        }
                    }
                }
                expandableView = expandableView2;
            }
        }
        ExpandableNotificationRow expandingNotification = ambientState.getExpandingNotification();
        stackScrollAlgorithmState.indexOfExpandingNotification = expandingNotification != null ? expandingNotification.isChildInGroup() ? stackScrollAlgorithmState.visibleChildren.indexOf(expandingNotification.getNotificationParent()) : stackScrollAlgorithmState.visibleChildren.indexOf(expandingNotification) : -1;
    }

    private float getPaddingForValue(Float f) {
        if (f == null) {
            return this.mPaddingBetweenElements;
        }
        if (f.floatValue() >= 0.0f) {
            return NotificationUtils.interpolate(this.mPaddingBetweenElements, this.mIncreasedPaddingBetweenElements, f.floatValue());
        }
        return NotificationUtils.interpolate(0.0f, this.mPaddingBetweenElements, 1.0f + f.floatValue());
    }

    private int updateNotGoneIndex(StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, int i, ExpandableView expandableView) {
        stackScrollState.getViewStateForView(expandableView).notGoneIndex = i;
        stackScrollAlgorithmState.visibleChildren.add(expandableView);
        return i + 1;
    }

    private void updatePositionsForState(StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, AmbientState ambientState) {
        float f = -stackScrollAlgorithmState.scrollY;
        int size = stackScrollAlgorithmState.visibleChildren.size();
        float fUpdateChild = f;
        for (int i = 0; i < size; i++) {
            fUpdateChild = updateChild(i, stackScrollState, stackScrollAlgorithmState, ambientState, fUpdateChild);
        }
    }

    protected float updateChild(int i, StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, AmbientState ambientState, float f) {
        ExpandableView expandableView = stackScrollAlgorithmState.visibleChildren.get(i);
        ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(expandableView);
        viewStateForView.location = 0;
        int paddingAfterChild = getPaddingAfterChild(stackScrollAlgorithmState, expandableView);
        int maxAllowedChildHeight = getMaxAllowedChildHeight(expandableView);
        viewStateForView.yTranslation = f;
        boolean z = expandableView instanceof FooterView;
        boolean z2 = expandableView instanceof EmptyShadeView;
        viewStateForView.location = 4;
        float topPadding = ambientState.getTopPadding() + ambientState.getStackTranslation();
        if (i <= stackScrollAlgorithmState.getIndexOfExpandingNotification()) {
            topPadding += ambientState.getExpandAnimationTopChange();
        }
        if (expandableView.mustStayOnScreen() && viewStateForView.yTranslation >= 0.0f) {
            viewStateForView.headsUpIsVisible = (viewStateForView.yTranslation + ((float) viewStateForView.height)) + topPadding < ambientState.getMaxHeadsUpTranslation();
        }
        if (z) {
            viewStateForView.yTranslation = Math.min(viewStateForView.yTranslation, ambientState.getInnerHeight() - maxAllowedChildHeight);
        } else if (z2) {
            viewStateForView.yTranslation = (ambientState.getInnerHeight() - maxAllowedChildHeight) + (ambientState.getStackTranslation() * 0.25f);
        } else {
            clampPositionToShelf(expandableView, viewStateForView, ambientState);
        }
        float f2 = viewStateForView.yTranslation + maxAllowedChildHeight + paddingAfterChild;
        if (f2 <= 0.0f) {
            viewStateForView.location = 2;
        }
        if (viewStateForView.location == 0) {
            Log.wtf("StackScrollAlgorithm", "Failed to assign location for child " + i);
        }
        viewStateForView.yTranslation += topPadding;
        return f2;
    }

    protected int getPaddingAfterChild(StackScrollAlgorithmState stackScrollAlgorithmState, ExpandableView expandableView) {
        return stackScrollAlgorithmState.getPaddingAfterChild(expandableView);
    }

    private void updateHeadsUpStates(StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, AmbientState ambientState) {
        int size = stackScrollAlgorithmState.visibleChildren.size();
        View view = null;
        for (int i = 0; i < size; i++) {
            ExpandableView expandableView = stackScrollAlgorithmState.visibleChildren.get(i);
            if (expandableView instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                if (expandableNotificationRow.isHeadsUp()) {
                    ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(expandableNotificationRow);
                    boolean z = true;
                    if (view == null && expandableNotificationRow.mustStayOnScreen() && !viewStateForView.headsUpIsVisible) {
                        viewStateForView.location = 1;
                        view = expandableNotificationRow;
                    }
                    if (view != expandableNotificationRow) {
                        z = false;
                    }
                    float f = viewStateForView.yTranslation + viewStateForView.height;
                    if (this.mIsExpanded && expandableNotificationRow.mustStayOnScreen() && !viewStateForView.headsUpIsVisible) {
                        clampHunToTop(ambientState, expandableNotificationRow, viewStateForView);
                        if (i == 0 && ambientState.isAboveShelf(expandableNotificationRow)) {
                            clampHunToMaxTranslation(ambientState, expandableNotificationRow, viewStateForView);
                            viewStateForView.hidden = false;
                        }
                    }
                    if (expandableNotificationRow.isPinned()) {
                        viewStateForView.yTranslation = Math.max(viewStateForView.yTranslation, this.mHeadsUpInset);
                        viewStateForView.height = Math.max(expandableNotificationRow.getIntrinsicHeight(), viewStateForView.height);
                        viewStateForView.hidden = false;
                        ExpandableViewState viewStateForView2 = stackScrollState.getViewStateForView(view);
                        if (viewStateForView2 != null && !z && (!this.mIsExpanded || f < viewStateForView2.yTranslation + viewStateForView2.height)) {
                            viewStateForView.height = expandableNotificationRow.getIntrinsicHeight();
                            viewStateForView.yTranslation = (viewStateForView2.yTranslation + viewStateForView2.height) - viewStateForView.height;
                        }
                    }
                    if (expandableNotificationRow.isHeadsUpAnimatingAway()) {
                        viewStateForView.hidden = false;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void clampHunToTop(AmbientState ambientState, ExpandableNotificationRow expandableNotificationRow, ExpandableViewState expandableViewState) {
        float fMax = Math.max(ambientState.getTopPadding() + ambientState.getStackTranslation(), expandableViewState.yTranslation);
        expandableViewState.height = (int) Math.max(expandableViewState.height - (fMax - expandableViewState.yTranslation), expandableNotificationRow.getCollapsedHeight());
        expandableViewState.yTranslation = fMax;
    }

    private void clampHunToMaxTranslation(AmbientState ambientState, ExpandableNotificationRow expandableNotificationRow, ExpandableViewState expandableViewState) {
        float fMin = Math.min(ambientState.getMaxHeadsUpTranslation(), ambientState.getInnerHeight() + ambientState.getTopPadding() + ambientState.getStackTranslation());
        float fMin2 = Math.min(expandableViewState.yTranslation, fMin - expandableNotificationRow.getCollapsedHeight());
        expandableViewState.height = (int) Math.min(expandableViewState.height, fMin - fMin2);
        expandableViewState.yTranslation = fMin2;
    }

    private void clampPositionToShelf(ExpandableView expandableView, ExpandableViewState expandableViewState, AmbientState ambientState) {
        if (ambientState.getShelf() == null) {
            return;
        }
        int innerHeight = ambientState.getInnerHeight() - ambientState.getShelf().getIntrinsicHeight();
        if (ambientState.isAppearing() && !expandableView.isAboveShelf()) {
            expandableViewState.yTranslation = Math.max(expandableViewState.yTranslation, innerHeight);
        }
        float f = innerHeight;
        expandableViewState.yTranslation = Math.min(expandableViewState.yTranslation, f);
        if (expandableViewState.yTranslation >= f) {
            expandableViewState.hidden = (expandableView.isExpandAnimationRunning() || expandableView.hasExpandingChild()) ? false : true;
            expandableViewState.inShelf = true;
            expandableViewState.headsUpIsVisible = false;
        }
    }

    protected int getMaxAllowedChildHeight(View view) {
        if (view instanceof ExpandableView) {
            return ((ExpandableView) view).getIntrinsicHeight();
        }
        return view == null ? this.mCollapsedSize : view.getHeight();
    }

    private void updateZValuesForState(StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, AmbientState ambientState) {
        float fUpdateChildZValue = 0.0f;
        for (int size = stackScrollAlgorithmState.visibleChildren.size() - 1; size >= 0; size--) {
            fUpdateChildZValue = updateChildZValue(size, fUpdateChildZValue, stackScrollState, stackScrollAlgorithmState, ambientState);
        }
    }

    protected float updateChildZValue(int i, float f, StackScrollState stackScrollState, StackScrollAlgorithmState stackScrollAlgorithmState, AmbientState ambientState) {
        ExpandableView expandableView = stackScrollAlgorithmState.visibleChildren.get(i);
        ExpandableViewState viewStateForView = stackScrollState.getViewStateForView(expandableView);
        int zDistanceBetweenElements = ambientState.getZDistanceBetweenElements();
        float baseZHeight = ambientState.getBaseZHeight();
        if (expandableView.mustStayOnScreen() && !viewStateForView.headsUpIsVisible && !ambientState.isDozingAndNotPulsing(expandableView) && viewStateForView.yTranslation < ambientState.getTopPadding() + ambientState.getStackTranslation()) {
            if (f != 0.0f) {
                f += 1.0f;
            } else {
                f += Math.min(1.0f, ((ambientState.getTopPadding() + ambientState.getStackTranslation()) - viewStateForView.yTranslation) / viewStateForView.height);
            }
            viewStateForView.zTranslation = baseZHeight + (zDistanceBetweenElements * f);
        } else if (i == 0 && ambientState.isAboveShelf(expandableView)) {
            int intrinsicHeight = ambientState.getShelf() == null ? 0 : ambientState.getShelf().getIntrinsicHeight();
            float innerHeight = (ambientState.getInnerHeight() - intrinsicHeight) + ambientState.getTopPadding() + ambientState.getStackTranslation();
            float pinnedHeadsUpHeight = viewStateForView.yTranslation + expandableView.getPinnedHeadsUpHeight() + this.mPaddingBetweenElements;
            if (innerHeight > pinnedHeadsUpHeight) {
                viewStateForView.zTranslation = baseZHeight;
            } else {
                viewStateForView.zTranslation = baseZHeight + (Math.min((pinnedHeadsUpHeight - innerHeight) / intrinsicHeight, 1.0f) * zDistanceBetweenElements);
            }
        } else {
            viewStateForView.zTranslation = baseZHeight;
        }
        viewStateForView.zTranslation += (1.0f - expandableView.getHeaderVisibleAmount()) * this.mPinnedZTranslationExtra;
        return f;
    }

    public void setIsExpanded(boolean z) {
        this.mIsExpanded = z;
    }

    public class StackScrollAlgorithmState {
        private int indexOfExpandingNotification;
        public int scrollY;
        public final ArrayList<ExpandableView> visibleChildren = new ArrayList<>();
        public final HashMap<ExpandableView, Float> paddingMap = new HashMap<>();

        public StackScrollAlgorithmState() {
        }

        public int getPaddingAfterChild(ExpandableView expandableView) {
            Float f = this.paddingMap.get(expandableView);
            if (f == null) {
                return StackScrollAlgorithm.this.mPaddingBetweenElements;
            }
            return (int) f.floatValue();
        }

        public int getIndexOfExpandingNotification() {
            return this.indexOfExpandingNotification;
        }
    }
}
