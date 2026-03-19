package com.mediatek.gallerybasic.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DecodeSpecLimitor {
    private static final int MAX_BMP_FILE_SIZE;
    private static final int MAX_GIF_FILE_SIZE;
    private static final long MAX_GIF_FRAME_PIXEL_SIZE = 1572864;
    private static final String MIME_GIF = "image/gif";
    private static final String TAG = "MtkGallery2/DecodeSpecLimitor";
    public static boolean sIsLowRamDevice = false;

    static {
        MAX_GIF_FILE_SIZE = sIsLowRamDevice ? 10485760 : 20971520;
        MAX_BMP_FILE_SIZE = sIsLowRamDevice ? 6291456 : 54525952;
    }

    public static boolean isOutOfSpecLimit(long j, int i, int i2, String str) {
        return isOutOfSpecInteral(j, i, i2, str);
    }

    public static boolean isOutOfSpecLimit(Context context, Uri uri) {
        if (context == null || uri == null) {
            return false;
        }
        try {
            InputStream inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
            if (inputStreamOpenInputStream == null) {
                return false;
            }
            try {
                int iAvailable = inputStreamOpenInputStream.available();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStreamOpenInputStream, null, options);
                try {
                    inputStreamOpenInputStream.close();
                } catch (IOException e) {
                }
                boolean zIsOutOfSpecInteral = isOutOfSpecInteral(iAvailable, options.outWidth, options.outHeight, options.outMimeType);
                if (zIsOutOfSpecInteral) {
                    Log.d(TAG, "<isOutOfSpecLimit> uri " + uri + ", out of spec limit");
                }
                return zIsOutOfSpecInteral;
            } catch (IOException e2) {
                return false;
            }
        } catch (FileNotFoundException e3) {
            return false;
        }
    }

    private static boolean isOutOfSpecInteral(long j, int i, int i2, String str) {
        return isOutOfGifSpec(j, ((long) i) * ((long) i2), str) || isOutOfBmpSpec(j, str);
    }

    private static boolean isOutOfGifSpec(long j, long j2, String str) {
        return str != null && str.equals(MIME_GIF) && (j > ((long) MAX_GIF_FILE_SIZE) || j2 > MAX_GIF_FRAME_PIXEL_SIZE);
    }

    private static boolean isOutOfBmpSpec(long j, String str) {
        return str != null && str.endsWith("bmp") && j > ((long) MAX_BMP_FILE_SIZE);
    }
}
