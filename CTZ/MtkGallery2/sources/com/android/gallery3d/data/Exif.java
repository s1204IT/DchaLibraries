package com.android.gallery3d.data;

import com.android.gallery3d.exif.ExifInterface;
import java.io.IOException;
import java.io.InputStream;

public class Exif {
    public static int getOrientation(InputStream inputStream) {
        if (inputStream == null) {
            return 0;
        }
        ExifInterface exifInterface = new ExifInterface();
        try {
            exifInterface.readExif(inputStream);
            Integer tagIntValue = exifInterface.getTagIntValue(ExifInterface.TAG_ORIENTATION);
            if (tagIntValue == null) {
                return 0;
            }
            return ExifInterface.getRotationForOrientationValue(tagIntValue.shortValue());
        } catch (IOException e) {
            com.mediatek.gallery3d.util.Log.w("Gallery2/GalleryExif", "Failed to read EXIF orientation", e);
            return 0;
        }
    }

    public static ExifInterface getExif(byte[] bArr) {
        ExifInterface exifInterface = new ExifInterface();
        try {
            exifInterface.readExif(bArr);
        } catch (IOException e) {
            com.mediatek.gallery3d.util.Log.w("Gallery2/GalleryExif", "Failed to read EXIF data", e);
        }
        return exifInterface;
    }

    public static int getOrientation(ExifInterface exifInterface) {
        Integer tagIntValue = exifInterface.getTagIntValue(ExifInterface.TAG_ORIENTATION);
        if (tagIntValue == null) {
            return 0;
        }
        return ExifInterface.getRotationForOrientationValue(tagIntValue.shortValue());
    }

    public static int getOrientation(byte[] bArr) {
        if (bArr == null) {
            return 0;
        }
        return getOrientation(getExif(bArr));
    }
}
