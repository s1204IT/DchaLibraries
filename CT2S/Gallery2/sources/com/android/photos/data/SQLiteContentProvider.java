package com.android.photos.data;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class SQLiteContentProvider extends ContentProvider {
    private final ThreadLocal<Boolean> mApplyingBatch = new ThreadLocal<>();
    private Set<Uri> mChangedUris;
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

    @Override
    public void shutdown() {
        getDatabaseHelper().close();
    }

    protected void postNotifyUri(Uri uri) {
        synchronized (this.mChangedUris) {
            this.mChangedUris.add(uri);
        }
    }

    public boolean isCallerSyncAdapter(Uri uri) {
        return false;
    }

    public SQLiteOpenHelper getDatabaseHelper() {
        return this.mOpenHelper;
    }

    private boolean applyingBatch() {
        return this.mApplyingBatch.get() != null && this.mApplyingBatch.get().booleanValue();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                Uri result = insertInTransaction(uri, values, callerIsSyncAdapter);
                db.setTransactionSuccessful();
                db.endTransaction();
                onEndTransaction(callerIsSyncAdapter);
                return result;
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
        Uri result2 = insertInTransaction(uri, values, callerIsSyncAdapter);
        return result2;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int numValues = values.length;
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        for (ContentValues contentValues : values) {
            try {
                insertInTransaction(uri, contentValues, callerIsSyncAdapter);
                db.yieldIfContendedSafely();
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        onEndTransaction(callerIsSyncAdapter);
        return numValues;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                int count = updateInTransaction(uri, values, selection, selectionArgs, callerIsSyncAdapter);
                db.setTransactionSuccessful();
                db.endTransaction();
                onEndTransaction(callerIsSyncAdapter);
                return count;
            } catch (Throwable th) {
                db.endTransaction();
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
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                int count = deleteInTransaction(uri, selection, selectionArgs, callerIsSyncAdapter);
                db.setTransactionSuccessful();
                db.endTransaction();
                onEndTransaction(callerIsSyncAdapter);
                return count;
            } catch (Throwable th) {
                db.endTransaction();
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
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
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
                    if (db.yieldIfContendedSafely(4000L)) {
                        ypCount++;
                    }
                }
                results[i] = operation.apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            this.mApplyingBatch.set(false);
            db.endTransaction();
            onEndTransaction(false);
        }
    }

    protected Set<Uri> onEndTransaction(boolean callerIsSyncAdapter) {
        Set<Uri> changed;
        synchronized (this.mChangedUris) {
            changed = new HashSet<>(this.mChangedUris);
            this.mChangedUris.clear();
        }
        ContentResolver resolver = getContext().getContentResolver();
        for (Uri uri : changed) {
            boolean syncToNetwork = !callerIsSyncAdapter && syncToNetwork(uri);
            notifyChange(resolver, uri, syncToNetwork);
        }
        return changed;
    }

    protected void notifyChange(ContentResolver resolver, Uri uri, boolean syncToNetwork) {
        resolver.notifyChange(uri, (ContentObserver) null, syncToNetwork);
    }

    protected boolean syncToNetwork(Uri uri) {
        return false;
    }
}
