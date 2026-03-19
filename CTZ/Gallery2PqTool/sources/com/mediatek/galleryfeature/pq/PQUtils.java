package com.mediatek.galleryfeature.pq;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;
import com.mediatek.gallerybasic.util.Log;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;

public class PQUtils {
    private static final int HIGH_RESOLUTION_THRESHOLD = 1920;
    private static final int LOG_THRESHOLD = 31;
    private static final int STEP_INSAMPLE_SIZE = 8;
    private static final String TAG = "MtkGallery2/PQUtils";
    public static Field mField = null;
    public static boolean mHasInitializedField;

    interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    public static void initOptions(BitmapFactory.Options options) {
        if (!mHasInitializedField) {
            try {
                mField = options.getClass().getField("inPostProc");
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "NoSuchFieldException: " + e);
            }
            mHasInitializedField = true;
        }
        if (mHasInitializedField && mField != null) {
            try {
                mField.set(options, true);
                Log.v(TAG, "<initOptions> inPostPro = " + mField.getBoolean(options));
            } catch (IllegalAccessException e2) {
                Log.e(TAG, "IllegalAccessException: " + e2);
            } catch (IllegalArgumentException e3) {
                Log.e(TAG, "IllegalArgumentException: " + e3);
            }
        }
    }

    public static int calculateInSampleSize(Context context, String str, int i) throws Throwable {
        float fMax;
        FileInputStream fileInputStream;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        FileInputStream fileInputStream2 = null;
        try {
            try {
                try {
                    fileInputStream = getFileInputStream(context, str);
                    if (fileInputStream != null) {
                        try {
                            FileDescriptor fd = fileInputStream.getFD();
                            if (fd != null) {
                                BitmapFactory.decodeFileDescriptor(fd, null, options);
                            }
                        } catch (FileNotFoundException e) {
                            fileInputStream2 = fileInputStream;
                            Log.e(TAG, "<caculateInSampleSize>bitmapfactory decodestream fail");
                            if (fileInputStream2 != null) {
                                fileInputStream2.close();
                            }
                            fMax = 1.0f;
                            if (options.outWidth > 0) {
                                fMax = i / Math.max(options.outWidth, options.outHeight);
                            }
                            options.inSampleSize = computeSampleSizeLarger(fMax);
                            Log.d(TAG, "<caculateInSampleSize> options.inSampleSize=" + options.inSampleSize + " width=" + options.outWidth + " height=" + options.outHeight + "targetSize=" + i);
                            return options.inSampleSize;
                        } catch (IOException e2) {
                            fileInputStream2 = fileInputStream;
                            Log.e(TAG, "<caculateInSampleSize>bitmapfactory decodestream fail");
                            if (fileInputStream2 != null) {
                                fileInputStream2.close();
                            }
                            fMax = 1.0f;
                            if (options.outWidth > 0) {
                            }
                            options.inSampleSize = computeSampleSizeLarger(fMax);
                            Log.d(TAG, "<caculateInSampleSize> options.inSampleSize=" + options.inSampleSize + " width=" + options.outWidth + " height=" + options.outHeight + "targetSize=" + i);
                            return options.inSampleSize;
                        } catch (Throwable th) {
                            th = th;
                            fileInputStream2 = fileInputStream;
                            if (fileInputStream2 != null) {
                                try {
                                    fileInputStream2.close();
                                } catch (IOException e3) {
                                    e3.printStackTrace();
                                }
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (FileNotFoundException e4) {
            } catch (IOException e5) {
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        } catch (IOException e6) {
            e6.printStackTrace();
        }
        fMax = 1.0f;
        if (options.outWidth > 0 && options.outHeight > 0) {
            fMax = i / Math.max(options.outWidth, options.outHeight);
        }
        options.inSampleSize = computeSampleSizeLarger(fMax);
        Log.d(TAG, "<caculateInSampleSize> options.inSampleSize=" + options.inSampleSize + " width=" + options.outWidth + " height=" + options.outHeight + "targetSize=" + i);
        return options.inSampleSize;
    }

    public static int getRotation(Context context, String str) throws Throwable {
        final int[] iArr = new int[1];
        Log.d(TAG, "<getRotation> Uri.parse(mUri)==" + str);
        if ("content".equals(Uri.parse(str).getScheme())) {
            querySource(context, Uri.parse(str), new String[]{ExtFieldsUtils.VIDEO_ROTATION_FIELD}, new ContentResolverQueryCallback() {
                @Override
                public void onCursorResult(Cursor cursor) {
                    iArr[0] = cursor.getInt(0);
                }
            });
        }
        return iArr[0];
    }

    public static FileInputStream getFileInputStream(Context context, String str) throws Throwable {
        try {
            final String[] strArr = new String[1];
            Log.d(TAG, "<getFileInputStream> Uri.parse(mUri)==" + str);
            if ("content".equals(Uri.parse(str).getScheme())) {
                querySource(context, Uri.parse(str), new String[]{"_data", ExtFieldsUtils.VIDEO_ROTATION_FIELD}, new ContentResolverQueryCallback() {
                    @Override
                    public void onCursorResult(Cursor cursor) {
                        strArr[0] = cursor.getString(0);
                    }
                });
            } else {
                strArr[0] = str;
            }
            Log.d(TAG, "<getFileInputStream> fullPath[0]=" + strArr[0]);
            return new FileInputStream(strArr[0]);
        } catch (FileNotFoundException e) {
            Log.e(TAG, " <getFileInputStream> FileNotFoundException!");
            return null;
        }
    }

    private static void querySource(Context context, Uri uri, String[] strArr, ContentResolverQueryCallback contentResolverQueryCallback) throws Throwable {
        querySourceFromContentResolver(context.getContentResolver(), uri, strArr, contentResolverQueryCallback);
    }

    private static void querySourceFromContentResolver(ContentResolver contentResolver, Uri uri, String[] strArr, ContentResolverQueryCallback contentResolverQueryCallback) throws Throwable {
        Cursor cursorQuery;
        try {
            cursorQuery = contentResolver.query(uri, strArr, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        contentResolverQueryCallback.onCursorResult(cursorQuery);
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    public static boolean isSupportedByRegionDecoder(String str) {
        if (str == null) {
            return false;
        }
        String lowerCase = str.toLowerCase();
        return lowerCase.startsWith("image/") && !lowerCase.equals("image/gif");
    }

    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float f, boolean z) {
        int iRound = Math.round(bitmap.getWidth() * f);
        int iRound2 = Math.round(bitmap.getHeight() * f);
        if (iRound < 1 || iRound2 < 1) {
            Log.d(TAG, "<resizeBitmapByScale>scaled width or height < 1, no need to resize");
            return bitmap;
        }
        if (iRound == bitmap.getWidth() && iRound2 == bitmap.getHeight()) {
            return bitmap;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iRound, iRound2, getConfig(bitmap));
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.scale(f, f);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, new Paint(6));
        if (z) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    public static int floorLog2(float f) {
        int i = 0;
        while (i < LOG_THRESHOLD && (1 << i) <= f) {
            i++;
        }
        return i - 1;
    }

    public static int clamp(int i, int i2, int i3) {
        if (i > i3) {
            return i3;
        }
        if (i < i2) {
            return i2;
        }
        return i;
    }

    public static int computeSampleSizeLarger(float f) {
        int iFloor = (int) Math.floor(1.0f / f);
        if (iFloor <= 1) {
            return 1;
        }
        return iFloor <= STEP_INSAMPLE_SIZE ? prevPowerOf2(iFloor) : (iFloor / STEP_INSAMPLE_SIZE) * STEP_INSAMPLE_SIZE;
    }

    public static int calculateLevelCount(int i, int i2) {
        return Math.max(0, ceilLog2(i / i2));
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int i, boolean z) {
        if (i == 0) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(i);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        if (z) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    public static boolean isHighResolution(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        if (Build.VERSION.SDK_INT >= 17) {
            windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        } else {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return displayMetrics.heightPixels >= HIGH_RESOLUTION_THRESHOLD || displayMetrics.widthPixels >= HIGH_RESOLUTION_THRESHOLD;
    }

    private static int ceilLog2(float f) {
        int i = 0;
        while (i < LOG_THRESHOLD && (1 << i) < f) {
            i++;
        }
        return i;
    }

    private static int prevPowerOf2(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.highestOneBit(i);
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return config;
    }
}
