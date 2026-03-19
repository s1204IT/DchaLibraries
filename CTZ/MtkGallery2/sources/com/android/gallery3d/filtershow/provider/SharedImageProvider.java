package com.android.gallery3d.filtershow.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ConditionVariable;
import android.os.ParcelFileDescriptor;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import java.io.File;
import java.io.FileNotFoundException;

public class SharedImageProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://com.android.gallery3d.filtershow.provider.SharedImageProvider/image");
    private static ConditionVariable sImageReadyCond = new ConditionVariable(true);
    private final String[] mMimeStreamType = {"image/jpeg"};

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public String[] getStreamTypes(Uri uri, String str) {
        return this.mMimeStreamType;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if (contentValues.containsKey("prepare")) {
            if (contentValues.getAsBoolean("prepare").booleanValue()) {
                sImageReadyCond.close();
                return null;
            }
            sImageReadyCond.open();
            return null;
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            return null;
        }
        if (strArr == null) {
            strArr = new String[]{BookmarkEnhance.COLUMN_ID, BookmarkEnhance.COLUMN_DATA, BookmarkEnhance.COLUMN_TITLE, "_size"};
        }
        sImageReadyCond.block();
        File file = new File(lastPathSegment);
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        Object[] objArr = new Object[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i].equalsIgnoreCase(BookmarkEnhance.COLUMN_ID)) {
                objArr[i] = 0;
            } else if (strArr[i].equalsIgnoreCase(BookmarkEnhance.COLUMN_DATA)) {
                objArr[i] = uri;
            } else if (strArr[i].equalsIgnoreCase(BookmarkEnhance.COLUMN_TITLE)) {
                objArr[i] = file.getName();
            } else if (strArr[i].equalsIgnoreCase("_size")) {
                objArr[i] = Long.valueOf(file.length());
            }
        }
        matrixCursor.addRow(objArr);
        return matrixCursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            return null;
        }
        sImageReadyCond.block();
        return ParcelFileDescriptor.open(new File(lastPathSegment), 268435456);
    }
}
