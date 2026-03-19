package com.android.providers.contacts.aggregation.util;

import java.util.Arrays;

public class NameDistance {
    private final boolean[] mMatchFlags1;
    private final boolean[] mMatchFlags2;
    private final int mMaxLength;
    private final boolean mPrefixOnly;

    public NameDistance(int i) {
        this.mMaxLength = i;
        this.mPrefixOnly = false;
        this.mMatchFlags1 = new boolean[i];
        this.mMatchFlags2 = new boolean[i];
    }

    public NameDistance() {
        this.mPrefixOnly = true;
        this.mMaxLength = 0;
        this.mMatchFlags2 = null;
        this.mMatchFlags1 = null;
    }

    public float getDistance(byte[] bArr, byte[] bArr2) {
        byte[] bArr3;
        byte[] bArr4;
        boolean z;
        if (bArr.length > bArr2.length) {
            bArr4 = bArr;
            bArr3 = bArr2;
        } else {
            bArr3 = bArr;
            bArr4 = bArr2;
        }
        int length = bArr3.length;
        int i = 0;
        if (length >= 3) {
            int i2 = 0;
            while (true) {
                if (i2 < bArr3.length) {
                    if (bArr3[i2] == bArr4[i2]) {
                        i2++;
                    } else {
                        z = false;
                        break;
                    }
                } else {
                    z = true;
                    break;
                }
            }
            if (z) {
                return 1.0f;
            }
        }
        if (this.mPrefixOnly) {
            return 0.0f;
        }
        if (length > this.mMaxLength) {
            length = this.mMaxLength;
        }
        int length2 = bArr4.length;
        if (length2 > this.mMaxLength) {
            length2 = this.mMaxLength;
        }
        Arrays.fill(this.mMatchFlags1, 0, length, false);
        Arrays.fill(this.mMatchFlags2, 0, length2, false);
        int i3 = (length2 / 2) - 1;
        if (i3 < 0) {
            i3 = 0;
        }
        int i4 = 0;
        int i5 = 0;
        while (i4 < length) {
            byte b = bArr3[i4];
            int i6 = i4 - i3;
            if (i6 < 0) {
                i6 = i;
            }
            int i7 = i4 + i3 + 1;
            if (i7 > length2) {
                i7 = length2;
            }
            while (true) {
                if (i6 >= i7) {
                    break;
                }
                if (this.mMatchFlags2[i6] || b != bArr4[i6]) {
                    i6++;
                } else {
                    boolean[] zArr = this.mMatchFlags1;
                    this.mMatchFlags2[i6] = true;
                    zArr[i4] = true;
                    i5++;
                    break;
                }
            }
            i4++;
            i = 0;
        }
        if (i5 == 0) {
            return 0.0f;
        }
        int i8 = 0;
        int i9 = 0;
        for (int i10 = 0; i10 < length; i10++) {
            if (this.mMatchFlags1[i10]) {
                while (!this.mMatchFlags2[i9]) {
                    i9++;
                }
                if (bArr3[i10] != bArr4[i9]) {
                    i8++;
                }
                i9++;
            }
        }
        float f = i5;
        float f2 = length2;
        float f3 = (((f / length) + (f / f2)) + ((f - (i8 / 2.0f)) / f)) / 3.0f;
        if (f3 < 0.7f) {
            return f3;
        }
        int i11 = 0;
        for (int i12 = 0; i12 < length && bArr[i12] == bArr2[i12]; i12++) {
            i11++;
        }
        return f3 + (Math.min(0.1f, 1.0f / f2) * i11 * (1.0f - f3));
    }
}
