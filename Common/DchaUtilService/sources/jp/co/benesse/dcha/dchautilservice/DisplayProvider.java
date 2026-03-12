package jp.co.benesse.dcha.dchautilservice;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Map;
import jp.co.benesse.dcha.util.Logger;
import jp.co.benesse.dcha.util.WindowManagerAdapter;

public class DisplayProvider extends ContentProvider {
    private static final String COLUMN_HEIGHT = "height";
    private static final String COLUMN_WIDTH = "width";
    private static final int DISPLAY_SIZE_ID = 1;
    private static final int LCD_SIZE_ID = 2;
    private static final String PACKAGE_SHO_HOME = "jp.co.benesse.touch.allgrade.b003.touchhomelauncher";
    private static final int UNSPECIFIED_ID = 0;
    private static final String TAG = DisplayProvider.class.getSimpleName();
    private static final String AUTHORITY = DisplayProvider.class.getName();
    private static UriMatcher sUriMatcher = new UriMatcher(-1);

    static {
        sUriMatcher.addURI(AUTHORITY, com.sts.tottori.extension.BuildConfig.FLAVOR, 0);
        sUriMatcher.addURI(AUTHORITY, "display_size", 1);
        sUriMatcher.addURI(AUTHORITY, "lcd_size", 2);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
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
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        int code;
        Cursor cursor;
        Logger.d(TAG, "query 0001");
        Cursor cursor2 = null;
        try {
            code = sUriMatcher.match(uri);
        } catch (Exception e) {
            e = e;
        }
        try {
            switch (code) {
                case 1:
                    Logger.d(TAG, "query 0002");
                    int[] size = WindowManagerAdapter.getDisplaySize(getContext());
                    cursor = new MatrixCursor(new String[]{COLUMN_WIDTH, COLUMN_HEIGHT});
                    ((MatrixCursor) cursor).addRow(new Integer[]{Integer.valueOf(size[0]), Integer.valueOf(size[1])});
                    cursor2 = cursor;
                    break;
                case 2:
                    Logger.d(TAG, "query 0003");
                    int[] size2 = {0, 0};
                    if (existsPackage(getContext(), PACKAGE_SHO_HOME)) {
                        size2[0] = 1280;
                        size2[1] = 800;
                    } else {
                        size2 = WindowManagerAdapter.getLcdSize(getContext());
                    }
                    cursor = new MatrixCursor(new String[]{COLUMN_WIDTH, COLUMN_HEIGHT});
                    ((MatrixCursor) cursor).addRow(new Integer[]{Integer.valueOf(size2[0]), Integer.valueOf(size2[1])});
                    cursor2 = cursor;
                    break;
                default:
                    Logger.d(TAG, "query 0004");
                    throw new IllegalArgumentException("Unknown URI " + uri);
            }
        } catch (Exception e2) {
            e = e2;
            cursor2 = cursor;
            Logger.e(TAG, "query 0005", e);
        }
        Logger.d(TAG, "query 0006");
        return cursor2;
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
                    Map<String, Integer> size = new HashMap<>();
                    size.put(COLUMN_WIDTH, 0);
                    size.put(COLUMN_HEIGHT, 0);
                    for (Map.Entry<String, Object> entry : values.valueSet()) {
                        String key = entry.getKey();
                        if (TextUtils.equals(COLUMN_WIDTH, key) || TextUtils.equals(COLUMN_HEIGHT, key)) {
                            Object value = entry.getValue();
                            if (value instanceof Integer) {
                                size.remove(key);
                                size.put(key, (Integer) value);
                            } else if (value instanceof Long) {
                                size.remove(key);
                                size.put(key, Integer.valueOf(((Long) value).intValue()));
                            }
                        }
                    }
                    boolean result = WindowManagerAdapter.setForcedDisplaySize(getContext(), size.get(COLUMN_WIDTH).intValue(), size.get(COLUMN_HEIGHT).intValue());
                    if (result) {
                        count = 0 + 1;
                    }
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

    protected boolean existsPackage(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            for (PackageInfo pkgInfo : pm.getInstalledPackages(8704)) {
                if (TextUtils.equals(packageName, pkgInfo.packageName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Logger.d(TAG, "existsPackage", e);
            return false;
        }
    }
}
