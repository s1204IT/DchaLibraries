package com.android.gallery3d.data;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.Future;

public class ComboAlbumSet extends MediaSet implements ContentListener {
    private final String mName;
    private final MediaSet[] mSets;

    public ComboAlbumSet(Path path, GalleryApp galleryApp, MediaSet[] mediaSetArr) {
        super(path, nextVersionNumber());
        this.mSets = mediaSetArr;
        for (MediaSet mediaSet : this.mSets) {
            mediaSet.addContentListener(this);
        }
        this.mName = galleryApp.getResources().getString(R.string.set_label_all_albums);
    }

    @Override
    public MediaSet getSubMediaSet(int i) {
        for (MediaSet mediaSet : this.mSets) {
            int subMediaSetCount = mediaSet.getSubMediaSetCount();
            if (i < subMediaSetCount) {
                return mediaSet.getSubMediaSet(i);
            }
            i -= subMediaSetCount;
        }
        return null;
    }

    @Override
    public int getSubMediaSetCount() {
        int subMediaSetCount = 0;
        for (MediaSet mediaSet : this.mSets) {
            subMediaSetCount += mediaSet.getSubMediaSetCount();
        }
        return subMediaSetCount;
    }

    @Override
    public String getName() {
        return this.mName;
    }

    @Override
    public boolean isLoading() {
        int length = this.mSets.length;
        for (int i = 0; i < length; i++) {
            if (this.mSets[i].isLoading()) {
                return true;
            }
        }
        return false;
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

    @Override
    public long reloadForSlideShow() {
        int length = this.mSets.length;
        boolean z = false;
        for (int i = 0; i < length; i++) {
            if (this.mSets[i].reloadForSlideShow() > this.mDataVersion) {
                z = true;
            }
        }
        if (z) {
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }

    @Override
    public long synchronizedAlbumData() {
        int length = this.mSets.length;
        boolean z = false;
        for (int i = 0; i < length; i++) {
            if (this.mSets[i].synchronizedAlbumData() > this.mDataVersion) {
                z = true;
            }
        }
        if (z) {
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }
}
