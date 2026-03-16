package com.android.gallery3d.data;

import android.support.v4.app.FragmentTransaction;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;

public class EmptyAlbumImage extends ActionImage {
    public EmptyAlbumImage(Path path, GalleryApp application) {
        super(path, application, R.drawable.placeholder_empty);
    }

    @Override
    public int getSupportedOperations() {
        return super.getSupportedOperations() | FragmentTransaction.TRANSIT_EXIT_MASK;
    }
}
