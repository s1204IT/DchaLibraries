package com.android.bluetooth.a2dpsink;

import android.bluetooth.BluetoothAudioConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothA2dpSink;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.a2dpsink.mbs.A2dpMediaBrowserService;
import com.android.bluetooth.btservice.ProfileService;
import java.util.ArrayList;
import java.util.List;

public class A2dpSinkService extends ProfileService {
    private static final boolean DBG = true;
    private static final String TAG = "A2dpSinkService";
    private static A2dpSinkService sA2dpSinkService;
    private A2dpSinkStateMachine mStateMachine;

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothA2dpSinkBinder(this);
    }

    @Override
    protected boolean start() {
        Log.d(TAG, "start()");
        startService(new Intent(this, (Class<?>) A2dpMediaBrowserService.class));
        this.mStateMachine = A2dpSinkStateMachine.make(this, this);
        setA2dpSinkService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        Log.d(TAG, "stop()");
        setA2dpSinkService(null);
        if (this.mStateMachine != null) {
            this.mStateMachine.doQuit();
        }
        stopService(new Intent(this, (Class<?>) A2dpMediaBrowserService.class));
        return true;
    }

    @Override
    protected void cleanup() {
        if (this.mStateMachine != null) {
            this.mStateMachine.cleanup();
        }
    }

    public static synchronized A2dpSinkService getA2dpSinkService() {
        if (sA2dpSinkService == null) {
            Log.w(TAG, "getA2dpSinkService(): service is null");
            return null;
        }
        if (!sA2dpSinkService.isAvailable()) {
            Log.w(TAG, "getA2dpSinkService(): service is not available ");
            return null;
        }
        return sA2dpSinkService;
    }

    private static synchronized void setA2dpSinkService(A2dpSinkService a2dpSinkService) {
        Log.d(TAG, "setA2dpSinkService(): set to: " + a2dpSinkService);
        sA2dpSinkService = a2dpSinkService;
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        int connectionState = this.mStateMachine.getConnectionState(bluetoothDevice);
        if (connectionState == 2 || connectionState == 1 || getPriority(bluetoothDevice) == 0) {
            return false;
        }
        this.mStateMachine.sendMessage(1, bluetoothDevice);
        return true;
    }

    boolean disconnect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        int connectionState = this.mStateMachine.getConnectionState(bluetoothDevice);
        if (connectionState != 2 && connectionState != 1) {
            return false;
        }
        this.mStateMachine.sendMessage(2, bluetoothDevice);
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mStateMachine.getConnectedDevices();
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mStateMachine.getDevicesMatchingConnectionStates(iArr);
    }

    int getConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mStateMachine.getConnectionState(bluetoothDevice);
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothA2dpSrcPriorityKey(bluetoothDevice.getAddress()), i);
        Log.d(TAG, "Saved priority " + bluetoothDevice + " = " + i);
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothA2dpSrcPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    public void informAvrcpPassThroughCmd(BluetoothDevice bluetoothDevice, int i, int i2) {
        if (this.mStateMachine != null) {
            if (i == 68 && i2 == 1) {
                this.mStateMachine.sendMessage(A2dpSinkStateMachine.EVENT_AVRCP_CT_PLAY);
            } else if ((i == 70 || i == 69) && i2 == 1) {
                this.mStateMachine.sendMessage(A2dpSinkStateMachine.EVENT_AVRCP_CT_PAUSE);
            }
        }
    }

    public void informTGStatePlaying(BluetoothDevice bluetoothDevice, boolean z) {
        if (this.mStateMachine != null) {
            if (!z) {
                this.mStateMachine.sendMessage(A2dpSinkStateMachine.EVENT_AVRCP_TG_PAUSE);
            } else {
                this.mStateMachine.sendMessage(A2dpSinkStateMachine.EVENT_AVRCP_TG_PLAY);
            }
        }
    }

    public void requestAudioFocus(BluetoothDevice bluetoothDevice, boolean z) {
        if (this.mStateMachine != null) {
            this.mStateMachine.sendMessage(A2dpSinkStateMachine.EVENT_REQUEST_FOCUS);
        }
    }

    synchronized boolean isA2dpPlaying(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        Log.d(TAG, "isA2dpPlaying(" + bluetoothDevice + ")");
        return this.mStateMachine.isPlaying(bluetoothDevice);
    }

    BluetoothAudioConfig getAudioConfig(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return this.mStateMachine.getAudioConfig(bluetoothDevice);
    }

    private static class BluetoothA2dpSinkBinder extends IBluetoothA2dpSink.Stub implements ProfileService.IProfileServiceBinder {
        private A2dpSinkService mService;

        private A2dpSinkService getService() {
            if (!Utils.checkCaller()) {
                Log.w(A2dpSinkService.TAG, "A2dp call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        BluetoothA2dpSinkBinder(A2dpSinkService a2dpSinkService) {
            this.mService = a2dpSinkService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            A2dpSinkService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            A2dpSinkService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            A2dpSinkService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedDevices();
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            A2dpSinkService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            A2dpSinkService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public boolean isA2dpPlaying(BluetoothDevice bluetoothDevice) {
            A2dpSinkService service = getService();
            if (service == null) {
                return false;
            }
            return service.isA2dpPlaying(bluetoothDevice);
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            A2dpSinkService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            A2dpSinkService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }

        public BluetoothAudioConfig getAudioConfig(BluetoothDevice bluetoothDevice) {
            A2dpSinkService service = getService();
            if (service == null) {
                return null;
            }
            return service.getAudioConfig(bluetoothDevice);
        }
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        if (this.mStateMachine != null) {
            this.mStateMachine.dump(sb);
        }
    }
}
