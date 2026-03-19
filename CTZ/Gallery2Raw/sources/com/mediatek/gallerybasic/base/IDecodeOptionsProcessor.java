package com.mediatek.gallerybasic.base;

import android.graphics.BitmapFactory;

public interface IDecodeOptionsProcessor {
    boolean processRegionDecodeOptions(String str, BitmapFactory.Options options);

    boolean processThumbDecodeOptions(String str, BitmapFactory.Options options);
}
