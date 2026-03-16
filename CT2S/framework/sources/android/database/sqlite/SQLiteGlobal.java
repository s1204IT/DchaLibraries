package android.database.sqlite;

import android.content.res.Resources;
import android.os.StatFs;
import android.os.SystemProperties;
import com.android.internal.R;

public final class SQLiteGlobal {
    private static final String TAG = "SQLiteGlobal";
    private static int sDefaultPageSize;
    private static final Object sLock = new Object();

    private static native int nativeReleaseMemory();

    private SQLiteGlobal() {
    }

    public static int releaseMemory() {
        return nativeReleaseMemory();
    }

    public static int getDefaultPageSize() {
        int i;
        synchronized (sLock) {
            if (sDefaultPageSize == 0) {
                sDefaultPageSize = new StatFs("/data").getBlockSize();
            }
            i = SystemProperties.getInt("debug.sqlite.pagesize", sDefaultPageSize);
        }
        return i;
    }

    public static String getDefaultJournalMode() {
        return SystemProperties.get("debug.sqlite.journalmode", Resources.getSystem().getString(R.string.db_default_journal_mode));
    }

    public static int getJournalSizeLimit() {
        return SystemProperties.getInt("debug.sqlite.journalsizelimit", Resources.getSystem().getInteger(R.integer.db_journal_size_limit));
    }

    public static String getDefaultSyncMode() {
        return SystemProperties.get("debug.sqlite.syncmode", Resources.getSystem().getString(R.string.db_default_sync_mode));
    }

    public static String getWALSyncMode() {
        return SystemProperties.get("debug.sqlite.wal.syncmode", Resources.getSystem().getString(R.string.db_wal_sync_mode));
    }

    public static int getWALAutoCheckpoint() {
        int value = SystemProperties.getInt("debug.sqlite.wal.autocheckpoint", Resources.getSystem().getInteger(R.integer.db_wal_autocheckpoint));
        return Math.max(1, value);
    }

    public static int getWALConnectionPoolSize() {
        int value = SystemProperties.getInt("debug.sqlite.wal.poolsize", Resources.getSystem().getInteger(R.integer.db_connection_pool_size));
        return Math.max(2, value);
    }
}
