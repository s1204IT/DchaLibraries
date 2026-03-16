package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.util.Log;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

public class TransformationUtils {
    public static final int PAINT_FLAGS = 7;
    private static final String TAG = "TransformationUtils";

    public static Bitmap centerCrop(Bitmap recycled, Bitmap toCrop, int width, int height) {
        float scale;
        Bitmap result;
        if (toCrop == null) {
            return null;
        }
        if (toCrop.getWidth() != width || toCrop.getHeight() != height) {
            float dx = 0.0f;
            float dy = 0.0f;
            Matrix m = new Matrix();
            if (toCrop.getWidth() * height > toCrop.getHeight() * width) {
                scale = height / toCrop.getHeight();
                dx = (width - (toCrop.getWidth() * scale)) * 0.5f;
            } else {
                scale = width / toCrop.getWidth();
                dy = (height - (toCrop.getHeight() * scale)) * 0.5f;
            }
            m.setScale(scale, scale);
            m.postTranslate(((int) dx) + 0.5f, ((int) dy) + 0.5f);
            if (recycled != null) {
                result = recycled;
            } else {
                result = Bitmap.createBitmap(width, height, toCrop.getConfig() == null ? Bitmap.Config.ARGB_8888 : toCrop.getConfig());
            }
            Canvas canvas = new Canvas(result);
            Paint paint = new Paint(7);
            canvas.drawBitmap(toCrop, m, paint);
            return result;
        }
        return toCrop;
    }

    public static Bitmap fitCenter(Bitmap toFit, BitmapPool pool, int width, int height) {
        if (toFit.getWidth() == width && toFit.getHeight() == height) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "requested target size matches input, returning input");
                return toFit;
            }
            return toFit;
        }
        float widthPercentage = width / toFit.getWidth();
        float heightPercentage = height / toFit.getHeight();
        float minPercentage = Math.min(widthPercentage, heightPercentage);
        int targetWidth = (int) (toFit.getWidth() * minPercentage);
        int targetHeight = (int) (toFit.getHeight() * minPercentage);
        if (toFit.getWidth() == targetWidth && toFit.getHeight() == targetHeight) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "adjusted target size matches input, returning input");
                return toFit;
            }
            return toFit;
        }
        Bitmap.Config config = toFit.getConfig() != null ? toFit.getConfig() : Bitmap.Config.ARGB_8888;
        Bitmap toReuse = pool.get(targetWidth, targetHeight, config);
        if (toReuse == null) {
            toReuse = Bitmap.createBitmap(targetWidth, targetHeight, config);
        }
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "request: " + width + "x" + height);
            Log.v(TAG, "toFit:   " + toFit.getWidth() + "x" + toFit.getHeight());
            Log.v(TAG, "toReuse: " + toReuse.getWidth() + "x" + toReuse.getHeight());
            Log.v(TAG, "minPct:   " + minPercentage);
        }
        Canvas canvas = new Canvas(toReuse);
        Matrix matrix = new Matrix();
        matrix.setScale(minPercentage, minPercentage);
        Paint paint = new Paint(7);
        canvas.drawBitmap(toFit, matrix, paint);
        return toReuse;
    }

    public static int getOrientation(String pathToOriginal) {
        try {
            ExifInterface exif = new ExifInterface(pathToOriginal);
            int orientation = exif.getAttributeInt("Orientation", 0);
            if (orientation == 6) {
                return 90;
            }
            if (orientation == 3) {
                return 180;
            }
            if (orientation != 8) {
                return 0;
            }
            return 270;
        } catch (Exception e) {
            if (!Log.isLoggable(TAG, 6)) {
                return 0;
            }
            Log.e(TAG, "Unable to get orientation for image with path=" + pathToOriginal, e);
            return 0;
        }
    }

    public static Bitmap orientImage(String pathToOriginal, Bitmap imageToOrient) {
        int degreesToRotate = getOrientation(pathToOriginal);
        return rotateImage(imageToOrient, degreesToRotate);
    }

    public static Bitmap rotateImage(Bitmap imageToOrient, int degreesToRotate) {
        if (degreesToRotate != 0) {
            try {
                Matrix matrix = new Matrix();
                matrix.setRotate(degreesToRotate);
                return Bitmap.createBitmap(imageToOrient, 0, 0, imageToOrient.getWidth(), imageToOrient.getHeight(), matrix, true);
            } catch (Exception e) {
                if (Log.isLoggable(TAG, 6)) {
                    Log.e(TAG, "Exception when trying to orient image", e);
                }
                e.printStackTrace();
                return imageToOrient;
            }
        }
        return imageToOrient;
    }

    public static int getExifOrientationDegrees(int exifOrientation) {
        switch (exifOrientation) {
            case 3:
            case 4:
                return 180;
            case 5:
            case 6:
                return 90;
            case 7:
            case 8:
                return 270;
            default:
                return 0;
        }
    }

    public static Bitmap rotateImageExif(Bitmap toOrient, BitmapPool pool, int exifOrientation) {
        Matrix matrix = new Matrix();
        switch (exifOrientation) {
            case 2:
                matrix.setScale(-1.0f, 1.0f);
                break;
            case 3:
                matrix.setRotate(180.0f);
                break;
            case 4:
                matrix.setRotate(180.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            case 5:
                matrix.setRotate(90.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            case 6:
                matrix.setRotate(90.0f);
                break;
            case 7:
                matrix.setRotate(-90.0f);
                matrix.postScale(-1.0f, 1.0f);
                break;
            case 8:
                matrix.setRotate(-90.0f);
                break;
            default:
                return toOrient;
        }
        RectF newRect = new RectF(0.0f, 0.0f, toOrient.getWidth(), toOrient.getHeight());
        matrix.mapRect(newRect);
        int newWidth = Math.round(newRect.width());
        int newHeight = Math.round(newRect.height());
        Bitmap result = pool.get(newWidth, newHeight, toOrient.getConfig());
        if (result == null) {
            result = Bitmap.createBitmap(newWidth, newHeight, toOrient.getConfig());
        }
        matrix.postTranslate(-newRect.left, -newRect.top);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(7);
        canvas.drawBitmap(toOrient, matrix, paint);
        return result;
    }
}
