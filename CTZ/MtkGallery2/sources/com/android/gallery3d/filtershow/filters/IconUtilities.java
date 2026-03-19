package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class IconUtilities {
    public static Bitmap getFXBitmap(Resources resources, int i) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        if (i != 0) {
            return BitmapFactory.decodeResource(resources, i, options);
        }
        return null;
    }
}
