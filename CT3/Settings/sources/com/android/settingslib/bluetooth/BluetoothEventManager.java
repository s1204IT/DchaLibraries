package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.android.settingslib.R$string;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class BluetoothEventManager {
    private Context mContext;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private final LocalBluetoothAdapter mLocalAdapter;
    private LocalBluetoothProfileManager mProfileManager;
    private android.os.Handler mReceiverHandler;
    private final Collection<BluetoothCallback> mCallbacks = new ArrayList();
    final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("BluetoothEventManager", "Received " + intent.getAction());
            String action = intent.getAction();
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            Handler handler = (Handler) BluetoothEventManager.this.mHandlerMap.get(action);
            if (handler == null) {
                return;
            }
            handler.onReceive(context, intent, device);
        }
    };
    private final IntentFilter mAdapterIntentFilter = new IntentFilter();
    private final IntentFilter mProfileIntentFilter = new IntentFilter();
    private final Map<String, Handler> mHandlerMap = new HashMap();

    interface Handler {
        void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice);
    }

    private void addHandler(String action, Handler handler) {
        this.mHandlerMap.put(action, handler);
        this.mAdapterIntentFilter.addAction(action);
    }

    void addProfileHandler(String action, Handler handler) {
        this.mHandlerMap.put(action, handler);
        this.mProfileIntentFilter.addAction(action);
    }

    void setProfileManager(LocalBluetoothProfileManager manager) {
        this.mProfileManager = manager;
    }

    BluetoothEventManager(LocalBluetoothAdapter localBluetoothAdapter, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, Context context) {
        this.mLocalAdapter = localBluetoothAdapter;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mContext = context;
        addHandler("android.bluetooth.adapter.action.STATE_CHANGED", new AdapterStateChangedHandler(this, null));
        addHandler("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED", new ConnectionStateChangedHandler(this, 0 == true ? 1 : 0));
        addHandler("android.bluetooth.adapter.action.DISCOVERY_STARTED", new ScanningStateChangedHandler(true));
        addHandler("android.bluetooth.adapter.action.DISCOVERY_FINISHED", new ScanningStateChangedHandler(false));
        addHandler("android.bluetooth.device.action.FOUND", new DeviceFoundHandler(this, 0 == true ? 1 : 0));
        addHandler("android.bluetooth.device.action.DISAPPEARED", new DeviceDisappearedHandler(this, 0 == true ? 1 : 0));
        addHandler("android.bluetooth.device.action.NAME_CHANGED", new NameChangedHandler(this, 0 == true ? 1 : 0));
        addHandler("android.bluetooth.device.action.ALIAS_CHANGED", new NameChangedHandler(this, 0 == true ? 1 : 0));
        addHandler("android.bluetooth.device.action.BOND_STATE_CHANGED", new BondStateChangedHandler(this, 0 == true ? 1 : 0));
        addHandler("android.bluetooth.device.action.PAIRING_CANCEL", new PairingCancelHandler(this, 0 == true ? 1 : 0));
        addHandler("android.bluetooth.device.action.CLASS_CHANGED", new ClassChangedHandler(this, 0 == true ? 1 : 0));
        addHandler("android.bluetooth.device.action.UUID", new UuidChangedHandler(this, 0 == true ? 1 : 0));
        addHandler("android.intent.action.DOCK_EVENT", new DockEventHandler(this, 0 == true ? 1 : 0));
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mAdapterIntentFilter, null, this.mReceiverHandler);
    }

    void registerProfileIntentReceiver() {
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mProfileIntentFilter, null, this.mReceiverHandler);
    }

    public void registerCallback(BluetoothCallback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(BluetoothCallback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(callback);
        }
    }

    private class AdapterStateChangedHandler implements Handler {
        AdapterStateChangedHandler(BluetoothEventManager this$0, AdapterStateChangedHandler adapterStateChangedHandler) {
            this();
        }

        private AdapterStateChangedHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
            BluetoothEventManager.this.mLocalAdapter.setBluetoothStateInt(state);
            synchronized (BluetoothEventManager.this.mCallbacks) {
                for (BluetoothCallback callback : BluetoothEventManager.this.mCallbacks) {
                    callback.onBluetoothStateChanged(state);
                }
            }
            BluetoothEventManager.this.mDeviceManager.onBluetoothStateChanged(state);
        }
    }

    private class ScanningStateChangedHandler implements Handler {
        private final boolean mStarted;

        ScanningStateChangedHandler(boolean started) {
            this.mStarted = started;
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            synchronized (BluetoothEventManager.this.mCallbacks) {
                for (BluetoothCallback callback : BluetoothEventManager.this.mCallbacks) {
                    callback.onScanningStateChanged(this.mStarted);
                }
            }
            Log.d("BluetoothEventManager", "scanning state change to " + this.mStarted);
            BluetoothEventManager.this.mDeviceManager.onScanningStateChanged(this.mStarted);
        }
    }

    private class DeviceFoundHandler implements Handler {
        DeviceFoundHandler(BluetoothEventManager this$0, DeviceFoundHandler deviceFoundHandler) {
            this();
        }

        private DeviceFoundHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            short rssi = intent.getShortExtra("android.bluetooth.device.extra.RSSI", Short.MIN_VALUE);
            BluetoothClass btClass = (BluetoothClass) intent.getParcelableExtra("android.bluetooth.device.extra.CLASS");
            String name = intent.getStringExtra("android.bluetooth.device.extra.NAME");
            Log.d("BluetoothEventManager", "Device " + name + " ,Class: " + (btClass != null ? Integer.valueOf(btClass.getMajorDeviceClass()) : null));
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                cachedDevice = BluetoothEventManager.this.mDeviceManager.addDevice(BluetoothEventManager.this.mLocalAdapter, BluetoothEventManager.this.mProfileManager, device);
                Log.d("BluetoothEventManager", "DeviceFoundHandler created new CachedBluetoothDevice: " + cachedDevice);
            }
            cachedDevice.setRssi(rssi);
            cachedDevice.setBtClass(btClass);
            cachedDevice.setNewName(name);
            cachedDevice.setVisible(true);
        }
    }

    private class ConnectionStateChangedHandler implements Handler {
        ConnectionStateChangedHandler(BluetoothEventManager this$0, ConnectionStateChangedHandler connectionStateChangedHandler) {
            this();
        }

        private ConnectionStateChangedHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            int state = intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", Integer.MIN_VALUE);
            BluetoothEventManager.this.dispatchConnectionStateChanged(cachedDevice, state);
        }
    }

    public void dispatchConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        synchronized (this.mCallbacks) {
            for (BluetoothCallback callback : this.mCallbacks) {
                callback.onConnectionStateChanged(cachedDevice, state);
            }
        }
    }

    void dispatchDeviceAdded(CachedBluetoothDevice cachedDevice) {
        synchronized (this.mCallbacks) {
            for (BluetoothCallback callback : this.mCallbacks) {
                callback.onDeviceAdded(cachedDevice);
            }
        }
    }

    private class DeviceDisappearedHandler implements Handler {
        DeviceDisappearedHandler(BluetoothEventManager this$0, DeviceDisappearedHandler deviceDisappearedHandler) {
            this();
        }

        private DeviceDisappearedHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w("BluetoothEventManager", "received ACTION_DISAPPEARED for an unknown device: " + device);
                return;
            }
            if (!CachedBluetoothDeviceManager.onDeviceDisappeared(cachedDevice)) {
                return;
            }
            synchronized (BluetoothEventManager.this.mCallbacks) {
                for (BluetoothCallback callback : BluetoothEventManager.this.mCallbacks) {
                    callback.onDeviceDeleted(cachedDevice);
                }
            }
        }
    }

    private class NameChangedHandler implements Handler {
        NameChangedHandler(BluetoothEventManager this$0, NameChangedHandler nameChangedHandler) {
            this();
        }

        private NameChangedHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            BluetoothEventManager.this.mDeviceManager.onDeviceNameUpdated(device);
        }
    }

    private class BondStateChangedHandler implements Handler {
        BondStateChangedHandler(BluetoothEventManager this$0, BondStateChangedHandler bondStateChangedHandler) {
            this();
        }

        private BondStateChangedHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            if (device == null) {
                Log.e("BluetoothEventManager", "ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
                return;
            }
            int bondState = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", Integer.MIN_VALUE);
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w("BluetoothEventManager", "CachedBluetoothDevice for device " + device + " not found, calling readPairedDevices().");
                if (!BluetoothEventManager.this.readPairedDevices()) {
                    Log.e("BluetoothEventManager", "Got bonding state changed for " + device + ", but we have no record of that device.");
                    return;
                }
                cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
                if (cachedDevice == null) {
                    Log.e("BluetoothEventManager", "Got bonding state changed for " + device + ", but device not added in cache.");
                    return;
                }
            }
            synchronized (BluetoothEventManager.this.mCallbacks) {
                for (BluetoothCallback callback : BluetoothEventManager.this.mCallbacks) {
                    callback.onDeviceBondStateChanged(cachedDevice, bondState);
                }
            }
            cachedDevice.onBondingStateChanged(bondState);
            if (bondState != 10) {
                return;
            }
            int reason = intent.getIntExtra("android.bluetooth.device.extra.REASON", Integer.MIN_VALUE);
            Log.d("BluetoothEventManager", cachedDevice.getName() + " show unbond message for " + reason);
            showUnbondMessage(context, cachedDevice.getName(), reason);
        }

        private void showUnbondMessage(Context context, String name, int reason) {
            int errorMsg;
            switch (reason) {
                case DefaultWfcSettingsExt.PAUSE:
                    errorMsg = R$string.bluetooth_pairing_pin_error_message;
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    errorMsg = R$string.bluetooth_pairing_rejected_error_message;
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                default:
                    Log.w("BluetoothEventManager", "showUnbondMessage: Not displaying any message for reason: " + reason);
                    return;
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    errorMsg = R$string.bluetooth_pairing_device_down_error_message;
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                    errorMsg = R$string.bluetooth_pairing_error_message;
                    break;
            }
            Utils.showError(context, name, errorMsg);
        }
    }

    private class ClassChangedHandler implements Handler {
        ClassChangedHandler(BluetoothEventManager this$0, ClassChangedHandler classChangedHandler) {
            this();
        }

        private ClassChangedHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            BluetoothEventManager.this.mDeviceManager.onBtClassChanged(device);
        }
    }

    private class UuidChangedHandler implements Handler {
        UuidChangedHandler(BluetoothEventManager this$0, UuidChangedHandler uuidChangedHandler) {
            this();
        }

        private UuidChangedHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            BluetoothEventManager.this.mDeviceManager.onUuidChanged(device);
        }
    }

    private class PairingCancelHandler implements Handler {
        PairingCancelHandler(BluetoothEventManager this$0, PairingCancelHandler pairingCancelHandler) {
            this();
        }

        private PairingCancelHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            if (device == null) {
                Log.e("BluetoothEventManager", "ACTION_PAIRING_CANCEL with no EXTRA_DEVICE");
                return;
            }
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.e("BluetoothEventManager", "ACTION_PAIRING_CANCEL with no cached device");
                return;
            }
            int errorMsg = R$string.bluetooth_pairing_error_message;
            if (context == null || cachedDevice == null) {
                return;
            }
            Utils.showError(context, cachedDevice.getName(), errorMsg);
        }
    }

    private class DockEventHandler implements Handler {
        DockEventHandler(BluetoothEventManager this$0, DockEventHandler dockEventHandler) {
            this();
        }

        private DockEventHandler() {
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice;
            int state = intent.getIntExtra("android.intent.extra.DOCK_STATE", 1);
            if (state != 0 || device == null || device.getBondState() != 10 || (cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device)) == null) {
                return;
            }
            cachedDevice.setVisible(false);
        }
    }

    boolean readPairedDevices() {
        Set<BluetoothDevice> bondedDevices = this.mLocalAdapter.getBondedDevices();
        if (bondedDevices == null) {
            return false;
        }
        boolean deviceAdded = false;
        for (BluetoothDevice device : bondedDevices) {
            CachedBluetoothDevice cachedDevice = this.mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                CachedBluetoothDevice cachedDevice2 = this.mDeviceManager.addDevice(this.mLocalAdapter, this.mProfileManager, device);
                dispatchDeviceAdded(cachedDevice2);
                deviceAdded = true;
            }
        }
        return deviceAdded;
    }
}
