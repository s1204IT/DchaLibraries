package com.android.server.usb;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.net.ConnectivityManager;
import android.os.BenesseExtension;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.FgThread;
import com.android.server.connectivity.Tethering;
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class UsbDeviceManager {
    private static final int ACCESSORY_REQUEST_TIMEOUT = 10000;
    private static final String ACCESSORY_START_MATCH = "DEVPATH=/devices/virtual/misc/usb_accessory";
    private static final String ACM_PORT_INDEX_PATH = "/sys/class/android_usb/android0/f_acm/port_index";
    private static final int AUDIO_MODE_SOURCE = 1;
    private static final String AUDIO_SOURCE_PCM_PATH = "/sys/class/android_usb/android0/f_audio_source/pcm";
    private static final String BOOT_MODE_PROPERTY = "ro.bootmode";
    private static final boolean DEBUG = true;
    private static final String FUNCTIONS_PATH = "/sys/class/android_usb/android0/functions";
    private static final int IFACE_BR_MAIN_ADDED = 1;
    private static final int IFACE_BR_MAIN_NONE = 0;
    private static final String MIDI_ALSA_PATH = "/sys/class/android_usb/android0/f_midi/alsa";
    private static final int MSG_BOOT_COMPLETED = 4;
    private static final int MSG_ENABLE_ACM = 101;
    private static final int MSG_ENABLE_ADB = 1;
    private static final int MSG_SET_BYPASS = 103;
    private static final int MSG_SET_BYPASS_MODE = 102;
    private static final int MSG_SET_CURRENT_FUNCTIONS = 2;
    private static final int MSG_SET_USB_DATA_UNLOCKED = 6;
    private static final int MSG_SYSTEM_READY = 3;
    private static final int MSG_UPDATE_HOST_STATE = 8;
    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_USER_RESTRICTIONS = 7;
    private static final int MSG_USER_SWITCHED = 5;
    private static final String MTP_STATE_MATCH = "DEVPATH=/devices/virtual/misc/mtp_usb";
    private static final String RNDIS_ETH_ADDR_PATH = "/sys/class/android_usb/android0/f_rndis/ethaddr";
    private static final String STATE_PATH = "/sys/class/android_usb/android0/state";
    private static final String TAG = "UsbDeviceManager";
    private static final int UPDATE_DELAY = 1000;
    private static final String USB_CONFIG_PROPERTY = "sys.usb.config";
    private static final String USB_PERSISTENT_CONFIG_PROPERTY = "persist.sys.usb.config";
    private static final String USB_STATE_MATCH = "DEVPATH=/devices/virtual/android_usb/android0";
    private static final String USB_STATE_PROPERTY = "sys.usb.state";
    private static final boolean bEvdoDtViaSupport = SystemProperties.get("ro.boot.opt_c2k_support").equals("1");
    private static int br0State = 0;
    private String[] mAccessoryStrings;
    private boolean mAcmEnabled;
    private String mAcmPortIdx;
    private boolean mAdbEnabled;
    private boolean mAudioSourceEnabled;
    private boolean mBootCompleted;
    private Intent mBroadcastedIntent;
    private final ContentResolver mContentResolver;
    private final Context mContext;

    @GuardedBy("mLock")
    private UsbSettingsManager mCurrentSettings;
    private UsbDebuggingManager mDebuggingManager;
    private UsbHandler mHandler;
    private final boolean mHasUsbAccessory;
    private boolean mIsUsbSimSecurity;
    private int mMidiCard;
    private int mMidiDevice;
    private boolean mMidiEnabled;
    private boolean mMtpAskDisconnect;
    private NotificationManager mNotificationManager;
    private INetworkManagementService mNwService;
    private Map<String, List<Pair<String, String>>> mOemModeMap;
    private final UsbAlsaManager mUsbAlsaManager;
    private String mUsbStorageType;
    private boolean mUseUsbNotification;
    private long mAccessoryModeRequestTime = 0;
    private final Object mLock = new Object();
    private boolean mHwDisconnected = true;
    private boolean mUsbConfigured = false;
    private final UEventObserver mUEventObserver = new UEventObserver() {
        public void onUEvent(UEventObserver.UEvent event) {
            Slog.v(UsbDeviceManager.TAG, "USB UEVENT: " + event.toString());
            String state = event.get("USB_STATE");
            String accessory = event.get("ACCESSORY");
            if (state != null) {
                UsbDeviceManager.this.mHandler.updateState(state);
            } else {
                if (!"START".equals(accessory)) {
                    return;
                }
                Slog.d(UsbDeviceManager.TAG, "got accessory start");
                UsbDeviceManager.this.startAccessoryMode();
            }
        }
    };
    private final BroadcastReceiver mHostReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UsbPort port = intent.getParcelableExtra("port");
            UsbPortStatus status = intent.getParcelableExtra("portStatus");
            UsbDeviceManager.this.mHandler.updateHostState(port, status);
        }
    };

    private native String[] nativeGetAccessoryStrings();

    private native int nativeGetAudioMode();

    private native boolean nativeIsStartRequested();

    private native ParcelFileDescriptor nativeOpenAccessory();

    private class AdbSettingsObserver extends ContentObserver {
        public AdbSettingsObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            boolean enable = Settings.Global.getInt(UsbDeviceManager.this.mContentResolver, "adb_enabled", 0) > 0;
            UsbDeviceManager.this.mHandler.sendMessage(1, enable);
        }
    }

    private class AcmSettingsObserver extends ContentObserver {
        public AcmSettingsObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            int portNum = Settings.Global.getInt(UsbDeviceManager.this.mContentResolver, "acm_enabled", 0);
            UsbDeviceManager.this.mHandler.sendMessage(101, Integer.valueOf(portNum));
        }
    }

    public UsbDeviceManager(Context context, UsbAlsaManager alsaManager) {
        this.mContext = context;
        this.mUsbAlsaManager = alsaManager;
        this.mContentResolver = context.getContentResolver();
        PackageManager pm = this.mContext.getPackageManager();
        this.mHasUsbAccessory = pm.hasSystemFeature("android.hardware.usb.accessory");
        this.mIsUsbSimSecurity = false;
        this.mMtpAskDisconnect = false;
        initRndisAddress();
        readOemUsbOverrideConfig();
        if ("1".equals(SystemProperties.get("ro.mtk_usb_cba_support")) && "OP01".equals(SystemProperties.get("persist.operator.optr"))) {
            Slog.d(TAG, "Have USB SIM Security!!");
            this.mIsUsbSimSecurity = true;
        }
        this.mHandler = new UsbHandler(FgThread.get().getLooper());
        if (nativeIsStartRequested()) {
            Slog.d(TAG, "accessory attached at boot");
            startAccessoryMode();
        }
        boolean secureAdbEnabled = SystemProperties.getBoolean("ro.adb.secure", false);
        boolean dataEncrypted = "1".equals(SystemProperties.get("vold.decrypt"));
        if (secureAdbEnabled && !dataEncrypted) {
            this.mDebuggingManager = new UsbDebuggingManager(context);
        }
        this.mContext.registerReceiver(this.mHostReceiver, new IntentFilter("android.hardware.usb.action.USB_PORT_CHANGED"));
        IBinder b = ServiceManager.getService("network_management");
        this.mNwService = INetworkManagementService.Stub.asInterface(b);
    }

    private UsbSettingsManager getCurrentSettings() {
        UsbSettingsManager usbSettingsManager;
        synchronized (this.mLock) {
            usbSettingsManager = this.mCurrentSettings;
        }
        return usbSettingsManager;
    }

    public void systemReady() {
        Slog.d(TAG, "systemReady");
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        String config = SystemProperties.get(USB_PERSISTENT_CONFIG_PROPERTY, "mtp");
        this.mUsbStorageType = SystemProperties.get("ro.sys.usb.storage.type", "mtp");
        Slog.d(TAG, "systemReady - mUsbStorageType: " + this.mUsbStorageType + ", config: " + config);
        StorageManager storageManager = StorageManager.from(this.mContext);
        StorageVolume primary = storageManager.getPrimaryVolume();
        boolean massStorageSupported = primary != null ? primary.allowMassStorage() : false;
        this.mUseUsbNotification = !massStorageSupported ? this.mContext.getResources().getBoolean(R.^attr-private.closeItemLayout) : false;
        if (this.mUsbStorageType.equals("mass_storage")) {
            StorageVolume[] volumes = storageManager.getVolumeList();
            if (volumes != null) {
                int i = 0;
                while (true) {
                    if (i >= volumes.length) {
                        break;
                    }
                    if (!volumes[i].allowMassStorage()) {
                        i++;
                    } else {
                        Slog.d(TAG, "systemReady - massStorageSupported: " + massStorageSupported);
                        massStorageSupported = true;
                        break;
                    }
                }
            }
            this.mUseUsbNotification = !massStorageSupported;
        } else {
            Slog.d(TAG, "systemReady - MTP(+UMS)");
            this.mUseUsbNotification = true;
        }
        try {
            Settings.Global.putInt(this.mContentResolver, "adb_enabled", this.mAdbEnabled ? 1 : 0);
        } catch (SecurityException e) {
            Slog.d(TAG, "ADB_ENABLED is restricted.");
        }
        this.mHandler.sendEmptyMessage(3);
    }

    public void bootCompleted() {
        Slog.d(TAG, "boot completed");
        this.mHandler.sendEmptyMessage(4);
    }

    public void setCurrentUser(int userId, UsbSettingsManager settings) {
        synchronized (this.mLock) {
            this.mCurrentSettings = settings;
            this.mHandler.obtainMessage(5, userId, 0).sendToTarget();
        }
    }

    public void updateUserRestrictions() {
        this.mHandler.sendEmptyMessage(7);
    }

    private void startAccessoryMode() {
        boolean enableAccessory;
        if (this.mHasUsbAccessory) {
            this.mAccessoryStrings = nativeGetAccessoryStrings();
            boolean enableAudio = nativeGetAudioMode() == 1;
            if (this.mAccessoryStrings == null || this.mAccessoryStrings[0] == null) {
                enableAccessory = false;
            } else {
                enableAccessory = this.mAccessoryStrings[1] != null;
            }
            String functions = null;
            if (enableAccessory && enableAudio) {
                functions = "accessory,audio_source";
            } else if (enableAccessory) {
                functions = "accessory";
            } else if (enableAudio) {
                functions = "audio_source";
            }
            if (functions == null) {
                return;
            }
            this.mAccessoryModeRequestTime = SystemClock.elapsedRealtime();
            setCurrentFunctions(functions);
        }
    }

    private static void initRndisAddress() {
        int[] address = new int[6];
        address[0] = 2;
        String serial = SystemProperties.get("ro.serialno", "1234567890ABCDEF");
        int serialLength = serial.length();
        for (int i = 0; i < serialLength; i++) {
            int i2 = (i % 5) + 1;
            address[i2] = address[i2] ^ serial.charAt(i);
        }
        String addrString = String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", Integer.valueOf(address[0]), Integer.valueOf(address[1]), Integer.valueOf(address[2]), Integer.valueOf(address[3]), Integer.valueOf(address[4]), Integer.valueOf(address[5]));
        try {
            FileUtils.stringToFile(RNDIS_ETH_ADDR_PATH, addrString);
        } catch (IOException e) {
            Slog.e(TAG, "failed to write to /sys/class/android_usb/android0/f_rndis/ethaddr");
        }
    }

    private final class UsbHandler extends Handler {
        private boolean mAdbNotificationShown;
        private Bypass mBypass;
        private boolean mConfigured;
        private boolean mConnected;
        private UsbAccessory mCurrentAccessory;
        private String mCurrentFunctions;
        private boolean mCurrentFunctionsApplied;
        private int mCurrentUser;
        private boolean mHostConnected;
        private Mbim mMbim;
        private boolean mSourcePower;
        private boolean mUsbDataUnlocked;
        private int mUsbNotificationId;
        private boolean mUsbSetBypassWithTether;

        private final class Bypass {
            private static final String ACTION_RADIO_AVAILABLE = "android.intent.action.RADIO_AVAILABLE";
            private static final String ACTION_USB_BYPASS_GETBYPASS = "com.via.bypass.action.getbypass";
            private static final String ACTION_USB_BYPASS_GETBYPASS_RESULT = "com.via.bypass.action.getbypass_result";
            private static final String ACTION_USB_BYPASS_SETBYPASS = "com.via.bypass.action.setbypass";
            private static final String ACTION_USB_BYPASS_SETBYPASS_RESULT = "com.via.bypass.action.setbypass_result";
            private static final String ACTION_USB_BYPASS_SETFUNCTION = "com.via.bypass.action.setfunction";
            private static final String ACTION_USB_BYPASS_SETTETHERFUNCTION = "com.via.bypass.action.settetherfunction";
            private static final String ACTION_VIA_ETS_DEV_CHANGED = "via.cdma.action.ets.dev.changed";
            private static final String ACTION_VIA_SET_ETS_DEV = "via.cdma.action.set.ets.dev";
            private static final String EXTRAL_VIA_ETS_DEV = "via.cdma.extral.ets.dev";
            private static final String USB_FUNCTION_BYPASS = "via_bypass";
            private static final String VALUE_BYPASS_CODE = "com.via.bypass.bypass_code";
            private static final String VALUE_ENABLE_BYPASS = "com.via.bypass.enable_bypass";
            private static final String VALUE_ISSET_BYPASS = "com.via.bypass.isset_bypass";
            private int mBypassToSet;
            private final int[] mBypassCodes = {1, 2, 4, 8, 16};
            private final String[] mBypassName = {"gps", "pcv", "atc", "ets", "data"};
            private int mBypassAll = 0;
            private boolean mEtsDevInUse = false;
            private final BroadcastReceiver mBypassReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int bypass;
                    Slog.i(UsbDeviceManager.TAG, "onReceive=" + intent.getAction());
                    if (intent.getAction() == null) {
                        return;
                    }
                    if (intent.getAction().equals(Bypass.ACTION_USB_BYPASS_SETFUNCTION)) {
                        Boolean enablebypass = Boolean.valueOf(intent.getBooleanExtra(Bypass.VALUE_ENABLE_BYPASS, false));
                        if (enablebypass.booleanValue()) {
                            UsbDeviceManager.this.setCurrentFunctions(Bypass.USB_FUNCTION_BYPASS);
                            return;
                        } else {
                            Bypass.this.closeBypassFunction();
                            return;
                        }
                    }
                    if (intent.getAction().equals(Bypass.ACTION_USB_BYPASS_SETTETHERFUNCTION)) {
                        Boolean enablebypass2 = Boolean.valueOf(intent.getBooleanExtra(Bypass.VALUE_ENABLE_BYPASS, false));
                        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
                        if (enablebypass2.booleanValue()) {
                            Slog.i(UsbDeviceManager.TAG, "Enable the byass with Tethering");
                            UsbHandler.this.mUsbSetBypassWithTether = true;
                            cm.setUsbTethering(true);
                            return;
                        } else {
                            Slog.i(UsbDeviceManager.TAG, "disable the byass with Tethering");
                            Bypass.this.updateBypassMode(0);
                            cm.setUsbTethering(false);
                            return;
                        }
                    }
                    if (intent.getAction().equals(Bypass.ACTION_USB_BYPASS_SETBYPASS)) {
                        int bypasscode = intent.getIntExtra(Bypass.VALUE_BYPASS_CODE, -1);
                        if (bypasscode >= 0 && bypasscode <= Bypass.this.mBypassAll) {
                            Bypass.this.setBypassMode(bypasscode);
                            return;
                        } else {
                            Bypass.this.notifySetBypassResult(false, Bypass.this.getCurrentBypassMode());
                            return;
                        }
                    }
                    if (intent.getAction().equals(Bypass.ACTION_USB_BYPASS_GETBYPASS)) {
                        Intent reintent = new Intent(Bypass.ACTION_USB_BYPASS_GETBYPASS_RESULT);
                        reintent.putExtra(Bypass.VALUE_BYPASS_CODE, Bypass.this.getCurrentBypassMode());
                        UsbDeviceManager.this.mContext.sendBroadcast(reintent);
                        return;
                    }
                    if (intent.getAction().equals(Bypass.ACTION_VIA_ETS_DEV_CHANGED)) {
                        boolean result = intent.getBooleanExtra("set.ets.dev.result", false);
                        if (result) {
                            bypass = Bypass.this.mBypassToSet;
                        } else {
                            bypass = Bypass.this.getCurrentBypassMode();
                        }
                        Message m = Message.obtain(UsbDeviceManager.this.mHandler, 103);
                        m.arg1 = bypass;
                        UsbHandler.this.sendMessage(m);
                        return;
                    }
                    if (!intent.getAction().equals(Bypass.ACTION_RADIO_AVAILABLE) || !Bypass.this.mEtsDevInUse) {
                        return;
                    }
                    Intent reintent2 = new Intent(Bypass.ACTION_VIA_SET_ETS_DEV);
                    reintent2.putExtra(Bypass.EXTRAL_VIA_ETS_DEV, 1);
                    UsbDeviceManager.this.mContext.sendBroadcast(reintent2);
                }
            };
            private File[] mBypassFiles = new File[this.mBypassName.length];

            public Bypass() {
                for (int i = 0; i < this.mBypassName.length; i++) {
                    String path = "/sys/class/usb_rawbulk/" + this.mBypassName[i] + "/enable";
                    this.mBypassFiles[i] = new File(path);
                    this.mBypassAll += this.mBypassCodes[i];
                }
                if (!UsbDeviceManager.bEvdoDtViaSupport) {
                    return;
                }
                IntentFilter intent = new IntentFilter(ACTION_USB_BYPASS_SETFUNCTION);
                intent.addAction(ACTION_USB_BYPASS_SETTETHERFUNCTION);
                intent.addAction(ACTION_USB_BYPASS_SETBYPASS);
                intent.addAction(ACTION_USB_BYPASS_GETBYPASS);
                intent.addAction(ACTION_VIA_ETS_DEV_CHANGED);
                intent.addAction(ACTION_RADIO_AVAILABLE);
                UsbDeviceManager.this.mContext.registerReceiver(this.mBypassReceiver, intent);
            }

            private int getCurrentBypassMode() {
                String code;
                int bypassmode = 0;
                for (int i = 0; i < this.mBypassCodes.length; i++) {
                    try {
                        if (i == 2) {
                            code = SystemProperties.get("sys.cp.bypass.at", "0");
                        } else {
                            code = FileUtils.readTextFile(this.mBypassFiles[i], 0, null);
                        }
                        Slog.d(UsbDeviceManager.TAG, "'" + this.mBypassFiles[i].getAbsolutePath() + "' value is " + code);
                        if (code != null && code.trim().equals("1")) {
                            bypassmode |= this.mBypassCodes[i];
                        }
                    } catch (IOException e) {
                        Slog.e(UsbDeviceManager.TAG, "failed to read bypass mode code!");
                    }
                }
                Slog.d(UsbDeviceManager.TAG, "getCurrentBypassMode()=" + bypassmode);
                return bypassmode;
            }

            private void setBypass(int bypassmode) {
                Slog.d(UsbDeviceManager.TAG, "setBypass bypass = " + bypassmode);
                int bypassResult = getCurrentBypassMode();
                if (bypassmode == bypassResult) {
                    Slog.d(UsbDeviceManager.TAG, "setBypass bypass == oldbypass!!");
                    notifySetBypassResult(true, bypassResult);
                    return;
                }
                for (int i = 0; i < this.mBypassCodes.length; i++) {
                    try {
                        if ((this.mBypassCodes[i] & bypassmode) != 0) {
                            Slog.d(UsbDeviceManager.TAG, "Write '" + this.mBypassFiles[i].getAbsolutePath() + "1");
                            if (i == 2) {
                                SystemProperties.set("sys.cp.bypass.at", "1");
                            } else {
                                FileUtils.stringToFile(this.mBypassFiles[i].getAbsolutePath(), "1");
                            }
                            bypassResult |= this.mBypassCodes[i];
                        } else {
                            Slog.d(UsbDeviceManager.TAG, "Write '" + this.mBypassFiles[i].getAbsolutePath() + "0");
                            if (i == 2) {
                                SystemProperties.set("sys.cp.bypass.at", "0");
                            } else {
                                FileUtils.stringToFile(this.mBypassFiles[i].getAbsolutePath(), "0");
                            }
                            if ((this.mBypassCodes[i] & bypassResult) != 0) {
                                bypassResult ^= this.mBypassCodes[i];
                            }
                        }
                        Slog.d(UsbDeviceManager.TAG, "Write '" + this.mBypassFiles[i].getAbsolutePath() + "' successsfully!");
                    } catch (IOException e) {
                        Slog.e(UsbDeviceManager.TAG, "failed to operate bypass!");
                        notifySetBypassResult(false, bypassResult);
                        return;
                    }
                }
                notifySetBypassResult(true, bypassResult);
                Slog.d(UsbDeviceManager.TAG, "setBypass success bypassResult = " + bypassResult);
            }

            void updateBypassMode(int bypassmode) {
                Slog.d(UsbDeviceManager.TAG, "updateBypassMode");
                if (!setEtsDev(bypassmode)) {
                    setBypass(bypassmode);
                } else {
                    Slog.d(UsbDeviceManager.TAG, "updateBypassMode mBypassToSet = " + this.mBypassToSet);
                    this.mBypassToSet = bypassmode;
                }
            }

            private boolean setEtsDev(int bypass) {
                int oldBypass = getCurrentBypassMode();
                Slog.d(UsbDeviceManager.TAG, "setEtsDev bypass = " + bypass + " oldBypass = " + oldBypass);
                if ((this.mBypassCodes[3] & bypass) != 0 && (this.mBypassCodes[3] & oldBypass) == 0) {
                    Slog.d(UsbDeviceManager.TAG, "setEtsDev mEtsDevInUse = true");
                    Intent reintent = new Intent(ACTION_VIA_SET_ETS_DEV);
                    reintent.putExtra(EXTRAL_VIA_ETS_DEV, 1);
                    UsbDeviceManager.this.mContext.sendBroadcast(reintent);
                    this.mEtsDevInUse = true;
                    return true;
                }
                if ((this.mBypassCodes[3] & bypass) != 0 || (this.mBypassCodes[3] & oldBypass) == 0) {
                    return false;
                }
                Slog.d(UsbDeviceManager.TAG, "setEtsDev mEtsDevInUse = false");
                Intent reintent2 = new Intent(ACTION_VIA_SET_ETS_DEV);
                reintent2.putExtra(EXTRAL_VIA_ETS_DEV, 0);
                UsbDeviceManager.this.mContext.sendBroadcast(reintent2);
                this.mEtsDevInUse = false;
                return true;
            }

            private void setBypassMode(int bypassmode) {
                Slog.d(UsbDeviceManager.TAG, "setBypassMode()=" + bypassmode);
                Message m = Message.obtain(UsbDeviceManager.this.mHandler, 102);
                m.arg1 = bypassmode;
                UsbHandler.this.sendMessage(m);
            }

            private void notifySetBypassResult(Boolean isset, int bypassCode) {
                if (!UsbDeviceManager.this.mBootCompleted) {
                    return;
                }
                Intent intent = new Intent(ACTION_USB_BYPASS_SETBYPASS_RESULT);
                intent.putExtra(VALUE_ISSET_BYPASS, isset);
                intent.putExtra(VALUE_BYPASS_CODE, bypassCode);
                UsbDeviceManager.this.mContext.sendBroadcast(intent);
            }

            void closeBypassFunction() {
                Slog.d(UsbDeviceManager.TAG, "closeBypassFunction() CurrentFunctions = " + UsbHandler.this.mCurrentFunctions + ",DefaultFunctions=" + UsbHandler.this.getDefaultFunctions());
                updateBypassMode(0);
                if (UsbHandler.this.mCurrentFunctions.contains(USB_FUNCTION_BYPASS)) {
                    UsbHandler.this.setEnabledFunctions(null, false);
                }
            }
        }

        private final class Mbim {
            private static final String ACTION_USB_MBIM_SETFUNCTION = "com.mbim.action.setfunction";
            private static final String USB_FUNCTION_MBIM = "mbim_dun";
            private static final String VALUE_ENABLE_MBIM = "com.mbim.enable";
            private final BroadcastReceiver mMbimReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Slog.i(UsbDeviceManager.TAG, "onReceive=" + intent.getAction());
                    if (intent.getAction() == null || !intent.getAction().equals(Mbim.ACTION_USB_MBIM_SETFUNCTION)) {
                        return;
                    }
                    int enable_mbim = intent.getIntExtra(Mbim.VALUE_ENABLE_MBIM, 0);
                    if (enable_mbim == 1) {
                        UsbDeviceManager.this.setCurrentFunctions(Mbim.USB_FUNCTION_MBIM);
                    } else {
                        Mbim.this.closeMbimFunction();
                    }
                }
            };

            public Mbim() {
                IntentFilter intent = new IntentFilter(ACTION_USB_MBIM_SETFUNCTION);
                UsbDeviceManager.this.mContext.registerReceiver(this.mMbimReceiver, intent);
            }

            void closeMbimFunction() {
                Slog.d(UsbDeviceManager.TAG, "closeMbimFunction() CurrentFunctions = " + UsbHandler.this.mCurrentFunctions + ",DefaultFunctions=" + UsbHandler.this.getDefaultFunctions());
                if (UsbHandler.this.mCurrentFunctions.contains(USB_FUNCTION_MBIM)) {
                    UsbHandler.this.setEnabledFunctions(null, false);
                }
            }
        }

        public UsbHandler(Looper looper) throws Throwable {
            super(looper);
            this.mUsbDataUnlocked = true;
            this.mCurrentUser = -10000;
            this.mUsbSetBypassWithTether = false;
            try {
                if (UsbDeviceManager.bEvdoDtViaSupport) {
                    this.mBypass = new Bypass();
                }
                this.mMbim = new Mbim();
                this.mCurrentFunctions = SystemProperties.get(UsbDeviceManager.USB_CONFIG_PROPERTY, "none");
                if ("none".equals(this.mCurrentFunctions)) {
                    this.mCurrentFunctions = "mtp";
                }
                this.mCurrentFunctionsApplied = this.mCurrentFunctions.equals(SystemProperties.get(UsbDeviceManager.USB_STATE_PROPERTY));
                UsbDeviceManager.this.mAdbEnabled = UsbManager.containsFunction(getDefaultFunctions(), "adb");
                UsbDeviceManager.this.mAcmEnabled = UsbManager.containsFunction(getDefaultFunctions(), "acm");
                UsbDeviceManager.this.mAcmPortIdx = "";
                setEnabledFunctions(null, false);
                String state = FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim();
                updateState(state);
                String value = SystemProperties.get("persist.radio.port_index", "");
                Slog.d(UsbDeviceManager.TAG, "persist.radio.port_index:" + value);
                if (value != null && !value.isEmpty() && validPortNum(value) > 0) {
                    UsbDeviceManager.this.mAcmPortIdx = value;
                    writeFile(UsbDeviceManager.ACM_PORT_INDEX_PATH, UsbDeviceManager.this.mAcmPortIdx);
                    String usbFunctions = UsbManager.addFunction(this.mCurrentFunctions, "acm");
                    SystemProperties.set(UsbDeviceManager.USB_CONFIG_PROPERTY, usbFunctions);
                }
                UsbDeviceManager.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("adb_enabled"), false, UsbDeviceManager.this.new AdbSettingsObserver());
                UsbDeviceManager.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("acm_enabled"), false, UsbDeviceManager.this.new AcmSettingsObserver());
                UsbDeviceManager.this.mUEventObserver.startObserving(UsbDeviceManager.USB_STATE_MATCH);
                UsbDeviceManager.this.mUEventObserver.startObserving(UsbDeviceManager.ACCESSORY_START_MATCH);
                UsbDeviceManager.this.mUEventObserver.startObserving(UsbDeviceManager.MTP_STATE_MATCH);
            } catch (Exception e) {
                Slog.e(UsbDeviceManager.TAG, "Error initializing UsbHandler", e);
            }
        }

        public void sendMessage(int what, boolean arg) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg ? 1 : 0;
            sendMessage(m);
        }

        public void sendMessage(int what, Object arg) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.obj = arg;
            sendMessage(m);
        }

        public void updateState(String state) {
            int connected;
            int configured;
            if ("HWDISCONNECTED".equals(state)) {
                connected = 0;
                configured = 0;
                UsbDeviceManager.this.mHwDisconnected = true;
            } else if ("DISCONNECTED".equals(state)) {
                connected = 0;
                configured = 0;
                UsbDeviceManager.this.mHwDisconnected = false;
            } else if ("CONNECTED".equals(state)) {
                connected = 1;
                configured = 0;
                UsbDeviceManager.this.mHwDisconnected = false;
            } else if ("CONFIGURED".equals(state)) {
                connected = 1;
                configured = 1;
                UsbDeviceManager.this.mHwDisconnected = false;
            } else {
                if ("MTPASKDISCONNECT".equals(state)) {
                    Slog.w(UsbDeviceManager.TAG, "MTPASKDISCONNECT");
                    UsbDeviceManager.this.mMtpAskDisconnect = true;
                    UsbDeviceManager.this.setCurrentFunctions(this.mCurrentFunctions);
                    return;
                }
                Slog.e(UsbDeviceManager.TAG, "unknown state " + state);
                return;
            }
            removeMessages(0);
            Message msg = Message.obtain(this, 0);
            msg.arg1 = connected;
            msg.arg2 = configured;
            sendMessageDelayed(msg, connected == 0 ? 1000 : 0);
        }

        public void updateHostState(UsbPort port, UsbPortStatus status) {
            boolean hostConnected = status.getCurrentDataRole() == 1;
            boolean sourcePower = status.getCurrentPowerRole() == 1;
            obtainMessage(8, hostConnected ? 1 : 0, sourcePower ? 1 : 0).sendToTarget();
        }

        private boolean waitForState(String state) {
            String value = null;
            for (int i = 0; i < 20; i++) {
                value = SystemProperties.get(UsbDeviceManager.USB_STATE_PROPERTY);
                if (state.equals(value)) {
                    return true;
                }
                SystemClock.sleep(50L);
            }
            Slog.e(UsbDeviceManager.TAG, "waitForState(" + state + ") FAILED: got " + value);
            return false;
        }

        private boolean setUsbConfig(String config) {
            Slog.d(UsbDeviceManager.TAG, "setUsbConfig(" + config + ")");
            SystemProperties.set(UsbDeviceManager.USB_CONFIG_PROPERTY, config);
            return waitForState(config);
        }

        private void setUsbDataUnlocked(boolean enable) {
            Slog.d(UsbDeviceManager.TAG, "setUsbDataUnlocked: " + enable);
            this.mUsbDataUnlocked = true;
            setEnabledFunctions(this.mCurrentFunctions, true);
        }

        private void setAdbEnabled(boolean enable) {
            Slog.d(UsbDeviceManager.TAG, "setAdbEnabled: " + enable);
            if (enable != UsbDeviceManager.this.mAdbEnabled) {
                UsbDeviceManager.this.mAdbEnabled = enable;
                String oldFunctions = getDefaultFunctions();
                String newFunctions = applyAdbFunction(oldFunctions);
                if (!oldFunctions.equals(newFunctions)) {
                    SystemProperties.set(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY, newFunctions);
                }
                setEnabledFunctions(this.mCurrentFunctions, false);
                updateAdbNotification();
            }
            if (UsbDeviceManager.this.mDebuggingManager == null) {
                return;
            }
            UsbDeviceManager.this.mDebuggingManager.setAdbEnabled(UsbDeviceManager.this.mAdbEnabled);
        }

        private void setAcmEnabled(boolean enable) {
            Slog.d(UsbDeviceManager.TAG, "setAcmEnabled: " + enable);
            if (enable == UsbDeviceManager.this.mAcmEnabled) {
                return;
            }
            UsbDeviceManager.this.mAcmEnabled = enable;
            setEnabledFunctions(this.mCurrentFunctions, true);
        }

        private void writeFile(String path, String data) throws Throwable {
            FileOutputStream fos;
            FileOutputStream fos2 = null;
            try {
                try {
                    fos = new FileOutputStream(path);
                } catch (IOException e) {
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                fos.write(data.getBytes());
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e2) {
                        Slog.w(UsbDeviceManager.TAG, "Unable to close fos at path: " + path);
                    }
                }
            } catch (IOException e3) {
                fos2 = fos;
                Slog.w(UsbDeviceManager.TAG, "Unable to write " + path);
                if (fos2 == null) {
                    return;
                }
                try {
                    fos2.close();
                } catch (IOException e4) {
                    Slog.w(UsbDeviceManager.TAG, "Unable to close fos at path: " + path);
                }
            } catch (Throwable th2) {
                th = th2;
                fos2 = fos;
                if (fos2 != null) {
                    try {
                        fos2.close();
                    } catch (IOException e5) {
                        Slog.w(UsbDeviceManager.TAG, "Unable to close fos at path: " + path);
                    }
                }
                throw th;
            }
        }

        private void setEnabledFunctions(String functions, boolean forceRestart) {
            Slog.d(UsbDeviceManager.TAG, "setEnabledFunctions functions=" + functions + ", forceRestart=" + forceRestart);
            if (UsbDeviceManager.this.mIsUsbSimSecurity) {
                String value = SystemProperties.get("persist.sys.usb.activation", "no");
                if (value.equals("no")) {
                    Slog.d(UsbDeviceManager.TAG, "Usb is non-activated! Not allowed to set USB func.");
                    return;
                }
            }
            if (UsbManager.containsFunction(this.mCurrentFunctions, "bicr")) {
                if (this.mCurrentFunctions.equals(functions)) {
                    forceRestart = false;
                } else {
                    Slog.d(UsbDeviceManager.TAG, "setEnabledFunctions - [CLEAN USB BICR SETTING]");
                    SystemProperties.set("sys.usb.bicr", "no");
                }
            }
            String br0Name = SystemProperties.get(Tethering.SYSTEM_PROPERTY_MD_DRT_BRIDGE_NAME);
            if (SystemProperties.get(Tethering.SYSTEM_PROPERTY_MD_DRT_TETHER_SUPPORT).equals("1") && br0Name != null && !br0Name.isEmpty()) {
                if (functions != null && UsbManager.containsFunction(functions, "rndis") && !UsbManager.containsFunction(this.mCurrentFunctions, "rndis")) {
                    Slog.i(UsbDeviceManager.TAG, "addBridge");
                    int unused = UsbDeviceManager.br0State = 1;
                    try {
                        UsbDeviceManager.this.mNwService.addBridge(br0Name);
                    } catch (RemoteException e) {
                        Slog.e(UsbDeviceManager.TAG, "Error addBridge: ", e);
                    }
                } else if ((functions == null || !UsbManager.containsFunction(functions, "rndis")) && UsbManager.containsFunction(this.mCurrentFunctions, "rndis")) {
                    Slog.i(UsbDeviceManager.TAG, "deleteBridge");
                    try {
                        UsbDeviceManager.this.mNwService.deleteBridge(br0Name);
                    } catch (RemoteException e2) {
                        Slog.e(UsbDeviceManager.TAG, "Error deleteBridge: ", e2);
                    }
                    int unused2 = UsbDeviceManager.br0State = 0;
                }
            }
            String oldFunctions = this.mCurrentFunctions;
            boolean oldFunctionsApplied = this.mCurrentFunctionsApplied;
            if (trySetEnabledFunctions(functions, forceRestart)) {
                return;
            }
            if (oldFunctionsApplied && !oldFunctions.equals(functions)) {
                Slog.e(UsbDeviceManager.TAG, "Failsafe 1: Restoring previous USB functions.");
                if (trySetEnabledFunctions(oldFunctions, false)) {
                    return;
                }
            }
            Slog.e(UsbDeviceManager.TAG, "Failsafe 2: Restoring default USB functions.");
            if (trySetEnabledFunctions(null, false)) {
                return;
            }
            Slog.e(UsbDeviceManager.TAG, "Failsafe 3: Restoring empty function list (with ADB if enabled).");
            if (trySetEnabledFunctions("none", false)) {
                return;
            }
            Slog.e(UsbDeviceManager.TAG, "Unable to set any USB functions!");
        }

        private boolean trySetEnabledFunctions(String functions, boolean forceRestart) throws Throwable {
            if (functions == null) {
                this.mUsbSetBypassWithTether = false;
                functions = getDefaultFunctions();
            }
            if (UsbDeviceManager.bEvdoDtViaSupport) {
                if ((UsbManager.containsFunction(functions, "rndis") || UsbManager.containsFunction(functions, "eem")) && this.mUsbSetBypassWithTether) {
                    functions = UsbManager.addFunction(functions, "via_bypass");
                    Slog.d(UsbDeviceManager.TAG, "add the bypass functions to tethering : " + functions);
                }
                this.mUsbSetBypassWithTether = false;
            }
            String functions2 = UsbDeviceManager.this.applyOemOverrideFunction(applyAcmFunction(applyAdbFunction(functions)));
            if (UsbDeviceManager.this.mIsUsbSimSecurity) {
                String value = SystemProperties.get("persist.sys.usb.activation", "no");
                if (value.equals("no")) {
                    Slog.d(UsbDeviceManager.TAG, "Usb is non-activated!");
                    functions2 = UsbManager.removeFunction(UsbManager.removeFunction(UsbManager.removeFunction(functions2, "adb"), "acm"), "dual_acm");
                }
            }
            if (!this.mCurrentFunctions.equals(functions2) || !this.mCurrentFunctionsApplied || forceRestart) {
                Slog.i(UsbDeviceManager.TAG, "Setting USB config to " + functions2);
                this.mCurrentFunctions = functions2;
                this.mCurrentFunctionsApplied = false;
                SystemClock.sleep(100L);
                setUsbConfig("none");
                SystemClock.sleep(100L);
                if (!setUsbConfig(functions2)) {
                    Slog.e(UsbDeviceManager.TAG, "Failed to switch USB config to " + functions2);
                    String br0Name = SystemProperties.get(Tethering.SYSTEM_PROPERTY_MD_DRT_BRIDGE_NAME);
                    if (UsbDeviceManager.br0State == 1) {
                        try {
                            Slog.e(UsbDeviceManager.TAG, "deleteBridge");
                            UsbDeviceManager.this.mNwService.deleteBridge(br0Name);
                        } catch (RemoteException e) {
                            Slog.e(UsbDeviceManager.TAG, "Error deleteBridge: ", e);
                        }
                        int unused = UsbDeviceManager.br0State = 0;
                    }
                    return false;
                }
                this.mCurrentFunctionsApplied = true;
            }
            return true;
        }

        private String applyAdbFunction(String functions) {
            if (UsbDeviceManager.this.mAdbEnabled) {
                return UsbManager.addFunction(functions, "adb");
            }
            return UsbManager.removeFunction(functions, "adb");
        }

        private int validPortNum(String port) {
            String[] tmp = port.split(",");
            int portNum = 0;
            for (int i = 0; i < tmp.length; i++) {
                if (Integer.valueOf(tmp[i]).intValue() > 0 && Integer.valueOf(tmp[i]).intValue() < 5) {
                    portNum++;
                }
            }
            if (portNum == tmp.length) {
                return portNum;
            }
            return 0;
        }

        private String applyAcmFunction(String functions) throws Throwable {
            String functions2;
            String acmIdx = SystemProperties.get("sys.usb.acm_idx", "");
            Slog.d(UsbDeviceManager.TAG, "applyAcmFunction - sys.usb.acm_idx=" + acmIdx + ",mAcmPortIdx=" + UsbDeviceManager.this.mAcmPortIdx);
            if (UsbDeviceManager.this.mAcmEnabled || ((acmIdx != null && !acmIdx.isEmpty()) || (UsbDeviceManager.this.mAcmPortIdx != null && !UsbDeviceManager.this.mAcmPortIdx.isEmpty()))) {
                int portNum = 0;
                String portStr = "";
                if (!acmIdx.isEmpty()) {
                    portNum = validPortNum(acmIdx);
                    if (portNum > 0) {
                        portStr = acmIdx;
                        UsbDeviceManager.this.mAcmPortIdx = acmIdx;
                    }
                } else if (!UsbDeviceManager.this.mAcmPortIdx.isEmpty() && (portNum = validPortNum(UsbDeviceManager.this.mAcmPortIdx)) > 0) {
                    portStr = UsbDeviceManager.this.mAcmPortIdx;
                }
                Slog.d(UsbDeviceManager.TAG, "applyAcmFunction - port_num=" + portNum);
                if (portNum > 0) {
                    Slog.d(UsbDeviceManager.TAG, "applyAcmFunction - Write port_str=" + portStr);
                    writeFile(UsbDeviceManager.ACM_PORT_INDEX_PATH, portStr);
                }
                String functions3 = UsbManager.removeFunction(UsbManager.removeFunction(functions, "acm"), "dual_acm");
                String tmp = portNum == 2 ? "dual_acm" : "acm";
                functions2 = UsbManager.addFunction(functions3, tmp);
            } else {
                functions2 = UsbManager.removeFunction(UsbManager.removeFunction(functions, "acm"), "dual_acm");
            }
            Slog.d(UsbDeviceManager.TAG, "applyAcmFunction - functions: " + functions2);
            return functions2;
        }

        private boolean isUsbTransferAllowed() {
            UserManager userManager = (UserManager) UsbDeviceManager.this.mContext.getSystemService("user");
            return !userManager.hasUserRestriction("no_usb_file_transfer");
        }

        private void updateCurrentAccessory() {
            boolean enteringAccessoryMode = UsbDeviceManager.this.mAccessoryModeRequestTime > 0 && SystemClock.elapsedRealtime() < UsbDeviceManager.this.mAccessoryModeRequestTime + JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
            Slog.d(UsbDeviceManager.TAG, "updateCurrentAccessory: enteringAccessoryMode = " + enteringAccessoryMode + ", mAccessoryModeRequestTime = " + UsbDeviceManager.this.mAccessoryModeRequestTime + ", mConfigured = " + this.mConfigured);
            if (this.mConfigured && enteringAccessoryMode) {
                if (UsbDeviceManager.this.mAccessoryStrings == null) {
                    Slog.e(UsbDeviceManager.TAG, "nativeGetAccessoryStrings failed");
                    return;
                }
                this.mCurrentAccessory = new UsbAccessory(UsbDeviceManager.this.mAccessoryStrings);
                Slog.d(UsbDeviceManager.TAG, "entering USB accessory mode: " + this.mCurrentAccessory);
                if (UsbDeviceManager.this.mBootCompleted) {
                    UsbDeviceManager.this.getCurrentSettings().accessoryAttached(this.mCurrentAccessory);
                    UsbDeviceManager.this.mAccessoryModeRequestTime = 0L;
                    return;
                }
                return;
            }
            if (enteringAccessoryMode && this.mConnected) {
                Slog.d(UsbDeviceManager.TAG, "USB Accessory Wrong state!!, need to FIXME");
                return;
            }
            Slog.d(UsbDeviceManager.TAG, "exited USB accessory mode");
            setEnabledFunctions(null, false);
            if (this.mCurrentAccessory != null) {
                if (UsbDeviceManager.this.mBootCompleted) {
                    UsbDeviceManager.this.getCurrentSettings().accessoryDetached(this.mCurrentAccessory);
                }
                this.mCurrentAccessory = null;
                UsbDeviceManager.this.mAccessoryStrings = null;
            }
        }

        private boolean isUsbStateChanged(Intent intent) {
            Set<String> keySet = intent.getExtras().keySet();
            if (UsbDeviceManager.this.mBroadcastedIntent == null) {
                for (String key : keySet) {
                    if (intent.getBooleanExtra(key, false) && !"mtp".equals(key)) {
                        return true;
                    }
                }
            } else {
                if (!keySet.equals(UsbDeviceManager.this.mBroadcastedIntent.getExtras().keySet())) {
                    return true;
                }
                for (String key2 : keySet) {
                    if (intent.getBooleanExtra(key2, false) != UsbDeviceManager.this.mBroadcastedIntent.getBooleanExtra(key2, false)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void updateUsbStateBroadcastIfNeeded() {
            Intent intent = new Intent("android.hardware.usb.action.USB_STATE");
            intent.addFlags(805306368);
            intent.putExtra("connected", this.mConnected);
            intent.putExtra("host_connected", this.mHostConnected);
            intent.putExtra("configured", this.mConfigured);
            intent.putExtra("unlocked", true);
            intent.putExtra("USB_HW_DISCONNECTED", UsbDeviceManager.this.mHwDisconnected);
            if (this.mCurrentFunctions != null) {
                String[] functions = this.mCurrentFunctions.split(",");
                for (String function : functions) {
                    if (!"none".equals(function)) {
                        intent.putExtra(function, true);
                    }
                }
            }
            if (!isUsbStateChanged(intent)) {
                Slog.d(UsbDeviceManager.TAG, "skip broadcasting " + intent + " extras: " + intent.getExtras());
                return;
            }
            Slog.d(UsbDeviceManager.TAG, "broadcasting " + intent + " extras: " + intent.getExtras());
            UsbDeviceManager.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            UsbDeviceManager.this.mBroadcastedIntent = intent;
        }

        private void updateUsbFunctions() throws Throwable {
            updateAudioSourceFunction();
            updateMidiFunction();
        }

        private void updateAudioSourceFunction() throws Throwable {
            Scanner scanner;
            boolean zContainsFunction = this.mConnected ? UsbManager.containsFunction(this.mCurrentFunctions, "audio_source") : false;
            if (zContainsFunction == UsbDeviceManager.this.mAudioSourceEnabled) {
                return;
            }
            if ((!zContainsFunction || !this.mConfigured) && zContainsFunction) {
                return;
            }
            int card = -1;
            int device = -1;
            if (zContainsFunction) {
                Scanner scanner2 = null;
                try {
                    try {
                        scanner = new Scanner(new File(UsbDeviceManager.AUDIO_SOURCE_PCM_PATH));
                    } catch (FileNotFoundException e) {
                        e = e;
                    }
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    card = scanner.nextInt();
                    device = scanner.nextInt();
                    if (scanner != null) {
                        scanner.close();
                    }
                } catch (FileNotFoundException e2) {
                    e = e2;
                    scanner2 = scanner;
                    Slog.e(UsbDeviceManager.TAG, "could not open audio source PCM file", e);
                    if (scanner2 != null) {
                        scanner2.close();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    scanner2 = scanner;
                    if (scanner2 != null) {
                        scanner2.close();
                    }
                    throw th;
                }
            }
            UsbDeviceManager.this.mUsbAlsaManager.setAccessoryAudioState(zContainsFunction, card, device);
            UsbDeviceManager.this.mAudioSourceEnabled = zContainsFunction;
        }

        private void updateMidiFunction() throws Throwable {
            Scanner scanner;
            boolean zContainsFunction = this.mConnected ? UsbManager.containsFunction(this.mCurrentFunctions, "midi") : false;
            if (zContainsFunction != UsbDeviceManager.this.mMidiEnabled) {
                if (zContainsFunction) {
                    Scanner scanner2 = null;
                    try {
                        try {
                            scanner = new Scanner(new File(UsbDeviceManager.MIDI_ALSA_PATH));
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (FileNotFoundException e) {
                        e = e;
                    }
                    try {
                        UsbDeviceManager.this.mMidiCard = scanner.nextInt();
                        UsbDeviceManager.this.mMidiDevice = scanner.nextInt();
                        if (scanner != null) {
                            scanner.close();
                        }
                    } catch (FileNotFoundException e2) {
                        e = e2;
                        scanner2 = scanner;
                        Slog.e(UsbDeviceManager.TAG, "could not open MIDI PCM file", e);
                        zContainsFunction = false;
                        if (scanner2 != null) {
                            scanner2.close();
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        scanner2 = scanner;
                        if (scanner2 != null) {
                            scanner2.close();
                        }
                        throw th;
                    }
                }
                UsbDeviceManager.this.mMidiEnabled = zContainsFunction;
            }
            UsbDeviceManager.this.mUsbAlsaManager.setPeripheralMidiState(UsbDeviceManager.this.mMidiEnabled ? this.mConfigured : false, UsbDeviceManager.this.mMidiCard, UsbDeviceManager.this.mMidiDevice);
        }

        @Override
        public void handleMessage(Message msg) throws Throwable {
            boolean zContainsFunction;
            switch (msg.what) {
                case 0:
                    this.mConnected = msg.arg1 == 1;
                    this.mConfigured = msg.arg2 == 1;
                    UsbDeviceManager.this.mUsbConfigured = this.mConfigured;
                    updateUsbNotification();
                    updateAdbNotification();
                    if (UsbManager.containsFunction(this.mCurrentFunctions, "accessory")) {
                        updateCurrentAccessory();
                    } else if (!this.mConnected) {
                        setEnabledFunctions(null, false);
                    }
                    if (UsbDeviceManager.this.mBootCompleted) {
                        updateUsbStateBroadcastIfNeeded();
                        updateUsbFunctions();
                    }
                    if (UsbDeviceManager.bEvdoDtViaSupport && !this.mConnected) {
                        this.mBypass.updateBypassMode(0);
                        break;
                    }
                    break;
                case 1:
                    setAdbEnabled(msg.arg1 == 1);
                    break;
                case 2:
                    String functions = (String) msg.obj;
                    if (UsbDeviceManager.this.mMtpAskDisconnect) {
                        setEnabledFunctions(functions, true);
                        UsbDeviceManager.this.mMtpAskDisconnect = false;
                    } else {
                        setEnabledFunctions(functions, false);
                    }
                    break;
                case 3:
                    updateUsbNotification();
                    updateAdbNotification();
                    updateUsbStateBroadcastIfNeeded();
                    updateUsbFunctions();
                    break;
                case 4:
                    UsbDeviceManager.this.mBootCompleted = true;
                    if (this.mCurrentAccessory != null) {
                        UsbDeviceManager.this.getCurrentSettings().accessoryAttached(this.mCurrentAccessory);
                    }
                    if (UsbDeviceManager.this.mDebuggingManager != null) {
                        UsbDeviceManager.this.mDebuggingManager.setAdbEnabled(UsbDeviceManager.this.mAdbEnabled);
                    }
                    break;
                case 5:
                    if (this.mCurrentUser != msg.arg1) {
                        if (UsbManager.containsFunction(this.mCurrentFunctions, "mtp")) {
                            zContainsFunction = true;
                        } else {
                            zContainsFunction = UsbManager.containsFunction(this.mCurrentFunctions, "ptp");
                        }
                        if (this.mUsbDataUnlocked && zContainsFunction && this.mCurrentUser != -10000) {
                            Slog.v(UsbDeviceManager.TAG, "Current user switched to " + this.mCurrentUser + "; resetting USB host stack for MTP or PTP");
                            this.mUsbDataUnlocked = false;
                            setEnabledFunctions(this.mCurrentFunctions, true);
                        }
                        this.mCurrentUser = msg.arg1;
                    }
                    break;
                case 6:
                    setUsbDataUnlocked(msg.arg1 == 1);
                    break;
                case 7:
                    setEnabledFunctions(this.mCurrentFunctions, false);
                    break;
                case 8:
                    this.mHostConnected = msg.arg1 == 1;
                    this.mSourcePower = msg.arg2 == 1;
                    updateUsbNotification();
                    if (UsbDeviceManager.this.mBootCompleted) {
                        updateUsbStateBroadcastIfNeeded();
                    }
                    break;
                case 101:
                    int portNum = ((Integer) msg.obj).intValue();
                    if (portNum >= 1 && portNum <= 4) {
                        UsbDeviceManager.this.mAcmPortIdx = String.valueOf(portNum);
                    } else if (portNum == 5) {
                        UsbDeviceManager.this.mAcmPortIdx = "1,3";
                    } else {
                        UsbDeviceManager.this.mAcmPortIdx = "";
                    }
                    Slog.d(UsbDeviceManager.TAG, "mAcmPortIdx=" + UsbDeviceManager.this.mAcmPortIdx);
                    setAcmEnabled(UsbDeviceManager.this.mAcmPortIdx.isEmpty() ? false : true);
                    break;
                case 102:
                    if (UsbDeviceManager.bEvdoDtViaSupport) {
                        this.mBypass.updateBypassMode(msg.arg1);
                    }
                    break;
                case 103:
                    if (UsbDeviceManager.bEvdoDtViaSupport) {
                        this.mBypass.setBypass(msg.arg1);
                    }
                    break;
            }
        }

        public UsbAccessory getCurrentAccessory() {
            return this.mCurrentAccessory;
        }

        private void updateUsbNotification() {
            if (UsbDeviceManager.this.mNotificationManager == null || !UsbDeviceManager.this.mUseUsbNotification || "0".equals(SystemProperties.get("persist.charging.notify"))) {
                return;
            }
            if (UsbDeviceManager.this.mIsUsbSimSecurity) {
                String value = SystemProperties.get("persist.sys.usb.activation", "no");
                if (value.equals("no")) {
                    return;
                }
            }
            int id = 0;
            Resources r = UsbDeviceManager.this.mContext.getResources();
            if (this.mConnected) {
                id = !this.mUsbDataUnlocked ? this.mSourcePower ? R.string.face_name_template : R.string.face_icon_content_description : UsbManager.containsFunction(this.mCurrentFunctions, "mtp") ? R.string.face_or_screen_lock_app_setting_name : UsbManager.containsFunction(this.mCurrentFunctions, "ptp") ? R.string.face_or_screen_lock_dialog_default_subtitle : UsbManager.containsFunction(this.mCurrentFunctions, "midi") ? R.string.face_recalibrate_notification_content : UsbManager.containsFunction(this.mCurrentFunctions, "accessory") ? R.string.face_recalibrate_notification_name : UsbManager.containsFunction(this.mCurrentFunctions, "mass_storage") ? com.mediatek.internal.R.string.usb_ums_notification_title : UsbManager.containsFunction(this.mCurrentFunctions, "bicr") ? com.mediatek.internal.R.string.usb_cd_installer_notification_title : this.mSourcePower ? R.string.face_name_template : R.string.face_icon_content_description;
            } else if (this.mSourcePower) {
                id = R.string.face_name_template;
            }
            if (id != this.mUsbNotificationId) {
                if (this.mUsbNotificationId != 0) {
                    UsbDeviceManager.this.mNotificationManager.cancelAsUser(null, this.mUsbNotificationId, UserHandle.ALL);
                    this.mUsbNotificationId = 0;
                }
                if (id != 0) {
                    CharSequence message = r.getText(R.string.face_recalibrate_notification_title);
                    CharSequence title = r.getText(id);
                    Intent intent = Intent.makeRestartActivityTask(new ComponentName("com.android.settings", "com.android.settings.deviceinfo.UsbModeChooserActivity"));
                    PendingIntent pi = PendingIntent.getActivityAsUser(UsbDeviceManager.this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
                    if (BenesseExtension.getDchaState() != 0) {
                        pi = null;
                    }
                    Notification notification = new Notification.Builder(UsbDeviceManager.this.mContext).setSmallIcon(R.drawable.list_selector_background_longpress_light).setWhen(0L).setOngoing(true).setTicker(title).setDefaults(0).setPriority(-2).setColor(UsbDeviceManager.this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(message).setContentIntent(pi).setVisibility(1).build();
                    UsbDeviceManager.this.mNotificationManager.notifyAsUser(null, id, notification, UserHandle.ALL);
                    this.mUsbNotificationId = id;
                }
            }
        }

        private void updateAdbNotification() {
            if (UsbDeviceManager.this.mNotificationManager == null) {
                return;
            }
            if (UsbDeviceManager.this.mIsUsbSimSecurity) {
                String value = SystemProperties.get("persist.sys.usb.activation", "no");
                if (value.equals("no")) {
                    return;
                }
            }
            if (!UsbDeviceManager.this.mAdbEnabled || !this.mConnected) {
                if (this.mAdbNotificationShown) {
                    this.mAdbNotificationShown = false;
                    UsbDeviceManager.this.mNotificationManager.cancelAsUser(null, R.string.face_sensor_privacy_enabled, UserHandle.ALL);
                    return;
                }
                return;
            }
            if ("0".equals(SystemProperties.get("persist.adb.notify")) || this.mAdbNotificationShown) {
                return;
            }
            Resources r = UsbDeviceManager.this.mContext.getResources();
            CharSequence title = r.getText(R.string.face_sensor_privacy_enabled);
            CharSequence message = r.getText(R.string.faceunlock_multiple_failures);
            Intent intent = Intent.makeRestartActivityTask(new ComponentName("com.android.settings", "com.android.settings.DevelopmentSettings"));
            PendingIntent pi = PendingIntent.getActivityAsUser(UsbDeviceManager.this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
            if (BenesseExtension.getDchaState() != 0) {
                pi = null;
            }
            Notification notification = new Notification.Builder(UsbDeviceManager.this.mContext).setSmallIcon(R.drawable.list_selector_background_longpress_light).setWhen(0L).setOngoing(true).setTicker(title).setDefaults(0).setPriority(0).setColor(UsbDeviceManager.this.mContext.getColor(R.color.system_accent3_600)).setContentTitle(title).setContentText(message).setContentIntent(pi).setVisibility(1).build();
            this.mAdbNotificationShown = true;
            UsbDeviceManager.this.mNotificationManager.notifyAsUser(null, R.string.face_sensor_privacy_enabled, notification, UserHandle.ALL);
        }

        private String getDefaultFunctions() {
            String func = SystemProperties.get(UsbDeviceManager.USB_PERSISTENT_CONFIG_PROPERTY, "none");
            if ("none".equals(func)) {
                return "mtp";
            }
            return func;
        }

        public void dump(IndentingPrintWriter pw) {
            pw.println("USB Device State:");
            pw.println("  mCurrentFunctions: " + this.mCurrentFunctions);
            pw.println("  mCurrentFunctionsApplied: " + this.mCurrentFunctionsApplied);
            pw.println("  mConnected: " + this.mConnected);
            pw.println("  mConfigured: " + this.mConfigured);
            pw.println("  mUsbDataUnlocked: " + this.mUsbDataUnlocked);
            pw.println("  mCurrentAccessory: " + this.mCurrentAccessory);
            try {
                pw.println("  Kernel state: " + FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
                pw.println("  Kernel function list: " + FileUtils.readTextFile(new File(UsbDeviceManager.FUNCTIONS_PATH), 0, null).trim());
            } catch (IOException e) {
                pw.println("IOException: " + e);
            }
        }
    }

    public UsbAccessory getCurrentAccessory() {
        return this.mHandler.getCurrentAccessory();
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory accessory) {
        UsbAccessory currentAccessory = this.mHandler.getCurrentAccessory();
        if (currentAccessory == null) {
            throw new IllegalArgumentException("no accessory attached");
        }
        if (!currentAccessory.equals(accessory)) {
            String error = accessory.toString() + " does not match current accessory " + currentAccessory;
            throw new IllegalArgumentException(error);
        }
        getCurrentSettings().checkPermission(accessory);
        return nativeOpenAccessory();
    }

    public boolean isFunctionEnabled(String function) {
        return UsbManager.containsFunction(SystemProperties.get(USB_CONFIG_PROPERTY), function);
    }

    public void setCurrentFunctions(String functions) {
        Slog.d(TAG, "setCurrentFunctions(" + functions + ")");
        this.mHandler.sendMessage(2, functions);
    }

    public int getCurrentState() {
        int state = this.mUsbConfigured ? 1 : 0;
        Slog.d(TAG, "getCurrentState - " + state);
        return state;
    }

    public void setUsbDataUnlocked(boolean unlocked) {
        Slog.d(TAG, "setUsbDataUnlocked(" + unlocked + ")");
        this.mHandler.sendMessage(6, unlocked);
    }

    private void readOemUsbOverrideConfig() {
        String[] configList = this.mContext.getResources().getStringArray(R.array.config_cdma_international_roaming_indicators);
        if (configList == null) {
            return;
        }
        for (String config : configList) {
            String[] items = config.split(":");
            if (items.length == 3) {
                if (this.mOemModeMap == null) {
                    this.mOemModeMap = new HashMap();
                }
                List<Pair<String, String>> overrideList = this.mOemModeMap.get(items[0]);
                if (overrideList == null) {
                    overrideList = new LinkedList<>();
                    this.mOemModeMap.put(items[0], overrideList);
                }
                overrideList.add(new Pair<>(items[1], items[2]));
            }
        }
    }

    private String applyOemOverrideFunction(String usbFunctions) {
        if (usbFunctions == null || this.mOemModeMap == null) {
            return usbFunctions;
        }
        String bootMode = SystemProperties.get(BOOT_MODE_PROPERTY, "unknown");
        List<Pair<String, String>> overrides = this.mOemModeMap.get(bootMode);
        if (overrides != null) {
            for (Pair<String, String> pair : overrides) {
                if (((String) pair.first).equals(usbFunctions)) {
                    Slog.d(TAG, "OEM USB override: " + ((String) pair.first) + " ==> " + ((String) pair.second));
                    return (String) pair.second;
                }
            }
        }
        return usbFunctions;
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        if (this.mDebuggingManager == null) {
            return;
        }
        this.mDebuggingManager.allowUsbDebugging(alwaysAllow, publicKey);
    }

    public void denyUsbDebugging() {
        if (this.mDebuggingManager == null) {
            return;
        }
        this.mDebuggingManager.denyUsbDebugging();
    }

    public void clearUsbDebuggingKeys() {
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.clearUsbDebuggingKeys();
            return;
        }
        throw new RuntimeException("Cannot clear Usb Debugging keys, UsbDebuggingManager not enabled");
    }

    public void dump(IndentingPrintWriter pw) {
        if (this.mHandler != null) {
            this.mHandler.dump(pw);
        }
        if (this.mDebuggingManager == null) {
            return;
        }
        this.mDebuggingManager.dump(pw);
    }
}
