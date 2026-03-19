package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.graphics.Bitmap;

@TargetApi(12)
public class BitmapWithMetadata {
    public Bitmap bitmap;
    public int rotationDegrees;

    public BitmapWithMetadata(Bitmap bitmap, int i) {
        this.bitmap = bitmap;
        this.rotationDegrees = i;
    }
}
