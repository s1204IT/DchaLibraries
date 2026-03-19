package com.android.calendar;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import com.mediatek.calendar.LogUtil;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class CalendarUtils {

    public static class TimeZoneUtils {
        private static AsyncTZHandler mHandler;
        private final String mPrefsName;
        private static final String[] TIMEZONE_TYPE_ARGS = {"timezoneType"};
        private static final String[] TIMEZONE_INSTANCES_ARGS = {"timezoneInstances"};
        public static final String[] CALENDAR_CACHE_POJECTION = {"key", "value"};
        private static StringBuilder mSB = new StringBuilder(50);
        private static Formatter mF = new Formatter(mSB, Locale.getDefault());
        private static volatile boolean mFirstTZRequest = true;
        private static volatile boolean mTZQueryInProgress = false;
        private static volatile boolean mUseHomeTZ = false;
        private static volatile String mHomeTZ = Time.getCurrentTimezone();
        private static HashMap<Looper, HashSet<Runnable>> mTZCallbacks = new HashMap<>();
        private static int mToken = 1;

        private class AsyncTZHandler extends AsyncQueryHandler {
            public AsyncTZHandler(ContentResolver contentResolver) {
                super(contentResolver);
            }

            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                synchronized (TimeZoneUtils.mTZCallbacks) {
                    try {
                        if (cursor == null) {
                            boolean unused = TimeZoneUtils.mTZQueryInProgress = false;
                            boolean unused2 = TimeZoneUtils.mFirstTZRequest = true;
                            return;
                        }
                        int columnIndexOrThrow = cursor.getColumnIndexOrThrow("key");
                        int columnIndexOrThrow2 = cursor.getColumnIndexOrThrow("value");
                        if (columnIndexOrThrow >= 0 && columnIndexOrThrow2 >= 0) {
                            SharedPreferences sharedPreferences = CalendarUtils.getSharedPreferences((Context) obj, TimeZoneUtils.this.mPrefsName);
                            String string = sharedPreferences.getString("preferences_home_tz", null);
                            LogUtil.oi("CalendarUtils", "homeTz:" + string);
                            boolean z = false;
                            while (cursor.moveToNext()) {
                                String string2 = cursor.getString(columnIndexOrThrow);
                                String string3 = cursor.getString(columnIndexOrThrow2);
                                LogUtil.oi("CalendarUtils", "onQueryComplete, key:" + string2 + "; value:" + string3);
                                if (TextUtils.equals(string2, "timezoneType")) {
                                    boolean z2 = !TextUtils.equals(string3, "auto");
                                    if (z2 != TimeZoneUtils.mUseHomeTZ) {
                                        boolean unused3 = TimeZoneUtils.mUseHomeTZ = z2;
                                        z = true;
                                    }
                                } else if (TextUtils.equals(string2, "timezoneInstancesPrevious") && !TextUtils.isEmpty(string3) && !TextUtils.equals(TimeZoneUtils.mHomeTZ, string3) && !TextUtils.isEmpty(string)) {
                                    String unused4 = TimeZoneUtils.mHomeTZ = string3;
                                    z = true;
                                }
                            }
                            cursor.close();
                            if (z) {
                                CalendarUtils.setSharedPreference(sharedPreferences, "preferences_home_tz_enabled", TimeZoneUtils.mUseHomeTZ);
                                CalendarUtils.setSharedPreference(sharedPreferences, "preferences_home_tz", TimeZoneUtils.mHomeTZ);
                            }
                            boolean unused5 = TimeZoneUtils.mTZQueryInProgress = false;
                            Looper looper = getLooper();
                            for (Looper looper2 : new HashSet(TimeZoneUtils.mTZCallbacks.keySet())) {
                                HashSet<Runnable> hashSet = (HashSet) TimeZoneUtils.mTZCallbacks.get(looper2);
                                if (looper2 != looper) {
                                    TimeZoneUtils.this.postTZCallbackToOriginalThread(looper2);
                                } else {
                                    for (Runnable runnable : hashSet) {
                                        if (runnable != null) {
                                            runnable.run();
                                        }
                                    }
                                    TimeZoneUtils.mTZCallbacks.remove(looper2);
                                }
                            }
                            return;
                        }
                        LogUtil.w("CalendarUtils", "Some problem happened in Cursor, the index is set to -1, return directly.");
                        boolean unused6 = TimeZoneUtils.mTZQueryInProgress = false;
                        boolean unused7 = TimeZoneUtils.mFirstTZRequest = true;
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }

        public TimeZoneUtils(String str) {
            this.mPrefsName = str;
        }

        public String formatDateRange(Context context, long j, long j2, int i) {
            String timeZone;
            String string;
            if ((i & 8192) != 0) {
                timeZone = "UTC";
            } else {
                timeZone = getTimeZone(context, null);
            }
            String str = timeZone;
            synchronized (mSB) {
                mSB.setLength(0);
                string = DateUtils.formatDateRange(context, mF, j, j2, i, str).toString();
            }
            return string;
        }

        public void setTimeZone(Context context, String str) {
            boolean z;
            if (TextUtils.isEmpty(str)) {
                return;
            }
            synchronized (mTZCallbacks) {
                if ("auto".equals(str)) {
                    z = mUseHomeTZ;
                    mUseHomeTZ = false;
                } else {
                    boolean z2 = (mUseHomeTZ && TextUtils.equals(mHomeTZ, str)) ? false : true;
                    mUseHomeTZ = true;
                    mHomeTZ = str;
                    z = z2;
                }
            }
            if (z) {
                SharedPreferences sharedPreferences = CalendarUtils.getSharedPreferences(context, this.mPrefsName);
                CalendarUtils.setSharedPreference(sharedPreferences, "preferences_home_tz_enabled", mUseHomeTZ);
                CalendarUtils.setSharedPreference(sharedPreferences, "preferences_home_tz", mHomeTZ);
                ContentValues contentValues = new ContentValues();
                if (mHandler != null) {
                    mHandler.cancelOperation(mToken);
                }
                mHandler = new AsyncTZHandler(context.getContentResolver());
                int i = mToken + 1;
                mToken = i;
                if (i == 0) {
                    mToken = 1;
                }
                contentValues.put("value", mUseHomeTZ ? "home" : "auto");
                mHandler.startUpdate(mToken, null, CalendarContract.CalendarCache.URI, contentValues, "key=?", TIMEZONE_TYPE_ARGS);
                if (mUseHomeTZ) {
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.put("value", mHomeTZ);
                    mHandler.startUpdate(mToken, null, CalendarContract.CalendarCache.URI, contentValues2, "key=?", TIMEZONE_INSTANCES_ARGS);
                }
            }
        }

        public String getTimeZone(Context context, Runnable runnable) {
            synchronized (mTZCallbacks) {
                if (mFirstTZRequest) {
                    SharedPreferences sharedPreferences = CalendarUtils.getSharedPreferences(context, this.mPrefsName);
                    mUseHomeTZ = sharedPreferences.getBoolean("preferences_home_tz_enabled", false);
                    mHomeTZ = sharedPreferences.getString("preferences_home_tz", Time.getCurrentTimezone());
                    if (Looper.myLooper() != null) {
                        mTZQueryInProgress = true;
                        mFirstTZRequest = false;
                        if (mHandler == null) {
                            mHandler = new AsyncTZHandler(context.getContentResolver());
                        }
                        mHandler.startQuery(0, context, CalendarContract.CalendarCache.URI, CALENDAR_CACHE_POJECTION, null, null, null);
                    }
                }
                if (mTZQueryInProgress) {
                    enqueueTimeZoneCallback(runnable);
                }
            }
            return mUseHomeTZ ? mHomeTZ : Time.getCurrentTimezone();
        }

        private void enqueueTimeZoneCallback(Runnable runnable) {
            Looper looperMyLooper = Looper.myLooper();
            HashSet<Runnable> hashSet = mTZCallbacks.get(looperMyLooper);
            if (hashSet == null) {
                hashSet = new HashSet<>();
                mTZCallbacks.put(looperMyLooper, hashSet);
            }
            hashSet.add(runnable);
        }

        private void postTZCallbackToOriginalThread(Looper looper) {
            Handler handler = new Handler(looper) {
                @Override
                public void handleMessage(Message message) {
                    if (message.what == 1) {
                        synchronized (TimeZoneUtils.mTZCallbacks) {
                            if (((Looper) message.obj) != getLooper()) {
                                Log.i("CalendarUtils", "sent to error looper, msg=" + message);
                            }
                            for (Runnable runnable : (HashSet) TimeZoneUtils.mTZCallbacks.get((Looper) message.obj)) {
                                if (runnable != null) {
                                    runnable.run();
                                }
                            }
                            TimeZoneUtils.mTZCallbacks.remove((Looper) message.obj);
                            Log.i("CalendarUtils", "handle time zone update in original thread, thread=" + Thread.currentThread());
                        }
                        return;
                    }
                    Log.i("CalendarUtils", "unkown message msg=" + message);
                }
            };
            handler.sendMessage(handler.obtainMessage(1, looper));
        }
    }

    public static void setSharedPreference(SharedPreferences sharedPreferences, String str, String str2) {
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.putString(str, str2);
        editorEdit.apply();
    }

    public static void setSharedPreference(SharedPreferences sharedPreferences, String str, boolean z) {
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.putBoolean(str, z);
        editorEdit.apply();
    }

    public static SharedPreferences getSharedPreferences(Context context, String str) {
        return context.getSharedPreferences(str, 0);
    }
}
