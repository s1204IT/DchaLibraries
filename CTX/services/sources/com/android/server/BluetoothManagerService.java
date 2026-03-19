package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothCallback;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothHeadset;
import android.bluetooth.IBluetoothManager;
import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothProfileServiceConnection;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ActivityNotFoundException;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Slog;
import android.util.StatsLog;
import com.android.internal.util.DumpUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.DumpState;
import com.android.server.pm.Settings;
import com.android.server.pm.UserRestrictionsUtils;
import com.android.server.usage.AppStandbyController;
import com.android.server.utils.PriorityDump;
import com.mediatek.cta.CtaManagerFactory;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class BluetoothManagerService extends IBluetoothManager.Stub {
    private static final String ACTION_PACKAGE_DATA_CLEARED = "android.intent.action.PACKAGE_DATA_CLEARED";
    private static final int ACTIVE_LOG_MAX_SIZE = 20;
    private static final int ADD_PROXY_DELAY_MS = 100;
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final int BLUETOOTH_OFF = 0;
    private static final int BLUETOOTH_ON_AIRPLANE = 2;
    private static final int BLUETOOTH_ON_BLUETOOTH = 1;
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    private static final int CRASH_LOG_MAX_SIZE = 100;
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).equals("sqc");
    private static final int ERROR_RESTART_TIME_MS = 3000;
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
    private static final int MESSAGE_RESTORE_USER_SETTING = 500;
    private static final int MESSAGE_SAVE_NAME_AND_ADDRESS = 201;
    private static final int MESSAGE_TIMEOUT_BIND = 100;
    private static final int MESSAGE_TIMEOUT_UNBIND = 101;
    private static final int MESSAGE_UNREGISTER_ADAPTER = 21;
    private static final int MESSAGE_UNREGISTER_STATE_CHANGE_CALLBACK = 31;
    private static final int MESSAGE_USER_SWITCHED = 300;
    private static final int MESSAGE_USER_UNLOCKED = 301;
    private static final int RESTORE_SETTING_TO_OFF = 0;
    private static final int RESTORE_SETTING_TO_ON = 1;
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
    private ActivityManager mActivityManager;
    private String mAddress;
    private boolean mBinding;
    private IBluetooth mBluetooth;
    private IBinder mBluetoothBinder;
    private IBluetoothGatt mBluetoothGatt;
    private final RemoteCallbackList<IBluetoothManagerCallback> mCallbacks;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int mCrashes;
    private boolean mEnable;
    private boolean mEnableBLE;
    private boolean mEnableExternal;
    private int mErrorRecoveryRetryCounter;
    private long mLastEnabledTime;
    private String mName;
    private final boolean mPermissionReviewRequired;
    private boolean mQuietEnableExternal;
    private int mState;
    private final RemoteCallbackList<IBluetoothStateChangeCallback> mStateChangeCallbacks;
    private final int mSystemUiUid;
    private boolean mUnbinding;
    private final ReentrantReadWriteLock mBluetoothLock = new ReentrantReadWriteLock();
    private boolean mQuietEnable = false;
    private final LinkedList<ActiveLog> mActiveLogs = new LinkedList<>();
    private final LinkedList<Long> mCrashTimestamps = new LinkedList<>();
    private Map<IBinder, ClientDeathRecipient> mBleApps = new ConcurrentHashMap();
    private boolean mIsUserSwitch = false;
    private final Map<Integer, ProfileServiceConnections> mProfileServices = new HashMap();
    private final IBluetoothCallback mBluetoothCallback = new IBluetoothCallback.Stub() {
        public void onBluetoothStateChange(int i, int i2) throws RemoteException {
            BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(60, i, i2));
        }
    };
    private final UserManagerInternal.UserRestrictionsListener mUserRestrictionsListener = new UserManagerInternal.UserRestrictionsListener() {
        public void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2) {
            if (UserRestrictionsUtils.restrictionsChanged(bundle2, bundle, "no_bluetooth_sharing")) {
                BluetoothManagerService.this.updateOppLauncherComponentState(i, bundle.getBoolean("no_bluetooth_sharing"));
            }
            if (i == 0 && UserRestrictionsUtils.restrictionsChanged(bundle2, bundle, "no_bluetooth")) {
                if (i != 0 || !bundle.getBoolean("no_bluetooth")) {
                    BluetoothManagerService.this.updateOppLauncherComponentState(i, bundle.getBoolean("no_bluetooth_sharing"));
                } else {
                    BluetoothManagerService.this.updateOppLauncherComponentState(i, true);
                    BluetoothManagerService.this.sendDisableMsg(3, BluetoothManagerService.this.mContext.getPackageName());
                }
            }
        }
    };
    private final ContentObserver mAirplaneModeObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean z) {
            ReentrantReadWriteLock.ReadLock lock;
            synchronized (this) {
                if (BluetoothManagerService.this.isBluetoothPersistedStateOn()) {
                    if (BluetoothManagerService.this.isAirplaneModeOn()) {
                        BluetoothManagerService.this.persistBluetoothSetting(2);
                    } else {
                        BluetoothManagerService.this.persistBluetoothSetting(1);
                    }
                }
                int state = 10;
                try {
                    try {
                        BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            state = BluetoothManagerService.this.mBluetooth.getState();
                            BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                            Slog.d(BluetoothManagerService.TAG, "Airplane Mode change - current state:  " + BluetoothAdapter.nameForState(state));
                            if (!BluetoothManagerService.this.isAirplaneModeOn()) {
                                BluetoothManagerService.this.clearBleApps();
                                if (state == 15) {
                                    try {
                                        try {
                                            BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                                            if (BluetoothManagerService.this.mBluetooth != null) {
                                                BluetoothManagerService.this.addActiveLog(2, BluetoothManagerService.this.mContext.getPackageName(), false);
                                                BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                                                BluetoothManagerService.this.mEnable = false;
                                                BluetoothManagerService.this.mEnableExternal = false;
                                            }
                                            lock = BluetoothManagerService.this.mBluetoothLock.readLock();
                                        } catch (RemoteException e) {
                                            Slog.e(BluetoothManagerService.TAG, "Unable to call onBrEdrDown", e);
                                            lock = BluetoothManagerService.this.mBluetoothLock.readLock();
                                        }
                                        lock.unlock();
                                    } finally {
                                    }
                                } else if (state == 12) {
                                    BluetoothManagerService.this.sendDisableMsg(2, BluetoothManagerService.this.mContext.getPackageName());
                                }
                            } else if (BluetoothManagerService.this.mEnableExternal && state != 12 && BluetoothManagerService.this.isBluetoothPersistedStateOn()) {
                                BluetoothManagerService.this.sendEnableMsg(BluetoothManagerService.this.mQuietEnableExternal, 2, BluetoothManagerService.this.mContext.getPackageName());
                            }
                        } else {
                            BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                            Slog.d(BluetoothManagerService.TAG, "Airplane Mode change - current state:  " + BluetoothAdapter.nameForState(state));
                            if (!BluetoothManagerService.this.isAirplaneModeOn()) {
                            }
                        }
                    } catch (RemoteException e2) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to call getState", e2);
                        return;
                    }
                } finally {
                }
            }
        }
    };
    private final BroadcastReceiver mReceiverDataCleared = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothManagerService.ACTION_PACKAGE_DATA_CLEARED.equals(intent.getAction())) {
                if (BluetoothManagerService.DBG) {
                    Slog.d(BluetoothManagerService.TAG, "Bluetooth package data cleared");
                }
                try {
                    BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "handleEnable: mBluetooth = " + BluetoothManagerService.this.mBluetooth + ", mBinding = " + BluetoothManagerService.this.mBinding);
                    }
                    if (BluetoothManagerService.this.mBluetooth == null && BluetoothManagerService.this.mEnable) {
                        if (BluetoothManagerService.DBG) {
                            Slog.d(BluetoothManagerService.TAG, "Bind AdapterService");
                        }
                        BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(100), 3000L);
                        if (!BluetoothManagerService.this.doBind(new Intent(IBluetooth.class.getName()), BluetoothManagerService.this.mConnection, 65, UserHandle.CURRENT)) {
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
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED".equals(action)) {
                String stringExtra = intent.getStringExtra("android.bluetooth.adapter.extra.LOCAL_NAME");
                if (BluetoothManagerService.DBG) {
                    Slog.d(BluetoothManagerService.TAG, "Bluetooth Adapter name changed to " + stringExtra);
                }
                if (stringExtra != null) {
                    BluetoothManagerService.this.storeNameAndAddress(stringExtra, null);
                    return;
                }
                return;
            }
            if ("android.bluetooth.adapter.action.BLUETOOTH_ADDRESS_CHANGED".equals(action)) {
                String stringExtra2 = intent.getStringExtra("android.bluetooth.adapter.extra.BLUETOOTH_ADDRESS");
                if (stringExtra2 != null) {
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "Bluetooth Adapter address changed to " + stringExtra2);
                    }
                    BluetoothManagerService.this.storeNameAndAddress(null, stringExtra2);
                    return;
                }
                if (BluetoothManagerService.DBG) {
                    Slog.e(BluetoothManagerService.TAG, "No Bluetooth Adapter address parameter found");
                    return;
                }
                return;
            }
            if ("android.os.action.SETTING_RESTORED".equals(action) && "bluetooth_on".equals(intent.getStringExtra("setting_name"))) {
                String stringExtra3 = intent.getStringExtra("previous_value");
                String stringExtra4 = intent.getStringExtra("new_value");
                if (BluetoothManagerService.DBG) {
                    Slog.d(BluetoothManagerService.TAG, "ACTION_SETTING_RESTORED with BLUETOOTH_ON, prevValue=" + stringExtra3 + ", newValue=" + stringExtra4);
                }
                if (stringExtra4 != null && stringExtra3 != null && !stringExtra3.equals(stringExtra4)) {
                    BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(SystemService.PHASE_SYSTEM_SERVICES_READY, stringExtra4.equals("0") ? 0 : 1, 0));
                }
            }
        }
    };
    private BluetoothServiceConnection mConnection = new BluetoothServiceConnection();
    private final BluetoothHandler mHandler = new BluetoothHandler(IoThread.get().getLooper());

    private static CharSequence timeToLog(long j) {
        return DateFormat.format("MM-dd HH:mm:ss", j);
    }

    private class ActiveLog {
        private boolean mEnable;
        private String mPackageName;
        private int mReason;
        private long mTimestamp;

        ActiveLog(int i, String str, boolean z, long j) {
            this.mReason = i;
            this.mPackageName = str;
            this.mEnable = z;
            this.mTimestamp = j;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append((Object) BluetoothManagerService.timeToLog(this.mTimestamp));
            sb.append(this.mEnable ? "  Enabled " : " Disabled ");
            sb.append(" due to ");
            sb.append(BluetoothManagerService.getEnableDisableReasonString(this.mReason));
            sb.append(" by ");
            sb.append(this.mPackageName);
            return sb.toString();
        }
    }

    BluetoothManagerService(Context context) {
        this.mContext = context;
        this.mPermissionReviewRequired = context.getResources().getBoolean(R.^attr-private.magnifierColorOverlay) || CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported();
        this.mCrashes = 0;
        this.mBluetooth = null;
        this.mBluetoothBinder = null;
        this.mBluetoothGatt = null;
        this.mBinding = false;
        this.mUnbinding = false;
        this.mEnable = false;
        this.mEnableBLE = false;
        this.mState = 10;
        this.mQuietEnableExternal = false;
        this.mEnableExternal = false;
        this.mAddress = null;
        this.mName = null;
        this.mErrorRecoveryRetryCounter = 0;
        this.mContentResolver = context.getContentResolver();
        registerForBleScanModeChange();
        this.mCallbacks = new RemoteCallbackList<>();
        this.mStateChangeCallbacks = new RemoteCallbackList<>();
        IntentFilter intentFilter = new IntentFilter(ACTION_PACKAGE_DATA_CLEARED);
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiver(this.mReceiverDataCleared, intentFilter);
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
        intentFilter2.addAction("android.bluetooth.adapter.action.BLUETOOTH_ADDRESS_CHANGED");
        intentFilter2.addAction("android.os.action.SETTING_RESTORED");
        intentFilter2.setPriority(1000);
        this.mContext.registerReceiver(this.mReceiver, intentFilter2);
        loadStoredNameAndAddress();
        if (isBluetoothPersistedStateOn()) {
            if (DBG) {
                Slog.d(TAG, "Startup: Bluetooth persisted state is ON.");
            }
            this.mEnableExternal = true;
        }
        String string = Settings.Global.getString(this.mContentResolver, "airplane_mode_radios");
        if (string == null || string.contains("bluetooth")) {
            this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        }
        try {
            packageUidAsUser = this.mContext.getResources().getBoolean(R.^attr-private.lightZ) ? -1 : this.mContext.getPackageManager().getPackageUidAsUser("com.android.systemui", DumpState.DUMP_DEXOPT, 0);
            Slog.d(TAG, "Detected SystemUiUid: " + Integer.toString(packageUidAsUser));
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Unable to resolve SystemUI's UID.", e);
        }
        this.mSystemUiUid = packageUidAsUser;
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    private boolean supportBluetoothPersistedState() {
        return this.mContext.getResources().getBoolean(R.^attr-private.panelMenuIsCompact);
    }

    private boolean isBluetoothPersistedStateOn() {
        if (!supportBluetoothPersistedState()) {
            return false;
        }
        int i = Settings.Global.getInt(this.mContentResolver, "bluetooth_on", -1);
        if (DBG) {
            Slog.d(TAG, "Bluetooth persisted state: " + i);
        }
        return i != 0;
    }

    private boolean isBluetoothPersistedStateOnBluetooth() {
        return supportBluetoothPersistedState() && Settings.Global.getInt(this.mContentResolver, "bluetooth_on", 1) == 1;
    }

    private void persistBluetoothSetting(int i) {
        if (DBG) {
            Slog.d(TAG, "Persisting Bluetooth Setting: " + i);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        Settings.Global.putInt(this.mContext.getContentResolver(), "bluetooth_on", i);
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private boolean isNameAndAddressSet() {
        return this.mName != null && this.mAddress != null && this.mName.length() > 0 && this.mAddress.length() > 0;
    }

    private void loadStoredNameAndAddress() {
        if (DBG) {
            Slog.d(TAG, "Loading stored name and address");
        }
        if (this.mContext.getResources().getBoolean(R.^attr-private.checkMarkGravity) && Settings.Secure.getInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 0) == 0) {
            if (DBG) {
                Slog.d(TAG, "invalid bluetooth name and address stored");
                return;
            }
            return;
        }
        this.mName = Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME);
        this.mAddress = Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS);
        if (DBG) {
            Slog.d(TAG, "Stored bluetooth Name=" + this.mName + ",Address=" + this.mAddress);
        }
    }

    private void storeNameAndAddress(String str, String str2) {
        if (str != null) {
            Settings.Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME, str);
            this.mName = str;
            if (DBG) {
                Slog.d(TAG, "Stored Bluetooth name: " + Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME));
            }
        }
        if (str2 != null) {
            Settings.Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS, str2);
            this.mAddress = str2;
            if (DBG) {
                Slog.d(TAG, "Stored Bluetoothaddress: " + Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS));
            }
        }
        if (str != null && str2 != null) {
            Settings.Secure.putInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 1);
        }
    }

    public IBluetooth registerAdapter(IBluetoothManagerCallback iBluetoothManagerCallback) {
        if (iBluetoothManagerCallback == null) {
            Slog.w(TAG, "Callback is null in registerAdapter");
            return null;
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(20);
        messageObtainMessage.obj = iBluetoothManagerCallback;
        this.mHandler.sendMessageAtFrontOfQueue(messageObtainMessage);
        try {
            this.mBluetoothLock.writeLock().lock();
            return this.mBluetooth;
        } finally {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    public void unregisterAdapter(IBluetoothManagerCallback iBluetoothManagerCallback) {
        if (iBluetoothManagerCallback == null) {
            Slog.w(TAG, "Callback is null in unregisterAdapter");
            return;
        }
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Message messageObtainMessage = this.mHandler.obtainMessage(21);
        messageObtainMessage.obj = iBluetoothManagerCallback;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void registerStateChangeCallback(IBluetoothStateChangeCallback iBluetoothStateChangeCallback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (iBluetoothStateChangeCallback == null) {
            Slog.w(TAG, "registerStateChangeCallback: Callback is null!");
            return;
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(30);
        messageObtainMessage.obj = iBluetoothStateChangeCallback;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void unregisterStateChangeCallback(IBluetoothStateChangeCallback iBluetoothStateChangeCallback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (iBluetoothStateChangeCallback == null) {
            Slog.w(TAG, "unregisterStateChangeCallback: Callback is null!");
            return;
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(31);
        messageObtainMessage.obj = iBluetoothStateChangeCallback;
        this.mHandler.sendMessageAtFrontOfQueue(messageObtainMessage);
    }

    public boolean isEnabled() {
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Slog.w(TAG, "isEnabled(): not allowed for non-active and non system user");
            return false;
        }
        try {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    return this.mBluetooth.isEnabled();
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "isEnabled()", e);
            }
            return false;
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    public int getState() {
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Slog.w(TAG, "getState(): report OFF for non-active and non system user");
            return 10;
        }
        try {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    return this.mBluetooth.getState();
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "getState()", e);
            }
            return 10;
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    class ClientDeathRecipient implements IBinder.DeathRecipient {
        private String mPackageName;

        ClientDeathRecipient(String str) {
            this.mPackageName = str;
        }

        @Override
        public void binderDied() {
            if (BluetoothManagerService.DBG) {
                Slog.d(BluetoothManagerService.TAG, "Binder is dead - unregister " + this.mPackageName);
            }
            for (Map.Entry entry : BluetoothManagerService.this.mBleApps.entrySet()) {
                IBinder iBinder = (IBinder) entry.getKey();
                if (((ClientDeathRecipient) entry.getValue()).equals(this)) {
                    BluetoothManagerService.this.updateBleAppCount(iBinder, false, this.mPackageName);
                    return;
                }
            }
        }

        public String getPackageName() {
            return this.mPackageName;
        }
    }

    public boolean isBleScanAlwaysAvailable() {
        if (isAirplaneModeOn() && !this.mEnable) {
            return false;
        }
        try {
            return Settings.Global.getInt(this.mContentResolver, "ble_scan_always_enabled") != 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    private void registerForBleScanModeChange() {
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("ble_scan_always_enabled"), false, new ContentObserver(null) {
            @Override
            public void onChange(boolean z) {
                if (!BluetoothManagerService.this.isBleScanAlwaysAvailable()) {
                    BluetoothManagerService.this.disableBleScanMode();
                    BluetoothManagerService.this.clearBleApps();
                    try {
                        try {
                            BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.addActiveLog(1, BluetoothManagerService.this.mContext.getPackageName(), false);
                                BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                            }
                        } catch (RemoteException e) {
                            Slog.e(BluetoothManagerService.TAG, "error when disabling bluetooth", e);
                        }
                    } finally {
                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    }
                }
            }
        });
    }

    private void disableBleScanMode() {
        try {
            try {
                this.mBluetoothLock.writeLock().lock();
                if (this.mBluetooth != null && this.mBluetooth.getState() != 12) {
                    if (DBG) {
                        Slog.d(TAG, "Reseting the mEnable flag for clean disable");
                    }
                    if (!this.mEnableExternal) {
                        this.mEnable = false;
                    }
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "getState()", e);
            }
        } finally {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    public int updateBleAppCount(IBinder iBinder, boolean z, String str) {
        ClientDeathRecipient clientDeathRecipient = this.mBleApps.get(iBinder);
        if (clientDeathRecipient == null && z) {
            ClientDeathRecipient clientDeathRecipient2 = new ClientDeathRecipient(str);
            try {
                iBinder.linkToDeath(clientDeathRecipient2, 0);
                this.mBleApps.put(iBinder, clientDeathRecipient2);
                if (DBG) {
                    Slog.d(TAG, "Registered for death of " + str);
                }
            } catch (RemoteException e) {
                throw new IllegalArgumentException("BLE app (" + str + ") already dead!");
            }
        } else if (!z && clientDeathRecipient != null) {
            iBinder.unlinkToDeath(clientDeathRecipient, 0);
            this.mBleApps.remove(iBinder);
            if (DBG) {
                Slog.d(TAG, "Unregistered for death of " + str);
            }
        }
        try {
            if (z) {
                try {
                    this.mBluetoothLock.readLock().lock();
                    if (this.mBluetooth == null || (this.mBluetooth.getState() != 15 && this.mBluetooth.getState() != 12)) {
                        this.mEnableBLE = true;
                    }
                } catch (RemoteException e2) {
                    Slog.e(TAG, "Unable to call getState", e2);
                }
            }
            int size = this.mBleApps.size();
            if (DBG) {
                Slog.d(TAG, size + " registered Ble Apps");
            }
            if (size == 0 && this.mEnable) {
                disableBleScanMode();
                if (!this.mEnable) {
                    sendBrEdrDownCallback();
                }
            }
            if (size == 0 && !this.mEnableExternal) {
                sendBrEdrDownCallback();
            }
            return size;
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    private void clearBleApps() {
        this.mBleApps.clear();
    }

    public boolean isBleAppPresent() {
        if (DBG) {
            Slog.d(TAG, "isBleAppPresent() count: " + this.mBleApps.size());
        }
        return this.mBleApps.size() > 0;
    }

    private void continueFromBleOnState() {
        if (DBG) {
            Slog.d(TAG, "continueFromBleOnState()");
        }
        try {
            try {
                this.mBluetoothLock.readLock().lock();
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to call onServiceUp", e);
            }
            if (this.mBluetooth == null) {
                Slog.e(TAG, "onBluetoothServiceUp: mBluetooth is null!");
                return;
            }
            if (this.mAddress == null) {
                storeNameAndAddress(null, this.mBluetooth.getAddress());
            }
            if (isBluetoothPersistedStateOnBluetooth() || !isBleAppPresent() || this.mEnableExternal) {
                this.mBluetooth.onLeServiceUp();
                persistBluetoothSetting(1);
            }
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    private void sendBrEdrDownCallback() {
        if (DBG) {
            Slog.d(TAG, "Calling sendBrEdrDownCallback callbacks");
        }
        if (this.mBluetooth == null) {
            Slog.w(TAG, "Bluetooth handle is null");
            return;
        }
        if (isBleAppPresent()) {
            if (this.mBluetoothGatt == null) {
                Slog.w(TAG, "BluetoothGatt is null");
                return;
            }
            try {
                this.mBluetoothGatt.unregAll();
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to disconnect all apps.", e);
                return;
            }
        }
        try {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    this.mBluetooth.onBrEdrDown();
                }
            } catch (RemoteException e2) {
                Slog.e(TAG, "Call to onBrEdrDown() failed.", e2);
            }
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    public boolean enableNoAutoConnect(String str) {
        if (isBluetoothDisallowed()) {
            if (DBG) {
                Slog.d(TAG, "enableNoAutoConnect(): not enabling - bluetooth disallowed");
                return false;
            }
            return false;
        }
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        if (DBG) {
            Slog.d(TAG, "enableNoAutoConnect():  mBluetooth =" + this.mBluetooth + " mBinding = " + this.mBinding);
        }
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1027) {
            throw new SecurityException("no permission to enable Bluetooth quietly");
        }
        synchronized (this.mReceiver) {
            this.mQuietEnableExternal = true;
            this.mEnableExternal = true;
            sendEnableMsg(true, 1, str);
        }
        return true;
    }

    public boolean enable(String str) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        boolean z = UserHandle.getAppId(callingUid) == 1000;
        if (isBluetoothDisallowed()) {
            if (DBG) {
                Slog.d(TAG, "enable(): not enabling - bluetooth disallowed");
            }
            return false;
        }
        if (!z) {
            if (!checkIfCallerIsForegroundUser()) {
                Slog.w(TAG, "enable(): not allowed for non-active and non system user");
                return false;
            }
            this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
            if (!isEnabled() && this.mPermissionReviewRequired && !CtaManagerFactory.getInstance().makeCtaManager().isSystemApp(this.mContext, str) && startConsentUiIfNeeded(str, callingUid, "android.bluetooth.adapter.action.REQUEST_ENABLE")) {
                return false;
            }
        }
        if (DBG) {
            Slog.d(TAG, "enable(" + str + "):  mBluetooth =" + this.mBluetooth + " mBinding = " + this.mBinding + " mState = " + BluetoothAdapter.nameForState(this.mState));
        }
        synchronized (this.mReceiver) {
            this.mQuietEnableExternal = false;
            if (!this.mEnableBLE) {
                this.mEnableExternal = true;
            } else {
                this.mEnableBLE = false;
            }
            sendEnableMsg(false, 1, str);
        }
        if (DBG) {
            Slog.d(TAG, "enable returning");
        }
        return true;
    }

    public boolean disable(String str, boolean z) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        if (!(UserHandle.getAppId(callingUid) == 1000)) {
            if (!checkIfCallerIsForegroundUser()) {
                Slog.w(TAG, "disable(): not allowed for non-active and non system user");
                return false;
            }
            this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
            if (isEnabled() && this.mPermissionReviewRequired && !CtaManagerFactory.getInstance().makeCtaManager().isSystemApp(this.mContext, str) && startConsentUiIfNeeded(str, callingUid, "android.bluetooth.adapter.action.REQUEST_DISABLE")) {
                return false;
            }
        }
        if (DBG) {
            Slog.d(TAG, "disable(): mBluetooth = " + this.mBluetooth + " mBinding = " + this.mBinding);
        }
        synchronized (this.mReceiver) {
            if (z) {
                try {
                    persistBluetoothSetting(0);
                } catch (Throwable th) {
                    throw th;
                }
            }
            this.mEnableExternal = false;
            sendDisableMsg(1, str);
        }
        return true;
    }

    private boolean startConsentUiIfNeeded(String str, int i, String str2) throws RemoteException {
        if (!checkBluetoothPermissionWhenPermissionReviewRequired() && str != null) {
            try {
                if (str.length() != 0) {
                    if (this.mContext.getPackageManager().getApplicationInfoAsUser(str, 268435456, UserHandle.getUserId(i)).uid != i) {
                        throw new SecurityException("Package " + str + " not in uid " + i);
                    }
                    Intent intent = new Intent(str2);
                    intent.putExtra("android.intent.extra.PACKAGE_NAME", str);
                    intent.setFlags(276824064);
                    try {
                        this.mContext.startActivity(intent);
                        return true;
                    } catch (ActivityNotFoundException e) {
                        Slog.e(TAG, "Intent to handle action " + str2 + " missing");
                        return false;
                    }
                }
            } catch (PackageManager.NameNotFoundException e2) {
                throw new RemoteException(e2.getMessage());
            }
        }
        return false;
    }

    private boolean checkBluetoothPermissionWhenPermissionReviewRequired() {
        return this.mPermissionReviewRequired && this.mContext.checkCallingPermission("android.permission.MANAGE_BLUETOOTH_WHEN_PERMISSION_REVIEW_REQUIRED") == 0;
    }

    public void unbindAndFinish() {
        if (DBG) {
            Slog.d(TAG, "unbindAndFinish(): " + this.mBluetooth + " mBinding = " + this.mBinding + " mUnbinding = " + this.mUnbinding);
        }
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mUnbinding) {
                return;
            }
            this.mUnbinding = true;
            this.mHandler.removeMessages(60);
            this.mHandler.removeMessages(MESSAGE_BIND_PROFILE_SERVICE);
            if (this.mBluetooth != null) {
                try {
                    this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to unregister BluetoothCallback", e);
                }
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

    public boolean bindBluetoothProfileService(int i, IBluetoothProfileServiceConnection iBluetoothProfileServiceConnection) {
        if (!this.mEnable || !isEnabled()) {
            if (DBG) {
                Slog.d(TAG, "Trying to bind to profile: " + i + ", while Bluetooth was disabled");
            }
            return false;
        }
        synchronized (this.mProfileServices) {
            if (this.mProfileServices.get(new Integer(i)) == null) {
                if (DBG) {
                    Slog.d(TAG, "Creating new ProfileServiceConnections object for profile: " + i);
                }
                if (i != 1) {
                    return false;
                }
                ProfileServiceConnections profileServiceConnections = new ProfileServiceConnections(new Intent(IBluetoothHeadset.class.getName()));
                if (!profileServiceConnections.bindService()) {
                    return false;
                }
                this.mProfileServices.put(new Integer(i), profileServiceConnections);
            }
            Message messageObtainMessage = this.mHandler.obtainMessage(MESSAGE_ADD_PROXY_DELAYED);
            messageObtainMessage.arg1 = i;
            messageObtainMessage.obj = iBluetoothProfileServiceConnection;
            this.mHandler.sendMessageDelayed(messageObtainMessage, 100L);
            return true;
        }
    }

    public void unbindBluetoothProfileService(int i, IBluetoothProfileServiceConnection iBluetoothProfileServiceConnection) {
        synchronized (this.mProfileServices) {
            ProfileServiceConnections profileServiceConnections = this.mProfileServices.get(new Integer(i));
            if (profileServiceConnections == null) {
                return;
            }
            profileServiceConnections.removeProxy(iBluetoothProfileServiceConnection);
        }
    }

    private void unbindAllBluetoothProfileServices() {
        synchronized (this.mProfileServices) {
            Iterator<Integer> it = this.mProfileServices.keySet().iterator();
            while (it.hasNext()) {
                ProfileServiceConnections profileServiceConnections = this.mProfileServices.get(it.next());
                try {
                    this.mContext.unbindService(profileServiceConnections);
                } catch (IllegalArgumentException e) {
                    Slog.e(TAG, "Unable to unbind service with intent: " + profileServiceConnections.mIntent, e);
                }
                profileServiceConnections.removeAllProxies();
            }
            this.mProfileServices.clear();
        }
    }

    public void handleOnBootPhase() {
        if (DBG) {
            Slog.d(TAG, "Bluetooth boot completed");
        }
        ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).addUserRestrictionsListener(this.mUserRestrictionsListener);
        if (isBluetoothDisallowed()) {
            return;
        }
        try {
            if (AppGlobals.getPackageManager().isFirstBoot() && this.mActivityManager.isLowRamDevice()) {
                Slog.d(TAG, "Low Ram: Change Bluetooth to persisted off for the first boot");
                persistBluetoothSetting(0);
                this.mEnableExternal = false;
            }
            if (this.mEnableExternal && isBluetoothPersistedStateOnBluetooth()) {
                if (DBG) {
                    Slog.d(TAG, "Auto-enabling Bluetooth.");
                }
                sendEnableMsg(this.mQuietEnableExternal, 6, this.mContext.getPackageName());
            } else if (!isNameAndAddressSet()) {
                if (DBG) {
                    Slog.d(TAG, "Getting adapter name and address");
                }
                this.mHandler.sendMessage(this.mHandler.obtainMessage(200));
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void handleOnSwitchUser(int i) {
        if (DBG) {
            Slog.d(TAG, "User " + i + " switched");
        }
        this.mHandler.obtainMessage(300, i, 0).sendToTarget();
    }

    public void handleOnUnlockUser(int i) {
        if (DBG) {
            Slog.d(TAG, "User " + i + " unlocked");
        }
        this.mHandler.obtainMessage(MESSAGE_USER_UNLOCKED, i, 0).sendToTarget();
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
                Message messageObtainMessage = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE);
                messageObtainMessage.obj = this;
                BluetoothManagerService.this.mHandler.sendMessageDelayed(messageObtainMessage, 3000L);
                return true;
            }
            Slog.w(BluetoothManagerService.TAG, "Unable to bind with intent: " + this.mIntent);
            return false;
        }

        private void addProxy(IBluetoothProfileServiceConnection iBluetoothProfileServiceConnection) {
            this.mProxies.register(iBluetoothProfileServiceConnection);
            if (this.mService == null) {
                if (!BluetoothManagerService.this.mHandler.hasMessages(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE, this)) {
                    Message messageObtainMessage = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE);
                    messageObtainMessage.obj = this;
                    BluetoothManagerService.this.mHandler.sendMessage(messageObtainMessage);
                    return;
                }
                return;
            }
            try {
                iBluetoothProfileServiceConnection.onServiceConnected(this.mClassName, this.mService);
            } catch (RemoteException e) {
                Slog.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e);
            }
        }

        private void removeProxy(IBluetoothProfileServiceConnection iBluetoothProfileServiceConnection) {
            if (iBluetoothProfileServiceConnection != null) {
                this.mProxies.unregister(iBluetoothProfileServiceConnection);
                if (this.mProxies.getRegisteredCallbackCount() == 0) {
                    Slog.e(BluetoothManagerService.TAG, "No proxy, unbind");
                    try {
                        BluetoothManagerService.this.mContext.unbindService(this);
                    } catch (IllegalArgumentException e) {
                        Slog.e(BluetoothManagerService.TAG, "Unable to unbind service", e);
                    }
                    onServiceDisconnected(this.mClassName);
                    return;
                }
                return;
            }
            Slog.w(BluetoothManagerService.TAG, "Trying to remove a null proxy");
        }

        private void removeAllProxies() {
            onServiceDisconnected(this.mClassName);
            this.mProxies.kill();
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BluetoothManagerService.this.mHandler.removeMessages(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE, this);
            this.mClassName = componentName;
            try {
                synchronized (this.mClassName) {
                    try {
                        this.mService = iBinder;
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
                    int iBeginBroadcast = this.mProxies.beginBroadcast();
                    for (int i = 0; i < iBeginBroadcast; i++) {
                        try {
                            try {
                                this.mProxies.getBroadcastItem(i).onServiceConnected(componentName, iBinder);
                            } catch (RemoteException e2) {
                                Slog.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e2);
                            }
                        } finally {
                            this.mProxies.finishBroadcast();
                            this.mInvokingProxyCallbacks = false;
                        }
                    }
                }
            } catch (NullPointerException e3) {
                Slog.e(BluetoothManagerService.TAG, "NullPointerException for synchronized(mClassName)", e3);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
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
                    int iBeginBroadcast = this.mProxies.beginBroadcast();
                    for (int i = 0; i < iBeginBroadcast; i++) {
                        try {
                            try {
                                this.mProxies.getBroadcastItem(i).onServiceDisconnected(componentName);
                            } catch (RemoteException e) {
                                Slog.e(BluetoothManagerService.TAG, "Unable to disconnect from proxy", e);
                            }
                        } finally {
                            this.mProxies.finishBroadcast();
                            this.mInvokingProxyCallbacks = false;
                        }
                    }
                }
            } catch (NullPointerException e2) {
                Slog.e(BluetoothManagerService.TAG, "NullPointerException for synchronized(mClassName)", e2);
            } catch (NoSuchElementException e3) {
                Slog.e(BluetoothManagerService.TAG, "NoSuchElementException when unlinkToDeath", e3);
            }
        }

        @Override
        public void binderDied() {
            if (BluetoothManagerService.DBG) {
                Slog.w(BluetoothManagerService.TAG, "Profile service for profile: " + this.mClassName + " died.");
            }
            onServiceDisconnected(this.mClassName);
            Message messageObtainMessage = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE);
            messageObtainMessage.obj = this;
            BluetoothManagerService.this.mHandler.sendMessageDelayed(messageObtainMessage, 3000L);
        }
    }

    private void sendBluetoothStateCallback(boolean z) {
        try {
            int iBeginBroadcast = this.mStateChangeCallbacks.beginBroadcast();
            if (DBG) {
                Slog.d(TAG, "Broadcasting onBluetoothStateChange(" + z + ") to " + iBeginBroadcast + " receivers.");
            }
            for (int i = 0; i < iBeginBroadcast; i++) {
                try {
                    this.mStateChangeCallbacks.getBroadcastItem(i).onBluetoothStateChange(z);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to call onBluetoothStateChange() on callback #" + i, e);
                }
            }
        } finally {
            this.mStateChangeCallbacks.finishBroadcast();
        }
    }

    private void sendBluetoothServiceUpCallback() {
        try {
            int iBeginBroadcast = this.mCallbacks.beginBroadcast();
            Slog.d(TAG, "Broadcasting onBluetoothServiceUp() to " + iBeginBroadcast + " receivers.");
            for (int i = 0; i < iBeginBroadcast; i++) {
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
        try {
            int iBeginBroadcast = this.mCallbacks.beginBroadcast();
            Slog.d(TAG, "Broadcasting onBluetoothServiceDown() to " + iBeginBroadcast + " receivers.");
            for (int i = 0; i < iBeginBroadcast; i++) {
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
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    return this.mBluetooth.getAddress();
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "getAddress(): Unable to retrieve address remotely. Returning cached address", e);
            }
            this.mBluetoothLock.readLock().unlock();
            return this.mAddress;
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    public String getName() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Slog.w(TAG, "getName(): not allowed for non-active and non system user");
            return null;
        }
        try {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    return this.mBluetooth.getName();
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "getName(): Unable to retrieve name remotely. Returning cached name", e);
            }
            this.mBluetoothLock.readLock().unlock();
            return this.mName;
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    private class BluetoothServiceConnection implements ServiceConnection {
        private BluetoothServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            String className = componentName.getClassName();
            if (BluetoothManagerService.DBG) {
                Slog.d(BluetoothManagerService.TAG, "BluetoothServiceConnection: " + className);
            }
            Message messageObtainMessage = BluetoothManagerService.this.mHandler.obtainMessage(40);
            if (className.equals("com.android.bluetooth.btservice.AdapterService")) {
                messageObtainMessage.arg1 = 1;
            } else if (className.equals("com.android.bluetooth.gatt.GattService")) {
                messageObtainMessage.arg1 = 2;
            } else {
                Slog.e(BluetoothManagerService.TAG, "Unknown service connected: " + className);
                return;
            }
            messageObtainMessage.obj = iBinder;
            BluetoothManagerService.this.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            String className = componentName.getClassName();
            if (BluetoothManagerService.DBG) {
                Slog.d(BluetoothManagerService.TAG, "BluetoothServiceConnection, disconnected: " + className);
            }
            Message messageObtainMessage = BluetoothManagerService.this.mHandler.obtainMessage(41);
            if (className.equals("com.android.bluetooth.btservice.AdapterService")) {
                messageObtainMessage.arg1 = 1;
            } else if (className.equals("com.android.bluetooth.gatt.GattService")) {
                messageObtainMessage.arg1 = 2;
            } else {
                Slog.e(BluetoothManagerService.TAG, "Unknown service disconnected: " + className);
                return;
            }
            BluetoothManagerService.this.mHandler.sendMessage(messageObtainMessage);
        }
    }

    private class BluetoothHandler extends Handler {
        boolean mGetNameAddressOnly;

        BluetoothHandler(Looper looper) {
            super(looper);
            this.mGetNameAddressOnly = false;
        }

        @Override
        public void handleMessage(Message message) {
            int state;
            BluetoothManagerService bluetoothManagerService;
            ReentrantReadWriteLock reentrantReadWriteLock;
            ReentrantReadWriteLock.WriteLock writeLock;
            String name;
            String address;
            boolean z;
            switch (message.what) {
                case 1:
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "MESSAGE_ENABLE(" + message.arg1 + "): mBluetooth = " + BluetoothManagerService.this.mBluetooth);
                    }
                    BluetoothManagerService.this.mHandler.removeMessages(42);
                    BluetoothManagerService.this.mEnable = true;
                    try {
                        try {
                            BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                state = BluetoothManagerService.this.mBluetooth.getState();
                                if (state == 15) {
                                    try {
                                        Slog.w(BluetoothManagerService.TAG, "BT Enable in BLE_ON State, going to ON");
                                        BluetoothManagerService.this.mBluetooth.onLeServiceUp();
                                        BluetoothManagerService.this.persistBluetoothSetting(1);
                                        return;
                                    } catch (RemoteException e) {
                                        e = e;
                                        Slog.e(BluetoothManagerService.TAG, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, e);
                                        BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                                        BluetoothManagerService.this.mQuietEnable = message.arg1 != 1;
                                        if (BluetoothManagerService.this.mBluetooth != null) {
                                        }
                                    }
                                }
                            } else {
                                state = 0;
                            }
                        } finally {
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                        state = 0;
                    }
                    BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
                    BluetoothManagerService.this.mQuietEnable = message.arg1 != 1;
                    if (BluetoothManagerService.this.mBluetooth != null) {
                        BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                        return;
                    } else {
                        if (state == 14 || state == 11 || state == 12) {
                            return;
                        }
                        BluetoothManagerService.this.waitForOnOff(false, true);
                        BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(42), 400L);
                        return;
                    }
                case 2:
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "MESSAGE_DISABLE: mBluetooth = " + BluetoothManagerService.this.mBluetooth);
                    }
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
                    BluetoothManagerService.this.mCallbacks.register((IBluetoothManagerCallback) message.obj);
                    return;
                case 21:
                    BluetoothManagerService.this.mCallbacks.unregister((IBluetoothManagerCallback) message.obj);
                    return;
                case 30:
                    BluetoothManagerService.this.mStateChangeCallbacks.register((IBluetoothStateChangeCallback) message.obj);
                    return;
                case 31:
                    BluetoothManagerService.this.mStateChangeCallbacks.unregister((IBluetoothStateChangeCallback) message.obj);
                    return;
                case 40:
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_SERVICE_CONNECTED: " + message.arg1);
                    }
                    IBinder iBinder = (IBinder) message.obj;
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (message.arg1 == 2) {
                            BluetoothManagerService.this.mBluetoothGatt = IBluetoothGatt.Stub.asInterface(Binder.allowBlocking(iBinder));
                            BluetoothManagerService.this.continueFromBleOnState();
                            return;
                        }
                        BluetoothManagerService.this.mIsUserSwitch = false;
                        BluetoothManagerService.this.mHandler.removeMessages(100);
                        BluetoothManagerService.this.mBinding = false;
                        BluetoothManagerService.this.mBluetoothBinder = iBinder;
                        BluetoothManagerService.this.mBluetooth = IBluetooth.Stub.asInterface(Binder.allowBlocking(iBinder));
                        if (!BluetoothManagerService.this.isNameAndAddressSet()) {
                            BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(200));
                            if (this.mGetNameAddressOnly) {
                                return;
                            }
                        }
                        try {
                            BluetoothManagerService.this.mBluetooth.registerCallback(BluetoothManagerService.this.mBluetoothCallback);
                            break;
                        } catch (RemoteException e3) {
                            Slog.e(BluetoothManagerService.TAG, "Unable to register BluetoothCallback", e3);
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
                        } catch (RemoteException e4) {
                            Slog.e(BluetoothManagerService.TAG, "Unable to call enable()", e4);
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
                    Slog.e(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED(" + message.arg1 + ")");
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (message.arg1 == 1) {
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.mBluetooth = null;
                                BluetoothManagerService.this.mIsUserSwitch = false;
                                BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                                BluetoothManagerService.this.addCrashLog();
                                BluetoothManagerService.this.addActiveLog(7, BluetoothManagerService.this.mContext.getPackageName(), false);
                                if (BluetoothManagerService.this.mEnable) {
                                    BluetoothManagerService.this.mEnable = false;
                                    BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(42), 200L);
                                }
                                BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                                if (BluetoothManagerService.this.mState == 11 || BluetoothManagerService.this.mState == 12) {
                                    BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                                    BluetoothManagerService.this.mState = 13;
                                }
                                if (BluetoothManagerService.this.mState == 13) {
                                    BluetoothManagerService.this.bluetoothStateChangeHandler(13, 15);
                                    BluetoothManagerService.this.mState = 15;
                                }
                                if (BluetoothManagerService.this.mState == 14 || BluetoothManagerService.this.mState == 15) {
                                    BluetoothManagerService.this.bluetoothStateChangeHandler(15, 16);
                                    BluetoothManagerService.this.mState = 16;
                                }
                                if (BluetoothManagerService.this.mState == 16) {
                                    BluetoothManagerService.this.bluetoothStateChangeHandler(16, 10);
                                }
                                BluetoothManagerService.this.mHandler.removeMessages(60);
                                BluetoothManagerService.this.mState = 10;
                                return;
                            }
                        } else if (message.arg1 == 2) {
                            BluetoothManagerService.this.mBluetoothGatt = null;
                        } else {
                            Slog.e(BluetoothManagerService.TAG, "Unknown argument for service disconnect!");
                        }
                        return;
                    } finally {
                    }
                case 42:
                    Slog.d(BluetoothManagerService.TAG, "MESSAGE_RESTART_BLUETOOTH_SERVICE");
                    BluetoothManagerService.this.mEnable = true;
                    BluetoothManagerService.this.addActiveLog(4, BluetoothManagerService.this.mContext.getPackageName(), true);
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                    return;
                case 60:
                    int i = message.arg1;
                    int i2 = message.arg2;
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_STATE_CHANGE: " + BluetoothAdapter.nameForState(i) + " > " + BluetoothAdapter.nameForState(i2));
                    }
                    BluetoothManagerService.this.mState = i2;
                    BluetoothManagerService.this.bluetoothStateChangeHandler(i, i2);
                    if (i == 14 && i2 == 10 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                        BluetoothManagerService.this.recoverBluetoothServiceFromError(false);
                    }
                    if (i == 11 && i2 == 15 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                        BluetoothManagerService.this.recoverBluetoothServiceFromError(true);
                    }
                    if (i == 16 && i2 == 10 && BluetoothManagerService.this.mEnable) {
                        Slog.d(BluetoothManagerService.TAG, "Entering STATE_OFF but mEnabled is true; restarting.");
                        BluetoothManagerService.this.waitForOnOff(false, true);
                        BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(42), 400L);
                    }
                    if ((i2 == 12 || i2 == 15) && BluetoothManagerService.this.mErrorRecoveryRetryCounter != 0) {
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
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "MESSAGE_GET_NAME_AND_ADDRESS");
                    }
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (BluetoothManagerService.this.mBluetooth == null && !BluetoothManagerService.this.mBinding) {
                            if (BluetoothManagerService.DBG) {
                                Slog.d(BluetoothManagerService.TAG, "Binding to service to get name and address");
                            }
                            this.mGetNameAddressOnly = true;
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(BluetoothManagerService.this.mHandler.obtainMessage(100), 3000L);
                            if (BluetoothManagerService.this.doBind(new Intent(IBluetooth.class.getName()), BluetoothManagerService.this.mConnection, 65, UserHandle.CURRENT)) {
                                BluetoothManagerService.this.mBinding = true;
                            } else {
                                BluetoothManagerService.this.mHandler.removeMessages(100);
                            }
                        } else if (BluetoothManagerService.this.mBluetooth != null) {
                            Message messageObtainMessage = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS);
                            messageObtainMessage.arg1 = 0;
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.mHandler.sendMessage(messageObtainMessage);
                            } else {
                                BluetoothManagerService.this.mHandler.sendMessageDelayed(messageObtainMessage, 500L);
                            }
                        }
                        return;
                    } finally {
                    }
                case BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS:
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "MESSAGE_SAVE_NAME_AND_ADDRESS");
                    }
                    try {
                        BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                        if (!BluetoothManagerService.this.mEnable && BluetoothManagerService.this.mBluetooth != null) {
                            try {
                                BluetoothManagerService.this.mBluetooth.enable();
                            } catch (RemoteException e5) {
                                Slog.e(BluetoothManagerService.TAG, "Unable to call enable()", e5);
                            }
                        }
                        BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.waitForBleOn();
                        }
                        try {
                            BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                try {
                                    name = BluetoothManagerService.this.mBluetooth.getName();
                                    try {
                                        address = BluetoothManagerService.this.mBluetooth.getAddress();
                                    } catch (RemoteException e6) {
                                        e = e6;
                                        Slog.e(BluetoothManagerService.TAG, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, e);
                                        address = null;
                                    }
                                } catch (RemoteException e7) {
                                    e = e7;
                                    name = null;
                                }
                                if (name != null && address != null) {
                                    BluetoothManagerService.this.storeNameAndAddress(name, address);
                                    z = this.mGetNameAddressOnly;
                                } else if (message.arg1 < 3) {
                                    Message messageObtainMessage2 = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS);
                                    messageObtainMessage2.arg1 = message.arg1 + 1;
                                    if (BluetoothManagerService.DBG) {
                                        Slog.d(BluetoothManagerService.TAG, "Retrying name/address remote retrieval and save.....Retry count =" + messageObtainMessage2.arg1);
                                    }
                                    BluetoothManagerService.this.mHandler.sendMessageDelayed(messageObtainMessage2, 500L);
                                } else {
                                    Slog.w(BluetoothManagerService.TAG, "Maximum name/address remoteretrieval retry exceeded");
                                    if (this.mGetNameAddressOnly) {
                                    }
                                }
                                if (!BluetoothManagerService.this.mEnable) {
                                    try {
                                        BluetoothManagerService.this.mBluetooth.onBrEdrDown();
                                    } catch (RemoteException e8) {
                                        Slog.e(BluetoothManagerService.TAG, "Unable to call disable()", e8);
                                    }
                                }
                                break;
                            } else {
                                BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(200));
                                z = false;
                            }
                            BluetoothManagerService.this.mBluetoothLock.writeLock().unlock();
                            if (!BluetoothManagerService.this.mEnable && BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.waitForOnOff(false, true);
                            }
                            if (z) {
                                BluetoothManagerService.this.unbindAndFinish();
                            }
                            this.mGetNameAddressOnly = false;
                            return;
                        } finally {
                        }
                    } finally {
                    }
                    break;
                case 300:
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "MESSAGE_USER_SWITCHED");
                    }
                    BluetoothManagerService.this.mHandler.removeMessages(300);
                    BluetoothManagerService.this.mIsUserSwitch = true;
                    if (BluetoothManagerService.this.mBluetooth != null) {
                        try {
                            if (BluetoothManagerService.this.isEnabled()) {
                                try {
                                    BluetoothManagerService.this.mBluetoothLock.readLock().lock();
                                    if (BluetoothManagerService.this.mBluetooth != null) {
                                        BluetoothManagerService.this.mBluetooth.unregisterCallback(BluetoothManagerService.this.mBluetoothCallback);
                                    }
                                } catch (RemoteException e9) {
                                    Slog.e(BluetoothManagerService.TAG, "Unable to unregister", e9);
                                }
                                BluetoothManagerService.this.mBluetoothLock.readLock().unlock();
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
                                BluetoothManagerService.this.addActiveLog(8, BluetoothManagerService.this.mContext.getPackageName(), false);
                                BluetoothManagerService.this.handleDisable();
                                BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                                BluetoothManagerService.this.waitForBleOn();
                                BluetoothManagerService.this.bluetoothStateChangeHandler(13, 15);
                                BluetoothManagerService.this.bluetoothStateChangeHandler(15, 16);
                                boolean z2 = !BluetoothManagerService.this.waitForOnOff(false, true);
                                BluetoothManagerService.this.bluetoothStateChangeHandler(16, 10);
                                BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                                try {
                                    BluetoothManagerService.this.mBluetoothLock.writeLock().lock();
                                    if (BluetoothManagerService.this.mBluetooth != null) {
                                        BluetoothManagerService.this.mBluetooth = null;
                                        BluetoothManagerService.this.mContext.unbindService(BluetoothManagerService.this.mConnection);
                                    }
                                    BluetoothManagerService.this.mBluetoothGatt = null;
                                    if (z2) {
                                        SystemClock.sleep(3000L);
                                    } else {
                                        SystemClock.sleep(300L);
                                    }
                                    BluetoothManagerService.this.mHandler.removeMessages(60);
                                    BluetoothManagerService.this.mState = 10;
                                    BluetoothManagerService.this.addActiveLog(8, BluetoothManagerService.this.mContext.getPackageName(), true);
                                    BluetoothManagerService.this.mEnable = true;
                                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                                    return;
                                } finally {
                                }
                            }
                        } finally {
                        }
                        break;
                    }
                    if (BluetoothManagerService.this.mBinding || BluetoothManagerService.this.mBluetooth != null) {
                        Message messageObtainMessage3 = BluetoothManagerService.this.mHandler.obtainMessage(300);
                        messageObtainMessage3.arg2 = 1 + message.arg2;
                        BluetoothManagerService.this.mHandler.sendMessageDelayed(messageObtainMessage3, 200L);
                        if (BluetoothManagerService.DBG) {
                            Slog.d(BluetoothManagerService.TAG, "Retry MESSAGE_USER_SWITCHED " + messageObtainMessage3.arg2);
                            return;
                        }
                        return;
                    }
                    return;
                case BluetoothManagerService.MESSAGE_USER_UNLOCKED:
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "MESSAGE_USER_UNLOCKED");
                    }
                    BluetoothManagerService.this.mHandler.removeMessages(300);
                    if (!BluetoothManagerService.this.mEnable || BluetoothManagerService.this.mBinding || BluetoothManagerService.this.mBluetooth != null || BluetoothManagerService.this.mIsUserSwitch) {
                        BluetoothManagerService.this.mIsUserSwitch = false;
                        return;
                    }
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "Enabled but not bound; retrying after unlock");
                    }
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                    return;
                case BluetoothManagerService.MESSAGE_ADD_PROXY_DELAYED:
                    ProfileServiceConnections profileServiceConnections = (ProfileServiceConnections) BluetoothManagerService.this.mProfileServices.get(Integer.valueOf(message.arg1));
                    if (profileServiceConnections == null) {
                        return;
                    }
                    profileServiceConnections.addProxy((IBluetoothProfileServiceConnection) message.obj);
                    return;
                case BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE:
                    ProfileServiceConnections profileServiceConnections2 = (ProfileServiceConnections) message.obj;
                    removeMessages(BluetoothManagerService.MESSAGE_BIND_PROFILE_SERVICE, message.obj);
                    if (profileServiceConnections2 == null) {
                        return;
                    }
                    profileServiceConnections2.bindService();
                    return;
                case SystemService.PHASE_SYSTEM_SERVICES_READY:
                    if (message.arg1 == 0 && BluetoothManagerService.this.mEnable) {
                        if (BluetoothManagerService.DBG) {
                            Slog.d(BluetoothManagerService.TAG, "Restore Bluetooth state to disabled");
                        }
                        BluetoothManagerService.this.persistBluetoothSetting(0);
                        BluetoothManagerService.this.mEnableExternal = false;
                        BluetoothManagerService.this.sendDisableMsg(9, BluetoothManagerService.this.mContext.getPackageName());
                        return;
                    }
                    if (message.arg1 != 1 || BluetoothManagerService.this.mEnable) {
                        return;
                    }
                    if (BluetoothManagerService.DBG) {
                        Slog.d(BluetoothManagerService.TAG, "Restore Bluetooth state to enabled");
                    }
                    BluetoothManagerService.this.mQuietEnableExternal = false;
                    BluetoothManagerService.this.mEnableExternal = true;
                    BluetoothManagerService.this.sendEnableMsg(false, 9, BluetoothManagerService.this.mContext.getPackageName());
                    return;
                default:
                    return;
            }
        }
    }

    private void handleEnable(boolean z) {
        this.mQuietEnable = z;
        try {
            this.mBluetoothLock.writeLock().lock();
            if (this.mBluetooth == null && !this.mBinding) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(100), 3000L);
                if (!doBind(new Intent(IBluetooth.class.getName()), this.mConnection, 65, UserHandle.CURRENT)) {
                    this.mHandler.removeMessages(100);
                    this.mIsUserSwitch = false;
                } else {
                    this.mBinding = true;
                }
            } else if (this.mBluetooth != null) {
                this.mIsUserSwitch = false;
                try {
                    if (!this.mQuietEnable) {
                        if (!this.mBluetooth.enable()) {
                            Slog.e(TAG, "IBluetooth.enable() returned false");
                        }
                    } else if (!this.mBluetooth.enableNoAutoConnect()) {
                        Slog.e(TAG, "IBluetooth.enableNoAutoConnect() returned false");
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to call enable()", e);
                }
            }
        } finally {
            this.mBluetoothLock.writeLock().unlock();
        }
    }

    boolean doBind(Intent intent, ServiceConnection serviceConnection, int i, UserHandle userHandle) {
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, serviceConnection, i, userHandle)) {
            Slog.e(TAG, "Fail to bind to: " + intent);
            return false;
        }
        return true;
    }

    private void handleDisable() {
        try {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    if (DBG) {
                        Slog.d(TAG, "Sending off request.");
                    }
                    if (!this.mBluetooth.disable()) {
                        Slog.e(TAG, "IBluetooth.disable() returned false");
                    }
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to call disable()", e);
            }
        } finally {
            this.mBluetoothLock.readLock().unlock();
        }
    }

    private boolean checkIfCallerIsForegroundUser() {
        int callingUserId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        UserInfo profileParent = ((UserManager) this.mContext.getSystemService("user")).getProfileParent(callingUserId);
        int i = profileParent != null ? profileParent.id : -10000;
        int appId = UserHandle.getAppId(callingUid);
        try {
            int currentUser = ActivityManager.getCurrentUser();
            boolean z = callingUserId == currentUser || i == currentUser || appId == 1027 || appId == this.mSystemUiUid;
            if (DBG && !z) {
                Slog.d(TAG, "checkIfCallerIsForegroundUser: valid=" + z + " callingUser=" + callingUserId + " parentUser=" + i + " foregroundUser=" + currentUser);
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void sendBleStateChanged(int i, int i2) {
        if (DBG) {
            Slog.d(TAG, "Sending BLE State Change: " + BluetoothAdapter.nameForState(i) + " > " + BluetoothAdapter.nameForState(i2));
        }
        Intent intent = new Intent("android.bluetooth.adapter.action.BLE_STATE_CHANGED");
        intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", i);
        intent.putExtra("android.bluetooth.adapter.extra.STATE", i2);
        intent.addFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
    }

    private void bluetoothStateChangeHandler(int i, int i2) {
        if (i == i2) {
            return;
        }
        boolean z = true;
        if (i2 == 15 || i2 == 10) {
            boolean z2 = i == 13 && i2 == 15;
            if (i2 == 10) {
                if (DBG) {
                    Slog.d(TAG, "Bluetooth is complete send Service Down");
                }
                sendBluetoothServiceDownCallback();
                unbindAndFinish();
                sendBleStateChanged(i, i2);
            } else if (!z2) {
                if (DBG) {
                    Slog.d(TAG, "Bluetooth is in LE only mode");
                }
                if (this.mBluetoothGatt != null || !this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
                    continueFromBleOnState();
                } else {
                    if (DBG) {
                        Slog.d(TAG, "Binding Bluetooth GATT service");
                    }
                    doBind(new Intent(IBluetoothGatt.class.getName()), this.mConnection, 65, UserHandle.CURRENT);
                }
                sendBleStateChanged(i, i2);
            } else if (z2) {
                if (DBG) {
                    Slog.d(TAG, "Intermediate off, back to LE only mode");
                }
                sendBleStateChanged(i, i2);
                sendBluetoothStateCallback(false);
                sendBrEdrDownCallback();
                i2 = 10;
            }
            z = false;
        } else if (i2 == 12) {
            boolean z3 = i2 == 12;
            this.mEnable = true;
            sendBluetoothStateCallback(z3);
            sendBleStateChanged(i, i2);
        } else if (i2 == 14 || i2 == 16) {
            sendBleStateChanged(i, i2);
            z = false;
        } else if (i2 == 11 || i2 == 13) {
            sendBleStateChanged(i, i2);
        }
        if (z) {
            if (i == 15) {
                i = 10;
            }
            Intent intent = new Intent("android.bluetooth.adapter.action.STATE_CHANGED");
            intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", i);
            intent.putExtra("android.bluetooth.adapter.extra.STATE", i2);
            intent.addFlags(67108864);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
        }
    }

    private boolean waitForOnOff(boolean z, boolean z2) {
        for (int i = 0; i < 10; i++) {
            try {
                try {
                    this.mBluetoothLock.readLock().lock();
                } catch (RemoteException e) {
                    Slog.e(TAG, "getState()", e);
                }
                if (this.mBluetooth == null) {
                    break;
                }
                if (z) {
                    if (this.mBluetooth.getState() == 12) {
                        return true;
                    }
                } else if (z2) {
                    if (this.mBluetooth.getState() == 10) {
                        return true;
                    }
                } else if (this.mBluetooth.getState() != 12) {
                    return true;
                }
                this.mBluetoothLock.readLock().unlock();
                if (z || z2) {
                    SystemClock.sleep(300L);
                } else {
                    SystemClock.sleep(50L);
                }
            } finally {
                this.mBluetoothLock.readLock().unlock();
            }
        }
        Slog.e(TAG, "waitForOnOff time out");
        return false;
    }

    private boolean waitForBleOn() {
        Slog.d(TAG, "waitForBleOn 6s");
        for (int i = 0; i < 10; i++) {
            try {
                try {
                    this.mBluetoothLock.readLock().lock();
                } catch (RemoteException e) {
                    Slog.e(TAG, "getState()", e);
                }
                if (this.mBluetooth == null) {
                    break;
                }
                if (this.mBluetooth.getState() == 15) {
                    this.mBluetoothLock.readLock().unlock();
                    return true;
                }
                this.mBluetoothLock.readLock().unlock();
                SystemClock.sleep(600L);
            } finally {
                this.mBluetoothLock.readLock().unlock();
            }
        }
        Slog.e(TAG, "waitForBleOn time out");
        return false;
    }

    private void sendDisableMsg(int i, String str) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
        addActiveLog(i, str, false);
    }

    private void sendEnableMsg(boolean z, int i, String str) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, z ? 1 : 0, 0));
        addActiveLog(i, str, true);
        this.mLastEnabledTime = SystemClock.elapsedRealtime();
    }

    private void addActiveLog(int i, String str, boolean z) {
        synchronized (this.mActiveLogs) {
            if (this.mActiveLogs.size() > 20) {
                this.mActiveLogs.remove();
            }
            this.mActiveLogs.add(new ActiveLog(i, str, z, System.currentTimeMillis()));
        }
        StatsLog.write_non_chained(67, Binder.getCallingUid(), null, z ? 1 : 2, i, str);
    }

    private void addCrashLog() {
        synchronized (this.mCrashTimestamps) {
            if (this.mCrashTimestamps.size() == 100) {
                this.mCrashTimestamps.removeFirst();
            }
            this.mCrashTimestamps.add(Long.valueOf(System.currentTimeMillis()));
            this.mCrashes++;
        }
    }

    private void recoverBluetoothServiceFromError(boolean z) {
        Slog.e(TAG, "recoverBluetoothServiceFromError");
        try {
            try {
                this.mBluetoothLock.readLock().lock();
                if (this.mBluetooth != null) {
                    this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to unregister", e);
            }
            this.mBluetoothLock.readLock().unlock();
            SystemClock.sleep(500L);
            addActiveLog(5, this.mContext.getPackageName(), false);
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
                if (z) {
                    clearBleApps();
                }
                this.mEnable = false;
                int i = this.mErrorRecoveryRetryCounter;
                this.mErrorRecoveryRetryCounter = i + 1;
                if (i < 6) {
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(42), 3000L);
                }
            } catch (Throwable th) {
                this.mBluetoothLock.writeLock().unlock();
                throw th;
            }
        } catch (Throwable th2) {
            this.mBluetoothLock.readLock().unlock();
            throw th2;
        }
    }

    private boolean isBluetoothDisallowed() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return ((UserManager) this.mContext.getSystemService(UserManager.class)).hasUserRestriction("no_bluetooth", UserHandle.SYSTEM);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void updateOppLauncherComponentState(int i, boolean z) {
        try {
            AppGlobals.getPackageManager().setComponentEnabledSetting(new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity"), z ? 2 : 0, 1, i);
        } catch (Exception e) {
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String[] strArr2 = strArr;
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            return;
        }
        String str = null;
        boolean z = strArr2.length > 0 && strArr2[0].startsWith(PriorityDump.PROTO_ARG);
        if (!z) {
            printWriter.println("Bluetooth Status");
            printWriter.println("  enabled: " + isEnabled());
            printWriter.println("  state: " + BluetoothAdapter.nameForState(this.mState));
            printWriter.println("  address: " + this.mAddress);
            printWriter.println("  name: " + this.mName);
            if (this.mEnable) {
                long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mLastEnabledTime;
                printWriter.println("  time since enabled: " + String.format(Locale.US, "%02d:%02d:%02d.%03d", Integer.valueOf((int) (jElapsedRealtime / AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT)), Integer.valueOf((int) ((jElapsedRealtime / 60000) % 60)), Integer.valueOf((int) ((jElapsedRealtime / 1000) % 60)), Integer.valueOf((int) (jElapsedRealtime % 1000))));
            }
            if (this.mActiveLogs.size() == 0) {
                printWriter.println("\nBluetooth never enabled!");
            } else {
                printWriter.println("\nEnable log:");
                Iterator<ActiveLog> it = this.mActiveLogs.iterator();
                while (it.hasNext()) {
                    printWriter.println("  " + it.next());
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("\nBluetooth crashed ");
            sb.append(this.mCrashes);
            sb.append(" time");
            sb.append(this.mCrashes == 1 ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "s");
            printWriter.println(sb.toString());
            if (this.mCrashes == 100) {
                printWriter.println("(last 100)");
            }
            Iterator<Long> it2 = this.mCrashTimestamps.iterator();
            while (it2.hasNext()) {
                printWriter.println("  " + ((Object) timeToLog(it2.next().longValue())));
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("\n");
            sb2.append(this.mBleApps.size());
            sb2.append(" BLE app");
            sb2.append(this.mBleApps.size() == 1 ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "s");
            sb2.append("registered");
            printWriter.println(sb2.toString());
            Iterator<ClientDeathRecipient> it3 = this.mBleApps.values().iterator();
            while (it3.hasNext()) {
                printWriter.println("  " + it3.next().getPackageName());
            }
            printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            printWriter.flush();
            if (strArr2.length == 0) {
                strArr2 = new String[]{"--print"};
            }
        }
        if (this.mBluetoothBinder == null) {
            str = "Bluetooth Service not connected";
        } else {
            try {
                this.mBluetoothBinder.dump(fileDescriptor, strArr2);
            } catch (RemoteException e) {
                str = "RemoteException while dumping Bluetooth Service";
            }
        }
        if (str == null || z) {
            return;
        }
        printWriter.println(str);
    }

    private static String getEnableDisableReasonString(int i) {
        switch (i) {
            case 1:
                return "APPLICATION_REQUEST";
            case 2:
                return "AIRPLANE_MODE";
            case 3:
                return "DISALLOWED";
            case 4:
                return "RESTARTED";
            case 5:
                return "START_ERROR";
            case 6:
                return "SYSTEM_BOOT";
            case 7:
                return "CRASH";
            case 8:
                return "USER_SWITCH";
            case 9:
                return "RESTORE_USER_SETTING";
            default:
                return "UNKNOWN[" + i + "]";
        }
    }
}
