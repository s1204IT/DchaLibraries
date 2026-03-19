package com.android.server.wifi.util;

import java.util.ArrayList;

public class ByteArrayRingBuffer {
    private ArrayList<byte[]> mArrayList;
    private int mBytesUsed;
    private int mMaxBytes;

    public ByteArrayRingBuffer(int i) {
        if (i < 1) {
            throw new IllegalArgumentException();
        }
        this.mArrayList = new ArrayList<>();
        this.mMaxBytes = i;
        this.mBytesUsed = 0;
    }

    public boolean appendBuffer(byte[] bArr) {
        pruneToSize(this.mMaxBytes - bArr.length);
        if (this.mBytesUsed + bArr.length > this.mMaxBytes) {
            return false;
        }
        this.mArrayList.add(bArr);
        this.mBytesUsed += bArr.length;
        return true;
    }

    public byte[] getBuffer(int i) {
        return this.mArrayList.get(i);
    }

    public int getNumBuffers() {
        return this.mArrayList.size();
    }

    public void resize(int i) {
        pruneToSize(i);
        this.mMaxBytes = i;
    }

    private void pruneToSize(int i) {
        int length = this.mBytesUsed;
        int i2 = 0;
        while (i2 < this.mArrayList.size() && length > i) {
            length -= this.mArrayList.get(i2).length;
            i2++;
        }
        this.mArrayList.subList(0, i2).clear();
        this.mBytesUsed = length;
    }
}
