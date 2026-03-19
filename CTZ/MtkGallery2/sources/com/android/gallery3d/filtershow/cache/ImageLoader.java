package com.android.gallery3d.filtershow.cache;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.util.XmpUtilHelper;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.base.IFilterShowImageLoader;
import com.mediatek.gallerybasic.util.BitmapUtils;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class ImageLoader {
    private static IFilterShowImageLoader[] sExtLoaders;

    public static String getMimeType(Uri uri) {
        String string;
        int iLastIndexOf;
        String fileExtensionFromUrl = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (fileExtensionFromUrl.equals("") && (iLastIndexOf = (string = uri.toString()).lastIndexOf(".")) != -1 && iLastIndexOf != string.length() - 1) {
            fileExtensionFromUrl = string.substring(iLastIndexOf + 1);
        }
        if (fileExtensionFromUrl == null) {
            return null;
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtensionFromUrl);
    }

    public static String getLocalPathFromUri(Context context, Uri uri) {
        Cursor cursorQuery;
        try {
            try {
                cursorQuery = context.getContentResolver().query(uri, new String[]{BookmarkEnhance.COLUMN_DATA}, null, null, null);
                if (cursorQuery == null) {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return null;
                }
                try {
                    int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(BookmarkEnhance.COLUMN_DATA);
                    cursorQuery.moveToFirst();
                    String string = cursorQuery.getString(columnIndexOrThrow);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return string;
                } catch (IllegalArgumentException e) {
                    e = e;
                    Log.e("ImageLoader", "Exception at getLocalPathFromUri()", e);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return null;
                }
            } catch (Throwable th) {
                if (context != 0) {
                    context.close();
                }
                return null;
            }
        } catch (IllegalArgumentException e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th2) {
            context = 0;
            if (context != 0) {
            }
            return null;
        }
    }

    public static int getMetadataOrientation(Context context, Uri uri) throws Throwable {
        Cursor cursorQuery;
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getOrientation");
        }
        Cursor cursor = null;
        InputStream inputStream = null;
        InputStream inputStream2 = null;
        try {
            cursorQuery = context.getContentResolver().query(uri, new String[]{ExtFieldsUtils.VIDEO_ROTATION_FIELD}, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        int i = cursorQuery.getInt(0);
                        if (i == 90) {
                            Utils.closeSilently(cursorQuery);
                            return 6;
                        }
                        if (i == 180) {
                            Utils.closeSilently(cursorQuery);
                            return 3;
                        }
                        if (i != 270) {
                            Utils.closeSilently(cursorQuery);
                            return 1;
                        }
                        Utils.closeSilently(cursorQuery);
                        return 8;
                    }
                } catch (SQLiteException e) {
                } catch (IllegalArgumentException e2) {
                } catch (IllegalStateException e3) {
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    Utils.closeSilently(cursor);
                    throw th;
                }
            }
        } catch (SQLiteException e4) {
            cursorQuery = null;
        } catch (IllegalArgumentException e5) {
            cursorQuery = null;
        } catch (IllegalStateException e6) {
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
        }
        Utils.closeSilently(cursorQuery);
        new ExifInterface();
        try {
            try {
                if ("file".equals(uri.getScheme())) {
                    if ("image/jpeg".equals(getMimeType(uri))) {
                        return getOrientationFromExif(uri.getPath(), null);
                    }
                    return 1;
                }
                InputStream inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
                try {
                    int orientationFromExif = getOrientationFromExif(null, inputStreamOpenInputStream);
                    if (inputStreamOpenInputStream != null) {
                        try {
                            inputStreamOpenInputStream.close();
                        } catch (IOException e7) {
                            Log.w("ImageLoader", "Failed to close InputStream", e7);
                        }
                    }
                    return orientationFromExif;
                } catch (FileNotFoundException e8) {
                    inputStream2 = inputStreamOpenInputStream;
                    e = e8;
                    Log.w("ImageLoader", "Failed to read EXIF orientation", e);
                    if (inputStream2 != null) {
                        try {
                            inputStream2.close();
                        } catch (IOException e9) {
                            Log.w("ImageLoader", "Failed to close InputStream", e9);
                        }
                    }
                    return 1;
                } catch (Throwable th3) {
                    inputStream = inputStreamOpenInputStream;
                    th = th3;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e10) {
                            Log.w("ImageLoader", "Failed to close InputStream", e10);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (FileNotFoundException e11) {
            e = e11;
        }
    }

    public static int getMetadataRotation(Context context, Uri uri) throws Throwable {
        int metadataOrientation = getMetadataOrientation(context, uri);
        if (metadataOrientation == 3) {
            return 180;
        }
        if (metadataOrientation == 6) {
            return 90;
        }
        if (metadataOrientation == 8) {
            return 270;
        }
        return 0;
    }

    public static Bitmap orientBitmap(Bitmap bitmap, int i) {
        Matrix matrix = new Matrix();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (i == 6 || i == 8 || i == 5 || i == 7) {
            height = width;
            width = height;
        }
        switch (i) {
            case 2:
                matrix.preScale(-1.0f, 1.0f);
                break;
            case 3:
                matrix.setRotate(180.0f, width / 2.0f, height / 2.0f);
                break;
            case 4:
                matrix.preScale(1.0f, -1.0f);
                break;
            case 5:
                matrix.setRotate(90.0f, width / 2.0f, height / 2.0f);
                matrix.preScale(1.0f, -1.0f);
                break;
            case 6:
                matrix.setRotate(90.0f, width / 2.0f, height / 2.0f);
                break;
            case 7:
                matrix.setRotate(270.0f, width / 2.0f, height / 2.0f);
                matrix.preScale(1.0f, -1.0f);
                break;
            case 8:
                matrix.setRotate(270.0f, width / 2.0f, height / 2.0f);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap loadRegionBitmap(Context context, BitmapCache bitmapCache, Uri uri, BitmapFactory.Options options, Rect rect) throws Throwable {
        int width;
        int height;
        InputStream inputStreamOpenInputStream;
        if (options.inSampleSize != 0) {
            return null;
        }
        try {
            try {
                inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
                try {
                    try {
                        BitmapRegionDecoder bitmapRegionDecoderNewInstance = BitmapRegionDecoder.newInstance(inputStreamOpenInputStream, false);
                        Rect rect2 = new Rect(0, 0, bitmapRegionDecoderNewInstance.getWidth(), bitmapRegionDecoderNewInstance.getHeight());
                        width = bitmapRegionDecoderNewInstance.getWidth();
                        try {
                            height = bitmapRegionDecoderNewInstance.getHeight();
                            try {
                                Rect rect3 = new Rect(rect);
                                if (!rect2.contains(rect3)) {
                                    rect3.intersect(rect2);
                                    rect.left = rect3.left;
                                    rect.top = rect3.top;
                                }
                                Bitmap bitmap = bitmapCache.getBitmap(rect3.width(), rect3.height(), 9);
                                options.inBitmap = bitmap;
                                Bitmap bitmapDecodeRegion = bitmapRegionDecoderNewInstance.decodeRegion(rect3, options);
                                if (bitmapDecodeRegion != bitmap) {
                                    bitmapCache.cache(bitmap);
                                }
                                Utils.closeSilently(inputStreamOpenInputStream);
                                return bitmapDecodeRegion;
                            } catch (IllegalArgumentException e) {
                                e = e;
                                Log.e("ImageLoader", "exc, image decoded " + width + " x " + height + " bounds: " + rect.left + "," + rect.top + " - " + rect.width() + "x" + rect.height() + " exc: " + e);
                                Utils.closeSilently(inputStreamOpenInputStream);
                                return null;
                            }
                        } catch (IllegalArgumentException e2) {
                            e = e2;
                            height = 0;
                        }
                    } catch (IllegalArgumentException e3) {
                        e = e3;
                        width = 0;
                        height = 0;
                    }
                } catch (FileNotFoundException e4) {
                    e = e4;
                    Log.e("ImageLoader", "FileNotFoundException for " + uri, e);
                    Utils.closeSilently(inputStreamOpenInputStream);
                    return null;
                } catch (IOException e5) {
                    e = e5;
                    Log.e("ImageLoader", "FileNotFoundException for " + uri, e);
                    Utils.closeSilently(inputStreamOpenInputStream);
                    return null;
                }
            } catch (Throwable th) {
                th = th;
                Utils.closeSilently((Closeable) context);
                throw th;
            }
        } catch (FileNotFoundException e6) {
            e = e6;
            inputStreamOpenInputStream = null;
        } catch (IOException e7) {
            e = e7;
            inputStreamOpenInputStream = null;
        } catch (IllegalArgumentException e8) {
            e = e8;
            width = 0;
            height = 0;
            inputStreamOpenInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            context = 0;
            Utils.closeSilently((Closeable) context);
            throw th;
        }
    }

    public static Rect loadBitmapBounds(Context context, Uri uri) throws Throwable {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        loadBitmap(context, uri, options);
        return new Rect(0, 0, options.outWidth, options.outHeight);
    }

    public static Bitmap loadDownsampledBitmap(Context context, Uri uri, int i) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inSampleSize = i;
        return BitmapUtils.replaceBackgroundColor(loadBitmap(context, uri, options), true);
    }

    public static Bitmap loadBitmap(Context context, Uri uri, BitmapFactory.Options options) throws Throwable {
        Cursor cursorQuery;
        InputStream inputStreamOpenInputStream;
        Bitmap bitmapDecodeStream;
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        StringBuilder sb = new StringBuilder();
        ?? r2 = "<loadBitmap> uri = ";
        sb.append("<loadBitmap> uri = ");
        sb.append(uri);
        Log.d("ImageLoader", sb.toString());
        Bitmap bitmapLoadBitmapFromExt = loadBitmapFromExt(context, uri, options);
        if (bitmapLoadBitmapFromExt != null) {
            return bitmapLoadBitmapFromExt;
        }
        String string = "";
        try {
            try {
                cursorQuery = context.getContentResolver().query(uri, new String[]{BookmarkEnhance.COLUMN_MEDIA_TYPE}, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            string = cursorQuery.getString(0);
                        }
                    } catch (IllegalStateException e) {
                        e = e;
                        Log.e("ImageLoader", "<loadBitmap> Exception when trying to fetch MIME_TYPE info", e);
                        if (cursorQuery != null) {
                        }
                        inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
                        try {
                            try {
                                bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream, null, options);
                                if (bitmapDecodeStream != null) {
                                    Log.d("ImageLoader", "<loadBitmap> wbmp, resizeBitmapByScale");
                                    bitmapDecodeStream = resizeBitmapByScale(bitmapDecodeStream, 1.0f / options.inSampleSize, true);
                                }
                                Utils.closeSilently(inputStreamOpenInputStream);
                                return bitmapDecodeStream;
                            } catch (FileNotFoundException e2) {
                                e = e2;
                                Log.e("ImageLoader", "FileNotFoundException for " + uri, e);
                                Utils.closeSilently(inputStreamOpenInputStream);
                                return null;
                            }
                        } catch (Throwable th) {
                            th = th;
                            Utils.closeSilently(inputStreamOpenInputStream);
                            throw th;
                        }
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                if (r2 != 0) {
                    r2.close();
                }
                throw th;
            }
        } catch (IllegalStateException e3) {
            e = e3;
            cursorQuery = null;
        } catch (Throwable th3) {
            th = th3;
            r2 = 0;
            if (r2 != 0) {
            }
            throw th;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        try {
            inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
            bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream, null, options);
            if (bitmapDecodeStream != null && bitmapDecodeStream.getConfig() == null && "image/vnd.wap.wbmp".equalsIgnoreCase(string)) {
                Log.d("ImageLoader", "<loadBitmap> wbmp, resizeBitmapByScale");
                bitmapDecodeStream = resizeBitmapByScale(bitmapDecodeStream, 1.0f / options.inSampleSize, true);
            }
            Utils.closeSilently(inputStreamOpenInputStream);
            return bitmapDecodeStream;
        } catch (FileNotFoundException e4) {
            e = e4;
            inputStreamOpenInputStream = null;
        } catch (Throwable th4) {
            th = th4;
            inputStreamOpenInputStream = null;
            Utils.closeSilently(inputStreamOpenInputStream);
            throw th;
        }
    }

    public static Bitmap loadConstrainedBitmap(Uri uri, Context context, int i, Rect rect, boolean z) throws Throwable {
        int iMax;
        if (i <= 0 || uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getScaledBitmap");
        }
        Rect rectLoadBitmapBounds = loadBitmapBounds(context, uri);
        if (rect != null) {
            rect.set(rectLoadBitmapBounds);
        }
        int iWidth = rectLoadBitmapBounds.width();
        int iHeight = rectLoadBitmapBounds.height();
        if (iWidth <= 0 || iHeight <= 0) {
            return null;
        }
        if (z) {
            iMax = Math.min(iWidth, iHeight);
        } else {
            iMax = Math.max(iWidth, iHeight);
        }
        int i2 = 1;
        while (iMax > i) {
            iMax >>>= 1;
            i2 <<= 1;
        }
        if (i2 <= 0 || Math.min(iWidth, iHeight) / i2 <= 0) {
            return null;
        }
        return loadDownsampledBitmap(context, uri, i2);
    }

    public static Bitmap loadOrientedConstrainedBitmap(Uri uri, Context context, int i, int i2, Rect rect) {
        Bitmap bitmapLoadConstrainedBitmap = loadConstrainedBitmap(uri, context, i, rect, false);
        if (bitmapLoadConstrainedBitmap != null) {
            Bitmap bitmapOrientBitmap = orientBitmap(bitmapLoadConstrainedBitmap, i2);
            if (bitmapOrientBitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                return bitmapOrientBitmap.copy(Bitmap.Config.ARGB_8888, true);
            }
            return bitmapOrientBitmap;
        }
        return bitmapLoadConstrainedBitmap;
    }

    public static Bitmap getScaleOneImageForPreset(Context context, BitmapCache bitmapCache, Uri uri, Rect rect, Rect rect2) {
        int iWidth;
        BitmapFactory.Options options = new BitmapFactory.Options();
        int i = 1;
        options.inMutable = true;
        if (rect2 != null && rect.width() > (iWidth = (int) (rect2.width() * 1.2f))) {
            int iWidth2 = rect.width();
            while (iWidth2 > iWidth) {
                i *= 2;
                iWidth2 /= i;
            }
            options.inSampleSize = i;
        }
        return loadRegionBitmap(context, bitmapCache, uri, options, rect);
    }

    public static Bitmap loadBitmapWithBackouts(Context context, Uri uri, int i) {
        boolean z = true;
        if (i <= 0) {
            i = 1;
        }
        Bitmap bitmapLoadDownsampledBitmap = null;
        int i2 = 0;
        while (z) {
            try {
                bitmapLoadDownsampledBitmap = loadDownsampledBitmap(context, uri, i);
                z = false;
            } catch (OutOfMemoryError e) {
                i2++;
                if (i2 >= 5) {
                    throw e;
                }
                System.gc();
                i *= 2;
                bitmapLoadDownsampledBitmap = null;
            }
        }
        return bitmapLoadDownsampledBitmap;
    }

    public static XMPMeta getXmpObject(Context context) {
        try {
            try {
                context = context.getContentResolver().openInputStream(MasterImage.getImage().getUri());
            } catch (Throwable th) {
                Utils.closeSilently((Closeable) context);
                return null;
            }
            try {
                XMPMeta xMPMetaExtractXMPMeta = XmpUtilHelper.extractXMPMeta(context);
                Utils.closeSilently((Closeable) context);
                return xMPMetaExtractXMPMeta;
            } catch (FileNotFoundException e) {
                Log.e("ImageLoader", "<getXmpObject> file not found");
                Utils.closeSilently((Closeable) context);
                return null;
            }
        } catch (FileNotFoundException e2) {
            context = 0;
        } catch (Throwable th2) {
            context = 0;
            Utils.closeSilently((Closeable) context);
            return null;
        }
    }

    public static boolean queryLightCycle360(Context context) throws Throwable {
        InputStream inputStreamOpenInputStream;
        InputStream inputStream = null;
        try {
            if (MasterImage.getImage().getUri() == null) {
                Log.d("ImageLoader", "<queryLightCycle360> uri is null, return false!!");
                Utils.closeSilently((Closeable) null);
                return false;
            }
            inputStreamOpenInputStream = context.getContentResolver().openInputStream(MasterImage.getImage().getUri());
            try {
                XMPMeta xMPMetaExtractXMPMeta = XmpUtilHelper.extractXMPMeta(inputStreamOpenInputStream);
                if (xMPMetaExtractXMPMeta == null) {
                    Utils.closeSilently(inputStreamOpenInputStream);
                    return false;
                }
                if (!xMPMetaExtractXMPMeta.doesPropertyExist("http://ns.google.com/photos/1.0/panorama/", "GPano:CroppedAreaImageWidthPixels")) {
                    Utils.closeSilently(inputStreamOpenInputStream);
                    return false;
                }
                if (!xMPMetaExtractXMPMeta.doesPropertyExist("http://ns.google.com/photos/1.0/panorama/", "GPano:FullPanoWidthPixels")) {
                    Utils.closeSilently(inputStreamOpenInputStream);
                    return false;
                }
                Integer propertyInteger = xMPMetaExtractXMPMeta.getPropertyInteger("http://ns.google.com/photos/1.0/panorama/", "GPano:CroppedAreaImageWidthPixels");
                Integer propertyInteger2 = xMPMetaExtractXMPMeta.getPropertyInteger("http://ns.google.com/photos/1.0/panorama/", "GPano:FullPanoWidthPixels");
                if (propertyInteger == null || propertyInteger2 == null) {
                    Utils.closeSilently(inputStreamOpenInputStream);
                    return false;
                }
                boolean zEquals = propertyInteger.equals(propertyInteger2);
                Utils.closeSilently(inputStreamOpenInputStream);
                return zEquals;
            } catch (XMPException e) {
                Utils.closeSilently(inputStreamOpenInputStream);
                return false;
            } catch (FileNotFoundException e2) {
                Utils.closeSilently(inputStreamOpenInputStream);
                return false;
            } catch (Throwable th) {
                inputStream = inputStreamOpenInputStream;
                th = th;
                Utils.closeSilently(inputStream);
                throw th;
            }
        } catch (XMPException e3) {
            inputStreamOpenInputStream = null;
        } catch (FileNotFoundException e4) {
            inputStreamOpenInputStream = null;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public static List<ExifTag> getExif(Context context, Uri uri) {
        String localPathFromUri = getLocalPathFromUri(context, uri);
        if (localPathFromUri == null || !"image/jpeg".equals(getMimeType(Uri.parse(localPathFromUri)))) {
            return null;
        }
        try {
            ExifInterface exifInterface = new ExifInterface();
            exifInterface.readExif(localPathFromUri);
            return exifInterface.getAllTags();
        } catch (IOException e) {
            Log.w("ImageLoader", "Failed to read EXIF tags", e);
            return null;
        }
    }

    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float f, boolean z) {
        int iRound = Math.round(bitmap.getWidth() * f);
        int iRound2 = Math.round(bitmap.getHeight() * f);
        if (iRound < 1 || iRound2 < 1) {
            Log.d("ImageLoader", "scaled width or height < 1, no need to resize");
            return bitmap;
        }
        if (iRound == bitmap.getWidth() && iRound2 == bitmap.getHeight()) {
            return bitmap;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iRound, iRound2, getConfig(bitmap));
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.scale(f, f);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, new Paint(6));
        if (z) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    private static Bitmap loadBitmapFromExt(Context context, Uri uri, BitmapFactory.Options options) {
        if (sExtLoaders == null) {
            sExtLoaders = (IFilterShowImageLoader[]) FeatureManager.getInstance().getImplement(IFilterShowImageLoader.class, new Object[0]);
        }
        Bitmap bitmapLoadBitmap = null;
        for (IFilterShowImageLoader iFilterShowImageLoader : sExtLoaders) {
            bitmapLoadBitmap = iFilterShowImageLoader.loadBitmap(context, uri, options);
            if (bitmapLoadBitmap != null) {
                return bitmapLoadBitmap;
            }
        }
        return bitmapLoadBitmap;
    }

    private static int getOrientationFromExif(String str, InputStream inputStream) {
        int orientationFromExif = FeatureHelper.getOrientationFromExif(str, inputStream);
        Log.d("ImageLoader", "<getOrientationFromExif> filePath & is & ori: " + str + " - " + inputStream + " - " + orientationFromExif);
        if (orientationFromExif == 90) {
            return 6;
        }
        if (orientationFromExif == 180) {
            return 3;
        }
        if (orientationFromExif == 270) {
            return 8;
        }
        return 1;
    }
}
