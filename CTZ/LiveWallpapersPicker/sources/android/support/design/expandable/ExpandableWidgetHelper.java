package android.support.design.expandable;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;

public final class ExpandableWidgetHelper {
    private boolean expanded = false;
    private int expandedComponentIdHint = 0;
    private final View widget;

    public ExpandableWidgetHelper(ExpandableWidget expandableWidget) {
        this.widget = (View) expandableWidget;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putBoolean("expanded", this.expanded);
        state.putInt("expandedComponentIdHint", this.expandedComponentIdHint);
        return state;
    }

    public void onRestoreInstanceState(Bundle state) {
        this.expanded = state.getBoolean("expanded", false);
        this.expandedComponentIdHint = state.getInt("expandedComponentIdHint", 0);
        if (this.expanded) {
            dispatchExpandedStateChanged();
        }
    }

    public int getExpandedComponentIdHint() {
        return this.expandedComponentIdHint;
    }

    private void dispatchExpandedStateChanged() {
        ?? parent = this.widget.getParent();
        if (parent instanceof CoordinatorLayout) {
            parent.dispatchDependentViewsChanged(this.widget);
        }
    }
}
