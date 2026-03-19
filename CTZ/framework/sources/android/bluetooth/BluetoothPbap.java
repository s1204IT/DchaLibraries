package android.bluetooth;

import android.bluetooth.IBluetoothPbap;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BluetoothPbap implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED";
    private static final boolean DBG = false;
    public static final int RESULT_CANCELED = 2;
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    private static final String TAG = "BluetoothPbap";
    private final Context mContext;
    private volatile IBluetoothPbap mService;
    private ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            BluetoothPbap.log("onBluetoothStateChange: up=" + z);
            if (!z) {
                BluetoothPbap.log("Unbinding service...");
                synchronized (BluetoothPbap.this.mConnection) {
                    try {
                        BluetoothPbap.this.mService = null;
                        BluetoothPbap.this.mContext.unbindService(BluetoothPbap.this.mConnection);
                    } catch (Exception e) {
                        Log.e(BluetoothPbap.TAG, "", e);
                    }
                }
                return;
            }
            synchronized (BluetoothPbap.this.mConnection) {
                try {
                } catch (Exception e2) {
                    Log.e(BluetoothPbap.TAG, "", e2);
                }
                if (BluetoothPbap.this.mService == null) {
                    BluetoothPbap.log("Binding service...");
                    BluetoothPbap.this.doBind();
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BluetoothPbap.log("Proxy object connected");
            BluetoothPbap.this.mService = IBluetoothPbap.Stub.asInterface(iBinder);
            if (BluetoothPbap.this.mServiceListener != null) {
                BluetoothPbap.this.mServiceListener.onServiceConnected(BluetoothPbap.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            BluetoothPbap.log("Proxy object disconnected");
            BluetoothPbap.this.mService = null;
            if (BluetoothPbap.this.mServiceListener != null) {
                BluetoothPbap.this.mServiceListener.onServiceDisconnected();
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    public interface ServiceListener {
        void onServiceConnected(BluetoothPbap bluetoothPbap);

        void onServiceDisconnected();
    }

    public BluetoothPbap(Context context, ServiceListener serviceListener) {
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

    boolean doBind() {
        Intent intent = new Intent(IBluetoothPbap.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth Pbap Service with " + intent);
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

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        log("getConnectedDevices()");
        IBluetoothPbap iBluetoothPbap = this.mService;
        if (iBluetoothPbap == null) {
            Log.w(TAG, "Proxy not attached to service");
            return new ArrayList();
        }
        try {
            return iBluetoothPbap.getConnectedDevices();
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
            return new ArrayList();
        }
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        log("getConnectionState: device=" + bluetoothDevice);
        IBluetoothPbap iBluetoothPbap = this.mService;
        if (iBluetoothPbap == null) {
            Log.w(TAG, "Proxy not attached to service");
            return 0;
        }
        try {
            return iBluetoothPbap.getConnectionState(bluetoothDevice);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
            return 0;
        }
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        log("getDevicesMatchingConnectionStates: states=" + Arrays.toString(iArr));
        IBluetoothPbap iBluetoothPbap = this.mService;
        if (iBluetoothPbap == null) {
            Log.w(TAG, "Proxy not attached to service");
            return new ArrayList();
        }
        try {
            return iBluetoothPbap.getDevicesMatchingConnectionStates(iArr);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
            return new ArrayList();
        }
    }

    public boolean isConnected(BluetoothDevice bluetoothDevice) {
        return getConnectionState(bluetoothDevice) == 2;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        log("disconnect()");
        IBluetoothPbap iBluetoothPbap = this.mService;
        if (iBluetoothPbap == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        try {
            iBluetoothPbap.disconnect(bluetoothDevice);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    private static void log(String str) {
    }
}
