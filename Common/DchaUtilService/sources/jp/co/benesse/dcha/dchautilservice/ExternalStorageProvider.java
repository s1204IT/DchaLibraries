package jp.co.benesse.dcha.dchautilservice;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import jp.co.benesse.dcha.util.Logger;
import jp.co.benesse.dcha.util.StorageManagerAdapter;

public class ExternalStorageProvider extends ContentProvider {
    private static final int EXISTS_ID = 4;
    private static final int GET_INFO_ID = 1;
    private static final int GET_SIZE_ID = 2;
    private static final int LIST_FILES_ID = 3;
    private static final int READ_TEXT_ID = 5;
    private static final int UNSPECIFIED_ID = 0;
    private static final String TAG = ExternalStorageProvider.class.getSimpleName();
    private static final String AUTHORITY = ExternalStorageProvider.class.getName();
    private static UriMatcher sUriMatcher = new UriMatcher(-1);

    static {
        sUriMatcher.addURI(AUTHORITY, com.sts.tottori.extension.BuildConfig.FLAVOR, 0);
        sUriMatcher.addURI(AUTHORITY, "get_info", 1);
        sUriMatcher.addURI(AUTHORITY, "get_size", 2);
        sUriMatcher.addURI(AUTHORITY, "list_files", LIST_FILES_ID);
        sUriMatcher.addURI(AUTHORITY, "exists", EXISTS_ID);
        sUriMatcher.addURI(AUTHORITY, "read_text", READ_TEXT_ID);
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
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) throws Throwable {
        Cursor cursor;
        String str;
        Logger.d(TAG, "query 0001");
        Cursor cursor2 = null;
        try {
            int code = sUriMatcher.match(uri);
            try {
                switch (code) {
                    case 1:
                        Logger.d(TAG, "query 0002");
                        String path = getPath(getContext());
                        String internalPath = getInternalPath(getContext());
                        String volumeState = getState(getContext());
                        Cursor cursor3 = new MatrixCursor(new String[]{"path", "internalPath", "state"});
                        ((MatrixCursor) cursor3).addRow(new String[]{path, internalPath, volumeState});
                        cursor2 = cursor3;
                        break;
                    case 2:
                        Logger.d(TAG, "query 0003");
                        String internalPath2 = getInternalPath(getContext());
                        StatFs statfs = new StatFs(internalPath2);
                        long total = statfs.getBlockSizeLong() * statfs.getBlockCountLong();
                        long free = statfs.getBlockSizeLong() * statfs.getAvailableBlocksLong();
                        Cursor cursor4 = new MatrixCursor(new String[]{"total", "free"});
                        ((MatrixCursor) cursor4).addRow(new Long[]{Long.valueOf(total), Long.valueOf(free)});
                        cursor2 = cursor4;
                        break;
                    case LIST_FILES_ID:
                        Logger.d(TAG, "query 0004");
                        String path2 = getPath(getContext());
                        File file = new File(path2);
                        if (!TextUtils.isEmpty(selection)) {
                            file = new File(file, selection);
                        }
                        File[] listFiles = file.listFiles();
                        if (listFiles != null) {
                            Cursor cursor5 = new MatrixCursor(new String[]{"name", "path", "type"});
                            for (File f : listFiles) {
                                MatrixCursor matrixCursor = (MatrixCursor) cursor5;
                                String[] strArr = new String[LIST_FILES_ID];
                                strArr[0] = f.getName();
                                strArr[1] = f.getPath();
                                if (f.isDirectory()) {
                                    str = "d";
                                } else {
                                    str = f.isFile() ? "f" : com.sts.tottori.extension.BuildConfig.FLAVOR;
                                }
                                strArr[2] = str;
                                matrixCursor.addRow(strArr);
                            }
                            cursor2 = cursor5;
                        }
                        break;
                    case EXISTS_ID:
                        Logger.d(TAG, "query 0005");
                        String path3 = getPath(getContext());
                        File file2 = new File(path3);
                        if (!TextUtils.isEmpty(selection)) {
                            file2 = new File(file2, selection);
                        }
                        Cursor cursor6 = new MatrixCursor(new String[]{"exists"});
                        MatrixCursor matrixCursor2 = (MatrixCursor) cursor6;
                        Integer[] numArr = new Integer[1];
                        numArr[0] = Integer.valueOf(file2.exists() ? 1 : 0);
                        matrixCursor2.addRow(numArr);
                        cursor2 = cursor6;
                        break;
                    case READ_TEXT_ID:
                        Logger.d(TAG, "query 0006");
                        String path4 = getPath(getContext());
                        File file3 = new File(path4);
                        if (!TextUtils.isEmpty(selection)) {
                            file3 = new File(file3, selection);
                        }
                        String textData = readText(file3);
                        Cursor cursor7 = new MatrixCursor(new String[]{"text"});
                        ((MatrixCursor) cursor7).addRow(new String[]{textData});
                        cursor2 = cursor7;
                        break;
                    default:
                        Logger.d(TAG, "query 0007");
                        throw new IllegalArgumentException("Unknown URI " + uri);
                }
            } catch (Exception e) {
                e = e;
                cursor2 = cursor;
                Logger.e(TAG, "query 0008", e);
            }
        } catch (Exception e2) {
            e = e2;
        }
        Logger.d(TAG, "query 0009");
        return cursor2;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    protected String getPath(Context context) {
        Logger.d(TAG, "getPath 0001");
        String storagePath = System.getenv("SECONDARY_STORAGE");
        if (Build.VERSION.SDK_INT >= 28) {
            Logger.d(TAG, "getPath 0002");
            storagePath = StorageManagerAdapter.getPath(context);
        } else {
            Logger.d(TAG, "getPath 0003");
            try {
                storagePath = new File(storagePath).getCanonicalPath();
            } catch (Exception e) {
                Logger.d(TAG, "getPath 0004", e);
            }
        }
        Logger.d(TAG, "getPath 0005 return:", storagePath);
        return storagePath;
    }

    protected String getInternalPath(Context context) {
        Logger.d(TAG, "getInternalPath 0001");
        String storagePath = System.getenv("SECONDARY_STORAGE");
        if (Build.VERSION.SDK_INT >= 28) {
            Logger.d(TAG, "getInternalPath 0002");
            storagePath = StorageManagerAdapter.getInternalPath(context);
        } else {
            Logger.d(TAG, "getInternalPath 0003");
            try {
                storagePath = new File(storagePath).getCanonicalPath();
            } catch (Exception e) {
                Logger.d(TAG, "getInternalPath 0004", e);
            }
        }
        Logger.d(TAG, "getInternalPath 0005 return:", storagePath);
        return storagePath;
    }

    protected String getState(Context context) {
        String volumeState;
        Logger.d(TAG, "getState 0001");
        if (Build.VERSION.SDK_INT >= 28) {
            Logger.d(TAG, "getState 0002");
            volumeState = StorageManagerAdapter.getState(context);
        } else {
            Logger.d(TAG, "getState 0003");
            String secondaryStoragePath = System.getenv("SECONDARY_STORAGE");
            volumeState = Environment.getExternalStorageState(new File(secondaryStoragePath));
        }
        Logger.d(TAG, "getState 0004 volumeState:", volumeState);
        return volumeState;
    }

    protected String readText(File file) throws Throwable {
        Logger.d(TAG, "readLine 0001");
        StringBuffer result = new StringBuffer();
        BufferedReader reader = null;
        try {
            try {
                BufferedReader reader2 = new BufferedReader(new FileReader(file));
                while (reader2.ready()) {
                    try {
                        result.append(reader2.readLine());
                        if (reader2.ready()) {
                            result.append("\n");
                        }
                    } catch (Exception e) {
                        e = e;
                        reader = reader2;
                        Logger.e(TAG, "readLine 0002", e);
                        if (reader != null) {
                            Logger.d(TAG, "readLine 0003");
                            try {
                                reader.close();
                                reader = null;
                            } catch (IOException e2) {
                                Logger.e(TAG, "readLine 0004", e2);
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        reader = reader2;
                        if (reader != null) {
                            Logger.d(TAG, "readLine 0003");
                            try {
                                reader.close();
                            } catch (IOException e3) {
                                Logger.e(TAG, "readLine 0004", e3);
                            }
                        }
                        throw th;
                    }
                }
                if (reader2 != null) {
                    Logger.d(TAG, "readLine 0003");
                    try {
                        reader2.close();
                        reader = null;
                    } catch (IOException e4) {
                        Logger.e(TAG, "readLine 0004", e4);
                        reader = reader2;
                    }
                } else {
                    reader = reader2;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Exception e5) {
            e = e5;
        }
        Logger.d(TAG, "readLine 0005 result:", result.toString());
        return result.toString();
    }
}
