package com.android.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/* loaded from: classes.dex */
public class TouchBlockingFrameLayout extends FrameLayout {
    public TouchBlockingFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        return false;
    }
}
