package com.android.documentsui.prefs;

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import java.util.function.Consumer;

public final class PreferencesMonitor {
    private final Consumer<String> mChangeCallback;
    private final SharedPreferences.OnSharedPreferenceChangeListener mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
            this.f$0.onSharedPreferenceChanged(sharedPreferences, str);
        }
    };
    private final String mPackageName;
    private final SharedPreferences mPrefs;

    public PreferencesMonitor(String str, SharedPreferences sharedPreferences, Consumer<String> consumer) {
        this.mPackageName = str;
        this.mPrefs = sharedPreferences;
        this.mChangeCallback = consumer;
    }

    public void start() {
        this.mPrefs.registerOnSharedPreferenceChangeListener(this.mListener);
    }

    public void stop() {
        this.mPrefs.unregisterOnSharedPreferenceChangeListener(this.mListener);
    }

    void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        if (Preferences.shouldBackup(str)) {
            this.mChangeCallback.accept(str);
            BackupManager.dataChanged(this.mPackageName);
        }
    }
}
