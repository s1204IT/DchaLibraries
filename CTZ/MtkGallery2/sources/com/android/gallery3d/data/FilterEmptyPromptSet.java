package com.android.gallery3d.data;

import java.util.ArrayList;

public class FilterEmptyPromptSet extends MediaSet implements ContentListener {
    private MediaSet mBaseSet;
    private ArrayList<MediaItem> mEmptyItem;

    public FilterEmptyPromptSet(Path path, MediaSet mediaSet, MediaItem mediaItem) {
        super(path, -1L);
        this.mEmptyItem = new ArrayList<>(1);
        this.mEmptyItem.add(mediaItem);
        this.mBaseSet = mediaSet;
        this.mBaseSet.addContentListener(this);
    }

    @Override
    public int getMediaItemCount() {
        int mediaItemCount = this.mBaseSet.getMediaItemCount();
        if (mediaItemCount > 0) {
            return mediaItemCount;
        }
        return 1;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        if (this.mBaseSet.getMediaItemCount() > 0) {
            return this.mBaseSet.getMediaItem(i, i2);
        }
        if (i == 0 && i2 == 1) {
            return this.mEmptyItem;
        }
        return new ArrayList<>();
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    @Override
    public boolean isCameraRoll() {
        return this.mBaseSet.isCameraRoll();
    }

    @Override
    public long reload() {
        return this.mBaseSet.reload();
    }

    @Override
    public String getName() {
        return this.mBaseSet.getName();
    }
}
