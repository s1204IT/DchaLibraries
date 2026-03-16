package android.bluetooth.le;

import android.bluetooth.BluetoothUuid;
import android.os.BatteryStats;
import android.os.ParcelUuid;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ScanRecord {
    private static final int DATA_TYPE_FLAGS = 1;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 9;
    private static final int DATA_TYPE_LOCAL_NAME_SHORT = 8;
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 255;
    private static final int DATA_TYPE_SERVICE_DATA = 22;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 7;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 6;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 3;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 2;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 5;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 4;
    private static final int DATA_TYPE_TX_POWER_LEVEL = 10;
    private static final String TAG = "ScanRecord";
    private final int mAdvertiseFlags;
    private final byte[] mBytes;
    private final String mDeviceName;
    private final SparseArray<byte[]> mManufacturerSpecificData;
    private final Map<ParcelUuid, byte[]> mServiceData;
    private final List<ParcelUuid> mServiceUuids;
    private final int mTxPowerLevel;

    public int getAdvertiseFlags() {
        return this.mAdvertiseFlags;
    }

    public List<ParcelUuid> getServiceUuids() {
        return this.mServiceUuids;
    }

    public SparseArray<byte[]> getManufacturerSpecificData() {
        return this.mManufacturerSpecificData;
    }

    public byte[] getManufacturerSpecificData(int manufacturerId) {
        return this.mManufacturerSpecificData.get(manufacturerId);
    }

    public Map<ParcelUuid, byte[]> getServiceData() {
        return this.mServiceData;
    }

    public byte[] getServiceData(ParcelUuid serviceDataUuid) {
        if (serviceDataUuid == null) {
            return null;
        }
        return this.mServiceData.get(serviceDataUuid);
    }

    public int getTxPowerLevel() {
        return this.mTxPowerLevel;
    }

    public String getDeviceName() {
        return this.mDeviceName;
    }

    public byte[] getBytes() {
        return this.mBytes;
    }

    private ScanRecord(List<ParcelUuid> serviceUuids, SparseArray<byte[]> manufacturerData, Map<ParcelUuid, byte[]> serviceData, int advertiseFlags, int txPowerLevel, String localName, byte[] bytes) {
        this.mServiceUuids = serviceUuids;
        this.mManufacturerSpecificData = manufacturerData;
        this.mServiceData = serviceData;
        this.mDeviceName = localName;
        this.mAdvertiseFlags = advertiseFlags;
        this.mTxPowerLevel = txPowerLevel;
        this.mBytes = bytes;
    }

    public static ScanRecord parseFromBytes(byte[] scanRecord) {
        int currentPos;
        if (scanRecord == null) {
            return null;
        }
        int advertiseFlag = -1;
        List<ParcelUuid> serviceUuids = new ArrayList<>();
        String localName = null;
        int txPowerLevel = Integer.MIN_VALUE;
        SparseArray<byte[]> manufacturerData = new SparseArray<>();
        Map<ParcelUuid, byte[]> serviceData = new ArrayMap<>();
        int currentPos2 = 0;
        while (currentPos2 < scanRecord.length) {
            try {
                currentPos = currentPos2 + 1;
            } catch (Exception e) {
            }
            try {
                int length = scanRecord[currentPos2] & BatteryStats.HistoryItem.CMD_NULL;
                if (length != 0) {
                    int dataLength = length - 1;
                    int currentPos3 = currentPos + 1;
                    int fieldType = scanRecord[currentPos] & 255;
                    switch (fieldType) {
                        case 1:
                            advertiseFlag = scanRecord[currentPos3] & BatteryStats.HistoryItem.CMD_NULL;
                            break;
                        case 2:
                        case 3:
                            parseServiceUuid(scanRecord, currentPos3, dataLength, 2, serviceUuids);
                            break;
                        case 4:
                        case 5:
                            parseServiceUuid(scanRecord, currentPos3, dataLength, 4, serviceUuids);
                            break;
                        case 6:
                        case 7:
                            parseServiceUuid(scanRecord, currentPos3, dataLength, 16, serviceUuids);
                            break;
                        case 8:
                        case 9:
                            localName = new String(extractBytes(scanRecord, currentPos3, dataLength));
                            break;
                        case 10:
                            txPowerLevel = scanRecord[currentPos3];
                            break;
                        case 22:
                            byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos3, 2);
                            ParcelUuid serviceDataUuid = BluetoothUuid.parseUuidFrom(serviceDataUuidBytes);
                            byte[] serviceDataArray = extractBytes(scanRecord, currentPos3 + 2, dataLength - 2);
                            serviceData.put(serviceDataUuid, serviceDataArray);
                            break;
                        case 255:
                            int manufacturerId = ((scanRecord[currentPos3 + 1] & BatteryStats.HistoryItem.CMD_NULL) << 8) + (scanRecord[currentPos3] & BatteryStats.HistoryItem.CMD_NULL);
                            byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos3 + 2, dataLength - 2);
                            manufacturerData.put(manufacturerId, manufacturerDataBytes);
                            break;
                    }
                    currentPos2 = currentPos3 + dataLength;
                } else {
                    if (serviceUuids.isEmpty()) {
                        serviceUuids = null;
                    }
                    return new ScanRecord(serviceUuids, manufacturerData, serviceData, advertiseFlag, txPowerLevel, localName, scanRecord);
                }
            } catch (Exception e2) {
                Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord));
                return new ScanRecord(null, null, null, -1, Integer.MIN_VALUE, null, scanRecord);
            }
        }
        if (serviceUuids.isEmpty()) {
        }
        return new ScanRecord(serviceUuids, manufacturerData, serviceData, advertiseFlag, txPowerLevel, localName, scanRecord);
    }

    public String toString() {
        return "ScanRecord [mAdvertiseFlags=" + this.mAdvertiseFlags + ", mServiceUuids=" + this.mServiceUuids + ", mManufacturerSpecificData=" + BluetoothLeUtils.toString(this.mManufacturerSpecificData) + ", mServiceData=" + BluetoothLeUtils.toString(this.mServiceData) + ", mTxPowerLevel=" + this.mTxPowerLevel + ", mDeviceName=" + this.mDeviceName + "]";
    }

    private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength, int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos, uuidLength);
            serviceUuids.add(BluetoothUuid.parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }
}
