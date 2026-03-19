package android.bluetooth;

import android.app.PendingIntent;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothMapClient;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothMapClient implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_MESSAGE_DELIVERED_SUCCESSFULLY = "android.bluetooth.mapmce.profile.action.MESSAGE_DELIVERED_SUCCESSFULLY";
    public static final String ACTION_MESSAGE_RECEIVED = "android.bluetooth.mapmce.profile.action.MESSAGE_RECEIVED";
    public static final String ACTION_MESSAGE_SENT_SUCCESSFULLY = "android.bluetooth.mapmce.profile.action.MESSAGE_SENT_SUCCESSFULLY";
    public static final String EXTRA_MESSAGE_HANDLE = "android.bluetooth.mapmce.profile.extra.MESSAGE_HANDLE";
    public static final String EXTRA_SENDER_CONTACT_NAME = "android.bluetooth.mapmce.profile.extra.SENDER_CONTACT_NAME";
    public static final String EXTRA_SENDER_CONTACT_URI = "android.bluetooth.mapmce.profile.extra.SENDER_CONTACT_URI";
    public static final int RESULT_CANCELED = 2;
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int STATE_ERROR = -1;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            if (BluetoothMapClient.DBG) {
                Log.d(BluetoothMapClient.TAG, "onBluetoothStateChange: up=" + z);
            }
            if (!z) {
                if (BluetoothMapClient.VDBG) {
                    Log.d(BluetoothMapClient.TAG, "Unbinding service...");
                }
                synchronized (BluetoothMapClient.this.mConnection) {
                    try {
                        BluetoothMapClient.this.mService = null;
                        BluetoothMapClient.this.mContext.unbindService(BluetoothMapClient.this.mConnection);
                    } catch (Exception e) {
                        Log.e(BluetoothMapClient.TAG, "", e);
                    }
                }
                return;
            }
            synchronized (BluetoothMapClient.this.mConnection) {
                try {
                } catch (Exception e2) {
                    Log.e(BluetoothMapClient.TAG, "", e2);
                }
                if (BluetoothMapClient.this.mService == null) {
                    if (BluetoothMapClient.VDBG) {
                        Log.d(BluetoothMapClient.TAG, "Binding service...");
                    }
                    BluetoothMapClient.this.doBind();
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (BluetoothMapClient.DBG) {
                Log.d(BluetoothMapClient.TAG, "Proxy object connected");
            }
            BluetoothMapClient.this.mService = IBluetoothMapClient.Stub.asInterface(iBinder);
            if (BluetoothMapClient.this.mServiceListener != null) {
                BluetoothMapClient.this.mServiceListener.onServiceConnected(18, BluetoothMapClient.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (BluetoothMapClient.DBG) {
                Log.d(BluetoothMapClient.TAG, "Proxy object disconnected");
            }
            BluetoothMapClient.this.mService = null;
            if (BluetoothMapClient.this.mServiceListener != null) {
                BluetoothMapClient.this.mServiceListener.onServiceDisconnected(18);
            }
        }
    };
    private final Context mContext;
    private volatile IBluetoothMapClient mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private static final String TAG = "BluetoothMapClient";
    private static final boolean DBG = Log.isLoggable(TAG, 3);
    private static final boolean VDBG = Log.isLoggable(TAG, 2);

    BluetoothMapClient(Context context, BluetoothProfile.ServiceListener serviceListener) {
        if (DBG) {
            Log.d(TAG, "Create BluetoothMapClient proxy object");
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
        Intent intent = new Intent(IBluetoothMapClient.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth MAP MCE Service with " + intent);
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

    public void close() {
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
        this.mServiceListener = null;
    }

    public boolean isConnected(BluetoothDevice bluetoothDevice) {
        if (VDBG) {
            Log.d(TAG, "isConnected(" + bluetoothDevice + ")");
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient != null) {
            try {
                return iBluetoothMapClient.isConnected(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
            return false;
        }
        return false;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            Log.d(TAG, "connect(" + bluetoothDevice + ")for MAPS MCE");
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient != null) {
            try {
                return iBluetoothMapClient.connect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
            return false;
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            Log.d(TAG, "disconnect(" + bluetoothDevice + ")");
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothMapClient.disconnect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (iBluetoothMapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        if (DBG) {
            Log.d(TAG, "getConnectedDevices()");
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient != null && isEnabled()) {
            try {
                return iBluetoothMapClient.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothMapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        if (DBG) {
            Log.d(TAG, "getDevicesMatchingStates()");
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient != null && isEnabled()) {
            try {
                return iBluetoothMapClient.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothMapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            Log.d(TAG, "getConnectionState(" + bluetoothDevice + ")");
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothMapClient.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothMapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        if (DBG) {
            Log.d(TAG, "setPriority(" + bluetoothDevice + ", " + i + ")");
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            if (i != 0 && i != 100) {
                return false;
            }
            try {
                return iBluetoothMapClient.setPriority(bluetoothDevice, i);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothMapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        if (VDBG) {
            Log.d(TAG, "getPriority(" + bluetoothDevice + ")");
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothMapClient.getPriority(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothMapClient == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean sendMessage(BluetoothDevice bluetoothDevice, Uri[] uriArr, String str, PendingIntent pendingIntent, PendingIntent pendingIntent2) {
        if (DBG) {
            Log.d(TAG, "sendMessage(" + bluetoothDevice + ", " + uriArr + ", " + str);
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient == null || !isEnabled() || !isValidDevice(bluetoothDevice)) {
            return false;
        }
        try {
            return iBluetoothMapClient.sendMessage(bluetoothDevice, uriArr, str, pendingIntent, pendingIntent2);
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    public boolean getUnreadMessages(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            Log.d(TAG, "getUnreadMessages(" + bluetoothDevice + ")");
        }
        IBluetoothMapClient iBluetoothMapClient = this.mService;
        if (iBluetoothMapClient == null || !isEnabled() || !isValidDevice(bluetoothDevice)) {
            return false;
        }
        try {
            return iBluetoothMapClient.getUnreadMessages(bluetoothDevice);
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    private boolean isEnabled() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null && defaultAdapter.getState() == 12) {
            return true;
        }
        if (DBG) {
            Log.d(TAG, "Bluetooth is Not enabled");
            return false;
        }
        return false;
    }

    private static boolean isValidDevice(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice != null && BluetoothAdapter.checkBluetoothAddress(bluetoothDevice.getAddress());
    }
}
