package android.icu.impl;

import android.icu.text.UnicodeSet;
import android.icu.util.OutputInt;

public final class BMPSet {
    static final boolean $assertionsDisabled = false;
    public static int U16_SURROGATE_OFFSET = 56613888;
    private int[] bmpBlockBits;
    private boolean[] latin1Contains;
    private final int[] list;
    private int[] list4kStarts;
    private final int listLength;
    private int[] table7FF;

    public BMPSet(int[] iArr, int i) {
        this.list = iArr;
        this.listLength = i;
        this.latin1Contains = new boolean[256];
        this.table7FF = new int[64];
        this.bmpBlockBits = new int[64];
        this.list4kStarts = new int[18];
        this.list4kStarts[0] = findCodePoint(2048, 0, this.listLength - 1);
        for (int i2 = 1; i2 <= 16; i2++) {
            this.list4kStarts[i2] = findCodePoint(i2 << 12, this.list4kStarts[i2 - 1], this.listLength - 1);
        }
        this.list4kStarts[17] = this.listLength - 1;
        initBits();
    }

    public BMPSet(BMPSet bMPSet, int[] iArr, int i) {
        this.list = iArr;
        this.listLength = i;
        this.latin1Contains = (boolean[]) bMPSet.latin1Contains.clone();
        this.table7FF = (int[]) bMPSet.table7FF.clone();
        this.bmpBlockBits = (int[]) bMPSet.bmpBlockBits.clone();
        this.list4kStarts = (int[]) bMPSet.list4kStarts.clone();
    }

    public boolean contains(int i) {
        if (i <= 255) {
            return this.latin1Contains[i];
        }
        if (i <= 2047) {
            return ((1 << (i >> 6)) & this.table7FF[i & 63]) != 0;
        }
        if (i < 55296 || (i >= 57344 && i <= 65535)) {
            int i2 = i >> 12;
            int i3 = (this.bmpBlockBits[(i >> 6) & 63] >> i2) & 65537;
            if (i3 <= 1) {
                return i3 != 0;
            }
            return containsSlow(i, this.list4kStarts[i2], this.list4kStarts[i2 + 1]);
        }
        if (i <= 1114111) {
            return containsSlow(i, this.list4kStarts[13], this.list4kStarts[17]);
        }
        return false;
    }

    public final int span(CharSequence charSequence, int i, UnicodeSet.SpanCondition spanCondition, OutputInt outputInt) {
        int i2;
        int i3;
        char cCharAt;
        int i4;
        char cCharAt2;
        int length = charSequence.length();
        char c = 16;
        char c2 = 57344;
        char c3 = 55296;
        char c4 = 255;
        int i5 = 0;
        if (UnicodeSet.SpanCondition.NOT_CONTAINED != spanCondition) {
            i2 = i;
            while (i2 < length) {
                char cCharAt3 = charSequence.charAt(i2);
                if (cCharAt3 <= c4) {
                    if (!this.latin1Contains[cCharAt3]) {
                        break;
                    }
                    i4 = i2;
                } else if (cCharAt3 <= 2047) {
                    if ((this.table7FF[cCharAt3 & '?'] & (1 << (cCharAt3 >> 6))) == 0) {
                        break;
                    }
                    i4 = i2;
                } else if (cCharAt3 < c3 || cCharAt3 >= 56320 || (i4 = i2 + 1) == length || (cCharAt2 = charSequence.charAt(i4)) < 56320 || cCharAt2 >= 57344) {
                    int i6 = cCharAt3 >> '\f';
                    int i7 = (this.bmpBlockBits[(cCharAt3 >> 6) & 63] >> i6) & 65537;
                    if (i7 <= 1) {
                        if (i7 == 0) {
                            break;
                        }
                        i4 = i2;
                    } else {
                        if (!containsSlow(cCharAt3, this.list4kStarts[i6], this.list4kStarts[i6 + 1])) {
                            break;
                        }
                        i4 = i2;
                    }
                } else {
                    if (!containsSlow(Character.toCodePoint(cCharAt3, cCharAt2), this.list4kStarts[c], this.list4kStarts[17])) {
                        break;
                    }
                    i5++;
                }
                i2 = i4 + 1;
                c = 16;
                c3 = 55296;
                c4 = 255;
            }
        } else {
            i2 = i;
            while (i2 < length) {
                char cCharAt4 = charSequence.charAt(i2);
                if (cCharAt4 <= 255) {
                    if (this.latin1Contains[cCharAt4]) {
                        break;
                    }
                } else if (cCharAt4 <= 2047) {
                    if (((1 << (cCharAt4 >> 6)) & this.table7FF[cCharAt4 & '?']) != 0) {
                        break;
                    }
                } else if (cCharAt4 < 55296 || cCharAt4 >= 56320 || (i3 = i2 + 1) == length || (cCharAt = charSequence.charAt(i3)) < 56320 || cCharAt >= c2) {
                    int i8 = cCharAt4 >> '\f';
                    int i9 = (this.bmpBlockBits[(cCharAt4 >> 6) & 63] >> i8) & 65537;
                    if (i9 <= 1) {
                        if (i9 != 0) {
                            break;
                        }
                    } else if (containsSlow(cCharAt4, this.list4kStarts[i8], this.list4kStarts[i8 + 1])) {
                        break;
                    }
                    i2 = i3 + 1;
                    c2 = 57344;
                } else {
                    if (containsSlow(Character.toCodePoint(cCharAt4, cCharAt), this.list4kStarts[16], this.list4kStarts[17])) {
                        break;
                    }
                    i5++;
                    i2 = i3 + 1;
                    c2 = 57344;
                }
                i3 = i2;
                i2 = i3 + 1;
                c2 = 57344;
            }
        }
        if (outputInt != null) {
            outputInt.value = (i2 - i) - i5;
        }
        return i2;
    }

    public final int spanBack(CharSequence charSequence, int i, UnicodeSet.SpanCondition spanCondition) {
        char cCharAt;
        char cCharAt2;
        if (UnicodeSet.SpanCondition.NOT_CONTAINED != spanCondition) {
            do {
                i--;
                char cCharAt3 = charSequence.charAt(i);
                if (cCharAt3 <= 255) {
                    if (!this.latin1Contains[cCharAt3]) {
                    }
                } else if (cCharAt3 <= 2047) {
                    if (((1 << (cCharAt3 >> 6)) & this.table7FF[cCharAt3 & '?']) == 0) {
                    }
                } else if (cCharAt3 < 55296 || cCharAt3 < 56320 || i == 0 || (cCharAt2 = charSequence.charAt(i - 1)) < 55296 || cCharAt2 >= 56320) {
                    int i2 = cCharAt3 >> '\f';
                    int i3 = (this.bmpBlockBits[(cCharAt3 >> 6) & 63] >> i2) & 65537;
                    if (i3 <= 1) {
                        if (i3 == 0) {
                        }
                    } else if (!containsSlow(cCharAt3, this.list4kStarts[i2], this.list4kStarts[i2 + 1])) {
                    }
                } else if (containsSlow(Character.toCodePoint(cCharAt2, cCharAt3), this.list4kStarts[16], this.list4kStarts[17])) {
                    i--;
                }
            } while (i != 0);
            return 0;
        }
        do {
            i--;
            char cCharAt4 = charSequence.charAt(i);
            if (cCharAt4 <= 255) {
                if (this.latin1Contains[cCharAt4]) {
                }
            } else if (cCharAt4 <= 2047) {
                if (((1 << (cCharAt4 >> 6)) & this.table7FF[cCharAt4 & '?']) != 0) {
                }
            } else if (cCharAt4 < 55296 || cCharAt4 < 56320 || i == 0 || (cCharAt = charSequence.charAt(i - 1)) < 55296 || cCharAt >= 56320) {
                int i4 = cCharAt4 >> '\f';
                int i5 = (this.bmpBlockBits[(cCharAt4 >> 6) & 63] >> i4) & 65537;
                if (i5 <= 1) {
                    if (i5 != 0) {
                    }
                } else if (containsSlow(cCharAt4, this.list4kStarts[i4], this.list4kStarts[i4 + 1])) {
                }
            } else if (!containsSlow(Character.toCodePoint(cCharAt, cCharAt4), this.list4kStarts[16], this.list4kStarts[17])) {
                i--;
            }
        } while (i != 0);
        return 0;
        return i + 1;
    }

    private static void set32x64Bits(int[] iArr, int i, int i2) {
        int i3 = i >> 6;
        int i4 = i & 63;
        int i5 = 1 << i3;
        if (i + 1 == i2) {
            iArr[i4] = iArr[i4] | i5;
            return;
        }
        int i6 = i2 >> 6;
        int i7 = i2 & 63;
        if (i3 == i6) {
            while (i4 < i7) {
                iArr[i4] = iArr[i4] | i5;
                i4++;
            }
            return;
        }
        if (i4 > 0) {
            while (true) {
                int i8 = i4 + 1;
                iArr[i4] = iArr[i4] | i5;
                if (i8 >= 64) {
                    break;
                } else {
                    i4 = i8;
                }
            }
            i3++;
        }
        if (i3 < i6) {
            int i9 = ~((1 << i3) - 1);
            if (i6 < 32) {
                i9 &= (1 << i6) - 1;
            }
            for (int i10 = 0; i10 < 64; i10++) {
                iArr[i10] = iArr[i10] | i9;
            }
        }
        int i11 = 1 << i6;
        for (int i12 = 0; i12 < i7; i12++) {
            iArr[i12] = iArr[i12] | i11;
        }
    }

    private void initBits() {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6 = 0;
        while (true) {
            i = i6 + 1;
            i2 = this.list[i6];
            if (i < this.listLength) {
                i3 = this.list[i];
                i++;
            } else {
                i3 = 1114112;
            }
            if (i2 >= 256) {
                break;
            }
            while (true) {
                i5 = i2 + 1;
                this.latin1Contains[i2] = true;
                if (i5 >= i3 || i5 >= 256) {
                    break;
                } else {
                    i2 = i5;
                }
            }
            if (i3 <= 256) {
                i6 = i;
            } else {
                i2 = i5;
                break;
            }
        }
        while (true) {
            i4 = 2048;
            if (i2 >= 2048) {
                break;
            }
            set32x64Bits(this.table7FF, i2, i3 <= 2048 ? i3 : 2048);
            if (i3 <= 2048) {
                int i7 = i + 1;
                i2 = this.list[i];
                if (i7 >= this.listLength) {
                    i = i7;
                    i3 = 1114112;
                } else {
                    int i8 = i7 + 1;
                    i3 = this.list[i7];
                    i = i8;
                }
            } else {
                i2 = 2048;
                break;
            }
        }
        while (i2 < 65536) {
            if (i3 > 65536) {
                i3 = 65536;
            }
            if (i2 < i4) {
                i2 = i4;
            }
            if (i2 < i3) {
                if ((i2 & 63) != 0) {
                    int i9 = i2 >> 6;
                    int[] iArr = this.bmpBlockBits;
                    int i10 = i9 & 63;
                    iArr[i10] = iArr[i10] | (65537 << (i9 >> 6));
                    i4 = (i9 + 1) << 6;
                    i2 = i4;
                }
                if (i2 < i3) {
                    if (i2 < (i3 & (-64))) {
                        set32x64Bits(this.bmpBlockBits, i2 >> 6, i3 >> 6);
                    }
                    if ((i3 & 63) != 0) {
                        int i11 = i3 >> 6;
                        int[] iArr2 = this.bmpBlockBits;
                        int i12 = i11 & 63;
                        iArr2[i12] = iArr2[i12] | (65537 << (i11 >> 6));
                        i3 = (i11 + 1) << 6;
                        i4 = i3;
                    }
                }
            }
            if (i3 != 65536) {
                int i13 = i + 1;
                i2 = this.list[i];
                if (i13 >= this.listLength) {
                    i = i13;
                    i3 = 1114112;
                } else {
                    int i14 = i13 + 1;
                    i3 = this.list[i13];
                    i = i14;
                }
            } else {
                return;
            }
        }
    }

    private int findCodePoint(int i, int i2, int i3) {
        if (i < this.list[i2]) {
            return i2;
        }
        if (i2 >= i3 || i >= this.list[i3 - 1]) {
            return i3;
        }
        while (true) {
            int i4 = (i2 + i3) >>> 1;
            if (i4 != i2) {
                if (i < this.list[i4]) {
                    i3 = i4;
                } else {
                    i2 = i4;
                }
            } else {
                return i3;
            }
        }
    }

    private final boolean containsSlow(int i, int i2, int i3) {
        return (findCodePoint(i, i2, i3) & 1) != 0;
    }
}
