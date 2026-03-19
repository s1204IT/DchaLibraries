package com.android.gallery3d.data;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;

public class DecodeUtils {

    private static class DecodeCanceller implements ThreadPool.CancelListener {
        BitmapFactory.Options mOptions;

        public DecodeCanceller(BitmapFactory.Options options) {
            this.mOptions = options;
        }

        @Override
        public void onCancel() {
            this.mOptions.requestCancelDecode();
        }
    }

    @TargetApi(11)
    public static void setOptionsMutable(BitmapFactory.Options options) {
        if (ApiHelper.HAS_OPTIONS_IN_MUTABLE) {
            options.inMutable = true;
        }
    }

    public static void decodeBounds(ThreadPool.JobContext jobContext, FileDescriptor fileDescriptor, BitmapFactory.Options options) {
        Utils.assertTrue(options != null);
        options.inJustDecodeBounds = true;
        jobContext.setCancelListener(new DecodeCanceller(options));
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        options.inJustDecodeBounds = false;
    }

    public static Bitmap decode(ThreadPool.JobContext jobContext, byte[] bArr, int i, int i2, BitmapFactory.Options options) {
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        jobContext.setCancelListener(new DecodeCanceller(options));
        setOptionsMutable(options);
        return ensureGLCompatibleBitmap(BitmapFactory.decodeByteArray(bArr, i, i2, options));
    }

    public static void decodeBounds(ThreadPool.JobContext jobContext, byte[] bArr, int i, int i2, BitmapFactory.Options options) {
        Utils.assertTrue(options != null);
        options.inJustDecodeBounds = true;
        jobContext.setCancelListener(new DecodeCanceller(options));
        BitmapFactory.decodeByteArray(bArr, i, i2, options);
        options.inJustDecodeBounds = false;
    }

    public static Bitmap decodeThumbnail(ThreadPool.JobContext jobContext, String str, BitmapFactory.Options options, int i, int i2) throws Throwable {
        FileInputStream fileInputStream;
        try {
            try {
                fileInputStream = new FileInputStream(str);
                try {
                    Bitmap bitmapDecodeThumbnail = decodeThumbnail(jobContext, fileInputStream.getFD(), options, i, i2);
                    Utils.closeSilently(fileInputStream);
                    return bitmapDecodeThumbnail;
                } catch (Exception e) {
                    e = e;
                    com.android.gallery3d.ui.Log.w("Gallery2/DecodeUtils", e);
                    Utils.closeSilently(fileInputStream);
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                Utils.closeSilently((Closeable) null);
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
            fileInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            Utils.closeSilently((Closeable) null);
            throw th;
        }
    }

    public static Bitmap decodeThumbnail(ThreadPool.JobContext jobContext, FileDescriptor fileDescriptor, BitmapFactory.Options options, int i, int i2) {
        int iMax;
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        jobContext.setCancelListener(new DecodeCanceller(options));
        options.inJustDecodeBounds = true;
        long jCurrentTimeMillis = System.currentTimeMillis();
        com.android.gallery3d.ui.Log.v("Gallery2/DecodeUtils", "decoding bmp's bounds begins at" + jCurrentTimeMillis);
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        com.android.gallery3d.ui.Log.v("Gallery2/DecodeUtils", "decoding bmp's bounds costs " + (System.currentTimeMillis() - jCurrentTimeMillis));
        if (jobContext.isCancelled()) {
            return null;
        }
        int i3 = options.outWidth;
        int i4 = options.outHeight;
        if (i2 == 2) {
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(i / Math.min(i3, i4));
            if ((i3 / options.inSampleSize) * (i4 / options.inSampleSize) > 640000) {
                options.inSampleSize = BitmapUtils.computeSampleSize((float) Math.sqrt(640000.0d / ((double) (i3 * i4))));
            }
        } else {
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(i / Math.max(i3, i4));
        }
        options.inJustDecodeBounds = false;
        setOptionsMutable(options);
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        com.android.gallery3d.ui.Log.v("Gallery2/DecodeUtils", "decoding bmp begins at" + jCurrentTimeMillis2);
        Bitmap bitmapDecodeFileDescriptor = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        com.android.gallery3d.ui.Log.v("Gallery2/DecodeUtils", "decoding bmp costs " + (System.currentTimeMillis() - jCurrentTimeMillis2));
        if (bitmapDecodeFileDescriptor == null) {
            return null;
        }
        float f = i;
        if (i2 == 2) {
            iMax = Math.min(bitmapDecodeFileDescriptor.getWidth(), bitmapDecodeFileDescriptor.getHeight());
        } else {
            iMax = Math.max(bitmapDecodeFileDescriptor.getWidth(), bitmapDecodeFileDescriptor.getHeight());
        }
        float f2 = f / iMax;
        if (f2 <= 0.5d) {
            bitmapDecodeFileDescriptor = BitmapUtils.resizeBitmapByScale(bitmapDecodeFileDescriptor, f2, true);
        }
        return ensureGLCompatibleBitmap(bitmapDecodeFileDescriptor);
    }

    public static Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != null) {
            return bitmap;
        }
        Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        bitmap.recycle();
        return bitmapCopy;
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(ThreadPool.JobContext jobContext, String str, boolean z) {
        try {
            return BitmapRegionDecoder.newInstance(str, z);
        } catch (Throwable th) {
            com.android.gallery3d.ui.Log.w("Gallery2/DecodeUtils", th);
            return null;
        }
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(ThreadPool.JobContext jobContext, FileDescriptor fileDescriptor, boolean z) {
        try {
            return BitmapRegionDecoder.newInstance(fileDescriptor, z);
        } catch (Throwable th) {
            com.android.gallery3d.ui.Log.w("Gallery2/DecodeUtils", th);
            return null;
        }
    }

    @TargetApi(11)
    public static Bitmap decodeUsingPool(ThreadPool.JobContext jobContext, byte[] bArr, int i, int i2, BitmapFactory.Options options) {
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        if (options.inSampleSize < 1) {
            options.inSampleSize = 1;
        }
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inBitmap = options.inSampleSize == 1 ? findCachedBitmap(jobContext, bArr, i, i2, options) : null;
        try {
            Bitmap bitmapDecode = decode(jobContext, bArr, i, i2, options);
            if (options.inBitmap != null && options.inBitmap != bitmapDecode) {
                GalleryBitmapPool.getInstance().put(options.inBitmap);
                options.inBitmap = null;
            }
            return bitmapDecode;
        } catch (IllegalArgumentException e) {
            if (options.inBitmap == null) {
                throw e;
            }
            com.android.gallery3d.ui.Log.w("Gallery2/DecodeUtils", "decode fail with a given bitmap, try decode to a new bitmap");
            GalleryBitmapPool.getInstance().put(options.inBitmap);
            options.inBitmap = null;
            return decode(jobContext, bArr, i, i2, options);
        }
    }

    private static Bitmap findCachedBitmap(ThreadPool.JobContext jobContext, byte[] bArr, int i, int i2, BitmapFactory.Options options) {
        decodeBounds(jobContext, bArr, i, i2, options);
        return GalleryBitmapPool.getInstance().get(options.outWidth, options.outHeight);
    }
}
