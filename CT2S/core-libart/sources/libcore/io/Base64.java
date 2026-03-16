package libcore.io;

import java.io.ObjectStreamConstants;
import java.nio.charset.StandardCharsets;
import libcore.util.EmptyArray;

public final class Base64 {
    private static final byte[] map = {65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, ObjectStreamConstants.TC_REFERENCE, ObjectStreamConstants.TC_CLASSDESC, ObjectStreamConstants.TC_OBJECT, ObjectStreamConstants.TC_STRING, ObjectStreamConstants.TC_ARRAY, ObjectStreamConstants.TC_CLASS, ObjectStreamConstants.TC_BLOCKDATA, ObjectStreamConstants.TC_ENDBLOCKDATA, ObjectStreamConstants.TC_RESET, ObjectStreamConstants.TC_BLOCKDATALONG, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47};

    private Base64() {
    }

    public static byte[] decode(byte[] in) {
        return decode(in, in.length);
    }

    public static byte[] decode(byte[] in, int len) {
        int outIndex;
        int outIndex2;
        int bits;
        int length = (len / 4) * 3;
        if (length == 0) {
            return EmptyArray.BYTE;
        }
        byte[] out = new byte[length];
        int pad = 0;
        while (true) {
            byte chr = in[len - 1];
            if (chr != 10 && chr != 13 && chr != 32 && chr != 9) {
                if (chr != 61) {
                    break;
                }
                pad++;
            }
            len--;
        }
        int inIndex = 0;
        int quantum = 0;
        int i = 0;
        int outIndex3 = 0;
        while (i < len) {
            byte chr2 = in[i];
            if (chr2 == 10 || chr2 == 13 || chr2 == 32) {
                outIndex2 = outIndex3;
            } else if (chr2 == 9) {
                outIndex2 = outIndex3;
            } else {
                if (chr2 >= 65 && chr2 <= 90) {
                    bits = chr2 - 65;
                } else if (chr2 >= 97 && chr2 <= 122) {
                    bits = chr2 - 71;
                } else if (chr2 >= 48 && chr2 <= 57) {
                    bits = chr2 + 4;
                } else if (chr2 == 43) {
                    bits = 62;
                } else if (chr2 == 47) {
                    bits = 63;
                } else {
                    return null;
                }
                quantum = (quantum << 6) | ((byte) bits);
                if (inIndex % 4 == 3) {
                    int outIndex4 = outIndex3 + 1;
                    out[outIndex3] = (byte) (quantum >> 16);
                    int outIndex5 = outIndex4 + 1;
                    out[outIndex4] = (byte) (quantum >> 8);
                    outIndex2 = outIndex5 + 1;
                    out[outIndex5] = (byte) quantum;
                } else {
                    outIndex2 = outIndex3;
                }
                inIndex++;
            }
            i++;
            outIndex3 = outIndex2;
        }
        if (pad > 0) {
            int quantum2 = quantum << (pad * 6);
            outIndex = outIndex3 + 1;
            out[outIndex3] = (byte) (quantum2 >> 16);
            if (pad == 1) {
                outIndex3 = outIndex + 1;
                out[outIndex] = (byte) (quantum2 >> 8);
                outIndex = outIndex3;
            }
        } else {
            outIndex = outIndex3;
        }
        byte[] result = new byte[outIndex];
        System.arraycopy(out, 0, result, 0, outIndex);
        return result;
    }

    public static String encode(byte[] in) {
        int index;
        int length = ((in.length + 2) * 4) / 3;
        byte[] out = new byte[length];
        int end = in.length - (in.length % 3);
        int index2 = 0;
        for (int i = 0; i < end; i += 3) {
            int index3 = index2 + 1;
            out[index2] = map[(in[i] & Character.DIRECTIONALITY_UNDEFINED) >> 2];
            int index4 = index3 + 1;
            out[index3] = map[((in[i] & 3) << 4) | ((in[i + 1] & Character.DIRECTIONALITY_UNDEFINED) >> 4)];
            int index5 = index4 + 1;
            out[index4] = map[((in[i + 1] & 15) << 2) | ((in[i + 2] & Character.DIRECTIONALITY_UNDEFINED) >> 6)];
            index2 = index5 + 1;
            out[index5] = map[in[i + 2] & 63];
        }
        switch (in.length % 3) {
            case 1:
                int index6 = index2 + 1;
                out[index2] = map[(in[end] & Character.DIRECTIONALITY_UNDEFINED) >> 2];
                int index7 = index6 + 1;
                out[index6] = map[(in[end] & 3) << 4];
                int index8 = index7 + 1;
                out[index7] = 61;
                out[index8] = 61;
                index = index8 + 1;
                break;
            case 2:
                int index9 = index2 + 1;
                out[index2] = map[(in[end] & Character.DIRECTIONALITY_UNDEFINED) >> 2];
                int index10 = index9 + 1;
                out[index9] = map[((in[end] & 3) << 4) | ((in[end + 1] & Character.DIRECTIONALITY_UNDEFINED) >> 4)];
                int index11 = index10 + 1;
                out[index10] = map[(in[end + 1] & 15) << 2];
                index2 = index11 + 1;
                out[index11] = 61;
            default:
                index = index2;
                break;
        }
        return new String(out, 0, index, StandardCharsets.US_ASCII);
    }
}
