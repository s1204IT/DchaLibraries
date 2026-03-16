package com.android.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBenesseExtensionService;
import android.os.SystemProperties;
import android.provider.Settings;

public class BenesseExtensionService extends IBenesseExtensionService.Stub {
    static final String TAG = "BenesseExtensionService";
    private Context mContext;
    private int mDchaState;
    private int mEnabledAdb;
    private String mLanguage;
    private Object mLock = new Object();
    private Handler mHandler = new Handler(true);

    BenesseExtensionService(Context context) {
        this.mContext = context;
        synchronized (this.mLock) {
            ContentObserver obs = new ContentObserver(this.mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    synchronized (BenesseExtensionService.this.mLock) {
                        int state = BenesseExtensionService.this.getDchaState();
                        SystemProperties.set("com.benesse.dcha_state", String.valueOf(state));
                        if (BenesseExtensionService.this.mDchaState != state) {
                            BenesseExtensionService.this.mDchaState = state;
                            BenesseExtensionService.this.updateEnabledBrowser();
                        }
                    }
                }
            };
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, obs, -1);
            this.mDchaState = getDchaState();
            SystemProperties.set("com.benesse.dcha_state", String.valueOf(this.mDchaState));
            ContentObserver obs2 = new ContentObserver(this.mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    synchronized (BenesseExtensionService.this.mLock) {
                        try {
                            int enableAdb = Settings.Global.getInt(BenesseExtensionService.this.mContext.getContentResolver(), "adb_enabled");
                            if (BenesseExtensionService.this.mEnabledAdb != enableAdb) {
                                BenesseExtensionService.this.mEnabledAdb = enableAdb;
                                BenesseExtensionService.this.updateEnabledBrowser();
                            }
                        } catch (Settings.SettingNotFoundException e) {
                        }
                    }
                }
            };
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("adb_enabled"), false, obs2, -1);
            try {
                this.mEnabledAdb = Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled");
            } catch (Settings.SettingNotFoundException e) {
            }
            ContentObserver obs3 = new ContentObserver(this.mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    synchronized (BenesseExtensionService.this.mLock) {
                        String lang = Settings.System.getString(BenesseExtensionService.this.mContext.getContentResolver(), "bc_locale_language");
                        if (lang == null || !lang.equals(BenesseExtensionService.this.mLanguage)) {
                            BenesseExtensionService.this.mLanguage = lang;
                            BenesseExtensionService.this.updateEnabledBrowser();
                        }
                    }
                }
            };
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("bc_locale_language"), false, obs3, -1);
            this.mLanguage = Settings.System.getString(this.mContext.getContentResolver(), "bc_locale_language");
            updateEnabledBrowser();
        }
    }

    public int getDchaState() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "dcha_state", 0);
    }

    public void setDchaState(int state) {
        Settings.System.putInt(this.mContext.getContentResolver(), "dcha_state", state);
    }

    private void updateEnabledBrowser() {
        int disableBrowser = (this.mDchaState != 0 && this.mEnabledAdb == 0 && "ja".equals(this.mLanguage)) ? 2 : 0;
        PackageManager pm = this.mContext.getPackageManager();
        int isBrowserEnabled = pm.getApplicationEnabledSetting("com.android.browser");
        int isSearchEnabled = pm.getApplicationEnabledSetting("com.android.quicksearchbox");
        if (disableBrowser != isBrowserEnabled) {
            pm.setApplicationEnabledSetting("com.android.browser", disableBrowser, 0);
        }
        if (disableBrowser != isSearchEnabled) {
            pm.setApplicationEnabledSetting("com.android.quicksearchbox", disableBrowser, 0);
        }
    }
}
