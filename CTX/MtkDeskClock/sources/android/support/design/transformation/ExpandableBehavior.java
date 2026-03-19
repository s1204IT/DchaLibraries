package android.support.design.transformation;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.design.expandable.ExpandableWidget;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.util.List;

public abstract class ExpandableBehavior extends CoordinatorLayout.Behavior<View> {
    private static final int STATE_COLLAPSED = 2;
    private static final int STATE_EXPANDED = 1;
    private static final int STATE_UNINITIALIZED = 0;
    private int currentState;

    @Override
    public abstract boolean layoutDependsOn(CoordinatorLayout coordinatorLayout, View view, View view2);

    protected abstract boolean onExpandedStateChange(View view, View view2, boolean z, boolean z2);

    public ExpandableBehavior() {
        this.currentState = 0;
    }

    public ExpandableBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.currentState = 0;
    }

    @Override
    @CallSuper
    public boolean onLayoutChild(CoordinatorLayout parent, final View child, int layoutDirection) {
        final ExpandableWidget dep;
        if (!ViewCompat.isLaidOut(child) && (dep = findExpandableWidget(parent, child)) != null && didStateChange(dep.isExpanded())) {
            this.currentState = dep.isExpanded() ? 1 : 2;
            final int expectedState = this.currentState;
            child.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    child.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (ExpandableBehavior.this.currentState == expectedState) {
                        ExpandableBehavior.this.onExpandedStateChange((View) dep, child, dep.isExpanded(), false);
                    }
                    return false;
                }
            });
            return false;
        }
        return false;
    }

    @Override
    @CallSuper
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View view) {
        ExpandableWidget expandableWidget = (ExpandableWidget) view;
        boolean expanded = expandableWidget.isExpanded();
        if (didStateChange(expanded)) {
            this.currentState = expandableWidget.isExpanded() ? 1 : 2;
            return onExpandedStateChange((View) expandableWidget, child, expandableWidget.isExpanded(), true);
        }
        return false;
    }

    @Nullable
    protected ExpandableWidget findExpandableWidget(CoordinatorLayout parent, View child) {
        List<View> dependencies = parent.getDependencies(child);
        int size = dependencies.size();
        for (int i = 0; i < size; i++) {
            View view = dependencies.get(i);
            if (layoutDependsOn(parent, child, view)) {
                return (ExpandableWidget) view;
            }
        }
        return null;
    }

    private boolean didStateChange(boolean expanded) {
        return expanded ? this.currentState == 0 || this.currentState == 2 : this.currentState == 1;
    }

    public static <T extends ExpandableBehavior> T from(View view, Class<T> klass) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (!(behavior instanceof ExpandableBehavior)) {
            throw new IllegalArgumentException("The view is not associated with ExpandableBehavior");
        }
        return klass.cast(behavior);
    }
}
