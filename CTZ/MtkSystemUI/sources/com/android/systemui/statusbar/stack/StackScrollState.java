package com.android.systemui.statusbar.stack;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

public class StackScrollState {
    private final ViewGroup mHostView;
    private WeakHashMap<ExpandableView, ExpandableViewState> mStateMap = new WeakHashMap<>();

    public StackScrollState(ViewGroup viewGroup) {
        this.mHostView = viewGroup;
    }

    public ViewGroup getHostView() {
        return this.mHostView;
    }

    public void resetViewStates() {
        int childCount = this.mHostView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) this.mHostView.getChildAt(i);
            resetViewState(expandableView);
            if (expandableView instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                List<ExpandableNotificationRow> notificationChildren = expandableNotificationRow.getNotificationChildren();
                if (expandableNotificationRow.isSummaryWithChildren() && notificationChildren != null) {
                    Iterator<ExpandableNotificationRow> it = notificationChildren.iterator();
                    while (it.hasNext()) {
                        resetViewState(it.next());
                    }
                }
            }
        }
    }

    private void resetViewState(ExpandableView expandableView) {
        ExpandableViewState expandableViewStateCreateNewViewState = this.mStateMap.get(expandableView);
        if (expandableViewStateCreateNewViewState == null) {
            expandableViewStateCreateNewViewState = expandableView.createNewViewState(this);
            this.mStateMap.put(expandableView, expandableViewStateCreateNewViewState);
        }
        expandableViewStateCreateNewViewState.height = expandableView.getIntrinsicHeight();
        expandableViewStateCreateNewViewState.gone = expandableView.getVisibility() == 8;
        expandableViewStateCreateNewViewState.alpha = 1.0f;
        expandableViewStateCreateNewViewState.shadowAlpha = 1.0f;
        expandableViewStateCreateNewViewState.notGoneIndex = -1;
        expandableViewStateCreateNewViewState.xTranslation = expandableView.getTranslationX();
        expandableViewStateCreateNewViewState.hidden = false;
        expandableViewStateCreateNewViewState.scaleX = expandableView.getScaleX();
        expandableViewStateCreateNewViewState.scaleY = expandableView.getScaleY();
        expandableViewStateCreateNewViewState.inShelf = false;
        expandableViewStateCreateNewViewState.headsUpIsVisible = false;
    }

    public ExpandableViewState getViewStateForView(View view) {
        return this.mStateMap.get(view);
    }

    public void removeViewStateForView(View view) {
        this.mStateMap.remove(view);
    }

    public void apply() {
        int childCount = this.mHostView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) this.mHostView.getChildAt(i);
            ExpandableViewState expandableViewState = this.mStateMap.get(expandableView);
            if (expandableViewState == null) {
                Log.wtf("StackScrollStateNoSuchChild", "No child state was found when applying this state to the hostView");
            } else if (!expandableViewState.gone) {
                expandableViewState.applyToView(expandableView);
            }
        }
    }
}
