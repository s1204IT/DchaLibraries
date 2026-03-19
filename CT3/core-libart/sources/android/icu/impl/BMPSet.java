package android.icu.impl;

import android.icu.text.UnicodeSet;
import android.icu.util.OutputInt;

public final class BMPSet {

    static final boolean f0assertionsDisabled;
    public static int U16_SURROGATE_OFFSET;
    private int[] bmpBlockBits;
    private boolean[] latin1Contains;
    private final int[] list;
    private int[] list4kStarts;
    private final int listLength;
    private int[] table7FF;

    static {
        f0assertionsDisabled = !BMPSet.class.desiredAssertionStatus();
        U16_SURROGATE_OFFSET = 56613888;
    }

    public BMPSet(int[] parentList, int parentListLength) {
        this.list = parentList;
        this.listLength = parentListLength;
        this.latin1Contains = new boolean[256];
        this.table7FF = new int[64];
        this.bmpBlockBits = new int[64];
        this.list4kStarts = new int[18];
        this.list4kStarts[0] = findCodePoint(2048, 0, this.listLength - 1);
        for (int i = 1; i <= 16; i++) {
            this.list4kStarts[i] = findCodePoint(i << 12, this.list4kStarts[i - 1], this.listLength - 1);
        }
        this.list4kStarts[17] = this.listLength - 1;
        initBits();
    }

    public BMPSet(BMPSet otherBMPSet, int[] newParentList, int newParentListLength) {
        this.list = newParentList;
        this.listLength = newParentListLength;
        this.latin1Contains = (boolean[]) otherBMPSet.latin1Contains.clone();
        this.table7FF = (int[]) otherBMPSet.table7FF.clone();
        this.bmpBlockBits = (int[]) otherBMPSet.bmpBlockBits.clone();
        this.list4kStarts = (int[]) otherBMPSet.list4kStarts.clone();
    }

    public boolean contains(int c) {
        if (c <= 255) {
            return this.latin1Contains[c];
        }
        if (c <= 2047) {
            return (this.table7FF[c & 63] & (1 << (c >> 6))) != 0;
        }
        if (c < 55296 || (c >= 57344 && c <= 65535)) {
            int lead = c >> 12;
            int twoBits = (this.bmpBlockBits[(c >> 6) & 63] >> lead) & 65537;
            if (twoBits <= 1) {
                return twoBits != 0;
            }
            return containsSlow(c, this.list4kStarts[lead], this.list4kStarts[lead + 1]);
        }
        if (c <= 1114111) {
            return containsSlow(c, this.list4kStarts[13], this.list4kStarts[17]);
        }
        return false;
    }

    public final int span(CharSequence s, int start, UnicodeSet.SpanCondition spanCondition, OutputInt outCount) {
        char c2;
        char c22;
        int i = start;
        int limit = s.length();
        int numSupplementary = 0;
        if (UnicodeSet.SpanCondition.NOT_CONTAINED != spanCondition) {
            while (i < limit) {
                char c = s.charAt(i);
                if (c <= 255) {
                    if (!this.latin1Contains[c]) {
                        break;
                    }
                    i++;
                } else if (c <= 2047) {
                    if ((this.table7FF[c & '?'] & (1 << (c >> 6))) == 0) {
                        break;
                    }
                    i++;
                } else if (c < 55296 || c >= 56320 || i + 1 == limit || (c22 = s.charAt(i + 1)) < 56320 || c22 >= 57344) {
                    int lead = c >> '\f';
                    int twoBits = (this.bmpBlockBits[(c >> 6) & 63] >> lead) & 65537;
                    if (twoBits <= 1) {
                        if (twoBits == 0) {
                            break;
                        }
                        i++;
                    } else {
                        if (!containsSlow(c, this.list4kStarts[lead], this.list4kStarts[lead + 1])) {
                            break;
                        }
                        i++;
                    }
                } else {
                    int supplementary = Character.toCodePoint(c, c22);
                    if (!containsSlow(supplementary, this.list4kStarts[16], this.list4kStarts[17])) {
                        break;
                    }
                    numSupplementary++;
                    i++;
                    i++;
                }
            }
        } else {
            while (i < limit) {
                char c3 = s.charAt(i);
                if (c3 <= 255) {
                    if (this.latin1Contains[c3]) {
                        break;
                    }
                    i++;
                } else if (c3 <= 2047) {
                    if ((this.table7FF[c3 & '?'] & (1 << (c3 >> 6))) != 0) {
                        break;
                    }
                    i++;
                } else if (c3 < 55296 || c3 >= 56320 || i + 1 == limit || (c2 = s.charAt(i + 1)) < 56320 || c2 >= 57344) {
                    int lead2 = c3 >> '\f';
                    int twoBits2 = (this.bmpBlockBits[(c3 >> 6) & 63] >> lead2) & 65537;
                    if (twoBits2 <= 1) {
                        if (twoBits2 != 0) {
                            break;
                        }
                        i++;
                    } else {
                        if (containsSlow(c3, this.list4kStarts[lead2], this.list4kStarts[lead2 + 1])) {
                            break;
                        }
                        i++;
                    }
                } else {
                    int supplementary2 = Character.toCodePoint(c3, c2);
                    if (containsSlow(supplementary2, this.list4kStarts[16], this.list4kStarts[17])) {
                        break;
                    }
                    numSupplementary++;
                    i++;
                    i++;
                }
            }
        }
        if (outCount != null) {
            int spanLength = i - start;
            outCount.value = spanLength - numSupplementary;
        }
        return i;
    }

    public final int spanBack(CharSequence s, int limit, UnicodeSet.SpanCondition spanCondition) {
        char c2;
        char c22;
        if (UnicodeSet.SpanCondition.NOT_CONTAINED != spanCondition) {
            do {
                limit--;
                char c = s.charAt(limit);
                if (c <= 255) {
                    if (!this.latin1Contains[c]) {
                    }
                } else if (c <= 2047) {
                    if ((this.table7FF[c & '?'] & (1 << (c >> 6))) != 0) {
                    }
                } else if (c < 55296 || c < 56320 || limit == 0 || (c22 = s.charAt(limit - 1)) < 55296 || c22 >= 56320) {
                    int lead = c >> '\f';
                    int twoBits = (this.bmpBlockBits[(c >> 6) & 63] >> lead) & 65537;
                    if (twoBits <= 1) {
                        if (twoBits == 0) {
                        }
                    } else if (!containsSlow(c, this.list4kStarts[lead], this.list4kStarts[lead + 1])) {
                    }
                } else {
                    int supplementary = Character.toCodePoint(c22, c);
                    if (containsSlow(supplementary, this.list4kStarts[16], this.list4kStarts[17])) {
                        limit--;
                    }
                }
            } while (limit != 0);
            return 0;
        }
        do {
            limit--;
            char c3 = s.charAt(limit);
            if (c3 <= 255) {
                if (!this.latin1Contains[c3]) {
                }
            } else if (c3 <= 2047) {
                if ((this.table7FF[c3 & '?'] & (1 << (c3 >> 6))) != 0) {
                }
            } else if (c3 < 55296 || c3 < 56320 || limit == 0 || (c2 = s.charAt(limit - 1)) < 55296 || c2 >= 56320) {
                int lead2 = c3 >> '\f';
                int twoBits2 = (this.bmpBlockBits[(c3 >> 6) & 63] >> lead2) & 65537;
                if (twoBits2 <= 1) {
                    if (twoBits2 != 0) {
                    }
                } else if (containsSlow(c3, this.list4kStarts[lead2], this.list4kStarts[lead2 + 1])) {
                }
            } else {
                int supplementary2 = Character.toCodePoint(c2, c3);
                if (!containsSlow(supplementary2, this.list4kStarts[16], this.list4kStarts[17])) {
                    limit--;
                }
            }
        } while (limit != 0);
        return 0;
        return limit + 1;
    }

    private static void set32x64Bits(int[] table, int start, int limit) {
        if (!f0assertionsDisabled) {
            if (!(64 == table.length)) {
                throw new AssertionError();
            }
        }
        int lead = start >> 6;
        int trail = start & 63;
        int bits = 1 << lead;
        if (start + 1 == limit) {
            table[trail] = table[trail] | bits;
            return;
        }
        int limitLead = limit >> 6;
        int limitTrail = limit & 63;
        if (lead != limitLead) {
            if (trail > 0) {
                while (true) {
                    int trail2 = trail + 1;
                    table[trail] = table[trail] | bits;
                    if (trail2 >= 64) {
                        break;
                    } else {
                        trail = trail2;
                    }
                }
                lead++;
            }
            if (lead < limitLead) {
                int bits2 = ~((1 << lead) - 1);
                if (limitLead < 32) {
                    bits2 &= (1 << limitLead) - 1;
                }
                for (int trail3 = 0; trail3 < 64; trail3++) {
                    table[trail3] = table[trail3] | bits2;
                }
            }
            int bits3 = 1 << limitLead;
            for (int trail4 = 0; trail4 < limitTrail; trail4++) {
                table[trail4] = table[trail4] | bits3;
            }
            return;
        }
        while (true) {
            int trail5 = trail;
            if (trail5 >= limitTrail) {
                return;
            }
            trail = trail5 + 1;
            table[trail5] = table[trail5] | bits;
        }
    }

    private void initBits() {
        int start;
        int limit;
        int listIndex;
        int start2;
        int listIndex2 = 0;
        while (true) {
            int listIndex3 = listIndex2 + 1;
            start = this.list[listIndex2];
            if (listIndex3 < this.listLength) {
                listIndex2 = listIndex3 + 1;
                limit = this.list[listIndex3];
            } else {
                limit = 1114112;
                listIndex2 = listIndex3;
            }
            if (start >= 256) {
                listIndex = listIndex2;
                break;
            }
            while (true) {
                start2 = start + 1;
                this.latin1Contains[start] = true;
                if (start2 >= limit || start2 >= 256) {
                    break;
                } else {
                    start = start2;
                }
            }
            if (limit > 256) {
                listIndex = listIndex2;
                start = start2;
                break;
            }
        }
        while (true) {
            if (start >= 2048) {
                break;
            }
            set32x64Bits(this.table7FF, start, limit <= 2048 ? limit : 2048);
            if (limit > 2048) {
                start = 2048;
                break;
            }
            int listIndex4 = listIndex + 1;
            start = this.list[listIndex];
            if (listIndex4 < this.listLength) {
                limit = this.list[listIndex4];
                listIndex4++;
            } else {
                limit = 1114112;
            }
            listIndex = listIndex4;
        }
        int minStart = 2048;
        while (start < 65536) {
            if (limit > 65536) {
                limit = 65536;
            }
            if (start < minStart) {
                start = minStart;
            }
            if (start < limit) {
                if ((start & 63) != 0) {
                    int start3 = start >> 6;
                    int[] iArr = this.bmpBlockBits;
                    int i = start3 & 63;
                    iArr[i] = iArr[i] | (65537 << (start3 >> 6));
                    start = (start3 + 1) << 6;
                    minStart = start;
                }
                if (start < limit) {
                    if (start < (limit & (-64))) {
                        set32x64Bits(this.bmpBlockBits, start >> 6, limit >> 6);
                    }
                    if ((limit & 63) != 0) {
                        int limit2 = limit >> 6;
                        int[] iArr2 = this.bmpBlockBits;
                        int i2 = limit2 & 63;
                        iArr2[i2] = iArr2[i2] | (65537 << (limit2 >> 6));
                        limit = (limit2 + 1) << 6;
                        minStart = limit;
                    }
                }
            }
            if (limit == 65536) {
                return;
            }
            int listIndex5 = listIndex + 1;
            start = this.list[listIndex];
            if (listIndex5 < this.listLength) {
                limit = this.list[listIndex5];
                listIndex5++;
            } else {
                limit = 1114112;
            }
            listIndex = listIndex5;
        }
    }

    private int findCodePoint(int c, int lo, int hi) {
        if (c < this.list[lo]) {
            return lo;
        }
        if (lo >= hi || c >= this.list[hi - 1]) {
            return hi;
        }
        while (true) {
            int i = (lo + hi) >>> 1;
            if (i != lo) {
                if (c < this.list[i]) {
                    hi = i;
                } else {
                    lo = i;
                }
            } else {
                return hi;
            }
        }
    }

    private final boolean containsSlow(int c, int lo, int hi) {
        return (findCodePoint(c, lo, hi) & 1) != 0;
    }
}
