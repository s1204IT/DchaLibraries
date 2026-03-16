package com.android.camera.session;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import com.android.camera.Storage;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.util.CameraUtil;

public class PlaceholderManager {
    private static final Log.Tag TAG = new Log.Tag("PlaceholderMgr");
    private final Context mContext;

    public static class Session {
        final String outputTitle;
        final Uri outputUri;
        final long time;

        Session(String title, Uri uri, long timestamp) {
            this.outputTitle = title;
            this.outputUri = uri;
            this.time = timestamp;
        }
    }

    public PlaceholderManager(Context context) {
        this.mContext = context;
    }

    public Session insertPlaceholder(String title, byte[] placeholder, long timestamp) {
        if (title == null || placeholder == null) {
            throw new IllegalArgumentException("Null argument passed to insertPlaceholder");
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(placeholder, 0, placeholder.length, options);
        int width = options.outWidth;
        int height = options.outHeight;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image had bad height/width");
        }
        Uri uri = Storage.addPlaceholder(placeholder, width, height);
        if (uri == null) {
            return null;
        }
        return new Session(title, uri, timestamp);
    }

    public Session convertToPlaceholder(Uri uri) {
        return createSessionFromUri(uri);
    }

    public Uri finishPlaceholder(Session session, Location location, int orientation, ExifInterface exif, byte[] jpeg, int width, int height, String mimeType) {
        Uri resultUri = Storage.updateImage(session.outputUri, this.mContext.getContentResolver(), session.outputTitle, session.time, location, orientation, exif, jpeg, width, height, mimeType);
        CameraUtil.broadcastNewPicture(this.mContext, resultUri);
        return resultUri;
    }

    public void replacePlaceholder(Session session, byte[] jpeg, int width, int height) {
        Storage.replacePlaceholder(session.outputUri, jpeg, width, height);
        CameraUtil.broadcastNewPicture(this.mContext, session.outputUri);
    }

    private Session createSessionFromUri(Uri uri) {
        ContentResolver resolver = this.mContext.getContentResolver();
        Cursor cursor = resolver.query(uri, new String[]{"datetaken", "_display_name"}, null, null, null);
        if (cursor == null || cursor.getCount() == 0) {
            return null;
        }
        int dateIndex = cursor.getColumnIndexOrThrow("datetaken");
        int nameIndex = cursor.getColumnIndexOrThrow("_display_name");
        cursor.moveToFirst();
        long date = cursor.getLong(dateIndex);
        String name = cursor.getString(nameIndex);
        if (name.toLowerCase().endsWith(Storage.JPEG_POSTFIX)) {
            name = name.substring(0, name.length() - Storage.JPEG_POSTFIX.length());
        }
        return new Session(name, uri, date);
    }
}
