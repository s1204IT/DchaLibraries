package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.ParcelUuid;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.RemoteDevices;
import com.android.bluetooth.hfp.BluetoothCmeError;
import com.android.vcard.VCardConfig;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class AdapterProperties {
    private static final int BD_ADDR_LEN = 6;
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothAdapterProperties";
    private static final boolean VDBG = false;
    private byte[] mAddress;
    private int mBluetoothClass;
    private int mDiscoverableTimeout;
    private boolean mDiscovering;
    private boolean mIsActivityAndEnergyReporting;
    private String mName;
    private int mNumOfAdvertisementInstancesSupported;
    private int mNumOfOffloadedIrkSupported;
    private int mNumOfOffloadedScanFilterSupported;
    private int mOffloadedScanResultStorageBytes;
    private HashMap<Integer, Pair<Integer, Integer>> mProfileConnectionState;
    private int mProfilesConnected;
    private int mProfilesConnecting;
    private int mProfilesDisconnecting;
    private RemoteDevices mRemoteDevices;
    private boolean mRpaOffloadSupported;
    private int mScanMode;
    private AdapterService mService;
    private ParcelUuid[] mUuids;
    private CopyOnWriteArrayList<BluetoothDevice> mBondedDevices = new CopyOnWriteArrayList<>();
    private int mConnectionState = 0;
    private int mState = 10;
    private Object mObject = new Object();
    private boolean mBluetoothDisabling = false;
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    public AdapterProperties(AdapterService service) {
        this.mService = service;
    }

    public void init(RemoteDevices remoteDevices) {
        if (this.mProfileConnectionState == null) {
            this.mProfileConnectionState = new HashMap<>();
        } else {
            this.mProfileConnectionState.clear();
        }
        this.mRemoteDevices = remoteDevices;
    }

    public void cleanup() {
        this.mRemoteDevices = null;
        if (this.mProfileConnectionState != null) {
            this.mProfileConnectionState.clear();
            this.mProfileConnectionState = null;
        }
        this.mService = null;
        if (!this.mBondedDevices.isEmpty()) {
            this.mBondedDevices.clear();
        }
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    String getName() {
        String str;
        synchronized (this.mObject) {
            str = this.mName;
        }
        return str;
    }

    boolean setName(String name) {
        boolean adapterPropertyNative;
        synchronized (this.mObject) {
            adapterPropertyNative = this.mService.setAdapterPropertyNative(1, name.getBytes());
        }
        return adapterPropertyNative;
    }

    int getBluetoothClass() {
        int i;
        synchronized (this.mObject) {
            i = this.mBluetoothClass;
        }
        return i;
    }

    int getScanMode() {
        int i;
        synchronized (this.mObject) {
            i = this.mScanMode;
        }
        return i;
    }

    boolean setScanMode(int scanMode) {
        boolean adapterPropertyNative;
        synchronized (this.mObject) {
            adapterPropertyNative = this.mService.setAdapterPropertyNative(7, Utils.intToByteArray(scanMode));
        }
        return adapterPropertyNative;
    }

    ParcelUuid[] getUuids() {
        ParcelUuid[] parcelUuidArr;
        synchronized (this.mObject) {
            parcelUuidArr = this.mUuids;
        }
        return parcelUuidArr;
    }

    boolean setUuids(ParcelUuid[] uuids) {
        boolean adapterPropertyNative;
        synchronized (this.mObject) {
            adapterPropertyNative = this.mService.setAdapterPropertyNative(3, Utils.uuidsToByteArray(uuids));
        }
        return adapterPropertyNative;
    }

    byte[] getAddress() {
        byte[] bArr;
        synchronized (this.mObject) {
            bArr = this.mAddress;
        }
        return bArr;
    }

    void setConnectionState(int mConnectionState) {
        synchronized (this.mObject) {
            this.mConnectionState = mConnectionState;
        }
    }

    int getConnectionState() {
        int i;
        synchronized (this.mObject) {
            i = this.mConnectionState;
        }
        return i;
    }

    void setState(int mState) {
        synchronized (this.mObject) {
            debugLog("Setting state to " + mState);
            this.mState = mState;
        }
    }

    int getState() {
        return this.mState;
    }

    int getNumOfAdvertisementInstancesSupported() {
        return this.mNumOfAdvertisementInstancesSupported;
    }

    boolean isRpaOffloadSupported() {
        return this.mRpaOffloadSupported;
    }

    int getNumOfOffloadedIrkSupported() {
        return this.mNumOfOffloadedIrkSupported;
    }

    int getNumOfOffloadedScanFilterSupported() {
        return this.mNumOfOffloadedScanFilterSupported;
    }

    int getOffloadedScanResultStorage() {
        return this.mOffloadedScanResultStorageBytes;
    }

    boolean isActivityAndEnergyReportingSupported() {
        return this.mIsActivityAndEnergyReporting;
    }

    BluetoothDevice[] getBondedDevices() {
        BluetoothDevice[] bondedDeviceList = new BluetoothDevice[0];
        synchronized (this.mObject) {
            if (this.mBondedDevices.isEmpty()) {
                return new BluetoothDevice[0];
            }
            try {
                BluetoothDevice[] bondedDeviceList2 = (BluetoothDevice[]) this.mBondedDevices.toArray(bondedDeviceList);
                infoLog("getBondedDevices: length=" + bondedDeviceList2.length);
                return bondedDeviceList2;
            } catch (ArrayStoreException e) {
                errorLog("Error retrieving bonded device array");
                return new BluetoothDevice[0];
            }
        }
    }

    void onBondStateChanged(BluetoothDevice device, int state) {
        if (device != null) {
            try {
                byte[] addrByte = Utils.getByteAddress(device);
                RemoteDevices.DeviceProperties prop = this.mRemoteDevices.getDeviceProperties(device);
                if (prop == null) {
                    prop = this.mRemoteDevices.addDeviceProperties(addrByte);
                }
                prop.setBondState(state);
                if (state == 12) {
                    if (!this.mBondedDevices.contains(device)) {
                        debugLog("Adding bonded device:" + device);
                        this.mBondedDevices.add(device);
                        return;
                    }
                    return;
                }
                if (state == 10) {
                    if (this.mBondedDevices.remove(device)) {
                        debugLog("Removing bonded device:" + device);
                    } else {
                        debugLog("Failed to remove device: " + device);
                    }
                }
            } catch (Exception ee) {
                Log.e(TAG, "Exception in onBondStateChanged : ", ee);
            }
        }
    }

    int getDiscoverableTimeout() {
        int i;
        synchronized (this.mObject) {
            i = this.mDiscoverableTimeout;
        }
        return i;
    }

    boolean setDiscoverableTimeout(int timeout) {
        boolean adapterPropertyNative;
        synchronized (this.mObject) {
            adapterPropertyNative = this.mService.setAdapterPropertyNative(9, Utils.intToByteArray(timeout));
        }
        return adapterPropertyNative;
    }

    int getProfileConnectionState(int profile) {
        int iIntValue;
        synchronized (this.mObject) {
            Pair<Integer, Integer> p = this.mProfileConnectionState.get(Integer.valueOf(profile));
            iIntValue = p != null ? ((Integer) p.first).intValue() : 0;
        }
        return iIntValue;
    }

    boolean isDiscovering() {
        boolean z;
        synchronized (this.mObject) {
            z = this.mDiscovering;
        }
        return z;
    }

    void sendConnectionStateChange(BluetoothDevice device, int profile, int state, int prevState) {
        if (!validateProfileConnectionState(state) || !validateProfileConnectionState(prevState)) {
            errorLog("Error in sendConnectionStateChange: prevState " + prevState + " state " + state);
            return;
        }
        synchronized (this.mObject) {
            updateProfileConnectionState(profile, state, prevState);
            if (updateCountersAndCheckForConnectionStateChange(state, prevState)) {
                setConnectionState(state);
                Intent intent = new Intent("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
                intent.putExtra("android.bluetooth.device.extra.DEVICE", device);
                intent.putExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", convertToAdapterState(state));
                intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_CONNECTION_STATE", convertToAdapterState(prevState));
                intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
                AdapterService adapterService = this.mService;
                UserHandle userHandle = UserHandle.ALL;
                AdapterService adapterService2 = this.mService;
                adapterService.sendBroadcastAsUser(intent, userHandle, ProfileService.BLUETOOTH_PERM);
                Log.d(TAG, "CONNECTION_STATE_CHANGE: " + device + ": " + prevState + " -> " + state);
            }
        }
    }

    private boolean validateProfileConnectionState(int state) {
        return state == 0 || state == 1 || state == 2 || state == 3;
    }

    private int convertToAdapterState(int state) {
        switch (state) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            default:
                Log.e(TAG, "Error in convertToAdapterState");
                return -1;
        }
    }

    private boolean updateCountersAndCheckForConnectionStateChange(int state, int prevState) {
        switch (prevState) {
            case 1:
                this.mProfilesConnecting--;
                break;
            case 2:
                this.mProfilesConnected--;
                break;
            case 3:
                this.mProfilesDisconnecting--;
                break;
        }
        switch (state) {
            case 0:
                if (this.mProfilesConnected != 0 || this.mProfilesConnecting != 0) {
                }
                break;
            case 1:
                this.mProfilesConnecting++;
                if (this.mProfilesConnected != 0 || this.mProfilesConnecting != 1) {
                }
                break;
            case 2:
                this.mProfilesConnected++;
                if (this.mProfilesConnected != 1) {
                }
                break;
            case 3:
                this.mProfilesDisconnecting++;
                if (this.mProfilesConnected != 0 || this.mProfilesDisconnecting != 1) {
                }
                break;
        }
        return true;
    }

    private void updateProfileConnectionState(int profile, int newState, int oldState) {
        int numDev = 1;
        int newHashState = newState;
        boolean update = true;
        Pair<Integer, Integer> stateNumDev = this.mProfileConnectionState.get(Integer.valueOf(profile));
        if (stateNumDev != null) {
            int currHashState = ((Integer) stateNumDev.first).intValue();
            numDev = ((Integer) stateNumDev.second).intValue();
            if (newState == currHashState) {
                numDev++;
            } else if (newState == 2 || (newState == 1 && currHashState != 2)) {
                numDev = 1;
            } else if (numDev == 1 && oldState == currHashState) {
                update = true;
            } else if (numDev > 1 && oldState == currHashState) {
                numDev--;
                if (currHashState == 2 || currHashState == 1) {
                    newHashState = currHashState;
                }
            } else {
                update = false;
            }
        }
        if (update) {
            this.mProfileConnectionState.put(Integer.valueOf(profile), new Pair<>(Integer.valueOf(newHashState), Integer.valueOf(numDev)));
        }
    }

    void adapterPropertyChangedCallback(int[] types, byte[][] values) {
        for (int i = 0; i < types.length; i++) {
            byte[] val = values[i];
            int type = types[i];
            infoLog("adapterPropertyChangedCallback with type:" + type + " len:" + val.length);
            synchronized (this.mObject) {
                switch (type) {
                    case 1:
                        this.mName = new String(val);
                        Intent intent = new Intent("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
                        intent.putExtra("android.bluetooth.adapter.extra.LOCAL_NAME", this.mName);
                        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
                        AdapterService adapterService = this.mService;
                        UserHandle userHandle = UserHandle.ALL;
                        AdapterService adapterService2 = this.mService;
                        adapterService.sendBroadcastAsUser(intent, userHandle, ProfileService.BLUETOOTH_PERM);
                        debugLog("Name is: " + this.mName);
                        break;
                    case 2:
                        this.mAddress = val;
                        debugLog("Address is:" + Utils.getAddressStringFromByte(this.mAddress));
                        break;
                    case 3:
                        this.mUuids = Utils.byteArrayToUuid(val);
                        break;
                    case 4:
                        this.mBluetoothClass = Utils.byteArrayToInt(val, 0);
                        debugLog("BT Class:" + this.mBluetoothClass);
                        break;
                    case 5:
                    case 6:
                    case 10:
                    case 11:
                    case 12:
                    default:
                        errorLog("Property change not handled in Java land:" + type);
                        break;
                    case AbstractionLayer.BT_STATUS_PARM_INVALID:
                        int mode = Utils.byteArrayToInt(val, 0);
                        AdapterService adapterService3 = this.mService;
                        this.mScanMode = AdapterService.convertScanModeFromHal(mode);
                        Intent intent2 = new Intent("android.bluetooth.adapter.action.SCAN_MODE_CHANGED");
                        intent2.putExtra("android.bluetooth.adapter.extra.SCAN_MODE", this.mScanMode);
                        intent2.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
                        AdapterService adapterService4 = this.mService;
                        AdapterService adapterService5 = this.mService;
                        adapterService4.sendBroadcast(intent2, ProfileService.BLUETOOTH_PERM);
                        debugLog("Scan Mode:" + this.mScanMode);
                        if (this.mBluetoothDisabling) {
                            this.mBluetoothDisabling = false;
                            this.mService.startBluetoothDisable();
                        }
                        break;
                    case 8:
                        int number = val.length / 6;
                        byte[] addrByte = new byte[6];
                        for (int j = 0; j < number; j++) {
                            System.arraycopy(val, j * 6, addrByte, 0, 6);
                            onBondStateChanged(this.mAdapter.getRemoteDevice(Utils.getAddressStringFromByte(addrByte)), 12);
                        }
                        break;
                    case AbstractionLayer.BT_STATUS_AUTH_FAILURE:
                        this.mDiscoverableTimeout = Utils.byteArrayToInt(val, 0);
                        debugLog("Discoverable Timeout:" + this.mDiscoverableTimeout);
                        break;
                    case BluetoothCmeError.SIM_FAILURE:
                        updateFeatureSupport(val);
                        break;
                }
            }
        }
    }

    void updateFeatureSupport(byte[] val) {
        this.mNumOfAdvertisementInstancesSupported = val[1] & 255;
        this.mRpaOffloadSupported = (val[2] & 255) != 0;
        this.mNumOfOffloadedIrkSupported = val[3] & 255;
        this.mNumOfOffloadedScanFilterSupported = val[4] & 255;
        this.mOffloadedScanResultStorageBytes = ((val[6] & 255) << 8) + (val[5] & 255);
        this.mIsActivityAndEnergyReporting = (val[7] & 255) != 0;
        Log.d(TAG, "BT_PROPERTY_LOCAL_LE_FEATURES: update from BT controller mNumOfAdvertisementInstancesSupported = " + this.mNumOfAdvertisementInstancesSupported + " mRpaOffloadSupported = " + this.mRpaOffloadSupported + " mNumOfOffloadedIrkSupported = " + this.mNumOfOffloadedIrkSupported + " mNumOfOffloadedScanFilterSupported = " + this.mNumOfOffloadedScanFilterSupported + " mOffloadedScanResultStorageBytes= " + this.mOffloadedScanResultStorageBytes + " mIsActivityAndEnergyReporting = " + this.mIsActivityAndEnergyReporting);
    }

    void onBluetoothReady() {
        Log.d(TAG, "ScanMode =  " + this.mScanMode);
        Log.d(TAG, "State =  " + getState());
        synchronized (this.mObject) {
            if (getState() == 11 && this.mScanMode == 20) {
                if (this.mDiscoverableTimeout != 0) {
                    setScanMode(1);
                } else {
                    setScanMode(2);
                }
                setDiscoverableTimeout(this.mDiscoverableTimeout);
            }
        }
    }

    void onBluetoothDisable() {
        debugLog("onBluetoothDisable()");
        this.mBluetoothDisabling = true;
        if (getState() == 13) {
            setScanMode(0);
        }
    }

    void discoveryStateChangeCallback(int state) {
        infoLog("Callback:discoveryStateChangeCallback with state:" + state);
        synchronized (this.mObject) {
            if (state == 0) {
                this.mDiscovering = false;
                Intent intent = new Intent("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
                AdapterService adapterService = this.mService;
                AdapterService adapterService2 = this.mService;
                adapterService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
            } else if (state == 1) {
                this.mDiscovering = true;
                Intent intent2 = new Intent("android.bluetooth.adapter.action.DISCOVERY_STARTED");
                AdapterService adapterService3 = this.mService;
                AdapterService adapterService4 = this.mService;
                adapterService3.sendBroadcast(intent2, ProfileService.BLUETOOTH_PERM);
            }
        }
    }

    private void infoLog(String msg) {
    }

    private void debugLog(String msg) {
        Log.d(TAG, msg);
    }

    private void errorLog(String msg) {
        Log.e(TAG, msg);
    }
}
