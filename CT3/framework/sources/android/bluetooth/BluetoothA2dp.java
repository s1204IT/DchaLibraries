package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothA2dp;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ProxyInfo;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothA2dp implements BluetoothProfile {
    public static final String ACTION_AVRCP_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.AVRCP_CONNECTION_STATE_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYING_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final int STATE_NOT_PLAYING = 11;
    public static final int STATE_PLAYING = 10;
    private static final String TAG = "BluetoothA2dp";
    private static final boolean VDBG = true;
    private Context mContext;
    private IBluetoothA2dp mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private boolean mNeedNotifyServiceDisconnect = false;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean up) {
            ServiceConnection serviceConnection;
            Log.d(BluetoothA2dp.TAG, "onBluetoothStateChange: up=" + up);
            if (!up) {
                Log.d(BluetoothA2dp.TAG, "Unbinding service...");
                serviceConnection = BluetoothA2dp.this.mConnection;
                synchronized (serviceConnection) {
                    try {
                        BluetoothA2dp.this.mService = null;
                        BluetoothA2dp.this.mContext.unbindService(BluetoothA2dp.this.mConnection);
                        if (BluetoothA2dp.this.mServiceListener != null && BluetoothA2dp.this.mNeedNotifyServiceDisconnect) {
                            Log.d(BluetoothA2dp.TAG, "onBluetoothStateChange: A2DP proxy disconnect");
                            BluetoothA2dp.this.mServiceListener.onServiceDisconnected(2);
                        }
                        BluetoothA2dp.this.mNeedNotifyServiceDisconnect = false;
                    } catch (Exception re) {
                        Log.e(BluetoothA2dp.TAG, ProxyInfo.LOCAL_EXCL_LIST, re);
                    }
                }
            } else {
                serviceConnection = BluetoothA2dp.this.mConnection;
                synchronized (serviceConnection) {
                    try {
                        if (BluetoothA2dp.this.mService == null) {
                            Log.d(BluetoothA2dp.TAG, "Binding service...");
                            BluetoothA2dp.this.doBind();
                        }
                    } catch (Exception re2) {
                        Log.e(BluetoothA2dp.TAG, ProxyInfo.LOCAL_EXCL_LIST, re2);
                    }
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            synchronized (BluetoothA2dp.this.mConnection) {
                Log.d(BluetoothA2dp.TAG, "Proxy object connected");
                BluetoothA2dp.this.mService = IBluetoothA2dp.Stub.asInterface(service);
                if (BluetoothA2dp.this.mServiceListener != null) {
                    BluetoothA2dp.this.mServiceListener.onServiceConnected(2, BluetoothA2dp.this);
                }
                BluetoothA2dp.this.mNeedNotifyServiceDisconnect = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            synchronized (BluetoothA2dp.this.mConnection) {
                Log.d(BluetoothA2dp.TAG, "Proxy object disconnected");
                BluetoothA2dp.this.mService = null;
                if (BluetoothA2dp.this.mServiceListener != null) {
                    BluetoothA2dp.this.mServiceListener.onServiceDisconnected(2);
                }
                BluetoothA2dp.this.mNeedNotifyServiceDisconnect = false;
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothA2dp(Context context, BluetoothProfile.ServiceListener l) {
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
        Intent intent = new Intent(IBluetoothA2dp.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle())) {
            Log.e(TAG, "Could not bind to Bluetooth A2DP Service with " + intent);
            return false;
        }
        return true;
    }

    void close() {
        this.mServiceListener = null;
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
    }

    public void finalize() {
    }

    public boolean connect(BluetoothDevice device) {
        log("connect(" + device + ")");
        if (this.mService != null && isEnabled() && isValidDevice(device)) {
            try {
                return this.mService.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        log("disconnect(" + device + ")");
        if (this.mService != null && isEnabled() && isValidDevice(device)) {
            try {
                return this.mService.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        log("getConnectedDevices()");
        if (this.mService != null && isEnabled()) {
            try {
                return this.mService.getConnectedDevices();
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
        log("getDevicesMatchingStates()");
        if (this.mService != null && isEnabled()) {
            try {
                return this.mService.getDevicesMatchingConnectionStates(states);
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
    public int getConnectionState(BluetoothDevice device) {
        log("getState(" + device + ")");
        if (this.mService != null && isEnabled() && isValidDevice(device)) {
            try {
                return this.mService.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
        log("setPriority(" + device + ", " + priority + ")");
        if (this.mService != null && isEnabled() && isValidDevice(device)) {
            if (priority != 0 && priority != 100) {
                return false;
            }
            try {
                return this.mService.setPriority(device, priority);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public int getPriority(BluetoothDevice device) {
        log("getPriority(" + device + ")");
        if (this.mService != null && isEnabled() && isValidDevice(device)) {
            try {
                return this.mService.getPriority(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean isAvrcpAbsoluteVolumeSupported() {
        Log.d(TAG, "isAvrcpAbsoluteVolumeSupported");
        if (this.mService != null && isEnabled()) {
            try {
                return this.mService.isAvrcpAbsoluteVolumeSupported();
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in isAvrcpAbsoluteVolumeSupported()", e);
                return false;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public void adjustAvrcpAbsoluteVolume(int direction) {
        Log.d(TAG, "adjustAvrcpAbsoluteVolume");
        if (this.mService != null && isEnabled()) {
            try {
                this.mService.adjustAvrcpAbsoluteVolume(direction);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in adjustAvrcpAbsoluteVolume()", e);
                return;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
    }

    public void setAvrcpAbsoluteVolume(int volume) {
        Log.d(TAG, "setAvrcpAbsoluteVolume");
        if (this.mService != null && isEnabled()) {
            try {
                this.mService.setAvrcpAbsoluteVolume(volume);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in setAvrcpAbsoluteVolume()", e);
                return;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
    }

    public boolean isA2dpPlaying(BluetoothDevice device) {
        if (this.mService != null && isEnabled() && isValidDevice(device)) {
            try {
                return this.mService.isA2dpPlaying(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean shouldSendVolumeKeys(BluetoothDevice device) {
        ParcelUuid[] uuids;
        if (!isEnabled() || !isValidDevice(device) || (uuids = device.getUuids()) == null) {
            return false;
        }
        for (ParcelUuid uuid : uuids) {
            if (BluetoothUuid.isAvrcpTarget(uuid)) {
                return true;
            }
        }
        return false;
    }

    public static String stateToString(int state) {
        switch (state) {
            case 0:
                return "disconnected";
            case 1:
                return "connecting";
            case 2:
                return "connected";
            case 3:
                return "disconnecting";
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            default:
                return "<unknown state " + state + ">";
            case 10:
                return "playing";
            case 11:
                return "not playing";
        }
    }

    private boolean isEnabled() {
        return this.mAdapter.getState() == 12;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
