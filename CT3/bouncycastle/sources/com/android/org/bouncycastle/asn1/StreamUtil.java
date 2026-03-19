package com.android.org.bouncycastle.asn1;

import java.io.IOException;

class StreamUtil {
    StreamUtil() {
    }

    static int findLimit(java.io.InputStream r10) {
        throw new UnsupportedOperationException("Method not decompiled: com.android.org.bouncycastle.asn1.StreamUtil.findLimit(java.io.InputStream):int");
    }

    static int calculateBodyLength(int length) {
        int count = 1;
        if (length > 127) {
            int size = 1;
            int val = length;
            while (true) {
                val >>>= 8;
                if (val == 0) {
                    break;
                }
                size++;
            }
            for (int i = (size - 1) * 8; i >= 0; i -= 8) {
                count++;
            }
        }
        return count;
    }

    static int calculateTagLength(int tagNo) throws IOException {
        if (tagNo < 31) {
            return 1;
        }
        if (tagNo < 128) {
            return 2;
        }
        byte[] stack = new byte[5];
        int pos = stack.length - 1;
        stack[pos] = (byte) (tagNo & 127);
        do {
            tagNo >>= 7;
            pos--;
            stack[pos] = (byte) ((tagNo & 127) | 128);
        } while (tagNo > 127);
        int length = (stack.length - pos) + 1;
        return length;
    }
}
