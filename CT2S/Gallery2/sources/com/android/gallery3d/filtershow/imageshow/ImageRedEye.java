package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import com.android.gallery3d.filtershow.filters.FilterPoint;
import com.android.gallery3d.filtershow.filters.RedEyeCandidate;

public class ImageRedEye extends ImagePoint {
    private RectF mCurrentRect;

    public ImageRedEye(Context context) {
        super(context);
        this.mCurrentRect = null;
    }

    @Override
    public void resetParameter() {
        super.resetParameter();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (event.getPointerCount() <= 1 && !didFinishScalingOperation()) {
            float ex = event.getX();
            float ey = event.getY();
            if (event.getAction() == 0) {
                this.mCurrentRect = new RectF();
                this.mCurrentRect.left = ex - mTouchPadding;
                this.mCurrentRect.top = ey - mTouchPadding;
            }
            if (event.getAction() == 2) {
                this.mCurrentRect.right = mTouchPadding + ex;
                this.mCurrentRect.bottom = mTouchPadding + ey;
            }
            if (event.getAction() == 1) {
                if (this.mCurrentRect != null) {
                    Matrix originalNoRotateToScreen = getImageToScreenMatrix(false);
                    Matrix originalToScreen = getImageToScreenMatrix(true);
                    Matrix invert = new Matrix();
                    originalToScreen.invert(invert);
                    RectF r = new RectF(this.mCurrentRect);
                    invert.mapRect(r);
                    RectF r2 = new RectF(this.mCurrentRect);
                    invert.reset();
                    originalNoRotateToScreen.invert(invert);
                    invert.mapRect(r2);
                    this.mRedEyeRep.addRect(r, r2);
                    resetImageCaches(this);
                }
                this.mCurrentRect = null;
            }
            this.mEditorRedEye.commitLocalRepresentation();
            invalidate();
        }
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(-65536);
        paint.setStrokeWidth(2.0f);
        if (this.mCurrentRect != null) {
            paint.setColor(-65536);
            RectF drawRect = new RectF(this.mCurrentRect);
            canvas.drawRect(drawRect, paint);
        }
    }

    @Override
    protected void drawPoint(FilterPoint point, Canvas canvas, Matrix originalToScreen, Matrix originalRotateToScreen, Paint paint) {
        RedEyeCandidate candidate = (RedEyeCandidate) point;
        RectF rect = candidate.getRect();
        RectF drawRect = new RectF();
        originalToScreen.mapRect(drawRect, rect);
        RectF fullRect = new RectF();
        originalRotateToScreen.mapRect(fullRect, rect);
        paint.setColor(-16776961);
        canvas.drawRect(fullRect, paint);
        canvas.drawLine(fullRect.centerX(), fullRect.top, fullRect.centerX(), fullRect.bottom, paint);
        canvas.drawLine(fullRect.left, fullRect.centerY(), fullRect.right, fullRect.centerY(), paint);
        paint.setColor(-16711936);
        float dw = drawRect.width();
        float dh = drawRect.height();
        float dx = fullRect.centerX() - (dw / 2.0f);
        float dy = fullRect.centerY() - (dh / 2.0f);
        drawRect.set(dx, dy, dx + dw, dy + dh);
        canvas.drawRect(drawRect, paint);
        canvas.drawLine(drawRect.centerX(), drawRect.top, drawRect.centerX(), drawRect.bottom, paint);
        canvas.drawLine(drawRect.left, drawRect.centerY(), drawRect.right, drawRect.centerY(), paint);
        canvas.drawCircle(drawRect.centerX(), drawRect.centerY(), mTouchPadding, paint);
    }
}
