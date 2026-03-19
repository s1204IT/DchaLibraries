package com.android.internal.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import java.io.InputStream;

public class LocalImageResolver {
    private static final int MAX_SAFE_ICON_SIZE_PX = 480;

    public static Drawable resolveImage(Uri uri, Context context) throws Throwable {
        int i;
        double d;
        BitmapFactory.Options boundsOptionsForImage = getBoundsOptionsForImage(uri, context);
        if (boundsOptionsForImage.outWidth == -1 || boundsOptionsForImage.outHeight == -1) {
            return null;
        }
        if (boundsOptionsForImage.outHeight > boundsOptionsForImage.outWidth) {
            i = boundsOptionsForImage.outHeight;
        } else {
            i = boundsOptionsForImage.outWidth;
        }
        if (i > 480) {
            d = i / 480;
        } else {
            d = 1.0d;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = getPowerOfTwoForSampleRatio(d);
        InputStream inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
        Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream, null, options);
        inputStreamOpenInputStream.close();
        return new BitmapDrawable(context.getResources(), bitmapDecodeStream);
    }

    private static BitmapFactory.Options getBoundsOptionsForImage(Uri uri, Context context) throws Throwable {
        InputStream inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStreamOpenInputStream, null, options);
        inputStreamOpenInputStream.close();
        return options;
    }

    private static int getPowerOfTwoForSampleRatio(double d) {
        return Math.max(1, Integer.highestOneBit((int) Math.floor(d)));
    }
}
