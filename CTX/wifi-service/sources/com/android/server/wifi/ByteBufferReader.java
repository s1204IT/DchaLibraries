package com.android.server.wifi;

import com.android.internal.annotations.VisibleForTesting;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class ByteBufferReader {

    @VisibleForTesting
    public static final int MAXIMUM_INTEGER_SIZE = 8;

    @VisibleForTesting
    public static final int MINIMUM_INTEGER_SIZE = 1;

    public static long readInteger(ByteBuffer byteBuffer, ByteOrder byteOrder, int i) {
        if (i < 1 || i > 8) {
            throw new IllegalArgumentException("Invalid size " + i);
        }
        byte[] bArr = new byte[i];
        byteBuffer.get(bArr);
        long j = 0;
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            for (int length = bArr.length - 1; length >= 0; length--) {
                j = (j << 8) | ((long) (bArr[length] & 255));
            }
        } else {
            for (byte b : bArr) {
                j = (j << 8) | ((long) (b & 255));
            }
        }
        return j;
    }

    public static String readString(ByteBuffer byteBuffer, int i, Charset charset) {
        byte[] bArr = new byte[i];
        byteBuffer.get(bArr);
        return new String(bArr, charset);
    }

    public static String readStringWithByteLength(ByteBuffer byteBuffer, Charset charset) {
        return readString(byteBuffer, byteBuffer.get() & 255, charset);
    }
}
