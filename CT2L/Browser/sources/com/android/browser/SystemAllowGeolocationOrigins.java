package com.android.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import java.util.HashSet;
import java.util.Set;

class SystemAllowGeolocationOrigins {
    private final Context mContext;
    private Runnable mMaybeApplySetting = new Runnable() {
        @Override
        public void run() {
            String newSetting = SystemAllowGeolocationOrigins.this.getSystemSetting();
            SharedPreferences preferences = BrowserSettings.getInstance().getPreferences();
            String lastReadSetting = preferences.getString("last_read_allow_geolocation_origins", "");
            if (!TextUtils.equals(lastReadSetting, newSetting)) {
                preferences.edit().putString("last_read_allow_geolocation_origins", newSetting).apply();
                Set<String> oldOrigins = SystemAllowGeolocationOrigins.parseAllowGeolocationOrigins(lastReadSetting);
                Set<String> newOrigins = SystemAllowGeolocationOrigins.parseAllowGeolocationOrigins(newSetting);
                Set<String> addedOrigins = SystemAllowGeolocationOrigins.this.setMinus(newOrigins, oldOrigins);
                Set<String> removedOrigins = SystemAllowGeolocationOrigins.this.setMinus(oldOrigins, newOrigins);
                SystemAllowGeolocationOrigins.this.removeOrigins(removedOrigins);
                SystemAllowGeolocationOrigins.this.addOrigins(addedOrigins);
            }
        }
    };
    private final SettingObserver mSettingObserver = new SettingObserver();

    public SystemAllowGeolocationOrigins(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public void start() {
        Uri uri = Settings.Secure.getUriFor("allowed_geolocation_origins");
        this.mContext.getContentResolver().registerContentObserver(uri, false, this.mSettingObserver);
        maybeApplySettingAsync();
    }

    public void stop() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingObserver);
    }

    void maybeApplySettingAsync() {
        BackgroundHandler.execute(this.mMaybeApplySetting);
    }

    private static HashSet<String> parseAllowGeolocationOrigins(String setting) {
        HashSet<String> origins = new HashSet<>();
        if (!TextUtils.isEmpty(setting)) {
            String[] arr$ = setting.split("\\s+");
            for (String origin : arr$) {
                if (!TextUtils.isEmpty(origin)) {
                    origins.add(origin);
                }
            }
        }
        return origins;
    }

    private <A> Set<A> setMinus(Set<A> x, Set<A> y) {
        HashSet<A> z = new HashSet<>(x.size());
        for (A a : x) {
            if (!y.contains(a)) {
                z.add(a);
            }
        }
        return z;
    }

    private String getSystemSetting() {
        String value = Settings.Secure.getString(this.mContext.getContentResolver(), "allowed_geolocation_origins");
        return value == null ? "" : value;
    }

    private void addOrigins(Set<String> origins) {
        for (String origin : origins) {
            GeolocationPermissions.getInstance().allow(origin);
        }
    }

    private void removeOrigins(Set<String> origins) {
        for (final String origin : origins) {
            GeolocationPermissions.getInstance().getAllowed(origin, new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    if (value != null && value.booleanValue()) {
                        GeolocationPermissions.getInstance().clear(origin);
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
        public void onChange(boolean selfChange) {
            SystemAllowGeolocationOrigins.this.maybeApplySettingAsync();
        }
    }
}
