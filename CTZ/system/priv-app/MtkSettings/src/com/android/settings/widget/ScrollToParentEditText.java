package com.android.settings.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
/* loaded from: classes.dex */
public class ScrollToParentEditText extends ImeAwareEditText {
    private Rect mRect;

    public ScrollToParentEditText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRect = new Rect();
    }

    @Override // android.view.View
    public boolean requestRectangleOnScreen(Rect rect, boolean z) {
        ViewParent parent = getParent();
        if (parent instanceof View) {
            View view = (View) parent;
            view.getDrawingRect(this.mRect);
            return view.requestRectangleOnScreen(this.mRect, z);
        }
        return super.requestRectangleOnScreen(rect, z);
    }
}
