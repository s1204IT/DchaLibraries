package com.android.systemui.shared.recents.view;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.android.systemui.shared.recents.utilities.Utilities;

/* loaded from: classes.dex */
public class AnimateableViewBounds extends ViewOutlineProvider {
    private static final float MAX_ALPHA = 0.8f;
    private static final float MIN_ALPHA = 0.1f;
    protected int mCornerRadius;
    protected View mSourceView;
    protected Rect mClipRect = new Rect();
    protected Rect mClipBounds = new Rect();
    protected Rect mLastClipBounds = new Rect();
    protected float mAlpha = 1.0f;

    public AnimateableViewBounds(View source, int cornerRadius) {
        this.mSourceView = source;
        this.mCornerRadius = cornerRadius;
    }

    public void reset() {
        this.mClipRect.set(0, 0, 0, 0);
        updateClipBounds();
    }

    @Override // android.view.ViewOutlineProvider
    public void getOutline(View view, Outline outline) {
        outline.setAlpha(Utilities.mapRange(this.mAlpha, 0.1f, MAX_ALPHA));
        if (this.mCornerRadius > 0) {
            outline.setRoundRect(this.mClipRect.left, this.mClipRect.top, this.mSourceView.getWidth() - this.mClipRect.right, this.mSourceView.getHeight() - this.mClipRect.bottom, this.mCornerRadius);
        } else {
            outline.setRect(this.mClipRect.left, this.mClipRect.top, this.mSourceView.getWidth() - this.mClipRect.right, this.mSourceView.getHeight() - this.mClipRect.bottom);
        }
    }

    public void setAlpha(float alpha) {
        if (Float.compare(alpha, this.mAlpha) != 0) {
            this.mAlpha = alpha;
            this.mSourceView.invalidateOutline();
        }
    }

    public float getAlpha() {
        return this.mAlpha;
    }

    public void setClipTop(int top) {
        this.mClipRect.top = top;
        updateClipBounds();
    }

    public int getClipTop() {
        return this.mClipRect.top;
    }

    public void setClipBottom(int bottom) {
        this.mClipRect.bottom = bottom;
        updateClipBounds();
    }

    public int getClipBottom() {
        return this.mClipRect.bottom;
    }

    public Rect getClipBounds() {
        return this.mClipBounds;
    }

    protected void updateClipBounds() {
        this.mClipBounds.set(Math.max(0, this.mClipRect.left), Math.max(0, this.mClipRect.top), this.mSourceView.getWidth() - Math.max(0, this.mClipRect.right), this.mSourceView.getHeight() - Math.max(0, this.mClipRect.bottom));
        if (!this.mLastClipBounds.equals(this.mClipBounds)) {
            this.mSourceView.setClipBounds(this.mClipBounds);
            this.mSourceView.invalidateOutline();
            this.mLastClipBounds.set(this.mClipBounds);
        }
    }
}
