package com.android.gallery3d.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;
import java.io.FileNotFoundException;

public class GalleryProvider extends ContentProvider {
    public static final Uri BASE_URI = Uri.parse("content://com.android.gallery3d.provider");
    private static final String[] SUPPORTED_PICASA_COLUMNS = {"user_account", "picasa_id", "_display_name", "_size", "mime_type", "datetaken", "latitude", "longitude", "orientation"};
    private DataManager mDataManager;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        long token = Binder.clearCallingIdentity();
        try {
            Path path = Path.fromString(uri.getPath());
            MediaItem item = (MediaItem) this.mDataManager.getMediaObject(path);
            return item != null ? item.getMimeType() : null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onCreate() {
        GalleryApp app = (GalleryApp) getContext().getApplicationContext();
        this.mDataManager = app.getDataManager();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursorQueryPicasaItem = null;
        long token = Binder.clearCallingIdentity();
        try {
            Path path = Path.fromString(uri.getPath());
            MediaObject object = this.mDataManager.getMediaObject(path);
            if (object == null) {
                Log.w("GalleryProvider", "cannot find: " + uri);
            } else if (PicasaSource.isPicasaImage(object)) {
                cursorQueryPicasaItem = queryPicasaItem(object, projection, selection, selectionArgs, sortOrder);
            }
            return cursorQueryPicasaItem;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private Cursor queryPicasaItem(MediaObject image, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (projection == null) {
            projection = SUPPORTED_PICASA_COLUMNS;
        }
        Object[] columnValues = new Object[projection.length];
        double latitude = PicasaSource.getLatitude(image);
        double longitude = PicasaSource.getLongitude(image);
        boolean isValidLatlong = GalleryUtils.isValidLocation(latitude, longitude);
        int n = projection.length;
        for (int i = 0; i < n; i++) {
            String column = projection[i];
            if ("user_account".equals(column)) {
                columnValues[i] = PicasaSource.getUserAccount(getContext(), image);
            } else if ("picasa_id".equals(column)) {
                columnValues[i] = Long.valueOf(PicasaSource.getPicasaId(image));
            } else if ("_display_name".equals(column)) {
                columnValues[i] = PicasaSource.getImageTitle(image);
            } else if ("_size".equals(column)) {
                columnValues[i] = Integer.valueOf(PicasaSource.getImageSize(image));
            } else if ("mime_type".equals(column)) {
                columnValues[i] = PicasaSource.getContentType(image);
            } else if ("datetaken".equals(column)) {
                columnValues[i] = Long.valueOf(PicasaSource.getDateTaken(image));
            } else if ("latitude".equals(column)) {
                columnValues[i] = isValidLatlong ? Double.valueOf(latitude) : null;
            } else if ("longitude".equals(column)) {
                columnValues[i] = isValidLatlong ? Double.valueOf(longitude) : null;
            } else if ("orientation".equals(column)) {
                columnValues[i] = Integer.valueOf(PicasaSource.getRotation(image));
            } else {
                Log.w("GalleryProvider", "unsupported column: " + column);
            }
        }
        MatrixCursor cursor = new MatrixCursor(projection);
        cursor.addRow(columnValues);
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        long token = Binder.clearCallingIdentity();
        try {
            if (mode.contains("w")) {
                throw new FileNotFoundException("cannot open file for write");
            }
            Path path = Path.fromString(uri.getPath());
            MediaObject object = this.mDataManager.getMediaObject(path);
            if (object == null) {
                throw new FileNotFoundException(uri.toString());
            }
            if (PicasaSource.isPicasaImage(object)) {
                return PicasaSource.openFile(getContext(), object, mode);
            }
            throw new FileNotFoundException("unspported type: " + object);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
