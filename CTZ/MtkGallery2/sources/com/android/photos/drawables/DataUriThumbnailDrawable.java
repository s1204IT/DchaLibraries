package com.android.photos.drawables;

import android.media.ExifInterface;
import android.text.TextUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DataUriThumbnailDrawable extends AutoThumbnailDrawable<String> {
    @Override
    protected byte[] getPreferredImageBytes(String str) {
        try {
            ExifInterface exifInterface = new ExifInterface(str);
            if (!exifInterface.hasThumbnail()) {
                return null;
            }
            return exifInterface.getThumbnail();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected InputStream getFallbackImageStream(String str) {
        try {
            return new FileInputStream(str);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    protected boolean dataChangedLocked(String str) {
        return !TextUtils.equals((CharSequence) this.mData, str);
    }
}
