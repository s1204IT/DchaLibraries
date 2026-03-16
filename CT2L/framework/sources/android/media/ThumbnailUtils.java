package android.media;

import android.app.backup.FullBackup;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaFile;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class ThumbnailUtils {
    private static final int MAX_NUM_PIXELS_MICRO_THUMBNAIL = 19200;
    private static final int MAX_NUM_PIXELS_THUMBNAIL = 196608;
    private static final int OPTIONS_NONE = 0;
    public static final int OPTIONS_RECYCLE_INPUT = 2;
    private static final int OPTIONS_SCALE_UP = 1;
    private static final String TAG = "ThumbnailUtils";
    public static final int TARGET_SIZE_MICRO_THUMBNAIL = 96;
    public static final int TARGET_SIZE_MINI_THUMBNAIL = 320;
    private static final int UNCONSTRAINED = -1;

    public static Bitmap createImageThumbnail(String filePath, int kind) throws Throwable {
        FileInputStream stream;
        boolean wantMini = kind == 1;
        int targetSize = wantMini ? 320 : 96;
        int maxPixels = wantMini ? 196608 : MAX_NUM_PIXELS_MICRO_THUMBNAIL;
        SizedThumbnailBitmap sizedThumbnailBitmap = new SizedThumbnailBitmap();
        Bitmap bitmap = null;
        MediaFile.MediaFileType fileType = MediaFile.getFileType(filePath);
        if (fileType != null && fileType.fileType == 31) {
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
            } catch (IOException e) {
                ex = e;
            } catch (OutOfMemoryError e2) {
                oom = e2;
            }
            try {
                FileDescriptor fd = stream.getFD();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFileDescriptor(fd, null, options);
                if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
                    if (stream == null) {
                        return null;
                    }
                    try {
                        stream.close();
                        return null;
                    } catch (IOException ex) {
                        Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex);
                        return null;
                    }
                }
                options.inSampleSize = computeSampleSize(options, targetSize, maxPixels);
                options.inJustDecodeBounds = false;
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
            } catch (IOException e3) {
                ex = e3;
                stream2 = stream;
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex);
                if (stream2 != null) {
                    try {
                        stream2.close();
                    } catch (IOException ex3) {
                        Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, ex3);
                    }
                }
            } catch (OutOfMemoryError e4) {
                oom = e4;
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
        }
        if (kind == 3) {
            bitmap = extractThumbnail(bitmap, 96, 96, 2);
        }
        return bitmap;
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
        if (kind == 1) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int max = Math.max(width, height);
            if (max > 512) {
                float scale = 512.0f / max;
                int w = Math.round(width * scale);
                int h = Math.round(height * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
            }
        } else if (kind == 3) {
            bitmap = extractThumbnail(bitmap, 96, 96, 2);
        }
        return bitmap;
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
        return transform(matrix, source, width, height, options | 1);
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
        if (upperBound >= lowerBound) {
            if (maxNumOfPixels == -1 && minSideLength == -1) {
                return 1;
            }
            if (minSideLength != -1) {
                return upperBound;
            }
            return lowerBound;
        }
        return lowerBound;
    }

    private static Bitmap makeBitmap(int minSideLength, int maxNumOfPixels, Uri uri, ContentResolver cr, ParcelFileDescriptor pfd, BitmapFactory.Options options) {
        try {
            if (pfd == null) {
                pfd = makeInputStream(uri, cr);
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
        } catch (OutOfMemoryError ex) {
            Log.e(TAG, "Got oom exception ", ex);
            return null;
        } finally {
            closeSilently(pfd);
        }
    }

    private static void closeSilently(ParcelFileDescriptor c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable th) {
            }
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
        Bitmap b2;
        boolean scaleUp = (options & 1) != 0;
        boolean recycle = (options & 2) != 0;
        int deltaX = source.getWidth() - targetWidth;
        int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            b2 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
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
        } else {
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
            b2 = Bitmap.createBitmap(b1, dx1 / 2, dy1 / 2, targetWidth, targetHeight);
            if (b2 != b1 && (recycle || b1 != source)) {
                b1.recycle();
            }
        }
        return b2;
    }

    private static class SizedThumbnailBitmap {
        public Bitmap mBitmap;
        public byte[] mThumbnailData;
        public int mThumbnailHeight;
        public int mThumbnailWidth;

        private SizedThumbnailBitmap() {
        }
    }

    private static void createThumbnailFromEXIF(String filePath, int targetSize, int maxPixels, SizedThumbnailBitmap sizedThumbBitmap) throws Throwable {
        ExifInterface exif;
        if (filePath != null) {
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
                Log.w(TAG, ex);
            }
            BitmapFactory.Options fullOptions = new BitmapFactory.Options();
            BitmapFactory.Options exifOptions = new BitmapFactory.Options();
            int exifThumbWidth = 0;
            if (thumbData != null) {
                exifOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length, exifOptions);
                exifOptions.inSampleSize = computeSampleSize(exifOptions, targetSize, maxPixels);
                exifThumbWidth = exifOptions.outWidth / exifOptions.inSampleSize;
            }
            fullOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, fullOptions);
            fullOptions.inSampleSize = computeSampleSize(fullOptions, targetSize, maxPixels);
            int fullThumbWidth = fullOptions.outWidth / fullOptions.inSampleSize;
            if (thumbData != null && exifThumbWidth >= fullThumbWidth) {
                int width = exifOptions.outWidth;
                int height = exifOptions.outHeight;
                exifOptions.inJustDecodeBounds = false;
                sizedThumbBitmap.mBitmap = BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length, exifOptions);
                if (sizedThumbBitmap.mBitmap != null) {
                    sizedThumbBitmap.mThumbnailData = thumbData;
                    sizedThumbBitmap.mThumbnailWidth = width;
                    sizedThumbBitmap.mThumbnailHeight = height;
                    return;
                }
                return;
            }
            fullOptions.inJustDecodeBounds = false;
            sizedThumbBitmap.mBitmap = BitmapFactory.decodeFile(filePath, fullOptions);
        }
    }
}
