package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

public abstract class ExpandableOutlineView extends ExpandableView {
    private boolean mCustomOutline;
    private float mOutlineAlpha;
    private final Rect mOutlineRect;
    ViewOutlineProvider mProvider;

    public ExpandableOutlineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mOutlineRect = new Rect();
        this.mOutlineAlpha = -1.0f;
        this.mProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int translation = (int) ExpandableOutlineView.this.getTranslation();
                if (!ExpandableOutlineView.this.mCustomOutline) {
                    outline.setRect(translation, ExpandableOutlineView.this.mClipTopAmount, ExpandableOutlineView.this.getWidth() + translation, Math.max(ExpandableOutlineView.this.getActualHeight(), ExpandableOutlineView.this.mClipTopAmount));
                } else {
                    outline.setRect(ExpandableOutlineView.this.mOutlineRect);
                }
                outline.setAlpha(ExpandableOutlineView.this.mOutlineAlpha);
            }
        };
        setOutlineProvider(this.mProvider);
    }

    @Override
    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        super.setActualHeight(actualHeight, notifyListeners);
        invalidateOutline();
    }

    @Override
    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        invalidateOutline();
    }

    protected void setOutlineAlpha(float alpha) {
        if (alpha == this.mOutlineAlpha) {
            return;
        }
        this.mOutlineAlpha = alpha;
        invalidateOutline();
    }

    @Override
    public float getOutlineAlpha() {
        return this.mOutlineAlpha;
    }

    protected void setOutlineRect(RectF rect) {
        if (rect != null) {
            setOutlineRect(rect.left, rect.top, rect.right, rect.bottom);
            return;
        }
        this.mCustomOutline = false;
        setClipToOutline(false);
        invalidateOutline();
    }

    @Override
    public int getOutlineTranslation() {
        return this.mCustomOutline ? this.mOutlineRect.left : (int) getTranslation();
    }

    public void updateOutline() {
        if (this.mCustomOutline) {
            return;
        }
        boolean hasOutline = true;
        if (isChildInGroup()) {
            hasOutline = isGroupExpanded() && !isGroupExpansionChanging();
        } else if (isSummaryWithChildren()) {
            hasOutline = isGroupExpanded() ? isGroupExpansionChanging() : true;
        }
        setOutlineProvider(hasOutline ? this.mProvider : null);
    }

    protected void setOutlineRect(float left, float top, float right, float bottom) {
        this.mCustomOutline = true;
        setClipToOutline(true);
        this.mOutlineRect.set((int) left, (int) top, (int) right, (int) bottom);
        this.mOutlineRect.bottom = (int) Math.max(top, this.mOutlineRect.bottom);
        this.mOutlineRect.right = (int) Math.max(left, this.mOutlineRect.right);
        invalidateOutline();
    }
}
