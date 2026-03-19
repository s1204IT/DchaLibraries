package com.mediatek.camera.common.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.CameraProfile;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.exif.ExifInterface;
import com.mediatek.camera.portability.SystemProperties;
import com.mediatek.camera.portability.storage.StorageManagerExt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

public class BitmapCreator {
    private static final String DCIM_CAMERA_FOLDER_ABSOLUTE_PATH;
    private static Method sGetDefaultPath;
    private static String sMountPoint;
    private static Uri sUri;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(BitmapCreator.class.getSimpleName());
    private static final String FOLDER_PATH = "/" + Environment.DIRECTORY_DCIM + "/Camera";

    static {
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
        sb.append("/Camera");
        DCIM_CAMERA_FOLDER_ABSOLUTE_PATH = sb.toString();
        sUri = null;
        try {
            Class<?> cls = Class.forName("com.mediatek.storage.StorageManagerEx");
            if (cls != null) {
                sGetDefaultPath = cls.getDeclaredMethod("getDefaultPath", new Class[0]);
            }
            if (sGetDefaultPath != null) {
                sGetDefaultPath.setAccessible(true);
            }
        } catch (ClassNotFoundException e) {
            LogHelper.e(TAG, "ClassNotFoundException: com.mediatek.storage.StorageManagerEx");
        } catch (NoSuchMethodException e2) {
            LogHelper.e(TAG, "NoSuchMethodException: getDefaultPath");
        }
    }

    private static class Media {
        public final long dateTaken;
        public final String filePath;
        public final long id;
        public final int mediaType;
        public final int orientation;
        public final Uri uri;

        public Media(long j, int i, long j2, Uri uri, int i2, String str) {
            this.id = j;
            this.orientation = i;
            this.dateTaken = j2;
            this.uri = uri;
            this.mediaType = i2;
            this.filePath = str;
        }

        public String toString() {
            return "Media(id=" + this.id + ", orientation=" + this.orientation + ", dateTaken=" + this.dateTaken + ", uri=" + this.uri + ", mediaType=" + this.mediaType + ", filePath=" + this.filePath + ")";
        }
    }

    public static Bitmap createBitmapFromJpeg(byte[] bArr, int i) {
        LogHelper.d(TAG, "[createBitmapFromJpeg] jpeg = " + bArr + ", targetWidth = " + i);
        if (bArr == null) {
            return null;
        }
        ExifInterface exif = getExif(bArr);
        int jpegOrientation = getJpegOrientation(exif);
        if (exif != null && exif.hasThumbnail() && exif.getThumbnailBitmap() != null) {
            LogHelper.d(TAG, "create bitmap from exif thumbnail");
            return rotateBitmap(exif.getThumbnailBitmap(), jpegOrientation);
        }
        int iHighestOneBit = Integer.highestOneBit((int) Math.ceil(((double) getJpegWidth(exif)) / ((double) i)));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = iHighestOneBit;
        try {
            Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options);
            LogHelper.d(TAG, "[createBitmapFromJpeg] end");
            return rotateBitmap(bitmapDecodeByteArray, jpegOrientation);
        } catch (OutOfMemoryError e) {
            LogHelper.e(TAG, "createBitmapFromJpeg fail", e);
            return null;
        }
    }

    public static Bitmap createBitmapFromYuv(byte[] bArr, int i, int i2, int i3, int i4, int i5) throws Throwable {
        LogHelper.d(TAG, "[createBitmapFromYuv] yuvData = " + bArr + ", yuvWidth = " + i2 + ", yuvHeight = " + i3 + ", orientation = " + i5 + ", imageFormat = " + i);
        if (isNeedDumpYuv()) {
            dumpYuv("/sdcard/postView.yuv", bArr);
        }
        if (bArr == null) {
            return null;
        }
        byte[] bArrCovertYuvDataToJpeg = covertYuvDataToJpeg(bArr, i, i2, i3);
        int iHighestOneBit = Integer.highestOneBit((int) Math.ceil(((double) Math.min(i2, i3)) / ((double) i4)));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = iHighestOneBit;
        try {
            Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArrCovertYuvDataToJpeg, 0, bArrCovertYuvDataToJpeg.length, options);
            LogHelper.d(TAG, "[createBitmapFromYuv] end");
            return rotateBitmap(bitmapDecodeByteArray, i5);
        } catch (OutOfMemoryError e) {
            LogHelper.e(TAG, "createBitmapFromYuv fail", e);
            return null;
        }
    }

    public static Bitmap createBitmapFromVideo(String str, int i) {
        return createBitmapFromVideo(str, null, i);
    }

    public static Bitmap createBitmapFromVideo(FileDescriptor fileDescriptor, int i) {
        return createBitmapFromVideo(null, fileDescriptor, i);
    }

    public static Bitmap getLastBitmapFromDatabase(ContentResolver contentResolver) throws Throwable {
        Cursor cursorQuery;
        Media media;
        Bitmap thumbnail;
        LogHelper.d(TAG, "getLastBitmapFromDatabase() begin.");
        Uri contentUri = MediaStore.Files.getContentUri("external");
        try {
            cursorQuery = contentResolver.query(contentUri.buildUpon().appendQueryParameter("limit", "1").build(), new String[]{"_id", "orientation", "datetaken", "_data", "media_type"}, "((media_type=" + Integer.toString(1) + " OR media_type=" + Integer.toString(3) + " ) AND bucket_id=" + getBucketId() + ")", null, "datetaken DESC,_id DESC");
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        long j = cursorQuery.getLong(0);
                        media = new Media(j, cursorQuery.getInt(1), cursorQuery.getLong(2), ContentUris.withAppendedId(contentUri, j), cursorQuery.getInt(4), cursorQuery.getString(3));
                    } else {
                        media = null;
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
            LogHelper.d(TAG, "getLastBitmapFromDatabase() media=" + media);
            if (media == null) {
                sUri = null;
                return null;
            }
            int i = media.orientation;
            try {
                if (media.mediaType != 1) {
                    if (media.mediaType == 3) {
                        thumbnail = MediaStore.Video.Thumbnails.getThumbnail(contentResolver, media.id, 1, null);
                        sUri = ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri("external"), media.id);
                        i = 0;
                    } else {
                        thumbnail = null;
                    }
                } else {
                    ExifInterface exif = getExif(media.filePath);
                    if (exif == null || !exif.hasThumbnail() || exif.getThumbnailBitmap() == null) {
                        thumbnail = MediaStore.Images.Thumbnails.getThumbnail(contentResolver, media.id, 1, null);
                    } else {
                        LogHelper.d(TAG, "get bitmap from exif thumbnail");
                        thumbnail = exif.getThumbnailBitmap();
                    }
                    sUri = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri("external"), media.id);
                }
                return rotateBitmap(thumbnail, i);
            } catch (OutOfMemoryError e) {
                LogHelper.e(TAG, "getThumbnail fail", e);
                LogHelper.d(TAG, "Quit getLastBitmap");
                return null;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }

    public static Uri getUriAfterQueryDb() {
        return sUri;
    }

    public static byte[] covertYuvDataToJpeg(byte[] bArr, int i, int i2, int i3) {
        Rect rect = new Rect(0, 0, i2, i3);
        YuvImage yuvImage = new YuvImage(bArr, i, i2, i3, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(rect, CameraProfile.getJpegEncodingQualityParameter(2), byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int i) {
        if (i != 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(i, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
            try {
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            } catch (IllegalArgumentException e) {
                LogHelper.w(TAG, "Failed to rotate bitmap", e);
            }
        }
        return bitmap;
    }

    private static ExifInterface getExif(byte[] bArr) {
        if (bArr != null) {
            ExifInterface exifInterface = new ExifInterface();
            try {
                exifInterface.readExif(bArr);
            } catch (IOException e) {
                LogHelper.w(TAG, "Failed to read EXIF data", e);
            }
            return exifInterface;
        }
        LogHelper.w(TAG, "JPEG data is null, can not get exif");
        return null;
    }

    private static ExifInterface getExif(String str) {
        if (str != null) {
            ExifInterface exifInterface = new ExifInterface();
            try {
                exifInterface.readExif(str);
            } catch (IOException e) {
                LogHelper.w(TAG, "Failed to read EXIF data", e);
            }
            return exifInterface;
        }
        LogHelper.w(TAG, "filePath is null, can not get exif");
        return null;
    }

    private static int getJpegOrientation(ExifInterface exifInterface) {
        if (exifInterface != null) {
            Integer tagIntValue = exifInterface.getTagIntValue(ExifInterface.TAG_ORIENTATION);
            if (tagIntValue == null) {
                return 0;
            }
            return ExifInterface.getRotationForOrientationValue(tagIntValue.shortValue());
        }
        LogHelper.w(TAG, "exif is null, can not get JpegOrientation");
        return 0;
    }

    private static int getJpegWidth(ExifInterface exifInterface) {
        int iIntValue;
        int iIntValue2 = 0;
        if (exifInterface != null) {
            Integer tagIntValue = exifInterface.getTagIntValue(ExifInterface.TAG_IMAGE_WIDTH);
            if (tagIntValue != null) {
                iIntValue = tagIntValue.intValue();
            } else {
                iIntValue = 0;
            }
            Integer tagIntValue2 = exifInterface.getTagIntValue(ExifInterface.TAG_IMAGE_LENGTH);
            if (tagIntValue2 != null) {
                iIntValue2 = tagIntValue2.intValue();
            }
            return Math.min(iIntValue, iIntValue2);
        }
        LogHelper.w(TAG, "exif is null, can not get JpegWidth");
        return 0;
    }

    private static Bitmap createBitmapFromVideo(String str, FileDescriptor fileDescriptor, int i) throws IOException {
        Bitmap frameAtTime;
        LogHelper.d(TAG, "[createBitmapFromVideo] filePath = " + str + ", targetWidth = " + i);
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            try {
                try {
                    try {
                        if (str != null) {
                            mediaMetadataRetriever.setDataSource(str);
                        } else {
                            mediaMetadataRetriever.setDataSource(fileDescriptor);
                        }
                        frameAtTime = mediaMetadataRetriever.getFrameAtTime(-1L);
                        try {
                            mediaMetadataRetriever.release();
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    } catch (IllegalArgumentException e2) {
                        e2.printStackTrace();
                        mediaMetadataRetriever.release();
                        frameAtTime = null;
                        if (frameAtTime != null) {
                        }
                    }
                } catch (RuntimeException e3) {
                    e3.printStackTrace();
                    mediaMetadataRetriever.release();
                    frameAtTime = null;
                    if (frameAtTime != null) {
                    }
                }
            } catch (RuntimeException e4) {
                e4.printStackTrace();
                frameAtTime = null;
                if (frameAtTime != null) {
                }
            }
            if (frameAtTime != null) {
                return null;
            }
            int width = frameAtTime.getWidth();
            int height = frameAtTime.getHeight();
            LogHelper.v(TAG, "[createBitmapFromVideo] bitmap = " + width + "x" + height);
            if (width <= i) {
                return frameAtTime;
            }
            float f = width;
            float f2 = i / f;
            int iRound = Math.round(f * f2);
            int iRound2 = Math.round(f2 * height);
            LogHelper.v(TAG, "[createBitmapFromVideo] w = " + iRound + "h" + iRound2);
            return Bitmap.createScaledBitmap(frameAtTime, iRound, iRound2, true);
        } catch (Throwable th) {
            try {
                mediaMetadataRetriever.release();
            } catch (RuntimeException e5) {
                e5.printStackTrace();
            }
            throw th;
        }
    }

    private static boolean isNeedDumpYuv() {
        boolean z = false;
        if (SystemProperties.getInt("vendor.debug.thumbnailFromYuv.enable", 0) == 1) {
            z = true;
        }
        LogHelper.d(TAG, "[isNeedDumpYuv] return :" + z);
        return z;
    }

    private static void dumpYuv(String str, byte[] bArr) throws Throwable {
        FileOutputStream fileOutputStream;
        FileOutputStream fileOutputStream2 = null;
        try {
            try {
                try {
                    LogHelper.d(TAG, "[dumpYuv] begin");
                    fileOutputStream = new FileOutputStream(str);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e2) {
            LogHelper.e(TAG, "[dumpYuv]IOException:", e2);
        }
        try {
            fileOutputStream.write(bArr);
            fileOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e3) {
            e = e3;
            fileOutputStream2 = fileOutputStream;
            LogHelper.e(TAG, "[dumpYuv]Failed to write image,ex:", e);
            if (fileOutputStream2 != null) {
                fileOutputStream2.close();
            }
            LogHelper.d(TAG, "[dumpYuv] end");
        } catch (Throwable th2) {
            th = th2;
            fileOutputStream2 = fileOutputStream;
            if (fileOutputStream2 != null) {
                try {
                    fileOutputStream2.close();
                } catch (IOException e4) {
                    LogHelper.e(TAG, "[dumpYuv]IOException:", e4);
                }
            }
            throw th;
        }
        LogHelper.d(TAG, "[dumpYuv] end");
    }

    private static String getBucketId() {
        String fileDirectory = getFileDirectory();
        LogHelper.d(TAG, "getBucketId directory = " + fileDirectory);
        return String.valueOf(fileDirectory.toLowerCase(Locale.ENGLISH).hashCode());
    }

    private static String getFileDirectory() {
        if (isExtendStorageCanUsed()) {
            return sMountPoint + FOLDER_PATH;
        }
        return DCIM_CAMERA_FOLDER_ABSOLUTE_PATH;
    }

    private static boolean isExtendStorageCanUsed() {
        return Build.VERSION.SDK_INT >= 23 && isDefaultPathCanUsed();
    }

    private static boolean isDefaultPathCanUsed() {
        boolean z = false;
        if (sGetDefaultPath != null) {
            try {
                sMountPoint = StorageManagerExt.getDefaultPath();
                File file = new File(sMountPoint + FOLDER_PATH);
                file.mkdirs();
                boolean zIsDirectory = file.isDirectory();
                boolean zCanWrite = file.canWrite();
                if (zIsDirectory && zCanWrite) {
                    z = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LogHelper.d(TAG, "[isDefaultPathCanUsed] isDefaultPathCanUsed = " + z);
        return z;
    }
}
