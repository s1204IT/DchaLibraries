package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
/* loaded from: a.zip:com/android/systemui/statusbar/AlphaOptimizedButton.class */
public class AlphaOptimizedButton extends Button {
    public AlphaOptimizedButton(Context context) {
        super(context);
    }

    public AlphaOptimizedButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public AlphaOptimizedButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public AlphaOptimizedButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override // android.widget.TextView, android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
