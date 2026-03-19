package com.android.providers.contacts.aggregation.util;

import android.app.ActivityManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.ArrayMap;
import java.lang.ref.SoftReference;
import java.util.BitSet;

public class CommonNicknameCache {
    private final SQLiteDatabase mDb;
    private BitSet mNicknameBloomFilter;
    private final ArrayMap<String, SoftReference<String[]>> mNicknameClusterCache = new ArrayMap<>();

    private static final class NicknameLookupPreloadQuery {
        public static final String[] COLUMNS = {"name"};
    }

    private interface NicknameLookupQuery {
        public static final String[] COLUMNS = {"cluster"};
    }

    public CommonNicknameCache(SQLiteDatabase sQLiteDatabase) {
        this.mDb = sQLiteDatabase;
    }

    private void preloadNicknameBloomFilter() {
        this.mNicknameBloomFilter = new BitSet(8192);
        Cursor cursorQuery = this.mDb.query("nickname_lookup", NicknameLookupPreloadQuery.COLUMNS, null, null, null, null, null);
        try {
            int count = cursorQuery.getCount();
            for (int i = 0; i < count; i++) {
                cursorQuery.moveToNext();
                this.mNicknameBloomFilter.set(cursorQuery.getString(0).hashCode() & 8191);
            }
        } finally {
            cursorQuery.close();
        }
    }

    public String[] getCommonNicknameClusters(String str) {
        String[] strArrLoadNicknameClusters;
        if (ActivityManager.isLowRamDeviceStatic()) {
            return null;
        }
        if (this.mNicknameBloomFilter == null) {
            preloadNicknameBloomFilter();
        }
        if (!this.mNicknameBloomFilter.get(str.hashCode() & 8191)) {
            return null;
        }
        synchronized (this.mNicknameClusterCache) {
            if (this.mNicknameClusterCache.containsKey(str)) {
                SoftReference<String[]> softReference = this.mNicknameClusterCache.get(str);
                if (softReference == null) {
                    return null;
                }
                strArrLoadNicknameClusters = softReference.get();
            } else {
                strArrLoadNicknameClusters = null;
            }
            if (strArrLoadNicknameClusters == null) {
                strArrLoadNicknameClusters = loadNicknameClusters(str);
                SoftReference<String[]> softReference2 = strArrLoadNicknameClusters != null ? new SoftReference<>(strArrLoadNicknameClusters) : null;
                synchronized (this.mNicknameClusterCache) {
                    this.mNicknameClusterCache.put(str, softReference2);
                }
            }
            return strArrLoadNicknameClusters;
        }
    }

    protected String[] loadNicknameClusters(String str) {
        String[] strArr;
        Cursor cursorQuery = this.mDb.query("nickname_lookup", NicknameLookupQuery.COLUMNS, "name=?", new String[]{str}, null, null, null);
        try {
            int count = cursorQuery.getCount();
            if (count > 0) {
                strArr = new String[count];
                for (int i = 0; i < count; i++) {
                    cursorQuery.moveToNext();
                    strArr[i] = cursorQuery.getString(0);
                }
            } else {
                strArr = null;
            }
            return strArr;
        } finally {
            cursorQuery.close();
        }
    }
}
