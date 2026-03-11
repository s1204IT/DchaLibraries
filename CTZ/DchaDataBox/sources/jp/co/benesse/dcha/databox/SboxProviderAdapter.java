package jp.co.benesse.dcha.databox;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import jp.co.benesse.dcha.util.FileUtils;
import jp.co.benesse.dcha.util.Logger;

public class SboxProviderAdapter {
    public static final String SBOX_DB_COLUMN_NAME_KEY = "key";
    public static final String SBOX_DB_COLUMN_NAME_VALUE = "value";
    private static final String TAG = SboxProviderAdapter.class.getSimpleName();
    public static final Uri SBOX_URI = Uri.parse("content://jp.co.benesse.touch.sbox/jp.co.benesse.dcha.databox");
    public static final Uri SBOX_WIPE_URI = Uri.parse("content://jp.co.benesse.touch.sbox/cmd/wipe");

    public String getValue(ContentResolver contentResolver, String str) throws Throwable {
        Cursor cursorQuery;
        Logger.d(TAG, "getValue key:", str);
        String string = null;
        try {
            try {
                cursorQuery = contentResolver.query(SBOX_URI, null, "key = ?", new String[]{str}, null);
                try {
                    boolean zMoveToFirst = cursorQuery.moveToFirst();
                    contentResolver = cursorQuery;
                    if (zMoveToFirst) {
                        string = cursorQuery.getString(cursorQuery.getColumnIndex("value"));
                        contentResolver = cursorQuery;
                    }
                } catch (Exception e) {
                    e = e;
                    Logger.e(TAG, "getValue Exception", e);
                    contentResolver = cursorQuery;
                }
            } catch (Throwable th) {
                th = th;
                FileUtils.close(contentResolver);
                throw th;
            }
        } catch (Exception e2) {
            e = e2;
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            contentResolver = 0;
            FileUtils.close(contentResolver);
            throw th;
        }
        FileUtils.close(contentResolver);
        Logger.d(TAG, "getValue value:", string);
        return string;
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
