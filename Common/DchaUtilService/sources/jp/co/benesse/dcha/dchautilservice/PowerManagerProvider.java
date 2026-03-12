package jp.co.benesse.dcha.dchautilservice;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import jp.co.benesse.dcha.util.Logger;
import jp.co.benesse.dcha.util.PowerManagerAdapter;

public class PowerManagerProvider extends ContentProvider {
    private static final int DISABLE_BATTERY_SAVER_ID = 1;
    private static final int UNSPECIFIED_ID = 0;
    private static final String TAG = PowerManagerProvider.class.getSimpleName();
    private static final String AUTHORITY = PowerManagerProvider.class.getName();
    private static UriMatcher sUriMatcher = new UriMatcher(-1);

    static {
        sUriMatcher.addURI(AUTHORITY, com.sts.tottori.extension.BuildConfig.FLAVOR, 0);
        sUriMatcher.addURI(AUTHORITY, "disable_battery_saver", 1);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri arg0) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Logger.d(TAG, "update 0001");
        int count = 0;
        try {
            int code = sUriMatcher.match(uri);
            switch (code) {
                case 1:
                    Logger.d(TAG, "update 0002");
                    PowerManagerAdapter.disableBatterySaver(getContext());
                    count = 0 + 1;
                    break;
                default:
                    Logger.d(TAG, "update 0003");
                    throw new IllegalArgumentException("Unknown URI " + uri);
            }
        } catch (Exception e) {
            Logger.e(TAG, "update 0004", e);
        }
        Logger.d(TAG, "update 0005 return:", Integer.valueOf(count));
        return count;
    }
}
