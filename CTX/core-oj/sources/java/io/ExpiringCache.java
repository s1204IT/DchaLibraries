package java.io;

import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class ExpiringCache {
    private int MAX_ENTRIES;
    private Map<String, Entry> map;
    private long millisUntilExpiration;
    private int queryCount;
    private int queryOverflow;

    static class Entry {
        private long timestamp;
        private String val;

        Entry(long j, String str) {
            this.timestamp = j;
            this.val = str;
        }

        long timestamp() {
            return this.timestamp;
        }

        void setTimestamp(long j) {
            this.timestamp = j;
        }

        String val() {
            return this.val;
        }

        void setVal(String str) {
            this.val = str;
        }
    }

    ExpiringCache() {
        this(30000L);
    }

    ExpiringCache(long j) {
        this.queryOverflow = HttpURLConnection.HTTP_MULT_CHOICE;
        this.MAX_ENTRIES = HttpURLConnection.HTTP_OK;
        this.millisUntilExpiration = j;
        this.map = new LinkedHashMap<String, Entry>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> entry) {
                return size() > ExpiringCache.this.MAX_ENTRIES;
            }
        };
    }

    synchronized String get(String str) {
        int i = this.queryCount + 1;
        this.queryCount = i;
        if (i >= this.queryOverflow) {
            cleanup();
        }
        Entry entryEntryFor = entryFor(str);
        if (entryEntryFor != null) {
            return entryEntryFor.val();
        }
        return null;
    }

    synchronized void put(String str, String str2) {
        int i = this.queryCount + 1;
        this.queryCount = i;
        if (i >= this.queryOverflow) {
            cleanup();
        }
        Entry entryEntryFor = entryFor(str);
        if (entryEntryFor != null) {
            entryEntryFor.setTimestamp(System.currentTimeMillis());
            entryEntryFor.setVal(str2);
        } else {
            this.map.put(str, new Entry(System.currentTimeMillis(), str2));
        }
    }

    synchronized void clear() {
        this.map.clear();
    }

    private Entry entryFor(String str) {
        Entry entry = this.map.get(str);
        if (entry != null) {
            long jCurrentTimeMillis = System.currentTimeMillis() - entry.timestamp();
            if (jCurrentTimeMillis < 0 || jCurrentTimeMillis >= this.millisUntilExpiration) {
                this.map.remove(str);
                return null;
            }
            return entry;
        }
        return entry;
    }

    private void cleanup() {
        Set<String> setKeySet = this.map.keySet();
        String[] strArr = new String[setKeySet.size()];
        Iterator<String> it = setKeySet.iterator();
        int i = 0;
        while (it.hasNext()) {
            strArr[i] = it.next();
            i++;
        }
        for (String str : strArr) {
            entryFor(str);
        }
        this.queryCount = 0;
    }
}
