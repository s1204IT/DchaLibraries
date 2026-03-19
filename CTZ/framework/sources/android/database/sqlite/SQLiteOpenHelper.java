package android.database.sqlite;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.FileUtils;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.io.File;

public abstract class SQLiteOpenHelper {
    private static final String TAG = SQLiteOpenHelper.class.getSimpleName();
    private final Context mContext;
    private SQLiteDatabase mDatabase;
    private boolean mIsInitializing;
    private final int mMinimumSupportedVersion;
    private final String mName;
    private final int mNewVersion;
    private SQLiteDatabase.OpenParams.Builder mOpenParamsBuilder;

    public abstract void onCreate(SQLiteDatabase sQLiteDatabase);

    public abstract void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2);

    public SQLiteOpenHelper(Context context, String str, SQLiteDatabase.CursorFactory cursorFactory, int i) {
        this(context, str, cursorFactory, i, (DatabaseErrorHandler) null);
    }

    public SQLiteOpenHelper(Context context, String str, SQLiteDatabase.CursorFactory cursorFactory, int i, DatabaseErrorHandler databaseErrorHandler) {
        this(context, str, cursorFactory, i, 0, databaseErrorHandler);
    }

    public SQLiteOpenHelper(Context context, String str, int i, SQLiteDatabase.OpenParams openParams) {
        this(context, str, i, 0, openParams.toBuilder());
    }

    public SQLiteOpenHelper(Context context, String str, SQLiteDatabase.CursorFactory cursorFactory, int i, int i2, DatabaseErrorHandler databaseErrorHandler) {
        this(context, str, i, i2, new SQLiteDatabase.OpenParams.Builder());
        this.mOpenParamsBuilder.setCursorFactory(cursorFactory);
        this.mOpenParamsBuilder.setErrorHandler(databaseErrorHandler);
    }

    private SQLiteOpenHelper(Context context, String str, int i, int i2, SQLiteDatabase.OpenParams.Builder builder) {
        Preconditions.checkNotNull(builder);
        if (i < 1) {
            throw new IllegalArgumentException("Version must be >= 1, was " + i);
        }
        this.mContext = context;
        this.mName = str;
        this.mNewVersion = i;
        this.mMinimumSupportedVersion = Math.max(0, i2);
        setOpenParamsBuilder(builder);
    }

    public String getDatabaseName() {
        return this.mName;
    }

    public void setWriteAheadLoggingEnabled(boolean z) {
        synchronized (this) {
            if (this.mOpenParamsBuilder.isWriteAheadLoggingEnabled() != z) {
                if (this.mDatabase != null && this.mDatabase.isOpen() && !this.mDatabase.isReadOnly()) {
                    if (z) {
                        this.mDatabase.enableWriteAheadLogging();
                    } else {
                        this.mDatabase.disableWriteAheadLogging();
                    }
                }
                this.mOpenParamsBuilder.setWriteAheadLoggingEnabled(z);
            }
            this.mOpenParamsBuilder.addOpenFlags(1073741824);
        }
    }

    public void setLookasideConfig(int i, int i2) {
        synchronized (this) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                throw new IllegalStateException("Lookaside memory config cannot be changed after opening the database");
            }
            this.mOpenParamsBuilder.setLookasideConfig(i, i2);
        }
    }

    public void setOpenParams(SQLiteDatabase.OpenParams openParams) {
        Preconditions.checkNotNull(openParams);
        synchronized (this) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                throw new IllegalStateException("OpenParams cannot be set after opening the database");
            }
            setOpenParamsBuilder(new SQLiteDatabase.OpenParams.Builder(openParams));
        }
    }

    private void setOpenParamsBuilder(SQLiteDatabase.OpenParams.Builder builder) {
        this.mOpenParamsBuilder = builder;
        this.mOpenParamsBuilder.addOpenFlags(268435456);
    }

    public void setIdleConnectionTimeout(long j) {
        synchronized (this) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                throw new IllegalStateException("Connection timeout setting cannot be changed after opening the database");
            }
            this.mOpenParamsBuilder.setIdleConnectionTimeout(j);
        }
    }

    public SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase databaseLocked;
        synchronized (this) {
            databaseLocked = getDatabaseLocked(true);
        }
        return databaseLocked;
    }

    public SQLiteDatabase getReadableDatabase() {
        SQLiteDatabase databaseLocked;
        synchronized (this) {
            databaseLocked = getDatabaseLocked(false);
        }
        return databaseLocked;
    }

    private SQLiteDatabase getDatabaseLocked(boolean z) throws Throwable {
        SQLiteDatabase sQLiteDatabase;
        SQLException e;
        if (this.mDatabase != null) {
            if (!this.mDatabase.isOpen()) {
                this.mDatabase = null;
            } else if (!z || !this.mDatabase.isReadOnly()) {
                return this.mDatabase;
            }
        }
        if (this.mIsInitializing) {
            throw new IllegalStateException("getDatabase called recursively");
        }
        SQLiteDatabase sQLiteDatabaseOpenDatabase = this.mDatabase;
        try {
            this.mIsInitializing = true;
            if (sQLiteDatabaseOpenDatabase != null) {
                if (z && sQLiteDatabaseOpenDatabase.isReadOnly()) {
                    sQLiteDatabaseOpenDatabase.reopenReadWrite();
                }
            } else if (this.mName == null) {
                sQLiteDatabaseOpenDatabase = SQLiteDatabase.createInMemory(this.mOpenParamsBuilder.build());
            } else {
                try {
                    File databasePath = this.mContext.getDatabasePath(this.mName);
                    SQLiteDatabase.OpenParams openParamsBuild = this.mOpenParamsBuilder.build();
                    try {
                        SQLiteDatabase sQLiteDatabaseOpenDatabase2 = SQLiteDatabase.openDatabase(databasePath, openParamsBuild);
                        try {
                            setFilePermissionsForDb(databasePath.getPath());
                            sQLiteDatabaseOpenDatabase = sQLiteDatabaseOpenDatabase2;
                        } catch (SQLException e2) {
                            e = e2;
                            if (z) {
                                throw e;
                            }
                            Log.e(TAG, "Couldn't open " + this.mName + " for writing (will try read-only):", e);
                            sQLiteDatabaseOpenDatabase = SQLiteDatabase.openDatabase(databasePath, openParamsBuild.toBuilder().addOpenFlags(1).build());
                        }
                    } catch (SQLException e3) {
                        e = e3;
                    }
                } catch (Throwable th) {
                    th = th;
                    sQLiteDatabaseOpenDatabase = sQLiteDatabase;
                    this.mIsInitializing = false;
                    if (sQLiteDatabaseOpenDatabase != null && sQLiteDatabaseOpenDatabase != this.mDatabase) {
                        sQLiteDatabaseOpenDatabase.close();
                    }
                    throw th;
                }
            }
            onConfigure(sQLiteDatabaseOpenDatabase);
            int version = sQLiteDatabaseOpenDatabase.getVersion();
            if (version != this.mNewVersion) {
                if (sQLiteDatabaseOpenDatabase.isReadOnly()) {
                    throw new SQLiteException("Can't upgrade read-only database from version " + sQLiteDatabaseOpenDatabase.getVersion() + " to " + this.mNewVersion + ": " + this.mName);
                }
                if (version > 0 && version < this.mMinimumSupportedVersion) {
                    File file = new File(sQLiteDatabaseOpenDatabase.getPath());
                    onBeforeDelete(sQLiteDatabaseOpenDatabase);
                    sQLiteDatabaseOpenDatabase.close();
                    if (!SQLiteDatabase.deleteDatabase(file)) {
                        throw new IllegalStateException("Unable to delete obsolete database " + this.mName + " with version " + version);
                    }
                    this.mIsInitializing = false;
                    SQLiteDatabase databaseLocked = getDatabaseLocked(z);
                    this.mIsInitializing = false;
                    if (sQLiteDatabaseOpenDatabase != null && sQLiteDatabaseOpenDatabase != this.mDatabase) {
                        sQLiteDatabaseOpenDatabase.close();
                    }
                    return databaseLocked;
                }
                sQLiteDatabaseOpenDatabase.beginTransaction();
                try {
                    if (version == 0) {
                        onCreate(sQLiteDatabaseOpenDatabase);
                    } else if (version > this.mNewVersion) {
                        onDowngrade(sQLiteDatabaseOpenDatabase, version, this.mNewVersion);
                    } else {
                        onUpgrade(sQLiteDatabaseOpenDatabase, version, this.mNewVersion);
                    }
                    sQLiteDatabaseOpenDatabase.setVersion(this.mNewVersion);
                    sQLiteDatabaseOpenDatabase.setTransactionSuccessful();
                    sQLiteDatabaseOpenDatabase.endTransaction();
                } catch (Throwable th2) {
                    sQLiteDatabaseOpenDatabase.endTransaction();
                    throw th2;
                }
            }
            onOpen(sQLiteDatabaseOpenDatabase);
            if (sQLiteDatabaseOpenDatabase.isReadOnly()) {
                Log.w(TAG, "Opened " + this.mName + " in read-only mode");
            }
            this.mDatabase = sQLiteDatabaseOpenDatabase;
            this.mIsInitializing = false;
            if (sQLiteDatabaseOpenDatabase != null && sQLiteDatabaseOpenDatabase != this.mDatabase) {
                sQLiteDatabaseOpenDatabase.close();
            }
            return sQLiteDatabaseOpenDatabase;
        } catch (Throwable th3) {
            th = th3;
            this.mIsInitializing = false;
            if (sQLiteDatabaseOpenDatabase != null) {
                sQLiteDatabaseOpenDatabase.close();
            }
            throw th;
        }
    }

    private static void setFilePermissionsForDb(String str) {
        FileUtils.setPermissions(str, DevicePolicyManager.PROFILE_KEYGUARD_FEATURES_AFFECT_OWNER, -1, -1);
    }

    public synchronized void close() {
        if (this.mIsInitializing) {
            throw new IllegalStateException("Closed during initialization");
        }
        if (this.mDatabase != null && this.mDatabase.isOpen()) {
            this.mDatabase.close();
            this.mDatabase = null;
        }
    }

    public void onConfigure(SQLiteDatabase sQLiteDatabase) {
    }

    public void onBeforeDelete(SQLiteDatabase sQLiteDatabase) {
    }

    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        throw new SQLiteException("Can't downgrade database from version " + i + " to " + i2);
    }

    public void onOpen(SQLiteDatabase sQLiteDatabase) {
    }
}
