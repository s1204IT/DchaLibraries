package org.tukaani.xz.simple;

public final class SPARC implements SimpleFilter {
    private final boolean isEncoder;
    private int pos;

    public SPARC(boolean z, int i) {
        this.isEncoder = z;
        this.pos = i;
    }

    @Override
    public int code(byte[] bArr, int i, int i2) {
        int i3;
        int i4 = (i2 + i) - 4;
        int i5 = i;
        while (i5 <= i4) {
            if ((bArr[i5] == 64 && (bArr[i5 + 1] & 192) == 0) || (bArr[i5] == 127 && (bArr[i5 + 1] & 192) == 192)) {
                int i6 = i5 + 1;
                int i7 = i5 + 2;
                int i8 = i5 + 3;
                int i9 = (((((bArr[i5] & 255) << 24) | ((bArr[i6] & 255) << 16)) | ((bArr[i7] & 255) << 8)) | (bArr[i8] & 255)) << 2;
                if (this.isEncoder) {
                    i3 = i9 + ((this.pos + i5) - i);
                } else {
                    i3 = i9 - ((this.pos + i5) - i);
                }
                int i10 = i3 >>> 2;
                int i11 = (i10 & 4194303) | (((0 - ((i10 >>> 22) & 1)) << 22) & 1073741823) | 1073741824;
                bArr[i5] = (byte) (i11 >>> 24);
                bArr[i6] = (byte) (i11 >>> 16);
                bArr[i7] = (byte) (i11 >>> 8);
                bArr[i8] = (byte) i11;
            }
            i5 += 4;
        }
        int i12 = i5 - i;
        this.pos += i12;
        return i12;
    }
}
