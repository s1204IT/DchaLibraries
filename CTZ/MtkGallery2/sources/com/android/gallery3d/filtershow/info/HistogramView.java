package com.android.gallery3d.filtershow.info;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;

public class HistogramView extends View {
    private int[] blueHistogram;
    private int[] greenHistogram;
    private Bitmap mBitmap;
    private Path mHistoPath;
    private Paint mPaint;
    private int[] redHistogram;

    class ComputeHistogramTask extends AsyncTask<Bitmap, Void, int[]> {
        ComputeHistogramTask() {
        }

        @Override
        protected int[] doInBackground(Bitmap... bitmapArr) {
            int[] iArr = new int[768];
            Bitmap bitmap = bitmapArr[0];
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] iArr2 = new int[width * height];
            bitmap.getPixels(iArr2, 0, width, 0, 0, width, height);
            for (int i = 0; i < width; i++) {
                for (int i2 = 0; i2 < height; i2++) {
                    int i3 = (i2 * width) + i;
                    int iRed = Color.red(iArr2[i3]);
                    int iGreen = Color.green(iArr2[i3]);
                    int iBlue = Color.blue(iArr2[i3]);
                    iArr[iRed] = iArr[iRed] + 1;
                    int i4 = 256 + iGreen;
                    iArr[i4] = iArr[i4] + 1;
                    int i5 = 512 + iBlue;
                    iArr[i5] = iArr[i5] + 1;
                }
            }
            return iArr;
        }

        @Override
        protected void onPostExecute(int[] iArr) {
            System.arraycopy(iArr, 0, HistogramView.this.redHistogram, 0, 256);
            System.arraycopy(iArr, 256, HistogramView.this.greenHistogram, 0, 256);
            System.arraycopy(iArr, 512, HistogramView.this.blueHistogram, 0, 256);
            HistogramView.this.invalidate();
        }
    }

    public HistogramView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint();
        this.redHistogram = new int[256];
        this.greenHistogram = new int[256];
        this.blueHistogram = new int[256];
        this.mHistoPath = new Path();
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        new ComputeHistogramTask().execute(this.mBitmap);
    }

    private void drawHistogram(Canvas canvas, int[] iArr, int i, PorterDuff.Mode mode) {
        int i2 = 0;
        for (int i3 = 0; i3 < iArr.length; i3++) {
            if (iArr[i3] > i2) {
                i2 = iArr[i3];
            }
        }
        float width = getWidth();
        float height = getHeight();
        float length = width / iArr.length;
        float f = height / i2;
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setARGB(100, 255, 255, 255);
        this.mPaint.setStrokeWidth((int) Math.ceil(length));
        this.mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(0.0f, 0.0f, 0.0f + width, height, this.mPaint);
        float f2 = 0.0f + (width / 3.0f);
        canvas.drawLine(f2, 0.0f, f2, height, this.mPaint);
        float f3 = 0.0f + ((2.0f * width) / 3.0f);
        canvas.drawLine(f3, 0.0f, f3, height, this.mPaint);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setColor(i);
        this.mPaint.setStrokeWidth(6.0f);
        this.mPaint.setXfermode(new PorterDuffXfermode(mode));
        this.mHistoPath.reset();
        this.mHistoPath.moveTo(0.0f, height);
        float f4 = 0.0f;
        float f5 = 0.0f;
        boolean z = false;
        for (int i4 = 0; i4 < iArr.length; i4++) {
            float f6 = (i4 * length) + 0.0f;
            float f7 = iArr[i4] * f;
            if (f7 != 0.0f) {
                float f8 = height - ((f5 + f7) / 2.0f);
                if (!z) {
                    this.mHistoPath.lineTo(f6, height);
                    z = true;
                }
                this.mHistoPath.lineTo(f6, f8);
                f4 = f6;
                f5 = f7;
            }
        }
        this.mHistoPath.lineTo(f4, height);
        this.mHistoPath.lineTo(width, height);
        this.mHistoPath.close();
        canvas.drawPath(this.mHistoPath, this.mPaint);
        this.mPaint.setStrokeWidth(2.0f);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setARGB(255, 200, 200, 200);
        canvas.drawPath(this.mHistoPath, this.mPaint);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawARGB(0, 0, 0, 0);
        drawHistogram(canvas, this.redHistogram, -65536, PorterDuff.Mode.SCREEN);
        drawHistogram(canvas, this.greenHistogram, -16711936, PorterDuff.Mode.SCREEN);
        drawHistogram(canvas, this.blueHistogram, -16776961, PorterDuff.Mode.SCREEN);
    }
}
