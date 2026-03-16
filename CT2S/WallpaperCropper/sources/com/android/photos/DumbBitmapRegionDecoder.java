package com.android.photos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import java.io.InputStream;

class DumbBitmapRegionDecoder implements SimpleBitmapRegionDecoder {
    Bitmap mBuffer;
    Canvas mTempCanvas;
    Paint mTempPaint;

    private DumbBitmapRegionDecoder(Bitmap b) {
        this.mBuffer = b;
    }

    public static DumbBitmapRegionDecoder newInstance(InputStream is) {
        Bitmap b = BitmapFactory.decodeStream(is);
        if (b != null) {
            return new DumbBitmapRegionDecoder(b);
        }
        return null;
    }

    @Override
    public int getWidth() {
        return this.mBuffer.getWidth();
    }

    @Override
    public int getHeight() {
        return this.mBuffer.getHeight();
    }

    @Override
    public Bitmap decodeRegion(Rect wantRegion, BitmapFactory.Options options) {
        if (this.mTempCanvas == null) {
            this.mTempCanvas = new Canvas();
            this.mTempPaint = new Paint();
            this.mTempPaint.setFilterBitmap(true);
        }
        int sampleSize = Math.max(options.inSampleSize, 1);
        Bitmap newBitmap = Bitmap.createBitmap(wantRegion.width() / sampleSize, wantRegion.height() / sampleSize, Bitmap.Config.ARGB_8888);
        this.mTempCanvas.setBitmap(newBitmap);
        this.mTempCanvas.save();
        this.mTempCanvas.scale(1.0f / sampleSize, 1.0f / sampleSize);
        this.mTempCanvas.drawBitmap(this.mBuffer, -wantRegion.left, -wantRegion.top, this.mTempPaint);
        this.mTempCanvas.restore();
        this.mTempCanvas.setBitmap(null);
        return newBitmap;
    }
}
