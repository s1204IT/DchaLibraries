package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewGroup;
/* loaded from: classes.dex */
public class FooterViewButton extends AlphaOptimizedButton {
    public FooterViewButton(Context context) {
        this(context, null);
    }

    public FooterViewButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public FooterViewButton(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public FooterViewButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    @Override // android.view.View
    public void getDrawingRect(Rect rect) {
        super.getDrawingRect(rect);
        float translationX = ((ViewGroup) this.mParent).getTranslationX();
        float translationY = ((ViewGroup) this.mParent).getTranslationY();
        rect.left = (int) (rect.left + translationX);
        rect.right = (int) (rect.right + translationX);
        rect.top = (int) (rect.top + translationY);
        rect.bottom = (int) (rect.bottom + translationY);
    }
}
