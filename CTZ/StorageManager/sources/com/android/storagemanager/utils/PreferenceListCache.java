package com.android.storagemanager.utils;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.ArrayMap;
import java.util.Iterator;

public class PreferenceListCache {
    private ArrayMap<String, Preference> mCache = new ArrayMap<>();
    private PreferenceGroup mGroup;

    public PreferenceListCache(PreferenceGroup preferenceGroup) {
        this.mGroup = preferenceGroup;
        int preferenceCount = preferenceGroup.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = preferenceGroup.getPreference(i);
            String key = preference.getKey();
            if (TextUtils.isEmpty(key) || this.mCache.containsKey(key)) {
                throw new IllegalArgumentException("Invalid key encountered in preference group " + preferenceGroup.getKey());
            }
            this.mCache.put(preference.getKey(), preference);
        }
    }

    public Preference getCachedPreference(String str) {
        return this.mCache.remove(str);
    }

    public void removeCachedPrefs() {
        Iterator<Preference> it = this.mCache.values().iterator();
        while (it.hasNext()) {
            this.mGroup.removePreference(it.next());
        }
    }
}
