package com.android.documentsui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.LruCache;

public class ThumbnailCache extends LruCache<Uri, Bitmap> {
    public ThumbnailCache(int maxSizeBytes) {
        super(maxSizeBytes);
    }

    @Override
    protected int sizeOf(Uri key, Bitmap value) {
        return value.getByteCount();
    }
}
