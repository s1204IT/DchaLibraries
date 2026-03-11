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

    public BookmarkContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIgnoreRequestLayout = false;
        init();
    }

    public BookmarkContainer(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIgnoreRequestLayout = false;
        init();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateTransitionDrawable(isPressed());
    }

    void init() {
        setFocusable(true);
        super.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        updateTransitionDrawable(false);
        if (this.mClickListener != null) {
            this.mClickListener.onClick(view);
        }
    }

    @Override
    public void requestLayout() {
        if (this.mIgnoreRequestLayout) {
            return;
        }
        super.requestLayout();
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        super.setBackgroundDrawable(drawable);
    }

    public void setIgnoreRequestLayout(boolean z) {
        this.mIgnoreRequestLayout = z;
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.mClickListener = onClickListener;
    }

    void updateTransitionDrawable(boolean z) {
        Drawable current;
        int longPressTimeout = ViewConfiguration.getLongPressTimeout();
        Drawable background = getBackground();
        if (background == null || !(background instanceof StateListDrawable) || (current = ((StateListDrawable) background).getCurrent()) == null || !(current instanceof TransitionDrawable)) {
            return;
        }
        if (z && isLongClickable()) {
            ((TransitionDrawable) current).startTransition(longPressTimeout);
        } else {
            ((TransitionDrawable) current).resetTransition();
        }
    }
}
