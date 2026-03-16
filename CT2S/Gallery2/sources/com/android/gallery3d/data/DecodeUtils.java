package com.android.gallery3d.data;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.util.FloatMath;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;
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

    public static Bitmap decode(ThreadPool.JobContext jc, byte[] bytes, int offset, int length, BitmapFactory.Options options) {
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        jc.setCancelListener(new DecodeCanceller(options));
        setOptionsMutable(options);
        return ensureGLCompatibleBitmap(BitmapFactory.decodeByteArray(bytes, offset, length, options));
    }

    public static void decodeBounds(ThreadPool.JobContext jc, byte[] bytes, int offset, int length, BitmapFactory.Options options) {
        Utils.assertTrue(options != null);
        options.inJustDecodeBounds = true;
        jc.setCancelListener(new DecodeCanceller(options));
        BitmapFactory.decodeByteArray(bytes, offset, length, options);
        options.inJustDecodeBounds = false;
    }

    public static Bitmap decodeThumbnail(ThreadPool.JobContext jc, String filePath, BitmapFactory.Options options, int targetSize, int type) throws Throwable {
        FileInputStream fis;
        FileInputStream fis2 = null;
        try {
            try {
                fis = new FileInputStream(filePath);
            } catch (Exception e) {
                ex = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            FileDescriptor fd = fis.getFD();
            Bitmap bitmapDecodeThumbnail = decodeThumbnail(jc, fd, options, targetSize, type);
            Utils.closeSilently(fis);
            return bitmapDecodeThumbnail;
        } catch (Exception e2) {
            ex = e2;
            fis2 = fis;
            com.android.gallery3d.ui.Log.w("DecodeUtils", ex);
            Utils.closeSilently(fis2);
            return null;
        } catch (Throwable th2) {
            th = th2;
            fis2 = fis;
            Utils.closeSilently(fis2);
            throw th;
        }
    }

    public static Bitmap decodeThumbnail(ThreadPool.JobContext jc, FileDescriptor fd, BitmapFactory.Options options, int targetSize, int type) {
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        jc.setCancelListener(new DecodeCanceller(options));
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        if (jc.isCancelled()) {
            return null;
        }
        int w = options.outWidth;
        int h = options.outHeight;
        if (type == 2) {
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(targetSize / Math.min(w, h));
            if ((w / options.inSampleSize) * (h / options.inSampleSize) > 640000) {
                options.inSampleSize = BitmapUtils.computeSampleSize(FloatMath.sqrt(640000.0f / (w * h)));
            }
        } else {
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(targetSize / Math.max(w, h));
        }
        options.inJustDecodeBounds = false;
        setOptionsMutable(options);
        Bitmap result = BitmapFactory.decodeFileDescriptor(fd, null, options);
        if (result == null) {
            return null;
        }
        float scale = targetSize / (type == 2 ? Math.min(result.getWidth(), result.getHeight()) : Math.max(result.getWidth(), result.getHeight()));
        if (scale <= 0.5d) {
            result = BitmapUtils.resizeBitmapByScale(result, scale, true);
        }
        return ensureGLCompatibleBitmap(result);
    }

    public static Bitmap decodeIfBigEnough(ThreadPool.JobContext jc, byte[] data, BitmapFactory.Options options, int targetSize) {
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        jc.setCancelListener(new DecodeCanceller(options));
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if (jc.isCancelled() || options.outWidth < targetSize || options.outHeight < targetSize) {
            return null;
        }
        options.inSampleSize = BitmapUtils.computeSampleSizeLarger(options.outWidth, options.outHeight, targetSize);
        options.inJustDecodeBounds = false;
        setOptionsMutable(options);
        return ensureGLCompatibleBitmap(BitmapFactory.decodeByteArray(data, 0, data.length, options));
    }

    public static Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != null) {
            return bitmap;
        }
        Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        bitmap.recycle();
        return bitmapCopy;
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(ThreadPool.JobContext jc, String filePath, boolean shareable) {
        try {
            return BitmapRegionDecoder.newInstance(filePath, shareable);
        } catch (Throwable t) {
            com.android.gallery3d.ui.Log.w("DecodeUtils", t);
            return null;
        }
    }

    public static BitmapRegionDecoder createBitmapRegionDecoder(ThreadPool.JobContext jc, FileDescriptor fd, boolean shareable) {
        try {
            return BitmapRegionDecoder.newInstance(fd, shareable);
        } catch (Throwable t) {
            com.android.gallery3d.ui.Log.w("DecodeUtils", t);
            return null;
        }
    }

    @TargetApi(11)
    public static Bitmap decodeUsingPool(ThreadPool.JobContext jc, byte[] data, int offset, int length, BitmapFactory.Options options) {
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        if (options.inSampleSize < 1) {
            options.inSampleSize = 1;
        }
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inBitmap = options.inSampleSize == 1 ? findCachedBitmap(jc, data, offset, length, options) : null;
        try {
            Bitmap bitmap = decode(jc, data, offset, length, options);
            if (options.inBitmap != null && options.inBitmap != bitmap) {
                GalleryBitmapPool.getInstance().put(options.inBitmap);
                options.inBitmap = null;
                return bitmap;
            }
            return bitmap;
        } catch (IllegalArgumentException e) {
            if (options.inBitmap == null) {
                throw e;
            }
            com.android.gallery3d.ui.Log.w("DecodeUtils", "decode fail with a given bitmap, try decode to a new bitmap");
            GalleryBitmapPool.getInstance().put(options.inBitmap);
            options.inBitmap = null;
            return decode(jc, data, offset, length, options);
        }
    }

    private static Bitmap findCachedBitmap(ThreadPool.JobContext jc, byte[] data, int offset, int length, BitmapFactory.Options options) {
        decodeBounds(jc, data, offset, length, options);
        return GalleryBitmapPool.getInstance().get(options.outWidth, options.outHeight);
    }
}
