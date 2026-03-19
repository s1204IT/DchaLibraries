package com.mediatek.camera.feature.mode.longexposure;

import android.content.ContentValues;
import android.location.Location;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import java.text.SimpleDateFormat;
import java.util.Date;

class LongExposureModeHelper {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(LongExposureModeHelper.class.getSimpleName());
    private ICameraContext mICameraContext;

    public LongExposureModeHelper(ICameraContext iCameraContext) {
        this.mICameraContext = iCameraContext;
    }

    public ContentValues createContentValues(byte[] bArr, String str, int i, int i2) {
        ContentValues contentValues = new ContentValues();
        long jCurrentTimeMillis = System.currentTimeMillis();
        String str2 = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss_S").format((Date) new java.sql.Date(jCurrentTimeMillis));
        String str3 = str2 + ".jpg";
        int orientationFromExif = CameraUtil.getOrientationFromExif(bArr);
        contentValues.put("datetaken", Long.valueOf(jCurrentTimeMillis));
        contentValues.put("title", str2);
        contentValues.put("_display_name", str3);
        contentValues.put("mime_type", "image/jpeg");
        contentValues.put("width", Integer.valueOf(i));
        contentValues.put("height", Integer.valueOf(i2));
        contentValues.put("orientation", Integer.valueOf(orientationFromExif));
        contentValues.put("_data", str + '/' + str3);
        Location location = this.mICameraContext.getLocation();
        if (location != null) {
            contentValues.put("latitude", Double.valueOf(location.getLatitude()));
            contentValues.put("longitude", Double.valueOf(location.getLongitude()));
        }
        LogHelper.d(TAG, "createContentValues, width : " + i + ",height = " + i2 + ",orientation = " + orientationFromExif);
        return contentValues;
    }
}
