package com.android.gallery3d.data;

import java.util.ArrayList;

public class SingleItemAlbum extends MediaSet {
    private final MediaItem mItem;
    private final String mName;

    public SingleItemAlbum(Path path, MediaItem mediaItem) {
        super(path, nextVersionNumber());
        this.mItem = mediaItem;
        this.mName = "SingleItemAlbum(" + this.mItem.getClass().getSimpleName() + ")";
    }

    @Override
    public int getMediaItemCount() {
        return 1;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        ArrayList<MediaItem> arrayList = new ArrayList<>();
        if (i <= 0 && i + i2 > 0) {
            arrayList.add(this.mItem);
        }
        return arrayList;
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
