package com.android.camera.tinyplanet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.concurrent.locks.Lock;

public class TinyPlanetPreview extends View {
    private Lock mLock;
    private Paint mPaint;
    private Bitmap mPreview;
    private PreviewSizeListener mPreviewSizeListener;
    private int mSize;

    public interface PreviewSizeListener {
        void onSizeChanged(int i);
    }

    public TinyPlanetPreview(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.mSize = 0;
    }

    public TinyPlanetPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint = new Paint();
        this.mSize = 0;
    }

    public TinyPlanetPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPaint = new Paint();
        this.mSize = 0;
    }

    public void setBitmap(Bitmap preview, Lock lock) {
        this.mPreview = preview;
        this.mLock = lock;
        invalidate();
    }

    public void setPreviewSizeChangeListener(PreviewSizeListener listener) {
        this.mPreviewSizeListener = listener;
        if (this.mSize > 0) {
            this.mPreviewSizeListener.onSizeChanged(this.mSize);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mLock != null && this.mLock.tryLock()) {
            try {
                if (this.mPreview != null && !this.mPreview.isRecycled()) {
                    canvas.drawBitmap(this.mPreview, 0.0f, 0.0f, this.mPaint);
                }
            } finally {
                this.mLock.unlock();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && this.mPreviewSizeListener != null) {
            int width = right - left;
            int height = bottom - top;
            int mSize = Math.min(width, height);
            if (mSize > 0 && this.mPreviewSizeListener != null) {
                this.mPreviewSizeListener.onSizeChanged(mSize);
            }
        }
    }
}
