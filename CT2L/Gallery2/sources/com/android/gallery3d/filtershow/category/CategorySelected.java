package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.android.gallery3d.R;

public class CategorySelected extends View {
    private int mMargin;
    private Paint mPaint;

    public CategorySelected(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint = new Paint();
        this.mMargin = 20;
        this.mMargin = getResources().getDimensionPixelSize(R.dimen.touch_circle_size);
    }

    @Override
    public void onDraw(Canvas canvas) {
        this.mPaint.reset();
        this.mPaint.setStrokeWidth(this.mMargin);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setColor(Color.argb(128, 128, 128, 128));
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, (getWidth() / 2) - this.mMargin, this.mPaint);
    }
}
