package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class AlphaOptimizedRelativeLayout extends RelativeLayout {
    public AlphaOptimizedRelativeLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
