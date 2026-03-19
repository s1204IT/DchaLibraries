package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.vcard.VCardConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

final class RemoteDevices {
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final int MAX_DEVICE_QUEUE_SIZE = 200;
    private static final int MESSAGE_UUID_INTENT = 1;
    private static final String TAG = "BluetoothRemoteDevices";
    private static final int UUID_INTENT_DELAY = 6000;
    private static BluetoothAdapter sAdapter;
    private static AdapterService sAdapterService;
    private static ArrayList<BluetoothDevice> sSdpTracker;
    private Queue<String> mDeviceQueue;
    private final HashMap<String, DeviceProperties> mDevices;
    private final Handler mHandler;
    private final Object mObject = new Object();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte b;
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != -790471265) {
                if (iHashCode != 545516589) {
                    b = (iHashCode == 1772843706 && action.equals("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT")) ? (byte) 1 : (byte) -1;
                } else if (action.equals("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")) {
                    b = 2;
                }
            } else if (action.equals("android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    RemoteDevices.this.onHfIndicatorValueChanged(intent);
                    break;
                case 1:
                    RemoteDevices.this.onVendorSpecificHeadsetEvent(intent);
                    break;
                case 2:
                    RemoteDevices.this.onHeadsetConnectionStateChanged(intent);
                    break;
                default:
                    Log.w(RemoteDevices.TAG, "Unhandled intent: " + intent);
                    break;
            }
        }
    };

    private class RemoteDevicesHandler extends Handler {
        RemoteDevicesHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            BluetoothDevice bluetoothDevice;
            if (message.what == 1 && (bluetoothDevice = (BluetoothDevice) message.obj) != null) {
                RemoteDevices.this.sendUuidIntent(bluetoothDevice);
            }
        }
    }

    RemoteDevices(AdapterService adapterService, Looper looper) {
        sAdapter = BluetoothAdapter.getDefaultAdapter();
        sAdapterService = adapterService;
        sSdpTracker = new ArrayList<>();
        this.mDevices = new HashMap<>();
        this.mDeviceQueue = new LinkedList();
        this.mHandler = new RemoteDevicesHandler(looper);
    }

    void init() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT");
        intentFilter.addCategory("android.bluetooth.headset.intent.category.companyid.85");
        intentFilter.addCategory("android.bluetooth.headset.intent.category.companyid.76");
        intentFilter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        sAdapterService.registerReceiver(this.mReceiver, intentFilter);
    }

    void cleanup() {
        sAdapterService.unregisterReceiver(this.mReceiver);
        reset();
    }

    void reset() {
        if (sSdpTracker != null) {
            sSdpTracker.clear();
        }
        if (this.mDevices != null) {
            this.mDevices.clear();
        }
        if (this.mDeviceQueue != null) {
            this.mDeviceQueue.clear();
        }
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    DeviceProperties getDeviceProperties(BluetoothDevice bluetoothDevice) {
        DeviceProperties deviceProperties;
        synchronized (this.mDevices) {
            deviceProperties = this.mDevices.get(bluetoothDevice.getAddress());
        }
        return deviceProperties;
    }

    BluetoothDevice getDevice(byte[] bArr) {
        synchronized (this.mDevices) {
            DeviceProperties deviceProperties = this.mDevices.get(Utils.getAddressStringFromByte(bArr));
            if (deviceProperties == null) {
                return null;
            }
            return deviceProperties.getDevice();
        }
    }

    @VisibleForTesting
    DeviceProperties addDeviceProperties(byte[] bArr) {
        synchronized (this.mDevices) {
            DeviceProperties deviceProperties = new DeviceProperties();
            deviceProperties.mDevice = sAdapter.getRemoteDevice(Utils.getAddressStringFromByte(bArr));
            deviceProperties.mAddress = bArr;
            String addressStringFromByte = Utils.getAddressStringFromByte(bArr);
            if (this.mDevices.put(addressStringFromByte, deviceProperties) == null) {
                this.mDeviceQueue.offer(addressStringFromByte);
                if (this.mDeviceQueue.size() > 200) {
                    String strPoll = this.mDeviceQueue.poll();
                    for (BluetoothDevice bluetoothDevice : sAdapterService.getBondedDevices()) {
                        if (bluetoothDevice.getAddress().equals(strPoll)) {
                            return deviceProperties;
                        }
                    }
                    debugLog("Removing device " + strPoll + " from property map");
                    this.mDevices.remove(strPoll);
                }
            }
            return deviceProperties;
        }
    }

    class DeviceProperties {
        private byte[] mAddress;
        private String mAlias;
        private BluetoothDevice mDevice;

        @VisibleForTesting
        int mDeviceType;
        private boolean mIsBondingInitiatedLocally;
        private String mName;
        private short mRssi;

        @VisibleForTesting
        ParcelUuid[] mUuids;
        private int mBluetoothClass = 7936;
        private int mBatteryLevel = -1;

        @VisibleForTesting
        int mBondState = 10;

        DeviceProperties() {
        }

        String getName() {
            String str;
            synchronized (RemoteDevices.this.mObject) {
                str = this.mName;
            }
            return str;
        }

        int getBluetoothClass() {
            int i;
            synchronized (RemoteDevices.this.mObject) {
                i = this.mBluetoothClass;
            }
            return i;
        }

        ParcelUuid[] getUuids() {
            ParcelUuid[] parcelUuidArr;
            synchronized (RemoteDevices.this.mObject) {
                parcelUuidArr = this.mUuids;
            }
            return parcelUuidArr;
        }

        byte[] getAddress() {
            byte[] bArr;
            synchronized (RemoteDevices.this.mObject) {
                bArr = this.mAddress;
            }
            return bArr;
        }

        BluetoothDevice getDevice() {
            BluetoothDevice bluetoothDevice;
            synchronized (RemoteDevices.this.mObject) {
                bluetoothDevice = this.mDevice;
            }
            return bluetoothDevice;
        }

        short getRssi() {
            short s;
            synchronized (RemoteDevices.this.mObject) {
                s = this.mRssi;
            }
            return s;
        }

        int getDeviceType() {
            int i;
            synchronized (RemoteDevices.this.mObject) {
                i = this.mDeviceType;
            }
            return i;
        }

        String getAlias() {
            String str;
            synchronized (RemoteDevices.this.mObject) {
                str = this.mAlias;
            }
            return str;
        }

        void setAlias(BluetoothDevice bluetoothDevice, String str) {
            synchronized (RemoteDevices.this.mObject) {
                this.mAlias = str;
                RemoteDevices.sAdapterService.setDevicePropertyNative(this.mAddress, 10, str.getBytes());
                Intent intent = new Intent("android.bluetooth.device.action.ALIAS_CHANGED");
                intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
                intent.putExtra("android.bluetooth.device.extra.NAME", str);
                RemoteDevices.sAdapterService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
            }
        }

        void setBondState(int i) {
            synchronized (RemoteDevices.this.mObject) {
                this.mBondState = i;
                if (i == 10) {
                    this.mUuids = null;
                }
            }
        }

        int getBondState() {
            int i;
            synchronized (RemoteDevices.this.mObject) {
                i = this.mBondState;
            }
            return i;
        }

        void setBondingInitiatedLocally(boolean z) {
            synchronized (RemoteDevices.this.mObject) {
                this.mIsBondingInitiatedLocally = z;
            }
        }

        boolean isBondingInitiatedLocally() {
            boolean z;
            synchronized (RemoteDevices.this.mObject) {
                z = this.mIsBondingInitiatedLocally;
            }
            return z;
        }

        int getBatteryLevel() {
            int i;
            synchronized (RemoteDevices.this.mObject) {
                i = this.mBatteryLevel;
            }
            return i;
        }

        void setBatteryLevel(int i) {
            synchronized (RemoteDevices.this.mObject) {
                this.mBatteryLevel = i;
            }
        }
    }

    private void sendUuidIntent(BluetoothDevice bluetoothDevice) {
        DeviceProperties deviceProperties = getDeviceProperties(bluetoothDevice);
        Intent intent = new Intent("android.bluetooth.device.action.UUID");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.device.extra.UUID", deviceProperties == null ? null : deviceProperties.mUuids);
        sAdapterService.sendBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
        sSdpTracker.remove(bluetoothDevice);
    }

    void setBondingInitiatedLocally(byte[] bArr) {
        DeviceProperties deviceProperties;
        BluetoothDevice device = getDevice(bArr);
        if (device == null) {
            deviceProperties = addDeviceProperties(bArr);
        } else {
            deviceProperties = getDeviceProperties(device);
        }
        deviceProperties.setBondingInitiatedLocally(true);
    }

    @VisibleForTesting
    void updateBatteryLevel(BluetoothDevice bluetoothDevice, int i) {
        if (bluetoothDevice == null || i < 0 || i > 100) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid parameters device=");
            sb.append(String.valueOf(bluetoothDevice == null));
            sb.append(", batteryLevel=");
            sb.append(String.valueOf(i));
            warnLog(sb.toString());
            return;
        }
        DeviceProperties deviceProperties = getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            deviceProperties = addDeviceProperties(Utils.getByteAddress(bluetoothDevice));
        }
        synchronized (this.mObject) {
            if (i == deviceProperties.getBatteryLevel()) {
                debugLog("Same battery level for device " + bluetoothDevice + " received " + String.valueOf(i) + "%");
                return;
            }
            deviceProperties.setBatteryLevel(i);
            sendBatteryLevelChangedBroadcast(bluetoothDevice, i);
            Log.d(TAG, "Updated device " + bluetoothDevice + " battery level to " + i + "%");
        }
    }

    @VisibleForTesting
    void resetBatteryLevel(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            warnLog("Device is null");
            return;
        }
        DeviceProperties deviceProperties = getDeviceProperties(bluetoothDevice);
        if (deviceProperties == null) {
            return;
        }
        synchronized (this.mObject) {
            if (deviceProperties.getBatteryLevel() == -1) {
                debugLog("Battery level was never set or is already reset, device=" + bluetoothDevice);
                return;
            }
            deviceProperties.setBatteryLevel(-1);
            sendBatteryLevelChangedBroadcast(bluetoothDevice, -1);
            Log.d(TAG, "Reset battery level, device=" + bluetoothDevice);
        }
    }

    private void sendBatteryLevelChangedBroadcast(BluetoothDevice bluetoothDevice, int i) {
        Intent intent = new Intent("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.device.extra.BATTERY_LEVEL", i);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        sAdapterService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private static boolean areUuidsEqual(ParcelUuid[] parcelUuidArr, ParcelUuid[] parcelUuidArr2) {
        int length;
        int length2;
        if (parcelUuidArr != null) {
            length = parcelUuidArr.length;
        } else {
            length = 0;
        }
        if (parcelUuidArr2 != null) {
            length2 = parcelUuidArr2.length;
        } else {
            length2 = 0;
        }
        if (length != length2) {
            return false;
        }
        HashSet hashSet = new HashSet();
        for (int i = 0; i < length; i++) {
            hashSet.add(parcelUuidArr[i]);
        }
        for (int i2 = 0; i2 < length2; i2++) {
            hashSet.remove(parcelUuidArr2[i2]);
        }
        return hashSet.isEmpty();
    }

    void devicePropertyChangedCallback(byte[] bArr, int[] iArr, byte[][] bArr2) {
        DeviceProperties deviceProperties;
        BluetoothDevice device = getDevice(bArr);
        if (device == null) {
            debugLog("Added new device property");
            DeviceProperties devicePropertiesAddDeviceProperties = addDeviceProperties(bArr);
            device = getDevice(bArr);
            deviceProperties = devicePropertiesAddDeviceProperties;
        } else {
            deviceProperties = getDeviceProperties(device);
        }
        if (iArr.length <= 0) {
            errorLog("No properties to update");
            return;
        }
        for (int i = 0; i < iArr.length; i++) {
            int i2 = iArr[i];
            byte[] bArr3 = bArr2[i];
            if (bArr3.length > 0) {
                synchronized (this.mObject) {
                    debugLog("Property type: " + i2);
                    switch (i2) {
                        case 1:
                            String str = new String(bArr3);
                            if (!str.equals(deviceProperties.mName)) {
                                deviceProperties.mName = str;
                                Intent intent = new Intent("android.bluetooth.device.action.NAME_CHANGED");
                                intent.putExtra("android.bluetooth.device.extra.DEVICE", device);
                                intent.putExtra("android.bluetooth.device.extra.NAME", deviceProperties.mName);
                                intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
                                AdapterService adapterService = sAdapterService;
                                AdapterService adapterService2 = sAdapterService;
                                adapterService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
                                debugLog("Remote Device name is: " + deviceProperties.mName);
                            } else {
                                Log.w(TAG, "Skip name update for " + device);
                            }
                            break;
                        case 2:
                            deviceProperties.mAddress = bArr3;
                            debugLog("Remote Address is:" + Utils.getAddressStringFromByte(bArr3));
                            break;
                        case 3:
                            int length = bArr3.length / 16;
                            ParcelUuid[] parcelUuidArrByteArrayToUuid = Utils.byteArrayToUuid(bArr3);
                            if (areUuidsEqual(parcelUuidArrByteArrayToUuid, deviceProperties.mUuids)) {
                                Log.w(TAG, "Skip uuids update for " + device.getAddress());
                            } else {
                                deviceProperties.mUuids = parcelUuidArrByteArrayToUuid;
                                if (sAdapterService.getState() == 12) {
                                    sAdapterService.deviceUuidUpdated(device);
                                    sendUuidIntent(device);
                                }
                            }
                            break;
                        case 4:
                            if (Utils.byteArrayToInt(bArr3) != deviceProperties.mBluetoothClass) {
                                deviceProperties.mBluetoothClass = Utils.byteArrayToInt(bArr3);
                                Intent intent2 = new Intent("android.bluetooth.device.action.CLASS_CHANGED");
                                intent2.putExtra("android.bluetooth.device.extra.DEVICE", device);
                                intent2.putExtra("android.bluetooth.device.extra.CLASS", new BluetoothClass(deviceProperties.mBluetoothClass));
                                intent2.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
                                AdapterService adapterService3 = sAdapterService;
                                AdapterService adapterService4 = sAdapterService;
                                adapterService3.sendBroadcast(intent2, ProfileService.BLUETOOTH_PERM);
                                debugLog("Remote class is:" + deviceProperties.mBluetoothClass);
                            } else {
                                Log.w(TAG, "Skip class update for " + device);
                            }
                            break;
                        case 5:
                            deviceProperties.mDeviceType = Utils.byteArrayToInt(bArr3);
                            break;
                        default:
                            switch (i2) {
                                case 10:
                                    deviceProperties.mAlias = new String(bArr3);
                                    debugLog("Remote device alias is: " + deviceProperties.mAlias);
                                    break;
                                case 11:
                                    deviceProperties.mRssi = bArr3[0];
                                    break;
                            }
                            break;
                    }
                }
            }
        }
    }

    void deviceFoundCallback(byte[] bArr) {
        BluetoothDevice device = getDevice(bArr);
        debugLog("deviceFoundCallback: Remote Address is:" + device);
        DeviceProperties deviceProperties = getDeviceProperties(device);
        if (deviceProperties == null) {
            errorLog("Device Properties is null for Device:" + device);
            return;
        }
        Intent intent = new Intent("android.bluetooth.device.action.FOUND");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", device);
        intent.putExtra("android.bluetooth.device.extra.CLASS", new BluetoothClass(deviceProperties.mBluetoothClass));
        intent.putExtra("android.bluetooth.device.extra.RSSI", deviceProperties.mRssi);
        intent.putExtra("android.bluetooth.device.extra.NAME", deviceProperties.mName);
        sAdapterService.sendBroadcastMultiplePermissions(intent, new String[]{ProfileService.BLUETOOTH_PERM, "android.permission.ACCESS_COARSE_LOCATION"});
    }

    void aclStateChangeCallback(int i, byte[] bArr, int i2) {
        Intent intent;
        BluetoothDevice device = getDevice(bArr);
        if (device == null) {
            errorLog("aclStateChangeCallback: device is NULL, address=" + Utils.getAddressStringFromByte(bArr) + ", newState=" + i2);
            return;
        }
        int state = sAdapterService.getState();
        Intent intent2 = null;
        if (i2 == 0) {
            if (state == 12 || state == 11) {
                intent2 = new Intent("android.bluetooth.device.action.ACL_CONNECTED");
            } else if (state == 15 || state == 14) {
                intent2 = new Intent("android.bluetooth.adapter.action.BLE_ACL_CONNECTED");
            }
            debugLog("aclStateChangeCallback: Adapter State: " + BluetoothAdapter.nameForState(state) + " Connected: " + device);
        } else {
            if (device.getBondState() == 11) {
                intent2 = new Intent("android.bluetooth.device.action.PAIRING_CANCEL");
                intent2.putExtra("android.bluetooth.device.extra.DEVICE", device);
                intent2.setPackage(sAdapterService.getString(R.string.pairing_ui_package));
                AdapterService adapterService = sAdapterService;
                AdapterService adapterService2 = sAdapterService;
                adapterService.sendBroadcast(intent2, ProfileService.BLUETOOTH_PERM);
            }
            if (state == 12 || state == 13) {
                intent = new Intent("android.bluetooth.device.action.ACL_DISCONNECTED");
            } else {
                if (state == 15 || state == 16) {
                    intent = new Intent("android.bluetooth.adapter.action.BLE_ACL_DISCONNECTED");
                }
                if (sAdapterService.getConnectionState(device) == 0) {
                    resetBatteryLevel(device);
                }
                debugLog("aclStateChangeCallback: Adapter State: " + BluetoothAdapter.nameForState(state) + " Disconnected: " + device);
            }
            intent2 = intent;
            if (sAdapterService.getConnectionState(device) == 0) {
            }
            debugLog("aclStateChangeCallback: Adapter State: " + BluetoothAdapter.nameForState(state) + " Disconnected: " + device);
        }
        if (intent2 != null) {
            intent2.putExtra("android.bluetooth.device.extra.DEVICE", device);
            intent2.addFlags(83886080);
            intent2.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
            AdapterService adapterService3 = sAdapterService;
            AdapterService adapterService4 = sAdapterService;
            adapterService3.sendBroadcast(intent2, ProfileService.BLUETOOTH_PERM);
            return;
        }
        Log.e(TAG, "aclStateChangeCallback intent is null. deviceBondState: " + device.getBondState());
    }

    void fetchUuids(BluetoothDevice bluetoothDevice) {
        if (sSdpTracker.contains(bluetoothDevice)) {
            return;
        }
        sSdpTracker.add(bluetoothDevice);
        Message messageObtainMessage = this.mHandler.obtainMessage(1);
        messageObtainMessage.obj = bluetoothDevice;
        this.mHandler.sendMessageDelayed(messageObtainMessage, 6000L);
        sAdapterService.getRemoteServicesNative(Utils.getBytesFromAddress(bluetoothDevice.getAddress()));
    }

    void updateUuids(BluetoothDevice bluetoothDevice) {
        Message messageObtainMessage = this.mHandler.obtainMessage(1);
        messageObtainMessage.obj = bluetoothDevice;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    @VisibleForTesting
    void onHeadsetConnectionStateChanged(Intent intent) {
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if (bluetoothDevice == null) {
            Log.e(TAG, "onHeadsetConnectionStateChanged() remote device is null");
        } else if (intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0) == 0) {
            resetBatteryLevel(bluetoothDevice);
        }
    }

    @VisibleForTesting
    void onHfIndicatorValueChanged(Intent intent) {
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if (bluetoothDevice == null) {
            Log.e(TAG, "onHfIndicatorValueChanged() remote device is null");
            return;
        }
        int intExtra = intent.getIntExtra("android.bluetooth.headset.extra.HF_INDICATORS_IND_ID", -1);
        int intExtra2 = intent.getIntExtra("android.bluetooth.headset.extra.HF_INDICATORS_IND_VALUE", -1);
        if (intExtra == 2) {
            updateBatteryLevel(bluetoothDevice, intExtra2);
        }
    }

    @VisibleForTesting
    void onVendorSpecificHeadsetEvent(Intent intent) {
        byte b;
        int batteryLevelFromXEventVsc;
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if (bluetoothDevice == null) {
            Log.e(TAG, "onVendorSpecificHeadsetEvent() remote device is null");
            return;
        }
        String stringExtra = intent.getStringExtra("android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD");
        if (stringExtra == null) {
            Log.e(TAG, "onVendorSpecificHeadsetEvent() command is null");
            return;
        }
        if (intent.getIntExtra("android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE", -1) != 2) {
            debugLog("onVendorSpecificHeadsetEvent() only SET command is processed");
            return;
        }
        Object[] objArr = (Object[]) intent.getExtras().get("android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_ARGS");
        if (objArr == null) {
            Log.e(TAG, "onVendorSpecificHeadsetEvent() arguments are null");
            return;
        }
        int iHashCode = stringExtra.hashCode();
        if (iHashCode != 1884621890) {
            b = (iHashCode == 2093671693 && stringExtra.equals("+XEVENT")) ? (byte) 0 : (byte) -1;
        } else if (stringExtra.equals("+IPHONEACCEV")) {
            b = 1;
        }
        switch (b) {
            case 0:
                batteryLevelFromXEventVsc = getBatteryLevelFromXEventVsc(objArr);
                break;
            case 1:
                batteryLevelFromXEventVsc = getBatteryLevelFromAppleBatteryVsc(objArr);
                break;
            default:
                batteryLevelFromXEventVsc = -1;
                break;
        }
        if (batteryLevelFromXEventVsc != -1) {
            updateBatteryLevel(bluetoothDevice, batteryLevelFromXEventVsc);
            infoLog("Updated device " + bluetoothDevice + " battery level to " + String.valueOf(batteryLevelFromXEventVsc) + "%");
        }
    }

    @VisibleForTesting
    static int getBatteryLevelFromAppleBatteryVsc(Object[] objArr) {
        int iIntValue;
        if (objArr.length == 0) {
            Log.w(TAG, "getBatteryLevelFromAppleBatteryVsc() empty arguments");
            return -1;
        }
        int i = 0;
        if (objArr[0] instanceof Integer) {
            int iIntValue2 = ((Integer) objArr[0]).intValue();
            if (objArr.length == (iIntValue2 * 2) + 1) {
                while (true) {
                    if (i < iIntValue2) {
                        int i2 = 2 * i;
                        Integer num = objArr[i2 + 1];
                        if (!(num instanceof Integer)) {
                            Log.w(TAG, "getBatteryLevelFromAppleBatteryVsc() error parsing indicator type");
                            return -1;
                        }
                        if (num.intValue() != 1) {
                            i++;
                        } else {
                            Integer num2 = objArr[i2 + 2];
                            if (!(num2 instanceof Integer)) {
                                Log.w(TAG, "getBatteryLevelFromAppleBatteryVsc() error parsing indicator value");
                                return -1;
                            }
                            iIntValue = num2.intValue();
                        }
                    } else {
                        iIntValue = -1;
                        break;
                    }
                }
            } else {
                Log.w(TAG, "getBatteryLevelFromAppleBatteryVsc() number of arguments does not match");
                return -1;
            }
        } else {
            Log.w(TAG, "getBatteryLevelFromAppleBatteryVsc() error parsing number of arguments");
            return -1;
        }
    }

    @VisibleForTesting
    static int getBatteryLevelFromXEventVsc(Object[] objArr) {
        if (objArr.length == 0) {
            Log.w(TAG, "getBatteryLevelFromXEventVsc() empty arguments");
            return -1;
        }
        String str = objArr[0];
        if (!(str instanceof String)) {
            Log.w(TAG, "getBatteryLevelFromXEventVsc() error parsing event name");
            return -1;
        }
        if (!str.equals("BATTERY")) {
            infoLog("getBatteryLevelFromXEventVsc() skip none BATTERY event: " + str);
            return -1;
        }
        if (objArr.length != 5) {
            Log.w(TAG, "getBatteryLevelFromXEventVsc() wrong battery level event length: " + String.valueOf(objArr.length));
            return -1;
        }
        if (!(objArr[1] instanceof Integer) || !(objArr[2] instanceof Integer)) {
            Log.w(TAG, "getBatteryLevelFromXEventVsc() error parsing event values");
            return -1;
        }
        int iIntValue = ((Integer) objArr[1]).intValue();
        int iIntValue2 = ((Integer) objArr[2]).intValue();
        if (iIntValue < 0 || iIntValue2 < 0 || iIntValue > iIntValue2) {
            Log.w(TAG, "getBatteryLevelFromXEventVsc() wrong event value, batteryLevel=" + String.valueOf(iIntValue) + ", numberOfLevels=" + String.valueOf(iIntValue2));
            return -1;
        }
        return (iIntValue * 100) / iIntValue2;
    }

    private static void errorLog(String str) {
        Log.e(TAG, str);
    }

    private static void debugLog(String str) {
        if (DBG) {
            Log.d(TAG, str);
        }
    }

    private static void infoLog(String str) {
        if (DBG) {
            Log.i(TAG, str);
        }
    }

    private static void warnLog(String str) {
        Log.w(TAG, str);
    }
}
