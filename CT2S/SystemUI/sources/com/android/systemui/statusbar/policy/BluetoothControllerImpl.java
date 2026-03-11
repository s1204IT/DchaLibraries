package com.android.systemui.statusbar.policy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.BluetoothUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;

public class BluetoothControllerImpl implements BluetoothController {
    private final BluetoothAdapter mAdapter;
    private boolean mConnecting;
    private final Context mContext;
    private boolean mEnabled;
    private final H mHandler;
    private BluetoothDevice mLastDevice;
    private static final boolean DEBUG = Log.isLoggable("BluetoothController", 3);
    private static final int[] CONNECTION_STATES = {0, 3, 1, 2};
    private final ArrayList<BluetoothController.Callback> mCallbacks = new ArrayList<>();
    private final Receiver mReceiver = new Receiver();
    private final ArrayMap<BluetoothDevice, DeviceInfo> mDeviceInfo = new ArrayMap<>();
    private final SparseArray<BluetoothProfile> mProfiles = new SparseArray<>();
    private final BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceDisconnected(int profile) {
            if (BluetoothControllerImpl.DEBUG) {
                Log.d("BluetoothController", "Disconnected from " + BluetoothUtil.profileToString(profile));
            }
            BluetoothControllerImpl.this.mHandler.removeMessages(1);
            BluetoothControllerImpl.this.mHandler.removeMessages(2);
            BluetoothControllerImpl.this.mHandler.obtainMessage(5, profile, 0).sendToTarget();
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (BluetoothControllerImpl.DEBUG) {
                Log.d("BluetoothController", "Connected to " + BluetoothUtil.profileToString(profile));
            }
            BluetoothControllerImpl.this.mHandler.obtainMessage(4, profile, 0, proxy).sendToTarget();
        }
    };

    public BluetoothControllerImpl(Context context, Looper bgLooper) {
        this.mContext = context;
        this.mHandler = new H(bgLooper);
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService("bluetooth");
        this.mAdapter = bluetoothManager.getAdapter();
        if (this.mAdapter == null) {
            Log.w("BluetoothController", "Default BT adapter not found");
            return;
        }
        this.mReceiver.register();
        setAdapterState(this.mAdapter.getState());
        updateBondedDevices();
        bindAllProfiles();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BluetoothController state:");
        pw.print("  mAdapter=");
        pw.println(this.mAdapter);
        pw.print("  mEnabled=");
        pw.println(this.mEnabled);
        pw.print("  mConnecting=");
        pw.println(this.mConnecting);
        pw.print("  mLastDevice=");
        pw.println(this.mLastDevice);
        pw.print("  mCallbacks.size=");
        pw.println(this.mCallbacks.size());
        pw.print("  mProfiles=");
        pw.println(profilesToString(this.mProfiles));
        pw.print("  mDeviceInfo.size=");
        pw.println(this.mDeviceInfo.size());
        for (int i = 0; i < this.mDeviceInfo.size(); i++) {
            BluetoothDevice device = this.mDeviceInfo.keyAt(i);
            DeviceInfo info = this.mDeviceInfo.valueAt(i);
            pw.print("    ");
            pw.print(BluetoothUtil.deviceToString(device));
            pw.print('(');
            pw.print(BluetoothUtil.uuidsToString(device));
            pw.print(')');
            pw.print("    ");
            pw.println(infoToString(info));
        }
    }

    private static String infoToString(DeviceInfo info) {
        if (info == null) {
            return null;
        }
        return "connectionState=" + BluetoothUtil.connectionStateToString(CONNECTION_STATES[info.connectionStateIndex]) + ",bonded=" + info.bonded + ",profiles=" + profilesToString(info.connectedProfiles);
    }

    private static String profilesToString(SparseArray<?> profiles) {
        int N = profiles.size();
        StringBuffer buffer = new StringBuffer();
        buffer.append('[');
        for (int i = 0; i < N; i++) {
            if (i != 0) {
                buffer.append(',');
            }
            buffer.append(BluetoothUtil.profileToString(profiles.keyAt(i)));
        }
        buffer.append(']');
        return buffer.toString();
    }

    @Override
    public void addStateChangedCallback(BluetoothController.Callback cb) {
        this.mCallbacks.add(cb);
        fireStateChange(cb);
    }

    @Override
    public void removeStateChangedCallback(BluetoothController.Callback cb) {
        this.mCallbacks.remove(cb);
    }

    @Override
    public boolean isBluetoothEnabled() {
        return this.mAdapter != null && this.mAdapter.isEnabled();
    }

    @Override
    public boolean isBluetoothConnected() {
        return this.mAdapter != null && this.mAdapter.getConnectionState() == 2;
    }

    @Override
    public boolean isBluetoothConnecting() {
        return this.mAdapter != null && this.mAdapter.getConnectionState() == 1;
    }

    @Override
    public void setBluetoothEnabled(boolean enabled) {
        if (this.mAdapter != null) {
            if (enabled) {
                this.mAdapter.enable();
            } else {
                this.mAdapter.disable();
            }
        }
    }

    @Override
    public boolean isBluetoothSupported() {
        return this.mAdapter != null;
    }

    @Override
    public ArraySet<BluetoothController.PairedDevice> getPairedDevices() {
        ArraySet<BluetoothController.PairedDevice> rt = new ArraySet<>();
        for (int i = 0; i < this.mDeviceInfo.size(); i++) {
            BluetoothDevice device = this.mDeviceInfo.keyAt(i);
            DeviceInfo info = this.mDeviceInfo.valueAt(i);
            if (info.bonded) {
                BluetoothController.PairedDevice paired = new BluetoothController.PairedDevice();
                paired.id = device.getAddress();
                paired.tag = device;
                paired.name = device.getAliasName();
                paired.state = connectionStateToPairedDeviceState(info.connectionStateIndex);
                rt.add(paired);
            }
        }
        return rt;
    }

    private static int connectionStateToPairedDeviceState(int index) {
        int state = CONNECTION_STATES[index];
        return state == 2 ? BluetoothController.PairedDevice.STATE_CONNECTED : state == 1 ? BluetoothController.PairedDevice.STATE_CONNECTING : state == 3 ? BluetoothController.PairedDevice.STATE_DISCONNECTING : BluetoothController.PairedDevice.STATE_DISCONNECTED;
    }

    @Override
    public void connect(BluetoothController.PairedDevice pd) {
        connect(pd, true);
    }

    @Override
    public void disconnect(BluetoothController.PairedDevice pd) {
        connect(pd, false);
    }

    private void connect(BluetoothController.PairedDevice pd, boolean connect) {
        if (this.mAdapter != null && pd != null && pd.tag != null) {
            BluetoothDevice device = (BluetoothDevice) pd.tag;
            DeviceInfo info = this.mDeviceInfo.get(device);
            String action = connect ? "connect" : "disconnect";
            if (DEBUG) {
                Log.d("BluetoothController", action + " " + BluetoothUtil.deviceToString(device));
            }
            ParcelUuid[] uuids = device.getUuids();
            if (uuids == null) {
                Log.w("BluetoothController", "No uuids returned, aborting " + action + " for " + BluetoothUtil.deviceToString(device));
                return;
            }
            SparseArray<Boolean> profiles = new SparseArray<>();
            if (connect) {
                for (ParcelUuid uuid : uuids) {
                    int profile = BluetoothUtil.uuidToProfile(uuid);
                    if (profile == 0) {
                        Log.w("BluetoothController", "Device " + BluetoothUtil.deviceToString(device) + " has an unsupported uuid: " + BluetoothUtil.uuidToString(uuid));
                    } else {
                        boolean connected = info.connectedProfiles.get(profile, false).booleanValue();
                        if (!connected) {
                            profiles.put(profile, true);
                        }
                    }
                }
            } else {
                profiles = info.connectedProfiles;
            }
            for (int i = 0; i < profiles.size(); i++) {
                int profile2 = profiles.keyAt(i);
                if (this.mProfiles.indexOfKey(profile2) >= 0) {
                    BluetoothUtil.Profile p = BluetoothUtil.getProfile(this.mProfiles.get(profile2));
                    if (p == null) {
                        Log.w("BluetoothController", "profile is empty: " + BluetoothUtil.profileToString(profile2));
                    } else {
                        boolean ok = connect ? p.connect(device) : p.disconnect(device);
                        if (DEBUG) {
                            Log.d("BluetoothController", action + " " + BluetoothUtil.profileToString(profile2) + " " + (ok ? "succeeded" : "failed"));
                        }
                    }
                } else {
                    Log.w("BluetoothController", "Unable get get Profile for " + BluetoothUtil.profileToString(profile2));
                }
            }
        }
    }

    @Override
    public String getLastDeviceName() {
        if (this.mLastDevice != null) {
            return this.mLastDevice.getAliasName();
        }
        return null;
    }

    public void updateBondedDevices() {
        this.mHandler.removeMessages(3);
        this.mHandler.sendEmptyMessage(3);
    }

    private void updateConnectionStates() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessage(1);
    }

    public void updateConnectionState(BluetoothDevice device, int profile, int state) {
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.obtainMessage(2, profile, state, device).sendToTarget();
        }
    }

    public void handleUpdateBondedDevices() {
        if (this.mAdapter != null) {
            Set<BluetoothDevice> bondedDevices = this.mAdapter.getBondedDevices();
            for (DeviceInfo info : this.mDeviceInfo.values()) {
                info.bonded = false;
            }
            int bondedCount = 0;
            BluetoothDevice lastBonded = null;
            if (bondedDevices != null) {
                for (BluetoothDevice bondedDevice : bondedDevices) {
                    boolean bonded = bondedDevice.getBondState() != 10;
                    updateInfo(bondedDevice).bonded = bonded;
                    if (bonded) {
                        bondedCount++;
                        lastBonded = bondedDevice;
                    }
                }
            }
            if (this.mLastDevice == null && bondedCount == 1) {
                this.mLastDevice = lastBonded;
            }
            updateConnectionStates();
            firePairedDevicesChanged();
        }
    }

    public void handleUpdateConnectionStates() {
        int N = this.mDeviceInfo.size();
        for (int i = 0; i < N; i++) {
            BluetoothDevice device = this.mDeviceInfo.keyAt(i);
            DeviceInfo info = updateInfo(device);
            info.connectionStateIndex = 0;
            info.connectedProfiles.clear();
            for (int j = 0; j < this.mProfiles.size(); j++) {
                int state = this.mProfiles.valueAt(j).getConnectionState(device);
                handleUpdateConnectionState(device, this.mProfiles.keyAt(j), state);
            }
        }
        handleConnectionChange();
        firePairedDevicesChanged();
    }

    public void handleUpdateConnectionState(BluetoothDevice device, int profile, int state) {
        if (DEBUG) {
            Log.d("BluetoothController", "updateConnectionState " + BluetoothUtil.deviceToString(device) + " " + BluetoothUtil.profileToString(profile) + " " + BluetoothUtil.connectionStateToString(state));
        }
        DeviceInfo info = updateInfo(device);
        int stateIndex = 0;
        int i = 0;
        while (true) {
            if (i >= CONNECTION_STATES.length) {
                break;
            }
            if (CONNECTION_STATES[i] != state) {
                i++;
            } else {
                stateIndex = i;
                break;
            }
        }
        info.profileStates.put(profile, Integer.valueOf(stateIndex));
        info.connectionStateIndex = 0;
        int N = info.profileStates.size();
        for (int i2 = 0; i2 < N; i2++) {
            if (info.profileStates.valueAt(i2).intValue() > info.connectionStateIndex) {
                info.connectionStateIndex = info.profileStates.valueAt(i2).intValue();
            }
        }
        if (state == 2) {
            info.connectedProfiles.put(profile, true);
        } else {
            info.connectedProfiles.remove(profile);
        }
    }

    public void handleConnectionChange() {
        if (this.mLastDevice != null && CONNECTION_STATES[this.mDeviceInfo.get(this.mLastDevice).connectionStateIndex] != 2) {
            this.mLastDevice = null;
            int size = this.mDeviceInfo.size();
            for (int i = 0; i < size; i++) {
                BluetoothDevice device = this.mDeviceInfo.keyAt(i);
                DeviceInfo info = this.mDeviceInfo.valueAt(i);
                if (CONNECTION_STATES[info.connectionStateIndex] == 2) {
                    this.mLastDevice = device;
                    return;
                }
            }
        }
    }

    private void bindAllProfiles() {
        this.mAdapter.getProfileProxy(this.mContext, this.mProfileListener, 2);
        this.mAdapter.getProfileProxy(this.mContext, this.mProfileListener, 10);
        this.mAdapter.getProfileProxy(this.mContext, this.mProfileListener, 11);
        this.mAdapter.getProfileProxy(this.mContext, this.mProfileListener, 1);
        this.mAdapter.getProfileProxy(this.mContext, this.mProfileListener, 16);
        this.mAdapter.getProfileProxy(this.mContext, this.mProfileListener, 4);
        this.mAdapter.getProfileProxy(this.mContext, this.mProfileListener, 9);
        this.mAdapter.getProfileProxy(this.mContext, this.mProfileListener, 5);
    }

    public void firePairedDevicesChanged() {
        for (BluetoothController.Callback cb : this.mCallbacks) {
            cb.onBluetoothPairedDevicesChanged();
        }
    }

    public void setAdapterState(int adapterState) {
        boolean enabled = adapterState == 12;
        if (this.mEnabled != enabled) {
            this.mEnabled = enabled;
            fireStateChange();
        }
    }

    public void setConnecting(boolean connecting) {
        if (this.mConnecting != connecting) {
            this.mConnecting = connecting;
            fireStateChange();
        }
    }

    private void fireStateChange() {
        for (BluetoothController.Callback cb : this.mCallbacks) {
            fireStateChange(cb);
        }
    }

    private void fireStateChange(BluetoothController.Callback cb) {
        cb.onBluetoothStateChange(this.mEnabled, this.mConnecting);
    }

    public static int getProfileFromAction(String action) {
        if ("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            return 2;
        }
        if ("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            return 1;
        }
        if ("android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            return 10;
        }
        if ("android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            return 16;
        }
        if ("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            return 4;
        }
        if ("android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            return 9;
        }
        if ("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            return 5;
        }
        if (DEBUG) {
            Log.d("BluetoothController", "Unknown action " + action);
        }
        return -1;
    }

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        public void register() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
            filter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
            filter.addAction("android.bluetooth.device.action.ALIAS_CHANGED");
            filter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED");
            filter.addAction("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
            BluetoothControllerImpl.this.mContext.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                BluetoothControllerImpl.this.setAdapterState(intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE));
                BluetoothControllerImpl.this.updateBondedDevices();
                if (BluetoothControllerImpl.DEBUG) {
                    Log.d("BluetoothController", "ACTION_STATE_CHANGED " + BluetoothControllerImpl.this.mEnabled);
                    return;
                }
                return;
            }
            if (action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                BluetoothControllerImpl.this.updateInfo(device);
                int state = intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", Integer.MIN_VALUE);
                BluetoothControllerImpl.this.mLastDevice = device;
                if (BluetoothControllerImpl.DEBUG) {
                    Log.d("BluetoothController", "ACTION_CONNECTION_STATE_CHANGED " + BluetoothUtil.connectionStateToString(state) + " " + BluetoothUtil.deviceToString(device));
                }
                BluetoothControllerImpl.this.setConnecting(state == 1);
                return;
            }
            if (action.equals("android.bluetooth.device.action.ALIAS_CHANGED")) {
                BluetoothControllerImpl.this.updateInfo(device);
                BluetoothControllerImpl.this.mLastDevice = device;
                return;
            }
            if (action.equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                if (BluetoothControllerImpl.DEBUG) {
                    Log.d("BluetoothController", "ACTION_BOND_STATE_CHANGED " + device);
                }
                BluetoothControllerImpl.this.updateBondedDevices();
                return;
            }
            int profile = BluetoothControllerImpl.getProfileFromAction(intent.getAction());
            int state2 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
            if (BluetoothControllerImpl.DEBUG) {
                Log.d("BluetoothController", "ACTION_CONNECTION_STATE_CHANGE " + BluetoothUtil.profileToString(profile) + " " + BluetoothUtil.connectionStateToString(state2));
            }
            if (profile != -1 && state2 != -1) {
                BluetoothControllerImpl.this.updateConnectionState(device, profile, state2);
            }
        }
    }

    public DeviceInfo updateInfo(BluetoothDevice device) {
        DeviceInfo info = this.mDeviceInfo.get(device);
        if (info == null) {
            info = new DeviceInfo();
        }
        this.mDeviceInfo.put(device, info);
        return info;
    }

    private class H extends Handler {
        public H(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    BluetoothControllerImpl.this.handleUpdateConnectionStates();
                    BluetoothControllerImpl.this.firePairedDevicesChanged();
                    break;
                case 2:
                    BluetoothControllerImpl.this.handleUpdateConnectionState((BluetoothDevice) msg.obj, msg.arg1, msg.arg2);
                    BluetoothControllerImpl.this.handleConnectionChange();
                    BluetoothControllerImpl.this.firePairedDevicesChanged();
                    break;
                case 3:
                    BluetoothControllerImpl.this.handleUpdateBondedDevices();
                    BluetoothControllerImpl.this.firePairedDevicesChanged();
                    break;
                case 4:
                    BluetoothControllerImpl.this.mProfiles.put(msg.arg1, (BluetoothProfile) msg.obj);
                    BluetoothControllerImpl.this.handleUpdateConnectionStates();
                    BluetoothControllerImpl.this.firePairedDevicesChanged();
                    break;
                case 5:
                    BluetoothControllerImpl.this.mProfiles.remove(msg.arg1);
                    BluetoothControllerImpl.this.handleUpdateConnectionStates();
                    BluetoothControllerImpl.this.firePairedDevicesChanged();
                    break;
            }
        }
    }

    private static class DeviceInfo {
        boolean bonded;
        SparseArray<Boolean> connectedProfiles;
        int connectionStateIndex;
        SparseArray<Integer> profileStates;

        private DeviceInfo() {
            this.connectionStateIndex = 0;
            this.connectedProfiles = new SparseArray<>();
            this.profileStates = new SparseArray<>();
        }
    }
}
