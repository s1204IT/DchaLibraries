package com.android.wallpaper.polarclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.wallpaper.WallpaperSettingsActivity;
import com.android.wallpaper.R;

public class PolarClockSettings extends WallpaperSettingsActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName("polar_clock_settings");
        addPreferencesFromResource(R.xml.polar_clock_prefs);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    }
}
