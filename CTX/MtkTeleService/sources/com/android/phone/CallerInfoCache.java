package com.android.phone;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import java.util.HashMap;

public class CallerInfoCache {
    private static final boolean DBG;
    private static final int INDEX_CUSTOM_RINGTONE = 2;
    private static final int INDEX_NORMALIZED_NUMBER = 1;
    private static final int INDEX_NUMBER = 0;
    private static final int INDEX_SEND_TO_VOICEMAIL = 3;
    private static final String LOG_TAG = CallerInfoCache.class.getSimpleName();
    public static final int MESSAGE_UPDATE_CACHE = 0;
    private static final String[] PROJECTION;
    private static final String SELECTION = "((custom_ringtone IS NOT NULL OR send_to_voicemail=1) AND data1 IS NOT NULL)";
    private static final boolean VDBG = false;
    private CacheAsyncTask mCacheAsyncTask;
    private final Context mContext;
    private volatile HashMap<String, CacheEntry> mNumberToEntry = new HashMap<>();

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
        PROJECTION = new String[]{"data1", "data4", "custom_ringtone", "send_to_voicemail"};
    }

    public static class CacheEntry {
        public final String customRingtone;
        public final boolean sendToVoicemail;

        public CacheEntry(String str, boolean z) {
            this.customRingtone = str;
            this.sendToVoicemail = z;
        }

        public String toString() {
            return "ringtone: " + this.customRingtone + ", " + this.sendToVoicemail;
        }
    }

    private class CacheAsyncTask extends AsyncTask<Void, Void, Void> {
        private PowerManager.WakeLock mWakeLock;

        private CacheAsyncTask() {
        }

        public void acquireWakeLockAndExecute() {
            this.mWakeLock = ((PowerManager) CallerInfoCache.this.mContext.getSystemService("power")).newWakeLock(1, CallerInfoCache.LOG_TAG);
            this.mWakeLock.acquire();
            execute(new Void[0]);
        }

        @Override
        protected Void doInBackground(Void... voidArr) throws Throwable {
            if (CallerInfoCache.DBG) {
                CallerInfoCache.log("Start refreshing cache.");
            }
            CallerInfoCache.this.refreshCacheEntry();
            return null;
        }

        @Override
        protected void onPostExecute(Void r1) {
            super.onPostExecute(r1);
            releaseWakeLock();
        }

        @Override
        protected void onCancelled(Void r1) {
            super.onCancelled(r1);
            releaseWakeLock();
        }

        private void releaseWakeLock() {
            if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
    }

    public static CallerInfoCache init(Context context) {
        if (DBG) {
            log("init()");
        }
        CallerInfoCache callerInfoCache = new CallerInfoCache(context);
        callerInfoCache.startAsyncCache();
        return callerInfoCache;
    }

    private CallerInfoCache(Context context) {
        this.mContext = context;
    }

    void startAsyncCache() {
        if (DBG) {
            log("startAsyncCache");
        }
        if (this.mCacheAsyncTask != null) {
            Log.w(LOG_TAG, "Previous cache task is remaining.");
            this.mCacheAsyncTask.cancel(true);
        }
        this.mCacheAsyncTask = new CacheAsyncTask();
        this.mCacheAsyncTask.acquireWakeLockAndExecute();
    }

    private void refreshCacheEntry() throws Throwable {
        Cursor cursorQuery;
        Throwable th;
        try {
            cursorQuery = this.mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Callable.CONTENT_URI, PROJECTION, SELECTION, null, null);
            try {
                if (cursorQuery != null) {
                    HashMap<String, CacheEntry> map = new HashMap<>(cursorQuery.getCount());
                    while (cursorQuery.moveToNext()) {
                        boolean z = false;
                        String string = cursorQuery.getString(0);
                        String string2 = cursorQuery.getString(1);
                        if (string2 == null) {
                            string2 = PhoneNumberUtils.normalizeNumber(string);
                        }
                        String string3 = cursorQuery.getString(2);
                        if (cursorQuery.getInt(3) == 1) {
                            z = true;
                        }
                        if (PhoneNumberUtils.isUriNumber(string)) {
                            putNewEntryWhenAppropriate(map, string, string3, z);
                        } else {
                            int length = string2.length();
                            if (length > 7) {
                                string2 = string2.substring(length - 7, length);
                            }
                            putNewEntryWhenAppropriate(map, string2, string3, z);
                        }
                    }
                    this.mNumberToEntry = map;
                    if (DBG) {
                        log("Caching entries are done. Total: " + map.size());
                    }
                } else {
                    Log.w(LOG_TAG, "cursor is null");
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (cursorQuery == null) {
                    throw th;
                }
                cursorQuery.close();
                throw th;
            }
        } catch (Throwable th3) {
            cursorQuery = null;
            th = th3;
        }
    }

    private void putNewEntryWhenAppropriate(HashMap<String, CacheEntry> map, String str, String str2, boolean z) {
        if (map.containsKey(str)) {
            if (!map.get(str).sendToVoicemail && z) {
                map.put(str, new CacheEntry(str2, z));
                return;
            }
            return;
        }
        map.put(str, new CacheEntry(str2, z));
    }

    public CacheEntry getCacheEntry(String str) {
        if (this.mNumberToEntry == null) {
            Log.w(LOG_TAG, "Fallback cache isn't ready.");
            return null;
        }
        if (PhoneNumberUtils.isUriNumber(str)) {
            return this.mNumberToEntry.get(str);
        }
        String strNormalizeNumber = PhoneNumberUtils.normalizeNumber(str);
        int length = strNormalizeNumber.length();
        if (length > 7) {
            strNormalizeNumber = strNormalizeNumber.substring(length - 7, length);
        }
        return this.mNumberToEntry.get(strNormalizeNumber);
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
