package com.android.gallery3d.filtershow.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ConditionVariable;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

public class SharedImageProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://com.android.gallery3d.filtershow.provider.SharedImageProvider/image");
    private static ConditionVariable mImageReadyCond = new ConditionVariable(false);
    private final String[] mMimeStreamType = {"image/jpeg"};

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        return "image/jpeg";
    }

    @Override
    public String[] getStreamTypes(Uri arg0, String mimeTypeFilter) {
        return this.mMimeStreamType;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (values.containsKey("prepare")) {
            if (values.getAsBoolean("prepare").booleanValue()) {
                mImageReadyCond.close();
                return null;
            }
            mImageReadyCond.open();
            return null;
        }
        return null;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String uriPath = uri.getLastPathSegment();
        if (uriPath == null) {
            return null;
        }
        if (projection == null) {
            projection = new String[]{"_id", "_data", "_display_name", "_size"};
        }
        mImageReadyCond.block();
        File path = new File(uriPath);
        MatrixCursor cursor = new MatrixCursor(projection);
        Object[] columns = new Object[projection.length];
        for (int i = 0; i < projection.length; i++) {
            if (projection[i].equalsIgnoreCase("_id")) {
                columns[i] = 0;
            } else if (projection[i].equalsIgnoreCase("_data")) {
                columns[i] = uri;
            } else if (projection[i].equalsIgnoreCase("_display_name")) {
                columns[i] = path.getName();
            } else if (projection[i].equalsIgnoreCase("_size")) {
                columns[i] = Long.valueOf(path.length());
            }
        }
        cursor.addRow(columns);
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String uriPath = uri.getLastPathSegment();
        if (uriPath == null) {
            return null;
        }
        mImageReadyCond.block();
        File path = new File(uriPath);
        int imode = 0 | 268435456;
        return ParcelFileDescriptor.open(path, imode);
    }
}
