package com.android.gallery3d.data;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;

public class UnlockImage extends ActionImage {
    public UnlockImage(Path path, GalleryApp galleryApp) {
        super(path, galleryApp, R.drawable.placeholder_locked);
    }

    @Override
    public int getSupportedOperations() {
        return super.getSupportedOperations() | 4096;
    }
}
