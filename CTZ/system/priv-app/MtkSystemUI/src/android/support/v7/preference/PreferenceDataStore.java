package android.support.v7.preference;

import java.util.Set;
/* loaded from: classes.dex */
public abstract class PreferenceDataStore {
    public void putString(String key, String value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    public void putStringSet(String key, Set<String> values) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    public void putInt(String key, int value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    public void putBoolean(String key, boolean value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    public String getString(String key, String defValue) {
        return defValue;
    }

    public Set<String> getStringSet(String key, Set<String> defValues) {
        return defValues;
    }

    public int getInt(String key, int defValue) {
        return defValue;
    }

    public boolean getBoolean(String key, boolean defValue) {
        return defValue;
    }
}
