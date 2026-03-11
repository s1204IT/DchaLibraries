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
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, new ContentObserver(this, this.mHandler) {
                final BenesseExtensionService this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void onChange(boolean z) {
                    synchronized (this.this$0.mLock) {
                        int dchaState = this.this$0.getDchaState();
                        SystemProperties.set("com.benesse.dcha_state", String.valueOf(dchaState));
                        if (this.this$0.mDchaState != dchaState) {
                            this.this$0.mDchaState = dchaState;
                            this.this$0.updateEnabledBrowser();
                        }
                    }
                }
            }, -1);
            this.mDchaState = getDchaState();
            SystemProperties.set("com.benesse.dcha_state", String.valueOf(this.mDchaState));
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("adb_enabled"), false, new ContentObserver(this, this.mHandler) {
                final BenesseExtensionService this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void onChange(boolean z) {
                    synchronized (this.this$0.mLock) {
                        try {
                            int i = Settings.Global.getInt(this.this$0.mContext.getContentResolver(), "adb_enabled");
                            if (this.this$0.mEnabledAdb != i) {
                                this.this$0.mEnabledAdb = i;
                                this.this$0.updateEnabledBrowser();
                            }
                        } catch (Settings.SettingNotFoundException e) {
                        }
                    }
                }
            }, -1);
            try {
                this.mEnabledAdb = Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled");
            } catch (Settings.SettingNotFoundException e) {
            }
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("bc_locale_language"), false, new ContentObserver(this, this.mHandler) {
                final BenesseExtensionService this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void onChange(boolean z) {
                    synchronized (this.this$0.mLock) {
                        String string = Settings.System.getString(this.this$0.mContext.getContentResolver(), "bc_locale_language");
                        if (string == null || !string.equals(this.this$0.mLanguage)) {
                            this.this$0.mLanguage = string;
                            this.this$0.updateEnabledBrowser();
                        }
                    }
                }
            }, -1);
            this.mLanguage = Settings.System.getString(this.mContext.getContentResolver(), "bc_locale_language");
            updateEnabledBrowser();
        }
    }

    public void updateEnabledBrowser() {
        int i = (this.mDchaState != 0 && this.mEnabledAdb == 0 && "ja".equals(this.mLanguage)) ? 2 : 0;
        PackageManager packageManager = this.mContext.getPackageManager();
        int applicationEnabledSetting = packageManager.getApplicationEnabledSetting("com.android.browser");
        int applicationEnabledSetting2 = packageManager.getApplicationEnabledSetting("com.android.quicksearchbox");
        if (i != applicationEnabledSetting) {
            packageManager.setApplicationEnabledSetting("com.android.browser", i, 0);
        }
        if (i != applicationEnabledSetting2) {
            packageManager.setApplicationEnabledSetting("com.android.quicksearchbox", i, 0);
        }
    }

    @Override
    public int getDchaState() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "dcha_state", 0);
    }

    @Override
    public void setDchaState(int i) {
        Settings.System.putInt(this.mContext.getContentResolver(), "dcha_state", i);
    }
}
