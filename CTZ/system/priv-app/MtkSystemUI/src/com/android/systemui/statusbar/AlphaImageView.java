package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/* loaded from: classes.dex */
public class AlphaImageView extends ImageView {
    public AlphaImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.widget.ImageView, android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
