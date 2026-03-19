package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothPbapClient;
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

public final class BluetoothPbapClient implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED";
    private static final boolean DBG = false;
    public static final int RESULT_CANCELED = 2;
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int STATE_ERROR = -1;
    private static final String TAG = "BluetoothPbapClient";
    private static final boolean VDBG = false;
    private final Context mContext;
    private volatile IBluetoothPbapClient mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            if (!z) {
                synchronized (BluetoothPbapClient.this.mConnection) {
                    try {
                        BluetoothPbapClient.this.mService = null;
                        BluetoothPbapClient.this.mContext.unbindService(BluetoothPbapClient.this.mConnection);
                    } catch (Exception e) {
                        Log.e(BluetoothPbapClient.TAG, "", e);
                    }
                }
                return;
            }
            synchronized (BluetoothPbapClient.this.mConnection) {
                try {
                } catch (Exception e2) {
                    Log.e(BluetoothPbapClient.TAG, "", e2);
                }
                if (BluetoothPbapClient.this.mService == null) {
                    BluetoothPbapClient.this.doBind();
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BluetoothPbapClient.this.mService = IBluetoothPbapClient.Stub.asInterface(Binder.allowBlocking(iBinder));
            if (BluetoothPbapClient.this.mServiceListener != null) {
                BluetoothPbapClient.this.mServiceListener.onServiceConnected(17, BluetoothPbapClient.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            BluetoothPbapClient.this.mService = null;
            if (BluetoothPbapClient.this.mServiceListener != null) {
                BluetoothPbapClient.this.mServiceListener.onServiceDisconnected(17);
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothPbapClient(Context context, BluetoothProfile.ServiceListener serviceListener) {
        this.mContext = context;
        this.mServiceListener = serviceListener;
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

    private boolean doBind() {
        Intent intent = new Intent(IBluetoothPbapClient.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth PBAP Client Service with " + intent);
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

    public boolean connect(BluetoothDevice bluetoothDevice) {
        IBluetoothPbapClient iBluetoothPbapClient = this.mService;
        if (iBluetoothPbapClient != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothPbapClient.connect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothPbapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        IBluetoothPbapClient iBluetoothPbapClient = this.mService;
        if (iBluetoothPbapClient != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                iBluetoothPbapClient.disconnect(bluetoothDevice);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothPbapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothPbapClient iBluetoothPbapClient = this.mService;
        if (iBluetoothPbapClient != null && isEnabled()) {
            try {
                return iBluetoothPbapClient.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothPbapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        IBluetoothPbapClient iBluetoothPbapClient = this.mService;
        if (iBluetoothPbapClient != null && isEnabled()) {
            try {
                return iBluetoothPbapClient.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothPbapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        IBluetoothPbapClient iBluetoothPbapClient = this.mService;
        if (iBluetoothPbapClient != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothPbapClient.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothPbapClient == null) {
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

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        IBluetoothPbapClient iBluetoothPbapClient = this.mService;
        if (iBluetoothPbapClient != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            if (i != 0 && i != 100) {
                return false;
            }
            try {
                return iBluetoothPbapClient.setPriority(bluetoothDevice, i);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothPbapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        IBluetoothPbapClient iBluetoothPbapClient = this.mService;
        if (iBluetoothPbapClient != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothPbapClient.getPriority(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothPbapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }
}
