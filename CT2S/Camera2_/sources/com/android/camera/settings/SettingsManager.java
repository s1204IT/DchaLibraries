package com.android.camera.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.camera.debug.Log;
import com.android.camera.util.Size;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    public static final String SCOPE_GLOBAL = "default_scope";
    private static final Log.Tag TAG = new Log.Tag("SettingsManager");
    private final Context mContext;
    private SharedPreferences mCustomPreferences;
    private final SharedPreferences mDefaultPreferences;
    private final String mPackageName;
    private final DefaultsStore mDefaultsStore = new DefaultsStore();
    private final List<OnSettingChangedListener> mListeners = new ArrayList();
    private final List<SharedPreferences.OnSharedPreferenceChangeListener> mSharedPreferenceListeners = new ArrayList();

    public interface OnSettingChangedListener {
        void onSettingChanged(SettingsManager settingsManager, String str);
    }

    public SettingsManager(Context context) {
        this.mContext = context;
        this.mPackageName = this.mContext.getPackageName();
        this.mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
    }

    public SharedPreferences getDefaultPreferences() {
        return this.mDefaultPreferences;
    }

    protected SharedPreferences openPreferences(String scope) {
        SharedPreferences preferences = this.mContext.getSharedPreferences(this.mPackageName + scope, 0);
        for (SharedPreferences.OnSharedPreferenceChangeListener listener : this.mSharedPreferenceListeners) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
        return preferences;
    }

    protected void closePreferences(SharedPreferences preferences) {
        for (SharedPreferences.OnSharedPreferenceChangeListener listener : this.mSharedPreferenceListeners) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    private SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceListener(final OnSettingChangedListener listener) {
        return new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                listener.onSettingChanged(SettingsManager.this, key);
            }
        };
    }

    public void addListener(OnSettingChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("OnSettingChangedListener cannot be null.");
        }
        if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
            SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceListener = getSharedPreferenceListener(listener);
            this.mSharedPreferenceListeners.add(sharedPreferenceListener);
            this.mDefaultPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceListener);
            if (this.mCustomPreferences != null) {
                this.mCustomPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceListener);
            }
            Log.v(TAG, "listeners: " + this.mListeners);
        }
    }

    public void removeListener(OnSettingChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException();
        }
        if (this.mListeners.contains(listener)) {
            int index = this.mListeners.indexOf(listener);
            this.mListeners.remove(listener);
            SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceListener = this.mSharedPreferenceListeners.get(index);
            this.mSharedPreferenceListeners.remove(index);
            this.mDefaultPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener);
            if (this.mCustomPreferences != null) {
                this.mCustomPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener);
            }
        }
    }

    public void removeAllListeners() {
        for (SharedPreferences.OnSharedPreferenceChangeListener listener : this.mSharedPreferenceListeners) {
            this.mDefaultPreferences.unregisterOnSharedPreferenceChangeListener(listener);
            if (this.mCustomPreferences != null) {
                this.mCustomPreferences.unregisterOnSharedPreferenceChangeListener(listener);
            }
        }
        this.mSharedPreferenceListeners.clear();
        this.mListeners.clear();
    }

    private SharedPreferences getPreferencesFromScope(String scope) {
        if (scope.equals(SCOPE_GLOBAL)) {
            return this.mDefaultPreferences;
        }
        if (this.mCustomPreferences != null) {
            closePreferences(this.mCustomPreferences);
        }
        this.mCustomPreferences = openPreferences(scope);
        return this.mCustomPreferences;
    }

    public void setDefaults(String key, String defaultValue, String[] possibleValues) {
        this.mDefaultsStore.storeDefaults(key, defaultValue, possibleValues);
    }

    public void setDefaults(String key, int defaultValue, int[] possibleValues) {
        String defaultValueString = Integer.toString(defaultValue);
        String[] possibleValuesString = new String[possibleValues.length];
        for (int i = 0; i < possibleValues.length; i++) {
            possibleValuesString[i] = Integer.toString(possibleValues[i]);
        }
        this.mDefaultsStore.storeDefaults(key, defaultValueString, possibleValuesString);
    }

    public void setDefaults(String key, boolean defaultValue) {
        String defaultValueString = defaultValue ? "1" : "0";
        String[] possibleValues = {"0", "1"};
        this.mDefaultsStore.storeDefaults(key, defaultValueString, possibleValues);
    }

    public String getStringDefault(String key) {
        return this.mDefaultsStore.getDefaultValue(key);
    }

    public Integer getIntegerDefault(String key) {
        String defaultValueString = this.mDefaultsStore.getDefaultValue(key);
        return Integer.valueOf(defaultValueString == null ? 0 : Integer.parseInt(defaultValueString));
    }

    public boolean getBooleanDefault(String key) {
        String defaultValueString = this.mDefaultsStore.getDefaultValue(key);
        return (defaultValueString == null || Integer.parseInt(defaultValueString) == 0) ? false : true;
    }

    public String getString(String scope, String key, String defaultValue) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        try {
            return preferences.getString(key, defaultValue);
        } catch (ClassCastException e) {
            Log.w(TAG, "existing preference with invalid type, removing and returning default", e);
            preferences.edit().remove(key).apply();
            return defaultValue;
        }
    }

    public String getString(String scope, String key) {
        return getString(scope, key, getStringDefault(key));
    }

    public Integer getInteger(String scope, String key, Integer defaultValue) {
        String defaultValueString = Integer.toString(defaultValue.intValue());
        String value = getString(scope, key, defaultValueString);
        return Integer.valueOf(convertToInt(value));
    }

    public Integer getInteger(String scope, String key) {
        return getInteger(scope, key, getIntegerDefault(key));
    }

    public boolean getBoolean(String scope, String key, boolean defaultValue) {
        String defaultValueString = defaultValue ? "1" : "0";
        String value = getString(scope, key, defaultValueString);
        return convertToBoolean(value);
    }

    public boolean getBoolean(String scope, String key) {
        return getBoolean(scope, key, getBooleanDefault(key));
    }

    public Size getSize(String scope, String key) {
        String strValue = getString(scope, key);
        if (strValue == null) {
            return null;
        }
        String[] widthHeight = strValue.split("x");
        if (widthHeight.length != 2) {
            return null;
        }
        try {
            int width = Integer.parseInt(widthHeight[0]);
            int height = Integer.parseInt(widthHeight[1]);
            return new Size(width, height);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int getIndexOfCurrentValue(String scope, String key) {
        String[] possibleValues = this.mDefaultsStore.getPossibleValues(key);
        if (possibleValues == null || possibleValues.length == 0) {
            throw new IllegalArgumentException("No possible values for scope=" + scope + " key=" + key);
        }
        String value = getString(scope, key);
        for (int i = 0; i < possibleValues.length; i++) {
            if (value.equals(possibleValues[i])) {
                return i;
            }
        }
        throw new IllegalStateException("Current value for scope=" + scope + " key=" + key + " not in list of possible values");
    }

    public void set(String scope, String key, String value) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        preferences.edit().putString(key, value).apply();
    }

    public void set(String scope, String key, int value) {
        set(scope, key, convert(value));
    }

    public void set(String scope, String key, boolean value) {
        set(scope, key, convert(value));
    }

    public void setToDefault(String scope, String key) {
        set(scope, key, getStringDefault(key));
    }

    public void setValueByIndex(String scope, String key, int index) {
        String[] possibleValues = this.mDefaultsStore.getPossibleValues(key);
        if (possibleValues.length == 0) {
            throw new IllegalArgumentException("No possible values for scope=" + scope + " key=" + key);
        }
        if (index >= 0 && index < possibleValues.length) {
            set(scope, key, possibleValues[index]);
            return;
        }
        throw new IndexOutOfBoundsException("For possible values of scope=" + scope + " key=" + key);
    }

    public boolean isSet(String scope, String key) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        return preferences.contains(key);
    }

    public boolean isDefault(String scope, String key) {
        String defaultValue = getStringDefault(key);
        String value = getString(scope, key);
        if (value == null) {
            return false;
        }
        return value.equals(defaultValue);
    }

    public void remove(String scope, String key) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        preferences.edit().remove(key).apply();
    }

    static String convert(int value) {
        return Integer.toString(value);
    }

    static int convertToInt(String value) {
        return Integer.parseInt(value);
    }

    static boolean convertToBoolean(String value) {
        return Integer.parseInt(value) != 0;
    }

    static String convert(boolean value) {
        return value ? "1" : "0";
    }
}
