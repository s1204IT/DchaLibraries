package android.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseConfiguration;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.util.Pair;
import java.io.File;
import java.util.List;

public final class DefaultDatabaseErrorHandler implements DatabaseErrorHandler {
    private static final String TAG = "DefaultDatabaseErrorHandler";

    @Override
    public void onCorruption(SQLiteDatabase dbObj) {
        Log.e(TAG, "Corruption reported by sqlite on database: " + dbObj.getPath());
        if (!dbObj.isOpen()) {
            deleteDatabaseFile(dbObj.getPath());
            return;
        }
        List<Pair<String, String>> attachedDbs = null;
        try {
            try {
                attachedDbs = dbObj.getAttachedDbs();
            } catch (Throwable th) {
                if (attachedDbs != null) {
                    for (Pair<String, String> p : attachedDbs) {
                        deleteDatabaseFile((String) p.second);
                    }
                    throw th;
                }
                deleteDatabaseFile(dbObj.getPath());
                throw th;
            }
        } catch (SQLiteException e) {
        }
        try {
            dbObj.close();
        } catch (SQLiteException e2) {
        }
        if (attachedDbs != null) {
            for (Pair<String, String> p2 : attachedDbs) {
                deleteDatabaseFile((String) p2.second);
            }
            return;
        }
        deleteDatabaseFile(dbObj.getPath());
    }

    private void deleteDatabaseFile(String fileName) {
        if (fileName.equalsIgnoreCase(SQLiteDatabaseConfiguration.MEMORY_DB_PATH) || fileName.trim().length() == 0) {
            return;
        }
        Log.e(TAG, "deleting the database file: " + fileName);
        try {
            SQLiteDatabase.deleteDatabase(new File(fileName));
        } catch (Exception e) {
            Log.w(TAG, "delete failed: " + e.getMessage());
        }
    }
}
