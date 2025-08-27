package com.android.browser.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

/* loaded from: classes.dex */
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

    void init() {
        setFocusable(true);
        super.setOnClickListener(this);
    }

    @Override // android.view.View
    public void setBackgroundDrawable(Drawable drawable) {
        super.setBackgroundDrawable(drawable);
    }

    @Override // android.view.View
    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.mClickListener = onClickListener;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateTransitionDrawable(isPressed());
    }

    void updateTransitionDrawable(boolean z) {
        Drawable current;
        int longPressTimeout = ViewConfiguration.getLongPressTimeout();
        Drawable background = getBackground();
        if (background != null && (background instanceof StateListDrawable) && (current = ((StateListDrawable) background).getCurrent()) != null && (current instanceof TransitionDrawable)) {
            if (z && isLongClickable()) {
                ((TransitionDrawable) current).startTransition(longPressTimeout);
            } else {
                ((TransitionDrawable) current).resetTransition();
            }
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        updateTransitionDrawable(false);
        if (this.mClickListener != null) {
            this.mClickListener.onClick(view);
        }
    }

    public void setIgnoreRequestLayout(boolean z) {
        this.mIgnoreRequestLayout = z;
    }

    @Override // android.widget.RelativeLayout, android.view.View, android.view.ViewParent
    public void requestLayout() {
        if (!this.mIgnoreRequestLayout) {
            super.requestLayout();
        }
    }
}
