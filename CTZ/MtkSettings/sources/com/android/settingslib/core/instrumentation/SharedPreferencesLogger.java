package com.android.settingslib.core.instrumentation;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.settingslib.wifi.AccessPoint;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class SharedPreferencesLogger implements SharedPreferences {
    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeature;
    private final Set<String> mPreferenceKeySet = new ConcurrentSkipListSet();
    private final String mTag;

    public SharedPreferencesLogger(Context context, String str, MetricsFeatureProvider metricsFeatureProvider) {
        this.mContext = context;
        this.mTag = str;
        this.mMetricsFeature = metricsFeatureProvider;
    }

    @Override
    public Map<String, ?> getAll() {
        return null;
    }

    @Override
    public String getString(String str, String str2) {
        return str2;
    }

    @Override
    public Set<String> getStringSet(String str, Set<String> set) {
        return set;
    }

    @Override
    public int getInt(String str, int i) {
        return i;
    }

    @Override
    public long getLong(String str, long j) {
        return j;
    }

    @Override
    public float getFloat(String str, float f) {
        return f;
    }

    @Override
    public boolean getBoolean(String str, boolean z) {
        return z;
    }

    @Override
    public boolean contains(String str) {
        return false;
    }

    @Override
    public SharedPreferences.Editor edit() {
        return new EditorLogger();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
    }

    public void logValue(String str, Object obj) {
        logValue(str, obj, false);
    }

    public void logValue(String str, Object obj, boolean z) {
        int iIntValue;
        String strBuildPrefKey = buildPrefKey(this.mTag, str);
        if (!z && !this.mPreferenceKeySet.contains(strBuildPrefKey)) {
            this.mPreferenceKeySet.add(strBuildPrefKey);
            return;
        }
        this.mMetricsFeature.count(this.mContext, buildCountName(strBuildPrefKey, obj), 1);
        Pair<Integer, Object> pairCreate = null;
        if (obj instanceof Long) {
            Long l = (Long) obj;
            if (l.longValue() > 2147483647L) {
                iIntValue = Preference.DEFAULT_ORDER;
            } else if (l.longValue() < -2147483648L) {
                iIntValue = AccessPoint.UNREACHABLE_RSSI;
            } else {
                iIntValue = l.intValue();
            }
            pairCreate = Pair.create(1089, Integer.valueOf(iIntValue));
        } else if (obj instanceof Integer) {
            pairCreate = Pair.create(1089, obj);
        } else if (obj instanceof Boolean) {
            pairCreate = Pair.create(1089, Integer.valueOf(((Boolean) obj).booleanValue() ? 1 : 0));
        } else if (obj instanceof Float) {
            pairCreate = Pair.create(995, obj);
        } else if (obj instanceof String) {
            Log.d("SharedPreferencesLogger", "Tried to log string preference " + strBuildPrefKey + " = " + obj);
        } else {
            Log.w("SharedPreferencesLogger", "Tried to log unloggable object" + obj);
        }
        if (pairCreate != null) {
            this.mMetricsFeature.action(this.mContext, 853, Pair.create(854, strBuildPrefKey), pairCreate);
        }
    }

    void logPackageName(String str, String str2) {
        this.mMetricsFeature.action(this.mContext, 853, str2, Pair.create(854, this.mTag + "/" + str));
    }

    public void safeLogValue(String str, String str2) {
        new AsyncPackageCheck().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, str, str2);
    }

    public static String buildCountName(String str, Object obj) {
        return str + "|" + obj;
    }

    public static String buildPrefKey(String str, String str2) {
        return str + "/" + str2;
    }

    private class AsyncPackageCheck extends AsyncTask<String, Void, Void> {
        private AsyncPackageCheck() {
        }

        @Override
        public Void doInBackground(String... strArr) {
            String str = strArr[0];
            String packageName = strArr[1];
            PackageManager packageManager = SharedPreferencesLogger.this.mContext.getPackageManager();
            try {
                ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(packageName);
                if (packageName != null) {
                    packageName = componentNameUnflattenFromString.getPackageName();
                }
            } catch (Exception e) {
            }
            try {
                packageManager.getPackageInfo(packageName, 4194304);
                SharedPreferencesLogger.this.logPackageName(str, packageName);
                return null;
            } catch (PackageManager.NameNotFoundException e2) {
                SharedPreferencesLogger.this.logValue(str, packageName, true);
                return null;
            }
        }
    }

    public class EditorLogger implements SharedPreferences.Editor {
        public EditorLogger() {
        }

        @Override
        public SharedPreferences.Editor putString(String str, String str2) {
            SharedPreferencesLogger.this.safeLogValue(str, str2);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String str, Set<String> set) {
            SharedPreferencesLogger.this.safeLogValue(str, TextUtils.join(",", set));
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String str, int i) {
            SharedPreferencesLogger.this.logValue(str, Integer.valueOf(i));
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String str, long j) {
            SharedPreferencesLogger.this.logValue(str, Long.valueOf(j));
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String str, float f) {
            SharedPreferencesLogger.this.logValue(str, Float.valueOf(f));
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String str, boolean z) {
            SharedPreferencesLogger.this.logValue(str, Boolean.valueOf(z));
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String str) {
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            return this;
        }

        @Override
        public boolean commit() {
            return true;
        }

        @Override
        public void apply() {
        }
    }
}
