package com.android.systemui.statusbar.stack;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import java.util.List;
import java.util.WeakHashMap;

public class StackScrollState {
    private final int mClearAllTopPadding;
    private final ViewGroup mHostView;
    private WeakHashMap<ExpandableView, StackViewState> mStateMap = new WeakHashMap<>();

    public StackScrollState(ViewGroup hostView) {
        this.mHostView = hostView;
        this.mClearAllTopPadding = hostView.getContext().getResources().getDimensionPixelSize(R.dimen.clear_all_padding_top);
    }

    public ViewGroup getHostView() {
        return this.mHostView;
    }

    public void resetViewStates() {
        int numChildren = this.mHostView.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            ExpandableView child = (ExpandableView) this.mHostView.getChildAt(i);
            resetViewState(child);
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                List<ExpandableNotificationRow> children = row.getNotificationChildren();
                if (row.isSummaryWithChildren() && children != null) {
                    for (ExpandableNotificationRow childRow : children) {
                        resetViewState(childRow);
                    }
                }
            }
        }
    }

    private void resetViewState(ExpandableView view) {
        StackViewState viewState = this.mStateMap.get(view);
        if (viewState == null) {
            viewState = new StackViewState();
            this.mStateMap.put(view, viewState);
        }
        viewState.height = view.getIntrinsicHeight();
        viewState.gone = view.getVisibility() == 8;
        viewState.alpha = 1.0f;
        viewState.shadowAlpha = 1.0f;
        viewState.notGoneIndex = -1;
        viewState.hidden = false;
    }

    public StackViewState getViewStateForView(View requestedView) {
        return this.mStateMap.get(requestedView);
    }

    public void removeViewStateForView(View child) {
        this.mStateMap.remove(child);
    }

    public void apply() {
        int numChildren = this.mHostView.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            ExpandableView child = (ExpandableView) this.mHostView.getChildAt(i);
            StackViewState state = this.mStateMap.get(child);
            if (applyState(child, state)) {
                if (child instanceof DismissView) {
                    DismissView dismissView = (DismissView) child;
                    boolean visible = state.clipTopAmount < this.mClearAllTopPadding;
                    dismissView.performVisibilityAnimation(visible && !dismissView.willBeGone());
                } else if (child instanceof EmptyShadeView) {
                    EmptyShadeView emptyShadeView = (EmptyShadeView) child;
                    boolean visible2 = state.clipTopAmount <= 0;
                    emptyShadeView.performVisibilityAnimation(visible2 && !emptyShadeView.willBeGone());
                }
            }
        }
    }

    public boolean applyState(ExpandableView view, StackViewState state) {
        if (state == null) {
            Log.wtf("StackScrollStateNoSuchChild", "No child state was found when applying this state to the hostView");
            return false;
        }
        if (state.gone) {
            return false;
        }
        applyViewState(view, state);
        int height = view.getActualHeight();
        int newHeight = state.height;
        if (height != newHeight) {
            view.setActualHeight(newHeight, false);
        }
        float shadowAlpha = view.getShadowAlpha();
        float newShadowAlpha = state.shadowAlpha;
        if (shadowAlpha != newShadowAlpha) {
            view.setShadowAlpha(newShadowAlpha);
        }
        view.setDimmed(state.dimmed, false);
        view.setHideSensitive(state.hideSensitive, false, 0L, 0L);
        view.setBelowSpeedBump(state.belowSpeedBump);
        view.setDark(state.dark, false, 0L);
        float oldClipTopAmount = view.getClipTopAmount();
        if (oldClipTopAmount != state.clipTopAmount) {
            view.setClipTopAmount(state.clipTopAmount);
        }
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) view;
            if (state.isBottomClipped) {
                row.setClipToActualHeight(true);
            }
            row.applyChildrenState(this);
            return true;
        }
        return true;
    }

    public void applyViewState(View view, ViewState state) {
        int newLayerType;
        float alpha = view.getAlpha();
        float yTranslation = view.getTranslationY();
        float xTranslation = view.getTranslationX();
        float zTranslation = view.getTranslationZ();
        float newAlpha = state.alpha;
        float newYTranslation = state.yTranslation;
        float newZTranslation = state.zTranslation;
        boolean z = newAlpha != 0.0f ? state.hidden : true;
        if (alpha != newAlpha && xTranslation == 0.0f) {
            boolean becomesFullyVisible = newAlpha == 1.0f;
            boolean zHasOverlappingRendering = (z || becomesFullyVisible) ? false : view.hasOverlappingRendering();
            int layerType = view.getLayerType();
            if (zHasOverlappingRendering) {
                newLayerType = 2;
            } else {
                newLayerType = 0;
            }
            if (layerType != newLayerType) {
                view.setLayerType(newLayerType, null);
            }
            view.setAlpha(newAlpha);
        }
        int oldVisibility = view.getVisibility();
        int newVisibility = z ? 4 : 0;
        if (newVisibility != oldVisibility && (!(view instanceof ExpandableView) || !((ExpandableView) view).willBeGone())) {
            view.setVisibility(newVisibility);
        }
        if (yTranslation != newYTranslation) {
            view.setTranslationY(newYTranslation);
        }
        if (zTranslation == newZTranslation) {
            return;
        }
        view.setTranslationZ(newZTranslation);
    }
}
