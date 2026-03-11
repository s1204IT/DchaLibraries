package com.android.browser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class EventRedirectingFrameLayout extends FrameLayout {
    private int mTargetChild;

    public EventRedirectingFrameLayout(Context context) {
        super(context);
    }

    public EventRedirectingFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EventRedirectingFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View child = getChildAt(this.mTargetChild);
        if (child != null) {
            return child.dispatchTouchEvent(ev);
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        View child = getChildAt(this.mTargetChild);
        if (child != null) {
            return child.dispatchKeyEvent(event);
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        View child = getChildAt(this.mTargetChild);
        if (child != null) {
            return child.dispatchKeyEventPreIme(event);
        }
        return false;
    }
}
