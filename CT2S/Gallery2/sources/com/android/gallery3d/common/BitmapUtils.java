package com.android.gallery3d.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.util.FloatMath;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BitmapUtils {
    public static int computeSampleSizeLarger(int w, int h, int minSideLength) {
        int initialSize = Math.max(w / minSideLength, h / minSideLength);
        if (initialSize <= 1) {
            return 1;
        }
        return initialSize <= 8 ? Utils.prevPowerOf2(initialSize) : (initialSize / 8) * 8;
    }

    public static int computeSampleSizeLarger(float scale) {
        int initialSize = (int) FloatMath.floor(1.0f / scale);
        if (initialSize <= 1) {
            return 1;
        }
        return initialSize <= 8 ? Utils.prevPowerOf2(initialSize) : (initialSize / 8) * 8;
    }

    public static int computeSampleSize(float scale) {
        Utils.assertTrue(scale > 0.0f);
        int initialSize = Math.max(1, (int) FloatMath.ceil(1.0f / scale));
        return initialSize <= 8 ? Utils.nextPowerOf2(initialSize) : ((initialSize + 7) / 8) * 8;
    }

    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width != bitmap.getWidth() || height != bitmap.getHeight()) {
            Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
            Canvas canvas = new Canvas(target);
            canvas.scale(scale, scale);
            Paint paint = new Paint(6);
            canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
            if (recycle) {
                bitmap.recycle();
            }
            return target;
        }
        return bitmap;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static Bitmap resizeDownBySideLength(Bitmap bitmap, int maxLength, boolean recycle) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scale = Math.min(maxLength / srcWidth, maxLength / srcHeight);
        return scale >= 1.0f ? bitmap : resizeBitmapByScale(bitmap, scale, recycle);
    }

    public static Bitmap resizeAndCropCenter(Bitmap bitmap, int size, boolean recycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w != size || h != size) {
            float scale = size / Math.min(w, h);
            Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
            int width = Math.round(bitmap.getWidth() * scale);
            int height = Math.round(bitmap.getHeight() * scale);
            Canvas canvas = new Canvas(target);
            canvas.translate((size - width) / 2.0f, (size - height) / 2.0f);
            canvas.scale(scale, scale);
            Paint paint = new Paint(6);
            canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
            if (recycle) {
                bitmap.recycle();
            }
            return target;
        }
        return bitmap;
    }

    public static Bitmap rotateBitmap(Bitmap source, int rotation, boolean recycle) {
        if (rotation != 0) {
            int w = source.getWidth();
            int h = source.getHeight();
            Matrix m = new Matrix();
            m.postRotate(rotation);
            Bitmap bitmap = Bitmap.createBitmap(source, 0, 0, w, h, m, true);
            if (recycle) {
                source.recycle();
            }
            return bitmap;
        }
        return source;
    }

    public static Bitmap createVideoThumbnail(String filePath) {
        Bitmap bitmap;
        Bitmap bitmap2;
        Class<?> clazz = null;
        Object instance = null;
        try {
            try {
                try {
                    try {
                        clazz = Class.forName("android.media.MediaMetadataRetriever");
                        instance = clazz.newInstance();
                        Method method = clazz.getMethod("setDataSource", String.class);
                        method.invoke(instance, filePath);
                        if (Build.VERSION.SDK_INT <= 9) {
                            bitmap = (Bitmap) clazz.getMethod("captureFrame", new Class[0]).invoke(instance, new Object[0]);
                            if (instance != null) {
                                try {
                                    clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                                } catch (Exception e) {
                                }
                            }
                        } else {
                            byte[] data = (byte[]) clazz.getMethod("getEmbeddedPicture", new Class[0]).invoke(instance, new Object[0]);
                            if (data == null || (bitmap2 = BitmapFactory.decodeByteArray(data, 0, data.length)) == null) {
                                bitmap = (Bitmap) clazz.getMethod("getFrameAtTime", new Class[0]).invoke(instance, new Object[0]);
                                if (instance != null) {
                                    try {
                                        clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                                    } catch (Exception e2) {
                                    }
                                }
                            } else {
                                if (instance != null) {
                                    try {
                                        clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                                    } catch (Exception e3) {
                                    }
                                }
                                bitmap = bitmap2;
                            }
                        }
                        return bitmap;
                    } catch (InstantiationException e4) {
                        Log.e("BitmapUtils", "createVideoThumbnail", e4);
                        if (instance != null) {
                            try {
                                clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                            } catch (Exception e5) {
                            }
                        }
                        return null;
                    } catch (InvocationTargetException e6) {
                        Log.e("BitmapUtils", "createVideoThumbnail", e6);
                        if (instance != null) {
                            try {
                                clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                            } catch (Exception e7) {
                            }
                        }
                        return null;
                    }
                } catch (NoSuchMethodException e8) {
                    Log.e("BitmapUtils", "createVideoThumbnail", e8);
                    if (instance != null) {
                        try {
                            clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                        } catch (Exception e9) {
                        }
                    }
                    return null;
                } catch (RuntimeException e10) {
                    if (instance != null) {
                        try {
                            clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                        } catch (Exception e11) {
                        }
                    }
                    return null;
                }
            } catch (ClassNotFoundException e12) {
                Log.e("BitmapUtils", "createVideoThumbnail", e12);
                if (instance != null) {
                    try {
                        clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                    } catch (Exception e13) {
                    }
                }
                return null;
            } catch (IllegalAccessException e14) {
                Log.e("BitmapUtils", "createVideoThumbnail", e14);
                if (instance != null) {
                    try {
                        clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                    } catch (Exception e15) {
                    }
                }
                return null;
            } catch (IllegalArgumentException e16) {
                if (instance != null) {
                    try {
                        clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                    } catch (Exception e17) {
                    }
                }
                return null;
            }
        } catch (Throwable th) {
            if (instance != null) {
                try {
                    clazz.getMethod("release", new Class[0]).invoke(instance, new Object[0]);
                } catch (Exception e18) {
                }
            }
            throw th;
        }
    }

    public static byte[] compressToBytes(Bitmap bitmap) {
        return compressToBytes(bitmap, 90);
    }

    public static byte[] compressToBytes(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    public static boolean isSupportedByRegionDecoder(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        String mimeType2 = mimeType.toLowerCase();
        return (!mimeType2.startsWith("image/") || mimeType2.equals("image/gif") || mimeType2.endsWith("bmp") || mimeType2.endsWith("tiff")) ? false : true;
    }

    public static boolean isRotationSupported(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.toLowerCase().equals("image/jpeg");
    }

    public static boolean isGifPicture(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.toLowerCase().equals("image/gif");
    }
}
