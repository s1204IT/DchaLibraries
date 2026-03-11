package com.android.photos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

interface SimpleBitmapRegionDecoder {
    Bitmap decodeRegion(Rect rect, BitmapFactory.Options options);

    int getHeight();

    int getWidth();
}
