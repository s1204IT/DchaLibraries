package com.android.documentsui.inspector;

import android.media.ExifInterface;
import android.os.Bundle;
import com.android.internal.util.Preconditions;

final class MetadataUtils {
    static final boolean $assertionsDisabled = false;

    private MetadataUtils() {
    }

    static boolean hasGeoCoordinates(Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        return hasVideoCoordinates(bundle.getBundle("android.media.metadata.video")) || hasExifGpsFields(bundle.getBundle("android:documentExif"));
    }

    static boolean hasVideoCoordinates(Bundle bundle) {
        return bundle != null && bundle.containsKey("android.media.metadata.video:latitude") && bundle.containsKey("android.media.metadata.video:longitude");
    }

    static boolean hasExifGpsFields(Bundle bundle) {
        return bundle != null && bundle.containsKey("GPSLatitude") && bundle.containsKey("GPSLongitude") && bundle.containsKey("GPSLatitudeRef") && bundle.containsKey("GPSLongitudeRef");
    }

    static float[] getGeoCoordinates(Bundle bundle) {
        Preconditions.checkNotNull(bundle);
        Bundle bundle2 = bundle.getBundle("android:documentExif");
        if (hasExifGpsFields(bundle2)) {
            return getExifGpsCoords(bundle2);
        }
        Bundle bundle3 = bundle.getBundle("android.media.metadata.video");
        if (hasVideoCoordinates(bundle3)) {
            return getVideoCoords(bundle3);
        }
        throw new IllegalArgumentException("Invalid metadata bundle: " + bundle);
    }

    static float[] getExifGpsCoords(Bundle bundle) {
        return new float[]{ExifInterface.convertRationalLatLonToFloat(bundle.getString("GPSLatitude"), bundle.getString("GPSLatitudeRef")), ExifInterface.convertRationalLatLonToFloat(bundle.getString("GPSLongitude"), bundle.getString("GPSLongitudeRef"))};
    }

    static float[] getVideoCoords(Bundle bundle) {
        return new float[]{bundle.getFloat("android.media.metadata.video:latitude"), bundle.getFloat("android.media.metadata.video:longitude")};
    }
}
