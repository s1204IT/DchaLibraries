package com.android.gallery3d.data;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;

public class EmptyAlbumImage extends ActionImage {
    public EmptyAlbumImage(Path path, GalleryApp galleryApp) {
        super(path, galleryApp, R.drawable.placeholder_empty);
    }

    @Override
    public int getSupportedOperations() {
        return super.getSupportedOperations() | 8192;
    }
}
