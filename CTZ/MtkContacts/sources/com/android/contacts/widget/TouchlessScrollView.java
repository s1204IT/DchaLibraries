package com.android.contacts.widget;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class TouchlessScrollView extends ScrollView {
    public TouchlessScrollView(Context context) {
        this(context, null);
    }

    public TouchlessScrollView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TouchlessScrollView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        int scrollY = getScrollY();
        setScrollY(0);
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        setScrollY(scrollY);
        return parcelableOnSaveInstanceState;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return false;
    }
}
