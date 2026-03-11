package com.android.browser.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class SQLiteContentProvider extends ContentProvider {
    private final ThreadLocal<Boolean> mApplyingBatch = new ThreadLocal<>();
    private Set<Uri> mChangedUris;
    protected SQLiteDatabase mDb;
    private SQLiteOpenHelper mOpenHelper;

    public abstract int deleteInTransaction(Uri uri, String str, String[] strArr, boolean z);

    public abstract SQLiteOpenHelper getDatabaseHelper(Context context);

    public abstract Uri insertInTransaction(Uri uri, ContentValues contentValues, boolean z);

    public abstract int updateInTransaction(Uri uri, ContentValues contentValues, String str, String[] strArr, boolean z);

    @Override
    public boolean onCreate() {
        Context context = getContext();
        this.mOpenHelper = getDatabaseHelper(context);
        this.mChangedUris = new HashSet();
        return true;
    }

    protected void postNotifyUri(Uri uri) {
        synchronized (this.mChangedUris) {
            this.mChangedUris.add(uri);
        }
    }

    public boolean isCallerSyncAdapter(Uri uri) {
        return false;
    }

    private boolean applyingBatch() {
        Log.d("cjzang", "SQLiteContentProvider---------->applyingBatch");
        return this.mApplyingBatch.get() != null && this.mApplyingBatch.get().booleanValue();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d("cjzang", "SQLiteContentProvider---------->insert uri =" + uri + "-----values =" + values);
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch = applyingBatch();
        Log.d("cjzang", "SQLiteContentProvider---------->applyingBatch =" + applyingBatch);
        if (!applyingBatch) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransaction();
            Log.d("cjzang", "SQLiteContentProvider---------->begin to insert------1");
            try {
                Uri result = insertInTransaction(uri, values, callerIsSyncAdapter);
                this.mDb.setTransactionSuccessful();
                Log.d("cjzang", "SQLiteContentProvider---------->finally");
                this.mDb.endTransaction();
                onEndTransaction(callerIsSyncAdapter);
                Log.d("cjzang", "SQLiteContentProvider---------->end to insert------1");
                return result;
            } catch (Throwable th) {
                Log.d("cjzang", "SQLiteContentProvider---------->finally");
                this.mDb.endTransaction();
                throw th;
            }
        }
        Log.d("cjzang", "SQLiteContentProvider---------->begin to insert------2");
        Uri result2 = insertInTransaction(uri, values, callerIsSyncAdapter);
        Log.d("cjzang", "SQLiteContentProvider---------->end to insert------2");
        return result2;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int numValues = values.length;
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        this.mDb = this.mOpenHelper.getWritableDatabase();
        this.mDb.beginTransaction();
        for (ContentValues contentValues : values) {
            try {
                insertInTransaction(uri, contentValues, callerIsSyncAdapter);
                this.mDb.yieldIfContendedSafely();
            } catch (Throwable th) {
                this.mDb.endTransaction();
                throw th;
            }
        }
        this.mDb.setTransactionSuccessful();
        this.mDb.endTransaction();
        onEndTransaction(callerIsSyncAdapter);
        return numValues;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransaction();
            try {
                int count = updateInTransaction(uri, values, selection, selectionArgs, callerIsSyncAdapter);
                this.mDb.setTransactionSuccessful();
                this.mDb.endTransaction();
                onEndTransaction(callerIsSyncAdapter);
                return count;
            } catch (Throwable th) {
                this.mDb.endTransaction();
                throw th;
            }
        }
        int count2 = updateInTransaction(uri, values, selection, selectionArgs, callerIsSyncAdapter);
        return count2;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransaction();
            try {
                int count = deleteInTransaction(uri, selection, selectionArgs, callerIsSyncAdapter);
                this.mDb.setTransactionSuccessful();
                this.mDb.endTransaction();
                onEndTransaction(callerIsSyncAdapter);
                return count;
            } catch (Throwable th) {
                this.mDb.endTransaction();
                throw th;
            }
        }
        int count2 = deleteInTransaction(uri, selection, selectionArgs, callerIsSyncAdapter);
        return count2;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        int ypCount = 0;
        int opCount = 0;
        boolean callerIsSyncAdapter = false;
        this.mDb = this.mOpenHelper.getWritableDatabase();
        this.mDb.beginTransaction();
        try {
            this.mApplyingBatch.set(true);
            int numOperations = operations.size();
            ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                opCount++;
                if (opCount >= 500) {
                    throw new OperationApplicationException("Too many content provider operations between yield points. The maximum number of operations per yield point is 500", ypCount);
                }
                ContentProviderOperation operation = operations.get(i);
                if (!callerIsSyncAdapter && isCallerSyncAdapter(operation.getUri())) {
                    callerIsSyncAdapter = true;
                }
                if (i > 0 && operation.isYieldAllowed()) {
                    opCount = 0;
                    if (this.mDb.yieldIfContendedSafely(4000L)) {
                        ypCount++;
                    }
                }
                results[i] = operation.apply(this, results, i);
            }
            this.mDb.setTransactionSuccessful();
            return results;
        } finally {
            this.mApplyingBatch.set(false);
            this.mDb.endTransaction();
            onEndTransaction(false);
        }
    }

    protected void onEndTransaction(boolean callerIsSyncAdapter) {
        Set<Uri> changed;
        Log.d("cjzang", "SQLiteContentProvider---------->onEndTransaction");
        synchronized (this.mChangedUris) {
            changed = new HashSet<>(this.mChangedUris);
            this.mChangedUris.clear();
        }
        ContentResolver resolver = getContext().getContentResolver();
        for (Uri uri : changed) {
            Log.d("cjzang", "SQLiteContentProvider---------->onEndTransaction-----uri =" + uri.toString());
            boolean syncToNetwork = !callerIsSyncAdapter && syncToNetwork(uri);
            resolver.notifyChange(uri, (ContentObserver) null, syncToNetwork);
        }
    }

    protected boolean syncToNetwork(Uri uri) {
        return false;
    }
}
