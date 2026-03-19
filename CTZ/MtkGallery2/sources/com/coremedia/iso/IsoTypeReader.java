package com.coremedia.iso;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public final class IsoTypeReader {
    public static long readUInt32BE(ByteBuffer byteBuffer) {
        long uInt8 = readUInt8(byteBuffer);
        return (((long) readUInt8(byteBuffer)) << 24) + (((long) readUInt8(byteBuffer)) << 16) + (readUInt8(byteBuffer) << 8) + (uInt8 << 0);
    }

    public static long readUInt32(ByteBuffer byteBuffer) {
        long j = byteBuffer.getInt();
        if (j < 0) {
            return j + 4294967296L;
        }
        return j;
    }

    public static int readUInt24(ByteBuffer byteBuffer) {
        return 0 + (readUInt16(byteBuffer) << 8) + byte2int(byteBuffer.get());
    }

    public static int readUInt16(ByteBuffer byteBuffer) {
        return 0 + (byte2int(byteBuffer.get()) << 8) + byte2int(byteBuffer.get());
    }

    public static int readUInt16BE(ByteBuffer byteBuffer) {
        return 0 + byte2int(byteBuffer.get()) + (byte2int(byteBuffer.get()) << 8);
    }

    public static int readUInt8(ByteBuffer byteBuffer) {
        return byte2int(byteBuffer.get());
    }

    public static int byte2int(byte b) {
        return b < 0 ? b + 256 : b;
    }

    public static String readString(ByteBuffer byteBuffer) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (true) {
            byte b = byteBuffer.get();
            if (b != 0) {
                byteArrayOutputStream.write(b);
            } else {
                return Utf8.convert(byteArrayOutputStream.toByteArray());
            }
        }
    }

    public static String readString(ByteBuffer byteBuffer, int i) {
        byte[] bArr = new byte[i];
        byteBuffer.get(bArr);
        return Utf8.convert(bArr);
    }

    public static long readUInt64(ByteBuffer byteBuffer) {
        long uInt32 = (readUInt32(byteBuffer) << 32) + 0;
        if (uInt32 < 0) {
            throw new RuntimeException("I don't know how to deal with UInt64! long is not sufficient and I don't want to use BigInt");
        }
        return uInt32 + readUInt32(byteBuffer);
    }

    public static double readFixedPoint1616(ByteBuffer byteBuffer) {
        byte[] bArr = new byte[4];
        byteBuffer.get(bArr);
        return ((double) ((((0 | ((bArr[0] << 24) & (-16777216))) | ((bArr[1] << 16) & 16711680)) | ((bArr[2] << 8) & 65280)) | (bArr[3] & 255))) / 65536.0d;
    }

    public static float readFixedPoint88(ByteBuffer byteBuffer) {
        byteBuffer.get(new byte[2]);
        return ((short) (((short) (0 | ((r0[0] << 8) & 65280))) | (r0[1] & 255))) / 256.0f;
    }

    public static String readIso639(ByteBuffer byteBuffer) {
        int uInt16 = readUInt16(byteBuffer);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append((char) (((uInt16 >> ((2 - i) * 5)) & 31) + 96));
        }
        return sb.toString();
    }

    public static String read4cc(ByteBuffer byteBuffer) {
        byte[] bArr = new byte[4];
        byteBuffer.get(bArr);
        return IsoFile.bytesToFourCC(bArr);
    }
}
