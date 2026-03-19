package android.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseConfiguration;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.util.Pair;
import java.io.File;
import java.util.Iterator;
import java.util.List;

public final class DefaultDatabaseErrorHandler implements DatabaseErrorHandler {
    private static final String TAG = "DefaultDatabaseErrorHandler";

    @Override
    public void onCorruption(SQLiteDatabase sQLiteDatabase) {
        Log.e(TAG, "Corruption reported by sqlite on database: " + sQLiteDatabase.getPath());
        if (!sQLiteDatabase.isOpen()) {
            deleteDatabaseFile(sQLiteDatabase.getPath());
            return;
        }
        List<Pair<String, String>> attachedDbs = null;
        try {
            try {
                attachedDbs = sQLiteDatabase.getAttachedDbs();
            } finally {
                if (attachedDbs != null) {
                    Iterator<Pair<String, String>> it = attachedDbs.iterator();
                    while (it.hasNext()) {
                        deleteDatabaseFile(it.next().second);
                    }
                } else {
                    deleteDatabaseFile(sQLiteDatabase.getPath());
                }
            }
        } catch (SQLiteException e) {
        }
        try {
            sQLiteDatabase.close();
        } catch (SQLiteException e2) {
        }
    }

    private void deleteDatabaseFile(String str) {
        if (str.equalsIgnoreCase(SQLiteDatabaseConfiguration.MEMORY_DB_PATH) || str.trim().length() == 0) {
            return;
        }
        Log.e(TAG, "deleting the database file: " + str);
        try {
            SQLiteDatabase.deleteDatabase(new File(str));
        } catch (Exception e) {
            Log.w(TAG, "delete failed: " + e.getMessage());
        }
    }
}
