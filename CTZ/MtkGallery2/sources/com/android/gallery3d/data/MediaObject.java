package com.android.gallery3d.data;

import android.net.Uri;

public abstract class MediaObject {
    private static long sVersionSerial = 0;
    protected long mDataVersion;
    protected final Path mPath;

    public interface PanoramaSupportCallback {
        void panoramaInfoAvailable(MediaObject mediaObject, boolean z, boolean z2);
    }

    public MediaObject(Path path, long j) {
        path.setObject(this);
        this.mPath = path;
        this.mDataVersion = j;
    }

    public Path getPath() {
        return this.mPath;
    }

    public int getSupportedOperations() {
        return 0;
    }

    public void getPanoramaSupport(PanoramaSupportCallback panoramaSupportCallback) {
        panoramaSupportCallback.panoramaInfoAvailable(this, false, false);
    }

    public void delete() {
        throw new UnsupportedOperationException();
    }

    public void rotate(int i) {
        throw new UnsupportedOperationException();
    }

    public Uri getContentUri() {
        Log.e("Gallery2/MediaObject", "Class " + getClass().getName() + "should implement getContentUri.");
        StringBuilder sb = new StringBuilder();
        sb.append("The object was created from path: ");
        sb.append(getPath());
        Log.e("Gallery2/MediaObject", sb.toString());
        throw new UnsupportedOperationException();
    }

    public Uri getPlayUri() {
        throw new UnsupportedOperationException();
    }

    public int getMediaType() {
        return 1;
    }

    public MediaDetails getDetails() {
        return new MediaDetails();
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

    public void cache(int i) {
        throw new UnsupportedOperationException();
    }

    public static synchronized long nextVersionNumber() {
        long j;
        j = sVersionSerial + 1;
        sVersionSerial = j;
        return j;
    }

    public static int getTypeFromString(String str) {
        if ("all".equals(str)) {
            return 6;
        }
        if ("image".equals(str)) {
            return 2;
        }
        if ("video".equals(str)) {
            return 4;
        }
        throw new IllegalArgumentException(str);
    }

    public long synchronizedAlbumData() {
        return this.mDataVersion;
    }
}
