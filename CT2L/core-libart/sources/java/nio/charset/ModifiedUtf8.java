package java.nio.charset;

import dalvik.bytecode.Opcodes;
import java.io.UTFDataFormatException;
import java.nio.ByteOrder;
import libcore.io.Memory;

public class ModifiedUtf8 {
    public static String decode(byte[] in, char[] out, int offset, int utfSize) throws UTFDataFormatException {
        int s = 0;
        int count = 0;
        while (count < utfSize) {
            int count2 = count + 1;
            char c = (char) in[offset + count];
            out[s] = c;
            if (c < 128) {
                s++;
                count = count2;
            } else {
                char c2 = out[s];
                if ((c2 & 224) == 192) {
                    if (count2 >= utfSize) {
                        throw new UTFDataFormatException("bad second byte at " + count2);
                    }
                    count = count2 + 1;
                    int b = in[offset + count2];
                    if ((b & 192) != 128) {
                        throw new UTFDataFormatException("bad second byte at " + (count - 1));
                    }
                    out[s] = (char) (((c2 & 31) << 6) | (b & 63));
                    s++;
                } else if ((c2 & 240) == 224) {
                    if (count2 + 1 >= utfSize) {
                        throw new UTFDataFormatException("bad third byte at " + (count2 + 1));
                    }
                    int count3 = count2 + 1;
                    int b2 = in[offset + count2];
                    int count4 = count3 + 1;
                    int c3 = in[offset + count3];
                    if ((b2 & 192) != 128 || (c3 & 192) != 128) {
                        throw new UTFDataFormatException("bad second or third byte at " + (count4 - 2));
                    }
                    out[s] = (char) (((c2 & 15) << 12) | ((b2 & 63) << 6) | (c3 & 63));
                    s++;
                    count = count4;
                } else {
                    throw new UTFDataFormatException("bad byte at " + (count2 - 1));
                }
            }
        }
        return new String(out, 0, s);
    }

    public static long countBytes(String s, boolean shortLength) throws UTFDataFormatException {
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
        byte[] result = new byte[utfCount + 2];
        Memory.pokeShort(result, 0, (short) utfCount, ByteOrder.BIG_ENDIAN);
        encode(result, 2, s);
        return result;
    }

    private ModifiedUtf8() {
    }
}
