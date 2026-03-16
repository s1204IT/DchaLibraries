package com.android.gallery3d.data;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.Future;

public class ComboAlbumSet extends MediaSet implements ContentListener {
    private final String mName;
    private final MediaSet[] mSets;

    public ComboAlbumSet(Path path, GalleryApp application, MediaSet[] mediaSets) {
        super(path, nextVersionNumber());
        this.mSets = mediaSets;
        MediaSet[] arr$ = this.mSets;
        for (MediaSet set : arr$) {
            set.addContentListener(this);
        }
        this.mName = application.getResources().getString(R.string.set_label_all_albums);
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        MediaSet[] arr$ = this.mSets;
        for (MediaSet set : arr$) {
            int size = set.getSubMediaSetCount();
            if (index < size) {
                return set.getSubMediaSet(index);
            }
            index -= size;
        }
        return null;
    }

    @Override
    public int getSubMediaSetCount() {
        int count = 0;
        MediaSet[] arr$ = this.mSets;
        for (MediaSet set : arr$) {
            count += set.getSubMediaSetCount();
        }
        return count;
    }

    @Override
    public String getName() {
        return this.mName;
    }

    @Override
    public boolean isLoading() {
        int n = this.mSets.length;
        for (int i = 0; i < n; i++) {
            if (this.mSets[i].isLoading()) {
                return true;
            }
        }
        return false;
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
