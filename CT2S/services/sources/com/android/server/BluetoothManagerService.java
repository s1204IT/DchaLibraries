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
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

class BluetoothManagerService extends IBluetoothManager.Stub {
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
    private IBluetoothGatt mBluetoothGatt;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private boolean mEnableExternal;
    private final int mSystemUiUid;
    private boolean mQuietEnable = false;
    private final Map<Integer, ProfileServiceConnections> mProfileServices = new HashMap();
    private final IBluetoothCallback mBluetoothCallback = new IBluetoothCallback.Stub() {
        public void onBluetoothStateChange(int prevState, int newState) throws RemoteException {
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_BLUETOOTH_STATE_CHANGE, prevState, newState);
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED".equals(action)) {
                String newName = intent.getStringExtra("android.bluetooth.adapter.extra.LOCAL_NAME");
                Log.d(BluetoothManagerService.TAG, "Bluetooth Adapter name changed to " + newName);
                if (newName != null) {
                    BluetoothManagerService.this.storeNameAndAddress(newName, null);
                    return;
                }
                return;
            }
            if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                synchronized (BluetoothManagerService.this.mReceiver) {
                    if (BluetoothManagerService.this.isBluetoothPersistedStateOn()) {
                        if (BluetoothManagerService.this.isAirplaneModeOn()) {
                            BluetoothManagerService.this.persistBluetoothSetting(2);
                        } else {
                            BluetoothManagerService.this.persistBluetoothSetting(1);
                        }
                    }
                    if (BluetoothManagerService.this.isAirplaneModeOn()) {
                        BluetoothManagerService.this.sendDisableMsg();
                    } else if (BluetoothManagerService.this.mEnableExternal) {
                        BluetoothManagerService.this.sendEnableMsg(BluetoothManagerService.this.mQuietEnableExternal);
                    }
                }
                return;
            }
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                BluetoothManagerService.this.mHandler.sendMessage(BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_USER_SWITCHED, intent.getIntExtra("android.intent.extra.user_handle", 0), 0));
                return;
            }
            if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                synchronized (BluetoothManagerService.this.mReceiver) {
                    if (BluetoothManagerService.this.mEnableExternal && BluetoothManagerService.this.isBluetoothPersistedStateOnBluetooth()) {
                        Log.d(BluetoothManagerService.TAG, "Auto-enabling Bluetooth.");
                        BluetoothManagerService.this.sendEnableMsg(BluetoothManagerService.this.mQuietEnableExternal);
                    }
                }
                if (!BluetoothManagerService.this.isNameAndAddressSet()) {
                    Log.d(BluetoothManagerService.TAG, "Retrieving Bluetooth Adapter name and address...");
                    BluetoothManagerService.this.getNameAndAddress();
                }
            }
        }
    };
    private BluetoothServiceConnection mConnection = new BluetoothServiceConnection();
    private final BluetoothHandler mHandler = new BluetoothHandler(IoThread.get().getLooper());
    private IBluetooth mBluetooth = null;
    private boolean mBinding = false;
    private boolean mUnbinding = false;
    private boolean mEnable = false;
    private int mState = 10;
    private boolean mQuietEnableExternal = false;
    private String mAddress = null;
    private String mName = null;
    private int mErrorRecoveryRetryCounter = 0;
    private final RemoteCallbackList<IBluetoothManagerCallback> mCallbacks = new RemoteCallbackList<>();
    private final RemoteCallbackList<IBluetoothStateChangeCallback> mStateChangeCallbacks = new RemoteCallbackList<>();

    private void registerForAirplaneMode(IntentFilter filter) {
        ContentResolver resolver = this.mContext.getContentResolver();
        String airplaneModeRadios = Settings.Global.getString(resolver, "airplane_mode_radios");
        Settings.Global.getString(resolver, "airplane_mode_toggleable_radios");
        boolean mIsAirplaneSensitive = airplaneModeRadios == null ? DBG : airplaneModeRadios.contains("bluetooth");
        if (mIsAirplaneSensitive) {
            filter.addAction("android.intent.action.AIRPLANE_MODE");
        }
    }

    BluetoothManagerService(Context context) {
        this.mContext = context;
        this.mEnableExternal = false;
        this.mContentResolver = context.getContentResolver();
        IntentFilter filter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        filter.addAction("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        registerForAirplaneMode(filter);
        filter.setPriority(1000);
        this.mContext.registerReceiver(this.mReceiver, filter);
        loadStoredNameAndAddress();
        if (isBluetoothPersistedStateOn()) {
            this.mEnableExternal = DBG;
        }
        int sysUiUid = -1;
        try {
            sysUiUid = this.mContext.getPackageManager().getPackageUid("com.android.systemui", 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, "Unable to resolve SystemUI's UID.", e);
        }
        this.mSystemUiUid = sysUiUid;
    }

    private final boolean isAirplaneModeOn() {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
            return DBG;
        }
        return false;
    }

    private final boolean isBluetoothPersistedStateOn() {
        if (Settings.Global.getInt(this.mContentResolver, "bluetooth_on", 0) != 0) {
            return DBG;
        }
        return false;
    }

    private final boolean isBluetoothPersistedStateOnBluetooth() {
        if (Settings.Global.getInt(this.mContentResolver, "bluetooth_on", 0) == 1) {
            return DBG;
        }
        return false;
    }

    private void persistBluetoothSetting(int value) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "bluetooth_on", value);
    }

    private boolean isNameAndAddressSet() {
        if (this.mName == null || this.mAddress == null || this.mName.length() <= 0 || this.mAddress.length() <= 0) {
            return false;
        }
        return DBG;
    }

    private void loadStoredNameAndAddress() {
        Log.d(TAG, "Loading stored name and address");
        if (this.mContext.getResources().getBoolean(R.^attr-private.errorMessageBackground) && Settings.Secure.getInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 0) == 0) {
            Log.d(TAG, "invalid bluetooth name and address stored");
            return;
        }
        this.mName = Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME);
        this.mAddress = Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS);
        Log.d(TAG, "Stored bluetooth Name=" + this.mName + ",Address=" + this.mAddress);
    }

    private void storeNameAndAddress(String name, String address) {
        if (name != null) {
            Settings.Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME, name);
            this.mName = name;
            Log.d(TAG, "Stored Bluetooth name: " + Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_NAME));
        }
        if (address != null) {
            Settings.Secure.putString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS, address);
            this.mAddress = address;
            Log.d(TAG, "Stored Bluetoothaddress: " + Settings.Secure.getString(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDRESS));
        }
        if (name != null && address != null) {
            Settings.Secure.putInt(this.mContentResolver, SECURE_SETTINGS_BLUETOOTH_ADDR_VALID, 1);
        }
    }

    public IBluetooth registerAdapter(IBluetoothManagerCallback callback) {
        IBluetooth iBluetooth;
        if (callback == null) {
            Log.w(TAG, "Callback is null in registerAdapter");
            return null;
        }
        Message msg = this.mHandler.obtainMessage(20);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
        synchronized (this.mConnection) {
            iBluetooth = this.mBluetooth;
        }
        return iBluetooth;
    }

    public void unregisterAdapter(IBluetoothManagerCallback callback) {
        if (callback == null) {
            Log.w(TAG, "Callback is null in unregisterAdapter");
            return;
        }
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Message msg = this.mHandler.obtainMessage(21);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
    }

    public void registerStateChangeCallback(IBluetoothStateChangeCallback callback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Message msg = this.mHandler.obtainMessage(30);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
    }

    public void unregisterStateChangeCallback(IBluetoothStateChangeCallback callback) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Message msg = this.mHandler.obtainMessage(31);
        msg.obj = callback;
        this.mHandler.sendMessage(msg);
    }

    public boolean isEnabled() {
        boolean z = false;
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Log.w(TAG, "isEnabled(): not allowed for non-active and non system user");
        } else {
            synchronized (this.mConnection) {
                try {
                    if (this.mBluetooth != null) {
                        if (this.mBluetooth.isEnabled()) {
                            z = DBG;
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "isEnabled()", e);
                }
            }
        }
        return z;
    }

    public void getNameAndAddress() {
        Log.d(TAG, "getNameAndAddress(): mBluetooth = " + this.mBluetooth + " mBinding = " + this.mBinding);
        Message msg = this.mHandler.obtainMessage(200);
        this.mHandler.sendMessage(msg);
    }

    public boolean enableNoAutoConnect() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        Log.d(TAG, "enableNoAutoConnect():  mBluetooth =" + this.mBluetooth + " mBinding = " + this.mBinding);
        int callingAppId = UserHandle.getAppId(Binder.getCallingUid());
        if (callingAppId != 1027) {
            throw new SecurityException("no permission to enable Bluetooth quietly");
        }
        synchronized (this.mReceiver) {
            this.mQuietEnableExternal = DBG;
            this.mEnableExternal = DBG;
            sendEnableMsg(DBG);
        }
        return DBG;
    }

    public boolean enable() {
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Log.w(TAG, "enable(): not allowed for non-active and non system user");
            return false;
        }
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        Log.d(TAG, "enable():  mBluetooth =" + this.mBluetooth + " mBinding = " + this.mBinding);
        synchronized (this.mReceiver) {
            this.mQuietEnableExternal = false;
            this.mEnableExternal = DBG;
            long callingIdentity = Binder.clearCallingIdentity();
            persistBluetoothSetting(1);
            Binder.restoreCallingIdentity(callingIdentity);
            sendEnableMsg(false);
        }
        return DBG;
    }

    public boolean disable(boolean persist) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permissicacheNameAndAddresson");
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Log.w(TAG, "disable(): not allowed for non-active and non system user");
            return false;
        }
        Log.d(TAG, "disable(): mBluetooth = " + this.mBluetooth + " mBinding = " + this.mBinding);
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
        return DBG;
    }

    public void unbindAndFinish() {
        Log.d(TAG, "unbindAndFinish(): " + this.mBluetooth + " mBinding = " + this.mBinding);
        synchronized (this.mConnection) {
            if (!this.mUnbinding) {
                this.mUnbinding = DBG;
                if (this.mBluetooth != null) {
                    if (!this.mConnection.isGetNameAddressOnly()) {
                        try {
                            this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
                        } catch (RemoteException re) {
                            Log.e(TAG, "Unable to unregister BluetoothCallback", re);
                        }
                    }
                    Log.d(TAG, "Sending unbind request.");
                    this.mBluetooth = null;
                    this.mContext.unbindService(this.mConnection);
                    this.mUnbinding = false;
                    this.mBinding = false;
                } else {
                    this.mUnbinding = false;
                }
            }
        }
    }

    public IBluetoothGatt getBluetoothGatt() {
        return this.mBluetoothGatt;
    }

    public boolean bindBluetoothProfileService(int bluetoothProfile, IBluetoothProfileServiceConnection proxy) {
        if (!this.mEnable) {
            Log.d(TAG, "Trying to bind to profile: " + bluetoothProfile + ", while Bluetooth was disabled");
            return false;
        }
        synchronized (this.mProfileServices) {
            if (this.mProfileServices.get(new Integer(bluetoothProfile)) == null) {
                Log.d(TAG, "Creating new ProfileServiceConnections object for profile: " + bluetoothProfile);
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
            return DBG;
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
                    Log.e(TAG, "Unable to unbind service with intent: " + psc.mIntent, e);
                }
                psc.removeAllProxies();
            }
            this.mProfileServices.clear();
        }
    }

    private final class ProfileServiceConnections implements ServiceConnection, IBinder.DeathRecipient {
        Intent mIntent;
        final RemoteCallbackList<IBluetoothProfileServiceConnection> mProxies = new RemoteCallbackList<>();
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
                return BluetoothManagerService.DBG;
            }
            Log.w(BluetoothManagerService.TAG, "Unable to bind with intent: " + this.mIntent);
            return false;
        }

        private void addProxy(IBluetoothProfileServiceConnection proxy) {
            this.mProxies.register(proxy);
            if (this.mService == null) {
                if (!BluetoothManagerService.this.mHandler.hasMessages(401, this)) {
                    Message msg = BluetoothManagerService.this.mHandler.obtainMessage(401);
                    msg.obj = this;
                    BluetoothManagerService.this.mHandler.sendMessage(msg);
                    return;
                }
                return;
            }
            try {
                proxy.onServiceConnected(this.mClassName, this.mService);
            } catch (RemoteException e) {
                Log.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e);
            }
        }

        private void removeProxy(IBluetoothProfileServiceConnection proxy) {
            if (proxy != null) {
                if (this.mProxies.unregister(proxy)) {
                    try {
                        proxy.onServiceDisconnected(this.mClassName);
                        return;
                    } catch (RemoteException e) {
                        Log.e(BluetoothManagerService.TAG, "Unable to disconnect proxy", e);
                        return;
                    }
                }
                return;
            }
            Log.w(BluetoothManagerService.TAG, "Trying to remove a null proxy");
        }

        private void removeAllProxies() {
            onServiceDisconnected(this.mClassName);
            this.mProxies.kill();
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothManagerService.this.mHandler.removeMessages(401, this);
            this.mService = service;
            this.mClassName = className;
            try {
                this.mService.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Log.e(BluetoothManagerService.TAG, "Unable to linkToDeath", e);
            }
            int n = this.mProxies.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    this.mProxies.getBroadcastItem(i).onServiceConnected(className, service);
                } catch (RemoteException e2) {
                    Log.e(BluetoothManagerService.TAG, "Unable to connect to proxy", e2);
                }
            }
            this.mProxies.finishBroadcast();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (this.mService != null) {
                this.mService.unlinkToDeath(this, 0);
                this.mService = null;
                this.mClassName = null;
                int n = this.mProxies.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    try {
                        this.mProxies.getBroadcastItem(i).onServiceDisconnected(className);
                    } catch (RemoteException e) {
                        Log.e(BluetoothManagerService.TAG, "Unable to disconnect from proxy", e);
                    }
                }
                this.mProxies.finishBroadcast();
            }
        }

        @Override
        public void binderDied() {
            Log.w(BluetoothManagerService.TAG, "Profile service for profile: " + this.mClassName + " died.");
            onServiceDisconnected(this.mClassName);
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(401);
            msg.obj = this;
            BluetoothManagerService.this.mHandler.sendMessageDelayed(msg, 3000L);
        }
    }

    private void sendBluetoothStateCallback(boolean isUp) {
        int n = this.mStateChangeCallbacks.beginBroadcast();
        Log.d(TAG, "Broadcasting onBluetoothStateChange(" + isUp + ") to " + n + " receivers.");
        for (int i = 0; i < n; i++) {
            try {
                this.mStateChangeCallbacks.getBroadcastItem(i).onBluetoothStateChange(isUp);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to call onBluetoothStateChange() on callback #" + i, e);
            }
        }
        this.mStateChangeCallbacks.finishBroadcast();
    }

    private void sendBluetoothServiceUpCallback() {
        if (!this.mConnection.isGetNameAddressOnly()) {
            Log.d(TAG, "Calling onBluetoothServiceUp callbacks");
            int n = this.mCallbacks.beginBroadcast();
            Log.d(TAG, "Broadcasting onBluetoothServiceUp() to " + n + " receivers.");
            for (int i = 0; i < n; i++) {
                try {
                    this.mCallbacks.getBroadcastItem(i).onBluetoothServiceUp(this.mBluetooth);
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to call onBluetoothServiceUp() on callback #" + i, e);
                }
            }
            this.mCallbacks.finishBroadcast();
        }
    }

    private void sendBluetoothServiceDownCallback() {
        if (!this.mConnection.isGetNameAddressOnly()) {
            Log.d(TAG, "Calling onBluetoothServiceDown callbacks");
            int n = this.mCallbacks.beginBroadcast();
            Log.d(TAG, "Broadcasting onBluetoothServiceDown() to " + n + " receivers.");
            for (int i = 0; i < n; i++) {
                try {
                    this.mCallbacks.getBroadcastItem(i).onBluetoothServiceDown();
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to call onBluetoothServiceDown() on callback #" + i, e);
                }
            }
            this.mCallbacks.finishBroadcast();
        }
    }

    public String getAddress() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Log.w(TAG, "getAddress(): not allowed for non-active and non system user");
            return null;
        }
        synchronized (this.mConnection) {
            if (this.mBluetooth != null) {
                try {
                    return this.mBluetooth.getAddress();
                } catch (RemoteException e) {
                    Log.e(TAG, "getAddress(): Unable to retrieve address remotely..Returning cached address", e);
                }
            }
            return this.mAddress;
        }
    }

    public String getName() {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (Binder.getCallingUid() != 1000 && !checkIfCallerIsForegroundUser()) {
            Log.w(TAG, "getName(): not allowed for non-active and non system user");
            return null;
        }
        synchronized (this.mConnection) {
            if (this.mBluetooth != null) {
                try {
                    return this.mBluetooth.getName();
                } catch (RemoteException e) {
                    Log.e(TAG, "getName(): Unable to retrieve name remotely..Returning cached name", e);
                }
            }
            return this.mName;
        }
    }

    private class BluetoothServiceConnection implements ServiceConnection {
        private boolean mGetNameAddressOnly;

        private BluetoothServiceConnection() {
        }

        public void setGetNameAddressOnly(boolean getOnly) {
            this.mGetNameAddressOnly = getOnly;
        }

        public boolean isGetNameAddressOnly() {
            return this.mGetNameAddressOnly;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(BluetoothManagerService.TAG, "BluetoothServiceConnection: " + className.getClassName());
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(40);
            if (className.getClassName().equals("com.android.bluetooth.btservice.AdapterService")) {
                msg.arg1 = 1;
            } else if (className.getClassName().equals("com.android.bluetooth.gatt.GattService")) {
                msg.arg1 = 2;
            } else {
                Log.e(BluetoothManagerService.TAG, "Unknown service connected: " + className.getClassName());
                return;
            }
            msg.obj = service;
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(BluetoothManagerService.TAG, "BluetoothServiceConnection, disconnected: " + className.getClassName());
            Message msg = BluetoothManagerService.this.mHandler.obtainMessage(41);
            if (className.getClassName().equals("com.android.bluetooth.btservice.AdapterService")) {
                msg.arg1 = 1;
            } else if (className.getClassName().equals("com.android.bluetooth.gatt.GattService")) {
                msg.arg1 = 2;
            } else {
                Log.e(BluetoothManagerService.TAG, "Unknown service disconnected: " + className.getClassName());
                return;
            }
            BluetoothManagerService.this.mHandler.sendMessage(msg);
        }
    }

    private class BluetoothHandler extends Handler {
        public BluetoothHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            boolean enableHciSnoopLog;
            Log.d(BluetoothManagerService.TAG, "Message: " + msg.what);
            switch (msg.what) {
                case 1:
                    Log.d(BluetoothManagerService.TAG, "MESSAGE_ENABLE: mBluetooth = " + BluetoothManagerService.this.mBluetooth);
                    BluetoothManagerService.this.mHandler.removeMessages(42);
                    BluetoothManagerService.this.mEnable = BluetoothManagerService.DBG;
                    BluetoothManagerService.this.handleEnable(msg.arg1 == 1 ? BluetoothManagerService.DBG : false);
                    return;
                case 2:
                    BluetoothManagerService.this.mHandler.removeMessages(42);
                    if (!BluetoothManagerService.this.mEnable || BluetoothManagerService.this.mBluetooth == null) {
                        BluetoothManagerService.this.mEnable = false;
                        BluetoothManagerService.this.handleDisable();
                        return;
                    } else {
                        BluetoothManagerService.this.waitForOnOff(BluetoothManagerService.DBG, false);
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
                    Log.d(BluetoothManagerService.TAG, sbAppend.append((Object) str).append(":").append(added).toString());
                    return;
                case 21:
                    String str2 = (IBluetoothManagerCallback) msg.obj;
                    boolean removed = BluetoothManagerService.this.mCallbacks.unregister(str2);
                    StringBuilder sbAppend2 = new StringBuilder().append("Removed callback: ");
                    if (str2 == null) {
                        str2 = "null";
                    }
                    Log.d(BluetoothManagerService.TAG, sbAppend2.append((Object) str2).append(":").append(removed).toString());
                    return;
                case 30:
                    IBluetoothStateChangeCallback callback = (IBluetoothStateChangeCallback) msg.obj;
                    if (callback != null) {
                        BluetoothManagerService.this.mStateChangeCallbacks.register(callback);
                        return;
                    }
                    return;
                case 31:
                    IBluetoothStateChangeCallback callback2 = (IBluetoothStateChangeCallback) msg.obj;
                    if (callback2 != null) {
                        BluetoothManagerService.this.mStateChangeCallbacks.unregister(callback2);
                        return;
                    }
                    return;
                case 40:
                    Log.d(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_SERVICE_CONNECTED: " + msg.arg1);
                    IBinder service = (IBinder) msg.obj;
                    synchronized (BluetoothManagerService.this.mConnection) {
                        if (msg.arg1 == 2) {
                            BluetoothManagerService.this.mBluetoothGatt = IBluetoothGatt.Stub.asInterface(service);
                            return;
                        }
                        BluetoothManagerService.this.mHandler.removeMessages(100);
                        BluetoothManagerService.this.mBinding = false;
                        BluetoothManagerService.this.mBluetooth = IBluetooth.Stub.asInterface(service);
                        try {
                            enableHciSnoopLog = Settings.Secure.getInt(BluetoothManagerService.this.mContentResolver, "bluetooth_hci_log", 0) == 1 ? BluetoothManagerService.DBG : false;
                        } catch (RemoteException e) {
                            Log.e(BluetoothManagerService.TAG, "Unable to call configHciSnoopLog", e);
                        }
                        if (BluetoothManagerService.this.mBluetooth.configHciSnoopLog(enableHciSnoopLog)) {
                            if (BluetoothManagerService.this.mConnection.isGetNameAddressOnly()) {
                            }
                            BluetoothManagerService.this.mConnection.setGetNameAddressOnly(false);
                            BluetoothManagerService.this.mBluetooth.registerCallback(BluetoothManagerService.this.mBluetoothCallback);
                            BluetoothManagerService.this.sendBluetoothServiceUpCallback();
                            if (BluetoothManagerService.this.mQuietEnable) {
                            }
                            if (BluetoothManagerService.this.mEnable) {
                            }
                        } else {
                            Log.e(BluetoothManagerService.TAG, "IBluetooth.configHciSnoopLog return false");
                            if (BluetoothManagerService.this.mConnection.isGetNameAddressOnly()) {
                                Message getMsg = BluetoothManagerService.this.mHandler.obtainMessage(200);
                                BluetoothManagerService.this.mHandler.sendMessage(getMsg);
                                if (!BluetoothManagerService.this.mEnable) {
                                    return;
                                }
                            }
                            BluetoothManagerService.this.mConnection.setGetNameAddressOnly(false);
                            try {
                                BluetoothManagerService.this.mBluetooth.registerCallback(BluetoothManagerService.this.mBluetoothCallback);
                                break;
                            } catch (RemoteException re) {
                                Log.e(BluetoothManagerService.TAG, "Unable to register BluetoothCallback", re);
                            }
                            BluetoothManagerService.this.sendBluetoothServiceUpCallback();
                            try {
                                if (BluetoothManagerService.this.mQuietEnable) {
                                    if (!BluetoothManagerService.this.mBluetooth.enable()) {
                                        Log.e(BluetoothManagerService.TAG, "IBluetooth.enable() returned false");
                                    }
                                } else if (!BluetoothManagerService.this.mBluetooth.enableNoAutoConnect()) {
                                    Log.e(BluetoothManagerService.TAG, "IBluetooth.enableNoAutoConnect() returned false");
                                }
                                break;
                            } catch (RemoteException e2) {
                                Log.e(BluetoothManagerService.TAG, "Unable to call enable()", e2);
                            }
                            if (BluetoothManagerService.this.mEnable) {
                                BluetoothManagerService.this.waitForOnOff(BluetoothManagerService.DBG, false);
                                BluetoothManagerService.this.handleDisable();
                                BluetoothManagerService.this.waitForOnOff(false, false);
                                return;
                            }
                            return;
                        }
                    }
                case 41:
                    Log.e(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_SERVICE_DISCONNECTED: " + msg.arg1);
                    synchronized (BluetoothManagerService.this.mConnection) {
                        if (msg.arg1 == 1) {
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.mBluetooth = null;
                                if (BluetoothManagerService.this.mEnable) {
                                    BluetoothManagerService.this.mEnable = false;
                                    Message restartMsg = BluetoothManagerService.this.mHandler.obtainMessage(42);
                                    BluetoothManagerService.this.mHandler.sendMessageDelayed(restartMsg, 200L);
                                }
                                if (!BluetoothManagerService.this.mConnection.isGetNameAddressOnly()) {
                                    BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                                    if (BluetoothManagerService.this.mState == 11 || BluetoothManagerService.this.mState == 12) {
                                        BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                                        BluetoothManagerService.this.mState = 13;
                                    }
                                    if (BluetoothManagerService.this.mState == 13) {
                                        BluetoothManagerService.this.bluetoothStateChangeHandler(13, 10);
                                    }
                                    BluetoothManagerService.this.mHandler.removeMessages(BluetoothManagerService.MESSAGE_BLUETOOTH_STATE_CHANGE);
                                    BluetoothManagerService.this.mState = 10;
                                }
                            }
                        } else if (msg.arg1 == 2) {
                            BluetoothManagerService.this.mBluetoothGatt = null;
                        } else {
                            Log.e(BluetoothManagerService.TAG, "Bad msg.arg1: " + msg.arg1);
                        }
                    }
                    return;
                case 42:
                    Log.d(BluetoothManagerService.TAG, "MESSAGE_RESTART_BLUETOOTH_SERVICE: Restart IBluetooth service");
                    BluetoothManagerService.this.mEnable = BluetoothManagerService.DBG;
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
                    return;
                case BluetoothManagerService.MESSAGE_BLUETOOTH_STATE_CHANGE:
                    int prevState = msg.arg1;
                    int newState = msg.arg2;
                    Log.d(BluetoothManagerService.TAG, "MESSAGE_BLUETOOTH_STATE_CHANGE: prevState = " + prevState + ", newState=" + newState);
                    BluetoothManagerService.this.mState = newState;
                    BluetoothManagerService.this.bluetoothStateChangeHandler(prevState, newState);
                    if (prevState == 11 && newState == 10 && BluetoothManagerService.this.mBluetooth != null && BluetoothManagerService.this.mEnable) {
                        BluetoothManagerService.this.recoverBluetoothServiceFromError();
                    }
                    if (newState == 12 && BluetoothManagerService.this.mErrorRecoveryRetryCounter != 0) {
                        Log.w(BluetoothManagerService.TAG, "bluetooth is recovered from error");
                        BluetoothManagerService.this.mErrorRecoveryRetryCounter = 0;
                        return;
                    }
                    return;
                case 100:
                    Log.e(BluetoothManagerService.TAG, "MESSAGE_TIMEOUT_BIND");
                    synchronized (BluetoothManagerService.this.mConnection) {
                        BluetoothManagerService.this.mBinding = false;
                        break;
                    }
                    return;
                case 101:
                    Log.e(BluetoothManagerService.TAG, "MESSAGE_TIMEOUT_UNBIND");
                    synchronized (BluetoothManagerService.this.mConnection) {
                        BluetoothManagerService.this.mUnbinding = false;
                        break;
                    }
                    return;
                case 200:
                    Log.d(BluetoothManagerService.TAG, "MESSAGE_GET_NAME_AND_ADDRESS");
                    synchronized (BluetoothManagerService.this.mConnection) {
                        if (BluetoothManagerService.this.mBluetooth != null || BluetoothManagerService.this.mBinding) {
                            Message saveMsg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS);
                            saveMsg.arg1 = 0;
                            if (BluetoothManagerService.this.mBluetooth != null) {
                                BluetoothManagerService.this.mHandler.sendMessage(saveMsg);
                            } else {
                                BluetoothManagerService.this.mHandler.sendMessageDelayed(saveMsg, 500L);
                            }
                        } else {
                            Log.d(BluetoothManagerService.TAG, "Binding to service to get name and address");
                            BluetoothManagerService.this.mConnection.setGetNameAddressOnly(BluetoothManagerService.DBG);
                            Message timeoutMsg = BluetoothManagerService.this.mHandler.obtainMessage(100);
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(timeoutMsg, 3000L);
                            Intent i = new Intent(IBluetooth.class.getName());
                            if (!BluetoothManagerService.this.doBind(i, BluetoothManagerService.this.mConnection, 65, UserHandle.CURRENT)) {
                                BluetoothManagerService.this.mHandler.removeMessages(100);
                            } else {
                                BluetoothManagerService.this.mBinding = BluetoothManagerService.DBG;
                            }
                        }
                        break;
                    }
                    return;
                case BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS:
                    boolean unbind = false;
                    Log.d(BluetoothManagerService.TAG, "MESSAGE_SAVE_NAME_AND_ADDRESS");
                    synchronized (BluetoothManagerService.this.mConnection) {
                        if (!BluetoothManagerService.this.mEnable && BluetoothManagerService.this.mBluetooth != null) {
                            try {
                                BluetoothManagerService.this.mBluetooth.enable();
                            } catch (RemoteException e3) {
                                Log.e(BluetoothManagerService.TAG, "Unable to call enable()", e3);
                            }
                            break;
                        } else {
                            break;
                        }
                    }
                    if (BluetoothManagerService.this.mBluetooth != null) {
                        BluetoothManagerService.this.waitForOnOff(BluetoothManagerService.DBG, false);
                    }
                    synchronized (BluetoothManagerService.this.mConnection) {
                        if (BluetoothManagerService.this.mBluetooth == null) {
                            Message getMsg2 = BluetoothManagerService.this.mHandler.obtainMessage(200);
                            BluetoothManagerService.this.mHandler.sendMessage(getMsg2);
                        } else {
                            String name = null;
                            String address = null;
                            try {
                                name = BluetoothManagerService.this.mBluetooth.getName();
                                address = BluetoothManagerService.this.mBluetooth.getAddress();
                                break;
                            } catch (RemoteException re2) {
                                Log.e(BluetoothManagerService.TAG, "", re2);
                            }
                            if (name != null && address != null) {
                                BluetoothManagerService.this.storeNameAndAddress(name, address);
                                if (BluetoothManagerService.this.mConnection.isGetNameAddressOnly()) {
                                    unbind = BluetoothManagerService.DBG;
                                }
                            } else if (msg.arg1 < 3) {
                                Message retryMsg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_SAVE_NAME_AND_ADDRESS);
                                retryMsg.arg1 = msg.arg1 + 1;
                                Log.d(BluetoothManagerService.TAG, "Retrying name/address remote retrieval and save.....Retry count =" + retryMsg.arg1);
                                BluetoothManagerService.this.mHandler.sendMessageDelayed(retryMsg, 500L);
                            } else {
                                Log.w(BluetoothManagerService.TAG, "Maximum name/address remote retrieval retry exceeded");
                                if (BluetoothManagerService.this.mConnection.isGetNameAddressOnly()) {
                                    unbind = BluetoothManagerService.DBG;
                                }
                            }
                            if (!BluetoothManagerService.this.mEnable) {
                                try {
                                    BluetoothManagerService.this.mBluetooth.disable();
                                } catch (RemoteException e4) {
                                    Log.e(BluetoothManagerService.TAG, "Unable to call disable()", e4);
                                }
                                break;
                            }
                        }
                        break;
                    }
                    if (!BluetoothManagerService.this.mEnable && BluetoothManagerService.this.mBluetooth != null) {
                        BluetoothManagerService.this.waitForOnOff(false, BluetoothManagerService.DBG);
                    }
                    if (unbind) {
                        BluetoothManagerService.this.unbindAndFinish();
                        return;
                    }
                    return;
                case BluetoothManagerService.MESSAGE_USER_SWITCHED:
                    Log.d(BluetoothManagerService.TAG, "MESSAGE_USER_SWITCHED");
                    BluetoothManagerService.this.mHandler.removeMessages(BluetoothManagerService.MESSAGE_USER_SWITCHED);
                    if (!BluetoothManagerService.this.mEnable || BluetoothManagerService.this.mBluetooth == null) {
                        if (BluetoothManagerService.this.mBinding || BluetoothManagerService.this.mBluetooth != null) {
                            Message userMsg = BluetoothManagerService.this.mHandler.obtainMessage(BluetoothManagerService.MESSAGE_USER_SWITCHED);
                            userMsg.arg2 = msg.arg2 + 1;
                            BluetoothManagerService.this.mHandler.sendMessageDelayed(userMsg, 200L);
                            Log.d(BluetoothManagerService.TAG, "delay MESSAGE_USER_SWITCHED " + userMsg.arg2);
                            return;
                        }
                        return;
                    }
                    synchronized (BluetoothManagerService.this.mConnection) {
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            try {
                                BluetoothManagerService.this.mBluetooth.unregisterCallback(BluetoothManagerService.this.mBluetoothCallback);
                            } catch (RemoteException re3) {
                                Log.e(BluetoothManagerService.TAG, "Unable to unregister", re3);
                            }
                            break;
                        } else {
                            break;
                        }
                    }
                    if (BluetoothManagerService.this.mState == 13) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 10);
                        BluetoothManagerService.this.mState = 10;
                    }
                    if (BluetoothManagerService.this.mState == 10) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 11);
                        BluetoothManagerService.this.mState = 11;
                    }
                    BluetoothManagerService.this.waitForOnOff(BluetoothManagerService.DBG, false);
                    if (BluetoothManagerService.this.mState == 11) {
                        BluetoothManagerService.this.bluetoothStateChangeHandler(BluetoothManagerService.this.mState, 12);
                    }
                    BluetoothManagerService.this.unbindAllBluetoothProfileServices();
                    BluetoothManagerService.this.handleDisable();
                    BluetoothManagerService.this.bluetoothStateChangeHandler(12, 13);
                    BluetoothManagerService.this.waitForOnOff(false, BluetoothManagerService.DBG);
                    BluetoothManagerService.this.bluetoothStateChangeHandler(13, 10);
                    BluetoothManagerService.this.sendBluetoothServiceDownCallback();
                    synchronized (BluetoothManagerService.this.mConnection) {
                        if (BluetoothManagerService.this.mBluetooth != null) {
                            BluetoothManagerService.this.mBluetooth = null;
                            BluetoothManagerService.this.mContext.unbindService(BluetoothManagerService.this.mConnection);
                        }
                        break;
                    }
                    SystemClock.sleep(100L);
                    BluetoothManagerService.this.mHandler.removeMessages(BluetoothManagerService.MESSAGE_BLUETOOTH_STATE_CHANGE);
                    BluetoothManagerService.this.mState = 10;
                    BluetoothManagerService.this.handleEnable(BluetoothManagerService.this.mQuietEnable);
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
                default:
                    return;
            }
        }
    }

    private void handleEnable(boolean quietMode) {
        this.mQuietEnable = quietMode;
        synchronized (this.mConnection) {
            if (this.mBluetooth == null && !this.mBinding) {
                Message timeoutMsg = this.mHandler.obtainMessage(100);
                this.mHandler.sendMessageDelayed(timeoutMsg, 3000L);
                this.mConnection.setGetNameAddressOnly(false);
                Intent i = new Intent(IBluetooth.class.getName());
                if (!doBind(i, this.mConnection, 65, UserHandle.CURRENT)) {
                    this.mHandler.removeMessages(100);
                } else {
                    this.mBinding = DBG;
                }
            } else {
                if (this.mBluetooth != null) {
                    if (this.mConnection.isGetNameAddressOnly()) {
                        this.mConnection.setGetNameAddressOnly(false);
                        try {
                            this.mBluetooth.registerCallback(this.mBluetoothCallback);
                        } catch (RemoteException re) {
                            Log.e(TAG, "Unable to register BluetoothCallback", re);
                        }
                        sendBluetoothServiceUpCallback();
                        try {
                            if (this.mQuietEnable) {
                                if (!this.mBluetooth.enable()) {
                                    Log.e(TAG, "IBluetooth.enable() returned false");
                                }
                            } else if (!this.mBluetooth.enableNoAutoConnect()) {
                                Log.e(TAG, "IBluetooth.enableNoAutoConnect() returned false");
                            }
                        } catch (RemoteException e) {
                            Log.e(TAG, "Unable to call enable()", e);
                        }
                    } else if (this.mQuietEnable) {
                    }
                }
            }
        }
    }

    boolean doBind(Intent intent, ServiceConnection conn, int flags, UserHandle user) {
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, conn, flags, user)) {
            return DBG;
        }
        Log.e(TAG, "Fail to bind to: " + intent);
        return false;
    }

    private void handleDisable() {
        synchronized (this.mConnection) {
            if (this.mBluetooth != null && !this.mConnection.isGetNameAddressOnly()) {
                Log.d(TAG, "Sending off request.");
                try {
                } catch (RemoteException e) {
                    Log.e(TAG, "Unable to call disable()", e);
                }
                if (!this.mBluetooth.disable()) {
                    Log.e(TAG, "IBluetooth.disable() returned false");
                }
            }
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
            boolean valid = (callingUser == foregroundUser || parentUser == foregroundUser || callingAppId == 1027 || callingAppId == this.mSystemUiUid) ? DBG : false;
            Log.d(TAG, "checkIfCallerIsForegroundUser: valid=" + valid + " callingUser=" + callingUser + " parentUser=" + parentUser + " foregroundUser=" + foregroundUser);
            return valid;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private void bluetoothStateChangeHandler(int prevState, int newState) {
        if (prevState != newState) {
            if (newState == 12 || newState == 10) {
                boolean isUp = newState == 12 ? DBG : false;
                sendBluetoothStateCallback(isUp);
                if (isUp) {
                    if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
                        Intent i = new Intent(IBluetoothGatt.class.getName());
                        doBind(i, this.mConnection, 65, UserHandle.CURRENT);
                    }
                } else if (!isUp && canUnbindBluetoothService()) {
                    unbindAllBluetoothProfileServices();
                    sendBluetoothServiceDownCallback();
                    unbindAndFinish();
                }
            }
            Intent intent = new Intent("android.bluetooth.adapter.action.STATE_CHANGED");
            intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_STATE", prevState);
            intent.putExtra("android.bluetooth.adapter.extra.STATE", newState);
            intent.addFlags(67108864);
            Log.d(TAG, "Bluetooth State Change Intent: " + prevState + " -> " + newState);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_PERM);
        }
    }

    private boolean waitForOnOff(boolean on, boolean off) {
        int i = 0;
        while (true) {
            if (i >= 10) {
                break;
            }
            synchronized (this.mConnection) {
                try {
                    if (this.mBluetooth != null) {
                        if (on) {
                            if (this.mBluetooth.getState() == 12) {
                                return DBG;
                            }
                        } else if (off) {
                            if (this.mBluetooth.getState() == 10) {
                                return DBG;
                            }
                        } else if (this.mBluetooth.getState() != 12) {
                            return DBG;
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "getState()", e);
                }
            }
            break;
            i++;
        }
        Log.e(TAG, "waitForOnOff time out");
        return false;
    }

    private void sendDisableMsg() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
    }

    private void sendEnableMsg(boolean quietMode) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, quietMode ? 1 : 0, 0));
    }

    private boolean canUnbindBluetoothService() {
        synchronized (this.mConnection) {
            try {
                if (!this.mEnable && this.mBluetooth != null) {
                    if (!this.mHandler.hasMessages(MESSAGE_BLUETOOTH_STATE_CHANGE)) {
                        z = this.mBluetooth.getState() == 10 ? DBG : false;
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "getState()", e);
            }
        }
        return z;
    }

    private void recoverBluetoothServiceFromError() {
        Log.e(TAG, "recoverBluetoothServiceFromError");
        synchronized (this.mConnection) {
            if (this.mBluetooth != null) {
                try {
                    this.mBluetooth.unregisterCallback(this.mBluetoothCallback);
                } catch (RemoteException re) {
                    Log.e(TAG, "Unable to unregister", re);
                }
            }
        }
        SystemClock.sleep(500L);
        handleDisable();
        waitForOnOff(false, DBG);
        sendBluetoothServiceDownCallback();
        synchronized (this.mConnection) {
            if (this.mBluetooth != null) {
                this.mBluetooth = null;
                this.mContext.unbindService(this.mConnection);
            }
        }
        this.mHandler.removeMessages(MESSAGE_BLUETOOTH_STATE_CHANGE);
        this.mState = 10;
        this.mEnable = false;
        int i = this.mErrorRecoveryRetryCounter;
        this.mErrorRecoveryRetryCounter = i + 1;
        if (i < 6) {
            Message restartMsg = this.mHandler.obtainMessage(42);
            this.mHandler.sendMessageDelayed(restartMsg, 3000L);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        writer.println("enabled: " + this.mEnable);
        writer.println("state: " + this.mState);
        writer.println("address: " + this.mAddress);
        writer.println("name: " + this.mName);
        if (this.mBluetooth == null) {
            writer.println("Bluetooth Service not connected");
            return;
        }
        try {
            writer.println(this.mBluetooth.dump());
        } catch (RemoteException e) {
            writer.println("RemoteException while calling Bluetooth Service");
        }
    }
}
