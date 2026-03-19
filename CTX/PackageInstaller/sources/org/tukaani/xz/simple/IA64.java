package org.tukaani.xz.simple;

public final class IA64 implements SimpleFilter {
    private static final int[] BRANCH_TABLE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 6, 6, 0, 0, 7, 7, 4, 4, 0, 0, 4, 4, 0, 0};
    private final boolean isEncoder;
    private int pos;

    public IA64(boolean z, int i) {
        this.isEncoder = z;
        this.pos = i;
    }

    @Override
    public int code(byte[] bArr, int i, int i2) {
        int i3;
        int i4;
        char c;
        int i5;
        char c2 = 16;
        int i6 = (i + i2) - 16;
        int i7 = i;
        while (i7 <= i6) {
            int i8 = BRANCH_TABLE[bArr[i7] & 31];
            int i9 = 5;
            int i10 = 0;
            while (i10 < 3) {
                if (((i8 >>> i10) & 1) == 0) {
                    c = c2;
                    i3 = i7;
                    i4 = i9;
                } else {
                    int i11 = i9 >>> 3;
                    int i12 = i9 & 7;
                    long j = 0;
                    int i13 = 0;
                    while (i13 < 6) {
                        j |= (((long) bArr[(i7 + i11) + i13]) & 255) << (8 * i13);
                        i13++;
                        i7 = i7;
                    }
                    i3 = i7;
                    long j2 = j >>> i12;
                    if (((j2 >>> 37) & 15) != 5 || ((j2 >>> 9) & 7) != 0) {
                        i4 = i9;
                        c = 16;
                    } else {
                        i4 = i9;
                        int i14 = (((((int) (j2 >>> 36)) & 1) << 20) | ((int) ((j2 >>> 13) & 1048575))) << 4;
                        if (this.isEncoder) {
                            i5 = i14 + ((this.pos + i3) - i);
                        } else {
                            i5 = i14 - ((this.pos + i3) - i);
                        }
                        long j3 = i5 >>> 4;
                        c = 16;
                        long j4 = ((((j2 & (-77309403137L)) | ((j3 & 1048575) << 13)) | ((j3 & 1048576) << 16)) << i12) | (((long) ((1 << i12) - 1)) & j);
                        for (int i15 = 0; i15 < 6; i15++) {
                            bArr[i3 + i11 + i15] = (byte) (j4 >>> (8 * i15));
                        }
                    }
                }
                i10++;
                i9 = i4 + 41;
                c2 = c;
                i7 = i3;
            }
            i7 += 16;
        }
        int i16 = i7 - i;
        this.pos += i16;
        return i16;
    }
}
