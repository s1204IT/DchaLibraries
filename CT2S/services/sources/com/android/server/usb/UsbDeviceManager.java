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
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
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
import com.android.server.FgThread;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class UsbDeviceManager {
    private static final int ACCESSORY_REQUEST_TIMEOUT = 10000;
    private static final String ACCESSORY_START_MATCH = "DEVPATH=/devices/virtual/misc/usb_accessory";
    private static final int AUDIO_MODE_NONE = 0;
    private static final int AUDIO_MODE_SOURCE = 1;
    private static final String AUDIO_SOURCE_PCM_PATH = "/sys/class/android_usb/android0/f_audio_source/pcm";
    private static final String BOOT_MODE_PROPERTY = "ro.bootmode";
    private static final boolean DEBUG = false;
    private static final String FUNCTIONS_PATH = "/sys/class/android_usb/android0/functions";
    private static final String MASS_STORAGE_FILE_PATH = "/sys/class/android_usb/android0/f_mass_storage/lun/file";
    private static final int MSG_BOOT_COMPLETED = 4;
    private static final int MSG_ENABLE_ADB = 1;
    private static final int MSG_SET_CURRENT_FUNCTIONS = 2;
    private static final int MSG_SYSTEM_READY = 3;
    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_USER_SWITCHED = 5;
    private static final String RNDIS_ETH_ADDR_PATH = "/sys/class/android_usb/android0/f_rndis/ethaddr";
    private static final String STATE_PATH = "/sys/class/android_usb/android0/state";
    private static final String TAG = UsbDeviceManager.class.getSimpleName();
    private static final int UPDATE_DELAY = 1000;
    private static final String USB_STATE_MATCH = "DEVPATH=/devices/virtual/android_usb/android0";
    private String[] mAccessoryStrings;
    private boolean mAdbEnabled;
    private boolean mAudioSourceEnabled;
    private boolean mBootCompleted;
    private final ContentResolver mContentResolver;
    private final Context mContext;

    @GuardedBy("mLock")
    private UsbSettingsManager mCurrentSettings;
    private UsbDebuggingManager mDebuggingManager;
    private UsbHandler mHandler;
    private final boolean mHasUsbAccessory;
    private NotificationManager mNotificationManager;
    private Map<String, List<Pair<String, String>>> mOemModeMap;
    private boolean mUseUsbNotification;
    private long mAccessoryModeRequestTime = 0;
    private final Object mLock = new Object();
    private final UEventObserver mUEventObserver = new UEventObserver() {
        public void onUEvent(UEventObserver.UEvent event) {
            String state = event.get("USB_STATE");
            String accessory = event.get("ACCESSORY");
            if (state != null) {
                UsbDeviceManager.this.mHandler.updateState(state);
            } else if ("START".equals(accessory)) {
                UsbDeviceManager.this.startAccessoryMode();
            }
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
            boolean enable = UsbDeviceManager.DEBUG;
            if (Settings.Global.getInt(UsbDeviceManager.this.mContentResolver, "adb_enabled", 0) > 0) {
                enable = true;
            }
            UsbDeviceManager.this.mHandler.sendMessage(1, enable);
        }
    }

    public UsbDeviceManager(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        PackageManager pm = this.mContext.getPackageManager();
        this.mHasUsbAccessory = pm.hasSystemFeature("android.hardware.usb.accessory");
        initRndisAddress();
        readOemUsbOverrideConfig();
        this.mHandler = new UsbHandler(FgThread.get().getLooper());
        if (nativeIsStartRequested()) {
            startAccessoryMode();
        }
        boolean secureAdbEnabled = SystemProperties.getBoolean("ro.adb.secure", DEBUG);
        boolean dataEncrypted = "1".equals(SystemProperties.get("vold.decrypt"));
        if (secureAdbEnabled && !dataEncrypted) {
            this.mDebuggingManager = new UsbDebuggingManager(context);
        }
    }

    public void setCurrentSettings(UsbSettingsManager settings) {
        synchronized (this.mLock) {
            this.mCurrentSettings = settings;
        }
    }

    private UsbSettingsManager getCurrentSettings() {
        UsbSettingsManager usbSettingsManager;
        synchronized (this.mLock) {
            usbSettingsManager = this.mCurrentSettings;
        }
        return usbSettingsManager;
    }

    public void systemReady() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        StorageManager storageManager = StorageManager.from(this.mContext);
        StorageVolume primary = storageManager.getPrimaryVolume();
        boolean massStorageSupported = primary != null && primary.allowMassStorage();
        this.mUseUsbNotification = !massStorageSupported;
        try {
            Settings.Global.putInt(this.mContentResolver, "adb_enabled", this.mAdbEnabled ? 1 : 0);
        } catch (SecurityException e) {
            Slog.d(TAG, "ADB_ENABLED is restricted.");
        }
        this.mHandler.sendEmptyMessage(3);
    }

    private void startAccessoryMode() {
        if (this.mHasUsbAccessory) {
            this.mAccessoryStrings = nativeGetAccessoryStrings();
            boolean enableAudio = nativeGetAudioMode() == 1;
            boolean enableAccessory = (this.mAccessoryStrings == null || this.mAccessoryStrings[0] == null || this.mAccessoryStrings[1] == null) ? false : true;
            String functions = null;
            if (enableAccessory && enableAudio) {
                functions = "accessory,audio_source";
            } else if (enableAccessory) {
                functions = "accessory";
            } else if (enableAudio) {
                functions = "audio_source";
            }
            if (functions != null) {
                this.mAccessoryModeRequestTime = SystemClock.elapsedRealtime();
                setCurrentFunctions(functions, DEBUG);
            }
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

    private static String addFunction(String functions, String function) {
        if (!"none".equals(functions)) {
            if (!containsFunction(functions, function)) {
                if (functions.length() > 0) {
                    functions = functions + ",";
                }
                functions = functions + function;
            }
            return functions;
        }
        return function;
    }

    private static String removeFunction(String functions, String function) {
        String[] split = functions.split(",");
        for (int i = 0; i < split.length; i++) {
            if (function.equals(split[i])) {
                split[i] = null;
            }
        }
        if (split.length == 1 && split[0] == null) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (String s : split) {
            if (s != null) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(s);
            }
        }
        return builder.toString();
    }

    private static boolean containsFunction(String functions, String function) {
        return Arrays.asList(functions.split(",")).contains(function);
    }

    private final class UsbHandler extends Handler {
        private boolean mAdbNotificationShown;
        private final BroadcastReceiver mBootCompletedReceiver;
        private boolean mConfigured;
        private boolean mConnected;
        private UsbAccessory mCurrentAccessory;
        private String mCurrentFunctions;
        private int mCurrentUser;
        private String mDefaultFunctions;
        private boolean mInUsbSetting;
        private int mUsbNotificationId;
        private final BroadcastReceiver mUserSwitchedReceiver;

        public UsbHandler(Looper looper) {
            super(looper);
            this.mCurrentUser = -10000;
            this.mInUsbSetting = UsbDeviceManager.DEBUG;
            this.mBootCompletedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    UsbDeviceManager.this.mHandler.sendEmptyMessage(4);
                }
            };
            this.mUserSwitchedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    UsbDeviceManager.this.mHandler.obtainMessage(5, userId, 0).sendToTarget();
                }
            };
            try {
                this.mDefaultFunctions = SystemProperties.get("persist.sys.usb.config", "adb");
                this.mDefaultFunctions = UsbDeviceManager.this.processOemUsbOverride(this.mDefaultFunctions);
                String config = SystemProperties.get("sys.usb.config", "none");
                if (!config.equals(this.mDefaultFunctions)) {
                    Slog.w(UsbDeviceManager.TAG, "resetting config to persistent property: " + this.mDefaultFunctions);
                    SystemProperties.set("sys.usb.config", this.mDefaultFunctions);
                }
                this.mCurrentFunctions = getDefaultFunctions();
                String state = FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim();
                updateState(state);
                UsbDeviceManager.this.mAdbEnabled = UsbDeviceManager.containsFunction(this.mCurrentFunctions, "adb");
                String value = SystemProperties.get("persist.service.adb.enable", "");
                if (value.length() > 0) {
                    char enable = value.charAt(0);
                    if (enable == '1') {
                        setAdbEnabled(true);
                    } else if (enable == '0') {
                        setAdbEnabled(UsbDeviceManager.DEBUG);
                    }
                    SystemProperties.set("persist.service.adb.enable", "");
                }
                UsbDeviceManager.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("adb_enabled"), UsbDeviceManager.DEBUG, UsbDeviceManager.this.new AdbSettingsObserver());
                UsbDeviceManager.this.mUEventObserver.startObserving(UsbDeviceManager.USB_STATE_MATCH);
                UsbDeviceManager.this.mUEventObserver.startObserving(UsbDeviceManager.ACCESSORY_START_MATCH);
                IntentFilter filter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
                filter.setPriority(1000);
                UsbDeviceManager.this.mContext.registerReceiver(this.mBootCompletedReceiver, filter);
                UsbDeviceManager.this.mContext.registerReceiver(this.mUserSwitchedReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
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

        public void sendMessage(int what, Object arg0, boolean arg1) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.obj = arg0;
            m.arg1 = arg1 ? 1 : 0;
            sendMessage(m);
        }

        public void updateState(String state) {
            int connected;
            int configured;
            if ("DISCONNECTED".equals(state)) {
                connected = 0;
                configured = 0;
            } else if ("CONNECTED".equals(state)) {
                connected = 1;
                configured = 0;
            } else if (!"CONFIGURED".equals(state)) {
                Slog.e(UsbDeviceManager.TAG, "unknown state " + state);
                return;
            } else {
                connected = 1;
                configured = 1;
            }
            removeMessages(0);
            Message msg = Message.obtain(this, 0);
            msg.arg1 = connected;
            msg.arg2 = configured;
            int delay = 0;
            if (connected == 0) {
                if (this.mInUsbSetting) {
                    delay = 6000;
                } else {
                    delay = 1000;
                }
            }
            sendMessageDelayed(msg, delay);
        }

        private boolean waitForState(String state) {
            for (int i = 0; i < 20; i++) {
                if (state.equals(SystemProperties.get("sys.usb.state"))) {
                    return true;
                }
                SystemClock.sleep(50L);
            }
            Slog.e(UsbDeviceManager.TAG, "waitForState(" + state + ") FAILED");
            return UsbDeviceManager.DEBUG;
        }

        private boolean setUsbConfig(String config) {
            SystemProperties.set("sys.usb.config", config);
            return waitForState(config);
        }

        private void setAdbEnabled(boolean enable) {
            if (enable != UsbDeviceManager.this.mAdbEnabled) {
                UsbDeviceManager.this.mAdbEnabled = enable;
                setEnabledFunctions(this.mDefaultFunctions, true);
                setEnabledFunctions(getDefaultFunctions(), UsbDeviceManager.DEBUG);
                updateAdbNotification();
            }
            if (UsbDeviceManager.this.mDebuggingManager != null) {
                UsbDeviceManager.this.mDebuggingManager.setAdbEnabled(UsbDeviceManager.this.mAdbEnabled);
            }
        }

        private void setEnabledFunctions(String functions, boolean makeDefault) {
            if (functions != null && makeDefault && !UsbDeviceManager.this.needsOemUsbOverride()) {
                String functions2 = UsbDeviceManager.this.mAdbEnabled ? UsbDeviceManager.addFunction(functions, "adb") : UsbDeviceManager.removeFunction(functions, "adb");
                if (!this.mDefaultFunctions.equals(functions2)) {
                    this.mInUsbSetting = true;
                    if (!setUsbConfig("none")) {
                        Slog.e(UsbDeviceManager.TAG, "Failed to disable USB");
                        setUsbConfig(this.mCurrentFunctions);
                        this.mInUsbSetting = UsbDeviceManager.DEBUG;
                        return;
                    }
                    SystemProperties.set("persist.sys.usb.config", functions2);
                    if (!waitForState(functions2)) {
                        Slog.e(UsbDeviceManager.TAG, "Failed to switch persistent USB config to " + functions2);
                        SystemProperties.set("persist.sys.usb.config", this.mDefaultFunctions);
                    } else {
                        this.mCurrentFunctions = functions2;
                        this.mDefaultFunctions = functions2;
                    }
                    this.mInUsbSetting = UsbDeviceManager.DEBUG;
                    return;
                }
                return;
            }
            boolean rndisTetherSetting = "rndis".equals(functions);
            if (functions == null) {
                functions = this.mDefaultFunctions;
            }
            String functions3 = UsbDeviceManager.this.processOemUsbOverride(functions);
            String functions4 = UsbDeviceManager.this.mAdbEnabled ? UsbDeviceManager.addFunction(functions3, "adb") : UsbDeviceManager.removeFunction(functions3, "adb");
            if (!this.mCurrentFunctions.equals(functions4) || rndisTetherSetting) {
                if (rndisTetherSetting && UsbDeviceManager.containsFunction(this.mCurrentFunctions, "rndis")) {
                    functions4 = this.mCurrentFunctions;
                }
                this.mInUsbSetting = true;
                if (!setUsbConfig("none")) {
                    Slog.e(UsbDeviceManager.TAG, "Failed to disable USB");
                    setUsbConfig(this.mCurrentFunctions);
                    this.mInUsbSetting = UsbDeviceManager.DEBUG;
                } else {
                    if (!setUsbConfig(functions4)) {
                        Slog.e(UsbDeviceManager.TAG, "Failed to switch USB config to " + functions4);
                        setUsbConfig(this.mCurrentFunctions);
                    } else {
                        this.mCurrentFunctions = functions4;
                    }
                    this.mInUsbSetting = UsbDeviceManager.DEBUG;
                }
            }
        }

        private void updateCurrentAccessory() {
            boolean enteringAccessoryMode = UsbDeviceManager.this.mAccessoryModeRequestTime > 0 && SystemClock.elapsedRealtime() < UsbDeviceManager.this.mAccessoryModeRequestTime + 10000;
            if (this.mConfigured && enteringAccessoryMode) {
                if (UsbDeviceManager.this.mAccessoryStrings == null) {
                    Slog.e(UsbDeviceManager.TAG, "nativeGetAccessoryStrings failed");
                    return;
                }
                this.mCurrentAccessory = new UsbAccessory(UsbDeviceManager.this.mAccessoryStrings);
                Slog.d(UsbDeviceManager.TAG, "entering USB accessory mode: " + this.mCurrentAccessory);
                if (UsbDeviceManager.this.mBootCompleted) {
                    UsbDeviceManager.this.getCurrentSettings().accessoryAttached(this.mCurrentAccessory);
                    return;
                }
                return;
            }
            if (!enteringAccessoryMode) {
                Slog.d(UsbDeviceManager.TAG, "exited USB accessory mode");
                setEnabledFunctions(getDefaultFunctions(), UsbDeviceManager.DEBUG);
                if (this.mCurrentAccessory != null) {
                    if (UsbDeviceManager.this.mBootCompleted) {
                        UsbDeviceManager.this.getCurrentSettings().accessoryDetached(this.mCurrentAccessory);
                    }
                    this.mCurrentAccessory = null;
                    UsbDeviceManager.this.mAccessoryStrings = null;
                }
            }
        }

        private void updateUsbState() {
            Intent intent = new Intent("android.hardware.usb.action.USB_STATE");
            intent.addFlags(536870912);
            intent.putExtra("connected", this.mConnected);
            intent.putExtra("configured", this.mConfigured);
            if (this.mCurrentFunctions != null) {
                String[] functions = this.mCurrentFunctions.split(",");
                for (String str : functions) {
                    intent.putExtra(str, true);
                }
            }
            UsbDeviceManager.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void updateAudioSourceFunction() throws Throwable {
            Scanner scanner;
            boolean enabled = UsbDeviceManager.containsFunction(this.mCurrentFunctions, "audio_source");
            if (enabled != UsbDeviceManager.this.mAudioSourceEnabled) {
                Intent intent = new Intent("android.media.action.USB_AUDIO_ACCESSORY_PLUG");
                intent.addFlags(536870912);
                intent.addFlags(1073741824);
                intent.putExtra("state", enabled ? 1 : 0);
                if (enabled) {
                    Scanner scanner2 = null;
                    try {
                        try {
                            scanner = new Scanner(new File(UsbDeviceManager.AUDIO_SOURCE_PCM_PATH));
                        } catch (Throwable th) {
                            th = th;
                        }
                    } catch (FileNotFoundException e) {
                        e = e;
                    }
                    try {
                        int card = scanner.nextInt();
                        int device = scanner.nextInt();
                        intent.putExtra("card", card);
                        intent.putExtra("device", device);
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
                UsbDeviceManager.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                UsbDeviceManager.this.mAudioSourceEnabled = enabled;
            }
        }

        @Override
        public void handleMessage(Message msg) throws Throwable {
            switch (msg.what) {
                case 0:
                    this.mConnected = msg.arg1 == 1;
                    this.mConfigured = msg.arg2 == 1;
                    updateUsbNotification();
                    updateAdbNotification();
                    if (UsbDeviceManager.containsFunction(this.mCurrentFunctions, "accessory")) {
                        updateCurrentAccessory();
                    } else if (!this.mConnected) {
                        setEnabledFunctions(getDefaultFunctions(), UsbDeviceManager.DEBUG);
                    }
                    if (UsbDeviceManager.this.mBootCompleted) {
                        updateUsbState();
                        updateAudioSourceFunction();
                    }
                    break;
                case 1:
                    setAdbEnabled(msg.arg1 == 1);
                    break;
                case 2:
                    String functions = (String) msg.obj;
                    boolean makeDefault = msg.arg1 == 1;
                    setEnabledFunctions(functions, makeDefault);
                    break;
                case 3:
                    updateUsbNotification();
                    updateAdbNotification();
                    updateUsbState();
                    updateAudioSourceFunction();
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
                    UserManager userManager = (UserManager) UsbDeviceManager.this.mContext.getSystemService("user");
                    UserHandle userHandle = new UserHandle(msg.arg1);
                    if (userManager.hasUserRestriction("no_usb_file_transfer", userHandle)) {
                        Slog.v(UsbDeviceManager.TAG, "Switched to user " + msg.arg1 + " with DISALLOW_USB_FILE_TRANSFER restriction; disabling USB.");
                        setUsbConfig("none");
                        this.mCurrentUser = msg.arg1;
                    } else {
                        boolean mtpActive = UsbDeviceManager.containsFunction(this.mCurrentFunctions, "mtp") || UsbDeviceManager.containsFunction(this.mCurrentFunctions, "ptp");
                        if (mtpActive && this.mCurrentUser != -10000) {
                            Slog.v(UsbDeviceManager.TAG, "Current user switched; resetting USB host stack for MTP");
                            setUsbConfig("none");
                            setUsbConfig(this.mCurrentFunctions);
                        }
                        this.mCurrentUser = msg.arg1;
                    }
                    break;
            }
        }

        public UsbAccessory getCurrentAccessory() {
            return this.mCurrentAccessory;
        }

        private void updateUsbNotification() {
            if (UsbDeviceManager.this.mNotificationManager != null && UsbDeviceManager.this.mUseUsbNotification) {
                int id = 0;
                Resources r = UsbDeviceManager.this.mContext.getResources();
                if (this.mConnected) {
                    if (!UsbDeviceManager.containsFunction(this.mCurrentFunctions, "mtp")) {
                        if (!UsbDeviceManager.containsFunction(this.mCurrentFunctions, "ptp")) {
                            if (!UsbDeviceManager.containsFunction(this.mCurrentFunctions, "mass_storage")) {
                                if (!UsbDeviceManager.containsFunction(this.mCurrentFunctions, "accessory")) {
                                    if (!UsbDeviceManager.containsFunction(this.mCurrentFunctions, "charge_only")) {
                                        if (!UsbDeviceManager.containsFunction(this.mCurrentFunctions, "pcsuite")) {
                                            if (!UsbDeviceManager.containsFunction(this.mCurrentFunctions, "acm")) {
                                                if (!UsbDeviceManager.containsFunction(this.mCurrentFunctions, "rndis")) {
                                                    Slog.e(UsbDeviceManager.TAG, "No known USB function in updateUsbNotification");
                                                    id = R.string.indeterminate_progress_46;
                                                }
                                            } else {
                                                id = R.string.indeterminate_progress_45;
                                            }
                                        } else {
                                            id = R.string.indeterminate_progress_44;
                                        }
                                    } else {
                                        id = R.string.indeterminate_progress_43;
                                    }
                                } else {
                                    id = R.string.indeterminate_progress_47;
                                }
                            } else {
                                id = R.string.indeterminate_progress_42;
                            }
                        } else {
                            id = R.string.indeterminate_progress_41;
                        }
                    } else {
                        id = R.string.indeterminate_progress_40;
                    }
                }
                if (id != this.mUsbNotificationId) {
                    if (this.mUsbNotificationId != 0) {
                        UsbDeviceManager.this.mNotificationManager.cancelAsUser(null, this.mUsbNotificationId, UserHandle.ALL);
                        this.mUsbNotificationId = 0;
                    }
                    if (id != 0) {
                        CharSequence message = r.getText(R.string.indeterminate_progress_48);
                        CharSequence title = r.getText(id);
                        Notification notification = new Notification();
                        notification.icon = R.drawable.jog_tab_bar_right_end_confirm_yellow;
                        notification.when = 0L;
                        notification.flags = 2;
                        notification.tickerText = title;
                        notification.defaults = 1;
                        notification.sound = null;
                        notification.vibrate = null;
                        notification.priority = -1;
                        Intent intent = Intent.makeRestartActivityTask(new ComponentName("com.android.settings", "com.android.settings.UsbSettings"));
                        PendingIntent pi = PendingIntent.getActivityAsUser(UsbDeviceManager.this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
                        notification.color = UsbDeviceManager.this.mContext.getResources().getColor(R.color.system_accent3_600);
                        notification.setLatestEventInfo(UsbDeviceManager.this.mContext, title, message, pi);
                        notification.visibility = 1;
                        UsbDeviceManager.this.mNotificationManager.notifyAsUser(null, id, notification, UserHandle.CURRENT);
                        this.mUsbNotificationId = id;
                    }
                }
            }
        }

        private void updateAdbNotification() {
            if (UsbDeviceManager.this.mNotificationManager != null) {
                if (UsbDeviceManager.this.mAdbEnabled && this.mConnected) {
                    if (!"0".equals(SystemProperties.get("persist.adb.notify")) && !this.mAdbNotificationShown) {
                        Resources r = UsbDeviceManager.this.mContext.getResources();
                        CharSequence title = r.getText(R.string.indeterminate_progress_52);
                        CharSequence message = r.getText(R.string.indeterminate_progress_53);
                        Notification notification = new Notification();
                        notification.icon = R.drawable.item_background_borderless_material_dark;
                        notification.when = 0L;
                        notification.flags = 2;
                        notification.tickerText = title;
                        notification.defaults = 0;
                        notification.sound = null;
                        notification.vibrate = null;
                        notification.priority = -1;
                        Intent intent = Intent.makeRestartActivityTask(new ComponentName("com.android.settings", "com.android.settings.DevelopmentSettings"));
                        PendingIntent pi = PendingIntent.getActivityAsUser(UsbDeviceManager.this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
                        notification.color = UsbDeviceManager.this.mContext.getResources().getColor(R.color.system_accent3_600);
                        notification.setLatestEventInfo(UsbDeviceManager.this.mContext, title, message, pi);
                        notification.visibility = 1;
                        this.mAdbNotificationShown = true;
                        UsbDeviceManager.this.mNotificationManager.notifyAsUser(null, R.string.indeterminate_progress_52, notification, UserHandle.ALL);
                        return;
                    }
                    return;
                }
                if (this.mAdbNotificationShown) {
                    this.mAdbNotificationShown = UsbDeviceManager.DEBUG;
                    UsbDeviceManager.this.mNotificationManager.cancelAsUser(null, R.string.indeterminate_progress_52, UserHandle.ALL);
                }
            }
        }

        private String getDefaultFunctions() {
            UserManager userManager = (UserManager) UsbDeviceManager.this.mContext.getSystemService("user");
            return userManager.hasUserRestriction("no_usb_file_transfer", new UserHandle(this.mCurrentUser)) ? "none" : this.mDefaultFunctions;
        }

        public void dump(FileDescriptor fd, PrintWriter pw) {
            pw.println("  USB Device State:");
            pw.println("    Current Functions: " + this.mCurrentFunctions);
            pw.println("    Default Functions: " + this.mDefaultFunctions);
            pw.println("    mConnected: " + this.mConnected);
            pw.println("    mConfigured: " + this.mConfigured);
            pw.println("    mCurrentAccessory: " + this.mCurrentAccessory);
            try {
                pw.println("    Kernel state: " + FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
                pw.println("    Kernel function list: " + FileUtils.readTextFile(new File(UsbDeviceManager.FUNCTIONS_PATH), 0, null).trim());
                pw.println("    Mass storage backing file: " + FileUtils.readTextFile(new File(UsbDeviceManager.MASS_STORAGE_FILE_PATH), 0, null).trim());
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

    public void setCurrentFunctions(String functions, boolean makeDefault) {
        this.mHandler.sendMessage(2, functions, makeDefault);
    }

    public void setMassStorageBackingFile(String path) {
        if (path == null) {
            path = "";
        }
        try {
            FileUtils.stringToFile(MASS_STORAGE_FILE_PATH, path);
        } catch (IOException e) {
            Slog.e(TAG, "failed to write to /sys/class/android_usb/android0/f_mass_storage/lun/file");
        }
    }

    private void readOemUsbOverrideConfig() {
        String[] configList = this.mContext.getResources().getStringArray(R.array.config_callBarringMMI_for_ims);
        if (configList != null) {
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
    }

    private boolean needsOemUsbOverride() {
        if (this.mOemModeMap == null) {
            return DEBUG;
        }
        String bootMode = SystemProperties.get(BOOT_MODE_PROPERTY, "unknown");
        if (this.mOemModeMap.get(bootMode) != null) {
            return true;
        }
        return DEBUG;
    }

    private String processOemUsbOverride(String usbFunctions) {
        if (usbFunctions != null && this.mOemModeMap != null) {
            String bootMode = SystemProperties.get(BOOT_MODE_PROPERTY, "unknown");
            List<Pair<String, String>> overrides = this.mOemModeMap.get(bootMode);
            if (overrides != null) {
                for (Pair<String, String> pair : overrides) {
                    if (((String) pair.first).equals(usbFunctions)) {
                        Slog.d(TAG, "OEM USB override: " + ((String) pair.first) + " ==> " + ((String) pair.second));
                        return (String) pair.second;
                    }
                }
                return usbFunctions;
            }
            return usbFunctions;
        }
        return usbFunctions;
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.allowUsbDebugging(alwaysAllow, publicKey);
        }
    }

    public void denyUsbDebugging() {
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.denyUsbDebugging();
        }
    }

    public void clearUsbDebuggingKeys() {
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.clearUsbDebuggingKeys();
            return;
        }
        throw new RuntimeException("Cannot clear Usb Debugging keys, UsbDebuggingManager not enabled");
    }

    public void dump(FileDescriptor fd, PrintWriter pw) {
        if (this.mHandler != null) {
            this.mHandler.dump(fd, pw);
        }
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.dump(fd, pw);
        }
    }
}
