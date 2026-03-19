package com.android.bluetooth.btservice;

import android.R;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.hid.HidHostService;
import com.android.bluetooth.pan.PanService;
import java.util.HashSet;
import java.util.List;

class PhonePolicy {
    private static final boolean DBG = true;
    private static final int MESSAGE_ADAPTER_STATE_TURNED_ON = 4;
    private static final int MESSAGE_CONNECT_OTHER_PROFILES = 3;
    private static final int MESSAGE_PROFILE_ACTIVE_DEVICE_CHANGED = 5;
    private static final int MESSAGE_PROFILE_CONNECTION_STATE_CHANGED = 1;
    private static final int MESSAGE_PROFILE_INIT_PRIORITIES = 2;
    private static final String TAG = "BluetoothPhonePolicy";

    @VisibleForTesting
    static int sConnectOtherProfilesTimeoutMillis = 6000;
    private final AdapterService mAdapterService;
    private final ServiceFactory mFactory;
    private final Handler mHandler;
    private final HashSet<BluetoothDevice> mHeadsetRetrySet = new HashSet<>();
    private final HashSet<BluetoothDevice> mA2dpRetrySet = new HashSet<>();
    private final HashSet<BluetoothDevice> mConnectOtherProfilesDeviceSet = new HashSet<>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                PhonePolicy.errorLog("Received intent with null action");
            }
            switch (action) {
                case "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED":
                    PhonePolicy.this.mHandler.obtainMessage(1, 1, -1, intent).sendToTarget();
                    break;
                case "android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED":
                    PhonePolicy.this.mHandler.obtainMessage(5, 1, -1, intent).sendToTarget();
                    break;
                case "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED":
                    PhonePolicy.this.mHandler.obtainMessage(1, 2, -1, intent).sendToTarget();
                    break;
                case "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED":
                    PhonePolicy.this.mHandler.obtainMessage(5, 2, -1, intent).sendToTarget();
                    break;
                case "android.bluetooth.adapter.action.STATE_CHANGED":
                    if (intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1) == 12) {
                        PhonePolicy.this.mHandler.obtainMessage(4).sendToTarget();
                        break;
                    }
                    break;
                case "android.bluetooth.device.action.UUID":
                    PhonePolicy.this.mHandler.obtainMessage(2, intent).sendToTarget();
                    break;
                default:
                    Log.e(PhonePolicy.TAG, "Received unexpected intent, action=" + action);
                    break;
            }
        }
    };

    @VisibleForTesting
    BroadcastReceiver getBroadcastReceiver() {
        return this.mReceiver;
    }

    class PhonePolicyHandler extends Handler {
        PhonePolicyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    Intent intent = (Intent) message.obj;
                    PhonePolicy.this.processProfileStateChanged((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE"), message.arg1, intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1), intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1));
                    break;
                case 2:
                    Intent intent2 = (Intent) message.obj;
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) intent2.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Parcelable[] parcelableArrayExtra = intent2.getParcelableArrayExtra("android.bluetooth.device.extra.UUID");
                    PhonePolicy.debugLog("Received ACTION_UUID for device " + bluetoothDevice);
                    if (parcelableArrayExtra != null) {
                        ParcelUuid[] parcelUuidArr = new ParcelUuid[parcelableArrayExtra.length];
                        for (int i = 0; i < parcelUuidArr.length; i++) {
                            parcelUuidArr[i] = (ParcelUuid) parcelableArrayExtra[i];
                            PhonePolicy.debugLog("index=" + i + "uuid=" + parcelUuidArr[i]);
                        }
                        PhonePolicy.this.processInitProfilePriorities(bluetoothDevice, parcelUuidArr);
                    }
                    break;
                case 3:
                    BluetoothDevice bluetoothDevice2 = (BluetoothDevice) message.obj;
                    PhonePolicy.this.processConnectOtherProfiles(bluetoothDevice2);
                    PhonePolicy.this.mConnectOtherProfilesDeviceSet.remove(bluetoothDevice2);
                    break;
                case 4:
                    PhonePolicy.this.resetStates();
                    PhonePolicy.this.autoConnect();
                    break;
                case 5:
                    PhonePolicy.this.processProfileActiveDeviceChanged((BluetoothDevice) ((Intent) message.obj).getParcelableExtra("android.bluetooth.device.extra.DEVICE"), message.arg1);
                    break;
            }
        }
    }

    protected void start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.device.action.UUID");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED");
        this.mAdapterService.registerReceiver(this.mReceiver, intentFilter);
    }

    protected void cleanup() {
        this.mAdapterService.unregisterReceiver(this.mReceiver);
        resetStates();
    }

    PhonePolicy(AdapterService adapterService, ServiceFactory serviceFactory) {
        this.mAdapterService = adapterService;
        this.mFactory = serviceFactory;
        this.mHandler = new PhonePolicyHandler(adapterService.getMainLooper());
    }

    private void processInitProfilePriorities(BluetoothDevice bluetoothDevice, ParcelUuid[] parcelUuidArr) {
        debugLog("processInitProfilePriorities() - device " + bluetoothDevice);
        HidHostService hidHostService = this.mFactory.getHidHostService();
        A2dpService a2dpService = this.mFactory.getA2dpService();
        HeadsetService headsetService = this.mFactory.getHeadsetService();
        PanService panService = this.mFactory.getPanService();
        HearingAidService hearingAidService = this.mFactory.getHearingAidService();
        if (hidHostService != null && ((BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Hid) || BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Hogp)) && hidHostService.getPriority(bluetoothDevice) == -1)) {
            hidHostService.setPriority(bluetoothDevice, 100);
        }
        if (headsetService != null && ((BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.HSP) || BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Handsfree)) && headsetService.getPriority(bluetoothDevice) == -1)) {
            headsetService.setPriority(bluetoothDevice, 100);
        }
        if (a2dpService != null && ((BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.AudioSink) || BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.AdvAudioDist)) && a2dpService.getPriority(bluetoothDevice) == -1)) {
            a2dpService.setPriority(bluetoothDevice, 100);
        }
        if (panService != null && BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.PANU) && panService.getPriority(bluetoothDevice) == -1 && this.mAdapterService.getResources().getBoolean(R.^attr-private.colorAccentPrimaryVariant)) {
            panService.setPriority(bluetoothDevice, 100);
        }
        if (hearingAidService != null && BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.HearingAid) && hearingAidService.getPriority(bluetoothDevice) == -1) {
            debugLog("setting hearing aid profile priority for device " + bluetoothDevice);
            hearingAidService.setPriority(bluetoothDevice, 100);
        }
    }

    protected void cleanProfilePriorities() {
        debugLog("clean profile priorities when factory reset");
        HidHostService hidHostService = this.mFactory.getHidHostService();
        A2dpService a2dpService = this.mFactory.getA2dpService();
        HeadsetService headsetService = this.mFactory.getHeadsetService();
        PanService panService = this.mFactory.getPanService();
        HearingAidService hearingAidService = this.mFactory.getHearingAidService();
        for (BluetoothDevice bluetoothDevice : this.mAdapterService.getBondedDevices()) {
            if (hidHostService != null) {
                hidHostService.setPriority(bluetoothDevice, -1);
            }
            if (headsetService != null) {
                headsetService.setPriority(bluetoothDevice, -1);
            }
            if (a2dpService != null) {
                a2dpService.setPriority(bluetoothDevice, -1);
            }
            if (panService != null) {
                panService.setPriority(bluetoothDevice, -1);
            }
            if (hearingAidService != null) {
                hearingAidService.setPriority(bluetoothDevice, -1);
            }
        }
    }

    private void processProfileStateChanged(BluetoothDevice bluetoothDevice, int i, int i2, int i3) {
        debugLog("processProfileStateChanged, device=" + bluetoothDevice + ", profile=" + i + ", " + i3 + " -> " + i2);
        if (i == 2 || i == 1) {
            if (i2 == 2) {
                switch (i) {
                    case 1:
                        this.mHeadsetRetrySet.remove(bluetoothDevice);
                        break;
                    case 2:
                        this.mA2dpRetrySet.remove(bluetoothDevice);
                        break;
                }
                connectOtherProfile(bluetoothDevice);
            }
            if (i3 == 1 && i2 == 0) {
                HeadsetService headsetService = this.mFactory.getHeadsetService();
                boolean z = false;
                boolean z2 = headsetService == null || headsetService.getConnectionState(bluetoothDevice) == 0;
                A2dpService a2dpService = this.mFactory.getA2dpService();
                if (a2dpService == null || a2dpService.getConnectionState(bluetoothDevice) == 0) {
                    z = true;
                }
                debugLog("processProfileStateChanged, device=" + bluetoothDevice + ", a2dpDisconnected=" + z + ", hsDisconnected=" + z2);
                if (z2 && z) {
                    removeAutoConnectFromA2dpSink(bluetoothDevice);
                    removeAutoConnectFromHeadset(bluetoothDevice);
                }
            }
        }
    }

    private void processProfileActiveDeviceChanged(BluetoothDevice bluetoothDevice, int i) {
        debugLog("processProfileActiveDeviceChanged, activeDevice=" + bluetoothDevice + ", profile=" + i);
        switch (i) {
            case 1:
            case 2:
                if (bluetoothDevice == null) {
                    warnLog("processProfileActiveDeviceChanged: ignore null A2DP active device");
                } else {
                    for (BluetoothDevice bluetoothDevice2 : this.mAdapterService.getBondedDevices()) {
                        removeAutoConnectFromA2dpSink(bluetoothDevice2);
                        removeAutoConnectFromHeadset(bluetoothDevice2);
                    }
                    setAutoConnectForA2dpSink(bluetoothDevice);
                    setAutoConnectForHeadset(bluetoothDevice);
                }
                break;
        }
    }

    private void resetStates() {
        this.mHeadsetRetrySet.clear();
        this.mA2dpRetrySet.clear();
    }

    private void autoConnect() {
        if (this.mAdapterService.getState() != 12) {
            errorLog("autoConnect: BT is not ON. Exiting autoConnect");
            return;
        }
        if (!this.mAdapterService.isQuietModeEnabled()) {
            debugLog("autoConnect: Initiate auto connection on BT on...");
            BluetoothDevice[] bondedDevices = this.mAdapterService.getBondedDevices();
            if (bondedDevices == null) {
                errorLog("autoConnect: bondedDevices are null");
                return;
            }
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                autoConnectHeadset(bluetoothDevice);
                autoConnectA2dp(bluetoothDevice);
            }
            return;
        }
        debugLog("autoConnect() - BT is in quiet mode. Not initiating auto connections");
    }

    private void autoConnectA2dp(BluetoothDevice bluetoothDevice) {
        A2dpService a2dpService = this.mFactory.getA2dpService();
        if (a2dpService == null) {
            warnLog("autoConnectA2dp: service is null, failed to connect to " + bluetoothDevice);
            return;
        }
        int priority = a2dpService.getPriority(bluetoothDevice);
        if (priority == 1000) {
            debugLog("autoConnectA2dp: connecting A2DP with " + bluetoothDevice);
            a2dpService.connect(bluetoothDevice);
            return;
        }
        debugLog("autoConnectA2dp: skipped auto-connect A2DP with device " + bluetoothDevice + " priority " + priority);
    }

    private void autoConnectHeadset(BluetoothDevice bluetoothDevice) {
        HeadsetService headsetService = this.mFactory.getHeadsetService();
        if (headsetService == null) {
            warnLog("autoConnectHeadset: service is null, failed to connect to " + bluetoothDevice);
            return;
        }
        int priority = headsetService.getPriority(bluetoothDevice);
        if (priority == 1000) {
            debugLog("autoConnectHeadset: Connecting HFP with " + bluetoothDevice);
            headsetService.connect(bluetoothDevice);
            return;
        }
        debugLog("autoConnectHeadset: skipped auto-connect HFP with device " + bluetoothDevice + " priority " + priority);
    }

    private void connectOtherProfile(BluetoothDevice bluetoothDevice) {
        if (this.mAdapterService.isQuietModeEnabled()) {
            debugLog("connectOtherProfile: in quiet mode, skip connect other profile " + bluetoothDevice);
            return;
        }
        if (this.mConnectOtherProfilesDeviceSet.contains(bluetoothDevice)) {
            debugLog("connectOtherProfile: already scheduled callback for " + bluetoothDevice);
            return;
        }
        this.mConnectOtherProfilesDeviceSet.add(bluetoothDevice);
        Message messageObtainMessage = this.mHandler.obtainMessage(3);
        messageObtainMessage.obj = bluetoothDevice;
        this.mHandler.sendMessageDelayed(messageObtainMessage, sConnectOtherProfilesTimeoutMillis);
    }

    private void processConnectOtherProfiles(BluetoothDevice bluetoothDevice) {
        debugLog("processConnectOtherProfiles, device=" + bluetoothDevice);
        if (this.mAdapterService.getState() != 12) {
            warnLog("processConnectOtherProfiles, adapter is not ON " + this.mAdapterService.getState());
            return;
        }
        HeadsetService headsetService = this.mFactory.getHeadsetService();
        A2dpService a2dpService = this.mFactory.getA2dpService();
        PanService panService = this.mFactory.getPanService();
        boolean zContains = false;
        boolean zIsEmpty = true;
        List<BluetoothDevice> connectedDevices = null;
        if (headsetService != null) {
            List<BluetoothDevice> connectedDevices2 = headsetService.getConnectedDevices();
            zIsEmpty = true & connectedDevices2.isEmpty();
            zContains = false | connectedDevices2.contains(bluetoothDevice);
        }
        if (a2dpService != null) {
            List<BluetoothDevice> connectedDevices3 = a2dpService.getConnectedDevices();
            zIsEmpty &= connectedDevices3.isEmpty();
            zContains |= connectedDevices3.contains(bluetoothDevice);
        }
        if (panService != null) {
            connectedDevices = panService.getConnectedDevices();
            zIsEmpty &= connectedDevices.isEmpty();
            zContains |= connectedDevices.contains(bluetoothDevice);
        }
        if (!zContains) {
            debugLog("processConnectOtherProfiles, all profiles disconnected for " + bluetoothDevice);
            this.mHeadsetRetrySet.remove(bluetoothDevice);
            this.mA2dpRetrySet.remove(bluetoothDevice);
            if (zIsEmpty) {
                debugLog("processConnectOtherProfiles, all profiles disconnected for all devices");
                resetStates();
                return;
            }
            return;
        }
        if (headsetService != null && !this.mHeadsetRetrySet.contains(bluetoothDevice) && headsetService.getPriority(bluetoothDevice) >= 100 && headsetService.getConnectionState(bluetoothDevice) == 0) {
            debugLog("Retrying connection to Headset with device " + bluetoothDevice);
            this.mHeadsetRetrySet.add(bluetoothDevice);
            headsetService.connect(bluetoothDevice);
        }
        if (a2dpService != null && !this.mA2dpRetrySet.contains(bluetoothDevice) && a2dpService.getPriority(bluetoothDevice) >= 100 && a2dpService.getConnectionState(bluetoothDevice) == 0) {
            debugLog("Retrying connection to A2DP with device " + bluetoothDevice);
            this.mA2dpRetrySet.add(bluetoothDevice);
            a2dpService.connect(bluetoothDevice);
        }
        if (panService != null && connectedDevices.isEmpty() && panService.getPriority(bluetoothDevice) >= 100 && panService.getConnectionState(bluetoothDevice) == 0) {
            debugLog("Retrying connection to PAN with device " + bluetoothDevice);
            panService.connect(bluetoothDevice);
        }
    }

    private void setAutoConnectForHeadset(BluetoothDevice bluetoothDevice) {
        HeadsetService headsetService = this.mFactory.getHeadsetService();
        if (headsetService == null) {
            warnLog("setAutoConnectForHeadset: HEADSET service is null");
            return;
        }
        if (headsetService.getPriority(bluetoothDevice) >= 100) {
            debugLog("setAutoConnectForHeadset: device " + bluetoothDevice + " PRIORITY_AUTO_CONNECT");
            headsetService.setPriority(bluetoothDevice, 1000);
        }
    }

    private void setAutoConnectForA2dpSink(BluetoothDevice bluetoothDevice) {
        A2dpService a2dpService = this.mFactory.getA2dpService();
        if (a2dpService == null) {
            warnLog("setAutoConnectForA2dpSink: A2DP service is null");
            return;
        }
        if (a2dpService.getPriority(bluetoothDevice) >= 100) {
            debugLog("setAutoConnectForA2dpSink: device " + bluetoothDevice + " PRIORITY_AUTO_CONNECT");
            a2dpService.setPriority(bluetoothDevice, 1000);
        }
    }

    private void removeAutoConnectFromHeadset(BluetoothDevice bluetoothDevice) {
        HeadsetService headsetService = this.mFactory.getHeadsetService();
        if (headsetService == null) {
            warnLog("removeAutoConnectFromHeadset: HEADSET service is null");
            return;
        }
        if (headsetService.getPriority(bluetoothDevice) >= 1000) {
            debugLog("removeAutoConnectFromHeadset: device " + bluetoothDevice + " PRIORITY_ON");
            headsetService.setPriority(bluetoothDevice, 100);
        }
    }

    private void removeAutoConnectFromA2dpSink(BluetoothDevice bluetoothDevice) {
        A2dpService a2dpService = this.mFactory.getA2dpService();
        if (a2dpService == null) {
            warnLog("removeAutoConnectFromA2dpSink: A2DP service is null");
            return;
        }
        if (a2dpService.getPriority(bluetoothDevice) >= 1000) {
            debugLog("removeAutoConnectFromA2dpSink: device " + bluetoothDevice + " PRIORITY_ON");
            a2dpService.setPriority(bluetoothDevice, 100);
        }
    }

    private static void debugLog(String str) {
        Log.i(TAG, str);
    }

    private static void warnLog(String str) {
        Log.w(TAG, str);
    }

    private static void errorLog(String str) {
        Log.e(TAG, str);
    }
}
