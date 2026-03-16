package com.android.camera;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.tinyplanet.TinyPlanetFragment;
import com.android.camera.util.ApiHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Storage {
    public static final String CAMERA_SESSION_SCHEME = "camera_session";
    private static final String GOOGLE_COM = "google.com";
    public static final String JPEG_POSTFIX = ".jpg";
    public static final long LOW_STORAGE_THRESHOLD_BYTES = 50000000;
    public static final long PREPARING = -2;
    public static final long UNAVAILABLE = -1;
    public static final long UNKNOWN_SIZE = -3;
    public static final String DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    public static final String DIRECTORY = DCIM + "/Camera";
    public static final String BUCKET_ID = String.valueOf(DIRECTORY.toLowerCase().hashCode());
    private static final Log.Tag TAG = new Log.Tag("Storage");
    private static HashMap<Uri, Uri> sSessionsToContentUris = new HashMap<>();
    private static HashMap<Uri, Uri> sContentUrisToSessions = new HashMap<>();
    private static HashMap<Uri, byte[]> sSessionsToPlaceholderBytes = new HashMap<>();
    private static HashMap<Uri, Point> sSessionsToSizes = new HashMap<>();
    private static HashMap<Uri, Integer> sSessionsToPlaceholderVersions = new HashMap<>();

    public static Uri addImage(ContentResolver resolver, String title, long date, Location location, int orientation, ExifInterface exif, byte[] jpeg, int width, int height) {
        return addImage(resolver, title, date, location, orientation, exif, jpeg, width, height, "image/jpeg");
    }

    private static Uri addImage(ContentResolver resolver, String title, long date, Location location, int orientation, ExifInterface exif, byte[] data, int width, int height, String mimeType) {
        String path = generateFilepath(title);
        long fileLength = writeFile(path, data, exif);
        if (fileLength >= 0) {
            return addImageToMediaStore(resolver, title, date, location, orientation, fileLength, path, width, height, mimeType);
        }
        return null;
    }

    private static Uri addImageToMediaStore(ContentResolver resolver, String title, long date, Location location, int orientation, long jpegLength, String path, int width, int height, String mimeType) {
        ContentValues values = getContentValuesForData(title, date, location, orientation, jpegLength, path, width, height, mimeType);
        try {
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            return uri;
        } catch (Throwable th) {
            Log.e(TAG, "Failed to write MediaStore" + th);
            return null;
        }
    }

    public static ContentValues getContentValuesForData(String title, long date, Location location, int orientation, long jpegLength, String path, int width, int height, String mimeType) {
        File file = new File(path);
        long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(file.lastModified());
        ContentValues values = new ContentValues(11);
        values.put(TinyPlanetFragment.ARGUMENT_TITLE, title);
        values.put("_display_name", title + JPEG_POSTFIX);
        values.put("datetaken", Long.valueOf(date));
        values.put("mime_type", mimeType);
        values.put("date_modified", Long.valueOf(dateModifiedSeconds));
        values.put("orientation", Integer.valueOf(orientation));
        values.put("_data", path);
        values.put("_size", Long.valueOf(jpegLength));
        setImageSize(values, width, height);
        if (location != null) {
            values.put("latitude", Double.valueOf(location.getLatitude()));
            values.put("longitude", Double.valueOf(location.getLongitude()));
        }
        return values;
    }

    public static Uri addPlaceholder(byte[] jpeg, int width, int height) {
        Uri.Builder builder = new Uri.Builder();
        String uuid = UUID.randomUUID().toString();
        builder.scheme(CAMERA_SESSION_SCHEME).authority(GOOGLE_COM).appendPath(uuid);
        Uri uri = builder.build();
        replacePlaceholder(uri, jpeg, width, height);
        return uri;
    }

    public static void replacePlaceholder(Uri uri, byte[] jpeg, int width, int height) {
        Point size = new Point(width, height);
        sSessionsToSizes.put(uri, size);
        sSessionsToPlaceholderBytes.put(uri, jpeg);
        Integer currentVersion = sSessionsToPlaceholderVersions.get(uri);
        sSessionsToPlaceholderVersions.put(uri, Integer.valueOf(currentVersion == null ? 0 : currentVersion.intValue() + 1));
    }

    public static Uri updateImage(Uri imageUri, ContentResolver resolver, String title, long date, Location location, int orientation, ExifInterface exif, byte[] jpeg, int width, int height, String mimeType) {
        String path = generateFilepath(title);
        writeFile(path, jpeg, exif);
        return updateImage(imageUri, resolver, title, date, location, orientation, jpeg.length, path, width, height, mimeType);
    }

    @TargetApi(16)
    private static void setImageSize(ContentValues values, int width, int height) {
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            values.put("width", Integer.valueOf(width));
            values.put("height", Integer.valueOf(height));
        }
    }

    private static long writeFile(String path, byte[] jpeg, ExifInterface exif) {
        if (exif != null) {
            try {
                exif.writeExif(jpeg, path);
                File f = new File(path);
                return f.length();
            } catch (Exception e) {
                Log.e(TAG, "Failed to write data", e);
                return -1L;
            }
        }
        return writeFile(path, jpeg);
    }

    private static long writeFile(String path, byte[] data) throws Throwable {
        long length;
        FileOutputStream out;
        FileOutputStream out2 = null;
        try {
            try {
                out = new FileOutputStream(path);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Exception e) {
            e = e;
        }
        try {
            out.write(data);
            length = data.length;
            try {
                out.close();
            } catch (Exception e2) {
                Log.e(TAG, "Failed to close file after write", e2);
            }
            out2 = out;
        } catch (Exception e3) {
            e = e3;
            out2 = out;
            Log.e(TAG, "Failed to write data", e);
            try {
                out2.close();
            } catch (Exception e4) {
                Log.e(TAG, "Failed to close file after write", e4);
            }
            length = -1;
        } catch (Throwable th2) {
            th = th2;
            out2 = out;
            try {
                out2.close();
            } catch (Exception e5) {
                Log.e(TAG, "Failed to close file after write", e5);
            }
            throw th;
        }
        return length;
    }

    private static Uri updateImage(Uri imageUri, ContentResolver resolver, String title, long date, Location location, int orientation, int jpegLength, String path, int width, int height, String mimeType) {
        ContentValues values = getContentValuesForData(title, date, location, orientation, jpegLength, path, width, height, mimeType);
        if (isSessionUri(imageUri)) {
            Uri resultUri = addImageToMediaStore(resolver, title, date, location, orientation, jpegLength, path, width, height, mimeType);
            sSessionsToContentUris.put(imageUri, resultUri);
            sContentUrisToSessions.put(resultUri, imageUri);
            return resultUri;
        }
        resolver.update(imageUri, values, null, null);
        return imageUri;
    }

    private static String generateFilepath(String title) {
        return DIRECTORY + '/' + title + JPEG_POSTFIX;
    }

    public static byte[] getJpegForSession(Uri uri) {
        return sSessionsToPlaceholderBytes.get(uri);
    }

    public static int getJpegVersionForSession(Uri uri) {
        return sSessionsToPlaceholderVersions.get(uri).intValue();
    }

    public static Point getSizeForSession(Uri uri) {
        return sSessionsToSizes.get(uri);
    }

    public static Uri getContentUriForSessionUri(Uri uri) {
        return sSessionsToContentUris.get(uri);
    }

    public static Uri getSessionUriFromContentUri(Uri contentUri) {
        return sContentUrisToSessions.get(contentUri);
    }

    public static boolean isSessionUri(Uri uri) {
        return uri.getScheme().equals(CAMERA_SESSION_SCHEME);
    }

    public static long getAvailableSpace() {
        String state = Environment.getExternalStorageState();
        Log.d(TAG, "External storage state=" + state);
        if ("checking".equals(state)) {
            return -2L;
        }
        if (!"mounted".equals(state)) {
            return -1L;
        }
        File dir = new File(DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return -1L;
        }
        try {
            StatFs stat = new StatFs(DIRECTORY);
            return ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize());
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
            return -3L;
        }
    }

    public static void ensureOSXCompatible() {
        File nnnAAAAA = new File(DCIM, "100ANDRO");
        if (!nnnAAAAA.exists() && !nnnAAAAA.mkdirs()) {
            Log.e(TAG, "Failed to create " + nnnAAAAA.getPath());
        }
    }
}
