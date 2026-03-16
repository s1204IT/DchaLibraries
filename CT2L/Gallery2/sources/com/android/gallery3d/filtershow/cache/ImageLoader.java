package com.android.gallery3d.filtershow.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.MimeTypeMap;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.util.XmpUtilHelper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class ImageLoader {
    public static String getMimeType(Uri src) {
        String postfix = MimeTypeMap.getFileExtensionFromUrl(src.toString());
        if (postfix == null) {
            return null;
        }
        String ret = MimeTypeMap.getSingleton().getMimeTypeFromExtension(postfix);
        return ret;
    }

    public static String getLocalPathFromUri(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
        if (cursor == null) {
            return null;
        }
        int index = cursor.getColumnIndexOrThrow("_data");
        cursor.moveToFirst();
        return cursor.getString(index);
    }

    public static int getMetadataOrientation(Context context, Uri uri) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getOrientation");
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{"orientation"}, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                int ori = cursor.getInt(0);
                switch (ori) {
                    case 90:
                        return 6;
                    case 180:
                        return 3;
                    case 270:
                        return 8;
                    default:
                        return 1;
                }
            }
        } catch (SQLiteException e) {
        } catch (IllegalArgumentException e2) {
        } catch (IllegalStateException e3) {
        } finally {
            Utils.closeSilently(cursor);
        }
        ExifInterface exif = new ExifInterface();
        InputStream is = null;
        try {
            try {
                if ("file".equals(uri.getScheme())) {
                    String mimeType = getMimeType(uri);
                    if (!"image/jpeg".equals(mimeType)) {
                        if (0 != 0) {
                            try {
                                is.close();
                            } catch (IOException e4) {
                                Log.w("ImageLoader", "Failed to close InputStream", e4);
                            }
                        }
                        return 1;
                    }
                    String path = uri.getPath();
                    exif.readExif(path);
                } else {
                    is = context.getContentResolver().openInputStream(uri);
                    exif.readExif(is);
                }
                int exif2 = parseExif(exif);
                if (is == null) {
                    return exif2;
                }
                try {
                    is.close();
                    return exif2;
                } catch (IOException e5) {
                    Log.w("ImageLoader", "Failed to close InputStream", e5);
                    return exif2;
                }
            } catch (IOException e6) {
                Log.w("ImageLoader", "Failed to read EXIF orientation", e6);
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e7) {
                        Log.w("ImageLoader", "Failed to close InputStream", e7);
                    }
                }
                return 1;
            }
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e8) {
                    Log.w("ImageLoader", "Failed to close InputStream", e8);
                }
            }
            throw th;
        }
    }

    private static int parseExif(ExifInterface exif) {
        Integer tagval = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
        if (tagval == null) {
            return 1;
        }
        int orientation = tagval.intValue();
        switch (orientation) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
            case 7:
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                return orientation;
            default:
                return 1;
        }
    }

    public static int getMetadataRotation(Context context, Uri uri) {
        int orientation = getMetadataOrientation(context, uri);
        switch (orientation) {
            case 3:
                return 180;
            case 4:
            case 5:
            case 7:
            default:
                return 0;
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return 90;
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                return 270;
        }
    }

    public static Bitmap orientBitmap(Bitmap bitmap, int ori) {
        Matrix matrix = new Matrix();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (ori == 6 || ori == 8 || ori == 5 || ori == 7) {
            w = h;
            h = w;
        }
        switch (ori) {
            case 2:
                matrix.preScale(-1.0f, 1.0f);
                break;
            case 3:
                matrix.setRotate(180.0f, w / 2.0f, h / 2.0f);
                break;
            case 4:
                matrix.preScale(1.0f, -1.0f);
                break;
            case 5:
                matrix.setRotate(90.0f, w / 2.0f, h / 2.0f);
                matrix.preScale(1.0f, -1.0f);
                break;
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                matrix.setRotate(90.0f, w / 2.0f, h / 2.0f);
                break;
            case 7:
                matrix.setRotate(270.0f, w / 2.0f, h / 2.0f);
                matrix.preScale(1.0f, -1.0f);
                break;
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                matrix.setRotate(270.0f, w / 2.0f, h / 2.0f);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap loadRegionBitmap(Context context, BitmapCache cache, Uri uri, BitmapFactory.Options options, Rect bounds) {
        InputStream is = null;
        int w = 0;
        int h = 0;
        try {
            if (options.inSampleSize != 0) {
                return null;
            }
            is = context.getContentResolver().openInputStream(uri);
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);
            Rect r = new Rect(0, 0, decoder.getWidth(), decoder.getHeight());
            w = decoder.getWidth();
            h = decoder.getHeight();
            Rect imageBounds = new Rect(bounds);
            if (!r.contains(imageBounds)) {
                imageBounds.intersect(r);
                bounds.left = imageBounds.left;
                bounds.top = imageBounds.top;
            }
            Bitmap reuse = cache.getBitmap(imageBounds.width(), imageBounds.height(), 9);
            options.inBitmap = reuse;
            Bitmap bitmap = decoder.decodeRegion(imageBounds, options);
            if (bitmap != reuse) {
                cache.cache(reuse);
            }
            return bitmap;
        } catch (IOException e) {
            Log.e("ImageLoader", "FileNotFoundException for " + uri, e);
            return null;
        } catch (FileNotFoundException e2) {
            Log.e("ImageLoader", "FileNotFoundException for " + uri, e2);
            return null;
        } catch (IllegalArgumentException e3) {
            Log.e("ImageLoader", "exc, image decoded " + w + " x " + h + " bounds: " + bounds.left + "," + bounds.top + " - " + bounds.width() + "x" + bounds.height() + " exc: " + e3);
            return null;
        } finally {
            Utils.closeSilently(is);
        }
    }

    public static Rect loadBitmapBounds(Context context, Uri uri) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        loadBitmap(context, uri, o);
        return new Rect(0, 0, o.outWidth, o.outHeight);
    }

    public static Bitmap loadDownsampledBitmap(Context context, Uri uri, int sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inSampleSize = sampleSize;
        return loadBitmap(context, uri, options);
    }

    public static Bitmap loadBitmap(Context context, Uri uri, BitmapFactory.Options o) {
        Bitmap bitmapDecodeStream = null;
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bitmapDecodeStream = BitmapFactory.decodeStream(is, null, o);
        } catch (FileNotFoundException e) {
            Log.e("ImageLoader", "FileNotFoundException for " + uri, e);
        } finally {
            Utils.closeSilently(is);
        }
        return bitmapDecodeStream;
    }

    public static Bitmap loadConstrainedBitmap(Uri uri, Context context, int maxSideLength, Rect originalBounds, boolean useMin) {
        int imageSide;
        if (maxSideLength <= 0 || uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getScaledBitmap");
        }
        Rect storedBounds = loadBitmapBounds(context, uri);
        if (originalBounds != null) {
            originalBounds.set(storedBounds);
        }
        int w = storedBounds.width();
        int h = storedBounds.height();
        if (w <= 0 || h <= 0) {
            return null;
        }
        if (useMin) {
            imageSide = Math.min(w, h);
        } else {
            imageSide = Math.max(w, h);
        }
        int sampleSize = 1;
        while (imageSide > maxSideLength) {
            imageSide >>>= 1;
            sampleSize <<= 1;
        }
        if (sampleSize <= 0 || Math.min(w, h) / sampleSize <= 0) {
            return null;
        }
        return loadDownsampledBitmap(context, uri, sampleSize);
    }

    public static Bitmap loadOrientedConstrainedBitmap(Uri uri, Context context, int maxSideLength, int orientation, Rect originalBounds) {
        Bitmap bmap = loadConstrainedBitmap(uri, context, maxSideLength, originalBounds, false);
        if (bmap != null) {
            Bitmap bmap2 = orientBitmap(bmap, orientation);
            if (bmap2.getConfig() != Bitmap.Config.ARGB_8888) {
                return bmap2.copy(Bitmap.Config.ARGB_8888, true);
            }
            return bmap2;
        }
        return bmap;
    }

    public static Bitmap getScaleOneImageForPreset(Context context, BitmapCache cache, Uri uri, Rect bounds, Rect destination) {
        int thresholdWidth;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        if (destination != null && bounds.width() > (thresholdWidth = (int) (destination.width() * 1.2f))) {
            int sampleSize = 1;
            int w = bounds.width();
            while (w > thresholdWidth) {
                sampleSize *= 2;
                w /= sampleSize;
            }
            options.inSampleSize = sampleSize;
        }
        return loadRegionBitmap(context, cache, uri, options, bounds);
    }

    public static Bitmap loadBitmapWithBackouts(Context context, Uri sourceUri, int sampleSize) {
        boolean noBitmap = true;
        int num_tries = 0;
        if (sampleSize <= 0) {
            sampleSize = 1;
        }
        Bitmap bmap = null;
        while (noBitmap) {
            try {
                bmap = loadDownsampledBitmap(context, sourceUri, sampleSize);
                noBitmap = false;
            } catch (OutOfMemoryError e) {
                num_tries++;
                if (num_tries >= 5) {
                    throw e;
                }
                bmap = null;
                System.gc();
                sampleSize *= 2;
            }
        }
        return bmap;
    }

    public static Bitmap loadOrientedBitmapWithBackouts(Context context, Uri sourceUri, int sampleSize) {
        Bitmap bitmap = loadBitmapWithBackouts(context, sourceUri, sampleSize);
        if (bitmap == null) {
            return null;
        }
        int orientation = getMetadataOrientation(context, sourceUri);
        return orientBitmap(bitmap, orientation);
    }

    public static XMPMeta getXmpObject(Context context) {
        try {
            InputStream is = context.getContentResolver().openInputStream(MasterImage.getImage().getUri());
            return XmpUtilHelper.extractXMPMeta(is);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static boolean queryLightCycle360(Context context) {
        boolean zEquals = false;
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(MasterImage.getImage().getUri());
            XMPMeta meta = XmpUtilHelper.extractXMPMeta(is);
            if (meta != null && meta.doesPropertyExist("http://ns.google.com/photos/1.0/panorama/", "GPano:CroppedAreaImageWidthPixels") && meta.doesPropertyExist("http://ns.google.com/photos/1.0/panorama/", "GPano:FullPanoWidthPixels")) {
                Integer cropValue = meta.getPropertyInteger("http://ns.google.com/photos/1.0/panorama/", "GPano:CroppedAreaImageWidthPixels");
                Integer fullValue = meta.getPropertyInteger("http://ns.google.com/photos/1.0/panorama/", "GPano:FullPanoWidthPixels");
                if (cropValue != null && fullValue != null) {
                    zEquals = cropValue.equals(fullValue);
                }
            }
        } catch (XMPException e) {
        } catch (FileNotFoundException e2) {
        } finally {
            Utils.closeSilently(is);
        }
        return zEquals;
    }

    public static List<ExifTag> getExif(Context context, Uri uri) {
        String path = getLocalPathFromUri(context, uri);
        if (path == null) {
            return null;
        }
        Uri localUri = Uri.parse(path);
        String mimeType = getMimeType(localUri);
        if (!"image/jpeg".equals(mimeType)) {
            return null;
        }
        try {
            ExifInterface exif = new ExifInterface();
            exif.readExif(path);
            return exif.getAllTags();
        } catch (IOException e) {
            Log.w("ImageLoader", "Failed to read EXIF tags", e);
            return null;
        }
    }
}
