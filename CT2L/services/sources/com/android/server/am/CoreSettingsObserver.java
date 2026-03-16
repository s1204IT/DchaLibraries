package com.android.server.am;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import java.util.HashMap;
import java.util.Map;

final class CoreSettingsObserver extends ContentObserver {
    private final ActivityManagerService mActivityManagerService;
    private final Bundle mCoreSettings;
    private static final String LOG_TAG = CoreSettingsObserver.class.getSimpleName();
    private static final Map<String, Class<?>> sSecureSettingToTypeMap = new HashMap();
    private static final Map<String, Class<?>> sSystemSettingToTypeMap = new HashMap();
    private static final Map<String, Class<?>> sGlobalSettingToTypeMap = new HashMap();

    static {
        sSecureSettingToTypeMap.put("long_press_timeout", Integer.TYPE);
        sSystemSettingToTypeMap.put("time_12_24", String.class);
        sGlobalSettingToTypeMap.put("debug_view_attributes", Integer.TYPE);
    }

    public CoreSettingsObserver(ActivityManagerService activityManagerService) {
        super(activityManagerService.mHandler);
        this.mCoreSettings = new Bundle();
        this.mActivityManagerService = activityManagerService;
        beginObserveCoreSettings();
        sendCoreSettings();
    }

    public Bundle getCoreSettingsLocked() {
        return (Bundle) this.mCoreSettings.clone();
    }

    @Override
    public void onChange(boolean selfChange) {
        synchronized (this.mActivityManagerService) {
            sendCoreSettings();
        }
    }

    private void sendCoreSettings() {
        populateSettings(this.mCoreSettings, sSecureSettingToTypeMap);
        populateSettings(this.mCoreSettings, sSystemSettingToTypeMap);
        populateSettings(this.mCoreSettings, sGlobalSettingToTypeMap);
        this.mActivityManagerService.onCoreSettingsChange(this.mCoreSettings);
    }

    private void beginObserveCoreSettings() {
        for (String setting : sSecureSettingToTypeMap.keySet()) {
            Uri uri = Settings.Secure.getUriFor(setting);
            this.mActivityManagerService.mContext.getContentResolver().registerContentObserver(uri, false, this);
        }
        for (String setting2 : sSystemSettingToTypeMap.keySet()) {
            Uri uri2 = Settings.System.getUriFor(setting2);
            this.mActivityManagerService.mContext.getContentResolver().registerContentObserver(uri2, false, this);
        }
        for (String setting3 : sGlobalSettingToTypeMap.keySet()) {
            Uri uri3 = Settings.Global.getUriFor(setting3);
            this.mActivityManagerService.mContext.getContentResolver().registerContentObserver(uri3, false, this);
        }
    }

    private void populateSettings(Bundle snapshot, Map<String, Class<?>> map) {
        String value;
        int value2;
        float value3;
        long value4;
        Context context = this.mActivityManagerService.mContext;
        for (Map.Entry<String, Class<?>> entry : map.entrySet()) {
            String setting = entry.getKey();
            Class<?> type = entry.getValue();
            if (type == String.class) {
                if (map == sSecureSettingToTypeMap) {
                    value = Settings.Secure.getString(context.getContentResolver(), setting);
                } else if (map == sSystemSettingToTypeMap) {
                    value = Settings.System.getString(context.getContentResolver(), setting);
                } else {
                    value = Settings.Global.getString(context.getContentResolver(), setting);
                }
                snapshot.putString(setting, value);
            } else if (type == Integer.TYPE) {
                if (map == sSecureSettingToTypeMap) {
                    value2 = Settings.Secure.getInt(context.getContentResolver(), setting, 0);
                } else if (map == sSystemSettingToTypeMap) {
                    value2 = Settings.System.getInt(context.getContentResolver(), setting, 0);
                } else {
                    value2 = Settings.Global.getInt(context.getContentResolver(), setting, 0);
                }
                snapshot.putInt(setting, value2);
            } else if (type == Float.TYPE) {
                if (map == sSecureSettingToTypeMap) {
                    value3 = Settings.Secure.getFloat(context.getContentResolver(), setting, 0.0f);
                } else if (map == sSystemSettingToTypeMap) {
                    value3 = Settings.System.getFloat(context.getContentResolver(), setting, 0.0f);
                } else {
                    value3 = Settings.Global.getFloat(context.getContentResolver(), setting, 0.0f);
                }
                snapshot.putFloat(setting, value3);
            } else if (type == Long.TYPE) {
                if (map == sSecureSettingToTypeMap) {
                    value4 = Settings.Secure.getLong(context.getContentResolver(), setting, 0L);
                } else if (map == sSystemSettingToTypeMap) {
                    value4 = Settings.System.getLong(context.getContentResolver(), setting, 0L);
                } else {
                    value4 = Settings.Global.getLong(context.getContentResolver(), setting, 0L);
                }
                snapshot.putLong(setting, value4);
            }
        }
    }
}
