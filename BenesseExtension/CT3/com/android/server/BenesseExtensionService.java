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
import android.provider.BrowserContract;
import android.provider.Settings;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/* loaded from: a04br3-02.05.000-framework.jar:com/android/server/BenesseExtensionService.class */
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

    /* JADX WARN: Removed duplicated region for block: B:49:0x014c  */
    /* JADX WARN: Removed duplicated region for block: B:66:0x0177  */
    /* JADX WARN: Removed duplicated region for block: B:84:0x0141 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:69:0x017a -> B:24:0x00d2). Please report as a decompilation issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    BenesseExtensionService(Context context) {
        Throwable th;
        Throwable th2;
        this.mContext = context;
        synchronized (this.mLock) {
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("dcha_state"), false, new ContentObserver(this, this.mHandler) { // from class: com.android.server.BenesseExtensionService.1
                final BenesseExtensionService this$0;

                {
                    this.this$0 = this;
                }

                /* JADX WARN: Removed duplicated region for block: B:44:0x00c0  */
                /* JADX WARN: Removed duplicated region for block: B:61:0x00f2  */
                /* JADX WARN: Removed duplicated region for block: B:76:0x00b3 A[EXC_TOP_SPLITTER, SYNTHETIC] */
                @Override // android.database.ContentObserver
                /*
                    Code decompiled incorrectly, please refer to instructions dump.
                */
                public void onChange(boolean z) {
                    FileOutputStream fileOutputStream;
                    Throwable th3;
                    Throwable th4 = null;
                    synchronized (this.this$0.mLock) {
                        int dchaState = this.this$0.getDchaState();
                        SystemProperties.set("persist.sys.bc.dcha_state", String.valueOf(dchaState));
                        try {
                            fileOutputStream = new FileOutputStream("/factory/mode/dchastate");
                            try {
                                fileOutputStream.write(dchaState);
                                Throwable th5 = null;
                                if (fileOutputStream != null) {
                                    try {
                                        try {
                                            fileOutputStream.close();
                                            th5 = null;
                                        } catch (Exception e) {
                                        }
                                    } catch (Throwable th6) {
                                        th5 = th6;
                                    }
                                }
                                if (th5 != null) {
                                    throw th5;
                                }
                                if (dchaState > 0) {
                                    this.this$0.updateUsbFunction();
                                }
                                if (this.this$0.mDchaState != dchaState) {
                                    this.this$0.mDchaState = dchaState;
                                    this.this$0.updateEnabledBrowser();
                                }
                                this.this$0.changeDisallowInstallUnknownSource(this.this$0.getDchaCompletedPast());
                            } catch (Throwable th7) {
                                th = th7;
                                try {
                                    throw th;
                                } catch (Throwable th8) {
                                    th4 = th;
                                    th = th8;
                                    th3 = th4;
                                    if (fileOutputStream != null) {
                                        try {
                                            fileOutputStream.close();
                                            th3 = th4;
                                        } catch (Throwable th9) {
                                            if (th4 == null) {
                                                th3 = th9;
                                            } else {
                                                th3 = th4;
                                                if (th4 != th9) {
                                                    th4.addSuppressed(th9);
                                                    th3 = th4;
                                                }
                                            }
                                        }
                                    }
                                    if (th3 != null) {
                                        throw th;
                                    }
                                    throw th3;
                                }
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            fileOutputStream = null;
                        }
                    }
                }
            }, -1);
            this.mDchaState = getDchaState();
            SystemProperties.set("persist.sys.bc.dcha_state", String.valueOf(this.mDchaState));
            if (this.mDchaState > 0) {
                updateUsbFunction();
            }
            FileOutputStream fileOutputStream = null;
            try {
                FileOutputStream fileOutputStream2 = new FileOutputStream("/factory/mode/dchastate");
                try {
                    fileOutputStream2.write(this.mDchaState);
                    Throwable th3 = null;
                    if (fileOutputStream2 != null) {
                        try {
                            try {
                                fileOutputStream2.close();
                                th3 = null;
                            } catch (Exception e) {
                            }
                        } catch (Throwable th4) {
                            th3 = th4;
                        }
                    }
                    if (th3 != null) {
                        throw th3;
                    }
                    this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("adb_enabled"), false, new ContentObserver(this, this.mHandler) { // from class: com.android.server.BenesseExtensionService.2
                        final BenesseExtensionService this$0;

                        {
                            this.this$0 = this;
                        }

                        @Override // android.database.ContentObserver
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
                    this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("bc_locale_language"), false, new ContentObserver(this, this.mHandler) { // from class: com.android.server.BenesseExtensionService.3
                        final BenesseExtensionService this$0;

                        {
                            this.this$0 = this;
                        }

                        /* JADX WARN: Removed duplicated region for block: B:9:0x002a  */
                        @Override // android.database.ContentObserver
                        /*
                            Code decompiled incorrectly, please refer to instructions dump.
                        */
                        public void onChange(boolean z) {
                            synchronized (this.this$0.mLock) {
                                String string = Settings.System.getString(this.this$0.mContext.getContentResolver(), "bc_locale_language");
                                if (string != null) {
                                    String str = string;
                                    if (string.equals("")) {
                                        str = "ja";
                                    }
                                    if (!str.equals(this.this$0.mLanguage)) {
                                        this.this$0.mLanguage = str;
                                        this.this$0.updateEnabledBrowser();
                                    }
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
                } catch (Throwable th5) {
                    fileOutputStream = fileOutputStream2;
                    th = th5;
                    try {
                        throw th;
                    } catch (Throwable th6) {
                        th = th;
                        th = th6;
                        th2 = th;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                                th2 = th;
                            } catch (Throwable th7) {
                                if (th == null) {
                                    th2 = th7;
                                } else {
                                    th2 = th;
                                    if (th != th7) {
                                        th.addSuppressed(th7);
                                        th2 = th;
                                    }
                                }
                            }
                        }
                        if (th2 != null) {
                            throw th;
                        }
                        throw th2;
                    }
                }
            } catch (Throwable th8) {
                th = th8;
                fileOutputStream = null;
                th = null;
            }
        }
    }

    private boolean changeAdbEnable() {
        if (getAdbEnabled() == 0 || getDchaState() == 3 || !getDchaCompletedPast() || Settings.System.getInt(this.mContext.getContentResolver(), BC_PASSWORD_HIT_FLAG, 0) != 0) {
            return false;
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 0);
        return true;
    }

    private void changeDisallowInstallUnknownSource(boolean z) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager != null) {
            userManager.setUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, z, UserHandle.SYSTEM);
        }
    }

    private int getAdbEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0);
    }

    private boolean getDchaCompletedPast() {
        boolean zExists = false;
        if (getUid(BenesseExtension.IGNORE_DCHA_COMPLETED_FILE) != 0) {
            zExists = BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists();
        }
        return zExists;
    }

    /* JADX WARN: Removed duplicated region for block: B:6:0x0017  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private String getLanguage() {
        String str;
        String str2 = SystemProperties.get(PROPERTY_LOCALE, JAPAN_LOCALE);
        if (str2 != null) {
            str = str2;
            if (str2.equals("")) {
                str = JAPAN_LOCALE;
            }
        }
        return str;
    }

    private int getUid(File file) {
        if (file.exists()) {
            return FileUtils.getUid(file.getPath());
        }
        return -1;
    }

    private void updateEnabledBrowser() {
        int i = getDchaCompletedPast() ? 2 : (getDchaState() != 0 && getAdbEnabled() == 0 && JAPAN_LOCALE.equals(getLanguage())) ? 2 : 0;
        PackageManager packageManager = this.mContext.getPackageManager();
        int applicationEnabledSetting = packageManager.getApplicationEnabledSetting(BrowserContract.AUTHORITY);
        int applicationEnabledSetting2 = packageManager.getApplicationEnabledSetting("com.android.quicksearchbox");
        if (i != applicationEnabledSetting) {
            packageManager.setApplicationEnabledSetting(BrowserContract.AUTHORITY, i, 0);
        }
        if (i != applicationEnabledSetting2) {
            packageManager.setApplicationEnabledSetting("com.android.quicksearchbox", i, 0);
        }
    }

    private void updateUsbFunction() {
        boolean z = true;
        String strAddFunction = UsbManager.USB_FUNCTION_PTP;
        String str = SystemProperties.get(UsbManager.ADB_PERSISTENT_PROPERTY, "none");
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0) != 1) {
            z = false;
        }
        if (z) {
            strAddFunction = UsbManager.addFunction(UsbManager.USB_FUNCTION_PTP, UsbManager.USB_FUNCTION_ADB);
        }
        if (strAddFunction.equals(str)) {
            return;
        }
        SystemProperties.set(UsbManager.ADB_PERSISTENT_PROPERTY, strAddFunction);
    }

    /* JADX WARN: Removed duplicated region for block: B:15:0x003b A[Catch: all -> 0x018a, all -> 0x018a, Throwable -> 0x019b, Throwable -> 0x019b, TRY_ENTER, TRY_LEAVE, TryCatch #10 {all -> 0x018a, Throwable -> 0x019b, blocks: (B:9:0x0027, B:13:0x0033, B:15:0x003b, B:15:0x003b, B:16:0x003e), top: B:88:0x0027 }] */
    /* JADX WARN: Removed duplicated region for block: B:32:0x007c  */
    /* JADX WARN: Removed duplicated region for block: B:52:0x0109  */
    /* JADX WARN: Removed duplicated region for block: B:64:0x0138  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x016c  */
    /* JADX WARN: Removed duplicated region for block: B:81:0x00fb A[EXC_TOP_SPLITTER, SYNTHETIC] */
    @Override // android.os.IBenesseExtensionService
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean checkPassword(String str) throws Throwable {
        Throwable th;
        Throwable th2;
        byte[] bArr;
        MessageDigest messageDigest;
        boolean zEquals;
        FileInputStream fileInputStream;
        if (str == null) {
            return false;
        }
        byte[] bArr2 = new byte[64];
        FileInputStream fileInputStream2 = null;
        try {
            fileInputStream = new FileInputStream(DCHA_HASH_FILEPATH);
        } catch (Throwable th3) {
            th = th3;
        }
        try {
            if (fileInputStream.read(bArr2) == 64) {
                bArr = bArr2;
                if (FileUtils.getUid(DCHA_HASH_FILEPATH) != 0) {
                    bArr = (byte[]) DEFAULT_HASH.clone();
                }
                Throwable th4 = null;
                if (fileInputStream != null) {
                    try {
                        try {
                            fileInputStream.close();
                            th4 = null;
                        } catch (IOException e) {
                            bArr = (byte[]) DEFAULT_HASH.clone();
                            messageDigest = MessageDigest.getInstance("SHA-256");
                            byte[] bArr3 = null;
                            if (messageDigest != null) {
                            }
                            zEquals = Arrays.equals(bArr, bArr3);
                            Log.i(TAG, "password comparison = " + zEquals);
                            if (zEquals) {
                            }
                            return zEquals;
                        }
                    } catch (Throwable th5) {
                        th4 = th5;
                    }
                }
                if (th4 != null) {
                    throw th4;
                }
            }
            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e2) {
                messageDigest = null;
            }
            byte[] bArr32 = null;
            if (messageDigest != null) {
                messageDigest.reset();
                byte[] bArrDigest = messageDigest.digest(str.getBytes());
                byte[] bArr4 = new byte[64];
                int i = 0;
                while (true) {
                    bArr32 = bArr4;
                    if (i >= bArrDigest.length) {
                        break;
                    }
                    bArr32 = bArr4;
                    if (i >= bArr4.length / 2) {
                        break;
                    }
                    bArr4[i * 2] = this.HEX_TABLE[(bArrDigest[i] >> 4) & 15];
                    bArr4[(i * 2) + 1] = this.HEX_TABLE[bArrDigest[i] & 15];
                    i++;
                }
            }
            zEquals = Arrays.equals(bArr, bArr32);
            Log.i(TAG, "password comparison = " + zEquals);
            if (zEquals) {
                Settings.System.putInt(this.mContext.getContentResolver(), BC_PASSWORD_HIT_FLAG, 1);
            }
            return zEquals;
        } catch (Throwable th6) {
            fileInputStream2 = fileInputStream;
            th = th6;
            try {
                throw th;
            } catch (Throwable th7) {
                th = th;
                th = th7;
                th2 = th;
                if (fileInputStream2 != null) {
                    try {
                        try {
                            fileInputStream2.close();
                            th2 = th;
                        } catch (IOException e3) {
                            bArr = (byte[]) DEFAULT_HASH.clone();
                            messageDigest = MessageDigest.getInstance("SHA-256");
                            byte[] bArr322 = null;
                            if (messageDigest != null) {
                            }
                            zEquals = Arrays.equals(bArr, bArr322);
                            Log.i(TAG, "password comparison = " + zEquals);
                            if (zEquals) {
                            }
                            return zEquals;
                        }
                    } catch (Throwable th8) {
                        if (th == null) {
                            th2 = th8;
                        } else {
                            th2 = th;
                            if (th != th8) {
                                th.addSuppressed(th8);
                                th2 = th;
                            }
                        }
                    }
                }
                if (th2 == null) {
                    throw th2;
                }
                throw th;
            }
        }
    }

    @Override // android.os.IBenesseExtensionService
    public boolean checkUsbCam() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return new File("/dev/video0").exists();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override // android.os.IBenesseExtensionService
    public int getDchaState() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "dcha_state", 0);
    }

    @Override // android.os.IBenesseExtensionService
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

    @Override // android.os.IBenesseExtensionService
    public void setDchaState(int i) {
        Settings.System.putInt(this.mContext.getContentResolver(), "dcha_state", i);
    }
}