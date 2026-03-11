package com.android.launcher3.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherSettings;
import java.util.ArrayList;
import java.util.Collection;

public class LauncherDbUtils {
    private static final String TAG = "LauncherDbUtils";

    public static boolean prepareScreenZeroToHostQsb(Context context, SQLiteDatabase sQLiteDatabase) {
        try {
            SQLiteTransaction sQLiteTransaction = new SQLiteTransaction(sQLiteDatabase);
            try {
                ArrayList<Long> screenIdsFromCursor = getScreenIdsFromCursor(sQLiteDatabase.query(LauncherSettings.WorkspaceScreens.TABLE_NAME, null, null, null, null, null, LauncherSettings.WorkspaceScreens.SCREEN_RANK));
                if (screenIdsFromCursor.isEmpty()) {
                    sQLiteTransaction.commit();
                    sQLiteTransaction.close();
                    return true;
                }
                if (screenIdsFromCursor.get(0).longValue() != 0) {
                    if (screenIdsFromCursor.indexOf(0L) > -1) {
                        long j = 1;
                        while (screenIdsFromCursor.indexOf(Long.valueOf(j)) > -1) {
                            j++;
                        }
                        renameScreen(sQLiteDatabase, 0L, j);
                    }
                    renameScreen(sQLiteDatabase, screenIdsFromCursor.get(0).longValue(), 0L);
                }
                if (DatabaseUtils.queryNumEntries(sQLiteDatabase, LauncherSettings.Favorites.TABLE_NAME, "container = -100 and screen = 0 and cellY = 0") == 0) {
                    sQLiteTransaction.commit();
                    sQLiteTransaction.close();
                    return true;
                }
                new LossyScreenMigrationTask(context, LauncherAppState.getIDP(context), sQLiteDatabase).migrateScreen0();
                sQLiteTransaction.commit();
                sQLiteTransaction.close();
                return true;
            } finally {
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update workspace size", e);
            return false;
        }
    }

    private static void renameScreen(SQLiteDatabase sQLiteDatabase, long j, long j2) {
        String[] strArr = {Long.toString(j)};
        ContentValues contentValues = new ContentValues();
        contentValues.put("_id", Long.valueOf(j2));
        sQLiteDatabase.update(LauncherSettings.WorkspaceScreens.TABLE_NAME, contentValues, "_id = ?", strArr);
        contentValues.clear();
        contentValues.put(LauncherSettings.Favorites.SCREEN, Long.valueOf(j2));
        sQLiteDatabase.update(LauncherSettings.Favorites.TABLE_NAME, contentValues, "container = -100 and screen = ?", strArr);
    }

    public static ArrayList<Long> getScreenIdsFromCursor(Cursor cursor) {
        try {
            return (ArrayList) iterateCursor(cursor, cursor.getColumnIndexOrThrow("_id"), new ArrayList());
        } finally {
            cursor.close();
        }
    }

    public static <T extends Collection<Long>> T iterateCursor(Cursor cursor, int i, T t) {
        while (cursor.moveToNext()) {
            t.add(Long.valueOf(cursor.getLong(i)));
        }
        return t;
    }

    public static class SQLiteTransaction implements AutoCloseable {
        private final SQLiteDatabase mDb;

        public SQLiteTransaction(SQLiteDatabase sQLiteDatabase) {
            this.mDb = sQLiteDatabase;
            sQLiteDatabase.beginTransaction();
        }

        public void commit() {
            this.mDb.setTransactionSuccessful();
        }

        @Override
        public void close() {
            this.mDb.endTransaction();
        }
    }
}
