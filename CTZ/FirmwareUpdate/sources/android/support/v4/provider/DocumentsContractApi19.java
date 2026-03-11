package android.support.v4.provider;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

class DocumentsContractApi19 {
    private static void closeQuietly(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e2) {
            }
        }
    }

    public static String getName(Context context, Uri uri) {
        return queryForString(context, uri, "_display_name", null);
    }

    public static long length(Context context, Uri uri) {
        return queryForLong(context, uri, "_size", 0L);
    }

    private static long queryForLong(Context context, Uri uri, String str, long j) throws Throwable {
        Throwable th;
        Cursor cursor;
        Cursor cursorQuery;
        try {
            cursorQuery = context.getContentResolver().query(uri, new String[]{str}, null, null, null);
            try {
                try {
                } catch (Exception e) {
                    e = e;
                    Log.w("DocumentFile", "Failed query: " + e);
                }
            } catch (Throwable th2) {
                th = th2;
                cursor = cursorQuery;
                closeQuietly(cursor);
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th3) {
            th = th3;
            cursor = null;
            closeQuietly(cursor);
            throw th;
        }
        if (!cursorQuery.moveToFirst() || cursorQuery.isNull(0)) {
            closeQuietly(cursorQuery);
        } else {
            j = cursorQuery.getLong(0);
            closeQuietly(cursorQuery);
        }
        return j;
    }

    private static String queryForString(Context context, Uri uri, String str, String str2) throws Throwable {
        Throwable th;
        Cursor cursor;
        Cursor cursorQuery;
        try {
            cursorQuery = context.getContentResolver().query(uri, new String[]{str}, null, null, null);
            try {
                try {
                } catch (Exception e) {
                    e = e;
                    Log.w("DocumentFile", "Failed query: " + e);
                }
            } catch (Throwable th2) {
                th = th2;
                cursor = cursorQuery;
                closeQuietly(cursor);
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th3) {
            th = th3;
            cursor = null;
            closeQuietly(cursor);
            throw th;
        }
        if (!cursorQuery.moveToFirst() || cursorQuery.isNull(0)) {
            closeQuietly(cursorQuery);
        } else {
            str2 = cursorQuery.getString(0);
            closeQuietly(cursorQuery);
        }
        return str2;
    }
}
