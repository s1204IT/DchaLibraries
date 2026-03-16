package com.android.camera;

import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import java.io.IOException;

public class Exif {
    private static final Log.Tag TAG = new Log.Tag("CameraExif");

    public static ExifInterface getExif(byte[] jpegData) {
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(jpegData);
        } catch (IOException e) {
            Log.w(TAG, "Failed to read EXIF data", e);
        }
        return exif;
    }

    public static int getOrientation(ExifInterface exif) {
        Integer val = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
        if (val == null) {
            return 0;
        }
        return ExifInterface.getRotationForOrientationValue(val.shortValue());
    }

    public static int getOrientation(byte[] jpegData) {
        if (jpegData == null) {
            return 0;
        }
        ExifInterface exif = getExif(jpegData);
        return getOrientation(exif);
    }
}
