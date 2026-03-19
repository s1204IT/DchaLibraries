package com.android.gallery3d.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import com.android.gallery3d.util.Log;
import com.mediatek.galleryportable.TraceHelper;
import com.mediatek.omadrm.OmaDrmStore;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BitmapUtils {
    public static int computeSampleSizeLarger(float f) {
        int iFloor = (int) Math.floor(1.0d / ((double) f));
        if (iFloor <= 1) {
            return 1;
        }
        if (iFloor > 8) {
            return (iFloor / 8) * 8;
        }
        return Utils.prevPowerOf2(iFloor);
    }

    public static int computeSampleSize(float f) {
        Utils.assertTrue(f > 0.0f);
        int iMax = Math.max(1, (int) Math.ceil(1.0f / f));
        if (iMax > 8) {
            return ((iMax + 7) / 8) * 8;
        }
        return Utils.nextPowerOf2(iMax);
    }

    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float f, boolean z) {
        int iRound = Math.round(bitmap.getWidth() * f);
        int iRound2 = Math.round(bitmap.getHeight() * f);
        if (iRound < 1 || iRound2 < 1) {
            Log.d("Gallery2/BitmapUtils", "<resizeBitmapByScale> scaled width or height < 1, no need to resize");
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

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static Bitmap resizeDownBySideLength(Bitmap bitmap, int i, boolean z) {
        float f = i;
        float fMin = Math.min(f / bitmap.getWidth(), f / bitmap.getHeight());
        return fMin >= 1.0f ? bitmap : resizeBitmapByScale(bitmap, fMin, z);
    }

    public static Bitmap resizeAndCropCenter(Bitmap bitmap, int i, boolean z) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == i && height == i) {
            return bitmap;
        }
        float fMin = i / Math.min(width, height);
        TraceHelper.beginSection(">>>>BitmapUtils-resizeAndCropCenter");
        TraceHelper.beginSection(">>>>BitmapUtils-createBitmap");
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, getConfig(bitmap));
        int iRound = Math.round(bitmap.getWidth() * fMin);
        int iRound2 = Math.round(bitmap.getHeight() * fMin);
        TraceHelper.endSection();
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.translate((i - iRound) / 2.0f, (i - iRound2) / 2.0f);
        canvas.scale(fMin, fMin);
        Paint paint = new Paint(6);
        TraceHelper.beginSection(">>>>BitmapUtils-canvas.drawBitmap");
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
        TraceHelper.endSection();
        TraceHelper.endSection();
        if (z) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    public static void recycleSilently(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        try {
            bitmap.recycle();
        } catch (Throwable th) {
            Log.w("Gallery2/BitmapUtils", "unable recycle bitmap", th);
        }
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

    public static Bitmap createVideoThumbnail(String str) throws Throwable {
        Class<?> cls;
        Object obj;
        Object objNewInstance;
        try {
            try {
                try {
                    cls = Class.forName("android.media.MediaMetadataRetriever");
                } catch (Throwable th) {
                    th = th;
                }
            } catch (ClassNotFoundException e) {
                e = e;
                cls = null;
                objNewInstance = null;
            } catch (IllegalAccessException e2) {
                e = e2;
                cls = null;
                objNewInstance = null;
            } catch (IllegalArgumentException e3) {
                cls = null;
                objNewInstance = null;
            } catch (InstantiationException e4) {
                e = e4;
                cls = null;
                objNewInstance = null;
            } catch (NoSuchMethodException e5) {
                e = e5;
                cls = null;
                objNewInstance = null;
            } catch (RuntimeException e6) {
                cls = null;
                objNewInstance = null;
            } catch (InvocationTargetException e7) {
                e = e7;
                cls = null;
                objNewInstance = null;
            } catch (Throwable th2) {
                th = th2;
                cls = null;
                obj = null;
            }
            try {
                TraceHelper.beginSection(">>>>BitmapUtils-MediaMetadataRetriever.new");
                objNewInstance = cls.newInstance();
                try {
                    TraceHelper.endSection();
                    Method method = cls.getMethod("setDataSource", String.class);
                    TraceHelper.beginSection(">>>>BitmapUtils-MediaMetadataRetriever.setDataSource");
                    method.invoke(objNewInstance, str);
                    TraceHelper.endSection();
                    if (Build.VERSION.SDK_INT <= 9) {
                        Bitmap bitmap = (Bitmap) cls.getMethod("captureFrame", new Class[0]).invoke(objNewInstance, new Object[0]);
                        if (objNewInstance != null) {
                            try {
                                cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                            } catch (Exception e8) {
                            }
                        }
                        return bitmap;
                    }
                    TraceHelper.beginSection(">>>>BitmapUtils-getEmbeddedPicture");
                    byte[] bArr = (byte[]) cls.getMethod("getEmbeddedPicture", new Class[0]).invoke(objNewInstance, new Object[0]);
                    TraceHelper.endSection();
                    if (bArr != null) {
                        TraceHelper.beginSection(">>>>BitmapUtils-decodeByteArray");
                        Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArr, 0, bArr.length);
                        TraceHelper.endSection();
                        if (bitmapDecodeByteArray != null) {
                            if (objNewInstance != null) {
                                try {
                                    cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                                } catch (Exception e9) {
                                }
                            }
                            return bitmapDecodeByteArray;
                        }
                    }
                    TraceHelper.beginSection(">>>>BitmapUtils-MediaMetadataRetriever.getFrameAtTime");
                    Bitmap bitmap2 = (Bitmap) cls.getMethod("getFrameAtTime", new Class[0]).invoke(objNewInstance, new Object[0]);
                    TraceHelper.endSection();
                    if (objNewInstance != null) {
                        try {
                            cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                        } catch (Exception e10) {
                        }
                    }
                    return bitmap2;
                } catch (ClassNotFoundException e11) {
                    e = e11;
                    Log.e("Gallery2/BitmapUtils", "createVideoThumbnail", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (IllegalAccessException e12) {
                    e = e12;
                    Log.e("Gallery2/BitmapUtils", "createVideoThumbnail", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (IllegalArgumentException e13) {
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (InstantiationException e14) {
                    e = e14;
                    Log.e("Gallery2/BitmapUtils", "createVideoThumbnail", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (NoSuchMethodException e15) {
                    e = e15;
                    Log.e("Gallery2/BitmapUtils", "createVideoThumbnail", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (RuntimeException e16) {
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (InvocationTargetException e17) {
                    e = e17;
                    Log.e("Gallery2/BitmapUtils", "createVideoThumbnail", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                }
            } catch (ClassNotFoundException e18) {
                e = e18;
                objNewInstance = null;
            } catch (IllegalAccessException e19) {
                e = e19;
                objNewInstance = null;
            } catch (IllegalArgumentException e20) {
                objNewInstance = null;
            } catch (InstantiationException e21) {
                e = e21;
                objNewInstance = null;
            } catch (NoSuchMethodException e22) {
                e = e22;
                objNewInstance = null;
            } catch (RuntimeException e23) {
                objNewInstance = null;
            } catch (InvocationTargetException e24) {
                e = e24;
                objNewInstance = null;
            } catch (Throwable th3) {
                th = th3;
                obj = null;
                if (obj != null) {
                    try {
                        cls.getMethod("release", new Class[0]).invoke(obj, new Object[0]);
                    } catch (Exception e25) {
                    }
                }
                throw th;
            }
        } catch (Exception e26) {
        }
    }

    public static byte[] compressToBytes(Bitmap bitmap) {
        return compressToBytes(bitmap, 100);
    }

    public static byte[] compressToBytes(Bitmap bitmap, int i) {
        TraceHelper.beginSection(">>>>BitmapUtils-new ByteArrayOutputStream");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(65536);
        TraceHelper.endSection();
        TraceHelper.beginSection(">>>>BitmapUtils-bitmap.compress");
        bitmap.compress(Bitmap.CompressFormat.JPEG, i, byteArrayOutputStream);
        TraceHelper.endSection();
        return byteArrayOutputStream.toByteArray();
    }

    public static boolean isSupportedByRegionDecoder(String str) {
        if (str == null) {
            return false;
        }
        String lowerCase = str.toLowerCase();
        return (!lowerCase.startsWith(OmaDrmStore.MimePrefix.IMAGE) || lowerCase.endsWith("gif") || lowerCase.endsWith("mpo") || lowerCase.endsWith("bmp")) ? false : true;
    }

    public static boolean isRotationSupported(String str) {
        if (str == null) {
            return false;
        }
        return str.toLowerCase().equals("image/jpeg");
    }

    public static Bitmap alignBitmapToEven(Bitmap bitmap, boolean z) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int i = (width % 2) + width;
        int i2 = (height % 2) + height;
        if (i == width && i2 == height) {
            return bitmap;
        }
        Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmap, i, i2, true);
        bitmap.recycle();
        return bitmapCreateScaledBitmap;
    }
}
