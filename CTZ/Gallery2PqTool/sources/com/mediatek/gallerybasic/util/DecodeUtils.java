package com.mediatek.gallerybasic.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

public class DecodeUtils {
    private static final String TAG = "MtkGallery2/DecodeUtils";

    public static Bitmap decodeBitmap(String str, BitmapFactory.Options options) {
        BitmapUtils.setOptionsMutable(options);
        return BitmapFactory.decodeFile(str, options);
    }

    public static Bitmap decodeBitmap(Context context, Uri uri, BitmapFactory.Options options) throws Throwable {
        ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor;
        Bitmap bitmapDecodeFileDescriptor;
        try {
            try {
                parcelFileDescriptorOpenFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
                try {
                    BitmapUtils.setOptionsMutable(options);
                    bitmapDecodeFileDescriptor = BitmapFactory.decodeFileDescriptor(parcelFileDescriptorOpenFileDescriptor.getFileDescriptor(), null, options);
                    Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
                    context = parcelFileDescriptorOpenFileDescriptor;
                } catch (FileNotFoundException e) {
                    e = e;
                    e.printStackTrace();
                    Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
                    bitmapDecodeFileDescriptor = null;
                    context = parcelFileDescriptorOpenFileDescriptor;
                }
            } catch (Throwable th) {
                th = th;
                Utils.closeSilently(context);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            e = e2;
            parcelFileDescriptorOpenFileDescriptor = null;
        } catch (Throwable th2) {
            th = th2;
            context = 0;
            Utils.closeSilently(context);
            throw th;
        }
        return bitmapDecodeFileDescriptor;
    }

    private static Bitmap decode(String str, int i) {
        if (str == null || str.equals("")) {
            Log.d(TAG, "<decode> error args, return null");
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(str, options);
        options.inSampleSize = BitmapUtils.computeSampleSizeLarger(options.outWidth, options.outHeight, i);
        options.inJustDecodeBounds = false;
        BitmapUtils.setOptionsMutable(options);
        return BitmapFactory.decodeFile(str, options);
    }

    public static Bitmap decodeVideoThumbnail(String str, BitmapFactory.Options options) throws Throwable {
        Class<?> cls;
        Object obj;
        Object objNewInstance;
        Bitmap bitmapDecodeByteArray;
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
                e = e3;
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
            } catch (InvocationTargetException e6) {
                e = e6;
                cls = null;
                objNewInstance = null;
            } catch (Throwable th2) {
                th = th2;
                cls = null;
                obj = null;
            }
            try {
                objNewInstance = cls.newInstance();
                try {
                    cls.getMethod("setDataSource", String.class).invoke(objNewInstance, str);
                    if (Build.VERSION.SDK_INT <= 9) {
                        Bitmap bitmap = (Bitmap) cls.getMethod("captureFrame", new Class[0]).invoke(objNewInstance, new Object[0]);
                        if (bitmap != null) {
                            options.outWidth = bitmap.getWidth();
                            options.outHeight = bitmap.getHeight();
                        }
                        if (objNewInstance != null) {
                            try {
                                cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                            } catch (IllegalAccessException e7) {
                                Log.e(TAG, "<decodeVideoThumbnail> release", e7);
                            } catch (IllegalArgumentException e8) {
                                Log.e(TAG, "<decodeVideoThumbnail> release", e8);
                            } catch (NoSuchMethodException e9) {
                                Log.e(TAG, "<decodeVideoThumbnail> release", e9);
                            } catch (InvocationTargetException e10) {
                                Log.e(TAG, "<decodeVideoThumbnail> release", e10);
                            }
                        }
                        return bitmap;
                    }
                    byte[] bArr = (byte[]) cls.getMethod("getEmbeddedPicture", new Class[0]).invoke(objNewInstance, new Object[0]);
                    if (bArr != null && (bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArr, 0, bArr.length)) != null) {
                        if (objNewInstance != null) {
                            try {
                                cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                            } catch (IllegalAccessException e11) {
                                Log.e(TAG, "<decodeVideoThumbnail> release", e11);
                            } catch (IllegalArgumentException e12) {
                                Log.e(TAG, "<decodeVideoThumbnail> release", e12);
                            } catch (NoSuchMethodException e13) {
                                Log.e(TAG, "<decodeVideoThumbnail> release", e13);
                            } catch (InvocationTargetException e14) {
                                Log.e(TAG, "<decodeVideoThumbnail> release", e14);
                            }
                        }
                        return bitmapDecodeByteArray;
                    }
                    Bitmap bitmap2 = (Bitmap) cls.getMethod("getFrameAtTime", new Class[0]).invoke(objNewInstance, new Object[0]);
                    if (bitmap2 != null) {
                        options.outWidth = bitmap2.getWidth();
                        options.outHeight = bitmap2.getHeight();
                    }
                    if (objNewInstance != null) {
                        try {
                            cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                        } catch (IllegalAccessException e15) {
                            Log.e(TAG, "<decodeVideoThumbnail> release", e15);
                        } catch (IllegalArgumentException e16) {
                            Log.e(TAG, "<decodeVideoThumbnail> release", e16);
                        } catch (NoSuchMethodException e17) {
                            Log.e(TAG, "<decodeVideoThumbnail> release", e17);
                        } catch (InvocationTargetException e18) {
                            Log.e(TAG, "<decodeVideoThumbnail> release", e18);
                        }
                    }
                    return bitmap2;
                } catch (ClassNotFoundException e19) {
                    e = e19;
                    Log.e(TAG, "<decodeVideoThumbnail>", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (IllegalAccessException e20) {
                    e = e20;
                    Log.e(TAG, "<decodeVideoThumbnail>", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (IllegalArgumentException e21) {
                    e = e21;
                    Log.e(TAG, "<decodeVideoThumbnail>", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (InstantiationException e22) {
                    e = e22;
                    Log.e(TAG, "<decodeVideoThumbnail>", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (NoSuchMethodException e23) {
                    e = e23;
                    Log.e(TAG, "<decodeVideoThumbnail>", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                } catch (InvocationTargetException e24) {
                    e = e24;
                    Log.e(TAG, "<decodeVideoThumbnail>", e);
                    if (objNewInstance != null) {
                        cls.getMethod("release", new Class[0]).invoke(objNewInstance, new Object[0]);
                    }
                    return null;
                }
            } catch (ClassNotFoundException e25) {
                e = e25;
                objNewInstance = null;
            } catch (IllegalAccessException e26) {
                e = e26;
                objNewInstance = null;
            } catch (IllegalArgumentException e27) {
                e = e27;
                objNewInstance = null;
            } catch (InstantiationException e28) {
                e = e28;
                objNewInstance = null;
            } catch (NoSuchMethodException e29) {
                e = e29;
                objNewInstance = null;
            } catch (InvocationTargetException e30) {
                e = e30;
                objNewInstance = null;
            } catch (Throwable th3) {
                th = th3;
                obj = null;
                if (obj != null) {
                    try {
                        cls.getMethod("release", new Class[0]).invoke(obj, new Object[0]);
                    } catch (IllegalAccessException e31) {
                        Log.e(TAG, "<decodeVideoThumbnail> release", e31);
                    } catch (IllegalArgumentException e32) {
                        Log.e(TAG, "<decodeVideoThumbnail> release", e32);
                    } catch (NoSuchMethodException e33) {
                        Log.e(TAG, "<decodeVideoThumbnail> release", e33);
                    } catch (InvocationTargetException e34) {
                        Log.e(TAG, "<decodeVideoThumbnail> release", e34);
                    }
                }
                throw th;
            }
        } catch (IllegalAccessException e35) {
            Log.e(TAG, "<decodeVideoThumbnail> release", e35);
        } catch (IllegalArgumentException e36) {
            Log.e(TAG, "<decodeVideoThumbnail> release", e36);
        } catch (NoSuchMethodException e37) {
            Log.e(TAG, "<decodeVideoThumbnail> release", e37);
        } catch (InvocationTargetException e38) {
            Log.e(TAG, "<decodeVideoThumbnail> release", e38);
        }
    }
}
