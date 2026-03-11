package com.android.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
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
        FileOutputStream fileOutputStream;
        FileOutputStream fileOutputStream2;
        Throwable th;
        Throwable th2;
        Throwable th3 = null;
        this.mContext = context;
        synchronized (this.mLock) {
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, new ContentObserver(this, this.mHandler) {
                final BenesseExtensionService this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void onChange(boolean z) {
                    Throwable th4;
                    FileOutputStream fileOutputStream3;
                    Throwable th5;
                    Throwable th6 = null;
                    synchronized (this.this$0.mLock) {
                        int dchaState = this.this$0.getDchaState();
                        SystemProperties.set("persist.sys.bc.dcha_state", String.valueOf(dchaState));
                        try {
                            fileOutputStream3 = new FileOutputStream("/factory/mode/dchastate");
                            try {
                                fileOutputStream3.write(dchaState);
                                if (fileOutputStream3 != null) {
                                    try {
                                        try {
                                            fileOutputStream3.close();
                                        } catch (Throwable th7) {
                                            th6 = th7;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                                if (th6 != null) {
                                    throw th6;
                                }
                                if (dchaState > 0) {
                                    this.this$0.updateUsbFunction();
                                }
                                if (this.this$0.mDchaState != dchaState) {
                                    this.this$0.mDchaState = dchaState;
                                    this.this$0.updateEnabledBrowser();
                                }
                                this.this$0.changeDisallowInstallUnknownSource(this.this$0.getDchaCompletedPast());
                            } catch (Throwable th8) {
                                th = th8;
                                try {
                                    throw th;
                                } catch (Throwable th9) {
                                    th4 = th9;
                                    th5 = th;
                                    if (fileOutputStream3 == null) {
                                        try {
                                            fileOutputStream3.close();
                                            th = th5;
                                        } catch (Throwable th10) {
                                            th = th10;
                                            if (th5 != null) {
                                                if (th5 != th) {
                                                    th5.addSuppressed(th);
                                                    th = th5;
                                                }
                                            }
                                        }
                                    } else {
                                        th = th5;
                                    }
                                    if (th != null) {
                                        throw th4;
                                    }
                                    throw th;
                                }
                            }
                        } catch (Throwable th11) {
                            th4 = th11;
                            fileOutputStream3 = null;
                            th5 = null;
                        }
                    }
                }
            }, -1);
            this.mDchaState = getDchaState();
            SystemProperties.set("persist.sys.bc.dcha_state", String.valueOf(this.mDchaState));
            if (this.mDchaState > 0) {
                updateUsbFunction();
            }
            try {
                fileOutputStream = new FileOutputStream("/factory/mode/dchastate");
            } catch (Throwable th4) {
                th = th4;
                fileOutputStream = null;
            }
            try {
                fileOutputStream.write(this.mDchaState);
                if (fileOutputStream != null) {
                    try {
                        try {
                            fileOutputStream.close();
                        } catch (Throwable th5) {
                            th3 = th5;
                        }
                    } catch (Exception e) {
                    }
                }
                if (th3 != null) {
                    throw th3;
                }
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
                                    Log.i(BenesseExtensionService.TAG, "getADBENABLE=" + this.this$0.getAdbEnabled());
                                    if (!this.this$0.changeAdbEnable()) {
                                        this.this$0.updateEnabledBrowser();
                                    }
                                }
                            } catch (Settings.SettingNotFoundException e2) {
                            }
                        }
                    }
                }, -1);
                try {
                    this.mEnabledAdb = Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled");
                } catch (Settings.SettingNotFoundException e2) {
                    this.mEnabledAdb = 0;
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
                            if (string == null || string.equals("")) {
                                string = "ja";
                            }
                            if (!string.equals(this.this$0.mLanguage)) {
                                this.this$0.mLanguage = string;
                                this.this$0.updateEnabledBrowser();
                            }
                        }
                    }
                }, -1);
                this.mLanguage = Settings.System.getString(this.mContext.getContentResolver(), "bc_locale_language");
                if (this.mLanguage == null || this.mLanguage.equals("")) {
                    this.mLanguage = "ja";
                }
                updateEnabledBrowser();
                changeDisallowInstallUnknownSource(getDchaCompletedPast());
            } catch (Throwable th6) {
                fileOutputStream2 = fileOutputStream;
                th = null;
                th2 = th6;
                if (fileOutputStream2 == null) {
                }
                if (th != null) {
                }
            }
        }
    }

    public boolean changeAdbEnable() {
        if (getAdbEnabled() == 0 || getDchaState() == 3 || !getDchaCompletedPast() || Settings.System.getInt(this.mContext.getContentResolver(), BC_PASSWORD_HIT_FLAG, 0) != 0) {
            return false;
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 0);
        return true;
    }

    public void changeDisallowInstallUnknownSource(boolean z) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager != null) {
            userManager.setUserRestriction("no_install_unknown_sources", z, UserHandle.SYSTEM);
        }
    }

    public int getAdbEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0);
    }

    public boolean getDchaCompletedPast() {
        if (getUid(BenesseExtension.IGNORE_DCHA_COMPLETED_FILE) != 0) {
            return BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists();
        }
        return false;
    }

    private String getLanguage() {
        String str = SystemProperties.get(PROPERTY_LOCALE, JAPAN_LOCALE);
        return (str == null || str.equals("")) ? JAPAN_LOCALE : str;
    }

    private int getUid(File file) {
        if (file.exists()) {
            return FileUtils.getUid(file.getPath());
        }
        return -1;
    }

    public void updateEnabledBrowser() {
        int i = 2;
        if (!getDchaCompletedPast() && (getDchaState() == 0 || getAdbEnabled() != 0 || !JAPAN_LOCALE.equals(getLanguage()))) {
            i = 0;
        }
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

    public void updateUsbFunction() {
        String str = SystemProperties.get("persist.sys.usb.config", "none");
        String strAddFunction = Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0) == 1 ? UsbManager.addFunction("ptp", "adb") : "ptp";
        if (strAddFunction.equals(str)) {
            return;
        }
        SystemProperties.set("persist.sys.usb.config", strAddFunction);
    }

    @Override
    public boolean checkPassword(String str) throws Throwable {
        FileInputStream fileInputStream;
        Throwable th;
        FileInputStream fileInputStream2;
        Throwable th2;
        byte[] bArr;
        MessageDigest messageDigest;
        byte[] bArr2;
        boolean zEquals = false;
        if (str != null) {
            byte[] bArr3 = new byte[64];
            try {
                fileInputStream2 = new FileInputStream(DCHA_HASH_FILEPATH);
                try {
                    if (fileInputStream2.read(bArr3) != 64 || FileUtils.getUid(DCHA_HASH_FILEPATH) != 0) {
                        bArr3 = (byte[]) DEFAULT_HASH.clone();
                    }
                    if (fileInputStream2 != null) {
                        try {
                            try {
                                fileInputStream2.close();
                                th = null;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        } catch (IOException e) {
                            bArr = (byte[]) DEFAULT_HASH.clone();
                        }
                    } else {
                        th = null;
                    }
                    if (th != null) {
                        throw th;
                    }
                    bArr = bArr3;
                    try {
                        messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
                    } catch (NoSuchAlgorithmException e2) {
                        messageDigest = null;
                    }
                    if (messageDigest == null) {
                        messageDigest.reset();
                        byte[] bArrDigest = messageDigest.digest(str.getBytes());
                        byte[] bArr4 = new byte[64];
                        for (int i = 0; i < bArrDigest.length && i < bArr4.length / 2; i++) {
                            bArr4[i * 2] = this.HEX_TABLE[(bArrDigest[i] >> 4) & 15];
                            bArr4[(i * 2) + 1] = this.HEX_TABLE[bArrDigest[i] & 15];
                        }
                        bArr2 = bArr4;
                    } else {
                        bArr2 = null;
                    }
                    zEquals = Arrays.equals(bArr, bArr2);
                    Log.i(TAG, "password comparison = " + zEquals);
                    if (zEquals) {
                        Settings.System.putInt(this.mContext.getContentResolver(), BC_PASSWORD_HIT_FLAG, 1);
                    }
                } catch (Throwable th4) {
                    th = null;
                    th2 = th4;
                    if (fileInputStream2 == null) {
                    }
                    if (th == null) {
                    }
                }
            } catch (Throwable th5) {
                fileInputStream = null;
                th = th5;
            }
        }
        return zEquals;
    }

    @Override
    public boolean checkUsbCam() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return new File("/dev/video0").exists();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public int getDchaState() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "dcha_state", 0);
    }

    @Override
    public String getString(String str) {
        if (!str.equals("bc:mac_address")) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return ((WifiManager) this.mContext.getSystemService("wifi")).getConnectionInfo().getMacAddress();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public void setDchaState(int i) {
        Settings.System.putInt(this.mContext.getContentResolver(), "dcha_state", i);
    }
}
