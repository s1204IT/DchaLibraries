package android.bluetooth;

import android.annotation.SystemApi;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHeadset;
import android.bluetooth.IBluetoothProfileServiceConnection;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothHeadset implements BluetoothProfile {
    public static final String ACTION_ACTIVE_DEVICE_CHANGED = "android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED";
    public static final String ACTION_AUDIO_STATE_CHANGED = "android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_HF_INDICATORS_VALUE_CHANGED = "android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED";
    public static final String ACTION_VENDOR_SPECIFIC_HEADSET_EVENT = "android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT";
    public static final int AT_CMD_TYPE_ACTION = 4;
    public static final int AT_CMD_TYPE_BASIC = 3;
    public static final int AT_CMD_TYPE_READ = 0;
    public static final int AT_CMD_TYPE_SET = 2;
    public static final int AT_CMD_TYPE_TEST = 1;
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    public static final String EXTRA_HF_INDICATORS_IND_ID = "android.bluetooth.headset.extra.HF_INDICATORS_IND_ID";
    public static final String EXTRA_HF_INDICATORS_IND_VALUE = "android.bluetooth.headset.extra.HF_INDICATORS_IND_VALUE";
    public static final String EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS = "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_ARGS";
    public static final String EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD = "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD";
    public static final String EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE = "android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE";
    private static final int MESSAGE_HEADSET_SERVICE_CONNECTED = 100;
    private static final int MESSAGE_HEADSET_SERVICE_DISCONNECTED = 101;
    public static final int STATE_AUDIO_CONNECTED = 12;
    public static final int STATE_AUDIO_CONNECTING = 11;
    public static final int STATE_AUDIO_DISCONNECTED = 10;
    private static final String TAG = "BluetoothHeadset";
    private static final boolean VDBG = false;
    public static final String VENDOR_RESULT_CODE_COMMAND_ANDROID = "+ANDROID";
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY = "android.bluetooth.headset.intent.category.companyid";
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV = "+IPHONEACCEV";
    public static final int VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL = 1;
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_XAPL = "+XAPL";
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_XEVENT = "+XEVENT";
    public static final String VENDOR_SPECIFIC_HEADSET_EVENT_XEVENT_BATTERY_LEVEL = "BATTERY";
    private Context mContext;
    private volatile IBluetoothHeadset mService;
    private BluetoothProfile.ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean z) {
            if (BluetoothHeadset.DBG) {
                Log.d(BluetoothHeadset.TAG, "onBluetoothStateChange: up=" + z);
            }
            if (!z) {
                synchronized (BluetoothHeadset.this.mAdapter) {
                    if (BluetoothHeadset.this.mService != null) {
                        if (BluetoothHeadset.DBG) {
                            Log.d(BluetoothHeadset.TAG, "Proxy object disconnected");
                        }
                        BluetoothHeadset.this.mService = null;
                        BluetoothHeadset.this.mHandler.sendMessage(BluetoothHeadset.this.mHandler.obtainMessage(101));
                    }
                }
                BluetoothHeadset.this.doUnbind();
                return;
            }
            synchronized (BluetoothHeadset.this.mConnection) {
                try {
                } catch (Exception e) {
                    Log.e(BluetoothHeadset.TAG, "", e);
                }
                if (BluetoothHeadset.this.mService == null) {
                    BluetoothHeadset.this.doBind();
                }
            }
        }
    };
    private final IBluetoothProfileServiceConnection mConnection = new IBluetoothProfileServiceConnection.Stub() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (BluetoothHeadset.DBG) {
                Log.d(BluetoothHeadset.TAG, "Proxy object connected");
            }
            BluetoothHeadset.this.mService = IBluetoothHeadset.Stub.asInterface(Binder.allowBlocking(iBinder));
            BluetoothHeadset.this.mHandler.sendMessage(BluetoothHeadset.this.mHandler.obtainMessage(100));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (BluetoothHeadset.this.mAdapter) {
                if (BluetoothHeadset.this.mService != null) {
                    if (BluetoothHeadset.DBG) {
                        Log.d(BluetoothHeadset.TAG, "Proxy object disconnected");
                    }
                    BluetoothHeadset.this.mService = null;
                    BluetoothHeadset.this.mHandler.sendMessage(BluetoothHeadset.this.mHandler.obtainMessage(101));
                }
            }
        }
    };
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 100:
                    if (BluetoothHeadset.this.mServiceListener != null) {
                        BluetoothHeadset.this.mServiceListener.onServiceConnected(1, BluetoothHeadset.this);
                    }
                    break;
                case 101:
                    if (BluetoothHeadset.this.mServiceListener != null) {
                        BluetoothHeadset.this.mServiceListener.onServiceDisconnected(1);
                    }
                    break;
            }
        }
    };
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothHeadset(Context context, BluetoothProfile.ServiceListener serviceListener) {
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
        try {
            return this.mAdapter.getBluetoothManager().bindBluetoothProfileService(1, this.mConnection);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to bind HeadsetService", e);
            return false;
        }
    }

    void doUnbind() {
        synchronized (this.mConnection) {
            try {
                this.mAdapter.getBluetoothManager().unbindBluetoothProfileService(1, this.mConnection);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to unbind HeadsetService", e);
            }
        }
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
        if (this.mService != null) {
            this.mService = null;
            doUnbind();
        }
        this.mServiceListener = null;
    }

    @SystemApi
    public boolean connect(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            log("connect(" + bluetoothDevice + ")");
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHeadset.connect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    @SystemApi
    public boolean disconnect(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            log("disconnect(" + bluetoothDevice + ")");
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHeadset.disconnect(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.getDevicesMatchingConnectionStates(iArr);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return new ArrayList();
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return new ArrayList();
    }

    @Override
    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHeadset.getConnectionState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    @SystemApi
    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        if (DBG) {
            log("setPriority(" + bluetoothDevice + ", " + i + ")");
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            if (i != 0 && i != 100) {
                return false;
            }
            try {
                return iBluetoothHeadset.setPriority(bluetoothDevice, i);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHeadset.getPriority(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public boolean startVoiceRecognition(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            log("startVoiceRecognition()");
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHeadset.startVoiceRecognition(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        return false;
    }

    public boolean stopVoiceRecognition(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            log("stopVoiceRecognition()");
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHeadset.stopVoiceRecognition(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        return false;
    }

    public boolean isAudioConnected(BluetoothDevice bluetoothDevice) {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHeadset.isAudioConnected(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        return false;
    }

    public static boolean isBluetoothVoiceDialingEnabled(Context context) {
        return context.getResources().getBoolean(R.bool.config_bluetooth_sco_off_call);
    }

    public int getAudioState(BluetoothDevice bluetoothDevice) {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && !isDisabled()) {
            try {
                return iBluetoothHeadset.getAudioState(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return 10;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
            return 10;
        }
        return 10;
    }

    public void setAudioRouteAllowed(boolean z) {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                iBluetoothHeadset.setAudioRouteAllowed(z);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
    }

    public boolean getAudioRouteAllowed() {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.getAudioRouteAllowed();
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

    public void setForceScoAudio(boolean z) {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                iBluetoothHeadset.setForceScoAudio(z);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
    }

    public boolean isAudioOn() {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.isAudioOn();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        return false;
    }

    public boolean connectAudio() {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.connectAudio();
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

    public boolean disconnectAudio() {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.disconnectAudio();
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

    public boolean startScoUsingVirtualVoiceCall() {
        if (DBG) {
            log("startScoUsingVirtualVoiceCall()");
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.startScoUsingVirtualVoiceCall();
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

    public boolean stopScoUsingVirtualVoiceCall() {
        if (DBG) {
            log("stopScoUsingVirtualVoiceCall()");
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.stopScoUsingVirtualVoiceCall();
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

    public void phoneStateChanged(int i, int i2, int i3, String str, int i4) {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                iBluetoothHeadset.phoneStateChanged(i, i2, i3, str, i4);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
    }

    public void clccResponse(int i, int i2, int i3, int i4, boolean z, String str, int i5) {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                iBluetoothHeadset.clccResponse(i, i2, i3, i4, z, str, i5);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        if (DBG) {
            Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
    }

    public boolean sendVendorSpecificResultCode(BluetoothDevice bluetoothDevice, String str, String str2) {
        if (DBG) {
            log("sendVendorSpecificResultCode()");
        }
        if (str == null) {
            throw new IllegalArgumentException("command is null");
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && isValidDevice(bluetoothDevice)) {
            try {
                return iBluetoothHeadset.sendVendorSpecificResultCode(bluetoothDevice, str, str2);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        return false;
    }

    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        if (DBG) {
            Log.d(TAG, "setActiveDevice: " + bluetoothDevice);
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled() && (bluetoothDevice == null || isValidDevice(bluetoothDevice))) {
            try {
                return iBluetoothHeadset.setActiveDevice(bluetoothDevice);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        return false;
    }

    public BluetoothDevice getActiveDevice() {
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.getActiveDevice();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
            return null;
        }
        return null;
    }

    public boolean isInbandRingingEnabled() {
        if (DBG) {
            log("isInbandRingingEnabled()");
        }
        IBluetoothHeadset iBluetoothHeadset = this.mService;
        if (iBluetoothHeadset != null && isEnabled()) {
            try {
                return iBluetoothHeadset.isInbandRingingEnabled();
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (iBluetoothHeadset == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        return false;
    }

    public static boolean isInbandRingingSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_bluetooth_hfp_inband_ringing_support);
    }

    private boolean isEnabled() {
        return this.mAdapter.getState() == 12;
    }

    private boolean isDisabled() {
        return this.mAdapter.getState() == 10;
    }

    private static boolean isValidDevice(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice != null && BluetoothAdapter.checkBluetoothAddress(bluetoothDevice.getAddress());
    }

    private static void log(String str) {
        Log.d(TAG, str);
    }
}
