package com.android.ex.chips;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import com.android.ex.chips.PhotoManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DefaultPhotoManager implements PhotoManager {
    private final ContentResolver mContentResolver;
    private final LruCache<Uri, byte[]> mPhotoCacheMap = new LruCache<>(20);

    private static class PhotoQuery {
        public static final String[] PROJECTION = {"data15"};
    }

    public DefaultPhotoManager(ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    @Override
    public void populatePhotoBytesAsync(RecipientEntry entry, PhotoManager.PhotoManagerCallback callback) {
        Uri photoThumbnailUri = entry.getPhotoThumbnailUri();
        if (photoThumbnailUri != null) {
            byte[] photoBytes = this.mPhotoCacheMap.get(photoThumbnailUri);
            if (photoBytes != null) {
                entry.setPhotoBytes(photoBytes);
                if (callback != null) {
                    callback.onPhotoBytesPopulated();
                    return;
                }
                return;
            }
            fetchPhotoAsync(entry, photoThumbnailUri, callback);
            return;
        }
        if (callback != null) {
            callback.onPhotoBytesAsyncLoadFailed();
        }
    }

    private void fetchPhotoAsync(final RecipientEntry entry, final Uri photoThumbnailUri, final PhotoManager.PhotoManagerCallback callback) {
        AsyncTask<Void, Void, byte[]> photoLoadTask = new AsyncTask<Void, Void, byte[]>() {
            @Override
            protected byte[] doInBackground(Void... params) {
                Cursor photoCursor = DefaultPhotoManager.this.mContentResolver.query(photoThumbnailUri, PhotoQuery.PROJECTION, null, null, null);
                if (photoCursor == null) {
                    try {
                        InputStream is = DefaultPhotoManager.this.mContentResolver.openInputStream(photoThumbnailUri);
                        if (is == null) {
                            return null;
                        }
                        byte[] buffer = new byte[16384];
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        while (true) {
                            try {
                                int size = is.read(buffer);
                                if (size != -1) {
                                    baos.write(buffer, 0, size);
                                } else {
                                    is.close();
                                    return baos.toByteArray();
                                }
                            } catch (Throwable th) {
                                is.close();
                                throw th;
                            }
                        }
                    } catch (IOException e) {
                        return null;
                    }
                } else {
                    try {
                        if (photoCursor.moveToFirst()) {
                            return photoCursor.getBlob(0);
                        }
                        return null;
                    } finally {
                        photoCursor.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(byte[] photoBytes) {
                entry.setPhotoBytes(photoBytes);
                if (photoBytes != null) {
                    DefaultPhotoManager.this.mPhotoCacheMap.put(photoThumbnailUri, photoBytes);
                    if (callback != null) {
                        callback.onPhotoBytesAsynchronouslyPopulated();
                        return;
                    }
                    return;
                }
                if (callback != null) {
                    callback.onPhotoBytesAsyncLoadFailed();
                }
            }
        };
        photoLoadTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Void[0]);
    }
}
