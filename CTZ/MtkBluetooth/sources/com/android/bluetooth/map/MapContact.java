package com.android.bluetooth.map;

import com.android.bluetooth.SignedLongLong;

public class MapContact {
    private final long mId;
    private final String mName;

    private MapContact(long j, String str) {
        this.mId = j;
        this.mName = str;
    }

    public static MapContact create(long j, String str) {
        return new MapContact(j, str);
    }

    public String getName() {
        return this.mName;
    }

    public long getId() {
        return this.mId;
    }

    public String getXBtUidString() {
        if (this.mId > 0) {
            return BluetoothMapUtils.getLongLongAsString(this.mId, 0L);
        }
        return null;
    }

    public SignedLongLong getXBtUid() {
        if (this.mId > 0) {
            return new SignedLongLong(this.mId, 0L);
        }
        return null;
    }

    public String toString() {
        return this.mName;
    }
}
