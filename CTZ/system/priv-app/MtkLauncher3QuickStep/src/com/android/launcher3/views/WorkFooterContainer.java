package com.android.launcher3.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

/* loaded from: classes.dex */
public class WorkFooterContainer extends RelativeLayout {
    public WorkFooterContainer(Context context) {
        super(context);
    }

    public WorkFooterContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public WorkFooterContainer(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.widget.RelativeLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        updateTranslation();
    }

    @Override // android.view.View
    public void offsetTopAndBottom(int i) {
        super.offsetTopAndBottom(i);
        updateTranslation();
    }

    private void updateTranslation() {
        if (getParent() instanceof View) {
            View view = (View) getParent();
            setTranslationY(Math.max(0, (view.getHeight() - view.getPaddingBottom()) - getBottom()));
        }
    }
}
