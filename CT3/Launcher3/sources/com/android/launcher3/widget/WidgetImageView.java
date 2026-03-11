package com.android.launcher3.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class WidgetImageView extends View {
    private Bitmap mBitmap;
    private final RectF mDstRectF;
    private final Paint mPaint;

    public WidgetImageView(Context context) {
        super(context);
        this.mPaint = new Paint(3);
        this.mDstRectF = new RectF();
    }

    public WidgetImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint = new Paint(3);
        this.mDstRectF = new RectF();
    }

    public WidgetImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPaint = new Paint(3);
        this.mDstRectF = new RectF();
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
        invalidate();
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mBitmap == null) {
            return;
        }
        updateDstRectF();
        canvas.drawBitmap(this.mBitmap, (Rect) null, this.mDstRectF, this.mPaint);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void updateDstRectF() {
        if (this.mBitmap.getWidth() > getWidth()) {
            float scale = getWidth() / this.mBitmap.getWidth();
            this.mDstRectF.set(0.0f, 0.0f, getWidth(), this.mBitmap.getHeight() * scale);
        } else {
            this.mDstRectF.set((getWidth() - this.mBitmap.getWidth()) * 0.5f, 0.0f, (getWidth() + this.mBitmap.getWidth()) * 0.5f, this.mBitmap.getHeight());
        }
    }

    public Rect getBitmapBounds() {
        updateDstRectF();
        Rect rect = new Rect();
        this.mDstRectF.round(rect);
        return rect;
    }
}
