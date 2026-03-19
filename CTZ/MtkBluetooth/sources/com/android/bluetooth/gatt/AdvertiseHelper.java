package com.android.bluetooth.gatt;

import android.bluetooth.BluetoothUuid;
import android.bluetooth.le.AdvertiseData;
import android.os.ParcelUuid;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

class AdvertiseHelper {
    private static final int COMPLETE_LIST_128_BIT_SERVICE_UUIDS = 7;
    private static final int COMPLETE_LIST_16_BIT_SERVICE_UUIDS = 3;
    private static final int COMPLETE_LIST_32_BIT_SERVICE_UUIDS = 5;
    private static final int COMPLETE_LOCAL_NAME = 9;
    private static final int DEVICE_NAME_MAX = 26;
    private static final int MANUFACTURER_SPECIFIC_DATA = 255;
    private static final int SERVICE_DATA_128_BIT_UUID = 33;
    private static final int SERVICE_DATA_16_BIT_UUID = 22;
    private static final int SERVICE_DATA_32_BIT_UUID = 32;
    private static final int SHORTENED_LOCAL_NAME = 8;
    private static final String TAG = "AdvertiseHelper";
    private static final int TX_POWER_LEVEL = 10;

    AdvertiseHelper() {
    }

    public static byte[] advertiseDataToBytes(AdvertiseData advertiseData, String str) {
        int i;
        if (advertiseData == null) {
            return new byte[0];
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (advertiseData.getIncludeDeviceName()) {
            try {
                byte[] bytes = str.getBytes("UTF-8");
                int length = bytes.length;
                if (length <= 26) {
                    i = 9;
                } else {
                    i = 8;
                    length = 26;
                }
                byteArrayOutputStream.write(length + 1);
                byteArrayOutputStream.write(i);
                byteArrayOutputStream.write(bytes, 0, length);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Can't include name - encoding error!", e);
            }
        }
        for (int i2 = 0; i2 < advertiseData.getManufacturerSpecificData().size(); i2++) {
            int iKeyAt = advertiseData.getManufacturerSpecificData().keyAt(i2);
            byte[] bArr = advertiseData.getManufacturerSpecificData().get(iKeyAt);
            byte[] bArr2 = new byte[(bArr == null ? 0 : bArr.length) + 2];
            bArr2[0] = (byte) (iKeyAt & 255);
            bArr2[1] = (byte) ((iKeyAt >> 8) & 255);
            if (bArr != null) {
                System.arraycopy(bArr, 0, bArr2, 2, bArr.length);
            }
            byteArrayOutputStream.write(bArr2.length + 1);
            byteArrayOutputStream.write(255);
            byteArrayOutputStream.write(bArr2, 0, bArr2.length);
        }
        if (advertiseData.getIncludeTxPowerLevel()) {
            byteArrayOutputStream.write(2);
            byteArrayOutputStream.write(10);
            byteArrayOutputStream.write(0);
        }
        if (advertiseData.getServiceUuids() != null) {
            ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
            ByteArrayOutputStream byteArrayOutputStream3 = new ByteArrayOutputStream();
            ByteArrayOutputStream byteArrayOutputStream4 = new ByteArrayOutputStream();
            Iterator<ParcelUuid> it = advertiseData.getServiceUuids().iterator();
            while (it.hasNext()) {
                byte[] bArrUuidToBytes = BluetoothUuid.uuidToBytes(it.next());
                if (bArrUuidToBytes.length == 2) {
                    byteArrayOutputStream2.write(bArrUuidToBytes, 0, bArrUuidToBytes.length);
                } else if (bArrUuidToBytes.length == 4) {
                    byteArrayOutputStream3.write(bArrUuidToBytes, 0, bArrUuidToBytes.length);
                } else {
                    byteArrayOutputStream4.write(bArrUuidToBytes, 0, bArrUuidToBytes.length);
                }
            }
            if (byteArrayOutputStream2.size() != 0) {
                byteArrayOutputStream.write(byteArrayOutputStream2.size() + 1);
                byteArrayOutputStream.write(3);
                byteArrayOutputStream.write(byteArrayOutputStream2.toByteArray(), 0, byteArrayOutputStream2.size());
            }
            if (byteArrayOutputStream3.size() != 0) {
                byteArrayOutputStream.write(byteArrayOutputStream3.size() + 1);
                byteArrayOutputStream.write(5);
                byteArrayOutputStream.write(byteArrayOutputStream3.toByteArray(), 0, byteArrayOutputStream3.size());
            }
            if (byteArrayOutputStream4.size() != 0) {
                byteArrayOutputStream.write(byteArrayOutputStream4.size() + 1);
                byteArrayOutputStream.write(7);
                byteArrayOutputStream.write(byteArrayOutputStream4.toByteArray(), 0, byteArrayOutputStream4.size());
            }
        }
        if (!advertiseData.getServiceData().isEmpty()) {
            for (ParcelUuid parcelUuid : advertiseData.getServiceData().keySet()) {
                byte[] bArr3 = advertiseData.getServiceData().get(parcelUuid);
                byte[] bArrUuidToBytes2 = BluetoothUuid.uuidToBytes(parcelUuid);
                int length2 = bArrUuidToBytes2.length;
                byte[] bArr4 = new byte[(bArr3 == null ? 0 : bArr3.length) + length2];
                System.arraycopy(bArrUuidToBytes2, 0, bArr4, 0, length2);
                if (bArr3 != null) {
                    System.arraycopy(bArr3, 0, bArr4, length2, bArr3.length);
                }
                if (bArrUuidToBytes2.length == 2) {
                    byteArrayOutputStream.write(bArr4.length + 1);
                    byteArrayOutputStream.write(22);
                    byteArrayOutputStream.write(bArr4, 0, bArr4.length);
                } else if (bArrUuidToBytes2.length == 4) {
                    byteArrayOutputStream.write(bArr4.length + 1);
                    byteArrayOutputStream.write(32);
                    byteArrayOutputStream.write(bArr4, 0, bArr4.length);
                } else {
                    byteArrayOutputStream.write(bArr4.length + 1);
                    byteArrayOutputStream.write(33);
                    byteArrayOutputStream.write(bArr4, 0, bArr4.length);
                }
            }
        }
        return byteArrayOutputStream.toByteArray();
    }
}
