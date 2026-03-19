package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHealth;
import android.bluetooth.IBluetoothHealthCallback;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ProxyInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothHealth implements BluetoothProfile {
    public static final int APP_CONFIG_REGISTRATION_FAILURE = 1;
    public static final int APP_CONFIG_REGISTRATION_SUCCESS = 0;
    public static final int APP_CONFIG_UNREGISTRATION_FAILURE = 3;
    public static final int APP_CONFIG_UNREGISTRATION_SUCCESS = 2;
    public static final int CHANNEL_TYPE_ANY = 12;
    public static final int CHANNEL_TYPE_RELIABLE = 10;
    public static final int CHANNEL_TYPE_STREAMING = 11;
    private static final boolean DBG = true;
    public static final int HEALTH_OPERATION_ERROR = 6001;
    public static final int HEALTH_OPERATION_GENERIC_FAILURE = 6003;
    public static final int HEALTH_OPERATION_INVALID_ARGS = 6002;
    public static final int HEALTH_OPERATION_NOT_ALLOWED = 6005;
    public static final int HEALTH_OPERATION_NOT_FOUND = 6004;
    public static final int HEALTH_OPERATION_SUCCESS = 6000;
    public static final int SINK_ROLE = 2;
    public static final int SOURCE_ROLE = 1;
    public static final int STATE_CHANNEL_CONNECTED = 2;
    public static final int STATE_CHANNEL_CONNECTING = 1;
    public static final int STATE_CHANNEL_DISCONNECTED = 0;
    public static final int STATE_CHANNEL_DISCONNECTING = 3;
    private static final String TAG = "BluetoothHealth";
    private static final boolean VDBG = true;
    private Context mContext;
    private IBluetoothHealth mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean up) {
            ServiceConnection serviceConnection;
            Log.d(BluetoothHealth.TAG, "onBluetoothStateChange: up=" + up);
            if (!up) {
                Log.d(BluetoothHealth.TAG, "Unbinding service...");
                serviceConnection = BluetoothHealth.this.mConnection;
                synchronized (serviceConnection) {
                    try {
                        BluetoothHealth.this.mService = null;
                        BluetoothHealth.this.mContext.unbindService(BluetoothHealth.this.mConnection);
                    } catch (Exception re) {
                        Log.e(BluetoothHealth.TAG, ProxyInfo.LOCAL_EXCL_LIST, re);
                    }
                }
            } else {
                serviceConnection = BluetoothHealth.this.mConnection;
                synchronized (serviceConnection) {
                    try {
                        if (BluetoothHealth.this.mService == null) {
                            Log.d(BluetoothHealth.TAG, "Binding service...");
                            BluetoothHealth.this.doBind();
                        }
                    } catch (Exception re2) {
                        Log.e(BluetoothHealth.TAG, ProxyInfo.LOCAL_EXCL_LIST, re2);
                    }
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(BluetoothHealth.TAG, "Proxy object connected");
            BluetoothHealth.this.mService = IBluetoothHealth.Stub.asInterface(service);
            if (BluetoothHealth.this.mServiceListener == null) {
                return;
            }
            BluetoothHealth.this.mServiceListener.onServiceConnected(3, BluetoothHealth.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(BluetoothHealth.TAG, "Proxy object disconnected");
            BluetoothHealth.this.mService = null;
            if (BluetoothHealth.this.mServiceListener == null) {
                return;
            }
            BluetoothHealth.this.mServiceListener.onServiceDisconnected(3);
        }
    };
    BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    public boolean registerSinkAppConfiguration(String name, int dataType, BluetoothHealthCallback callback) {
        if (!isEnabled() || name == null) {
            return false;
        }
        log("registerSinkApplication(" + name + ":" + dataType + ")");
        return registerAppConfiguration(name, dataType, 2, 12, callback);
    }

    public boolean registerAppConfiguration(String name, int dataType, int role, int channelType, BluetoothHealthCallback callback) {
        if (!isEnabled() || !checkAppParam(name, role, channelType, callback)) {
            return false;
        }
        log("registerApplication(" + name + ":" + dataType + ")");
        BluetoothHealthCallbackWrapper wrapper = new BluetoothHealthCallbackWrapper(callback);
        BluetoothHealthAppConfiguration config = new BluetoothHealthAppConfiguration(name, dataType, role, channelType);
        if (this.mService != null) {
            try {
                boolean result = this.mService.registerAppConfiguration(config, wrapper);
                return result;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        Log.d(TAG, Log.getStackTraceString(new Throwable()));
        return false;
    }

    public boolean unregisterAppConfiguration(BluetoothHealthAppConfiguration config) {
        if (this.mService != null && isEnabled() && config != null) {
            try {
                boolean result = this.mService.unregisterAppConfiguration(config);
                return result;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        Log.d(TAG, Log.getStackTraceString(new Throwable()));
        return false;
    }

    public boolean connectChannelToSource(BluetoothDevice device, BluetoothHealthAppConfiguration config) {
        if (this.mService != null && isEnabled() && isValidDevice(device) && config != null) {
            try {
                return this.mService.connectChannelToSource(device, config);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        Log.d(TAG, Log.getStackTraceString(new Throwable()));
        return false;
    }

    public boolean connectChannelToSink(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelType) {
        if (this.mService != null && isEnabled() && isValidDevice(device) && config != null) {
            try {
                return this.mService.connectChannelToSink(device, config, channelType);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        Log.d(TAG, Log.getStackTraceString(new Throwable()));
        return false;
    }

    public boolean disconnectChannel(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelId) {
        if (this.mService != null && isEnabled() && isValidDevice(device) && config != null) {
            try {
                return this.mService.disconnectChannel(device, config, channelId);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        Log.d(TAG, Log.getStackTraceString(new Throwable()));
        return false;
    }

    public ParcelFileDescriptor getMainChannelFd(BluetoothDevice device, BluetoothHealthAppConfiguration config) {
        if (this.mService != null && isEnabled() && isValidDevice(device) && config != null) {
            try {
                return this.mService.getMainChannelFd(device, config);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return null;
    }

    @Override
    public int getConnectionState(BluetoothDevice device) {
        if (this.mService != null && isEnabled() && isValidDevice(device)) {
            try {
                return this.mService.getHealthDeviceConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return 0;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        Log.d(TAG, Log.getStackTraceString(new Throwable()));
        return 0;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        if (this.mService != null && isEnabled()) {
            try {
                return this.mService.getConnectedHealthDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (this.mService != null && isEnabled()) {
            try {
                return this.mService.getHealthDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    private static class BluetoothHealthCallbackWrapper extends IBluetoothHealthCallback.Stub {
        private BluetoothHealthCallback mCallback;

        public BluetoothHealthCallbackWrapper(BluetoothHealthCallback callback) {
            this.mCallback = callback;
        }

        @Override
        public void onHealthAppConfigurationStatusChange(BluetoothHealthAppConfiguration config, int status) {
            this.mCallback.onHealthAppConfigurationStatusChange(config, status);
        }

        @Override
        public void onHealthChannelStateChange(BluetoothHealthAppConfiguration config, BluetoothDevice device, int prevState, int newState, ParcelFileDescriptor fd, int channelId) {
            this.mCallback.onHealthChannelStateChange(config, device, prevState, newState, fd, channelId);
        }
    }

    BluetoothHealth(Context context, BluetoothProfile.ServiceListener l) {
        this.mContext = context;
        this.mServiceListener = l;
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                Log.d(TAG, "Register mBluetoothStateChangeCallback = " + this.mBluetoothStateChangeCallback);
                mgr.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothHealth.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle())) {
            Log.e(TAG, "Could not bind to Bluetooth Health Service with " + intent);
            return false;
        }
        return true;
    }

    void close() {
        log("close()");
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                Log.d(TAG, "Unregister mBluetoothStateChangeCallback = " + this.mBluetoothStateChangeCallback);
                mgr.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        synchronized (this.mConnection) {
            if (this.mService != null) {
                try {
                    this.mService = null;
                    this.mContext.unbindService(this.mConnection);
                } catch (Exception re) {
                    Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, re);
                }
            }
        }
        this.mServiceListener = null;
    }

    private boolean isEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.getState() == 12) {
            return true;
        }
        log("Bluetooth is Not enabled");
        return false;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private boolean checkAppParam(String name, int role, int channelType, BluetoothHealthCallback callback) {
        if (name == null || (!(role == 1 || role == 2) || (!(channelType == 10 || channelType == 11 || channelType == 12) || callback == null))) {
            return false;
        }
        return (role == 1 && channelType == 12) ? false : true;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
