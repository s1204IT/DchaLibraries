package jp.co.benesse.dcha.databox.file;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import java.io.IOException;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public final class FileProvider extends ContentProvider {
    private static final String TAG = FileProvider.class.getSimpleName();
    public static final String AUTHORITY = FileProvider.class.getName();
    private static final UriMatcher uriMatcher = new UriMatcher(-1);

    @Override // android.content.ContentProvider
    public final int delete(Uri uri, String str, String[] strArr) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public final Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override // android.content.ContentProvider
    public final boolean onCreate() {
        return true;
    }

    @Override // android.content.ContentProvider
    public final Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return null;
    }

    @Override // android.content.ContentProvider
    public final int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }

    static {
        uriMatcher.addURI(AUTHORITY, ContractFile.TOP_DIR.pathName, ContractFile.TOP_DIR.codeForMany);
    }

    @Override // android.content.ContentProvider
    public final String getType(Uri uri) {
        if (uriMatcher.match(uri) == ContractFile.TOP_DIR.codeForMany) {
            String absolutePath = getContext().getFilesDir().getAbsolutePath();
            try {
                return getContext().getFilesDir().getCanonicalPath();
            } catch (IOException e) {
                Logger.e(TAG, e);
                return absolutePath;
            }
        }
        throw new IllegalArgumentException("unknown uri : " + uri);
    }
}
