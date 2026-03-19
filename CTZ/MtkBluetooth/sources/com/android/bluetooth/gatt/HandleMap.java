package com.android.bluetooth.gatt;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class HandleMap {
    private static final boolean DBG = GattServiceConfig.DBG;
    private static final String TAG = "BtGatt.HandleMap";
    public static final int TYPE_CHARACTERISTIC = 2;
    public static final int TYPE_DESCRIPTOR = 3;
    public static final int TYPE_SERVICE = 1;
    public static final int TYPE_UNDEFINED = 0;
    List<Entry> mEntries;
    int mLastCharacteristic = 0;
    Map<Integer, Integer> mRequestMap;

    class Entry {
        public boolean advertisePreferred;
        public int charHandle;
        public int handle;
        public int instance;
        public int serverIf;
        public int serviceHandle;
        public int serviceType;
        public boolean started;
        public int type;
        public UUID uuid;

        Entry(int i, int i2, UUID uuid, int i3, int i4) {
            this.serverIf = 0;
            this.type = 0;
            this.handle = 0;
            this.uuid = null;
            this.instance = 0;
            this.serviceType = 0;
            this.serviceHandle = 0;
            this.charHandle = 0;
            this.started = false;
            this.advertisePreferred = false;
            this.serverIf = i;
            this.type = 1;
            this.handle = i2;
            this.uuid = uuid;
            this.instance = i4;
            this.serviceType = i3;
        }

        Entry(int i, int i2, UUID uuid, int i3, int i4, boolean z) {
            this.serverIf = 0;
            this.type = 0;
            this.handle = 0;
            this.uuid = null;
            this.instance = 0;
            this.serviceType = 0;
            this.serviceHandle = 0;
            this.charHandle = 0;
            this.started = false;
            this.advertisePreferred = false;
            this.serverIf = i;
            this.type = 1;
            this.handle = i2;
            this.uuid = uuid;
            this.instance = i4;
            this.serviceType = i3;
            this.advertisePreferred = z;
        }

        Entry(int i, int i2, int i3, UUID uuid, int i4) {
            this.serverIf = 0;
            this.type = 0;
            this.handle = 0;
            this.uuid = null;
            this.instance = 0;
            this.serviceType = 0;
            this.serviceHandle = 0;
            this.charHandle = 0;
            this.started = false;
            this.advertisePreferred = false;
            this.serverIf = i;
            this.type = i2;
            this.handle = i3;
            this.uuid = uuid;
            this.serviceHandle = i4;
        }

        Entry(int i, int i2, int i3, UUID uuid, int i4, int i5) {
            this.serverIf = 0;
            this.type = 0;
            this.handle = 0;
            this.uuid = null;
            this.instance = 0;
            this.serviceType = 0;
            this.serviceHandle = 0;
            this.charHandle = 0;
            this.started = false;
            this.advertisePreferred = false;
            this.serverIf = i;
            this.type = i2;
            this.handle = i3;
            this.uuid = uuid;
            this.serviceHandle = i4;
            this.charHandle = i5;
        }
    }

    HandleMap() {
        this.mEntries = null;
        this.mRequestMap = null;
        this.mEntries = new ArrayList();
        this.mRequestMap = new HashMap();
    }

    void clear() {
        this.mEntries.clear();
        this.mRequestMap.clear();
    }

    void addService(int i, int i2, UUID uuid, int i3, int i4, boolean z) {
        this.mEntries.add(new Entry(i, i2, uuid, i3, i4, z));
    }

    void addCharacteristic(int i, int i2, UUID uuid, int i3) {
        this.mLastCharacteristic = i2;
        this.mEntries.add(new Entry(i, 2, i2, uuid, i3));
    }

    void addDescriptor(int i, int i2, UUID uuid, int i3) {
        this.mEntries.add(new Entry(i, 3, i2, uuid, i3, this.mLastCharacteristic));
    }

    void setStarted(int i, int i2, boolean z) {
        for (Entry entry : this.mEntries) {
            if (entry.type == 1 && entry.serverIf == i && entry.handle == i2) {
                entry.started = z;
                return;
            }
        }
    }

    Entry getByHandle(int i) {
        for (Entry entry : this.mEntries) {
            if (entry.handle == i) {
                return entry;
            }
        }
        Log.e(TAG, "getByHandle() - Handle " + i + " not found!");
        return null;
    }

    boolean checkServiceExists(UUID uuid, int i) {
        for (Entry entry : this.mEntries) {
            if (entry.type == 1 && entry.handle == i && entry.uuid.equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    void deleteService(int i, int i2) {
        Iterator<Entry> it = this.mEntries.iterator();
        while (it.hasNext()) {
            Entry next = it.next();
            if (next.serverIf == i && (next.handle == i2 || next.serviceHandle == i2)) {
                it.remove();
            }
        }
    }

    List<Entry> getEntries() {
        return this.mEntries;
    }

    void addRequest(int i, int i2) {
        this.mRequestMap.put(Integer.valueOf(i), Integer.valueOf(i2));
    }

    void deleteRequest(int i) {
        this.mRequestMap.remove(Integer.valueOf(i));
    }

    Entry getByRequestId(int i) {
        Integer num = this.mRequestMap.get(Integer.valueOf(i));
        if (num == null) {
            Log.e(TAG, "getByRequestId() - Request ID " + i + " not found!");
            return null;
        }
        return getByHandle(num.intValue());
    }

    void dump(StringBuilder sb) {
        sb.append("  Entries: " + this.mEntries.size() + "\n");
        sb.append("  Requests: " + this.mRequestMap.size() + "\n");
        for (Entry entry : this.mEntries) {
            sb.append("  " + entry.serverIf + ": [" + entry.handle + "] ");
            switch (entry.type) {
                case 1:
                    sb.append("Service " + entry.uuid);
                    sb.append(", started " + entry.started);
                    break;
                case 2:
                    sb.append("  Characteristic " + entry.uuid);
                    break;
                case 3:
                    sb.append("    Descriptor " + entry.uuid);
                    break;
            }
            sb.append("\n");
        }
    }
}
