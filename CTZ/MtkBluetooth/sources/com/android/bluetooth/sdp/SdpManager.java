package com.android.bluetooth.sdp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.SdpMasRecord;
import android.bluetooth.SdpMnsRecord;
import android.bluetooth.SdpOppOpsRecord;
import android.bluetooth.SdpPseRecord;
import android.bluetooth.SdpRecord;
import android.bluetooth.SdpSapsRecord;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import java.util.ArrayList;
import java.util.Arrays;

public class SdpManager {
    private static final boolean D = true;
    public static final int MAP_L2CAP_PSM = 4137;
    public static final int MAP_RFCOMM_CHANNEL = 26;
    private static final int MESSAGE_SDP_INTENT = 2;
    public static final int MNS_L2CAP_PSM = 4135;
    public static final int MNS_RFCOMM_CHANNEL = 22;
    public static final int NEXT_L2CAP_CHANNEL = 2;
    public static final int NEXT_RFCOMM_CHANNEL = 1;
    public static final int OPP_L2CAP_PSM = 4131;
    public static final int OPP_RFCOMM_CHANNEL = 12;
    public static final int PBAP_L2CAP_PSM = 4133;
    public static final byte PBAP_REPO_FAVORITES = 8;
    public static final byte PBAP_REPO_LOCAL = 1;
    public static final byte PBAP_REPO_SIM = 2;
    public static final byte PBAP_REPO_SPEED_DAIL = 4;
    public static final int PBAP_RFCOMM_CHANNEL = 19;
    public static final int SAP_RFCOMM_CHANNEL = 16;
    private static final int SDP_INTENT_DELAY = 11000;
    private static final String TAG = "SdpManager";
    private static final boolean V = false;
    private static AdapterService sAdapterService;
    private static boolean sNativeAvailable;
    static SdpSearchTracker sSdpSearchTracker;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 2) {
                SdpSearchInstance sdpSearchInstance = (SdpSearchInstance) message.obj;
                Log.w(SdpManager.TAG, "Search timedout for UUID " + sdpSearchInstance.getUuid());
                synchronized (SdpManager.TRACKER_LOCK) {
                    SdpManager.this.sendSdpIntent(sdpSearchInstance, null, false);
                }
            }
        }
    };
    public static int SDP_CHANNEL = 2;
    static boolean sSearchInProgress = false;
    static final Object TRACKER_LOCK = new Object();
    private static SdpManager sSdpManager = null;

    private static native void classInitNative();

    private native void cleanupNative();

    private native void initializeNative();

    private native int sdpCreateMapMasRecordNative(String str, int i, int i2, int i3, int i4, int i5, int i6);

    private native int sdpCreateMapMnsRecordNative(String str, int i, int i2, int i3, int i4);

    private native int sdpCreateOppOpsRecordNative(String str, int i, int i2, int i3, byte[] bArr);

    private native int sdpCreatePbapPseRecordNative(String str, int i, int i2, int i3, int i4, int i5);

    private native int sdpCreateSapsRecordNative(String str, int i, int i2);

    private native boolean sdpRemoveSdpRecordNative(int i);

    private native boolean sdpSearchNative(byte[] bArr, byte[] bArr2);

    static {
        classInitNative();
    }

    private class SdpSearchInstance {
        private final BluetoothDevice mDevice;
        private boolean mSearching = true;
        private int mStatus;
        private final ParcelUuid mUuid;

        SdpSearchInstance(int i, BluetoothDevice bluetoothDevice, ParcelUuid parcelUuid) {
            this.mStatus = 0;
            this.mDevice = bluetoothDevice;
            this.mUuid = parcelUuid;
            this.mStatus = i;
        }

        public BluetoothDevice getDevice() {
            return this.mDevice;
        }

        public ParcelUuid getUuid() {
            return this.mUuid;
        }

        public int getStatus() {
            return this.mStatus;
        }

        public void setStatus(int i) {
            this.mStatus = i;
        }

        public void startSearch() {
            this.mSearching = true;
            SdpManager.this.mHandler.sendMessageDelayed(SdpManager.this.mHandler.obtainMessage(2, this), 11000L);
        }

        public void stopSearch() {
            if (this.mSearching) {
                SdpManager.this.mHandler.removeMessages(2, this);
            }
            this.mSearching = false;
        }

        public boolean isSearching() {
            return this.mSearching;
        }
    }

    class SdpSearchTracker {
        private final ArrayList<SdpSearchInstance> mList = new ArrayList<>();

        SdpSearchTracker() {
        }

        void clear() {
            this.mList.clear();
        }

        boolean add(SdpSearchInstance sdpSearchInstance) {
            return this.mList.add(sdpSearchInstance);
        }

        boolean remove(SdpSearchInstance sdpSearchInstance) {
            return this.mList.remove(sdpSearchInstance);
        }

        SdpSearchInstance getNext() {
            if (this.mList.size() > 0) {
                return this.mList.get(0);
            }
            return null;
        }

        SdpSearchInstance getSearchInstance(byte[] bArr, byte[] bArr2) {
            String addressStringFromByte = Utils.getAddressStringFromByte(bArr);
            ParcelUuid parcelUuid = Utils.byteArrayToUuid(bArr2)[0];
            for (SdpSearchInstance sdpSearchInstance : this.mList) {
                if (sdpSearchInstance.getDevice().getAddress().equals(addressStringFromByte) && sdpSearchInstance.getUuid().equals(parcelUuid)) {
                    return sdpSearchInstance;
                }
            }
            return null;
        }

        boolean isSearching(BluetoothDevice bluetoothDevice, ParcelUuid parcelUuid) {
            String address = bluetoothDevice.getAddress();
            for (SdpSearchInstance sdpSearchInstance : this.mList) {
                if (sdpSearchInstance.getDevice().getAddress().equals(address) && sdpSearchInstance.getUuid().equals(parcelUuid)) {
                    return sdpSearchInstance.isSearching();
                }
            }
            return false;
        }
    }

    private SdpManager(AdapterService adapterService) {
        sSdpSearchTracker = new SdpSearchTracker();
        sAdapterService = adapterService;
        initializeNative();
        sNativeAvailable = true;
    }

    public static SdpManager init(AdapterService adapterService) {
        sSdpManager = new SdpManager(adapterService);
        return sSdpManager;
    }

    public static SdpManager getDefaultManager() {
        return sSdpManager;
    }

    public void cleanup() {
        if (sSdpSearchTracker != null) {
            synchronized (TRACKER_LOCK) {
                sSdpSearchTracker.clear();
            }
        }
        if (sNativeAvailable) {
            cleanupNative();
            sNativeAvailable = false;
        }
        sSdpManager = null;
    }

    void sdpMasRecordFoundCallback(int i, byte[] bArr, byte[] bArr2, int i2, int i3, int i4, int i5, int i6, int i7, String str, boolean z) {
        synchronized (TRACKER_LOCK) {
            SdpSearchInstance searchInstance = sSdpSearchTracker.getSearchInstance(bArr, bArr2);
            Parcelable sdpMasRecord = null;
            if (searchInstance == null) {
                Log.e(TAG, "sdpRecordFoundCallback: Search instance is NULL");
                return;
            }
            searchInstance.setStatus(i);
            if (i == 0) {
                sdpMasRecord = new SdpMasRecord(i2, i3, i4, i5, i6, i7, str);
            }
            Log.d(TAG, "UUID: " + Arrays.toString(bArr2));
            Log.d(TAG, "UUID in parcel: " + Utils.byteArrayToUuid(bArr2)[0].toString());
            sendSdpIntent(searchInstance, sdpMasRecord, z);
        }
    }

    void sdpMnsRecordFoundCallback(int i, byte[] bArr, byte[] bArr2, int i2, int i3, int i4, int i5, String str, boolean z) {
        SdpMnsRecord sdpMnsRecord;
        synchronized (TRACKER_LOCK) {
            SdpSearchInstance searchInstance = sSdpSearchTracker.getSearchInstance(bArr, bArr2);
            if (searchInstance == null) {
                Log.e(TAG, "sdpRecordFoundCallback: Search instance is NULL");
                return;
            }
            searchInstance.setStatus(i);
            if (i == 0) {
                sdpMnsRecord = new SdpMnsRecord(i2, i3, i4, i5, str);
            } else {
                sdpMnsRecord = null;
            }
            Log.d(TAG, "UUID: " + Arrays.toString(bArr2));
            Log.d(TAG, "UUID in parcel: " + Utils.byteArrayToUuid(bArr2)[0].toString());
            sendSdpIntent(searchInstance, sdpMnsRecord, z);
        }
    }

    void sdpPseRecordFoundCallback(int i, byte[] bArr, byte[] bArr2, int i2, int i3, int i4, int i5, int i6, String str, boolean z) {
        synchronized (TRACKER_LOCK) {
            SdpSearchInstance searchInstance = sSdpSearchTracker.getSearchInstance(bArr, bArr2);
            Parcelable sdpPseRecord = null;
            if (searchInstance == null) {
                Log.e(TAG, "sdpRecordFoundCallback: Search instance is NULL");
                return;
            }
            searchInstance.setStatus(i);
            if (i == 0) {
                sdpPseRecord = new SdpPseRecord(i2, i3, i4, i5, i6, str);
            }
            Log.d(TAG, "UUID: " + Arrays.toString(bArr2));
            Log.d(TAG, "UUID in parcel: " + Utils.byteArrayToUuid(bArr2)[0].toString());
            sendSdpIntent(searchInstance, sdpPseRecord, z);
        }
    }

    void sdpOppOpsRecordFoundCallback(int i, byte[] bArr, byte[] bArr2, int i2, int i3, int i4, String str, byte[] bArr3, boolean z) {
        SdpOppOpsRecord sdpOppOpsRecord;
        synchronized (TRACKER_LOCK) {
            SdpSearchInstance searchInstance = sSdpSearchTracker.getSearchInstance(bArr, bArr2);
            if (searchInstance == null) {
                Log.e(TAG, "sdpOppOpsRecordFoundCallback: Search instance is NULL");
                return;
            }
            searchInstance.setStatus(i);
            if (i == 0) {
                sdpOppOpsRecord = new SdpOppOpsRecord(str, i3, i2, i4, bArr3);
            } else {
                sdpOppOpsRecord = null;
            }
            Log.d(TAG, "UUID: " + Arrays.toString(bArr2));
            Log.d(TAG, "UUID in parcel: " + Utils.byteArrayToUuid(bArr2)[0].toString());
            sendSdpIntent(searchInstance, sdpOppOpsRecord, z);
        }
    }

    void sdpSapsRecordFoundCallback(int i, byte[] bArr, byte[] bArr2, int i2, int i3, String str, boolean z) {
        synchronized (TRACKER_LOCK) {
            SdpSearchInstance searchInstance = sSdpSearchTracker.getSearchInstance(bArr, bArr2);
            SdpSapsRecord sdpSapsRecord = null;
            if (searchInstance == null) {
                Log.e(TAG, "sdpSapsRecordFoundCallback: Search instance is NULL");
                return;
            }
            searchInstance.setStatus(i);
            if (i == 0) {
                sdpSapsRecord = new SdpSapsRecord(i2, i3, str);
            }
            Log.d(TAG, "UUID: " + Arrays.toString(bArr2));
            Log.d(TAG, "UUID in parcel: " + Utils.byteArrayToUuid(bArr2)[0].toString());
            sendSdpIntent(searchInstance, sdpSapsRecord, z);
        }
    }

    void sdpRecordFoundCallback(int i, byte[] bArr, byte[] bArr2, int i2, byte[] bArr3) {
        synchronized (TRACKER_LOCK) {
            SdpSearchInstance searchInstance = sSdpSearchTracker.getSearchInstance(bArr, bArr2);
            SdpRecord sdpRecord = null;
            if (searchInstance == null) {
                Log.e(TAG, "sdpRecordFoundCallback: Search instance is NULL");
                return;
            }
            searchInstance.setStatus(i);
            if (i == 0) {
                Log.d(TAG, "sdpRecordFoundCallback: found a sdp record of size " + i2);
                Log.d(TAG, "Record:" + Arrays.toString(bArr3));
                sdpRecord = new SdpRecord(i2, bArr3);
            }
            Log.d(TAG, "UUID: " + Arrays.toString(bArr2));
            Log.d(TAG, "UUID in parcel: " + Utils.byteArrayToUuid(bArr2)[0].toString());
            sendSdpIntent(searchInstance, sdpRecord, false);
        }
    }

    public void sdpSearch(BluetoothDevice bluetoothDevice, ParcelUuid parcelUuid) {
        if (!sNativeAvailable) {
            Log.e(TAG, "Native not initialized!");
            return;
        }
        synchronized (TRACKER_LOCK) {
            if (sSdpSearchTracker.isSearching(bluetoothDevice, parcelUuid)) {
                return;
            }
            sSdpSearchTracker.add(new SdpSearchInstance(0, bluetoothDevice, parcelUuid));
            startSearch();
        }
    }

    private void startSearch() {
        SdpSearchInstance next = sSdpSearchTracker.getNext();
        if (next != null && !sSearchInProgress) {
            Log.d(TAG, "Starting search for UUID: " + next.getUuid());
            sSearchInProgress = true;
            next.startSearch();
            sdpSearchNative(Utils.getBytesFromAddress(next.getDevice().getAddress()), Utils.uuidToByteArray(next.getUuid()));
            return;
        }
        Log.d(TAG, "startSearch(): nextInst = " + next + " mSearchInProgress = " + sSearchInProgress + " - search busy or queue empty.");
    }

    private void sendSdpIntent(SdpSearchInstance sdpSearchInstance, Parcelable parcelable, boolean z) {
        sdpSearchInstance.stopSearch();
        Intent intent = new Intent("android.bluetooth.device.action.SDP_RECORD");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", sdpSearchInstance.getDevice());
        intent.putExtra("android.bluetooth.device.extra.SDP_SEARCH_STATUS", sdpSearchInstance.getStatus());
        if (parcelable != null) {
            intent.putExtra("android.bluetooth.device.extra.SDP_RECORD", parcelable);
        }
        intent.putExtra("android.bluetooth.device.extra.UUID", sdpSearchInstance.getUuid());
        sAdapterService.sendBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
        if (!z) {
            sSdpSearchTracker.remove(sdpSearchInstance);
            sSearchInProgress = false;
            startSearch();
        }
    }

    public int createMapMasRecord(String str, int i, int i2, int i3, int i4, int i5, int i6) {
        if (!sNativeAvailable) {
            throw new RuntimeException("SdpManager sNativeAvailable == false - native not initialized");
        }
        return sdpCreateMapMasRecordNative(str, i, i2, i3, i4, i5, i6);
    }

    public int createMapMnsRecord(String str, int i, int i2, int i3, int i4) {
        if (!sNativeAvailable) {
            throw new RuntimeException("SdpManager sNativeAvailable == false - native not initialized");
        }
        return sdpCreateMapMnsRecordNative(str, i, i2, i3, i4);
    }

    public int createPbapPseRecord(String str, int i, int i2, int i3, int i4, int i5) {
        if (!sNativeAvailable) {
            throw new RuntimeException("SdpManager sNativeAvailable == false - native not initialized");
        }
        return sdpCreatePbapPseRecordNative(str, i, i2, i3, i4, i5);
    }

    public int createOppOpsRecord(String str, int i, int i2, int i3, byte[] bArr) {
        if (!sNativeAvailable) {
            throw new RuntimeException("SdpManager sNativeAvailable == false - native not initialized");
        }
        return sdpCreateOppOpsRecordNative(str, i, i2, i3, bArr);
    }

    public int createSapsRecord(String str, int i, int i2) {
        if (!sNativeAvailable) {
            throw new RuntimeException("SdpManager sNativeAvailable == false - native not initialized");
        }
        return sdpCreateSapsRecordNative(str, i, i2);
    }

    public boolean removeSdpRecord(int i) {
        if (!sNativeAvailable) {
            throw new RuntimeException("SdpManager sNativeAvailable == false - native not initialized");
        }
        return sdpRemoveSdpRecordNative(i);
    }
}
