package com.mediatek.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Point;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBenesseExtensionService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import com.android.internal.app.ColorDisplayController;
import com.mediatek.server.BenesseExtensionService;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class BenesseExtensionService extends IBenesseExtensionService.Stub {
    static final String ACTION_DT_FW_UPDATED = "com.panasonic.sanyo.ts.intent.action.DIGITIZER_FIRMWARE_UPDATED";
    static final String ACTION_TP_FW_UPDATED = "com.panasonic.sanyo.ts.intent.action.TOUCHPANEL_FIRMWARE_UPDATED";
    static final String BC_COMPATSCREEN = "bc:compatscreen";
    static final String BC_DT_FW_UPDATE = "bc:digitizer:fw_update";
    static final String BC_DT_FW_VERSION = "bc:digitizer:fw_version";
    static final String BC_MAC_ADDRESS = "bc:mac_address";
    static final String BC_NIGHTCOLOR_CURRENT = "bc:nightcolor:current";
    static final String BC_NIGHTCOLOR_MAX = "bc:nightcolor:max";
    static final String BC_NIGHTCOLOR_MIN = "bc:nightcolor:min";
    static final String BC_NIGHTMODE_ACTIVE = "bc:nightmode:active";
    static final String BC_PASSWORD_HIT_FLAG = "bc_password_hit";
    static final String BC_SERIAL_NO = "bc:serial_no";
    static final String BC_TP_FW_UPDATE = "bc:touchpanel:fw_update";
    static final String BC_TP_FW_VERSION = "bc:touchpanel:fw_version";
    static final String DCHA_HASH_FILEPATH = "/factory/dcha_hash";
    static final String DCHA_STATE = "dcha_state";
    static final String EXTRA_RESULT = "result";
    static final String HASH_ALGORITHM = "SHA-256";
    static final String JAPAN_LOCALE = "ja-JP";
    static final String PACKAGE_NAME_BROWSER = "com.android.browser";
    static final String PACKAGE_NAME_QSB = "com.android.quicksearchbox";
    static final String PACKAGE_NAME_TRACEUR = "com.android.traceur";
    static final String PROPERTY_DCHA_STATE = "persist.sys.bc.dcha_state";
    static final String PROPERTY_LOCALE = "persist.sys.locale";
    static final String TAG = "BenesseExtensionService";
    private ColorDisplayController mColorDisplayController;
    private Context mContext;
    private IWindowManager mWindowManager;
    static final File SYSFILE_TP_VERSION = new File("/sys/devices/platform/soc/11007000.i2c/i2c-0/0-000a/tp_fwver");
    static final File SYSFILE_DT_VERSION = new File("/sys/devices/platform/soc/11009000.i2c/i2c-2/2-0009/digi_fwver");
    private static final byte[] DEFAULT_HASH = "a1e3cf8aa7858a458972592ebb9438e967da30d196bd6191cc77606cc60af183".getBytes();
    static final int[][] mTable = {new int[]{240, 1920, 1200}, new int[]{160, 1024, 768}, new int[]{160, 1280, 800}};
    private boolean mIsUpdating = false;
    private final byte[] HEX_TABLE = "0123456789abcdef".getBytes();
    private Handler mHandler = new Handler(true);
    private ContentObserver mDchaStateObserver = new ContentObserver(this, this.mHandler) {
        final BenesseExtensionService this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onChange(boolean z) {
            synchronized (this.this$0.mLock) {
                SystemProperties.set(BenesseExtensionService.PROPERTY_DCHA_STATE, String.valueOf(this.this$0.getDchaStateInternal()));
                this.this$0.changeSafemodeRestriction(this.this$0.getDchaStateInternal());
                this.this$0.updateBrowserEnabled();
                this.this$0.changeDefaultUsbFunction(this.this$0.getDchaStateInternal());
                this.this$0.changeDisallowInstallUnknownSource(this.this$0.getDchaCompletedPast());
                this.this$0.updateTraceurEnabled();
            }
        }
    };
    private ContentObserver mAdbObserver = new ContentObserver(this, this.mHandler) {
        final BenesseExtensionService this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onChange(boolean z) {
            synchronized (this.this$0.mLock) {
                Log.i(BenesseExtensionService.TAG, "getADBENABLE=" + this.this$0.getAdbEnabled());
                if (!this.this$0.changeAdbEnable()) {
                    this.this$0.updateBrowserEnabled();
                }
            }
        }
    };
    private BroadcastReceiver mLanguageReceiver = new BroadcastReceiver(this) {
        final BenesseExtensionService this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this.this$0.mLock) {
                if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                    this.this$0.updateBrowserEnabled();
                }
            }
        }
    };
    private Object mLock = new Object();

    class UpdateParams {
        public String broadcast;
        public String[] cmd;
        final BenesseExtensionService this$0;

        private UpdateParams(BenesseExtensionService benesseExtensionService) {
            this.this$0 = benesseExtensionService;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    BenesseExtensionService(Context context) {
        this.mContext = context;
        synchronized (this.mLock) {
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(DCHA_STATE), false, this.mDchaStateObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("adb_enabled"), false, this.mAdbObserver, -1);
            this.mContext.registerReceiver(this.mLanguageReceiver, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
            changeSafemodeRestriction(getDchaStateInternal());
            updateBrowserEnabled();
            changeDefaultUsbFunction(getDchaStateInternal());
            changeDisallowInstallUnknownSource(getDchaCompletedPast());
            updateTraceurEnabled();
        }
        this.mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.checkService("window"));
        this.mColorDisplayController = new ColorDisplayController(context);
    }

    public boolean changeAdbEnable() {
        if (getAdbEnabled() == 0 || BenesseExtension.getDchaState() == 3 || !getDchaCompletedPast() || getInt(BC_PASSWORD_HIT_FLAG) != 0) {
            return false;
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 0);
        return true;
    }

    public void changeDefaultUsbFunction(int i) {
        if (i > 0) {
            ((UsbManager) this.mContext.getSystemService(UsbManager.class)).setScreenUnlockedFunctions(0L);
        }
    }

    public void changeDisallowInstallUnknownSource(boolean z) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager != null) {
            userManager.setUserRestriction("no_install_unknown_sources", z, UserHandle.SYSTEM);
        }
    }

    public void changeSafemodeRestriction(int i) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager != null) {
            userManager.setUserRestriction("no_safe_boot", i > 0, UserHandle.SYSTEM);
        }
    }

    private boolean checkHexFile(String str) {
        FileReader fileReader;
        try {
            try {
                fileReader = new FileReader(str);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String str2 = null;
                while (true) {
                    try {
                        String line = bufferedReader.readLine();
                        if (line == null) {
                            $closeResource(null, bufferedReader);
                            $closeResource(null, fileReader);
                            if (str2.charAt(7) == '0' && str2.charAt(8) == '1') {
                                return true;
                            }
                            Log.e(TAG, "----- last line is not end of file! -----");
                            return false;
                        }
                        if (line.charAt(0) == ';') {
                            Log.w(TAG, "----- found comment line. -----");
                        } else {
                            if (!line.matches(":[a-fA-F0-9]+") || line.length() % 2 == 0) {
                                break;
                            }
                            int iDigit = 0;
                            for (int i = 1; i < line.length() - 1; i += 2) {
                                iDigit += (Character.digit(line.charAt(i), 16) << 4) + Character.digit(line.charAt(i + 1), 16);
                            }
                            if ((iDigit & 255) != 0) {
                                Log.e(TAG, "----- wrong checksum! -----");
                                $closeResource(null, bufferedReader);
                                $closeResource(null, fileReader);
                                return false;
                            }
                            str2 = line;
                        }
                    } catch (Throwable th) {
                        th = th;
                        try {
                            throw th;
                        } catch (Throwable th2) {
                            th = th2;
                            $closeResource(th, bufferedReader);
                            throw th;
                        }
                    }
                }
                Log.e(TAG, "----- invalid data! -----");
                $closeResource(null, bufferedReader);
                $closeResource(null, fileReader);
                return false;
            } catch (Throwable th3) {
                $closeResource(null, fileReader);
                throw th3;
            }
        } catch (Throwable th4) {
            Log.e(TAG, "----- Exception occurred!!! -----", th4);
            return false;
        }
    }

    private boolean executeFwUpdate(final UpdateParams updateParams) {
        if (this.mIsUpdating) {
            Log.e(TAG, "----- FW update : already updating! -----");
            return false;
        }
        this.mIsUpdating = true;
        new Thread(new Runnable(this, updateParams) {
            private final BenesseExtensionService f$0;
            private final BenesseExtensionService.UpdateParams f$1;

            {
                this.f$0 = this;
                this.f$1 = updateParams;
            }

            @Override
            public final void run() {
                BenesseExtensionService.lambda$executeFwUpdate$1(this.f$0, this.f$1);
            }
        }).start();
        return true;
    }

    public int getAdbEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0);
    }

    private int getCompatScreenMode() {
        Point point = new Point();
        try {
            int baseDisplayDensity = this.mWindowManager.getBaseDisplayDensity(0);
            this.mWindowManager.getBaseDisplaySize(0, point);
            for (int i = 0; i < mTable.length; i++) {
                if (baseDisplayDensity == mTable[i][0] && point.x == mTable[i][1] && point.y == mTable[i][2]) {
                    return i;
                }
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "----- getCompatScreenMode() : Exception occurred! -----", e);
            return -1;
        }
    }

    public boolean getDchaCompletedPast() {
        return !BenesseExtension.IGNORE_DCHA_COMPLETED_FILE.exists() && BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists();
    }

    public int getDchaStateInternal() {
        return Settings.System.getInt(this.mContext.getContentResolver(), DCHA_STATE, 0);
    }

    private String getFirmwareVersion(File file) {
        FileReader fileReader;
        if (this.mIsUpdating) {
            return null;
        }
        if (!file.exists()) {
            return "";
        }
        try {
            try {
                fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                try {
                    String line = bufferedReader.readLine();
                    $closeResource(null, bufferedReader);
                    $closeResource(null, fileReader);
                    return line;
                } catch (Throwable th) {
                    th = th;
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        th = th2;
                        $closeResource(th, bufferedReader);
                        throw th;
                    }
                }
            } catch (Throwable th3) {
                $closeResource(null, fileReader);
                throw th3;
            }
        } catch (Throwable th4) {
            return "";
        }
    }

    private String getLanguage() {
        String str = SystemProperties.get(PROPERTY_LOCALE, JAPAN_LOCALE);
        return (str == null || str.equals("")) ? JAPAN_LOCALE : str;
    }

    private UpdateParams getUpdateParams(String str, String str2) {
        byte b;
        UpdateParams updateParams = new UpdateParams();
        int iHashCode = str.hashCode();
        if (iHashCode != 1247406799) {
            b = (iHashCode == 1964675707 && str.equals(BC_TP_FW_UPDATE)) ? (byte) 0 : (byte) -1;
        } else if (str.equals(BC_DT_FW_UPDATE)) {
            b = 1;
        }
        switch (b) {
            case 0:
                updateParams.cmd = new String[]{"/system/bin/.wacom_flash", str2, "1", "i2c-0"};
                updateParams.broadcast = ACTION_TP_FW_UPDATED;
                return updateParams;
            case 1:
                updateParams.cmd = new String[]{"/system/bin/.wac_flash", str2, "i2c-2"};
                updateParams.broadcast = ACTION_DT_FW_UPDATED;
                return updateParams;
            default:
                return null;
        }
    }

    public static void lambda$executeFwUpdate$0(BenesseExtensionService benesseExtensionService, UpdateParams updateParams, int i) {
        benesseExtensionService.mIsUpdating = false;
        benesseExtensionService.mContext.sendBroadcastAsUser(new Intent(updateParams.broadcast).putExtra(EXTRA_RESULT, i), UserHandle.ALL);
    }

    public static void lambda$executeFwUpdate$1(final BenesseExtensionService benesseExtensionService, final UpdateParams updateParams) {
        final int iWaitFor;
        try {
            iWaitFor = Runtime.getRuntime().exec(updateParams.cmd).waitFor();
        } catch (Throwable th) {
            Log.e(TAG, "----- Exception occurred! -----", th);
            iWaitFor = -1;
        }
        benesseExtensionService.mHandler.post(new Runnable(benesseExtensionService, updateParams, iWaitFor) {
            private final BenesseExtensionService f$0;
            private final BenesseExtensionService.UpdateParams f$1;
            private final int f$2;

            {
                this.f$0 = benesseExtensionService;
                this.f$1 = updateParams;
                this.f$2 = iWaitFor;
            }

            @Override
            public final void run() {
                BenesseExtensionService.lambda$executeFwUpdate$0(this.f$0, this.f$1, this.f$2);
            }
        });
    }

    private boolean setCompatScreenMode(int i) {
        if (i < 0 || i >= mTable.length) {
            return false;
        }
        try {
            this.mWindowManager.setForcedDisplayDensityForUser(0, mTable[i][0], -2);
            this.mWindowManager.setForcedDisplaySize(0, mTable[i][1], mTable[i][2]);
            return getCompatScreenMode() == i;
        } catch (RemoteException e) {
            Log.e(TAG, "----- setCompatScreenMode() : Exception occurred! -----", e);
            return false;
        }
    }

    public void updateBrowserEnabled() {
        int i = 2;
        if (!getDchaCompletedPast() && (getDchaStateInternal() == 0 || getAdbEnabled() != 0 || !JAPAN_LOCALE.equals(getLanguage()))) {
            i = 0;
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        int applicationEnabledSetting = packageManager.getApplicationEnabledSetting(PACKAGE_NAME_BROWSER);
        int applicationEnabledSetting2 = packageManager.getApplicationEnabledSetting(PACKAGE_NAME_QSB);
        if (i != applicationEnabledSetting) {
            packageManager.setApplicationEnabledSetting(PACKAGE_NAME_BROWSER, i, 0);
        }
        if (i != applicationEnabledSetting2) {
            packageManager.setApplicationEnabledSetting(PACKAGE_NAME_QSB, i, 0);
        }
    }

    public void updateTraceurEnabled() {
        if (getDchaStateInternal() != 0) {
            return;
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager.getApplicationEnabledSetting(PACKAGE_NAME_TRACEUR) != 0) {
            packageManager.setApplicationEnabledSetting(PACKAGE_NAME_TRACEUR, 0, 0);
        }
    }

    @Override
    public boolean checkPassword(String str) throws Exception {
        byte[] bArr;
        MessageDigest messageDigest;
        byte[] bArr2;
        FileInputStream fileInputStream;
        Throwable th;
        Throwable th2;
        boolean zEquals = false;
        if (str != null) {
            byte[] bArr3 = new byte[64];
            try {
                fileInputStream = new FileInputStream(DCHA_HASH_FILEPATH);
            } catch (IOException e) {
                bArr = (byte[]) DEFAULT_HASH.clone();
            }
            try {
                if (fileInputStream.read(bArr3) != 64) {
                    bArr3 = (byte[]) DEFAULT_HASH.clone();
                }
                $closeResource(null, fileInputStream);
                bArr = bArr3;
                try {
                    messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
                } catch (NoSuchAlgorithmException e2) {
                    messageDigest = null;
                }
                if (messageDigest != null) {
                    messageDigest.reset();
                    byte[] bArrDigest = messageDigest.digest(str.getBytes());
                    byte[] bArr4 = new byte[64];
                    for (int i = 0; i < bArrDigest.length && i < bArr4.length / 2; i++) {
                        int i2 = i * 2;
                        bArr4[i2] = this.HEX_TABLE[(bArrDigest[i] >> 4) & 15];
                        bArr4[i2 + 1] = this.HEX_TABLE[bArrDigest[i] & 15];
                    }
                    bArr2 = bArr4;
                } else {
                    bArr2 = null;
                }
                zEquals = Arrays.equals(bArr, bArr2);
                Log.i(TAG, "password comparison = " + zEquals);
                if (zEquals) {
                    putInt(BC_PASSWORD_HIT_FLAG, 1);
                }
            } catch (Throwable th3) {
                th = th3;
                th2 = null;
                $closeResource(th2, fileInputStream);
                throw th;
            }
        }
        return zEquals;
    }

    @Override
    public int getDchaState() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return getDchaStateInternal();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public int getInt(String str) {
        byte b = 0;
        ?? compatScreenMode = -1;
        compatScreenMode = -1;
        if (str != null) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                switch (str.hashCode()) {
                    case -286987330:
                        b = !str.equals(BC_NIGHTMODE_ACTIVE) ? (byte) -1 : (byte) 1;
                        break;
                    case 367025166:
                        b = !str.equals(BC_NIGHTCOLOR_MAX) ? (byte) -1 : (byte) 2;
                        break;
                    case 367025404:
                        b = !str.equals(BC_NIGHTCOLOR_MIN) ? (byte) -1 : (byte) 3;
                        break;
                    case 1209732899:
                        b = !str.equals(BC_NIGHTCOLOR_CURRENT) ? (byte) -1 : (byte) 4;
                        break;
                    case 1359997191:
                        if (!str.equals(BC_COMPATSCREEN)) {
                            b = -1;
                        }
                        break;
                    case 1664403245:
                        b = !str.equals(BC_PASSWORD_HIT_FLAG) ? (byte) -1 : (byte) 5;
                        break;
                    default:
                        b = -1;
                        break;
                }
                switch (b) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return compatScreenMode;
    }

    @Override
    public String getString(String str) {
        if (str == null) {
            return null;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        byte b = -1;
        try {
            int iHashCode = str.hashCode();
            if (iHashCode != -1125691405) {
                if (iHashCode != 94655307) {
                    if (iHashCode != 600943506) {
                        if (iHashCode == 1361443174 && str.equals(BC_TP_FW_VERSION)) {
                            b = 2;
                        }
                    } else if (str.equals(BC_DT_FW_VERSION)) {
                        b = 3;
                    }
                } else if (str.equals(BC_MAC_ADDRESS)) {
                    b = 0;
                }
            } else if (str.equals(BC_SERIAL_NO)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
                    if (wifiManager == null) {
                        return null;
                    }
                    WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                    if (connectionInfo == null) {
                        return null;
                    }
                    return connectionInfo.getMacAddress();
                case 1:
                    return Build.getSerial();
                case 2:
                    return getFirmwareVersion(SYSFILE_TP_VERSION);
                case 3:
                    return getFirmwareVersion(SYSFILE_DT_VERSION);
                default:
                    return null;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public boolean putInt(String str, int i) {
        byte b;
        if (str == null) {
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int iHashCode = str.hashCode();
            if (iHashCode != -286987330) {
                if (iHashCode != 1209732899) {
                    if (iHashCode != 1359997191) {
                        b = (iHashCode == 1664403245 && str.equals(BC_PASSWORD_HIT_FLAG)) ? (byte) 3 : (byte) -1;
                    } else if (str.equals(BC_COMPATSCREEN)) {
                        b = 0;
                    }
                } else if (str.equals(BC_NIGHTCOLOR_CURRENT)) {
                    b = 2;
                }
            } else if (str.equals(BC_NIGHTMODE_ACTIVE)) {
                b = 1;
            }
            switch (b) {
                case 0:
                    return setCompatScreenMode(i);
                case 1:
                    if (i == 0 || i == 1) {
                        return this.mColorDisplayController.setActivated(i == 1);
                    }
                    return false;
                case 2:
                    return this.mColorDisplayController.setColorTemperature(i);
                case 3:
                    Settings.System.putInt(this.mContext.getContentResolver(), BC_PASSWORD_HIT_FLAG, i);
                    return true;
                default:
                    return false;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public boolean putString(String str, String str2) {
        boolean zExecuteFwUpdate = false;
        if (str != null && str2 != null) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            byte b = -1;
            try {
                int iHashCode = str.hashCode();
                if (iHashCode != 1247406799) {
                    if (iHashCode == 1964675707 && str.equals(BC_TP_FW_UPDATE)) {
                        b = 0;
                    }
                } else if (str.equals(BC_DT_FW_UPDATE)) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                    case 1:
                        String strReplaceFirst = str2.replaceFirst("^/sdcard/", "/data/media/0/");
                        if (new File(strReplaceFirst).isFile()) {
                            if (checkHexFile(strReplaceFirst)) {
                                zExecuteFwUpdate = executeFwUpdate(getUpdateParams(str, strReplaceFirst));
                            }
                            break;
                        } else {
                            Log.e(TAG, "----- putString() : invalid file. name[" + str + "] value[" + str2 + "] -----");
                            break;
                        }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return zExecuteFwUpdate;
    }

    @Override
    public void setDchaState(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Settings.System.putInt(this.mContext.getContentResolver(), DCHA_STATE, i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }
}
