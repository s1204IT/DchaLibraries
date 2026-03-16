package com.android.dex;

import com.android.dex.util.ByteInput;
import com.android.dex.util.ByteOutput;

public final class Leb128 {
    private Leb128() {
    }

    public static int unsignedLeb128Size(int value) {
        int remaining = value >> 7;
        int count = 0;
        while (remaining != 0) {
            remaining >>= 7;
            count++;
        }
        return count + 1;
    }

    public static int signedLeb128Size(int value) {
        int remaining = value >> 7;
        int count = 0;
        boolean hasMore = true;
        int end = (Integer.MIN_VALUE & value) == 0 ? 0 : -1;
        while (hasMore) {
            hasMore = (remaining == end && (remaining & 1) == ((value >> 6) & 1)) ? false : true;
            value = remaining;
            remaining >>= 7;
            count++;
        }
        return count;
    }

    public static int readSignedLeb128(ByteInput in) {
        int cur;
        int result = 0;
        int count = 0;
        int signBits = -1;
        do {
            cur = in.readByte() & Character.DIRECTIONALITY_UNDEFINED;
            result |= (cur & 127) << (count * 7);
            signBits <<= 7;
            count++;
            if ((cur & 128) != 128) {
                break;
            }
        } while (count < 5);
        if ((cur & 128) == 128) {
            throw new DexException("invalid LEB128 sequence");
        }
        if (((signBits >> 1) & result) != 0) {
            return result | signBits;
        }
        return result;
    }

    public static int readUnsignedLeb128(ByteInput in) {
        int cur;
        int result = 0;
        int count = 0;
        do {
            cur = in.readByte() & Character.DIRECTIONALITY_UNDEFINED;
            result |= (cur & 127) << (count * 7);
            count++;
            if ((cur & 128) != 128) {
                break;
            }
        } while (count < 5);
        if ((cur & 128) == 128) {
            throw new DexException("invalid LEB128 sequence");
        }
        return result;
    }

    public static void writeUnsignedLeb128(ByteOutput out, int value) {
        for (int remaining = value >>> 7; remaining != 0; remaining >>>= 7) {
            out.writeByte((byte) ((value & 127) | 128));
            value = remaining;
        }
        out.writeByte((byte) (value & 127));
    }

    public static void writeSignedLeb128(ByteOutput out, int value) {
        int remaining = value >> 7;
        boolean hasMore = true;
        int end = (Integer.MIN_VALUE & value) == 0 ? 0 : -1;
        while (hasMore) {
            hasMore = (remaining == end && (remaining & 1) == ((value >> 6) & 1)) ? false : true;
            out.writeByte((byte) ((hasMore ? 128 : 0) | (value & 127)));
            value = remaining;
            remaining >>= 7;
        }
    }
}
