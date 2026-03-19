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
import android.util.Log;
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
        this.mOpenHelper = getDatabaseHelper(getContext());
        return true;
    }

    protected SQLiteOpenHelper getDatabaseHelper() {
        return this.mOpenHelper;
    }

    private boolean applyingBatch() {
        return this.mApplyingBatch.get() != null && this.mApplyingBatch.get().booleanValue();
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Uri uriInsertInTransaction;
        boolean zApplyingBatch = applyingBatch();
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(uri);
        if (!zApplyingBatch) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransactionWithListener(this);
            long jClearCallingIdentityInternal = clearCallingIdentityInternal();
            try {
                uriInsertInTransaction = insertInTransaction(uri, contentValues, isCallerSyncAdapter);
                if (uriInsertInTransaction != null) {
                    this.mNotifyChange = true;
                }
                this.mDb.setTransactionSuccessful();
                onEndTransaction(!isCallerSyncAdapter && shouldSyncFor(uri));
            } finally {
                restoreCallingIdentityInternal(jClearCallingIdentityInternal);
                this.mDb.endTransaction();
            }
        } else {
            uriInsertInTransaction = insertInTransaction(uri, contentValues, isCallerSyncAdapter);
            if (uriInsertInTransaction != null) {
                this.mNotifyChange = true;
            }
        }
        return uriInsertInTransaction;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        int length = contentValuesArr.length;
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(uri);
        this.mDb = this.mOpenHelper.getWritableDatabase();
        this.mDb.beginTransactionWithListener(this);
        long jClearCallingIdentityInternal = clearCallingIdentityInternal();
        for (ContentValues contentValues : contentValuesArr) {
            try {
                if (insertInTransaction(uri, contentValues, isCallerSyncAdapter) != null) {
                    this.mNotifyChange = true;
                }
                this.mDb.yieldIfContendedSafely();
            } catch (Throwable th) {
                restoreCallingIdentityInternal(jClearCallingIdentityInternal);
                this.mDb.endTransaction();
                throw th;
            }
        }
        this.mDb.setTransactionSuccessful();
        restoreCallingIdentityInternal(jClearCallingIdentityInternal);
        this.mDb.endTransaction();
        onEndTransaction(!isCallerSyncAdapter);
        return length;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int iUpdateInTransaction;
        boolean zApplyingBatch = applyingBatch();
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(uri);
        if (!zApplyingBatch) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransactionWithListener(this);
            long jClearCallingIdentityInternal = clearCallingIdentityInternal();
            try {
                iUpdateInTransaction = updateInTransaction(uri, contentValues, str, strArr, isCallerSyncAdapter);
                if (iUpdateInTransaction > 0) {
                    this.mNotifyChange = true;
                }
                this.mDb.setTransactionSuccessful();
                onEndTransaction(!isCallerSyncAdapter && shouldSyncFor(uri));
            } finally {
                restoreCallingIdentityInternal(jClearCallingIdentityInternal);
                this.mDb.endTransaction();
            }
        } else {
            iUpdateInTransaction = updateInTransaction(uri, contentValues, str, strArr, isCallerSyncAdapter);
            if (iUpdateInTransaction > 0) {
                this.mNotifyChange = true;
            }
        }
        return iUpdateInTransaction;
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int iDeleteInTransaction;
        boolean zApplyingBatch = applyingBatch();
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(uri);
        if (!zApplyingBatch) {
            this.mDb = this.mOpenHelper.getWritableDatabase();
            this.mDb.beginTransactionWithListener(this);
            long jClearCallingIdentityInternal = clearCallingIdentityInternal();
            try {
                iDeleteInTransaction = deleteInTransaction(uri, str, strArr, isCallerSyncAdapter);
                if (iDeleteInTransaction > 0) {
                    this.mNotifyChange = true;
                }
                this.mDb.setTransactionSuccessful();
                onEndTransaction(!isCallerSyncAdapter && shouldSyncFor(uri));
            } finally {
                restoreCallingIdentityInternal(jClearCallingIdentityInternal);
                this.mDb.endTransaction();
            }
        } else {
            iDeleteInTransaction = deleteInTransaction(uri, str, strArr, isCallerSyncAdapter);
            if (iDeleteInTransaction > 0) {
                this.mNotifyChange = true;
            }
        }
        return iDeleteInTransaction;
    }

    protected boolean getIsCallerSyncAdapter(Uri uri) {
        boolean booleanQueryParameter = QueryParameterUtils.readBooleanQueryParameter(uri, "caller_is_syncadapter", false);
        if (this.mIsCallerSyncAdapter == null || this.mIsCallerSyncAdapter.booleanValue()) {
            this.mIsCallerSyncAdapter = Boolean.valueOf(booleanQueryParameter);
        }
        return booleanQueryParameter;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        int size = arrayList.size();
        if (size == 0) {
            return new ContentProviderResult[0];
        }
        this.mDb = this.mOpenHelper.getWritableDatabase();
        this.mDb.beginTransactionWithListener(this);
        boolean isCallerSyncAdapter = getIsCallerSyncAdapter(arrayList.get(0).getUri());
        long jClearCallingIdentityInternal = clearCallingIdentityInternal();
        try {
            this.mApplyingBatch.set(true);
            ContentProviderResult[] contentProviderResultArr = new ContentProviderResult[size];
            for (int i = 0; i < size; i++) {
                ContentProviderOperation contentProviderOperation = arrayList.get(i);
                if (i > 0 && contentProviderOperation.isYieldAllowed()) {
                    this.mDb.yieldIfContendedSafely(4000L);
                }
                contentProviderResultArr[i] = contentProviderOperation.apply(this, contentProviderResultArr, i);
            }
            this.mDb.setTransactionSuccessful();
            return contentProviderResultArr;
        } finally {
            this.mApplyingBatch.set(false);
            this.mDb.endTransaction();
            onEndTransaction(!isCallerSyncAdapter);
            restoreCallingIdentityInternal(jClearCallingIdentityInternal);
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

    protected void onEndTransaction(boolean z) {
        if (this.mNotifyChange) {
            this.mNotifyChange = false;
            notifyChange(z);
        }
    }

    protected String getCachedCallingPackage() {
        return this.mCallingPackage.get();
    }

    protected long clearCallingIdentityInternal() {
        int iMyUid = Process.myUid();
        int callingUid = Binder.getCallingUid();
        if (iMyUid != callingUid) {
            try {
                this.mOriginalCallingUid.set(Integer.valueOf(callingUid));
                this.mCallingPackage.set(getCallingPackage());
            } catch (SecurityException e) {
                Log.e("SQLiteContentProvider", "Error getting the calling package.", e);
            }
        }
        return Binder.clearCallingIdentity();
    }

    protected void restoreCallingIdentityInternal(long j) {
        Binder.restoreCallingIdentity(j);
        int callingUid = Binder.getCallingUid();
        if (this.mOriginalCallingUid.get() != null && this.mOriginalCallingUid.get().intValue() == callingUid) {
            this.mCallingPackage.set(null);
            this.mOriginalCallingUid.set(null);
        }
    }

    SQLiteDatabase getWritableDatabase() {
        if (this.mOpenHelper != null) {
            return this.mOpenHelper.getWritableDatabase();
        }
        return null;
    }
}
