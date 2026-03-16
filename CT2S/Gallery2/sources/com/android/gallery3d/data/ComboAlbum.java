package com.android.gallery3d.data;

import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.Future;
import java.util.ArrayList;

public class ComboAlbum extends MediaSet implements ContentListener {
    private String mName;
    private final MediaSet[] mSets;

    public ComboAlbum(Path path, MediaSet[] mediaSets, String name) {
        super(path, nextVersionNumber());
        this.mSets = mediaSets;
        MediaSet[] arr$ = this.mSets;
        for (MediaSet set : arr$) {
            set.addContentListener(this);
        }
        this.mName = name;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        ArrayList<MediaItem> items = new ArrayList<>();
        MediaSet[] arr$ = this.mSets;
        for (MediaSet set : arr$) {
            int size = set.getMediaItemCount();
            if (count < 1) {
                break;
            }
            if (start < size) {
                int fetchCount = start + count <= size ? count : size - start;
                ArrayList<MediaItem> fetchItems = set.getMediaItem(start, fetchCount);
                items.addAll(fetchItems);
                count -= fetchItems.size();
                start = 0;
            } else {
                start -= size;
            }
        }
        return items;
    }

    @Override
    public int getMediaItemCount() {
        int count = 0;
        MediaSet[] arr$ = this.mSets;
        for (MediaSet set : arr$) {
            count += set.getMediaItemCount();
        }
        return count;
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    @Override
    public String getName() {
        return this.mName;
    }

    public void useNameOfChild(int i) {
        if (i < this.mSets.length) {
            this.mName = this.mSets[i].getName();
        }
    }

    @Override
    public long reload() {
        boolean changed = false;
        int n = this.mSets.length;
        for (int i = 0; i < n; i++) {
            long version = this.mSets[i].reload();
            if (version > this.mDataVersion) {
                changed = true;
            }
        }
        if (changed) {
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public Future<Integer> requestSync(MediaSet.SyncListener listener) {
        return requestSyncOnMultipleSets(this.mSets, listener);
    }
}
