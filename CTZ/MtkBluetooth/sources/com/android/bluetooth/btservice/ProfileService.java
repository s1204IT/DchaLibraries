package com.android.bluetooth.btservice;

import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;

public abstract class ProfileService extends Service {
    public static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    public static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    public static final String BLUETOOTH_PRIVILEGED = "android.permission.BLUETOOTH_PRIVILEGED";
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final int PROFILE_SERVICE_MODE = 2;
    private BluetoothAdapter mAdapter;
    private AdapterService mAdapterService;
    private IProfileServiceBinder mBinder;
    private BroadcastReceiver mUserSwitchedReceiver;
    private boolean mProfileStarted = false;
    private final String mName = getName();

    public interface IProfileServiceBinder extends IBinder {
        void cleanup();
    }

    protected abstract IProfileServiceBinder initBinder();

    protected abstract boolean start();

    protected abstract boolean stop();

    public String getName() {
        return getClass().getSimpleName();
    }

    protected boolean isAvailable() {
        return this.mProfileStarted;
    }

    protected void create() {
    }

    protected void cleanup() {
    }

    protected void setCurrentUser(int i) {
    }

    protected void setUserUnlocked(int i) {
    }

    protected ProfileService() {
    }

    @Override
    public void onCreate() {
        if (DBG) {
            Log.d(this.mName, "onCreate");
        }
        super.onCreate();
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mBinder = initBinder();
        create();
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        if (DBG) {
            Log.d(this.mName, "onStartCommand()");
        }
        if (checkCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN") != 0) {
            Log.e(this.mName, "Permission denied!");
            return 2;
        }
        if (intent == null) {
            Log.d(this.mName, "onStartCommand ignoring null intent.");
            return 2;
        }
        if (AdapterService.ACTION_SERVICE_STATE_CHANGED.equals(intent.getStringExtra(AdapterService.EXTRA_ACTION))) {
            int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
            if (intExtra == 10) {
                doStop();
            } else if (intExtra == 12) {
                doStart();
            }
        }
        return 2;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DBG) {
            Log.d(this.mName, "onBind");
        }
        if (this.mAdapter != null && this.mBinder == null) {
            throw new UnsupportedOperationException("Cannot bind to " + this.mName);
        }
        return this.mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (DBG) {
            Log.d(this.mName, "onUnbind");
        }
        return super.onUnbind(intent);
    }

    public void dump(StringBuilder sb) {
        sb.append("\nProfile: ");
        sb.append(this.mName);
        sb.append("\n");
    }

    public void dumpProto(BluetoothMetricsProto.BluetoothLog.Builder builder) {
    }

    public static void println(StringBuilder sb, String str) {
        sb.append("  ");
        sb.append(str);
        sb.append("\n");
    }

    @Override
    public void onDestroy() {
        cleanup();
        if (this.mBinder != null) {
            this.mBinder.cleanup();
            this.mBinder = null;
        }
        this.mAdapter = null;
        super.onDestroy();
    }

    private void doStart() {
        if (this.mAdapter == null) {
            Log.w(this.mName, "Can't start profile service: device does not have BT");
            return;
        }
        this.mAdapterService = AdapterService.getAdapterService();
        if (this.mAdapterService == null) {
            Log.w(this.mName, "Could not add this profile because AdapterService is null.");
            return;
        }
        this.mAdapterService.addProfile(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mUserSwitchedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (intExtra == -10000) {
                    Log.e(ProfileService.this.mName, "userChangeReceiver received an invalid EXTRA_USER_HANDLE");
                    return;
                }
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    Log.d(ProfileService.this.mName, "User switched to userId " + intExtra);
                    ProfileService.this.setCurrentUser(intExtra);
                    return;
                }
                if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                    Log.d(ProfileService.this.mName, "Unlocked userId " + intExtra);
                    ProfileService.this.setUserUnlocked(intExtra);
                }
            }
        };
        getApplicationContext().registerReceiver(this.mUserSwitchedReceiver, intentFilter);
        int currentUser = ActivityManager.getCurrentUser();
        setCurrentUser(currentUser);
        if (UserManager.get(getApplicationContext()).isUserUnlocked(currentUser)) {
            setUserUnlocked(currentUser);
        }
        this.mProfileStarted = start();
        if (!this.mProfileStarted) {
            Log.e(this.mName, "Error starting profile. start() returned false.");
        } else {
            this.mAdapterService.onProfileServiceStateChanged(this, 12);
        }
    }

    private void doStop() {
        if (!this.mProfileStarted) {
            Log.w(this.mName, "doStop() called, but the profile is not running.");
        }
        this.mProfileStarted = false;
        if (this.mAdapterService != null) {
            this.mAdapterService.onProfileServiceStateChanged(this, 10);
        }
        if (!stop()) {
            Log.e(this.mName, "Unable to stop profile");
        }
        if (this.mAdapterService != null) {
            this.mAdapterService.removeProfile(this);
        }
        if (this.mUserSwitchedReceiver != null) {
            getApplicationContext().unregisterReceiver(this.mUserSwitchedReceiver);
            this.mUserSwitchedReceiver = null;
        }
        stopSelf();
    }

    protected BluetoothDevice getDevice(byte[] bArr) {
        if (this.mAdapter != null) {
            return this.mAdapter.getRemoteDevice(Utils.getAddressStringFromByte(bArr));
        }
        return null;
    }
}
