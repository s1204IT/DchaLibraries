package com.android.camera.util;

import android.location.Location;
import com.android.camera.exif.ExifInterface;

public class ExifUtil {
    public static void addLocationToExif(ExifInterface exif, Location location) {
        exif.addGpsTags(location.getLatitude(), location.getLongitude());
        exif.addGpsDateTimeStampTag(location.getTime());
        double altitude = location.getAltitude();
        if (altitude != 0.0d) {
            short altitudeRef = altitude < 0.0d ? (short) 1 : (short) 0;
            exif.setTag(exif.buildTag(ExifInterface.TAG_GPS_ALTITUDE_REF, Short.valueOf(altitudeRef)));
        }
    }
}
