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
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

/* loaded from: classes.dex */
public class BluetoothControllerImpl implements BluetoothCallback, CachedBluetoothDevice.Callback, LocalBluetoothProfileManager.ServiceListener, BluetoothController {
    private static final boolean DEBUG;
    private final Handler mBgHandler;
    private final int mCurrentUser;
    private boolean mEnabled;
    private int mState;
    private final UserManager mUserManager;
    private final WeakHashMap<CachedBluetoothDevice, ActuallyCachedState> mCachedState = new WeakHashMap<>();
    private final List<CachedBluetoothDevice> mConnectedDevices = new ArrayList();
    private int mConnectionState = 0;
    private final H mHandler = new H(Looper.getMainLooper());
    private final LocalBluetoothManager mLocalBluetoothManager = (LocalBluetoothManager) Dependency.get(LocalBluetoothManager.class);

    static {
        DEBUG = Log.isLoggable("BluetoothController", 3) || FeatureOptions.LOG_ENABLE;
    }

    public BluetoothControllerImpl(Context context, Looper looper) {
        this.mBgHandler = new Handler(looper);
        if (this.mLocalBluetoothManager != null) {
            this.mLocalBluetoothManager.getEventManager().setReceiverHandler(this.mBgHandler);
            this.mLocalBluetoothManager.getEventManager().registerCallback(this);
            this.mLocalBluetoothManager.getProfileManager().addServiceListener(this);
            onBluetoothStateChanged(this.mLocalBluetoothManager.getBluetoothAdapter().getBluetoothState());
        }
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mCurrentUser = ActivityManager.getCurrentUser();
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public boolean canConfigBluetooth() {
        return (this.mUserManager.hasUserRestriction("no_config_bluetooth", UserHandle.of(this.mCurrentUser)) || this.mUserManager.hasUserRestriction("no_bluetooth", UserHandle.of(this.mCurrentUser))) ? false : true;
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("BluetoothController state:");
        printWriter.print("  mLocalBluetoothManager=");
        printWriter.println(this.mLocalBluetoothManager);
        if (this.mLocalBluetoothManager == null) {
            return;
        }
        printWriter.print("  mEnabled=");
        printWriter.println(this.mEnabled);
        printWriter.print("  mConnectionState=");
        printWriter.println(stateToString(this.mConnectionState));
        printWriter.print("  mConnectedDevices=");
        printWriter.println(this.mConnectedDevices);
        printWriter.print("  mCallbacks.size=");
        printWriter.println(this.mHandler.mCallbacks.size());
        printWriter.println("  Bluetooth Devices:");
        Iterator<CachedBluetoothDevice> it = getDevices().iterator();
        while (it.hasNext()) {
            printWriter.println("    " + getDeviceString(it.next()));
        }
    }

    private static String stateToString(int i) {
        switch (i) {
            case 0:
                return "DISCONNECTED";
            case 1:
                return "CONNECTING";
            case 2:
                return "CONNECTED";
            case 3:
                return "DISCONNECTING";
            default:
                return "UNKNOWN(" + i + ")";
        }
    }

    private String getDeviceString(CachedBluetoothDevice cachedBluetoothDevice) {
        return cachedBluetoothDevice.getName() + " " + cachedBluetoothDevice.getBondState() + " " + cachedBluetoothDevice.isConnected();
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public int getBondState(CachedBluetoothDevice cachedBluetoothDevice) {
        return getCachedState(cachedBluetoothDevice).mBondState;
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public List<CachedBluetoothDevice> getConnectedDevices() {
        return this.mConnectedDevices;
    }

    /* JADX DEBUG: Method merged with bridge method: addCallback(Ljava/lang/Object;)V */
    @Override // com.android.systemui.statusbar.policy.CallbackController
    public void addCallback(BluetoothController.Callback callback) {
        this.mHandler.obtainMessage(3, callback).sendToTarget();
        this.mHandler.sendEmptyMessage(2);
    }

    /* JADX DEBUG: Method merged with bridge method: removeCallback(Ljava/lang/Object;)V */
    @Override // com.android.systemui.statusbar.policy.CallbackController
    public void removeCallback(BluetoothController.Callback callback) {
        this.mHandler.obtainMessage(4, callback).sendToTarget();
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public boolean isBluetoothEnabled() {
        return this.mEnabled;
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public int getBluetoothState() {
        return this.mState;
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public boolean isBluetoothConnected() {
        return this.mConnectionState == 2;
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public boolean isBluetoothConnecting() {
        return this.mConnectionState == 1;
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public void setBluetoothEnabled(boolean z) {
        if (this.mLocalBluetoothManager != null) {
            this.mLocalBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(z);
        }
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public boolean isBluetoothSupported() {
        return this.mLocalBluetoothManager != null;
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public void connect(CachedBluetoothDevice cachedBluetoothDevice) {
        if (this.mLocalBluetoothManager == null || cachedBluetoothDevice == null) {
            return;
        }
        cachedBluetoothDevice.connect(true);
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public void disconnect(CachedBluetoothDevice cachedBluetoothDevice) {
        if (this.mLocalBluetoothManager == null || cachedBluetoothDevice == null) {
            return;
        }
        cachedBluetoothDevice.disconnect();
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public String getConnectedDeviceName() {
        if (this.mConnectedDevices.size() == 1) {
            return this.mConnectedDevices.get(0).getName();
        }
        return null;
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController
    public Collection<CachedBluetoothDevice> getDevices() {
        if (this.mLocalBluetoothManager != null) {
            return this.mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        }
        return null;
    }

    private void updateConnected() {
        int connectionState = this.mLocalBluetoothManager.getBluetoothAdapter().getConnectionState();
        this.mConnectedDevices.clear();
        for (CachedBluetoothDevice cachedBluetoothDevice : getDevices()) {
            int maxConnectionState = cachedBluetoothDevice.getMaxConnectionState();
            if (maxConnectionState > connectionState) {
                connectionState = maxConnectionState;
            }
            if (cachedBluetoothDevice.isConnected()) {
                this.mConnectedDevices.add(cachedBluetoothDevice);
            }
        }
        if (this.mConnectedDevices.isEmpty() && connectionState == 2) {
            connectionState = 0;
        }
        if (connectionState != this.mConnectionState) {
            this.mConnectionState = connectionState;
            this.mHandler.sendEmptyMessage(2);
        }
    }

    @Override // com.android.settingslib.bluetooth.BluetoothCallback
    public void onBluetoothStateChanged(int i) {
        this.mEnabled = i == 12 || i == 11;
        if (DEBUG) {
            Log.d("BluetoothController", "onBluetoothStateChanged, bluetoothState = " + i + ", mEnabled = " + this.mEnabled);
        }
        this.mState = i;
        updateConnected();
        this.mHandler.sendEmptyMessage(2);
    }

    @Override // com.android.settingslib.bluetooth.BluetoothCallback
    public void onScanningStateChanged(boolean z) {
    }

    @Override // com.android.settingslib.bluetooth.BluetoothCallback
    public void onDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice) {
        cachedBluetoothDevice.registerCallback(this);
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.android.settingslib.bluetooth.BluetoothCallback
    public void onDeviceDeleted(CachedBluetoothDevice cachedBluetoothDevice) {
        this.mCachedState.remove(cachedBluetoothDevice);
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.android.settingslib.bluetooth.BluetoothCallback
    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        this.mCachedState.remove(cachedBluetoothDevice);
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.android.settingslib.bluetooth.CachedBluetoothDevice.Callback
    public void onDeviceAttributesChanged() {
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.android.settingslib.bluetooth.BluetoothCallback
    public void onConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        this.mCachedState.remove(cachedBluetoothDevice);
        updateConnected();
        this.mHandler.sendEmptyMessage(2);
    }

    @Override // com.android.settingslib.bluetooth.BluetoothCallback
    public void onActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
    }

    @Override // com.android.settingslib.bluetooth.BluetoothCallback
    public void onAudioModeChanged() {
    }

    private ActuallyCachedState getCachedState(CachedBluetoothDevice cachedBluetoothDevice) {
        ActuallyCachedState actuallyCachedState = this.mCachedState.get(cachedBluetoothDevice);
        if (actuallyCachedState == null) {
            ActuallyCachedState actuallyCachedState2 = new ActuallyCachedState(cachedBluetoothDevice, this.mHandler);
            this.mBgHandler.post(actuallyCachedState2);
            this.mCachedState.put(cachedBluetoothDevice, actuallyCachedState2);
            return actuallyCachedState2;
        }
        return actuallyCachedState;
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfileManager.ServiceListener
    public void onServiceConnected() {
        updateConnected();
        this.mHandler.sendEmptyMessage(1);
    }

    @Override // com.android.settingslib.bluetooth.LocalBluetoothProfileManager.ServiceListener
    public void onServiceDisconnected() {
    }

    private static class ActuallyCachedState implements Runnable {
        private int mBondState;
        private final WeakReference<CachedBluetoothDevice> mDevice;
        private int mMaxConnectionState;
        private final Handler mUiHandler;

        private ActuallyCachedState(CachedBluetoothDevice cachedBluetoothDevice, Handler handler) {
            this.mBondState = 10;
            this.mMaxConnectionState = 0;
            this.mDevice = new WeakReference<>(cachedBluetoothDevice);
            this.mUiHandler = handler;
        }

        @Override // java.lang.Runnable
        public void run() {
            CachedBluetoothDevice cachedBluetoothDevice = this.mDevice.get();
            if (cachedBluetoothDevice != null) {
                this.mBondState = cachedBluetoothDevice.getBondState();
                this.mMaxConnectionState = cachedBluetoothDevice.getMaxConnectionState();
                this.mUiHandler.removeMessages(1);
                this.mUiHandler.sendEmptyMessage(1);
            }
        }
    }

    private final class H extends Handler {
        private final ArrayList<BluetoothController.Callback> mCallbacks;

        public H(Looper looper) {
            super(looper);
            this.mCallbacks = new ArrayList<>();
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    firePairedDevicesChanged();
                    break;
                case 2:
                    fireStateChange();
                    break;
                case 3:
                    this.mCallbacks.add((BluetoothController.Callback) message.obj);
                    break;
                case 4:
                    this.mCallbacks.remove((BluetoothController.Callback) message.obj);
                    break;
            }
        }

        private void firePairedDevicesChanged() {
            Iterator<BluetoothController.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onBluetoothDevicesChanged();
            }
        }

        private void fireStateChange() {
            Iterator<BluetoothController.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                fireStateChange(it.next());
            }
        }

        private void fireStateChange(BluetoothController.Callback callback) {
            callback.onBluetoothStateChange(BluetoothControllerImpl.this.mEnabled);
        }
    }
}
