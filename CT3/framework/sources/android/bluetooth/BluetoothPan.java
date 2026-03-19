package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothPan;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ProxyInfo;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothPan implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final String EXTRA_LOCAL_ROLE = "android.bluetooth.pan.extra.LOCAL_ROLE";
    public static final int LOCAL_NAP_ROLE = 1;
    public static final int LOCAL_PANU_ROLE = 2;
    public static final int PAN_CONNECT_FAILED_ALREADY_CONNECTED = 1001;
    public static final int PAN_CONNECT_FAILED_ATTEMPT_FAILED = 1002;
    public static final int PAN_DISCONNECT_FAILED_NOT_CONNECTED = 1000;
    public static final int PAN_OPERATION_GENERIC_FAILURE = 1003;
    public static final int PAN_OPERATION_SUCCESS = 1004;
    public static final int PAN_ROLE_NONE = 0;
    public static final int REMOTE_NAP_ROLE = 1;
    public static final int REMOTE_PANU_ROLE = 2;
    private static final String TAG = "BluetoothPan";
    private static final boolean VDBG = true;
    private Context mContext;
    private IBluetoothPan mPanService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean on) {
            ServiceConnection serviceConnection;
            Log.d(BluetoothPan.TAG, "onBluetoothStateChange on: " + on);
            if (on) {
                serviceConnection = BluetoothPan.this.mConnection;
                synchronized (serviceConnection) {
                    try {
                        if (BluetoothPan.this.mPanService == null && BluetoothPan.this.mContext != null) {
                            Log.d(BluetoothPan.TAG, "onBluetoothStateChange calling doBind()");
                            BluetoothPan.this.doBind();
                        }
                    } catch (IllegalStateException e) {
                        Log.e(BluetoothPan.TAG, "onBluetoothStateChange: could not bind to PAN service: ", e);
                    } catch (SecurityException e2) {
                        Log.e(BluetoothPan.TAG, "onBluetoothStateChange: could not bind to PAN service: ", e2);
                    }
                }
            } else {
                Log.d(BluetoothPan.TAG, "Unbinding service...");
                serviceConnection = BluetoothPan.this.mConnection;
                synchronized (serviceConnection) {
                    try {
                        BluetoothPan.this.mPanService = null;
                        if (BluetoothPan.this.mContext == null) {
                            Log.d(BluetoothPan.TAG, "Context is null");
                        } else {
                            BluetoothPan.this.mContext.unbindService(BluetoothPan.this.mConnection);
                        }
                    } catch (Exception re) {
                        Log.e(BluetoothPan.TAG, ProxyInfo.LOCAL_EXCL_LIST, re);
                    }
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            synchronized (BluetoothPan.this.mConnection) {
                Log.d(BluetoothPan.TAG, "BluetoothPAN Proxy object connected");
                BluetoothPan.this.mPanService = IBluetoothPan.Stub.asInterface(service);
                if (BluetoothPan.this.mServiceListener != null) {
                    BluetoothPan.this.mServiceListener.onServiceConnected(5, BluetoothPan.this);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            synchronized (BluetoothPan.this.mConnection) {
                Log.d(BluetoothPan.TAG, "BluetoothPAN Proxy object disconnected");
                BluetoothPan.this.mPanService = null;
                if (BluetoothPan.this.mServiceListener != null) {
                    BluetoothPan.this.mServiceListener.onServiceDisconnected(5);
                }
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothPan(Context context, BluetoothProfile.ServiceListener l) {
        this.mContext = context;
        this.mServiceListener = l;
        try {
            Log.d(TAG, "Register mBluetoothStateChangeCallback = " + this.mStateChangeCallback);
            this.mAdapter.getBluetoothManager().registerStateChangeCallback(this.mStateChangeCallback);
        } catch (RemoteException re) {
            Log.w(TAG, "Unable to register BluetoothStateChangeCallback", re);
        }
        Log.d(TAG, "BluetoothPan() call bindService");
        doBind();
    }

    boolean doBind() {
        if (this.mContext == null) {
            Log.e(TAG, "Context is null");
            return false;
        }
        Intent intent = new Intent(IBluetoothPan.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle())) {
            Log.e(TAG, "Could not bind to Bluetooth Pan Service with " + intent);
            return false;
        }
        return true;
    }

    void close() {
        log("close()");
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                Log.d(TAG, "Unregister mBluetoothStateChangeCallback = " + this.mStateChangeCallback);
                mgr.unregisterStateChangeCallback(this.mStateChangeCallback);
            } catch (RemoteException re) {
                Log.w(TAG, "Unable to unregister BluetoothStateChangeCallback", re);
            }
        }
        synchronized (this.mConnection) {
            if (this.mPanService != null) {
                try {
                    this.mPanService = null;
                    if (this.mContext == null) {
                        Log.d(TAG, "Context is null");
                    } else {
                        this.mContext.unbindService(this.mConnection);
                    }
                } catch (Exception re2) {
                    Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, re2);
                }
                this.mContext = null;
                this.mServiceListener = null;
            } else {
                this.mContext = null;
                this.mServiceListener = null;
            }
        }
    }

    protected void finalize() {
        close();
    }

    public boolean connect(BluetoothDevice device) {
        log("connect(" + device + ")");
        if (this.mPanService != null && isEnabled() && isValidDevice(device)) {
            try {
                return this.mPanService.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (this.mPanService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        log("disconnect(" + device + ")");
        if (this.mPanService != null && isEnabled() && isValidDevice(device)) {
            try {
                return this.mPanService.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (this.mPanService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        log("getConnectedDevices()");
        if (this.mPanService != null && isEnabled()) {
            try {
                return this.mPanService.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (this.mPanService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        log("getDevicesMatchingStates()");
        if (this.mPanService != null && isEnabled()) {
            try {
                return this.mPanService.getDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (this.mPanService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice device) {
        log("getState(" + device + ")");
        if (this.mPanService != null && isEnabled() && isValidDevice(device)) {
            try {
                return this.mPanService.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (this.mPanService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public void setBluetoothTethering(boolean value) {
        log("setBluetoothTethering(" + value + ")");
        if (this.mPanService == null || !isEnabled()) {
            return;
        }
        try {
            this.mPanService.setBluetoothTethering(value);
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
        }
    }

    public boolean isTetheringOn() {
        log("isTetheringOn()");
        if (this.mPanService != null && isEnabled()) {
            try {
                return this.mPanService.isTetheringOn();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        return false;
    }

    private boolean isEnabled() {
        return this.mAdapter.getState() == 12;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
