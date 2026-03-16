package com.android.gallery3d.filtershow.tools;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
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
    private final Callback mCallback;
    private final Context mContext;
    private int mCurrentProcessingStep = 1;
    private final File mDestinationFile;
    private final Bitmap mPreviewImage;
    private final Uri mSelectedImageUri;
    private final Uri mSourceUri;

    public interface Callback {
        void onPreviewSaved(Uri uri);

        void onProgress(int i, int i2);
    }

    public interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    public SaveImage(Context context, Uri sourceUri, Uri selectedImageUri, File destination, Bitmap previewImage, Callback callback) {
        this.mContext = context;
        this.mSourceUri = sourceUri;
        this.mCallback = callback;
        this.mPreviewImage = previewImage;
        if (destination == null) {
            this.mDestinationFile = getNewFile(context, selectedImageUri);
        } else {
            this.mDestinationFile = destination;
        }
        this.mSelectedImageUri = selectedImageUri;
    }

    public static File getFinalSaveDirectory(Context context, Uri sourceUri) {
        File saveDirectory = getSaveDirectory(context, sourceUri);
        if (saveDirectory == null || !saveDirectory.canWrite()) {
            saveDirectory = new File(Environment.getExternalStorageDirectory(), "EditedOnlinePhotos");
        }
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        return saveDirectory;
    }

    public static File getNewFile(Context context, Uri sourceUri) {
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        String filename = new SimpleDateFormat("_yyyyMMdd_HHmmss").format((Date) new java.sql.Date(System.currentTimeMillis()));
        return hasPanoPrefix(context, sourceUri) ? new File(saveDirectory, "PANO" + filename + ".jpg") : new File(saveDirectory, "IMG" + filename + ".jpg");
    }

    public static void deleteAuxFiles(ContentResolver contentResolver, Uri srcContentUri) {
        final String[] fullPath = new String[1];
        String[] queryProjection = {"_data"};
        querySourceFromContentResolver(contentResolver, srcContentUri, queryProjection, new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                fullPath[0] = cursor.getString(0);
            }
        });
        if (fullPath[0] != null) {
            File currentFile = new File(fullPath[0]);
            String filename = currentFile.getName();
            int firstDotPos = filename.indexOf(".");
            final String filenameNoExt = firstDotPos == -1 ? filename : filename.substring(0, firstDotPos);
            File auxDir = getLocalAuxDirectory(currentFile);
            if (auxDir.exists()) {
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(new StringBuilder().append(filenameNoExt).append(".").toString());
                    }
                };
                File[] auxFiles = auxDir.listFiles(filter);
                for (File file : auxFiles) {
                    file.delete();
                }
            }
        }
    }

    public Object getPanoramaXMPData(Uri source, ImagePreset preset) {
        Object xmp = null;
        if (preset.isPanoramaSafe()) {
            InputStream is = null;
            try {
                is = this.mContext.getContentResolver().openInputStream(source);
                xmp = XmpUtilHelper.extractXMPMeta(is);
            } catch (FileNotFoundException e) {
                Log.w("SaveImage", "Failed to get XMP data from image: ", e);
            } finally {
                Utils.closeSilently(is);
            }
        }
        return xmp;
    }

    public boolean putPanoramaXMPData(File file, Object xmp) {
        if (xmp != null) {
            return XmpUtilHelper.writeXMPMeta(file.getAbsolutePath(), xmp);
        }
        return false;
    }

    public ExifInterface getExifData(Uri source) {
        ExifInterface exif = new ExifInterface();
        String mimeType = this.mContext.getContentResolver().getType(this.mSelectedImageUri);
        if (mimeType == null && (mimeType = ImageLoader.getMimeType(this.mSelectedImageUri)) == null) {
            return null;
        }
        if (mimeType.equals("image/jpeg")) {
            InputStream inStream = null;
            try {
                inStream = this.mContext.getContentResolver().openInputStream(source);
                exif.readExif(inStream);
            } catch (IOException e) {
                Log.w("SaveImage", "Cannot read exif for: " + source, e);
            } catch (FileNotFoundException e2) {
                Log.w("SaveImage", "Cannot find file: " + source, e2);
            } finally {
                Utils.closeSilently(inStream);
            }
            return exif;
        }
        return exif;
    }

    public boolean putExifData(File file, ExifInterface exif, Bitmap image, int jpegCompressQuality) {
        boolean ret = false;
        OutputStream s = null;
        try {
            s = exif.getExifWriterStream(file.getAbsolutePath());
            Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;
            if (jpegCompressQuality <= 0) {
                jpegCompressQuality = 1;
            }
            image.compress(compressFormat, jpegCompressQuality, s);
            s.flush();
            s.close();
            s = null;
            ret = true;
        } catch (FileNotFoundException e) {
            Log.w("SaveImage", "File not found: " + file.getAbsolutePath(), e);
        } catch (IOException e2) {
            Log.w("SaveImage", "Could not write exif: ", e2);
        } finally {
            Utils.closeSilently(s);
        }
        return ret;
    }

    private Uri resetToOriginalImageIfNeeded(ImagePreset preset, boolean doAuxBackup) {
        File srcFile;
        if (preset.hasModifications() || (srcFile = getLocalFileFromUri(this.mContext, this.mSourceUri)) == null) {
            return null;
        }
        srcFile.renameTo(this.mDestinationFile);
        Uri uri = linkNewFileToUri(this.mContext, this.mSelectedImageUri, this.mDestinationFile, System.currentTimeMillis(), doAuxBackup);
        return uri;
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

    private void updateExifData(ExifInterface exif, long time) {
        exif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, time, TimeZone.getDefault());
        exif.setTag(exif.buildTag(ExifInterface.TAG_ORIENTATION, (short) 1));
        exif.removeCompressedThumbnail();
    }

    public Uri processAndSaveImage(ImagePreset preset, boolean flatten, int quality, float sizeFactor, boolean exit) {
        Bitmap bitmap;
        Uri uri = null;
        if (exit) {
            uri = resetToOriginalImageIfNeeded(preset, !flatten);
        }
        if (uri != null) {
            return null;
        }
        resetProgress();
        boolean noBitmap = true;
        int num_tries = 0;
        int sampleSize = 1;
        Uri newSourceUri = this.mSourceUri;
        if (!flatten) {
            newSourceUri = moveSrcToAuxIfNeeded(this.mSourceUri, this.mDestinationFile);
        }
        Uri savedUri = this.mSelectedImageUri;
        if (this.mPreviewImage != null) {
            if (flatten) {
                Object xmp = getPanoramaXMPData(newSourceUri, preset);
                ExifInterface exif = getExifData(newSourceUri);
                if (exif == null) {
                    return null;
                }
                long time = System.currentTimeMillis();
                updateExifData(exif, time);
                if (putExifData(this.mDestinationFile, exif, this.mPreviewImage, quality)) {
                    putPanoramaXMPData(this.mDestinationFile, xmp);
                    ContentValues values = getContentValues(this.mContext, this.mSelectedImageUri, this.mDestinationFile, time);
                    this.mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                }
            } else {
                Object xmp2 = getPanoramaXMPData(newSourceUri, preset);
                ExifInterface exif2 = getExifData(newSourceUri);
                if (exif2 == null) {
                    return null;
                }
                long time2 = System.currentTimeMillis();
                updateExifData(exif2, time2);
                if (putExifData(this.mDestinationFile, exif2, this.mPreviewImage, quality)) {
                    putPanoramaXMPData(this.mDestinationFile, xmp2);
                    if (!flatten) {
                        XmpPresets.writeFilterXMP(this.mContext, newSourceUri, this.mDestinationFile, preset);
                    }
                    savedUri = linkNewFileToUri(this.mContext, this.mSelectedImageUri, this.mDestinationFile, time2, !flatten);
                }
            }
            if (this.mCallback != null) {
                this.mCallback.onPreviewSaved(savedUri);
            }
        }
        while (noBitmap) {
            try {
                updateProgress();
                bitmap = ImageLoader.loadOrientedBitmapWithBackouts(this.mContext, newSourceUri, sampleSize);
            } catch (OutOfMemoryError e) {
                num_tries++;
                if (num_tries >= 5) {
                    throw e;
                }
                System.gc();
                sampleSize *= 2;
                resetProgress();
            }
            if (bitmap == null) {
                return null;
            }
            if (sizeFactor != 1.0f) {
                int w = (int) (bitmap.getWidth() * sizeFactor);
                int h = (int) (bitmap.getHeight() * sizeFactor);
                if (w == 0 || h == 0) {
                    w = 1;
                    h = 1;
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
            }
            updateProgress();
            CachingPipeline pipeline = new CachingPipeline(FiltersManager.getManager(), "Saving");
            Bitmap bitmap2 = pipeline.renderFinalImage(bitmap, preset);
            updateProgress();
            Object xmp3 = getPanoramaXMPData(newSourceUri, preset);
            ExifInterface exif3 = getExifData(newSourceUri);
            if (exif3 == null) {
                return null;
            }
            long time3 = System.currentTimeMillis();
            updateProgress();
            updateExifData(exif3, time3);
            updateProgress();
            if (putExifData(this.mDestinationFile, exif3, bitmap2, quality)) {
                putPanoramaXMPData(this.mDestinationFile, xmp3);
                if (!flatten) {
                    XmpPresets.writeFilterXMP(this.mContext, newSourceUri, this.mDestinationFile, preset);
                    uri = updateFile(this.mContext, savedUri, this.mDestinationFile, time3);
                } else {
                    ContentValues values2 = getContentValues(this.mContext, this.mSelectedImageUri, this.mDestinationFile, time3);
                    this.mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values2);
                }
            }
            updateProgress();
            noBitmap = false;
        }
        return uri;
    }

    private Uri moveSrcToAuxIfNeeded(Uri srcUri, File dstFile) {
        File srcFile = getLocalFileFromUri(this.mContext, srcUri);
        if (srcFile == null) {
            Log.d("SaveImage", "Source file is not a local file, no update.");
            return srcUri;
        }
        File auxDiretory = getLocalAuxDirectory(dstFile);
        if (!auxDiretory.exists()) {
            boolean success = auxDiretory.mkdirs();
            if (!success) {
                return srcUri;
            }
        }
        File noMedia = new File(auxDiretory, ".nomedia");
        if (!noMedia.exists()) {
            try {
                noMedia.createNewFile();
            } catch (IOException e) {
                Log.e("SaveImage", "Can't create the nomedia");
                return srcUri;
            }
        }
        File newSrcFile = new File(auxDiretory, dstFile.getName());
        String to = newSrcFile.getName();
        String from = srcFile.getName();
        String to2 = to.substring(to.lastIndexOf("."));
        String from2 = from.substring(from.lastIndexOf("."));
        if (!to2.equals(from2)) {
            String name = dstFile.getName();
            newSrcFile = new File(auxDiretory, name.substring(0, name.lastIndexOf(".")) + from2);
        }
        if (!newSrcFile.exists()) {
            boolean success2 = srcFile.renameTo(newSrcFile);
            if (!success2) {
                return srcUri;
            }
        }
        return Uri.fromFile(newSrcFile);
    }

    private static File getLocalAuxDirectory(File dstFile) {
        File dstDirectory = dstFile.getParentFile();
        File auxDiretory = new File(dstDirectory + "/.aux");
        return auxDiretory;
    }

    public static Uri makeAndInsertUri(Context context, Uri sourceUri) {
        long time = System.currentTimeMillis();
        String filename = new SimpleDateFormat("_yyyyMMdd_HHmmss").format((Date) new java.sql.Date(time));
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        File file = new File(saveDirectory, filename + ".JPG");
        return linkNewFileToUri(context, sourceUri, file, time, false);
    }

    public static void saveImage(ImagePreset preset, FilterShowActivity filterShowActivity, File destination) {
        Uri selectedImageUri = filterShowActivity.getSelectedImageUri();
        Uri sourceImageUri = MasterImage.getImage().getUri();
        boolean flatten = true;
        if (preset.contains((byte) 6)) {
            flatten = true;
        }
        Intent processIntent = ProcessingService.getSaveIntent(filterShowActivity, preset, destination, selectedImageUri, sourceImageUri, flatten, 90, 1.0f, true);
        filterShowActivity.startService(processIntent);
        if (!filterShowActivity.isSimpleEditAction()) {
            String toastMessage = filterShowActivity.getResources().getString(R.string.save_and_processing);
            Toast.makeText(filterShowActivity, toastMessage, 0).show();
        }
    }

    public static void querySource(Context context, Uri sourceUri, String[] projection, ContentResolverQueryCallback callback) {
        ContentResolver contentResolver = context.getContentResolver();
        querySourceFromContentResolver(contentResolver, sourceUri, projection, callback);
    }

    private static void querySourceFromContentResolver(ContentResolver contentResolver, Uri sourceUri, String[] projection, ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(sourceUri, projection, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                callback.onCursorResult(cursor);
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    private static File getSaveDirectory(Context context, Uri sourceUri) {
        File file = getLocalFileFromUri(context, sourceUri);
        if (file != null) {
            return file.getParentFile();
        }
        return null;
    }

    private static File getLocalFileFromUri(Context context, Uri srcUri) {
        if (srcUri == null) {
            Log.e("SaveImage", "srcUri is null.");
            return null;
        }
        String scheme = srcUri.getScheme();
        if (scheme == null) {
            Log.e("SaveImage", "scheme is null.");
            return null;
        }
        final File[] file = new File[1];
        if (scheme.equals("content")) {
            if (srcUri.getAuthority().equals("media")) {
                querySource(context, srcUri, new String[]{"_data"}, new ContentResolverQueryCallback() {
                    @Override
                    public void onCursorResult(Cursor cursor) {
                        file[0] = new File(cursor.getString(0));
                    }
                });
            }
        } else if (scheme.equals("file")) {
            file[0] = new File(srcUri.getPath());
        }
        return file[0];
    }

    private static String getTrueFilename(Context context, Uri src) {
        if (context == null || src == null) {
            return null;
        }
        final String[] trueName = new String[1];
        querySource(context, src, new String[]{"_data"}, new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                trueName[0] = new File(cursor.getString(0)).getName();
            }
        });
        return trueName[0];
    }

    private static boolean hasPanoPrefix(Context context, Uri src) {
        String name = getTrueFilename(context, src);
        return name != null && name.startsWith("PANO");
    }

    public static Uri linkNewFileToUri(Context context, Uri sourceUri, File file, long time, boolean deleteOriginal) {
        File oldSelectedFile = getLocalFileFromUri(context, sourceUri);
        ContentValues values = getContentValues(context, sourceUri, file, time);
        boolean fileUri = isFileUri(sourceUri);
        if (fileUri || oldSelectedFile == null || !deleteOriginal) {
            Uri result = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            return result;
        }
        context.getContentResolver().update(sourceUri, values, null, null);
        if (!oldSelectedFile.exists()) {
            return sourceUri;
        }
        oldSelectedFile.delete();
        return sourceUri;
    }

    public static Uri updateFile(Context context, Uri sourceUri, File file, long time) {
        ContentValues values = getContentValues(context, sourceUri, file, time);
        context.getContentResolver().update(sourceUri, values, null, null);
        return sourceUri;
    }

    private static ContentValues getContentValues(Context context, Uri sourceUri, File file, long time) {
        final ContentValues values = new ContentValues();
        long time2 = time / 1000;
        values.put("title", file.getName());
        values.put("_display_name", file.getName());
        values.put("mime_type", "image/jpeg");
        values.put("datetaken", Long.valueOf(time2));
        values.put("date_modified", Long.valueOf(time2));
        values.put("date_added", Long.valueOf(time2));
        values.put("orientation", (Integer) 0);
        values.put("_data", file.getAbsolutePath());
        values.put("_size", Long.valueOf(file.length()));
        values.put("mini_thumb_magic", (Integer) 0);
        String[] projection = {"datetaken", "latitude", "longitude"};
        querySource(context, sourceUri, projection, new ContentResolverQueryCallback() {
            @Override
            public void onCursorResult(Cursor cursor) {
                values.put("datetaken", Long.valueOf(cursor.getLong(0)));
                double latitude = cursor.getDouble(1);
                double longitude = cursor.getDouble(2);
                if (latitude != 0.0d || longitude != 0.0d) {
                    values.put("latitude", Double.valueOf(latitude));
                    values.put("longitude", Double.valueOf(longitude));
                }
            }
        });
        return values;
    }

    private static boolean isFileUri(Uri sourceUri) {
        String scheme = sourceUri.getScheme();
        return scheme != null && scheme.equals("file");
    }
}
