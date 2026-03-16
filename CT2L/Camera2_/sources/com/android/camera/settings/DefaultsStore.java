package com.android.camera.settings;

import java.util.HashMap;

class DefaultsStore {
    private static HashMap<String, Defaults> mDefaultsInternalStore = new HashMap<>();

    DefaultsStore() {
    }

    private static class Defaults {
        private String mDefaultValue;
        private String[] mPossibleValues;

        public Defaults(String defaultValue, String[] possibleValues) {
            this.mDefaultValue = defaultValue;
            this.mPossibleValues = possibleValues;
        }

        public String getDefaultValue() {
            return this.mDefaultValue;
        }

        public String[] getPossibleValues() {
            return this.mPossibleValues;
        }
    }

    public void storeDefaults(String key, String defaultValue, String[] possibleValues) {
        Defaults defaults = new Defaults(defaultValue, possibleValues);
        mDefaultsInternalStore.put(key, defaults);
    }

    public String getDefaultValue(String key) {
        Defaults defaults = mDefaultsInternalStore.get(key);
        if (defaults == null) {
            return null;
        }
        return defaults.getDefaultValue();
    }

    public String[] getPossibleValues(String key) {
        Defaults defaults = mDefaultsInternalStore.get(key);
        if (defaults == null) {
            return null;
        }
        return defaults.getPossibleValues();
    }
}
