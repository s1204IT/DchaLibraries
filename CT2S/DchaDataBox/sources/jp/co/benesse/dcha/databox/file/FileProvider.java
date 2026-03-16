package jp.co.benesse.dcha.databox.file;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public final class FileProvider extends ContentProvider {
    static final String AUTHORITY = FileProvider.class.getName();
    private static final UriMatcher uriMatcher = new UriMatcher(-1);

    static {
        uriMatcher.addURI(AUTHORITY, ContractFile.TOP_DIR.pathName, ContractFile.TOP_DIR.codeForMany);
    }

    @Override
    public final boolean onCreate() {
        return true;
    }

    @Override
    public final String getType(Uri uri) {
        int code = uriMatcher.match(uri);
        if (code == ContractFile.TOP_DIR.codeForMany) {
            return getContext().getFilesDir().getAbsolutePath();
        }
        throw new IllegalArgumentException("unknown uri : " + uri);
    }

    @Override
    public final Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public final int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public final int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
}
