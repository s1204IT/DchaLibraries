package com.android.providers.telephony;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ThreadCache {
    private static Context sContext;
    private static ThreadCache sInstance = null;
    private static Set<ThreadEntry> sThreadCache = null;

    private ThreadCache(Context context) {
        sContext = context;
        sThreadCache = new HashSet();
    }

    public static synchronized void init(Context context) {
        logD("init");
        if (sInstance == null) {
            sContext = context;
            sInstance = new ThreadCache(context);
        }
    }

    public static ThreadCache getInstance() {
        return sInstance;
    }

    private static void logD(String str) {
        Log.d("ThreadCache", str);
    }

    public void add(Cursor cursor, List<String> list) {
        if (sThreadCache == null) {
            return;
        }
        synchronized (sInstance) {
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst() && list != null && list.size() > 0) {
                        sThreadCache.add(new ThreadEntry(cursor.getLong(0), list));
                        logD("add item, threadId = " + cursor.getLong(0) + " , recipients count = " + list.size() + ", cache size = " + sThreadCache.size());
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    public void remove(long j) {
        if (sThreadCache == null) {
            return;
        }
        logD("Remove item, threadId = " + j + ", before remove, cache size = " + sThreadCache.size());
        synchronized (sInstance) {
            Iterator it = new HashSet(sThreadCache).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ThreadEntry threadEntry = (ThreadEntry) it.next();
                if (threadEntry.getThreadId() == j) {
                    sThreadCache.remove(threadEntry);
                    break;
                }
            }
        }
        logD("Remove item, threadId = " + j + ", after remove, cache size = " + sThreadCache.size());
    }

    public void removeAll() {
        if (sThreadCache == null) {
            sInstance = null;
            return;
        }
        synchronized (sInstance) {
            logD("Remove all items");
            sThreadCache.clear();
            sThreadCache = null;
            sInstance = null;
        }
    }

    public long getThreadId(List<String> list, boolean z) {
        if (sThreadCache == null) {
            return 0L;
        }
        for (ThreadEntry threadEntry : sThreadCache) {
            if (isEquals(threadEntry.getAddresses(), list, z)) {
                logD("Get related thread id = " + threadEntry.getThreadId());
                return threadEntry.getThreadId();
            }
        }
        logD("Can not get related thread id ");
        return 0L;
    }

    private boolean isEquals(List<String> list, List<String> list2, boolean z) {
        boolean z2;
        if (list == null || list2 == null || list.size() != list2.size()) {
            logD("isEquals, Different addr size");
            return false;
        }
        List<String> lowerCase = toLowerCase(list);
        List<String> lowerCase2 = toLowerCase(list2);
        do {
            z2 = true;
            if (lowerCase.size() <= 0) {
                return true;
            }
            for (int i = 0; i < lowerCase2.size(); i++) {
                if (lowerCase.get(0).equals(lowerCase2.get(i)) || PhoneNumberUtils.compare(lowerCase.get(0), lowerCase2.get(i), z)) {
                    lowerCase2.remove(i);
                    lowerCase.remove(0);
                    break;
                }
            }
            z2 = false;
        } while (z2);
        return false;
    }

    private List<String> toLowerCase(List<String> list) {
        ArrayList arrayList = new ArrayList();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                String lowerCase = list.get(i);
                if (Telephony.Mms.isEmailAddress(lowerCase)) {
                    lowerCase = lowerCase.toLowerCase();
                }
                arrayList.add(lowerCase);
            }
        }
        return arrayList;
    }

    public Cursor formCursor(long j) {
        logD("formCursor, threadId = " + j);
        if (j <= 0) {
            return null;
        }
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id"});
        matrixCursor.addRow(new Object[]{Long.valueOf(j)});
        return matrixCursor;
    }

    class ThreadEntry {
        private List<String> addresses;
        private long threadId;

        public ThreadEntry(long j, List<String> list) {
            this.threadId = 0L;
            this.addresses = null;
            this.threadId = j;
            this.addresses = list;
        }

        public long getThreadId() {
            return this.threadId;
        }

        public List<String> getAddresses() {
            return this.addresses;
        }
    }
}
