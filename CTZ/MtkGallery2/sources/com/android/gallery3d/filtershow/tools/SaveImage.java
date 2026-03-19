package com.android.gallery3d.filtershow.tools;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import com.adobe.xmp.XMPMeta;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.CachingPipeline;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.pipeline.ProcessingService;
import com.android.gallery3d.util.XmpUtilHelper;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.base.IFilterShowImageSaver;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SaveImage {
    private static final int EDIT_PHOTO_SIZE_LIMIT;
    private static boolean sClearRefocusFlag = false;
    private static IFilterShowImageSaver[] sExtSavers;
    private final Callback mCallback;
    private final Context mContext;
    private int mCurrentProcessingStep = 1;
    private final File mDestinationFile;
    private final Bitmap mPreviewImage;
    private final Uri mSelectedImageUri;
    private final Uri mSourceUri;

    public interface Callback {
        void onProgress(int i, int i2);
    }

    public interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    public SaveImage(Context context, Uri uri, Uri uri2, File file, Bitmap bitmap, Callback callback) {
        this.mContext = context;
        this.mSourceUri = uri;
        this.mCallback = callback;
        this.mPreviewImage = bitmap;
        if (file == null) {
            this.mDestinationFile = getNewFile(context, uri2);
        } else {
            this.mDestinationFile = file;
        }
        this.mSelectedImageUri = uri2;
    }

    public static File getFinalSaveDirectory(Context context, Uri uri) throws Throwable {
        File saveDirectory = getSaveDirectory(context, uri);
        if (saveDirectory == null || !saveDirectory.canWrite()) {
            saveDirectory = new File(Environment.getExternalStorageDirectory(), "EditedOnlinePhotos");
        }
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        return saveDirectory;
    }

    public static File getNewFile(Context context, Uri uri) throws Throwable {
        File finalSaveDirectory = getFinalSaveDirectory(context, uri);
        String str = new SimpleDateFormat("_yyyyMMdd_HHmmss_SSS").format(new Date(System.currentTimeMillis()));
        String str2 = hasPanoPrefix(context, uri) ? "PANO" : "IMG";
        File file = new File(finalSaveDirectory, str2 + str + ".jpg");
        if (!file.exists()) {
            return file;
        }
        Log.d("SaveImage", "<getNewFile> " + file.getAbsolutePath() + " already exists, then make new file!!");
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(new SimpleDateFormat("_SSS").format(new Date(System.currentTimeMillis())));
        return new File(finalSaveDirectory, str2 + sb.toString() + ".jpg");
    }

    public static void deleteAuxFiles(ContentResolver contentResolver, Uri uri) throws Throwable {
        final String[] strArr = new String[1];
        querySourceFromContentResolver(contentResolver, uri, new String[]{BookmarkEnhance.COLUMN_DATA}, new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                strArr[0] = cursor.getString(0);
            }
        });
        if (strArr[0] != null) {
            File file = new File(strArr[0]);
            final String name = file.getName();
            int iIndexOf = name.indexOf(".");
            if (iIndexOf != -1) {
                name = name.substring(0, iIndexOf);
            }
            File localAuxDirectory = getLocalAuxDirectory(file);
            if (localAuxDirectory.exists()) {
                for (File file2 : localAuxDirectory.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file3, String str) {
                        if (str.startsWith(name + ".")) {
                            return true;
                        }
                        return false;
                    }
                })) {
                    file2.delete();
                }
            }
        }
    }

    public Object getPanoramaXMPData(Uri uri, ImagePreset imagePreset) throws Throwable {
        InputStream inputStreamOpenInputStream;
        try {
            if (imagePreset.isPanoramaSafe()) {
                try {
                    inputStreamOpenInputStream = this.mContext.getContentResolver().openInputStream(uri);
                    try {
                        XMPMeta xMPMetaExtractXMPMeta = XmpUtilHelper.extractXMPMeta(inputStreamOpenInputStream);
                        Utils.closeSilently(inputStreamOpenInputStream);
                        return xMPMetaExtractXMPMeta;
                    } catch (FileNotFoundException e) {
                        e = e;
                        Log.w("SaveImage", "Failed to get XMP data from image: ", e);
                        Utils.closeSilently(inputStreamOpenInputStream);
                        return null;
                    }
                } catch (FileNotFoundException e2) {
                    e = e2;
                    inputStreamOpenInputStream = null;
                } catch (Throwable th) {
                    th = th;
                    uri = 0;
                    Utils.closeSilently((Closeable) uri);
                    throw th;
                }
            }
            return null;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public boolean putPanoramaXMPData(File file, Object obj) {
        if (obj != null) {
            return XmpUtilHelper.writeXMPMeta(file.getAbsolutePath(), obj);
        }
        return false;
    }

    public ExifInterface getExifData(Uri uri) throws Throwable {
        InputStream inputStreamOpenInputStream;
        ExifInterface exifInterface = new ExifInterface();
        String type = this.mContext.getContentResolver().getType(this.mSelectedImageUri);
        if ((type != null || (type = ImageLoader.getMimeType(this.mSelectedImageUri)) != null) && type.equals("image/jpeg")) {
            InputStream inputStream = null;
            try {
                try {
                    inputStreamOpenInputStream = this.mContext.getContentResolver().openInputStream(uri);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e) {
                e = e;
            } catch (IOException e2) {
                e = e2;
            }
            try {
                exifInterface.readExif(inputStreamOpenInputStream);
                Utils.closeSilently(inputStreamOpenInputStream);
            } catch (FileNotFoundException e3) {
                e = e3;
                inputStream = inputStreamOpenInputStream;
                Log.w("SaveImage", "Cannot find file: " + uri, e);
                Utils.closeSilently(inputStream);
            } catch (IOException e4) {
                e = e4;
                inputStream = inputStreamOpenInputStream;
                Log.w("SaveImage", "Cannot read exif for: " + uri, e);
                Utils.closeSilently(inputStream);
            } catch (Throwable th2) {
                th = th2;
                inputStream = inputStreamOpenInputStream;
                Utils.closeSilently(inputStream);
                throw th;
            }
        }
        return exifInterface;
    }

    public boolean putExifData(File file, ExifInterface exifInterface, Bitmap bitmap, int i) throws Throwable {
        OutputStream exifWriterStream;
        OutputStream outputStream = null;
        try {
            try {
                exifWriterStream = exifInterface.getExifWriterStream(file.getAbsolutePath());
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
            e = e;
        } catch (IOException e2) {
            e = e2;
        }
        try {
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
            if (i <= 0) {
                i = 1;
            }
            bitmap.compress(compressFormat, i, exifWriterStream);
            exifWriterStream.flush();
            exifWriterStream.close();
            Utils.closeSilently((Closeable) null);
            return true;
        } catch (FileNotFoundException e3) {
            e = e3;
            outputStream = exifWriterStream;
            Log.w("SaveImage", "File not found: " + file.getAbsolutePath(), e);
            Utils.closeSilently(outputStream);
            return false;
        } catch (IOException e4) {
            e = e4;
            outputStream = exifWriterStream;
            Log.w("SaveImage", "Could not write exif: ", e);
            Utils.closeSilently(outputStream);
            return false;
        } catch (Throwable th2) {
            th = th2;
            outputStream = exifWriterStream;
            Utils.closeSilently(outputStream);
            throw th;
        }
    }

    private Uri resetToOriginalImageIfNeeded(ImagePreset imagePreset, boolean z) {
        File localFileFromUri;
        if (!imagePreset.hasModifications() && (localFileFromUri = getLocalFileFromUri(this.mContext, this.mSourceUri)) != null) {
            localFileFromUri.renameTo(this.mDestinationFile);
            return linkNewFileToUri(this.mContext, this.mSelectedImageUri, this.mDestinationFile, System.currentTimeMillis(), z);
        }
        return null;
    }

    private void resetProgress() {
        this.mCurrentProcessingStep = 0;
    }

    private void updateProgress() {
        if (this.mCallback != null) {
            Callback callback = this.mCallback;
            int i = this.mCurrentProcessingStep + 1;
            this.mCurrentProcessingStep = i;
            callback.onProgress(6, i);
        }
    }

    private void updateExifData(ExifInterface exifInterface, long j) {
        exifInterface.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, j, TimeZone.getDefault());
        exifInterface.setTag(exifInterface.buildTag(ExifInterface.TAG_ORIENTATION, (short) 1));
        exifInterface.removeCompressedThumbnail();
    }

    public Uri processAndSaveImage(ImagePreset imagePreset, boolean z, int i, float f, boolean z2) throws Throwable {
        Uri uriResetToOriginalImageIfNeeded;
        File outPutFile;
        Uri uri;
        boolean z3;
        int i2;
        int i3;
        Bitmap bitmapLoadBitmapWithBackouts;
        float f2 = f;
        Uri uri2 = null;
        if (z2) {
            uriResetToOriginalImageIfNeeded = resetToOriginalImageIfNeeded(imagePreset, !z);
        } else {
            uriResetToOriginalImageIfNeeded = null;
        }
        if (uriResetToOriginalImageIfNeeded != null) {
            return null;
        }
        resetProgress();
        Uri uri3 = this.mSourceUri;
        if (!z) {
            outPutFile = getOutPutFile(this.mContext, this.mSourceUri);
        } else {
            outPutFile = null;
        }
        int metadataOrientation = ImageLoader.getMetadataOrientation(this.mContext, this.mSourceUri);
        Uri uri4 = this.mSelectedImageUri;
        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        boolean z4 = true;
        if (originalBounds == null) {
            uri = uriResetToOriginalImageIfNeeded;
            z3 = true;
            i2 = 1;
        } else {
            int iCeil = (int) Math.ceil((((double) originalBounds.width()) * ((double) originalBounds.height())) / ((double) EDIT_PHOTO_SIZE_LIMIT));
            if (iCeil < 1) {
                iCeil = 1;
            }
            Log.d("SaveImage", "<processAndSaveImage> sIsLowRamDevice " + FeatureConfig.sIsLowRamDevice + ", decode sample size " + iCeil + ", sizeFactor for render " + f2);
            uri = uriResetToOriginalImageIfNeeded;
            i2 = iCeil;
            z3 = true;
        }
        int i4 = 0;
        while (z3) {
            try {
                updateProgress();
                bitmapLoadBitmapWithBackouts = ImageLoader.loadBitmapWithBackouts(this.mContext, uri3, i2);
            } catch (OutOfMemoryError e) {
                e = e;
                i3 = i2;
            }
            if (bitmapLoadBitmapWithBackouts == null) {
                return uri2;
            }
            Bitmap bitmapOrientBitmap = ImageLoader.orientBitmap(bitmapLoadBitmapWithBackouts, metadataOrientation);
            if (f2 != 1.0f) {
                int width = (int) (bitmapOrientBitmap.getWidth() * f2);
                int height = (int) (bitmapOrientBitmap.getHeight() * f2);
                ?? r4 = height;
                ?? r14 = width;
                if (width == 0 || height == 0) {
                    boolean z5 = z4;
                    r14 = z5;
                    r4 = z5;
                }
                bitmapOrientBitmap = Bitmap.createScaledBitmap(bitmapOrientBitmap, r14, r4, z4);
            }
            updateProgress();
            Bitmap bitmapRenderFinalImage = new CachingPipeline(FiltersManager.getManager(), "Saving").renderFinalImage(bitmapOrientBitmap, imagePreset);
            updateProgress();
            Object panoramaXMPData = getPanoramaXMPData(uri3, imagePreset);
            ExifInterface exifData = getExifData(uri3);
            i3 = i2;
            try {
                long jCurrentTimeMillis = System.currentTimeMillis();
                updateProgress();
                updateExifData(exifData, jCurrentTimeMillis);
                updateExifDataFromExt(uri3);
                updateProgress();
                try {
                    if (putExifData(this.mDestinationFile, exifData, bitmapRenderFinalImage, i)) {
                        putPanoramaXMPData(this.mDestinationFile, panoramaXMPData);
                        if (!z) {
                            XmpPresets.writeFilterXMP(this.mContext, uri3, this.mDestinationFile, imagePreset);
                        }
                        Uri uriLinkNewFileToUri = linkNewFileToUri(this.mContext, this.mSelectedImageUri, this.mDestinationFile, jCurrentTimeMillis, !z);
                        try {
                            updataImageDimensionInDB(this.mContext, this.mDestinationFile, bitmapRenderFinalImage.getWidth(), bitmapRenderFinalImage.getHeight());
                            uri = uriLinkNewFileToUri;
                        } catch (OutOfMemoryError e2) {
                            e = e2;
                            uri = uriLinkNewFileToUri;
                            i4++;
                            if (i4 >= 5) {
                                throw e;
                            }
                            System.gc();
                            i2 = i3 * 2;
                            resetProgress();
                            f2 = f;
                            uri2 = null;
                        }
                    }
                    updateProgress();
                    i2 = i3;
                    f2 = f;
                    uri2 = null;
                    z3 = false;
                } catch (OutOfMemoryError e3) {
                    e = e3;
                }
            } catch (OutOfMemoryError e4) {
                e = e4;
            }
            z4 = true;
        }
        if (!z && outPutFile != null) {
            deleteOldFile(outPutFile);
        }
        return uri;
    }

    private static File getLocalAuxDirectory(File file) {
        return new File(file.getParentFile() + "/.aux");
    }

    public static Uri makeAndInsertUri(Context context, Uri uri) throws Throwable {
        long jCurrentTimeMillis = System.currentTimeMillis();
        String str = new SimpleDateFormat("_yyyyMMdd_HHmmss_SSS").format(new Date(jCurrentTimeMillis));
        return linkNewFileToUri(context, uri, new File(getFinalSaveDirectory(context, uri), "IMG" + str + ".JPG"), jCurrentTimeMillis, false);
    }

    public static void saveImage(ImagePreset imagePreset, FilterShowActivity filterShowActivity, File file) {
        filterShowActivity.startService(ProcessingService.getSaveIntent(filterShowActivity, imagePreset, file, filterShowActivity.getSelectedImageUri(), MasterImage.getImage().getUri(), imagePreset.contains((byte) 6), 90, 1.0f, true));
        if (!filterShowActivity.isSimpleEditAction()) {
            Toast.makeText(filterShowActivity, filterShowActivity.getResources().getString(R.string.save_and_processing), 0).show();
        }
    }

    public static void querySource(Context context, Uri uri, String[] strArr, ContentResolverQueryCallback contentResolverQueryCallback) throws Throwable {
        querySourceFromContentResolver(context.getContentResolver(), uri, strArr, contentResolverQueryCallback);
    }

    private static void querySourceFromContentResolver(ContentResolver contentResolver, Uri uri, String[] strArr, ContentResolverQueryCallback contentResolverQueryCallback) throws Throwable {
        Cursor cursorQuery;
        try {
            cursorQuery = contentResolver.query(uri, strArr, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToNext()) {
                        contentResolverQueryCallback.onCursorResult(cursorQuery);
                    }
                } catch (Exception e) {
                    if (cursorQuery == null) {
                        return;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery == null) {
                return;
            }
        } catch (Exception e2) {
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
        cursorQuery.close();
    }

    private static File getSaveDirectory(Context context, Uri uri) throws Throwable {
        File localFileFromUri = getLocalFileFromUri(context, uri);
        if (localFileFromUri != null) {
            return localFileFromUri.getParentFile();
        }
        return null;
    }

    private static File getLocalFileFromUri(Context context, Uri uri) throws Throwable {
        if (uri == null) {
            Log.e("SaveImage", "srcUri is null.");
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            Log.e("SaveImage", "scheme is null.");
            return null;
        }
        final File[] fileArr = new File[1];
        if (scheme.equals("content")) {
            if (uri.getAuthority().equals("media")) {
                querySource(context, uri, new String[]{BookmarkEnhance.COLUMN_DATA}, new ContentResolverQueryCallback() {
                    @Override
                    public void onCursorResult(Cursor cursor) {
                        fileArr[0] = new File(cursor.getString(0));
                    }
                });
            }
        } else if (scheme.equals("file")) {
            fileArr[0] = new File(uri.getPath());
        }
        return fileArr[0];
    }

    private static String getTrueFilename(Context context, Uri uri) throws Throwable {
        if (context == null || uri == null) {
            return null;
        }
        final String[] strArr = new String[1];
        querySource(context, uri, new String[]{BookmarkEnhance.COLUMN_DATA}, new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                strArr[0] = new File(cursor.getString(0)).getName();
            }
        });
        return strArr[0];
    }

    private static boolean hasPanoPrefix(Context context, Uri uri) throws Throwable {
        String trueFilename = getTrueFilename(context, uri);
        return trueFilename != null && trueFilename.startsWith("PANO");
    }

    public static Uri linkNewFileToUri(Context context, Uri uri, File file, long j, boolean z) throws Throwable {
        File localFileFromUri = getLocalFileFromUri(context, uri);
        ContentValues contentValues = getContentValues(context, uri, file, j);
        if (isFileUri(uri) || localFileFromUri == null || !z) {
            return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        }
        context.getContentResolver().update(uri, contentValues, null, null);
        if (localFileFromUri.exists()) {
            localFileFromUri.delete();
            return uri;
        }
        return uri;
    }

    private static ContentValues getContentValues(Context context, Uri uri, File file, long j) throws Throwable {
        final ContentValues contentValues = new ContentValues();
        long j2 = j / 1000;
        contentValues.put("title", file.getName());
        contentValues.put(BookmarkEnhance.COLUMN_TITLE, file.getName());
        contentValues.put(BookmarkEnhance.COLUMN_MEDIA_TYPE, "image/jpeg");
        contentValues.put("datetaken", Long.valueOf(j2));
        contentValues.put("date_modified", Long.valueOf(j2));
        contentValues.put(BookmarkEnhance.COLUMN_ADD_DATE, Long.valueOf(j2));
        contentValues.put(ExtFieldsUtils.VIDEO_ROTATION_FIELD, (Integer) 0);
        contentValues.put(BookmarkEnhance.COLUMN_DATA, file.getAbsolutePath());
        contentValues.put("_size", Long.valueOf(file.length()));
        contentValues.put("mini_thumb_magic", (Integer) 0);
        querySource(context, uri, new String[]{"datetaken", "latitude", "longitude"}, new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                contentValues.put("datetaken", Long.valueOf(cursor.getLong(0)));
                double d = cursor.getDouble(1);
                double d2 = cursor.getDouble(2);
                if (d != 0.0d || d2 != 0.0d) {
                    contentValues.put("latitude", Double.valueOf(d));
                    contentValues.put("longitude", Double.valueOf(d2));
                }
            }
        });
        return contentValues;
    }

    private static boolean isFileUri(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equals("file")) {
            return true;
        }
        return false;
    }

    public static boolean updataImageDimensionInDB(Context context, File file, int i, int i2) {
        if (file == null) {
            return false;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("width", Integer.valueOf(i));
        contentValues.put("height", Integer.valueOf(i2));
        contentValues.put("_size", Long.valueOf(file.length()));
        updateMediaDatabaseFromExt(file, contentValues);
        int iUpdate = context.getContentResolver().update(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues, "_data=?", new String[]{file.getAbsolutePath()});
        Log.d("SaveImage", "updataImageDimensionInDB for " + file.getAbsolutePath() + ", r = " + iUpdate);
        return iUpdate > 0;
    }

    public static File getOutPutFile(Context context, Uri uri) throws Throwable {
        final String[] strArr = new String[1];
        querySource(context, uri, new String[]{BookmarkEnhance.COLUMN_DATA}, new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                strArr[0] = cursor.getString(0);
            }
        });
        Log.d("SaveImage", " <getOutPutFile> filePath=" + strArr[0]);
        if (strArr[0] != null) {
            return new File(strArr[0]);
        }
        return null;
    }

    private void deleteOldFile(File file) {
        if (file != null) {
            Log.d("SaveImage", "<deleteOldFile> fullPath=" + file);
            if (!file.delete()) {
                Log.w("SaveImage", "<deleteOldFile> can not deleteOldFile: " + file);
            }
        }
    }

    static {
        EDIT_PHOTO_SIZE_LIMIT = (FeatureConfig.sIsLowRamDevice || FeatureConfig.IS_GMO_RAM_OPTIMIZE) ? 5242880 : 10485760;
    }

    private static void updateExifDataFromExt(Uri uri) {
        if (sExtSavers == null) {
            sExtSavers = (IFilterShowImageSaver[]) FeatureManager.getInstance().getImplement(IFilterShowImageSaver.class, new Object[0]);
        }
        for (IFilterShowImageSaver iFilterShowImageSaver : sExtSavers) {
            iFilterShowImageSaver.updateExifData(uri);
        }
    }

    private static void updateMediaDatabaseFromExt(File file, ContentValues contentValues) {
        if (sExtSavers == null) {
            sExtSavers = (IFilterShowImageSaver[]) FeatureManager.getInstance().getImplement(IFilterShowImageSaver.class, new Object[0]);
        }
        for (IFilterShowImageSaver iFilterShowImageSaver : sExtSavers) {
            iFilterShowImageSaver.updateMediaDatabase(file, contentValues);
        }
    }
}
