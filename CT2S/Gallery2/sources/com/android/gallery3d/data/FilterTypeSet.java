package com.android.gallery3d.data;

import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;

public class FilterTypeSet extends MediaSet implements ContentListener {
    private final ArrayList<MediaSet> mAlbums;
    private final MediaSet mBaseSet;
    private final DataManager mDataManager;
    private final int mMediaType;
    private final ArrayList<Path> mPaths;

    public FilterTypeSet(Path path, DataManager dataManager, MediaSet baseSet, int mediaType) {
        super(path, -1L);
        this.mPaths = new ArrayList<>();
        this.mAlbums = new ArrayList<>();
        this.mDataManager = dataManager;
        this.mBaseSet = baseSet;
        this.mMediaType = mediaType;
        this.mBaseSet.addContentListener(this);
    }

    @Override
    public String getName() {
        return this.mBaseSet.getName();
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return this.mAlbums.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return this.mAlbums.size();
    }

    @Override
    public int getMediaItemCount() {
        return this.mPaths.size();
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        return ClusterAlbum.getMediaItemFromPath(this.mPaths, start, count, this.mDataManager);
    }

    @Override
    public long reload() {
        if (this.mBaseSet.reload() > this.mDataVersion) {
            updateData();
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    private void updateData() {
        this.mAlbums.clear();
        String basePath = "/filter/mediatype/" + this.mMediaType;
        int n = this.mBaseSet.getSubMediaSetCount();
        for (int i = 0; i < n; i++) {
            MediaSet set = this.mBaseSet.getSubMediaSet(i);
            String filteredPath = basePath + "/{" + set.getPath().toString() + "}";
            MediaSet filteredSet = this.mDataManager.getMediaSet(filteredPath);
            filteredSet.reload();
            if (filteredSet.getMediaItemCount() > 0 || filteredSet.getSubMediaSetCount() > 0) {
                this.mAlbums.add(filteredSet);
            }
        }
        this.mPaths.clear();
        final int total = this.mBaseSet.getMediaItemCount();
        final Path[] buf = new Path[total];
        this.mBaseSet.enumerateMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if (item.getMediaType() == FilterTypeSet.this.mMediaType && index >= 0 && index < total) {
                    Path path = item.getPath();
                    buf[index] = path;
                }
            }
        });
        for (int i2 = 0; i2 < total; i2++) {
            if (buf[i2] != null) {
                this.mPaths.add(buf[i2]);
            }
        }
    }

    @Override
    public int getSupportedOperations() {
        return 5;
    }

    @Override
    public void delete() {
        MediaSet.ItemConsumer consumer = new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if ((item.getSupportedOperations() & 1) != 0) {
                    item.delete();
                }
            }
        };
        this.mDataManager.mapMediaItems(this.mPaths, consumer, 0);
    }
}
