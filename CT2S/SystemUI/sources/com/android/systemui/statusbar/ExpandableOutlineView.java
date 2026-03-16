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
    private final Rect mOutlineRect;

    public ExpandableOutlineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mOutlineRect = new Rect();
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (ExpandableOutlineView.this.mCustomOutline) {
                    outline.setRect(ExpandableOutlineView.this.mOutlineRect);
                } else {
                    outline.setRect(0, ExpandableOutlineView.this.mClipTopAmount, ExpandableOutlineView.this.getWidth(), Math.max(ExpandableOutlineView.this.getActualHeight(), ExpandableOutlineView.this.mClipTopAmount));
                }
            }
        });
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

    protected void setOutlineRect(RectF rect) {
        if (rect != null) {
            setOutlineRect(rect.left, rect.top, rect.right, rect.bottom);
        } else {
            this.mCustomOutline = false;
            invalidateOutline();
        }
    }

    protected void setOutlineRect(float left, float top, float right, float bottom) {
        this.mCustomOutline = true;
        this.mOutlineRect.set((int) left, (int) top, (int) right, (int) bottom);
        this.mOutlineRect.bottom = (int) Math.max(top, this.mOutlineRect.bottom);
        this.mOutlineRect.right = (int) Math.max(left, this.mOutlineRect.right);
        invalidateOutline();
    }
}
