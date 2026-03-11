package com.android.launcher3.util;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

public interface SettingsObserver {
    void onSettingChanged(boolean z);

    void register(String str, String... strArr);

    void unregister();

    public static abstract class Secure extends ContentObserver implements SettingsObserver {
        private String mKeySetting;
        private ContentResolver mResolver;

        public Secure(ContentResolver contentResolver) {
            super(new Handler());
            this.mResolver = contentResolver;
        }

        @Override
        public void register(String str, String... strArr) {
            this.mKeySetting = str;
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor(this.mKeySetting), false, this);
            for (String str2 : strArr) {
                this.mResolver.registerContentObserver(Settings.Secure.getUriFor(str2), false, this);
            }
            onChange(true);
        }

        @Override
        public void unregister() {
            this.mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean z) {
            super.onChange(z);
            onSettingChanged(Settings.Secure.getInt(this.mResolver, this.mKeySetting, 1) == 1);
        }
    }

    public static abstract class System extends ContentObserver implements SettingsObserver {
        private String mKeySetting;
        private ContentResolver mResolver;

        public System(ContentResolver contentResolver) {
            super(new Handler());
            this.mResolver = contentResolver;
        }

        @Override
        public void register(String str, String... strArr) {
            this.mKeySetting = str;
            this.mResolver.registerContentObserver(Settings.System.getUriFor(this.mKeySetting), false, this);
            for (String str2 : strArr) {
                this.mResolver.registerContentObserver(Settings.System.getUriFor(str2), false, this);
            }
            onChange(true);
        }

        @Override
        public void unregister() {
            this.mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean z) {
            super.onChange(z);
            onSettingChanged(Settings.System.getInt(this.mResolver, this.mKeySetting, 1) == 1);
        }
    }
}
