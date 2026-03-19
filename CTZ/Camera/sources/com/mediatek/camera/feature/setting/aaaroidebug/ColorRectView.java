package com.mediatek.camera.feature.setting.aaaroidebug;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogUtil;

public class ColorRectView extends View {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ColorRectView.class.getSimpleName());
    private int mColor;
    private Paint mPointPaint;
    private Paint mRectPaint;
    private Rect[] mRects;

    public ColorRectView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mColor = context.obtainStyledAttributes(attributeSet, R.styleable.colorRectView).getColor(0, -65536);
        this.mRectPaint = new Paint();
        this.mRectPaint.setColor(this.mColor);
        this.mRectPaint.setStrokeWidth(r4.getColor(1, 5));
        this.mRectPaint.setStyle(Paint.Style.STROKE);
        this.mPointPaint = new Paint();
        this.mPointPaint.setColor(this.mColor);
        this.mPointPaint.setStrokeWidth(r4.getColor(1, 5) * 2);
        this.mPointPaint.setStyle(Paint.Style.FILL);
    }

    public void setRects(Rect[] rectArr) {
        if (!isRectArrayEqual(rectArr, this.mRects)) {
            this.mRects = rectArr;
            invalidate();
        }
    }

    private static boolean isRectArrayEqual(Rect[] rectArr, Rect[] rectArr2) {
        if (rectArr == null && rectArr2 == null) {
            return true;
        }
        if (rectArr == null && rectArr2 != null) {
            return false;
        }
        if ((rectArr != null && rectArr2 == null) || rectArr.length != rectArr2.length) {
            return false;
        }
        for (int i = 0; i < rectArr.length; i++) {
            if ((rectArr[i] == null && rectArr2[i] != null) || !rectArr[i].equals(rectArr2[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mRects != null) {
            for (Rect rect : this.mRects) {
                canvas.drawRect(rect, this.mRectPaint);
                canvas.drawPoint(r3.centerX(), r3.centerY(), this.mPointPaint);
            }
        }
        super.onDraw(canvas);
    }
}
