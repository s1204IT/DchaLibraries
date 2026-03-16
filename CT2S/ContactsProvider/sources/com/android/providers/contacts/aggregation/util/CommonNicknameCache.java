package com.android.providers.contacts.aggregation.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.google.android.collect.Maps;
import java.lang.ref.SoftReference;
import java.util.BitSet;
import java.util.HashMap;

public class CommonNicknameCache {
    private final SQLiteDatabase mDb;
    private BitSet mNicknameBloomFilter;
    private HashMap<String, SoftReference<String[]>> mNicknameClusterCache = Maps.newHashMap();

    private static final class NicknameLookupPreloadQuery {
        public static final String[] COLUMNS = {"name"};
    }

    private interface NicknameLookupQuery {
        public static final String[] COLUMNS = {"cluster"};
    }

    public CommonNicknameCache(SQLiteDatabase db) {
        this.mDb = db;
    }

    private void preloadNicknameBloomFilter() {
        this.mNicknameBloomFilter = new BitSet(8192);
        Cursor cursor = this.mDb.query("nickname_lookup", NicknameLookupPreloadQuery.COLUMNS, null, null, null, null, null);
        try {
            int count = cursor.getCount();
            for (int i = 0; i < count; i++) {
                cursor.moveToNext();
                String normalizedName = cursor.getString(0);
                int hashCode = normalizedName.hashCode();
                this.mNicknameBloomFilter.set(hashCode & 8191);
            }
        } finally {
            cursor.close();
        }
    }

    public String[] getCommonNicknameClusters(String normalizedName) {
        if (this.mNicknameBloomFilter == null) {
            preloadNicknameBloomFilter();
        }
        int hashCode = normalizedName.hashCode();
        if (!this.mNicknameBloomFilter.get(hashCode & 8191)) {
            return null;
        }
        String[] clusters = null;
        synchronized (this.mNicknameClusterCache) {
            if (this.mNicknameClusterCache.containsKey(normalizedName)) {
                SoftReference<String[]> ref = this.mNicknameClusterCache.get(normalizedName);
                if (ref == null) {
                    return null;
                }
                clusters = ref.get();
            }
            if (clusters == null) {
                clusters = loadNicknameClusters(normalizedName);
                SoftReference<String[]> ref2 = clusters == null ? null : new SoftReference<>(clusters);
                synchronized (this.mNicknameClusterCache) {
                    this.mNicknameClusterCache.put(normalizedName, ref2);
                }
            }
            return clusters;
        }
    }

    protected String[] loadNicknameClusters(String normalizedName) {
        String[] clusters = null;
        Cursor cursor = this.mDb.query("nickname_lookup", NicknameLookupQuery.COLUMNS, "name=?", new String[]{normalizedName}, null, null, null);
        try {
            int count = cursor.getCount();
            if (count > 0) {
                clusters = new String[count];
                for (int i = 0; i < count; i++) {
                    cursor.moveToNext();
                    clusters[i] = cursor.getString(0);
                }
            }
            return clusters;
        } finally {
            cursor.close();
        }
    }
}
