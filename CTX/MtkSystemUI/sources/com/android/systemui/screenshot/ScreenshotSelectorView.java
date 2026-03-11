package com.android.systemui.screenshot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class ScreenshotSelectorView extends View {
    private final Paint mPaintBackground;
    private final Paint mPaintSelection;
    private Rect mSelectionRect;
    private Point mStartPoint;

    public ScreenshotSelectorView(Context context) {
        this(context, null);
    }

    public ScreenshotSelectorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaintBackground = new Paint(-16777216);
        this.mPaintBackground.setAlpha(160);
        this.mPaintSelection = new Paint(0);
        this.mPaintSelection.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void startSelection(int i, int i2) {
        this.mStartPoint = new Point(i, i2);
        this.mSelectionRect = new Rect(i, i2, i, i2);
    }

    public void updateSelection(int i, int i2) {
        if (this.mSelectionRect != null) {
            this.mSelectionRect.left = Math.min(this.mStartPoint.x, i);
            this.mSelectionRect.right = Math.max(this.mStartPoint.x, i);
            this.mSelectionRect.top = Math.min(this.mStartPoint.y, i2);
            this.mSelectionRect.bottom = Math.max(this.mStartPoint.y, i2);
            invalidate();
        }
    }

    public Rect getSelectionRect() {
        return this.mSelectionRect;
    }

    public void stopSelection() {
        this.mStartPoint = null;
        this.mSelectionRect = null;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(this.mLeft, this.mTop, this.mRight, this.mBottom, this.mPaintBackground);
        if (this.mSelectionRect != null) {
            canvas.drawRect(this.mSelectionRect, this.mPaintSelection);
        }
    }
}
