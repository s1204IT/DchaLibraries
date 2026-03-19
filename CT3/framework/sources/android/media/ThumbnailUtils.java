package android.media;

import android.app.admin.DevicePolicyManager;
import android.app.backup.FullBackup;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaFile;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class ThumbnailUtils {
    private static final boolean IS_DENSITY_XXXHIGH;
    private static final int MAX_NUM_PIXELS_MICRO_THUMBNAIL = 19200;
    private static final int MAX_NUM_PIXELS_THUMBNAIL;
    private static final int OPTIONS_NONE = 0;
    public static final int OPTIONS_RECYCLE_INPUT = 2;
    private static final int OPTIONS_SCALE_UP = 1;
    private static final String TAG = "ThumbnailUtils";
    public static final int TARGET_SIZE_MICRO_THUMBNAIL = 96;
    public static final int TARGET_SIZE_MINI_THUMBNAIL;
    private static final int UNCONSTRAINED = -1;

    static {
        IS_DENSITY_XXXHIGH = DisplayMetrics.DENSITY_DEVICE >= 640;
        MAX_NUM_PIXELS_THUMBNAIL = IS_DENSITY_XXXHIGH ? 442368 : DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
        TARGET_SIZE_MINI_THUMBNAIL = IS_DENSITY_XXXHIGH ? 480 : 320;
    }

    public static Bitmap createImageThumbnail(String filePath, int kind) throws Throwable {
        FileInputStream stream;
        boolean wantMini = kind == 1;
        int targetSize = wantMini ? TARGET_SIZE_MINI_THUMBNAIL : 96;
        int maxPixels = wantMini ? MAX_NUM_PIXELS_THUMBNAIL : MAX_NUM_PIXELS_MICRO_THUMBNAIL;
        SizedThumbnailBitmap sizedThumbnailBitmap = new SizedThumbnailBitmap(null);
        Bitmap bitmap = null;
        MediaFile.MediaFileType fileType = MediaFile.getFileType(filePath);
        if (fileType != null && (fileType.fileType == 401 || MediaFile.isRawImageFileType(fileType.fileType))) {
            createThumbnailFromEXIF(filePath, targetSize, maxPixels, sizedThumbnailBitmap);
            bitmap = sizedThumbnailBitmap.mBitmap;
        }
        if (bitmap == null) {
            FileInputStream stream2 = null;
            try {
                try {
                    stream = new FileInputStream(filePath);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    FileDescriptor fd = stream.getFD();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFileDescriptor(fd, null, options);
                    if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException ex) {
                                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex);
                            }
                        }
                        return null;
                    }
                    options.inSampleSize = adustSampleSize(computeSampleSize(options, targetSize, maxPixels), options);
                    options.inJustDecodeBounds = false;
                    if (filePath.endsWith(".dcf")) {
                        options.inSampleSize |= 256;
                    }
                    options.inDither = false;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException ex2) {
                            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex2);
                        }
                    }
                } catch (IOException e) {
                    ex = e;
                    stream2 = stream;
                    Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex);
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException ex3) {
                            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex3);
                        }
                    }
                } catch (OutOfMemoryError e2) {
                    oom = e2;
                    stream2 = stream;
                    Log.e(TAG, "Unable to decode file " + filePath + ". OutOfMemoryError.", oom);
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException ex4) {
                            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex4);
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    stream2 = stream;
                    if (stream2 != null) {
                        try {
                            stream2.close();
                        } catch (IOException ex5) {
                            Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex5);
                        }
                    }
                    throw th;
                }
            } catch (IOException e3) {
                ex = e3;
            } catch (OutOfMemoryError e4) {
                oom = e4;
            }
        }
        return kind == 3 ? extractThumbnail(bitmap, 96, 96, 2) : bitmap;
    }

    public static Bitmap createVideoThumbnail(String filePath, int kind) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime(-1L);
            try {
                retriever.release();
            } catch (RuntimeException e) {
            }
        } catch (IllegalArgumentException e2) {
            try {
                retriever.release();
            } catch (RuntimeException e3) {
            }
        } catch (RuntimeException e4) {
            try {
                retriever.release();
            } catch (RuntimeException e5) {
            }
        } catch (Throwable th) {
            try {
                retriever.release();
            } catch (RuntimeException e6) {
            }
            throw th;
        }
        if (bitmap == null) {
            return null;
        }
        if (kind != 1) {
            return kind == 3 ? extractThumbnail(bitmap, 96, 96, 2) : bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int max = Math.max(width, height);
        if (max <= 512) {
            return bitmap;
        }
        float scale = 512.0f / max;
        int w = Math.round(width * scale);
        int h = Math.round(height * scale);
        return Bitmap.createScaledBitmap(bitmap, w, h, true);
    }

    public static Bitmap extractThumbnail(Bitmap source, int width, int height) {
        return extractThumbnail(source, width, height, 0);
    }

    public static Bitmap extractThumbnail(Bitmap source, int width, int height, int options) {
        float scale;
        if (source == null) {
            return null;
        }
        if (source.getWidth() < source.getHeight()) {
            scale = width / source.getWidth();
        } else {
            scale = height / source.getHeight();
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        RectF srcR = new RectF(0.0f, 0.0f, source.getWidth(), source.getHeight());
        RectF deviceR = new RectF();
        matrix.mapRect(deviceR, srcR);
        if (deviceR.width() * deviceR.height() >= 1048576.0f) {
            Bitmap thumbnail = transform(matrix, source, width, height, options);
            return thumbnail;
        }
        Bitmap thumbnail2 = transform(matrix, source, width, height, options | 1);
        return thumbnail2;
    }

    private static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        if (initialSize <= 8) {
            int roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
            return roundedSize;
        }
        int roundedSize2 = ((initialSize + 7) / 8) * 8;
        return roundedSize2;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = maxNumOfPixels == -1 ? 1 : (int) Math.ceil(Math.sqrt((w * h) / ((double) maxNumOfPixels)));
        int upperBound = minSideLength == -1 ? 128 : (int) Math.min(Math.floor(w / ((double) minSideLength)), Math.floor(h / ((double) minSideLength)));
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        if (maxNumOfPixels == -1 && minSideLength == -1) {
            return 1;
        }
        if (minSideLength == -1) {
            return lowerBound;
        }
        return upperBound;
    }

    private static Bitmap makeBitmap(int minSideLength, int maxNumOfPixels, Uri uri, ContentResolver cr, ParcelFileDescriptor pfd, BitmapFactory.Options options) {
        if (pfd == null) {
            try {
                pfd = makeInputStream(uri, cr);
            } catch (OutOfMemoryError ex) {
                Log.e(TAG, "Got oom exception ", ex);
                return null;
            } finally {
                closeSilently(pfd);
            }
        }
        if (pfd == null) {
            return null;
        }
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        FileDescriptor fd = pfd.getFileDescriptor();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
            return null;
        }
        options.inSampleSize = computeSampleSize(options, minSideLength, maxNumOfPixels);
        options.inJustDecodeBounds = false;
        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap b = BitmapFactory.decodeFileDescriptor(fd, null, options);
        return b;
    }

    private static void closeSilently(ParcelFileDescriptor c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Throwable th) {
        }
    }

    private static ParcelFileDescriptor makeInputStream(Uri uri, ContentResolver cr) {
        try {
            return cr.openFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
        } catch (IOException e) {
            return null;
        }
    }

    private static Bitmap transform(Matrix scaler, Bitmap source, int targetWidth, int targetHeight, int options) {
        Bitmap b1;
        boolean scaleUp = (options & 1) != 0;
        boolean recycle = (options & 2) != 0;
        int deltaX = source.getWidth() - targetWidth;
        int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b2);
            int deltaXHalf = Math.max(0, deltaX / 2);
            int deltaYHalf = Math.max(0, deltaY / 2);
            Rect src = new Rect(deltaXHalf, deltaYHalf, Math.min(targetWidth, source.getWidth()) + deltaXHalf, Math.min(targetHeight, source.getHeight()) + deltaYHalf);
            int dstX = (targetWidth - src.width()) / 2;
            int dstY = (targetHeight - src.height()) / 2;
            Rect dst = new Rect(dstX, dstY, targetWidth - dstX, targetHeight - dstY);
            c.drawBitmap(source, src, dst, (Paint) null);
            if (recycle) {
                source.recycle();
            }
            c.setBitmap(null);
            return b2;
        }
        float bitmapWidthF = source.getWidth();
        float bitmapHeightF = source.getHeight();
        float bitmapAspect = bitmapWidthF / bitmapHeightF;
        float viewAspect = targetWidth / targetHeight;
        if (bitmapAspect > viewAspect) {
            float scale = targetHeight / bitmapHeightF;
            if (scale < 0.9f || scale > 1.0f) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        } else {
            float scale2 = targetWidth / bitmapWidthF;
            if (scale2 < 0.9f || scale2 > 1.0f) {
                scaler.setScale(scale2, scale2);
            } else {
                scaler = null;
            }
        }
        if (scaler != null) {
            b1 = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), scaler, true);
        } else {
            b1 = source;
        }
        if (recycle && b1 != source) {
            source.recycle();
        }
        int dx1 = Math.max(0, b1.getWidth() - targetWidth);
        int dy1 = Math.max(0, b1.getHeight() - targetHeight);
        Bitmap b22 = Bitmap.createBitmap(b1, dx1 / 2, dy1 / 2, targetWidth, targetHeight);
        if (b22 != b1 && (recycle || b1 != source)) {
            b1.recycle();
        }
        return b22;
    }

    private static class SizedThumbnailBitmap {
        public Bitmap mBitmap;
        public byte[] mThumbnailData;
        public int mThumbnailHeight;
        public int mThumbnailWidth;

        SizedThumbnailBitmap(SizedThumbnailBitmap sizedThumbnailBitmap) {
            this();
        }

        private SizedThumbnailBitmap() {
        }
    }

    private static void createThumbnailFromEXIF(String filePath, int targetSize, int maxPixels, SizedThumbnailBitmap sizedThumbBitmap) throws Throwable {
        ExifInterface exif;
        if (filePath == null) {
            return;
        }
        byte[] thumbData = null;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            ex = e;
        }
        try {
            thumbData = exif.getThumbnail();
        } catch (IOException e2) {
            ex = e2;
            Log.w(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex);
        }
        BitmapFactory.Options fullOptions = new BitmapFactory.Options();
        BitmapFactory.Options exifOptions = new BitmapFactory.Options();
        int exifThumbWidth = 0;
        if (thumbData != null) {
            exifOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length, exifOptions);
            exifOptions.inSampleSize = adustSampleSize(computeSampleSize(exifOptions, targetSize, maxPixels), exifOptions);
            exifThumbWidth = exifOptions.outWidth / exifOptions.inSampleSize;
        }
        fullOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, fullOptions);
        if (fullOptions.outWidth == -1 || fullOptions.outHeight == -1) {
            sizedThumbBitmap.mBitmap = null;
            return;
        }
        fullOptions.inSampleSize = adustSampleSize(computeSampleSize(fullOptions, targetSize, maxPixels), fullOptions);
        int fullThumbWidth = fullOptions.outWidth / fullOptions.inSampleSize;
        if (thumbData != null && exifThumbWidth >= fullThumbWidth) {
            int width = exifOptions.outWidth;
            int height = exifOptions.outHeight;
            exifOptions.inJustDecodeBounds = false;
            sizedThumbBitmap.mBitmap = BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length, exifOptions);
            if (sizedThumbBitmap.mBitmap == null) {
                return;
            }
            sizedThumbBitmap.mThumbnailData = thumbData;
            sizedThumbBitmap.mThumbnailWidth = width;
            sizedThumbBitmap.mThumbnailHeight = height;
            return;
        }
        fullOptions.inJustDecodeBounds = false;
        sizedThumbBitmap.mBitmap = BitmapFactory.decodeFile(filePath, fullOptions);
    }

    public static Bitmap extractBufferThumbnail(byte[] source, int srcWidth, int srcHeight, int dstWidth, int dstHeight, int options) {
        float scale;
        if (source == null) {
            return null;
        }
        if (srcWidth < srcHeight) {
            scale = dstWidth / srcWidth;
        } else {
            scale = dstHeight / srcHeight;
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        RectF srcR = new RectF(0.0f, 0.0f, srcWidth, srcHeight);
        RectF deviceR = new RectF();
        matrix.mapRect(deviceR, srcR);
        if (deviceR.width() * deviceR.height() >= 1048576.0f) {
            Bitmap thumbnail = transformBuffer(matrix, source, srcWidth, srcHeight, dstWidth, dstHeight, options);
            return thumbnail;
        }
        Bitmap thumbnail2 = transformBuffer(matrix, source, srcWidth, srcHeight, dstWidth, dstHeight, options | 1);
        return thumbnail2;
    }

    private static Bitmap transformBuffer(Matrix scaler, byte[] source, int srcWidth, int srcHeight, int targetWidth, int targetHeight, int options) {
        Rect src;
        boolean scaleUp = (options & 1) != 0;
        int deltaX = srcWidth - targetWidth;
        int deltaY = srcHeight - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b2);
            int deltaXHalf = Math.max(0, deltaX / 2);
            int deltaYHalf = Math.max(0, deltaY / 2);
            Rect src2 = new Rect(deltaXHalf, deltaYHalf, Math.min(targetWidth, srcWidth) + deltaXHalf, Math.min(targetHeight, srcHeight) + deltaYHalf);
            int dstX = (targetWidth - src2.width()) / 2;
            int dstY = (targetHeight - src2.height()) / 2;
            Rect dst = new Rect(dstX, dstY, targetWidth - dstX, targetHeight - dstY);
            BitmapFactory.Options Options = new BitmapFactory.Options();
            Options.inDither = false;
            Options.inSampleSize = 1;
            Options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap b1 = null;
            if (source != null) {
                b1 = BitmapFactory.decodeByteArray(source, 0, source.length, Options);
            }
            if (b1 == null) {
                return null;
            }
            c.drawBitmap(b1, src2, dst, (Paint) null);
            b1.recycle();
            return b2;
        }
        float bitmapWidthF = srcWidth;
        float bitmapHeightF = srcHeight;
        float bitmapAspect = bitmapWidthF / bitmapHeightF;
        float viewAspect = targetWidth / targetHeight;
        float finalScale = 1.0f;
        if (bitmapAspect > viewAspect) {
            float scale = targetHeight / bitmapHeightF;
            if (scale < 0.9f || scale > 1.0f) {
                scaler.setScale(scale, scale);
                finalScale = scale;
            }
        } else {
            float scale2 = targetWidth / bitmapWidthF;
            if (scale2 < 0.9f || scale2 > 1.0f) {
                scaler.setScale(scale2, scale2);
                finalScale = scale2;
            }
        }
        Bitmap b12 = null;
        BitmapFactory.Options Options2 = new BitmapFactory.Options();
        Options2.inDither = false;
        Options2.inPreferredConfig = Bitmap.Config.ARGB_8888;
        int inPreferSize = (int) Math.max(srcWidth * finalScale, srcHeight * finalScale);
        if (source != null) {
            b12 = BitmapFactory.decodeByteArray(source, 0, source.length, Options2);
        }
        if (b12 == null) {
            return null;
        }
        int scaledBitmapWidth = b12.getWidth();
        int scaledBitmapHeight = b12.getHeight();
        Bitmap b22 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas c2 = new Canvas(b22);
        RectF dst2 = new RectF(0.0f, 0.0f, targetWidth, targetHeight);
        int maxSize = scaledBitmapWidth > scaledBitmapHeight ? scaledBitmapWidth : scaledBitmapHeight;
        if (maxSize == inPreferSize) {
            int croppedX = Math.max(0, (scaledBitmapWidth - targetWidth) / 2);
            int croppedY = Math.max(0, (scaledBitmapHeight - targetHeight) / 2);
            src = new Rect(croppedX, croppedY, Math.min(croppedX + targetWidth, scaledBitmapWidth), Math.min(croppedY + targetHeight, scaledBitmapHeight));
        } else {
            int croppedX2 = Math.max(0, (srcWidth - ((int) (targetWidth / finalScale))) / 2);
            int croppedY2 = Math.max(0, (srcHeight - ((int) (targetHeight / finalScale))) / 2);
            src = new Rect(croppedX2, croppedY2, Math.min(((int) (targetWidth / finalScale)) + croppedX2, srcWidth), Math.min(((int) (targetHeight / finalScale)) + croppedY2, srcHeight));
        }
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        c2.drawBitmap(b12, src, dst2, paint);
        b12.recycle();
        return b22;
    }

    private static int adustSampleSize(int inSampleSize, BitmapFactory.Options options) {
        if (inSampleSize < 1 || options == null) {
            return 1;
        }
        int imageShortterDimension = options.outWidth < options.outHeight ? options.outWidth : options.outHeight;
        while (inSampleSize > 1 && imageShortterDimension / inSampleSize < 96) {
            inSampleSize >>= 1;
        }
        return inSampleSize;
    }
}
