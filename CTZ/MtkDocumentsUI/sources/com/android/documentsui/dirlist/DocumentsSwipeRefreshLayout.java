package com.android.documentsui.dirlist;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DocumentsSwipeRefreshLayout extends SwipeRefreshLayout {
    private static final int[] COLOR_RES = {R.attr.colorAccent};
    private static int COLOR_ACCENT_INDEX = 0;

    public DocumentsSwipeRefreshLayout(Context context) {
        this(context, null);
    }

    public DocumentsSwipeRefreshLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(COLOR_RES);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(COLOR_ACCENT_INDEX, -1);
        typedArrayObtainStyledAttributes.recycle();
        setColorSchemeResources(resourceId);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return false;
    }
}
