package com.android.contacts.common.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;

public class LayoutSuppressingQuickContactBadge extends QuickContactBadge {
    public LayoutSuppressingQuickContactBadge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void requestLayout() {
        forceLayout();
    }
}
