package com.android.settingslib.notification;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ZenRadioLayout extends LinearLayout {
    public ZenRadioLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        ViewGroup viewGroup = (ViewGroup) getChildAt(0);
        ViewGroup viewGroup2 = (ViewGroup) getChildAt(1);
        int childCount = viewGroup.getChildCount();
        if (childCount != viewGroup2.getChildCount()) {
            throw new IllegalStateException("Expected matching children");
        }
        View viewFindLastClickable = null;
        boolean z = false;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = viewGroup.getChildAt(i3);
            View childAt2 = viewGroup2.getChildAt(i3);
            if (viewFindLastClickable != null) {
                childAt.setAccessibilityTraversalAfter(viewFindLastClickable.getId());
            }
            View viewFindFirstClickable = findFirstClickable(childAt2);
            if (viewFindFirstClickable != null) {
                viewFindFirstClickable.setAccessibilityTraversalAfter(childAt.getId());
            }
            viewFindLastClickable = findLastClickable(childAt2);
            if (childAt.getLayoutParams().height != childAt2.getMeasuredHeight()) {
                childAt.getLayoutParams().height = childAt2.getMeasuredHeight();
                z = true;
            }
        }
        if (z) {
            super.onMeasure(i, i2);
        }
    }

    private View findFirstClickable(View view) {
        if (view.isClickable()) {
            return view;
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < view.getChildCount(); i++) {
                View viewFindFirstClickable = findFirstClickable(view.getChildAt(i));
                if (viewFindFirstClickable != null) {
                    return viewFindFirstClickable;
                }
            }
            return null;
        }
        return null;
    }

    private View findLastClickable(View view) {
        if (view.isClickable()) {
            return view;
        }
        if (view instanceof ViewGroup) {
            for (int childCount = view.getChildCount() - 1; childCount >= 0; childCount--) {
                View viewFindLastClickable = findLastClickable(view.getChildAt(childCount));
                if (viewFindLastClickable != null) {
                    return viewFindLastClickable;
                }
            }
            return null;
        }
        return null;
    }
}
