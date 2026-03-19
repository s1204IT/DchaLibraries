package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothSap;
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

public final class BluetoothSap implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.sap.profile.action.CONNECTION_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final int RESULT_CANCELED = 2;
    public static final int RESULT_SUCCESS = 1;
    public static final int STATE_ERROR = -1;
    private static final String TAG = "BluetoothSap";
    private static final boolean VDBG = false;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            Log.d(BluetoothSap.TAG, "onBluetoothStateChange: up=" + z);
            if (!z) {
                synchronized (BluetoothSap.this.mConnection) {
                    try {
                        BluetoothSap.this.mService = null;
                        BluetoothSap.this.mContext.unbindService(BluetoothSap.this.mConnection);
                    } catch (Exception e) {
                        Log.e(BluetoothSap.TAG, "", e);
                    }
                }
                return;
            }
            synchronized (BluetoothSap.this.mConnection) {
                try {
                } catch (Exception e2) {
                    Log.e(BluetoothSap.TAG, "", e2);
                }
                if (BluetoothSap.this.mService == null) {
                    BluetoothSap.this.doBind();
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BluetoothSap.log("Proxy object connected");
            BluetoothSap.this.mService = IBluetoothSap.Stub.asInterface(Binder.allowBlocking(iBinder));
            if (BluetoothSap.this.mServiceListener != null) {
                BluetoothSap.this.mServiceListener.onServiceConnected(10, BluetoothSap.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            BluetoothSap.log("Proxy object disconnected");
            BluetoothSap.this.mService = null;
            if (BluetoothSap.this.mServiceListener != null) {
                BluetoothSap.this.mServiceListener.onServiceDisconnected(10);
            }
        }
    };
    private final Context mContext;
    private volatile IBluetoothSap mService;
    private BluetoothProfile.ServiceListener mServiceListener;

    BluetoothSap(Context context, BluetoothProfile.ServiceListener serviceListener) {
        Log.d(TAG, "Create BluetoothSap proxy object");
        this.mContext = context;
        this.mServiceListener = serviceListener;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager bluetoothManager = this.mAdapter.getBluetoothManager();
        if (bluetoothManager != null) {
            try {
                bluetoothManager.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothSap.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth SAP Service with " + intent);
            return false;
        }
        return true;
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    public synchronized void close() {
        IBluetoothManager bluetoothManager = this.mAdapter.getBluetoothManager();
        if (bluetoothManager != null) {
            try {
                bluetoothManager.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
            synchronized (this.mConnection) {
                if (this.mService != null) {
                    try {
                        this.mService = null;
                        this.mContext.unbindService(this.mConnection);
                    } catch (Exception e2) {
                        Log.e(TAG, "", e2);
                    }
                }
            }
            this.mServiceListener = null;
        }
    }

    public int getState() {
        IBluetoothSap iBluetoothSap = this.mService;
        if (iBluetoothSap != null) {
            try {
                return iBluetoothSap.getState();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return -1;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        log(Log.getStackTraceString(new Throwable()));
        return -1;
    }

    public BluetoothDevice getClient() {
        IBluetoothSap iBluetoothSap = this.mService;
        if (iBluetoothSap != null) {
            try {
                return iBluetoothSap.getClient();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return null;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        log(Log.getStackTraceString(new Throwable()));
        return null;
    }

    public boolean isConnected(BluetoothDevice bluetoothDevice) {
        IBluetoothSap iBluetoothSap = this.mService;
        if (iBluetoothSap != null) {
            try {
                return iBluetoothSap.isConnected(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        log(Log.getStackTraceString(new Throwable()));
        return false;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        log("connect(" + bluetoothDevice + ")not supported for SAPS");
        return false;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        log("disconnect(" + bluetoothDevice + ")");
        IBluetoothSap iBluetoothSap = this.mService;
        if (iBluetoothSap != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothSap.disconnect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothSap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        log("getConnectedDevices()");
        IBluetoothSap iBluetoothSap = this.mService;
        if (iBluetoothSap != null && isEnabled()) {
            try {
                return iBluetoothSap.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothSap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        log("getDevicesMatchingStates()");
        IBluetoothSap iBluetoothSap = this.mService;
        if (iBluetoothSap != null && isEnabled()) {
            try {
                return iBluetoothSap.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothSap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        log("getConnectionState(" + bluetoothDevice + ")");
        IBluetoothSap iBluetoothSap = this.mService;
        if (iBluetoothSap != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothSap.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothSap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        log("setPriority(" + bluetoothDevice + ", " + i + ")");
        IBluetoothSap iBluetoothSap = this.mService;
        if (iBluetoothSap != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            if (i != 0 && i != 100) {
                return false;
            }
            try {
                return iBluetoothSap.setPriority(bluetoothDevice, i);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothSap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        IBluetoothSap iBluetoothSap = this.mService;
        if (iBluetoothSap != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothSap.getPriority(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothSap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    private static void log(String str) {
        Log.d(TAG, str);
    }

    private boolean isEnabled() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null && defaultAdapter.getState() == 12) {
            return true;
        }
        log("Bluetooth is Not enabled");
        return false;
    }

    private static boolean isValidDevice(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice != null && BluetoothAdapter.checkBluetoothAddress(bluetoothDevice.getAddress());
    }
}
