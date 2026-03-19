package com.android.bluetooth.hid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothHidHost;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.vcard.VCardConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HidHostService extends ProfileService {
    private static final int CONN_STATE_CONNECTED = 0;
    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_DISCONNECTED = 2;
    private static final int CONN_STATE_DISCONNECTING = 3;
    private static final boolean DBG = false;
    private static final int MESSAGE_CONNECT = 1;
    private static final int MESSAGE_CONNECT_STATE_CHANGED = 3;
    private static final int MESSAGE_DISCONNECT = 2;
    private static final int MESSAGE_GET_IDLE_TIME = 14;
    private static final int MESSAGE_GET_PROTOCOL_MODE = 4;
    private static final int MESSAGE_GET_REPORT = 8;
    private static final int MESSAGE_ON_GET_IDLE_TIME = 15;
    private static final int MESSAGE_ON_GET_PROTOCOL_MODE = 6;
    private static final int MESSAGE_ON_GET_REPORT = 9;
    private static final int MESSAGE_ON_HANDSHAKE = 13;
    private static final int MESSAGE_ON_VIRTUAL_UNPLUG = 12;
    private static final int MESSAGE_SEND_DATA = 11;
    private static final int MESSAGE_SET_IDLE_TIME = 16;
    private static final int MESSAGE_SET_PROTOCOL_MODE = 7;
    private static final int MESSAGE_SET_REPORT = 10;
    private static final int MESSAGE_VIRTUAL_UNPLUG = 5;
    private static final String TAG = "BluetoothHidHostService";
    private static HidHostService sHidHostService;
    private Map<BluetoothDevice, Integer> mInputDevices;
    private boolean mNativeAvailable;
    private BluetoothDevice mTargetDevice = null;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int iIntValue;
            if (HidHostService.sHidHostService == null || !HidHostService.sHidHostService.isAvailable()) {
                Log.e(HidHostService.TAG, "handleMessage: service is null or unavailable sHidHostService:" + HidHostService.sHidHostService);
                return;
            }
            switch (message.what) {
                case 1:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                    if (!HidHostService.this.connectHidNative(Utils.getByteAddress(bluetoothDevice))) {
                        HidHostService.this.broadcastConnectionState(bluetoothDevice, 3);
                        HidHostService.this.broadcastConnectionState(bluetoothDevice, 0);
                    } else {
                        HidHostService.this.mTargetDevice = bluetoothDevice;
                    }
                    break;
                case 2:
                    BluetoothDevice bluetoothDevice2 = (BluetoothDevice) message.obj;
                    if (!HidHostService.this.disconnectHidNative(Utils.getByteAddress(bluetoothDevice2))) {
                        HidHostService.this.broadcastConnectionState(bluetoothDevice2, 3);
                        HidHostService.this.broadcastConnectionState(bluetoothDevice2, 0);
                    }
                    break;
                case 3:
                    BluetoothDevice device = HidHostService.this.getDevice((byte[]) message.obj);
                    int i = message.arg1;
                    Integer num = (Integer) HidHostService.this.mInputDevices.get(device);
                    if (num != null) {
                        iIntValue = num.intValue();
                    } else {
                        iIntValue = 0;
                    }
                    if (HidHostService.this.okToBroadcastConnectState(device, i, iIntValue)) {
                        HidHostService.this.broadcastConnectionState(device, HidHostService.convertHalState(i));
                    }
                    if (i == 0 && HidHostService.this.mTargetDevice != null && HidHostService.this.mTargetDevice.equals(device)) {
                        HidHostService.this.mTargetDevice = null;
                        AdapterService.getAdapterService().enable(false);
                        break;
                    }
                    break;
                case 4:
                    if (!HidHostService.this.getProtocolModeNative(Utils.getByteAddress((BluetoothDevice) message.obj))) {
                        Log.e(HidHostService.TAG, "Error: get protocol mode native returns false");
                    }
                    break;
                case 5:
                    if (!HidHostService.this.virtualUnPlugNative(Utils.getByteAddress((BluetoothDevice) message.obj))) {
                        Log.e(HidHostService.TAG, "Error: virtual unplug native returns false");
                    }
                    break;
                case 6:
                    HidHostService.this.broadcastProtocolMode(HidHostService.this.getDevice((byte[]) message.obj), message.arg1);
                    break;
                case 7:
                    BluetoothDevice bluetoothDevice3 = (BluetoothDevice) message.obj;
                    byte b = (byte) message.arg1;
                    Log.d(HidHostService.TAG, "sending set protocol mode(" + ((int) b) + ")");
                    if (!HidHostService.this.setProtocolModeNative(Utils.getByteAddress(bluetoothDevice3), b)) {
                        Log.e(HidHostService.TAG, "Error: set protocol mode native returns false");
                    }
                    break;
                case 8:
                    BluetoothDevice bluetoothDevice4 = (BluetoothDevice) message.obj;
                    Bundle data = message.getData();
                    if (!HidHostService.this.getReportNative(Utils.getByteAddress(bluetoothDevice4), data.getByte("android.bluetooth.BluetoothHidHost.extra.REPORT_TYPE"), data.getByte("android.bluetooth.BluetoothHidHost.extra.REPORT_ID"), data.getInt("android.bluetooth.BluetoothHidHost.extra.REPORT_BUFFER_SIZE"))) {
                        Log.e(HidHostService.TAG, "Error: get report native returns false");
                    }
                    break;
                case 9:
                    BluetoothDevice device2 = HidHostService.this.getDevice((byte[]) message.obj);
                    Bundle data2 = message.getData();
                    HidHostService.this.broadcastReport(device2, data2.getByteArray("android.bluetooth.BluetoothHidHost.extra.REPORT"), data2.getInt("android.bluetooth.BluetoothHidHost.extra.REPORT_BUFFER_SIZE"));
                    break;
                case 10:
                    BluetoothDevice bluetoothDevice5 = (BluetoothDevice) message.obj;
                    Bundle data3 = message.getData();
                    if (!HidHostService.this.setReportNative(Utils.getByteAddress(bluetoothDevice5), data3.getByte("android.bluetooth.BluetoothHidHost.extra.REPORT_TYPE"), data3.getString("android.bluetooth.BluetoothHidHost.extra.REPORT"))) {
                        Log.e(HidHostService.TAG, "Error: set report native returns false");
                    }
                    break;
                case 11:
                    if (!HidHostService.this.sendDataNative(Utils.getByteAddress((BluetoothDevice) message.obj), message.getData().getString("android.bluetooth.BluetoothHidHost.extra.REPORT"))) {
                        Log.e(HidHostService.TAG, "Error: send data native returns false");
                    }
                    break;
                case 12:
                    HidHostService.this.broadcastVirtualUnplugStatus(HidHostService.this.getDevice((byte[]) message.obj), message.arg1);
                    break;
                case 13:
                    HidHostService.this.broadcastHandshake(HidHostService.this.getDevice((byte[]) message.obj), message.arg1);
                    break;
                case 14:
                    if (!HidHostService.this.getIdleTimeNative(Utils.getByteAddress((BluetoothDevice) message.obj))) {
                        Log.e(HidHostService.TAG, "Error: get idle time native returns false");
                    }
                    break;
                case 15:
                    HidHostService.this.broadcastIdleTime(HidHostService.this.getDevice((byte[]) message.obj), message.arg1);
                    break;
                case 16:
                    if (!HidHostService.this.setIdleTimeNative(Utils.getByteAddress((BluetoothDevice) message.obj), message.getData().getByte("android.bluetooth.BluetoothHidHost.extra.IDLE_TIME"))) {
                        Log.e(HidHostService.TAG, "Error: get idle time native returns false");
                    }
                    break;
            }
        }
    };

    private static native void classInitNative();

    private native void cleanupNative();

    private native boolean connectHidNative(byte[] bArr);

    private native boolean disconnectHidNative(byte[] bArr);

    private native boolean getIdleTimeNative(byte[] bArr);

    private native boolean getProtocolModeNative(byte[] bArr);

    private native boolean getReportNative(byte[] bArr, byte b, byte b2, int i);

    private native void initializeNative();

    private native boolean sendDataNative(byte[] bArr, String str);

    private native boolean setIdleTimeNative(byte[] bArr, byte b);

    private native boolean setProtocolModeNative(byte[] bArr, byte b);

    private native boolean setReportNative(byte[] bArr, byte b, String str);

    private native boolean virtualUnPlugNative(byte[] bArr);

    static {
        classInitNative();
    }

    @Override
    public ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothHidHostBinder(this);
    }

    @Override
    protected boolean start() {
        this.mInputDevices = Collections.synchronizedMap(new HashMap());
        initializeNative();
        this.mNativeAvailable = true;
        setHidHostService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        return true;
    }

    @Override
    protected void cleanup() {
        if (this.mNativeAvailable) {
            cleanupNative();
            this.mNativeAvailable = false;
        }
        if (this.mInputDevices != null) {
            for (BluetoothDevice bluetoothDevice : this.mInputDevices.keySet()) {
                if (getConnectionState(bluetoothDevice) != 0) {
                    broadcastConnectionState(bluetoothDevice, 0);
                }
            }
            this.mInputDevices.clear();
        }
        setHidHostService(null);
    }

    public static synchronized HidHostService getHidHostService() {
        if (sHidHostService == null) {
            Log.w(TAG, "getHidHostService(): service is null");
            return null;
        }
        if (!sHidHostService.isAvailable()) {
            Log.w(TAG, "getHidHostService(): service is not available ");
            return null;
        }
        return sHidHostService;
    }

    private static synchronized void setHidHostService(HidHostService hidHostService) {
        sHidHostService = hidHostService;
    }

    private static class BluetoothHidHostBinder extends IBluetoothHidHost.Stub implements ProfileService.IProfileServiceBinder {
        private HidHostService mService;

        BluetoothHidHostBinder(HidHostService hidHostService) {
            this.mService = hidHostService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        private HidHostService getService() {
            if (!Utils.checkCaller()) {
                Log.w(HidHostService.TAG, "InputDevice call not allowed for non-active user");
                return null;
            }
            if (this.mService != null && this.mService.isAvailable()) {
                return this.mService;
            }
            Log.w(HidHostService.TAG, "Service is null");
            return null;
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            HidHostService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            return getDevicesMatchingConnectionStates(new int[]{2});
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            HidHostService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.setPriority(bluetoothDevice, i);
        }

        public int getPriority(BluetoothDevice bluetoothDevice) {
            HidHostService service = getService();
            if (service == null) {
                return -1;
            }
            return service.getPriority(bluetoothDevice);
        }

        public boolean getProtocolMode(BluetoothDevice bluetoothDevice) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.getProtocolMode(bluetoothDevice);
        }

        public boolean virtualUnplug(BluetoothDevice bluetoothDevice) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.virtualUnplug(bluetoothDevice);
        }

        public boolean setProtocolMode(BluetoothDevice bluetoothDevice, int i) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.setProtocolMode(bluetoothDevice, i);
        }

        public boolean getReport(BluetoothDevice bluetoothDevice, byte b, byte b2, int i) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.getReport(bluetoothDevice, b, b2, i);
        }

        public boolean setReport(BluetoothDevice bluetoothDevice, byte b, String str) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.setReport(bluetoothDevice, b, str);
        }

        public boolean sendData(BluetoothDevice bluetoothDevice, String str) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.sendData(bluetoothDevice, str);
        }

        public boolean setIdleTime(BluetoothDevice bluetoothDevice, byte b) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.setIdleTime(bluetoothDevice, b);
        }

        public boolean getIdleTime(BluetoothDevice bluetoothDevice) {
            HidHostService service = getService();
            if (service == null) {
                return false;
            }
            return service.getIdleTime(bluetoothDevice);
        }
    }

    boolean connect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (getConnectionState(bluetoothDevice) != 0) {
            Log.e(TAG, "Hid Device not disconnected: " + bluetoothDevice);
            return false;
        }
        if (getPriority(bluetoothDevice) == 0) {
            Log.e(TAG, "Hid Device PRIORITY_OFF: " + bluetoothDevice);
            return false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, bluetoothDevice));
        return true;
    }

    boolean disconnect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, bluetoothDevice));
        return true;
    }

    int getConnectionState(BluetoothDevice bluetoothDevice) {
        if (this.mInputDevices.get(bluetoothDevice) == null) {
            return 0;
        }
        return this.mInputDevices.get(bluetoothDevice).intValue();
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList();
        for (BluetoothDevice bluetoothDevice : this.mInputDevices.keySet()) {
            int connectionState = getConnectionState(bluetoothDevice);
            int length = iArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (iArr[i] != connectionState) {
                    i++;
                } else {
                    arrayList.add(bluetoothDevice);
                    break;
                }
            }
        }
        return arrayList;
    }

    public boolean setPriority(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        Settings.Global.putInt(getContentResolver(), Settings.Global.getBluetoothHidHostPriorityKey(bluetoothDevice.getAddress()), i);
        return true;
    }

    public int getPriority(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        return Settings.Global.getInt(getContentResolver(), Settings.Global.getBluetoothHidHostPriorityKey(bluetoothDevice.getAddress()), -1);
    }

    boolean getProtocolMode(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        if (getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4, bluetoothDevice));
        return true;
    }

    boolean virtualUnplug(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        if (getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, bluetoothDevice));
        return true;
    }

    boolean setProtocolMode(BluetoothDevice bluetoothDevice, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        if (getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(7);
        messageObtainMessage.obj = bluetoothDevice;
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
        return true;
    }

    boolean getReport(BluetoothDevice bluetoothDevice, byte b, byte b2, int i) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        if (getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(8);
        messageObtainMessage.obj = bluetoothDevice;
        Bundle bundle = new Bundle();
        bundle.putByte("android.bluetooth.BluetoothHidHost.extra.REPORT_TYPE", b);
        bundle.putByte("android.bluetooth.BluetoothHidHost.extra.REPORT_ID", b2);
        bundle.putInt("android.bluetooth.BluetoothHidHost.extra.REPORT_BUFFER_SIZE", i);
        messageObtainMessage.setData(bundle);
        this.mHandler.sendMessage(messageObtainMessage);
        return true;
    }

    boolean setReport(BluetoothDevice bluetoothDevice, byte b, String str) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        if (getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(10);
        messageObtainMessage.obj = bluetoothDevice;
        Bundle bundle = new Bundle();
        bundle.putByte("android.bluetooth.BluetoothHidHost.extra.REPORT_TYPE", b);
        bundle.putString("android.bluetooth.BluetoothHidHost.extra.REPORT", str);
        messageObtainMessage.setData(bundle);
        this.mHandler.sendMessage(messageObtainMessage);
        return true;
    }

    boolean sendData(BluetoothDevice bluetoothDevice, String str) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        if (getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        return sendDataNative(Utils.getByteAddress(bluetoothDevice), str);
    }

    boolean getIdleTime(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        if (getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(14, bluetoothDevice));
        return true;
    }

    boolean setIdleTime(BluetoothDevice bluetoothDevice, byte b) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH_ADMIN permission");
        if (getConnectionState(bluetoothDevice) != 2) {
            return false;
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(16);
        messageObtainMessage.obj = bluetoothDevice;
        Bundle bundle = new Bundle();
        bundle.putByte("android.bluetooth.BluetoothHidHost.extra.IDLE_TIME", b);
        messageObtainMessage.setData(bundle);
        this.mHandler.sendMessage(messageObtainMessage);
        return true;
    }

    private void onGetProtocolMode(byte[] bArr, int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(6);
        messageObtainMessage.obj = bArr;
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void onGetIdleTime(byte[] bArr, int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(15);
        messageObtainMessage.obj = bArr;
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void onGetReport(byte[] bArr, byte[] bArr2, int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(9);
        messageObtainMessage.obj = bArr;
        Bundle bundle = new Bundle();
        bundle.putByteArray("android.bluetooth.BluetoothHidHost.extra.REPORT", bArr2);
        bundle.putInt("android.bluetooth.BluetoothHidHost.extra.REPORT_BUFFER_SIZE", i);
        messageObtainMessage.setData(bundle);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void onHandshake(byte[] bArr, int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(13);
        messageObtainMessage.obj = bArr;
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void onVirtualUnplug(byte[] bArr, int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(12);
        messageObtainMessage.obj = bArr;
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private boolean okToBroadcastConnectState(BluetoothDevice bluetoothDevice, int i, int i2) {
        if (i == 0 && ((i2 == 0 || i2 == 1) && !okToConnect(bluetoothDevice))) {
            disconnectHidNative(Utils.getByteAddress(bluetoothDevice));
            return false;
        }
        if (i == 1 && getPriority(bluetoothDevice) == 0) {
            return false;
        }
        return (i == 3 && getPriority(bluetoothDevice) == 0) ? false : true;
    }

    private void onConnectStateChanged(byte[] bArr, int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(3);
        messageObtainMessage.obj = bArr;
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void broadcastConnectionState(BluetoothDevice bluetoothDevice, int i) {
        Integer num = this.mInputDevices.get(bluetoothDevice);
        int iIntValue = num == null ? 0 : num.intValue();
        if (iIntValue == i) {
            Log.w(TAG, "no state change: " + i);
            return;
        }
        if (i == 2) {
            MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.HID_HOST);
        }
        this.mInputDevices.put(bluetoothDevice, Integer.valueOf(i));
        Log.d(TAG, "Connection state " + bluetoothDevice + ": " + iIntValue + "->" + i);
        Intent intent = new Intent("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", iIntValue);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        sendBroadcastAsUser(intent, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
    }

    private void broadcastHandshake(BluetoothDevice bluetoothDevice, int i) {
        Intent intent = new Intent("android.bluetooth.input.profile.action.HANDSHAKE");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.BluetoothHidHost.extra.STATUS", i);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void broadcastProtocolMode(BluetoothDevice bluetoothDevice, int i) {
        Intent intent = new Intent("android.bluetooth.input.profile.action.PROTOCOL_MODE_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.BluetoothHidHost.extra.PROTOCOL_MODE", i);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void broadcastReport(BluetoothDevice bluetoothDevice, byte[] bArr, int i) {
        Intent intent = new Intent("android.bluetooth.input.profile.action.REPORT");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.BluetoothHidHost.extra.REPORT", bArr);
        intent.putExtra("android.bluetooth.BluetoothHidHost.extra.REPORT_BUFFER_SIZE", i);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void broadcastVirtualUnplugStatus(BluetoothDevice bluetoothDevice, int i) {
        Intent intent = new Intent("android.bluetooth.input.profile.action.VIRTUAL_UNPLUG_STATUS");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.BluetoothHidHost.extra.VIRTUAL_UNPLUG_STATUS", i);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void broadcastIdleTime(BluetoothDevice bluetoothDevice, int i) {
        Intent intent = new Intent("android.bluetooth.input.profile.action.IDLE_TIME_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.BluetoothHidHost.extra.IDLE_TIME", i);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    @VisibleForTesting(otherwise = 3)
    public boolean okToConnect(BluetoothDevice bluetoothDevice) {
        AdapterService adapterService = AdapterService.getAdapterService();
        if (adapterService == null) {
            Log.w(TAG, "okToConnect: adapter service is null");
            return false;
        }
        if (adapterService.isQuietModeEnabled() && this.mTargetDevice == null) {
            Log.w(TAG, "okToConnect: return false as quiet mode enabled");
            return false;
        }
        int priority = getPriority(bluetoothDevice);
        int bondState = adapterService.getBondState(bluetoothDevice);
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

    private static int convertHalState(int i) {
        switch (i) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Log.e(TAG, "bad hid connection state: " + i);
                break;
        }
        return 0;
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        println(sb, "mTargetDevice: " + this.mTargetDevice);
        println(sb, "mInputDevices:");
        for (BluetoothDevice bluetoothDevice : this.mInputDevices.keySet()) {
            println(sb, "  " + bluetoothDevice + " : " + this.mInputDevices.get(bluetoothDevice));
        }
    }
}
