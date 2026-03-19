package com.android.bluetooth.btservice;

import android.R;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.util.Pair;
import android.util.StatsLog;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.RemoteDevices;
import com.android.vcard.VCardConfig;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class AdapterProperties {
    private static final String A2DP_OFFLOAD_DISABLED_PROPERTY = "persist.bluetooth.a2dp_offload.disabled";
    private static final String A2DP_OFFLOAD_SUPPORTED_PROPERTY = "ro.bluetooth.a2dp_offload.supported";
    private static final int BD_ADDR_LEN = 6;
    private static final long DEFAULT_DISCOVERY_TIMEOUT_MS = 12800;
    static final int MAX_CONNECTED_AUDIO_DEVICES_LOWER_BOND = 1;
    private static final String MAX_CONNECTED_AUDIO_DEVICES_PROPERTY = "persist.bluetooth.maxconnectedaudiodevices";
    private static final int MAX_CONNECTED_AUDIO_DEVICES_UPPER_BOUND = 5;
    private static final String TAG = "AdapterProperties";
    private volatile byte[] mAddress;
    private volatile BluetoothClass mBluetoothClass;
    private volatile int mDiscoverableTimeout;
    private boolean mDiscovering;
    private long mDiscoveryEndMs;
    private boolean mIsActivityAndEnergyReporting;
    private boolean mIsDebugLogSupported;
    private boolean mIsExtendedScanSupported;
    private boolean mIsLe2MPhySupported;
    private boolean mIsLeCodedPhySupported;
    private boolean mIsLeExtendedAdvertisingSupported;
    private boolean mIsLePeriodicAdvertisingSupported;
    private int mLeMaximumAdvertisingDataLength;
    private volatile String mName;
    private int mNumOfAdvertisementInstancesSupported;
    private int mNumOfOffloadedIrkSupported;
    private int mNumOfOffloadedScanFilterSupported;
    private int mOffloadedScanResultStorageBytes;
    private int mProfilesConnected;
    private int mProfilesConnecting;
    private int mProfilesDisconnecting;
    private boolean mReceiverRegistered;
    private RemoteDevices mRemoteDevices;
    private boolean mRpaOffloadSupported;
    private volatile int mScanMode;
    private AdapterService mService;
    private int mTotNumOfTrackableAdv;
    private volatile ParcelUuid[] mUuids;
    private int mVersSupported;
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final boolean VDBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private CopyOnWriteArrayList<BluetoothDevice> mBondedDevices = new CopyOnWriteArrayList<>();
    private final HashMap<Integer, Pair<Integer, Integer>> mProfileConnectionState = new HashMap<>();
    private volatile int mConnectionState = 0;
    private volatile int mState = 10;
    private int mMaxConnectedAudioDevices = 1;
    private boolean mA2dpOffloadEnabled = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.w(AdapterProperties.TAG, "Received intent with null action");
            }
            switch (action) {
                case "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(1, intent);
                    break;
                case "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(2, intent);
                    break;
                case "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(16, intent);
                    break;
                case "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(11, intent);
                    break;
                case "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(19, intent);
                    break;
                case "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(4, intent);
                    break;
                case "android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(12, intent);
                    break;
                case "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(5, intent);
                    break;
                case "android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(9, intent);
                    break;
                case "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(18, intent);
                    break;
                case "android.bluetooth.sap.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(10, intent);
                    break;
                case "android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(17, intent);
                    break;
                case "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED":
                    AdapterProperties.this.sendConnectionStateChange(6, intent);
                    break;
                default:
                    Log.w(AdapterProperties.TAG, "Received unknown intent " + intent);
                    break;
            }
        }
    };
    private final Object mObject = new Object();
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    AdapterProperties(AdapterService adapterService) {
        this.mService = adapterService;
    }

    public void init(RemoteDevices remoteDevices) {
        this.mProfileConnectionState.clear();
        this.mRemoteDevices = remoteDevices;
        int integer = this.mService.getResources().getInteger(R.integer.config_activityShortDur);
        int i = SystemProperties.getInt(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, integer);
        this.mMaxConnectedAudioDevices = Math.min(Math.max(i, 1), 5);
        Log.i(TAG, "init(), maxConnectedAudioDevices, default=" + integer + ", propertyOverlayed=" + i + ", finalValue=" + this.mMaxConnectedAudioDevices);
        boolean z = false;
        if (SystemProperties.getBoolean(A2DP_OFFLOAD_SUPPORTED_PROPERTY, false) && !SystemProperties.getBoolean(A2DP_OFFLOAD_DISABLED_PROPERTY, false)) {
            z = true;
        }
        this.mA2dpOffloadEnabled = z;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.sap.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED");
        if (this.mService != null) {
            this.mService.registerReceiver(this.mReceiver, intentFilter);
            this.mReceiverRegistered = true;
        }
    }

    public void cleanup() {
        this.mRemoteDevices = null;
        this.mProfileConnectionState.clear();
        if (this.mReceiverRegistered) {
            this.mService.unregisterReceiver(this.mReceiver);
            this.mReceiverRegistered = false;
        }
        this.mService = null;
        this.mBondedDevices.clear();
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    String getName() {
        return this.mName;
    }

    boolean setName(String str) {
        boolean adapterPropertyNative;
        synchronized (this.mObject) {
            adapterPropertyNative = this.mService.setAdapterPropertyNative(1, str.getBytes());
        }
        return adapterPropertyNative;
    }

    boolean setBluetoothClass(BluetoothClass bluetoothClass) {
        boolean adapterPropertyNative;
        synchronized (this.mObject) {
            adapterPropertyNative = this.mService.setAdapterPropertyNative(4, bluetoothClass.getClassOfDeviceBytes());
            if (adapterPropertyNative) {
                this.mBluetoothClass = bluetoothClass;
            }
        }
        return adapterPropertyNative;
    }

    BluetoothClass getBluetoothClass() {
        BluetoothClass bluetoothClass;
        synchronized (this.mObject) {
            bluetoothClass = this.mBluetoothClass;
        }
        return bluetoothClass;
    }

    int getScanMode() {
        return this.mScanMode;
    }

    boolean setScanMode(int i) {
        synchronized (this.mObject) {
            if (this.mService != null) {
                return this.mService.setAdapterPropertyNative(7, Utils.intToByteArray(i));
            }
            return false;
        }
    }

    ParcelUuid[] getUuids() {
        return this.mUuids;
    }

    byte[] getAddress() {
        return this.mAddress;
    }

    void setConnectionState(int i) {
        this.mConnectionState = i;
    }

    int getConnectionState() {
        return this.mConnectionState;
    }

    void setState(int i) {
        debugLog("Setting state to " + BluetoothAdapter.nameForState(i));
        this.mState = i;
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

    boolean isLe2MPhySupported() {
        return this.mIsLe2MPhySupported;
    }

    boolean isLeCodedPhySupported() {
        return this.mIsLeCodedPhySupported;
    }

    boolean isLeExtendedAdvertisingSupported() {
        return this.mIsLeExtendedAdvertisingSupported;
    }

    boolean isLePeriodicAdvertisingSupported() {
        return this.mIsLePeriodicAdvertisingSupported;
    }

    int getLeMaximumAdvertisingDataLength() {
        return this.mLeMaximumAdvertisingDataLength;
    }

    int getTotalNumOfTrackableAdvertisements() {
        return this.mTotNumOfTrackableAdv;
    }

    int getMaxConnectedAudioDevices() {
        return this.mMaxConnectedAudioDevices;
    }

    boolean isA2dpOffloadEnabled() {
        return this.mA2dpOffloadEnabled;
    }

    BluetoothDevice[] getBondedDevices() {
        BluetoothDevice[] bluetoothDeviceArr = new BluetoothDevice[0];
        try {
            bluetoothDeviceArr = (BluetoothDevice[]) this.mBondedDevices.toArray(bluetoothDeviceArr);
        } catch (ArrayStoreException e) {
            errorLog("Error retrieving bonded device array");
        }
        infoLog("getBondedDevices: length=" + bluetoothDeviceArr.length);
        return bluetoothDeviceArr;
    }

    @VisibleForTesting
    void onBondStateChanged(BluetoothDevice bluetoothDevice, int i) {
        if (bluetoothDevice == null) {
            Log.w(TAG, "onBondStateChanged, device is null");
            return;
        }
        try {
            byte[] byteAddress = Utils.getByteAddress(bluetoothDevice);
            RemoteDevices.DeviceProperties deviceProperties = this.mRemoteDevices.getDeviceProperties(bluetoothDevice);
            if (deviceProperties == null) {
                deviceProperties = this.mRemoteDevices.addDeviceProperties(byteAddress);
            }
            deviceProperties.setBondState(i);
            if (i == 12) {
                if (!this.mBondedDevices.contains(bluetoothDevice)) {
                    debugLog("Adding bonded device:" + bluetoothDevice);
                    this.mBondedDevices.add(bluetoothDevice);
                    return;
                }
                return;
            }
            if (i == 10) {
                if (this.mBondedDevices.remove(bluetoothDevice)) {
                    debugLog("Removing bonded device:" + bluetoothDevice);
                    return;
                }
                debugLog("Failed to remove device: " + bluetoothDevice);
            }
        } catch (Exception e) {
            Log.w(TAG, "onBondStateChanged: Exception ", e);
        }
    }

    int getDiscoverableTimeout() {
        return this.mDiscoverableTimeout;
    }

    boolean setDiscoverableTimeout(int i) {
        synchronized (this.mObject) {
            if (this.mService != null) {
                return this.mService.setAdapterPropertyNative(9, Utils.intToByteArray(i));
            }
            return false;
        }
    }

    int getProfileConnectionState(int i) {
        synchronized (this.mObject) {
            Pair<Integer, Integer> pair = this.mProfileConnectionState.get(Integer.valueOf(i));
            if (pair != null) {
                return ((Integer) pair.first).intValue();
            }
            return 0;
        }
    }

    long discoveryEndMillis() {
        return this.mDiscoveryEndMs;
    }

    boolean isDiscovering() {
        return this.mDiscovering;
    }

    private void sendConnectionStateChange(int i, Intent intent) {
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1);
        int intExtra2 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
        Log.d(TAG, "PROFILE_CONNECTION_STATE_CHANGE: profile=" + i + ", device=" + bluetoothDevice + ", " + intExtra + " -> " + intExtra2);
        String string = Settings.Secure.getString(this.mService.getContentResolver(), "android_id");
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        sb.append(bluetoothDevice.getAddress());
        StatsLog.write(68, intExtra2, sb.toString().hashCode() & 65535, i);
        if (!isNormalStateTransition(intExtra, intExtra2)) {
            Log.w(TAG, "PROFILE_CONNECTION_STATE_CHANGE: unexpected transition for profile=" + i + ", device=" + bluetoothDevice + ", " + intExtra + " -> " + intExtra2);
        }
        sendConnectionStateChange(bluetoothDevice, i, intExtra2, intExtra);
    }

    void sendConnectionStateChange(BluetoothDevice bluetoothDevice, int i, int i2, int i3) {
        if (!validateProfileConnectionState(i2) || !validateProfileConnectionState(i3)) {
            errorLog("sendConnectionStateChange: invalid state transition " + i3 + " -> " + i2);
            return;
        }
        synchronized (this.mObject) {
            updateProfileConnectionState(i, i2, i3);
            if (updateCountersAndCheckForConnectionStateChange(i2, i3)) {
                int iConvertToAdapterState = convertToAdapterState(i2);
                int iConvertToAdapterState2 = convertToAdapterState(i3);
                setConnectionState(iConvertToAdapterState);
                Intent intent = new Intent("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
                intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
                intent.putExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", iConvertToAdapterState);
                intent.putExtra("android.bluetooth.adapter.extra.PREVIOUS_CONNECTION_STATE", iConvertToAdapterState2);
                intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
                Log.d(TAG, "ADAPTER_CONNECTION_STATE_CHANGE: " + bluetoothDevice + ": " + iConvertToAdapterState2 + " -> " + iConvertToAdapterState);
                if (!isNormalStateTransition(i3, i2)) {
                    Log.w(TAG, "ADAPTER_CONNECTION_STATE_CHANGE: unexpected transition for profile=" + i + ", device=" + bluetoothDevice + ", " + i3 + " -> " + i2);
                }
                this.mService.sendBroadcastAsUser(intent, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
            }
        }
    }

    private boolean validateProfileConnectionState(int i) {
        return i == 0 || i == 1 || i == 2 || i == 3;
    }

    private static int convertToAdapterState(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            default:
                Log.e(TAG, "convertToAdapterState, unknow state " + i);
                return -1;
        }
    }

    private static boolean isNormalStateTransition(int i, int i2) {
        switch (i) {
            case 0:
                if (i2 != 1) {
                    break;
                }
                break;
            case 1:
            case 3:
                if (i2 != 0 && i2 != 2) {
                    break;
                }
                break;
            case 2:
                if (i2 != 3) {
                    break;
                }
                break;
        }
        return false;
    }

    private boolean updateCountersAndCheckForConnectionStateChange(int i, int i2) {
        switch (i2) {
            case 1:
                if (this.mProfilesConnecting <= 0) {
                    Log.e(TAG, "mProfilesConnecting " + this.mProfilesConnecting);
                    throw new IllegalStateException("Invalid state transition, " + i2 + " -> " + i);
                }
                this.mProfilesConnecting--;
                break;
            case 2:
                if (this.mProfilesConnected <= 0) {
                    Log.e(TAG, "mProfilesConnected " + this.mProfilesConnected);
                    throw new IllegalStateException("Invalid state transition, " + i2 + " -> " + i);
                }
                this.mProfilesConnected--;
                break;
            case 3:
                if (this.mProfilesDisconnecting <= 0) {
                    Log.e(TAG, "mProfilesDisconnecting " + this.mProfilesDisconnecting);
                    throw new IllegalStateException("Invalid state transition, " + i2 + " -> " + i);
                }
                this.mProfilesDisconnecting--;
                break;
        }
        switch (i) {
            case 0:
                if (this.mProfilesConnected == 0 && this.mProfilesConnecting == 0) {
                    return true;
                }
                return false;
            case 1:
                this.mProfilesConnecting++;
                if (this.mProfilesConnected == 0 && this.mProfilesConnecting == 1) {
                    return true;
                }
                return false;
            case 2:
                this.mProfilesConnected++;
                if (this.mProfilesConnected == 1) {
                    return true;
                }
                return false;
            case 3:
                this.mProfilesDisconnecting++;
                if (this.mProfilesConnected == 0 && this.mProfilesDisconnecting == 1) {
                    return true;
                }
                return false;
            default:
                return true;
        }
    }

    private void updateProfileConnectionState(int i, int i2, int i3) {
        int i4;
        Pair<Integer, Integer> pair = this.mProfileConnectionState.get(Integer.valueOf(i));
        boolean z = true;
        if (pair != null) {
            int iIntValue = ((Integer) pair.first).intValue();
            int iIntValue2 = ((Integer) pair.second).intValue();
            if (i2 == iIntValue) {
                i4 = iIntValue2 + 1;
            } else if (i2 != 2 && (i2 != 1 || iIntValue == 2)) {
                if (iIntValue2 != 1 || i3 != iIntValue) {
                    if (iIntValue2 > 1 && i3 == iIntValue) {
                        i4 = iIntValue2 - 1;
                        if (iIntValue == 2 || iIntValue == 1) {
                            i2 = iIntValue;
                        }
                    } else {
                        z = false;
                        i4 = iIntValue2;
                    }
                } else {
                    i4 = iIntValue2;
                }
            } else {
                i4 = 1;
            }
        }
        if (z) {
            this.mProfileConnectionState.put(Integer.valueOf(i), new Pair<>(Integer.valueOf(i2), Integer.valueOf(i4)));
        }
    }

    void adapterPropertyChangedCallback(int[] iArr, byte[][] bArr) {
        for (int i = 0; i < iArr.length; i++) {
            byte[] bArr2 = bArr[i];
            int i2 = iArr[i];
            infoLog("adapterPropertyChangedCallback with type:" + i2 + " len:" + bArr2.length);
            synchronized (this.mObject) {
                if (i2 != 13) {
                    switch (i2) {
                        case 1:
                            this.mName = new String(bArr2);
                            Intent intent = new Intent("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
                            intent.putExtra("android.bluetooth.adapter.extra.LOCAL_NAME", this.mName);
                            intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
                            this.mService.sendBroadcastAsUser(intent, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
                            debugLog("Name is: " + this.mName);
                            break;
                        case 2:
                            this.mAddress = bArr2;
                            String addressStringFromByte = Utils.getAddressStringFromByte(this.mAddress);
                            debugLog("Address is:" + addressStringFromByte);
                            Intent intent2 = new Intent("android.bluetooth.adapter.action.BLUETOOTH_ADDRESS_CHANGED");
                            intent2.putExtra("android.bluetooth.adapter.extra.BLUETOOTH_ADDRESS", addressStringFromByte);
                            intent2.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
                            this.mService.sendBroadcastAsUser(intent2, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
                            break;
                        case 3:
                            this.mUuids = Utils.byteArrayToUuid(bArr2);
                            break;
                        case 4:
                            if (bArr2 != null && bArr2.length == 3) {
                                int i3 = (bArr2[0] << 16) + (bArr2[1] << 8) + bArr2[2];
                                if (i3 != 0) {
                                    this.mBluetoothClass = new BluetoothClass(i3);
                                }
                                debugLog("BT Class:" + this.mBluetoothClass);
                                break;
                            }
                            debugLog("Invalid BT CoD value from stack.");
                            return;
                        default:
                            switch (i2) {
                                case 7:
                                    this.mScanMode = AdapterService.convertScanModeFromHal(Utils.byteArrayToInt(bArr2, 0));
                                    Intent intent3 = new Intent("android.bluetooth.adapter.action.SCAN_MODE_CHANGED");
                                    intent3.putExtra("android.bluetooth.adapter.extra.SCAN_MODE", this.mScanMode);
                                    intent3.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
                                    this.mService.sendBroadcast(intent3, ProfileService.BLUETOOTH_PERM);
                                    debugLog("Scan Mode:" + this.mScanMode);
                                    break;
                                case 8:
                                    int length = bArr2.length / 6;
                                    byte[] bArr3 = new byte[6];
                                    for (int i4 = 0; i4 < length; i4++) {
                                        System.arraycopy(bArr2, i4 * 6, bArr3, 0, 6);
                                        onBondStateChanged(this.mAdapter.getRemoteDevice(Utils.getAddressStringFromByte(bArr3)), 12);
                                    }
                                    break;
                                case 9:
                                    this.mDiscoverableTimeout = Utils.byteArrayToInt(bArr2, 0);
                                    debugLog("Discoverable Timeout:" + this.mDiscoverableTimeout);
                                    break;
                                default:
                                    try {
                                        errorLog("Property change not handled in Java land:" + i2);
                                    } catch (Throwable th) {
                                        throw th;
                                    }
                                    break;
                            }
                            break;
                    }
                } else {
                    updateFeatureSupport(bArr2);
                }
            }
        }
    }

    private void updateFeatureSupport(byte[] bArr) {
        this.mVersSupported = ((bArr[1] & 255) << 8) + (bArr[0] & 255);
        this.mNumOfAdvertisementInstancesSupported = bArr[3] & 255;
        this.mRpaOffloadSupported = (bArr[4] & 255) != 0;
        this.mNumOfOffloadedIrkSupported = bArr[5] & 255;
        this.mNumOfOffloadedScanFilterSupported = bArr[6] & 255;
        this.mIsActivityAndEnergyReporting = (bArr[7] & 255) != 0;
        this.mOffloadedScanResultStorageBytes = ((bArr[9] & 255) << 8) + (bArr[8] & 255);
        this.mTotNumOfTrackableAdv = ((bArr[11] & 255) << 8) + (bArr[10] & 255);
        this.mIsExtendedScanSupported = (bArr[12] & 255) != 0;
        this.mIsDebugLogSupported = (bArr[13] & 255) != 0;
        this.mIsLe2MPhySupported = (bArr[14] & 255) != 0;
        this.mIsLeCodedPhySupported = (bArr[15] & 255) != 0;
        this.mIsLeExtendedAdvertisingSupported = (bArr[16] & 255) != 0;
        this.mIsLePeriodicAdvertisingSupported = (bArr[17] & 255) != 0;
        this.mLeMaximumAdvertisingDataLength = (bArr[18] & 255) + ((bArr[19] & 255) << 8);
        Log.d(TAG, "BT_PROPERTY_LOCAL_LE_FEATURES: update from BT controller mNumOfAdvertisementInstancesSupported = " + this.mNumOfAdvertisementInstancesSupported + " mRpaOffloadSupported = " + this.mRpaOffloadSupported + " mNumOfOffloadedIrkSupported = " + this.mNumOfOffloadedIrkSupported + " mNumOfOffloadedScanFilterSupported = " + this.mNumOfOffloadedScanFilterSupported + " mOffloadedScanResultStorageBytes= " + this.mOffloadedScanResultStorageBytes + " mIsActivityAndEnergyReporting = " + this.mIsActivityAndEnergyReporting + " mVersSupported = " + this.mVersSupported + " mTotNumOfTrackableAdv = " + this.mTotNumOfTrackableAdv + " mIsExtendedScanSupported = " + this.mIsExtendedScanSupported + " mIsDebugLogSupported = " + this.mIsDebugLogSupported + " mIsLe2MPhySupported = " + this.mIsLe2MPhySupported + " mIsLeCodedPhySupported = " + this.mIsLeCodedPhySupported + " mIsLeExtendedAdvertisingSupported = " + this.mIsLeExtendedAdvertisingSupported + " mIsLePeriodicAdvertisingSupported = " + this.mIsLePeriodicAdvertisingSupported + " mLeMaximumAdvertisingDataLength = " + this.mLeMaximumAdvertisingDataLength);
    }

    void onBluetoothReady() {
        debugLog("onBluetoothReady, state=" + BluetoothAdapter.nameForState(getState()) + ", ScanMode=" + this.mScanMode);
        synchronized (this.mObject) {
            if (this.mProfilesConnected == 0 && this.mProfilesConnecting == 0 && this.mProfilesDisconnecting == 0) {
                setConnectionState(0);
                this.mProfileConnectionState.clear();
                this.mProfilesConnected = 0;
                this.mProfilesConnecting = 0;
                this.mProfilesDisconnecting = 0;
            }
            setScanMode(1);
            setDiscoverableTimeout(this.mDiscoverableTimeout);
        }
    }

    void onBleDisable() {
        debugLog("onBleDisable");
        setScanMode(0);
    }

    void onBluetoothDisable() {
        debugLog("onBluetoothDisable()");
        this.mService.cancelDiscovery();
        setScanMode(0);
    }

    void discoveryStateChangeCallback(int i) {
        infoLog("Callback:discoveryStateChangeCallback with state:" + i);
        synchronized (this.mObject) {
            try {
                if (i == 0) {
                    this.mDiscovering = false;
                    this.mDiscoveryEndMs = System.currentTimeMillis();
                    this.mService.sendBroadcast(new Intent("android.bluetooth.adapter.action.DISCOVERY_FINISHED"), ProfileService.BLUETOOTH_PERM);
                } else if (i == 1) {
                    this.mDiscovering = true;
                    this.mDiscoveryEndMs = System.currentTimeMillis() + DEFAULT_DISCOVERY_TIMEOUT_MS;
                    this.mService.sendBroadcast(new Intent("android.bluetooth.adapter.action.DISCOVERY_STARTED"), ProfileService.BLUETOOTH_PERM);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(TAG);
        printWriter.println("  Name: " + getName());
        printWriter.println("  Address: " + Utils.getAddressStringFromByte(this.mAddress));
        printWriter.println("  BluetoothClass: " + getBluetoothClass());
        printWriter.println("  ScanMode: " + dumpScanMode(getScanMode()));
        printWriter.println("  ConnectionState: " + dumpConnectionState(getConnectionState()));
        printWriter.println("  State: " + BluetoothAdapter.nameForState(getState()));
        printWriter.println("  MaxConnectedAudioDevices: " + getMaxConnectedAudioDevices());
        printWriter.println("  A2dpOffloadEnabled: " + this.mA2dpOffloadEnabled);
        printWriter.println("  Discovering: " + this.mDiscovering);
        printWriter.println("  DiscoveryEndMs: " + this.mDiscoveryEndMs);
        printWriter.println("  Bonded devices:");
        for (BluetoothDevice bluetoothDevice : this.mBondedDevices) {
            printWriter.println("    " + bluetoothDevice.getAddress() + " [" + dumpDeviceType(bluetoothDevice.getType()) + "] " + bluetoothDevice.getName());
        }
    }

    private String dumpDeviceType(int i) {
        switch (i) {
            case 0:
                return " ???? ";
            case 1:
                return "BR/EDR";
            case 2:
                return "  LE  ";
            case 3:
                return " DUAL ";
            default:
                return "Invalid device type: " + i;
        }
    }

    private String dumpConnectionState(int i) {
        switch (i) {
            case 0:
                return "STATE_DISCONNECTED";
            case 1:
                return "STATE_CONNECTING";
            case 2:
                return "STATE_CONNECTED";
            case 3:
                return "STATE_DISCONNECTING";
            default:
                return "Unknown Connection State " + i;
        }
    }

    private String dumpScanMode(int i) {
        switch (i) {
            case 20:
                return "SCAN_MODE_NONE";
            case 21:
                return "SCAN_MODE_CONNECTABLE";
            case 22:
            default:
                return "Unknown Scan Mode " + i;
            case 23:
                return "SCAN_MODE_CONNECTABLE_DISCOVERABLE";
        }
    }

    private static void infoLog(String str) {
        if (VDBG) {
            Log.i(TAG, str);
        }
    }

    private static void debugLog(String str) {
        if (DBG) {
            Log.d(TAG, str);
        }
    }

    private static void errorLog(String str) {
        Log.e(TAG, str);
    }
}
