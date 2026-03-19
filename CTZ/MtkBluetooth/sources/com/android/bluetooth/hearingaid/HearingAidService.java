package com.android.bluetooth.hearingaid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothHearingAid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.HandlerThread;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class HearingAidService extends ProfileService {
    private static final boolean DBG = false;
    private static final int MAX_HEARING_AID_STATE_MACHINES = 10;
    private static final String TAG = "HearingAidService";
    private static HearingAidService sHearingAidService;
    private AdapterService mAdapterService;

    @VisibleForTesting
    AudioManager mAudioManager;
    private BroadcastReceiver mBondStateChangedReceiver;
    private BroadcastReceiver mConnectionStateChangedReceiver;

    @VisibleForTesting
    HearingAidNativeInterface mHearingAidNativeInterface;
    private BluetoothDevice mPreviousAudioDevice;
    private HandlerThread mStateMachinesThread;
    private final Map<BluetoothDevice, HearingAidStateMachine> mStateMachines = new HashMap();
    private final Map<BluetoothDevice, Long> mDeviceHiSyncIdMap = new ConcurrentHashMap();
    private final Map<BluetoothDevice, Integer> mDeviceCapabilitiesMap = new HashMap();
    private long mActiveDeviceHiSyncId = 0;

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothHearingAidBinder(this);
    }

    @Override
    protected void create() {
    }

    @Override
    protected boolean start() {
        if (sHearingAidService != null) {
            throw new IllegalStateException("start() called twice");
        }
        this.mAdapterService = (AdapterService) Objects.requireNonNull(AdapterService.getAdapterService(), "AdapterService cannot be null when HearingAidService starts");
        this.mHearingAidNativeInterface = (HearingAidNativeInterface) Objects.requireNonNull(HearingAidNativeInterface.getInstance(), "HearingAidNativeInterface cannot be null when HearingAidService starts");
        this.mAudioManager = (AudioManager) getSystemService("audio");
        Objects.requireNonNull(this.mAudioManager, "AudioManager cannot be null when HearingAidService starts");
        this.mStateMachines.clear();
        this.mStateMachinesThread = new HandlerThread("HearingAidService.StateMachines");
        this.mStateMachinesThread.start();
        this.mDeviceHiSyncIdMap.clear();
        this.mDeviceCapabilitiesMap.clear();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        this.mBondStateChangedReceiver = new BondStateChangedReceiver();
        registerReceiver(this.mBondStateChangedReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED");
        this.mConnectionStateChangedReceiver = new ConnectionStateChangedReceiver();
        registerReceiver(this.mConnectionStateChangedReceiver, intentFilter2);
        setHearingAidService(this);
        this.mHearingAidNativeInterface.init();
        return true;
    }

    @Override
    protected boolean stop() {
        if (sHearingAidService == null) {
            Log.w(TAG, "stop() called before start()");
            return true;
        }
        this.mHearingAidNativeInterface.cleanup();
        this.mHearingAidNativeInterface = null;
        setHearingAidService(null);
        unregisterReceiver(this.mBondStateChangedReceiver);
        this.mBondStateChangedReceiver = null;
        unregisterReceiver(this.mConnectionStateChangedReceiver);
        this.mConnectionStateChangedReceiver = null;
        synchronized (this.mStateMachines) {
            for (HearingAidStateMachine hearingAidStateMachine : this.mStateMachines.values()) {
                hearingAidStateMachine.doQuit();
                hearingAidStateMachine.cleanup();
            }
            this.mStateMachines.clear();
        }
        this.mDeviceHiSyncIdMap.clear();
        this.mDeviceCapabilitiesMap.clear();
        if (this.mStateMachinesThread != null) {
            this.mStateMachinesThread.quitSafely();
            this.mStateMachinesThread = null;
        }
        this.mAudioManager = null;
        this.mHearingAidNativeInterface = null;
        this.mAdapterService = null;
        return true;
    }

    @Override
    protected void cleanup() {
    }

    public static synchronized HearingAidService getHearingAidService() {
        if (sHearingAidService == null) {
            Log.w(TAG, "getHearingAidService(): service is NULL");
            return null;
        }
        if (!sHearingAidService.isAvailable()) {
            Log.w(TAG, "getHearingAidService(): service is not available");
            return null;
        }
        return sHearingAidService;
    }

    private static synchronized void setHearingAidService(HearingAidService hearingAidService) {
        sHearingAidService = hearingAidService;
    }

    boolean connect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        if (bluetoothDevice == null || getPriority(bluetoothDevice) == 0) {
            return false;
        }
        if (!BluetoothUuid.isUuidPresent(this.mAdapterService.getRemoteUuids(bluetoothDevice), BluetoothUuid.HearingAid)) {
            Log.e(TAG, "Cannot connect to " + bluetoothDevice + " : Remote does not have Hearing Aid UUID");
            return false;
        }
        long jLongValue = this.mDeviceHiSyncIdMap.getOrDefault(bluetoothDevice, 0L).longValue();
        if (jLongValue != this.mActiveDeviceHiSyncId && jLongValue != 0 && this.mActiveDeviceHiSyncId != 0) {
            Iterator<BluetoothDevice> it = getConnectedDevices().iterator();
            while (it.hasNext()) {
                disconnect(it.next());
            }
        }
        synchronized (this.mStateMachines) {
            HearingAidStateMachine orCreateStateMachine = getOrCreateStateMachine(bluetoothDevice);
            if (orCreateStateMachine == null) {
                Log.e(TAG, "Cannot connect to " + bluetoothDevice + " : no state machine");
            }
            orCreateStateMachine.sendMessage(1);
        }
        for (BluetoothDevice bluetoothDevice2 : this.mDeviceHiSyncIdMap.keySet()) {
            if (!bluetoothDevice.equals(bluetoothDevice2) && this.mDeviceHiSyncIdMap.getOrDefault(bluetoothDevice2, 0L).longValue() == jLongValue) {
                synchronized (this.mStateMachines) {
                    HearingAidStateMachine orCreateStateMachine2 = getOrCreateStateMachine(bluetoothDevice2);
                    if (orCreateStateMachine2 == null) {
                        Log.e(TAG, "Ignored connect request for " + bluetoothDevice + " : no state machine");
                    } else {
                        orCreateStateMachine2.sendMessage(1);
                        if (jLongValue == 0 && !bluetoothDevice.equals(bluetoothDevice2)) {
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }

    boolean disconnect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        if (bluetoothDevice == null) {
            return false;
        }
        long jLongValue = this.mDeviceHiSyncIdMap.getOrDefault(bluetoothDevice, 0L).longValue();
        for (BluetoothDevice bluetoothDevice2 : this.mDeviceHiSyncIdMap.keySet()) {
            if (this.mDeviceHiSyncIdMap.getOrDefault(bluetoothDevice2, 0L).longValue() == jLongValue) {
                synchronized (this.mStateMachines) {
                    HearingAidStateMachine hearingAidStateMachine = this.mStateMachines.get(bluetoothDevice2);
                    if (hearingAidStateMachine == null) {
                        Log.e(TAG, "Ignored disconnect request for " + bluetoothDevice + " : no state machine");
                    } else {
                        hearingAidStateMachine.sendMessage(2);
                        if (jLongValue == 0 && !bluetoothDevice.equals(bluetoothDevice2)) {
                            return true;
                        }
                    }
                }
            }
        }
        return true;
    }

    List<BluetoothDevice> getConnectedDevices() {
        ArrayList arrayList;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            arrayList = new ArrayList();
            for (HearingAidStateMachine hearingAidStateMachine : this.mStateMachines.values()) {
                if (hearingAidStateMachine.isConnected()) {
                    arrayList.add(hearingAidStateMachine.getDevice());
                }
            }
        }
        return arrayList;
    }

    @VisibleForTesting(otherwise = 3)
    public boolean okToConnect(BluetoothDevice bluetoothDevice) {
        if (this.mAdapterService.isQuietModeEnabled()) {
            Log.e(TAG, "okToConnect: cannot connect to " + bluetoothDevice + " : quiet mode enabled");
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
        BluetoothDevice[] bondedDevices;
        int connectionState;
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList();
        if (iArr == null || (bondedDevices = this.mAdapterService.getBondedDevices()) == null) {
            return arrayList;
        }
        synchronized (this.mStateMachines) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                if (BluetoothUuid.isUuidPresent(bluetoothDevice.getUuids(), BluetoothUuid.HearingAid)) {
                    HearingAidStateMachine hearingAidStateMachine = this.mStateMachines.get(bluetoothDevice);
                    if (hearingAidStateMachine != null) {
                        connectionState = hearingAidStateMachine.getConnectionState();
                    } else {
                        connectionState = 0;
                    }
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

    @VisibleForTesting
    List<BluetoothDevice> getDevices() {
        ArrayList arrayList = new ArrayList();
        synchronized (this.mStateMachines) {
            Iterator<HearingAidStateMachine> it = this.mStateMachines.values().iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().getDevice());
            }
        }
        return arrayList;
    }

    int getConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        synchronized (this.mStateMachines) {
            HearingAidStateMachine hearingAidStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (hearingAidStateMachine == null) {
                return 0;
            }
            return hearingAidStateMachine.getConnectionState();
        }
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothHearingAidPriorityKey(bluetoothDevice.getAddress()), i);
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothHearingAidPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    void setVolume(int i) {
        this.mHearingAidNativeInterface.setVolume(i);
    }

    long getHiSyncId(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return 0L;
        }
        return this.mDeviceHiSyncIdMap.getOrDefault(bluetoothDevice, 0L).longValue();
    }

    int getCapabilities(BluetoothDevice bluetoothDevice) {
        return this.mDeviceCapabilitiesMap.getOrDefault(bluetoothDevice, -1).intValue();
    }

    public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        synchronized (this.mStateMachines) {
            try {
                if (bluetoothDevice == null) {
                    if (this.mActiveDeviceHiSyncId != 0) {
                        reportActiveDevice(null);
                        this.mActiveDeviceHiSyncId = 0L;
                    }
                    return true;
                }
                if (getConnectionState(bluetoothDevice) != 2) {
                    Log.e(TAG, "setActiveDevice(" + bluetoothDevice + "): failed because device not connected");
                    return false;
                }
                Long orDefault = this.mDeviceHiSyncIdMap.getOrDefault(bluetoothDevice, 0L);
                if (orDefault.longValue() != this.mActiveDeviceHiSyncId) {
                    this.mActiveDeviceHiSyncId = orDefault.longValue();
                    reportActiveDevice(bluetoothDevice);
                }
                return true;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    List<BluetoothDevice> getActiveDevices() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(null);
        arrayList.add(null);
        synchronized (this.mStateMachines) {
            if (this.mActiveDeviceHiSyncId == 0) {
                return arrayList;
            }
            for (BluetoothDevice bluetoothDevice : this.mDeviceHiSyncIdMap.keySet()) {
                if (getConnectionState(bluetoothDevice) == 2) {
                    if (this.mDeviceHiSyncIdMap.get(bluetoothDevice).longValue() == this.mActiveDeviceHiSyncId) {
                        if ((getCapabilities(bluetoothDevice) & 1) == 1) {
                            arrayList.set(1, bluetoothDevice);
                        } else {
                            arrayList.set(0, bluetoothDevice);
                        }
                    }
                }
            }
            return arrayList;
        }
    }

    void messageFromNative(HearingAidStackEvent hearingAidStackEvent) {
        Objects.requireNonNull(hearingAidStackEvent.device, "Device should never be null, event: " + hearingAidStackEvent);
        if (hearingAidStackEvent.type == 2) {
            BluetoothDevice bluetoothDevice = hearingAidStackEvent.device;
            int i = hearingAidStackEvent.valueInt1;
            long j = hearingAidStackEvent.valueLong2;
            this.mDeviceCapabilitiesMap.put(bluetoothDevice, Integer.valueOf(i));
            this.mDeviceHiSyncIdMap.put(bluetoothDevice, Long.valueOf(j));
            return;
        }
        synchronized (this.mStateMachines) {
            BluetoothDevice bluetoothDevice2 = hearingAidStackEvent.device;
            HearingAidStateMachine orCreateStateMachine = this.mStateMachines.get(bluetoothDevice2);
            if (orCreateStateMachine == null && hearingAidStackEvent.type == 1) {
                switch (hearingAidStackEvent.valueInt1) {
                    case 1:
                    case 2:
                        orCreateStateMachine = getOrCreateStateMachine(bluetoothDevice2);
                        break;
                }
            }
            if (orCreateStateMachine == null) {
                Log.e(TAG, "Cannot process stack event: no state machine: " + hearingAidStackEvent);
                return;
            }
            orCreateStateMachine.sendMessage(101, hearingAidStackEvent);
        }
    }

    private HearingAidStateMachine getOrCreateStateMachine(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.e(TAG, "getOrCreateStateMachine failed: device cannot be null");
            return null;
        }
        synchronized (this.mStateMachines) {
            HearingAidStateMachine hearingAidStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (hearingAidStateMachine != null) {
                return hearingAidStateMachine;
            }
            if (this.mStateMachines.size() >= 10) {
                Log.e(TAG, "Maximum number of HearingAid state machines reached: 10");
                return null;
            }
            HearingAidStateMachine hearingAidStateMachineMake = HearingAidStateMachine.make(bluetoothDevice, this, this.mHearingAidNativeInterface, this.mStateMachinesThread.getLooper());
            this.mStateMachines.put(bluetoothDevice, hearingAidStateMachineMake);
            return hearingAidStateMachineMake;
        }
    }

    private void reportActiveDevice(BluetoothDevice bluetoothDevice) {
        Intent intent = new Intent("android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.addFlags(83886080);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
        if (bluetoothDevice == null) {
            this.mAudioManager.setHearingAidDeviceConnectionState(this.mPreviousAudioDevice, 0);
            this.mPreviousAudioDevice = null;
        } else {
            if (this.mPreviousAudioDevice != null) {
                this.mAudioManager.setHearingAidDeviceConnectionState(this.mPreviousAudioDevice, 0);
            }
            this.mAudioManager.setHearingAidDeviceConnectionState(bluetoothDevice, 2);
            this.mPreviousAudioDevice = bluetoothDevice;
        }
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
            HearingAidService.this.bondStateChanged(bluetoothDevice, intExtra);
        }
    }

    @VisibleForTesting
    void bondStateChanged(BluetoothDevice bluetoothDevice, int i) {
        if (i != 10) {
            return;
        }
        synchronized (this.mStateMachines) {
            HearingAidStateMachine hearingAidStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (hearingAidStateMachine == null) {
                return;
            }
            if (hearingAidStateMachine.getConnectionState() != 0) {
                return;
            }
            removeStateMachine(bluetoothDevice);
        }
    }

    private void removeStateMachine(BluetoothDevice bluetoothDevice) {
        synchronized (this.mStateMachines) {
            HearingAidStateMachine hearingAidStateMachine = this.mStateMachines.get(bluetoothDevice);
            if (hearingAidStateMachine == null) {
                Log.w(TAG, "removeStateMachine: device " + bluetoothDevice + " does not have a state machine");
                return;
            }
            Log.i(TAG, "removeStateMachine: removing state machine for device: " + bluetoothDevice);
            hearingAidStateMachine.doQuit();
            hearingAidStateMachine.cleanup();
            this.mStateMachines.remove(bluetoothDevice);
        }
    }

    private List<BluetoothDevice> getConnectedPeerDevices(long j) {
        ArrayList arrayList = new ArrayList();
        for (BluetoothDevice bluetoothDevice : getConnectedDevices()) {
            if (getHiSyncId(bluetoothDevice) == j) {
                arrayList.add(bluetoothDevice);
            }
        }
        return arrayList;
    }

    @VisibleForTesting
    synchronized void connectionStateChanged(BluetoothDevice bluetoothDevice, int i, int i2) {
        if (bluetoothDevice == null || i == i2) {
            Log.e(TAG, "connectionStateChanged: unexpected invocation. device=" + bluetoothDevice + " fromState=" + i + " toState=" + i2);
            return;
        }
        if (i2 == 2) {
            long hiSyncId = getHiSyncId(bluetoothDevice);
            if (hiSyncId == 0 || getConnectedPeerDevices(hiSyncId).size() == 1) {
                MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.HEARING_AID);
            }
            setActiveDevice(bluetoothDevice);
        }
        if (i == 2 && getConnectedDevices().isEmpty()) {
            setActiveDevice(null);
        }
        if (i2 == 0 && this.mAdapterService.getBondState(bluetoothDevice) == 10) {
            removeStateMachine(bluetoothDevice);
        }
    }

    private class ConnectionStateChangedReceiver extends BroadcastReceiver {
        private ConnectionStateChangedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED".equals(intent.getAction())) {
                return;
            }
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
            HearingAidService.this.connectionStateChanged(bluetoothDevice, intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1), intExtra);
        }
    }

    @VisibleForTesting
    static class BluetoothHearingAidBinder extends IBluetoothHearingAid.Stub implements ProfileService.IProfileServiceBinder {
        private HearingAidService mService;

        private HearingAidService getService() {
            if (!Utils.checkCaller()) {
                Log.w(HearingAidService.TAG, "HearingAid call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        BluetoothHearingAidBinder(HearingAidService hearingAidService) {
            this.mService = hearingAidService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            HearingAidService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            HearingAidService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            HearingAidService service = getService();
            if (service == null) {
                return new ArrayList();
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            HearingAidService service = getService();
            if (service == null) {
                return new ArrayList();
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            HearingAidService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public boolean setActiveDevice(BluetoothDevice bluetoothDevice) {
            HearingAidService service = getService();
            if (service == null) {
                return false;
            }
            return service.setActiveDevice(bluetoothDevice);
        }

        public List<BluetoothDevice> getActiveDevices() {
            HearingAidService service = getService();
            if (service == null) {
                return new ArrayList();
            }
            return service.getActiveDevices();
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            HearingAidService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            HearingAidService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }

        public void setVolume(int i) {
            HearingAidService service = getService();
            if (service == null) {
                return;
            }
            service.setVolume(i);
        }

        public void adjustVolume(int i) {
        }

        public int getVolume() {
            return 0;
        }

        public long getHiSyncId(BluetoothDevice bluetoothDevice) {
            HearingAidService service = getService();
            if (service == null) {
                return 0L;
            }
            return service.getHiSyncId(bluetoothDevice);
        }

        public int getDeviceSide(BluetoothDevice bluetoothDevice) {
            HearingAidService service = getService();
            if (service == null) {
                return 1;
            }
            return service.getCapabilities(bluetoothDevice) & 1;
        }

        public int getDeviceMode(BluetoothDevice bluetoothDevice) {
            HearingAidService service = getService();
            if (service == null) {
                return 1;
            }
            return (service.getCapabilities(bluetoothDevice) >> 1) & 1;
        }
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        Iterator<HearingAidStateMachine> it = this.mStateMachines.values().iterator();
        while (it.hasNext()) {
            it.next().dump(sb);
        }
    }
}
