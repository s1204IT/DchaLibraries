package com.android.gallery3d.data;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;

public class CameraShortcutImage extends ActionImage {
    public CameraShortcutImage(Path path, GalleryApp galleryApp) {
        super(path, galleryApp, R.drawable.placeholder_camera);
    }

    @Override
    public int getSupportedOperations() {
        return super.getSupportedOperations() | 32768;
    }
}
