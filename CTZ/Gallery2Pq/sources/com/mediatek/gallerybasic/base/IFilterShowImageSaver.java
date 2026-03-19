package com.mediatek.gallerybasic.base;

import android.content.ContentValues;
import android.net.Uri;
import java.io.File;

public interface IFilterShowImageSaver {
    void updateExifData(Uri uri);

    void updateMediaDatabase(File file, ContentValues contentValues);
}
