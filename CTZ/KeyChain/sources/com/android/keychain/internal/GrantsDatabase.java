package com.android.keychain.internal;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GrantsDatabase {
    public DatabaseHelper mDatabaseHelper;

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "grants.db", (SQLiteDatabase.CursorFactory) null, 2);
        }

        void createSelectableTable(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS userselectable (  alias STRING NOT NULL,  is_selectable STRING NOT NULL,  UNIQUE (alias))");
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE grants (  alias STRING NOT NULL,  uid INTEGER NOT NULL,  UNIQUE (alias,uid))");
            createSelectableTable(sQLiteDatabase);
        }

        private boolean hasEntryInUserSelectableTable(SQLiteDatabase sQLiteDatabase, String str) {
            return DatabaseUtils.longForQuery(sQLiteDatabase, "SELECT COUNT(*) FROM userselectable WHERE alias=?", new String[]{str}) > 0;
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            Log.w("KeyChain", "upgrade from version " + i + " to version " + i2);
            if (i == 1) {
                createSelectableTable(sQLiteDatabase);
                Cursor cursorQuery = sQLiteDatabase.query("grants", new String[]{"alias"}, null, null, "alias", null, null);
                while (cursorQuery != null) {
                    Throwable th = null;
                    try {
                        try {
                            if (!cursorQuery.moveToNext()) {
                                break;
                            }
                            String string = cursorQuery.getString(0);
                            if (!hasEntryInUserSelectableTable(sQLiteDatabase, string)) {
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("alias", string);
                                contentValues.put("is_selectable", Boolean.toString(true));
                                sQLiteDatabase.replace("userselectable", null, contentValues);
                            }
                        } catch (Throwable th2) {
                            if (cursorQuery != null) {
                                if (th != null) {
                                    try {
                                        cursorQuery.close();
                                    } catch (Throwable th3) {
                                        th.addSuppressed(th3);
                                    }
                                } else {
                                    cursorQuery.close();
                                }
                            }
                            throw th2;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
        }
    }

    public GrantsDatabase(Context context) {
        this.mDatabaseHelper = new DatabaseHelper(context);
    }

    public void destroy() {
        this.mDatabaseHelper.close();
        this.mDatabaseHelper = null;
    }

    boolean hasGrantInternal(SQLiteDatabase sQLiteDatabase, int i, String str) {
        return DatabaseUtils.longForQuery(sQLiteDatabase, "SELECT COUNT(*) FROM grants WHERE uid=? AND alias=?", new String[]{String.valueOf(i), str}) > 0;
    }

    public boolean hasGrant(int i, String str) {
        return hasGrantInternal(this.mDatabaseHelper.getWritableDatabase(), i, str);
    }

    public void setGrant(int i, String str, boolean z) {
        SQLiteDatabase writableDatabase = this.mDatabaseHelper.getWritableDatabase();
        if (z) {
            if (!hasGrantInternal(writableDatabase, i, str)) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("alias", str);
                contentValues.put("uid", Integer.valueOf(i));
                writableDatabase.insert("grants", "alias", contentValues);
                return;
            }
            return;
        }
        writableDatabase.delete("grants", "uid=? AND alias=?", new String[]{String.valueOf(i), str});
    }

    public void removeAliasInformation(String str) {
        SQLiteDatabase writableDatabase = this.mDatabaseHelper.getWritableDatabase();
        writableDatabase.delete("grants", "alias=?", new String[]{str});
        writableDatabase.delete("userselectable", "alias=?", new String[]{str});
    }

    public void removeAllAliasesInformation() {
        SQLiteDatabase writableDatabase = this.mDatabaseHelper.getWritableDatabase();
        writableDatabase.delete("grants", null, null);
        writableDatabase.delete("userselectable", null, null);
    }

    public void purgeOldGrants(PackageManager packageManager) throws Exception {
        SQLiteDatabase writableDatabase = this.mDatabaseHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        Cursor cursorQuery = writableDatabase.query("grants", new String[]{"uid"}, null, null, "uid", null, null);
        while (true) {
            Throwable th = null;
            if (cursorQuery == null) {
                break;
            }
            try {
                try {
                    if (!cursorQuery.moveToNext()) {
                        break;
                    }
                    int i = cursorQuery.getInt(0);
                    if (!(packageManager.getPackagesForUid(i) != null)) {
                        Log.d("KeyChain", String.format("deleting grants for UID %d because its package is no longer installed", Integer.valueOf(i)));
                        writableDatabase.delete("grants", "uid=?", new String[]{Integer.toString(i)});
                    }
                } finally {
                }
            } catch (Throwable th2) {
                if (cursorQuery != null) {
                    $closeResource(th, cursorQuery);
                }
                throw th2;
            }
        }
        writableDatabase.setTransactionSuccessful();
        if (cursorQuery != null) {
            $closeResource(null, cursorQuery);
        }
        writableDatabase.endTransaction();
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
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

    public void setIsUserSelectable(String str, boolean z) {
        SQLiteDatabase writableDatabase = this.mDatabaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("alias", str);
        contentValues.put("is_selectable", Boolean.toString(z));
        writableDatabase.replace("userselectable", null, contentValues);
    }

    public boolean isUserSelectable(String str) throws Exception {
        Cursor cursorQuery = this.mDatabaseHelper.getWritableDatabase().query("userselectable", new String[]{"is_selectable"}, "alias=?", new String[]{str}, null, null, null);
        Throwable th = null;
        if (cursorQuery != null) {
            try {
                try {
                    if (cursorQuery.moveToNext()) {
                        boolean z = Boolean.parseBoolean(cursorQuery.getString(0));
                        if (cursorQuery.getCount() > 1) {
                            Log.w("KeyChain", String.format("Have more than one result for alias %s", str));
                        }
                        return z;
                    }
                } finally {
                }
            } finally {
                if (cursorQuery != null) {
                }
            }
            if (cursorQuery != null) {
                $closeResource(th, cursorQuery);
            }
        }
        if (cursorQuery != null) {
            $closeResource(null, cursorQuery);
        }
        return false;
    }
}
