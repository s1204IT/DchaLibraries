package org.tukaani.xz.simple;

public final class ARMThumb implements SimpleFilter {
    private final boolean isEncoder;
    private int pos;

    public ARMThumb(boolean z, int i) {
        this.isEncoder = z;
        this.pos = i + 4;
    }

    @Override
    public int code(byte[] bArr, int i, int i2) {
        int i3;
        int i4 = (i2 + i) - 4;
        int i5 = i;
        while (i5 <= i4) {
            int i6 = i5 + 1;
            if ((bArr[i6] & 248) == 240) {
                int i7 = i5 + 3;
                if ((bArr[i7] & 248) == 248) {
                    int i8 = i5 + 2;
                    int i9 = (((((bArr[i6] & 7) << 19) | ((bArr[i5] & 255) << 11)) | ((bArr[i7] & 7) << 8)) | (bArr[i8] & 255)) << 1;
                    if (this.isEncoder) {
                        i3 = i9 + ((this.pos + i5) - i);
                    } else {
                        i3 = i9 - ((this.pos + i5) - i);
                    }
                    int i10 = i3 >>> 1;
                    bArr[i6] = (byte) (240 | ((i10 >>> 19) & 7));
                    bArr[i5] = (byte) (i10 >>> 11);
                    bArr[i7] = (byte) (((i10 >>> 8) & 7) | 248);
                    bArr[i8] = (byte) i10;
                    i5 = i8;
                }
            }
            i5 += 2;
        }
        int i11 = i5 - i;
        this.pos += i11;
        return i11;
    }
}
