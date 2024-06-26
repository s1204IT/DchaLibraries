package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
/* loaded from: classes.dex */
public class AlphaOptimizedImageView extends ImageView {
    public AlphaOptimizedImageView(Context context) {
        this(context, null);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AlphaOptimizedImageView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override // android.widget.ImageView, android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
