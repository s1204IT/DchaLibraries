package com.android.providers.contacts;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteTransactionListener;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ContactsTransaction {
    private final boolean mBatch;
    private boolean mYieldFailed;
    private final List<SQLiteDatabase> mDatabasesForTransaction = Lists.newArrayList();
    private final Map<String, SQLiteDatabase> mDatabaseTagMap = Maps.newHashMap();
    private boolean mIsDirty = false;

    public ContactsTransaction(boolean z) {
        this.mBatch = z;
    }

    public boolean isBatch() {
        return this.mBatch;
    }

    public boolean isDirty() {
        return this.mIsDirty;
    }

    public void markDirty() {
        this.mIsDirty = true;
    }

    public void markYieldFailed() {
        this.mYieldFailed = true;
    }

    public void startTransactionForDb(SQLiteDatabase sQLiteDatabase, String str, SQLiteTransactionListener sQLiteTransactionListener) {
        if (!hasDbInTransaction(str)) {
            this.mDatabasesForTransaction.add(0, sQLiteDatabase);
            this.mDatabaseTagMap.put(str, sQLiteDatabase);
            if (sQLiteTransactionListener != null) {
                sQLiteDatabase.beginTransactionWithListenerNonExclusive(sQLiteTransactionListener);
            } else {
                sQLiteDatabase.beginTransactionNonExclusive();
            }
        }
    }

    public boolean hasDbInTransaction(String str) {
        return this.mDatabaseTagMap.containsKey(str);
    }

    public SQLiteDatabase getDbForTag(String str) {
        return this.mDatabaseTagMap.get(str);
    }

    public SQLiteDatabase removeDbForTag(String str) {
        SQLiteDatabase sQLiteDatabase = this.mDatabaseTagMap.get(str);
        this.mDatabaseTagMap.remove(str);
        this.mDatabasesForTransaction.remove(sQLiteDatabase);
        return sQLiteDatabase;
    }

    public void markSuccessful(boolean z) {
        if (!this.mBatch || z) {
            Iterator<SQLiteDatabase> it = this.mDatabasesForTransaction.iterator();
            while (it.hasNext()) {
                it.next().setTransactionSuccessful();
            }
        }
    }

    public void finish(boolean z) {
        if (!this.mBatch || z) {
            for (SQLiteDatabase sQLiteDatabase : this.mDatabasesForTransaction) {
                if (!this.mYieldFailed || sQLiteDatabase.isDbLockedByCurrentThread()) {
                    sQLiteDatabase.endTransaction();
                }
            }
            this.mDatabasesForTransaction.clear();
            this.mDatabaseTagMap.clear();
            this.mIsDirty = false;
        }
    }
}
