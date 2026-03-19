package com.android.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBenesseExtensionService;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class BenesseExtensionService extends IBenesseExtensionService.Stub {
    static final String BC_PASSWORD_HIT_FLAG = "bc_password_hit";
    static final String DCHA_HASH_FILEPATH = "/factory/dcha_hash";
    private static final byte[] DEFAULT_HASH = "9b66c16d267c7c3331acafd4cb449219118998678205e8843b5e1094a9b14237".getBytes();
    static final String HASH_ALGORITHM = "SHA-256";
    static final String JAPAN_LOCALE = "ja-JP";
    static final String PROPERTY_LOCALE = "persist.sys.locale";
    static final String TAG = "BenesseExtensionService";
    private Context mContext;
    private int mDchaState;
    private int mEnabledAdb;
    private String mLanguage;
    private final byte[] HEX_TABLE = "0123456789abcdef".getBytes();
    private Object mLock = new Object();
    private Handler mHandler = new Handler(true);

    BenesseExtensionService(Context context) {
        FileOutputStream fos;
        Throwable th = null;
        this.mContext = context;
        synchronized (this.mLock) {
            ContentObserver obs = new ContentObserver(this.mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    Throwable th2 = null;
                    synchronized (BenesseExtensionService.this.mLock) {
                        int state = BenesseExtensionService.this.getDchaState();
                        SystemProperties.set("persist.sys.bc.dcha_state", String.valueOf(state));
                        FileOutputStream fos2 = null;
                        try {
                            FileOutputStream fos3 = new FileOutputStream("/factory/mode/dchastate");
                            try {
                                fos3.write(state);
                                if (fos3 != null) {
                                    try {
                                        try {
                                            fos3.close();
                                        } catch (Exception e) {
                                        }
                                    } catch (Throwable th3) {
                                        th2 = th3;
                                    }
                                }
                                if (th2 != null) {
                                    throw th2;
                                }
                                if (state > 0) {
                                    BenesseExtensionService.this.updateUsbFunction();
                                }
                                if (BenesseExtensionService.this.mDchaState != state) {
                                    BenesseExtensionService.this.mDchaState = state;
                                    BenesseExtensionService.this.updateEnabledBrowser();
                                }
                                BenesseExtensionService.this.changeDisallowInstallUnknownSource(BenesseExtensionService.this.getDchaCompletedPast());
                            } catch (Throwable th4) {
                                th = th4;
                                fos2 = fos3;
                                if (fos2 != null) {
                                }
                                if (th2 != null) {
                                }
                            }
                        } catch (Throwable th5) {
                            th = th5;
                        }
                    }
                }
            };
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, obs, -1);
            this.mDchaState = getDchaState();
            SystemProperties.set("persist.sys.bc.dcha_state", String.valueOf(this.mDchaState));
            if (this.mDchaState > 0) {
                updateUsbFunction();
            }
            FileOutputStream fos2 = null;
            try {
                fos = new FileOutputStream("/factory/mode/dchastate");
            } catch (Throwable th2) {
                th = th2;
            }
            try {
                fos.write(this.mDchaState);
                if (fos != null) {
                    try {
                        try {
                            fos.close();
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    } catch (Exception e) {
                    }
                }
                if (th != null) {
                    throw th;
                }
                ContentObserver obs2 = new ContentObserver(this.mHandler) {
                    @Override
                    public void onChange(boolean selfChange) {
                        synchronized (BenesseExtensionService.this.mLock) {
                            try {
                                int enableAdb = Settings.Global.getInt(BenesseExtensionService.this.mContext.getContentResolver(), "adb_enabled");
                                if (BenesseExtensionService.this.mEnabledAdb != enableAdb) {
                                    BenesseExtensionService.this.mEnabledAdb = enableAdb;
                                    Log.i(BenesseExtensionService.TAG, "getADBENABLE=" + BenesseExtensionService.this.getAdbEnabled());
                                    if (!BenesseExtensionService.this.changeAdbEnable()) {
                                        BenesseExtensionService.this.updateEnabledBrowser();
                                    }
                                }
                            } catch (Settings.SettingNotFoundException e2) {
                            }
                        }
                    }
                };
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("adb_enabled"), false, obs2, -1);
                try {
                    this.mEnabledAdb = Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled");
                } catch (Settings.SettingNotFoundException e2) {
                    this.mEnabledAdb = 0;
                }
                ContentObserver obs3 = new ContentObserver(this.mHandler) {
                    @Override
                    public void onChange(boolean selfChange) {
                        synchronized (BenesseExtensionService.this.mLock) {
                            String lang = Settings.System.getString(BenesseExtensionService.this.mContext.getContentResolver(), "bc_locale_language");
                            if (lang == null || lang.equals("")) {
                                lang = "ja";
                            }
                            if (!lang.equals(BenesseExtensionService.this.mLanguage)) {
                                BenesseExtensionService.this.mLanguage = lang;
                                BenesseExtensionService.this.updateEnabledBrowser();
                            }
                        }
                    }
                };
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("bc_locale_language"), false, obs3, -1);
                this.mLanguage = Settings.System.getString(this.mContext.getContentResolver(), "bc_locale_language");
                if (this.mLanguage == null || this.mLanguage.equals("")) {
                    this.mLanguage = "ja";
                }
                updateEnabledBrowser();
                changeDisallowInstallUnknownSource(getDchaCompletedPast());
            } catch (Throwable th4) {
                th = th4;
                fos2 = fos;
                try {
                    throw th;
                } catch (Throwable th5) {
                    th = th;
                    th = th5;
                    if (fos2 != null) {
                        try {
                            fos2.close();
                        } catch (Throwable th6) {
                            if (th == null) {
                                th = th6;
                            } else if (th != th6) {
                                th.addSuppressed(th6);
                            }
                        }
                    }
                    if (th != null) {
                        throw th;
                    }
                    throw th;
                }
            }
        }
    }

    public int getDchaState() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "dcha_state", 0);
    }

    private boolean getDchaCompletedPast() {
        if (getUid(BenesseExtension.IGNORE_DCHA_COMPLETED_FILE) != 0) {
            return BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists();
        }
        return false;
    }

    public void setDchaState(int state) {
        Settings.System.putInt(this.mContext.getContentResolver(), "dcha_state", state);
    }

    public String getString(String name) {
        if (name.equals("bc:mac_address")) {
            long callingIdentity = Binder.clearCallingIdentity();
            try {
                WifiManager manager = (WifiManager) this.mContext.getSystemService("wifi");
                WifiInfo info = manager.getConnectionInfo();
                String address = info.getMacAddress();
                return address;
            } finally {
                Binder.restoreCallingIdentity(callingIdentity);
            }
        }
        return null;
    }

    public boolean checkUsbCam() {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            File f = new File("/dev/video0");
            return f.exists();
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private void updateEnabledBrowser() {
        int disableBrowser;
        if (getDchaCompletedPast()) {
            disableBrowser = 2;
        } else {
            disableBrowser = (getDchaState() != 0 && getAdbEnabled() == 0 && JAPAN_LOCALE.equals(getLanguage())) ? 2 : 0;
        }
        PackageManager pm = this.mContext.getPackageManager();
        int isBrowserEnabled = pm.getApplicationEnabledSetting("com.android.browser");
        int isSearchEnabled = pm.getApplicationEnabledSetting("com.android.quicksearchbox");
        if (disableBrowser != isBrowserEnabled) {
            pm.setApplicationEnabledSetting("com.android.browser", disableBrowser, 0);
        }
        if (disableBrowser == isSearchEnabled) {
            return;
        }
        pm.setApplicationEnabledSetting("com.android.quicksearchbox", disableBrowser, 0);
    }

    private void updateUsbFunction() {
        String currentFunc = SystemProperties.get("persist.sys.usb.config", "none");
        boolean adbEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0) == 1;
        String newFunc = adbEnabled ? UsbManager.addFunction("ptp", "adb") : "ptp";
        if (newFunc.equals(currentFunc)) {
            return;
        }
        SystemProperties.set("persist.sys.usb.config", newFunc);
    }

    public boolean checkPassword(String pwd) throws Throwable {
        MessageDigest messageDigest;
        boolean result_cmp;
        FileInputStream f;
        if (pwd == null) {
            return false;
        }
        byte[] hash1 = new byte[64];
        byte[] hash2 = null;
        Throwable th = null;
        FileInputStream f2 = null;
        try {
            f = new FileInputStream(DCHA_HASH_FILEPATH);
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            if (f.read(hash1) != 64 || FileUtils.getUid(DCHA_HASH_FILEPATH) != 0) {
                hash1 = (byte[]) DEFAULT_HASH.clone();
            }
            if (f != null) {
                try {
                    try {
                        f.close();
                    } catch (IOException e) {
                        hash1 = (byte[]) DEFAULT_HASH.clone();
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            if (th != null) {
                throw th;
            }
            try {
                messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            } catch (NoSuchAlgorithmException e2) {
                messageDigest = null;
            }
            if (messageDigest != null) {
                messageDigest.reset();
                byte[] tmp = messageDigest.digest(pwd.getBytes());
                hash2 = new byte[64];
                for (int loop = 0; loop < tmp.length && loop < hash2.length / 2; loop++) {
                    hash2[loop * 2] = this.HEX_TABLE[(tmp[loop] >> 4) & 15];
                    hash2[(loop * 2) + 1] = this.HEX_TABLE[tmp[loop] & 15];
                }
            }
            result_cmp = Arrays.equals(hash1, hash2);
            Log.i(TAG, "password comparison = " + result_cmp);
            if (result_cmp) {
                Settings.System.putInt(this.mContext.getContentResolver(), BC_PASSWORD_HIT_FLAG, 1);
            }
            return result_cmp;
        } catch (Throwable th4) {
            th = th4;
            f2 = f;
            try {
                throw th;
            } catch (Throwable th5) {
                th = th;
                th = th5;
                if (f2 != null) {
                    try {
                        f2.close();
                    } catch (Throwable th6) {
                        if (th == null) {
                            th = th6;
                        } else if (th != th6) {
                            th.addSuppressed(th6);
                        }
                    }
                }
                if (th == null) {
                    throw th;
                }
                throw th;
            }
        }
    }

    private void changeDisallowInstallUnknownSource(boolean state) {
        UserManager mgr = (UserManager) this.mContext.getSystemService("user");
        if (mgr != null) {
            mgr.setUserRestriction("no_install_unknown_sources", state, UserHandle.SYSTEM);
        }
    }

    private int getAdbEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0);
    }

    private String getLanguage() {
        String lang = SystemProperties.get(PROPERTY_LOCALE, JAPAN_LOCALE);
        return (lang == null || lang.equals("")) ? JAPAN_LOCALE : lang;
    }

    private boolean changeAdbEnable() {
        if (getAdbEnabled() == 0 || getDchaState() == 3 || !getDchaCompletedPast() || Settings.System.getInt(this.mContext.getContentResolver(), BC_PASSWORD_HIT_FLAG, 0) != 0) {
            return false;
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 0);
        return true;
    }

    private int getUid(File file) {
        if (!file.exists()) {
            return -1;
        }
        return FileUtils.getUid(file.getPath());
    }
}
