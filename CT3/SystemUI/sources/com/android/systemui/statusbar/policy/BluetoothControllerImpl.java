package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.systemui.statusbar.policy.BluetoothController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

public class BluetoothControllerImpl implements BluetoothController, BluetoothCallback, CachedBluetoothDevice.Callback {
    private static final boolean DEBUG = Log.isLoggable("BluetoothController", 3);
    private final int mCurrentUser;
    private boolean mEnabled;
    private CachedBluetoothDevice mLastDevice;
    private final LocalBluetoothManager mLocalBluetoothManager;
    private final UserManager mUserManager;
    private int mConnectionState = 0;
    private final H mHandler = new H(this, null);

    public BluetoothControllerImpl(Context context, Looper bgLooper) {
        this.mLocalBluetoothManager = LocalBluetoothManager.getInstance(context, null);
        if (this.mLocalBluetoothManager != null) {
            this.mLocalBluetoothManager.getEventManager().setReceiverHandler(new Handler(bgLooper));
            this.mLocalBluetoothManager.getEventManager().registerCallback(this);
            onBluetoothStateChanged(this.mLocalBluetoothManager.getBluetoothAdapter().getBluetoothState());
        }
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mCurrentUser = ActivityManager.getCurrentUser();
    }

    @Override
    public boolean canConfigBluetooth() {
        return !this.mUserManager.hasUserRestriction("no_config_bluetooth", UserHandle.of(this.mCurrentUser));
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BluetoothController state:");
        pw.print("  mLocalBluetoothManager=");
        pw.println(this.mLocalBluetoothManager);
        if (this.mLocalBluetoothManager == null) {
            return;
        }
        pw.print("  mEnabled=");
        pw.println(this.mEnabled);
        pw.print("  mConnectionState=");
        pw.println(stateToString(this.mConnectionState));
        pw.print("  mLastDevice=");
        pw.println(this.mLastDevice);
        pw.print("  mCallbacks.size=");
        pw.println(this.mHandler.mCallbacks.size());
        pw.println("  Bluetooth Devices:");
        for (CachedBluetoothDevice device : this.mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy()) {
            pw.println("    " + getDeviceString(device));
        }
    }

    private static String stateToString(int state) {
        switch (state) {
            case 0:
                return "DISCONNECTED";
            case 1:
                return "CONNECTING";
            case 2:
                return "CONNECTED";
            case 3:
                return "DISCONNECTING";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

    private String getDeviceString(CachedBluetoothDevice device) {
        return device.getName() + " " + device.getBondState() + " " + device.isConnected();
    }

    @Override
    public void addStateChangedCallback(BluetoothController.Callback cb) {
        this.mHandler.obtainMessage(3, cb).sendToTarget();
        this.mHandler.sendEmptyMessage(2);
    }

    @Override
    public void removeStateChangedCallback(BluetoothController.Callback cb) {
        this.mHandler.obtainMessage(4, cb).sendToTarget();
    }

    @Override
    public boolean isBluetoothEnabled() {
        return this.mEnabled;
    }

    @Override
    public boolean isBluetoothConnected() {
        return this.mConnectionState == 2;
    }

    @Override
    public boolean isBluetoothConnecting() {
        return this.mConnectionState == 1;
    }

    @Override
    public void setBluetoothEnabled(boolean enabled) {
        if (this.mLocalBluetoothManager == null) {
            return;
        }
        this.mLocalBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(enabled);
    }

    @Override
    public boolean isBluetoothSupported() {
        return this.mLocalBluetoothManager != null;
    }

    @Override
    public void connect(CachedBluetoothDevice device) {
        if (this.mLocalBluetoothManager == null || device == null) {
            return;
        }
        device.connect(true);
    }

    @Override
    public void disconnect(CachedBluetoothDevice device) {
        if (this.mLocalBluetoothManager == null || device == null) {
            return;
        }
        device.disconnect();
    }

    @Override
    public String getLastDeviceName() {
        if (this.mLastDevice != null) {
            return this.mLastDevice.getName();
        }
        return null;
    }

    @Override
    public Collection<CachedBluetoothDevice> getDevices() {
        if (this.mLocalBluetoothManager != null) {
            return this.mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        }
        return null;
    }

    private void updateConnected() {
        int state = this.mLocalBluetoothManager.getBluetoothAdapter().getConnectionState();
        if (state != this.mConnectionState) {
            this.mConnectionState = state;
            this.mHandler.sendEmptyMessage(2);
        }
        if (this.mLastDevice != null && this.mLastDevice.isConnected()) {
            return;
        }
        this.mLastDevice = null;
        for (CachedBluetoothDevice device : getDevices()) {
            if (device.isConnected()) {
                this.mLastDevice = device;
            }
        }
        if (this.mLastDevice != null || this.mConnectionState != 2) {
            return;
        }
        this.mConnectionState = 0;
        this.mHandler.sendEmptyMessage(2);
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        this.mEnabled = bluetoothState == 12;
        this.mHandler.sendEmptyMessage(2);
    }

    @Override
    public void onScanningStateChanged(boolean started) {
    }

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        cachedDevice.registerCallback(this);
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    @Override
    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    @Override
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    @Override
    public void onDeviceAttributesChanged() {
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    @Override
    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        this.mLastDevice = cachedDevice;
        updateConnected();
        this.mConnectionState = state;
        this.mHandler.sendEmptyMessage(2);
    }

    private final class H extends Handler {
        private final ArrayList<BluetoothController.Callback> mCallbacks;

        H(BluetoothControllerImpl this$0, H h) {
            this();
        }

        private H() {
            this.mCallbacks = new ArrayList<>();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    firePairedDevicesChanged();
                    break;
                case 2:
                    fireStateChange();
                    break;
                case 3:
                    this.mCallbacks.add((BluetoothController.Callback) msg.obj);
                    break;
                case 4:
                    this.mCallbacks.remove((BluetoothController.Callback) msg.obj);
                    break;
            }
        }

        private void firePairedDevicesChanged() {
            for (BluetoothController.Callback cb : this.mCallbacks) {
                cb.onBluetoothDevicesChanged();
            }
        }

        private void fireStateChange() {
            for (BluetoothController.Callback cb : this.mCallbacks) {
                fireStateChange(cb);
            }
        }

        private void fireStateChange(BluetoothController.Callback cb) {
            cb.onBluetoothStateChange(BluetoothControllerImpl.this.mEnabled);
        }
    }
}
