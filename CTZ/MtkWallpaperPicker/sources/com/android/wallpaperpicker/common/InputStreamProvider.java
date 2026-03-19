package com.android.wallpaperpicker.common;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import com.android.gallery3d.common.ExifOrientation;
import com.android.gallery3d.common.Utils;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class InputStreamProvider {
    public abstract InputStream newStreamNotNull() throws IOException;

    public InputStream newStream() {
        try {
            return newStreamNotNull();
        } catch (IOException e) {
            return null;
        }
    }

    public Point getImageBounds() {
        InputStream inputStreamNewStream = newStream();
        if (inputStreamNewStream != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStreamNewStream, null, options);
            Utils.closeSilently(inputStreamNewStream);
            if (options.outWidth != 0 && options.outHeight != 0) {
                return new Point(options.outWidth, options.outHeight);
            }
        }
        return null;
    }

    public Bitmap readCroppedBitmap(RectF rectF, int i, int i2, int i3) throws Throwable {
        InputStream inputStreamNewStreamNotNull;
        BitmapRegionDecoder bitmapRegionDecoderNewInstance;
        Bitmap bitmapCreateBitmap;
        int iRound;
        int iRound2;
        Bitmap bitmapCreateBitmap2;
        Bitmap bitmapDecodeStream;
        Rect rect = new Rect();
        Matrix matrix = new Matrix();
        Point imageBounds = getImageBounds();
        if (imageBounds == null) {
            Log.w("InputStreamProvider", "cannot get bounds for image");
            return null;
        }
        if (i3 > 0) {
            matrix.setRotate(i3);
            Matrix matrix2 = new Matrix();
            matrix2.setRotate(-i3);
            rectF.roundOut(rect);
            rectF.set(rect);
            float[] fArr = {imageBounds.x, imageBounds.y};
            matrix.mapPoints(fArr);
            fArr[0] = Math.abs(fArr[0]);
            fArr[1] = Math.abs(fArr[1]);
            rectF.offset((-fArr[0]) / 2.0f, (-fArr[1]) / 2.0f);
            matrix2.mapRect(rectF);
            rectF.offset(imageBounds.x / 2, imageBounds.y / 2);
        }
        rectF.roundOut(rect);
        if (rect.width() <= 0 || rect.height() <= 0) {
            Log.w("InputStreamProvider", "crop has bad values for full size image");
            return null;
        }
        int iMax = Math.max(1, Math.min(rect.width() / i, rect.height() / i2));
        try {
            inputStreamNewStreamNotNull = newStreamNotNull();
            try {
                try {
                    bitmapRegionDecoderNewInstance = BitmapRegionDecoder.newInstance(inputStreamNewStreamNotNull, false);
                    Utils.closeSilently(inputStreamNewStreamNotNull);
                } catch (IOException e) {
                    e = e;
                    Log.w("InputStreamProvider", "cannot open region decoder", e);
                    Utils.closeSilently(inputStreamNewStreamNotNull);
                    bitmapRegionDecoderNewInstance = null;
                }
            } catch (Throwable th) {
                th = th;
                Utils.closeSilently(inputStreamNewStreamNotNull);
                throw th;
            }
        } catch (IOException e2) {
            e = e2;
            inputStreamNewStreamNotNull = null;
        } catch (Throwable th2) {
            th = th2;
            inputStreamNewStreamNotNull = null;
            Utils.closeSilently(inputStreamNewStreamNotNull);
            throw th;
        }
        if (bitmapRegionDecoderNewInstance != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            if (iMax > 1) {
                options.inSampleSize = iMax;
            }
            bitmapCreateBitmap = bitmapRegionDecoderNewInstance.decodeRegion(rect, options);
            bitmapRegionDecoderNewInstance.recycle();
        } else {
            bitmapCreateBitmap = null;
        }
        if (bitmapCreateBitmap == null) {
            InputStream inputStreamNewStream = newStream();
            if (inputStreamNewStream != null) {
                BitmapFactory.Options options2 = new BitmapFactory.Options();
                if (iMax > 1) {
                    options2.inSampleSize = iMax;
                }
                try {
                    bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamNewStream, null, options2);
                } catch (OutOfMemoryError e3) {
                    Log.e("InputStreamProvider", "Failed to decodeStream " + inputStreamNewStream, e3);
                    return null;
                } finally {
                    Utils.closeSilently(inputStreamNewStream);
                }
            } else {
                bitmapDecodeStream = null;
            }
            if (bitmapDecodeStream != null) {
                float width = imageBounds.x / bitmapDecodeStream.getWidth();
                rectF.left /= width;
                rectF.top /= width;
                rectF.bottom /= width;
                rectF.right /= width;
                rectF.roundOut(rect);
                if (rect.width() > bitmapDecodeStream.getWidth()) {
                    rect.right = rect.left + bitmapDecodeStream.getWidth();
                }
                if (rect.right > bitmapDecodeStream.getWidth()) {
                    rect.offset(-(rect.right - bitmapDecodeStream.getWidth()), 0);
                }
                if (rect.height() > bitmapDecodeStream.getHeight()) {
                    rect.bottom = rect.top + bitmapDecodeStream.getHeight();
                }
                if (rect.bottom > bitmapDecodeStream.getHeight()) {
                    rect.offset(0, -(rect.bottom - bitmapDecodeStream.getHeight()));
                }
                try {
                    bitmapCreateBitmap = Bitmap.createBitmap(bitmapDecodeStream, rect.left, rect.top, rect.width(), rect.height());
                } catch (OutOfMemoryError e4) {
                    Log.e("InputStreamProvider", "Failed to create Bitmap");
                }
            }
        }
        if (bitmapCreateBitmap == null) {
            return null;
        }
        if ((i <= 0 || i2 <= 0) && i3 <= 0) {
            return bitmapCreateBitmap;
        }
        float[] fArr2 = {bitmapCreateBitmap.getWidth(), bitmapCreateBitmap.getHeight()};
        matrix.mapPoints(fArr2);
        fArr2[0] = Math.abs(fArr2[0]);
        fArr2[1] = Math.abs(fArr2[1]);
        if (i <= 0 || i2 <= 0) {
            iRound = Math.round(fArr2[0]);
            iRound2 = Math.round(fArr2[1]);
        } else {
            iRound = i;
            iRound2 = i2;
        }
        RectF rectF2 = new RectF(0.0f, 0.0f, fArr2[0], fArr2[1]);
        RectF rectF3 = new RectF(0.0f, 0.0f, iRound, iRound2);
        Matrix matrix3 = new Matrix();
        if (i3 == 0) {
            matrix3.setRectToRect(rectF2, rectF3, Matrix.ScaleToFit.FILL);
        } else {
            Matrix matrix4 = new Matrix();
            matrix4.setTranslate((-bitmapCreateBitmap.getWidth()) / 2.0f, (-bitmapCreateBitmap.getHeight()) / 2.0f);
            Matrix matrix5 = new Matrix();
            matrix5.setRotate(i3);
            Matrix matrix6 = new Matrix();
            matrix6.setTranslate(fArr2[0] / 2.0f, fArr2[1] / 2.0f);
            Matrix matrix7 = new Matrix();
            matrix7.setRectToRect(rectF2, rectF3, Matrix.ScaleToFit.FILL);
            Matrix matrix8 = new Matrix();
            matrix8.setConcat(matrix5, matrix4);
            Matrix matrix9 = new Matrix();
            matrix9.setConcat(matrix7, matrix6);
            matrix3.setConcat(matrix9, matrix8);
        }
        try {
            bitmapCreateBitmap2 = Bitmap.createBitmap((int) rectF3.width(), (int) rectF3.height(), Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e5) {
            Log.e("InputStreamProvider", "Failed to create Bitmap");
            bitmapCreateBitmap2 = null;
        }
        if (bitmapCreateBitmap2 == null) {
            return bitmapCreateBitmap;
        }
        Canvas canvas = new Canvas(bitmapCreateBitmap2);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmapCreateBitmap, matrix3, paint);
        return bitmapCreateBitmap2;
    }

    public int getRotationFromExif(Context context) throws Throwable {
        InputStream inputStreamNewStreamNotNull;
        InputStream inputStream = null;
        try {
            try {
                inputStreamNewStreamNotNull = newStreamNotNull();
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException | NullPointerException e) {
            e = e;
        }
        try {
            int rotation = ExifOrientation.readRotation(new BufferedInputStream(inputStreamNewStreamNotNull), context);
            Utils.closeSilently(inputStreamNewStreamNotNull);
            return rotation;
        } catch (IOException | NullPointerException e2) {
            e = e2;
            inputStream = inputStreamNewStreamNotNull;
            Log.w("InputStreamProvider", "Getting exif data failed", e);
            Utils.closeSilently(inputStream);
            return 0;
        } catch (Throwable th2) {
            th = th2;
            inputStream = inputStreamNewStreamNotNull;
            Utils.closeSilently(inputStream);
            throw th;
        }
    }

    public static InputStreamProvider fromUri(final Context context, final Uri uri) {
        return new InputStreamProvider() {
            @Override
            public InputStream newStreamNotNull() throws IOException {
                return new BufferedInputStream(context.getContentResolver().openInputStream(uri));
            }
        };
    }

    public static InputStreamProvider fromResource(final Resources resources, final int i) {
        return new InputStreamProvider() {
            @Override
            public InputStream newStreamNotNull() {
                return new BufferedInputStream(resources.openRawResource(i));
            }
        };
    }

    public static InputStreamProvider fromBytes(final byte[] bArr) {
        return new InputStreamProvider() {
            @Override
            public InputStream newStreamNotNull() {
                return new BufferedInputStream(new ByteArrayInputStream(bArr));
            }
        };
    }
}
