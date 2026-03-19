package com.android.mtp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.mtp.MtpObjectInfo;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.io.FileNotFoundException;
import java.util.Set;

class Mapper {
    static final boolean $assertionsDisabled = false;
    private static final String[] EMPTY_ARGS = new String[0];
    private final MtpDatabase mDatabase;
    private final Set<String> mInMappingIds = new ArraySet();

    Mapper(MtpDatabase mtpDatabase) {
        this.mDatabase = mtpDatabase;
    }

    synchronized boolean putDeviceDocument(MtpDeviceRecord mtpDeviceRecord) throws FileNotFoundException {
        boolean zPutDocuments;
        SQLiteDatabase sQLiteDatabase = this.mDatabase.getSQLiteDatabase();
        sQLiteDatabase.beginTransaction();
        try {
            ContentValues[] contentValuesArr = {new ContentValues()};
            ContentValues[] contentValuesArr2 = {new ContentValues()};
            MtpDatabase.getDeviceDocumentValues(contentValuesArr[0], contentValuesArr2[0], mtpDeviceRecord);
            zPutDocuments = putDocuments(null, contentValuesArr, contentValuesArr2, "parent_document_id IS NULL", EMPTY_ARGS, MtpDatabase.strings("device_id", "mapping_key"));
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
        return zPutDocuments;
    }

    synchronized boolean putStorageDocuments(String str, int[] iArr, MtpRoot[] mtpRootArr) throws FileNotFoundException {
        boolean zPutDocuments;
        SQLiteDatabase sQLiteDatabase = this.mDatabase.getSQLiteDatabase();
        sQLiteDatabase.beginTransaction();
        try {
            ContentValues[] contentValuesArr = new ContentValues[mtpRootArr.length];
            ContentValues[] contentValuesArr2 = new ContentValues[mtpRootArr.length];
            for (int i = 0; i < mtpRootArr.length; i++) {
                contentValuesArr[i] = new ContentValues();
                contentValuesArr2[i] = new ContentValues();
                MtpDatabase.getStorageDocumentValues(contentValuesArr[i], contentValuesArr2[i], str, iArr, mtpRootArr[i]);
            }
            zPutDocuments = putDocuments(str, contentValuesArr, contentValuesArr2, "parent_document_id = ?", MtpDatabase.strings(str), MtpDatabase.strings("storage_id", "_display_name"));
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
        return zPutDocuments;
    }

    synchronized void putChildDocuments(int i, String str, int[] iArr, MtpObjectInfo[] mtpObjectInfoArr, long[] jArr) throws FileNotFoundException {
        ContentValues[] contentValuesArr = new ContentValues[mtpObjectInfoArr.length];
        for (int i2 = 0; i2 < mtpObjectInfoArr.length; i2++) {
            contentValuesArr[i2] = new ContentValues();
            MtpDatabase.getObjectDocumentValues(contentValuesArr[i2], i, str, iArr, mtpObjectInfoArr[i2], jArr[i2]);
        }
        putDocuments(str, contentValuesArr, null, "parent_document_id = ?", MtpDatabase.strings(str), MtpDatabase.strings("object_handle", "_display_name"));
    }

    void clearMapping() {
        SQLiteDatabase sQLiteDatabase = this.mDatabase.getSQLiteDatabase();
        sQLiteDatabase.beginTransaction();
        try {
            this.mInMappingIds.clear();
            try {
                startAddingDocuments(null);
                stopAddingDocuments(null);
                sQLiteDatabase.setTransactionSuccessful();
            } catch (FileNotFoundException e) {
                Log.e("MtpDocumentsProvider", "Unexpected FileNotFoundException.", e);
                throw new RuntimeException(e);
            }
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    void startAddingDocuments(String str) throws FileNotFoundException {
        String str2;
        String[] strArrStrings;
        if (str != null) {
            str2 = "parent_document_id = ?";
            strArrStrings = MtpDatabase.strings(str);
        } else {
            str2 = "parent_document_id IS NULL";
            strArrStrings = EMPTY_ARGS;
        }
        SQLiteDatabase sQLiteDatabase = this.mDatabase.getSQLiteDatabase();
        sQLiteDatabase.beginTransaction();
        try {
            getParentOrHaltMapping(str);
            Preconditions.checkState(!this.mInMappingIds.contains(str));
            ContentValues contentValues = new ContentValues();
            contentValues.put("row_state", (Integer) 1);
            sQLiteDatabase.update("Documents", contentValues, str2 + " AND row_state = ?", DatabaseUtils.appendSelectionArgs(strArrStrings, MtpDatabase.strings(0)));
            sQLiteDatabase.setTransactionSuccessful();
            this.mInMappingIds.add(str);
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    private boolean putDocuments(String str, ContentValues[] contentValuesArr, ContentValues[] contentValuesArr2, String str2, String[] strArr, String[] strArr2) throws FileNotFoundException {
        Throwable th;
        int i;
        long jInsert;
        Throwable th2;
        boolean z;
        ContentValues[] contentValuesArr3 = contentValuesArr;
        SQLiteDatabase sQLiteDatabase = this.mDatabase.getSQLiteDatabase();
        sQLiteDatabase.beginTransaction();
        try {
            getParentOrHaltMapping(str);
            Preconditions.checkState(this.mInMappingIds.contains(str));
            ContentValues contentValues = new ContentValues();
            ContentValues contentValues2 = new ContentValues();
            int i2 = 0;
            boolean z2 = false;
            while (i2 < contentValuesArr3.length) {
                ContentValues contentValues3 = contentValuesArr3[i2];
                Throwable th3 = null;
                ContentValues contentValues4 = contentValuesArr2 != null ? contentValuesArr2[i2] : null;
                Cursor cursorQueryCandidate = queryCandidate(str2, strArr, strArr2, contentValues3);
                if (cursorQueryCandidate == null) {
                    try {
                        try {
                            i = i2;
                            jInsert = sQLiteDatabase.insert("Documents", null, contentValues3);
                            z = true;
                            z2 = true;
                            contentValues3.put("document_id", Long.valueOf(jInsert));
                            if (contentValues4 == null) {
                                try {
                                    contentValues4.put("root_id", Long.valueOf(jInsert));
                                    th = null;
                                    try {
                                        sQLiteDatabase.replace("RootExtra", null, contentValues4);
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                    th = null;
                                }
                            }
                            if (z2) {
                                this.mDatabase.writeRowSnapshot(String.valueOf(jInsert), contentValues2);
                                contentValues.put("row_state", String.valueOf(0));
                                if (!contentValues.equals(contentValues2)) {
                                    z2 = z;
                                }
                            }
                            if (cursorQueryCandidate == null) {
                                cursorQueryCandidate.close();
                            }
                            i2 = i + 1;
                            contentValuesArr3 = contentValuesArr;
                        } catch (Throwable th6) {
                            th2 = th6;
                            th = th3;
                        }
                    } catch (Throwable th7) {
                        th3 = th7;
                        throw th3;
                    }
                } else {
                    try {
                        cursorQueryCandidate.moveToNext();
                        i = i2;
                        jInsert = cursorQueryCandidate.getLong(0);
                        if (!z2) {
                            try {
                                this.mDatabase.writeRowSnapshot(String.valueOf(jInsert), contentValues);
                            } catch (Throwable th8) {
                                th2 = th8;
                                th = null;
                            }
                        }
                        z = true;
                        sQLiteDatabase.update("Documents", contentValues3, "document_id = ?", MtpDatabase.strings(Long.valueOf(jInsert)));
                        contentValues3.put("document_id", Long.valueOf(jInsert));
                        if (contentValues4 == null) {
                        }
                        if (z2) {
                        }
                        if (cursorQueryCandidate == null) {
                        }
                        i2 = i + 1;
                        contentValuesArr3 = contentValuesArr;
                    } catch (Throwable th9) {
                        th = th9;
                        th = null;
                    }
                }
                th2 = th;
                if (cursorQueryCandidate == null) {
                    throw th2;
                }
                if (th == null) {
                    cursorQueryCandidate.close();
                    throw th2;
                }
                try {
                    cursorQueryCandidate.close();
                    throw th2;
                } catch (Throwable th10) {
                    th.addSuppressed(th10);
                    throw th2;
                }
            }
            sQLiteDatabase.setTransactionSuccessful();
            return z2;
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    boolean stopAddingDocuments(String str) throws FileNotFoundException {
        String str2;
        String[] strArrStrings;
        boolean z = true;
        if (str != null) {
            str2 = "parent_document_id = ?";
            strArrStrings = MtpDatabase.strings(str);
        } else {
            str2 = "parent_document_id IS NULL";
            strArrStrings = EMPTY_ARGS;
        }
        SQLiteDatabase sQLiteDatabase = this.mDatabase.getSQLiteDatabase();
        sQLiteDatabase.beginTransaction();
        try {
            Identifier parentOrHaltMapping = getParentOrHaltMapping(str);
            Preconditions.checkState(this.mInMappingIds.contains(str));
            this.mInMappingIds.remove(str);
            if (parentOrHaltMapping == null || parentOrHaltMapping.mDocumentType == 0) {
                if (!this.mDatabase.disconnectDocumentsRecursively("row_state = ? AND " + str2, DatabaseUtils.appendSelectionArgs(MtpDatabase.strings(1), strArrStrings))) {
                    z = false;
                }
            } else {
                if (this.mDatabase.deleteDocumentsAndRootsRecursively("row_state IN (?, ?) AND " + str2, DatabaseUtils.appendSelectionArgs(MtpDatabase.strings(1, 2), strArrStrings))) {
                }
            }
            sQLiteDatabase.setTransactionSuccessful();
            return z;
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    void cancelAddingDocuments(String str) {
        String str2;
        String[] strArrStrings;
        if (str != null) {
            str2 = "parent_document_id = ?";
            strArrStrings = MtpDatabase.strings(str);
        } else {
            str2 = "parent_document_id IS NULL";
            strArrStrings = EMPTY_ARGS;
        }
        SQLiteDatabase sQLiteDatabase = this.mDatabase.getSQLiteDatabase();
        sQLiteDatabase.beginTransaction();
        try {
            if (!this.mInMappingIds.contains(str)) {
                return;
            }
            this.mInMappingIds.remove(str);
            ContentValues contentValues = new ContentValues();
            contentValues.put("row_state", (Integer) 0);
            this.mDatabase.getSQLiteDatabase().update("Documents", contentValues, str2 + " AND row_state = ?", DatabaseUtils.appendSelectionArgs(strArrStrings, MtpDatabase.strings(1)));
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    private Cursor queryCandidate(String str, String[] strArr, String[] strArr2, ContentValues contentValues) {
        for (String str2 : strArr2) {
            Cursor cursorQueryCandidate = queryCandidate(str, strArr, str2, contentValues);
            if (cursorQueryCandidate.getCount() == 0) {
                cursorQueryCandidate.close();
            } else {
                return cursorQueryCandidate;
            }
        }
        return null;
    }

    private Cursor queryCandidate(String str, String[] strArr, String str2, ContentValues contentValues) {
        return this.mDatabase.getSQLiteDatabase().query("Documents", MtpDatabase.strings("document_id"), str + " AND row_state IN (?, ?) AND " + str2 + " = ?", DatabaseUtils.appendSelectionArgs(strArr, MtpDatabase.strings(1, 2, contentValues.getAsString(str2))), null, null, null, "1");
    }

    private Identifier getParentOrHaltMapping(String str) throws FileNotFoundException {
        if (str == null) {
            return null;
        }
        try {
            return this.mDatabase.createIdentifier(str);
        } catch (FileNotFoundException e) {
            this.mInMappingIds.remove(str);
            throw e;
        }
    }
}
