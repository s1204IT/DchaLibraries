package com.android.gallery3d.data;

import android.net.Uri;
import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;

public abstract class MediaSource {
    private String mPrefix;

    public abstract MediaObject createMediaObject(Path path);

    protected MediaSource(String str) {
        this.mPrefix = str;
    }

    public String getPrefix() {
        return this.mPrefix;
    }

    public Path findPathByUri(Uri uri, String str) {
        return null;
    }

    public void pause() {
    }

    public void resume() {
    }

    public Path getDefaultSetOf(Path path) {
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

        public PathId(Path path, int i) {
            this.path = path;
            this.id = i;
        }
    }

    public void mapMediaItems(ArrayList<PathId> arrayList, MediaSet.ItemConsumer itemConsumer) {
        MediaObject object;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            PathId pathId = arrayList.get(i);
            synchronized (DataManager.LOCK) {
                object = pathId.path.getObject();
                if (object == null) {
                    try {
                        object = createMediaObject(pathId.path);
                    } catch (Throwable th) {
                        Log.w("Gallery2/MediaSource", "cannot create media object: " + pathId.path, th);
                    }
                }
            }
            if (object != null) {
                itemConsumer.consume(pathId.id, (MediaItem) object);
            }
        }
    }
}
