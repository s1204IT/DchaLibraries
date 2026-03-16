package com.android.camera.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.location.Location;
import android.net.Uri;
import com.android.camera.exif.ExifInterface;

public interface MediaSaver {

    public interface OnMediaSavedListener {
        void onMediaSaved(Uri uri);
    }

    public interface QueueListener {
        void onQueueStatus(boolean z);
    }

    void addImage(byte[] bArr, String str, long j, Location location, int i, int i2, int i3, ExifInterface exifInterface, OnMediaSavedListener onMediaSavedListener, ContentResolver contentResolver);

    void addImage(byte[] bArr, String str, long j, Location location, int i, ExifInterface exifInterface, OnMediaSavedListener onMediaSavedListener, ContentResolver contentResolver);

    void addImage(byte[] bArr, String str, Location location, int i, int i2, int i3, ExifInterface exifInterface, OnMediaSavedListener onMediaSavedListener, ContentResolver contentResolver);

    void addVideo(String str, ContentValues contentValues, OnMediaSavedListener onMediaSavedListener, ContentResolver contentResolver);

    boolean isQueueFull();

    void setQueueListener(QueueListener queueListener);
}
