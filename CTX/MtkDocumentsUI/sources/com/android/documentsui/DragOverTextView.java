package com.android.documentsui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public final class DragOverTextView extends TextView {
    private static final int[] STATE_HIGHLIGHTED = {R.attr.state_highlighted};
    private boolean mHighlighted;

    public DragOverTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHighlighted = false;
    }

    @Override
    protected int[] onCreateDrawableState(int i) {
        int[] iArrOnCreateDrawableState = super.onCreateDrawableState(i + 1);
        if (this.mHighlighted) {
            mergeDrawableStates(iArrOnCreateDrawableState, STATE_HIGHLIGHTED);
        }
        return iArrOnCreateDrawableState;
    }

    public void setHighlight(boolean z) {
        this.mHighlighted = z;
        refreshDrawableState();
    }
}
