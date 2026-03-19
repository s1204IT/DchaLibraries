package com.mediatek.camera.common.relation;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataStore {
    private final Context mContext;
    private final String mPackageName;
    private final Object mLock = new Object();
    private final Map<String, SharedPreferencesWrapper> mPrefWrapperMap = new ConcurrentHashMap();
    private final CopyOnWriteArrayList<String> mGlobalKeys = new CopyOnWriteArrayList<>();

    public DataStore(Context context) {
        this.mContext = context;
        this.mPackageName = this.mContext.getPackageName();
    }

    public String getGlobalScope() {
        return "_global_scope";
    }

    public String getCameraScope(int i) {
        return "_preferences_" + i;
    }

    public void setValue(String str, String str2, String str3, boolean z) {
        setValue(str, str2, str3, z, false);
    }

    public void setValue(String str, String str2, String str3, boolean z, boolean z2) {
        if ("_global_scope".equals(str3)) {
            this.mGlobalKeys.add(str);
        }
        getSharedPreferencesWrapperSync(str3).setValue(str, str2, z);
        if (z2) {
            getSharedPreferencesWrapperSync(str3 + "_saving_timestamp").setValue(str, String.valueOf(System.currentTimeMillis()), false);
        }
    }

    public String getValue(String str, String str2, String str3) {
        if (this.mGlobalKeys.contains(str)) {
            str3 = "_global_scope";
        }
        return getSharedPreferencesWrapperSync(str3).getValue(str, str2);
    }

    public List<String> getSettingsKeepSavingTime(int i) {
        SharedPreferencesWrapper sharedPreferencesWrapperSync = getSharedPreferencesWrapperSync(getCameraScope(i) + "_saving_timestamp");
        SharedPreferencesWrapper sharedPreferencesWrapperSync2 = getSharedPreferencesWrapperSync("_global_scope_saving_timestamp");
        LinkedList linkedList = new LinkedList();
        LinkedList linkedList2 = new LinkedList();
        sortSettingByTimestamp(sharedPreferencesWrapperSync.getAll(), linkedList, linkedList2);
        sortSettingByTimestamp(sharedPreferencesWrapperSync2.getAll(), linkedList, linkedList2);
        return linkedList;
    }

    private SharedPreferencesWrapper getSharedPreferencesWrapperSync(String str) {
        SharedPreferencesWrapper sharedPreferencesWrapper;
        synchronized (this.mLock) {
            sharedPreferencesWrapper = this.mPrefWrapperMap.get(str);
            if (sharedPreferencesWrapper == null) {
                sharedPreferencesWrapper = new SharedPreferencesWrapper(str);
                this.mPrefWrapperMap.put(str, sharedPreferencesWrapper);
            }
        }
        return sharedPreferencesWrapper;
    }

    private void sortSettingByTimestamp(Map<String, ?> map, List<String> list, List<Long> list2) {
        for (String str : map.keySet()) {
            Long lValueOf = Long.valueOf(Long.parseLong((String) map.get(str)));
            int size = 0;
            while (true) {
                if (size >= list2.size()) {
                    size = -1;
                    break;
                } else if (lValueOf.longValue() > list2.get(size).longValue()) {
                    break;
                } else {
                    size++;
                }
            }
            if (size == -1) {
                size = list.size();
            }
            list.add(size, str);
            list2.add(size, lValueOf);
        }
    }

    private class SharedPreferencesWrapper {
        private final String mScope;
        private final SharedPreferences mSharedPreferences;
        private final Map<String, String> mValueCache = new ConcurrentHashMap();

        SharedPreferencesWrapper(String str) {
            this.mScope = str;
            this.mSharedPreferences = getPreferencesFromScope(this.mScope);
        }

        void setValue(String str, String str2, boolean z) {
            if (z) {
                this.mValueCache.put(str, str2);
            } else {
                this.mSharedPreferences.edit().putString(str, str2).apply();
            }
        }

        public String getValue(String str, String str2) {
            if (this.mValueCache.containsKey(str)) {
                return this.mValueCache.get(str);
            }
            return this.mSharedPreferences.getString(str, str2);
        }

        public Map<String, ?> getAll() {
            return this.mSharedPreferences.getAll();
        }

        private SharedPreferences getPreferencesFromScope(String str) {
            if (str.equals("_global_scope")) {
                return PreferenceManager.getDefaultSharedPreferences(DataStore.this.mContext);
            }
            return openPreferences(str);
        }

        private SharedPreferences openPreferences(String str) {
            return DataStore.this.mContext.getSharedPreferences(DataStore.this.mPackageName + str, 0);
        }
    }
}
