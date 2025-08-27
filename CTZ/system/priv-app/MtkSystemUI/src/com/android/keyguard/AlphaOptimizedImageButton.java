package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

/* loaded from: classes.dex */
public class AlphaOptimizedImageButton extends ImageButton {
    public AlphaOptimizedImageButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.widget.ImageView, android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
