package jp.co.benesse.dcha.databox;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import jp.co.benesse.dcha.util.FileUtils;
import jp.co.benesse.dcha.util.Logger;

/* loaded from: classes.dex */
public class SboxProviderAdapter {
    public static final String SBOX_DB_COLUMN_NAME_KEY = "key";
    public static final String SBOX_DB_COLUMN_NAME_VALUE = "value";
    private static final String TAG = SboxProviderAdapter.class.getSimpleName();
    public static final Uri SBOX_URI = Uri.parse("content://jp.co.benesse.touch.sbox/jp.co.benesse.dcha.databox");
    public static final Uri SBOX_WIPE_URI = Uri.parse("content://jp.co.benesse.touch.sbox/cmd/wipe");

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:19:0x0058 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:22:0x0011 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:9:0x0035 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r12v0, types: [android.content.ContentResolver] */
    /* JADX WARN: Type inference failed for: r12v1 */
    /* JADX WARN: Type inference failed for: r12v10 */
    /* JADX WARN: Type inference failed for: r12v11 */
    /* JADX WARN: Type inference failed for: r12v3, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r12v4 */
    /* JADX WARN: Type inference failed for: r12v6, types: [java.io.Closeable] */
    /* JADX WARN: Type inference failed for: r12v9 */
    public String getValue(ContentResolver contentResolver, String str) throws Throwable {
        Cursor cursorQuery;
        Logger.d(TAG, "getValue key:", str);
        String string = null;
        try {
            try {
                cursorQuery = contentResolver.query(SBOX_URI, null, "key = ?", new String[]{str}, null);
            } catch (Exception e) {
                e = e;
                cursorQuery = null;
            } catch (Throwable th) {
                th = th;
                contentResolver = 0;
                FileUtils.close(contentResolver);
                throw th;
            }
            try {
                boolean zMoveToFirst = cursorQuery.moveToFirst();
                contentResolver = cursorQuery;
                if (zMoveToFirst) {
                    string = cursorQuery.getString(cursorQuery.getColumnIndex("value"));
                    contentResolver = cursorQuery;
                }
            } catch (Exception e2) {
                e = e2;
                Logger.e(TAG, "getValue Exception", e);
                contentResolver = cursorQuery;
                FileUtils.close(contentResolver);
                Logger.d(TAG, "getValue value:", string);
                return string;
            }
            FileUtils.close(contentResolver);
            Logger.d(TAG, "getValue value:", string);
            return string;
        } catch (Throwable th2) {
            th = th2;
            FileUtils.close(contentResolver);
            throw th;
        }
    }

    public void setValue(ContentResolver contentResolver, String str, String str2) {
        Logger.d(TAG, "setValue key:", str, "value:", str2);
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("key", str);
            contentValues.put("value", str2);
            contentResolver.delete(SBOX_URI, "key = ?", new String[]{str});
            contentResolver.insert(SBOX_URI, contentValues);
        } catch (Exception e) {
            Logger.e(TAG, "setValue Exception", e);
        }
    }

    public void wipe(ContentResolver contentResolver) {
        Logger.d(TAG, "wipe");
        try {
            contentResolver.delete(SBOX_WIPE_URI, null, null);
        } catch (Exception e) {
            Logger.e(TAG, "wipe Exception", e);
        }
    }
}
