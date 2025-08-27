package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.android.settingslib.R;
import com.android.settingslib.wifi.AccessPoint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/* loaded from: classes.dex */
public class BluetoothEventManager {
    private Context mContext;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private final LocalBluetoothAdapter mLocalAdapter;
    private LocalBluetoothProfileManager mProfileManager;
    private android.os.Handler mReceiverHandler;
    private final Collection<BluetoothCallback> mCallbacks = new ArrayList();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.settingslib.bluetooth.BluetoothEventManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            Handler handler = (Handler) BluetoothEventManager.this.mHandlerMap.get(action);
            if (handler != null) {
                handler.onReceive(context, intent, bluetoothDevice);
            }
        }
    };
    private final BroadcastReceiver mProfileBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.settingslib.bluetooth.BluetoothEventManager.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Log.v("BluetoothEventManager", "Received " + intent.getAction());
            String action = intent.getAction();
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            Handler handler = (Handler) BluetoothEventManager.this.mHandlerMap.get(action);
            if (handler != null) {
                handler.onReceive(context, intent, bluetoothDevice);
            }
        }
    };
    private final IntentFilter mAdapterIntentFilter = new IntentFilter();
    private final IntentFilter mProfileIntentFilter = new IntentFilter();
    private final Map<String, Handler> mHandlerMap = new HashMap();

    interface Handler {
        void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice);
    }

    private void addHandler(String str, Handler handler) {
        this.mHandlerMap.put(str, handler);
        this.mAdapterIntentFilter.addAction(str);
    }

    void addProfileHandler(String str, Handler handler) {
        this.mHandlerMap.put(str, handler);
        this.mProfileIntentFilter.addAction(str);
    }

    void setProfileManager(LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mProfileManager = localBluetoothProfileManager;
    }

    BluetoothEventManager(LocalBluetoothAdapter localBluetoothAdapter, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, Context context) {
        this.mLocalAdapter = localBluetoothAdapter;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mContext = context;
        addHandler("android.bluetooth.adapter.action.STATE_CHANGED", new AdapterStateChangedHandler());
        addHandler("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED", new ConnectionStateChangedHandler());
        addHandler("android.bluetooth.adapter.action.DISCOVERY_STARTED", new ScanningStateChangedHandler(true));
        addHandler("android.bluetooth.adapter.action.DISCOVERY_FINISHED", new ScanningStateChangedHandler(false));
        addHandler("android.bluetooth.device.action.FOUND", new DeviceFoundHandler());
        addHandler("android.bluetooth.device.action.DISAPPEARED", new DeviceDisappearedHandler());
        addHandler("android.bluetooth.device.action.NAME_CHANGED", new NameChangedHandler());
        addHandler("android.bluetooth.device.action.ALIAS_CHANGED", new NameChangedHandler());
        addHandler("android.bluetooth.device.action.BOND_STATE_CHANGED", new BondStateChangedHandler());
        addHandler("android.bluetooth.device.action.CLASS_CHANGED", new ClassChangedHandler());
        addHandler("android.bluetooth.device.action.UUID", new UuidChangedHandler());
        addHandler("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED", new BatteryLevelChangedHandler());
        addHandler("android.intent.action.DOCK_EVENT", new DockEventHandler());
        addHandler("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED", new ActiveDeviceChangedHandler());
        addHandler("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED", new ActiveDeviceChangedHandler());
        addHandler("android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED", new ActiveDeviceChangedHandler());
        addHandler("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED", new AudioModeChangedHandler());
        addHandler("android.intent.action.PHONE_STATE", new AudioModeChangedHandler());
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mAdapterIntentFilter, null, this.mReceiverHandler);
        this.mContext.registerReceiver(this.mProfileBroadcastReceiver, this.mProfileIntentFilter, null, this.mReceiverHandler);
    }

    void registerProfileIntentReceiver() {
        this.mContext.registerReceiver(this.mProfileBroadcastReceiver, this.mProfileIntentFilter, null, this.mReceiverHandler);
    }

    public void registerCallback(BluetoothCallback bluetoothCallback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(bluetoothCallback);
        }
    }

    public void unregisterCallback(BluetoothCallback bluetoothCallback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(bluetoothCallback);
        }
    }

    private class AdapterStateChangedHandler implements Handler {
        private AdapterStateChangedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", AccessPoint.UNREACHABLE_RSSI);
            if (intExtra == 10) {
                context.unregisterReceiver(BluetoothEventManager.this.mProfileBroadcastReceiver);
                BluetoothEventManager.this.registerProfileIntentReceiver();
            }
            BluetoothEventManager.this.mLocalAdapter.setBluetoothStateInt(intExtra);
            synchronized (BluetoothEventManager.this.mCallbacks) {
                Iterator it = BluetoothEventManager.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((BluetoothCallback) it.next()).onBluetoothStateChanged(intExtra);
                }
            }
            BluetoothEventManager.this.mDeviceManager.onBluetoothStateChanged(intExtra);
        }
    }

    private class ScanningStateChangedHandler implements Handler {
        private final boolean mStarted;

        ScanningStateChangedHandler(boolean z) {
            this.mStarted = z;
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            synchronized (BluetoothEventManager.this.mCallbacks) {
                Iterator it = BluetoothEventManager.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((BluetoothCallback) it.next()).onScanningStateChanged(this.mStarted);
                }
            }
            Log.d("BluetoothEventManager", "scanning state change to " + this.mStarted);
            BluetoothEventManager.this.mDeviceManager.onScanningStateChanged(this.mStarted);
        }
    }

    private class DeviceFoundHandler implements Handler {
        private DeviceFoundHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            short shortExtra = intent.getShortExtra("android.bluetooth.device.extra.RSSI", Short.MIN_VALUE);
            BluetoothClass bluetoothClass = (BluetoothClass) intent.getParcelableExtra("android.bluetooth.device.extra.CLASS");
            String stringExtra = intent.getStringExtra("android.bluetooth.device.extra.NAME");
            StringBuilder sb = new StringBuilder();
            sb.append("Device ");
            sb.append(stringExtra);
            sb.append(" ,Class: ");
            sb.append(bluetoothClass != null ? Integer.valueOf(bluetoothClass.getMajorDeviceClass()) : null);
            Log.d("BluetoothEventManager", sb.toString());
            CachedBluetoothDevice cachedBluetoothDeviceFindDevice = BluetoothEventManager.this.mDeviceManager.findDevice(bluetoothDevice);
            if (cachedBluetoothDeviceFindDevice == null) {
                cachedBluetoothDeviceFindDevice = BluetoothEventManager.this.mDeviceManager.addDevice(BluetoothEventManager.this.mLocalAdapter, BluetoothEventManager.this.mProfileManager, bluetoothDevice);
                Log.d("BluetoothEventManager", "DeviceFoundHandler created new CachedBluetoothDevice: " + cachedBluetoothDeviceFindDevice);
            }
            cachedBluetoothDeviceFindDevice.setRssi(shortExtra);
            cachedBluetoothDeviceFindDevice.setBtClass(bluetoothClass);
            cachedBluetoothDeviceFindDevice.setNewName(stringExtra);
            cachedBluetoothDeviceFindDevice.setJustDiscovered(true);
        }
    }

    private class ConnectionStateChangedHandler implements Handler {
        private ConnectionStateChangedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            BluetoothEventManager.this.dispatchConnectionStateChanged(BluetoothEventManager.this.mDeviceManager.findDevice(bluetoothDevice), intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", AccessPoint.UNREACHABLE_RSSI));
        }
    }

    private void dispatchConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        synchronized (this.mCallbacks) {
            Iterator<BluetoothCallback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onConnectionStateChanged(cachedBluetoothDevice, i);
            }
        }
    }

    void dispatchDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice) {
        synchronized (this.mCallbacks) {
            Iterator<BluetoothCallback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onDeviceAdded(cachedBluetoothDevice);
            }
        }
    }

    void dispatchDeviceRemoved(CachedBluetoothDevice cachedBluetoothDevice) {
        synchronized (this.mCallbacks) {
            Iterator<BluetoothCallback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onDeviceDeleted(cachedBluetoothDevice);
            }
        }
    }

    private class DeviceDisappearedHandler implements Handler {
        private DeviceDisappearedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            CachedBluetoothDevice cachedBluetoothDeviceFindDevice = BluetoothEventManager.this.mDeviceManager.findDevice(bluetoothDevice);
            if (cachedBluetoothDeviceFindDevice == null) {
                Log.w("BluetoothEventManager", "received ACTION_DISAPPEARED for an unknown device: " + bluetoothDevice);
                return;
            }
            if (CachedBluetoothDeviceManager.onDeviceDisappeared(cachedBluetoothDeviceFindDevice)) {
                synchronized (BluetoothEventManager.this.mCallbacks) {
                    Iterator it = BluetoothEventManager.this.mCallbacks.iterator();
                    while (it.hasNext()) {
                        ((BluetoothCallback) it.next()).onDeviceDeleted(cachedBluetoothDeviceFindDevice);
                    }
                }
            }
        }
    }

    private class NameChangedHandler implements Handler {
        private NameChangedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            BluetoothEventManager.this.mDeviceManager.onDeviceNameUpdated(bluetoothDevice);
        }
    }

    private class BondStateChangedHandler implements Handler {
        private BondStateChangedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            if (bluetoothDevice == null) {
                Log.e("BluetoothEventManager", "ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
                return;
            }
            int intExtra = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", AccessPoint.UNREACHABLE_RSSI);
            CachedBluetoothDevice cachedBluetoothDeviceFindDevice = BluetoothEventManager.this.mDeviceManager.findDevice(bluetoothDevice);
            if (cachedBluetoothDeviceFindDevice == null) {
                Log.w("BluetoothEventManager", "CachedBluetoothDevice for device " + bluetoothDevice + " not found, calling readPairedDevices().");
                if (BluetoothEventManager.this.readPairedDevices()) {
                    Log.e("BluetoothEventManager", "Got bonding state changed for " + bluetoothDevice + ", and we have record of that device.");
                    cachedBluetoothDeviceFindDevice = BluetoothEventManager.this.mDeviceManager.findDevice(bluetoothDevice);
                }
                if (cachedBluetoothDeviceFindDevice == null) {
                    Log.w("BluetoothEventManager", "Got bonding state changed for " + bluetoothDevice + ", but we have no record of that device.");
                    cachedBluetoothDeviceFindDevice = BluetoothEventManager.this.mDeviceManager.addDevice(BluetoothEventManager.this.mLocalAdapter, BluetoothEventManager.this.mProfileManager, bluetoothDevice);
                    BluetoothEventManager.this.dispatchDeviceAdded(cachedBluetoothDeviceFindDevice);
                }
            }
            synchronized (BluetoothEventManager.this.mCallbacks) {
                Iterator it = BluetoothEventManager.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((BluetoothCallback) it.next()).onDeviceBondStateChanged(cachedBluetoothDeviceFindDevice, intExtra);
                }
            }
            cachedBluetoothDeviceFindDevice.onBondingStateChanged(intExtra);
            if (intExtra == 10) {
                if (cachedBluetoothDeviceFindDevice.getHiSyncId() != 0) {
                    BluetoothEventManager.this.mDeviceManager.onDeviceUnpaired(cachedBluetoothDeviceFindDevice);
                }
                int intExtra2 = intent.getIntExtra("android.bluetooth.device.extra.REASON", AccessPoint.UNREACHABLE_RSSI);
                Log.d("BluetoothEventManager", cachedBluetoothDeviceFindDevice.getName() + " show unbond message for " + intExtra2);
                showUnbondMessage(context, cachedBluetoothDeviceFindDevice.getName(), intExtra2);
            }
        }

        private void showUnbondMessage(Context context, String str, int i) {
            int i2;
            switch (i) {
                case 1:
                    i2 = R.string.bluetooth_pairing_pin_error_message;
                    break;
                case 2:
                    i2 = R.string.bluetooth_pairing_rejected_error_message;
                    break;
                case 3:
                default:
                    Log.w("BluetoothEventManager", "showUnbondMessage: Not displaying any message for reason: " + i);
                    return;
                case 4:
                    i2 = R.string.bluetooth_pairing_device_down_error_message;
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                    i2 = R.string.bluetooth_pairing_error_message;
                    break;
            }
            Utils.showError(context, str, i2);
        }
    }

    private class ClassChangedHandler implements Handler {
        private ClassChangedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            BluetoothEventManager.this.mDeviceManager.onBtClassChanged(bluetoothDevice);
        }
    }

    private class UuidChangedHandler implements Handler {
        private UuidChangedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            BluetoothEventManager.this.mDeviceManager.onUuidChanged(bluetoothDevice);
        }
    }

    private class DockEventHandler implements Handler {
        private DockEventHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            CachedBluetoothDevice cachedBluetoothDeviceFindDevice;
            if (intent.getIntExtra("android.intent.extra.DOCK_STATE", 1) == 0 && bluetoothDevice != null && bluetoothDevice.getBondState() == 10 && (cachedBluetoothDeviceFindDevice = BluetoothEventManager.this.mDeviceManager.findDevice(bluetoothDevice)) != null) {
                cachedBluetoothDeviceFindDevice.setJustDiscovered(false);
            }
        }
    }

    private class BatteryLevelChangedHandler implements Handler {
        private BatteryLevelChangedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            CachedBluetoothDevice cachedBluetoothDeviceFindDevice = BluetoothEventManager.this.mDeviceManager.findDevice(bluetoothDevice);
            if (cachedBluetoothDeviceFindDevice != null) {
                cachedBluetoothDeviceFindDevice.refresh();
            }
        }
    }

    boolean readPairedDevices() {
        Set<BluetoothDevice> bondedDevices = this.mLocalAdapter.getBondedDevices();
        boolean z = false;
        if (bondedDevices == null) {
            return false;
        }
        for (BluetoothDevice bluetoothDevice : bondedDevices) {
            if (this.mDeviceManager.findDevice(bluetoothDevice) == null) {
                dispatchDeviceAdded(this.mDeviceManager.addDevice(this.mLocalAdapter, this.mProfileManager, bluetoothDevice));
                z = true;
            }
        }
        return z;
    }

    private class ActiveDeviceChangedHandler implements Handler {
        private ActiveDeviceChangedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            int i;
            String action = intent.getAction();
            if (action != null) {
                CachedBluetoothDevice cachedBluetoothDeviceFindDevice = BluetoothEventManager.this.mDeviceManager.findDevice(bluetoothDevice);
                if (Objects.equals(action, "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED")) {
                    i = 2;
                } else if (Objects.equals(action, "android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED")) {
                    i = 1;
                } else if (Objects.equals(action, "android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED")) {
                    i = 21;
                } else {
                    Log.w("BluetoothEventManager", "ActiveDeviceChangedHandler: unknown action " + action);
                    return;
                }
                BluetoothEventManager.this.dispatchActiveDeviceChanged(cachedBluetoothDeviceFindDevice, i);
                return;
            }
            Log.w("BluetoothEventManager", "ActiveDeviceChangedHandler: action is null");
        }
    }

    private void dispatchActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        this.mDeviceManager.onActiveDeviceChanged(cachedBluetoothDevice, i);
        synchronized (this.mCallbacks) {
            Iterator<BluetoothCallback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onActiveDeviceChanged(cachedBluetoothDevice, i);
            }
        }
    }

    private class AudioModeChangedHandler implements Handler {
        private AudioModeChangedHandler() {
        }

        @Override // com.android.settingslib.bluetooth.BluetoothEventManager.Handler
        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            if (intent.getAction() != null) {
                BluetoothEventManager.this.dispatchAudioModeChanged();
            } else {
                Log.w("BluetoothEventManager", "AudioModeChangedHandler() action is null");
            }
        }
    }

    private void dispatchAudioModeChanged() {
        this.mDeviceManager.dispatchAudioModeChanged();
        synchronized (this.mCallbacks) {
            Iterator<BluetoothCallback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onAudioModeChanged();
            }
        }
    }

    void dispatchProfileConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i, int i2) {
        synchronized (this.mCallbacks) {
            Iterator<BluetoothCallback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onProfileConnectionStateChanged(cachedBluetoothDevice, i, i2);
            }
        }
        this.mDeviceManager.onProfileConnectionStateChanged(cachedBluetoothDevice, i, i2);
    }
}
