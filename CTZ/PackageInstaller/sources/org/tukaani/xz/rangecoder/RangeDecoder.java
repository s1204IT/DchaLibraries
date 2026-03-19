package org.tukaani.xz.rangecoder;

import java.io.IOException;

public abstract class RangeDecoder extends RangeCoder {
    int range = 0;
    int code = 0;

    public abstract void normalize() throws IOException;

    public int decodeBit(short[] sArr, int i) throws IOException {
        normalize();
        short s = sArr[i];
        int i2 = (this.range >>> 11) * s;
        if ((this.code ^ Integer.MIN_VALUE) < (Integer.MIN_VALUE ^ i2)) {
            this.range = i2;
            sArr[i] = (short) (s + ((2048 - s) >>> 5));
            return 0;
        }
        this.range -= i2;
        this.code -= i2;
        sArr[i] = (short) (s - (s >>> 5));
        return 1;
    }

    public int decodeBitTree(short[] sArr) throws IOException {
        int iDecodeBit = 1;
        do {
            iDecodeBit = decodeBit(sArr, iDecodeBit) | (iDecodeBit << 1);
        } while (iDecodeBit < sArr.length);
        return iDecodeBit - sArr.length;
    }

    public int decodeReverseBitTree(short[] sArr) throws IOException {
        int i = 0;
        int i2 = 0;
        int i3 = 1;
        while (true) {
            int iDecodeBit = decodeBit(sArr, i3);
            i3 = (i3 << 1) | iDecodeBit;
            int i4 = i2 + 1;
            i |= iDecodeBit << i2;
            if (i3 < sArr.length) {
                i2 = i4;
            } else {
                return i;
            }
        }
    }

    public int decodeDirectBits(int i) throws IOException {
        int i2 = 0;
        do {
            normalize();
            this.range >>>= 1;
            int i3 = (this.code - this.range) >>> 31;
            this.code -= this.range & (i3 - 1);
            i2 = (i2 << 1) | (1 - i3);
            i--;
        } while (i != 0);
        return i2;
    }
}
