package com.android.gallery3d.app;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

class InfoExtImpl {
    private int mEllipseLength;
    private Paint mInfoPaint = new Paint(1);
    private String mInfoText;
    private String mVisibleText;

    public InfoExtImpl(float f) {
        this.mInfoPaint.setColor(-3223858);
        this.mInfoPaint.setTextSize(f);
        this.mInfoPaint.setTextAlign(Paint.Align.CENTER);
        this.mEllipseLength = (int) Math.ceil(this.mInfoPaint.measureText("..."));
    }

    public void draw(Canvas canvas, Rect rect) {
        if (this.mInfoText != null && this.mVisibleText != null) {
            canvas.drawText(this.mVisibleText, rect.centerX(), rect.centerY(), this.mInfoPaint);
        }
    }

    public void setInfo(String str) {
        this.mInfoText = str;
    }

    public void updateVisibleText(View view, Rect rect, Rect rect2) {
        if (this.mInfoText == null) {
            this.mVisibleText = null;
            return;
        }
        float fMeasureText = this.mInfoPaint.measureText(this.mInfoText);
        float fWidth = ((rect.width() - (rect2.width() * 2)) - view.getPaddingLeft()) - view.getPaddingRight();
        if (fMeasureText > 0.0f && fWidth > 0.0f && fMeasureText > fWidth) {
            float length = this.mInfoText.length();
            int i = (int) (((fWidth - this.mEllipseLength) * length) / fMeasureText);
            com.mediatek.gallery3d.util.Log.v("VP_InfoExt", "updateVisibleText() infoText=" + this.mInfoText + " text width=" + fMeasureText + ", space=" + fWidth + ", originalNum=" + length + ", realNum=" + i + ", getPaddingLeft()=" + view.getPaddingLeft() + ", getPaddingRight()=" + view.getPaddingRight() + ", progressBar=" + rect + ", timeBounds=" + rect2);
            StringBuilder sb = new StringBuilder();
            sb.append(this.mInfoText.substring(0, i));
            sb.append("...");
            this.mVisibleText = sb.toString();
        } else {
            this.mVisibleText = this.mInfoText;
        }
        com.mediatek.gallery3d.util.Log.v("VP_InfoExt", "updateVisibleText() infoText=" + this.mInfoText + ", visibleText=" + this.mVisibleText + ", text width=" + fMeasureText + ", space=" + fWidth);
    }
}
