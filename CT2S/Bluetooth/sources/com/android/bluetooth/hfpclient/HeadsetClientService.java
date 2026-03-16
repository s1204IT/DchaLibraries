package com.android.bluetooth.hfpclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.IBluetoothHeadsetClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.ProfileService;
import java.util.ArrayList;
import java.util.List;

public class HeadsetClientService extends ProfileService {
    private static final boolean DBG = true;
    private static final String TAG = "HeadsetClientService";
    private static HeadsetClientService sHeadsetClientService;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                if (streamType == 6) {
                    int streamValue = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                    int streamPrevValue = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1);
                    if (streamValue != -1 && streamValue != streamPrevValue) {
                        HeadsetClientService.this.mStateMachine.sendMessage(HeadsetClientService.this.mStateMachine.obtainMessage(8, streamValue, 0));
                    }
                }
            }
        }
    };
    private HeadsetClientStateMachine mStateMachine;

    @Override
    protected String getName() {
        return TAG;
    }

    @Override
    public ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothHeadsetClientBinder(this);
    }

    @Override
    protected boolean start() {
        this.mStateMachine = HeadsetClientStateMachine.make(this);
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        try {
            registerReceiver(this.mBroadcastReceiver, filter);
        } catch (Exception e) {
            Log.w(TAG, "Unable to register broadcat receiver", e);
        }
        setHeadsetClientService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        try {
            unregisterReceiver(this.mBroadcastReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Unable to unregister broadcast receiver", e);
        }
        this.mStateMachine.doQuit();
        return true;
    }

    @Override
    protected boolean cleanup() {
        if (this.mStateMachine != null) {
            this.mStateMachine.cleanup();
        }
        clearHeadsetClientService();
        return true;
    }

    private static class BluetoothHeadsetClientBinder extends IBluetoothHeadsetClient.Stub implements ProfileService.IProfileServiceBinder {
        private HeadsetClientService mService;

        public BluetoothHeadsetClientBinder(HeadsetClientService svc) {
            this.mService = svc;
        }

        @Override
        public boolean cleanup() {
            this.mService = null;
            return true;
        }

        private HeadsetClientService getService() {
            if (!Utils.checkCaller()) {
                Log.w(HeadsetClientService.TAG, "HeadsetClient call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        public boolean connect(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(device);
        }

        public boolean disconnect(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(device);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            HeadsetClientService service = getService();
            return service == null ? new ArrayList(0) : service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
            HeadsetClientService service = getService();
            if (service != null) {
                return service.getDevicesMatchingConnectionStates(states);
            }
            return new ArrayList(0);
        }

        public int getConnectionState(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(device);
        }

        public boolean setPriority(BluetoothDevice device, int priority) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(device, priority);
        }

        public int getPriority(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(device);
        }

        public boolean startVoiceRecognition(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.startVoiceRecognition(device);
        }

        public boolean stopVoiceRecognition(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.stopVoiceRecognition(device);
        }

        public boolean acceptIncomingConnect(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.acceptIncomingConnect(device);
        }

        public boolean rejectIncomingConnect(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.rejectIncomingConnect(device);
        }

        public int getAudioState(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getAudioState(device);
        }

        public boolean connectAudio() {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.connectAudio();
        }

        public boolean disconnectAudio() {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnectAudio();
        }

        public boolean acceptCall(BluetoothDevice device, int flag) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.acceptCall(device, flag);
        }

        public boolean rejectCall(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.rejectCall(device);
        }

        public boolean holdCall(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.holdCall(device);
        }

        public boolean terminateCall(BluetoothDevice device, int index) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.terminateCall(device, index);
        }

        public boolean explicitCallTransfer(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.explicitCallTransfer(device);
        }

        public boolean enterPrivateMode(BluetoothDevice device, int index) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.enterPrivateMode(device, index);
        }

        public boolean redial(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.redial(device);
        }

        public boolean dial(BluetoothDevice device, String number) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.dial(device, number);
        }

        public boolean dialMemory(BluetoothDevice device, int location) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.dialMemory(device, location);
        }

        public List<BluetoothHeadsetClientCall> getCurrentCalls(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return null;
            }
            return service.getCurrentCalls(device);
        }

        public boolean sendDTMF(BluetoothDevice device, byte code) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.sendDTMF(device, code);
        }

        public boolean getLastVoiceTagNumber(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.getLastVoiceTagNumber(device);
        }

        public Bundle getCurrentAgEvents(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return null;
            }
            return service.getCurrentAgEvents(device);
        }

        public Bundle getCurrentAgFeatures(BluetoothDevice device) {
            HeadsetClientService service = getService();
            if (service == null) {
                return null;
            }
            return service.getCurrentAgFeatures(device);
        }

        public BluetoothHeadsetClientCall getCall(BluetoothDevice device, int state) {
            HeadsetClientService service = getService();
            if (service == null) {
                return null;
            }
            return service.getCall(device, state);
        }
    }

    public static synchronized HeadsetClientService getHeadsetClientService() {
        HeadsetClientService headsetClientService;
        if (sHeadsetClientService != null && sHeadsetClientService.isAvailable()) {
            Log.d(TAG, "getHeadsetClientService(): returning " + sHeadsetClientService);
            headsetClientService = sHeadsetClientService;
        } else {
            if (sHeadsetClientService == null) {
                Log.d(TAG, "getHeadsetClientService(): service is NULL");
            } else if (!sHeadsetClientService.isAvailable()) {
                Log.d(TAG, "getHeadsetClientService(): service is not available");
            }
            headsetClientService = null;
        }
        return headsetClientService;
    }

    private static synchronized void setHeadsetClientService(HeadsetClientService instance) {
        if (instance != null) {
            if (instance.isAvailable()) {
                Log.d(TAG, "setHeadsetClientService(): set to: " + sHeadsetClientService);
                sHeadsetClientService = instance;
            } else if (sHeadsetClientService == null) {
                Log.d(TAG, "setHeadsetClientService(): service not available");
            } else if (!sHeadsetClientService.isAvailable()) {
                Log.d(TAG, "setHeadsetClientService(): service is cleaning up");
            }
        }
    }

    private static synchronized void clearHeadsetClientService() {
        sHeadsetClientService = null;
    }

    public boolean connect(BluetoothDevice device) {
        int connectionState;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        if (getPriority(device) == 0 || (connectionState = this.mStateMachine.getConnectionState(device)) == 2 || connectionState == 1) {
            return false;
        }
        this.mStateMachine.sendMessage(1, device);
        return true;
    }

    boolean disconnect(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH ADMIN permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
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
        Log.d(TAG, "Saved priority " + device + " = " + priority);
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
            return false;
        }
        this.mStateMachine.sendMessage(5);
        return true;
    }

    boolean stopVoiceRecognition(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        this.mStateMachine.sendMessage(6);
        return true;
    }

    boolean acceptIncomingConnect(BluetoothDevice device) {
        return false;
    }

    boolean rejectIncomingConnect(BluetoothDevice device) {
        return false;
    }

    int getAudioState(BluetoothDevice device) {
        return this.mStateMachine.getAudioState(device);
    }

    boolean connectAudio() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH_ADMIN permission");
        if (!this.mStateMachine.isConnected() || this.mStateMachine.isAudioOn()) {
            return false;
        }
        this.mStateMachine.sendMessage(3);
        return true;
    }

    boolean disconnectAudio() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_ADMIN_PERM, "Need BLUETOOTH_ADMIN permission");
        if (!this.mStateMachine.isAudioOn()) {
            return false;
        }
        this.mStateMachine.sendMessage(4);
        return true;
    }

    boolean holdCall(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(14);
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    boolean acceptCall(BluetoothDevice device, int flag) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(12);
        msg.arg1 = flag;
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    boolean rejectCall(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(13);
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    boolean terminateCall(BluetoothDevice device, int index) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(15);
        msg.arg1 = index;
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    boolean enterPrivateMode(BluetoothDevice device, int index) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(16);
        msg.arg1 = index;
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    boolean redial(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(9);
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    boolean dial(BluetoothDevice device, String number) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(10);
        msg.obj = number;
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    boolean dialMemory(BluetoothDevice device, int location) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(11);
        msg.arg1 = location;
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    public boolean sendDTMF(BluetoothDevice device, byte code) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(17);
        msg.arg1 = code;
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    public boolean getLastVoiceTagNumber(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(19);
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    public List<BluetoothHeadsetClientCall> getCurrentCalls(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2) {
            return null;
        }
        return this.mStateMachine.getCurrentCalls();
    }

    public boolean explicitCallTransfer(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message msg = this.mStateMachine.obtainMessage(18);
        this.mStateMachine.sendMessage(msg);
        return true;
    }

    public Bundle getCurrentAgEvents(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2) {
            return null;
        }
        return this.mStateMachine.getCurrentAgEvents();
    }

    public Bundle getCurrentAgFeatures(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2) {
            return null;
        }
        return this.mStateMachine.getCurrentAgFeatures();
    }

    public BluetoothHeadsetClientCall getCall(BluetoothDevice device, int state) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        int connectionState = this.mStateMachine.getConnectionState(device);
        if (connectionState != 2) {
            return null;
        }
        return this.mStateMachine.getCall(state);
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        if (this.mStateMachine != null) {
            this.mStateMachine.dump(sb);
        }
    }
}
