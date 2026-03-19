package com.mediatek.gallerybasic.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

public interface IFilterShowImageLoader {
    Bitmap loadBitmap(Context context, Uri uri, BitmapFactory.Options options);
}
