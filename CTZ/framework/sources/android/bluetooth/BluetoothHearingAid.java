package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHearingAid;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.telephony.ims.ImsConferenceState;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class BluetoothHearingAid implements BluetoothProfile {
    public static final String ACTION_ACTIVE_DEVICE_CHANGED = "android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYING_STATE_CHANGED = "android.bluetooth.hearingaid.profile.action.PLAYING_STATE_CHANGED";
    private static final boolean DBG = false;
    public static final long HI_SYNC_ID_INVALID = 0;
    public static final int MODE_BINAURAL = 1;
    public static final int MODE_MONAURAL = 0;
    public static final int SIDE_LEFT = 0;
    public static final int SIDE_RIGHT = 1;
    public static final int STATE_NOT_PLAYING = 11;
    public static final int STATE_PLAYING = 10;
    private static final String TAG = "BluetoothHearingAid";
    private static final boolean VDBG = false;
    private Context mContext;

    @GuardedBy("mServiceLock")
    private IBluetoothHearingAid mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final ReentrantReadWriteLock mServiceLock = new ReentrantReadWriteLock();
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            try {
                try {
                    if (!z) {
                        try {
                            BluetoothHearingAid.this.mServiceLock.writeLock().lock();
                            BluetoothHearingAid.this.mService = null;
                            BluetoothHearingAid.this.mContext.unbindService(BluetoothHearingAid.this.mConnection);
                        } catch (Exception e) {
                            Log.e(BluetoothHearingAid.TAG, "", e);
                        }
                        return;
                    }
                    try {
                        BluetoothHearingAid.this.mServiceLock.readLock().lock();
                        if (BluetoothHearingAid.this.mService == null) {
                            BluetoothHearingAid.this.doBind();
                        }
                    } catch (Exception e2) {
                        Log.e(BluetoothHearingAid.TAG, "", e2);
                    }
                } finally {
                    BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
                }
            } finally {
                BluetoothHearingAid.this.mServiceLock.readLock().unlock();
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            try {
                BluetoothHearingAid.this.mServiceLock.writeLock().lock();
                BluetoothHearingAid.this.mService = IBluetoothHearingAid.Stub.asInterface(Binder.allowBlocking(iBinder));
                BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
                if (BluetoothHearingAid.this.mServiceListener != null) {
                    BluetoothHearingAid.this.mServiceListener.onServiceConnected(21, BluetoothHearingAid.this);
                }
            } catch (Throwable th) {
                BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
                throw th;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            try {
                BluetoothHearingAid.this.mServiceLock.writeLock().lock();
                BluetoothHearingAid.this.mService = null;
                BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
                if (BluetoothHearingAid.this.mServiceListener != null) {
                    BluetoothHearingAid.this.mServiceListener.onServiceDisconnected(21);
                }
            } catch (Throwable th) {
                BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
                throw th;
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothHearingAid(Context context, BluetoothProfile.ServiceListener serviceListener) {
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

    void doBind() {
        Intent intent = new Intent(IBluetoothHearingAid.class.getName());
        ComponentName componentNameResolveSystemService = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(componentNameResolveSystemService);
        if (componentNameResolveSystemService == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle())) {
            Log.e(TAG, "Could not bind to Bluetooth Hearing Aid Service with " + intent);
        }
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
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && (bluetoothDevice == null || isValidDevice(bluetoothDevice))) {
                this.mService.setActiveDevice(bluetoothDevice);
                return true;
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

    public List<BluetoothDevice> getActiveDevices() {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                return this.mService.getActiveDevices();
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

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
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

    public int getVolume() {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled()) {
                return this.mService.getVolume();
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

    public void adjustVolume(int i) {
        try {
            try {
                this.mServiceLock.readLock().lock();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            } else if (isEnabled()) {
                this.mService.adjustVolume(i);
            }
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public void setVolume(int i) {
        try {
            try {
                this.mServiceLock.readLock().lock();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            } else if (isEnabled()) {
                this.mService.setVolume(i);
            }
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public long getHiSyncId(BluetoothDevice bluetoothDevice) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
                return 0L;
            }
            if (isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.getHiSyncId(bluetoothDevice);
            }
            return 0L;
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return 0L;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getDeviceSide(BluetoothDevice bluetoothDevice) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.getDeviceSide(bluetoothDevice);
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

    public int getDeviceMode(BluetoothDevice bluetoothDevice) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(bluetoothDevice)) {
                return this.mService.getDeviceMode(bluetoothDevice);
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
