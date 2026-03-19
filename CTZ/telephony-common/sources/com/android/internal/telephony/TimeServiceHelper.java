package com.android.internal.telephony;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

public class TimeServiceHelper {
    private static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    private final Context mContext;
    private final ContentResolver mCr;
    private Listener mListener;

    public interface Listener {
        void onTimeDetectionChange(boolean z);

        void onTimeZoneDetectionChange(boolean z);
    }

    public TimeServiceHelper(Context context) {
        this.mContext = context;
        this.mCr = context.getContentResolver();
    }

    public void setListener(final Listener listener) {
        if (listener == null) {
            throw new NullPointerException("listener==null");
        }
        if (this.mListener != null) {
            throw new IllegalStateException("listener already set");
        }
        this.mListener = listener;
        this.mCr.registerContentObserver(Settings.Global.getUriFor("auto_time"), true, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                listener.onTimeDetectionChange(TimeServiceHelper.this.isTimeDetectionEnabled());
            }
        });
        this.mCr.registerContentObserver(Settings.Global.getUriFor("auto_time_zone"), true, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                listener.onTimeZoneDetectionChange(TimeServiceHelper.this.isTimeZoneDetectionEnabled());
            }
        });
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    public boolean isTimeZoneSettingInitialized() {
        return isTimeZoneSettingInitializedStatic();
    }

    public boolean isTimeDetectionEnabled() {
        try {
            return Settings.Global.getInt(this.mCr, "auto_time") > 0;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    public boolean isTimeZoneDetectionEnabled() {
        try {
            return Settings.Global.getInt(this.mCr, "auto_time_zone") > 0;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    public void setDeviceTimeZone(String str) {
        setDeviceTimeZoneStatic(this.mContext, str);
    }

    public void setDeviceTime(long j) {
        SystemClock.setCurrentTimeMillis(j);
        Intent intent = new Intent("android.intent.action.NETWORK_SET_TIME");
        intent.addFlags(536870912);
        intent.putExtra("time", j);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    static boolean isTimeZoneSettingInitializedStatic() {
        String str = SystemProperties.get(TIMEZONE_PROPERTY);
        return (str == null || str.length() <= 0 || str.equals("GMT")) ? false : true;
    }

    static void setDeviceTimeZoneStatic(Context context, String str) {
        ((AlarmManager) context.getSystemService("alarm")).setTimeZone(str);
        Intent intent = new Intent("android.intent.action.NETWORK_SET_TIMEZONE");
        intent.addFlags(536870912);
        intent.putExtra("time-zone", str);
        context.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }
}
