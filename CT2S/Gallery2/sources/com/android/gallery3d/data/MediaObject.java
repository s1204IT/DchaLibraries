package com.android.gallery3d.data;

import android.net.Uri;

public abstract class MediaObject {
    private static long sVersionSerial = 0;
    protected long mDataVersion;
    protected final Path mPath;

    public interface PanoramaSupportCallback {
        void panoramaInfoAvailable(MediaObject mediaObject, boolean z, boolean z2);
    }

    public MediaObject(Path path, long version) {
        path.setObject(this);
        this.mPath = path;
        this.mDataVersion = version;
    }

    public Path getPath() {
        return this.mPath;
    }

    public int getSupportedOperations() {
        return 0;
    }

    public void getPanoramaSupport(PanoramaSupportCallback callback) {
        callback.panoramaInfoAvailable(this, false, false);
    }

    public void delete() {
        throw new UnsupportedOperationException();
    }

    public void rotate(int degrees) {
        throw new UnsupportedOperationException();
    }

    public Uri getContentUri() {
        String className = getClass().getName();
        Log.e("MediaObject", "Class " + className + "should implement getContentUri.");
        Log.e("MediaObject", "The object was created from path: " + getPath());
        throw new UnsupportedOperationException();
    }

    public Uri getPlayUri() {
        throw new UnsupportedOperationException();
    }

    public int getMediaType() {
        return 1;
    }

    public MediaDetails getDetails() {
        MediaDetails details = new MediaDetails();
        return details;
    }

    public long getDataVersion() {
        return this.mDataVersion;
    }

    public int getCacheFlag() {
        return 0;
    }

    public int getCacheStatus() {
        throw new UnsupportedOperationException();
    }

    public long getCacheSize() {
        throw new UnsupportedOperationException();
    }

    public void cache(int flag) {
        throw new UnsupportedOperationException();
    }

    public static synchronized long nextVersionNumber() {
        long j;
        j = sVersionSerial + 1;
        sVersionSerial = j;
        return j;
    }

    public static int getTypeFromString(String s) {
        if ("all".equals(s)) {
            return 6;
        }
        if ("image".equals(s)) {
            return 2;
        }
        if ("video".equals(s)) {
            return 4;
        }
        throw new IllegalArgumentException(s);
    }
}
