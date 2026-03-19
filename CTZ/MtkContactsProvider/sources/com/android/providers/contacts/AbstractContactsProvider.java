package com.android.providers.contacts;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteTransactionListener;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import com.android.internal.util.ProviderAccessStats;
import java.io.PrintWriter;
import java.util.ArrayList;

public abstract class AbstractContactsProvider extends ContentProvider implements SQLiteTransactionListener {
    public static final boolean VERBOSE_LOGGING = Log.isLoggable("ContactsProvider", 2);
    private ContactsDatabaseHelper mDbHelper;
    private String mSerializeDbTag;
    private SQLiteOpenHelper mSerializeOnDbHelper;
    private SQLiteTransactionListener mSerializedDbTransactionListener;
    protected final ProviderAccessStats mStats = new ProviderAccessStats();
    private ThreadLocal<ContactsTransaction> mTransactionHolder;

    protected abstract int deleteInTransaction(Uri uri, String str, String[] strArr);

    protected abstract ThreadLocal<ContactsTransaction> getTransactionHolder();

    protected abstract Uri insertInTransaction(Uri uri, ContentValues contentValues);

    protected abstract ContactsDatabaseHelper newDatabaseHelper(Context context);

    protected abstract void notifyChange();

    protected abstract int updateInTransaction(Uri uri, ContentValues contentValues, String str, String[] strArr);

    protected abstract boolean yield(ContactsTransaction contactsTransaction);

    @Override
    public boolean onCreate() {
        this.mDbHelper = newDatabaseHelper(getContext());
        this.mTransactionHolder = getTransactionHolder();
        return true;
    }

    public ContactsDatabaseHelper getDatabaseHelper() {
        return this.mDbHelper;
    }

    public void setDbHelperToSerializeOn(SQLiteOpenHelper sQLiteOpenHelper, String str, SQLiteTransactionListener sQLiteTransactionListener) {
        this.mSerializeOnDbHelper = sQLiteOpenHelper;
        this.mSerializeDbTag = str;
        this.mSerializedDbTransactionListener = sQLiteTransactionListener;
    }

    public ContactsTransaction getCurrentTransaction() {
        return this.mTransactionHolder.get();
    }

    private boolean isInBatch() {
        ContactsTransaction contactsTransaction = this.mTransactionHolder.get();
        return contactsTransaction != null && contactsTransaction.isBatch();
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int callingUid = Binder.getCallingUid();
        this.mStats.incrementInsertStats(callingUid, isInBatch());
        try {
            ContactsTransaction contactsTransactionStartTransaction = startTransaction(false);
            try {
                Uri uriInsertInTransaction = insertInTransaction(uri, contentValues);
                if (uriInsertInTransaction != null) {
                    contactsTransactionStartTransaction.markDirty();
                }
                contactsTransactionStartTransaction.markSuccessful(false);
                return uriInsertInTransaction;
            } finally {
                endTransaction(false);
            }
        } finally {
            this.mStats.finishOperation(callingUid);
        }
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        int callingUid = Binder.getCallingUid();
        this.mStats.incrementDeleteStats(callingUid, isInBatch());
        try {
            ContactsTransaction contactsTransactionStartTransaction = startTransaction(false);
            try {
                try {
                    int iDeleteInTransaction = deleteInTransaction(uri, str, strArr);
                    if (iDeleteInTransaction > 0) {
                        contactsTransactionStartTransaction.markDirty();
                    }
                    contactsTransactionStartTransaction.markSuccessful(false);
                    return iDeleteInTransaction;
                } catch (SQLiteFullException e) {
                    Log.e("ContactsProvider", "[delete]catch SQLiteFullException for delete");
                    try {
                        endTransaction(false);
                    } catch (SQLiteCantOpenDatabaseException e2) {
                        Log.e("ContactsProvider", "[delete]catch SQLiteCantOpenDatabaseException for endTransaction");
                    }
                    return 0;
                }
            } finally {
                try {
                    endTransaction(false);
                } catch (SQLiteCantOpenDatabaseException e3) {
                    Log.e("ContactsProvider", "[delete]catch SQLiteCantOpenDatabaseException for endTransaction");
                }
            }
        } finally {
            this.mStats.finishOperation(callingUid);
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int callingUid = Binder.getCallingUid();
        this.mStats.incrementUpdateStats(callingUid, isInBatch());
        try {
            ContactsTransaction contactsTransactionStartTransaction = startTransaction(false);
            try {
                int iUpdateInTransaction = updateInTransaction(uri, contentValues, str, strArr);
                if (iUpdateInTransaction > 0) {
                    contactsTransactionStartTransaction.markDirty();
                }
                contactsTransactionStartTransaction.markSuccessful(false);
                return iUpdateInTransaction;
            } finally {
                endTransaction(false);
            }
        } finally {
            this.mStats.finishOperation(callingUid);
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        int callingUid = Binder.getCallingUid();
        this.mStats.incrementBatchStats(callingUid);
        try {
            ContactsTransaction contactsTransactionStartTransaction = startTransaction(true);
            int length = contentValuesArr.length;
            int i = 0;
            for (ContentValues contentValues : contentValuesArr) {
                try {
                    insert(uri, contentValues);
                    i++;
                    if (i >= 50) {
                        try {
                            yield(contactsTransactionStartTransaction);
                            i = 0;
                        } catch (RuntimeException e) {
                            contactsTransactionStartTransaction.markYieldFailed();
                            throw e;
                        }
                    }
                } catch (Throwable th) {
                    endTransaction(true);
                    throw th;
                }
            }
            contactsTransactionStartTransaction.markSuccessful(true);
            endTransaction(true);
            return length;
        } finally {
            this.mStats.finishOperation(callingUid);
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        int callingUid = Binder.getCallingUid();
        this.mStats.incrementBatchStats(callingUid);
        try {
            if (VERBOSE_LOGGING) {
                Log.v("ContactsProvider", "applyBatch: " + arrayList.size() + " ops");
            }
            ContactsTransaction contactsTransactionStartTransaction = startTransaction(true);
            try {
                int size = arrayList.size();
                ContentProviderResult[] contentProviderResultArr = new ContentProviderResult[size];
                int i = 0;
                int i2 = 0;
                for (int i3 = 0; i3 < size; i3++) {
                    i++;
                    if (i >= 500) {
                        throw new OperationApplicationException("Too many content provider operations between yield points. The maximum number of operations per yield point is 500", i2);
                    }
                    ContentProviderOperation contentProviderOperation = arrayList.get(i3);
                    if (i3 > 0 && contentProviderOperation.isYieldAllowed()) {
                        if (VERBOSE_LOGGING) {
                            Log.v("ContactsProvider", "applyBatch: " + i + " ops finished; about to yield...");
                        }
                        try {
                            if (yield(contactsTransactionStartTransaction)) {
                                i2++;
                            }
                            i = 0;
                        } catch (RuntimeException e) {
                            contactsTransactionStartTransaction.markYieldFailed();
                            throw e;
                        }
                    }
                    contentProviderResultArr[i3] = contentProviderOperation.apply(this, contentProviderResultArr, i3);
                }
                contactsTransactionStartTransaction.markSuccessful(true);
                return contentProviderResultArr;
            } finally {
                endTransaction(true);
            }
        } finally {
            this.mStats.finishOperation(callingUid);
        }
    }

    private ContactsTransaction startTransaction(boolean z) {
        ContactsTransaction contactsTransaction = this.mTransactionHolder.get();
        if (contactsTransaction == null) {
            contactsTransaction = new ContactsTransaction(z);
            if (this.mSerializeOnDbHelper != null) {
                contactsTransaction.startTransactionForDb(this.mSerializeOnDbHelper.getWritableDatabase(), this.mSerializeDbTag, this.mSerializedDbTransactionListener);
            }
            this.mTransactionHolder.set(contactsTransaction);
        }
        return contactsTransaction;
    }

    private void endTransaction(boolean z) {
        ContactsTransaction contactsTransaction = this.mTransactionHolder.get();
        if (contactsTransaction != null) {
            if (!contactsTransaction.isBatch() || z) {
                boolean z2 = false;
                try {
                    if (contactsTransaction.isDirty()) {
                        z2 = true;
                    }
                    contactsTransaction.finish(z);
                    if (z2) {
                        notifyChange();
                    }
                } finally {
                    this.mTransactionHolder.set(null);
                }
            }
        }
    }

    protected void dump(PrintWriter printWriter, String str) {
        printWriter.print("Database: ");
        printWriter.println(str);
        this.mStats.dump(printWriter, "  ");
        if (this.mDbHelper == null) {
            printWriter.println("mDbHelper is null");
            return;
        }
        try {
            printWriter.println();
            printWriter.println("  Accounts:");
            SQLiteDatabase readableDatabase = this.mDbHelper.getReadableDatabase();
            Throwable th = null;
            Cursor cursorRawQuery = readableDatabase.rawQuery("SELECT * FROM accounts ORDER BY _id", null);
            try {
                try {
                    cursorRawQuery.moveToPosition(-1);
                    while (cursorRawQuery.moveToNext()) {
                        printWriter.print("    ");
                        dumpLongColumn(printWriter, cursorRawQuery, "_id");
                        printWriter.print(" ");
                        dumpStringColumn(printWriter, cursorRawQuery, "account_name");
                        printWriter.print(" ");
                        dumpStringColumn(printWriter, cursorRawQuery, "account_type");
                        printWriter.print(" ");
                        dumpStringColumn(printWriter, cursorRawQuery, "data_set");
                        printWriter.println();
                    }
                    printWriter.println();
                    printWriter.println("  Contacts:");
                    printWriter.print("    # of visible: ");
                    printWriter.print(longForQuery(readableDatabase, "SELECT count(*) FROM default_directory"));
                    printWriter.println();
                    printWriter.print("    # of invisible: ");
                    printWriter.print(longForQuery(readableDatabase, "SELECT count(*) FROM contacts"));
                    printWriter.println();
                    printWriter.print("    Max # of raw contacts: ");
                    printWriter.print(longForQuery(readableDatabase, "SELECT max(c) FROM (SELECT _id, count(*) as c FROM raw_contacts GROUP BY contact_id)"));
                    printWriter.println();
                    printWriter.print("    Avg # of raw contacts: ");
                    printWriter.print(doubleForQuery(readableDatabase, "SELECT avg(c) FROM (SELECT _id, count(*) as c FROM raw_contacts GROUP BY contact_id)"));
                    printWriter.println();
                    printWriter.println();
                    printWriter.println("  Raw contacts (per account):");
                    Cursor cursorRawQuery2 = readableDatabase.rawQuery("SELECT aid, sum(c) AS s, max(c) AS m, avg(c) AS a FROM (SELECT account_id AS aid, contact_id AS cid, count(*) AS c FROM raw_contacts GROUP BY aid, cid) GROUP BY aid", null);
                    try {
                        cursorRawQuery2.moveToPosition(-1);
                        while (cursorRawQuery2.moveToNext()) {
                            printWriter.print("    ");
                            dumpLongColumn(printWriter, cursorRawQuery2, "aid");
                            printWriter.print(" total # of raw contacts: ");
                            dumpStringColumn(printWriter, cursorRawQuery2, "s");
                            printWriter.print(", max # per contact: ");
                            dumpLongColumn(printWriter, cursorRawQuery2, "m");
                            printWriter.print(", avg # per contact: ");
                            dumpDoubleColumn(printWriter, cursorRawQuery2, "a");
                            printWriter.println();
                        }
                        if (cursorRawQuery2 != null) {
                            $closeResource(null, cursorRawQuery2);
                        }
                        printWriter.println();
                        printWriter.println("  Data (per account):");
                        cursorRawQuery = readableDatabase.rawQuery("SELECT aid, sum(c) AS s, max(c) AS m, avg(c) AS a FROM (SELECT aid, rid, count(*) AS c FROM (SELECT d._id AS did, d.raw_contact_id AS rid, r.account_id AS aid FROM data AS d JOIN raw_contacts AS r ON d.raw_contact_id=r._id) GROUP BY aid, rid) GROUP BY aid", null);
                        try {
                            cursorRawQuery.moveToPosition(-1);
                            while (cursorRawQuery.moveToNext()) {
                                printWriter.print("    ");
                                dumpLongColumn(printWriter, cursorRawQuery, "aid");
                                printWriter.print(" total # of data:");
                                dumpLongColumn(printWriter, cursorRawQuery, "s");
                                printWriter.print(", max # per raw contact: ");
                                dumpLongColumn(printWriter, cursorRawQuery, "m");
                                printWriter.print(", avg # per raw contact: ");
                                dumpDoubleColumn(printWriter, cursorRawQuery, "a");
                                printWriter.println();
                            }
                            if (cursorRawQuery != null) {
                                $closeResource(null, cursorRawQuery);
                            }
                        } finally {
                            if (cursorRawQuery != null) {
                                $closeResource(null, cursorRawQuery);
                            }
                        }
                    } finally {
                        if (cursorRawQuery2 != null) {
                            $closeResource(null, cursorRawQuery2);
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } finally {
                if (cursorRawQuery != null) {
                    $closeResource(th, cursorRawQuery);
                }
            }
        } catch (Exception e) {
            printWriter.println("Error: " + e);
        }
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

    private static void dumpStringColumn(PrintWriter printWriter, Cursor cursor, String str) {
        int columnIndex = cursor.getColumnIndex(str);
        if (columnIndex == -1) {
            printWriter.println("Column not found: " + str);
            return;
        }
        String string = cursor.getString(columnIndex);
        if (string == null) {
            printWriter.print("(null)");
        } else if (string.length() == 0) {
            printWriter.print("\"\"");
        } else {
            printWriter.print(string);
        }
    }

    private static void dumpLongColumn(PrintWriter printWriter, Cursor cursor, String str) {
        int columnIndex = cursor.getColumnIndex(str);
        if (columnIndex == -1) {
            printWriter.println("Column not found: " + str);
            return;
        }
        if (cursor.isNull(columnIndex)) {
            printWriter.print("(null)");
        } else {
            printWriter.print(cursor.getLong(columnIndex));
        }
    }

    private static void dumpDoubleColumn(PrintWriter printWriter, Cursor cursor, String str) {
        int columnIndex = cursor.getColumnIndex(str);
        if (columnIndex == -1) {
            printWriter.println("Column not found: " + str);
            return;
        }
        if (cursor.isNull(columnIndex)) {
            printWriter.print("(null)");
        } else {
            printWriter.print(cursor.getDouble(columnIndex));
        }
    }

    private static long longForQuery(SQLiteDatabase sQLiteDatabase, String str) {
        return DatabaseUtils.longForQuery(sQLiteDatabase, str, null);
    }

    private static double doubleForQuery(SQLiteDatabase sQLiteDatabase, String str) throws Exception {
        Throwable th = null;
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery(str, null);
        try {
            try {
                if (!cursorRawQuery.moveToFirst()) {
                    return -1.0d;
                }
                double d = cursorRawQuery.getDouble(0);
                if (cursorRawQuery != null) {
                    $closeResource(null, cursorRawQuery);
                }
                return d;
            } finally {
            }
        } finally {
            if (cursorRawQuery != null) {
            }
        }
        if (cursorRawQuery != null) {
            $closeResource(th, cursorRawQuery);
        }
    }
}
