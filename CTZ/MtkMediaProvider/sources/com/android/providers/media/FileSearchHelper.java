package com.android.providers.media;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.media.MediaFile;
import android.net.Uri;
import android.os.Binder;
import android.text.TextUtils;
import java.util.Arrays;
import java.util.Locale;

public class FileSearchHelper {
    private static String[] sSearchFileCols = {"_id", "(CASE WHEN media_type=1 THEN 2130837508 ELSE CASE WHEN media_type=2 THEN 2130837506 ELSE CASE WHEN media_type=3 THEN 2130837514 ELSE CASE WHEN file_type=4 THEN 2130837513 ELSE CASE WHEN file_type=5 THEN 2130837515 ELSE CASE WHEN file_type=6 THEN 2130837505 ELSE CASE WHEN format=12289 THEN 2130837507 ELSE 2130837512 END END END END END END END) AS suggest_icon_1", "file_name AS suggest_text_1", "_data AS suggest_text_2", "_data AS suggest_intent_data", "_id AS suggest_shortcut_id"};

    public static Cursor doFileSearch(SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String str) {
        if (sQLiteDatabase == null || sQLiteQueryBuilder == null || uri == null) {
            MtkLog.e("FileSearchHelper", "doFileSearch: Param error!");
            return null;
        }
        String strTrim = Uri.decode(uri.getPath().endsWith("/") ? "" : uri.getLastPathSegment()).trim();
        if (TextUtils.isEmpty(strTrim)) {
            return null;
        }
        String[] strArr = {"%" + strTrim.replace("\\", "\\\\").replace("%", "\\%").replace("'", "\\'") + "%"};
        sQLiteQueryBuilder.setTables("files");
        if (MediaUtils.LOG_QUERY) {
            MtkLog.d("FileSearchHelper", "doFileSearch: uri = " + uri + ", selection = file_name LIKE ? ESCAPE '\\', selectionArgs = " + Arrays.toString(strArr) + ", caller pid = " + Binder.getCallingPid());
        }
        return sQLiteQueryBuilder.query(sQLiteDatabase, sSearchFileCols, "file_name LIKE ? ESCAPE '\\'", strArr, null, null, null, str);
    }

    public static void computeFileName(String str, ContentValues contentValues) {
        if (str == null || contentValues == null) {
            MtkLog.e("FileSearchHelper", "computeFileName: Param error!");
            return;
        }
        int iLastIndexOf = str.lastIndexOf(47);
        if (iLastIndexOf >= 0) {
            str = str.substring(iLastIndexOf + 1);
        }
        contentValues.put("file_name", str);
    }

    public static void computeFileType(String str, ContentValues contentValues) {
        String mimeTypeForFile;
        if (str == null || contentValues == null) {
            MtkLog.e("FileSearchHelper", "computeFileType: Param error!");
            return;
        }
        Integer asInteger = contentValues.getAsInteger("format");
        if ((asInteger != null && asInteger.intValue() == 12289) || (mimeTypeForFile = MediaFile.getMimeTypeForFile(str)) == null) {
            return;
        }
        String lowerCase = mimeTypeForFile.toLowerCase();
        if (lowerCase.startsWith("image/")) {
            contentValues.put("file_type", (Integer) 1);
            return;
        }
        if (lowerCase.startsWith("audio/")) {
            contentValues.put("file_type", (Integer) 2);
            return;
        }
        if (lowerCase.startsWith("video/")) {
            contentValues.put("file_type", (Integer) 3);
            return;
        }
        if (lowerCase.startsWith("text/")) {
            contentValues.put("file_type", (Integer) 4);
        } else if (lowerCase.equals("application/zip")) {
            contentValues.put("file_type", (Integer) 5);
        } else if (lowerCase.equals("application/vnd.android.package-archive")) {
            contentValues.put("file_type", (Integer) 6);
        }
    }

    public static void computeRingtoneAttributes(String str, String str2, ContentValues contentValues) {
        int i;
        boolean z;
        if (str == null || str2 == null || contentValues == null) {
            MtkLog.e("FileSearchHelper", "computeRingtoneAttributes: Param error!");
            return;
        }
        int state = getState("/ringtones/", str, str2);
        if (state != 0) {
            contentValues.put("is_ringtone", Boolean.valueOf(state > 0));
            z = state > 0;
            i = state + 0;
        } else {
            i = 0;
            z = false;
        }
        int state2 = getState("/notifications/", str, str2);
        if (state2 != 0) {
            contentValues.put("is_notification", Boolean.valueOf(state2 > 0));
            z = z || state2 > 0;
            i += state2;
        }
        int state3 = getState("/alarms/", str, str2);
        if (state3 != 0) {
            contentValues.put("is_alarm", Boolean.valueOf(state3 > 0));
            z = z || state3 > 0;
            i += state3;
        }
        int state4 = getState("/podcasts/", str, str2);
        if (state4 != 0) {
            contentValues.put("is_podcast", Boolean.valueOf(state4 > 0));
            z = z || state4 > 0;
            i += state4;
        }
        if (getState("/music/", str, str2) > 0) {
            contentValues.put("is_music", (Boolean) true);
        } else if (z) {
            contentValues.put("is_music", (Boolean) false);
        } else if (i < 0) {
            contentValues.put("is_music", (Boolean) true);
        }
    }

    private static int getState(String str, String str2, String str3) {
        if (!str2.endsWith("/")) {
            str2 = str2 + "/";
        }
        if (!str3.endsWith("/")) {
            str3 = str3 + "/";
        }
        int iIndexOf = str2.toLowerCase(Locale.ENGLISH).indexOf(str);
        if (str3.toLowerCase(Locale.ENGLISH).indexOf(str) > 0) {
            return 1;
        }
        if (iIndexOf <= 0) {
            return 0;
        }
        return -1;
    }

    public static Cursor doShortcutSearch(SQLiteDatabase sQLiteDatabase, SQLiteQueryBuilder sQLiteQueryBuilder, Uri uri, String str) {
        if (sQLiteDatabase == null || sQLiteQueryBuilder == null || uri == null) {
            MtkLog.e("FileSearchHelper", "doShortcutSearch: Param error!");
            return null;
        }
        String strTrim = Uri.decode(uri.getLastPathSegment()).trim();
        if (TextUtils.isEmpty(strTrim)) {
            MtkLog.e("FileSearchHelper", "doShortcutSearch: Null id!");
            return null;
        }
        String[] strArr = {strTrim};
        sQLiteQueryBuilder.setTables("files");
        if (MediaUtils.LOG_QUERY) {
            MtkLog.d("FileSearchHelper", "doShortcutSearch: uri = " + uri + ",selection = _id=?, selectionArgs = " + Arrays.toString(strArr) + ", caller pid = " + Binder.getCallingPid());
        }
        return sQLiteQueryBuilder.query(sQLiteDatabase, sSearchFileCols, "_id=?", strArr, null, null, null, str);
    }
}
