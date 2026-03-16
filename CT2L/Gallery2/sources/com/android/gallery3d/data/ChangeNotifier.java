package com.android.gallery3d.data;

import android.net.Uri;
import com.android.gallery3d.app.GalleryApp;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChangeNotifier {
    private AtomicBoolean mContentDirty = new AtomicBoolean(true);
    private MediaSet mMediaSet;

    public ChangeNotifier(MediaSet set, Uri uri, GalleryApp application) throws Throwable {
        this.mMediaSet = set;
        application.getDataManager().registerChangeNotifier(uri, this);
    }

    public ChangeNotifier(MediaSet set, Uri[] uris, GalleryApp application) throws Throwable {
        this.mMediaSet = set;
        for (Uri uri : uris) {
            application.getDataManager().registerChangeNotifier(uri, this);
        }
    }

    public boolean isDirty() {
        return this.mContentDirty.compareAndSet(true, false);
    }

    protected void onChange(boolean selfChange) {
        if (this.mContentDirty.compareAndSet(false, true)) {
            this.mMediaSet.notifyContentChanged();
        }
    }
}
