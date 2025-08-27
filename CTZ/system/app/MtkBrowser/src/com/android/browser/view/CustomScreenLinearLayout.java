package com.android.browser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/* loaded from: classes.dex */
public class CustomScreenLinearLayout extends LinearLayout {
    public CustomScreenLinearLayout(Context context) {
        super(context);
        setChildrenDrawingOrderEnabled(true);
    }

    public CustomScreenLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setChildrenDrawingOrderEnabled(true);
    }

    public CustomScreenLinearLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setChildrenDrawingOrderEnabled(true);
    }

    @Override // android.view.ViewGroup
    protected int getChildDrawingOrder(int i, int i2) {
        return (i - i2) - 1;
    }
}
