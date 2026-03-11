package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import com.android.internal.logging.MetricsLogger;
import java.util.Map;
import java.util.Set;

public class SharedPreferencesLogger implements SharedPreferences {
    private final Context mContext;
    private final String mTag;

    public SharedPreferencesLogger(Context context, String tag) {
        this.mContext = context;
        this.mTag = tag;
    }

    @Override
    public Map<String, ?> getAll() {
        return null;
    }

    @Override
    public String getString(String key, String defValue) {
        return defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        return false;
    }

    @Override
    public SharedPreferences.Editor edit() {
        return new EditorLogger();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
    }

    public void logValue(String key, String value) {
        MetricsLogger.count(this.mContext, this.mTag + "/" + key + "|" + value, 1);
    }

    public void logPackageName(String key, String value) {
        MetricsLogger.count(this.mContext, this.mTag + "/" + key, 1);
        MetricsLogger.action(this.mContext, 350, this.mTag + "/" + key + "|" + value);
    }

    public void safeLogValue(String key, String value) {
        new AsyncPackageCheck(this, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, key, value);
    }

    private class AsyncPackageCheck extends AsyncTask<String, Void, Void> {
        AsyncPackageCheck(SharedPreferencesLogger this$0, AsyncPackageCheck asyncPackageCheck) {
            this();
        }

        private AsyncPackageCheck() {
        }

        @Override
        public Void doInBackground(String... params) {
            String key = params[0];
            String value = params[1];
            PackageManager pm = SharedPreferencesLogger.this.mContext.getPackageManager();
            try {
                ComponentName name = ComponentName.unflattenFromString(value);
                if (value != null) {
                    value = name.getPackageName();
                }
            } catch (Exception e) {
            }
            try {
                pm.getPackageInfo(value, 8192);
                SharedPreferencesLogger.this.logPackageName(key, value);
            } catch (PackageManager.NameNotFoundException e2) {
                SharedPreferencesLogger.this.logValue(key, value);
            }
            return null;
        }
    }

    public class EditorLogger implements SharedPreferences.Editor {
        public EditorLogger() {
        }

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            SharedPreferencesLogger.this.safeLogValue(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            SharedPreferencesLogger.this.safeLogValue(key, TextUtils.join(",", values));
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            SharedPreferencesLogger.this.logValue(key, String.valueOf(value));
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            SharedPreferencesLogger.this.logValue(key, String.valueOf(value));
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            SharedPreferencesLogger.this.logValue(key, String.valueOf(value));
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            SharedPreferencesLogger.this.logValue(key, String.valueOf(value));
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
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
