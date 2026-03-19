package com.android.server.wifi.hotspot2;

public class IconEvent {
    private final long mBSSID;
    private final byte[] mData;
    private final String mFileName;
    private final int mSize;

    public IconEvent(long j, String str, int i, byte[] bArr) {
        this.mBSSID = j;
        this.mFileName = str;
        this.mSize = i;
        this.mData = bArr;
    }

    public long getBSSID() {
        return this.mBSSID;
    }

    public String getFileName() {
        return this.mFileName;
    }

    public int getSize() {
        return this.mSize;
    }

    public byte[] getData() {
        return this.mData;
    }

    public String toString() {
        return "IconEvent: BSSID=" + String.format("%012x", Long.valueOf(this.mBSSID)) + ", fileName='" + this.mFileName + "', size=" + this.mSize;
    }
}
