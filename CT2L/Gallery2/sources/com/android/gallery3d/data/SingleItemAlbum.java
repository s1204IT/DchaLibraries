package com.android.gallery3d.data;

import java.util.ArrayList;

public class SingleItemAlbum extends MediaSet {
    private final MediaItem mItem;
    private final String mName;

    public SingleItemAlbum(Path path, MediaItem item) {
        super(path, nextVersionNumber());
        this.mItem = item;
        this.mName = "SingleItemAlbum(" + this.mItem.getClass().getSimpleName() + ")";
    }

    @Override
    public int getMediaItemCount() {
        return 1;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        ArrayList<MediaItem> result = new ArrayList<>();
        if (start <= 0 && start + count > 0) {
            result.add(this.mItem);
        }
        return result;
    }

    public MediaItem getItem() {
        return this.mItem;
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    @Override
    public String getName() {
        return this.mName;
    }

    @Override
    public long reload() {
        return this.mDataVersion;
    }
}
