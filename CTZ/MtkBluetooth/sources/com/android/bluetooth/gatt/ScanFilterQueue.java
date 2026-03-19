package com.android.bluetooth.gatt;

import android.bluetooth.BluetoothUuid;
import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

class ScanFilterQueue {
    private static final byte DEVICE_TYPE_ALL = 2;
    private static final int MAX_LEN_PER_FIELD = 26;
    public static final int TYPE_DEVICE_ADDRESS = 0;
    public static final int TYPE_LOCAL_NAME = 4;
    public static final int TYPE_MANUFACTURER_DATA = 5;
    public static final int TYPE_SERVICE_DATA = 6;
    public static final int TYPE_SERVICE_DATA_CHANGED = 1;
    public static final int TYPE_SERVICE_UUID = 2;
    public static final int TYPE_SOLICIT_UUID = 3;
    private Set<Entry> mEntries = new HashSet();

    ScanFilterQueue() {
    }

    class Entry {
        public byte addr_type;
        public String address;
        public int company;
        public int company_mask;
        public byte[] data;
        public byte[] data_mask;
        public String name;
        public byte type;
        public UUID uuid;
        public UUID uuid_mask;

        Entry() {
        }
    }

    void addDeviceAddress(String str, byte b) {
        Entry entry = new Entry();
        entry.type = (byte) 0;
        entry.address = str;
        entry.addr_type = b;
        this.mEntries.add(entry);
    }

    void addServiceChanged() {
        Entry entry = new Entry();
        entry.type = (byte) 1;
        this.mEntries.add(entry);
    }

    void addUuid(UUID uuid) {
        Entry entry = new Entry();
        entry.type = (byte) 2;
        entry.uuid = uuid;
        entry.uuid_mask = new UUID(0L, 0L);
        this.mEntries.add(entry);
    }

    void addUuid(UUID uuid, UUID uuid2) {
        Entry entry = new Entry();
        entry.type = (byte) 2;
        entry.uuid = uuid;
        entry.uuid_mask = uuid2;
        this.mEntries.add(entry);
    }

    void addSolicitUuid(UUID uuid) {
        Entry entry = new Entry();
        entry.type = (byte) 3;
        entry.uuid = uuid;
        this.mEntries.add(entry);
    }

    void addName(String str) {
        Entry entry = new Entry();
        entry.type = (byte) 4;
        entry.name = str;
        this.mEntries.add(entry);
    }

    void addManufacturerData(int i, byte[] bArr) {
        Entry entry = new Entry();
        entry.type = (byte) 5;
        entry.company = i;
        entry.company_mask = 65535;
        entry.data = bArr;
        entry.data_mask = new byte[bArr.length];
        Arrays.fill(entry.data_mask, (byte) -1);
        this.mEntries.add(entry);
    }

    void addManufacturerData(int i, int i2, byte[] bArr, byte[] bArr2) {
        Entry entry = new Entry();
        entry.type = (byte) 5;
        entry.company = i;
        entry.company_mask = i2;
        entry.data = bArr;
        entry.data_mask = bArr2;
        this.mEntries.add(entry);
    }

    void addServiceData(byte[] bArr, byte[] bArr2) {
        Entry entry = new Entry();
        entry.type = (byte) 6;
        entry.data = bArr;
        entry.data_mask = bArr2;
        this.mEntries.add(entry);
    }

    Entry pop() {
        if (this.mEntries.isEmpty()) {
            return null;
        }
        Iterator<Entry> it = this.mEntries.iterator();
        Entry next = it.next();
        it.remove();
        return next;
    }

    int getFeatureSelection() {
        Iterator<Entry> it = this.mEntries.iterator();
        int i = 0;
        while (it.hasNext()) {
            i |= 1 << it.next().type;
        }
        return i;
    }

    Entry[] toArray() {
        return (Entry[]) this.mEntries.toArray(new Entry[this.mEntries.size()]);
    }

    void addScanFilter(ScanFilter scanFilter) {
        if (scanFilter == null) {
            return;
        }
        if (scanFilter.getDeviceName() != null) {
            addName(scanFilter.getDeviceName());
        }
        if (scanFilter.getDeviceAddress() != null) {
            addDeviceAddress(scanFilter.getDeviceAddress(), (byte) 2);
        }
        if (scanFilter.getServiceUuid() != null) {
            if (scanFilter.getServiceUuidMask() == null) {
                addUuid(scanFilter.getServiceUuid().getUuid());
            } else {
                addUuid(scanFilter.getServiceUuid().getUuid(), scanFilter.getServiceUuidMask().getUuid());
            }
        }
        if (scanFilter.getManufacturerData() != null) {
            if (scanFilter.getManufacturerDataMask() == null) {
                addManufacturerData(scanFilter.getManufacturerId(), scanFilter.getManufacturerData());
            } else {
                addManufacturerData(scanFilter.getManufacturerId(), 65535, scanFilter.getManufacturerData(), scanFilter.getManufacturerDataMask());
            }
        }
        if (scanFilter.getServiceDataUuid() != null && scanFilter.getServiceData() != null) {
            ParcelUuid serviceDataUuid = scanFilter.getServiceDataUuid();
            byte[] serviceData = scanFilter.getServiceData();
            byte[] serviceDataMask = scanFilter.getServiceDataMask();
            if (serviceDataMask == null) {
                serviceDataMask = new byte[serviceData.length];
                Arrays.fill(serviceDataMask, (byte) -1);
            }
            byte[] bArrConcate = concate(serviceDataUuid, serviceData);
            byte[] bArrConcate2 = concate(serviceDataUuid, serviceDataMask);
            if (bArrConcate != null && bArrConcate2 != null) {
                addServiceData(bArrConcate, bArrConcate2);
            }
        }
    }

    private byte[] concate(ParcelUuid parcelUuid, byte[] bArr) {
        byte[] bArrUuidToBytes = BluetoothUuid.uuidToBytes(parcelUuid);
        int length = bArrUuidToBytes.length + (bArr == null ? 0 : bArr.length);
        if (length > 26) {
            return null;
        }
        byte[] bArr2 = new byte[length];
        System.arraycopy(bArrUuidToBytes, 0, bArr2, 0, bArrUuidToBytes.length);
        if (bArr != null) {
            System.arraycopy(bArr, 0, bArr2, bArrUuidToBytes.length, bArr.length);
        }
        return bArr2;
    }
}
