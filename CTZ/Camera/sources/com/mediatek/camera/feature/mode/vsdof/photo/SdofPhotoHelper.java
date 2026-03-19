package com.mediatek.camera.feature.mode.vsdof.photo;

import android.app.Activity;
import android.content.ContentValues;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SdofPhotoHelper {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SdofPhotoHelper.class.getSimpleName());
    private ICameraContext mICameraContext;
    private ImageFileName mImageFileName = new ImageFileName("'IMG'_yyyyMMdd_HHmmss_S");

    public SdofPhotoHelper(ICameraContext iCameraContext) {
        this.mICameraContext = iCameraContext;
    }

    public ContentValues createContentValues(byte[] bArr, String str, int i, int i2) {
        ContentValues contentValues = new ContentValues();
        long jCurrentTimeMillis = System.currentTimeMillis();
        String strGenerateTitle = this.mImageFileName.generateTitle(jCurrentTimeMillis);
        String str2 = strGenerateTitle + ".jpg";
        int orientationFromExif = CameraUtil.getOrientationFromExif(bArr);
        contentValues.put("datetaken", Long.valueOf(jCurrentTimeMillis));
        contentValues.put("title", strGenerateTitle);
        contentValues.put("_display_name", str2);
        contentValues.put("mime_type", "image/jpeg");
        contentValues.put("width", Integer.valueOf(i));
        contentValues.put("height", Integer.valueOf(i2));
        contentValues.put("orientation", Integer.valueOf(orientationFromExif));
        contentValues.put("_data", str + '/' + str2);
        Location location = this.mICameraContext.getLocation();
        if (location != null) {
            contentValues.put("latitude", Double.valueOf(location.getLatitude()));
            contentValues.put("longitude", Double.valueOf(location.getLongitude()));
        }
        LogHelper.d(TAG, "createContentValues, width : " + i + ",height = " + i2 + ",orientation = " + orientationFromExif);
        return contentValues;
    }

    public int getCameraInfoOrientation(String str, Activity activity) {
        try {
            CameraCharacteristics cameraCharacteristics = ((CameraManager) activity.getSystemService("camera")).getCameraCharacteristics(str);
            if (cameraCharacteristics == null) {
                LogHelper.e(TAG, "[getCameraInfoOrientation] characteristics is null");
                return 0;
            }
            return ((Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean isMirror(String str, Activity activity) {
        try {
            CameraCharacteristics cameraCharacteristics = ((CameraManager) activity.getSystemService("camera")).getCameraCharacteristics(str);
            if (cameraCharacteristics == null) {
                LogHelper.e(TAG, "[isMirror] characteristics is null");
                return false;
            }
            int iIntValue = ((Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue();
            ((Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
            return iIntValue == 0;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
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
