package com.android.gallery3d.data;

import com.android.gallery3d.exif.ExifInterface;
import java.io.IOException;
import java.io.InputStream;

public class Exif {
    public static int getOrientation(InputStream is) {
        if (is == null) {
            return 0;
        }
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(is);
            Integer val = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
            if (val != null) {
                return ExifInterface.getRotationForOrientationValue(val.shortValue());
            }
            return 0;
        } catch (IOException e) {
            android.util.Log.w("GalleryExif", "Failed to read EXIF orientation", e);
            return 0;
        }
    }

    public static ExifInterface getExif(byte[] jpegData) {
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(jpegData);
        } catch (IOException e) {
            android.util.Log.w("GalleryExif", "Failed to read EXIF data", e);
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
