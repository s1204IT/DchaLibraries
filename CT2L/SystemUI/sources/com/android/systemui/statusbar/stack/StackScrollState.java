package com.android.systemui.statusbar.stack;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.SpeedBumpView;
import java.util.HashMap;
import java.util.Map;

public class StackScrollState {
    private final int mClearAllTopPadding;
    private final ViewGroup mHostView;
    private final Rect mClipRect = new Rect();
    private Map<ExpandableView, ViewState> mStateMap = new HashMap();

    public static class ViewState {
        float alpha;
        boolean belowSpeedBump;
        int clipTopAmount;
        boolean dark;
        boolean dimmed;
        boolean gone;
        int height;
        boolean hideSensitive;
        int location;
        int notGoneIndex;
        float scale;
        int topOverLap;
        float yTranslation;
        float zTranslation;
    }

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
            ViewState viewState = this.mStateMap.get(child);
            if (viewState == null) {
                viewState = new ViewState();
                this.mStateMap.put(child, viewState);
            }
            viewState.height = child.getIntrinsicHeight();
            viewState.gone = child.getVisibility() == 8;
            viewState.alpha = 1.0f;
            viewState.notGoneIndex = -1;
        }
    }

    public ViewState getViewStateForView(View requestedView) {
        return this.mStateMap.get(requestedView);
    }

    public void removeViewStateForView(View child) {
        this.mStateMap.remove(child);
    }

    public void apply() {
        int numChildren = this.mHostView.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            ExpandableView child = (ExpandableView) this.mHostView.getChildAt(i);
            ViewState state = this.mStateMap.get(child);
            if (state == null) {
                Log.wtf("StackScrollStateNoSuchChild", "No child state was found when applying this state to the hostView");
            } else if (!state.gone) {
                float alpha = child.getAlpha();
                float yTranslation = child.getTranslationY();
                float xTranslation = child.getTranslationX();
                float zTranslation = child.getTranslationZ();
                float scale = child.getScaleX();
                int height = child.getActualHeight();
                float newAlpha = state.alpha;
                float newYTranslation = state.yTranslation;
                float newZTranslation = state.zTranslation;
                float newScale = state.scale;
                int newHeight = state.height;
                boolean becomesInvisible = newAlpha == 0.0f;
                if (alpha != newAlpha && xTranslation == 0.0f) {
                    boolean becomesFullyVisible = newAlpha == 1.0f;
                    boolean newLayerTypeIsHardware = (becomesInvisible || becomesFullyVisible) ? false : true;
                    int layerType = child.getLayerType();
                    int newLayerType = newLayerTypeIsHardware ? 2 : 0;
                    if (layerType != newLayerType) {
                        child.setLayerType(newLayerType, null);
                    }
                    child.setAlpha(newAlpha);
                }
                int oldVisibility = child.getVisibility();
                int newVisibility = becomesInvisible ? 4 : 0;
                if (newVisibility != oldVisibility) {
                    child.setVisibility(newVisibility);
                }
                if (yTranslation != newYTranslation) {
                    child.setTranslationY(newYTranslation);
                }
                if (zTranslation != newZTranslation) {
                    child.setTranslationZ(newZTranslation);
                }
                if (scale != newScale) {
                    child.setScaleX(newScale);
                    child.setScaleY(newScale);
                }
                if (height != newHeight) {
                    child.setActualHeight(newHeight, false);
                }
                child.setDimmed(state.dimmed, false);
                child.setDark(state.dark, false, 0L);
                child.setHideSensitive(state.hideSensitive, false, 0L, 0L);
                child.setBelowSpeedBump(state.belowSpeedBump);
                float oldClipTopAmount = child.getClipTopAmount();
                if (oldClipTopAmount != state.clipTopAmount) {
                    child.setClipTopAmount(state.clipTopAmount);
                }
                updateChildClip(child, newHeight, state.topOverLap);
                if (child instanceof SpeedBumpView) {
                    performSpeedBumpAnimation(i, (SpeedBumpView) child, state, 0L);
                } else if (child instanceof DismissView) {
                    DismissView dismissView = (DismissView) child;
                    boolean visible = state.topOverLap < this.mClearAllTopPadding;
                    dismissView.performVisibilityAnimation(visible && !dismissView.willBeGone());
                } else if (child instanceof EmptyShadeView) {
                    EmptyShadeView emptyShadeView = (EmptyShadeView) child;
                    boolean visible2 = state.topOverLap <= 0;
                    emptyShadeView.performVisibilityAnimation(visible2 && !emptyShadeView.willBeGone());
                }
            }
        }
    }

    private void updateChildClip(View child, int height, int clipInset) {
        this.mClipRect.set(0, clipInset, child.getWidth(), height);
        child.setClipBounds(this.mClipRect);
    }

    public void performSpeedBumpAnimation(int i, SpeedBumpView speedBump, ViewState state, long delay) {
        View nextChild = getNextChildNotGone(i);
        if (nextChild != null) {
            float lineEnd = state.yTranslation + (state.height / 2);
            ViewState nextState = getViewStateForView(nextChild);
            boolean startIsAboveNext = nextState.yTranslation > lineEnd;
            speedBump.animateDivider(startIsAboveNext, delay, null);
        }
    }

    private View getNextChildNotGone(int childIndex) {
        int childCount = this.mHostView.getChildCount();
        for (int i = childIndex + 1; i < childCount; i++) {
            View child = this.mHostView.getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return null;
    }
}
