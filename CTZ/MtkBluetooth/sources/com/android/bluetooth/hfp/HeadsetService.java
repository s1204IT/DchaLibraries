package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.sap.SapService;
import com.android.internal.annotations.VisibleForTesting;
import com.android.vcard.VCardConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

public class HeadsetService extends ProfileService {
    private static final int DIALING_OUT_TIMEOUT_MS = 10000;
    private static final String DISABLE_INBAND_RINGING_PROPERTY = "persist.bluetooth.disableinbandringing";
    private static final String TAG = "HeadsetService";
    private static HeadsetService sHeadsetService;
    private BluetoothDevice mActiveDevice;
    private AdapterService mAdapterService;
    private boolean mCreated;
    private DialingOutTimeoutEvent mDialingOutTimeoutEvent;
    private boolean mForceScoAudio;
    private boolean mInbandRingingRuntimeDisable;
    private HeadsetNativeInterface mNativeInterface;
    private boolean mOnlyReceiveDisconnectedCallState;
    private boolean mStarted;
    private HandlerThread mStateMachinesThread;
    private HeadsetSystemInterface mSystemInterface;
    private boolean mVirtualCallStarted;
    private boolean mVoiceRecognitionStarted;
    private VoiceRecognitionTimeoutEvent mVoiceRecognitionTimeoutEvent;
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final ParcelUuid[] HEADSET_UUIDS = {BluetoothUuid.HSP, BluetoothUuid.Handsfree};
    private static final int[] CONNECTING_CONNECTED_STATES = {1, 2};

    @VisibleForTesting
    static int sStartVrTimeoutMs = SapService.MSG_SERVERSESSION_CLOSE;
    private int mMaxHeadsetConnections = 1;
    private final HashMap<BluetoothDevice, HeadsetStateMachine> mStateMachines = new HashMap<>();
    private boolean mAudioRouteAllowed = true;
    private final BroadcastReceiver mHeadsetReceiver = new AnonymousClass1();

    interface StateMachineTask {
        void execute(HeadsetStateMachine headsetStateMachine);
    }

    @Override
    public ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothHeadsetBinder(this);
    }

    @Override
    protected void create() {
        Log.i(TAG, "create()");
        if (this.mCreated) {
            throw new IllegalStateException("create() called twice");
        }
        this.mCreated = true;
    }

    @Override
    protected boolean start() {
        Log.i(TAG, "start()");
        if (this.mStarted) {
            Log.w(TAG, "HeadsetService start() called twice ");
            return true;
        }
        this.mAdapterService = (AdapterService) Objects.requireNonNull(AdapterService.getAdapterService(), "AdapterService cannot be null when HeadsetService starts");
        this.mStateMachinesThread = new HandlerThread("HeadsetService.StateMachines");
        this.mStateMachinesThread.start();
        this.mSystemInterface = HeadsetObjectsFactory.getInstance().makeSystemInterface(this);
        this.mSystemInterface.init();
        this.mMaxHeadsetConnections = this.mAdapterService.getMaxConnectedAudioDevices();
        this.mNativeInterface = HeadsetObjectsFactory.getInstance().getNativeInterface();
        this.mNativeInterface.init(this.mMaxHeadsetConnections + 1, isInbandRingingEnabled());
        if (this.mStateMachines.size() > 0) {
            throw new IllegalStateException("start(): mStateMachines is not empty, " + this.mStateMachines.size() + " is already created. Was stop() called properly?");
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        intentFilter.addAction("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY");
        intentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        registerReceiver(this.mHeadsetReceiver, intentFilter);
        setHeadsetService(this);
        setActiveDevice(null);
        this.mStarted = true;
        return true;
    }

    @Override
    protected boolean stop() {
        Log.i(TAG, "stop()");
        if (!this.mStarted) {
            Log.w(TAG, "stop() called before start()");
            return true;
        }
        this.mStarted = false;
        setHeadsetService(null);
        unregisterReceiver(this.mHeadsetReceiver);
        synchronized (this.mStateMachines) {
            this.mActiveDevice = null;
            broadcastActiveDevice(null);
            this.mInbandRingingRuntimeDisable = false;
            this.mForceScoAudio = false;
            this.mAudioRouteAllowed = true;
            this.mMaxHeadsetConnections = 1;
            this.mVoiceRecognitionStarted = false;
            this.mVirtualCallStarted = false;
            if (this.mDialingOutTimeoutEvent != null) {
                this.mStateMachinesThread.getThreadHandler().removeCallbacks(this.mDialingOutTimeoutEvent);
                this.mDialingOutTimeoutEvent = null;
            }
            if (this.mVoiceRecognitionTimeoutEvent != null) {
                this.mStateMachinesThread.getThreadHandler().removeCallbacks(this.mVoiceRecognitionTimeoutEvent);
                this.mVoiceRecognitionTimeoutEvent = null;
                if (this.mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {
                    this.mSystemInterface.getVoiceRecognitionWakeLock().release();
                }
            }
            Iterator<HeadsetStateMachine> it = this.mStateMachines.values().iterator();
            while (it.hasNext()) {
                HeadsetObjectsFactory.getInstance().destroyStateMachine(it.next());
            }
            this.mStateMachines.clear();
        }
        this.mNativeInterface.cleanup();
        this.mSystemInterface.stop();
        this.mStateMachinesThread.quitSafely();
        this.mStateMachinesThread = null;
        this.mAdapterService = null;
        return true;
    }

    @Override
    protected void cleanup() {
        Log.i(TAG, "cleanup");
        if (!this.mCreated) {
            Log.w(TAG, "cleanup() called before create()");
        }
        this.mCreated = false;
    }

    public boolean isAlive() {
        return isAvailable() && this.mCreated && this.mStarted;
    }

    @VisibleForTesting
    public Looper getStateMachinesThreadLooper() {
        return this.mStateMachinesThread.getLooper();
    }

    private boolean doForStateMachine(BluetoothDevice bluetoothDevice, StateMachineTask stateMachineTask) {
        synchronized (this.mStateMachines) {
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                return false;
            }
            stateMachineTask.execute(headsetStateMachine);
            return true;
        }
    }

    private void doForEachConnectedStateMachine(StateMachineTask stateMachineTask) {
        synchronized (this.mStateMachines) {
            Iterator<BluetoothDevice> it = getConnectedDevices().iterator();
            while (it.hasNext()) {
                stateMachineTask.execute(this.mStateMachines.get(it.next()));
            }
        }
    }

    private List<BluetoothDevice> getConnectingConnectedDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList();
        synchronized (this.mStateMachines) {
            for (HeadsetStateMachine headsetStateMachine : this.mStateMachines.values()) {
                if (headsetStateMachine.getConnectionState() == 2 || headsetStateMachine.getConnectionState() == 1) {
                    arrayList.add(headsetStateMachine.getDevice());
                }
            }
        }
        Log.d(TAG, "getConnectingConnectedDevices: " + arrayList);
        return arrayList;
    }

    private void doForEachConnectingConnectedStateMachine(StateMachineTask stateMachineTask) {
        synchronized (this.mStateMachines) {
            Iterator<BluetoothDevice> it = getConnectingConnectedDevices().iterator();
            while (it.hasNext()) {
                stateMachineTask.execute(this.mStateMachines.get(it.next()));
            }
        }
    }

    void onDeviceStateChanged(final HeadsetDeviceState headsetDeviceState) {
        doForEachConnectedStateMachine(new StateMachineTask() {
            @Override
            public final void execute(HeadsetStateMachine headsetStateMachine) {
                headsetStateMachine.sendMessage(10, headsetDeviceState);
            }
        });
    }

    void messageFromNative(HeadsetStackEvent headsetStackEvent) {
        Objects.requireNonNull(headsetStackEvent.device, "Device should never be null, event: " + headsetStackEvent);
        synchronized (this.mStateMachines) {
            HeadsetStateMachine headsetStateMachineMakeStateMachine = this.mStateMachines.get(headsetStackEvent.device);
            if (headsetStackEvent.type == 1) {
                switch (headsetStackEvent.valueInt) {
                    case 1:
                    case 2:
                        if (headsetStateMachineMakeStateMachine == null) {
                            headsetStateMachineMakeStateMachine = HeadsetObjectsFactory.getInstance().makeStateMachine(headsetStackEvent.device, this.mStateMachinesThread.getLooper(), this, this.mAdapterService, this.mNativeInterface, this.mSystemInterface);
                            this.mStateMachines.put(headsetStackEvent.device, headsetStateMachineMakeStateMachine);
                        }
                        break;
                }
            }
            if (headsetStateMachineMakeStateMachine == null) {
                throw new IllegalStateException("State machine not found for stack event: " + headsetStackEvent);
            }
            headsetStateMachineMakeStateMachine.sendMessage(101, headsetStackEvent);
        }
    }

    class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override
        public void onReceive(Context context, final Intent intent) {
            byte b;
            String action = intent.getAction();
            if (action == null) {
                Log.w(HeadsetService.TAG, "mHeadsetReceiver, action is null");
                return;
            }
            int iHashCode = action.hashCode();
            if (iHashCode != -1940635523) {
                if (iHashCode != -1538406691) {
                    if (iHashCode != -725473775) {
                        b = (iHashCode == 2116862345 && action.equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) ? (byte) 3 : (byte) -1;
                    } else if (action.equals("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY")) {
                        b = 2;
                    }
                } else if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                    b = 0;
                }
            } else if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    int intExtra = intent.getIntExtra("level", -1);
                    int intExtra2 = intent.getIntExtra("scale", -1);
                    if (intExtra >= 0 && intExtra2 > 0) {
                        HeadsetService.this.mSystemInterface.getHeadsetPhoneState().setCindBatteryCharge((intExtra * 5) / intExtra2);
                        return;
                    }
                    Log.e(HeadsetService.TAG, "Bad Battery Changed intent: batteryLevel=" + intExtra + ", scale=" + intExtra2);
                    return;
                case 1:
                    if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) == 6) {
                        HeadsetService.this.doForEachConnectedStateMachine(new StateMachineTask() {
                            @Override
                            public final void execute(HeadsetStateMachine headsetStateMachine) {
                                headsetStateMachine.sendMessage(7, intent);
                            }
                        });
                        return;
                    }
                    return;
                case 2:
                    int intExtra3 = intent.getIntExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    HeadsetService.logD("Received BluetoothDevice.ACTION_CONNECTION_ACCESS_REPLY, device=" + bluetoothDevice + ", type=" + intExtra3);
                    if (intExtra3 == 2) {
                        synchronized (HeadsetService.this.mStateMachines) {
                            HeadsetStateMachine headsetStateMachine = (HeadsetStateMachine) HeadsetService.this.mStateMachines.get(bluetoothDevice);
                            if (headsetStateMachine == null) {
                                Log.w(HeadsetService.TAG, "Cannot find state machine for " + bluetoothDevice);
                                return;
                            }
                            headsetStateMachine.sendMessage(8, intent);
                            return;
                        }
                    }
                    return;
                case 3:
                    int intExtra4 = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", Integer.MIN_VALUE);
                    BluetoothDevice bluetoothDevice2 = (BluetoothDevice) Objects.requireNonNull((BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE"), "ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
                    HeadsetService.logD("Bond state changed for device: " + bluetoothDevice2 + " state: " + intExtra4);
                    if (intExtra4 == 10) {
                        synchronized (HeadsetService.this.mStateMachines) {
                            HeadsetStateMachine headsetStateMachine2 = (HeadsetStateMachine) HeadsetService.this.mStateMachines.get(bluetoothDevice2);
                            if (headsetStateMachine2 != null) {
                                if (headsetStateMachine2.getConnectionState() == 0) {
                                    HeadsetService.this.removeStateMachine(bluetoothDevice2);
                                }
                            }
                        }
                        return;
                    }
                    return;
                default:
                    Log.w(HeadsetService.TAG, "Unknown action " + action);
                    return;
            }
        }
    }

    private static class BluetoothHeadsetBinder extends IBluetoothHeadset.Stub implements ProfileService.IProfileServiceBinder {
        private volatile HeadsetService mService;

        BluetoothHeadsetBinder(HeadsetService headsetService) {
            this.mService = headsetService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        private HeadsetService getService() {
            HeadsetService headsetService = this.mService;
            if (!Utils.checkCallerAllowManagedProfiles(headsetService)) {
                Log.w(HeadsetService.TAG, "Headset call not allowed for non-active user");
                return null;
            }
            if (headsetService == null) {
                Log.w(HeadsetService.TAG, "Service is null");
                return null;
            }
            if (!headsetService.isAlive()) {
                Log.w(HeadsetService.TAG, "Service is not alive");
                return null;
            }
            return headsetService;
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            HeadsetService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            HeadsetService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            HeadsetService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            HeadsetService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }

        public boolean startVoiceRecognition(BluetoothDevice bluetoothDevice) {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.startVoiceRecognition(bluetoothDevice);
        }

        public boolean stopVoiceRecognition(BluetoothDevice bluetoothDevice) {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.stopVoiceRecognition(bluetoothDevice);
        }

        public boolean isAudioOn() {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.isAudioOn();
        }

        public boolean isAudioConnected(BluetoothDevice bluetoothDevice) {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.isAudioConnected(bluetoothDevice);
        }

        public int getAudioState(BluetoothDevice bluetoothDevice) {
            HeadsetService service = getService();
            if (service == null) {
                return 10;
            }
            return service.getAudioState(bluetoothDevice);
        }

        public boolean connectAudio() {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.connectAudio();
        }

        public boolean disconnectAudio() {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnectAudio();
        }

        public void setAudioRouteAllowed(boolean z) {
            HeadsetService service = getService();
            if (service == null) {
                return;
            }
            service.setAudioRouteAllowed(z);
        }

        public boolean getAudioRouteAllowed() {
            HeadsetService service = getService();
            if (service != null) {
                return service.getAudioRouteAllowed();
            }
            return false;
        }

        public void setForceScoAudio(boolean z) {
            HeadsetService service = getService();
            if (service == null) {
                return;
            }
            service.setForceScoAudio(z);
        }

        public boolean startScoUsingVirtualVoiceCall() {
            HeadsetService service = getService();
            if (service != null) {
                return service.startScoUsingVirtualVoiceCall();
            }
            return false;
        }

        public boolean stopScoUsingVirtualVoiceCall() {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.stopScoUsingVirtualVoiceCall();
        }

        public void phoneStateChanged(int i, int i2, int i3, String str, int i4) {
            HeadsetService service = getService();
            if (service != null) {
                service.phoneStateChanged(i, i2, i3, str, i4, false);
            }
        }

        public void clccResponse(int i, int i2, int i3, int i4, boolean z, String str, int i5) {
            HeadsetService service = getService();
            if (service != null) {
                service.clccResponse(i, i2, i3, i4, z, str, i5);
            }
        }

        public boolean sendVendorSpecificResultCode(BluetoothDevice bluetoothDevice, String str, String str2) {
            HeadsetService service = getService();
            if (service != null) {
                return service.sendVendorSpecificResultCode(bluetoothDevice, str, str2);
            }
            return false;
        }

        public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.setActiveDevice(bluetoothDevice);
        }

        public BluetoothDevice getActiveDevice() {
            HeadsetService service = getService();
            if (service == null) {
                return null;
            }
            return service.getActiveDevice();
        }

        public boolean isInbandRingingEnabled() {
            HeadsetService service = getService();
            if (service == null) {
                return false;
            }
            return service.isInbandRingingEnabled();
        }
    }

    public static synchronized HeadsetService getHeadsetService() {
        if (sHeadsetService == null) {
            Log.w(TAG, "getHeadsetService(): service is NULL");
            return null;
        }
        if (!sHeadsetService.isAvailable()) {
            Log.w(TAG, "getHeadsetService(): service is not available");
            return null;
        }
        logD("getHeadsetService(): returning " + sHeadsetService);
        return sHeadsetService;
    }

    private static synchronized void setHeadsetService(HeadsetService headsetService) {
        logD("setHeadsetService(): set to: " + headsetService);
        sHeadsetService = headsetService;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        boolean z = false;
        if (getPriority(bluetoothDevice) == 0) {
            Log.w(TAG, "connect: PRIORITY_OFF, device=" + bluetoothDevice + ", " + Utils.getUidPidString());
            return false;
        }
        if (!BluetoothUuid.containsAnyUuid(this.mAdapterService.getRemoteUuids(bluetoothDevice), HEADSET_UUIDS)) {
            Log.e(TAG, "connect: Cannot connect to " + bluetoothDevice + ": no headset UUID, " + Utils.getUidPidString());
            return false;
        }
        synchronized (this.mStateMachines) {
            Log.i(TAG, "connect: device=" + bluetoothDevice + ", " + Utils.getUidPidString());
            HeadsetStateMachine headsetStateMachineMakeStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachineMakeStateMachine == null) {
                headsetStateMachineMakeStateMachine = HeadsetObjectsFactory.getInstance().makeStateMachine(bluetoothDevice, this.mStateMachinesThread.getLooper(), this, this.mAdapterService, this.mNativeInterface, this.mSystemInterface);
                this.mStateMachines.put(bluetoothDevice, headsetStateMachineMakeStateMachine);
            }
            int connectionState = headsetStateMachineMakeStateMachine.getConnectionState();
            if (connectionState != 2 && connectionState != 1) {
                List<BluetoothDevice> devicesMatchingConnectionStates = getDevicesMatchingConnectionStates(CONNECTING_CONNECTED_STATES);
                if (devicesMatchingConnectionStates.size() >= this.mMaxHeadsetConnections) {
                    if (this.mMaxHeadsetConnections != 1) {
                        Log.w(TAG, "Max connection has reached, rejecting connection to " + bluetoothDevice);
                        return false;
                    }
                    z = true;
                }
                if (z) {
                    Iterator<BluetoothDevice> it = devicesMatchingConnectionStates.iterator();
                    while (it.hasNext()) {
                        disconnect(it.next());
                    }
                    setActiveDevice(null);
                }
                headsetStateMachineMakeStateMachine.sendMessage(1, bluetoothDevice);
                return true;
            }
            Log.w(TAG, "connect: device " + bluetoothDevice + " is already connected/connecting, connectionState=" + connectionState);
            return false;
        }
    }

    boolean disconnect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Log.i(TAG, "disconnect: device=" + bluetoothDevice + ", " + Utils.getUidPidString());
        synchronized (this.mStateMachines) {
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                Log.w(TAG, "disconnect: device " + bluetoothDevice + " not ever connected/connecting");
                return false;
            }
            int connectionState = headsetStateMachine.getConnectionState();
            if (connectionState != 2 && connectionState != 1) {
                Log.w(TAG, "disconnect: device " + bluetoothDevice + " not connected/connecting, connectionState=" + connectionState);
                return false;
            }
            headsetStateMachine.sendMessage(2, bluetoothDevice);
            return true;
        }
    }

    public List<BluetoothDevice> getConnectedDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList();
        synchronized (this.mStateMachines) {
            for (HeadsetStateMachine headsetStateMachine : this.mStateMachines.values()) {
                if (headsetStateMachine.getConnectionState() == 2) {
                    arrayList.add(headsetStateMachine.getDevice());
                }
            }
        }
        return arrayList;
    }

    @VisibleForTesting
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList();
        if (iArr == null) {
            return arrayList;
        }
        if (getHeadsetService() == null) {
            Log.w(TAG, "getDevicesMatchingConnectionStates: return devices as HeadsetService null");
            return arrayList;
        }
        BluetoothDevice[] bondedDevices = this.mAdapterService.getBondedDevices();
        if (bondedDevices == null) {
            return arrayList;
        }
        synchronized (this.mStateMachines) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                if (BluetoothUuid.containsAnyUuid(this.mAdapterService.getRemoteUuids(bluetoothDevice), HEADSET_UUIDS)) {
                    int connectionState = getConnectionState(bluetoothDevice);
                    int length = iArr.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        if (connectionState != iArr[i]) {
                            i++;
                        } else {
                            arrayList.add(bluetoothDevice);
                            break;
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                return 0;
            }
            return headsetStateMachine.getConnectionState();
        }
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothHeadsetPriorityKey(bluetoothDevice.getAddress()), i);
        Log.i(TAG, "setPriority: device=" + bluetoothDevice + ", priority=" + i + ", " + Utils.getUidPidString());
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothHeadsetPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    boolean startVoiceRecognition(BluetoothDevice bluetoothDevice) {
        boolean z;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Log.i(TAG, "startVoiceRecognition: device=" + bluetoothDevice + ", " + Utils.getUidPidString());
        synchronized (this.mStateMachines) {
            if (this.mVoiceRecognitionStarted) {
                Log.w(TAG, "startVoiceRecognition: voice recognition is still active, just called stopVoiceRecognition, returned " + stopVoiceRecognition(this.mActiveDevice) + " on " + this.mActiveDevice + ", please try again");
                this.mVoiceRecognitionStarted = false;
                return false;
            }
            if (!isAudioModeIdle()) {
                Log.w(TAG, "startVoiceRecognition: audio mode not idle, active device is " + this.mActiveDevice);
                return false;
            }
            if (isAudioOn()) {
                Log.w(TAG, "startVoiceRecognition: audio is still active, please wait for audio to be disconnected, disconnectAudio() returned " + disconnectAudio() + ", active device is " + this.mActiveDevice);
                return false;
            }
            if (bluetoothDevice == null) {
                Log.i(TAG, "device is null, use active device " + this.mActiveDevice + " instead");
                bluetoothDevice = this.mActiveDevice;
            }
            if (this.mVoiceRecognitionTimeoutEvent != null) {
                if (!this.mVoiceRecognitionTimeoutEvent.mVoiceRecognitionDevice.equals(bluetoothDevice)) {
                    Log.w(TAG, "startVoiceRecognition: device " + bluetoothDevice + " is not the same as requesting device " + this.mVoiceRecognitionTimeoutEvent.mVoiceRecognitionDevice + ", fall back to requesting device");
                    bluetoothDevice = this.mVoiceRecognitionTimeoutEvent.mVoiceRecognitionDevice;
                }
                this.mStateMachinesThread.getThreadHandler().removeCallbacks(this.mVoiceRecognitionTimeoutEvent);
                this.mVoiceRecognitionTimeoutEvent = null;
                if (this.mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {
                    this.mSystemInterface.getVoiceRecognitionWakeLock().release();
                }
                z = true;
            } else {
                z = false;
            }
            if (!Objects.equals(bluetoothDevice, this.mActiveDevice) && !setActiveDevice(bluetoothDevice)) {
                Log.w(TAG, "startVoiceRecognition: failed to set " + bluetoothDevice + " as active");
                return false;
            }
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                Log.w(TAG, "startVoiceRecognition: " + bluetoothDevice + " is never connected");
                return false;
            }
            int connectionState = headsetStateMachine.getConnectionState();
            if (connectionState != 2 && connectionState != 1) {
                Log.w(TAG, "startVoiceRecognition: " + bluetoothDevice + " is not connected or connecting");
                return false;
            }
            this.mVoiceRecognitionStarted = true;
            if (z) {
                headsetStateMachine.sendMessage(15, 1, 0, bluetoothDevice);
            } else {
                headsetStateMachine.sendMessage(5, bluetoothDevice);
            }
            headsetStateMachine.sendMessage(3, bluetoothDevice);
            return true;
        }
    }

    boolean stopVoiceRecognition(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Log.i(TAG, "stopVoiceRecognition: device=" + bluetoothDevice + ", " + Utils.getUidPidString());
        synchronized (this.mStateMachines) {
            if (!Objects.equals(this.mActiveDevice, bluetoothDevice)) {
                Log.w(TAG, "startVoiceRecognition: requested device " + bluetoothDevice + " is not active, use active device " + this.mActiveDevice + " instead");
                bluetoothDevice = this.mActiveDevice;
            }
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                Log.w(TAG, "stopVoiceRecognition: " + bluetoothDevice + " is never connected");
                return false;
            }
            int connectionState = headsetStateMachine.getConnectionState();
            if (connectionState != 2 && connectionState != 1) {
                Log.w(TAG, "stopVoiceRecognition: " + bluetoothDevice + " is not connected or connecting");
                return false;
            }
            if (!this.mVoiceRecognitionStarted) {
                Log.w(TAG, "stopVoiceRecognition: voice recognition was not started");
                return false;
            }
            this.mVoiceRecognitionStarted = false;
            headsetStateMachine.sendMessage(6, bluetoothDevice);
            headsetStateMachine.sendMessage(4, bluetoothDevice);
            return true;
        }
    }

    boolean isAudioOn() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return getNonIdleAudioDevices().size() > 0;
    }

    boolean isAudioConnected(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                return false;
            }
            return headsetStateMachine.getAudioState() == 12;
        }
    }

    int getAudioState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                return 10;
            }
            return headsetStateMachine.getAudioState();
        }
    }

    public void setAudioRouteAllowed(boolean z) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Log.i(TAG, "setAudioRouteAllowed: allowed=" + z + ", " + Utils.getUidPidString());
        this.mAudioRouteAllowed = z;
        this.mNativeInterface.setScoAllowed(z);
    }

    public boolean getAudioRouteAllowed() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mAudioRouteAllowed;
    }

    public void setForceScoAudio(boolean z) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Log.i(TAG, "setForceScoAudio: forced=" + z + ", " + Utils.getUidPidString());
        this.mForceScoAudio = z;
    }

    @VisibleForTesting
    public boolean getForceScoAudio() {
        return this.mForceScoAudio;
    }

    @VisibleForTesting
    public BluetoothDevice getFirstConnectedAudioDevice() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mStateMachines) {
            Iterator<BluetoothDevice> it = getDevicesMatchingConnectionStates(CONNECTING_CONNECTED_STATES).iterator();
            while (it.hasNext()) {
                HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(it.next());
                if (headsetStateMachine != null) {
                    arrayList.add(headsetStateMachine);
                }
            }
        }
        arrayList.sort(Comparator.comparingLong(new ToLongFunction() {
            @Override
            public final long applyAsLong(Object obj) {
                return ((HeadsetStateMachine) obj).getConnectingTimestampMs();
            }
        }));
        if (arrayList.size() > 0) {
            return ((HeadsetStateMachine) arrayList.get(0)).getDevice();
        }
        return null;
    }

    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Log.i(TAG, "setActiveDevice: device=" + bluetoothDevice + ", " + Utils.getUidPidString());
        synchronized (this.mStateMachines) {
            try {
                if (bluetoothDevice == null) {
                    if (this.mVoiceRecognitionStarted && !stopVoiceRecognition(this.mActiveDevice)) {
                        Log.w(TAG, "setActiveDevice: fail to stopVoiceRecognition from " + this.mActiveDevice);
                    }
                    if (this.mVirtualCallStarted && !stopScoUsingVirtualVoiceCall()) {
                        Log.w(TAG, "setActiveDevice: fail to stopScoUsingVirtualVoiceCall from " + this.mActiveDevice);
                    }
                    if (getAudioState(this.mActiveDevice) != 10 && !disconnectAudio(this.mActiveDevice)) {
                        Log.w(TAG, "setActiveDevice: disconnectAudio failed on " + this.mActiveDevice);
                    }
                    this.mActiveDevice = null;
                    broadcastActiveDevice(null);
                    return true;
                }
                if (bluetoothDevice.equals(this.mActiveDevice)) {
                    Log.i(TAG, "setActiveDevice: device " + bluetoothDevice + " is already active");
                    return true;
                }
                if (getConnectionState(bluetoothDevice) != 2) {
                    Log.e(TAG, "setActiveDevice: Cannot set " + bluetoothDevice + " as active, device is not connected");
                    return false;
                }
                if (!this.mNativeInterface.setActiveDevice(bluetoothDevice)) {
                    Log.e(TAG, "setActiveDevice: Cannot set " + bluetoothDevice + " as active in native layer");
                    return false;
                }
                BluetoothDevice bluetoothDevice2 = this.mActiveDevice;
                this.mActiveDevice = bluetoothDevice;
                if (getAudioState(bluetoothDevice2) != 10) {
                    if (!disconnectAudio(bluetoothDevice2)) {
                        Log.e(TAG, "setActiveDevice: fail to disconnectAudio from " + bluetoothDevice2);
                        this.mActiveDevice = bluetoothDevice2;
                        this.mNativeInterface.setActiveDevice(bluetoothDevice2);
                        return false;
                    }
                    broadcastActiveDevice(this.mActiveDevice);
                } else if (shouldPersistAudio()) {
                    logD("mOnlyReceiveDisconnectedCallState: " + this.mOnlyReceiveDisconnectedCallState);
                    if (!this.mOnlyReceiveDisconnectedCallState && !connectAudio(this.mActiveDevice)) {
                        Log.e(TAG, "setActiveDevice: fail to connectAudio to " + this.mActiveDevice);
                        this.mActiveDevice = bluetoothDevice2;
                        this.mNativeInterface.setActiveDevice(bluetoothDevice2);
                        return false;
                    }
                    broadcastActiveDevice(this.mActiveDevice);
                } else {
                    broadcastActiveDevice(this.mActiveDevice);
                }
                return true;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public BluetoothDevice getActiveDevice() {
        BluetoothDevice bluetoothDevice;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            bluetoothDevice = this.mActiveDevice;
        }
        return bluetoothDevice;
    }

    boolean connectAudio() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        synchronized (this.mStateMachines) {
            BluetoothDevice bluetoothDevice = this.mActiveDevice;
            if (bluetoothDevice == null) {
                Log.w(TAG, "connectAudio: no active device, " + Utils.getUidPidString());
                return false;
            }
            return connectAudio(bluetoothDevice);
        }
    }

    boolean connectAudio(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Log.i(TAG, "connectAudio: device=" + bluetoothDevice + ", " + Utils.getUidPidString());
        synchronized (this.mStateMachines) {
            if (!isScoAcceptable(bluetoothDevice)) {
                Log.w(TAG, "connectAudio, rejected SCO request to " + bluetoothDevice);
                return false;
            }
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                Log.w(TAG, "connectAudio: device " + bluetoothDevice + " was never connected/connecting");
                return false;
            }
            if (headsetStateMachine.getConnectionState() != 2) {
                Log.w(TAG, "connectAudio: profile not connected");
                return false;
            }
            if (headsetStateMachine.getAudioState() != 10) {
                logD("connectAudio: audio is not idle for device " + bluetoothDevice);
                return true;
            }
            if (isAudioOn()) {
                Log.w(TAG, "connectAudio: audio is not idle, current audio devices are " + Arrays.toString(getNonIdleAudioDevices().toArray()));
                return false;
            }
            headsetStateMachine.sendMessage(3, bluetoothDevice);
            return true;
        }
    }

    private List<BluetoothDevice> getNonIdleAudioDevices() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mStateMachines) {
            for (HeadsetStateMachine headsetStateMachine : this.mStateMachines.values()) {
                if (headsetStateMachine.getAudioState() != 10) {
                    arrayList.add(headsetStateMachine.getDevice());
                }
            }
        }
        return arrayList;
    }

    boolean disconnectAudio() {
        boolean z;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        synchronized (this.mStateMachines) {
            z = false;
            for (BluetoothDevice bluetoothDevice : getNonIdleAudioDevices()) {
                if (disconnectAudio(bluetoothDevice)) {
                    z = true;
                } else {
                    Log.e(TAG, "disconnectAudio() from " + bluetoothDevice + " failed");
                }
            }
        }
        if (!z) {
            logD("disconnectAudio() no active audio connection");
        }
        return z;
    }

    boolean disconnectAudio(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        synchronized (this.mStateMachines) {
            Log.i(TAG, "disconnectAudio: device=" + bluetoothDevice + ", " + Utils.getUidPidString());
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                Log.w(TAG, "disconnectAudio: device " + bluetoothDevice + " was never connected/connecting");
                return false;
            }
            if (headsetStateMachine.getAudioState() == 10) {
                Log.w(TAG, "disconnectAudio, audio is already disconnected for " + bluetoothDevice);
                return false;
            }
            headsetStateMachine.sendMessage(4, bluetoothDevice);
            return true;
        }
    }

    boolean isVirtualCallStarted() {
        boolean z;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            z = this.mVirtualCallStarted;
        }
        return z;
    }

    private boolean startScoUsingVirtualVoiceCall() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Log.i(TAG, "startScoUsingVirtualVoiceCall: " + Utils.getUidPidString());
        synchronized (this.mStateMachines) {
            if (this.mVoiceRecognitionStarted) {
                Log.w(TAG, "startScoUsingVirtualVoiceCall: voice recognition is still active, just called stopVoiceRecognition, returned " + stopVoiceRecognition(this.mActiveDevice) + " on " + this.mActiveDevice + ", please try again");
                this.mVoiceRecognitionStarted = false;
                return false;
            }
            if (!isAudioModeIdle()) {
                Log.w(TAG, "startScoUsingVirtualVoiceCall: audio mode not idle, active device is " + this.mActiveDevice);
                return false;
            }
            if (isAudioOn()) {
                Log.w(TAG, "startScoUsingVirtualVoiceCall: audio is still active, please wait for audio to be disconnected, disconnectAudio() returned " + disconnectAudio() + ", active device is " + this.mActiveDevice);
                return false;
            }
            if (this.mActiveDevice == null) {
                Log.w(TAG, "startScoUsingVirtualVoiceCall: no active device");
                return false;
            }
            this.mVirtualCallStarted = true;
            phoneStateChanged(0, 0, 2, "", 0, true);
            phoneStateChanged(0, 0, 3, "", 0, true);
            phoneStateChanged(1, 0, 6, "", 0, true);
            return true;
        }
    }

    boolean stopScoUsingVirtualVoiceCall() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Log.i(TAG, "stopScoUsingVirtualVoiceCall: " + Utils.getUidPidString());
        synchronized (this.mStateMachines) {
            if (!this.mVirtualCallStarted) {
                Log.w(TAG, "stopScoUsingVirtualVoiceCall: virtual call not started");
                return false;
            }
            this.mVirtualCallStarted = false;
            phoneStateChanged(0, 0, 6, "", 0, true);
            return true;
        }
    }

    class DialingOutTimeoutEvent implements Runnable {
        BluetoothDevice mDialingOutDevice;

        DialingOutTimeoutEvent(BluetoothDevice bluetoothDevice) {
            this.mDialingOutDevice = bluetoothDevice;
        }

        @Override
        public void run() {
            synchronized (HeadsetService.this.mStateMachines) {
                HeadsetService.this.mDialingOutTimeoutEvent = null;
                HeadsetService.this.doForStateMachine(this.mDialingOutDevice, new StateMachineTask() {
                    @Override
                    public final void execute(HeadsetStateMachine headsetStateMachine) {
                        headsetStateMachine.sendMessage(14, 0, 0, this.f$0.mDialingOutDevice);
                    }
                });
            }
        }

        public String toString() {
            return "DialingOutTimeoutEvent[" + this.mDialingOutDevice + "]";
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean dialOutgoingCall(BluetoothDevice bluetoothDevice, String str) {
        synchronized (this.mStateMachines) {
            Log.i(TAG, "dialOutgoingCall: from " + bluetoothDevice);
            if (!isOnStateMachineThread()) {
                Log.e(TAG, "dialOutgoingCall must be called from state machine thread");
                return false;
            }
            if (this.mDialingOutTimeoutEvent != null) {
                Log.e(TAG, "dialOutgoingCall, already dialing by " + this.mDialingOutTimeoutEvent);
                return false;
            }
            if (isVirtualCallStarted() && !stopScoUsingVirtualVoiceCall()) {
                Log.e(TAG, "dialOutgoingCall failed to stop current virtual call");
                return false;
            }
            if (!setActiveDevice(bluetoothDevice)) {
                Log.e(TAG, "dialOutgoingCall failed to set active device to " + bluetoothDevice);
                return false;
            }
            Intent intent = new Intent("android.intent.action.CALL_PRIVILEGED", Uri.fromParts("tel", str, null));
            intent.setFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
            startActivity(intent);
            this.mDialingOutTimeoutEvent = new DialingOutTimeoutEvent(bluetoothDevice);
            this.mStateMachinesThread.getThreadHandler().postDelayed(this.mDialingOutTimeoutEvent, 10000L);
            return true;
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean hasDeviceInitiatedDialingOut() {
        boolean z;
        synchronized (this.mStateMachines) {
            z = this.mDialingOutTimeoutEvent != null;
        }
        return z;
    }

    class VoiceRecognitionTimeoutEvent implements Runnable {
        BluetoothDevice mVoiceRecognitionDevice;

        VoiceRecognitionTimeoutEvent(BluetoothDevice bluetoothDevice) {
            this.mVoiceRecognitionDevice = bluetoothDevice;
        }

        @Override
        public void run() {
            synchronized (HeadsetService.this.mStateMachines) {
                if (HeadsetService.this.mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {
                    HeadsetService.this.mSystemInterface.getVoiceRecognitionWakeLock().release();
                }
                HeadsetService.this.mVoiceRecognitionTimeoutEvent = null;
                HeadsetService.this.doForStateMachine(this.mVoiceRecognitionDevice, new StateMachineTask() {
                    @Override
                    public final void execute(HeadsetStateMachine headsetStateMachine) {
                        headsetStateMachine.sendMessage(15, 0, 0, this.f$0.mVoiceRecognitionDevice);
                    }
                });
            }
        }

        public String toString() {
            return "VoiceRecognitionTimeoutEvent[" + this.mVoiceRecognitionDevice + "]";
        }
    }

    boolean startVoiceRecognitionByHeadset(BluetoothDevice bluetoothDevice) {
        synchronized (this.mStateMachines) {
            Log.i(TAG, "startVoiceRecognitionByHeadset: from " + bluetoothDevice);
            if (this.mVoiceRecognitionStarted) {
                Log.w(TAG, "startVoiceRecognitionByHeadset: voice recognition is still active, just called stopVoiceRecognition, returned " + stopVoiceRecognition(this.mActiveDevice) + " on " + this.mActiveDevice + ", please try again");
                this.mVoiceRecognitionStarted = false;
                return false;
            }
            if (bluetoothDevice == null) {
                Log.e(TAG, "startVoiceRecognitionByHeadset: fromDevice is null");
                return false;
            }
            if (!isAudioModeIdle()) {
                Log.w(TAG, "startVoiceRecognitionByHeadset: audio mode not idle, active device is " + this.mActiveDevice);
                return false;
            }
            if (isAudioOn()) {
                Log.w(TAG, "startVoiceRecognitionByHeadset: audio is still active, please wait for audio to be disconnected, disconnectAudio() returned " + disconnectAudio() + ", active device is " + this.mActiveDevice);
                return false;
            }
            if (this.mVoiceRecognitionTimeoutEvent != null) {
                Log.w(TAG, "startVoiceRecognitionByHeadset: failed request from " + bluetoothDevice + ", already pending by " + this.mVoiceRecognitionTimeoutEvent);
                return false;
            }
            if (!setActiveDevice(bluetoothDevice)) {
                Log.w(TAG, "startVoiceRecognitionByHeadset: failed to set " + bluetoothDevice + " as active");
                return false;
            }
            IDeviceIdleController iDeviceIdleControllerAsInterface = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
            if (iDeviceIdleControllerAsInterface == null) {
                Log.w(TAG, "startVoiceRecognitionByHeadset: deviceIdleController is null, device=" + bluetoothDevice);
                return false;
            }
            try {
                iDeviceIdleControllerAsInterface.exitIdle("voice-command");
                if (!this.mSystemInterface.activateVoiceRecognition()) {
                    Log.w(TAG, "startVoiceRecognitionByHeadset: failed request from " + bluetoothDevice);
                    return false;
                }
                this.mVoiceRecognitionTimeoutEvent = new VoiceRecognitionTimeoutEvent(bluetoothDevice);
                this.mStateMachinesThread.getThreadHandler().postDelayed(this.mVoiceRecognitionTimeoutEvent, sStartVrTimeoutMs);
                if (!this.mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {
                    this.mSystemInterface.getVoiceRecognitionWakeLock().acquire(sStartVrTimeoutMs);
                }
                return true;
            } catch (RemoteException e) {
                Log.w(TAG, "startVoiceRecognitionByHeadset: failed to exit idle, device=" + bluetoothDevice + ", error=" + e.getMessage());
                return false;
            }
        }
    }

    boolean stopVoiceRecognitionByHeadset(BluetoothDevice bluetoothDevice) {
        synchronized (this.mStateMachines) {
            Log.i(TAG, "stopVoiceRecognitionByHeadset: from " + bluetoothDevice);
            if (!Objects.equals(bluetoothDevice, this.mActiveDevice)) {
                Log.w(TAG, "stopVoiceRecognitionByHeadset: " + bluetoothDevice + " is not active, active device is " + this.mActiveDevice);
                return false;
            }
            if (!this.mVoiceRecognitionStarted && this.mVoiceRecognitionTimeoutEvent == null) {
                Log.w(TAG, "stopVoiceRecognitionByHeadset: voice recognition not started, device=" + bluetoothDevice);
                return false;
            }
            if (this.mVoiceRecognitionTimeoutEvent != null) {
                if (this.mSystemInterface.getVoiceRecognitionWakeLock().isHeld()) {
                    this.mSystemInterface.getVoiceRecognitionWakeLock().release();
                }
                this.mStateMachinesThread.getThreadHandler().removeCallbacks(this.mVoiceRecognitionTimeoutEvent);
                this.mVoiceRecognitionTimeoutEvent = null;
            }
            if (this.mVoiceRecognitionStarted) {
                if (!disconnectAudio()) {
                    Log.w(TAG, "stopVoiceRecognitionByHeadset: failed to disconnect audio from " + bluetoothDevice);
                }
                this.mVoiceRecognitionStarted = false;
            }
            if (!this.mSystemInterface.deactivateVoiceRecognition()) {
                Log.w(TAG, "stopVoiceRecognitionByHeadset: failed request from " + bluetoothDevice);
                return false;
            }
            return true;
        }
    }

    private void phoneStateChanged(final int i, final int i2, final int i3, final String str, final int i4, boolean z) {
        enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", "Need MODIFY_PHONE_STATE permission");
        synchronized (this.mStateMachines) {
            if (i + i2 > 0 || i3 != 6) {
                if (!z) {
                    if (this.mVirtualCallStarted) {
                        stopScoUsingVirtualVoiceCall();
                    }
                }
                if (this.mVoiceRecognitionStarted) {
                    stopVoiceRecognition(this.mActiveDevice);
                }
                if (this.mDialingOutTimeoutEvent != null) {
                    if (i3 == 2) {
                        this.mStateMachinesThread.getThreadHandler().removeCallbacks(this.mDialingOutTimeoutEvent);
                        doForStateMachine(this.mDialingOutTimeoutEvent.mDialingOutDevice, new StateMachineTask() {
                            @Override
                            public final void execute(HeadsetStateMachine headsetStateMachine) {
                                headsetStateMachine.sendMessage(14, 1, 0, this.f$0.mDialingOutTimeoutEvent.mDialingOutDevice);
                            }
                        });
                    } else if ((i3 == 0 || i3 == 6) && !this.mStateMachinesThread.getThreadHandler().hasCallbacks(this.mDialingOutTimeoutEvent)) {
                        this.mDialingOutTimeoutEvent = null;
                    }
                }
            } else if (this.mDialingOutTimeoutEvent != null) {
            }
        }
        Log.d(TAG, "numActive=" + i + ", numHeld=" + i2 + ", callState=" + i3 + ", number=" + str + ", type =" + i4 + ", isVirtual=" + z);
        this.mStateMachinesThread.getThreadHandler().post(new Runnable() {
            @Override
            public final void run() {
                HeadsetService.lambda$phoneStateChanged$2(this.f$0, i3, i, i2);
            }
        });
        doForEachConnectedStateMachine(new StateMachineTask() {
            @Override
            public final void execute(HeadsetStateMachine headsetStateMachine) {
                headsetStateMachine.sendMessage(9, new HeadsetCallState(i, i2, i3, str, i4));
            }
        });
        this.mStateMachinesThread.getThreadHandler().post(new Runnable() {
            @Override
            public final void run() {
                HeadsetService.lambda$phoneStateChanged$4(this.f$0, i3);
            }
        });
    }

    public static void lambda$phoneStateChanged$2(HeadsetService headsetService, int i, int i2, int i3) {
        if (i == 7 && headsetService.mSystemInterface.getHeadsetPhoneState().getNumActiveCall() == 0 && headsetService.mSystemInterface.getHeadsetPhoneState().getNumHeldCall() == 0) {
            headsetService.mOnlyReceiveDisconnectedCallState = true;
        } else {
            headsetService.mOnlyReceiveDisconnectedCallState = false;
        }
        boolean zShouldCallAudioBeActive = headsetService.shouldCallAudioBeActive();
        headsetService.mSystemInterface.getHeadsetPhoneState().setNumActiveCall(i2);
        headsetService.mSystemInterface.getHeadsetPhoneState().setNumHeldCall(i3);
        headsetService.mSystemInterface.getHeadsetPhoneState().setCallState(i);
        if (i != 7 && headsetService.shouldCallAudioBeActive() && !zShouldCallAudioBeActive) {
            headsetService.mSystemInterface.getAudioManager().setParameters("A2dpSuspended=true");
        }
    }

    public static void lambda$phoneStateChanged$4(HeadsetService headsetService, int i) {
        if (i == 6 && !headsetService.shouldCallAudioBeActive() && !headsetService.isAudioOn()) {
            headsetService.mSystemInterface.getAudioManager().setParameters("A2dpSuspended=false");
        }
    }

    private void clccResponse(final int i, final int i2, final int i3, final int i4, final boolean z, final String str, final int i5) {
        enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", "Need MODIFY_PHONE_STATE permission");
        doForEachConnectingConnectedStateMachine(new StateMachineTask() {
            @Override
            public final void execute(HeadsetStateMachine headsetStateMachine) {
                headsetStateMachine.sendMessage(11, new HeadsetClccResponse(i, i2, i3, i4, z, str, i5));
            }
        });
    }

    private boolean sendVendorSpecificResultCode(BluetoothDevice bluetoothDevice, String str, String str2) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                Log.w(TAG, "sendVendorSpecificResultCode: device " + bluetoothDevice + " was never connected/connecting");
                return false;
            }
            if (headsetStateMachine.getConnectionState() != 2) {
                return false;
            }
            if (!str.equals("+ANDROID")) {
                Log.w(TAG, "Disallowed unsolicited result code command: " + str);
                return false;
            }
            headsetStateMachine.sendMessage(12, new HeadsetVendorSpecificResultCode(bluetoothDevice, str, str2));
            return true;
        }
    }

    boolean isInbandRingingEnabled() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return (!BluetoothHeadset.isInbandRingingSupported(this) || SystemProperties.getBoolean(DISABLE_INBAND_RINGING_PROPERTY, false) || this.mInbandRingingRuntimeDisable) ? false : true;
    }

    @VisibleForTesting
    public void onConnectionStateChangedFromStateMachine(BluetoothDevice bluetoothDevice, int i, int i2) {
        synchronized (this.mStateMachines) {
            List<BluetoothDevice> devicesMatchingConnectionStates = getDevicesMatchingConnectionStates(CONNECTING_CONNECTED_STATES);
            if (i != 2 && i2 == 2) {
                if (devicesMatchingConnectionStates.size() > 1) {
                    this.mInbandRingingRuntimeDisable = true;
                    doForEachConnectedStateMachine(new StateMachineTask() {
                        @Override
                        public final void execute(HeadsetStateMachine headsetStateMachine) {
                            headsetStateMachine.sendMessage(13, 0);
                        }
                    });
                }
                MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.HEADSET);
            }
            if (i != 0 && i2 == 0) {
                if (devicesMatchingConnectionStates.size() <= 1) {
                    this.mInbandRingingRuntimeDisable = false;
                    doForEachConnectedStateMachine(new StateMachineTask() {
                        @Override
                        public final void execute(HeadsetStateMachine headsetStateMachine) {
                            headsetStateMachine.sendMessage(13, 1);
                        }
                    });
                }
                if (bluetoothDevice.equals(this.mActiveDevice)) {
                    setActiveDevice(null);
                }
            }
        }
    }

    private boolean isAudioModeIdle() {
        synchronized (this.mStateMachines) {
            if (!this.mVoiceRecognitionStarted && !this.mVirtualCallStarted && this.mSystemInterface.isCallIdle()) {
                return true;
            }
            Log.i(TAG, "isAudioModeIdle: not idle, mVoiceRecognitionStarted=" + this.mVoiceRecognitionStarted + ", mVirtualCallStarted=" + this.mVirtualCallStarted + ", isCallIdle=" + this.mSystemInterface.isCallIdle());
            return false;
        }
    }

    private boolean shouldCallAudioBeActive() {
        return this.mSystemInterface.isInCall() || (this.mSystemInterface.isRinging() && isInbandRingingEnabled());
    }

    private boolean shouldPersistAudio() {
        return !this.mVirtualCallStarted && shouldCallAudioBeActive();
    }

    @VisibleForTesting
    public void onAudioStateChangedFromStateMachine(BluetoothDevice bluetoothDevice, int i, int i2) {
        synchronized (this.mStateMachines) {
            if (i2 == 10) {
                if (i != 10) {
                    if (this.mActiveDevice != null && !this.mActiveDevice.equals(bluetoothDevice) && shouldPersistAudio() && !connectAudio(this.mActiveDevice)) {
                        Log.w(TAG, "onAudioStateChangedFromStateMachine, failed to connect audio to new active device " + this.mActiveDevice + ", after " + bluetoothDevice + " is disconnected from SCO");
                    }
                }
                if (this.mVoiceRecognitionStarted && !stopVoiceRecognitionByHeadset(bluetoothDevice)) {
                    Log.w(TAG, "onAudioStateChangedFromStateMachine: failed to stop voice recognition");
                }
                if (this.mVirtualCallStarted && !stopScoUsingVirtualVoiceCall()) {
                    Log.w(TAG, "onAudioStateChangedFromStateMachine: failed to stop virtual voice call");
                }
                if (this.mSystemInterface.isCallIdle()) {
                    this.mSystemInterface.getAudioManager().setParameters("A2dpSuspended=false");
                }
            }
        }
    }

    private void broadcastActiveDevice(BluetoothDevice bluetoothDevice) {
        logD("broadcastActiveDevice: " + bluetoothDevice);
        Intent intent = new Intent("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.addFlags(83886080);
        sendBroadcastAsUser(intent, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
    }

    public boolean okToAcceptConnection(BluetoothDevice bluetoothDevice) {
        if (getHeadsetService() == null) {
            Log.w(TAG, "okToAcceptConnection: return false as HeadsetService null");
            return false;
        }
        if (this.mAdapterService.isQuietModeEnabled()) {
            Log.w(TAG, "okToAcceptConnection: return false as quiet mode enabled");
            return false;
        }
        int priority = getPriority(bluetoothDevice);
        int bondState = this.mAdapterService.getBondState(bluetoothDevice);
        if (bondState != 12) {
            Log.w(TAG, "okToAcceptConnection: return false, bondState=" + bondState);
            return false;
        }
        if (priority != -1 && priority != 100 && priority != 1000) {
            Log.w(TAG, "okToAcceptConnection: return false, priority=" + priority);
            return false;
        }
        if (getDevicesMatchingConnectionStates(CONNECTING_CONNECTED_STATES).size() >= this.mMaxHeadsetConnections) {
            Log.w(TAG, "Maximum number of connections " + this.mMaxHeadsetConnections + " was reached, rejecting connection from " + bluetoothDevice);
            return false;
        }
        return true;
    }

    public boolean isScoAcceptable(BluetoothDevice bluetoothDevice) {
        synchronized (this.mStateMachines) {
            if (bluetoothDevice != null) {
                try {
                    if (bluetoothDevice.equals(this.mActiveDevice)) {
                        if (this.mForceScoAudio) {
                            return true;
                        }
                        if (!this.mAudioRouteAllowed) {
                            Log.w(TAG, "isScoAcceptable: rejected SCO since audio route is not allowed");
                            return false;
                        }
                        if (!this.mVoiceRecognitionStarted && !this.mVirtualCallStarted) {
                            if (shouldCallAudioBeActive()) {
                                return true;
                            }
                            Log.w(TAG, "isScoAcceptable: rejected SCO, inCall=" + this.mSystemInterface.isInCall() + ", voiceRecognition=" + this.mVoiceRecognitionStarted + ", ringing=" + this.mSystemInterface.isRinging() + ", inbandRinging=" + isInbandRingingEnabled() + ", isVirtualCallStarted=" + this.mVirtualCallStarted);
                            return false;
                        }
                        return true;
                    }
                } finally {
                }
            }
            Log.w(TAG, "isScoAcceptable: rejected SCO since " + bluetoothDevice + " is not the current active device " + this.mActiveDevice);
            return false;
        }
    }

    void removeStateMachine(BluetoothDevice bluetoothDevice) {
        synchronized (this.mStateMachines) {
            HeadsetStateMachine headsetStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (headsetStateMachine == null) {
                Log.w(TAG, "removeStateMachine(), " + bluetoothDevice + " does not have a state machine");
                return;
            }
            Log.i(TAG, "removeStateMachine(), removing state machine for device: " + bluetoothDevice);
            HeadsetObjectsFactory.getInstance().destroyStateMachine(headsetStateMachine);
            this.mStateMachines.remove(bluetoothDevice);
        }
    }

    private boolean isOnStateMachineThread() {
        Looper looperMyLooper = Looper.myLooper();
        return (looperMyLooper == null || this.mStateMachinesThread == null || looperMyLooper.getThread().getId() != this.mStateMachinesThread.getId()) ? false : true;
    }

    @Override
    public void dump(StringBuilder sb) {
        synchronized (this.mStateMachines) {
            super.dump(sb);
            ProfileService.println(sb, "mMaxHeadsetConnections: " + this.mMaxHeadsetConnections);
            ProfileService.println(sb, "DefaultMaxHeadsetConnections: " + this.mAdapterService.getMaxConnectedAudioDevices());
            ProfileService.println(sb, "mActiveDevice: " + this.mActiveDevice);
            ProfileService.println(sb, "isInbandRingingEnabled: " + isInbandRingingEnabled());
            ProfileService.println(sb, "isInbandRingingSupported: " + BluetoothHeadset.isInbandRingingSupported(this));
            ProfileService.println(sb, "mInbandRingingRuntimeDisable: " + this.mInbandRingingRuntimeDisable);
            ProfileService.println(sb, "mAudioRouteAllowed: " + this.mAudioRouteAllowed);
            ProfileService.println(sb, "mVoiceRecognitionStarted: " + this.mVoiceRecognitionStarted);
            ProfileService.println(sb, "mVoiceRecognitionTimeoutEvent: " + this.mVoiceRecognitionTimeoutEvent);
            ProfileService.println(sb, "mVirtualCallStarted: " + this.mVirtualCallStarted);
            ProfileService.println(sb, "mDialingOutTimeoutEvent: " + this.mDialingOutTimeoutEvent);
            ProfileService.println(sb, "mForceScoAudio: " + this.mForceScoAudio);
            ProfileService.println(sb, "mCreated: " + this.mCreated);
            ProfileService.println(sb, "mStarted: " + this.mStarted);
            ProfileService.println(sb, "AudioManager.isBluetoothScoOn(): " + this.mSystemInterface.getAudioManager().isBluetoothScoOn());
            ProfileService.println(sb, "Telecom.isInCall(): " + this.mSystemInterface.isInCall());
            ProfileService.println(sb, "Telecom.isRinging(): " + this.mSystemInterface.isRinging());
            for (HeadsetStateMachine headsetStateMachine : this.mStateMachines.values()) {
                ProfileService.println(sb, "==== StateMachine for " + headsetStateMachine.getDevice() + " ====");
                headsetStateMachine.dump(sb);
            }
        }
    }

    private static void logD(String str) {
        if (DBG) {
            Log.d(TAG, str);
        }
    }
}
