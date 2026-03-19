package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothAvrcpController;
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

public final class BluetoothAvrcpController implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYER_SETTING = "android.bluetooth.avrcp-controller.profile.action.PLAYER_SETTING";
    private static final boolean DBG = false;
    public static final String EXTRA_PLAYER_SETTING = "android.bluetooth.avrcp-controller.profile.extra.PLAYER_SETTING";
    private static final String TAG = "BluetoothAvrcpController";
    private static final boolean VDBG = false;
    private Context mContext;
    private volatile IBluetoothAvrcpController mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            if (!z) {
                synchronized (BluetoothAvrcpController.this.mConnection) {
                    try {
                        BluetoothAvrcpController.this.mService = null;
                        BluetoothAvrcpController.this.mContext.unbindService(BluetoothAvrcpController.this.mConnection);
                    } catch (Exception e) {
                        Log.e(BluetoothAvrcpController.TAG, "", e);
                    }
                }
                return;
            }
            synchronized (BluetoothAvrcpController.this.mConnection) {
                try {
                } catch (Exception e2) {
                    Log.e(BluetoothAvrcpController.TAG, "", e2);
                }
                if (BluetoothAvrcpController.this.mService == null) {
                    BluetoothAvrcpController.this.doBind();
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BluetoothAvrcpController.this.mService = IBluetoothAvrcpController.Stub.asInterface(Binder.allowBlocking(iBinder));
            if (BluetoothAvrcpController.this.mServiceListener != null) {
                BluetoothAvrcpController.this.mServiceListener.onServiceConnected(12, BluetoothAvrcpController.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            BluetoothAvrcpController.this.mService = null;
            if (BluetoothAvrcpController.this.mServiceListener != null) {
                BluetoothAvrcpController.this.mServiceListener.onServiceDisconnected(12);
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothAvrcpController(Context context, BluetoothProfile.ServiceListener serviceListener) {
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
        Intent intent = new Intent(IBluetoothAvrcpController.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth AVRCP Controller Service with " + intent);
            return false;
        }
        return true;
    }

    void close() {
        this.mServiceListener = null;
        IBluetoothManager bluetoothManager = this.mAdapter.getBluetoothManager();
        if (bluetoothManager != null) {
            try {
                bluetoothManager.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
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
    }

    public void finalize() {
        close();
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothAvrcpController iBluetoothAvrcpController = this.mService;
        if (iBluetoothAvrcpController != null && isEnabled()) {
            try {
                return iBluetoothAvrcpController.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothAvrcpController == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        IBluetoothAvrcpController iBluetoothAvrcpController = this.mService;
        if (iBluetoothAvrcpController != null && isEnabled()) {
            try {
                return iBluetoothAvrcpController.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothAvrcpController == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        IBluetoothAvrcpController iBluetoothAvrcpController = this.mService;
        if (iBluetoothAvrcpController != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothAvrcpController.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothAvrcpController == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice bluetoothDevice) {
        IBluetoothAvrcpController iBluetoothAvrcpController = this.mService;
        if (iBluetoothAvrcpController == null || !isEnabled()) {
            return null;
        }
        try {
            return iBluetoothAvrcpController.getPlayerSettings(bluetoothDevice);
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getMetadata() " + e);
            return null;
        }
    }

    public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings bluetoothAvrcpPlayerSettings) {
        IBluetoothAvrcpController iBluetoothAvrcpController = this.mService;
        if (iBluetoothAvrcpController != null && isEnabled()) {
            try {
                return iBluetoothAvrcpController.setPlayerApplicationSetting(bluetoothAvrcpPlayerSettings);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in setPlayerApplicationSetting() " + e);
                return false;
            }
        }
        if (iBluetoothAvrcpController == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public void sendGroupNavigationCmd(BluetoothDevice bluetoothDevice, int i, int i2) {
        Log.d(TAG, "sendGroupNavigationCmd dev = " + bluetoothDevice + " key " + i + " State = " + i2);
        IBluetoothAvrcpController iBluetoothAvrcpController = this.mService;
        if (iBluetoothAvrcpController != null && isEnabled()) {
            try {
                iBluetoothAvrcpController.sendGroupNavigationCmd(bluetoothDevice, i, i2);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in sendGroupNavigationCmd()", e);
                return;
            }
        }
        if (iBluetoothAvrcpController == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
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
