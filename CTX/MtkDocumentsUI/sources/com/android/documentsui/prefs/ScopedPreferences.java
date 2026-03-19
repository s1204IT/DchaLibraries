package com.android.documentsui.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.documentsui.R;

public interface ScopedPreferences {
    boolean getShowDeviceRoot();

    void setShowDeviceRoot(boolean z);

    static ScopedPreferences create(Context context, String str) {
        return new RuntimeScopedPreferences(context, PreferenceManager.getDefaultSharedPreferences(context), str);
    }

    public static final class RuntimeScopedPreferences implements ScopedPreferences {
        static final boolean $assertionsDisabled = false;
        private final boolean mDefaultShowDeviceRoot;
        private final String mScope;
        private final SharedPreferences mSharedPrefs;

        private RuntimeScopedPreferences(Context context, SharedPreferences sharedPreferences, String str) {
            this.mSharedPrefs = sharedPreferences;
            this.mScope = str;
            this.mDefaultShowDeviceRoot = context.getResources().getBoolean(R.bool.config_default_show_device_root);
        }

        @Override
        public boolean getShowDeviceRoot() {
            return this.mSharedPrefs.getBoolean("includeDeviceRoot", this.mDefaultShowDeviceRoot);
        }

        @Override
        public void setShowDeviceRoot(boolean z) {
            this.mSharedPrefs.edit().putBoolean("includeDeviceRoot", z).apply();
        }
    }

    static boolean shouldBackup(String str) {
        return "includeDeviceRoot".equals(str);
    }
}
