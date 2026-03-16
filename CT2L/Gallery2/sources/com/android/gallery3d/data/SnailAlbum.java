package com.android.gallery3d.data;

import java.util.concurrent.atomic.AtomicBoolean;

public class SnailAlbum extends SingleItemAlbum {
    private AtomicBoolean mDirty;

    public SnailAlbum(Path path, SnailItem item) {
        super(path, item);
        this.mDirty = new AtomicBoolean(false);
    }

    @Override
    public long reload() {
        if (this.mDirty.compareAndSet(true, false)) {
            ((SnailItem) getItem()).updateVersion();
            this.mDataVersion = nextVersionNumber();
        }
        return this.mDataVersion;
    }
}
