package android.icu.impl;

import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.OutputInt;
import java.util.ArrayList;

public class UnicodeSetStringSpan {
    public static final int ALL = 127;
    static final short ALL_CP_CONTAINED = 255;
    public static final int BACK = 16;
    public static final int BACK_UTF16_CONTAINED = 18;
    public static final int BACK_UTF16_NOT_CONTAINED = 17;
    public static final int CONTAINED = 2;
    public static final int FWD = 32;
    public static final int FWD_UTF16_CONTAINED = 34;
    public static final int FWD_UTF16_NOT_CONTAINED = 33;
    static final short LONG_SPAN = 254;
    public static final int NOT_CONTAINED = 1;
    public static final int WITH_COUNT = 64;
    private boolean all;
    private int maxLength16;
    private OffsetList offsets;
    private boolean someRelevant;
    private short[] spanLengths;
    private UnicodeSet spanNotSet;
    private UnicodeSet spanSet;
    private ArrayList<String> strings;

    public UnicodeSetStringSpan(UnicodeSet set, ArrayList<String> setStrings, int which) {
        int allocSize;
        int spanBackLengthsOffset;
        this.spanSet = new UnicodeSet(0, 1114111);
        this.strings = setStrings;
        this.all = which == 127;
        this.spanSet.retainAll(set);
        if ((which & 1) != 0) {
            this.spanNotSet = this.spanSet;
        }
        this.offsets = new OffsetList();
        int stringsLength = this.strings.size();
        this.someRelevant = false;
        for (int i = 0; i < stringsLength; i++) {
            String string = this.strings.get(i);
            int length16 = string.length();
            if (this.spanSet.span(string, UnicodeSet.SpanCondition.CONTAINED) < length16) {
                this.someRelevant = true;
            }
            if (length16 > this.maxLength16) {
                this.maxLength16 = length16;
            }
        }
        if (!this.someRelevant && (which & 64) == 0) {
            return;
        }
        if (this.all) {
            this.spanSet.freeze();
        }
        if (this.all) {
            allocSize = stringsLength * 2;
        } else {
            allocSize = stringsLength;
        }
        this.spanLengths = new short[allocSize];
        if (this.all) {
            spanBackLengthsOffset = stringsLength;
        } else {
            spanBackLengthsOffset = 0;
        }
        for (int i2 = 0; i2 < stringsLength; i2++) {
            String string2 = this.strings.get(i2);
            int length162 = string2.length();
            int spanLength = this.spanSet.span(string2, UnicodeSet.SpanCondition.CONTAINED);
            if (spanLength < length162) {
                if ((which & 2) != 0) {
                    if ((which & 32) != 0) {
                        this.spanLengths[i2] = makeSpanLengthByte(spanLength);
                    }
                    if ((which & 16) != 0) {
                        this.spanLengths[spanBackLengthsOffset + i2] = makeSpanLengthByte(length162 - this.spanSet.spanBack(string2, length162, UnicodeSet.SpanCondition.CONTAINED));
                    }
                } else {
                    short[] sArr = this.spanLengths;
                    this.spanLengths[spanBackLengthsOffset + i2] = 0;
                    sArr[i2] = 0;
                }
                if ((which & 1) != 0) {
                    if ((which & 32) != 0) {
                        int c = string2.codePointAt(0);
                        addToSpanNotSet(c);
                    }
                    if ((which & 16) != 0) {
                        int c2 = string2.codePointBefore(length162);
                        addToSpanNotSet(c2);
                    }
                }
            } else if (this.all) {
                short[] sArr2 = this.spanLengths;
                this.spanLengths[spanBackLengthsOffset + i2] = ALL_CP_CONTAINED;
                sArr2[i2] = ALL_CP_CONTAINED;
            } else {
                this.spanLengths[i2] = ALL_CP_CONTAINED;
            }
        }
        if (!this.all) {
            return;
        }
        this.spanNotSet.freeze();
    }

    public UnicodeSetStringSpan(UnicodeSetStringSpan otherStringSpan, ArrayList<String> newParentSetStrings) {
        this.spanSet = otherStringSpan.spanSet;
        this.strings = newParentSetStrings;
        this.maxLength16 = otherStringSpan.maxLength16;
        this.someRelevant = otherStringSpan.someRelevant;
        this.all = true;
        if (otherStringSpan.spanNotSet == otherStringSpan.spanSet) {
            this.spanNotSet = this.spanSet;
        } else {
            this.spanNotSet = (UnicodeSet) otherStringSpan.spanNotSet.clone();
        }
        this.offsets = new OffsetList();
        this.spanLengths = (short[]) otherStringSpan.spanLengths.clone();
    }

    public boolean needsStringSpanUTF16() {
        return this.someRelevant;
    }

    public boolean contains(int c) {
        return this.spanSet.contains(c);
    }

    private void addToSpanNotSet(int c) {
        if (this.spanNotSet == null || this.spanNotSet == this.spanSet) {
            if (this.spanSet.contains(c)) {
                return;
            } else {
                this.spanNotSet = this.spanSet.cloneAsThawed();
            }
        }
        this.spanNotSet.add(c);
    }

    public int span(CharSequence s, int start, UnicodeSet.SpanCondition spanCondition) {
        if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
            return spanNot(s, start, null);
        }
        int spanLimit = this.spanSet.span(s, start, UnicodeSet.SpanCondition.CONTAINED);
        if (spanLimit == s.length()) {
            return spanLimit;
        }
        return spanWithStrings(s, start, spanLimit, spanCondition);
    }

    private synchronized int spanWithStrings(CharSequence charSequence, int i, int i2, UnicodeSet.SpanCondition spanCondition) {
        int i3 = 0;
        if (spanCondition == UnicodeSet.SpanCondition.CONTAINED) {
            i3 = this.maxLength16;
        }
        this.offsets.setMaxLength(i3);
        int length = charSequence.length();
        int i4 = i2;
        int i5 = length - i2;
        int i6 = i2 - i;
        int size = this.strings.size();
        while (true) {
            if (spanCondition == UnicodeSet.SpanCondition.CONTAINED) {
                for (int i7 = 0; i7 < size; i7++) {
                    short s = this.spanLengths[i7];
                    if (s != 255) {
                        String str = this.strings.get(i7);
                        int length2 = str.length();
                        int iOffsetByCodePoints = s;
                        if (s >= 254) {
                            iOffsetByCodePoints = str.offsetByCodePoints(length2, -1);
                        }
                        if (iOffsetByCodePoints > i6) {
                            iOffsetByCodePoints = i6;
                        }
                        int i8 = length2 - iOffsetByCodePoints;
                        int i9 = iOffsetByCodePoints;
                        while (i8 <= i5) {
                            if (!this.offsets.containsOffset(i8) && matches16CPB(charSequence, i4 - i9, length, str, length2)) {
                                if (i8 == i5) {
                                    return length;
                                }
                                this.offsets.addOffset(i8);
                            }
                            if (i9 != 0) {
                                i8++;
                                i9--;
                            }
                        }
                    }
                }
            } else {
                int i10 = 0;
                int i11 = 0;
                int i12 = 0;
                while (i12 < size) {
                    short s2 = this.spanLengths[i12];
                    String str2 = this.strings.get(i12);
                    int length3 = str2.length();
                    int i13 = s2;
                    if (s2 >= 254) {
                        i13 = length3;
                    }
                    if (i13 > i6) {
                        i13 = i6;
                    }
                    int i14 = length3 - i13;
                    int i15 = i13;
                    while (true) {
                        if (i14 > i5 || i15 < i11) {
                            break;
                        }
                        if ((i15 > i11 || i14 > i10) && matches16CPB(charSequence, i4 - i15, length, str2, length3)) {
                            i10 = i14;
                            i11 = i15;
                            break;
                        }
                        i14++;
                        i15--;
                    }
                    i12++;
                    i11 = i11;
                }
                if (i10 != 0 || i11 != 0) {
                    i4 += i10;
                    i5 -= i10;
                    if (i5 == 0) {
                        return length;
                    }
                    i6 = 0;
                }
            }
            if (i6 != 0 || i4 == 0) {
                if (this.offsets.isEmpty()) {
                    return i4;
                }
                int iPopMinimum = this.offsets.popMinimum(null);
                i4 += iPopMinimum;
                i5 -= iPopMinimum;
                i6 = 0;
            } else if (this.offsets.isEmpty()) {
                int iSpan = this.spanSet.span(charSequence, i4, UnicodeSet.SpanCondition.CONTAINED);
                i6 = iSpan - i4;
                if (i6 == i5 || i6 == 0) {
                    break;
                }
                i4 += i6;
                i5 -= i6;
            } else {
                int iSpanOne = spanOne(this.spanSet, charSequence, i4, i5);
                if (iSpanOne > 0) {
                    if (iSpanOne == i5) {
                        return length;
                    }
                    i4 += iSpanOne;
                    i5 -= iSpanOne;
                    this.offsets.shift(iSpanOne);
                    i6 = 0;
                } else {
                    int iPopMinimum2 = this.offsets.popMinimum(null);
                    i4 += iPopMinimum2;
                    i5 -= iPopMinimum2;
                    i6 = 0;
                }
            }
        }
    }

    public int spanAndCount(java.lang.CharSequence r12, int r13, android.icu.text.UnicodeSet.SpanCondition r14, android.icu.util.OutputInt r15) {
        if (r14 == android.icu.text.UnicodeSet.SpanCondition.NOT_CONTAINED) {
            return spanNot(r12, r13, r15);
        }
        if (r14 == android.icu.text.UnicodeSet.SpanCondition.CONTAINED) {
            return spanContainedAndCount(r12, r13, r15);
        }
        r9 = r11.strings.size();
        r3 = r12.length();
        r6 = r13;
        r7 = r3 - r13;
        r0 = 0;
        while (r7 != 0) {
            r1 = spanOne(r11.spanSet, r12, r6, r7);
            r5 = r1 > 0 ? r1 : 0;
            r2 = 0;
            while (r2 < r9) {
                r8 = r11.strings.get(r2);
                r4 = r8.length();
                if (r5 < r4 && r4 <= r7 && matches16CPB(r12, r6, r3, r8, r4)) {
                    r5 = r4;
                }
                r2 = r2 + 1;
            }
            if (r5 == 0) {
                r15.value = r0;
                return r6;
            }
            r0 = r0 + 1;
            r6 = r6 + r5;
            r7 = r7 - r5;
        }
        r15.value = r0;
        return r6;
    }

    private synchronized int spanContainedAndCount(CharSequence s, int start, OutputInt outCount) {
        this.offsets.setMaxLength(this.maxLength16);
        int stringsLength = this.strings.size();
        int length = s.length();
        int pos = start;
        int rest = length - start;
        int count = 0;
        while (rest != 0) {
            int cpLength = spanOne(this.spanSet, s, pos, rest);
            if (cpLength > 0) {
                this.offsets.addOffsetAndCount(cpLength, count + 1);
            }
            for (int i = 0; i < stringsLength; i++) {
                String string = this.strings.get(i);
                int length16 = string.length();
                if (length16 <= rest && !this.offsets.hasCountAtOffset(length16, count + 1) && matches16CPB(s, pos, length, string, length16)) {
                    this.offsets.addOffsetAndCount(length16, count + 1);
                }
            }
            if (this.offsets.isEmpty()) {
                outCount.value = count;
                return pos;
            }
            int minOffset = this.offsets.popMinimum(outCount);
            count = outCount.value;
            pos += minOffset;
            rest -= minOffset;
        }
        outCount.value = count;
        return pos;
    }

    public synchronized int spanBack(CharSequence charSequence, int i, UnicodeSet.SpanCondition spanCondition) {
        if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
            return spanNotBack(charSequence, i);
        }
        int iSpanBack = this.spanSet.spanBack(charSequence, i, UnicodeSet.SpanCondition.CONTAINED);
        if (iSpanBack == 0) {
            return 0;
        }
        int i2 = i - iSpanBack;
        int i3 = 0;
        if (spanCondition == UnicodeSet.SpanCondition.CONTAINED) {
            i3 = this.maxLength16;
        }
        this.offsets.setMaxLength(i3);
        int size = this.strings.size();
        int i4 = 0;
        if (this.all) {
            i4 = size;
        }
        while (true) {
            if (spanCondition == UnicodeSet.SpanCondition.CONTAINED) {
                for (int i5 = 0; i5 < size; i5++) {
                    short s = this.spanLengths[i4 + i5];
                    if (s != 255) {
                        String str = this.strings.get(i5);
                        int length = str.length();
                        int iOffsetByCodePoints = s;
                        if (s >= 254) {
                            iOffsetByCodePoints = length - str.offsetByCodePoints(0, 1);
                        }
                        if (iOffsetByCodePoints > i2) {
                            iOffsetByCodePoints = i2;
                        }
                        int i6 = length - iOffsetByCodePoints;
                        int i7 = iOffsetByCodePoints;
                        while (i6 <= iSpanBack) {
                            if (!this.offsets.containsOffset(i6) && matches16CPB(charSequence, iSpanBack - i6, i, str, length)) {
                                if (i6 == iSpanBack) {
                                    return 0;
                                }
                                this.offsets.addOffset(i6);
                            }
                            if (i7 != 0) {
                                i6++;
                                i7--;
                            }
                        }
                    }
                }
            } else {
                int i8 = 0;
                int i9 = 0;
                int i10 = 0;
                while (i10 < size) {
                    short s2 = this.spanLengths[i4 + i10];
                    String str2 = this.strings.get(i10);
                    int length2 = str2.length();
                    int i11 = s2;
                    if (s2 >= 254) {
                        i11 = length2;
                    }
                    if (i11 > i2) {
                        i11 = i2;
                    }
                    int i12 = length2 - i11;
                    int i13 = i11;
                    while (true) {
                        if (i12 > iSpanBack || i13 < i9) {
                            break;
                        }
                        if ((i13 > i9 || i12 > i8) && matches16CPB(charSequence, iSpanBack - i12, i, str2, length2)) {
                            i8 = i12;
                            i9 = i13;
                            break;
                        }
                        i12++;
                        i13--;
                    }
                    i10++;
                    i9 = i9;
                }
                if (i8 != 0 || i9 != 0) {
                    iSpanBack -= i8;
                    if (iSpanBack == 0) {
                        return 0;
                    }
                    i2 = 0;
                }
            }
            if (i2 != 0 || iSpanBack == i) {
                if (this.offsets.isEmpty()) {
                    return iSpanBack;
                }
                iSpanBack -= this.offsets.popMinimum(null);
                i2 = 0;
            } else if (this.offsets.isEmpty()) {
                int i14 = iSpanBack;
                iSpanBack = this.spanSet.spanBack(charSequence, i14, UnicodeSet.SpanCondition.CONTAINED);
                i2 = i14 - iSpanBack;
                if (iSpanBack == 0 || i2 == 0) {
                    break;
                }
            } else {
                int iSpanOneBack = spanOneBack(this.spanSet, charSequence, iSpanBack);
                if (iSpanOneBack > 0) {
                    if (iSpanOneBack == iSpanBack) {
                        return 0;
                    }
                    iSpanBack -= iSpanOneBack;
                    this.offsets.shift(iSpanOneBack);
                    i2 = 0;
                } else {
                    iSpanBack -= this.offsets.popMinimum(null);
                    i2 = 0;
                }
            }
        }
    }

    private int spanNot(CharSequence s, int start, OutputInt outCount) {
        int spanLimit;
        int rest;
        int cpLength;
        String string;
        int length16;
        int length = s.length();
        int pos = start;
        int i = length - start;
        int stringsLength = this.strings.size();
        int count = 0;
        do {
            if (outCount == null) {
                spanLimit = this.spanNotSet.span(s, pos, UnicodeSet.SpanCondition.NOT_CONTAINED);
            } else {
                spanLimit = this.spanNotSet.spanAndCount(s, pos, UnicodeSet.SpanCondition.NOT_CONTAINED, outCount);
                count += outCount.value;
                outCount.value = count;
            }
            if (spanLimit == length) {
                return length;
            }
            int pos2 = spanLimit;
            rest = length - spanLimit;
            cpLength = spanOne(this.spanSet, s, pos2, rest);
            if (cpLength > 0) {
                return pos2;
            }
            for (int i2 = 0; i2 < stringsLength; i2++) {
                if (this.spanLengths[i2] != 255 && (length16 = (string = this.strings.get(i2)).length()) <= rest && matches16CPB(s, pos2, length, string, length16)) {
                    return pos2;
                }
            }
            pos = pos2 - cpLength;
            count++;
        } while (rest + cpLength != 0);
        if (outCount != null) {
            outCount.value = count;
        }
        return length;
    }

    private int spanNotBack(CharSequence s, int length) {
        String string;
        int length16;
        int pos = length;
        int stringsLength = this.strings.size();
        do {
            int pos2 = this.spanNotSet.spanBack(s, pos, UnicodeSet.SpanCondition.NOT_CONTAINED);
            if (pos2 == 0) {
                return 0;
            }
            int cpLength = spanOneBack(this.spanSet, s, pos2);
            if (cpLength > 0) {
                return pos2;
            }
            for (int i = 0; i < stringsLength; i++) {
                if (this.spanLengths[i] != 255 && (length16 = (string = this.strings.get(i)).length()) <= pos2 && matches16CPB(s, pos2 - length16, length, string, length16)) {
                    return pos2;
                }
            }
            pos = pos2 + cpLength;
        } while (pos != 0);
        return 0;
    }

    static short makeSpanLengthByte(int spanLength) {
        return spanLength < 254 ? (short) spanLength : LONG_SPAN;
    }

    private static boolean matches16(CharSequence s, int start, String t, int length) {
        int end = start + length;
        do {
            int length2 = length;
            length = length2 - 1;
            if (length2 > 0) {
                end--;
            } else {
                return true;
            }
        } while (s.charAt(end) == t.charAt(length));
        return false;
    }

    static boolean matches16CPB(CharSequence s, int start, int limit, String t, int tlength) {
        if (!matches16(s, start, t, tlength)) {
            return false;
        }
        if (start > 0 && Character.isHighSurrogate(s.charAt(start - 1)) && Character.isLowSurrogate(s.charAt(start))) {
            return false;
        }
        return (start + tlength < limit && Character.isHighSurrogate(s.charAt((start + tlength) + (-1))) && Character.isLowSurrogate(s.charAt(start + tlength))) ? false : true;
    }

    static int spanOne(UnicodeSet set, CharSequence s, int start, int length) {
        char c = s.charAt(start);
        if (c >= 55296 && c <= 56319 && length >= 2) {
            char c2 = s.charAt(start + 1);
            if (UTF16.isTrailSurrogate(c2)) {
                int supplementary = Character.toCodePoint(c, c2);
                return set.contains(supplementary) ? 2 : -2;
            }
        }
        return set.contains(c) ? 1 : -1;
    }

    static int spanOneBack(UnicodeSet set, CharSequence s, int length) {
        char c = s.charAt(length - 1);
        if (c >= 56320 && c <= 57343 && length >= 2) {
            char c2 = s.charAt(length - 2);
            if (UTF16.isLeadSurrogate(c2)) {
                int supplementary = Character.toCodePoint(c2, c);
                return set.contains(supplementary) ? 2 : -2;
            }
        }
        return set.contains(c) ? 1 : -1;
    }

    private static final class OffsetList {

        static final boolean f27assertionsDisabled;
        private int length;
        private int[] list = new int[16];
        private int start;

        static {
            f27assertionsDisabled = !OffsetList.class.desiredAssertionStatus();
        }

        public void setMaxLength(int maxLength) {
            if (maxLength > this.list.length) {
                this.list = new int[maxLength];
            }
            clear();
        }

        public void clear() {
            int i = this.list.length;
            while (true) {
                int i2 = i;
                i = i2 - 1;
                if (i2 > 0) {
                    this.list[i] = 0;
                } else {
                    this.length = 0;
                    this.start = 0;
                    return;
                }
            }
        }

        public boolean isEmpty() {
            return this.length == 0;
        }

        public void shift(int delta) {
            int i = this.start + delta;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            if (this.list[i] != 0) {
                this.list[i] = 0;
                this.length--;
            }
            this.start = i;
        }

        public void addOffset(int offset) {
            int i = this.start + offset;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            if (!f27assertionsDisabled) {
                if (!(this.list[i] == 0)) {
                    throw new AssertionError();
                }
            }
            this.list[i] = 1;
            this.length++;
        }

        public void addOffsetAndCount(int offset, int count) {
            if (!f27assertionsDisabled) {
                if (!(count > 0)) {
                    throw new AssertionError();
                }
            }
            int i = this.start + offset;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            if (this.list[i] == 0) {
                this.list[i] = count;
                this.length++;
            } else {
                if (count >= this.list[i]) {
                    return;
                }
                this.list[i] = count;
            }
        }

        public boolean containsOffset(int offset) {
            int i = this.start + offset;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            return this.list[i] != 0;
        }

        public boolean hasCountAtOffset(int offset, int count) {
            int i = this.start + offset;
            if (i >= this.list.length) {
                i -= this.list.length;
            }
            int oldCount = this.list[i];
            return oldCount != 0 && oldCount <= count;
        }

        public int popMinimum(OutputInt outCount) {
            int count;
            int count2;
            int i = this.start;
            do {
                i++;
                if (i < this.list.length) {
                    count2 = this.list[i];
                } else {
                    int result = this.list.length - this.start;
                    int i2 = 0;
                    while (true) {
                        count = this.list[i2];
                        if (count != 0) {
                            break;
                        }
                        i2++;
                    }
                    this.list[i2] = 0;
                    this.length--;
                    this.start = i2;
                    if (outCount != null) {
                        outCount.value = count;
                    }
                    return result + i2;
                }
            } while (count2 == 0);
            this.list[i] = 0;
            this.length--;
            int result2 = i - this.start;
            this.start = i;
            if (outCount != null) {
                outCount.value = count2;
            }
            return result2;
        }
    }
}
