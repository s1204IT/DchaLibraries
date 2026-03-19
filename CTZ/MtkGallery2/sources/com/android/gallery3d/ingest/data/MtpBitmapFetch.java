package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.mtp.MtpDevice;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.android.gallery3d.R;
import com.android.gallery3d.data.Exif;
import com.android.photos.data.GalleryBitmapPool;
import com.mediatek.gallery3d.util.Log;

@TargetApi(12)
public class MtpBitmapFetch {
    private static int sDefaultThumbColor;
    private static int sMaxSize = 0;
    private static int sDefaultThumbWidth = 640;
    private static int sDefaultThumbHeight = 480;

    public static void recycleThumbnail(Bitmap bitmap) {
        if (bitmap != null) {
            GalleryBitmapPool.getInstance().put(bitmap);
        }
    }

    public static Bitmap getThumbnail(MtpDevice mtpDevice, IngestObjectInfo ingestObjectInfo) {
        byte[] thumbnail = mtpDevice.getThumbnail(ingestObjectInfo.getObjectHandle());
        if (thumbnail == null) {
            return getDefaultThumbnail();
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length, options);
        if (options.outWidth == 0 || options.outHeight == 0) {
            return getDefaultThumbnail();
        }
        options.inBitmap = GalleryBitmapPool.getInstance().get(options.outWidth, options.outHeight);
        options.inMutable = true;
        options.inJustDecodeBounds = false;
        options.inSampleSize = 1;
        try {
            return BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length, options);
        } catch (IllegalArgumentException e) {
            return BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
        }
    }

    public static BitmapWithMetadata getFullsize(MtpDevice mtpDevice, IngestObjectInfo ingestObjectInfo) {
        return getFullsize(mtpDevice, ingestObjectInfo, sMaxSize);
    }

    public static BitmapWithMetadata getFullsize(MtpDevice mtpDevice, IngestObjectInfo ingestObjectInfo, int i) {
        Bitmap bitmapDecodeByteArray;
        byte[] object = mtpDevice.getObject(ingestObjectInfo.getObjectHandle(), ingestObjectInfo.getCompressedSize());
        if (object == null) {
            return null;
        }
        if (i <= 0) {
            bitmapDecodeByteArray = BitmapFactory.decodeByteArray(object, 0, object.length);
        } else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(object, 0, object.length, options);
            int iMax = Math.max(options.outHeight, options.outWidth);
            int i2 = 1;
            while (true) {
                iMax >>= 1;
                if (iMax < i) {
                    break;
                }
                i2++;
            }
            options.inSampleSize = i2;
            options.inJustDecodeBounds = false;
            bitmapDecodeByteArray = BitmapFactory.decodeByteArray(object, 0, object.length, options);
        }
        if (bitmapDecodeByteArray == null) {
            return null;
        }
        return new BitmapWithMetadata(bitmapDecodeByteArray, Exif.getOrientation(object));
    }

    public static void configureForContext(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
        sMaxSize = Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels);
        sDefaultThumbWidth = Math.min(displayMetrics.heightPixels, displayMetrics.widthPixels);
        sDefaultThumbHeight = (int) (sDefaultThumbWidth / (Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels) / Math.min(displayMetrics.heightPixels, displayMetrics.widthPixels)));
        Log.d("Gallery2/MtpBitmapFetch", "<configureForContext> sDefaultThumbWidth = " + sDefaultThumbWidth + ", sDefaultThumbHeight = " + sDefaultThumbHeight);
        sDefaultThumbColor = context.getResources().getColor(R.color.photo_placeholder);
    }

    private static Bitmap getDefaultThumbnail() {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(sDefaultThumbWidth, sDefaultThumbHeight, Bitmap.Config.RGB_565);
        bitmapCreateBitmap.eraseColor(sDefaultThumbColor);
        return bitmapCreateBitmap;
    }
}
