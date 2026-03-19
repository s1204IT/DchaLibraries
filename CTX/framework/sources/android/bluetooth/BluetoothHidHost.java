package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHidHost;
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

public final class BluetoothHidHost implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_HANDSHAKE = "android.bluetooth.input.profile.action.HANDSHAKE";
    public static final String ACTION_IDLE_TIME_CHANGED = "android.bluetooth.input.profile.action.IDLE_TIME_CHANGED";
    public static final String ACTION_PROTOCOL_MODE_CHANGED = "android.bluetooth.input.profile.action.PROTOCOL_MODE_CHANGED";
    public static final String ACTION_REPORT = "android.bluetooth.input.profile.action.REPORT";
    public static final String ACTION_VIRTUAL_UNPLUG_STATUS = "android.bluetooth.input.profile.action.VIRTUAL_UNPLUG_STATUS";
    private static final boolean DBG = true;
    public static final String EXTRA_IDLE_TIME = "android.bluetooth.BluetoothHidHost.extra.IDLE_TIME";
    public static final String EXTRA_PROTOCOL_MODE = "android.bluetooth.BluetoothHidHost.extra.PROTOCOL_MODE";
    public static final String EXTRA_REPORT = "android.bluetooth.BluetoothHidHost.extra.REPORT";
    public static final String EXTRA_REPORT_BUFFER_SIZE = "android.bluetooth.BluetoothHidHost.extra.REPORT_BUFFER_SIZE";
    public static final String EXTRA_REPORT_ID = "android.bluetooth.BluetoothHidHost.extra.REPORT_ID";
    public static final String EXTRA_REPORT_TYPE = "android.bluetooth.BluetoothHidHost.extra.REPORT_TYPE";
    public static final String EXTRA_STATUS = "android.bluetooth.BluetoothHidHost.extra.STATUS";
    public static final String EXTRA_VIRTUAL_UNPLUG_STATUS = "android.bluetooth.BluetoothHidHost.extra.VIRTUAL_UNPLUG_STATUS";
    public static final int INPUT_CONNECT_FAILED_ALREADY_CONNECTED = 5001;
    public static final int INPUT_CONNECT_FAILED_ATTEMPT_FAILED = 5002;
    public static final int INPUT_DISCONNECT_FAILED_NOT_CONNECTED = 5000;
    public static final int INPUT_OPERATION_GENERIC_FAILURE = 5003;
    public static final int INPUT_OPERATION_SUCCESS = 5004;
    public static final int PROTOCOL_BOOT_MODE = 1;
    public static final int PROTOCOL_REPORT_MODE = 0;
    public static final int PROTOCOL_UNSUPPORTED_MODE = 255;
    public static final byte REPORT_TYPE_FEATURE = 3;
    public static final byte REPORT_TYPE_INPUT = 1;
    public static final byte REPORT_TYPE_OUTPUT = 2;
    private static final String TAG = "BluetoothHidHost";
    private static final boolean VDBG = false;
    public static final int VIRTUAL_UNPLUG_STATUS_FAIL = 1;
    public static final int VIRTUAL_UNPLUG_STATUS_SUCCESS = 0;
    private Context mContext;
    private volatile IBluetoothHidHost mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            Log.d(BluetoothHidHost.TAG, "onBluetoothStateChange: up=" + z);
            if (!z) {
                synchronized (BluetoothHidHost.this.mConnection) {
                    try {
                        BluetoothHidHost.this.mService = null;
                        BluetoothHidHost.this.mContext.unbindService(BluetoothHidHost.this.mConnection);
                    } catch (Exception e) {
                        Log.e(BluetoothHidHost.TAG, "", e);
                    }
                }
                return;
            }
            synchronized (BluetoothHidHost.this.mConnection) {
                try {
                } catch (Exception e2) {
                    Log.e(BluetoothHidHost.TAG, "", e2);
                }
                if (BluetoothHidHost.this.mService == null) {
                    BluetoothHidHost.this.doBind();
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(BluetoothHidHost.TAG, "Proxy object connected");
            BluetoothHidHost.this.mService = IBluetoothHidHost.Stub.asInterface(Binder.allowBlocking(iBinder));
            if (BluetoothHidHost.this.mServiceListener != null) {
                BluetoothHidHost.this.mServiceListener.onServiceConnected(4, BluetoothHidHost.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(BluetoothHidHost.TAG, "Proxy object disconnected");
            BluetoothHidHost.this.mService = null;
            if (BluetoothHidHost.this.mServiceListener != null) {
                BluetoothHidHost.this.mServiceListener.onServiceDisconnected(4);
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothHidHost(Context context, BluetoothProfile.ServiceListener serviceListener) {
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
        Intent intent = new Intent(IBluetoothHidHost.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth HID Service with " + intent);
            return false;
        }
        return true;
    }

    void close() {
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

    public boolean connect(BluetoothDevice bluetoothDevice) {
        log("connect(" + bluetoothDevice + ")");
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.connect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        log("disconnect(" + bluetoothDevice + ")");
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.disconnect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled()) {
            try {
                return iBluetoothHidHost.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled()) {
            try {
                return iBluetoothHidHost.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        log("setPriority(" + bluetoothDevice + ", " + i + ")");
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            if (i != 0 && i != 100) {
                return false;
            }
            try {
                return iBluetoothHidHost.setPriority(bluetoothDevice, i);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.getPriority(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    private boolean isEnabled() {
        return this.mAdapter.getState() == 12;
    }

    private static boolean isValidDevice(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice != null && BluetoothAdapter.checkBluetoothAddress(bluetoothDevice.getAddress());
    }

    public boolean virtualUnplug(BluetoothDevice bluetoothDevice) {
        log("virtualUnplug(" + bluetoothDevice + ")");
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.virtualUnplug(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean getProtocolMode(BluetoothDevice bluetoothDevice) {
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.getProtocolMode(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean setProtocolMode(BluetoothDevice bluetoothDevice, int i) {
        log("setProtocolMode(" + bluetoothDevice + ")");
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.setProtocolMode(bluetoothDevice, i);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean getReport(BluetoothDevice bluetoothDevice, byte b, byte b2, int i) {
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.getReport(bluetoothDevice, b, b2, i);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean setReport(BluetoothDevice bluetoothDevice, byte b, String str) {
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.setReport(bluetoothDevice, b, str);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean sendData(BluetoothDevice bluetoothDevice, String str) {
        log("sendData(" + bluetoothDevice + "), report=" + str);
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.sendData(bluetoothDevice, str);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean getIdleTime(BluetoothDevice bluetoothDevice) {
        log("getIdletime(" + bluetoothDevice + ")");
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.getIdleTime(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean setIdleTime(BluetoothDevice bluetoothDevice, byte b) {
        log("setIdletime(" + bluetoothDevice + "), idleTime=" + ((int) b));
        IBluetoothHidHost iBluetoothHidHost = this.mService;
        if (iBluetoothHidHost != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHidHost.setIdleTime(bluetoothDevice, b);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHidHost == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    private static void log(String str) {
        Log.d(TAG, str);
    }
}
