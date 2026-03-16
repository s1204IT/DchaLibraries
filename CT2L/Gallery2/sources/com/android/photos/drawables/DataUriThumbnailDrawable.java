package com.android.photos.drawables;

import android.media.ExifInterface;
import android.text.TextUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DataUriThumbnailDrawable extends AutoThumbnailDrawable<String> {
    @Override
    protected byte[] getPreferredImageBytes(String data) {
        try {
            ExifInterface exif = new ExifInterface(data);
            if (!exif.hasThumbnail()) {
                return null;
            }
            byte[] thumbnail = exif.getThumbnail();
            return thumbnail;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected InputStream getFallbackImageStream(String data) {
        try {
            return new FileInputStream(data);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    protected boolean dataChangedLocked(String data) {
        return !TextUtils.equals((CharSequence) this.mData, data);
    }
}
