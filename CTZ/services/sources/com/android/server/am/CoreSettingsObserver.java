package com.android.server.am;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.provider.Settings;
import com.android.internal.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class CoreSettingsObserver extends ContentObserver {
    private final ActivityManagerService mActivityManagerService;
    private final Bundle mCoreSettings;
    private static final String LOG_TAG = CoreSettingsObserver.class.getSimpleName();

    @VisibleForTesting
    static final Map<String, Class<?>> sSecureSettingToTypeMap = new HashMap();

    @VisibleForTesting
    static final Map<String, Class<?>> sSystemSettingToTypeMap = new HashMap();

    @VisibleForTesting
    static final Map<String, Class<?>> sGlobalSettingToTypeMap = new HashMap();

    static {
        sSecureSettingToTypeMap.put("long_press_timeout", Integer.TYPE);
        sSecureSettingToTypeMap.put("multi_press_timeout", Integer.TYPE);
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
    public void onChange(boolean z) {
        synchronized (this.mActivityManagerService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                sendCoreSettings();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    private void sendCoreSettings() {
        populateSettings(this.mCoreSettings, sSecureSettingToTypeMap);
        populateSettings(this.mCoreSettings, sSystemSettingToTypeMap);
        populateSettings(this.mCoreSettings, sGlobalSettingToTypeMap);
        this.mActivityManagerService.onCoreSettingsChange(this.mCoreSettings);
    }

    private void beginObserveCoreSettings() {
        Iterator<String> it = sSecureSettingToTypeMap.keySet().iterator();
        while (it.hasNext()) {
            this.mActivityManagerService.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(it.next()), false, this);
        }
        Iterator<String> it2 = sSystemSettingToTypeMap.keySet().iterator();
        while (it2.hasNext()) {
            this.mActivityManagerService.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(it2.next()), false, this);
        }
        Iterator<String> it3 = sGlobalSettingToTypeMap.keySet().iterator();
        while (it3.hasNext()) {
            this.mActivityManagerService.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(it3.next()), false, this);
        }
    }

    @VisibleForTesting
    void populateSettings(Bundle bundle, Map<String, Class<?>> map) {
        String string;
        Context context = this.mActivityManagerService.mContext;
        for (Map.Entry<String, Class<?>> entry : map.entrySet()) {
            String key = entry.getKey();
            if (map == sSecureSettingToTypeMap) {
                string = Settings.Secure.getString(context.getContentResolver(), key);
            } else if (map == sSystemSettingToTypeMap) {
                string = Settings.System.getString(context.getContentResolver(), key);
            } else {
                string = Settings.Global.getString(context.getContentResolver(), key);
            }
            if (string != null) {
                Class<?> value = entry.getValue();
                if (value == String.class) {
                    bundle.putString(key, string);
                } else if (value == Integer.TYPE) {
                    bundle.putInt(key, Integer.parseInt(string));
                } else if (value == Float.TYPE) {
                    bundle.putFloat(key, Float.parseFloat(string));
                } else if (value == Long.TYPE) {
                    bundle.putLong(key, Long.parseLong(string));
                }
            }
        }
    }
}
