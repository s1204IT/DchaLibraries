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

    public EventRedirectingFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public EventRedirectingFrameLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        View childAt = getChildAt(this.mTargetChild);
        if (childAt != null) {
            return childAt.dispatchKeyEvent(keyEvent);
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent keyEvent) {
        View childAt = getChildAt(this.mTargetChild);
        if (childAt != null) {
            return childAt.dispatchKeyEventPreIme(keyEvent);
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        View childAt = getChildAt(this.mTargetChild);
        if (childAt != null) {
            return childAt.dispatchTouchEvent(motionEvent);
        }
        return false;
    }
}
