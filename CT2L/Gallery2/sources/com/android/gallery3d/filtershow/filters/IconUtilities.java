package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class IconUtilities {
    public static Bitmap getFXBitmap(Resources res, int id) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        if (id != 0) {
            return BitmapFactory.decodeResource(res, id, o);
        }
        return null;
    }
}
