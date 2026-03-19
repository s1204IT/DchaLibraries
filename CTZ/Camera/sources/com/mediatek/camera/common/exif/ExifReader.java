package com.mediatek.camera.common.exif;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.io.IOException;
import java.io.InputStream;

class ExifReader {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ExifReader.class.getSimpleName());
    private final ExifInterface mInterface;

    ExifReader(ExifInterface exifInterface) {
        this.mInterface = exifInterface;
    }

    protected ExifData read(InputStream inputStream) throws ExifInvalidFormatException, IOException {
        ExifParser exifParser = ExifParser.parse(inputStream, this.mInterface);
        ExifData exifData = new ExifData(exifParser.getByteOrder());
        for (int next = exifParser.next(); next != 5; next = exifParser.next()) {
            switch (next) {
                case 0:
                    exifData.addIfdData(new IfdData(exifParser.getCurrentIfd()));
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    ExifTag tag = exifParser.getTag();
                    if (!tag.hasValue()) {
                        exifParser.registerForTagValue(tag);
                    } else {
                        exifData.getIfdData(tag.getIfd()).setTag(tag);
                    }
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    ExifTag tag2 = exifParser.getTag();
                    if (tag2.getDataType() == 7) {
                        exifParser.readFullTagValue(tag2);
                    }
                    exifData.getIfdData(tag2.getIfd()).setTag(tag2);
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    byte[] bArr = new byte[exifParser.getCompressedImageSize()];
                    if (bArr.length == exifParser.read(bArr)) {
                        exifData.setCompressedThumbnail(bArr);
                    } else {
                        LogHelper.w(TAG, "Failed to read the compressed thumbnail");
                    }
                    break;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    byte[] bArr2 = new byte[exifParser.getStripSize()];
                    if (bArr2.length == exifParser.read(bArr2)) {
                        exifData.setStripBytes(exifParser.getStripIndex(), bArr2);
                    } else {
                        LogHelper.w(TAG, "Failed to read the strip bytes");
                    }
                    break;
            }
        }
        return exifData;
    }
}
