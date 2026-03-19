package android.icu.text;

import android.icu.util.ByteArrayWrapper;

public final class RawCollationKey extends ByteArrayWrapper {
    public RawCollationKey() {
    }

    public RawCollationKey(int i) {
        this.bytes = new byte[i];
    }

    public RawCollationKey(byte[] bArr) {
        this.bytes = bArr;
    }

    public RawCollationKey(byte[] bArr, int i) {
        super(bArr, i);
    }

    public int compareTo(RawCollationKey rawCollationKey) {
        int iCompareTo = super.compareTo((ByteArrayWrapper) rawCollationKey);
        if (iCompareTo < 0) {
            return -1;
        }
        return iCompareTo == 0 ? 0 : 1;
    }
}
