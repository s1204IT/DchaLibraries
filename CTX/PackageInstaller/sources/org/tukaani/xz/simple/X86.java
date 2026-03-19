package org.tukaani.xz.simple;

public final class X86 implements SimpleFilter {
    private static final boolean[] MASK_TO_ALLOWED_STATUS = {true, true, true, false, true, false, false, false};
    private static final int[] MASK_TO_BIT_NUMBER = {0, 1, 2, 2, 3, 3, 3, 3};
    private final boolean isEncoder;
    private int pos;
    private int prevMask = 0;

    private static boolean test86MSByte(byte b) {
        int i = b & 255;
        return i == 0 || i == 255;
    }

    public X86(boolean z, int i) {
        this.isEncoder = z;
        this.pos = i + 5;
    }

    @Override
    public int code(byte[] bArr, int i, int i2) {
        int i3;
        int i4 = (i2 + i) - 5;
        int i5 = i - 1;
        int i6 = i;
        while (true) {
            if (i6 > i4) {
                break;
            }
            if ((bArr[i6] & 254) == 232) {
                int i7 = i6 - i5;
                if ((i7 & (-4)) != 0) {
                    this.prevMask = 0;
                } else {
                    this.prevMask = (this.prevMask << (i7 - 1)) & 7;
                    if (this.prevMask != 0 && (!MASK_TO_ALLOWED_STATUS[this.prevMask] || test86MSByte(bArr[(i6 + 4) - MASK_TO_BIT_NUMBER[this.prevMask]]))) {
                        this.prevMask = (this.prevMask << 1) | 1;
                    }
                    i5 = i6;
                }
                int i8 = i6 + 4;
                if (test86MSByte(bArr[i8])) {
                    int i9 = i6 + 1;
                    int i10 = i6 + 2;
                    int i11 = i6 + 3;
                    int i12 = (bArr[i9] & 255) | ((bArr[i10] & 255) << 8) | ((bArr[i11] & 255) << 16) | ((bArr[i8] & 255) << 24);
                    while (true) {
                        if (this.isEncoder) {
                            i3 = i12 + ((this.pos + i6) - i);
                        } else {
                            i3 = i12 - ((this.pos + i6) - i);
                        }
                        if (this.prevMask == 0) {
                            break;
                        }
                        int i13 = MASK_TO_BIT_NUMBER[this.prevMask] * 8;
                        if (!test86MSByte((byte) (i3 >>> (24 - i13)))) {
                            break;
                        }
                        i12 = i3 ^ ((1 << (32 - i13)) - 1);
                    }
                    bArr[i9] = (byte) i3;
                    bArr[i10] = (byte) (i3 >>> 8);
                    bArr[i11] = (byte) (i3 >>> 16);
                    bArr[i8] = (byte) (~(((i3 >>> 24) & 1) - 1));
                    i5 = i6;
                    i6 = i8;
                } else {
                    this.prevMask = (this.prevMask << 1) | 1;
                    i5 = i6;
                }
            }
            i6++;
        }
        int i14 = i6 - i5;
        this.prevMask = (i14 & (-4)) == 0 ? this.prevMask << (i14 - 1) : 0;
        int i15 = i6 - i;
        this.pos += i15;
        return i15;
    }
}
