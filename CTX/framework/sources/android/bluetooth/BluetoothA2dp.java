package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothA2dp;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.telephony.ims.ImsConferenceState;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class BluetoothA2dp implements BluetoothProfile {
    public static final String ACTION_ACTIVE_DEVICE_CHANGED = "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED";
    public static final String ACTION_AVRCP_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.AVRCP_CONNECTION_STATE_CHANGED";
    public static final String ACTION_CODEC_CONFIG_CHANGED = "android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYING_STATE_CHANGED = "android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final int OPTIONAL_CODECS_NOT_SUPPORTED = 0;
    public static final int OPTIONAL_CODECS_PREF_DISABLED = 0;
    public static final int OPTIONAL_CODECS_PREF_ENABLED = 1;
    public static final int OPTIONAL_CODECS_PREF_UNKNOWN = -1;
    public static final int OPTIONAL_CODECS_SUPPORTED = 1;
    public static final int OPTIONAL_CODECS_SUPPORT_UNKNOWN = -1;
    public static final int STATE_NOT_PLAYING = 11;
    public static final int STATE_PLAYING = 10;
    private static final String TAG = "BluetoothA2dp";
    private static final boolean VDBG = false;
    private Context mContext;

    @GuardedBy("mServiceLock")
    private IBluetoothA2dp mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final ReentrantReadWriteLock mServiceLock = new ReentrantReadWriteLock();
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            Log.d(BluetoothA2dp.TAG, "onBluetoothStateChange: up=" + z);
            if (!z) {
                try {
                    try {
                        BluetoothA2dp.this.mServiceLock.writeLock().lock();
                        BluetoothA2dp.this.mService = null;
                        BluetoothA2dp.this.mContext.unbindService(BluetoothA2dp.this.mConnection);
                    } catch (Exception e) {
                        Log.e(BluetoothA2dp.TAG, "", e);
                    }
                    return;
                } finally {
                    BluetoothA2dp.this.mServiceLock.writeLock().unlock();
                }
            }
            try {
                try {
                    BluetoothA2dp.this.mServiceLock.readLock().lock();
                    if (BluetoothA2dp.this.mService == null) {
                        BluetoothA2dp.this.doBind();
                    }
                } catch (Exception e2) {
                    Log.e(BluetoothA2dp.TAG, "", e2);
                }
            } finally {
                BluetoothA2dp.this.mServiceLock.readLock().unlock();
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(BluetoothA2dp.TAG, "Proxy object connected");
            try {
                BluetoothA2dp.this.mServiceLock.writeLock().lock();
                BluetoothA2dp.this.mService = IBluetoothA2dp.Stub.asInterface(Binder.allowBlocking(iBinder));
                BluetoothA2dp.this.mServiceLock.writeLock().unlock();
                if (BluetoothA2dp.this.mServiceListener != null) {
                    BluetoothA2dp.this.mServiceListener.onServiceConnected(2, BluetoothA2dp.this);
                }
            } catch (Throwable th) {
                BluetoothA2dp.this.mServiceLock.writeLock().unlock();
                throw th;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(BluetoothA2dp.TAG, "Proxy object disconnected");
            try {
                BluetoothA2dp.this.mServiceLock.writeLock().lock();
                BluetoothA2dp.this.mService = null;
                BluetoothA2dp.this.mServiceLock.writeLock().unlock();
                if (BluetoothA2dp.this.mServiceListener != null) {
                    BluetoothA2dp.this.mServiceListener.onServiceDisconnected(2);
                }
            } catch (Throwable th) {
                BluetoothA2dp.this.mServiceLock.writeLock().unlock();
                throw th;
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothA2dp(Context context, BluetoothProfile.ServiceListener serviceListener) {
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
        Intent intent = new Intent(IBluetoothA2dp.class.getName());
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
        try {
            try {
                this.mServiceLock.writeLock().lock();
                if (this.mService != null) {
                    this.mService = null;
                    this.mContext.unbindService(this.mConnection);
                }
            } catch (Exception e2) {
                Log.e(TAG, "", e2);
            }
        } finally {
            this.mServiceLock.writeLock().unlock();
        }
    }

    public void finalize() {
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        log("connect(" + bluetoothDevice + ")");
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.connect(bluetoothDevice);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        log("disconnect(" + bluetoothDevice + ")");
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.disconnect(bluetoothDevice);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                return this.mService.getConnectedDevices();
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new ArrayList();
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                return this.mService.getDevicesMatchingConnectionStates(iArr);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new ArrayList();
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.getConnectionState(bluetoothDevice);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return 0;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        log("setActiveDevice(" + bluetoothDevice + ")");
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && (bluetoothDevice == null || isValidDevice(bluetoothDevice))) {
                return this.mService.setActiveDevice(bluetoothDevice);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public BluetoothDevice getActiveDevice() {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                return this.mService.getActiveDevice();
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return null;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        log("setPriority(" + bluetoothDevice + ", " + i + ")");
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled() || !isValidDevice(bluetoothDevice)) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                return false;
            }
            if (i == 0 || i == 100) {
                return this.mService.setPriority(bluetoothDevice, i);
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.getPriority(bluetoothDevice);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return 0;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isAvrcpAbsoluteVolumeSupported() {
        Log.d(TAG, "isAvrcpAbsoluteVolumeSupported");
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                return this.mService.isAvrcpAbsoluteVolumeSupported();
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in isAvrcpAbsoluteVolumeSupported()", e);
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public void setAvrcpAbsoluteVolume(int i) {
        Log.d(TAG, "setAvrcpAbsoluteVolume");
        try {
            try {
                this.mServiceLock.readLock().lock();
                if (this.mService != null && isEnabled()) {
                    this.mService.setAvrcpAbsoluteVolume(i);
                }
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in setAvrcpAbsoluteVolume()", e);
            }
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean isA2dpPlaying(BluetoothDevice bluetoothDevice) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.isA2dpPlaying(bluetoothDevice);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public boolean shouldSendVolumeKeys(BluetoothDevice bluetoothDevice) {
        ParcelUuid[] uuids;
        if (!isEnabled() || !isValidDevice(bluetoothDevice) || (uuids = bluetoothDevice.getUuids()) == null) {
            return false;
        }
        for (ParcelUuid parcelUuid : uuids) {
            if (BluetoothUuid.isAvrcpTarget(parcelUuid)) {
                return true;
            }
        }
        return false;
    }

    public BluetoothCodecStatus getCodecStatus(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "getCodecStatus(" + bluetoothDevice + ")");
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                return this.mService.getCodecStatus(bluetoothDevice);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getCodecStatus()", e);
            return null;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public void setCodecConfigPreference(BluetoothDevice bluetoothDevice, BluetoothCodecConfig bluetoothCodecConfig) {
        Log.d(TAG, "setCodecConfigPreference(" + bluetoothDevice + ")");
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                this.mService.setCodecConfigPreference(bluetoothDevice, bluetoothCodecConfig);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in setCodecConfigPreference()", e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public void enableOptionalCodecs(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "enableOptionalCodecs(" + bluetoothDevice + ")");
        enableDisableOptionalCodecs(bluetoothDevice, true);
    }

    public void disableOptionalCodecs(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "disableOptionalCodecs(" + bluetoothDevice + ")");
        enableDisableOptionalCodecs(bluetoothDevice, false);
    }

    private void enableDisableOptionalCodecs(BluetoothDevice bluetoothDevice, boolean z) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                if (z) {
                    this.mService.enableOptionalCodecs(bluetoothDevice);
                } else {
                    this.mService.disableOptionalCodecs(bluetoothDevice);
                }
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in enableDisableOptionalCodecs()", e);
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int supportsOptionalCodecs(BluetoothDevice bluetoothDevice) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.supportsOptionalCodecs(bluetoothDevice);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getSupportsOptionalCodecs()", e);
            return -1;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getOptionalCodecsEnabled(BluetoothDevice bluetoothDevice) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.getOptionalCodecsEnabled(bluetoothDevice);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return -1;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getSupportsOptionalCodecs()", e);
            return -1;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public void setOptionalCodecsEnabled(BluetoothDevice bluetoothDevice, int i) {
        try {
            if (i != -1 && i != 0 && i != 1) {
                Log.e(TAG, "Invalid value passed to setOptionalCodecsEnabled: " + i);
                return;
            }
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                this.mService.setOptionalCodecsEnabled(bluetoothDevice, i);
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
        } finally {
            this.mServiceLock.readLock().unlock();
        }
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

    private boolean isValidDevice(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice != null && BluetoothAdapter.checkBluetoothAddress(bluetoothDevice.getAddress());
    }

    private static void log(String str) {
        Log.d(TAG, str);
    }
}
