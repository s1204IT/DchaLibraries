package com.android.mtp;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.media.MediaFile;
import android.mtp.MtpObjectInfo;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MetadataReader;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class MtpDatabase {
    private final SQLiteDatabase mDatabase;
    private final Mapper mMapper = new Mapper(this);

    SQLiteDatabase getSQLiteDatabase() {
        return this.mDatabase;
    }

    MtpDatabase(Context context, int i) {
        this.mDatabase = new OpenHelper(context, i).getWritableDatabase();
    }

    void close() {
        this.mDatabase.close();
    }

    Mapper getMapper() {
        return this.mMapper;
    }

    Cursor queryRoots(Resources resources, String[] strArr) {
        Cursor cursor;
        char c;
        char c2 = 3;
        char c3 = 2;
        Cursor cursorQuery = this.mDatabase.query("Documents", strings("device_id"), "row_state IN (?, ?) AND document_type = ?", strings(0, 1, 0), "device_id", null, null, null);
        try {
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            sQLiteQueryBuilder.setTables(MtpDatabaseConstants.JOIN_ROOTS);
            sQLiteQueryBuilder.setProjectionMap(MtpDatabaseConstants.COLUMN_MAP_ROOTS);
            String[] strArr2 = strArr;
            MatrixCursor matrixCursor = new MatrixCursor(strArr2);
            ContentValues contentValues = new ContentValues();
            while (cursorQuery.moveToNext()) {
                int i = cursorQuery.getInt(0);
                SQLiteDatabase sQLiteDatabase = this.mDatabase;
                Object[] objArr = new Object[4];
                objArr[0] = 0;
                objArr[1] = 1;
                objArr[c3] = 1;
                objArr[c2] = Integer.valueOf(i);
                ContentValues contentValues2 = contentValues;
                Cursor cursorQuery2 = sQLiteQueryBuilder.query(sQLiteDatabase, strArr2, "row_state IN (?, ?) AND document_type = ? AND device_id = ?", strings(objArr), null, null, null);
                try {
                    contentValues2.clear();
                    cursor = cursorQuery2;
                    try {
                        Cursor cursorQuery3 = sQLiteQueryBuilder.query(this.mDatabase, strArr, "row_state IN (?, ?) AND document_type = ? AND device_id = ?", strings(0, 1, 0, Integer.valueOf(i)), null, null, null);
                        Throwable th = null;
                        try {
                            try {
                                cursorQuery3.moveToNext();
                                DatabaseUtils.cursorRowToContentValues(cursorQuery3, contentValues2);
                                if (cursor.getCount() != 0) {
                                    int columnIndex = cursor.getColumnIndex("capacity_bytes");
                                    int columnIndex2 = cursor.getColumnIndex("available_bytes");
                                    long j = 0;
                                    long j2 = 0;
                                    while (cursor.moveToNext()) {
                                        if (columnIndex != -1) {
                                            j += cursor.getLong(columnIndex);
                                        }
                                        if (columnIndex2 != -1) {
                                            j2 += cursor.getLong(columnIndex2);
                                        }
                                    }
                                    contentValues2.put("capacity_bytes", Long.valueOf(j));
                                    contentValues2.put("available_bytes", Long.valueOf(j2));
                                } else {
                                    contentValues2.putNull("capacity_bytes");
                                    contentValues2.putNull("available_bytes");
                                }
                                if (cursor.getCount() == 1 && contentValues2.containsKey("title")) {
                                    cursor.moveToFirst();
                                    c = 2;
                                    contentValues2.put("title", resources.getString(R.string.root_name, contentValues2.getAsString("title"), cursor.getString(cursor.getColumnIndex("title"))));
                                } else {
                                    c = 2;
                                }
                                cursor.close();
                                putValuesToCursor(contentValues2, matrixCursor);
                                strArr2 = strArr;
                                contentValues = contentValues2;
                                c2 = 3;
                                c3 = c;
                            } finally {
                            }
                        } finally {
                            if (cursorQuery3 != null) {
                                $closeResource(th, cursorQuery3);
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        cursor.close();
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    cursor = cursorQuery2;
                }
            }
            return matrixCursor;
        } finally {
            cursorQuery.close();
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

    @VisibleForTesting
    Cursor queryRootDocuments(String[] strArr) {
        return this.mDatabase.query("Documents", strArr, "row_state IN (?, ?) AND document_type = ?", strings(0, 1, 1), null, null, null);
    }

    Cursor queryChildDocuments(String[] strArr, String str) {
        return this.mDatabase.query("Documents", strArr, "row_state IN (?, ?) AND parent_document_id = ?", strings(0, 1, str), null, null, null);
    }

    String[] getStorageDocumentIds(String str) throws Exception {
        Preconditions.checkArgument(createIdentifier(str).mDocumentType == 0);
        Cursor cursorQuery = this.mDatabase.query("Documents", strings("document_id"), "row_state IN (?, ?) AND parent_document_id = ? AND document_type = ?", strings(0, 1, str, 1), null, null, null);
        try {
            String[] strArr = new String[cursorQuery.getCount()];
            int i = 0;
            while (cursorQuery.moveToNext()) {
                strArr[i] = cursorQuery.getString(0);
                i++;
            }
            return strArr;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    Cursor queryDocument(String str, String[] strArr) {
        return this.mDatabase.query("Documents", strArr, "document_id = ?", strings(str), null, null, null, "1");
    }

    String getDocumentIdForDevice(int i) {
        Cursor cursorQuery = this.mDatabase.query("Documents", strings("document_id"), "document_type = ? AND device_id = ?", strings(0, Integer.valueOf(i)), null, null, null, "1");
        try {
            if (cursorQuery.moveToNext()) {
                return cursorQuery.getString(0);
            }
            return null;
        } finally {
            cursorQuery.close();
        }
    }

    Identifier getParentIdentifier(String str) throws FileNotFoundException {
        Cursor cursorQuery = this.mDatabase.query("Documents", strings("parent_document_id"), "document_id = ?", strings(str), null, null, null, "1");
        try {
            if (cursorQuery.moveToNext()) {
                return createIdentifier(cursorQuery.getString(0));
            }
            throw new FileNotFoundException("Cannot find a row having ID = " + str);
        } finally {
            cursorQuery.close();
        }
    }

    String getDeviceDocumentId(int i) throws Exception {
        Cursor cursorQuery = this.mDatabase.query("Documents", strings("document_id"), "device_id = ? AND document_type = ? AND row_state != ?", strings(Integer.valueOf(i), 0, 2), null, null, null, "1");
        try {
            if (cursorQuery.getCount() > 0) {
                cursorQuery.moveToNext();
                return cursorQuery.getString(0);
            }
            throw new FileNotFoundException("The device ID not found: " + i);
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    String putNewDocument(int i, String str, int[] iArr, MtpObjectInfo mtpObjectInfo, long j) {
        ContentValues contentValues = new ContentValues();
        getObjectDocumentValues(contentValues, i, str, iArr, mtpObjectInfo, j);
        this.mDatabase.beginTransaction();
        try {
            long jInsert = this.mDatabase.insert("Documents", null, contentValues);
            this.mDatabase.setTransactionSuccessful();
            return Long.toString(jInsert);
        } finally {
            this.mDatabase.endTransaction();
        }
    }

    void deleteDocument(String str) {
        deleteDocumentsAndRootsRecursively("document_id = ?", strings(str));
    }

    Identifier createIdentifier(String str) throws FileNotFoundException {
        Cursor cursorQuery = this.mDatabase.query("Documents", strings("device_id", "storage_id", "object_handle", "document_type"), "document_id = ? AND row_state IN (?, ?)", strings(str, 0, 1), null, null, null, "1");
        try {
            if (cursorQuery.getCount() == 0) {
                throw new FileNotFoundException("ID \"" + str + "\" is not found.");
            }
            cursorQuery.moveToNext();
            return new Identifier(cursorQuery.getInt(0), cursorQuery.getInt(1), cursorQuery.getInt(2), str, cursorQuery.getInt(3));
        } finally {
            cursorQuery.close();
        }
    }

    boolean deleteDocumentsAndRootsRecursively(String str, String[] strArr) {
        this.mDatabase.beginTransaction();
        try {
            boolean z = true;
            Cursor cursorQuery = this.mDatabase.query("Documents", strings("document_id"), str, strArr, null, null, null);
            boolean z2 = false;
            while (cursorQuery.moveToNext()) {
                try {
                    if (deleteDocumentsAndRootsRecursively("parent_document_id = ?", strings(cursorQuery.getString(0)))) {
                        z2 = true;
                    }
                } catch (Throwable th) {
                    cursorQuery.close();
                    throw th;
                }
            }
            cursorQuery.close();
            if (!deleteDocumentsAndRoots(str, strArr)) {
                z = z2;
            }
            this.mDatabase.setTransactionSuccessful();
            return z;
        } finally {
            this.mDatabase.endTransaction();
        }
    }

    boolean disconnectDocumentsRecursively(String str, String[] strArr) {
        this.mDatabase.beginTransaction();
        try {
            boolean z = true;
            Cursor cursorQuery = this.mDatabase.query("Documents", strings("document_id"), str, strArr, null, null, null);
            Throwable th = null;
            boolean z2 = false;
            while (cursorQuery.moveToNext()) {
                try {
                    try {
                        if (disconnectDocumentsRecursively("parent_document_id = ?", strings(cursorQuery.getString(0)))) {
                            z2 = true;
                        }
                    } finally {
                    }
                } finally {
                    if (cursorQuery != null) {
                        $closeResource(th, cursorQuery);
                    }
                }
            }
            if (!disconnectDocuments(str, strArr)) {
                z = z2;
            }
            this.mDatabase.setTransactionSuccessful();
            return z;
        } finally {
            this.mDatabase.endTransaction();
        }
    }

    boolean deleteDocumentsAndRoots(String str, String[] strArr) {
        this.mDatabase.beginTransaction();
        try {
            int iDelete = this.mDatabase.delete("RootExtra", "root_id IN (" + SQLiteQueryBuilder.buildQueryString(false, "Documents", new String[]{"document_id"}, str, null, null, null, null) + ")", strArr) + 0 + this.mDatabase.delete("Documents", str, strArr);
            this.mDatabase.setTransactionSuccessful();
            return iDelete != 0;
        } finally {
            this.mDatabase.endTransaction();
        }
    }

    boolean disconnectDocuments(String str, String[] strArr) {
        this.mDatabase.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("row_state", (Integer) 2);
            contentValues.putNull("device_id");
            contentValues.putNull("storage_id");
            contentValues.putNull("object_handle");
            boolean z = this.mDatabase.update("Documents", contentValues, str, strArr) != 0;
            this.mDatabase.setTransactionSuccessful();
            return z;
        } finally {
            this.mDatabase.endTransaction();
        }
    }

    void writeRowSnapshot(String str, ContentValues contentValues) throws Exception {
        Cursor cursorQuery = this.mDatabase.query(MtpDatabaseConstants.JOIN_ROOTS, strings("*"), "document_id = ?", strings(str), null, null, null, "1");
        try {
            if (cursorQuery.getCount() == 0) {
                throw new FileNotFoundException();
            }
            cursorQuery.moveToNext();
            contentValues.clear();
            DatabaseUtils.cursorRowToContentValues(cursorQuery, contentValues);
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    void updateObject(String str, int i, String str2, int[] iArr, MtpObjectInfo mtpObjectInfo, Long l) {
        ContentValues contentValues = new ContentValues();
        getObjectDocumentValues(contentValues, i, str2, iArr, mtpObjectInfo, l.longValue());
        this.mDatabase.beginTransaction();
        try {
            this.mDatabase.update("Documents", contentValues, "document_id = ?", strings(str));
            this.mDatabase.setTransactionSuccessful();
        } finally {
            this.mDatabase.endTransaction();
        }
    }

    Identifier getUnmappedDocumentsParent(int i) throws Exception {
        Cursor cursorQuery = this.mDatabase.query("Documents AS child INNER JOIN Documents AS parent ON child.parent_document_id = parent.document_id", strings("parent.device_id", "parent.storage_id", "parent.object_handle", "parent.document_id", "parent.document_type"), "parent.device_id = ? AND parent.row_state IN (?, ?) AND parent.document_type != ? AND child.row_state = ?", strings(Integer.valueOf(i), 0, 1, 0, 2), null, null, null, "1");
        try {
            if (cursorQuery.getCount() == 0) {
                return null;
            }
            cursorQuery.moveToNext();
            Identifier identifier = new Identifier(cursorQuery.getInt(0), cursorQuery.getInt(1), cursorQuery.getInt(2), cursorQuery.getString(3), cursorQuery.getInt(4));
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return identifier;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    void cleanDatabase(Uri[] uriArr) {
        this.mDatabase.beginTransaction();
        try {
            HashSet hashSet = new HashSet();
            int length = uriArr.length;
            int i = 0;
            while (true) {
                Throwable th = null;
                if (i < length) {
                    String documentId = DocumentsContract.getDocumentId(uriArr[i]);
                    while (documentId != null && !hashSet.contains(documentId)) {
                        hashSet.add(documentId);
                        Cursor cursorQuery = this.mDatabase.query("Documents", strings("parent_document_id"), "document_id = ?", strings(documentId), null, null, null);
                        try {
                            try {
                                documentId = cursorQuery.moveToNext() ? cursorQuery.getString(0) : null;
                            } finally {
                            }
                        } finally {
                            if (cursorQuery != null) {
                                $closeResource(th, cursorQuery);
                            }
                        }
                    }
                    i++;
                } else {
                    deleteDocumentsAndRoots("document_id NOT IN " + getIdList(hashSet), null);
                    this.mDatabase.setTransactionSuccessful();
                    return;
                }
            }
        } finally {
            this.mDatabase.endTransaction();
        }
    }

    int getLastBootCount() throws Exception {
        Cursor cursorQuery = this.mDatabase.query("LastBootCount", strings("value"), null, null, null, null, null);
        try {
            if (cursorQuery.moveToNext()) {
                return cursorQuery.getInt(0);
            }
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            return 0;
        } finally {
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
        }
    }

    void setLastBootCount(int i) {
        Preconditions.checkArgumentNonnegative(i, "Boot count must not be negative.");
        this.mDatabase.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("value", Integer.valueOf(i));
            this.mDatabase.delete("LastBootCount", null, null);
            this.mDatabase.insert("LastBootCount", null, contentValues);
            this.mDatabase.setTransactionSuccessful();
        } finally {
            this.mDatabase.endTransaction();
        }
    }

    private static class OpenHelper extends SQLiteOpenHelper {
        public OpenHelper(Context context, int i) {
            super(context, i == 1 ? null : "database", (SQLiteDatabase.CursorFactory) null, 5);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE Documents (document_id INTEGER PRIMARY KEY AUTOINCREMENT,device_id INTEGER,storage_id INTEGER,object_handle INTEGER,parent_document_id INTEGER,row_state INTEGER NOT NULL,document_type INTEGER NOT NULL,mapping_key STRING,mime_type TEXT NOT NULL,_display_name TEXT NOT NULL,summary TEXT,last_modified INTEGER,icon INTEGER,flags INTEGER NOT NULL,_size INTEGER);");
            sQLiteDatabase.execSQL("CREATE TABLE RootExtra (root_id INTEGER PRIMARY KEY,flags INTEGER NOT NULL,available_bytes INTEGER,capacity_bytes INTEGER,mime_types TEXT NOT NULL);");
            sQLiteDatabase.execSQL("CREATE TABLE LastBootCount (value INTEGER NOT NULL);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS Documents");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS RootExtra");
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS LastBootCount");
            onCreate(sQLiteDatabase);
        }
    }

    @VisibleForTesting
    static void deleteDatabase(Context context) {
        context.deleteDatabase("database");
    }

    static void getDeviceDocumentValues(ContentValues contentValues, ContentValues contentValues2, MtpDeviceRecord mtpDeviceRecord) {
        contentValues.clear();
        contentValues.put("device_id", Integer.valueOf(mtpDeviceRecord.deviceId));
        contentValues.putNull("storage_id");
        contentValues.putNull("object_handle");
        contentValues.putNull("parent_document_id");
        contentValues.put("row_state", (Integer) 0);
        contentValues.put("document_type", (Integer) 0);
        contentValues.put("mapping_key", mtpDeviceRecord.deviceKey);
        contentValues.put("mime_type", "vnd.android.document/directory");
        contentValues.put("_display_name", mtpDeviceRecord.name);
        contentValues.putNull("summary");
        contentValues.putNull("last_modified");
        contentValues.put("icon", Integer.valueOf(R.drawable.ic_root_mtp));
        contentValues.put("flags", Integer.valueOf(getDocumentFlags(mtpDeviceRecord.operationsSupported, "vnd.android.document/directory", 0L, 0, 0) & (-9)));
        contentValues.putNull("_size");
        contentValues2.clear();
        contentValues2.put("flags", Integer.valueOf(getRootFlags(mtpDeviceRecord.operationsSupported)));
        contentValues2.putNull("available_bytes");
        contentValues2.putNull("capacity_bytes");
        contentValues2.put("mime_types", "");
    }

    static void getStorageDocumentValues(ContentValues contentValues, ContentValues contentValues2, String str, int[] iArr, MtpRoot mtpRoot) {
        contentValues.clear();
        contentValues.put("device_id", Integer.valueOf(mtpRoot.mDeviceId));
        contentValues.put("storage_id", Integer.valueOf(mtpRoot.mStorageId));
        contentValues.putNull("object_handle");
        contentValues.put("parent_document_id", str);
        contentValues.put("row_state", (Integer) 0);
        contentValues.put("document_type", (Integer) 1);
        contentValues.put("mime_type", "vnd.android.document/directory");
        contentValues.put("_display_name", mtpRoot.mDescription);
        contentValues.putNull("summary");
        contentValues.putNull("last_modified");
        contentValues.put("icon", Integer.valueOf(R.drawable.ic_root_mtp));
        contentValues.put("flags", Integer.valueOf(getDocumentFlags(iArr, "vnd.android.document/directory", 0L, 0, 1)));
        contentValues.put("_size", Long.valueOf(mtpRoot.mMaxCapacity - mtpRoot.mFreeSpace));
        contentValues2.put("flags", Integer.valueOf(getRootFlags(iArr)));
        contentValues2.put("available_bytes", Long.valueOf(mtpRoot.mFreeSpace));
        contentValues2.put("capacity_bytes", Long.valueOf(mtpRoot.mMaxCapacity));
        contentValues2.put("mime_types", "");
    }

    static void getObjectDocumentValues(ContentValues contentValues, int i, String str, int[] iArr, MtpObjectInfo mtpObjectInfo, long j) {
        contentValues.clear();
        String mimeType = getMimeType(mtpObjectInfo);
        contentValues.put("device_id", Integer.valueOf(i));
        contentValues.put("storage_id", Integer.valueOf(mtpObjectInfo.getStorageId()));
        contentValues.put("object_handle", Integer.valueOf(mtpObjectInfo.getObjectHandle()));
        contentValues.put("parent_document_id", str);
        contentValues.put("row_state", (Integer) 0);
        contentValues.put("document_type", (Integer) 2);
        contentValues.put("mime_type", mimeType);
        contentValues.put("_display_name", mtpObjectInfo.getName());
        contentValues.putNull("summary");
        contentValues.put("last_modified", mtpObjectInfo.getDateModified() != 0 ? Long.valueOf(mtpObjectInfo.getDateModified()) : null);
        contentValues.putNull("icon");
        contentValues.put("flags", Integer.valueOf(getDocumentFlags(iArr, mimeType, mtpObjectInfo.getThumbCompressedSizeLong(), mtpObjectInfo.getProtectionStatus(), 2)));
        if (j >= 0) {
            contentValues.put("_size", Long.valueOf(j));
        } else {
            contentValues.putNull("_size");
        }
    }

    private static String getMimeType(MtpObjectInfo mtpObjectInfo) {
        if (mtpObjectInfo.getFormat() == 12289) {
            return "vnd.android.document/directory";
        }
        String mimeTypeForFormatCode = MediaFile.getMimeTypeForFormatCode(mtpObjectInfo.getFormat());
        String mimeTypeForFile = MediaFile.getMimeTypeForFile(mtpObjectInfo.getName());
        if (mimeTypeForFile != null && MediaFile.getFormatCode("", mimeTypeForFile) == mtpObjectInfo.getFormat()) {
            return mimeTypeForFile;
        }
        if (mimeTypeForFormatCode != null) {
            return mimeTypeForFormatCode;
        }
        if (mimeTypeForFile != null) {
            return mimeTypeForFile;
        }
        return "application/octet-stream";
    }

    private static int getRootFlags(int[] iArr) {
        if (MtpDeviceRecord.isWritingSupported(iArr)) {
            return 19;
        }
        return 18;
    }

    private static int getDocumentFlags(int[] iArr, String str, long j, int i, int i2) {
        int i3 = (!str.equals("vnd.android.document/directory") && MtpDeviceRecord.isWritingSupported(iArr) && i == 0) ? 2 : 0;
        if (MtpDeviceRecord.isSupported(iArr, 4107) && ((i == 0 || i == 32771) && i2 == 2)) {
            i3 |= 4;
        }
        if (str.equals("vnd.android.document/directory") && MtpDeviceRecord.isWritingSupported(iArr) && i == 0) {
            i3 |= 8;
        }
        if (MetadataReader.isSupportedMimeType(str)) {
            i3 |= 131072;
        }
        if (j > 0) {
            return i3 | 1;
        }
        return i3;
    }

    static String[] strings(Object... objArr) {
        String[] strArr = new String[objArr.length];
        for (int i = 0; i < objArr.length; i++) {
            strArr[i] = Objects.toString(objArr[i]);
        }
        return strArr;
    }

    static void putValuesToCursor(ContentValues contentValues, MatrixCursor matrixCursor) {
        MatrixCursor.RowBuilder rowBuilderNewRow = matrixCursor.newRow();
        for (String str : matrixCursor.getColumnNames()) {
            rowBuilderNewRow.add(contentValues.get(str));
        }
    }

    private static String getIdList(Set<String> set) {
        String str = "(";
        for (String str2 : set) {
            if (str.length() > 1) {
                str = str + ",";
            }
            str = str + str2;
        }
        return str + ")";
    }
}
