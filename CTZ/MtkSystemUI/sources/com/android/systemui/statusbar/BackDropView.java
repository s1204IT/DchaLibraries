package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class BackDropView extends FrameLayout {
    private Runnable mOnVisibilityChangedRunnable;

    public BackDropView(Context context) {
        super(context);
    }

    public BackDropView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public BackDropView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public BackDropView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (view == this && this.mOnVisibilityChangedRunnable != null) {
            this.mOnVisibilityChangedRunnable.run();
        }
    }

    public void setOnVisibilityChangedRunnable(Runnable runnable) {
        this.mOnVisibilityChangedRunnable = runnable;
    }
}
