package com.android.dex;

import com.android.dex.util.ByteInput;
import dalvik.bytecode.Opcodes;
import java.io.UTFDataFormatException;

public final class Mutf8 {
    private Mutf8() {
    }

    public static String decode(ByteInput in, char[] out) throws UTFDataFormatException {
        int s = 0;
        while (true) {
            char a = (char) (in.readByte() & Character.DIRECTIONALITY_UNDEFINED);
            if (a == 0) {
                return new String(out, 0, s);
            }
            out[s] = a;
            if (a < 128) {
                s++;
            } else if ((a & 224) == 192) {
                int b = in.readByte() & Character.DIRECTIONALITY_UNDEFINED;
                if ((b & 192) != 128) {
                    throw new UTFDataFormatException("bad second byte");
                }
                out[s] = (char) (((a & 31) << 6) | (b & 63));
                s++;
            } else if ((a & 240) == 224) {
                int b2 = in.readByte() & Character.DIRECTIONALITY_UNDEFINED;
                int c = in.readByte() & Character.DIRECTIONALITY_UNDEFINED;
                if ((b2 & 192) != 128 || (c & 192) != 128) {
                    break;
                }
                out[s] = (char) (((a & 15) << 12) | ((b2 & 63) << 6) | (c & 63));
                s++;
            } else {
                throw new UTFDataFormatException("bad byte");
            }
        }
        throw new UTFDataFormatException("bad second or third byte");
    }

    private static long countBytes(String s, boolean shortLength) throws UTFDataFormatException {
        long result = 0;
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char ch = s.charAt(i);
            if (ch != 0 && ch <= 127) {
                result++;
            } else if (ch <= 2047) {
                result += 2;
            } else {
                result += 3;
            }
            if (shortLength && result > 65535) {
                throw new UTFDataFormatException("String more than 65535 UTF bytes long");
            }
        }
        return result;
    }

    public static void encode(byte[] dst, int offset, String s) {
        int offset2;
        int length = s.length();
        int i = 0;
        int offset3 = offset;
        while (i < length) {
            char ch = s.charAt(i);
            if (ch != 0 && ch <= 127) {
                offset2 = offset3 + 1;
                dst[offset3] = (byte) ch;
            } else if (ch <= 2047) {
                int offset4 = offset3 + 1;
                dst[offset3] = (byte) (((ch >> 6) & 31) | 192);
                dst[offset4] = (byte) ((ch & '?') | 128);
                offset2 = offset4 + 1;
            } else {
                int offset5 = offset3 + 1;
                dst[offset3] = (byte) (((ch >> '\f') & 15) | Opcodes.OP_SHL_INT_LIT8);
                int offset6 = offset5 + 1;
                dst[offset5] = (byte) (((ch >> 6) & 63) | 128);
                offset2 = offset6 + 1;
                dst[offset6] = (byte) ((ch & '?') | 128);
            }
            i++;
            offset3 = offset2;
        }
    }

    public static byte[] encode(String s) throws UTFDataFormatException {
        int utfCount = (int) countBytes(s, true);
        byte[] result = new byte[utfCount];
        encode(result, 0, s);
        return result;
    }
}
