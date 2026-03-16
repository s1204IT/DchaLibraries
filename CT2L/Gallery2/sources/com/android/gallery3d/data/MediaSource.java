package com.android.gallery3d.data;

import android.net.Uri;
import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;

public abstract class MediaSource {
    private String mPrefix;

    public abstract MediaObject createMediaObject(Path path);

    protected MediaSource(String prefix) {
        this.mPrefix = prefix;
    }

    public String getPrefix() {
        return this.mPrefix;
    }

    public Path findPathByUri(Uri uri, String type) {
        return null;
    }

    public void pause() {
    }

    public void resume() {
    }

    public Path getDefaultSetOf(Path item) {
        return null;
    }

    public long getTotalUsedCacheSize() {
        return 0L;
    }

    public long getTotalTargetCacheSize() {
        return 0L;
    }

    public static class PathId {
        public int id;
        public Path path;

        public PathId(Path path, int id) {
            this.path = path;
            this.id = id;
        }
    }

    public void mapMediaItems(ArrayList<PathId> list, MediaSet.ItemConsumer consumer) {
        MediaObject obj;
        int n = list.size();
        for (int i = 0; i < n; i++) {
            PathId pid = list.get(i);
            synchronized (DataManager.LOCK) {
                obj = pid.path.getObject();
                if (obj == null) {
                    try {
                        obj = createMediaObject(pid.path);
                    } catch (Throwable th) {
                        Log.w("MediaSource", "cannot create media object: " + pid.path, th);
                    }
                }
            }
            if (obj != null) {
                consumer.consume(pid.id, (MediaItem) obj);
            }
        }
    }
}
