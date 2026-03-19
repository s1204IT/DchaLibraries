package com.mediatek.appworkingset;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Iterator;

class AWSDBHelper extends SQLiteOpenHelper {
    private static final String CREATE_TABLE_PKG_PRIORITY_LIST = "CREATE TABLE aws_pkg_priority(prim_key_id INTEGER PRIMARY KEY,pkg_name TEXT,priority INTEGER)";
    private static final String CREATE_TABLE_PKG_PROC_LIST = "CREATE TABLE aws_pkg_process(prim_key_id INTEGER PRIMARY KEY,pkg_id INTEGER,proc_name TEXT,uid INTEGER,launch_mem INTEGER)";
    static final int DB_EMPTY = 1;
    private static final String DB_NAME = "awshelper.db";
    private static final int DB_VERSION = 1;
    static final boolean DEBUG_DB = false;
    static final int ERROR_MAX_EXCEEDED = -2;
    static final int ERROR_MAX_PKG_PRIORITY_LIST = 200;
    static final int ERROR_MAX_PKG_PROC_LIST = 2000;
    static final int MAX_EXCEEDED = -1;
    static final int MAX_PKG_PRIORITY_LIST = 100;
    static final int MAX_PKG_PROC_LIST = 1000;
    static final String TAG = "AWSDBHelper";
    final Context mContext;
    private boolean mIsReady;
    final AWSManager mManager;
    private int mNumPackagePriorityList;
    SparseArray<String> mPackageIDMap;
    ArrayList<PkgPriority> mPackagePriorityList;
    ArrayMap<String, PkgPriority> mPackagePriorityMap;

    public interface PackagePriorityList {
        public static final String KEY_ID = "prim_key_id";
        public static final String KEY_PKG_NAME = "pkg_name";
        public static final String KEY_PRIORITY = "priority";
        public static final String TABLE_NAME = "aws_pkg_priority";
    }

    public interface PackageProcessList {
        public static final String KEY_ID = "prim_key_id";
        public static final String KEY_LAUNCH_MEM = "launch_mem";
        public static final String KEY_PKG_ID = "pkg_id";
        public static final String KEY_PROC_NAME = "proc_name";
        public static final String KEY_UID = "uid";
        public static final String TABLE_NAME = "aws_pkg_process";
    }

    public AWSDBHelper(Context context, AWSManager aWSManager) {
        super(context, DB_NAME, (SQLiteDatabase.CursorFactory) null, 1);
        this.mIsReady = false;
        if (aWSManager != null && context != null) {
            this.mManager = aWSManager;
            this.mContext = context;
            this.mIsReady = true;
        } else {
            this.mManager = null;
            this.mContext = null;
            Log.e(TAG, "AWSDBHelper construct fail:" + aWSManager + "," + context);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(CREATE_TABLE_PKG_PROC_LIST);
        sQLiteDatabase.execSQL(CREATE_TABLE_PKG_PRIORITY_LIST);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS aws_pkg_process");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS aws_pkg_priority");
        onCreate(sQLiteDatabase);
    }

    private boolean IsSystemAndReady(String str) {
        if (this.mIsReady) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 0 || callingUid == 1000) {
                return true;
            }
            throw new SecurityException(str + " called from non-system process");
        }
        Log.v(TAG, "not ready");
        return false;
    }

    protected void readDB() {
        if (!IsSystemAndReady("readDB")) {
            Log.v(TAG, "not ready, skip reading db");
            return;
        }
        ArrayList arrayList = new ArrayList();
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager == null) {
            Log.v(TAG, "PM not ready, skip reading db");
            return;
        }
        int packagePriorityList = getPackagePriorityList();
        if (packagePriorityList == -2) {
            return;
        }
        if (packagePriorityList == 1) {
            Log.v(TAG, "DB size empty");
            return;
        }
        if (checkSizeOfPackageProcessList() == -2) {
            return;
        }
        Iterator<ApplicationInfo> it = packageManager.getInstalledApplications(128).iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().packageName);
        }
        ArrayList<PkgPriority> arrayList2 = new ArrayList();
        Iterator<PkgPriority> it2 = this.mPackagePriorityList.iterator();
        while (it2.hasNext()) {
            PkgPriority next = it2.next();
            Iterator it3 = arrayList.iterator();
            while (it3.hasNext()) {
                if (next.getPkgName().equals((String) it3.next())) {
                    arrayList2.add(next);
                    it2.remove();
                }
            }
        }
        if (arrayList2.isEmpty()) {
            Log.v(TAG, "Nothing to read from db");
        } else {
            SQLiteDatabase readableDatabase = getReadableDatabase();
            String[] strArr = {PackageProcessList.KEY_PROC_NAME, PackageProcessList.KEY_UID, PackageProcessList.KEY_LAUNCH_MEM};
            ArrayMap arrayMap = new ArrayMap();
            readableDatabase.beginTransaction();
            try {
                for (PkgPriority pkgPriority : arrayList2) {
                    arrayMap.put(pkgPriority.getPkgName(), readableDatabase.query(PackageProcessList.TABLE_NAME, strArr, "pkg_id=" + pkgPriority.getPkgID().intValue(), null, null, null, null, null));
                }
                readableDatabase.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "query PkgProc, " + e);
            } finally {
                readableDatabase.endTransaction();
            }
            for (int size = arrayList2.size() - 1; size >= 0; size--) {
                String pkgName = ((PkgPriority) arrayList2.get(size)).getPkgName();
                Cursor cursor = (Cursor) arrayMap.get(pkgName);
                if (cursor != null) {
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        ProcessRecordStore processRecordStore = new ProcessRecordStore(cursor.getString(cursor.getColumnIndex(PackageProcessList.KEY_PROC_NAME)), cursor.getInt(cursor.getColumnIndex(PackageProcessList.KEY_UID)), pkgName, cursor.getLong(cursor.getColumnIndex(PackageProcessList.KEY_LAUNCH_MEM)));
                        this.mManager.updateProcessNames(processRecordStore);
                        this.mManager.updateLaunchProcList(pkgName, processRecordStore);
                        cursor.moveToNext();
                    }
                }
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        if (this.mPackagePriorityList.isEmpty()) {
            Log.v(TAG, "Nothing for db to delete");
        } else {
            Log.v(TAG, "Deleting Unmapped...");
            SQLiteDatabase writableDatabase = getWritableDatabase();
            writableDatabase.beginTransaction();
            try {
                Iterator<PkgPriority> it4 = this.mPackagePriorityList.iterator();
                while (it4.hasNext()) {
                    String[] strArr2 = {String.valueOf(it4.next().getPkgID().intValue())};
                    writableDatabase.delete(PackageProcessList.TABLE_NAME, "pkg_id=?", strArr2);
                    writableDatabase.delete(PackagePriorityList.TABLE_NAME, "prim_key_id=?", strArr2);
                }
                writableDatabase.setTransactionSuccessful();
            } catch (Exception e2) {
                Log.e(TAG, "Delete Unmapped, " + e2);
                writableDatabase.endTransaction();
            } finally {
                writableDatabase.endTransaction();
            }
        }
        close();
        Log.v(TAG, "readDB done");
    }

    protected void updateDB() {
        int i;
        PkgPriority pkgPriority;
        boolean z;
        int i2;
        int iCheckSizeOfPackagePriorityList;
        int i3;
        if (!IsSystemAndReady("updateDB")) {
            Log.v(TAG, "not ready, skip update db");
            return;
        }
        if (getPackagePriorityList() == -2) {
            return;
        }
        ArrayList<PkgProcess> arrayList = new ArrayList();
        ArrayList<PkgProcess> arrayList2 = new ArrayList();
        ArrayList<PkgPriority> arrayList3 = new ArrayList();
        ArrayList<PkgPriority> arrayList4 = new ArrayList();
        ArrayList arrayList5 = new ArrayList();
        int i4 = 0;
        synchronized (this.mManager.mPackagesProcessMap) {
            PkgPriorityNode pkgPriorityNode = this.mManager.mPDirty;
            PkgPriorityNode pkgPriorityNode2 = this.mManager.mPHead.next;
            if (pkgPriorityNode2 == null) {
                Log.v(TAG, "No dirty, nothing to update");
                return;
            }
            PkgPriorityNode pkgPriorityNode3 = pkgPriorityNode2;
            int i5 = 1;
            while (true) {
                String pkgName = pkgPriorityNode3.getPkgName();
                PkgPriority pkgPriority2 = this.mPackagePriorityMap.get(pkgName);
                if (pkgPriority2 == null) {
                    i = i4 + 1;
                    PkgPriority pkgPriority3 = new PkgPriority(null, pkgName, i5);
                    arrayList4.add(pkgPriority3);
                    arrayList5.add(pkgPriority3);
                    pkgPriority = pkgPriority3;
                    z = true;
                } else {
                    pkgPriority2.setPriority(i5);
                    arrayList3.add(pkgPriority2);
                    arrayList5.add(pkgPriority2);
                    this.mPackagePriorityList.remove(pkgPriority2);
                    pkgPriority = pkgPriority2;
                    z = false;
                    i = i4;
                }
                for (ProcessRecordStore processRecordStore : pkgPriorityNode3.procList) {
                    String procName = processRecordStore.getProcName();
                    int uid = processRecordStore.getUid();
                    long launchMem = processRecordStore.getLaunchMem(pkgName);
                    if (z) {
                        arrayList2.add(new PkgProcess(procName, uid, launchMem, pkgName));
                    } else {
                        arrayList.add(new PkgProcess(Integer.valueOf(pkgPriority.getPkgID().intValue()), procName, uid, launchMem));
                    }
                }
                PkgPriorityNode pkgPriorityNode4 = pkgPriorityNode3.next;
                i2 = i5 + 1;
                if (pkgPriorityNode4.next == null || pkgPriorityNode4.prev == pkgPriorityNode) {
                    break;
                }
                pkgPriorityNode3 = pkgPriorityNode4;
                i5 = i2;
                i4 = i;
            }
            for (PkgPriority pkgPriority4 : this.mPackagePriorityList) {
                pkgPriority4.setPriority(i2);
                arrayList5.add(pkgPriority4);
                i2++;
            }
            SQLiteDatabase writableDatabase = getWritableDatabase();
            writableDatabase.beginTransaction();
            try {
                for (PkgPriority pkgPriority5 : arrayList4) {
                    ContentValues contentValues = new ContentValues();
                    String pkgName2 = pkgPriority5.getPkgName();
                    contentValues.put(PackagePriorityList.KEY_PKG_NAME, pkgPriority5.getPkgName());
                    contentValues.put(PackagePriorityList.KEY_PRIORITY, Integer.valueOf(pkgPriority5.getPriority()));
                    int iInsertWithOnConflict = (int) writableDatabase.insertWithOnConflict(PackagePriorityList.TABLE_NAME, null, contentValues, 4);
                    if (iInsertWithOnConflict != -1) {
                        this.mPackageIDMap.put(iInsertWithOnConflict, pkgName2);
                    }
                }
                for (PkgPriority pkgPriority6 : arrayList3) {
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.put(PackagePriorityList.KEY_PRIORITY, Integer.valueOf(pkgPriority6.getPriority()));
                    writableDatabase.update(PackagePriorityList.TABLE_NAME, contentValues2, "prim_key_id=" + pkgPriority6.getPkgID(), null);
                }
                for (PkgPriority pkgPriority7 : this.mPackagePriorityList) {
                    ContentValues contentValues3 = new ContentValues();
                    contentValues3.put(PackagePriorityList.KEY_PRIORITY, Integer.valueOf(pkgPriority7.getPriority()));
                    writableDatabase.update(PackagePriorityList.TABLE_NAME, contentValues3, "prim_key_id=" + pkgPriority7.getPkgID(), null);
                }
                writableDatabase.setTransactionSuccessful();
                writableDatabase.endTransaction();
            } catch (Exception e) {
                Log.e(TAG, "Update to PackagePriorityList, " + e);
                writableDatabase.endTransaction();
            } finally {
            }
            for (PkgProcess pkgProcess : arrayList2) {
                pkgProcess.setPkgID(Integer.valueOf(this.mPackageIDMap.keyAt(this.mPackageIDMap.indexOfValue(pkgProcess.getdummyPkgName()))));
            }
            writableDatabase = getWritableDatabase();
            writableDatabase.beginTransaction();
            try {
                Iterator it = arrayList3.iterator();
                while (it.hasNext()) {
                    int iIntValue = ((PkgPriority) it.next()).getPkgID().intValue();
                    this.mPackageIDMap.get(iIntValue);
                    writableDatabase.delete(PackageProcessList.TABLE_NAME, "pkg_id=?", new String[]{String.valueOf(iIntValue)});
                }
                writableDatabase.setTransactionSuccessful();
                writableDatabase.endTransaction();
            } catch (Exception e2) {
                Log.e(TAG, "Delete exceed for PackageProcessList, " + e2);
                writableDatabase.endTransaction();
            } finally {
            }
            writableDatabase = getWritableDatabase();
            writableDatabase.beginTransaction();
            try {
                for (PkgProcess pkgProcess2 : arrayList2) {
                    ContentValues contentValues4 = new ContentValues();
                    contentValues4.put(PackageProcessList.KEY_PKG_ID, pkgProcess2.getPkgID());
                    contentValues4.put(PackageProcessList.KEY_PROC_NAME, pkgProcess2.getProcName());
                    contentValues4.put(PackageProcessList.KEY_UID, Integer.valueOf(pkgProcess2.getUID()));
                    contentValues4.put(PackageProcessList.KEY_LAUNCH_MEM, Long.valueOf(pkgProcess2.getlaunchMem()));
                    if (writableDatabase.insertWithOnConflict(PackageProcessList.TABLE_NAME, null, contentValues4, 4) == -1) {
                    }
                }
                for (PkgProcess pkgProcess3 : arrayList) {
                    ContentValues contentValues5 = new ContentValues();
                    contentValues5.put(PackageProcessList.KEY_PKG_ID, pkgProcess3.getPkgID());
                    contentValues5.put(PackageProcessList.KEY_PROC_NAME, pkgProcess3.getProcName());
                    contentValues5.put(PackageProcessList.KEY_UID, Integer.valueOf(pkgProcess3.getUID()));
                    contentValues5.put(PackageProcessList.KEY_LAUNCH_MEM, Long.valueOf(pkgProcess3.getlaunchMem()));
                    if (writableDatabase.insertWithOnConflict(PackageProcessList.TABLE_NAME, null, contentValues5, 4) == -1) {
                    }
                    while (r3.hasNext()) {
                    }
                }
                writableDatabase.setTransactionSuccessful();
            } catch (Exception e3) {
                Log.e(TAG, "Insert All new ones PackageProcessList, " + e3);
            } finally {
            }
            int iCheckSizeOfPackageProcessList = checkSizeOfPackageProcessList();
            if (iCheckSizeOfPackageProcessList == -2 || (iCheckSizeOfPackagePriorityList = checkSizeOfPackagePriorityList()) == -2) {
                return;
            }
            if ((iCheckSizeOfPackageProcessList == -1 || iCheckSizeOfPackagePriorityList == -1) && (this.mNumPackagePriorityList + i) - 100 > 0) {
                writableDatabase.beginTransaction();
                try {
                    int size = arrayList5.size();
                    int i6 = size - 1;
                    while (true) {
                        int i7 = i6;
                        if (size - i7 > i3) {
                            break;
                        }
                        String[] strArr = {String.valueOf(((PkgPriority) arrayList5.get(i7)).getPkgID())};
                        writableDatabase.delete(PackageProcessList.TABLE_NAME, "pkg_id=?", strArr);
                        writableDatabase.delete(PackagePriorityList.TABLE_NAME, "prim_key_id=?", strArr);
                        i6 = i7 - 1;
                    }
                    writableDatabase.setTransactionSuccessful();
                } catch (Exception e4) {
                    Log.e(TAG, "Delete exceed for ProcPriorityList, numToDelete= " + i3 + ", newlyAdd= " + i + ", mNumPackagePriorityList= " + this.mNumPackagePriorityList + ", MAX_PKG_PRIORITY_LIST= 100" + e4);
                    writableDatabase.endTransaction();
                } finally {
                }
            }
            close();
            Log.v(TAG, "DB updated done");
        }
    }

    private int checkSizeOfPackageProcessList() {
        long jQueryNumEntries = DatabaseUtils.queryNumEntries(getReadableDatabase(), PackageProcessList.TABLE_NAME);
        if (!(jQueryNumEntries <= 2000)) {
            Log.v(TAG, "DB exceed maximum, delete all:2000");
            deleteAllTable();
            return -2;
        }
        if (jQueryNumEntries <= 1000) {
            return 0;
        }
        Log.v(TAG, "DB exceed maximum, delete one:1000");
        return -1;
    }

    private int checkSizeOfPackagePriorityList() {
        long jQueryNumEntries = DatabaseUtils.queryNumEntries(getReadableDatabase(), PackagePriorityList.TABLE_NAME);
        this.mNumPackagePriorityList = (int) jQueryNumEntries;
        if (!(jQueryNumEntries <= 200)) {
            Log.v(TAG, "DB exceed maximum, delete all:" + jQueryNumEntries + ">" + ERROR_MAX_PKG_PRIORITY_LIST);
            deleteAllTable();
            return -2;
        }
        if (jQueryNumEntries <= 100) {
            return 0;
        }
        Log.v(TAG, "DB exceed maximum, delete one:" + jQueryNumEntries + ">100");
        return -1;
    }

    private int getPackagePriorityList() {
        if (this.mPackagePriorityMap == null) {
            this.mPackagePriorityMap = new ArrayMap<>();
        } else {
            this.mPackagePriorityMap.clear();
        }
        if (this.mPackagePriorityList == null) {
            this.mPackagePriorityList = new ArrayList<>();
        } else {
            this.mPackagePriorityList.clear();
        }
        if (this.mPackageIDMap == null) {
            this.mPackageIDMap = new SparseArray<>();
        } else {
            this.mPackageIDMap.clear();
        }
        Cursor cursorQuery = getReadableDatabase().query(PackagePriorityList.TABLE_NAME, new String[]{"prim_key_id", PackagePriorityList.KEY_PKG_NAME, PackagePriorityList.KEY_PRIORITY}, null, null, null, null, PackagePriorityList.KEY_PRIORITY);
        int count = cursorQuery.getCount();
        this.mNumPackagePriorityList = count;
        if (count > ERROR_MAX_PKG_PRIORITY_LIST) {
            Log.v(TAG, "DB exceed maximum, delete all:200");
            deleteAllTable();
            cursorQuery.close();
            return -2;
        }
        cursorQuery.moveToFirst();
        for (int i = 0; i < count; i++) {
            int i2 = cursorQuery.getInt(cursorQuery.getColumnIndex("prim_key_id"));
            String string = cursorQuery.getString(cursorQuery.getColumnIndex(PackagePriorityList.KEY_PKG_NAME));
            int i3 = cursorQuery.getInt(cursorQuery.getColumnIndex(PackagePriorityList.KEY_PRIORITY));
            Log.v(TAG, "Reading :ID,pkgName,priority=" + i2 + "," + string + "," + i3);
            PkgPriority pkgPriority = new PkgPriority(Integer.valueOf(i2), string, i3);
            this.mPackagePriorityMap.put(string, pkgPriority);
            this.mPackageIDMap.put(i2, string);
            this.mPackagePriorityList.add(pkgPriority);
            cursorQuery.moveToNext();
        }
        cursorQuery.close();
        if (this.mNumPackagePriorityList <= 100) {
            return this.mNumPackagePriorityList != 0 ? 0 : 1;
        }
        return -1;
    }

    private void deleteAllTable() {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.beginTransaction();
        try {
            writableDatabase.delete(PackageProcessList.TABLE_NAME, null, null);
            writableDatabase.delete(PackagePriorityList.TABLE_NAME, null, null);
            writableDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "deleteAllTable, " + e);
            writableDatabase.endTransaction();
        } finally {
            writableDatabase.endTransaction();
        }
    }

    private class PkgPriority {
        private Integer ID;
        private String pkgName;
        private int pkgPriority;

        public PkgPriority(Integer num, String str, int i) {
            this.ID = num;
            this.pkgName = str;
            this.pkgPriority = i;
        }

        public Integer getPkgID() {
            return this.ID;
        }

        public String getPkgName() {
            return this.pkgName;
        }

        public int getPriority() {
            return this.pkgPriority;
        }

        public void setPriority(int i) {
            this.pkgPriority = i;
        }
    }

    private class PkgProcess {
        private Integer PkgID;
        private String dummyPkgName;
        private long launchMem;
        private String procName;
        private int uid;

        public PkgProcess(String str, int i, long j, String str2) {
            this.PkgID = null;
            this.procName = str;
            this.uid = i;
            this.launchMem = j;
            this.dummyPkgName = str2;
        }

        public PkgProcess(Integer num, String str, int i, long j) {
            this.PkgID = num;
            this.procName = str;
            this.uid = i;
            this.launchMem = j;
        }

        public Integer getPkgID() {
            return this.PkgID;
        }

        public String getProcName() {
            return this.procName;
        }

        public int getUID() {
            return this.uid;
        }

        public long getlaunchMem() {
            return this.launchMem;
        }

        public String getdummyPkgName() {
            return this.dummyPkgName;
        }

        public void setPkgID(Integer num) {
            this.PkgID = num;
        }
    }
}
