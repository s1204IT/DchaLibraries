package com.android.providers.calendar;

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
import android.os.Binder;
import android.os.Process;
import java.util.ArrayList;

public abstract class SQLiteContentProvider extends ContentProvider implements SQLiteTransactionListener {
    protected SQLiteDatabase mDb;
    private Boolean mIsCallerSyncAdapter;
    private volatile boolean mNotifyChange;
    private SQLiteOpenHelper mOpenHelper;
    private final ThreadLocal<Boolean> mApplyingBatch = new ThreadLocal<>();
    private final ThreadLocal<String> mCallingPackage = new ThreadLocal<>();
    private final ThreadLocal<Integer> mOriginalCallingUid = new ThreadLocal<>();

    protected abstract int deleteInTransaction(Uri uri, String str, String[] strArr, boolean z);

    protected abstract SQLiteOpenHelper getDatabaseHelper(Context context);

    protected abstract Uri insertInTransaction(Uri uri, ContentValues contentValues, boolean z);

    protected abstract void notifyChange(boolean z);

    protected abstract boolean shouldSyncFor(Uri uri);

    protected abstract int updateInTransaction(Uri uri, ContentValues contentValues, String str, String[] strArr, boolean z);

    @Override
    public boolean onCreate() {
        Context context = getContext();
        this.mOpenHelper = getDatabaseHelper(context);
        return true;
    }

    protected SQLiteOpenHelper getDatabaseHelper() {
        return this.mOpenHelper;
    }

    private boolean applyingBatch() {
        return this.mApplyingBatch.get() != null && this.mApplyingBatch.get().booleanValue();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri result;
        boolean applyingBatch = applyingBatch();
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(uri);
        if (!applyingBatch) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransactionWithListener(this);
            long identity = clearCallingIdentityInternal();
            try {
                result = insertInTransaction(uri, values, isCallerSyncAdapter);
                if (result != null) {
                    this.mNotifyChange = true;
                }
                this.mDb.setTransactionSuccessful();
                onEndTransaction(!isCallerSyncAdapter && shouldSyncFor(uri));
            } finally {
                restoreCallingIdentityInternal(identity);
                this.mDb.endTransaction();
            }
        } else {
            result = insertInTransaction(uri, values, isCallerSyncAdapter);
            if (result != null) {
                this.mNotifyChange = true;
            }
        }
        return result;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int numValues = values.length;
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(uri);
        this.mDb = this.mOpenHelper.getWritableDatabase();
        this.mDb.beginTransactionWithListener(this);
        long identity = clearCallingIdentityInternal();
        for (ContentValues contentValues : values) {
            try {
                Uri result = insertInTransaction(uri, contentValues, isCallerSyncAdapter);
                if (result != null) {
                    this.mNotifyChange = true;
                }
                this.mDb.yieldIfContendedSafely();
            } finally {
                restoreCallingIdentityInternal(identity);
                this.mDb.endTransaction();
            }
        }
        this.mDb.setTransactionSuccessful();
        onEndTransaction(isCallerSyncAdapter ? false : true);
        return numValues;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count;
        boolean applyingBatch = applyingBatch();
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(uri);
        if (!applyingBatch) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransactionWithListener(this);
            long identity = clearCallingIdentityInternal();
            try {
                count = updateInTransaction(uri, values, selection, selectionArgs, isCallerSyncAdapter);
                if (count > 0) {
                    this.mNotifyChange = true;
                }
                this.mDb.setTransactionSuccessful();
                onEndTransaction(!isCallerSyncAdapter && shouldSyncFor(uri));
            } finally {
                restoreCallingIdentityInternal(identity);
                this.mDb.endTransaction();
            }
        } else {
            count = updateInTransaction(uri, values, selection, selectionArgs, isCallerSyncAdapter);
            if (count > 0) {
                this.mNotifyChange = true;
            }
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count;
        boolean applyingBatch = applyingBatch();
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(uri);
        if (!applyingBatch) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransactionWithListener(this);
            long identity = clearCallingIdentityInternal();
            try {
                count = deleteInTransaction(uri, selection, selectionArgs, isCallerSyncAdapter);
                if (count > 0) {
                    this.mNotifyChange = true;
                }
                this.mDb.setTransactionSuccessful();
                onEndTransaction(!isCallerSyncAdapter && shouldSyncFor(uri));
            } finally {
                restoreCallingIdentityInternal(identity);
                this.mDb.endTransaction();
            }
        } else {
            count = deleteInTransaction(uri, selection, selectionArgs, isCallerSyncAdapter);
            if (count > 0) {
                this.mNotifyChange = true;
            }
        }
        return count;
    }

    protected boolean getIsCallerSyncAdapter(Uri uri) {
        boolean isCurrentSyncAdapter = QueryParameterUtils.readBooleanQueryParameter(uri, "caller_is_syncadapter", false);
        if (this.mIsCallerSyncAdapter == null || this.mIsCallerSyncAdapter.booleanValue()) {
            this.mIsCallerSyncAdapter = Boolean.valueOf(isCurrentSyncAdapter);
        }
        return isCurrentSyncAdapter;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        int numOperations = operations.size();
        if (numOperations == 0) {
            return new ContentProviderResult[0];
        }
        this.mDb = this.mOpenHelper.getWritableDatabase();
        this.mDb.beginTransactionWithListener(this);
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(operations.get(0).getUri());
        long identity = clearCallingIdentityInternal();
        try {
            this.mApplyingBatch.set(true);
            ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                ContentProviderOperation operation = operations.get(i);
                if (i > 0 && operation.isYieldAllowed()) {
                    this.mDb.yieldIfContendedSafely(4000L);
                }
                results[i] = operation.apply(this, results, i);
            }
            this.mDb.setTransactionSuccessful();
            this.mApplyingBatch.set(false);
            this.mDb.endTransaction();
            onEndTransaction(!isCallerSyncAdapter);
            restoreCallingIdentityInternal(identity);
            return results;
        } catch (Throwable th) {
            this.mApplyingBatch.set(false);
            this.mDb.endTransaction();
            onEndTransaction(isCallerSyncAdapter ? false : true);
            restoreCallingIdentityInternal(identity);
            throw th;
        }
    }

    @Override
    public void onBegin() {
        this.mIsCallerSyncAdapter = null;
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

    protected void onEndTransaction(boolean syncToNetwork) {
        if (this.mNotifyChange) {
            this.mNotifyChange = false;
            notifyChange(syncToNetwork);
        }
    }

    protected String getCachedCallingPackage() {
        return this.mCallingPackage.get();
    }

    protected long clearCallingIdentityInternal() {
        int uid = Process.myUid();
        int callingUid = Binder.getCallingUid();
        if (uid != callingUid) {
            try {
                this.mOriginalCallingUid.set(Integer.valueOf(callingUid));
                String callingPackage = getCallingPackage();
                this.mCallingPackage.set(callingPackage);
            } catch (SecurityException e) {
            }
        }
        return Binder.clearCallingIdentity();
    }

    protected void restoreCallingIdentityInternal(long identity) {
        Binder.restoreCallingIdentity(identity);
        int callingUid = Binder.getCallingUid();
        if (this.mOriginalCallingUid.get() != null && this.mOriginalCallingUid.get().intValue() == callingUid) {
            this.mCallingPackage.set(null);
            this.mOriginalCallingUid.set(null);
        }
    }
}
