package com.android.server.usb;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.hardware.usb.gadget.V1_0.GadgetFunction;
import android.hardware.usb.gadget.V1_0.IUsbGadget;
import android.hardware.usb.gadget.V1_0.IUsbGadgetCallback;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.BenesseExtension;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
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
import com.android.internal.logging.MetricsLogger;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.SomeArgs;
import com.android.internal.usb.DumpUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

public class UsbDeviceManager implements ActivityManagerInternal.ScreenObserver {
    private static final int ACCESSORY_REQUEST_TIMEOUT = 10000;
    private static final String ACCESSORY_START_MATCH = "DEVPATH=/devices/virtual/misc/usb_accessory";
    private static final String ADB_NOTIFICATION_CHANNEL_ID_TV = "usbdevicemanager.adb.tv";
    private static final int AUDIO_MODE_SOURCE = 1;
    private static final String AUDIO_SOURCE_PCM_PATH = "/sys/class/android_usb/android0/f_audio_source/pcm";
    private static final String BOOT_MODE_PROPERTY = "ro.bootmode";
    private static final boolean DEBUG = false;
    private static final String FUNCTIONS_PATH = "/sys/class/android_usb/android0/functions";
    private static final String MIDI_ALSA_PATH = "/sys/class/android_usb/android0/f_midi/alsa";
    private static final int MSG_ACCESSORY_MODE_ENTER_TIMEOUT = 8;
    private static final int MSG_BOOT_COMPLETED = 4;
    private static final int MSG_ENABLE_ADB = 1;
    private static final int MSG_FUNCTION_SWITCH_TIMEOUT = 17;
    private static final int MSG_GET_CURRENT_USB_FUNCTIONS = 16;
    private static final int MSG_LOCALE_CHANGED = 11;
    private static final int MSG_SET_CHARGING_FUNCTIONS = 14;
    private static final int MSG_SET_CURRENT_FUNCTIONS = 2;
    private static final int MSG_SET_FUNCTIONS_TIMEOUT = 15;
    private static final int MSG_SET_SCREEN_UNLOCKED_FUNCTIONS = 12;
    private static final int MSG_SYSTEM_READY = 3;
    private static final int MSG_UPDATE_CHARGING_STATE = 9;
    private static final int MSG_UPDATE_HOST_STATE = 10;
    private static final int MSG_UPDATE_PORT_STATE = 7;
    private static final int MSG_UPDATE_SCREEN_LOCK = 13;
    private static final int MSG_UPDATE_STATE = 0;
    private static final int MSG_UPDATE_USER_RESTRICTIONS = 6;
    private static final int MSG_USER_SWITCHED = 5;
    private static final String NORMAL_BOOT = "normal";
    private static final String RNDIS_ETH_ADDR_PATH = "/sys/class/android_usb/android0/f_rndis/ethaddr";
    private static final String STATE_PATH = "/sys/class/android_usb/android0/state";
    static final String UNLOCKED_CONFIG_PREF = "usb-screen-unlocked-config-%d";
    private static final int UPDATE_DELAY = 1000;
    private static final String USB_PREFS_XML = "UsbDeviceManagerPrefs.xml";
    private static final String USB_STATE_MATCH = "DEVPATH=/devices/virtual/android_usb/android0";

    @GuardedBy("mLock")
    private String[] mAccessoryStrings;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private HashMap<Long, FileDescriptor> mControlFds;

    @GuardedBy("mLock")
    private UsbProfileGroupSettingsManager mCurrentSettings;
    private UsbDebuggingManager mDebuggingManager;
    private UsbHandler mHandler;
    private final boolean mHasUsbAccessory;
    private final Object mLock = new Object();
    private final UEventObserver mUEventObserver;
    private static final String TAG = UsbDeviceManager.class.getSimpleName();
    private static Set<Integer> sBlackListedInterfaces = new HashSet();

    private native String[] nativeGetAccessoryStrings();

    private native int nativeGetAudioMode();

    private native boolean nativeIsStartRequested();

    private native ParcelFileDescriptor nativeOpenAccessory();

    private native FileDescriptor nativeOpenControl(String str);

    static {
        sBlackListedInterfaces.add(1);
        sBlackListedInterfaces.add(2);
        sBlackListedInterfaces.add(3);
        sBlackListedInterfaces.add(7);
        sBlackListedInterfaces.add(8);
        sBlackListedInterfaces.add(9);
        sBlackListedInterfaces.add(10);
        sBlackListedInterfaces.add(11);
        sBlackListedInterfaces.add(13);
        sBlackListedInterfaces.add(14);
        sBlackListedInterfaces.add(Integer.valueOf(UsbDescriptor.CLASSID_WIRELESS));
    }

    private class AdbSettingsObserver extends ContentObserver {
        public AdbSettingsObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean z) {
            UsbDeviceManager.this.mHandler.sendMessage(1, Settings.Global.getInt(UsbDeviceManager.this.mContentResolver, "adb_enabled", 0) > 0);
        }
    }

    private final class UsbUEventObserver extends UEventObserver {
        private UsbUEventObserver() {
        }

        public void onUEvent(UEventObserver.UEvent uEvent) {
            String str = uEvent.get("USB_STATE");
            String str2 = uEvent.get("ACCESSORY");
            if (str != null) {
                UsbDeviceManager.this.mHandler.updateState(str);
            } else if ("START".equals(str2)) {
                UsbDeviceManager.this.startAccessoryMode();
            }
        }
    }

    public void onKeyguardStateChanged(boolean z) {
        this.mHandler.sendMessage(13, z && ((KeyguardManager) this.mContext.getSystemService(KeyguardManager.class)).isDeviceSecure(ActivityManager.getCurrentUser()));
    }

    public void onAwakeStateChanged(boolean z) {
    }

    public void onUnlockUser(int i) {
        onKeyguardStateChanged(false);
    }

    public UsbDeviceManager(Context context, UsbAlsaManager usbAlsaManager, UsbSettingsManager usbSettingsManager) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mHasUsbAccessory = this.mContext.getPackageManager().hasSystemFeature("android.hardware.usb.accessory");
        initRndisAddress();
        boolean z = true;
        try {
            IUsbGadget.getService(true);
        } catch (RemoteException e) {
            Slog.e(TAG, "USB GADGET HAL present but exception thrown", e);
        } catch (NoSuchElementException e2) {
            Slog.i(TAG, "USB GADGET HAL not present in the device", e2);
        }
        z = false;
        this.mControlFds = new HashMap<>();
        FileDescriptor fileDescriptorNativeOpenControl = nativeOpenControl("mtp");
        if (fileDescriptorNativeOpenControl == null) {
            Slog.e(TAG, "Failed to open control for mtp");
        }
        this.mControlFds.put(4L, fileDescriptorNativeOpenControl);
        FileDescriptor fileDescriptorNativeOpenControl2 = nativeOpenControl("ptp");
        if (fileDescriptorNativeOpenControl == null) {
            Slog.e(TAG, "Failed to open control for mtp");
        }
        this.mControlFds.put(16L, fileDescriptorNativeOpenControl2);
        boolean z2 = SystemProperties.getBoolean("ro.adb.secure", false);
        boolean zEquals = "1".equals(SystemProperties.get("vold.decrypt"));
        if (z2 && !zEquals) {
            this.mDebuggingManager = new UsbDebuggingManager(context);
        }
        if (z) {
            this.mHandler = new UsbHandlerLegacy(FgThread.get().getLooper(), this.mContext, this, this.mDebuggingManager, usbAlsaManager, usbSettingsManager);
        } else {
            this.mHandler = new UsbHandlerHal(FgThread.get().getLooper(), this.mContext, this, this.mDebuggingManager, usbAlsaManager, usbSettingsManager);
        }
        if (nativeIsStartRequested()) {
            startAccessoryMode();
        }
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                UsbDeviceManager.this.mHandler.updateHostState(intent.getParcelableExtra("port"), intent.getParcelableExtra("portStatus"));
            }
        };
        BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                UsbDeviceManager.this.mHandler.sendMessage(9, intent.getIntExtra("plugged", -1) == 2);
            }
        };
        BroadcastReceiver broadcastReceiver3 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Iterator<Map.Entry<String, UsbDevice>> it = ((UsbManager) context2.getSystemService("usb")).getDeviceList().entrySet().iterator();
                if (intent.getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                    UsbDeviceManager.this.mHandler.sendMessage(10, (Object) it, true);
                } else {
                    UsbDeviceManager.this.mHandler.sendMessage(10, (Object) it, false);
                }
            }
        };
        BroadcastReceiver broadcastReceiver4 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                UsbDeviceManager.this.mHandler.sendEmptyMessage(11);
            }
        };
        this.mContext.registerReceiver(broadcastReceiver, new IntentFilter("android.hardware.usb.action.USB_PORT_CHANGED"));
        this.mContext.registerReceiver(broadcastReceiver2, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        IntentFilter intentFilter = new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        this.mContext.registerReceiver(broadcastReceiver3, intentFilter);
        this.mContext.registerReceiver(broadcastReceiver4, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        this.mUEventObserver = new UsbUEventObserver();
        this.mUEventObserver.startObserving(USB_STATE_MATCH);
        this.mUEventObserver.startObserving(ACCESSORY_START_MATCH);
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("adb_enabled"), false, new AdbSettingsObserver());
    }

    UsbProfileGroupSettingsManager getCurrentSettings() {
        UsbProfileGroupSettingsManager usbProfileGroupSettingsManager;
        synchronized (this.mLock) {
            usbProfileGroupSettingsManager = this.mCurrentSettings;
        }
        return usbProfileGroupSettingsManager;
    }

    String[] getAccessoryStrings() {
        String[] strArr;
        synchronized (this.mLock) {
            strArr = this.mAccessoryStrings;
        }
        return strArr;
    }

    public void systemReady() {
        ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).registerScreenObserver(this);
        this.mHandler.sendEmptyMessage(3);
    }

    public void bootCompleted() {
        this.mHandler.sendEmptyMessage(4);
    }

    public void setCurrentUser(int i, UsbProfileGroupSettingsManager usbProfileGroupSettingsManager) {
        synchronized (this.mLock) {
            this.mCurrentSettings = usbProfileGroupSettingsManager;
            this.mHandler.obtainMessage(5, i, 0).sendToTarget();
        }
    }

    public void updateUserRestrictions() {
        this.mHandler.sendEmptyMessage(6);
    }

    private void startAccessoryMode() {
        long j;
        if (this.mHasUsbAccessory) {
            this.mAccessoryStrings = nativeGetAccessoryStrings();
            boolean z = false;
            boolean z2 = nativeGetAudioMode() == 1;
            if (this.mAccessoryStrings != null && this.mAccessoryStrings[0] != null && this.mAccessoryStrings[1] != null) {
                z = true;
            }
            if (z) {
                j = 2;
            } else {
                j = 0;
            }
            if (z2) {
                j |= 64;
            }
            if (j != 0) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(8), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                setCurrentFunctions(j);
            }
        }
    }

    private static void initRndisAddress() {
        int[] iArr = new int[6];
        iArr[0] = 2;
        String str = SystemProperties.get("ro.serialno", "1234567890ABCDEF");
        int length = str.length();
        for (int i = 0; i < length; i++) {
            int i2 = (i % 5) + 1;
            iArr[i2] = iArr[i2] ^ str.charAt(i);
        }
        try {
            FileUtils.stringToFile(RNDIS_ETH_ADDR_PATH, String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X", Integer.valueOf(iArr[0]), Integer.valueOf(iArr[1]), Integer.valueOf(iArr[2]), Integer.valueOf(iArr[3]), Integer.valueOf(iArr[4]), Integer.valueOf(iArr[5])));
        } catch (IOException e) {
            Slog.e(TAG, "failed to write to /sys/class/android_usb/android0/f_rndis/ethaddr");
        }
    }

    static abstract class UsbHandler extends Handler {
        protected static final String USB_PERSISTENT_CONFIG_PROPERTY = "persist.sys.usb.config";
        protected boolean mAdbEnabled;
        private boolean mAdbNotificationShown;
        private boolean mAudioAccessoryConnected;
        private boolean mAudioAccessorySupported;
        private boolean mAudioSourceEnabled;
        protected boolean mBootCompleted;
        private Intent mBroadcastedIntent;
        private boolean mConfigured;
        private boolean mConnected;
        protected final ContentResolver mContentResolver;
        private final Context mContext;
        private UsbAccessory mCurrentAccessory;
        protected long mCurrentFunctions;
        protected boolean mCurrentFunctionsApplied;
        protected boolean mCurrentUsbFunctionsReceived;
        protected int mCurrentUser;
        private final UsbDebuggingManager mDebuggingManager;
        private boolean mHideUsbNotification;
        private boolean mHostConnected;
        private int mMidiCard;
        private int mMidiDevice;
        private boolean mMidiEnabled;
        private NotificationManager mNotificationManager;
        private boolean mPendingBootBroadcast;
        private boolean mScreenLocked;
        protected long mScreenUnlockedFunctions;
        protected SharedPreferences mSettings;
        private final UsbSettingsManager mSettingsManager;
        private boolean mSinkPower;
        private boolean mSourcePower;
        private boolean mSupportsAllCombinations;
        private boolean mSystemReady;
        private final UsbAlsaManager mUsbAlsaManager;
        private boolean mUsbCharging;
        protected final UsbDeviceManager mUsbDeviceManager;
        private int mUsbNotificationId;
        protected boolean mUseUsbNotification;

        protected abstract void setEnabledFunctions(long j, boolean z);

        UsbHandler(Looper looper, Context context, UsbDeviceManager usbDeviceManager, UsbDebuggingManager usbDebuggingManager, UsbAlsaManager usbAlsaManager, UsbSettingsManager usbSettingsManager) {
            super(looper);
            this.mContext = context;
            this.mDebuggingManager = usbDebuggingManager;
            this.mUsbDeviceManager = usbDeviceManager;
            this.mUsbAlsaManager = usbAlsaManager;
            this.mSettingsManager = usbSettingsManager;
            this.mContentResolver = context.getContentResolver();
            this.mCurrentUser = ActivityManager.getCurrentUser();
            this.mScreenLocked = true;
            this.mAdbEnabled = UsbHandlerLegacy.containsFunction(getSystemProperty(USB_PERSISTENT_CONFIG_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS), "adb");
            this.mSettings = getPinnedSharedPrefs(this.mContext);
            if (this.mSettings == null) {
                Slog.e(UsbDeviceManager.TAG, "Couldn't load shared preferences");
            } else {
                this.mScreenUnlockedFunctions = UsbManager.usbFunctionsFromString(this.mSettings.getString(String.format(Locale.ENGLISH, UsbDeviceManager.UNLOCKED_CONFIG_PREF, Integer.valueOf(this.mCurrentUser)), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
            }
            StorageVolume primaryVolume = StorageManager.from(this.mContext).getPrimaryVolume();
            this.mUseUsbNotification = !(primaryVolume != null && primaryVolume.allowMassStorage()) && this.mContext.getResources().getBoolean(R.^attr-private.pointerIconText);
        }

        public void sendMessage(int i, boolean z) {
            removeMessages(i);
            Message messageObtain = Message.obtain(this, i);
            messageObtain.arg1 = z ? 1 : 0;
            sendMessage(messageObtain);
        }

        public void sendMessage(int i, Object obj) {
            removeMessages(i);
            Message messageObtain = Message.obtain(this, i);
            messageObtain.obj = obj;
            sendMessage(messageObtain);
        }

        public void sendMessage(int i, Object obj, boolean z) {
            removeMessages(i);
            Message messageObtain = Message.obtain(this, i);
            messageObtain.obj = obj;
            messageObtain.arg1 = z ? 1 : 0;
            sendMessage(messageObtain);
        }

        public void sendMessage(int i, boolean z, boolean z2) {
            removeMessages(i);
            Message messageObtain = Message.obtain(this, i);
            messageObtain.arg1 = z ? 1 : 0;
            messageObtain.arg2 = z2 ? 1 : 0;
            sendMessage(messageObtain);
        }

        public void sendMessageDelayed(int i, boolean z, long j) {
            removeMessages(i);
            Message messageObtain = Message.obtain(this, i);
            messageObtain.arg1 = z ? 1 : 0;
            sendMessageDelayed(messageObtain, j);
        }

        public void updateState(String str) {
            int i;
            int i2;
            if (!"DISCONNECTED".equals(str)) {
                if (!"CONNECTED".equals(str)) {
                    if (!"CONFIGURED".equals(str)) {
                        Slog.e(UsbDeviceManager.TAG, "unknown state " + str);
                        return;
                    }
                    i = 1;
                } else {
                    i = 1;
                    i2 = 0;
                    removeMessages(0);
                    if (i == 1) {
                        removeMessages(17);
                    }
                    Message messageObtain = Message.obtain(this, 0);
                    messageObtain.arg1 = i;
                    messageObtain.arg2 = i2;
                    sendMessageDelayed(messageObtain, i != 0 ? 1000L : 0L);
                }
            } else {
                i = 0;
            }
            i2 = i;
            removeMessages(0);
            if (i == 1) {
            }
            Message messageObtain2 = Message.obtain(this, 0);
            messageObtain2.arg1 = i;
            messageObtain2.arg2 = i2;
            sendMessageDelayed(messageObtain2, i != 0 ? 1000L : 0L);
        }

        public void updateHostState(UsbPort usbPort, UsbPortStatus usbPortStatus) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = usbPort;
            someArgsObtain.arg2 = usbPortStatus;
            removeMessages(7);
            sendMessageDelayed(obtainMessage(7, someArgsObtain), 1000L);
        }

        private void setAdbEnabled(boolean z) {
            if (z != this.mAdbEnabled) {
                this.mAdbEnabled = z;
                if (z) {
                    setSystemProperty(USB_PERSISTENT_CONFIG_PROPERTY, "adb");
                } else {
                    setSystemProperty(USB_PERSISTENT_CONFIG_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
                setEnabledFunctions(this.mCurrentFunctions, true);
                updateAdbNotification(false);
            }
            if (this.mDebuggingManager != null) {
                this.mDebuggingManager.setAdbEnabled(this.mAdbEnabled);
            }
        }

        protected boolean isUsbTransferAllowed() {
            return !((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_usb_file_transfer");
        }

        private void updateCurrentAccessory() {
            boolean zHasMessages = hasMessages(8);
            if (this.mConfigured && zHasMessages) {
                String[] accessoryStrings = this.mUsbDeviceManager.getAccessoryStrings();
                if (accessoryStrings == null) {
                    Slog.e(UsbDeviceManager.TAG, "nativeGetAccessoryStrings failed");
                    return;
                }
                this.mCurrentAccessory = new UsbAccessory(accessoryStrings);
                Slog.d(UsbDeviceManager.TAG, "entering USB accessory mode: " + this.mCurrentAccessory);
                if (this.mBootCompleted) {
                    this.mUsbDeviceManager.getCurrentSettings().accessoryAttached(this.mCurrentAccessory);
                    return;
                }
                return;
            }
            if (!zHasMessages) {
                notifyAccessoryModeExit();
            }
        }

        private void notifyAccessoryModeExit() {
            Slog.d(UsbDeviceManager.TAG, "exited USB accessory mode");
            setEnabledFunctions(0L, false);
            if (this.mCurrentAccessory != null) {
                if (this.mBootCompleted) {
                    this.mSettingsManager.usbAccessoryRemoved(this.mCurrentAccessory);
                }
                this.mCurrentAccessory = null;
            }
        }

        protected SharedPreferences getPinnedSharedPrefs(Context context) {
            return context.createDeviceProtectedStorageContext().getSharedPreferences(new File(Environment.getDataSystemDeDirectory(0), UsbDeviceManager.USB_PREFS_XML), 0);
        }

        private boolean isUsbStateChanged(Intent intent) {
            Set<String> setKeySet = intent.getExtras().keySet();
            if (this.mBroadcastedIntent == null) {
                Iterator<String> it = setKeySet.iterator();
                while (it.hasNext()) {
                    if (intent.getBooleanExtra(it.next(), false)) {
                        return true;
                    }
                }
            } else {
                if (!setKeySet.equals(this.mBroadcastedIntent.getExtras().keySet())) {
                    return true;
                }
                for (String str : setKeySet) {
                    if (intent.getBooleanExtra(str, false) != this.mBroadcastedIntent.getBooleanExtra(str, false)) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected void updateUsbStateBroadcastIfNeeded(long j) {
            boolean z;
            Intent intent = new Intent("android.hardware.usb.action.USB_STATE");
            intent.addFlags(822083584);
            intent.putExtra("connected", this.mConnected);
            intent.putExtra("host_connected", this.mHostConnected);
            intent.putExtra("configured", this.mConfigured);
            if (isUsbTransferAllowed() && isUsbDataTransferActive(this.mCurrentFunctions)) {
                z = true;
            } else {
                z = false;
            }
            intent.putExtra("unlocked", z);
            while (j != 0) {
                intent.putExtra(UsbManager.usbFunctionsToString(Long.highestOneBit(j)), true);
                j -= Long.highestOneBit(j);
            }
            if (!isUsbStateChanged(intent)) {
                return;
            }
            sendStickyBroadcast(intent);
            this.mBroadcastedIntent = intent;
        }

        protected void sendStickyBroadcast(Intent intent) {
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void updateUsbFunctions() throws Throwable {
            updateMidiFunction();
        }

        private void updateMidiFunction() throws Throwable {
            Scanner scanner;
            boolean z = (this.mCurrentFunctions & 8) != 0;
            boolean z2 = this.mMidiEnabled;
            ?? r3 = z2;
            if (z != z2) {
                if (z) {
                    r3 = 0;
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
                        this.mMidiCard = scanner.nextInt();
                        int iNextInt = scanner.nextInt();
                        this.mMidiDevice = iNextInt;
                        scanner.close();
                        r3 = iNextInt;
                    } catch (FileNotFoundException e2) {
                        e = e2;
                        scanner2 = scanner;
                        Slog.e(UsbDeviceManager.TAG, "could not open MIDI file", e);
                        if (scanner2 != null) {
                            scanner2.close();
                        }
                        z = false;
                        r3 = scanner2;
                    } catch (Throwable th2) {
                        th = th2;
                        r3 = scanner;
                        if (r3 != 0) {
                            r3.close();
                        }
                        throw th;
                    }
                }
                this.mMidiEnabled = z;
            }
            this.mUsbAlsaManager.setPeripheralMidiState(this.mMidiEnabled && this.mConfigured, this.mMidiCard, this.mMidiDevice);
        }

        private void setScreenUnlockedFunctions() {
            setEnabledFunctions(this.mScreenUnlockedFunctions, false);
        }

        long getAppliedFunctions(long j) {
            if (j == 0) {
                return getChargingFunctions();
            }
            if (this.mAdbEnabled) {
                return j | 1;
            }
            return j;
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case 0:
                    this.mConnected = message.arg1 == 1;
                    this.mConfigured = message.arg2 == 1;
                    updateUsbNotification(false);
                    updateAdbNotification(false);
                    if (this.mBootCompleted) {
                        updateUsbStateBroadcastIfNeeded(getAppliedFunctions(this.mCurrentFunctions));
                    }
                    if ((this.mCurrentFunctions & 2) != 0) {
                        updateCurrentAccessory();
                    }
                    if (this.mBootCompleted) {
                        if (!this.mConnected && !hasMessages(8) && !hasMessages(17)) {
                            if (!this.mScreenLocked && this.mScreenUnlockedFunctions != 0) {
                                setScreenUnlockedFunctions();
                            } else {
                                setEnabledFunctions(0L, false);
                            }
                        }
                        updateUsbFunctions();
                    } else {
                        this.mPendingBootBroadcast = true;
                    }
                    break;
                case 1:
                    setAdbEnabled(message.arg1 == 1);
                    break;
                case 2:
                    setEnabledFunctions(((Long) message.obj).longValue(), false);
                    break;
                case 3:
                    this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
                    if (isTv()) {
                        this.mNotificationManager.createNotificationChannel(new NotificationChannel(UsbDeviceManager.ADB_NOTIFICATION_CHANNEL_ID_TV, this.mContext.getString(R.string.CndMmi), 4));
                    }
                    this.mSystemReady = true;
                    finishBoot();
                    break;
                case 4:
                    this.mBootCompleted = true;
                    finishBoot();
                    break;
                case 5:
                    if (this.mCurrentUser != message.arg1) {
                        this.mCurrentUser = message.arg1;
                        this.mScreenLocked = true;
                        this.mScreenUnlockedFunctions = 0L;
                        if (this.mSettings != null) {
                            this.mScreenUnlockedFunctions = UsbManager.usbFunctionsFromString(this.mSettings.getString(String.format(Locale.ENGLISH, UsbDeviceManager.UNLOCKED_CONFIG_PREF, Integer.valueOf(this.mCurrentUser)), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
                        }
                        setEnabledFunctions(0L, false);
                    }
                    break;
                case 6:
                    if (isUsbDataTransferActive(this.mCurrentFunctions) && !isUsbTransferAllowed()) {
                        setEnabledFunctions(0L, true);
                        break;
                    }
                    break;
                case 7:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    boolean z = this.mHostConnected;
                    UsbPort usbPort = (UsbPort) someArgs.arg1;
                    UsbPortStatus usbPortStatus = (UsbPortStatus) someArgs.arg2;
                    this.mHostConnected = usbPortStatus.getCurrentDataRole() == 1;
                    this.mSourcePower = usbPortStatus.getCurrentPowerRole() == 1;
                    this.mSinkPower = usbPortStatus.getCurrentPowerRole() == 2;
                    this.mAudioAccessoryConnected = usbPortStatus.getCurrentMode() == 4;
                    this.mAudioAccessorySupported = usbPort.isModeSupported(4);
                    this.mSupportsAllCombinations = usbPortStatus.isRoleCombinationSupported(1, 1) && usbPortStatus.isRoleCombinationSupported(2, 1) && usbPortStatus.isRoleCombinationSupported(1, 2) && usbPortStatus.isRoleCombinationSupported(2, 1);
                    someArgs.recycle();
                    updateUsbNotification(false);
                    if (this.mBootCompleted) {
                        if (this.mHostConnected || z) {
                            updateUsbStateBroadcastIfNeeded(getAppliedFunctions(this.mCurrentFunctions));
                        }
                    } else {
                        this.mPendingBootBroadcast = true;
                    }
                    break;
                case 8:
                    if (!this.mConnected || (this.mCurrentFunctions & 2) == 0) {
                        notifyAccessoryModeExit();
                    }
                    break;
                case 9:
                    this.mUsbCharging = message.arg1 == 1;
                    updateUsbNotification(false);
                    break;
                case 10:
                    Iterator it = (Iterator) message.obj;
                    int i = message.arg1;
                    this.mHideUsbNotification = false;
                    while (it.hasNext()) {
                        UsbDevice usbDevice = (UsbDevice) ((Map.Entry) it.next()).getValue();
                        int configurationCount = usbDevice.getConfigurationCount() - 1;
                        while (configurationCount >= 0) {
                            UsbConfiguration configuration = usbDevice.getConfiguration(configurationCount);
                            configurationCount--;
                            int interfaceCount = configuration.getInterfaceCount() - 1;
                            while (true) {
                                if (interfaceCount >= 0) {
                                    UsbInterface usbInterface = configuration.getInterface(interfaceCount);
                                    interfaceCount--;
                                    if (UsbDeviceManager.sBlackListedInterfaces.contains(Integer.valueOf(usbInterface.getInterfaceClass()))) {
                                        this.mHideUsbNotification = true;
                                    }
                                }
                            }
                        }
                    }
                    updateUsbNotification(false);
                    break;
                case 11:
                    updateAdbNotification(true);
                    updateUsbNotification(true);
                    break;
                case 12:
                    this.mScreenUnlockedFunctions = ((Long) message.obj).longValue();
                    if (this.mSettings != null) {
                        SharedPreferences.Editor editorEdit = this.mSettings.edit();
                        editorEdit.putString(String.format(Locale.ENGLISH, UsbDeviceManager.UNLOCKED_CONFIG_PREF, Integer.valueOf(this.mCurrentUser)), UsbManager.usbFunctionsToString(this.mScreenUnlockedFunctions));
                        editorEdit.commit();
                    }
                    if (!this.mScreenLocked && this.mScreenUnlockedFunctions != 0) {
                        setScreenUnlockedFunctions();
                        break;
                    }
                    break;
                case 13:
                    if ((message.arg1 == 1) != this.mScreenLocked) {
                        this.mScreenLocked = message.arg1 == 1;
                        if (this.mBootCompleted) {
                            if (this.mScreenLocked) {
                                if (!this.mConnected) {
                                    setEnabledFunctions(0L, false);
                                }
                                break;
                            } else if (this.mScreenUnlockedFunctions != 0 && this.mCurrentFunctions == 0) {
                                setScreenUnlockedFunctions();
                                break;
                            }
                        }
                    }
                    break;
            }
        }

        protected void finishBoot() throws Throwable {
            if (this.mBootCompleted && this.mCurrentUsbFunctionsReceived && this.mSystemReady) {
                if (this.mPendingBootBroadcast) {
                    updateUsbStateBroadcastIfNeeded(getAppliedFunctions(this.mCurrentFunctions));
                    this.mPendingBootBroadcast = false;
                }
                if (!this.mScreenLocked && this.mScreenUnlockedFunctions != 0) {
                    setScreenUnlockedFunctions();
                } else {
                    setEnabledFunctions(0L, false);
                }
                if (this.mCurrentAccessory != null) {
                    this.mUsbDeviceManager.getCurrentSettings().accessoryAttached(this.mCurrentAccessory);
                }
                if (this.mDebuggingManager != null) {
                    this.mDebuggingManager.setAdbEnabled(this.mAdbEnabled);
                }
                try {
                    putGlobalSettings(this.mContentResolver, "adb_enabled", this.mAdbEnabled ? 1 : 0);
                } catch (SecurityException e) {
                    Slog.d(UsbDeviceManager.TAG, "ADB_ENABLED is restricted.");
                }
                updateUsbNotification(false);
                updateAdbNotification(false);
                updateUsbFunctions();
            }
        }

        protected boolean isUsbDataTransferActive(long j) {
            return ((4 & j) == 0 && (j & 16) == 0) ? false : true;
        }

        public UsbAccessory getCurrentAccessory() {
            return this.mCurrentAccessory;
        }

        protected void updateUsbNotification(boolean z) {
            int i;
            int i2;
            PendingIntent activity;
            String str;
            CharSequence text;
            if (this.mNotificationManager == null || !this.mUseUsbNotification || "0".equals(getSystemProperty("persist.charging.notify", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS))) {
                return;
            }
            if (this.mHideUsbNotification && !this.mSupportsAllCombinations) {
                if (this.mUsbNotificationId != 0) {
                    this.mNotificationManager.cancelAsUser(null, this.mUsbNotificationId, UserHandle.ALL);
                    this.mUsbNotificationId = 0;
                    Slog.d(UsbDeviceManager.TAG, "Clear notification");
                    return;
                }
                return;
            }
            Resources resources = this.mContext.getResources();
            CharSequence text2 = resources.getText(R.string.miniresolver_private_space_messages_information);
            int i3 = 32;
            if (this.mAudioAccessoryConnected && !this.mAudioAccessorySupported) {
                i3 = 41;
                i = 17041014;
            } else if (this.mConnected) {
                if (this.mCurrentFunctions == 4) {
                    i = R.string.miniresolver_open_work;
                    i2 = 27;
                } else if (this.mCurrentFunctions == 16) {
                    i = R.string.miniresolver_sms_information;
                    i2 = 28;
                } else if (this.mCurrentFunctions == 8) {
                    i = R.string.mime_type_video_ext;
                    i2 = 29;
                } else if (this.mCurrentFunctions == 32) {
                    i = R.string.miniresolver_switch_to_work;
                    i2 = 47;
                } else if (this.mCurrentFunctions == 2) {
                    i = R.string.mime_type_spreadsheet_ext;
                    i2 = 30;
                } else {
                    i = 0;
                    i2 = 0;
                }
                if (this.mSourcePower) {
                    if (i != 0) {
                        text2 = resources.getText(R.string.miniresolver_private_space_phone_information);
                    }
                    i3 = 31;
                    i = 17041011;
                } else if (i == 0) {
                    i = 17041000;
                }
                i3 = i2;
            } else if (this.mSourcePower) {
                i3 = 31;
                i = 17041011;
            } else if (!this.mHostConnected || !this.mSinkPower || !this.mUsbCharging) {
                i = 0;
                i3 = 0;
            }
            if (i3 != this.mUsbNotificationId || z) {
                if (this.mUsbNotificationId != 0) {
                    this.mNotificationManager.cancelAsUser(null, this.mUsbNotificationId, UserHandle.ALL);
                    Slog.d(UsbDeviceManager.TAG, "Clear notification");
                    this.mUsbNotificationId = 0;
                }
                if (i3 != 0) {
                    CharSequence text3 = resources.getText(i);
                    if (i != 17041014) {
                        PendingIntent activityAsUser = PendingIntent.getActivityAsUser(this.mContext, 0, Intent.makeRestartActivityTask(new ComponentName("com.android.settings", "com.android.settings.Settings$UsbDetailsActivity")), 0, null, UserHandle.CURRENT);
                        str = SystemNotificationChannels.USB;
                        CharSequence charSequence = text2;
                        activity = activityAsUser;
                        text = charSequence;
                    } else {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.settings", "com.android.settings.HelpTrampoline");
                        intent.putExtra("android.intent.extra.TEXT", "help_url_audio_accessory_not_supported");
                        activity = this.mContext.getPackageManager().resolveActivity(intent, 0) != null ? PendingIntent.getActivity(this.mContext, 0, intent, 0) : null;
                        str = SystemNotificationChannels.ALERTS;
                        text = resources.getText(R.string.miniresolver_use_personal_browser);
                    }
                    if (BenesseExtension.getDchaState() != 0) {
                        activity = null;
                    }
                    Notification.Builder visibility = new Notification.Builder(this.mContext, str).setSmallIcon(R.drawable.pointer_hand_large_icon).setWhen(0L).setOngoing(true).setTicker(text3).setDefaults(0).setColor(this.mContext.getColor(R.color.car_colorPrimary)).setContentTitle(text3).setContentText(text).setContentIntent(activity).setVisibility(1);
                    if (i == 17041014) {
                        visibility.setStyle(new Notification.BigTextStyle().bigText(text));
                    }
                    this.mNotificationManager.notifyAsUser(null, i3, visibility.build(), UserHandle.ALL);
                    Slog.d(UsbDeviceManager.TAG, "push notification:" + ((Object) text3));
                    this.mUsbNotificationId = i3;
                }
            }
        }

        protected void updateAdbNotification(boolean z) {
            if (this.mNotificationManager == null) {
                return;
            }
            if (this.mAdbEnabled && this.mConnected) {
                if ("0".equals(getSystemProperty("persist.adb.notify", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS))) {
                    return;
                }
                if (z && this.mAdbNotificationShown) {
                    this.mAdbNotificationShown = false;
                    this.mNotificationManager.cancelAsUser(null, 26, UserHandle.ALL);
                }
                if (!this.mAdbNotificationShown) {
                    Resources resources = this.mContext.getResources();
                    CharSequence text = resources.getText(R.string.ClirMmi);
                    CharSequence text2 = resources.getText(R.string.ClipMmi);
                    Intent intent = new Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
                    intent.addFlags(268468224);
                    PendingIntent activityAsUser = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
                    if (BenesseExtension.getDchaState() != 0) {
                        activityAsUser = null;
                    }
                    Notification notificationBuild = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVELOPER).setSmallIcon(R.drawable.pointer_hand_large_icon).setWhen(0L).setOngoing(true).setTicker(text).setDefaults(0).setColor(this.mContext.getColor(R.color.car_colorPrimary)).setContentTitle(text).setContentText(text2).setContentIntent(activityAsUser).setVisibility(1).extend(new Notification.TvExtender().setChannelId(UsbDeviceManager.ADB_NOTIFICATION_CHANNEL_ID_TV)).build();
                    this.mAdbNotificationShown = true;
                    this.mNotificationManager.notifyAsUser(null, 26, notificationBuild, UserHandle.ALL);
                    return;
                }
                return;
            }
            if (this.mAdbNotificationShown) {
                this.mAdbNotificationShown = false;
                this.mNotificationManager.cancelAsUser(null, 26, UserHandle.ALL);
            }
        }

        private boolean isTv() {
            return this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        }

        protected long getChargingFunctions() {
            if (this.mAdbEnabled) {
                return 1L;
            }
            return 4L;
        }

        protected void setSystemProperty(String str, String str2) {
            SystemProperties.set(str, str2);
        }

        protected String getSystemProperty(String str, String str2) {
            return SystemProperties.get(str, str2);
        }

        protected void putGlobalSettings(ContentResolver contentResolver, String str, int i) {
            Settings.Global.putInt(contentResolver, str, i);
        }

        public long getEnabledFunctions() {
            return this.mCurrentFunctions;
        }

        public long getScreenUnlockedFunctions() {
            return this.mScreenUnlockedFunctions;
        }

        private void dumpFunctions(DualDumpOutputStream dualDumpOutputStream, String str, long j, long j2) {
            for (int i = 0; i < 63; i++) {
                long j3 = 1 << i;
                if ((j2 & j3) != 0) {
                    if (dualDumpOutputStream.isProto()) {
                        dualDumpOutputStream.write(str, j, j3);
                    } else {
                        dualDumpOutputStream.write(str, j, GadgetFunction.toString(j3));
                    }
                }
            }
        }

        public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
            long jStart = dualDumpOutputStream.start(str, j);
            dumpFunctions(dualDumpOutputStream, "current_functions", 2259152797697L, this.mCurrentFunctions);
            dualDumpOutputStream.write("current_functions_applied", 1133871366146L, this.mCurrentFunctionsApplied);
            dumpFunctions(dualDumpOutputStream, "screen_unlocked_functions", 2259152797699L, this.mScreenUnlockedFunctions);
            dualDumpOutputStream.write("screen_locked", 1133871366148L, this.mScreenLocked);
            dualDumpOutputStream.write("connected", 1133871366149L, this.mConnected);
            dualDumpOutputStream.write("configured", 1133871366150L, this.mConfigured);
            if (this.mCurrentAccessory != null) {
                DumpUtils.writeAccessory(dualDumpOutputStream, "current_accessory", 1146756268039L, this.mCurrentAccessory);
            }
            dualDumpOutputStream.write("host_connected", 1133871366152L, this.mHostConnected);
            dualDumpOutputStream.write("source_power", 1133871366153L, this.mSourcePower);
            dualDumpOutputStream.write("sink_power", 1133871366154L, this.mSinkPower);
            dualDumpOutputStream.write("usb_charging", 1133871366155L, this.mUsbCharging);
            dualDumpOutputStream.write("hide_usb_notification", 1133871366156L, this.mHideUsbNotification);
            dualDumpOutputStream.write("audio_accessory_connected", 1133871366157L, this.mAudioAccessoryConnected);
            dualDumpOutputStream.write("adb_enabled", 1133871366158L, this.mAdbEnabled);
            try {
                com.android.internal.util.dump.DumpUtils.writeStringIfNotNull(dualDumpOutputStream, "kernel_state", 1138166333455L, FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
            } catch (Exception e) {
                Slog.e(UsbDeviceManager.TAG, "Could not read kernel state", e);
            }
            try {
                com.android.internal.util.dump.DumpUtils.writeStringIfNotNull(dualDumpOutputStream, "kernel_function_list", 1138166333456L, FileUtils.readTextFile(new File(UsbDeviceManager.FUNCTIONS_PATH), 0, null).trim());
            } catch (Exception e2) {
                Slog.e(UsbDeviceManager.TAG, "Could not read kernel function list", e2);
            }
            dualDumpOutputStream.end(jStart);
        }
    }

    private static final class UsbHandlerLegacy extends UsbHandler {
        private static final String USB_CONFIG_PROPERTY = "sys.usb.config";
        private static final String USB_STATE_PROPERTY = "sys.usb.state";
        private String mCurrentFunctionsStr;
        private String mCurrentOemFunctions;
        private HashMap<String, HashMap<String, Pair<String, String>>> mOemModeMap;
        private boolean mUsbDataUnlocked;

        UsbHandlerLegacy(Looper looper, Context context, UsbDeviceManager usbDeviceManager, UsbDebuggingManager usbDebuggingManager, UsbAlsaManager usbAlsaManager, UsbSettingsManager usbSettingsManager) {
            super(looper, context, usbDeviceManager, usbDebuggingManager, usbAlsaManager, usbSettingsManager);
            try {
                readOemUsbOverrideConfig(context);
                this.mCurrentOemFunctions = getSystemProperty(getPersistProp(false), "none");
                if (isNormalBoot()) {
                    this.mCurrentFunctionsStr = getSystemProperty(USB_CONFIG_PROPERTY, "none");
                    this.mCurrentFunctionsApplied = this.mCurrentFunctionsStr.equals(getSystemProperty(USB_STATE_PROPERTY, "none"));
                } else {
                    this.mCurrentFunctionsStr = getSystemProperty(getPersistProp(true), "none");
                    this.mCurrentFunctionsApplied = getSystemProperty(USB_CONFIG_PROPERTY, "none").equals(getSystemProperty(USB_STATE_PROPERTY, "none"));
                }
                this.mCurrentFunctions = 0L;
                this.mCurrentUsbFunctionsReceived = true;
                updateState(FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
            } catch (Exception e) {
                Slog.e(UsbDeviceManager.TAG, "Error initializing UsbHandler", e);
            }
        }

        private void readOemUsbOverrideConfig(Context context) {
            String[] stringArray = context.getResources().getStringArray(R.array.config_defaultAllowlistLaunchOnPrivateDisplayPackages);
            if (stringArray != null) {
                for (String str : stringArray) {
                    String[] strArrSplit = str.split(":");
                    if (strArrSplit.length == 3 || strArrSplit.length == 4) {
                        if (this.mOemModeMap == null) {
                            this.mOemModeMap = new HashMap<>();
                        }
                        HashMap<String, Pair<String, String>> map = this.mOemModeMap.get(strArrSplit[0]);
                        if (map == null) {
                            map = new HashMap<>();
                            this.mOemModeMap.put(strArrSplit[0], map);
                        }
                        if (!map.containsKey(strArrSplit[1])) {
                            if (strArrSplit.length == 3) {
                                map.put(strArrSplit[1], new Pair<>(strArrSplit[2], BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
                            } else {
                                map.put(strArrSplit[1], new Pair<>(strArrSplit[2], strArrSplit[3]));
                            }
                        }
                    }
                }
            }
        }

        private String applyOemOverrideFunction(String str) {
            String strAddFunction;
            if (str == null || this.mOemModeMap == null) {
                return str;
            }
            String systemProperty = getSystemProperty(UsbDeviceManager.BOOT_MODE_PROPERTY, UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
            Slog.d(UsbDeviceManager.TAG, "applyOemOverride usbfunctions=" + str + " bootmode=" + systemProperty);
            HashMap<String, Pair<String, String>> map = this.mOemModeMap.get(systemProperty);
            if (map != null && !systemProperty.equals(UsbDeviceManager.NORMAL_BOOT) && !systemProperty.equals(UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN)) {
                Pair<String, String> pair = map.get(str);
                if (pair != null) {
                    Slog.d(UsbDeviceManager.TAG, "OEM USB override: " + str + " ==> " + ((String) pair.first) + " persist across reboot " + ((String) pair.second));
                    if (!((String) pair.second).equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) {
                        if (this.mAdbEnabled) {
                            strAddFunction = addFunction((String) pair.second, "adb");
                        } else {
                            strAddFunction = (String) pair.second;
                        }
                        Slog.d(UsbDeviceManager.TAG, "OEM USB override persisting: " + strAddFunction + "in prop: " + getPersistProp(false));
                        setSystemProperty(getPersistProp(false), strAddFunction);
                    }
                    return (String) pair.first;
                }
                if (this.mAdbEnabled) {
                    setSystemProperty(getPersistProp(false), addFunction("none", "adb"));
                } else {
                    setSystemProperty(getPersistProp(false), "none");
                }
            }
            return str;
        }

        private boolean waitForState(String str) {
            String systemProperty = null;
            for (int i = 0; i < 20; i++) {
                systemProperty = getSystemProperty(USB_STATE_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                if (str.equals(systemProperty)) {
                    return true;
                }
                SystemClock.sleep(50L);
            }
            Slog.e(UsbDeviceManager.TAG, "waitForState(" + str + ") FAILED: got " + systemProperty);
            return false;
        }

        private void setUsbConfig(String str) {
            setSystemProperty(USB_CONFIG_PROPERTY, str);
        }

        @Override
        protected void setEnabledFunctions(long j, boolean z) {
            boolean zIsUsbDataTransferActive = isUsbDataTransferActive(j);
            if (zIsUsbDataTransferActive != this.mUsbDataUnlocked) {
                this.mUsbDataUnlocked = zIsUsbDataTransferActive;
                updateUsbNotification(false);
                z = true;
            }
            long j2 = this.mCurrentFunctions;
            boolean z2 = this.mCurrentFunctionsApplied;
            if (trySetEnabledFunctions(j, z)) {
                return;
            }
            if (z2 && j2 != j) {
                Slog.e(UsbDeviceManager.TAG, "Failsafe 1: Restoring previous USB functions.");
                if (trySetEnabledFunctions(j2, false)) {
                    return;
                }
            }
            Slog.e(UsbDeviceManager.TAG, "Failsafe 2: Restoring default USB functions.");
            if (!trySetEnabledFunctions(0L, false)) {
                Slog.e(UsbDeviceManager.TAG, "Failsafe 3: Restoring empty function list (with ADB if enabled).");
                if (!trySetEnabledFunctions(0L, false)) {
                    Slog.e(UsbDeviceManager.TAG, "Unable to set any USB functions!");
                }
            }
        }

        private boolean isNormalBoot() {
            String systemProperty = getSystemProperty(UsbDeviceManager.BOOT_MODE_PROPERTY, UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
            return systemProperty.equals(UsbDeviceManager.NORMAL_BOOT) || systemProperty.equals(UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
        }

        protected String applyAdbFunction(String str) {
            if (str == null) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            if (this.mAdbEnabled) {
                return addFunction(str, "adb");
            }
            return removeFunction(str, "adb");
        }

        private boolean trySetEnabledFunctions(long j, boolean z) {
            String strUsbFunctionsToString;
            if (j != 0) {
                strUsbFunctionsToString = UsbManager.usbFunctionsToString(j);
            } else {
                strUsbFunctionsToString = null;
            }
            this.mCurrentFunctions = j;
            if (strUsbFunctionsToString == null || applyAdbFunction(strUsbFunctionsToString).equals("none")) {
                strUsbFunctionsToString = UsbManager.usbFunctionsToString(getChargingFunctions());
            }
            String strApplyAdbFunction = applyAdbFunction(strUsbFunctionsToString);
            String strApplyOemOverrideFunction = applyOemOverrideFunction(strApplyAdbFunction);
            if (!isNormalBoot() && !this.mCurrentFunctionsStr.equals(strApplyAdbFunction)) {
                setSystemProperty(getPersistProp(true), strApplyAdbFunction);
            }
            if ((!strApplyAdbFunction.equals(strApplyOemOverrideFunction) && !this.mCurrentOemFunctions.equals(strApplyOemOverrideFunction)) || !this.mCurrentFunctionsStr.equals(strApplyAdbFunction) || !this.mCurrentFunctionsApplied || z) {
                Slog.i(UsbDeviceManager.TAG, "Setting USB config to " + strApplyAdbFunction);
                this.mCurrentFunctionsStr = strApplyAdbFunction;
                this.mCurrentOemFunctions = strApplyOemOverrideFunction;
                this.mCurrentFunctionsApplied = false;
                setUsbConfig("none");
                if (!waitForState("none")) {
                    Slog.e(UsbDeviceManager.TAG, "Failed to kick USB config");
                    return false;
                }
                setUsbConfig(strApplyOemOverrideFunction);
                if (this.mBootCompleted && (containsFunction(strApplyAdbFunction, "mtp") || containsFunction(strApplyAdbFunction, "ptp"))) {
                    updateUsbStateBroadcastIfNeeded(getAppliedFunctions(this.mCurrentFunctions));
                }
                if (!waitForState(strApplyOemOverrideFunction)) {
                    Slog.e(UsbDeviceManager.TAG, "Failed to switch USB config to " + strApplyAdbFunction);
                    return false;
                }
                this.mCurrentFunctionsApplied = true;
            }
            return true;
        }

        private String getPersistProp(boolean z) {
            String systemProperty = getSystemProperty(UsbDeviceManager.BOOT_MODE_PROPERTY, UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
            if (systemProperty.equals(UsbDeviceManager.NORMAL_BOOT) || systemProperty.equals(UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN)) {
                return "persist.sys.usb.config";
            }
            if (z) {
                return "persist.sys.usb." + systemProperty + ".func";
            }
            return "persist.sys.usb." + systemProperty + ".config";
        }

        private static String addFunction(String str, String str2) {
            if ("none".equals(str)) {
                return str2;
            }
            if (!containsFunction(str, str2)) {
                if (str.length() > 0) {
                    str = str + ",";
                }
                return str + str2;
            }
            return str;
        }

        private static String removeFunction(String str, String str2) {
            String[] strArrSplit = str.split(",");
            for (int i = 0; i < strArrSplit.length; i++) {
                if (str2.equals(strArrSplit[i])) {
                    strArrSplit[i] = null;
                }
            }
            if (strArrSplit.length == 1 && strArrSplit[0] == null) {
                return "none";
            }
            StringBuilder sb = new StringBuilder();
            for (String str3 : strArrSplit) {
                if (str3 != null) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(str3);
                }
            }
            return sb.toString();
        }

        static boolean containsFunction(String str, String str2) {
            int iIndexOf = str.indexOf(str2);
            if (iIndexOf < 0) {
                return false;
            }
            if (iIndexOf > 0 && str.charAt(iIndexOf - 1) != ',') {
                return false;
            }
            int length = iIndexOf + str2.length();
            if (length < str.length() && str.charAt(length) != ',') {
                return false;
            }
            return true;
        }
    }

    private static final class UsbHandlerHal extends UsbHandler {
        protected static final String ADBD = "adbd";
        protected static final String CTL_START = "ctl.start";
        protected static final String CTL_STOP = "ctl.stop";
        private static final int ENUMERATION_TIME_OUT_MS = 2000;
        private static final int SET_FUNCTIONS_LEEWAY_MS = 500;
        private static final int SET_FUNCTIONS_TIMEOUT_MS = 3000;
        private static final int USB_GADGET_HAL_DEATH_COOKIE = 2000;
        private int mCurrentRequest;
        protected boolean mCurrentUsbFunctionsRequested;

        @GuardedBy("mGadgetProxyLock")
        private IUsbGadget mGadgetProxy;
        private final Object mGadgetProxyLock;

        UsbHandlerHal(Looper looper, Context context, UsbDeviceManager usbDeviceManager, UsbDebuggingManager usbDebuggingManager, UsbAlsaManager usbAlsaManager, UsbSettingsManager usbSettingsManager) {
            super(looper, context, usbDeviceManager, usbDebuggingManager, usbAlsaManager, usbSettingsManager);
            this.mGadgetProxyLock = new Object();
            this.mCurrentRequest = 0;
            try {
                if (!IServiceManager.getService().registerForNotifications(IUsbGadget.kInterfaceName, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, new ServiceNotification())) {
                    Slog.e(UsbDeviceManager.TAG, "Failed to register usb gadget service start notification");
                    return;
                }
                synchronized (this.mGadgetProxyLock) {
                    this.mGadgetProxy = IUsbGadget.getService(true);
                    this.mGadgetProxy.linkToDeath(new UsbGadgetDeathRecipient(), 2000L);
                    this.mCurrentFunctions = 0L;
                    this.mGadgetProxy.getCurrentUsbFunctions(new UsbGadgetCallback());
                    this.mCurrentUsbFunctionsRequested = true;
                }
                updateState(FileUtils.readTextFile(new File(UsbDeviceManager.STATE_PATH), 0, null).trim());
            } catch (RemoteException e) {
                Slog.e(UsbDeviceManager.TAG, "Usb Gadget hal not responding", e);
            } catch (NoSuchElementException e2) {
                Slog.e(UsbDeviceManager.TAG, "Usb gadget hal not found", e2);
            } catch (Exception e3) {
                Slog.e(UsbDeviceManager.TAG, "Error initializing UsbHandler", e3);
            }
        }

        final class UsbGadgetDeathRecipient implements IHwBinder.DeathRecipient {
            UsbGadgetDeathRecipient() {
            }

            @Override
            public void serviceDied(long j) {
                if (j == 2000) {
                    Slog.e(UsbDeviceManager.TAG, "Usb Gadget hal service died cookie: " + j);
                    synchronized (UsbHandlerHal.this.mGadgetProxyLock) {
                        UsbHandlerHal.this.mGadgetProxy = null;
                    }
                }
            }
        }

        final class ServiceNotification extends IServiceNotification.Stub {
            ServiceNotification() {
            }

            public void onRegistration(String str, String str2, boolean z) {
                Slog.i(UsbDeviceManager.TAG, "Usb gadget hal service started " + str + " " + str2);
                synchronized (UsbHandlerHal.this.mGadgetProxyLock) {
                    try {
                        UsbHandlerHal.this.mGadgetProxy = IUsbGadget.getService();
                        UsbHandlerHal.this.mGadgetProxy.linkToDeath(UsbHandlerHal.this.new UsbGadgetDeathRecipient(), 2000L);
                    } catch (RemoteException e) {
                        Slog.e(UsbDeviceManager.TAG, "Usb Gadget hal not responding", e);
                    } catch (NoSuchElementException e2) {
                        Slog.e(UsbDeviceManager.TAG, "Usb gadget hal not found", e2);
                    }
                    if (!UsbHandlerHal.this.mCurrentFunctionsApplied && !UsbHandlerHal.this.mCurrentUsbFunctionsRequested) {
                        UsbHandlerHal.this.setEnabledFunctions(UsbHandlerHal.this.mCurrentFunctions, false);
                    }
                }
            }
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case 14:
                    setEnabledFunctions(0L, false);
                    break;
                case 15:
                    Slog.e(UsbDeviceManager.TAG, "Set functions timed out! no reply from usb hal");
                    if (message.arg1 != 1) {
                        setEnabledFunctions(0L, false);
                    }
                    break;
                case 16:
                    Slog.e(UsbDeviceManager.TAG, "prcessing MSG_GET_CURRENT_USB_FUNCTIONS");
                    this.mCurrentUsbFunctionsReceived = true;
                    if (this.mCurrentUsbFunctionsRequested) {
                        Slog.e(UsbDeviceManager.TAG, "updating mCurrentFunctions");
                        this.mCurrentFunctions = ((Long) message.obj).longValue() & (-2);
                        Slog.e(UsbDeviceManager.TAG, "mCurrentFunctions:" + this.mCurrentFunctions + "applied:" + message.arg1);
                        this.mCurrentFunctionsApplied = message.arg1 == 1;
                    }
                    finishBoot();
                    break;
                case 17:
                    if (message.arg1 != 1) {
                        setEnabledFunctions(0L, !this.mAdbEnabled);
                    }
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }

        private class UsbGadgetCallback extends IUsbGadgetCallback.Stub {
            boolean mChargingFunctions;
            long mFunctions;
            int mRequest;

            UsbGadgetCallback() {
            }

            UsbGadgetCallback(int i, long j, boolean z) {
                this.mRequest = i;
                this.mFunctions = j;
                this.mChargingFunctions = z;
            }

            @Override
            public void setCurrentUsbFunctionsCb(long j, int i) {
                if (UsbHandlerHal.this.mCurrentRequest != this.mRequest || !UsbHandlerHal.this.hasMessages(15) || this.mFunctions != j) {
                    return;
                }
                UsbHandlerHal.this.removeMessages(15);
                Slog.e(UsbDeviceManager.TAG, "notifyCurrentFunction request:" + this.mRequest + " status:" + i);
                if (i == 0) {
                    UsbHandlerHal.this.mCurrentFunctionsApplied = true;
                } else if (!this.mChargingFunctions) {
                    Slog.e(UsbDeviceManager.TAG, "Setting default fuctions");
                    UsbHandlerHal.this.sendEmptyMessage(14);
                }
            }

            @Override
            public void getCurrentUsbFunctionsCb(long j, int i) {
                UsbHandlerHal.this.sendMessage(16, Long.valueOf(j), i == 2);
            }
        }

        private void setUsbConfig(long j, boolean z) {
            String str = UsbDeviceManager.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("setUsbConfig(");
            sb.append(j);
            sb.append(") request:");
            int i = this.mCurrentRequest + 1;
            this.mCurrentRequest = i;
            sb.append(i);
            Slog.d(str, sb.toString());
            removeMessages(17);
            removeMessages(15);
            removeMessages(14);
            synchronized (this.mGadgetProxyLock) {
                if (this.mGadgetProxy == null) {
                    Slog.e(UsbDeviceManager.TAG, "setUsbConfig mGadgetProxy is null");
                    return;
                }
                try {
                    if ((1 & j) != 0) {
                        setSystemProperty(CTL_START, ADBD);
                    } else {
                        setSystemProperty(CTL_STOP, ADBD);
                    }
                    this.mGadgetProxy.setCurrentUsbFunctions(j, new UsbGadgetCallback(this.mCurrentRequest, j, z), 2500L);
                    sendMessageDelayed(15, z, 3000L);
                    sendMessageDelayed(17, z, 5000L);
                } catch (RemoteException e) {
                    Slog.e(UsbDeviceManager.TAG, "Remoteexception while calling setCurrentUsbFunctions", e);
                }
            }
        }

        @Override
        protected void setEnabledFunctions(long j, boolean z) {
            if (this.mCurrentFunctions != j || !this.mCurrentFunctionsApplied || z) {
                Slog.i(UsbDeviceManager.TAG, "Setting USB config to " + UsbManager.usbFunctionsToString(j));
                this.mCurrentFunctions = j;
                this.mCurrentFunctionsApplied = false;
                this.mCurrentUsbFunctionsRequested = false;
                boolean z2 = j == 0;
                long appliedFunctions = getAppliedFunctions(j);
                setUsbConfig(appliedFunctions, z2);
                if (this.mBootCompleted && isUsbDataTransferActive(appliedFunctions)) {
                    updateUsbStateBroadcastIfNeeded(appliedFunctions);
                }
            }
        }
    }

    public UsbAccessory getCurrentAccessory() {
        return this.mHandler.getCurrentAccessory();
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory usbAccessory, UsbUserSettingsManager usbUserSettingsManager) {
        UsbAccessory currentAccessory = this.mHandler.getCurrentAccessory();
        if (currentAccessory == null) {
            throw new IllegalArgumentException("no accessory attached");
        }
        if (!currentAccessory.equals(usbAccessory)) {
            throw new IllegalArgumentException(usbAccessory.toString() + " does not match current accessory " + currentAccessory);
        }
        usbUserSettingsManager.checkPermission(usbAccessory);
        return nativeOpenAccessory();
    }

    public long getCurrentFunctions() {
        return this.mHandler.getEnabledFunctions();
    }

    public ParcelFileDescriptor getControlFd(long j) {
        FileDescriptor fileDescriptor = this.mControlFds.get(Long.valueOf(j));
        if (fileDescriptor == null) {
            return null;
        }
        try {
            return ParcelFileDescriptor.dup(fileDescriptor);
        } catch (IOException e) {
            Slog.e(TAG, "Could not dup fd for " + j);
            return null;
        }
    }

    public long getScreenUnlockedFunctions() {
        return this.mHandler.getScreenUnlockedFunctions();
    }

    public void setCurrentFunctions(long j) {
        if (j == 0) {
            MetricsLogger.action(this.mContext, 1275);
        } else if (j == 4) {
            MetricsLogger.action(this.mContext, 1276);
        } else if (j == 16) {
            MetricsLogger.action(this.mContext, 1277);
        } else if (j == 8) {
            MetricsLogger.action(this.mContext, 1279);
        } else if (j == 32) {
            MetricsLogger.action(this.mContext, 1278);
        } else if (j == 2) {
            MetricsLogger.action(this.mContext, 1280);
        }
        this.mHandler.sendMessage(2, Long.valueOf(j));
    }

    public void setScreenUnlockedFunctions(long j) {
        this.mHandler.sendMessage(12, Long.valueOf(j));
    }

    public void allowUsbDebugging(boolean z, String str) {
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.allowUsbDebugging(z, str);
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

    public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        long jStart = dualDumpOutputStream.start(str, j);
        if (this.mHandler != null) {
            this.mHandler.dump(dualDumpOutputStream, "handler", 1146756268033L);
        }
        if (this.mDebuggingManager != null) {
            this.mDebuggingManager.dump(dualDumpOutputStream, "debugging_manager", 1146756268034L);
        }
        dualDumpOutputStream.end(jStart);
    }
}
