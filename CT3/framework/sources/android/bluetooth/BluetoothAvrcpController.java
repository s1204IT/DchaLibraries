package android.bluetooth;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothAvrcpController;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.net.ProxyInfo;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothAvrcpController implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYER_SETTING = "android.bluetooth.avrcp-controller.profile.action.PLAYER_SETTING";
    public static final String ACTION_TRACK_EVENT = "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";
    private static final boolean DBG = false;
    public static final String EXTRA_METADATA = "android.bluetooth.avrcp-controller.profile.extra.METADATA";
    public static final String EXTRA_PLAYBACK = "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK";
    public static final String EXTRA_PLAYER_SETTING = "android.bluetooth.avrcp-controller.profile.extra.PLAYER_SETTING";
    public static final int KEY_STATE_PRESSED = 0;
    public static final int KEY_STATE_RELEASED = 1;
    public static final int PASS_THRU_CMD_ID_BACKWARD = 76;
    public static final int PASS_THRU_CMD_ID_FF = 73;
    public static final int PASS_THRU_CMD_ID_FORWARD = 75;
    public static final int PASS_THRU_CMD_ID_NEXT_GRP = 0;
    public static final int PASS_THRU_CMD_ID_PAUSE = 70;
    public static final int PASS_THRU_CMD_ID_PLAY = 68;
    public static final int PASS_THRU_CMD_ID_PREV_GRP = 1;
    public static final int PASS_THRU_CMD_ID_REWIND = 72;
    public static final int PASS_THRU_CMD_ID_STOP = 69;
    public static final int PASS_THRU_CMD_ID_VOL_DOWN = 66;
    public static final int PASS_THRU_CMD_ID_VOL_UP = 65;
    private static final String TAG = "BluetoothAvrcpController";
    private static final boolean VDBG = false;
    private Context mContext;
    private IBluetoothAvrcpController mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean up) {
            ServiceConnection serviceConnection;
            if (!up) {
                serviceConnection = BluetoothAvrcpController.this.mConnection;
                synchronized (serviceConnection) {
                    try {
                        BluetoothAvrcpController.this.mService = null;
                        BluetoothAvrcpController.this.mContext.unbindService(BluetoothAvrcpController.this.mConnection);
                    } catch (Exception re) {
                        Log.e(BluetoothAvrcpController.TAG, ProxyInfo.LOCAL_EXCL_LIST, re);
                    }
                }
            } else {
                serviceConnection = BluetoothAvrcpController.this.mConnection;
                synchronized (serviceConnection) {
                    try {
                        if (BluetoothAvrcpController.this.mService == null) {
                            BluetoothAvrcpController.this.doBind();
                        }
                    } catch (Exception re2) {
                        Log.e(BluetoothAvrcpController.TAG, ProxyInfo.LOCAL_EXCL_LIST, re2);
                    }
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothAvrcpController.this.mService = IBluetoothAvrcpController.Stub.asInterface(service);
            if (BluetoothAvrcpController.this.mServiceListener == null) {
                return;
            }
            BluetoothAvrcpController.this.mServiceListener.onServiceConnected(12, BluetoothAvrcpController.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            BluetoothAvrcpController.this.mService = null;
            if (BluetoothAvrcpController.this.mServiceListener == null) {
                return;
            }
            BluetoothAvrcpController.this.mServiceListener.onServiceDisconnected(12);
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothAvrcpController(Context context, BluetoothProfile.ServiceListener l) {
        this.mContext = context;
        this.mServiceListener = l;
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, ProxyInfo.LOCAL_EXCL_LIST, e);
            }
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothAvrcpController.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle())) {
            Log.e(TAG, "Could not bind to Bluetooth AVRCP Controller Service with " + intent);
            return false;
        }
        return true;
    }

    void close() {
        this.mServiceListener = null;
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
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
        close();
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
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

    public void sendPassThroughCmd(BluetoothDevice device, int keyCode, int keyState) {
        if (this.mService != null && isEnabled()) {
            try {
                this.mService.sendPassThroughCmd(device, keyCode, keyState);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in sendPassThroughCmd()", e);
                return;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
    }

    public BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice device) {
        if (this.mService == null || !isEnabled()) {
            return null;
        }
        try {
            BluetoothAvrcpPlayerSettings settings = this.mService.getPlayerSettings(device);
            return settings;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getMetadata() " + e);
            return null;
        }
    }

    public MediaMetadata getMetadata(BluetoothDevice device) {
        if (this.mService == null || !isEnabled()) {
            return null;
        }
        try {
            MediaMetadata metadata = this.mService.getMetadata(device);
            return metadata;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getMetadata() " + e);
            return null;
        }
    }

    public PlaybackState getPlaybackState(BluetoothDevice device) {
        if (this.mService == null || !isEnabled()) {
            return null;
        }
        try {
            PlaybackState playbackState = this.mService.getPlaybackState(device);
            return playbackState;
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in getPlaybackState() " + e);
            return null;
        }
    }

    public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings plAppSetting) {
        if (this.mService != null && isEnabled()) {
            try {
                return this.mService.setPlayerApplicationSetting(plAppSetting);
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in setPlayerApplicationSetting() " + e);
                return false;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public void sendGroupNavigationCmd(BluetoothDevice device, int keyCode, int keyState) {
        Log.d(TAG, "sendGroupNavigationCmd dev = " + device + " key " + keyCode + " State = " + keyState);
        if (this.mService != null && isEnabled()) {
            try {
                this.mService.sendGroupNavigationCmd(device, keyCode, keyState);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "Error talking to BT service in sendGroupNavigationCmd()", e);
                return;
            }
        }
        if (this.mService == null) {
            Log.w(TAG, "Proxy not attached to service");
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
