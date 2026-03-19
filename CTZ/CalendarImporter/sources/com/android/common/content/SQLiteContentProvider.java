package com.android.common.content;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;
import java.util.ArrayList;

public abstract class SQLiteContentProvider extends ContentProvider implements SQLiteTransactionListener {
    private static final int MAX_OPERATIONS_PER_YIELD_POINT = 500;
    private static final int SLEEP_AFTER_YIELD_DELAY = 4000;
    private static final String TAG = "SQLiteContentProvider";
    private final ThreadLocal<Boolean> mApplyingBatch = new ThreadLocal<>();
    protected SQLiteDatabase mDb;
    private volatile boolean mNotifyChange;
    private SQLiteOpenHelper mOpenHelper;

    protected abstract int deleteInTransaction(Uri uri, String str, String[] strArr);

    protected abstract SQLiteOpenHelper getDatabaseHelper(Context context);

    protected abstract Uri insertInTransaction(Uri uri, ContentValues contentValues);

    protected abstract void notifyChange();

    protected abstract int updateInTransaction(Uri uri, ContentValues contentValues, String str, String[] strArr);

    public int getMaxOperationsPerYield() {
        return MAX_OPERATIONS_PER_YIELD_POINT;
    }

    @Override
    public boolean onCreate() {
        this.mOpenHelper = getDatabaseHelper(getContext());
        return true;
    }

    public SQLiteOpenHelper getDatabaseHelper() {
        return this.mOpenHelper;
    }

    private boolean applyingBatch() {
        return this.mApplyingBatch.get() != null && this.mApplyingBatch.get().booleanValue();
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Uri uriInsertInTransaction;
        if (!applyingBatch()) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransactionWithListener(this);
            try {
                uriInsertInTransaction = insertInTransaction(uri, contentValues);
                if (uriInsertInTransaction != null) {
                    this.mNotifyChange = true;
                }
                this.mDb.setTransactionSuccessful();
                this.mDb.endTransaction();
                onEndTransaction();
            } catch (Throwable th) {
                this.mDb.endTransaction();
                throw th;
            }
        } else {
            uriInsertInTransaction = insertInTransaction(uri, contentValues);
            if (uriInsertInTransaction != null) {
                this.mNotifyChange = true;
            }
        }
        return uriInsertInTransaction;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        int length = contentValuesArr.length;
        this.mDb = this.mOpenHelper.getWritableDatabase();
        this.mDb.beginTransactionWithListener(this);
        for (ContentValues contentValues : contentValuesArr) {
            try {
                if (insertInTransaction(uri, contentValues) != null) {
                    this.mNotifyChange = true;
                }
                boolean z = this.mNotifyChange;
                SQLiteDatabase sQLiteDatabase = this.mDb;
                this.mDb.yieldIfContendedSafely();
                this.mDb = sQLiteDatabase;
                this.mNotifyChange = z;
            } catch (Throwable th) {
                this.mDb.endTransaction();
                throw th;
            }
        }
        this.mDb.setTransactionSuccessful();
        this.mDb.endTransaction();
        onEndTransaction();
        return length;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int iUpdateInTransaction;
        if (!applyingBatch()) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransactionWithListener(this);
            try {
                iUpdateInTransaction = updateInTransaction(uri, contentValues, str, strArr);
                if (iUpdateInTransaction > 0) {
                    this.mNotifyChange = true;
                }
                this.mDb.setTransactionSuccessful();
                this.mDb.endTransaction();
                onEndTransaction();
            } catch (Throwable th) {
                this.mDb.endTransaction();
                throw th;
            }
        } else {
            iUpdateInTransaction = updateInTransaction(uri, contentValues, str, strArr);
            if (iUpdateInTransaction > 0) {
                this.mNotifyChange = true;
            }
        }
        return iUpdateInTransaction;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iDeleteInTransaction;
        if (!applyingBatch()) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransactionWithListener(this);
            try {
                iDeleteInTransaction = deleteInTransaction(uri, str, strArr);
                if (iDeleteInTransaction > 0) {
                    this.mNotifyChange = true;
                }
                this.mDb.setTransactionSuccessful();
                this.mDb.endTransaction();
                onEndTransaction();
            } catch (Throwable th) {
                this.mDb.endTransaction();
                throw th;
            }
        } else {
            iDeleteInTransaction = deleteInTransaction(uri, str, strArr);
            if (iDeleteInTransaction > 0) {
                this.mNotifyChange = true;
            }
        }
        return iDeleteInTransaction;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        this.mDb = this.mOpenHelper.getWritableDatabase();
        this.mDb.beginTransactionWithListener(this);
        try {
            this.mApplyingBatch.set(true);
            int size = arrayList.size();
            ContentProviderResult[] contentProviderResultArr = new ContentProviderResult[size];
            int i = 0;
            int i2 = 0;
            for (int i3 = 0; i3 < size; i3++) {
                i++;
                if (i > getMaxOperationsPerYield()) {
                    throw new OperationApplicationException("Too many content provider operations between yield points. The maximum number of operations per yield point is 500", i2);
                }
                ContentProviderOperation contentProviderOperation = arrayList.get(i3);
                if (i3 > 0 && contentProviderOperation.isYieldAllowed()) {
                    boolean z = this.mNotifyChange;
                    if (this.mDb.yieldIfContendedSafely(4000L)) {
                        this.mDb = this.mOpenHelper.getWritableDatabase();
                        this.mNotifyChange = z;
                        i2++;
                    }
                    i = 0;
                }
                contentProviderResultArr[i3] = contentProviderOperation.apply(this, contentProviderResultArr, i3);
            }
            this.mDb.setTransactionSuccessful();
            return contentProviderResultArr;
        } finally {
            this.mApplyingBatch.set(false);
            this.mDb.endTransaction();
            onEndTransaction();
        }
    }

    @Override
    public void onBegin() {
        onBeginTransaction();
    }

    @Override
    public void onCommit() {
        beforeTransactionCommit();
    }

    @Override
    public void onRollback() {
    }

    protected void onBeginTransaction() {
    }

    protected void beforeTransactionCommit() {
    }

    protected void onEndTransaction() {
        if (this.mNotifyChange) {
            this.mNotifyChange = false;
            notifyChange();
        }
    }
}
