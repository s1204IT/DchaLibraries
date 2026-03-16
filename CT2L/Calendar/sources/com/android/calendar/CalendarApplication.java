package com.android.calendar;

import android.app.Application;

public class CalendarApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        GeneralPreferences.setDefaultValues(this);
        Utils.setSharedPreference(this, "preferences_version", Utils.getVersionCode(this));
        ExtensionsFactory.init(getAssets());
    }
}
