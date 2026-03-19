package com.android.bluetooth.pbapclient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.obex.HeaderSet;

public final class ObexAppParameters {
    private final HashMap<Byte, byte[]> mParams = new HashMap<>();

    public ObexAppParameters() {
    }

    public ObexAppParameters(byte[] bArr) {
        if (bArr != 0) {
            int i = 0;
            while (i < bArr.length && bArr.length - i >= 2) {
                int i2 = i + 1;
                byte b = bArr[i];
                int i3 = i2 + 1;
                int i4 = bArr[i2];
                if ((bArr.length - i3) - i4 >= 0) {
                    byte[] bArr2 = new byte[i4];
                    System.arraycopy(bArr, i3, bArr2, 0, i4);
                    add(b, bArr2);
                    i = i3 + i4;
                } else {
                    return;
                }
            }
        }
    }

    public static ObexAppParameters fromHeaderSet(HeaderSet headerSet) {
        try {
            return new ObexAppParameters((byte[]) headerSet.getHeader(76));
        } catch (IOException e) {
            return null;
        }
    }

    public byte[] getHeader() {
        Iterator<Map.Entry<Byte, byte[]>> it = this.mParams.entrySet().iterator();
        int length = 0;
        while (it.hasNext()) {
            length += it.next().getValue().length + 2;
        }
        byte[] bArr = new byte[length];
        int i = 0;
        for (Map.Entry<Byte, byte[]> entry : this.mParams.entrySet()) {
            int length2 = entry.getValue().length;
            int i2 = i + 1;
            bArr[i] = entry.getKey().byteValue();
            int i3 = i2 + 1;
            bArr[i2] = (byte) length2;
            System.arraycopy(entry.getValue(), 0, bArr, i3, length2);
            i = i3 + length2;
        }
        return bArr;
    }

    public void addToHeaderSet(HeaderSet headerSet) {
        if (this.mParams.size() > 0) {
            headerSet.setHeader(76, getHeader());
        }
    }

    public boolean exists(byte b) {
        return this.mParams.containsKey(Byte.valueOf(b));
    }

    public void add(byte b, byte b2) {
        this.mParams.put(Byte.valueOf(b), ByteBuffer.allocate(1).put(b2).array());
    }

    public void add(byte b, short s) {
        this.mParams.put(Byte.valueOf(b), ByteBuffer.allocate(2).putShort(s).array());
    }

    public void add(byte b, int i) {
        this.mParams.put(Byte.valueOf(b), ByteBuffer.allocate(4).putInt(i).array());
    }

    public void add(byte b, long j) {
        this.mParams.put(Byte.valueOf(b), ByteBuffer.allocate(8).putLong(j).array());
    }

    public void add(byte b, String str) {
        this.mParams.put(Byte.valueOf(b), str.getBytes());
    }

    public void add(byte b, byte[] bArr) {
        this.mParams.put(Byte.valueOf(b), bArr);
    }

    public byte getByte(byte b) {
        byte[] bArr = this.mParams.get(Byte.valueOf(b));
        if (bArr == null || bArr.length < 1) {
            return (byte) 0;
        }
        return ByteBuffer.wrap(bArr).get();
    }

    public short getShort(byte b) {
        byte[] bArr = this.mParams.get(Byte.valueOf(b));
        if (bArr == null || bArr.length < 2) {
            return (short) 0;
        }
        return ByteBuffer.wrap(bArr).getShort();
    }

    public int getInt(byte b) {
        byte[] bArr = this.mParams.get(Byte.valueOf(b));
        if (bArr == null || bArr.length < 4) {
            return 0;
        }
        return ByteBuffer.wrap(bArr).getInt();
    }

    public String getString(byte b) {
        byte[] bArr = this.mParams.get(Byte.valueOf(b));
        if (bArr == null) {
            return null;
        }
        return new String(bArr);
    }

    public byte[] getByteArray(byte b) {
        return this.mParams.get(Byte.valueOf(b));
    }

    public String toString() {
        return this.mParams.toString();
    }
}
