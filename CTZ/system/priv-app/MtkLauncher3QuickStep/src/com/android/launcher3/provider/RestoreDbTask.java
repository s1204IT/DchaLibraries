package com.android.launcher3.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Utilities;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.provider.LauncherDbUtils;
import com.android.launcher3.util.LogConfig;
import java.io.InvalidObjectException;

/* loaded from: classes.dex */
public class RestoreDbTask {
    private static final String INFO_COLUMN_DEFAULT_VALUE = "dflt_value";
    private static final String INFO_COLUMN_NAME = "name";
    private static final String RESTORE_TASK_PENDING = "restore_task_pending";
    private static final String TAG = "RestoreDbTask";

    public static boolean performRestore(LauncherProvider.DatabaseHelper databaseHelper) {
        SQLiteDatabase writableDatabase = databaseHelper.getWritableDatabase();
        try {
            LauncherDbUtils.SQLiteTransaction sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(writableDatabase);
            Throwable th = null;
            try {
                try {
                    new RestoreDbTask().sanitizeDB(databaseHelper, writableDatabase);
                    sQLiteTransaction.commit();
                    return true;
                } finally {
                }
            } finally {
                $closeResource(th, sQLiteTransaction);
            }
        } catch (Exception e) {
            FileLog.e(TAG, "Failed to verify db", e);
            return false;
        }
    }

    private static /* synthetic */ void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private void sanitizeDB(LauncherProvider.DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase) throws Exception {
        long defaultProfileId = getDefaultProfileId(sQLiteDatabase);
        int iDelete = sQLiteDatabase.delete(LauncherSettings.Favorites.TABLE_NAME, "profileId != ?", new String[]{Long.toString(defaultProfileId)});
        if (iDelete > 0) {
            FileLog.d(TAG, iDelete + " items belonging to a managed profile, were deleted");
        }
        boolean zIsPropertyEnabled = Utilities.isPropertyEnabled(LogConfig.KEEP_ALL_ICONS);
        ContentValues contentValues = new ContentValues();
        contentValues.put(LauncherSettings.Favorites.RESTORED, Integer.valueOf((zIsPropertyEnabled ? 8 : 0) | 1));
        sQLiteDatabase.update(LauncherSettings.Favorites.TABLE_NAME, contentValues, null, null);
        contentValues.put(LauncherSettings.Favorites.RESTORED, Integer.valueOf(7 | (zIsPropertyEnabled ? 8 : 0)));
        sQLiteDatabase.update(LauncherSettings.Favorites.TABLE_NAME, contentValues, "itemType = ?", new String[]{Integer.toString(4)});
        long defaultUserSerial = databaseHelper.getDefaultUserSerial();
        if (Utilities.longCompare(defaultProfileId, defaultUserSerial) != 0) {
            FileLog.d(TAG, "Changing primary user id from " + defaultProfileId + " to " + defaultUserSerial);
            migrateProfileId(sQLiteDatabase, defaultUserSerial);
        }
    }

    protected void migrateProfileId(SQLiteDatabase sQLiteDatabase, long j) throws SQLException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LauncherSettings.Favorites.PROFILE_ID, Long.valueOf(j));
        sQLiteDatabase.update(LauncherSettings.Favorites.TABLE_NAME, contentValues, null, null);
        sQLiteDatabase.execSQL("ALTER TABLE favorites RENAME TO favorites_old;");
        LauncherSettings.Favorites.addTableToDb(sQLiteDatabase, j, false);
        sQLiteDatabase.execSQL("INSERT INTO favorites SELECT * FROM favorites_old;");
        sQLiteDatabase.execSQL("DROP TABLE favorites_old;");
    }

    protected long getDefaultProfileId(SQLiteDatabase sQLiteDatabase) throws Exception {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("PRAGMA table_info (favorites)", null);
        try {
            int columnIndex = cursorRawQuery.getColumnIndex(INFO_COLUMN_NAME);
            while (cursorRawQuery.moveToNext()) {
                if (LauncherSettings.Favorites.PROFILE_ID.equals(cursorRawQuery.getString(columnIndex))) {
                    return cursorRawQuery.getLong(cursorRawQuery.getColumnIndex(INFO_COLUMN_DEFAULT_VALUE));
                }
            }
            throw new InvalidObjectException("Table does not have a profile id column");
        } finally {
            if (cursorRawQuery != null) {
                $closeResource(null, cursorRawQuery);
            }
        }
    }

    public static boolean isPending(Context context) {
        return Utilities.getPrefs(context).getBoolean(RESTORE_TASK_PENDING, false);
    }

    public static void setPending(Context context, boolean z) {
        FileLog.d(TAG, "Restore data received through full backup " + z);
        Utilities.getPrefs(context).edit().putBoolean(RESTORE_TASK_PENDING, z).commit();
    }
}
