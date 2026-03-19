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
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.mediatek.media.MediaFactory;
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

    public static Bitmap createImageThumbnail(String str, int i) throws Throwable {
        Bitmap bitmapCreateThumbnailFromMetadataRetriever;
        FileInputStream fileInputStream;
        Bitmap bitmapDecodeFileDescriptor;
        ?? r2 = i == 1;
        int i2 = r2 != false ? 320 : 96;
        int i3 = r2 != false ? 196608 : MAX_NUM_PIXELS_MICRO_THUMBNAIL;
        ?? r6 = 0;
        FileInputStream fileInputStream2 = null;
        SizedThumbnailBitmap sizedThumbnailBitmap = new SizedThumbnailBitmap();
        MediaFile.MediaFileType fileType = MediaFile.getFileType(str);
        if (fileType == null) {
            bitmapCreateThumbnailFromMetadataRetriever = null;
        } else if (fileType.fileType == 401 || MediaFile.isRawImageFileType(fileType.fileType)) {
            createThumbnailFromEXIF(str, i2, i3, sizedThumbnailBitmap);
            bitmapCreateThumbnailFromMetadataRetriever = sizedThumbnailBitmap.mBitmap;
        } else if (fileType.fileType == 407) {
            bitmapCreateThumbnailFromMetadataRetriever = createThumbnailFromMetadataRetriever(str, i2, i3);
        }
        try {
            try {
            } catch (Throwable th) {
                th = th;
                fileInputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
        if (bitmapCreateThumbnailFromMetadataRetriever == null) {
            try {
                fileInputStream = new FileInputStream(str);
            } catch (IOException e2) {
                e = e2;
            } catch (OutOfMemoryError e3) {
                e = e3;
            }
            try {
                FileDescriptor fd = fileInputStream.getFD();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFileDescriptor(fd, null, options);
                if (!options.mCancel && options.outWidth != -1 && options.outHeight != -1) {
                    options.inSampleSize = computeSampleSize(options, i2, i3);
                    options.inJustDecodeBounds = false;
                    MediaFactory.getInstance().getThumbnailUtilsEx().correctOptions(str, options);
                    options.inDither = false;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    bitmapDecodeFileDescriptor = BitmapFactory.decodeFileDescriptor(fd, null, options);
                    try {
                        fileInputStream.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "", e4);
                    }
                }
                try {
                    fileInputStream.close();
                } catch (IOException e5) {
                    Log.e(TAG, "", e5);
                }
                return null;
            } catch (IOException e6) {
                e = e6;
                fileInputStream2 = fileInputStream;
                Log.e(TAG, "", e);
                if (fileInputStream2 != null) {
                    fileInputStream2.close();
                }
                bitmapDecodeFileDescriptor = bitmapCreateThumbnailFromMetadataRetriever;
            } catch (OutOfMemoryError e7) {
                e = e7;
                r6 = fileInputStream;
                Log.e(TAG, "Unable to decode file " + str + ". OutOfMemoryError.", e);
                if (r6 != 0) {
                    r6.close();
                }
                bitmapDecodeFileDescriptor = bitmapCreateThumbnailFromMetadataRetriever;
            } catch (Throwable th2) {
                th = th2;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e8) {
                        Log.e(TAG, "", e8);
                    }
                }
                throw th;
            }
        } else {
            bitmapDecodeFileDescriptor = bitmapCreateThumbnailFromMetadataRetriever;
        }
        return i == 3 ? extractThumbnail(bitmapDecodeFileDescriptor, 96, 96, 2) : bitmapDecodeFileDescriptor;
    }

    public static Bitmap createVideoThumbnail(String str, int i) {
        Bitmap frameAtTime;
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            try {
                try {
                    mediaMetadataRetriever.setDataSource(str);
                    frameAtTime = mediaMetadataRetriever.getFrameAtTime(-1L);
                    try {
                        mediaMetadataRetriever.release();
                    } catch (RuntimeException e) {
                    }
                } catch (RuntimeException e2) {
                    frameAtTime = null;
                    if (frameAtTime != null) {
                    }
                }
            } catch (RuntimeException e3) {
                mediaMetadataRetriever.release();
                frameAtTime = null;
                if (frameAtTime != null) {
                }
            }
        } catch (IllegalArgumentException e4) {
            mediaMetadataRetriever.release();
            frameAtTime = null;
            if (frameAtTime != null) {
            }
        } catch (Throwable th) {
            try {
                mediaMetadataRetriever.release();
            } catch (RuntimeException e5) {
            }
            throw th;
        }
        if (frameAtTime != null) {
            return null;
        }
        if (i != 1) {
            return i == 3 ? extractThumbnail(frameAtTime, 96, 96, 2) : frameAtTime;
        }
        int width = frameAtTime.getWidth();
        int height = frameAtTime.getHeight();
        int iMax = Math.max(width, height);
        if (iMax <= 512) {
            return frameAtTime;
        }
        float f = 512.0f / iMax;
        return Bitmap.createScaledBitmap(frameAtTime, Math.round(width * f), Math.round(f * height), true);
    }

    public static Bitmap extractThumbnail(Bitmap bitmap, int i, int i2) {
        return extractThumbnail(bitmap, i, i2, 0);
    }

    public static Bitmap extractThumbnail(Bitmap bitmap, int i, int i2, int i3) {
        float height;
        if (bitmap == null) {
            return null;
        }
        if (bitmap.getWidth() < bitmap.getHeight()) {
            height = i / bitmap.getWidth();
        } else {
            height = i2 / bitmap.getHeight();
        }
        Matrix matrix = new Matrix();
        matrix.setScale(height, height);
        return transform(matrix, bitmap, i, i2, i3 | 1);
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
        if (i2 != -1) {
            iCeil = (int) Math.ceil(Math.sqrt((d * d2) / ((double) i2)));
        } else {
            iCeil = 1;
        }
        if (i == -1) {
            iMin = 128;
        } else {
            double d3 = i;
            iMin = (int) Math.min(Math.floor(d / d3), Math.floor(d2 / d3));
        }
        if (iMin < iCeil) {
            return iCeil;
        }
        if (i2 == -1 && i == -1) {
            return 1;
        }
        if (i == -1) {
            return iCeil;
        }
        return iMin;
    }

    private static Bitmap makeBitmap(int i, int i2, Uri uri, ContentResolver contentResolver, ParcelFileDescriptor parcelFileDescriptor, BitmapFactory.Options options) {
        if (parcelFileDescriptor == null) {
            try {
                parcelFileDescriptor = makeInputStream(uri, contentResolver);
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Got oom exception ", e);
                return null;
            } finally {
                closeSilently(parcelFileDescriptor);
            }
        }
        if (parcelFileDescriptor == null) {
            return null;
        }
        if (options == null) {
            options = new BitmapFactory.Options();
        }
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        if (!options.mCancel && options.outWidth != -1 && options.outHeight != -1) {
            options.inSampleSize = computeSampleSize(options, i, i2);
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
        }
        return null;
    }

    private static void closeSilently(ParcelFileDescriptor parcelFileDescriptor) {
        if (parcelFileDescriptor == null) {
            return;
        }
        try {
            parcelFileDescriptor.close();
        } catch (Throwable th) {
        }
    }

    private static ParcelFileDescriptor makeInputStream(Uri uri, ContentResolver contentResolver) {
        try {
            return contentResolver.openFileDescriptor(uri, FullBackup.ROOT_TREE_TOKEN);
        } catch (IOException e) {
            return null;
        }
    }

    private static Bitmap transform(Matrix matrix, Bitmap bitmap, int i, int i2, int i3) {
        Matrix matrix2;
        Bitmap bitmapCreateBitmap;
        Bitmap bitmapCreateBitmap2;
        Matrix matrix3 = matrix;
        boolean z = (i3 & 1) != 0;
        boolean z2 = (i3 & 2) != 0;
        int width = bitmap.getWidth() - i;
        int height = bitmap.getHeight() - i2;
        if (!z && (width < 0 || height < 0)) {
            Bitmap bitmapCreateBitmap3 = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap3);
            int iMax = Math.max(0, width / 2);
            int iMax2 = Math.max(0, height / 2);
            Rect rect = new Rect(iMax, iMax2, Math.min(i, bitmap.getWidth()) + iMax, Math.min(i2, bitmap.getHeight()) + iMax2);
            int iWidth = (i - rect.width()) / 2;
            int iHeight = (i2 - rect.height()) / 2;
            canvas.drawBitmap(bitmap, rect, new Rect(iWidth, iHeight, i - iWidth, i2 - iHeight), (Paint) null);
            if (z2) {
                bitmap.recycle();
            }
            canvas.setBitmap(null);
            return bitmapCreateBitmap3;
        }
        float width2 = bitmap.getWidth();
        float height2 = bitmap.getHeight();
        float f = i;
        float f2 = i2;
        if (width2 / height2 > f / f2) {
            float f3 = f2 / height2;
            if (f3 < 0.9f || f3 > 1.0f) {
                matrix3.setScale(f3, f3);
            } else {
                matrix3 = null;
            }
        } else {
            float f4 = f / width2;
            if (f4 < 0.9f || f4 > 1.0f) {
                matrix3.setScale(f4, f4);
            } else {
                matrix2 = null;
                if (matrix2 == null) {
                    bitmapCreateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix2, true);
                } else {
                    bitmapCreateBitmap = bitmap;
                }
                if (z2 && bitmapCreateBitmap != bitmap) {
                    bitmap.recycle();
                }
                bitmapCreateBitmap2 = Bitmap.createBitmap(bitmapCreateBitmap, Math.max(0, bitmapCreateBitmap.getWidth() - i) / 2, Math.max(0, bitmapCreateBitmap.getHeight() - i2) / 2, i, i2);
                if (bitmapCreateBitmap2 != bitmapCreateBitmap && (z2 || bitmapCreateBitmap != bitmap)) {
                    bitmapCreateBitmap.recycle();
                }
                return bitmapCreateBitmap2;
            }
        }
        matrix2 = matrix3;
        if (matrix2 == null) {
        }
        if (z2) {
            bitmap.recycle();
        }
        bitmapCreateBitmap2 = Bitmap.createBitmap(bitmapCreateBitmap, Math.max(0, bitmapCreateBitmap.getWidth() - i) / 2, Math.max(0, bitmapCreateBitmap.getHeight() - i2) / 2, i, i2);
        if (bitmapCreateBitmap2 != bitmapCreateBitmap) {
            bitmapCreateBitmap.recycle();
        }
        return bitmapCreateBitmap2;
    }

    private static class SizedThumbnailBitmap {
        public Bitmap mBitmap;
        public byte[] mThumbnailData;
        public int mThumbnailHeight;
        public int mThumbnailWidth;

        private SizedThumbnailBitmap() {
        }
    }

    private static void createThumbnailFromEXIF(String str, int i, int i2, SizedThumbnailBitmap sizedThumbnailBitmap) throws Throwable {
        int i3;
        if (str == null) {
            return;
        }
        byte[] thumbnail = null;
        try {
            thumbnail = new ExifInterface(str).getThumbnail();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        BitmapFactory.Options options2 = new BitmapFactory.Options();
        if (thumbnail != null) {
            options2.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length, options2);
            options2.inSampleSize = computeSampleSize(options2, i, i2);
            i3 = options2.outWidth / options2.inSampleSize;
        } else {
            i3 = 0;
        }
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(str, options);
        options.inSampleSize = computeSampleSize(options, i, i2);
        int i4 = options.outWidth / options.inSampleSize;
        if (thumbnail != null && i3 >= i4) {
            int i5 = options2.outWidth;
            int i6 = options2.outHeight;
            options2.inJustDecodeBounds = false;
            sizedThumbnailBitmap.mBitmap = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length, options2);
            if (sizedThumbnailBitmap.mBitmap != null) {
                sizedThumbnailBitmap.mThumbnailData = thumbnail;
                sizedThumbnailBitmap.mThumbnailWidth = i5;
                sizedThumbnailBitmap.mThumbnailHeight = i6;
                return;
            }
            return;
        }
        options.inJustDecodeBounds = false;
        sizedThumbnailBitmap.mBitmap = BitmapFactory.decodeFile(str, options);
    }

    private static Bitmap createThumbnailFromMetadataRetriever(String str, int i, int i2) {
        if (str == null) {
            return null;
        }
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(str);
            MediaMetadataRetriever.BitmapParams bitmapParams = new MediaMetadataRetriever.BitmapParams();
            bitmapParams.setPreferredConfig(Bitmap.Config.ARGB_8888);
            return mediaMetadataRetriever.getThumbnailImageAtIndex(-1, bitmapParams, i, i2);
        } catch (RuntimeException e) {
            return null;
        } finally {
            mediaMetadataRetriever.release();
        }
    }
}
