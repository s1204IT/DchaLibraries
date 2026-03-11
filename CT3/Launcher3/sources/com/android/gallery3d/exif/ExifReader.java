package com.android.gallery3d.exif;

import android.util.Log;
import com.android.launcher3.compat.PackageInstallerCompat;
import java.io.IOException;
import java.io.InputStream;

class ExifReader {
    private final ExifInterface mInterface;

    ExifReader(ExifInterface iRef) {
        this.mInterface = iRef;
    }

    protected ExifData read(InputStream inputStream) throws ExifInvalidFormatException, IOException {
        ExifParser parser = ExifParser.parse(inputStream, this.mInterface);
        ExifData exifData = new ExifData(parser.getByteOrder());
        for (int event = parser.next(); event != 5; event = parser.next()) {
            switch (event) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                    exifData.addIfdData(new IfdData(parser.getCurrentIfd()));
                    break;
                case PackageInstallerCompat.STATUS_INSTALLING:
                    ExifTag tag = parser.getTag();
                    if (!tag.hasValue()) {
                        parser.registerForTagValue(tag);
                    } else {
                        exifData.getIfdData(tag.getIfd()).setTag(tag);
                    }
                    break;
                case PackageInstallerCompat.STATUS_FAILED:
                    ExifTag tag2 = parser.getTag();
                    if (tag2.getDataType() == 7) {
                        parser.readFullTagValue(tag2);
                    }
                    exifData.getIfdData(tag2.getIfd()).setTag(tag2);
                    break;
                case 3:
                    byte[] buf = new byte[parser.getCompressedImageSize()];
                    if (buf.length == parser.read(buf)) {
                        exifData.setCompressedThumbnail(buf);
                    } else {
                        Log.w("ExifReader", "Failed to read the compressed thumbnail");
                    }
                    break;
                case 4:
                    byte[] buf2 = new byte[parser.getStripSize()];
                    if (buf2.length == parser.read(buf2)) {
                        exifData.setStripBytes(parser.getStripIndex(), buf2);
                    } else {
                        Log.w("ExifReader", "Failed to read the strip bytes");
                    }
                    break;
            }
        }
        return exifData;
    }
}
