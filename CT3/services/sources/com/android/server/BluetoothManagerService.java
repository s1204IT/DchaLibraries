package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothCallback;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothHeadset;
import android.bluetooth.IBluetoothManager;
import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothProfileServiceConnection;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.audio.AudioService;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.pm.PackageManagerService;
import com.mediatek.Manifest;
import com.mediatek.cta.CtaUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class BluetoothManagerService extends IBluetoothManager.Stub {
    private static final String ACTION_BOOT_IPO = "android.intent.action.ACTION_BOOT_IPO";
    private static final String ACTION_PACKAGE_DATA_CLEARED = "android.intent.action.PACKAGE_DATA_CLEARED";
    private static final String ACTION_SERVICE_STATE_CHANGED = "com.android.bluetooth.btservice.action.STATE_CHANGED";
    private static final int ADD_PROXY_DELAY_MS = 100;
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final int BLUETOOTH_OFF = 0;
    private static final int BLUETOOTH_ON_AIRPLANE = 2;
    private static final int BLUETOOTH_ON_BLUETOOTH = 1;
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    private static final boolean DBG = true;
    private static final int ERROR_RESTART_TIME_MS = 3000;
    private static final String EXTRA_ACTION = "action";
    private static final int MAX_ERROR_RESTART_RETRIES = 6;
    private static final int MAX_SAVE_RETRIES = 3;
    private static final int MESSAGE_ADD_PROXY_DELAYED = 400;
    private static final int MESSAGE_BIND_PROFILE_SERVICE = 401;
    private static final int MESSAGE_BLUETOOTH_SERVICE_CONNECTED = 40;
    private static final int MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED = 41;
    private static final int MESSAGE_BLUETOOTH_STATE_CHANGE = 60;
    private static final int MESSAGE_DISABLE = 2;
    private static final int MESSAGE_ENABLE = 1;
    private static final int MESSAGE_GET_NAME_AND_ADDRESS = 200;
    private static final int MESSAGE_REGISTER_ADAPTER = 20;
    private static final int MESSAGE_REGISTER_STATE_CHANGE_CALLBACK = 30;
    private static final int MESSAGE_RESTART_BLUETOOTH_SERVICE = 42;
    private static final int MESSAGE_SAVE_NAME_AND_ADDRESS = 201;
    private static final int MESSAGE_TIMEOUT_BIND = 100;
    private static final int MESSAGE_TIMEOUT_UNBIND = 101;
    private static final int MESSAGE_UNREGISTER_ADAPTER = 21;
    private static final int MESSAGE_UNREGISTER_STATE_CHANGE_CALLBACK = 31;
    private static final int MESSAGE_USER_SWITCHED = 300;
    private static final int MESSAGE_USER_UNLOCKED = 301;
    private static final int MESSAGE_WHOLE_CHIP_RESET = 5010;
    private static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";
    private static final String SECURE_SETTINGS_BLUETOOTH_ADDR_VALID = "bluetooth_addr_valid";
    private static final String SECURE_SETTINGS_BLUETOOTH_NAME = "bluetooth_name";
    private static final int SERVICE_IBLUETOOTH = 1;
    private static final int SERVICE_IBLUETOOTHGATT = 2;
    private static final int SERVICE_RESTART_TIME_MS = 200;
    private static final String TAG = "BluetoothManagerService";
    private static final int TIMEOUT_BIND_MS = 3000;
    private static final int TIMEOUT_SAVE_MS = 500;
    private static final int USER_SWITCHED_TIME_MS = 200;
    private static int mBleAppCount = 0;
    private final RemoteCallbackList<IBluetoothManagerCallback> mCallbacks;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private boolean mEnableExternal;
    private final RemoteCallbackList<IBluetoothStateChangeCallback> mStateChangeCallbacks;
    private final int mSystemUiUid;
    private final ReentrantReadWriteLock mBluetoothLock = new ReentrantReadWriteLock();
    private boolean mQuietEnable = false;
    private final Map<Integer, ProfileServiceConnections> mProfileServices = new HashMap();
    private final IBluetoothCallback mBluetoothCallback = new IBluetoothCallback.Stub() {
        public void onBluetoothStateChange(int prevState, int newState) throws RemoteException {
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(60, prevState, newState);
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }

        public void onWholeChipReset() throws RemoteException {
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_WHOLE_CHIP_RESET);
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BroadcastReceiver broadcastReceiver;
            String action = intent.getAction();
            if ("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED".equals(action)) {
                String newName = intent.getStringExtra("android.bluetooth.adapter.extra.LOCAL_NAME");
                Slog.d(BluetoothManagerService.TAG, "Bluetooth Adapter name changed to " + newName);
                if (newName == null) {
                    return;
                }
                BluetoothManagerService.this.storeNameAndAddress(newName, null);
                return;
            }
            if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                boolean airplaneMode = intent.getBooleanExtra(AudioService.CONNECT_INTENT_KEY_STATE, false);
                Slog.d(BluetoothManagerService.TAG, "Receive airplane mode change: airplaneMode = " + airplaneMode);
                broadcastReceiver = BluetoothManagerService.this.mReceiver;
                synchronized (broadcastReceiver) {
                    if (BluetoothManagerService.this.isBluetoothPersistedStateOn()) {
                        if (airplaneMode) {
                            BluetoothManagerService.this.persistBluetoothSetting(2);
                        } else {
                            BluetoothManagerService.this.persistBluetoothSetting(1);
                        }
                    }
                    int st = 10;
                    try {
                        try {
                            BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                st = BluetoothManagerService.this.mBluetooth.getState();
                            }
                        } catch (RemoteException e) {
                            Slog.e(BluetoothManagerService.TAG, "Unable to call getState", e);
                            BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                        }
                        Slog.d(BluetoothManagerService.TAG, AudioService.CONNECT_INTENT_KEY_STATE + st);
                        if (airplaneMode) {
                            synchronized (this) {
                                int unused = BluetoothManagerService.mBleAppCount = 0;
                                BluetoothManagerService.this.mBleApps.clear();
                            }
                            if (st == 15 || st == 14 || st == 10) {
                                try {
                                    try {
                                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                                        if (BluetoothManagerService.this.mBluetooth != null) {
                                            BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                                        }
                                    } catch (RemoteException e2) {
                                        Slog.e(BluetoothManagerService.TAG, "Unable to call onBrEdrDown", e2);
                                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                                    }
                                } finally {
                                }
                            } else if (st == 12 || st == 11) {
                                Slog.d(BluetoothManagerService.TAG, "Calling disable");
                                BluetoothManagerService.this.sendDisableMsg();
                            }
                        } else if (BluetoothManagerService.this.mEnableExternal && st != 12) {
                            Slog.d(BluetoothManagerService.TAG, "Calling enable");
                            BluetoothManagerService.this.sendEnableMsg(BluetoothManagerService.this.mQuietEnableExternal);
                        }
                    } finally {
                    }
                }
            } else {
                if (!"android.intent.action.ACTION_BOOT_IPO".equals(action)) {
                    return;
                }
                Slog.d(BluetoothManagerService.TAG, "Bluetooth boot completed");
                broadcastReceiver = BluetoothManagerService.this.mReceiver;
                synchronized (broadcastReceiver) {
                    boolean bluetoothStateBluetooth = BluetoothManagerService.this.isBluetoothPersistedStateOnBluetooth();
                    Slog.d(BluetoothManagerService.TAG, "Recevie action: " + action + ", mEnableExternal = " + BluetoothManagerService.this.mEnableExternal + ", bluetoothStateBluetooth = " + bluetoothStateBluetooth);
                    if ("android.intent.action.ACTION_BOOT_IPO".equals(action) && BluetoothManagerService.this.isBluetoothPersistedStateOn()) {
                        BluetoothManagerService.this.mEnableExternal = true;
                        Slog.d(BluetoothManagerService.TAG, "isBluetoothPersistedStateOn() = true, mEnableExternal = " + BluetoothManagerService.this.mEnableExternal);
                    }
                    if (BluetoothManagerService.this.mEnableExternal && bluetoothStateBluetooth) {
                        Slog.d(BluetoothManagerService.TAG, "Auto-enabling Bluetooth.");
                        BluetoothManagerService.this.sendEnableMsg(BluetoothManagerService.this.mQuietEnableExternal);
                    }
                    if (!BluetoothManagerService.this.isNameAndAddressSet()) {
                        Slog.d(BluetoothManagerService.TAG, "Retrieving Bluetooth Adapter name and address...");
                        Message getMsg = BluetoothManagerService.this.mHandler.obtainMessage(200);
                        BluetoothManagerService.this.mHandler.sendMessage(getMsg);
                    }
                }
            }
        }
    };
    private final BroadcastReceiver mReceiverDataCleared = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!BluetoothManagerService.ACTION_PACKAGE_DATA_CLEARED.equals(action)) {
                return;
            }
            Slog.d(BluetoothManagerService.TAG, "Bluetooth package data cleared");
            try {
                BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                Slog.d(BluetoothManagerService.TAG, "handleEnable: mBluetooth = " + BluetoothManagerService.this.mBluetooth + ", mBinding = " + BluetoothManagerService.this.mBinding);
                if (BluetoothManagerService.this.mBluetooth == null && BluetoothManagerService.this.mEnable) {
                    Slog.d(BluetoothManagerService.TAG, "Bind AdapterService");
                    Message timeoutMsg = BluetoothManagerService.this.mHandler.obtainMessage(100);
                    BluetoothManagerService.this.mHandler.sendMessageDelayed(timeoutMsg, 3000L);
                    Intent i = new Intent(IBluetooth.class.getName());
                    if (!BluetoothManagerService.this.doBind(i, BluetoothManagerService.this.mConnection, 65, UserHandle.CURRENT)) {
                        BluetoothManagerService.this.mHandler.removeMessages(100);
                        Slog.e(BluetoothManagerService.TAG, "Fail to bind to: " + IBluetooth.class.getName());
                    } else {
                        BluetoothManagerService.this.mBinding = true;
                    }
                }
            } finally {
                BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
            }
        }
    };
    Map<IBinder, ClientDeathRecipient> mBleApps = new HashMap();
    private BluetoothServiceConnection mConnection = new BluetoothServiceConnection(this, null);
    private final BluetoothHandler mHandler = new BluetoothHandler(IoThread.get().getLooper());
    private IBluetooth mBluetooth = null;
    private IBinder mBluetoothBinder = null;
    private IBluetoothGatt mBluetoothGatt = null;
    private boolean mBinding = false;
    private boolean mUnbinding = false;
    private boolean mEnable = false;
    private int mState = 10;
    private boolean mQuietEnableExternal = false;
    private String mAddress = null;
    private String mName = null;
    private int mErrorRecoveryRetryCounter = 0;

    private void registerForAirplaneMode(IntentFilter filter) {
        ContentResolver resolver = this.mContext.getContentResolver();
        String airplaneModeRadios = Settings.Global.getString(resolver, "airplane_mode_radios");
        Settings.Global.getString(resolver, "airplane_mode_toggleable_radios");
        boolean mIsAirplaneSensitive = airplaneModeRadios == null ? true : airplaneModeRadios.contains("bluetooth");
        if (!mIsAirplaneSensitive) {
            return;
        }
        filter.addAction("android.intent.action.AIRPLANE_MODE");
    }

    BluetoothManagerService(Context context) {
        this.mContext = context;
        this.mEnableExternal = false;
        this.mContentResolver = context.getContentResolver();
        registerForBleScanModeChange();
        this.mCallbacks = new RemoteCallbackList<>();
        this.mStateChangeCallbacks = new RemoteCallbackList<>();
        IntentFilter filterDataCleared = new IntentFilter(ACTION_PACKAGE_DATA_CLEARED);
        filterDataCleared.addDataScheme("package");
        this.mContext.registerReceiver(this.mReceiverDataCleared, filterDataCleared);
        IntentFilter filter_IPO = new IntentFilter("android.intent.action.ACTION_BOOT_IPO");
        filter_IPO.setPriority(JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
        this.mContext.registerReceiver(this.mReceiver, filter_IPO);
        IntentFilter filter = new IntentFilter("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
        registerForAirplaneMode(filter);
        filter.setPriority(1000);
        this.mContext.registerReceiver(this.mReceiver, filter);
        loadStoredNameAndAddress();
        if (isBluetoothPersistedStateOn()) {
            this.mEnableExternal = true;
            Slog.d(TAG, "isBluetoothPersistedStateOn() = true, mEnableExternal = " + this.mEnableExternal);
        }
        int sysUiUid = -1;
        try {
            sysUiUid = this.mContext.getPackageManager().getPackageUidAsUser("com.android.systemui", PackageManagerService.DumpState.DUMP_DEXOPT, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Unable to resolve SystemUI's UID.", e);
        }
        this.mSystemUiUid = sysUiUid;
    }

    private final boolean isAirplaneModeOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private final boolean isBluetoothPersistedStateOn() {
        int bluetoothState = Settings.Global.getInt(this.mContentResolver, "bluetooth_on", 0);
        Slog.d(TAG, "isBluetoothPersistedStateOn, bluetoothState = " + bluetoothState);
        return bluetoothState != 0;
    }

    private final boolean isBluetoothPersistedStateOnBluetooth() {
        return Settings.Global.getInt(this.mContentResolver, "bluetooth_on", 0) == 1;
    }

    private void persistBluetoothSetting(int value) {
        Slog.d(TAG, "persistBluetoothSetting: value = " + value);
        Settings.Global.putInt(this.mContext.getContentResolver(), "bluetooth_on", value);
    }

    private boolean isNameAndAddressSet() {
        return this.mName != null && this.mAddress != null && this.mName.length() > 0 && this.mAddress.length() > 0;
    }

    private void loadStoredNameAndAddress() {
        Slog.d(TAG, "Loading stored name and address");
        if (this.mContext.getResources().getBoolean(R.^attr-private.floatingToolbarOpenDrawable) && Settings.Secure.getInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 0) == 0) {
            Slog.d(TAG, "invalid bluetooth name and address stored");
            return;
        }
        this.mName = Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME);
        this.mAddress = Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS);
        Slog.d(TAG, "Stored bluetooth Name=" + this.mName + ",Address=" + this.mAddress);
    }

    private void storeNameAndAddress(String name, String address) {
        if (name != null) {
            Settings.Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME, name);
            this.mName = name;
            Slog.d(TAG, "Stored Bluetooth name: " + Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME));
        }
        if (address != null) {
            Settings.Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS, address);
            this.mAddress = address;
            Slog.d(TAG, "Stored Bluetoothaddress: " + Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS));
        }
        if (name == null || address == null) {
            return;
        }
        Settings.Secure.putInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 1);
    }

    public IBluetooth registerAdapter(IBluetoothManagerCallback callback) {
        if (callback == null) {
            Slog.w(TAG, "Callback is null in registerAdapter");
            return null;
        }
        Message msg = this.mHandler.obtainMessage(20);
        msg.obj = callback;
        this.mHandler.sendMessageAtFrontOfQueue(msg);
        try {
            this.mBluetoothLock.writeLock().lock();
            return this.mBluetooth;
        } finally {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    public void unregisterAdapter(IBluetoothManagerCallback callback) {
        if (callback == null) {
            Slog.w(TAG, "Callback is null in unregisterAdapter");
            return;
        }
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Message msg = this.mHandler.obtainMessage(21);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
    }

    public void registerStateChangeCallback(IBluetoothStateChangeCallback callback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Slog.d(TAG, "registerStateChangeCallback: callback = " + callback);
        Message msg = this.mHandler.obtainMessage(30);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
    }

    public void unregisterStateChangeCallback(IBluetoothStateChangeCallback callback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Slog.d(TAG, "unregisterStateChangeCallback: callback = " + callback);
        if (callback == null) {
            Slog.e(TAG, "Abnormal case happens, callback is NULL");
            return;
        }
        Message msg = this.mHandler.obtainMessage(31);
        msg.obj = callback;
        this.mHandler.sendMessageAtFrontOfQueue(msg);
    }

    public boolean isEnabled() {
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Slog.w(TAG, "isEnabled(): not allowed for non-active and non system user");
            return false;
        }
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                return this.mBluetooth.isEnabled();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "isEnabled()", e);
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
        return false;
    }

    class ClientDeathRecipient implements IBinder.DeathRecipient {
        ClientDeathRecipient() {
        }

        @Override
        public void binderDied() {
            Slog.d(BluetoothManagerService.TAG, "Binder is dead -  unregister Ble App");
            if (BluetoothManagerService.mBleAppCount > 0) {
                BluetoothManagerService.mBleAppCount--;
            }
            if (BluetoothManagerService.mBleAppCount != 0) {
                return;
            }
            Slog.d(BluetoothManagerService.TAG, "Disabling LE only mode after application crash");
            try {
                BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                if (BluetoothManagerService.this.mBluetooth != null) {
                    BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                }
            } catch (RemoteException e) {
                Slog.e(BluetoothManagerService.TAG, "Unable to call onBrEdrDown", e);
            } finally {
                BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
            }
        }
    }

    public boolean isBleScanAlwaysAvailable() {
        try {
            return Settings.Global.getInt(this.mContentResolver, "ble_scan_always_enabled") != 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    private void registerForBleScanModeChange() {
        ContentObserver contentObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                if (BluetoothManagerService.this.isBleScanAlwaysAvailable()) {
                    return;
                }
                BluetoothManagerService.this.disableBleScanMode();
                BluetoothManagerService.this.clearBleApps();
                try {
                    BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                    if (BluetoothManagerService.this.mBluetooth != null) {
                        BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                    }
                } catch (RemoteException e) {
                    Slog.e(BluetoothManagerService.TAG, "error when disabling bluetooth", e);
                } finally {
                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                }
            }
        };
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("ble_scan_always_enabled"), false, contentObserver);
    }

    private void disableBleScanMode() {
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mBluetooth != null && this.mBluetooth.getState() != 12) {
                Slog.d(TAG, "Reseting the mEnable flag for clean disable");
                this.mEnable = false;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getState()", e);
        } finally {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    public int updateBleAppCount(IBinder token, boolean enable) {
        if (enable) {
            if (this.mBleApps.get(token) == null) {
                ClientDeathRecipient deathRec = new ClientDeathRecipient();
                try {
                    token.linkToDeath(deathRec, 0);
                    this.mBleApps.put(token, deathRec);
                    synchronized (this) {
                        mBleAppCount++;
                    }
                    Slog.d(TAG, "Registered for death Notification");
                } catch (RemoteException e) {
                    throw new IllegalArgumentException("Wake lock is already dead.");
                }
            }
        } else {
            ClientDeathRecipient r = this.mBleApps.get(token);
            if (r != null) {
                token.unlinkToDeath(r, 0);
                this.mBleApps.remove(token);
                synchronized (this) {
                    if (mBleAppCount > 0) {
                        mBleAppCount--;
                    }
                }
                Slog.d(TAG, "Unregistered for death Notification");
            }
        }
        Slog.d(TAG, "Updated BleAppCount" + mBleAppCount);
        if (mBleAppCount == 0 && this.mEnable) {
            disableBleScanMode();
        }
        return mBleAppCount;
    }

    private void clearBleApps() {
        synchronized (this) {
            this.mBleApps.clear();
            mBleAppCount = 0;
        }
    }

    public boolean isBleAppPresent() {
        Slog.d(TAG, "isBleAppPresent() count: " + mBleAppCount);
        return mBleAppCount > 0;
    }

    private void onBluetoothGattServiceUp() {
        Slog.d(TAG, "BluetoothGatt Service is Up");
        try {
            this.mBluetoothLock.readLock().lock();
            if (!isBleAppPresent() && this.mBluetooth != null && this.mBluetooth.getState() == 15) {
                this.mBluetooth.onLeServiceUp();
                long callingIdentity = Binder.clearCallingIdentity();
                persistBluetoothSetting(1);
                Binder.restoreCallingIdentity(callingIdentity);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call onServiceUp", e);
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    private void sendBrEdrDownCallback() {
        Slog.d(TAG, "Calling sendBrEdrDownCallback callbacks");
        if (this.mBluetooth == null) {
            Slog.w(TAG, "Bluetooth handle is null");
            return;
        }
        if (!isBleAppPresent()) {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    this.mBluetooth.onBrEdrDown();
                }
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, "Call to onBrEdrDown() failed.", e);
                return;
            } finally {
                this.mBluetoothLock.readLock().unlock();
            }
        }
        try {
            this.mBluetoothGatt.unregAll();
        } catch (RemoteException e2) {
            Slog.e(TAG, "Unable to disconnect all apps.", e2);
        }
    }

    public boolean enableNoAutoConnect() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        Slog.d(TAG, "enableNoAutoConnect():  mBluetooth =" + this.mBluetooth + " mBinding = " + this.mBinding);
        int callingAppId = UserHandle.getAppId(Binder.getCallingUid());
        if (callingAppId != 1027) {
            throw new SecurityException("no permission to enable Bluetooth quietly");
        }
        synchronized (this.mReceiver) {
            this.mQuietEnableExternal = true;
            this.mEnableExternal = true;
            sendEnableMsg(true);
        }
        return true;
    }

    public boolean enable() {
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Slog.w(TAG, "enable(): not allowed for non-active and non system user");
            return false;
        }
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        CtaUtils.enforceCheckPermission(Manifest.permission.CTA_ENABLE_BT, "Enable bluetooth");
        Slog.d(TAG, "enable():  mBluetooth =" + this.mBluetooth + " mBinding = " + this.mBinding + " mState = " + this.mState);
        synchronized (this.mReceiver) {
            this.mQuietEnableExternal = false;
            this.mEnableExternal = true;
            sendEnableMsg(false);
        }
        Slog.d(TAG, "enable returning");
        return true;
    }

    public boolean disable(boolean persist) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permissicacheNameAndAddresson");
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Slog.w(TAG, "disable(): not allowed for non-active and non system user");
            return false;
        }
        Slog.d(TAG, "disable(): mBluetooth = " + this.mBluetooth + " mBinding = " + this.mBinding);
        synchronized (this.mReceiver) {
            if (persist) {
                long callingIdentity = Binder.clearCallingIdentity();
                persistBluetoothSetting(0);
                Binder.restoreCallingIdentity(callingIdentity);
                this.mEnableExternal = false;
                sendDisableMsg();
            } else {
                this.mEnableExternal = false;
                sendDisableMsg();
            }
        }
        return true;
    }

    public void unbindAndFinish() {
        Slog.d(TAG, "unbindAndFinish(): " + this.mBluetooth + " mBinding = " + this.mBinding);
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mUnbinding) {
                return;
            }
            this.mUnbinding = true;
            this.mHandler.removeMessages(60);
            if (this.mBluetooth != null) {
                try {
                    this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
                } catch (RemoteException re) {
                    Slog.e(TAG, "Unable to unregister BluetoothCallback", re);
                }
                Slog.d(TAG, "Sending unbind request.");
                this.mBluetoothBinder = null;
                this.mBluetooth = null;
                this.mContext.unbindService(this.mConnection);
                this.mUnbinding = false;
                this.mBinding = false;
            } else {
                this.mUnbinding = false;
            }
            this.mBluetoothGatt = null;
        } finally {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    public IBluetoothGatt getBluetoothGatt() {
        return this.mBluetoothGatt;
    }

    public boolean bindBluetoothProfileService(int bluetoothProfile, IBluetoothProfileServiceConnection proxy) {
        if (!this.mEnable) {
            Slog.d(TAG, "Trying to bind to profile: " + bluetoothProfile + ", while Bluetooth was disabled");
            return false;
        }
        synchronized (this.mProfileServices) {
            if (this.mProfileServices.get(new Integer(bluetoothProfile)) == null) {
                Slog.d(TAG, "Creating new ProfileServiceConnections object for profile: " + bluetoothProfile);
                if (bluetoothProfile != 1) {
                    return false;
                }
                Intent intent = new Intent(IBluetoothHeadset.class.getName());
                ProfileServiceConnections psc = new ProfileServiceConnections(intent);
                if (!psc.bindService()) {
                    return false;
                }
                this.mProfileServices.put(new Integer(bluetoothProfile), psc);
            }
            Message addProxyMsg = this.mHandler.obtainMessage(MESSAGE_ADD_PROXY_DELAYED);
            addProxyMsg.arg1 = bluetoothProfile;
            addProxyMsg.obj = proxy;
            this.mHandler.sendMessageDelayed(addProxyMsg, 100L);
            return true;
        }
    }

    public void unbindBluetoothProfileService(int bluetoothProfile, IBluetoothProfileServiceConnection proxy) {
        synchronized (this.mProfileServices) {
            ProfileServiceConnections psc = this.mProfileServices.get(new Integer(bluetoothProfile));
            if (psc == null) {
                return;
            }
            psc.removeProxy(proxy);
        }
    }

    private void unbindAllBluetoothProfileServices() {
        synchronized (this.mProfileServices) {
            for (Integer i : this.mProfileServices.keySet()) {
                ProfileServiceConnections psc = this.mProfileServices.get(i);
                try {
                    this.mContext.unbindService(psc);
                } catch (IllegalArgumentException e) {
                    Slog.e(TAG, "Unable to unbind service with intent: " + psc.mIntent, e);
                }
                psc.removeAllProxies();
            }
            this.mProfileServices.clear();
        }
    }

    public void handleOnBootPhase() {
        Slog.d(TAG, "Bluetooth boot completed");
        if (this.mEnableExternal && isBluetoothPersistedStateOnBluetooth()) {
            Slog.d(TAG, "Auto-enabling Bluetooth.");
            sendEnableMsg(this.mQuietEnableExternal);
        } else {
            if (isNameAndAddressSet()) {
                return;
            }
            Slog.d(TAG, "Getting adapter name and address");
            Message getMsg = this.mHandler.obtainMessage(200);
            this.mHandler.sendMessage(getMsg);
        }
    }

    public void handleOnSwitchUser(int userHandle) {
        Slog.d(TAG, "User " + userHandle + " switched");
        this.mHandler.obtainMessage(MESSAGE_USER_SWITCHED, userHandle, 0).sendToTarget();
    }

    public void handleOnUnlockUser(int userHandle) {
        Slog.d(TAG, "User " + userHandle + " unlocked");
        this.mHandler.obtainMessage(301, userHandle, 0).sendToTarget();
    }

    private final class ProfileServiceConnections implements ServiceConnection, IBinder.DeathRecipient {
        Intent mIntent;
        final RemoteCallbackList<IBluetoothProfileServiceConnection> mProxies = new RemoteCallbackList<>();
        boolean mInvokingProxyCallbacks = false;
        IBinder mService = null;
        ComponentName mClassName = null;

        ProfileServiceConnections(Intent intent) {
            this.mIntent = intent;
        }

        private boolean bindService() {
            if (this.mIntent != null && this.mService == null && BluetoothManagerService.this.doBind(this.mIntent, this, 0, UserHandle.CURRENT_OR_SELF)) {
                Message msg = BluetoothManagerService.this.mHandler.obtainMessage(401);
                msg.obj = this;
                BluetoothManagerService.this.mHandler.sendMessageDelayed(msg, 3000L);
                return true;
            }
            Slog.w(BluetoothManagerService.TAG, "Unable to bind with intent: " + this.mIntent);
            return false;
        }

        private void addProxy(IBluetoothProfileServiceConnection proxy) {
            this.mProxies.register(proxy);
            if (this.mService != null) {
                try {
                    proxy.onServiceConnected(this.mClassName, this.mService);
                    return;
                } catch (RemoteException e) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e);
                    return;
                }
            }
            if (BluetoothManagerService.this.mHandler.hasMessages(401, this)) {
                return;
            }
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(401);
            msg.obj = this;
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }

        private void removeProxy(IBluetoothProfileServiceConnection proxy) {
            if (proxy != null) {
                if (!this.mProxies.unregister(proxy)) {
                    return;
                }
                try {
                    proxy.onServiceDisconnected(this.mClassName);
                    return;
                } catch (RemoteException e) {
                    Slog.e(BluetoothManagerService.TAG, "Unable to disconnect proxy", e);
                    return;
                }
            }
            Slog.w(BluetoothManagerService.TAG, "Trying to remove a null proxy");
        }

        private void removeAllProxies() {
            onServiceDisconnected(this.mClassName);
            this.mProxies.kill();
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothManagerService.this.mHandler.removeMessages(401, this);
            this.mClassName = className;
            try {
                synchronized (this.mClassName) {
                    try {
                        this.mService = service;
                        this.mService.linkToDeath(this, 0);
                    } catch (RemoteException e) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to linkToDeath", e);
                    }
                }
                if (this.mInvokingProxyCallbacks) {
                    Slog.e(BluetoothManagerService.TAG, "Proxy callbacks already in progress.");
                    return;
                }
                this.mInvokingProxyCallbacks = true;
                synchronized (this.mProxies) {
                    int n = this.mProxies.beginBroadcast();
                    for (int i = 0; i < n; i++) {
                        try {
                            try {
                                this.mProxies.getBroadcastItem(i).onServiceConnected(className, service);
                            } catch (RemoteException e2) {
                                Slog.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e2);
                            }
                        } finally {
                            this.mProxies.finishBroadcast();
                            this.mInvokingProxyCallbacks = false;
                        }
                    }
                }
            } catch (NullPointerException npe) {
                Slog.e(BluetoothManagerService.TAG, "NullPointerException for synchronized(mClassName)", npe);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (this.mService == null || this.mClassName == null) {
                return;
            }
            try {
                synchronized (this.mClassName) {
                    this.mService.unlinkToDeath(this, 0);
                    this.mService = null;
                    this.mClassName = null;
                }
                if (this.mInvokingProxyCallbacks) {
                    Slog.e(BluetoothManagerService.TAG, "Proxy callbacks already in progress.");
                    return;
                }
                this.mInvokingProxyCallbacks = true;
                synchronized (this.mProxies) {
                    int n = this.mProxies.beginBroadcast();
                    for (int i = 0; i < n; i++) {
                        try {
                            try {
                                this.mProxies.getBroadcastItem(i).onServiceDisconnected(className);
                            } catch (RemoteException e) {
                                Slog.e(BluetoothManagerService.TAG, "Unable to disconnect from proxy", e);
                            }
                        } finally {
                            this.mProxies.finishBroadcast();
                            this.mInvokingProxyCallbacks = false;
                        }
                    }
                }
            } catch (NullPointerException npe) {
                Slog.e(BluetoothManagerService.TAG, "NullPointerException for synchronized(mClassName)", npe);
            }
        }

        @Override
        public void binderDied() {
            Slog.w(BluetoothManagerService.TAG, "Profile service for profile: " + this.mClassName + " died.");
            onServiceDisconnected(this.mClassName);
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(401);
            msg.obj = this;
            BluetoothManagerService.this.mHandler.sendMessageDelayed(msg, 3000L);
        }
    }

    private void sendBluetoothStateCallback(boolean isUp) {
        try {
            int n = this.mStateChangeCallbacks.beginBroadcast();
            Slog.d(TAG, "Broadcasting onBluetoothStateChange(" + isUp + ") to " + n + " receivers.");
            for (int i = 0; i < n; i++) {
                try {
                    this.mStateChangeCallbacks.getBroadcastItem(i).onBluetoothStateChange(isUp);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to call onBluetoothStateChange() on callback #" + i, e);
                }
            }
        } finally {
            this.mStateChangeCallbacks.finishBroadcast();
        }
    }

    private void sendBluetoothServiceUpCallback() {
        Slog.d(TAG, "Calling onBluetoothServiceUp callbacks");
        try {
            int n = this.mCallbacks.beginBroadcast();
            Slog.d(TAG, "Broadcasting onBluetoothServiceUp() to " + n + " receivers.");
            for (int i = 0; i < n; i++) {
                try {
                    this.mCallbacks.getBroadcastItem(i).onBluetoothServiceUp(this.mBluetooth);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to call onBluetoothServiceUp() on callback #" + i, e);
                }
            }
        } finally {
            this.mCallbacks.finishBroadcast();
        }
    }

    private void sendBluetoothServiceDownCallback() {
        Slog.d(TAG, "Calling onBluetoothServiceDown callbacks");
        try {
            int n = this.mCallbacks.beginBroadcast();
            Slog.d(TAG, "Broadcasting onBluetoothServiceDown() to " + n + " receivers.");
            for (int i = 0; i < n; i++) {
                try {
                    this.mCallbacks.getBroadcastItem(i).onBluetoothServiceDown();
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to call onBluetoothServiceDown() on callback #" + i, e);
                }
            }
        } finally {
            this.mCallbacks.finishBroadcast();
        }
    }

    public String getAddress() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Slog.w(TAG, "getAddress(): not allowed for non-active and non system user");
            return null;
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.LOCAL_MAC_ADDRESS") != 0) {
            return "02:00:00:00:00:00";
        }
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                return this.mBluetooth.getAddress();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getAddress(): Unable to retrieve address remotely. Returning cached address", e);
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
        Slog.e(TAG, "getAddress: Return from mAddress = " + this.mAddress);
        return this.mAddress;
    }

    public String getName() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Slog.w(TAG, "getName(): not allowed for non-active and non system user");
            return null;
        }
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                return this.mBluetooth.getName();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getName(): Unable to retrieve name remotely. Returning cached name", e);
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
        Slog.e(TAG, "getAddress: Return from mName = " + this.mName);
        return this.mName;
    }

    private class BluetoothServiceConnection implements ServiceConnection {
        BluetoothServiceConnection(BluetoothManagerService this$0, BluetoothServiceConnection bluetoothServiceConnection) {
            this();
        }

        private BluetoothServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Slog.d(BluetoothManagerService.TAG, "BluetoothServiceConnection: " + className.getClassName());
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(40);
            if (className.getClassName().equals("com.android.bluetooth.btservice.AdapterService")) {
                msg.arg1 = 1;
            } else if (className.getClassName().equals("com.android.bluetooth.gatt.GattService")) {
                msg.arg1 = 2;
            } else {
                Slog.e(BluetoothManagerService.TAG, "Unknown service connected: " + className.getClassName());
                return;
            }
            msg.obj = service;
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Slog.d(BluetoothManagerService.TAG, "BluetoothServiceConnection, disconnected: " + className.getClassName());
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(41);
            if (className.getClassName().equals("com.android.bluetooth.btservice.AdapterService")) {
                msg.arg1 = 1;
            } else if (className.getClassName().equals("com.android.bluetooth.gatt.GattService")) {
                msg.arg1 = 2;
            } else {
                Slog.e(BluetoothManagerService.TAG, "Unknown service disconnected: " + className.getClassName());
                return;
            }
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }
    }

    private class BluetoothHandler extends Handler {
        boolean mGetNameAddressOnly;

        public BluetoothHandler(Looper looper) {
            super(looper);
            this.mGetNameAddressOnly = false;
        }

        @Override
        public void handleMessage(Message msg) {
            BluetoothManagerService bluetoothManagerService;
            ReentrantReadWriteLock reentrantReadWriteLock;
            ReentrantReadWriteLock.WriteLock writeLock;
            Slog.d(BluetoothManagerService.TAG, "Message: " + msg.what);
            switch (msg.what) {
                case 1:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_ENABLE: mBluetooth = " + BluetoothManagerService.this.mBluetooth);
                    BluetoothManagerService.this.mHandler.removeMessages(42);
                    BluetoothManagerService.this.mEnable = true;
                    try {
                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            int state = BluetoothManagerService.this.mBluetooth.getState();
                            if (state == 15) {
                                Slog.w(BluetoothManagerService.TAG, "BT is in BLE_ON State");
                                BluetoothManagerService.this.mBluetooth.onLeServiceUp();
                                return;
                            }
                        }
                    } catch (RemoteException e) {
                        Slog.e(BluetoothManagerService.TAG, "", e);
                    } finally {
                    }
                    BluetoothManagerService.this.mQuietEnable = msg.arg1 == 1;
                    if (BluetoothManagerService.this.mBluetooth == null) {
                        BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                        return;
                    }
                    BluetoothManagerService.this.waitForOnOff(false, true);
                    Message restartMsg = BluetoothManagerService.this.mHandler.obtainMessage(42);
                    BluetoothManagerService.this.mHandler.sendMessageDelayed(restartMsg, 400L);
                    return;
                case 2:
                    BluetoothManagerService.this.mHandler.removeMessages(42);
                    if (!BluetoothManagerService.this.mEnable || BluetoothManagerService.this.mBluetooth == null) {
                        BluetoothManagerService.this.mEnable = false;
                        BluetoothManagerService.this.handleDisable();
                        return;
                    } else {
                        BluetoothManagerService.this.waitForOnOff(true, false);
                        BluetoothManagerService.this.mEnable = false;
                        BluetoothManagerService.this.handleDisable();
                        BluetoothManagerService.this.waitForOnOff(false, false);
                        return;
                    }
                case 20:
                    String str = (IBluetoothManagerCallback) msg.obj;
                    boolean added = BluetoothManagerService.this.mCallbacks.register(str);
                    StringBuilder sbAppend = new StringBuilder().append("Added callback: ");
                    if (str == null) {
                        str = "null";
                    }
                    Slog.d(BluetoothManagerService.TAG, sbAppend.append((Object) str).append(":").append(added).toString());
                    return;
                case 21:
                    String str2 = (IBluetoothManagerCallback) msg.obj;
                    boolean removed = BluetoothManagerService.this.mCallbacks.unregister(str2);
                    StringBuilder sbAppend2 = new StringBuilder().append("Removed callback: ");
                    if (str2 == null) {
                        str2 = "null";
                    }
                    Slog.d(BluetoothManagerService.TAG, sbAppend2.append((Object) str2).append(":").append(removed).toString());
                    return;
                case 30:
                    IBluetoothStateChangeCallback callback = (IBluetoothStateChangeCallback) msg.obj;
                    Slog.d(BluetoothManagerService.TAG, "Register callback = " + callback);
                    if (callback != null) {
                        BluetoothManagerService.this.mStateChangeCallbacks.register(callback);
                        return;
                    }
                    return;
                case 31:
                    IBluetoothStateChangeCallback callback2 = (IBluetoothStateChangeCallback) msg.obj;
                    Slog.d(BluetoothManagerService.TAG, "Unregister callback = " + callback2);
                    if (callback2 != null) {
                        BluetoothManagerService.this.mStateChangeCallbacks.unregister(callback2);
                        return;
                    }
                    return;
                case 40:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_SERVICE_CONNECTED: " + msg.arg1);
                    IBinder service = (IBinder) msg.obj;
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (msg.arg1 == 2) {
                            BluetoothManagerService.this.mBluetoothGatt = IBluetoothGatt.Stub.asInterface(service);
                            BluetoothManagerService.this.onBluetoothGattServiceUp();
                            return;
                        }
                        BluetoothManagerService.this.mHandler.removeMessages(100);
                        BluetoothManagerService.this.mBinding = false;
                        BluetoothManagerService.this.mBluetoothBinder = service;
                        BluetoothManagerService.this.mBluetooth = IBluetooth.Stub.asInterface(service);
                        if (!BluetoothManagerService.this.isNameAndAddressSet()) {
                            Message getMsg = BluetoothManagerService.this.mHandler.obtainMessage(200);
                            BluetoothManagerService.this.mHandler.sendMessage(getMsg);
                            if (this.mGetNameAddressOnly) {
                                return;
                            }
                        }
                        try {
                            boolean enableHciSnoopLog = Settings.Secure.getInt(BluetoothManagerService.this.mContentResolver, "bluetooth_hci_log", 0) == 1;
                            if (!BluetoothManagerService.this.mBluetooth.configHciSnoopLog(enableHciSnoopLog)) {
                                Slog.e(BluetoothManagerService.TAG, "IBluetooth.configHciSnoopLog return false");
                            }
                            break;
                        } catch (RemoteException e2) {
                            Slog.e(BluetoothManagerService.TAG, "Unable to call configHciSnoopLog", e2);
                        }
                        try {
                            BluetoothManagerService.this.mBluetooth.registerCallback(BluetoothManagerService.this.mBluetoothCallback);
                            break;
                        } catch (RemoteException re) {
                            Slog.e(BluetoothManagerService.TAG, "Unable to register BluetoothCallback", re);
                        }
                        BluetoothManagerService.this.sendBluetoothServiceUpCallback();
                        try {
                            if (BluetoothManagerService.this.mQuietEnable) {
                                if (!BluetoothManagerService.this.mBluetooth.enableNoAutoConnect()) {
                                    Slog.e(BluetoothManagerService.TAG, "IBluetooth.enableNoAutoConnect() returned false");
                                }
                            } else if (!BluetoothManagerService.this.mBluetooth.enable()) {
                                Slog.e(BluetoothManagerService.TAG, "IBluetooth.enable() returned false");
                            }
                            break;
                        } catch (RemoteException e3) {
                            Slog.e(BluetoothManagerService.TAG, "Unable to call enable()", e3);
                        }
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                        if (BluetoothManagerService.this.mEnable) {
                            return;
                        }
                        BluetoothManagerService.this.waitForOnOff(true, false);
                        BluetoothManagerService.this.handleDisable();
                        BluetoothManagerService.this.waitForOnOff(false, false);
                        return;
                    } finally {
                    }
                case 41:
                    Slog.e(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED: " + msg.arg1);
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (msg.arg1 == 1) {
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.mBluetooth = null;
                                BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                                if (BluetoothManagerService.this.mEnable) {
                                    BluetoothManagerService.this.mEnable = false;
                                    Message restartMsg2 = BluetoothManagerService.this.mHandler.obtainMessage(42);
                                    BluetoothManagerService.this.mHandler.sendMessageDelayed(restartMsg2, 200L);
                                }
                                BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                                if (BluetoothManagerService.this.mState == 11 || BluetoothManagerService.this.mState == 12) {
                                    BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                                    BluetoothManagerService.this.mState = 13;
                                }
                                if (BluetoothManagerService.this.mState == 13) {
                                    BluetoothManagerService.this.bluetoothStateChangeHandler(13, 10);
                                }
                                BluetoothManagerService.this.mHandler.removeMessages(60);
                                BluetoothManagerService.this.mState = 10;
                            }
                        } else if (msg.arg1 == 2) {
                            BluetoothManagerService.this.mBluetoothGatt = null;
                            BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                        } else {
                            Slog.e(BluetoothManagerService.TAG, "Bad msg.arg1: " + msg.arg1);
                            BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                        }
                        return;
                    } finally {
                    }
                case 42:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_RESTART_BLUETOOTH_SERVICE: Restart IBluetooth service");
                    BluetoothManagerService.this.mEnable = true;
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                    return;
                case 60:
                    int prevState = msg.arg1;
                    int newState = msg.arg2;
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_STATE_CHANGE: prevState = " + prevState + ", newState=" + newState);
                    BluetoothManagerService.this.mState = newState;
                    BluetoothManagerService.this.bluetoothStateChangeHandler(prevState, newState);
                    if (prevState == 14 && newState == 10 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                        BluetoothManagerService.this.recoverBluetoothServiceFromError();
                    }
                    if (prevState == 11 && newState == 15 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                        BluetoothManagerService.this.recoverBluetoothServiceFromError();
                    }
                    if (prevState == 16 && newState == 10 && BluetoothManagerService.this.mEnable) {
                        Slog.d(BluetoothManagerService.TAG, "Entering STATE_OFF but mEnabled is true; restarting.");
                        BluetoothManagerService.this.waitForOnOff(false, true);
                        Message restartMsg3 = BluetoothManagerService.this.mHandler.obtainMessage(42);
                        BluetoothManagerService.this.mHandler.sendMessageDelayed(restartMsg3, 400L);
                    }
                    if ((newState == 12 || newState == 15) && BluetoothManagerService.this.mErrorRecoveryRetryCounter != 0) {
                        Slog.w(BluetoothManagerService.TAG, "bluetooth is recovered from error");
                        BluetoothManagerService.this.mErrorRecoveryRetryCounter = 0;
                        return;
                    }
                    return;
                case 100:
                    Slog.e(BluetoothManagerService.TAG, "MESSAGE_TIMEOUT_BIND");
                    BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                    BluetoothManagerService.this.mBinding = false;
                    return;
                case 101:
                    Slog.e(BluetoothManagerService.TAG, "MESSAGE_TIMEOUT_UNBIND");
                    BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                    BluetoothManagerService.this.mUnbinding = false;
                    return;
                case 200:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_GET_NAME_AND_ADDRESS");
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (BluetoothManagerService.this.mBluetooth == null && !BluetoothManagerService.this.mBinding) {
                            Slog.d(BluetoothManagerService.TAG, "Binding to service to get name and address");
                            this.mGetNameAddressOnly = true;
                            Message timeoutMsg = BluetoothManagerService.this.mHandler.obtainMessage(100);
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(timeoutMsg, 3000L);
                            Intent i = new Intent(IBluetooth.class.getName());
                            if (BluetoothManagerService.this.doBind(i, BluetoothManagerService.this.mConnection, 65, UserHandle.CURRENT)) {
                                BluetoothManagerService.this.mBinding = true;
                            } else {
                                BluetoothManagerService.this.mHandler.removeMessages(100);
                                Slog.e(BluetoothManagerService.TAG, "fail to bind to: " + IBluetooth.class.getName());
                            }
                        } else if (BluetoothManagerService.this.mBluetooth != null) {
                            Message saveMsg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS);
                            saveMsg.arg1 = 0;
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.mHandler.sendMessage(saveMsg);
                            } else {
                                BluetoothManagerService.this.mHandler.sendMessageDelayed(saveMsg, 500L);
                            }
                        }
                        return;
                    } finally {
                    }
                case BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS:
                    boolean unbind = false;
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_SAVE_NAME_AND_ADDRESS");
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (!BluetoothManagerService.this.mEnable && BluetoothManagerService.this.mBluetooth != null) {
                            try {
                                BluetoothManagerService.this.mBluetooth.enable();
                            } catch (RemoteException e4) {
                                Slog.e(BluetoothManagerService.TAG, "Unable to call enable()", e4);
                            }
                        }
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.waitForBleOn();
                        }
                        try {
                            BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                String name = null;
                                String address = null;
                                try {
                                    name = BluetoothManagerService.this.mBluetooth.getName();
                                    address = BluetoothManagerService.this.mBluetooth.getAddress();
                                } catch (RemoteException re2) {
                                    Slog.e(BluetoothManagerService.TAG, "", re2);
                                }
                                if (name != null && address != null) {
                                    BluetoothManagerService.this.storeNameAndAddress(name, address);
                                    if (this.mGetNameAddressOnly) {
                                        unbind = true;
                                    }
                                } else if (msg.arg1 < 3) {
                                    Message retryMsg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS);
                                    retryMsg.arg1 = msg.arg1 + 1;
                                    Slog.d(BluetoothManagerService.TAG, "Retrying name/address remote retrieval and save.....Retry count =" + retryMsg.arg1);
                                    BluetoothManagerService.this.mHandler.sendMessageDelayed(retryMsg, 500L);
                                } else {
                                    Slog.w(BluetoothManagerService.TAG, "Maximum name/address remoteretrieval retry exceeded");
                                    if (this.mGetNameAddressOnly) {
                                        unbind = true;
                                    }
                                }
                                if (!BluetoothManagerService.this.mEnable) {
                                    try {
                                        BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                                    } catch (RemoteException e5) {
                                        Slog.e(BluetoothManagerService.TAG, "Unable to call disable()", e5);
                                    }
                                }
                                break;
                            } else {
                                Message getMsg2 = BluetoothManagerService.this.mHandler.obtainMessage(200);
                                BluetoothManagerService.this.mHandler.sendMessage(getMsg2);
                            }
                            BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                            if (!BluetoothManagerService.this.mEnable && BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.waitForOnOff(false, true);
                            }
                            if (unbind) {
                                BluetoothManagerService.this.unbindAndFinish();
                            }
                            this.mGetNameAddressOnly = false;
                            return;
                        } finally {
                        }
                    } finally {
                    }
                    break;
                case BluetoothManagerService.MESSAGE_USER_SWITCHED:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_USER_SWITCHED");
                    BluetoothManagerService.this.mHandler.removeMessages(BluetoothManagerService.MESSAGE_USER_SWITCHED);
                    if (!BluetoothManagerService.this.mEnable || BluetoothManagerService.this.mBluetooth == null) {
                        if (BluetoothManagerService.this.mBinding || BluetoothManagerService.this.mBluetooth != null) {
                            Message userMsg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_USER_SWITCHED);
                            userMsg.arg2 = msg.arg2 + 1;
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(userMsg, 200L);
                            Slog.d(BluetoothManagerService.TAG, "delay MESSAGE_USER_SWITCHED " + userMsg.arg2);
                            return;
                        }
                        return;
                    }
                    try {
                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.mBluetooth.unregisterCallback(BluetoothManagerService.this.mBluetoothCallback);
                        }
                    } catch (RemoteException re3) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to unregister", re3);
                    } finally {
                    }
                    if (BluetoothManagerService.this.mState == 13) {
                        BluetoothManagerService.this.waitForBleOn();
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 15);
                        BluetoothManagerService.this.mState = 15;
                    }
                    if (BluetoothManagerService.this.mState == 15) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 16);
                        BluetoothManagerService.this.mState = 16;
                    }
                    if (BluetoothManagerService.this.mState == 16) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 10);
                        BluetoothManagerService.this.mState = 10;
                    }
                    if (BluetoothManagerService.this.mState == 10) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 14);
                        BluetoothManagerService.this.mState = 14;
                    }
                    if (BluetoothManagerService.this.mState == 14) {
                        BluetoothManagerService.this.waitForBleOn();
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 15);
                        BluetoothManagerService.this.mState = 15;
                    }
                    if (BluetoothManagerService.this.mState == 15) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 11);
                        BluetoothManagerService.this.mState = 11;
                    }
                    BluetoothManagerService.this.waitForOnOff(true, false);
                    if (BluetoothManagerService.this.mState == 11) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 12);
                    }
                    BluetoothManagerService.this.unbindAllBluetoothProfileServices();
                    BluetoothManagerService.this.handleDisable();
                    BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                    BluetoothManagerService.this.waitForBleOn();
                    BluetoothManagerService.this.bluetoothStateChangeHandler(13, 15);
                    BluetoothManagerService.this.bluetoothStateChangeHandler(15, 16);
                    boolean didDisableTimeout = !BluetoothManagerService.this.waitForOnOff(false, true);
                    BluetoothManagerService.this.bluetoothStateChangeHandler(16, 10);
                    BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.mBluetooth = null;
                            BluetoothManagerService.this.mContext.unbindService(BluetoothManagerService.this.mConnection);
                        }
                        BluetoothManagerService.this.mBluetoothGatt = null;
                        if (didDisableTimeout) {
                            SystemClock.sleep(3000L);
                        } else {
                            SystemClock.sleep(100L);
                        }
                        BluetoothManagerService.this.mHandler.removeMessages(60);
                        BluetoothManagerService.this.mState = 10;
                        BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                        return;
                    } finally {
                    }
                case 301:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_USER_UNLOCKED");
                    BluetoothManagerService.this.mHandler.removeMessages(BluetoothManagerService.MESSAGE_USER_SWITCHED);
                    if (BluetoothManagerService.this.mEnable && !BluetoothManagerService.this.mBinding && BluetoothManagerService.this.mBluetooth == null) {
                        Slog.d(BluetoothManagerService.TAG, "Enabled but not bound; retrying after unlock");
                        BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                        return;
                    }
                    return;
                case BluetoothManagerService.MESSAGE_ADD_PROXY_DELAYED:
                    ProfileServiceConnections psc = (ProfileServiceConnections) BluetoothManagerService.this.mProfileServices.get(new Integer(msg.arg1));
                    if (psc != null) {
                        IBluetoothProfileServiceConnection proxy = (IBluetoothProfileServiceConnection) msg.obj;
                        psc.addProxy(proxy);
                        return;
                    }
                    return;
                case 401:
                    ProfileServiceConnections psc2 = (ProfileServiceConnections) msg.obj;
                    removeMessages(401, msg.obj);
                    if (psc2 != null) {
                        psc2.bindService();
                        return;
                    }
                    return;
                case BluetoothManagerService.MESSAGE_WHOLE_CHIP_RESET:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_WHOLE_CHIP_RESET");
                    BluetoothManagerService.this.handleWholeChipReset();
                    return;
                default:
                    return;
            }
        }
    }

    private void handleWholeChipReset() {
        Slog.d(TAG, "handleWholeChipReset");
        sendDisableMsg();
        sendEnableMsg(this.mQuietEnableExternal);
    }

    private void handleEnable(boolean quietMode) {
        this.mQuietEnable = quietMode;
        try {
            this.mBluetoothLock.writeLock().lock();
            Slog.d(TAG, "handleEnable: mBluetooth = " + this.mBluetooth + ", mBinding = " + this.mBinding + "quietMode = " + quietMode);
            if (this.mBluetooth == null && !this.mBinding) {
                Slog.d(TAG, "Bind AdapterService");
                Message timeoutMsg = this.mHandler.obtainMessage(100);
                this.mHandler.sendMessageDelayed(timeoutMsg, 3000L);
                Intent i = new Intent(IBluetooth.class.getName());
                if (doBind(i, this.mConnection, 65, UserHandle.CURRENT)) {
                    this.mBinding = true;
                } else {
                    this.mHandler.removeMessages(100);
                    Slog.e(TAG, "Fail to bind to: " + IBluetooth.class.getName());
                }
            } else if (this.mBluetooth != null) {
                try {
                    if (this.mQuietEnable) {
                        if (!this.mBluetooth.enableNoAutoConnect()) {
                            Slog.e(TAG, "IBluetooth.enableNoAutoConnect() returned false");
                        }
                    } else if (!this.mBluetooth.enable()) {
                        Slog.e(TAG, "IBluetooth.enable() returned false");
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to call enable()", e);
                }
            }
        } finally {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    boolean doBind(Intent intent, ServiceConnection conn, int flags, UserHandle user) {
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !this.mContext.bindServiceAsUser(intent, conn, flags, user)) {
            Slog.e(TAG, "Fail to bind to: " + intent);
            return false;
        }
        return true;
    }

    private void handleDisable() {
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                Slog.d(TAG, "Sending off request.");
                if (!this.mBluetooth.disable()) {
                    Slog.e(TAG, "IBluetooth.disable() returned false");
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call disable()", e);
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    private boolean checkIfCallerIsForegroundUser() {
        int callingUser = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        long callingIdentity = Binder.clearCallingIdentity();
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        UserInfo ui = um.getProfileParent(callingUser);
        int parentUser = ui != null ? ui.id : -10000;
        int callingAppId = UserHandle.getAppId(callingUid);
        try {
            int foregroundUser = ActivityManager.getCurrentUser();
            boolean valid = callingUser == foregroundUser || parentUser == foregroundUser || callingAppId == 1027 || callingAppId == this.mSystemUiUid;
            Slog.d(TAG, "checkIfCallerIsForegroundUser: valid=" + valid + " callingUser=" + callingUser + " parentUser=" + parentUser + " foregroundUser=" + foregroundUser);
            return valid;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private void sendBleStateChanged(int prevState, int newState) {
        Slog.d(TAG, "BLE State Change Intent: " + prevState + " -> " + newState);
        Intent intent = new Intent("android.bluetooth.adapter.action.BLE_STATE_CHANGED");
        intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", prevState);
        intent.putExtra("android.bluetooth.adapter.extra.STATE", newState);
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
    }

    private void bluetoothStateChangeHandler(int prevState, int newState) {
        boolean isStandardBroadcast = true;
        if (prevState == newState) {
            return;
        }
        if (newState == 15 || newState == 10) {
            boolean intermediate_off = prevState == 13 && newState == 15;
            if (newState == 10) {
                Slog.d(TAG, "Bluetooth is complete turn off");
                sendBluetoothServiceDownCallback();
                unbindAndFinish();
                sendBleStateChanged(prevState, newState);
                isStandardBroadcast = false;
            } else if (!intermediate_off) {
                Slog.d(TAG, "Bluetooth is in LE only mode");
                if (this.mBluetoothGatt != null) {
                    Slog.d(TAG, "Calling BluetoothGattServiceUp");
                    onBluetoothGattServiceUp();
                } else {
                    Slog.d(TAG, "Binding Bluetooth GATT service");
                    if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
                        Intent i = new Intent(IBluetoothGatt.class.getName());
                        doBind(i, this.mConnection, 65, UserHandle.CURRENT);
                    }
                }
                sendBleStateChanged(prevState, newState);
                isStandardBroadcast = false;
            } else if (intermediate_off) {
                Slog.d(TAG, "Intermediate off, back to LE only mode");
                sendBleStateChanged(prevState, newState);
                sendBluetoothStateCallback(false);
                newState = 10;
                sendBrEdrDownCallback();
            }
        } else if (newState == 12) {
            boolean isUp = newState == 12;
            this.mEnable = true;
            sendBluetoothStateCallback(isUp);
            sendBleStateChanged(prevState, newState);
        } else if (newState == 14 || newState == 16) {
            sendBleStateChanged(prevState, newState);
            isStandardBroadcast = false;
        } else if (newState == 11 || newState == 13) {
            sendBleStateChanged(prevState, newState);
        }
        if (!isStandardBroadcast) {
            return;
        }
        if (prevState == 15) {
            prevState = 10;
        }
        Intent intent = new Intent("android.bluetooth.adapter.action.STATE_CHANGED");
        intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", prevState);
        intent.putExtra("android.bluetooth.adapter.extra.STATE", newState);
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
    }

    private boolean waitForOnOff(boolean on, boolean off) {
        int i = 0;
        while (true) {
            if (i >= 20) {
                break;
            }
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth == null) {
                    break;
                }
                if (on) {
                    if (this.mBluetooth.getState() == 12) {
                        return true;
                    }
                } else if (off) {
                    if (this.mBluetooth.getState() == 10) {
                        return true;
                    }
                } else if (this.mBluetooth.getState() != 12) {
                    return true;
                }
                this.mBluetoothLock.readLock().unlock();
                if (on || off) {
                    SystemClock.sleep(300L);
                } else {
                    SystemClock.sleep(50L);
                }
                i++;
            } catch (RemoteException e) {
                Slog.e(TAG, "getState()", e);
            } finally {
                this.mBluetoothLock.readLock().unlock();
            }
        }
        Slog.e(TAG, "waitForOnOff time out");
        return false;
    }

    private boolean waitForBleOn() {
        int i = 0;
        while (true) {
            if (i >= 10) {
                break;
            }
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth == null) {
                    break;
                }
                if (this.mBluetooth.getState() == 15) {
                    return true;
                }
                this.mBluetoothLock.readLock().unlock();
                SystemClock.sleep(300L);
                i++;
            } catch (RemoteException e) {
                Slog.e(TAG, "getState()", e);
            } finally {
                this.mBluetoothLock.readLock().unlock();
            }
        }
        Slog.e(TAG, "waitForBleOn time out");
        return false;
    }

    private void sendDisableMsg() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
    }

    private void sendEnableMsg(boolean quietMode) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, quietMode ? 1 : 0, 0));
    }

    private void recoverBluetoothServiceFromError() {
        Slog.e(TAG, "recoverBluetoothServiceFromError");
        try {
            this.mBluetoothLock.readLock().lock();
            if (this.mBluetooth != null) {
                this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
            }
        } catch (RemoteException re) {
            Slog.e(TAG, "Unable to unregister", re);
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
        SystemClock.sleep(500L);
        handleDisable();
        waitForOnOff(false, true);
        sendBluetoothServiceDownCallback();
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mBluetooth != null) {
                this.mBluetooth = null;
                this.mContext.unbindService(this.mConnection);
            }
            this.mBluetoothGatt = null;
            this.mBluetoothLock.writeLock().unlock();
            this.mHandler.removeMessages(60);
            this.mState = 10;
            this.mEnable = false;
            int i = this.mErrorRecoveryRetryCounter;
            this.mErrorRecoveryRetryCounter = i + 1;
            if (i >= 6) {
                return;
            }
            Message restartMsg = this.mHandler.obtainMessage(42);
            this.mHandler.sendMessageDelayed(restartMsg, 3000L);
        } catch (Throwable th) {
            this.mBluetoothLock.writeLock().unlock();
            throw th;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        String errorMsg = null;
        if (this.mBluetoothBinder == null) {
            errorMsg = "Bluetooth Service not connected";
        } else {
            try {
                this.mBluetoothBinder.dump(fd, args);
            } catch (RemoteException e) {
                errorMsg = "RemoteException while calling Bluetooth Service";
            }
        }
        if (errorMsg == null) {
            return;
        }
        if (args.length > 0 && args[0].startsWith("--proto")) {
            return;
        }
        writer.println(errorMsg);
    }
}
