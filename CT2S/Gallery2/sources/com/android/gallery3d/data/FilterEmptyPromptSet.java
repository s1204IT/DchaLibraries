package com.android.gallery3d.data;

import java.util.ArrayList;

public class FilterEmptyPromptSet extends MediaSet implements ContentListener {
    private MediaSet mBaseSet;
    private ArrayList<MediaItem> mEmptyItem;

    public FilterEmptyPromptSet(Path path, MediaSet baseSet, MediaItem emptyItem) {
        super(path, -1L);
        this.mEmptyItem = new ArrayList<>(1);
        this.mEmptyItem.add(emptyItem);
        this.mBaseSet = baseSet;
        this.mBaseSet.addContentListener(this);
    }

    @Override
    public int getMediaItemCount() {
        int itemCount = this.mBaseSet.getMediaItemCount();
        if (itemCount > 0) {
            return itemCount;
        }
        return 1;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        int itemCount = this.mBaseSet.getMediaItemCount();
        if (itemCount > 0) {
            return this.mBaseSet.getMediaItem(start, count);
        }
        if (start == 0 && count == 1) {
            return this.mEmptyItem;
        }
        throw new ArrayIndexOutOfBoundsException();
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
