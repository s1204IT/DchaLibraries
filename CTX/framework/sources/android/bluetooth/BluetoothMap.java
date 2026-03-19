package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothMap;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothMap implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED";
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    public static final int RESULT_CANCELED = 2;
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int STATE_ERROR = -1;
    private static final String TAG = "BluetoothMap";
    private static final boolean VDBG = false;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            if (BluetoothMap.DBG) {
                Log.d(BluetoothMap.TAG, "onBluetoothStateChange: up=" + z);
            }
            if (!z) {
                synchronized (BluetoothMap.this.mConnection) {
                    try {
                        BluetoothMap.this.mService = null;
                        BluetoothMap.this.mContext.unbindService(BluetoothMap.this.mConnection);
                    } catch (Exception e) {
                        Log.e(BluetoothMap.TAG, "", e);
                    }
                }
                return;
            }
            synchronized (BluetoothMap.this.mConnection) {
                try {
                } catch (Exception e2) {
                    Log.e(BluetoothMap.TAG, "", e2);
                }
                if (BluetoothMap.this.mService == null) {
                    BluetoothMap.this.doBind();
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (BluetoothMap.DBG) {
                BluetoothMap.log("Proxy object connected");
            }
            BluetoothMap.this.mService = IBluetoothMap.Stub.asInterface(Binder.allowBlocking(iBinder));
            if (BluetoothMap.this.mServiceListener != null) {
                BluetoothMap.this.mServiceListener.onServiceConnected(9, BluetoothMap.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (BluetoothMap.DBG) {
                BluetoothMap.log("Proxy object disconnected");
            }
            BluetoothMap.this.mService = null;
            if (BluetoothMap.this.mServiceListener != null) {
                BluetoothMap.this.mServiceListener.onServiceDisconnected(9);
            }
        }
    };
    private final Context mContext;
    private volatile IBluetoothMap mService;
    private BluetoothProfile.ServiceListener mServiceListener;

    BluetoothMap(Context context, BluetoothProfile.ServiceListener serviceListener) {
        if (DBG) {
            Log.d(TAG, "Create BluetoothMap proxy object");
        }
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
        Intent intent = new Intent(IBluetoothMap.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth MAP Service with " + intent);
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
        IBluetoothMap iBluetoothMap = this.mService;
        if (iBluetoothMap != null) {
            try {
                return iBluetoothMap.getState();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return -1;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            log(Log.getStackTraceString(new Throwable()));
            return -1;
        }
        return -1;
    }

    public BluetoothDevice getClient() {
        IBluetoothMap iBluetoothMap = this.mService;
        if (iBluetoothMap != null) {
            try {
                return iBluetoothMap.getClient();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return null;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            log(Log.getStackTraceString(new Throwable()));
            return null;
        }
        return null;
    }

    public boolean isConnected(BluetoothDevice bluetoothDevice) {
        IBluetoothMap iBluetoothMap = this.mService;
        if (iBluetoothMap != null) {
            try {
                return iBluetoothMap.isConnected(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            log(Log.getStackTraceString(new Throwable()));
            return false;
        }
        return false;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            log("connect(" + bluetoothDevice + ")not supported for MAPS");
            return false;
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            log("disconnect(" + bluetoothDevice + ")");
        }
        IBluetoothMap iBluetoothMap = this.mService;
        if (iBluetoothMap != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothMap.disconnect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothMap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public static boolean doesClassMatchSink(BluetoothClass bluetoothClass) {
        int deviceClass = bluetoothClass.getDeviceClass();
        if (deviceClass == 256 || deviceClass == 260 || deviceClass == 264 || deviceClass == 268) {
            return true;
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        if (DBG) {
            log("getConnectedDevices()");
        }
        IBluetoothMap iBluetoothMap = this.mService;
        if (iBluetoothMap != null && isEnabled()) {
            try {
                return iBluetoothMap.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothMap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        if (DBG) {
            log("getDevicesMatchingStates()");
        }
        IBluetoothMap iBluetoothMap = this.mService;
        if (iBluetoothMap != null && isEnabled()) {
            try {
                return iBluetoothMap.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothMap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            log("getConnectionState(" + bluetoothDevice + ")");
        }
        IBluetoothMap iBluetoothMap = this.mService;
        if (iBluetoothMap != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothMap.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothMap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        if (DBG) {
            log("setPriority(" + bluetoothDevice + ", " + i + ")");
        }
        IBluetoothMap iBluetoothMap = this.mService;
        if (iBluetoothMap != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            if (i != 0 && i != 100) {
                return false;
            }
            try {
                return iBluetoothMap.setPriority(bluetoothDevice, i);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothMap == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        IBluetoothMap iBluetoothMap = this.mService;
        if (iBluetoothMap != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothMap.getPriority(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothMap == null) {
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
