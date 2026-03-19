package com.android.gallery3d.app;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

class SecondaryProgressExtImpl {
    private int mBufferPercent;
    private Rect mSecondaryBar = new Rect();
    private Paint mSecondaryPaint = new Paint();

    public SecondaryProgressExtImpl() {
        this.mSecondaryPaint.setColor(-10706747);
    }

    public void draw(Canvas canvas, Rect rect) {
        if (this.mBufferPercent >= 0) {
            this.mSecondaryBar.set(rect);
            this.mSecondaryBar.right = this.mSecondaryBar.left + ((this.mBufferPercent * rect.width()) / 100);
            canvas.drawRect(this.mSecondaryBar, this.mSecondaryPaint);
        }
        com.mediatek.gallery3d.util.Log.v("VP_SecondaryProgress", "draw() bufferPercent=" + this.mBufferPercent + ", secondaryBar=" + this.mSecondaryBar);
    }

    public void setSecondaryProgress(Rect rect, int i) {
        this.mBufferPercent = i;
    }
}
