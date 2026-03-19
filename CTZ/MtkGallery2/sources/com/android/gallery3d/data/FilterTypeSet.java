package com.android.gallery3d.data;

import com.android.gallery3d.data.MediaSet;
import java.util.ArrayList;

public class FilterTypeSet extends MediaSet implements ContentListener {
    private final ArrayList<MediaSet> mAlbums;
    private final MediaSet mBaseSet;
    private final DataManager mDataManager;
    private final int mMediaType;
    private final ArrayList<Path> mPaths;

    public FilterTypeSet(Path path, DataManager dataManager, MediaSet mediaSet, int i) {
        super(path, -1L);
        this.mPaths = new ArrayList<>();
        this.mAlbums = new ArrayList<>();
        this.mDataManager = dataManager;
        this.mBaseSet = mediaSet;
        this.mMediaType = i;
        this.mBaseSet.addContentListener(this);
    }

    @Override
    public String getName() {
        return this.mBaseSet.getName();
    }

    @Override
    public MediaSet getSubMediaSet(int i) {
        return this.mAlbums.get(i);
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
    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        return ClusterAlbum.getMediaItemFromPath(this.mPaths, i, i2, this.mDataManager);
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
        String str = "/filter/mediatype/" + this.mMediaType;
        int subMediaSetCount = this.mBaseSet.getSubMediaSetCount();
        for (int i = 0; i < subMediaSetCount; i++) {
            MediaSet mediaSet = this.mDataManager.getMediaSet(str + "/{" + this.mBaseSet.getSubMediaSet(i).getPath().toString() + "}");
            mediaSet.reload();
            if (mediaSet.getMediaItemCount() > 0 || mediaSet.getSubMediaSetCount() > 0) {
                this.mAlbums.add(mediaSet);
            }
        }
        this.mPaths.clear();
        final int mediaItemCount = this.mBaseSet.getMediaItemCount();
        final Path[] pathArr = new Path[mediaItemCount];
        this.mBaseSet.enumerateMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i2, MediaItem mediaItem) {
                if (mediaItem.getMediaType() != FilterTypeSet.this.mMediaType || i2 < 0 || i2 >= mediaItemCount) {
                    return;
                }
                pathArr[i2] = mediaItem.getPath();
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        });
        for (int i2 = 0; i2 < mediaItemCount; i2++) {
            if (pathArr[i2] != null) {
                this.mPaths.add(pathArr[i2]);
            }
        }
    }

    @Override
    public int getSupportedOperations() {
        return 5;
    }

    @Override
    public void delete() {
        this.mDataManager.mapMediaItems(this.mPaths, new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i, MediaItem mediaItem) {
                if ((mediaItem.getSupportedOperations() & 1) != 0) {
                    mediaItem.delete();
                }
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        }, 0);
    }
}
