package com.android.browser.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

public class BookmarkContainer extends RelativeLayout implements View.OnClickListener {
    private View.OnClickListener mClickListener;
    private boolean mIgnoreRequestLayout;

    public BookmarkContainer(Context context) {
        super(context);
        this.mIgnoreRequestLayout = false;
        init();
    }

    public BookmarkContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIgnoreRequestLayout = false;
        init();
    }

    public BookmarkContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mIgnoreRequestLayout = false;
        init();
    }

    void init() {
        setFocusable(true);
        super.setOnClickListener(this);
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        super.setBackgroundDrawable(d);
    }

    @Override
    public void setOnClickListener(View.OnClickListener l) {
        this.mClickListener = l;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateTransitionDrawable(isPressed());
    }

    void updateTransitionDrawable(boolean pressed) {
        Drawable d;
        int longPressTimeout = ViewConfiguration.getLongPressTimeout();
        Drawable selector = getBackground();
        if (selector == null || !(selector instanceof StateListDrawable) || (d = ((StateListDrawable) selector).getCurrent()) == null || !(d instanceof TransitionDrawable)) {
            return;
        }
        if (pressed && isLongClickable()) {
            ((TransitionDrawable) d).startTransition(longPressTimeout);
        } else {
            ((TransitionDrawable) d).resetTransition();
        }
    }

    @Override
    public void onClick(View view) {
        updateTransitionDrawable(false);
        if (this.mClickListener == null) {
            return;
        }
        this.mClickListener.onClick(view);
    }

    public void setIgnoreRequestLayout(boolean ignore) {
        this.mIgnoreRequestLayout = ignore;
    }

    @Override
    public void requestLayout() {
        if (this.mIgnoreRequestLayout) {
            return;
        }
        super.requestLayout();
    }
}
