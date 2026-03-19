package com.android.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class SystemAllowGeolocationOrigins {
    private final Context mContext;
    private Runnable mMaybeApplySetting = new Runnable() {
        @Override
        public void run() {
            String systemSetting = SystemAllowGeolocationOrigins.this.getSystemSetting();
            SharedPreferences preferences = BrowserSettings.getInstance().getPreferences();
            String string = preferences.getString("last_read_allow_geolocation_origins", "");
            if (TextUtils.equals(string, systemSetting)) {
                return;
            }
            preferences.edit().putString("last_read_allow_geolocation_origins", systemSetting).apply();
            HashSet allowGeolocationOrigins = SystemAllowGeolocationOrigins.parseAllowGeolocationOrigins(string);
            HashSet allowGeolocationOrigins2 = SystemAllowGeolocationOrigins.parseAllowGeolocationOrigins(systemSetting);
            Set minus = SystemAllowGeolocationOrigins.this.setMinus(allowGeolocationOrigins2, allowGeolocationOrigins);
            SystemAllowGeolocationOrigins.this.removeOrigins(SystemAllowGeolocationOrigins.this.setMinus(allowGeolocationOrigins, allowGeolocationOrigins2));
            SystemAllowGeolocationOrigins.this.addOrigins(minus);
        }
    };
    private final SettingObserver mSettingObserver = new SettingObserver();

    public SystemAllowGeolocationOrigins(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public void start() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("allowed_geolocation_origins"), false, this.mSettingObserver);
        maybeApplySettingAsync();
    }

    public void stop() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingObserver);
    }

    void maybeApplySettingAsync() {
        BackgroundHandler.execute(this.mMaybeApplySetting);
    }

    private static HashSet<String> parseAllowGeolocationOrigins(String str) {
        HashSet<String> hashSet = new HashSet<>();
        if (!TextUtils.isEmpty(str)) {
            for (String str2 : str.split("\\s+")) {
                if (!TextUtils.isEmpty(str2)) {
                    hashSet.add(str2);
                }
            }
        }
        return hashSet;
    }

    private <A> Set<A> setMinus(Set<A> set, Set<A> set2) {
        HashSet hashSet = new HashSet(set.size());
        for (A a : set) {
            if (!set2.contains(a)) {
                hashSet.add(a);
            }
        }
        return hashSet;
    }

    private String getSystemSetting() {
        String string = Settings.Secure.getString(this.mContext.getContentResolver(), "allowed_geolocation_origins");
        return string == null ? "" : string;
    }

    private void addOrigins(Set<String> set) {
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            GeolocationPermissions.getInstance().allow(it.next());
        }
    }

    private void removeOrigins(Set<String> set) {
        for (final String str : set) {
            GeolocationPermissions.getInstance().getAllowed(str, new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean bool) {
                    if (bool != null && bool.booleanValue()) {
                        GeolocationPermissions.getInstance().clear(str);
                    }
                }
            });
        }
    }

    private class SettingObserver extends ContentObserver {
        SettingObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean z) {
            SystemAllowGeolocationOrigins.this.maybeApplySettingAsync();
        }
    }
}
