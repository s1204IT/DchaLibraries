package com.mediatek.camera.feature.setting;

import android.content.ContentValues;
import com.mediatek.camera.common.utils.CameraUtil;
import java.text.SimpleDateFormat;
import java.util.Date;

class CsNamingRule {
    private final ImageFileName mImageFileName = new ImageFileName("'IMG'_yyyyMMdd_HHmmss");

    CsNamingRule() {
    }

    ContentValues createContentValues(byte[] bArr, String str, int i, int i2, long j, int i3) {
        ContentValues contentValues = new ContentValues();
        String str2 = this.mImageFileName.generateTitle(j) + "_" + i3 + "CS";
        int orientationFromExif = CameraUtil.getOrientationFromExif(bArr);
        String str3 = this.mImageFileName.generateTitle(j) + "_" + i3 + "CS.jpg";
        contentValues.put("datetaken", Long.valueOf(System.currentTimeMillis()));
        contentValues.put("title", str2);
        contentValues.put("_display_name", str3);
        contentValues.put("mime_type", "image/jpeg");
        contentValues.put("width", Integer.valueOf(i));
        contentValues.put("height", Integer.valueOf(i2));
        contentValues.put("orientation", Integer.valueOf(orientationFromExif));
        contentValues.put("_data", str + '/' + str3);
        return contentValues;
    }

    private class ImageFileName {
        private final SimpleDateFormat mSimpleDateFormat;

        public ImageFileName(String str) {
            this.mSimpleDateFormat = new SimpleDateFormat(str);
        }

        public String generateTitle(long j) {
            return this.mSimpleDateFormat.format((Date) new java.sql.Date(j));
        }
    }
}
