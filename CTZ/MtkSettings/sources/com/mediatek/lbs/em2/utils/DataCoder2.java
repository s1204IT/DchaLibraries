package com.mediatek.lbs.em2.utils;

import java.io.IOException;

public class DataCoder2 {

    public static class DataCoderBuffer {
        public byte[] mBuff;
        public int mOffset = 0;

        public DataCoderBuffer(int i) {
            this.mBuff = new byte[i];
        }

        public void write(byte[] bArr) {
            for (byte b : bArr) {
                this.mBuff[this.mOffset] = b;
                this.mOffset++;
            }
        }

        public void clear() {
            this.mOffset = 0;
        }

        public void flush() {
            this.mOffset = 0;
        }

        public void readFully(byte[] bArr, int i, int i2) {
            for (int i3 = 0; i3 < i2; i3++) {
                bArr[i3] = this.mBuff[this.mOffset];
                this.mOffset++;
            }
        }
    }

    public static void putBoolean(DataCoderBuffer dataCoderBuffer, boolean z) throws IOException {
        putByte(dataCoderBuffer, !z ? (byte) 0 : (byte) 1);
    }

    public static void putByte(DataCoderBuffer dataCoderBuffer, byte b) throws IOException {
        dataCoderBuffer.mBuff[dataCoderBuffer.mOffset] = b;
        dataCoderBuffer.mOffset++;
    }

    public static void putShort(DataCoderBuffer dataCoderBuffer, short s) throws IOException {
        putByte(dataCoderBuffer, (byte) (s & 255));
        putByte(dataCoderBuffer, (byte) ((s >> 8) & 255));
    }

    public static void putInt(DataCoderBuffer dataCoderBuffer, int i) throws IOException {
        putShort(dataCoderBuffer, (short) (i & 65535));
        putShort(dataCoderBuffer, (short) ((i >> 16) & 65535));
    }

    public static void putString(DataCoderBuffer dataCoderBuffer, String str) throws IOException {
        if (str == null) {
            putByte(dataCoderBuffer, (byte) 0);
            return;
        }
        putByte(dataCoderBuffer, (byte) 1);
        byte[] bytes = str.getBytes();
        putInt(dataCoderBuffer, bytes.length + 1);
        dataCoderBuffer.write(bytes);
        putByte(dataCoderBuffer, (byte) 0);
    }

    public static boolean getBoolean(DataCoderBuffer dataCoderBuffer) throws IOException {
        return getByte(dataCoderBuffer) != 0;
    }

    public static byte getByte(DataCoderBuffer dataCoderBuffer) throws IOException {
        byte b = dataCoderBuffer.mBuff[dataCoderBuffer.mOffset];
        dataCoderBuffer.mOffset++;
        return b;
    }

    public static short getShort(DataCoderBuffer dataCoderBuffer) throws IOException {
        return (short) ((getByte(dataCoderBuffer) << 8) | ((short) ((getByte(dataCoderBuffer) & 255) | 0)));
    }

    public static int getInt(DataCoderBuffer dataCoderBuffer) throws IOException {
        return (getShort(dataCoderBuffer) << 16) | (getShort(dataCoderBuffer) & 65535) | 0;
    }

    public static String getString(DataCoderBuffer dataCoderBuffer) throws IOException {
        if (getByte(dataCoderBuffer) == 0) {
            return null;
        }
        int i = getInt(dataCoderBuffer);
        byte[] bArr = new byte[i];
        dataCoderBuffer.readFully(bArr, 0, i);
        return new String(bArr).trim();
    }

    public static byte[] getBinary(DataCoderBuffer dataCoderBuffer) throws IOException {
        int i = getInt(dataCoderBuffer);
        byte[] bArr = new byte[i];
        dataCoderBuffer.readFully(bArr, 0, i);
        return bArr;
    }
}
