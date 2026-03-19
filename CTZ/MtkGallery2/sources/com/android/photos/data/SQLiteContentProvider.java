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
        this.mOpenHelper = getDatabaseHelper(getContext());
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
    public Uri insert(Uri uri, ContentValues contentValues) {
        boolean zIsCallerSyncAdapter = isCallerSyncAdapter(uri);
        if (!applyingBatch()) {
            SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
            writableDatabase.beginTransaction();
            try {
                Uri uriInsertInTransaction = insertInTransaction(uri, contentValues, zIsCallerSyncAdapter);
                writableDatabase.setTransactionSuccessful();
                writableDatabase.endTransaction();
                onEndTransaction(zIsCallerSyncAdapter);
                return uriInsertInTransaction;
            } catch (Throwable th) {
                writableDatabase.endTransaction();
                throw th;
            }
        }
        return insertInTransaction(uri, contentValues, zIsCallerSyncAdapter);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        int length = contentValuesArr.length;
        boolean zIsCallerSyncAdapter = isCallerSyncAdapter(uri);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        for (ContentValues contentValues : contentValuesArr) {
            try {
                insertInTransaction(uri, contentValues, zIsCallerSyncAdapter);
                writableDatabase.yieldIfContendedSafely();
            } catch (Throwable th) {
                writableDatabase.endTransaction();
                throw th;
            }
        }
        writableDatabase.setTransactionSuccessful();
        writableDatabase.endTransaction();
        onEndTransaction(zIsCallerSyncAdapter);
        return length;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        boolean zIsCallerSyncAdapter = isCallerSyncAdapter(uri);
        if (!applyingBatch()) {
            SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
            writableDatabase.beginTransaction();
            try {
                int iUpdateInTransaction = updateInTransaction(uri, contentValues, str, strArr, zIsCallerSyncAdapter);
                writableDatabase.setTransactionSuccessful();
                writableDatabase.endTransaction();
                onEndTransaction(zIsCallerSyncAdapter);
                return iUpdateInTransaction;
            } catch (Throwable th) {
                writableDatabase.endTransaction();
                throw th;
            }
        }
        return updateInTransaction(uri, contentValues, str, strArr, zIsCallerSyncAdapter);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        boolean zIsCallerSyncAdapter = isCallerSyncAdapter(uri);
        if (!applyingBatch()) {
            SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
            writableDatabase.beginTransaction();
            try {
                int iDeleteInTransaction = deleteInTransaction(uri, str, strArr, zIsCallerSyncAdapter);
                writableDatabase.setTransactionSuccessful();
                writableDatabase.endTransaction();
                onEndTransaction(zIsCallerSyncAdapter);
                return iDeleteInTransaction;
            } catch (Throwable th) {
                writableDatabase.endTransaction();
                throw th;
            }
        }
        return deleteInTransaction(uri, str, strArr, zIsCallerSyncAdapter);
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws Throwable {
        boolean z;
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            this.mApplyingBatch.set(true);
            int size = arrayList.size();
            ContentProviderResult[] contentProviderResultArr = new ContentProviderResult[size];
            int i = 0;
            z = false;
            int i2 = 0;
            for (int i3 = 0; i3 < size; i3++) {
                i++;
                if (i >= 500) {
                    throw new OperationApplicationException("Too many content provider operations between yield points. The maximum number of operations per yield point is 500", i2);
                }
                try {
                    ContentProviderOperation contentProviderOperation = arrayList.get(i3);
                    if (!z && isCallerSyncAdapter(contentProviderOperation.getUri())) {
                        z = true;
                    }
                    if (i3 > 0 && contentProviderOperation.isYieldAllowed()) {
                        if (writableDatabase.yieldIfContendedSafely(4000L)) {
                            i2++;
                        }
                        i = 0;
                    }
                    contentProviderResultArr[i3] = contentProviderOperation.apply(this, contentProviderResultArr, i3);
                } catch (Throwable th) {
                    th = th;
                    this.mApplyingBatch.set(false);
                    writableDatabase.endTransaction();
                    onEndTransaction(z);
                    throw th;
                }
            }
            writableDatabase.setTransactionSuccessful();
            this.mApplyingBatch.set(false);
            writableDatabase.endTransaction();
            onEndTransaction(z);
            return contentProviderResultArr;
        } catch (Throwable th2) {
            th = th2;
            z = false;
        }
    }

    protected Set<Uri> onEndTransaction(boolean z) {
        HashSet<Uri> hashSet;
        synchronized (this.mChangedUris) {
            hashSet = new HashSet(this.mChangedUris);
            this.mChangedUris.clear();
        }
        ContentResolver contentResolver = getContext().getContentResolver();
        for (Uri uri : hashSet) {
            notifyChange(contentResolver, uri, !z && syncToNetwork(uri));
        }
        return hashSet;
    }

    protected void notifyChange(ContentResolver contentResolver, Uri uri, boolean z) {
        contentResolver.notifyChange(uri, (ContentObserver) null, z);
    }

    protected boolean syncToNetwork(Uri uri) {
        return false;
    }
}
