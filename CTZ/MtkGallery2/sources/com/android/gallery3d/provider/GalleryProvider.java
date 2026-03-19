package com.android.gallery3d.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;
import java.io.FileNotFoundException;

public class GalleryProvider extends ContentProvider {
    public static final Uri BASE_URI = Uri.parse("content://com.android.gallery3d.provider");
    private static final String[] SUPPORTED_PICASA_COLUMNS = {"user_account", "picasa_id", BookmarkEnhance.COLUMN_TITLE, "_size", BookmarkEnhance.COLUMN_MEDIA_TYPE, "datetaken", "latitude", "longitude", ExtFieldsUtils.VIDEO_ROTATION_FIELD};
    private DataManager mDataManager;

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            MediaItem mediaItem = (MediaItem) this.mDataManager.getMediaObject(Path.fromString(uri.getPath()));
            return mediaItem != null ? mediaItem.getMimeType() : null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onCreate() {
        this.mDataManager = ((GalleryApp) getContext().getApplicationContext()).getDataManager();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            MediaObject mediaObject = this.mDataManager.getMediaObject(Path.fromString(uri.getPath()));
            if (mediaObject != null) {
                if (PicasaSource.isPicasaImage(mediaObject)) {
                    return queryPicasaItem(mediaObject, strArr, str, strArr2, str2);
                }
                return null;
            }
            Log.w("Gallery2/GalleryProvider", "cannot find: " + uri);
            return null;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private Cursor queryPicasaItem(MediaObject mediaObject, String[] strArr, String str, String[] strArr2, String str2) {
        if (strArr == null) {
            strArr = SUPPORTED_PICASA_COLUMNS;
        }
        Object[] objArr = new Object[strArr.length];
        double latitude = PicasaSource.getLatitude(mediaObject);
        double longitude = PicasaSource.getLongitude(mediaObject);
        boolean zIsValidLocation = GalleryUtils.isValidLocation(latitude, longitude);
        int length = strArr.length;
        for (int i = 0; i < length; i++) {
            String str3 = strArr[i];
            if ("user_account".equals(str3)) {
                objArr[i] = PicasaSource.getUserAccount(getContext(), mediaObject);
            } else if ("picasa_id".equals(str3)) {
                objArr[i] = Long.valueOf(PicasaSource.getPicasaId(mediaObject));
            } else if (BookmarkEnhance.COLUMN_TITLE.equals(str3)) {
                objArr[i] = PicasaSource.getImageTitle(mediaObject);
            } else if ("_size".equals(str3)) {
                objArr[i] = Integer.valueOf(PicasaSource.getImageSize(mediaObject));
            } else if (BookmarkEnhance.COLUMN_MEDIA_TYPE.equals(str3)) {
                objArr[i] = PicasaSource.getContentType(mediaObject);
            } else if ("datetaken".equals(str3)) {
                objArr[i] = Long.valueOf(PicasaSource.getDateTaken(mediaObject));
            } else if ("latitude".equals(str3)) {
                objArr[i] = zIsValidLocation ? Double.valueOf(latitude) : null;
            } else if ("longitude".equals(str3)) {
                objArr[i] = zIsValidLocation ? Double.valueOf(longitude) : null;
            } else if (ExtFieldsUtils.VIDEO_ROTATION_FIELD.equals(str3)) {
                objArr[i] = Integer.valueOf(PicasaSource.getRotation(mediaObject));
            } else {
                Log.w("Gallery2/GalleryProvider", "unsupported column: " + str3);
            }
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        matrixCursor.addRow(objArr);
        return matrixCursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (str.contains("w")) {
                throw new FileNotFoundException("cannot open file for write");
            }
            MediaObject mediaObject = this.mDataManager.getMediaObject(Path.fromString(uri.getPath()));
            if (mediaObject == null) {
                throw new FileNotFoundException(uri.toString());
            }
            if (PicasaSource.isPicasaImage(mediaObject)) {
                return PicasaSource.openFile(getContext(), mediaObject, str);
            }
            throw new FileNotFoundException("unspported type: " + mediaObject);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException();
    }
}
