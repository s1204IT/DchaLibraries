package com.android.bluetooth.hfpclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.IBluetoothHeadsetClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.hfpclient.connserv.HfpClientConnectionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HeadsetClientService extends ProfileService {
    private static final boolean DBG = false;
    public static final String HFP_CLIENT_STOP_TAG = "hfp_client_stop_tag";
    private static final int MAX_STATE_MACHINES_POSSIBLE = 1;
    private static final String TAG = "HeadsetClientService";
    private static HeadsetClientService sHeadsetClientService;
    private HashMap<BluetoothDevice, HeadsetClientStateMachine> mStateMachineMap = new HashMap<>();
    private NativeInterface mNativeInterface = null;
    private HandlerThread mSmThread = null;
    private HeadsetClientStateMachineFactory mSmFactory = null;
    private AudioManager mAudioManager = null;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION") && intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) == 0) {
                int intExtra = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                int iAmToHfVol = HeadsetClientStateMachine.amToHfVol(intExtra);
                HeadsetClientService.this.mAudioManager.setParameters("hfp_volume=" + iAmToHfVol);
                synchronized (this) {
                    for (HeadsetClientStateMachine headsetClientStateMachine : HeadsetClientService.this.mStateMachineMap.values()) {
                        if (headsetClientStateMachine != null) {
                            headsetClientStateMachine.sendMessage(8, intExtra);
                        }
                    }
                }
            }
        }
    };

    @Override
    public ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothHeadsetClientBinder(this);
    }

    @Override
    protected synchronized boolean start() {
        this.mNativeInterface = new NativeInterface();
        this.mAudioManager = (AudioManager) getSystemService("audio");
        if (this.mAudioManager == null) {
            Log.e(TAG, "AudioManager service doesn't exist?");
        } else {
            this.mAudioManager.setParameters("hfp_enable=false");
        }
        this.mSmFactory = new HeadsetClientStateMachineFactory();
        this.mStateMachineMap.clear();
        registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
        startService(new Intent(this, (Class<?>) HfpClientConnectionService.class));
        this.mSmThread = new HandlerThread("HeadsetClient.SM");
        this.mSmThread.start();
        setHeadsetClientService(this);
        return true;
    }

    @Override
    protected synchronized boolean stop() {
        if (sHeadsetClientService == null) {
            Log.w(TAG, "stop() called without start()");
            return false;
        }
        setHeadsetClientService(null);
        unregisterReceiver(this.mBroadcastReceiver);
        Iterator<Map.Entry<BluetoothDevice, HeadsetClientStateMachine>> it = this.mStateMachineMap.entrySet().iterator();
        while (it.hasNext()) {
            this.mStateMachineMap.get(it.next().getKey()).doQuit();
            it.remove();
        }
        Intent intent = new Intent(this, (Class<?>) HfpClientConnectionService.class);
        intent.putExtra(HFP_CLIENT_STOP_TAG, true);
        startService(intent);
        this.mSmThread.quit();
        this.mSmThread = null;
        this.mNativeInterface.cleanup();
        this.mNativeInterface = null;
        return true;
    }

    private static class BluetoothHeadsetClientBinder extends IBluetoothHeadsetClient.Stub implements ProfileService.IProfileServiceBinder {
        private HeadsetClientService mService;

        BluetoothHeadsetClientBinder(HeadsetClientService headsetClientService) {
            this.mService = headsetClientService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        private HeadsetClientService getService() {
            if (!Utils.checkCaller()) {
                Log.w(HeadsetClientService.TAG, "HeadsetClient call not allowed for non-active user");
                return null;
            }
            if (this.mService != null && this.mService.isAvailable()) {
                return this.mService;
            }
            Log.e(HeadsetClientService.TAG, "HeadsetClientService is not available.");
            return null;
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            HeadsetClientService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            HeadsetClientService service = getService();
            if (service != null) {
                return service.getDevicesMatchingConnectionStates(iArr);
            }
            return new ArrayList(0);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service != null) {
                return service.getConnectionState(bluetoothDevice);
            }
            return 0;
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }

        public boolean startVoiceRecognition(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.startVoiceRecognition(bluetoothDevice);
        }

        public boolean stopVoiceRecognition(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.stopVoiceRecognition(bluetoothDevice);
        }

        public int getAudioState(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getAudioState(bluetoothDevice);
        }

        public void setAudioRouteAllowed(BluetoothDevice bluetoothDevice, boolean z) {
            Log.e(HeadsetClientService.TAG, "setAudioRouteAllowed API not supported");
        }

        public boolean getAudioRouteAllowed(BluetoothDevice bluetoothDevice) {
            Log.e(HeadsetClientService.TAG, "getAudioRouteAllowed API not supported");
            return false;
        }

        public boolean connectAudio(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.connectAudio(bluetoothDevice);
        }

        public boolean disconnectAudio(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnectAudio(bluetoothDevice);
        }

        public boolean acceptCall(BluetoothDevice bluetoothDevice, int i) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.acceptCall(bluetoothDevice, i);
        }

        public boolean rejectCall(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.rejectCall(bluetoothDevice);
        }

        public boolean holdCall(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.holdCall(bluetoothDevice);
        }

        public boolean terminateCall(BluetoothDevice bluetoothDevice, BluetoothHeadsetClientCall bluetoothHeadsetClientCall) {
            HeadsetClientService service = getService();
            if (service != null) {
                return service.terminateCall(bluetoothDevice, bluetoothHeadsetClientCall != null ? bluetoothHeadsetClientCall.getUUID() : null);
            }
            Log.w(HeadsetClientService.TAG, "service is null");
            return false;
        }

        public boolean explicitCallTransfer(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.explicitCallTransfer(bluetoothDevice);
        }

        public boolean enterPrivateMode(BluetoothDevice bluetoothDevice, int i) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.enterPrivateMode(bluetoothDevice, i);
        }

        public BluetoothHeadsetClientCall dial(BluetoothDevice bluetoothDevice, String str) {
            HeadsetClientService service = getService();
            if (service == null) {
                return null;
            }
            return service.dial(bluetoothDevice, str);
        }

        public List<BluetoothHeadsetClientCall> getCurrentCalls(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return new ArrayList();
            }
            return service.getCurrentCalls(bluetoothDevice);
        }

        public boolean sendDTMF(BluetoothDevice bluetoothDevice, byte b) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.sendDTMF(bluetoothDevice, b);
        }

        public boolean getLastVoiceTagNumber(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return false;
            }
            return service.getLastVoiceTagNumber(bluetoothDevice);
        }

        public Bundle getCurrentAgEvents(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return null;
            }
            return service.getCurrentAgEvents(bluetoothDevice);
        }

        public Bundle getCurrentAgFeatures(BluetoothDevice bluetoothDevice) {
            HeadsetClientService service = getService();
            if (service == null) {
                return null;
            }
            return service.getCurrentAgFeatures(bluetoothDevice);
        }
    }

    public static synchronized HeadsetClientService getHeadsetClientService() {
        if (sHeadsetClientService == null) {
            Log.w(TAG, "getHeadsetClientService(): service is null");
            return null;
        }
        if (!sHeadsetClientService.isAvailable()) {
            Log.w(TAG, "getHeadsetClientService(): service is not available ");
            return null;
        }
        return sHeadsetClientService;
    }

    private static synchronized void setHeadsetClientService(HeadsetClientService headsetClientService) {
        sHeadsetClientService = headsetClientService;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        if (getPriority(bluetoothDevice) == 0) {
            Log.w(TAG, "Connection not allowed: <" + bluetoothDevice.getAddress() + "> is PRIORITY_OFF");
            return false;
        }
        stateMachine.sendMessage(1, bluetoothDevice);
        return true;
    }

    boolean disconnect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        int connectionState = stateMachine.getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        stateMachine.sendMessage(2, bluetoothDevice);
        return true;
    }

    public synchronized List<BluetoothDevice> getConnectedDevices() {
        ArrayList arrayList;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        arrayList = new ArrayList();
        for (BluetoothDevice bluetoothDevice : this.mStateMachineMap.keySet()) {
            HeadsetClientStateMachine headsetClientStateMachine = this.mStateMachineMap.get(bluetoothDevice);
            if (headsetClientStateMachine != null && headsetClientStateMachine.getConnectionState(bluetoothDevice) == 2) {
                arrayList.add(bluetoothDevice);
            }
        }
        return arrayList;
    }

    private synchronized List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        ArrayList arrayList;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        arrayList = new ArrayList();
        for (BluetoothDevice bluetoothDevice : this.mStateMachineMap.keySet()) {
            for (int i : iArr) {
                HeadsetClientStateMachine headsetClientStateMachine = this.mStateMachineMap.get(bluetoothDevice);
                if (headsetClientStateMachine != null && headsetClientStateMachine.getConnectionState(bluetoothDevice) == i) {
                    arrayList.add(bluetoothDevice);
                }
            }
        }
        return arrayList;
    }

    private synchronized int getConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine headsetClientStateMachine = this.mStateMachineMap.get(bluetoothDevice);
        if (headsetClientStateMachine != null) {
            return headsetClientStateMachine.getConnectionState(bluetoothDevice);
        }
        return 0;
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothHeadsetPriorityKey(bluetoothDevice.getAddress()), i);
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothHeadsetPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    boolean startVoiceRecognition(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        if (stateMachine.getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        stateMachine.sendMessage(5);
        return true;
    }

    boolean stopVoiceRecognition(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        if (stateMachine.getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        stateMachine.sendMessage(6);
        return true;
    }

    int getAudioState(BluetoothDevice bluetoothDevice) {
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return -1;
        }
        return stateMachine.getAudioState(bluetoothDevice);
    }

    boolean connectAudio(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        if (!stateMachine.isConnected() || stateMachine.isAudioOn()) {
            return false;
        }
        stateMachine.sendMessage(3);
        return true;
    }

    boolean disconnectAudio(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        if (!stateMachine.isAudioOn()) {
            return false;
        }
        stateMachine.sendMessage(4);
        return true;
    }

    boolean holdCall(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        int connectionState = stateMachine.getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        stateMachine.sendMessage(stateMachine.obtainMessage(14));
        return true;
    }

    boolean acceptCall(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this) {
            for (Map.Entry<BluetoothDevice, HeadsetClientStateMachine> entry : this.mStateMachineMap.entrySet()) {
                if (entry.getValue() != null && !entry.getKey().equals(bluetoothDevice)) {
                    if (entry.getValue().getConnectionState(entry.getKey()) == 2) {
                        entry.getValue().obtainMessage(15).sendToTarget();
                    }
                }
            }
        }
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        if (stateMachine.getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        Message messageObtainMessage = stateMachine.obtainMessage(12);
        messageObtainMessage.arg1 = i;
        stateMachine.sendMessage(messageObtainMessage);
        return true;
    }

    boolean rejectCall(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        int connectionState = stateMachine.getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        stateMachine.sendMessage(stateMachine.obtainMessage(13));
        return true;
    }

    boolean terminateCall(BluetoothDevice bluetoothDevice, UUID uuid) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        int connectionState = stateMachine.getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message messageObtainMessage = stateMachine.obtainMessage(15);
        messageObtainMessage.obj = uuid;
        stateMachine.sendMessage(messageObtainMessage);
        return true;
    }

    boolean enterPrivateMode(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        int connectionState = stateMachine.getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message messageObtainMessage = stateMachine.obtainMessage(16);
        messageObtainMessage.arg1 = i;
        stateMachine.sendMessage(messageObtainMessage);
        return true;
    }

    BluetoothHeadsetClientCall dial(BluetoothDevice bluetoothDevice, String str) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return null;
        }
        int connectionState = stateMachine.getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            return null;
        }
        BluetoothHeadsetClientCall bluetoothHeadsetClientCall = new BluetoothHeadsetClientCall(bluetoothDevice, -1, 2, str, false, true, stateMachine.getInBandRing());
        Message messageObtainMessage = stateMachine.obtainMessage(10);
        messageObtainMessage.obj = bluetoothHeadsetClientCall;
        stateMachine.sendMessage(messageObtainMessage);
        return bluetoothHeadsetClientCall;
    }

    public boolean sendDTMF(BluetoothDevice bluetoothDevice, byte b) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        int connectionState = stateMachine.getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        Message messageObtainMessage = stateMachine.obtainMessage(17);
        messageObtainMessage.arg1 = b;
        stateMachine.sendMessage(messageObtainMessage);
        return true;
    }

    public boolean getLastVoiceTagNumber(BluetoothDevice bluetoothDevice) {
        return false;
    }

    public List<BluetoothHeadsetClientCall> getCurrentCalls(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return null;
        }
        if (stateMachine.getConnectionState(bluetoothDevice) != 2) {
            return null;
        }
        return stateMachine.getCurrentCalls();
    }

    public boolean explicitCallTransfer(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return false;
        }
        int connectionState = stateMachine.getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        stateMachine.sendMessage(stateMachine.obtainMessage(18));
        return true;
    }

    public Bundle getCurrentAgEvents(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return null;
        }
        if (stateMachine.getConnectionState(bluetoothDevice) != 2) {
            return null;
        }
        return stateMachine.getCurrentAgEvents();
    }

    public Bundle getCurrentAgFeatures(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HeadsetClientStateMachine stateMachine = getStateMachine(bluetoothDevice);
        if (stateMachine == null) {
            Log.e(TAG, "Cannot allocate SM for device " + bluetoothDevice);
            return null;
        }
        if (stateMachine.getConnectionState(bluetoothDevice) != 2) {
            return null;
        }
        return stateMachine.getCurrentAgFeatures();
    }

    public void messageFromNative(StackEvent stackEvent) {
        HeadsetClientStateMachine stateMachine = getStateMachine(stackEvent.device);
        if (stateMachine == null) {
            Log.w(TAG, "No SM found for event " + stackEvent);
        }
        stateMachine.sendMessage(100, stackEvent);
    }

    private synchronized HeadsetClientStateMachine getStateMachine(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "getStateMachine failed: Device cannot be null");
            return null;
        }
        HeadsetClientStateMachine headsetClientStateMachine = this.mStateMachineMap.get(bluetoothDevice);
        if (headsetClientStateMachine != null) {
            return headsetClientStateMachine;
        }
        if (this.mStateMachineMap.keySet().size() > 1) {
            Log.e(TAG, "Max state machines reached, possible DOS attack 1");
            return null;
        }
        Log.d(TAG, "Creating a new state machine");
        HeadsetClientStateMachine headsetClientStateMachineMake = this.mSmFactory.make(this, this.mSmThread);
        headsetClientStateMachineMake.setNativeInterface(this.mNativeInterface);
        this.mStateMachineMap.put(bluetoothDevice, headsetClientStateMachineMake);
        return headsetClientStateMachineMake;
    }

    synchronized boolean isScoRouted() {
        for (Map.Entry<BluetoothDevice, HeadsetClientStateMachine> entry : this.mStateMachineMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getAudioState(entry.getKey()) == 2) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void dump(StringBuilder sb) {
        super.dump(sb);
        for (HeadsetClientStateMachine headsetClientStateMachine : this.mStateMachineMap.values()) {
            if (headsetClientStateMachine != null) {
                println(sb, "State machine:");
                println(sb, "=============");
                headsetClientStateMachine.dump(sb);
            }
        }
    }

    protected synchronized Map<BluetoothDevice, HeadsetClientStateMachine> getStateMachineMap() {
        return this.mStateMachineMap;
    }

    protected void setSMFactory(HeadsetClientStateMachineFactory headsetClientStateMachineFactory) {
        this.mSmFactory = headsetClientStateMachineFactory;
    }

    AudioManager getAudioManager() {
        return this.mAudioManager;
    }
}
