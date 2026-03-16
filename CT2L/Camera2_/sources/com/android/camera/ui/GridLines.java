package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera2.R;

public class GridLines extends View implements PreviewStatusListener.PreviewAreaChangedListener {
    private RectF mDrawBounds;
    Paint mPaint;

    public GridLines(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint = new Paint();
        this.mPaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.grid_line_width));
        this.mPaint.setColor(getResources().getColor(R.color.grid_line));
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mDrawBounds != null) {
            float thirdWidth = this.mDrawBounds.width() / 3.0f;
            float thirdHeight = this.mDrawBounds.height() / 3.0f;
            for (int i = 1; i < 3; i++) {
                float x = thirdWidth * i;
                canvas.drawLine(this.mDrawBounds.left + x, this.mDrawBounds.top, this.mDrawBounds.left + x, this.mDrawBounds.bottom, this.mPaint);
                float y = thirdHeight * i;
                canvas.drawLine(this.mDrawBounds.left, this.mDrawBounds.top + y, this.mDrawBounds.right, this.mDrawBounds.top + y, this.mPaint);
            }
        }
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        setDrawBounds(previewArea);
    }

    private void setDrawBounds(RectF previewArea) {
        this.mDrawBounds = new RectF(previewArea);
        invalidate();
    }
}
