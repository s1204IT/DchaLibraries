package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothPan;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
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
    private static final boolean VDBG = false;
    private Context mContext;
    private volatile IBluetoothPan mPanService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            Log.d(BluetoothPan.TAG, "onBluetoothStateChange on: " + z);
            if (z) {
                synchronized (BluetoothPan.this.mConnection) {
                    try {
                    } catch (IllegalStateException e) {
                        Log.e(BluetoothPan.TAG, "onBluetoothStateChange: could not bind to PAN service:", e);
                    } catch (SecurityException e2) {
                        Log.e(BluetoothPan.TAG, "onBluetoothStateChange: could not bind to PAN service:", e2);
                    }
                    if (BluetoothPan.this.mPanService == null && BluetoothPan.this.mContext != null) {
                        BluetoothPan.this.doBind();
                    }
                }
                return;
            }
            synchronized (BluetoothPan.this.mConnection) {
                try {
                    BluetoothPan.this.mPanService = null;
                    if (BluetoothPan.this.mContext != null) {
                        BluetoothPan.this.mContext.unbindService(BluetoothPan.this.mConnection);
                    } else {
                        Log.w(BluetoothPan.TAG, "onBluetoothStateChange nContext is null");
                    }
                } catch (Exception e3) {
                    Log.e(BluetoothPan.TAG, "", e3);
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(BluetoothPan.TAG, "BluetoothPAN Proxy object connected");
            BluetoothPan.this.mPanService = IBluetoothPan.Stub.asInterface(Binder.allowBlocking(iBinder));
            if (BluetoothPan.this.mServiceListener != null) {
                BluetoothPan.this.mServiceListener.onServiceConnected(5, BluetoothPan.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(BluetoothPan.TAG, "BluetoothPAN Proxy object disconnected");
            BluetoothPan.this.mPanService = null;
            if (BluetoothPan.this.mServiceListener != null) {
                BluetoothPan.this.mServiceListener.onServiceDisconnected(5);
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothPan(Context context, BluetoothProfile.ServiceListener serviceListener) {
        this.mContext = context;
        this.mServiceListener = serviceListener;
        try {
            this.mAdapter.getBluetoothManager().registerStateChangeCallback(this.mStateChangeCallback);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to register BluetoothStateChangeCallback", e);
        }
        doBind();
    }

    boolean doBind() {
        if (this.mContext == null) {
            Log.w(TAG, "Context is null");
            return false;
        }
        Intent intent = new Intent(IBluetoothPan.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth Pan Service with " + intent);
            return false;
        }
        return true;
    }

    void close() {
        IBluetoothManager bluetoothManager = this.mAdapter.getBluetoothManager();
        if (bluetoothManager != null) {
            try {
                bluetoothManager.unregisterStateChangeCallback(this.mStateChangeCallback);
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to unregister BluetoothStateChangeCallback", e);
            }
        }
        synchronized (this.mConnection) {
            if (this.mPanService != null) {
                try {
                    this.mPanService = null;
                    if (this.mContext != null) {
                        this.mContext.unbindService(this.mConnection);
                    } else {
                        Log.w(TAG, "mContext is null");
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "", e2);
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

    public boolean connect(BluetoothDevice bluetoothDevice) {
        log("connect(" + bluetoothDevice + ")");
        IBluetoothPan iBluetoothPan = this.mPanService;
        if (iBluetoothPan != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothPan.connect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothPan == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        log("disconnect(" + bluetoothDevice + ")");
        IBluetoothPan iBluetoothPan = this.mPanService;
        if (iBluetoothPan != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothPan.disconnect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothPan == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothPan iBluetoothPan = this.mPanService;
        if (iBluetoothPan != null && isEnabled()) {
            try {
                return iBluetoothPan.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothPan == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        IBluetoothPan iBluetoothPan = this.mPanService;
        if (iBluetoothPan != null && isEnabled()) {
            try {
                return iBluetoothPan.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothPan == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        IBluetoothPan iBluetoothPan = this.mPanService;
        if (iBluetoothPan != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothPan.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothPan == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public void setBluetoothTethering(boolean z) {
        log("setBluetoothTethering(" + z + ")");
        IBluetoothPan iBluetoothPan = this.mPanService;
        if (iBluetoothPan != null && isEnabled()) {
            try {
                iBluetoothPan.setBluetoothTethering(z);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            }
        }
    }

    public boolean isTetheringOn() {
        IBluetoothPan iBluetoothPan = this.mPanService;
        if (iBluetoothPan != null && isEnabled()) {
            try {
                return iBluetoothPan.isTetheringOn();
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

    private static boolean isValidDevice(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice != null && BluetoothAdapter.checkBluetoothAddress(bluetoothDevice.getAddress());
    }

    private static void log(String str) {
        Log.d(TAG, str);
    }
}
