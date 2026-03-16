package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.ProfileService;
import java.util.ArrayList;
import java.util.List;

public class HeadsetService extends ProfileService {
    private static final boolean DBG = false;
    private static final String MODIFY_PHONE_STATE = "android.permission.MODIFY_PHONE_STATE";
    private static final String TAG = "HeadsetService";
    private static HeadsetService sHeadsetService;
    private final BroadcastReceiver mHeadsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                HeadsetService.this.mStateMachine.sendMessage(10, intent);
                return;
            }
            if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                if (streamType == 6) {
                    HeadsetService.this.mStateMachine.sendMessage(7, intent);
                    return;
                }
                return;
            }
            if (action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY")) {
                int requestType = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                if (requestType == 2) {
                    Log.v(HeadsetService.TAG, "Received BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY");
                    HeadsetService.this.mStateMachine.handleAccessPermissionResult(intent);
                }
            }
        }
    };
    private HeadsetStateMachine mStateMachine;

    @Override
    protected String getName() {
        return TAG;
    }

    @Override
    public ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothHeadsetBinder(this);
    }

    @Override
    protected boolean start() {
        this.mStateMachine = HeadsetStateMachine.make(this);
        IntentFilter filter = new IntentFilter("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        try {
            registerReceiver(this.mHeadsetReceiver, filter);
        } catch (Exception e) {
            Log.w(TAG, "Unable to register headset receiver", e);
        }
        setHeadsetService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        try {
            if (this.mHeadsetReceiver != null) {
                unregisterReceiver(this.mHeadsetReceiver);
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to unregister headset receiver", e);
        }
        this.mStateMachine.doQuit();
        return true;
    }

    @Override
    protected boolean cleanup() {
        if (this.mStateMachine != null) {
            this.mStateMachine.cleanup();
        }
        clearHeadsetService();
        return true;
    }

    private static class BluetoothHeadsetBinder extends IBluetoothHeadset.Stub implements ProfileService.IProfileServiceBinder {
        private HeadsetService mService;

        public BluetoothHeadsetBinder(HeadsetService svc) {
            this.mService = svc;
        }

        @Override
        public boolean cleanup() {
            this.mService = null;
            return true;
        }

        private HeadsetService getService() {
            if (!Utils.checkCallerAllowManagedProfiles(this.mService)) {
                Log.w(HeadsetService.TAG, "Headset call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        public boolean connect(BluetoothDevice device) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.connect(device);
        }

        public boolean disconnect(BluetoothDevice device) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.disconnect(device);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            HeadsetService service = getService();
            return service == null ? new ArrayList(0) : service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
            HeadsetService service = getService();
            return service == null ? new ArrayList(0) : service.getDevicesMatchingConnectionStates(states);
        }

        public int getConnectionState(BluetoothDevice device) {
            HeadsetService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(device);
        }

        public boolean setPriority(BluetoothDevice device, int priority) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.setPriority(device, priority);
        }

        public int getPriority(BluetoothDevice device) {
            HeadsetService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(device);
        }

        public boolean startVoiceRecognition(BluetoothDevice device) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.startVoiceRecognition(device);
        }

        public boolean stopVoiceRecognition(BluetoothDevice device) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.stopVoiceRecognition(device);
        }

        public boolean isAudioOn() {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.isAudioOn();
        }

        public boolean isAudioConnected(BluetoothDevice device) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.isAudioConnected(device);
        }

        public int getBatteryUsageHint(BluetoothDevice device) {
            HeadsetService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getBatteryUsageHint(device);
        }

        public boolean acceptIncomingConnect(BluetoothDevice device) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.acceptIncomingConnect(device);
        }

        public boolean rejectIncomingConnect(BluetoothDevice device) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.rejectIncomingConnect(device);
        }

        public int getAudioState(BluetoothDevice device) {
            HeadsetService service = getService();
            if (service == null) {
                return 10;
            }
            return service.getAudioState(device);
        }

        public boolean connectAudio() {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.connectAudio();
        }

        public boolean disconnectAudio() {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.disconnectAudio();
        }

        public boolean startScoUsingVirtualVoiceCall(BluetoothDevice device) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.startScoUsingVirtualVoiceCall(device);
        }

        public boolean stopScoUsingVirtualVoiceCall(BluetoothDevice device) {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.stopScoUsingVirtualVoiceCall(device);
        }

        public void phoneStateChanged(int numActive, int numHeld, int callState, String number, int type) {
            HeadsetService service = getService();
            if (service != null) {
                service.phoneStateChanged(numActive, numHeld, callState, number, type);
            }
        }

        public void clccResponse(int index, int direction, int status, int mode, boolean mpty, String number, int type) {
            HeadsetService service = getService();
            if (service != null) {
                service.clccResponse(index, direction, status, mode, mpty, number, type);
            }
        }

        public boolean sendVendorSpecificResultCode(BluetoothDevice device, String command, String arg) {
            HeadsetService service = getService();
            if (service != null) {
                return service.sendVendorSpecificResultCode(device, command, arg);
            }
            return HeadsetService.DBG;
        }

        public boolean enableWBS() {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.enableWBS();
        }

        public boolean disableWBS() {
            HeadsetService service = getService();
            return service == null ? HeadsetService.DBG : service.disableWBS();
        }
    }

    public static synchronized HeadsetService getHeadsetService() {
        return (sHeadsetService == null || !sHeadsetService.isAvailable()) ? null : sHeadsetService;
    }

    private static synchronized void setHeadsetService(HeadsetService instance) {
        if (instance != null) {
            if (instance.isAvailable()) {
                sHeadsetService = instance;
            }
        }
    }

    private static synchronized void clearHeadsetService() {
        sHeadsetService = null;
    }

    public boolean connect(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        if (getPriority(device) == 0) {
            return DBG;
        }
        int connectionState = this.mStateMachine.getConnectionState(device);
        Log.d(TAG, "connectionState = " + connectionState);
        if (connectionState == 2 || connectionState == 1) {
            return DBG;
        }
        this.mStateMachine.sendMessage(1, device);
        return true;
    }

    boolean disconnect(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return DBG;
        }
        this.mStateMachine.sendMessage(2, device);
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mStateMachine.getConnectedDevices();
    }

    private List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mStateMachine.getDevicesMatchingConnectionStates(states);
    }

    int getConnectionState(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mStateMachine.getConnectionState(device);
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothHeadsetPriorityKey(device.getAddress()), priority);
        return true;
    }

    public int getPriority(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH_ADMIN permission");
        int priority = Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothHeadsetPriorityKey(device.getAddress()), -1);
        return priority;
    }

    boolean startVoiceRecognition(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return DBG;
        }
        this.mStateMachine.sendMessage(5);
        return true;
    }

    boolean stopVoiceRecognition(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return DBG;
        }
        this.mStateMachine.sendMessage(6);
        return true;
    }

    boolean isAudioOn() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mStateMachine.isAudioOn();
    }

    boolean isAudioConnected(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mStateMachine.isAudioConnected(device);
    }

    int getBatteryUsageHint(BluetoothDevice device) {
        return 0;
    }

    boolean acceptIncomingConnect(BluetoothDevice device) {
        return DBG;
    }

    boolean rejectIncomingConnect(BluetoothDevice device) {
        return DBG;
    }

    int getAudioState(BluetoothDevice device) {
        return this.mStateMachine.getAudioState(device);
    }

    boolean connectAudio() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (!this.mStateMachine.isConnected() || this.mStateMachine.isAudioOn()) {
            return DBG;
        }
        this.mStateMachine.sendMessage(3);
        return true;
    }

    boolean disconnectAudio() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (!this.mStateMachine.isAudioOn()) {
            return DBG;
        }
        this.mStateMachine.sendMessage(4);
        return true;
    }

    boolean startScoUsingVirtualVoiceCall(BluetoothDevice device) {
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return DBG;
        }
        this.mStateMachine.sendMessage(14, device);
        return true;
    }

    boolean stopScoUsingVirtualVoiceCall(BluetoothDevice device) {
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return DBG;
        }
        this.mStateMachine.sendMessage(15, device);
        return true;
    }

    private void phoneStateChanged(int numActive, int numHeld, int callState, String number, int type) {
        enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
        Message msg = this.mStateMachine.obtainMessage(9);
        msg.obj = new HeadsetCallState(numActive, numHeld, callState, number, type);
        msg.arg1 = 0;
        this.mStateMachine.sendMessage(msg);
    }

    private void clccResponse(int index, int direction, int status, int mode, boolean mpty, String number, int type) {
        enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, null);
        this.mStateMachine.sendMessage(12, new HeadsetClccResponse(index, direction, status, mode, mpty, number, type));
    }

    private boolean sendVendorSpecificResultCode(BluetoothDevice device, String command, String arg) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2) {
            return DBG;
        }
        if (!command.equals("+ANDROID")) {
            Log.w(TAG, "Disallowed unsolicited result code command: " + command);
            return DBG;
        }
        this.mStateMachine.sendMessage(13, new HeadsetVendorSpecificResultCode(device, command, arg));
        return true;
    }

    boolean enableWBS() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (!this.mStateMachine.isConnected() || this.mStateMachine.isAudioOn()) {
            return DBG;
        }
        for (BluetoothDevice device : getConnectedDevices()) {
            this.mStateMachine.sendMessage(16, device);
        }
        return true;
    }

    boolean disableWBS() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (!this.mStateMachine.isConnected() || this.mStateMachine.isAudioOn()) {
            return DBG;
        }
        for (BluetoothDevice device : getConnectedDevices()) {
            this.mStateMachine.sendMessage(17, device);
        }
        return true;
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        if (this.mStateMachine != null) {
            this.mStateMachine.dump(sb);
        }
    }
}
