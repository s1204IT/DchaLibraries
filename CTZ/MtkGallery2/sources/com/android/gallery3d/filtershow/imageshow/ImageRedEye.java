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
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        if (motionEvent.getPointerCount() > 1 || didFinishScalingOperation()) {
            return true;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if (motionEvent.getAction() == 0) {
            this.mCurrentRect = new RectF();
            this.mCurrentRect.left = x - mTouchPadding;
            this.mCurrentRect.top = y - mTouchPadding;
        }
        if (motionEvent.getAction() == 2) {
            this.mCurrentRect.right = x + mTouchPadding;
            this.mCurrentRect.bottom = y + mTouchPadding;
        }
        if (motionEvent.getAction() == 1) {
            if (this.mCurrentRect != null) {
                Matrix imageToScreenMatrix = getImageToScreenMatrix(false);
                Matrix imageToScreenMatrix2 = getImageToScreenMatrix(true);
                Matrix matrix = new Matrix();
                imageToScreenMatrix2.invert(matrix);
                RectF rectF = new RectF(this.mCurrentRect);
                matrix.mapRect(rectF);
                RectF rectF2 = new RectF(this.mCurrentRect);
                matrix.reset();
                imageToScreenMatrix.invert(matrix);
                matrix.mapRect(rectF2);
                this.mRedEyeRep.addRect(rectF, rectF2);
                resetImageCaches(this);
            }
            this.mCurrentRect = null;
        }
        this.mEditorRedEye.commitLocalRepresentation();
        invalidate();
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
            canvas.drawRect(new RectF(this.mCurrentRect), paint);
        }
    }

    @Override
    protected void drawPoint(FilterPoint filterPoint, Canvas canvas, Matrix matrix, Matrix matrix2, Paint paint) {
        RectF rect = ((RedEyeCandidate) filterPoint).getRect();
        RectF rectF = new RectF();
        matrix.mapRect(rectF, rect);
        RectF rectF2 = new RectF();
        matrix2.mapRect(rectF2, rect);
        paint.setColor(-16776961);
        canvas.drawRect(rectF2, paint);
        canvas.drawLine(rectF2.centerX(), rectF2.top, rectF2.centerX(), rectF2.bottom, paint);
        canvas.drawLine(rectF2.left, rectF2.centerY(), rectF2.right, rectF2.centerY(), paint);
        paint.setColor(-16711936);
        float fWidth = rectF.width();
        float fHeight = rectF.height();
        float fCenterX = rectF2.centerX() - (fWidth / 2.0f);
        float fCenterY = rectF2.centerY() - (fHeight / 2.0f);
        rectF.set(fCenterX, fCenterY, fWidth + fCenterX, fHeight + fCenterY);
        canvas.drawRect(rectF, paint);
        canvas.drawLine(rectF.centerX(), rectF.top, rectF.centerX(), rectF.bottom, paint);
        canvas.drawLine(rectF.left, rectF.centerY(), rectF.right, rectF.centerY(), paint);
        canvas.drawCircle(rectF.centerX(), rectF.centerY(), mTouchPadding, paint);
    }
}
