package android.support.v4.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

class DocumentsContractApi19 {
    public static String getName(Context context, Uri self) {
        return queryForString(context, self, "_display_name", null);
    }

    public static long length(Context context, Uri self) {
        return queryForLong(context, self, "_size", 0L);
    }

    private static String queryForString(Context context, Uri self, String column, String defaultValue) {
        ContentResolver resolver = context.getContentResolver();
        Cursor c = null;
        try {
            try {
                c = resolver.query(self, new String[]{column}, null, null, null);
                if (c.moveToFirst() && !c.isNull(0)) {
                    return c.getString(0);
                }
            } catch (Exception e) {
                Log.w("DocumentFile", "Failed query: " + e);
            }
            return defaultValue;
        } finally {
            closeQuietly(c);
        }
    }

    private static long queryForLong(Context context, Uri self, String column, long defaultValue) {
        ContentResolver resolver = context.getContentResolver();
        Cursor c = null;
        try {
            try {
                c = resolver.query(self, new String[]{column}, null, null, null);
                if (c.moveToFirst() && !c.isNull(0)) {
                    return c.getLong(0);
                }
            } catch (Exception e) {
                Log.w("DocumentFile", "Failed query: " + e);
            }
            return defaultValue;
        } finally {
            closeQuietly(c);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }
}
