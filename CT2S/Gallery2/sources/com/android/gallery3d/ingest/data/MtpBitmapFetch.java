package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.mtp.MtpDevice;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.android.gallery3d.data.Exif;
import com.android.photos.data.GalleryBitmapPool;

@TargetApi(12)
public class MtpBitmapFetch {
    private static int sMaxSize = 0;

    public static void recycleThumbnail(Bitmap b) {
        if (b != null) {
            GalleryBitmapPool.getInstance().put(b);
        }
    }

    public static Bitmap getThumbnail(MtpDevice device, IngestObjectInfo info) {
        byte[] imageBytes = device.getThumbnail(info.getObjectHandle());
        if (imageBytes == null) {
            return null;
        }
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, o);
        if (o.outWidth == 0 || o.outHeight == 0) {
            return null;
        }
        o.inBitmap = GalleryBitmapPool.getInstance().get(o.outWidth, o.outHeight);
        o.inMutable = true;
        o.inJustDecodeBounds = false;
        o.inSampleSize = 1;
        try {
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, o);
        } catch (IllegalArgumentException e) {
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }
    }

    public static BitmapWithMetadata getFullsize(MtpDevice device, IngestObjectInfo info) {
        return getFullsize(device, info, sMaxSize);
    }

    public static BitmapWithMetadata getFullsize(MtpDevice device, IngestObjectInfo info, int maxSide) {
        Bitmap created;
        byte[] imageBytes = device.getObject(info.getObjectHandle(), info.getCompressedSize());
        if (imageBytes == null) {
            return null;
        }
        if (maxSide > 0) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, o);
            int w = o.outWidth;
            int h = o.outHeight;
            int comp = Math.max(h, w);
            int sampleSize = 1;
            while ((comp >> 1) >= maxSide) {
                comp >>= 1;
                sampleSize++;
            }
            o.inSampleSize = sampleSize;
            o.inJustDecodeBounds = false;
            created = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, o);
        } else {
            created = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }
        if (created == null) {
            return null;
        }
        int orientation = Exif.getOrientation(imageBytes);
        return new BitmapWithMetadata(created, orientation);
    }

    public static void configureForContext(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        wm.getDefaultDisplay().getMetrics(metrics);
        sMaxSize = Math.max(metrics.heightPixels, metrics.widthPixels);
    }
}
