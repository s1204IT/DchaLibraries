package com.android.camera.settings;

import android.content.SharedPreferences;
import com.android.camera.debug.Log;

public abstract class SettingsUpgrader {
    protected static final String OLD_SETTINGS_VALUE_NONE = "none";
    protected static final String OLD_SETTINGS_VALUE_OFF = "off";
    protected static final String OLD_SETTINGS_VALUE_ON = "on";
    private static final Log.Tag TAG = new Log.Tag("SettingsUpgrader");
    private final int mTargetVersion;
    private final String mVersionKey;

    protected abstract void upgrade(SettingsManager settingsManager, int i, int i2);

    public SettingsUpgrader(String versionKey, int targetVersion) {
        this.mVersionKey = versionKey;
        this.mTargetVersion = targetVersion;
    }

    public void upgrade(SettingsManager settingsManager) {
        int lastVersion = getLastVersion(settingsManager);
        if (lastVersion != this.mTargetVersion) {
            upgrade(settingsManager, lastVersion, this.mTargetVersion);
        }
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, this.mVersionKey, this.mTargetVersion);
    }

    protected int getLastVersion(SettingsManager settingsManager) {
        return settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, this.mVersionKey).intValue();
    }

    protected boolean removeBoolean(SharedPreferences oldPreferencesLocation, String key) {
        boolean value = false;
        try {
            value = oldPreferencesLocation.getBoolean(key, false);
        } catch (ClassCastException e) {
            Log.e(TAG, "error reading old value, removing and returning default", e);
        }
        oldPreferencesLocation.edit().remove(key).apply();
        return value;
    }

    protected int removeInteger(SharedPreferences oldPreferencesLocation, String key) {
        int value = 0;
        try {
            value = oldPreferencesLocation.getInt(key, 0);
        } catch (ClassCastException e) {
            Log.e(TAG, "error reading old value, removing and returning default", e);
        }
        oldPreferencesLocation.edit().remove(key).apply();
        return value;
    }

    protected String removeString(SharedPreferences oldPreferencesLocation, String key) {
        String value = null;
        try {
            value = oldPreferencesLocation.getString(key, null);
        } catch (ClassCastException e) {
            Log.e(TAG, "error reading old value, removing and returning default", e);
        }
        oldPreferencesLocation.edit().remove(key).apply();
        return value;
    }
}
