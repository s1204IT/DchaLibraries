package com.android.bluetooth.a2dp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.HandlerThread;
import android.provider.Settings;
import android.support.annotation.GuardedBy;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.avrcp.AvrcpTargetService;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class A2dpService extends ProfileService {
    private static final boolean DBG = true;
    private static final int MAX_A2DP_STATE_MACHINES = 50;
    private static final String TAG = "A2dpService";
    private static A2dpService sA2dpService;
    private A2dpCodecConfig mA2dpCodecConfig;

    @VisibleForTesting
    A2dpNativeInterface mA2dpNativeInterface;

    @GuardedBy("mStateMachines")
    private BluetoothDevice mActiveDevice;
    private BluetoothAdapter mAdapter;
    private AdapterService mAdapterService;
    private AudioManager mAudioManager;
    private BroadcastReceiver mBondStateChangedReceiver;
    private BroadcastReceiver mConnectionStateChangedReceiver;
    private HandlerThread mStateMachinesThread;
    private final ConcurrentMap<BluetoothDevice, A2dpStateMachine> mStateMachines = new ConcurrentHashMap();
    private int mMaxConnectedAudioDevices = 1;
    boolean mA2dpOffloadEnabled = false;

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothA2dpBinder(this);
    }

    @Override
    protected void create() {
        Log.i(TAG, "create()");
    }

    @Override
    protected boolean start() {
        Log.i(TAG, "start()");
        if (sA2dpService != null) {
            Log.w(TAG, "A2dpService start() called twice ");
            return true;
        }
        this.mAdapter = (BluetoothAdapter) Objects.requireNonNull(BluetoothAdapter.getDefaultAdapter(), "BluetoothAdapter cannot be null when A2dpService starts");
        this.mAdapterService = (AdapterService) Objects.requireNonNull(AdapterService.getAdapterService(), "AdapterService cannot be null when A2dpService starts");
        this.mA2dpNativeInterface = (A2dpNativeInterface) Objects.requireNonNull(A2dpNativeInterface.getInstance(), "A2dpNativeInterface cannot be null when A2dpService starts");
        this.mAudioManager = (AudioManager) getSystemService("audio");
        Objects.requireNonNull(this.mAudioManager, "AudioManager cannot be null when A2dpService starts");
        this.mMaxConnectedAudioDevices = this.mAdapterService.getMaxConnectedAudioDevices();
        Log.i(TAG, "Max connected audio devices set to " + this.mMaxConnectedAudioDevices);
        this.mStateMachines.clear();
        this.mStateMachinesThread = new HandlerThread("A2dpService.StateMachines");
        this.mStateMachinesThread.start();
        this.mA2dpCodecConfig = new A2dpCodecConfig(this, this.mA2dpNativeInterface);
        this.mA2dpNativeInterface.init(this.mMaxConnectedAudioDevices, this.mA2dpCodecConfig.codecConfigPriorities());
        this.mA2dpOffloadEnabled = this.mAdapterService.isA2dpOffloadEnabled();
        Log.d(TAG, "A2DP offload flag set to " + this.mA2dpOffloadEnabled);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        this.mBondStateChangedReceiver = new BondStateChangedReceiver();
        registerReceiver(this.mBondStateChangedReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        this.mConnectionStateChangedReceiver = new ConnectionStateChangedReceiver();
        registerReceiver(this.mConnectionStateChangedReceiver, intentFilter2);
        setA2dpService(this);
        setActiveDevice(null);
        return true;
    }

    @Override
    protected boolean stop() {
        Log.i(TAG, "stop()");
        if (sA2dpService == null) {
            Log.w(TAG, "stop() called before start()");
            return true;
        }
        if (this.mActiveDevice != null && AvrcpTargetService.get() != null) {
            AvrcpTargetService.get().storeVolumeForDevice(this.mActiveDevice);
        }
        removeActiveDevice(true);
        setA2dpService(null);
        try {
            unregisterReceiver(this.mConnectionStateChangedReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "mConnectionStateChangedReceiver Receiver not registered.");
        }
        this.mConnectionStateChangedReceiver = null;
        try {
            unregisterReceiver(this.mBondStateChangedReceiver);
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "mBondStateChangedReceiver Receiver not registered.");
        }
        this.mBondStateChangedReceiver = null;
        if (this.mA2dpNativeInterface != null) {
            this.mA2dpNativeInterface.cleanup();
            this.mA2dpNativeInterface = null;
        }
        this.mA2dpCodecConfig = null;
        synchronized (this.mStateMachines) {
            for (A2dpStateMachine a2dpStateMachine : this.mStateMachines.values()) {
                a2dpStateMachine.doQuit();
                a2dpStateMachine.cleanup();
            }
            this.mStateMachines.clear();
        }
        if (this.mStateMachinesThread != null) {
            this.mStateMachinesThread.quitSafely();
            this.mStateMachinesThread = null;
        }
        this.mMaxConnectedAudioDevices = 1;
        this.mAudioManager = null;
        this.mA2dpNativeInterface = null;
        this.mAdapterService = null;
        this.mAdapter = null;
        return true;
    }

    @Override
    protected void cleanup() {
        Log.i(TAG, "cleanup()");
    }

    public static synchronized A2dpService getA2dpService() {
        if (sA2dpService == null) {
            Log.w(TAG, "getA2dpService(): service is null");
            return null;
        }
        if (!sA2dpService.isAvailable()) {
            Log.w(TAG, "getA2dpService(): service is not available");
            return null;
        }
        return sA2dpService;
    }

    private static synchronized void setA2dpService(A2dpService a2dpService) {
        Log.d(TAG, "setA2dpService(): set to: " + a2dpService);
        sA2dpService = a2dpService;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        Log.d(TAG, "connect(): " + bluetoothDevice);
        if (getPriority(bluetoothDevice) == 0) {
            Log.e(TAG, "Cannot connect to " + bluetoothDevice + " : PRIORITY_OFF");
            return false;
        }
        if (!BluetoothUuid.isUuidPresent(this.mAdapterService.getRemoteUuids(bluetoothDevice), BluetoothUuid.AudioSink)) {
            Log.e(TAG, "Cannot connect to " + bluetoothDevice + " : Remote does not have A2DP Sink UUID");
            return false;
        }
        synchronized (this.mStateMachines) {
            if (!connectionAllowedCheckMaxDevices(bluetoothDevice)) {
                if (this.mMaxConnectedAudioDevices == 1) {
                    for (BluetoothDevice bluetoothDevice2 : getDevicesMatchingConnectionStates(new int[]{2, 1, 3})) {
                        if (bluetoothDevice2.equals(bluetoothDevice)) {
                            Log.w(TAG, "Connecting to device " + bluetoothDevice + " : disconnect skipped");
                        } else {
                            disconnect(bluetoothDevice2);
                        }
                    }
                } else {
                    Log.e(TAG, "Cannot connect to " + bluetoothDevice + " : too many connected devices");
                    return false;
                }
            }
            A2dpStateMachine orCreateStateMachine = getOrCreateStateMachine(bluetoothDevice);
            if (orCreateStateMachine == null) {
                Log.e(TAG, "Cannot connect to " + bluetoothDevice + " : no state machine");
                return false;
            }
            orCreateStateMachine.sendMessage(1);
            return true;
        }
    }

    boolean disconnect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        Log.d(TAG, "disconnect(): " + bluetoothDevice);
        synchronized (this.mStateMachines) {
            A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (a2dpStateMachine == null) {
                Log.e(TAG, "Ignored disconnect request for " + bluetoothDevice + " : no state machine");
                return false;
            }
            a2dpStateMachine.sendMessage(2);
            return true;
        }
    }

    public List<BluetoothDevice> getConnectedDevices() {
        ArrayList arrayList;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            arrayList = new ArrayList();
            for (A2dpStateMachine a2dpStateMachine : this.mStateMachines.values()) {
                if (a2dpStateMachine.isConnected()) {
                    arrayList.add(a2dpStateMachine.getDevice());
                }
            }
        }
        return arrayList;
    }

    private boolean connectionAllowedCheckMaxDevices(BluetoothDevice bluetoothDevice) {
        synchronized (this.mStateMachines) {
            int i = 0;
            for (A2dpStateMachine a2dpStateMachine : this.mStateMachines.values()) {
                switch (a2dpStateMachine.getConnectionState()) {
                    case 1:
                    case 2:
                        if (Objects.equals(bluetoothDevice, a2dpStateMachine.getDevice())) {
                            return true;
                        }
                        i++;
                        break;
                        break;
                }
            }
            return i < this.mMaxConnectedAudioDevices;
        }
    }

    @VisibleForTesting(otherwise = 3)
    public boolean okToConnect(BluetoothDevice bluetoothDevice, boolean z) {
        Log.i(TAG, "okToConnect: device " + bluetoothDevice + " isOutgoingRequest: " + z);
        if (this.mAdapterService.isQuietModeEnabled() && !z) {
            Log.e(TAG, "okToConnect: cannot connect to " + bluetoothDevice + " : quiet mode enabled");
            return false;
        }
        if (!connectionAllowedCheckMaxDevices(bluetoothDevice)) {
            Log.e(TAG, "okToConnect: cannot connect to " + bluetoothDevice + " : too many connected devices");
            return false;
        }
        int priority = getPriority(bluetoothDevice);
        int bondState = this.mAdapterService.getBondState(bluetoothDevice);
        if (bondState != 12) {
            Log.w(TAG, "okToConnect: return false, bondState=" + bondState);
            return false;
        }
        if (priority != -1 && priority != 100 && priority != 1000) {
            Log.w(TAG, "okToConnect: return false, priority=" + priority);
            return false;
        }
        return true;
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        int connectionState;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList();
        Set<BluetoothDevice> bondedDevices = this.mAdapter.getBondedDevices();
        synchronized (this.mStateMachines) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                if (BluetoothUuid.isUuidPresent(this.mAdapterService.getRemoteUuids(bluetoothDevice), BluetoothUuid.AudioSink)) {
                    A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
                    if (a2dpStateMachine != null) {
                        connectionState = a2dpStateMachine.getConnectionState();
                    } else {
                        connectionState = 0;
                    }
                    for (int i : iArr) {
                        if (connectionState == i) {
                            arrayList.add(bluetoothDevice);
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    @VisibleForTesting
    List<BluetoothDevice> getDevices() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mStateMachines) {
            Iterator<A2dpStateMachine> it = this.mStateMachines.values().iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().getDevice());
            }
        }
        return arrayList;
    }

    public int getConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (a2dpStateMachine == null) {
                return 0;
            }
            return a2dpStateMachine.getConnectionState();
        }
    }

    private void removeActiveDevice(boolean z) {
        BluetoothDevice bluetoothDevice = this.mActiveDevice;
        synchronized (this.mStateMachines) {
            this.mActiveDevice = null;
            broadcastActiveDevice(null);
            if (bluetoothDevice == null) {
                return;
            }
            boolean z2 = !z && getConnectionState(bluetoothDevice) == 2;
            Log.i(TAG, "removeActiveDevice: suppressNoisyIntent=" + z2);
            this.mAudioManager.setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(bluetoothDevice, 0, 2, z2, -1);
            if (!this.mA2dpNativeInterface.setActiveDevice(null)) {
                Log.w(TAG, "setActiveDevice(null): Cannot remove active device in native layer");
            }
        }
    }

    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        boolean z;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        synchronized (this.mStateMachines) {
            BluetoothDevice bluetoothDevice2 = this.mActiveDevice;
            Log.d(TAG, "setActiveDevice(" + bluetoothDevice + "): previous is " + bluetoothDevice2);
            if (bluetoothDevice2 != null && AvrcpTargetService.get() != null) {
                AvrcpTargetService.get().storeVolumeForDevice(bluetoothDevice2);
            }
            if (bluetoothDevice == null) {
                removeActiveDevice(false);
                return true;
            }
            A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (a2dpStateMachine == null) {
                Log.e(TAG, "setActiveDevice(" + bluetoothDevice + "): Cannot set as active: no state machine");
                return false;
            }
            if (a2dpStateMachine.getConnectionState() != 2) {
                Log.e(TAG, "setActiveDevice(" + bluetoothDevice + "): Cannot set as active: device is not connected");
                return false;
            }
            if (!this.mA2dpNativeInterface.setActiveDevice(bluetoothDevice)) {
                Log.e(TAG, "setActiveDevice(" + bluetoothDevice + "): Cannot set as active in native layer");
                return false;
            }
            boolean z2 = !Objects.equals(bluetoothDevice, this.mActiveDevice);
            this.mActiveDevice = bluetoothDevice;
            broadcastActiveDevice(this.mActiveDevice);
            if (z2) {
                if (bluetoothDevice2 != null) {
                    if (this.mAudioManager.isStreamMute(3)) {
                        z = false;
                    } else {
                        this.mAudioManager.adjustStreamVolume(3, -100, 0);
                        z = true;
                    }
                    this.mAudioManager.setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(bluetoothDevice2, 0, 2, true, -1);
                } else {
                    z = false;
                }
                int rememberedVolumeForDevice = -1;
                if (AvrcpTargetService.get() != null) {
                    AvrcpTargetService.get().volumeDeviceSwitched(this.mActiveDevice);
                    rememberedVolumeForDevice = AvrcpTargetService.get().getRememberedVolumeForDevice(this.mActiveDevice);
                }
                this.mAudioManager.setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(this.mActiveDevice, 2, 2, true, rememberedVolumeForDevice);
                if (z) {
                    this.mAudioManager.adjustStreamVolume(3, 100, 0);
                }
            }
            return true;
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

    private boolean isActiveDevice(BluetoothDevice bluetoothDevice) {
        boolean z;
        synchronized (this.mStateMachines) {
            if (bluetoothDevice != null) {
                try {
                    z = Objects.equals(bluetoothDevice, this.mActiveDevice);
                } finally {
                }
            }
        }
        return z;
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothA2dpSinkPriorityKey(bluetoothDevice.getAddress()), i);
        Log.d(TAG, "Saved priority " + bluetoothDevice + " = " + i);
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothA2dpSinkPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    public boolean isAvrcpAbsoluteVolumeSupported() {
        return false;
    }

    public void setAvrcpAbsoluteVolume(int i) {
        if (AvrcpTargetService.get() != null) {
            AvrcpTargetService.get().sendVolumeChanged(i);
        }
    }

    boolean isA2dpPlaying(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Log.d(TAG, "isA2dpPlaying(" + bluetoothDevice + ")");
        synchronized (this.mStateMachines) {
            A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (a2dpStateMachine == null) {
                return false;
            }
            return a2dpStateMachine.isPlaying();
        }
    }

    public BluetoothCodecStatus getCodecStatus(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Log.d(TAG, "getCodecStatus(" + bluetoothDevice + ")");
        synchronized (this.mStateMachines) {
            if (bluetoothDevice == null) {
                try {
                    bluetoothDevice = this.mActiveDevice;
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (bluetoothDevice == null) {
                return null;
            }
            A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (a2dpStateMachine == null) {
                return null;
            }
            return a2dpStateMachine.getCodecStatus();
        }
    }

    public void setCodecConfigPreference(BluetoothDevice bluetoothDevice, BluetoothCodecConfig bluetoothCodecConfig) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Log.d(TAG, "setCodecConfigPreference(" + bluetoothDevice + "): " + Objects.toString(bluetoothCodecConfig));
        if (bluetoothDevice == null) {
            bluetoothDevice = this.mActiveDevice;
        }
        if (bluetoothDevice == null) {
            Log.e(TAG, "Cannot set codec config preference: no active A2DP device");
        } else {
            this.mA2dpCodecConfig.setCodecConfigPreference(bluetoothDevice, bluetoothCodecConfig);
        }
    }

    public void enableOptionalCodecs(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Log.d(TAG, "enableOptionalCodecs(" + bluetoothDevice + ")");
        if (bluetoothDevice == null) {
            bluetoothDevice = this.mActiveDevice;
        }
        if (bluetoothDevice == null) {
            Log.e(TAG, "Cannot enable optional codecs: no active A2DP device");
        } else {
            this.mA2dpCodecConfig.enableOptionalCodecs(bluetoothDevice);
        }
    }

    public void disableOptionalCodecs(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Log.d(TAG, "disableOptionalCodecs(" + bluetoothDevice + ")");
        if (bluetoothDevice == null) {
            bluetoothDevice = this.mActiveDevice;
        }
        if (bluetoothDevice == null) {
            Log.e(TAG, "Cannot disable optional codecs: no active A2DP device");
        } else {
            this.mA2dpCodecConfig.disableOptionalCodecs(bluetoothDevice);
        }
    }

    public int getSupportsOptionalCodecs(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothA2dpSupportsOptionalCodecsKey(bluetoothDevice.getAddress()), -1);
    }

    public void setSupportsOptionalCodecs(BluetoothDevice bluetoothDevice, boolean z) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothA2dpSupportsOptionalCodecsKey(bluetoothDevice.getAddress()), z ? 1 : 0);
    }

    public int getOptionalCodecsEnabled(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothA2dpOptionalCodecsEnabledKey(bluetoothDevice.getAddress()), -1);
    }

    public void setOptionalCodecsEnabled(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        if (i != -1 && i != 0 && i != 1) {
            Log.w(TAG, "Unexpected value passed to setOptionalCodecsEnabled:" + i);
            return;
        }
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothA2dpOptionalCodecsEnabledKey(bluetoothDevice.getAddress()), i);
    }

    void messageFromNative(A2dpStackEvent a2dpStackEvent) {
        Objects.requireNonNull(a2dpStackEvent.device, "Device should never be null, event: " + a2dpStackEvent);
        synchronized (this.mStateMachines) {
            BluetoothDevice bluetoothDevice = a2dpStackEvent.device;
            A2dpStateMachine orCreateStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (orCreateStateMachine == null && a2dpStackEvent.type == 1) {
                switch (a2dpStackEvent.valueInt) {
                    case 1:
                    case 2:
                        if (!connectionAllowedCheckMaxDevices(bluetoothDevice)) {
                            Log.e(TAG, "Cannot connect to " + bluetoothDevice + " : too many connected devices");
                            return;
                        }
                        orCreateStateMachine = getOrCreateStateMachine(bluetoothDevice);
                        break;
                        break;
                }
            }
            if (orCreateStateMachine == null) {
                Log.e(TAG, "Cannot process stack event: no state machine: " + a2dpStackEvent);
                return;
            }
            orCreateStateMachine.sendMessage(101, a2dpStackEvent);
        }
    }

    void codecConfigUpdated(BluetoothDevice bluetoothDevice, BluetoothCodecStatus bluetoothCodecStatus, boolean z) {
        broadcastCodecConfig(bluetoothDevice, bluetoothCodecStatus);
        if (isActiveDevice(bluetoothDevice) && !z) {
            this.mAudioManager.handleBluetoothA2dpDeviceConfigChange(bluetoothDevice);
        }
    }

    private A2dpStateMachine getOrCreateStateMachine(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "getOrCreateStateMachine failed: device cannot be null");
            return null;
        }
        synchronized (this.mStateMachines) {
            A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (a2dpStateMachine != null) {
                return a2dpStateMachine;
            }
            if (this.mStateMachines.size() >= MAX_A2DP_STATE_MACHINES) {
                Log.e(TAG, "Maximum number of A2DP state machines reached: 50");
                return null;
            }
            Log.d(TAG, "Creating a new state machine for " + bluetoothDevice);
            A2dpStateMachine a2dpStateMachineMake = A2dpStateMachine.make(bluetoothDevice, this, this.mA2dpNativeInterface, this.mStateMachinesThread.getLooper());
            this.mStateMachines.put(bluetoothDevice, a2dpStateMachineMake);
            return a2dpStateMachineMake;
        }
    }

    private void broadcastActiveDevice(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "broadcastActiveDevice(" + bluetoothDevice + ")");
        Intent intent = new Intent("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.addFlags(83886080);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void broadcastCodecConfig(BluetoothDevice bluetoothDevice, BluetoothCodecStatus bluetoothCodecStatus) {
        Log.d(TAG, "broadcastCodecConfig(" + bluetoothDevice + "): " + bluetoothCodecStatus);
        Intent intent = new Intent("android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED");
        intent.putExtra("android.bluetooth.codec.extra.CODEC_STATUS", bluetoothCodecStatus);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.addFlags(83886080);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private class BondStateChangedReceiver extends BroadcastReceiver {
        private BondStateChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.bluetooth.device.action.BOND_STATE_CHANGED".equals(intent.getAction())) {
                return;
            }
            int intExtra = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", Integer.MIN_VALUE);
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            Objects.requireNonNull(bluetoothDevice, "ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
            A2dpService.this.bondStateChanged(bluetoothDevice, intExtra);
        }
    }

    @VisibleForTesting
    void bondStateChanged(BluetoothDevice bluetoothDevice, int i) {
        Log.d(TAG, "Bond state changed for device: " + bluetoothDevice + " state: " + i);
        if (i != 10) {
            return;
        }
        synchronized (this.mStateMachines) {
            A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (a2dpStateMachine == null) {
                return;
            }
            if (a2dpStateMachine.getConnectionState() != 0) {
                return;
            }
            removeStateMachine(bluetoothDevice);
        }
    }

    private void removeStateMachine(BluetoothDevice bluetoothDevice) {
        synchronized (this.mStateMachines) {
            A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (a2dpStateMachine == null) {
                Log.w(TAG, "removeStateMachine: device " + bluetoothDevice + " does not have a state machine");
                return;
            }
            Log.i(TAG, "removeStateMachine: removing state machine for device: " + bluetoothDevice);
            a2dpStateMachine.doQuit();
            a2dpStateMachine.cleanup();
            this.mStateMachines.remove(bluetoothDevice);
        }
    }

    private void updateOptionalCodecsSupport(BluetoothDevice bluetoothDevice) {
        boolean z;
        int supportsOptionalCodecs = getSupportsOptionalCodecs(bluetoothDevice);
        synchronized (this.mStateMachines) {
            A2dpStateMachine a2dpStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (a2dpStateMachine == null) {
                return;
            }
            BluetoothCodecStatus codecStatus = a2dpStateMachine.getCodecStatus();
            if (codecStatus != null) {
                for (BluetoothCodecConfig bluetoothCodecConfig : codecStatus.getCodecsSelectableCapabilities()) {
                    if (!bluetoothCodecConfig.isMandatoryCodec()) {
                        z = true;
                        break;
                    }
                }
                z = false;
            } else {
                z = false;
            }
            if (supportsOptionalCodecs == -1) {
                setSupportsOptionalCodecs(bluetoothDevice, z);
            } else if (z != (supportsOptionalCodecs == 1)) {
            }
            if (z) {
                int optionalCodecsEnabled = getOptionalCodecsEnabled(bluetoothDevice);
                if (optionalCodecsEnabled == 1) {
                    enableOptionalCodecs(bluetoothDevice);
                } else if (optionalCodecsEnabled == 0) {
                    disableOptionalCodecs(bluetoothDevice);
                }
            }
        }
    }

    private void connectionStateChanged(BluetoothDevice bluetoothDevice, int i, int i2) {
        if (bluetoothDevice == null || i == i2) {
            return;
        }
        synchronized (this.mStateMachines) {
            if (i2 == 2) {
                try {
                    updateOptionalCodecsSupport(bluetoothDevice);
                    MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.A2DP);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (i2 == 2 && this.mMaxConnectedAudioDevices == 1) {
                setActiveDevice(bluetoothDevice);
            }
            if (isActiveDevice(bluetoothDevice) && i == 2) {
                setActiveDevice(null);
            }
            if (i2 == 0 && this.mAdapterService.getBondState(bluetoothDevice) == 10) {
                removeStateMachine(bluetoothDevice);
            }
        }
    }

    private class ConnectionStateChangedReceiver extends BroadcastReceiver {
        private ConnectionStateChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED".equals(intent.getAction())) {
                return;
            }
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
            A2dpService.this.connectionStateChanged(bluetoothDevice, intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1), intExtra);
        }
    }

    @VisibleForTesting
    static class BluetoothA2dpBinder extends IBluetoothA2dp.Stub implements ProfileService.IProfileServiceBinder {
        private A2dpService mService;

        private A2dpService getService() {
            if (!Utils.checkCaller()) {
                Log.w(A2dpService.TAG, "A2DP call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        BluetoothA2dpBinder(A2dpService a2dpService) {
            this.mService = a2dpService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            A2dpService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            A2dpService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return false;
            }
            return service.setActiveDevice(bluetoothDevice);
        }

        public BluetoothDevice getActiveDevice() {
            A2dpService service = getService();
            if (service == null) {
                return null;
            }
            return service.getActiveDevice();
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            A2dpService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }

        public boolean isAvrcpAbsoluteVolumeSupported() {
            A2dpService service = getService();
            if (service == null) {
                return false;
            }
            return service.isAvrcpAbsoluteVolumeSupported();
        }

        public void setAvrcpAbsoluteVolume(int i) {
            A2dpService service = getService();
            if (service == null) {
                return;
            }
            service.setAvrcpAbsoluteVolume(i);
        }

        public boolean isA2dpPlaying(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return false;
            }
            return service.isA2dpPlaying(bluetoothDevice);
        }

        public BluetoothCodecStatus getCodecStatus(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return null;
            }
            return service.getCodecStatus(bluetoothDevice);
        }

        public void setCodecConfigPreference(BluetoothDevice bluetoothDevice, BluetoothCodecConfig bluetoothCodecConfig) {
            A2dpService service = getService();
            if (service == null) {
                return;
            }
            service.setCodecConfigPreference(bluetoothDevice, bluetoothCodecConfig);
        }

        public void enableOptionalCodecs(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return;
            }
            service.enableOptionalCodecs(bluetoothDevice);
        }

        public void disableOptionalCodecs(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return;
            }
            service.disableOptionalCodecs(bluetoothDevice);
        }

        public int supportsOptionalCodecs(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getSupportsOptionalCodecs(bluetoothDevice);
        }

        public int getOptionalCodecsEnabled(BluetoothDevice bluetoothDevice) {
            A2dpService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getOptionalCodecsEnabled(bluetoothDevice);
        }

        public void setOptionalCodecsEnabled(BluetoothDevice bluetoothDevice, int i) {
            A2dpService service = getService();
            if (service == null) {
                return;
            }
            service.setOptionalCodecsEnabled(bluetoothDevice, i);
        }
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        ProfileService.println(sb, "mActiveDevice: " + this.mActiveDevice);
        Iterator<A2dpStateMachine> it = this.mStateMachines.values().iterator();
        while (it.hasNext()) {
            it.next().dump(sb);
        }
    }
}
