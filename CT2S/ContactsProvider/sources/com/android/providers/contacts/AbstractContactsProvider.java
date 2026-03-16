package com.android.providers.contacts;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;
import android.util.Log;
import java.util.ArrayList;

public abstract class AbstractContactsProvider extends ContentProvider implements SQLiteTransactionListener {
    public static final boolean VERBOSE_LOGGING = Log.isLoggable("ContactsProvider", 2);
    private SQLiteOpenHelper mDbHelper;
    private String mSerializeDbTag;
    private SQLiteOpenHelper mSerializeOnDbHelper;
    private SQLiteTransactionListener mSerializedDbTransactionListener;
    private ThreadLocal<ContactsTransaction> mTransactionHolder;

    protected abstract int deleteInTransaction(Uri uri, String str, String[] strArr);

    protected abstract SQLiteOpenHelper getDatabaseHelper(Context context);

    protected abstract ThreadLocal<ContactsTransaction> getTransactionHolder();

    protected abstract Uri insertInTransaction(Uri uri, ContentValues contentValues);

    protected abstract void notifyChange();

    protected abstract int updateInTransaction(Uri uri, ContentValues contentValues, String str, String[] strArr);

    protected abstract boolean yield(ContactsTransaction contactsTransaction);

    @Override
    public boolean onCreate() {
        Context context = getContext();
        this.mDbHelper = getDatabaseHelper(context);
        this.mTransactionHolder = getTransactionHolder();
        return true;
    }

    public SQLiteOpenHelper getDatabaseHelper() {
        return this.mDbHelper;
    }

    public void setDbHelperToSerializeOn(SQLiteOpenHelper serializeOnDbHelper, String tag, SQLiteTransactionListener listener) {
        this.mSerializeOnDbHelper = serializeOnDbHelper;
        this.mSerializeDbTag = tag;
        this.mSerializedDbTransactionListener = listener;
    }

    public ContactsTransaction getCurrentTransaction() {
        return this.mTransactionHolder.get();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        ContactsTransaction transaction = startTransaction(false);
        try {
            Uri result = insertInTransaction(uri, values);
            if (result != null) {
                transaction.markDirty();
            }
            transaction.markSuccessful(false);
            return result;
        } finally {
            endTransaction(false);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        ContactsTransaction transaction = startTransaction(false);
        try {
            int deleted = deleteInTransaction(uri, selection, selectionArgs);
            if (deleted > 0) {
                transaction.markDirty();
            }
            transaction.markSuccessful(false);
            return deleted;
        } finally {
            endTransaction(false);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        ContactsTransaction transaction = startTransaction(false);
        try {
            int updated = updateInTransaction(uri, values, selection, selectionArgs);
            if (updated > 0) {
                transaction.markDirty();
            }
            transaction.markSuccessful(false);
            return updated;
        } finally {
            endTransaction(false);
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        ContactsTransaction transaction = startTransaction(true);
        int numValues = values.length;
        int opCount = 0;
        for (ContentValues contentValues : values) {
            try {
                insert(uri, contentValues);
                opCount++;
                if (opCount >= 50) {
                    opCount = 0;
                    try {
                        yield(transaction);
                    } catch (RuntimeException re) {
                        transaction.markYieldFailed();
                        throw re;
                    }
                }
            } finally {
                endTransaction(true);
            }
        }
        transaction.markSuccessful(true);
        return numValues;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "applyBatch: " + operations.size() + " ops");
        }
        int ypCount = 0;
        int opCount = 0;
        ContactsTransaction transaction = startTransaction(true);
        try {
            int numOperations = operations.size();
            ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                opCount++;
                if (opCount >= 500) {
                    throw new OperationApplicationException("Too many content provider operations between yield points. The maximum number of operations per yield point is 500", ypCount);
                }
                ContentProviderOperation operation = operations.get(i);
                if (i > 0 && operation.isYieldAllowed()) {
                    if (VERBOSE_LOGGING) {
                        Log.v("ContactsProvider", "applyBatch: " + opCount + " ops finished; about to yield...");
                    }
                    opCount = 0;
                    try {
                        if (yield(transaction)) {
                            ypCount++;
                        }
                    } catch (RuntimeException re) {
                        transaction.markYieldFailed();
                        throw re;
                    }
                }
                results[i] = operation.apply(this, results, i);
            }
            transaction.markSuccessful(true);
            return results;
        } finally {
            endTransaction(true);
        }
    }

    private ContactsTransaction startTransaction(boolean callerIsBatch) {
        ContactsTransaction transaction = this.mTransactionHolder.get();
        if (transaction == null) {
            transaction = new ContactsTransaction(callerIsBatch);
            if (this.mSerializeOnDbHelper != null) {
                transaction.startTransactionForDb(this.mSerializeOnDbHelper.getWritableDatabase(), this.mSerializeDbTag, this.mSerializedDbTransactionListener);
            }
            this.mTransactionHolder.set(transaction);
        }
        return transaction;
    }

    private void endTransaction(boolean callerIsBatch) {
        ContactsTransaction transaction = this.mTransactionHolder.get();
        if (transaction != null) {
            if (!transaction.isBatch() || callerIsBatch) {
                try {
                    if (transaction.isDirty()) {
                        notifyChange();
                    }
                    transaction.finish(callerIsBatch);
                } finally {
                    this.mTransactionHolder.set(null);
                }
            }
        }
    }
}
