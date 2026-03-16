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
import android.support.v4.app.NotificationCompat;
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
        protected int[] doInBackground(Bitmap... params) {
            int[] histo = new int[768];
            Bitmap bitmap = params[0];
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int index = (j * w) + i;
                    int r = Color.red(pixels[index]);
                    int g = Color.green(pixels[index]);
                    int b = Color.blue(pixels[index]);
                    histo[r] = histo[r] + 1;
                    int i2 = g + NotificationCompat.FLAG_LOCAL_ONLY;
                    histo[i2] = histo[i2] + 1;
                    int i3 = b + NotificationCompat.FLAG_GROUP_SUMMARY;
                    histo[i3] = histo[i3] + 1;
                }
            }
            return histo;
        }

        @Override
        protected void onPostExecute(int[] result) {
            System.arraycopy(result, 0, HistogramView.this.redHistogram, 0, NotificationCompat.FLAG_LOCAL_ONLY);
            System.arraycopy(result, NotificationCompat.FLAG_LOCAL_ONLY, HistogramView.this.greenHistogram, 0, NotificationCompat.FLAG_LOCAL_ONLY);
            System.arraycopy(result, NotificationCompat.FLAG_GROUP_SUMMARY, HistogramView.this.blueHistogram, 0, NotificationCompat.FLAG_LOCAL_ONLY);
            HistogramView.this.invalidate();
        }
    }

    public HistogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint = new Paint();
        this.redHistogram = new int[NotificationCompat.FLAG_LOCAL_ONLY];
        this.greenHistogram = new int[NotificationCompat.FLAG_LOCAL_ONLY];
        this.blueHistogram = new int[NotificationCompat.FLAG_LOCAL_ONLY];
        this.mHistoPath = new Path();
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        new ComputeHistogramTask().execute(this.mBitmap);
    }

    private void drawHistogram(Canvas canvas, int[] histogram, int color, PorterDuff.Mode mode) {
        int max = 0;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > max) {
                max = histogram[i];
            }
        }
        float w = getWidth();
        float h = getHeight();
        float wl = w / histogram.length;
        float wh = h / max;
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setARGB(100, 255, 255, 255);
        this.mPaint.setStrokeWidth((int) Math.ceil(wl));
        this.mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(0.0f, 0.0f, 0.0f + w, h, this.mPaint);
        canvas.drawLine(0.0f + (w / 3.0f), 0.0f, 0.0f + (w / 3.0f), h, this.mPaint);
        canvas.drawLine(0.0f + ((2.0f * w) / 3.0f), 0.0f, 0.0f + ((2.0f * w) / 3.0f), h, this.mPaint);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setColor(color);
        this.mPaint.setStrokeWidth(6.0f);
        this.mPaint.setXfermode(new PorterDuffXfermode(mode));
        this.mHistoPath.reset();
        this.mHistoPath.moveTo(0.0f, h);
        boolean firstPointEncountered = false;
        float prev = 0.0f;
        float last = 0.0f;
        for (int i2 = 0; i2 < histogram.length; i2++) {
            float x = (i2 * wl) + 0.0f;
            float l = histogram[i2] * wh;
            if (l != 0.0f) {
                float v = h - ((l + prev) / 2.0f);
                if (!firstPointEncountered) {
                    this.mHistoPath.lineTo(x, h);
                    firstPointEncountered = true;
                }
                this.mHistoPath.lineTo(x, v);
                prev = l;
                last = x;
            }
        }
        this.mHistoPath.lineTo(last, h);
        this.mHistoPath.lineTo(w, h);
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
