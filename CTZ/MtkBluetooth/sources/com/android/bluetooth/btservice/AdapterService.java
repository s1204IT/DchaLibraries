package com.android.bluetooth.btservice;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothCallback;
import android.bluetooth.IBluetoothSocketManager;
import android.bluetooth.OobData;
import android.bluetooth.UidTraffic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StatsLog;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.RemoteDevices;
import com.android.bluetooth.gatt.GattService;
import com.android.bluetooth.sdp.SdpManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class AdapterService extends Service {
    private static final String ACTION_ALARM_WAKEUP = "com.android.bluetooth.btservice.action.ALARM_WAKEUP";
    public static final String ACTION_LOAD_ADAPTER_PROPERTIES = "com.android.bluetooth.btservice.action.LOAD_ADAPTER_PROPERTIES";
    public static final String ACTION_SERVICE_STATE_CHANGED = "com.android.bluetooth.btservice.action.STATE_CHANGED";
    public static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    static final String BLUETOOTH_BTSNOOP_ENABLE_PROPERTY = "persist.bluetooth.btsnoopenable";
    static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    public static final String BLUETOOTH_PRIVILEGED = "android.permission.BLUETOOTH_PRIVILEGED";
    private static final int CLEANUP_NATIVE_TIMEOUT_DELAY = 2000;
    private static final int CONTROLLER_ENERGY_UPDATE_TIMEOUT_MILLIS = 30;
    public static final String EXTRA_ACTION = "action";
    static final String LOCAL_MAC_ADDRESS_PERM = "android.permission.LOCAL_MAC_ADDRESS";
    private static final String MESSAGE_ACCESS_PERMISSION_PREFERENCE_FILE = "message_access_permission";
    private static final int MESSAGE_CLEANUP_NATIVE_TIMEOUT = 4;
    private static final int MESSAGE_PROFILE_SERVICE_REGISTERED = 2;
    private static final int MESSAGE_PROFILE_SERVICE_STATE_CHANGED = 1;
    private static final int MESSAGE_PROFILE_SERVICE_UNREGISTERED = 3;
    private static final int MIN_ADVT_INSTANCES_FOR_MA = 5;
    private static final int MIN_OFFLOADED_FILTERS = 10;
    private static final int MIN_OFFLOADED_SCAN_STORAGE_BYTES = 1024;
    private static final String PHONEBOOK_ACCESS_PERMISSION_PREFERENCE_FILE = "phonebook_access_permission";
    public static final int PROFILE_CONN_REJECTED = 2;
    static final String RECEIVE_MAP_PERM = "android.permission.RECEIVE_BLUETOOTH_MAP";
    private static final String SIM_ACCESS_PERMISSION_PREFERENCE_FILE = "sim_access_permission";
    private static final String TAG = "BluetoothAdapterService";
    private static AdapterService sAdapterService;
    public static final BroadcastReceiver sUserSwitchedReceiver;
    private ActiveDeviceManager mActiveDeviceManager;
    private AdapterProperties mAdapterProperties;
    private AdapterState mAdapterStateMachine;
    private AlarmManager mAlarmManager;
    private IBatteryStats mBatteryStats;
    private AdapterServiceBinder mBinder;
    private BondStateMachine mBondStateMachine;
    private RemoteCallbackList<IBluetoothCallback> mCallbacks;
    private boolean mCleaningUp;
    private int mCurrentRequestId;
    private long mEnergyUsedTotalVoltAmpSecMicro;
    private Handler mHandlerForCleanUpTimeout;
    private HandlerThread mHandlerThread;
    private long mIdleTimeTotalMs;
    private JniCallbacks mJniCallbacks;
    private boolean mNativeAvailable;
    private PendingIntent mPendingAlarm;
    private PhonePolicy mPhonePolicy;
    private PowerManager mPowerManager;
    private ProfileObserver mProfileObserver;
    private RemoteDevices mRemoteDevices;
    private long mRxTimeTotalMs;
    private int mStackReportedState;
    private long mTxTimeTotalMs;
    private UserManager mUserManager;
    private PowerManager.WakeLock mWakeLock;
    private String mWakeLockName;
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final boolean VERBOSE = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private final Object mEnergyInfoLock = new Object();
    private final SparseArray<UidTraffic> mUidTraffic = new SparseArray<>();
    private final ArrayList<ProfileService> mRegisteredProfiles = new ArrayList<>();
    private final ArrayList<ProfileService> mRunningProfiles = new ArrayList<>();
    private boolean mSnoopLogSettingAtEnable = false;
    private SdpManager mSdpManager = null;
    private final HashMap<String, Integer> mProfileServicesState = new HashMap<>();
    private boolean mQuietmode = false;
    private final AdapterServiceHandler mHandler = new AdapterServiceHandler();
    private final BroadcastReceiver mAlarmBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (AdapterService.this) {
                AdapterService.this.mPendingAlarm = null;
                AdapterService.this.alarmFiredNative();
            }
        }
    };

    private native void alarmFiredNative();

    private native boolean cancelDiscoveryNative();

    static native void classInitNative();

    private native byte[] dumpMetricsNative();

    private native void dumpNative(FileDescriptor fileDescriptor, String[] strArr);

    private native IBinder getSocketManagerNative();

    private native void interopDatabaseAddNative(int i, byte[] bArr, int i2);

    private native void interopDatabaseClearNative();

    private native boolean pinReplyNative(byte[] bArr, boolean z, int i, byte[] bArr2);

    private native int readEnergyInfo();

    private static native void setForegroundUserIdNative(int i);

    private native void setSystemUiUidNative(int i);

    private native boolean sspReplyNative(byte[] bArr, int i, boolean z, int i2);

    private native boolean startDiscoveryNative();

    native boolean cancelBondNative(byte[] bArr);

    native void cleanupNative();

    native boolean createBondNative(byte[] bArr, int i);

    native boolean createBondOutOfBandNative(byte[] bArr, int i, OobData oobData);

    native boolean disableNative();

    native boolean enableNative(boolean z);

    native boolean factoryResetNative();

    native boolean getAdapterPropertiesNative();

    native boolean getAdapterPropertyNative(int i);

    native int getConnectionStateNative(byte[] bArr);

    native boolean getDevicePropertyNative(byte[] bArr, int i);

    native boolean getRemoteMasInstancesNative(byte[] bArr);

    native boolean getRemoteServicesNative(byte[] bArr);

    native boolean initNative(boolean z);

    native boolean removeBondNative(byte[] bArr);

    native boolean sdpSearchNative(byte[] bArr, byte[] bArr2);

    native boolean setAdapterPropertyNative(int i);

    native boolean setAdapterPropertyNative(int i, byte[] bArr);

    native boolean setDevicePropertyNative(byte[] bArr, int i, byte[] bArr2);

    static {
        classInitNative();
        sUserSwitchedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                    int intExtra = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    Utils.setForegroundUserId(intExtra);
                    AdapterService.setForegroundUserIdNative(intExtra);
                }
            }
        };
    }

    public static synchronized AdapterService getAdapterService() {
        if (DBG) {
            Log.d(TAG, "getAdapterService() - returning " + sAdapterService);
        }
        return sAdapterService;
    }

    private static synchronized void setAdapterService(AdapterService adapterService) {
        if (DBG) {
            Log.d(TAG, "setAdapterService() - trying to set service to " + adapterService);
        }
        if (adapterService == null) {
            return;
        }
        sAdapterService = adapterService;
    }

    private static synchronized void clearAdapterService(AdapterService adapterService) {
        if (sAdapterService == adapterService) {
            sAdapterService = null;
        }
    }

    public void addProfile(ProfileService profileService) {
        this.mHandler.obtainMessage(2, profileService).sendToTarget();
    }

    public void removeProfile(ProfileService profileService) {
        this.mHandler.obtainMessage(3, profileService).sendToTarget();
    }

    public void onProfileServiceStateChanged(ProfileService profileService, int i) {
        if (i != 12 && i != 10) {
            throw new IllegalArgumentException(BluetoothAdapter.nameForState(i));
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(1);
        messageObtainMessage.obj = profileService;
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    class AdapterServiceHandler extends Handler {
        AdapterServiceHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            AdapterService.this.debugLog("handleMessage() - Message: " + message.what);
            switch (message.what) {
                case 1:
                    AdapterService.this.debugLog("handleMessage() - MESSAGE_PROFILE_SERVICE_STATE_CHANGED");
                    processProfileServiceStateChanged((ProfileService) message.obj, message.arg1);
                    break;
                case 2:
                    AdapterService.this.debugLog("handleMessage() - MESSAGE_PROFILE_SERVICE_REGISTERED");
                    registerProfileService((ProfileService) message.obj);
                    break;
                case 3:
                    AdapterService.this.debugLog("handleMessage() - MESSAGE_PROFILE_SERVICE_UNREGISTERED");
                    unregisterProfileService((ProfileService) message.obj);
                    break;
            }
        }

        private void registerProfileService(ProfileService profileService) {
            if (!AdapterService.this.mRegisteredProfiles.contains(profileService)) {
                AdapterService.this.mRegisteredProfiles.add(profileService);
                return;
            }
            Log.e(AdapterService.TAG, profileService.getName() + " already registered.");
        }

        private void unregisterProfileService(ProfileService profileService) {
            if (AdapterService.this.mRegisteredProfiles.contains(profileService)) {
                AdapterService.this.mRegisteredProfiles.remove(profileService);
                return;
            }
            Log.e(AdapterService.TAG, profileService.getName() + " not registered (UNREGISTERED).");
        }

        private void processProfileServiceStateChanged(ProfileService profileService, int i) {
            if (i == 10) {
                if (AdapterService.this.mRegisteredProfiles.contains(profileService)) {
                    if (AdapterService.this.mRunningProfiles.contains(profileService)) {
                        AdapterService.this.mRunningProfiles.remove(profileService);
                        if (AdapterService.this.mRunningProfiles.size() != 1 || !GattService.class.getSimpleName().equals(((ProfileService) AdapterService.this.mRunningProfiles.get(0)).getName())) {
                            if (AdapterService.this.mRunningProfiles.size() == 0) {
                                AdapterService.this.disableNative();
                                AdapterService.this.mAdapterStateMachine.sendMessage(8);
                                return;
                            }
                            return;
                        }
                        AdapterService.this.mAdapterStateMachine.sendMessage(6);
                        return;
                    }
                    Log.e(AdapterService.TAG, profileService.getName() + " not running.");
                    return;
                }
                Log.e(AdapterService.TAG, profileService.getName() + " not registered (STATE_OFF).");
                return;
            }
            if (i == 12) {
                if (AdapterService.this.mRegisteredProfiles.contains(profileService)) {
                    if (!AdapterService.this.mRunningProfiles.contains(profileService)) {
                        AdapterService.this.mRunningProfiles.add(profileService);
                        if (GattService.class.getSimpleName().equals(profileService.getName())) {
                            AdapterService.this.enableNativeWithGuestFlag();
                            return;
                        }
                        if (AdapterService.this.mRegisteredProfiles.size() == Config.getSupportedProfiles().length && AdapterService.this.mRegisteredProfiles.size() == AdapterService.this.mRunningProfiles.size()) {
                            AdapterService.this.mAdapterProperties.onBluetoothReady();
                            AdapterService.this.updateUuids();
                            AdapterService.this.setBluetoothClassFromConfig();
                            AdapterService.this.mAdapterStateMachine.sendMessage(5);
                            return;
                        }
                        return;
                    }
                    Log.e(AdapterService.TAG, profileService.getName() + " already running.");
                    return;
                }
                Log.e(AdapterService.TAG, profileService.getName() + " not registered (STATE_ON).");
                return;
            }
            Log.e(AdapterService.TAG, "Unhandled profile state: " + i);
        }
    }

    private void updateInteropDatabase() {
        interopDatabaseClearNative();
        String string = Settings.Global.getString(getContentResolver(), "bluetooth_interoperability_list");
        if (string == null) {
            return;
        }
        Log.d(TAG, "updateInteropDatabase: [" + string + "]");
        String[] strArrSplit = string.split(";");
        int length = strArrSplit.length;
        for (int i = 0; i < length; i++) {
            String[] strArrSplit2 = strArrSplit[i].split(",");
            if (strArrSplit2.length == 2) {
                try {
                    int i2 = Integer.parseInt(strArrSplit2[1]);
                    int length2 = (strArrSplit2[0].length() + 1) / 3;
                    if (length2 < 1 || length2 > 6) {
                        Log.e(TAG, "updateInteropDatabase: Malformed address string '" + strArrSplit2[0] + "'");
                    } else {
                        byte[] bArr = new byte[6];
                        int i3 = 0;
                        int i4 = 0;
                        while (i3 < strArrSplit2[0].length()) {
                            if (strArrSplit2[0].charAt(i3) == ':') {
                                i3++;
                            } else {
                                int i5 = i4 + 1;
                                try {
                                    int i6 = i3 + 2;
                                    bArr[i4] = (byte) Integer.parseInt(strArrSplit2[0].substring(i3, i6), 16);
                                    i4 = i5;
                                    i3 = i6;
                                } catch (NumberFormatException e) {
                                    i4 = 0;
                                }
                            }
                        }
                        if (i4 != 0) {
                            interopDatabaseAddNative(i2, bArr, length2);
                        }
                    }
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "updateInteropDatabase: Invalid feature '" + strArrSplit2[1] + "'");
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        debugLog("onCreate()");
        this.mRemoteDevices = new RemoteDevices(this, Looper.getMainLooper());
        this.mRemoteDevices.init();
        this.mBinder = new AdapterServiceBinder(this);
        this.mAdapterProperties = new AdapterProperties(this);
        this.mAdapterStateMachine = AdapterState.make(this);
        this.mJniCallbacks = new JniCallbacks(this, this.mAdapterProperties);
        initNative(getApplicationContext().getPackageManager().hasSystemFeature("android.software.leanback_only"));
        this.mNativeAvailable = true;
        this.mCallbacks = new RemoteCallbackList<>();
        getAdapterPropertyNative(2);
        getAdapterPropertyNative(1);
        getAdapterPropertyNative(4);
        this.mAlarmManager = (AlarmManager) getSystemService(NotificationCompat.CATEGORY_ALARM);
        this.mPowerManager = (PowerManager) getSystemService("power");
        this.mUserManager = (UserManager) getSystemService("user");
        this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mSdpManager = SdpManager.init(this);
        registerReceiver(this.mAlarmBroadcastReceiver, new IntentFilter(ACTION_ALARM_WAKEUP));
        this.mProfileObserver = new ProfileObserver(getApplicationContext(), this, new Handler());
        this.mProfileObserver.start();
        if (getResources().getBoolean(R.bool.enable_phone_policy)) {
            Log.i(TAG, "Phone policy enabled");
            this.mPhonePolicy = new PhonePolicy(this, new ServiceFactory());
            this.mPhonePolicy.start();
        } else {
            Log.i(TAG, "Phone policy disabled");
        }
        this.mActiveDeviceManager = new ActiveDeviceManager(this, new ServiceFactory());
        this.mActiveDeviceManager.start();
        setAdapterService(this);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                AdapterService.this.getSharedPreferences(AdapterService.PHONEBOOK_ACCESS_PERMISSION_PREFERENCE_FILE, 0);
                AdapterService.this.getSharedPreferences(AdapterService.MESSAGE_ACCESS_PERMISSION_PREFERENCE_FILE, 0);
                AdapterService.this.getSharedPreferences(AdapterService.SIM_ACCESS_PERMISSION_PREFERENCE_FILE, 0);
                return null;
            }
        }.execute(new Void[0]);
        this.mHandlerThread = new HandlerThread("CLEANUP_NATIVE_TIMEOUT_THREAD");
        this.mHandlerThread.start();
        this.mHandlerForCleanUpTimeout = new Handler(this.mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                AdapterService.this.debugLog("handleMessage() - Message: " + message.what);
                if (message.what == 4) {
                    AdapterService.this.debugLog("MESSAGE_CLEANUP_NATIVE_TIMEOUT");
                    AdapterService.this.onDestroy();
                }
            }
        };
        try {
            int packageUidAsUser = getApplicationContext().getPackageManager().getPackageUidAsUser("com.android.systemui", 1048576, 0);
            Utils.setSystemUiUid(packageUidAsUser);
            setSystemUiUidNative(packageUidAsUser);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to resolve SystemUI's UID.", e);
        }
        getApplicationContext().registerReceiverAsUser(sUserSwitchedReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_SWITCHED"), null, null);
        int currentUser = ActivityManager.getCurrentUser();
        Utils.setForegroundUserId(currentUser);
        setForegroundUserIdNative(currentUser);
    }

    @Override
    public IBinder onBind(Intent intent) {
        debugLog("onBind()");
        return this.mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        debugLog("onUnbind() - calling cleanup");
        cleanup();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        debugLog("onDestroy()");
        this.mProfileObserver.stop();
        this.mHandlerThread.quitSafely();
        if (!isMock()) {
            Log.i(TAG, "Force exit to cleanup internal state in Bluetooth stack");
            System.exit(0);
        }
    }

    void bringUpBle() {
        debugLog("bleOnProcessStart()");
        if (getResources().getBoolean(android.R.^attr-private.colorAccentSecondary)) {
            Config.init(getApplicationContext());
        }
        this.mRemoteDevices.reset();
        this.mAdapterProperties.init(this.mRemoteDevices);
        debugLog("bleOnProcessStart() - Make Bond State Machine");
        this.mBondStateMachine = BondStateMachine.make(this, this.mAdapterProperties, this.mRemoteDevices);
        this.mJniCallbacks.init(this.mBondStateMachine, this.mRemoteDevices);
        try {
            this.mBatteryStats.noteResetBleScan();
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException trying to send a reset to BatteryStats");
        }
        StatsLog.write_non_chained(2, -1, null, 2, false, false, false);
        setProfileServiceState(GattService.class, 12);
    }

    void bringDownBle() {
        stopGattProfileService();
    }

    void stateChangeCallback(int i) {
        if (i == 0) {
            debugLog("stateChangeCallback: disableNative() completed");
            return;
        }
        if (i == 1) {
            this.mAdapterStateMachine.sendMessage(7);
            return;
        }
        Log.e(TAG, "Incorrect status " + i + " in stateChangeCallback");
    }

    void setBluetoothClassFromConfig() {
        int iRetrieveBluetoothClassConfig = retrieveBluetoothClassConfig();
        if (iRetrieveBluetoothClassConfig != 0) {
            this.mAdapterProperties.setBluetoothClass(new BluetoothClass(iRetrieveBluetoothClassConfig));
        }
    }

    private int retrieveBluetoothClassConfig() {
        return Settings.Global.getInt(getContentResolver(), "bluetooth_class_of_device", 0);
    }

    private boolean storeBluetoothClassConfig(int i) {
        boolean zPutInt = Settings.Global.putInt(getContentResolver(), "bluetooth_class_of_device", i);
        if (!zPutInt) {
            Log.e(TAG, "Error storing BluetoothClass config - " + i);
        }
        return zPutInt;
    }

    void startProfileServices() {
        debugLog("startCoreServices()");
        Class[] supportedProfiles = Config.getSupportedProfiles();
        if (supportedProfiles.length == 1 && GattService.class.getSimpleName().equals(supportedProfiles[0].getSimpleName())) {
            this.mAdapterProperties.onBluetoothReady();
            updateUuids();
            setBluetoothClassFromConfig();
            this.mAdapterStateMachine.sendMessage(5);
            return;
        }
        setAllProfileServiceStates(supportedProfiles, 12);
    }

    void stopProfileServices() {
        this.mAdapterProperties.onBluetoothDisable();
        Class[] supportedProfiles = Config.getSupportedProfiles();
        if (supportedProfiles.length == 1 && this.mRunningProfiles.size() == 1 && GattService.class.getSimpleName().equals(this.mRunningProfiles.get(0).getName())) {
            debugLog("stopProfileServices() - No profiles services to stop or already stopped.");
            this.mAdapterStateMachine.sendMessage(6);
        } else {
            setAllProfileServiceStates(supportedProfiles, 10);
        }
    }

    private void stopGattProfileService() {
        this.mAdapterProperties.onBleDisable();
        if (this.mRunningProfiles.size() == 0) {
            debugLog("stopGattProfileService() - No profiles services to stop.");
            this.mAdapterStateMachine.sendMessage(8);
        }
        setProfileServiceState(GattService.class, 10);
    }

    void updateAdapterState(int i, int i2) {
        this.mAdapterProperties.setState(i2);
        if (this.mCallbacks != null) {
            int iBeginBroadcast = this.mCallbacks.beginBroadcast();
            debugLog("updateAdapterState() - Broadcasting state " + BluetoothAdapter.nameForState(i2) + " to " + iBeginBroadcast + " receivers.");
            for (int i3 = 0; i3 < iBeginBroadcast; i3++) {
                try {
                    this.mCallbacks.getBroadcastItem(i3).onBluetoothStateChange(i, i2);
                } catch (RemoteException e) {
                    debugLog("updateAdapterState() - Callback #" + i3 + " failed (" + e + ")");
                }
            }
            this.mCallbacks.finishBroadcast();
        }
        if (i2 == 14) {
            this.mSnoopLogSettingAtEnable = SystemProperties.getBoolean(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, false);
            return;
        }
        if (i2 == 15 && i != 10) {
            if (this.mSnoopLogSettingAtEnable != SystemProperties.getBoolean(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, false)) {
                this.mAdapterStateMachine.sendMessage(4);
            }
        }
    }

    void cleanup() {
        debugLog("cleanup()");
        if (this.mCleaningUp) {
            errorLog("cleanup() - Service already starting to cleanup, ignoring request...");
            return;
        }
        clearAdapterService(this);
        this.mCleaningUp = true;
        unregisterReceiver(this.mAlarmBroadcastReceiver);
        if (this.mPendingAlarm != null) {
            this.mAlarmManager.cancel(this.mPendingAlarm);
            this.mPendingAlarm = null;
        }
        synchronized (this) {
            if (this.mWakeLock != null) {
                if (this.mWakeLock.isHeld()) {
                    this.mWakeLock.release();
                }
                this.mWakeLock = null;
            }
        }
        if (this.mAdapterStateMachine != null) {
            this.mAdapterStateMachine.doQuit();
        }
        if (this.mBondStateMachine != null) {
            this.mBondStateMachine.doQuit();
        }
        if (this.mRemoteDevices != null) {
            this.mRemoteDevices.cleanup();
        }
        if (this.mSdpManager != null) {
            this.mSdpManager.cleanup();
            this.mSdpManager = null;
        }
        if (this.mAdapterProperties != null) {
            this.mAdapterProperties.cleanup();
        }
        if (this.mJniCallbacks != null) {
            this.mJniCallbacks.cleanup();
        }
        if (this.mPhonePolicy != null) {
            this.mPhonePolicy.cleanup();
        }
        if (this.mActiveDeviceManager != null) {
            this.mActiveDeviceManager.cleanup();
        }
        if (this.mProfileServicesState != null) {
            this.mProfileServicesState.clear();
        }
        if (this.mBinder != null) {
            this.mBinder.cleanup();
            this.mBinder = null;
        }
        if (this.mCallbacks != null) {
            this.mCallbacks.kill();
        }
        if (this.mNativeAvailable) {
            debugLog("cleanup() - Cleaning up adapter native");
            this.mHandlerForCleanUpTimeout.sendMessageDelayed(this.mHandlerForCleanUpTimeout.obtainMessage(4), 2000L);
            cleanupNative();
            this.mHandlerForCleanUpTimeout.removeMessages(4);
            this.mNativeAvailable = false;
        }
    }

    private void setProfileServiceState(Class cls, int i) {
        Intent intent = new Intent(this, (Class<?>) cls);
        intent.putExtra(EXTRA_ACTION, ACTION_SERVICE_STATE_CHANGED);
        intent.putExtra("android.bluetooth.adapter.extra.STATE", i);
        startService(intent);
    }

    private void setAllProfileServiceStates(Class[] clsArr, int i) {
        for (Class cls : clsArr) {
            if (!GattService.class.getSimpleName().equals(cls.getSimpleName())) {
                setProfileServiceState(cls, i);
            }
        }
    }

    private boolean isAvailable() {
        return !this.mCleaningUp;
    }

    private static class AdapterServiceBinder extends IBluetooth.Stub {
        private AdapterService mService;

        AdapterServiceBinder(AdapterService adapterService) {
            this.mService = adapterService;
        }

        public void cleanup() {
            this.mService = null;
        }

        public AdapterService getService() {
            if (this.mService != null && this.mService.isAvailable()) {
                return this.mService;
            }
            return null;
        }

        public boolean isEnabled() {
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.isEnabled();
        }

        public int getState() {
            AdapterService service = getService();
            if (service == null) {
                return 10;
            }
            return service.getState();
        }

        public boolean enable() {
            if (Binder.getCallingUid() != 1000 && !Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "enable() - Not allowed for non-active user and non system user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.enable();
        }

        public boolean enableNoAutoConnect() {
            if (Binder.getCallingUid() != 1000 && !Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "enableNoAuto() - Not allowed for non-active user and non system user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.enableNoAutoConnect();
        }

        public boolean disable() {
            if (Binder.getCallingUid() != 1000 && !Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "disable() - Not allowed for non-active user and non system user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.disable();
        }

        public String getAddress() {
            if (Binder.getCallingUid() != 1000 && !Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "getAddress() - Not allowed for non-active user and non system user");
                return null;
            }
            AdapterService service = getService();
            if (service == null) {
                return null;
            }
            return service.getAddress();
        }

        public ParcelUuid[] getUuids() {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "getUuids() - Not allowed for non-active user");
                return new ParcelUuid[0];
            }
            AdapterService service = getService();
            if (service == null) {
                return new ParcelUuid[0];
            }
            return service.getUuids();
        }

        public String getName() {
            if (Binder.getCallingUid() != 1000 && !Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "getName() - Not allowed for non-active user and non system user");
                return null;
            }
            AdapterService service = getService();
            if (service == null) {
                return null;
            }
            return service.getName();
        }

        public boolean setName(String str) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setName() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setName(str);
        }

        public BluetoothClass getBluetoothClass() {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "getBluetoothClass() - Not allowed for non-active user");
                return null;
            }
            AdapterService service = getService();
            if (service == null) {
                return null;
            }
            return service.getBluetoothClass();
        }

        public boolean setBluetoothClass(BluetoothClass bluetoothClass) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setBluetoothClass() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setBluetoothClass(bluetoothClass);
        }

        public int getScanMode() {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "getScanMode() - Not allowed for non-active user");
                return 20;
            }
            AdapterService service = getService();
            if (service == null) {
                return 20;
            }
            return service.getScanMode();
        }

        public boolean setScanMode(int i, int i2) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setScanMode() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setScanMode(i, i2);
        }

        public int getDiscoverableTimeout() {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "getDiscoverableTimeout() - Not allowed for non-active user");
                return 0;
            }
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getDiscoverableTimeout();
        }

        public boolean setDiscoverableTimeout(int i) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setDiscoverableTimeout() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setDiscoverableTimeout(i);
        }

        public boolean startDiscovery() {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "startDiscovery() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.startDiscovery();
        }

        public boolean cancelDiscovery() {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "cancelDiscovery() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.cancelDiscovery();
        }

        public boolean isDiscovering() {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "isDiscovering() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.isDiscovering();
        }

        public long getDiscoveryEndMillis() {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "getDiscoveryEndMillis() - Not allowed for non-active user");
                return -1L;
            }
            AdapterService service = getService();
            if (service == null) {
                return -1L;
            }
            return service.getDiscoveryEndMillis();
        }

        public BluetoothDevice[] getBondedDevices() {
            AdapterService service = getService();
            if (service == null) {
                return new BluetoothDevice[0];
            }
            return service.getBondedDevices();
        }

        public int getAdapterConnectionState() {
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getAdapterConnectionState();
        }

        public int getProfileConnectionState(int i) {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "getProfileConnectionState- Not allowed for non-active user");
                return 0;
            }
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getProfileConnectionState(i);
        }

        public boolean createBond(BluetoothDevice bluetoothDevice, int i) {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "createBond() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.createBond(bluetoothDevice, i, null);
        }

        public boolean createBondOutOfBand(BluetoothDevice bluetoothDevice, int i, OobData oobData) {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "createBondOutOfBand() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.createBond(bluetoothDevice, i, oobData);
        }

        public boolean cancelBondProcess(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "cancelBondProcess() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.cancelBondProcess(bluetoothDevice);
        }

        public boolean removeBond(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "removeBond() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.removeBond(bluetoothDevice);
        }

        public int getBondState(BluetoothDevice bluetoothDevice) {
            AdapterService service = getService();
            if (service == null) {
                return 10;
            }
            return service.getBondState(bluetoothDevice);
        }

        public boolean isBondingInitiatedLocally(BluetoothDevice bluetoothDevice) {
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.isBondingInitiatedLocally(bluetoothDevice);
        }

        public long getSupportedProfiles() {
            AdapterService service = getService();
            if (service == null) {
                return 0L;
            }
            return service.getSupportedProfiles();
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public String getRemoteName(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "getRemoteName() - Not allowed for non-active user");
                return null;
            }
            AdapterService service = getService();
            if (service == null) {
                return null;
            }
            return service.getRemoteName(bluetoothDevice);
        }

        public int getRemoteType(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "getRemoteType() - Not allowed for non-active user");
                return 0;
            }
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getRemoteType(bluetoothDevice);
        }

        public String getRemoteAlias(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "getRemoteAlias() - Not allowed for non-active user");
                return null;
            }
            AdapterService service = getService();
            if (service == null) {
                return null;
            }
            return service.getRemoteAlias(bluetoothDevice);
        }

        public boolean setRemoteAlias(BluetoothDevice bluetoothDevice, String str) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setRemoteAlias() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setRemoteAlias(bluetoothDevice, str);
        }

        public int getRemoteClass(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "getRemoteClass() - Not allowed for non-active user");
                return 0;
            }
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getRemoteClass(bluetoothDevice);
        }

        public ParcelUuid[] getRemoteUuids(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "getRemoteUuids() - Not allowed for non-active user");
                return new ParcelUuid[0];
            }
            AdapterService service = getService();
            if (service == null) {
                return new ParcelUuid[0];
            }
            return service.getRemoteUuids(bluetoothDevice);
        }

        public boolean fetchRemoteUuids(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(AdapterService.TAG, "fetchRemoteUuids() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.fetchRemoteUuids(bluetoothDevice);
        }

        public boolean setPin(BluetoothDevice bluetoothDevice, boolean z, int i, byte[] bArr) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setPin() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPin(bluetoothDevice, z, i, bArr);
        }

        public boolean setPasskey(BluetoothDevice bluetoothDevice, boolean z, int i, byte[] bArr) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setPasskey() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPasskey(bluetoothDevice, z, i, bArr);
        }

        public boolean setPairingConfirmation(BluetoothDevice bluetoothDevice, boolean z) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setPairingConfirmation() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPairingConfirmation(bluetoothDevice, z);
        }

        public int getPhonebookAccessPermission(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "getPhonebookAccessPermission() - Not allowed for non-active user");
                return 0;
            }
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getPhonebookAccessPermission(bluetoothDevice);
        }

        public boolean setPhonebookAccessPermission(BluetoothDevice bluetoothDevice, int i) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setPhonebookAccessPermission() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPhonebookAccessPermission(bluetoothDevice, i);
        }

        public int getMessageAccessPermission(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "getMessageAccessPermission() - Not allowed for non-active user");
                return 0;
            }
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getMessageAccessPermission(bluetoothDevice);
        }

        public boolean setMessageAccessPermission(BluetoothDevice bluetoothDevice, int i) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setMessageAccessPermission() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setMessageAccessPermission(bluetoothDevice, i);
        }

        public int getSimAccessPermission(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "getSimAccessPermission() - Not allowed for non-active user");
                return 0;
            }
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getSimAccessPermission(bluetoothDevice);
        }

        public boolean setSimAccessPermission(BluetoothDevice bluetoothDevice, int i) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "setSimAccessPermission() - Not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.setSimAccessPermission(bluetoothDevice, i);
        }

        public void sendConnectionStateChange(BluetoothDevice bluetoothDevice, int i, int i2, int i3) {
            AdapterService service = getService();
            if (service == null) {
                return;
            }
            service.sendConnectionStateChange(bluetoothDevice, i, i2, i3);
        }

        public IBluetoothSocketManager getSocketManager() {
            AdapterService service = getService();
            if (service == null) {
                return null;
            }
            return service.getSocketManager();
        }

        public boolean sdpSearch(BluetoothDevice bluetoothDevice, ParcelUuid parcelUuid) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "sdpSea(): not allowed for non-active user");
                return false;
            }
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.sdpSearch(bluetoothDevice, parcelUuid);
        }

        public int getBatteryLevel(BluetoothDevice bluetoothDevice) {
            if (!Utils.checkCaller()) {
                Log.w(AdapterService.TAG, "getBatteryLevel(): not allowed for non-active user");
                return -1;
            }
            AdapterService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getBatteryLevel(bluetoothDevice);
        }

        public int getMaxConnectedAudioDevices() {
            AdapterService service = getService();
            if (service == null) {
                return 1;
            }
            return service.getMaxConnectedAudioDevices();
        }

        public boolean isA2dpOffloadEnabled() {
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.isA2dpOffloadEnabled();
        }

        public boolean factoryReset() {
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.factoryReset();
        }

        public void registerCallback(IBluetoothCallback iBluetoothCallback) {
            AdapterService service = getService();
            if (service == null) {
                return;
            }
            service.registerCallback(iBluetoothCallback);
        }

        public void unregisterCallback(IBluetoothCallback iBluetoothCallback) {
            AdapterService service = getService();
            if (service == null) {
                return;
            }
            service.unregisterCallback(iBluetoothCallback);
        }

        public boolean isMultiAdvertisementSupported() {
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.isMultiAdvertisementSupported();
        }

        public boolean isOffloadedFilteringSupported() {
            AdapterService service = getService();
            return service != null && service.getNumOfOffloadedScanFilterSupported() >= 10;
        }

        public boolean isOffloadedScanBatchingSupported() {
            AdapterService service = getService();
            return service != null && service.getOffloadedScanResultStorage() >= 1024;
        }

        public boolean isLe2MPhySupported() {
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.isLe2MPhySupported();
        }

        public boolean isLeCodedPhySupported() {
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.isLeCodedPhySupported();
        }

        public boolean isLeExtendedAdvertisingSupported() {
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.isLeExtendedAdvertisingSupported();
        }

        public boolean isLePeriodicAdvertisingSupported() {
            AdapterService service = getService();
            if (service == null) {
                return false;
            }
            return service.isLePeriodicAdvertisingSupported();
        }

        public int getLeMaximumAdvertisingDataLength() {
            AdapterService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getLeMaximumAdvertisingDataLength();
        }

        public boolean isActivityAndEnergyReportingSupported() {
            AdapterService service = getService();
            if (service != null) {
                return service.isActivityAndEnergyReportingSupported();
            }
            return false;
        }

        public BluetoothActivityEnergyInfo reportActivityInfo() {
            AdapterService service = getService();
            if (service != null) {
                return service.reportActivityInfo();
            }
            return null;
        }

        public void requestActivityInfo(ResultReceiver resultReceiver) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("controller_activity", reportActivityInfo());
            resultReceiver.send(0, bundle);
        }

        public void onLeServiceUp() {
            AdapterService service = getService();
            if (service == null) {
                return;
            }
            service.onLeServiceUp();
        }

        public void onBrEdrDown() {
            AdapterService service = getService();
            if (service == null) {
                return;
            }
            service.onBrEdrDown();
        }

        public void dump(FileDescriptor fileDescriptor, String[] strArr) {
            PrintWriter printWriter = new PrintWriter(new FileOutputStream(fileDescriptor));
            AdapterService service = getService();
            if (service == null) {
                return;
            }
            service.dump(fileDescriptor, printWriter, strArr);
            printWriter.close();
        }
    }

    public boolean isEnabled() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getState() == 12;
    }

    public int getState() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        if (this.mAdapterProperties != null) {
            return this.mAdapterProperties.getState();
        }
        return 10;
    }

    public boolean enable() {
        return enable(false);
    }

    public boolean enableNoAutoConnect() {
        return enable(true);
    }

    public synchronized boolean enable(boolean z) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        if (this.mUserManager.hasUserRestriction("no_bluetooth", UserHandle.SYSTEM)) {
            debugLog("enable() called when Bluetooth was disallowed");
            return false;
        }
        debugLog("enable() - Enable called with quiet mode status =  " + z);
        this.mQuietmode = z;
        this.mAdapterStateMachine.sendMessage(3);
        return true;
    }

    boolean disable() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        debugLog("disable() called with mRunningProfiles.size() = " + this.mRunningProfiles.size());
        SystemProperties.set("persist.vendor.bluetooth.state", "0");
        this.mAdapterStateMachine.sendMessage(2);
        return true;
    }

    String getAddress() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        enforceCallingOrSelfPermission(LOCAL_MAC_ADDRESS_PERM, "Need LOCAL_MAC_ADDRESS permission");
        return Utils.getAddressStringFromByte(this.mAdapterProperties.getAddress());
    }

    ParcelUuid[] getUuids() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getUuids();
    }

    public String getName() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        try {
            return this.mAdapterProperties.getName();
        } catch (Throwable th) {
            debugLog("getName() - Unexpected exception (" + th + ")");
            return null;
        }
    }

    boolean setName(String str) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        return this.mAdapterProperties.setName(str);
    }

    BluetoothClass getBluetoothClass() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        return this.mAdapterProperties.getBluetoothClass();
    }

    boolean setBluetoothClass(BluetoothClass bluetoothClass) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED", "Need BLUETOOTH PRIVILEGED permission");
        debugLog("setBluetoothClass() to " + bluetoothClass);
        boolean bluetoothClass2 = this.mAdapterProperties.setBluetoothClass(bluetoothClass);
        if (!bluetoothClass2) {
            Log.e(TAG, "setBluetoothClass() to " + bluetoothClass + " failed");
        }
        return bluetoothClass2 && storeBluetoothClassConfig(bluetoothClass.getClassOfDevice());
    }

    int getScanMode() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getScanMode();
    }

    boolean setScanMode(int i, int i2) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        setDiscoverableTimeout(i2);
        return this.mAdapterProperties.setScanMode(convertScanModeToHal(i));
    }

    int getDiscoverableTimeout() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getDiscoverableTimeout();
    }

    boolean setDiscoverableTimeout(int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.setDiscoverableTimeout(i);
    }

    boolean startDiscovery() {
        debugLog("startDiscovery");
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        return startDiscoveryNative();
    }

    boolean cancelDiscovery() {
        debugLog("cancelDiscovery");
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        return cancelDiscoveryNative();
    }

    boolean isDiscovering() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.isDiscovering();
    }

    long getDiscoveryEndMillis() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.discoveryEndMillis();
    }

    public BluetoothDevice[] getBondedDevices() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getBondedDevices();
    }

    int getAdapterConnectionState() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getConnectionState();
    }

    int getProfileConnectionState(int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getProfileConnectionState(i);
    }

    boolean sdpSearch(BluetoothDevice bluetoothDevice, ParcelUuid parcelUuid) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        if (this.mSdpManager != null) {
            this.mSdpManager.sdpSearch(bluetoothDevice, parcelUuid);
            return true;
        }
        return false;
    }

    boolean createBond(BluetoothDevice bluetoothDevice, int i, OobData oobData) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if ((deviceProperties != null && deviceProperties.getBondState() != 10) || getState() == 10 || getState() == 14 || getState() == 16) {
            return false;
        }
        this.mRemoteDevices.setBondingInitiatedLocally(Utils.getByteAddress(bluetoothDevice));
        cancelDiscoveryNative();
        Message messageObtainMessage = this.mBondStateMachine.obtainMessage(1);
        messageObtainMessage.obj = bluetoothDevice;
        messageObtainMessage.arg1 = i;
        if (oobData != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(BondStateMachine.OOBDATA, oobData);
            messageObtainMessage.setData(bundle);
        }
        this.mBondStateMachine.sendMessage(messageObtainMessage);
        return true;
    }

    public boolean isQuietModeEnabled() {
        debugLog("isQuetModeEnabled() - Enabled = " + this.mQuietmode);
        return this.mQuietmode;
    }

    public void updateUuids() {
        debugLog("updateUuids() - Updating UUIDs for bonded devices");
        BluetoothDevice[] bondedDevices = getBondedDevices();
        if (bondedDevices == null) {
            return;
        }
        for (BluetoothDevice bluetoothDevice : bondedDevices) {
            this.mRemoteDevices.updateUuids(bluetoothDevice);
        }
    }

    public void deviceUuidUpdated(BluetoothDevice bluetoothDevice) {
        Message messageObtainMessage = this.mBondStateMachine.obtainMessage(10);
        messageObtainMessage.obj = bluetoothDevice;
        this.mBondStateMachine.sendMessage(messageObtainMessage);
    }

    boolean cancelBondProcess(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        byte[] bytesFromAddress = Utils.getBytesFromAddress(bluetoothDevice.getAddress());
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties != null) {
            deviceProperties.setBondingInitiatedLocally(false);
        }
        return cancelBondNative(bytesFromAddress);
    }

    boolean removeBond(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null || deviceProperties.getBondState() != 12) {
            return false;
        }
        deviceProperties.setBondingInitiatedLocally(false);
        Message messageObtainMessage = this.mBondStateMachine.obtainMessage(3);
        messageObtainMessage.obj = bluetoothDevice;
        this.mBondStateMachine.sendMessage(messageObtainMessage);
        return true;
    }

    @VisibleForTesting
    public int getBondState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            return 10;
        }
        return deviceProperties.getBondState();
    }

    boolean isBondingInitiatedLocally(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            return false;
        }
        return deviceProperties.isBondingInitiatedLocally();
    }

    long getSupportedProfiles() {
        return Config.getSupportedProfilesBitMask();
    }

    int getConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return getConnectionStateNative(Utils.getBytesFromAddress(bluetoothDevice.getAddress()));
    }

    public String getRemoteName(BluetoothDevice bluetoothDevice) {
        RemoteDevices.DeviceProperties deviceProperties;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        if (this.mRemoteDevices == null || (deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice)) == null) {
            return null;
        }
        return deviceProperties.getName();
    }

    int getRemoteType(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            return 0;
        }
        return deviceProperties.getDeviceType();
    }

    String getRemoteAlias(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            return null;
        }
        return deviceProperties.getAlias();
    }

    boolean setRemoteAlias(BluetoothDevice bluetoothDevice, String str) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            return false;
        }
        deviceProperties.setAlias(bluetoothDevice, str);
        return true;
    }

    int getRemoteClass(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            return 0;
        }
        return deviceProperties.getBluetoothClass();
    }

    @VisibleForTesting
    public ParcelUuid[] getRemoteUuids(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            return null;
        }
        return deviceProperties.getUuids();
    }

    boolean fetchRemoteUuids(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        this.mRemoteDevices.fetchUuids(bluetoothDevice);
        return true;
    }

    int getBatteryLevel(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            return -1;
        }
        return deviceProperties.getBatteryLevel();
    }

    boolean setPin(BluetoothDevice bluetoothDevice, boolean z, int i, byte[] bArr) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null || (deviceProperties.getBondState() != 11 && deviceProperties.getBondState() != 12)) {
            return false;
        }
        if (bArr.length != i) {
            EventLog.writeEvent(1397638484, "139287605", -1, "PIN code length mismatch");
            return false;
        }
        return pinReplyNative(Utils.getBytesFromAddress(bluetoothDevice.getAddress()), z, i, bArr);
    }

    boolean setPasskey(BluetoothDevice bluetoothDevice, boolean z, int i, byte[] bArr) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null || deviceProperties.getBondState() != 11) {
            return false;
        }
        if (bArr.length != i) {
            EventLog.writeEvent(1397638484, "139287605", -1, "Passkey length mismatch");
            return false;
        }
        return sspReplyNative(Utils.getBytesFromAddress(bluetoothDevice.getAddress()), 1, z, Utils.byteArrayToInt(bArr));
    }

    boolean setPairingConfirmation(BluetoothDevice bluetoothDevice, boolean z) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED", "Need BLUETOOTH PRIVILEGED permission");
        RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null || deviceProperties.getBondState() != 11) {
            return false;
        }
        return sspReplyNative(Utils.getBytesFromAddress(bluetoothDevice.getAddress()), 0, z, 0);
    }

    int getPhonebookAccessPermission(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        SharedPreferences sharedPreferences = getSharedPreferences(PHONEBOOK_ACCESS_PERMISSION_PREFERENCE_FILE, 0);
        if (sharedPreferences.contains(bluetoothDevice.getAddress())) {
            return sharedPreferences.getBoolean(bluetoothDevice.getAddress(), false) ? 1 : 2;
        }
        return 0;
    }

    boolean setPhonebookAccessPermission(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED", "Need BLUETOOTH PRIVILEGED permission");
        SharedPreferences.Editor editorEdit = getSharedPreferences(PHONEBOOK_ACCESS_PERMISSION_PREFERENCE_FILE, 0).edit();
        if (i != 0) {
            editorEdit.putBoolean(bluetoothDevice.getAddress(), i == 1);
        } else {
            editorEdit.remove(bluetoothDevice.getAddress());
        }
        editorEdit.apply();
        return true;
    }

    int getMessageAccessPermission(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        SharedPreferences sharedPreferences = getSharedPreferences(MESSAGE_ACCESS_PERMISSION_PREFERENCE_FILE, 0);
        if (sharedPreferences.contains(bluetoothDevice.getAddress())) {
            return sharedPreferences.getBoolean(bluetoothDevice.getAddress(), false) ? 1 : 2;
        }
        return 0;
    }

    boolean setMessageAccessPermission(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED", "Need BLUETOOTH PRIVILEGED permission");
        SharedPreferences.Editor editorEdit = getSharedPreferences(MESSAGE_ACCESS_PERMISSION_PREFERENCE_FILE, 0).edit();
        if (i != 0) {
            editorEdit.putBoolean(bluetoothDevice.getAddress(), i == 1);
        } else {
            editorEdit.remove(bluetoothDevice.getAddress());
        }
        editorEdit.apply();
        return true;
    }

    int getSimAccessPermission(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        SharedPreferences sharedPreferences = getSharedPreferences(SIM_ACCESS_PERMISSION_PREFERENCE_FILE, 0);
        if (sharedPreferences.contains(bluetoothDevice.getAddress())) {
            return sharedPreferences.getBoolean(bluetoothDevice.getAddress(), false) ? 1 : 2;
        }
        return 0;
    }

    boolean setSimAccessPermission(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED", "Need BLUETOOTH PRIVILEGED permission");
        boolean z = false;
        SharedPreferences.Editor editorEdit = getSharedPreferences(SIM_ACCESS_PERMISSION_PREFERENCE_FILE, 0).edit();
        if (i == 0) {
            editorEdit.remove(bluetoothDevice.getAddress());
        } else {
            String address = bluetoothDevice.getAddress();
            if (i == 1) {
                z = true;
            }
            editorEdit.putBoolean(address, z);
        }
        editorEdit.apply();
        return true;
    }

    void sendConnectionStateChange(BluetoothDevice bluetoothDevice, int i, int i2, int i3) {
        if (getState() == 10) {
            return;
        }
        this.mAdapterProperties.sendConnectionStateChange(bluetoothDevice, i, i2, i3);
    }

    IBluetoothSocketManager getSocketManager() {
        IBinder socketManagerNative = getSocketManagerNative();
        if (socketManagerNative == null) {
            return null;
        }
        return IBluetoothSocketManager.Stub.asInterface(socketManagerNative);
    }

    boolean factoryReset() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED", "Need BLUETOOTH permission");
        this.mPhonePolicy.cleanProfilePriorities();
        return factoryResetNative();
    }

    void registerCallback(IBluetoothCallback iBluetoothCallback) {
        this.mCallbacks.register(iBluetoothCallback);
    }

    void unregisterCallback(IBluetoothCallback iBluetoothCallback) {
        this.mCallbacks.unregister(iBluetoothCallback);
    }

    public int getNumOfAdvertisementInstancesSupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getNumOfAdvertisementInstancesSupported();
    }

    public boolean isMultiAdvertisementSupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return getNumOfAdvertisementInstancesSupported() >= 5;
    }

    public boolean isRpaOffloadSupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.isRpaOffloadSupported();
    }

    public int getNumOfOffloadedIrkSupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getNumOfOffloadedIrkSupported();
    }

    public int getNumOfOffloadedScanFilterSupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getNumOfOffloadedScanFilterSupported();
    }

    public int getOffloadedScanResultStorage() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getOffloadedScanResultStorage();
    }

    private boolean isActivityAndEnergyReportingSupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED", "Need BLUETOOTH permission");
        return this.mAdapterProperties.isActivityAndEnergyReportingSupported();
    }

    public boolean isLe2MPhySupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.isLe2MPhySupported();
    }

    public boolean isLeCodedPhySupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.isLeCodedPhySupported();
    }

    public boolean isLeExtendedAdvertisingSupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.isLeExtendedAdvertisingSupported();
    }

    public boolean isLePeriodicAdvertisingSupported() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.isLePeriodicAdvertisingSupported();
    }

    public int getLeMaximumAdvertisingDataLength() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getLeMaximumAdvertisingDataLength();
    }

    public int getMaxConnectedAudioDevices() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getMaxConnectedAudioDevices();
    }

    public boolean isA2dpOffloadEnabled() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.isA2dpOffloadEnabled();
    }

    private BluetoothActivityEnergyInfo reportActivityInfo() {
        BluetoothActivityEnergyInfo bluetoothActivityEnergyInfo;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_PRIVILEGED", "Need BLUETOOTH permission");
        if (this.mAdapterProperties.getState() != 12 || !this.mAdapterProperties.isActivityAndEnergyReportingSupported()) {
            return null;
        }
        readEnergyInfo();
        synchronized (this.mEnergyInfoLock) {
            try {
                this.mEnergyInfoLock.wait(30L);
            } catch (InterruptedException e) {
            }
            bluetoothActivityEnergyInfo = new BluetoothActivityEnergyInfo(SystemClock.elapsedRealtime(), this.mStackReportedState, this.mTxTimeTotalMs, this.mRxTimeTotalMs, this.mIdleTimeTotalMs, this.mEnergyUsedTotalVoltAmpSecMicro);
            int i = 0;
            for (int i2 = 0; i2 < this.mUidTraffic.size(); i2++) {
                UidTraffic uidTrafficValueAt = this.mUidTraffic.valueAt(i2);
                if (uidTrafficValueAt.getTxBytes() != 0 || uidTrafficValueAt.getRxBytes() != 0) {
                    i++;
                }
            }
            UidTraffic[] uidTrafficArr = i > 0 ? new UidTraffic[i] : null;
            int i3 = 0;
            for (int i4 = 0; i4 < this.mUidTraffic.size(); i4++) {
                UidTraffic uidTrafficValueAt2 = this.mUidTraffic.valueAt(i4);
                if (uidTrafficValueAt2.getTxBytes() != 0 || uidTrafficValueAt2.getRxBytes() != 0) {
                    uidTrafficArr[i3] = uidTrafficValueAt2.clone();
                    i3++;
                }
            }
            bluetoothActivityEnergyInfo.setUidTraffic(uidTrafficArr);
        }
        return bluetoothActivityEnergyInfo;
    }

    public int getTotalNumOfTrackableAdvertisements() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH", "Need BLUETOOTH permission");
        return this.mAdapterProperties.getTotalNumOfTrackableAdvertisements();
    }

    void onLeServiceUp() {
        this.mAdapterStateMachine.sendMessage(1);
    }

    void onBrEdrDown() {
        this.mAdapterStateMachine.sendMessage(4);
    }

    private static int convertScanModeToHal(int i) {
        switch (i) {
            case 20:
                return 0;
            case 21:
                return 1;
            case 22:
            default:
                return -1;
            case 23:
                return 2;
        }
    }

    static int convertScanModeFromHal(int i) {
        switch (i) {
            case 0:
                return 20;
            case 1:
                return 21;
            case 2:
                return 23;
            default:
                return -1;
        }
    }

    private boolean setWakeAlarm(long j, boolean z) {
        synchronized (this) {
            if (this.mPendingAlarm != null) {
                this.mAlarmManager.cancel(this.mPendingAlarm);
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
            int i = z ? 2 : 3;
            this.mPendingAlarm = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_ALARM_WAKEUP), 1073741824);
            this.mAlarmManager.setExact(i, jElapsedRealtime, this.mPendingAlarm);
        }
        return true;
    }

    private boolean acquireWakeLock(String str) {
        synchronized (this) {
            if (this.mWakeLock == null) {
                this.mWakeLockName = str;
                this.mWakeLock = this.mPowerManager.newWakeLock(1, str);
            }
            if (!this.mWakeLock.isHeld()) {
                this.mWakeLock.acquire();
            }
        }
        return true;
    }

    private boolean releaseWakeLock(String str) {
        synchronized (this) {
            if (this.mWakeLock == null) {
                errorLog("Repeated wake lock release; aborting release: " + str);
                return false;
            }
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
            return true;
        }
    }

    private void energyInfoCallback(int i, int i2, long j, long j2, long j3, long j4, UidTraffic[] uidTrafficArr) throws RemoteException {
        long j5;
        long jAddExact;
        long j6;
        long j7;
        long jAddExact2;
        long j8;
        long j9;
        if (i2 < 0 || i2 > 3) {
            j5 = j4;
        } else if (j4 == 0) {
            try {
                long jMultiplyExact = Math.multiplyExact(j, getTxCurrentMa());
                long jMultiplyExact2 = Math.multiplyExact(j2, getRxCurrentMa());
                jAddExact = (long) (Math.addExact(Math.addExact(jMultiplyExact, jMultiplyExact2), Math.multiplyExact(j3, getIdleCurrentMa())) * getOperatingVolt());
            } catch (ArithmeticException e) {
                Slog.wtf(TAG, "overflow in bluetooth energy callback", e);
                jAddExact = j4;
            }
            synchronized (this.mEnergyInfoLock) {
                this.mStackReportedState = i2;
                try {
                    long jAddExact3 = Math.addExact(this.mTxTimeTotalMs, j);
                    long jAddExact4 = Math.addExact(this.mRxTimeTotalMs, j2);
                    long jAddExact5 = Math.addExact(this.mIdleTimeTotalMs, j3);
                    j7 = jAddExact;
                    jAddExact2 = Math.addExact(this.mEnergyUsedTotalVoltAmpSecMicro, jAddExact);
                    j6 = jAddExact3;
                    j8 = jAddExact4;
                    j9 = jAddExact5;
                } catch (ArithmeticException e2) {
                    Slog.wtf(TAG, "overflow in bluetooth energy callback", e2);
                    j6 = this.mTxTimeTotalMs;
                    j7 = jAddExact;
                    long j10 = this.mRxTimeTotalMs;
                    long j11 = this.mIdleTimeTotalMs;
                    jAddExact2 = this.mEnergyUsedTotalVoltAmpSecMicro;
                    j8 = j10;
                    j9 = j11;
                }
                this.mTxTimeTotalMs = j6;
                this.mRxTimeTotalMs = j8;
                this.mIdleTimeTotalMs = j9;
                this.mEnergyUsedTotalVoltAmpSecMicro = jAddExact2;
                for (UidTraffic uidTraffic : uidTrafficArr) {
                    UidTraffic uidTraffic2 = this.mUidTraffic.get(uidTraffic.getUid());
                    if (uidTraffic2 == null) {
                        this.mUidTraffic.put(uidTraffic.getUid(), uidTraffic);
                    } else {
                        uidTraffic2.addRxBytes(uidTraffic.getRxBytes());
                        uidTraffic2.addTxBytes(uidTraffic.getTxBytes());
                    }
                }
                this.mEnergyInfoLock.notifyAll();
            }
            j5 = j7;
        } else {
            jAddExact = j4;
            synchronized (this.mEnergyInfoLock) {
            }
        }
        verboseLog("energyInfoCallback() status = " + i + "txTime = " + j + "rxTime = " + j2 + "idleTime = " + j3 + "energyUsed = " + j5 + "ctrlState = " + i2 + "traffic = " + Arrays.toString(uidTrafficArr));
    }

    private int getIdleCurrentMa() {
        return getResources().getInteger(android.R.integer.config_activeTaskDurationHours);
    }

    private int getTxCurrentMa() {
        return getResources().getInteger(android.R.integer.config_am_tieredCachedAdjUiTierSize);
    }

    private int getRxCurrentMa() {
        return getResources().getInteger(android.R.integer.config_allowedUnprivilegedKeepalivePerUid);
    }

    private double getOperatingVolt() {
        return ((double) getResources().getInteger(android.R.integer.config_alertDialogController)) / 1000.0d;
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        if (strArr.length == 0) {
            printWriter.println("Skipping dump in APP SERVICES, see bluetooth_manager section.");
            printWriter.println("Use --print argument for dumpsys direct from AdapterService.");
            return;
        }
        verboseLog("dumpsys arguments, check for protobuf output: " + TextUtils.join(" ", strArr));
        if (strArr[0].equals("--proto-bin")) {
            dumpMetrics(fileDescriptor);
            return;
        }
        printWriter.println();
        this.mAdapterProperties.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("mSnoopLogSettingAtEnable = " + this.mSnoopLogSettingAtEnable);
        printWriter.println();
        this.mAdapterStateMachine.dump(fileDescriptor, printWriter, strArr);
        StringBuilder sb = new StringBuilder();
        Iterator<ProfileService> it = this.mRegisteredProfiles.iterator();
        while (it.hasNext()) {
            it.next().dump(sb);
        }
        printWriter.write(sb.toString());
        printWriter.flush();
        dumpNative(fileDescriptor, strArr);
    }

    private void dumpMetrics(FileDescriptor fileDescriptor) {
        BluetoothMetricsProto.BluetoothLog.Builder builderNewBuilder = BluetoothMetricsProto.BluetoothLog.newBuilder();
        byte[] bArrDumpMetricsNative = dumpMetricsNative();
        debugLog("dumpMetrics: native metrics size is " + bArrDumpMetricsNative.length);
        if (bArrDumpMetricsNative.length > 0) {
            try {
                builderNewBuilder.mergeFrom(bArrDumpMetricsNative);
            } catch (InvalidProtocolBufferException e) {
                Log.w(TAG, "dumpMetrics: problem parsing metrics protobuf, " + e.getMessage());
                return;
            }
        }
        builderNewBuilder.setNumBondedDevices(getBondedDevices().length);
        MetricsLogger.dumpProto(builderNewBuilder);
        Iterator<ProfileService> it = this.mRegisteredProfiles.iterator();
        while (it.hasNext()) {
            it.next().dumpProto(builderNewBuilder);
        }
        byte[] bArrEncode = Base64.encode(builderNewBuilder.build().toByteArray(), 0);
        debugLog("dumpMetrics: combined metrics size is " + bArrEncode.length);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileDescriptor);
            try {
                fileOutputStream.write(bArrEncode);
                fileOutputStream.close();
            } finally {
            }
        } catch (IOException e2) {
            errorLog("dumpMetrics: error writing combined protobuf to fd, " + e2.getMessage());
        }
    }

    private void debugLog(String str) {
        if (DBG) {
            Log.d(TAG, str);
        }
    }

    private void verboseLog(String str) {
        if (VERBOSE) {
            Log.v(TAG, str);
        }
    }

    private void errorLog(String str) {
        Log.e(TAG, str);
    }

    private void enableNativeWithGuestFlag() {
        if (!enableNative(UserManager.get(this).isGuestUser())) {
            Log.e(TAG, "enableNative() returned false");
        }
    }

    public boolean isMock() {
        return false;
    }
}
