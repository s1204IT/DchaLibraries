package com.android.providers.blockednumber;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

public class BlockedNumberDatabaseHelper {
    private static BlockedNumberDatabaseHelper sInstance;
    private final Context mContext;
    private final OpenHelper mOpenHelper;

    private static final class OpenHelper extends SQLiteOpenHelper {
        public OpenHelper(Context context, String str, SQLiteDatabase.CursorFactory cursorFactory, int i) {
            super(context, str, cursorFactory, i);
            setIdleConnectionTimeout(30000L);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            createTables(sQLiteDatabase);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            if (i < 2) {
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS blocked");
                createTables(sQLiteDatabase);
            }
        }

        private void createTables(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE blocked (_id INTEGER PRIMARY KEY AUTOINCREMENT,original_number TEXT NOT NULL UNIQUE,e164_number TEXT)");
            sQLiteDatabase.execSQL("CREATE INDEX blocked_number_idx_original ON blocked (original_number);");
            sQLiteDatabase.execSQL("CREATE INDEX blocked_number_idx_e164 ON blocked (e164_number);");
        }
    }

    @VisibleForTesting
    public static BlockedNumberDatabaseHelper newInstanceForTest(Context context) {
        return new BlockedNumberDatabaseHelper(context, true);
    }

    private BlockedNumberDatabaseHelper(Context context, boolean z) {
        Preconditions.checkNotNull(context);
        this.mContext = context;
        this.mOpenHelper = new OpenHelper(this.mContext, z ? null : "blockednumbers.db", null, 2);
    }

    public static synchronized BlockedNumberDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BlockedNumberDatabaseHelper(context, false);
        }
        return sInstance;
    }

    public SQLiteDatabase getReadableDatabase() {
        return this.mOpenHelper.getReadableDatabase();
    }

    public SQLiteDatabase getWritableDatabase() {
        return this.mOpenHelper.getWritableDatabase();
    }
}
