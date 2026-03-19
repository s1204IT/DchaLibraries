package java.nio.charset;

import android.icu.lang.UCharacterEnums;
import android.icu.text.Bidi;
import java.io.UTFDataFormatException;

public class ModifiedUtf8 {
    public static long countBytes(String s, boolean shortLength) throws UTFDataFormatException {
        long counter = 0;
        int strLen = s.length();
        for (int i = 0; i < strLen; i++) {
            char c = s.charAt(i);
            if (c < 128) {
                counter++;
                if (c == 0) {
                    counter++;
                }
            } else if (c < 2048) {
                counter += 2;
            } else {
                counter += 3;
            }
        }
        if (shortLength && counter > 65535) {
            throw new UTFDataFormatException("Size of the encoded string doesn't fit in two bytes");
        }
        return counter;
    }

    public static void encode(byte[] dst, int offset, String s) {
        int offset2;
        int strLen = s.length();
        int i = 0;
        int offset3 = offset;
        while (i < strLen) {
            char c = s.charAt(i);
            if (c < 128) {
                if (c == 0) {
                    int offset4 = offset3 + 1;
                    dst[offset3] = -64;
                    dst[offset4] = Bidi.LEVEL_OVERRIDE;
                    offset2 = offset4 + 1;
                } else {
                    offset2 = offset3 + 1;
                    dst[offset3] = (byte) c;
                }
            } else if (c < 2048) {
                int offset5 = offset3 + 1;
                dst[offset3] = (byte) ((c >>> 6) | 192);
                dst[offset5] = (byte) ((c & '?') | 128);
                offset2 = offset5 + 1;
            } else {
                int offset6 = offset3 + 1;
                dst[offset3] = (byte) ((c >>> '\f') | 224);
                int offset7 = offset6 + 1;
                dst[offset6] = (byte) (((c >>> 6) & 63) | 128);
                offset2 = offset7 + 1;
                dst[offset7] = (byte) ((c & '?') | 128);
            }
            i++;
            offset3 = offset2;
        }
    }

    public static byte[] encode(String s) throws UTFDataFormatException {
        long size = countBytes(s, true);
        byte[] output = new byte[((int) size) + 2];
        encode(output, 2, s);
        output[0] = (byte) (size >>> 8);
        output[1] = (byte) size;
        return output;
    }

    public static String decode(byte[] in, char[] out, int offset, int length) throws UTFDataFormatException {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("Illegal arguments: offset " + offset + ". Length: " + length);
        }
        int outputIndex = 0;
        int limitIndex = offset + length;
        while (offset < limitIndex) {
            int i = in[offset] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            offset++;
            if (i < 128) {
                out[outputIndex] = (char) i;
                outputIndex++;
            } else if (192 <= i && i < 224) {
                int i2 = (i & 31) << 6;
                if (offset == limitIndex) {
                    throw new UTFDataFormatException("unexpected end of input");
                }
                if ((in[offset] & 192) != 128) {
                    throw new UTFDataFormatException("bad second byte at " + offset);
                }
                out[outputIndex] = (char) ((in[offset] & 63) | i2);
                offset++;
                outputIndex++;
            } else if (i < 240) {
                int i3 = (i & 31) << 12;
                if (offset + 1 >= limitIndex) {
                    throw new UTFDataFormatException("unexpected end of input");
                }
                if ((in[offset] & 192) != 128) {
                    throw new UTFDataFormatException("bad second byte at " + offset);
                }
                int i4 = i3 | ((in[offset] & 63) << 6);
                int offset2 = offset + 1;
                if ((in[offset2] & 192) != 128) {
                    throw new UTFDataFormatException("bad third byte at " + offset2);
                }
                out[outputIndex] = (char) ((in[offset2] & 63) | i4);
                offset = offset2 + 1;
                outputIndex++;
            } else {
                throw new UTFDataFormatException("Invalid UTF8 byte " + i + " at position " + (offset - 1));
            }
        }
        return String.valueOf(out, 0, outputIndex);
    }
}
