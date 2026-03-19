package org.tukaani.xz.simple;

public final class PowerPC implements SimpleFilter {
    private final boolean isEncoder;
    private int pos;

    public PowerPC(boolean z, int i) {
        this.isEncoder = z;
        this.pos = i;
    }

    @Override
    public int code(byte[] bArr, int i, int i2) {
        int i3;
        int i4 = (i2 + i) - 4;
        int i5 = i;
        while (i5 <= i4) {
            if ((bArr[i5] & 252) == 72) {
                int i6 = i5 + 3;
                if ((bArr[i6] & 3) == 1) {
                    int i7 = i5 + 1;
                    int i8 = i5 + 2;
                    int i9 = ((bArr[i5] & 3) << 24) | ((bArr[i7] & 255) << 16) | ((bArr[i8] & 255) << 8) | (bArr[i6] & 252);
                    if (this.isEncoder) {
                        i3 = i9 + ((this.pos + i5) - i);
                    } else {
                        i3 = i9 - ((this.pos + i5) - i);
                    }
                    bArr[i5] = (byte) (72 | ((i3 >>> 24) & 3));
                    bArr[i7] = (byte) (i3 >>> 16);
                    bArr[i8] = (byte) (i3 >>> 8);
                    bArr[i6] = (byte) ((bArr[i6] & 3) | i3);
                }
            }
            i5 += 4;
        }
        int i10 = i5 - i;
        this.pos += i10;
        return i10;
    }
}
