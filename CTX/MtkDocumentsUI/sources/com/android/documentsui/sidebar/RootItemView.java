package com.android.documentsui.sidebar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.android.documentsui.R;

public final class RootItemView extends LinearLayout {
    private static final int[] STATE_HIGHLIGHTED = {R.attr.state_highlighted};
    private boolean mHighlighted;

    public RootItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHighlighted = false;
    }

    @Override
    public int[] onCreateDrawableState(int i) {
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

    public void drawRipple() {
        setPressed(true);
        setPressed(false);
    }
}
