package com.mediatek.camera.common.mode.photo;

import android.content.ContentValues;
import android.location.Location;
import android.media.Image;
import android.os.Environment;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.photo.heif.HeifWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HeifHelper {
    private ICameraContext mICameraContext;
    private ImageFileName mImageFileName = new ImageFileName("'IMG'_yyyyMMdd_HHmmss_S");
    private static final LogUtil.Tag TAG = new LogUtil.Tag(HeifHelper.class.getSimpleName());
    public static int FORMAT_HEIF = 35;
    public static int orientation = -1;

    public HeifHelper(ICameraContext iCameraContext) {
        this.mICameraContext = iCameraContext;
    }

    public static int getCaptureFormat(String str) {
        if ("heif".equalsIgnoreCase(str)) {
            return FORMAT_HEIF;
        }
        return 256;
    }

    public static byte[] getYUVBuffer(Image image) throws Throwable {
        FileOutputStream fileOutputStream;
        Image.Plane plane = image.getPlanes()[0];
        Image.Plane plane2 = image.getPlanes()[1];
        Image.Plane plane3 = image.getPlanes()[2];
        int iRemaining = plane.getBuffer().remaining();
        int iRemaining2 = plane2.getBuffer().remaining();
        int iRemaining3 = plane3.getBuffer().remaining();
        LogHelper.i(TAG, "[getBuffer] Yb = " + iRemaining + " Ub = " + iRemaining2 + " Vb = " + iRemaining3);
        int i = iRemaining + iRemaining2;
        byte[] bArr = new byte[i + iRemaining3];
        plane.getBuffer().get(bArr, 0, iRemaining);
        plane2.getBuffer().get(bArr, iRemaining, iRemaining2);
        plane3.getBuffer().get(bArr, i, iRemaining3);
        FileOutputStream fileOutputStream2 = null;
        try {
            try {
                try {
                    LogHelper.d(TAG, "save the data to SD Card");
                    fileOutputStream = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "yuv").getAbsolutePath());
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e2) {
            LogHelper.e(TAG, "IOException:", e2);
        }
        try {
            fileOutputStream.write(bArr);
            fileOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e3) {
            e = e3;
            fileOutputStream2 = fileOutputStream;
            LogHelper.e(TAG, "Failed to write image,ex:", e);
            if (fileOutputStream2 != null) {
                fileOutputStream2.close();
            }
            return bArr;
        } catch (Throwable th2) {
            th = th2;
            fileOutputStream2 = fileOutputStream;
            if (fileOutputStream2 != null) {
                try {
                    fileOutputStream2.close();
                } catch (IOException e4) {
                    LogHelper.e(TAG, "IOException:", e4);
                }
            }
            throw th;
        }
        return bArr;
    }

    public ContentValues getContentValues(int i, int i2) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        String fileDirectory = this.mICameraContext.getStorageService().getFileDirectory();
        this.mImageFileName.generateTitle(jCurrentTimeMillis);
        return createContentValues(orientation, fileDirectory, jCurrentTimeMillis, i, i2);
    }

    public static void saveData(byte[] bArr, int i, int i2, int i3, String str) {
        String str2 = str + ".tmp";
        HeifWriter.Builder builder = new HeifWriter.Builder(str, i, i2, 0);
        builder.setGridEnabled(true);
        builder.setRotation(i3);
        try {
            HeifWriter heifWriterBuild = builder.build();
            heifWriterBuild.start();
            heifWriterBuild.addYuvBuffer(FORMAT_HEIF, bArr);
            try {
                long jCurrentTimeMillis = System.currentTimeMillis();
                heifWriterBuild.stop(10000L);
                LogHelper.i(TAG, "[saveData] save heif file consume time = " + (System.currentTimeMillis() - jCurrentTimeMillis));
                new File(str2).renameTo(new File(str));
            } catch (Exception e) {
                LogHelper.e(TAG, "Exception", e);
            }
            heifWriterBuild.close();
        } catch (IOException e2) {
            LogHelper.e(TAG, "getjpeg IOException ", e2);
        }
    }

    public ContentValues createContentValues(int i, String str, long j, int i2, int i3) {
        ContentValues contentValues = new ContentValues();
        String strGenerateTitle = this.mImageFileName.generateTitle(j);
        String str2 = strGenerateTitle + ".heic";
        contentValues.put("datetaken", Long.valueOf(j));
        contentValues.put("title", strGenerateTitle);
        contentValues.put("_display_name", str2);
        contentValues.put("mime_type", "image/heic");
        contentValues.put("width", Integer.valueOf(i2));
        contentValues.put("height", Integer.valueOf(i3));
        contentValues.put("orientation", Integer.valueOf(i));
        contentValues.put("_data", str + '/' + str2);
        Location location = this.mICameraContext.getLocation();
        if (location != null) {
            contentValues.put("latitude", Double.valueOf(location.getLatitude()));
            contentValues.put("longitude", Double.valueOf(location.getLongitude()));
        }
        LogHelper.d(TAG, "createContentValues, width : " + i2 + ",height = " + i3 + ",orientation = " + i);
        return contentValues;
    }

    private class ImageFileName {
        private SimpleDateFormat mSimpleDateFormat;

        public ImageFileName(String str) {
            this.mSimpleDateFormat = new SimpleDateFormat(str);
        }

        public String generateTitle(long j) {
            return this.mSimpleDateFormat.format((Date) new java.sql.Date(j));
        }
    }
}
