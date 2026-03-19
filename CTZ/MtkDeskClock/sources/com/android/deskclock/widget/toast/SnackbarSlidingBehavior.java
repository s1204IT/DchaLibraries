package com.android.deskclock.widget.toast;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;
import java.util.Iterator;

@Keep
public final class SnackbarSlidingBehavior extends CoordinatorLayout.Behavior<View> {
    public SnackbarSlidingBehavior(Context context, AttributeSet attributeSet) {
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout coordinatorLayout, View view, View view2) {
        return view2 instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout coordinatorLayout, View view, View view2) {
        updateTranslationY(coordinatorLayout, view);
        return false;
    }

    @Override
    public void onDependentViewRemoved(CoordinatorLayout coordinatorLayout, View view, View view2) {
        updateTranslationY(coordinatorLayout, view);
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout coordinatorLayout, View view, int i) {
        updateTranslationY(coordinatorLayout, view);
        return false;
    }

    private void updateTranslationY(CoordinatorLayout coordinatorLayout, View view) {
        Iterator<View> it = coordinatorLayout.getDependencies(view).iterator();
        float fMin = 0.0f;
        while (it.hasNext()) {
            fMin = Math.min(fMin, it.next().getY() - view.getBottom());
        }
        view.setTranslationY(fMin);
    }
}
