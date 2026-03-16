package com.android.providers.contacts;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteTransactionListener;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.util.List;
import java.util.Map;

public class ContactsTransaction {
    private final boolean mBatch;
    private boolean mYieldFailed;
    private final List<SQLiteDatabase> mDatabasesForTransaction = Lists.newArrayList();
    private final Map<String, SQLiteDatabase> mDatabaseTagMap = Maps.newHashMap();
    private boolean mIsDirty = false;

    public ContactsTransaction(boolean batch) {
        this.mBatch = batch;
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

    public void startTransactionForDb(SQLiteDatabase db, String tag, SQLiteTransactionListener listener) {
        if (!hasDbInTransaction(tag)) {
            this.mDatabasesForTransaction.add(0, db);
            this.mDatabaseTagMap.put(tag, db);
            if (listener != null) {
                db.beginTransactionWithListener(listener);
            } else {
                db.beginTransaction();
            }
        }
    }

    public boolean hasDbInTransaction(String tag) {
        return this.mDatabaseTagMap.containsKey(tag);
    }

    public SQLiteDatabase getDbForTag(String tag) {
        return this.mDatabaseTagMap.get(tag);
    }

    public SQLiteDatabase removeDbForTag(String tag) {
        SQLiteDatabase db = this.mDatabaseTagMap.get(tag);
        this.mDatabaseTagMap.remove(tag);
        this.mDatabasesForTransaction.remove(db);
        return db;
    }

    public void markSuccessful(boolean callerIsBatch) {
        if (!this.mBatch || callerIsBatch) {
            for (SQLiteDatabase db : this.mDatabasesForTransaction) {
                db.setTransactionSuccessful();
            }
        }
    }

    public void finish(boolean callerIsBatch) {
        if (!this.mBatch || callerIsBatch) {
            for (SQLiteDatabase db : this.mDatabasesForTransaction) {
                if (!this.mYieldFailed || db.isDbLockedByCurrentThread()) {
                    db.endTransaction();
                }
            }
            this.mDatabasesForTransaction.clear();
            this.mDatabaseTagMap.clear();
            this.mIsDirty = false;
        }
    }
}
