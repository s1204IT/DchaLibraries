package android.support.v17.leanback.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
/* loaded from: classes.dex */
class NonOverlappingFrameLayout extends FrameLayout {
    public NonOverlappingFrameLayout(Context context) {
        this(context, null);
    }

    public NonOverlappingFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public NonOverlappingFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
