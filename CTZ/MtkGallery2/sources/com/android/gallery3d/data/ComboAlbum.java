package com.android.gallery3d.data;

import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.Future;
import java.util.ArrayList;

public class ComboAlbum extends MediaSet implements ContentListener {
    private String mName;
    private final MediaSet[] mSets;

    public ComboAlbum(Path path, MediaSet[] mediaSetArr, String str) {
        super(path, nextVersionNumber());
        this.mSets = mediaSetArr;
        for (MediaSet mediaSet : this.mSets) {
            mediaSet.addContentListener(this);
        }
        this.mName = str;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        int i3;
        ArrayList<MediaItem> arrayList = new ArrayList<>();
        int i4 = i;
        for (MediaSet mediaSet : this.mSets) {
            int mediaItemCount = mediaSet.getMediaItemCount();
            if (i2 < 1) {
                break;
            }
            if (i4 < mediaItemCount) {
                if (i4 + i2 > mediaItemCount) {
                    i3 = mediaItemCount - i4;
                } else {
                    i3 = i2;
                }
                ArrayList<MediaItem> mediaItem = mediaSet.getMediaItem(i4, i3);
                arrayList.addAll(mediaItem);
                i2 -= mediaItem.size();
                i4 = 0;
            } else {
                i4 -= mediaItemCount;
            }
        }
        return arrayList;
    }

    @Override
    public int getMediaItemCount() {
        int mediaItemCount = 0;
        for (MediaSet mediaSet : this.mSets) {
            mediaItemCount += mediaSet.getMediaItemCount();
        }
        return mediaItemCount;
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
        int length = this.mSets.length;
        boolean z = false;
        for (int i = 0; i < length; i++) {
            if (this.mSets[i].reload() > this.mDataVersion) {
                z = true;
            }
        }
        if (z) {
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public Future<Integer> requestSync(MediaSet.SyncListener syncListener) {
        return requestSyncOnMultipleSets(this.mSets, syncListener);
    }
}
