package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/* loaded from: classes.dex */
public class AlphaOptimizedTextView extends TextView {
    public AlphaOptimizedTextView(Context context) {
        super(context);
    }

    public AlphaOptimizedTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public AlphaOptimizedTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public AlphaOptimizedTextView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override // android.widget.TextView, android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
