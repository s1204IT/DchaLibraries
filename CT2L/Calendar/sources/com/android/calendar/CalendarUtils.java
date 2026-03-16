package com.android.calendar;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Looper;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import java.util.Formatter;
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
        private static HashSet<Runnable> mTZCallbacks = new HashSet<>();
        private static int mToken = 1;

        private class AsyncTZHandler extends AsyncQueryHandler {
            public AsyncTZHandler(ContentResolver cr) {
                super(cr);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                synchronized (TimeZoneUtils.mTZCallbacks) {
                    if (cursor == null) {
                        boolean unused = TimeZoneUtils.mTZQueryInProgress = false;
                        boolean unused2 = TimeZoneUtils.mFirstTZRequest = true;
                        return;
                    }
                    boolean writePrefs = false;
                    int keyColumn = cursor.getColumnIndexOrThrow("key");
                    int valueColumn = cursor.getColumnIndexOrThrow("value");
                    while (cursor.moveToNext()) {
                        String key = cursor.getString(keyColumn);
                        String value = cursor.getString(valueColumn);
                        if (TextUtils.equals(key, "timezoneType")) {
                            boolean useHomeTZ = !TextUtils.equals(value, "auto");
                            if (useHomeTZ != TimeZoneUtils.mUseHomeTZ) {
                                writePrefs = true;
                                boolean unused3 = TimeZoneUtils.mUseHomeTZ = useHomeTZ;
                            }
                        } else if (TextUtils.equals(key, "timezoneInstancesPrevious") && !TextUtils.isEmpty(value) && !TextUtils.equals(TimeZoneUtils.mHomeTZ, value)) {
                            writePrefs = true;
                            String unused4 = TimeZoneUtils.mHomeTZ = value;
                        }
                    }
                    cursor.close();
                    if (writePrefs) {
                        SharedPreferences prefs = CalendarUtils.getSharedPreferences((Context) cookie, TimeZoneUtils.this.mPrefsName);
                        CalendarUtils.setSharedPreference(prefs, "preferences_home_tz_enabled", TimeZoneUtils.mUseHomeTZ);
                        CalendarUtils.setSharedPreference(prefs, "preferences_home_tz", TimeZoneUtils.mHomeTZ);
                    }
                    boolean unused5 = TimeZoneUtils.mTZQueryInProgress = false;
                    for (Runnable callback : TimeZoneUtils.mTZCallbacks) {
                        if (callback != null) {
                            callback.run();
                        }
                    }
                    TimeZoneUtils.mTZCallbacks.clear();
                }
            }
        }

        public TimeZoneUtils(String prefsName) {
            this.mPrefsName = prefsName;
        }

        public String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
            String tz;
            String date;
            if ((flags & 8192) != 0) {
                tz = "UTC";
            } else {
                tz = getTimeZone(context, null);
            }
            synchronized (mSB) {
                mSB.setLength(0);
                date = DateUtils.formatDateRange(context, mF, startMillis, endMillis, flags, tz).toString();
            }
            return date;
        }

        public void setTimeZone(Context context, String timeZone) {
            if (!TextUtils.isEmpty(timeZone)) {
                boolean updatePrefs = false;
                synchronized (mTZCallbacks) {
                    if ("auto".equals(timeZone)) {
                        if (mUseHomeTZ) {
                            updatePrefs = true;
                        }
                        mUseHomeTZ = false;
                    } else {
                        if (!mUseHomeTZ || !TextUtils.equals(mHomeTZ, timeZone)) {
                            updatePrefs = true;
                        }
                        mUseHomeTZ = true;
                        mHomeTZ = timeZone;
                    }
                }
                if (updatePrefs) {
                    SharedPreferences prefs = CalendarUtils.getSharedPreferences(context, this.mPrefsName);
                    CalendarUtils.setSharedPreference(prefs, "preferences_home_tz_enabled", mUseHomeTZ);
                    CalendarUtils.setSharedPreference(prefs, "preferences_home_tz", mHomeTZ);
                    ContentValues values = new ContentValues();
                    if (mHandler != null) {
                        mHandler.cancelOperation(mToken);
                    }
                    mHandler = new AsyncTZHandler(context.getContentResolver());
                    int i = mToken + 1;
                    mToken = i;
                    if (i == 0) {
                        mToken = 1;
                    }
                    values.put("value", mUseHomeTZ ? "home" : "auto");
                    mHandler.startUpdate(mToken, null, CalendarContract.CalendarCache.URI, values, "key=?", TIMEZONE_TYPE_ARGS);
                    if (mUseHomeTZ) {
                        ContentValues values2 = new ContentValues();
                        values2.put("value", mHomeTZ);
                        mHandler.startUpdate(mToken, null, CalendarContract.CalendarCache.URI, values2, "key=?", TIMEZONE_INSTANCES_ARGS);
                    }
                }
            }
        }

        public String getTimeZone(Context context, Runnable callback) {
            synchronized (mTZCallbacks) {
                if (mFirstTZRequest) {
                    SharedPreferences prefs = CalendarUtils.getSharedPreferences(context, this.mPrefsName);
                    mUseHomeTZ = prefs.getBoolean("preferences_home_tz_enabled", false);
                    mHomeTZ = prefs.getString("preferences_home_tz", Time.getCurrentTimezone());
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
                    mTZCallbacks.add(callback);
                }
            }
            return mUseHomeTZ ? mHomeTZ : Time.getCurrentTimezone();
        }
    }

    public static void setSharedPreference(SharedPreferences prefs, String key, String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void setSharedPreference(SharedPreferences prefs, String key, boolean value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static SharedPreferences getSharedPreferences(Context context, String prefsName) {
        return context.getSharedPreferences(prefsName, 0);
    }
}
