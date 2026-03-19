package com.mediatek.camera.common.mode.photo;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhotoModeHelper {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PhotoModeHelper.class.getSimpleName());
    private ICameraContext mICameraContext;
    private ImageFileName mImageFileName = new ImageFileName("'IMG'_yyyyMMdd_HHmmss_S");

    public PhotoModeHelper(ICameraContext iCameraContext) {
        this.mICameraContext = iCameraContext;
    }

    public ContentValues createContentValues(byte[] bArr, String str, int i, int i2) {
        ContentValues contentValues = new ContentValues();
        long jCurrentTimeMillis = System.currentTimeMillis();
        String strGenerateTitle = this.mImageFileName.generateTitle(jCurrentTimeMillis);
        String str2 = strGenerateTitle + ".jpg";
        int orientationFromExif = CameraUtil.getOrientationFromExif(bArr);
        contentValues.put("datetaken", Long.valueOf(jCurrentTimeMillis));
        contentValues.put("title", strGenerateTitle);
        contentValues.put("_display_name", str2);
        contentValues.put("mime_type", "image/jpeg");
        contentValues.put("width", Integer.valueOf(i));
        contentValues.put("height", Integer.valueOf(i2));
        contentValues.put("orientation", Integer.valueOf(orientationFromExif));
        contentValues.put("_data", str + '/' + str2);
        Location location = this.mICameraContext.getLocation();
        if (location != null) {
            contentValues.put("latitude", Double.valueOf(location.getLatitude()));
            contentValues.put("longitude", Double.valueOf(location.getLongitude()));
        }
        LogHelper.d(TAG, "createContentValues, width : " + i + ",height = " + i2 + ",orientation = " + orientationFromExif);
        return contentValues;
    }

    public static Bitmap makeBitmap(byte[] bArr, int i) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options);
            if (!options.mCancel && options.outWidth != -1 && options.outHeight != -1) {
                options.inSampleSize = computeSampleSize(options, -1, i);
                options.inJustDecodeBounds = false;
                options.inDither = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                return BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options);
            }
            return null;
        } catch (OutOfMemoryError e) {
            LogHelper.e(TAG, "[makeBitmap] Got oom exception:", e);
            return null;
        }
    }

    public static Bitmap rotateAndMirror(Bitmap bitmap, int i, boolean z) {
        if ((i != 0 || z) && bitmap != null) {
            Matrix matrix = new Matrix();
            if (z) {
                matrix.postScale(-1.0f, 1.0f);
                i = (i + 360) % 360;
                if (i == 0 || i == 180) {
                    matrix.postTranslate(bitmap.getWidth(), 0.0f);
                } else if (i == 90 || i == 270) {
                    matrix.postTranslate(bitmap.getHeight(), 0.0f);
                } else {
                    throw new IllegalArgumentException("Invalid degrees=" + i);
                }
            }
            if (i != 0) {
                matrix.postRotate(i, bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
            }
            try {
                Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (bitmap != bitmapCreateBitmap) {
                    bitmap.recycle();
                    return bitmapCreateBitmap;
                }
                return bitmap;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return bitmap;
            }
        }
        return bitmap;
    }

    public static void closeSilently(Closeable closeable) {
        if (closeable == null) {
            LogHelper.w(TAG, "[closeSilently] closeable is null ,return");
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCameraInfoOrientation(String str, Activity activity) {
        try {
            CameraCharacteristics cameraCharacteristics = ((CameraManager) activity.getSystemService("camera")).getCameraCharacteristics(str);
            if (cameraCharacteristics == null) {
                LogHelper.e(TAG, "[getCameraInfoOrientation] characteristics is null");
                return 0;
            }
            return ((Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean isMirror(String str, Activity activity) {
        try {
            CameraCharacteristics cameraCharacteristics = ((CameraManager) activity.getSystemService("camera")).getCameraCharacteristics(str);
            if (cameraCharacteristics == null) {
                LogHelper.e(TAG, "[isMirror] characteristics is null");
                return false;
            }
            int iIntValue = ((Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue();
            ((Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
            return iIntValue == 0;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int computeSampleSize(BitmapFactory.Options options, int i, int i2) {
        int iComputeInitialSampleSize = computeInitialSampleSize(options, i, i2);
        if (iComputeInitialSampleSize > 8) {
            return 8 * ((iComputeInitialSampleSize + 7) / 8);
        }
        int i3 = 1;
        while (i3 < iComputeInitialSampleSize) {
            i3 <<= 1;
        }
        return i3;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int i, int i2) {
        int iCeil;
        int iMin;
        double d = options.outWidth;
        double d2 = options.outHeight;
        if (i2 >= 0) {
            iCeil = (int) Math.ceil(Math.sqrt((d * d2) / ((double) i2)));
        } else {
            iCeil = 1;
        }
        if (i < 0) {
            iMin = 128;
        } else {
            double d3 = i;
            iMin = (int) Math.min(Math.floor(d / d3), Math.floor(d2 / d3));
        }
        if (iMin < iCeil) {
            return iCeil;
        }
        if (i2 < 0 && i < 0) {
            return 1;
        }
        if (i < 0) {
            return iCeil;
        }
        return iMin;
    }

    private class ImageFileName {
        private SimpleDateFormat mSimpleDateFormat;

        public ImageFileName(String str) {
            this.mSimpleDateFormat = new SimpleDateFormat(str);
        }

        public String generateTitle(long j) {
            return this.mSimpleDateFormat.format((Date) new java.sql.Date(j));
        }
    }
}
