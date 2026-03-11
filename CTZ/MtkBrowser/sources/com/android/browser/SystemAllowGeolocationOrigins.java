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
    private Runnable mMaybeApplySetting = new Runnable(this) {
        final SystemAllowGeolocationOrigins this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void run() {
            String systemSetting = this.this$0.getSystemSetting();
            SharedPreferences preferences = BrowserSettings.getInstance().getPreferences();
            String string = preferences.getString("last_read_allow_geolocation_origins", "");
            if (TextUtils.equals(string, systemSetting)) {
                return;
            }
            preferences.edit().putString("last_read_allow_geolocation_origins", systemSetting).apply();
            HashSet allowGeolocationOrigins = SystemAllowGeolocationOrigins.parseAllowGeolocationOrigins(string);
            HashSet allowGeolocationOrigins2 = SystemAllowGeolocationOrigins.parseAllowGeolocationOrigins(systemSetting);
            Set minus = this.this$0.setMinus(allowGeolocationOrigins2, allowGeolocationOrigins);
            this.this$0.removeOrigins(this.this$0.setMinus(allowGeolocationOrigins, allowGeolocationOrigins2));
            this.this$0.addOrigins(minus);
        }
    };
    private final SettingObserver mSettingObserver = new SettingObserver(this);

    private class SettingObserver extends ContentObserver {
        final SystemAllowGeolocationOrigins this$0;

        SettingObserver(SystemAllowGeolocationOrigins systemAllowGeolocationOrigins) {
            super(new Handler());
            this.this$0 = systemAllowGeolocationOrigins;
        }

        @Override
        public void onChange(boolean z) {
            this.this$0.maybeApplySettingAsync();
        }
    }

    public SystemAllowGeolocationOrigins(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public void addOrigins(Set<String> set) {
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            GeolocationPermissions.getInstance().allow(it.next());
        }
    }

    public String getSystemSetting() {
        String string = Settings.Secure.getString(this.mContext.getContentResolver(), "allowed_geolocation_origins");
        return string == null ? "" : string;
    }

    public static HashSet<String> parseAllowGeolocationOrigins(String str) {
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

    public void removeOrigins(Set<String> set) {
        for (String str : set) {
            GeolocationPermissions.getInstance().getAllowed(str, new ValueCallback<Boolean>(this, str) {
                final SystemAllowGeolocationOrigins this$0;
                final String val$origin;

                {
                    this.this$0 = this;
                    this.val$origin = str;
                }

                @Override
                public void onReceiveValue(Boolean bool) {
                    if (bool == null || !bool.booleanValue()) {
                        return;
                    }
                    GeolocationPermissions.getInstance().clear(this.val$origin);
                }
            });
        }
    }

    public <A> Set<A> setMinus(Set<A> set, Set<A> set2) {
        HashSet hashSet = new HashSet(set.size());
        for (A a : set) {
            if (!set2.contains(a)) {
                hashSet.add(a);
            }
        }
        return hashSet;
    }

    void maybeApplySettingAsync() {
        BackgroundHandler.execute(this.mMaybeApplySetting);
    }

    public void start() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("allowed_geolocation_origins"), false, this.mSettingObserver);
        maybeApplySettingAsync();
    }

    public void stop() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingObserver);
    }
}
