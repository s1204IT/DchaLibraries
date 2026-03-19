package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHidDevice;
import android.bluetooth.IBluetoothHidDeviceCallback;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public final class BluetoothHidDevice implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED";
    public static final byte ERROR_RSP_INVALID_PARAM = 4;
    public static final byte ERROR_RSP_INVALID_RPT_ID = 2;
    public static final byte ERROR_RSP_NOT_READY = 1;
    public static final byte ERROR_RSP_SUCCESS = 0;
    public static final byte ERROR_RSP_UNKNOWN = 14;
    public static final byte ERROR_RSP_UNSUPPORTED_REQ = 3;
    public static final byte PROTOCOL_BOOT_MODE = 0;
    public static final byte PROTOCOL_REPORT_MODE = 1;
    public static final byte REPORT_TYPE_FEATURE = 3;
    public static final byte REPORT_TYPE_INPUT = 1;
    public static final byte REPORT_TYPE_OUTPUT = 2;
    public static final byte SUBCLASS1_COMBO = -64;
    public static final byte SUBCLASS1_KEYBOARD = 64;
    public static final byte SUBCLASS1_MOUSE = -128;
    public static final byte SUBCLASS1_NONE = 0;
    public static final byte SUBCLASS2_CARD_READER = 6;
    public static final byte SUBCLASS2_DIGITIZER_TABLET = 5;
    public static final byte SUBCLASS2_GAMEPAD = 2;
    public static final byte SUBCLASS2_JOYSTICK = 1;
    public static final byte SUBCLASS2_REMOTE_CONTROL = 3;
    public static final byte SUBCLASS2_SENSING_DEVICE = 4;
    public static final byte SUBCLASS2_UNCATEGORIZED = 0;
    private static final String TAG = BluetoothHidDevice.class.getSimpleName();
    private Context mContext;
    private volatile IBluetoothHidDevice mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            Log.d(BluetoothHidDevice.TAG, "onBluetoothStateChange: up=" + z);
            synchronized (BluetoothHidDevice.this.mConnection) {
                if (z) {
                    try {
                        if (BluetoothHidDevice.this.mService == null) {
                            Log.d(BluetoothHidDevice.TAG, "Binding HID Device service...");
                            BluetoothHidDevice.this.doBind();
                        }
                    } catch (IllegalStateException e) {
                        Log.e(BluetoothHidDevice.TAG, "onBluetoothStateChange: could not bind to HID Dev service: ", e);
                    } catch (SecurityException e2) {
                        Log.e(BluetoothHidDevice.TAG, "onBluetoothStateChange: could not bind to HID Dev service: ", e2);
                    }
                }
                Log.d(BluetoothHidDevice.TAG, "Unbinding service...");
                BluetoothHidDevice.this.doUnbind();
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(BluetoothHidDevice.TAG, "onServiceConnected()");
            BluetoothHidDevice.this.mService = IBluetoothHidDevice.Stub.asInterface(iBinder);
            if (BluetoothHidDevice.this.mServiceListener != null) {
                BluetoothHidDevice.this.mServiceListener.onServiceConnected(19, BluetoothHidDevice.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(BluetoothHidDevice.TAG, "onServiceDisconnected()");
            BluetoothHidDevice.this.mService = null;
            if (BluetoothHidDevice.this.mServiceListener != null) {
                BluetoothHidDevice.this.mServiceListener.onServiceDisconnected(19);
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    public static abstract class Callback {
        private static final String TAG = "BluetoothHidDevCallback";

        public void onAppStatusChanged(BluetoothDevice bluetoothDevice, boolean z) {
            Log.d(TAG, "onAppStatusChanged: pluggedDevice=" + bluetoothDevice + " registered=" + z);
        }

        public void onConnectionStateChanged(BluetoothDevice bluetoothDevice, int i) {
            Log.d(TAG, "onConnectionStateChanged: device=" + bluetoothDevice + " state=" + i);
        }

        public void onGetReport(BluetoothDevice bluetoothDevice, byte b, byte b2, int i) {
            Log.d(TAG, "onGetReport: device=" + bluetoothDevice + " type=" + ((int) b) + " id=" + ((int) b2) + " bufferSize=" + i);
        }

        public void onSetReport(BluetoothDevice bluetoothDevice, byte b, byte b2, byte[] bArr) {
            Log.d(TAG, "onSetReport: device=" + bluetoothDevice + " type=" + ((int) b) + " id=" + ((int) b2));
        }

        public void onSetProtocol(BluetoothDevice bluetoothDevice, byte b) {
            Log.d(TAG, "onSetProtocol: device=" + bluetoothDevice + " protocol=" + ((int) b));
        }

        public void onInterruptData(BluetoothDevice bluetoothDevice, byte b, byte[] bArr) {
            Log.d(TAG, "onInterruptData: device=" + bluetoothDevice + " reportId=" + ((int) b));
        }

        public void onVirtualCableUnplug(BluetoothDevice bluetoothDevice) {
            Log.d(TAG, "onVirtualCableUnplug: device=" + bluetoothDevice);
        }
    }

    private static class CallbackWrapper extends IBluetoothHidDeviceCallback.Stub {
        private final Callback mCallback;
        private final Executor mExecutor;

        CallbackWrapper(Executor executor, Callback callback) {
            this.mExecutor = executor;
            this.mCallback = callback;
        }

        @Override
        public void onAppStatusChanged(final BluetoothDevice bluetoothDevice, final boolean z) {
            clearCallingIdentity();
            this.mExecutor.execute(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onAppStatusChanged(bluetoothDevice, z);
                }
            });
        }

        @Override
        public void onConnectionStateChanged(final BluetoothDevice bluetoothDevice, final int i) {
            clearCallingIdentity();
            this.mExecutor.execute(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onConnectionStateChanged(bluetoothDevice, i);
                }
            });
        }

        @Override
        public void onGetReport(final BluetoothDevice bluetoothDevice, final byte b, final byte b2, final int i) {
            clearCallingIdentity();
            this.mExecutor.execute(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onGetReport(bluetoothDevice, b, b2, i);
                }
            });
        }

        @Override
        public void onSetReport(final BluetoothDevice bluetoothDevice, final byte b, final byte b2, final byte[] bArr) {
            clearCallingIdentity();
            this.mExecutor.execute(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onSetReport(bluetoothDevice, b, b2, bArr);
                }
            });
        }

        @Override
        public void onSetProtocol(final BluetoothDevice bluetoothDevice, final byte b) {
            clearCallingIdentity();
            this.mExecutor.execute(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onSetProtocol(bluetoothDevice, b);
                }
            });
        }

        @Override
        public void onInterruptData(final BluetoothDevice bluetoothDevice, final byte b, final byte[] bArr) {
            clearCallingIdentity();
            this.mExecutor.execute(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onInterruptData(bluetoothDevice, b, bArr);
                }
            });
        }

        @Override
        public void onVirtualCableUnplug(final BluetoothDevice bluetoothDevice) {
            clearCallingIdentity();
            this.mExecutor.execute(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.mCallback.onVirtualCableUnplug(bluetoothDevice);
                }
            });
        }
    }

    BluetoothHidDevice(Context context, BluetoothProfile.ServiceListener serviceListener) {
        this.mContext = context;
        this.mServiceListener = serviceListener;
        IBluetoothManager bluetoothManager = this.mAdapter.getBluetoothManager();
        if (bluetoothManager != null) {
            try {
                bluetoothManager.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothHidDevice.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth HID Device Service with " + intent);
            return false;
        }
        Log.d(TAG, "Bound to HID Device Service");
        return true;
    }

    void doUnbind() {
        this.mService = null;
        try {
            this.mContext.unbindService(this.mConnection);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unable to unbind HidDevService", e);
        }
    }

    void close() {
        IBluetoothManager bluetoothManager = this.mAdapter.getBluetoothManager();
        if (bluetoothManager != null) {
            try {
                bluetoothManager.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        synchronized (this.mConnection) {
            doUnbind();
        }
        this.mServiceListener = null;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return 0;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        return 0;
    }

    public boolean registerApp(BluetoothHidDeviceAppSdpSettings bluetoothHidDeviceAppSdpSettings, BluetoothHidDeviceAppQosSettings bluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings bluetoothHidDeviceAppQosSettings2, Executor executor, Callback callback) {
        if (bluetoothHidDeviceAppSdpSettings == null) {
            throw new IllegalArgumentException("sdp parameter cannot be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor parameter cannot be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback parameter cannot be null");
        }
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.registerApp(bluetoothHidDeviceAppSdpSettings, bluetoothHidDeviceAppQosSettings, bluetoothHidDeviceAppQosSettings2, new CallbackWrapper(executor, callback));
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean unregisterApp() {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.unregisterApp();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean sendReport(BluetoothDevice bluetoothDevice, int i, byte[] bArr) {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.sendReport(bluetoothDevice, i, bArr);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean replyReport(BluetoothDevice bluetoothDevice, byte b, byte b2, byte[] bArr) {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.replyReport(bluetoothDevice, b, b2, bArr);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean reportError(BluetoothDevice bluetoothDevice, byte b) {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.reportError(bluetoothDevice, b);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public String getUserAppName() {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.getUserAppName();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return "";
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        return "";
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.connect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        IBluetoothHidDevice iBluetoothHidDevice = this.mService;
        if (iBluetoothHidDevice != null) {
            try {
                return iBluetoothHidDevice.disconnect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }
}
