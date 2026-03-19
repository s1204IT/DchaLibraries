package com.android.calendar;

import android.app.Application;
import com.mediatek.calendar.InjectedServices;

public class CalendarApplication extends Application {
    private static InjectedServices sInjectedServices;

    @Override
    public void onCreate() {
        super.onCreate();
        GeneralPreferences.setDefaultValues(this);
        Utils.setSharedPreference(this, "preferences_version", Utils.getVersionCode(this));
        ExtensionsFactory.init(getAssets());
    }

    @Override
    public Object getSystemService(String str) {
        Object systemService;
        if (sInjectedServices != null && (systemService = sInjectedServices.getSystemService(str)) != null) {
            return systemService;
        }
        return super.getSystemService(str);
    }
}
