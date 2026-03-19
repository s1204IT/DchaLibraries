package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothA2dpSink;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.ims.ImsConferenceState;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothA2dpSink implements BluetoothProfile {
    public static final String ACTION_AUDIO_CONFIG_CHANGED = "android.bluetooth.a2dp-sink.profile.action.AUDIO_CONFIG_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYING_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final String EXTRA_AUDIO_CONFIG = "android.bluetooth.a2dp-sink.profile.extra.AUDIO_CONFIG";
    public static final int STATE_NOT_PLAYING = 11;
    public static final int STATE_PLAYING = 10;
    private static final String TAG = "BluetoothA2dpSink";
    private static final boolean VDBG = false;
    private Context mContext;
    private volatile IBluetoothA2dpSink mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            Log.d(BluetoothA2dpSink.TAG, "onBluetoothStateChange: up=" + z);
            if (!z) {
                synchronized (BluetoothA2dpSink.this.mConnection) {
                    try {
                        BluetoothA2dpSink.this.mService = null;
                        BluetoothA2dpSink.this.mContext.unbindService(BluetoothA2dpSink.this.mConnection);
                    } catch (Exception e) {
                        Log.e(BluetoothA2dpSink.TAG, "", e);
                    }
                }
                return;
            }
            synchronized (BluetoothA2dpSink.this.mConnection) {
                try {
                } catch (Exception e2) {
                    Log.e(BluetoothA2dpSink.TAG, "", e2);
                }
                if (BluetoothA2dpSink.this.mService == null) {
                    BluetoothA2dpSink.this.doBind();
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(BluetoothA2dpSink.TAG, "Proxy object connected");
            BluetoothA2dpSink.this.mService = IBluetoothA2dpSink.Stub.asInterface(Binder.allowBlocking(iBinder));
            if (BluetoothA2dpSink.this.mServiceListener != null) {
                BluetoothA2dpSink.this.mServiceListener.onServiceConnected(11, BluetoothA2dpSink.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(BluetoothA2dpSink.TAG, "Proxy object disconnected");
            BluetoothA2dpSink.this.mService = null;
            if (BluetoothA2dpSink.this.mServiceListener != null) {
                BluetoothA2dpSink.this.mServiceListener.onServiceDisconnected(11);
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothA2dpSink(Context context, BluetoothProfile.ServiceListener serviceListener) {
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
        Intent intent = new Intent(IBluetoothA2dpSink.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth A2DP Service with " + intent);
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

    public boolean connect(BluetoothDevice bluetoothDevice) {
        log("connect(" + bluetoothDevice + ")");
        IBluetoothA2dpSink iBluetoothA2dpSink = this.mService;
        if (iBluetoothA2dpSink != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothA2dpSink.connect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothA2dpSink == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        log("disconnect(" + bluetoothDevice + ")");
        IBluetoothA2dpSink iBluetoothA2dpSink = this.mService;
        if (iBluetoothA2dpSink != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothA2dpSink.disconnect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothA2dpSink == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothA2dpSink iBluetoothA2dpSink = this.mService;
        if (iBluetoothA2dpSink != null && isEnabled()) {
            try {
                return iBluetoothA2dpSink.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothA2dpSink == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        IBluetoothA2dpSink iBluetoothA2dpSink = this.mService;
        if (iBluetoothA2dpSink != null && isEnabled()) {
            try {
                return iBluetoothA2dpSink.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothA2dpSink == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        IBluetoothA2dpSink iBluetoothA2dpSink = this.mService;
        if (iBluetoothA2dpSink != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothA2dpSink.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothA2dpSink == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public BluetoothAudioConfig getAudioConfig(BluetoothDevice bluetoothDevice) {
        IBluetoothA2dpSink iBluetoothA2dpSink = this.mService;
        if (iBluetoothA2dpSink != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothA2dpSink.getAudioConfig(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return null;
            }
        }
        if (iBluetoothA2dpSink == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return null;
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        log("setPriority(" + bluetoothDevice + ", " + i + ")");
        IBluetoothA2dpSink iBluetoothA2dpSink = this.mService;
        if (iBluetoothA2dpSink != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            if (i != 0 && i != 100) {
                return false;
            }
            try {
                return iBluetoothA2dpSink.setPriority(bluetoothDevice, i);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothA2dpSink == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        IBluetoothA2dpSink iBluetoothA2dpSink = this.mService;
        if (iBluetoothA2dpSink != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothA2dpSink.getPriority(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothA2dpSink == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean isA2dpPlaying(BluetoothDevice bluetoothDevice) {
        IBluetoothA2dpSink iBluetoothA2dpSink = this.mService;
        if (iBluetoothA2dpSink != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothA2dpSink.isA2dpPlaying(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothA2dpSink == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public static String stateToString(int i) {
        switch (i) {
            case 0:
                return ImsConferenceState.STATUS_DISCONNECTED;
            case 1:
                return "connecting";
            case 2:
                return "connected";
            case 3:
                return ImsConferenceState.STATUS_DISCONNECTING;
            default:
                switch (i) {
                    case 10:
                        return "playing";
                    case 11:
                        return "not playing";
                    default:
                        return "<unknown state " + i + ">";
                }
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
