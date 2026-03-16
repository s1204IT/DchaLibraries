package com.android.gallery3d.jpegstream;

import java.nio.ByteOrder;

public class StreamUtils {
    private StreamUtils() {
    }

    public static boolean byteToIntArray(int[] output, byte[] input, ByteOrder endianness) {
        int length = input.length - (input.length % 4);
        if (output.length * 4 < length) {
            throw new ArrayIndexOutOfBoundsException("Output array is too short to hold input");
        }
        if (endianness == ByteOrder.BIG_ENDIAN) {
            int i = 0;
            int j = 0;
            while (i < output.length) {
                output[i] = ((input[j] & 255) << 24) | ((input[j + 1] & 255) << 16) | ((input[j + 2] & 255) << 8) | (input[j + 3] & 255);
                i++;
                j += 4;
            }
        } else {
            int i2 = 0;
            int j2 = 0;
            while (i2 < output.length) {
                output[i2] = ((input[j2 + 3] & 255) << 24) | ((input[j2 + 2] & 255) << 16) | ((input[j2 + 1] & 255) << 8) | (input[j2] & 255);
                i2++;
                j2 += 4;
            }
        }
        return input.length % 4 != 0;
    }

    public static int[] byteToIntArray(byte[] input, ByteOrder endianness) {
        int[] output = new int[input.length / 4];
        byteToIntArray(output, input, endianness);
        return output;
    }

    public static int[] byteToIntArray(byte[] input) {
        return byteToIntArray(input, ByteOrder.nativeOrder());
    }

    public static int pixelSize(int format) {
        switch (format) {
            case 1:
                return 1;
            case 3:
                return 3;
            case 4:
            case 260:
                return 4;
            default:
                return -1;
        }
    }
}
