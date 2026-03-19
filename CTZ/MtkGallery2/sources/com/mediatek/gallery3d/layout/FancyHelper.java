package com.mediatek.gallery3d.layout;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.LocalVideo;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallerybasic.base.MediaData;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FancyHelper {
    private static int sHeightPixels = -1;
    private static int sWidthPixels = -1;

    public static boolean isFancyLayoutSupported() {
        return true;
    }

    public static final boolean isLandItem(MediaItem mediaItem) {
        if (mediaItem == 0) {
            return false;
        }
        int orientation = mediaItem instanceof LocalVideo ? mediaItem.getOrientation() : mediaItem.getRotation();
        return (orientation == 90 || orientation == 270) ? mediaItem.getWidth() < mediaItem.getHeight() : mediaItem.getHeight() < mediaItem.getWidth();
    }

    public static Path getMediaSetPath(MediaData mediaData) {
        if (mediaData == null) {
            return null;
        }
        return Path.fromString("/local/all/" + mediaData.bucketId);
    }

    public static int getHeightPixels() {
        return sHeightPixels;
    }

    public static int getWidthPixels() {
        return sWidthPixels;
    }

    public static int getScreenWidthAtFancyMode() {
        return Math.min(sHeightPixels, sWidthPixels);
    }

    public static int getFullScreenLabelWidth(int i) {
        if (i > 1) {
            return (i * 2) + 7;
        }
        return getScreenWidthAtFancyMode();
    }

    public static int getSlotWidthAtFancyMode() {
        return (getScreenWidthAtFancyMode() - 7) / 2;
    }

    public static void doFancyInitialization(int i, int i2) {
        int iMin = Math.min(i, i2);
        if (sHeightPixels > sWidthPixels) {
            sWidthPixels = iMin;
        } else {
            sHeightPixels = iMin;
        }
        Log.d("MtkGallery2/FancyHelper", "<doFancyInitialization> <Fancy> w x h: " + sWidthPixels + ", " + sHeightPixels);
        MediaItem.setFancyThumbnailSizes(Math.min(sHeightPixels, sWidthPixels) / 3);
    }

    public static void initializeFancyThumbnailSizes(DisplayMetrics displayMetrics) {
        MediaItem.setFancyThumbnailSizes(Math.min(displayMetrics.heightPixels, displayMetrics.widthPixels) / 3);
        sHeightPixels = displayMetrics.heightPixels;
        sWidthPixels = displayMetrics.widthPixels;
    }

    public static Bitmap resizeByWidthOrLength(Bitmap bitmap, int i, boolean z, boolean z2) {
        float f;
        int iRound;
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (z) {
            if (width == i) {
                return bitmap;
            }
            f = i / width;
            iRound = Math.round(height * f);
        } else {
            if (height == i) {
                return bitmap;
            }
            f = i / height;
            int iRound2 = Math.round(width * f);
            iRound = i;
            i = iRound2;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, iRound, getConfig(bitmap));
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.scale(f, f);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, new Paint(6));
        if (z2) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    public static Bitmap resizeAndCropCenter(Bitmap bitmap, int i, int i2, boolean z, boolean z2) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == i && height == i2) {
            return bitmap;
        }
        float f = i / width;
        float f2 = i2 / height;
        if (z) {
            f2 = f;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, getConfig(bitmap));
        int iRound = Math.round(bitmap.getWidth() * f2);
        int iRound2 = Math.round(bitmap.getHeight() * f2);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.translate((i - iRound) / 2.0f, (i2 - iRound2) / 2.0f);
        canvas.scale(f2, f2);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, new Paint(6));
        if (z2) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    public static Bitmap resizeAndCropCenter(Bitmap bitmap, int i, boolean z) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == i && height == i) {
            return bitmap;
        }
        float fMin = i / Math.min(width, height);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, getConfig(bitmap));
        int iRound = Math.round(bitmap.getWidth() * fMin);
        int iRound2 = Math.round(bitmap.getHeight() * fMin);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.translate((i - iRound) / 2.0f, (i - iRound2) / 2.0f);
        canvas.scale(fMin, fMin);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, new Paint(6));
        if (z) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        if (bitmap == null) {
            return Bitmap.Config.ARGB_8888;
        }
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static Bitmap decodeThumbnail(ThreadPool.JobContext jobContext, String str, BitmapFactory.Options options, int i, int i2) throws Throwable {
        FileInputStream fileInputStream;
        try {
            if (jobContext == null) {
                Log.d("MtkGallery2/FancyHelper", "<decodeThumbnail> jc is null");
                return null;
            }
            try {
                fileInputStream = new FileInputStream(str);
                try {
                    Bitmap bitmapDecodeThumbnail = decodeThumbnail(jobContext, fileInputStream.getFD(), options, i, i2);
                    Utils.closeSilently(fileInputStream);
                    return bitmapDecodeThumbnail;
                } catch (FileNotFoundException e) {
                    e = e;
                    Log.d("MtkGallery2/FancyHelper", "<decodeThumbnail> FileNotFoundException ", e);
                    Utils.closeSilently(fileInputStream);
                    return null;
                } catch (IOException e2) {
                    e = e2;
                    Log.d("MtkGallery2/FancyHelper", "<decodeThumbnail> IOException ", e);
                    Utils.closeSilently(fileInputStream);
                    return null;
                }
            } catch (FileNotFoundException e3) {
                e = e3;
                fileInputStream = null;
            } catch (IOException e4) {
                e = e4;
                fileInputStream = null;
            } catch (Throwable th) {
                th = th;
                Utils.closeSilently((Closeable) null);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static Bitmap decodeThumbnail(ThreadPool.JobContext jobContext, FileDescriptor fileDescriptor, BitmapFactory.Options options, int i, int i2) {
        if (i2 != 3) {
            return null;
        }
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        jobContext.setCancelListener(new DecodeCanceller(options));
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        if (jobContext.isCancelled()) {
            return null;
        }
        int i3 = options.outWidth;
        float f = i3;
        float f2 = options.outHeight;
        if (f / f2 >= 2.5f || f2 / f >= 2.5f) {
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger((getScreenWidthAtFancyMode() / 2) / Math.min(i3, r1));
        } else {
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(i / Math.max(i3, r1));
        }
        Log.d("MtkGallery2/FancyHelper", "<decodeThumbnail> set samplesize to " + options.inSampleSize);
        options.inJustDecodeBounds = false;
        setOptionsMutable(options);
        long jCurrentTimeMillis = System.currentTimeMillis();
        Bitmap bitmapDecodeFileDescriptor = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        Log.d("MtkGallery2/FancyHelper", "<decodeThumbnail> decoding bmp costs " + (System.currentTimeMillis() - jCurrentTimeMillis));
        if (bitmapDecodeFileDescriptor == null) {
            return null;
        }
        float fMin = i / Math.min(bitmapDecodeFileDescriptor.getWidth(), bitmapDecodeFileDescriptor.getHeight());
        if (fMin <= 0.5f) {
            Log.d("MtkGallery2/FancyHelper", "<decodeThumbnail> resize down to scale " + fMin);
            bitmapDecodeFileDescriptor = BitmapUtils.resizeBitmapByScale(bitmapDecodeFileDescriptor, fMin, true);
        }
        return ensureGLCompatibleBitmap(bitmapDecodeFileDescriptor);
    }

    @TargetApi(11)
    public static void setOptionsMutable(BitmapFactory.Options options) {
        if (ApiHelper.HAS_OPTIONS_IN_MUTABLE) {
            options.inMutable = true;
        }
    }

    public static Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != null) {
            return bitmap;
        }
        Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        bitmap.recycle();
        return bitmapCopy;
    }

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
}
