package com.android.server.policy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class RecentApplicationsBackground extends LinearLayout {
    private static final String TAG = "RecentApplicationsBackground";
    private Drawable mBackground;
    private boolean mBackgroundSizeChanged;
    private Rect mTmp0;
    private Rect mTmp1;

    public RecentApplicationsBackground(Context context) {
        this(context, null);
        init();
    }

    public RecentApplicationsBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTmp0 = new Rect();
        this.mTmp1 = new Rect();
        init();
    }

    private void init() {
        this.mBackground = getBackground();
        setBackgroundDrawable(null);
        setPadding(0, 0, 0, 0);
        setGravity(17);
    }

    protected boolean setFrame(int left, int top, int right, int bottom) {
        setWillNotDraw(false);
        if (this.mLeft != left || this.mRight != right || this.mTop != top || this.mBottom != bottom) {
            this.mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        if (who != this.mBackground) {
            return super.verifyDrawable(who);
        }
        return true;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mBackground != null) {
            this.mBackground.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        Drawable d = this.mBackground;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable background = this.mBackground;
        if (background != null && this.mBackgroundSizeChanged) {
            this.mBackgroundSizeChanged = false;
            Rect chld = this.mTmp0;
            Rect bkg = this.mTmp1;
            this.mBackground.getPadding(bkg);
            getChildBounds(chld);
            int top = chld.top - bkg.top;
            int bottom = chld.bottom + bkg.bottom;
            int right = getRight();
            background.setBounds(0, top, right, bottom);
        }
        this.mBackground.draw(canvas);
        canvas.drawARGB(191, 0, 0, 0);
        super.draw(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mBackground.setCallback(this);
        setWillNotDraw(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mBackground.setCallback(null);
    }

    private void getChildBounds(Rect r) {
        r.top = Integer.MAX_VALUE;
        r.left = Integer.MAX_VALUE;
        r.right = Integer.MIN_VALUE;
        r.bottom = Integer.MIN_VALUE;
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == 0) {
                r.left = Math.min(r.left, v.getLeft());
                r.top = Math.min(r.top, v.getTop());
                r.right = Math.max(r.right, v.getRight());
                r.bottom = Math.max(r.bottom, v.getBottom());
            }
        }
    }
}
