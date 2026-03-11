package com.android.systemui.recents.views;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewOutlineProvider;
import com.android.systemui.recents.misc.Utilities;

public class AnimateableViewBounds extends ViewOutlineProvider {

    @ViewDebug.ExportedProperty(category = "recents")
    int mCornerRadius;
    View mSourceView;

    @ViewDebug.ExportedProperty(category = "recents")
    Rect mClipRect = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    Rect mClipBounds = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    Rect mLastClipBounds = new Rect();

    @ViewDebug.ExportedProperty(category = "recents")
    float mAlpha = 1.0f;

    public AnimateableViewBounds(View source, int cornerRadius) {
        this.mSourceView = source;
        this.mCornerRadius = cornerRadius;
    }

    public void reset() {
        this.mClipRect.set(-1, -1, -1, -1);
        updateClipBounds();
    }

    @Override
    public void getOutline(View view, Outline outline) {
        outline.setAlpha(Utilities.mapRange(this.mAlpha, 0.1f, 0.8f));
        if (this.mCornerRadius > 0) {
            outline.setRoundRect(this.mClipRect.left, this.mClipRect.top, this.mSourceView.getWidth() - this.mClipRect.right, this.mSourceView.getHeight() - this.mClipRect.bottom, this.mCornerRadius);
        } else {
            outline.setRect(this.mClipRect.left, this.mClipRect.top, this.mSourceView.getWidth() - this.mClipRect.right, this.mSourceView.getHeight() - this.mClipRect.bottom);
        }
    }

    void setAlpha(float alpha) {
        if (Float.compare(alpha, this.mAlpha) == 0) {
            return;
        }
        this.mAlpha = alpha;
        this.mSourceView.invalidateOutline();
    }

    public float getAlpha() {
        return this.mAlpha;
    }

    public void setClipBottom(int bottom) {
        this.mClipRect.bottom = bottom;
        updateClipBounds();
    }

    private void updateClipBounds() {
        this.mClipBounds.set(Math.max(0, this.mClipRect.left), Math.max(0, this.mClipRect.top), this.mSourceView.getWidth() - Math.max(0, this.mClipRect.right), this.mSourceView.getHeight() - Math.max(0, this.mClipRect.bottom));
        if (this.mLastClipBounds.equals(this.mClipBounds)) {
            return;
        }
        this.mSourceView.setClipBounds(this.mClipBounds);
        this.mSourceView.invalidateOutline();
        this.mLastClipBounds.set(this.mClipBounds);
    }
}
