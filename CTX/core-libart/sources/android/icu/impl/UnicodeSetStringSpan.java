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
    private final int maxLength16;
    private OffsetList offsets;
    private boolean someRelevant;
    private short[] spanLengths;
    private UnicodeSet spanNotSet;
    private UnicodeSet spanSet;
    private ArrayList<String> strings;

    public UnicodeSetStringSpan(UnicodeSet unicodeSet, ArrayList<String> arrayList, int i) {
        int i2;
        this.spanSet = new UnicodeSet(0, 1114111);
        this.strings = arrayList;
        this.all = i == 127;
        this.spanSet.retainAll(unicodeSet);
        int i3 = i & 1;
        if (i3 != 0) {
            this.spanNotSet = this.spanSet;
        }
        this.offsets = new OffsetList();
        int size = this.strings.size();
        this.someRelevant = false;
        int i4 = 0;
        for (int i5 = 0; i5 < size; i5++) {
            String str = this.strings.get(i5);
            int length = str.length();
            if (this.spanSet.span(str, UnicodeSet.SpanCondition.CONTAINED) < length) {
                this.someRelevant = true;
            }
            if (length > i4) {
                i4 = length;
            }
        }
        this.maxLength16 = i4;
        if (!this.someRelevant && (i & 64) == 0) {
            return;
        }
        if (this.all) {
            this.spanSet.freeze();
        }
        if (this.all) {
            i2 = size * 2;
        } else {
            i2 = size;
        }
        this.spanLengths = new short[i2];
        int i6 = this.all ? size : 0;
        for (int i7 = 0; i7 < size; i7++) {
            String str2 = this.strings.get(i7);
            int length2 = str2.length();
            int iSpan = this.spanSet.span(str2, UnicodeSet.SpanCondition.CONTAINED);
            if (iSpan < length2) {
                if ((i & 2) != 0) {
                    if ((i & 32) != 0) {
                        this.spanLengths[i7] = makeSpanLengthByte(iSpan);
                    }
                    if ((i & 16) != 0) {
                        this.spanLengths[i6 + i7] = makeSpanLengthByte(length2 - this.spanSet.spanBack(str2, length2, UnicodeSet.SpanCondition.CONTAINED));
                    }
                } else {
                    short[] sArr = this.spanLengths;
                    this.spanLengths[i6 + i7] = 0;
                    sArr[i7] = 0;
                }
                if (i3 != 0) {
                    if ((i & 32) != 0) {
                        addToSpanNotSet(str2.codePointAt(0));
                    }
                    if ((i & 16) != 0) {
                        addToSpanNotSet(str2.codePointBefore(length2));
                    }
                }
            } else if (this.all) {
                short[] sArr2 = this.spanLengths;
                this.spanLengths[i6 + i7] = ALL_CP_CONTAINED;
                sArr2[i7] = ALL_CP_CONTAINED;
            } else {
                this.spanLengths[i7] = ALL_CP_CONTAINED;
            }
        }
        if (this.all) {
            this.spanNotSet.freeze();
        }
    }

    public UnicodeSetStringSpan(UnicodeSetStringSpan unicodeSetStringSpan, ArrayList<String> arrayList) {
        this.spanSet = unicodeSetStringSpan.spanSet;
        this.strings = arrayList;
        this.maxLength16 = unicodeSetStringSpan.maxLength16;
        this.someRelevant = unicodeSetStringSpan.someRelevant;
        this.all = true;
        if (Utility.sameObjects(unicodeSetStringSpan.spanNotSet, unicodeSetStringSpan.spanSet)) {
            this.spanNotSet = this.spanSet;
        } else {
            this.spanNotSet = (UnicodeSet) unicodeSetStringSpan.spanNotSet.clone();
        }
        this.offsets = new OffsetList();
        this.spanLengths = (short[]) unicodeSetStringSpan.spanLengths.clone();
    }

    public boolean needsStringSpanUTF16() {
        return this.someRelevant;
    }

    public boolean contains(int i) {
        return this.spanSet.contains(i);
    }

    private void addToSpanNotSet(int i) {
        if (Utility.sameObjects(this.spanNotSet, null) || Utility.sameObjects(this.spanNotSet, this.spanSet)) {
            if (this.spanSet.contains(i)) {
                return;
            } else {
                this.spanNotSet = this.spanSet.cloneAsThawed();
            }
        }
        this.spanNotSet.add(i);
    }

    public int span(CharSequence charSequence, int i, UnicodeSet.SpanCondition spanCondition) {
        if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
            return spanNot(charSequence, i, null);
        }
        int iSpan = this.spanSet.span(charSequence, i, UnicodeSet.SpanCondition.CONTAINED);
        if (iSpan == charSequence.length()) {
            return iSpan;
        }
        return spanWithStrings(charSequence, i, iSpan, spanCondition);
    }

    private synchronized int spanWithStrings(CharSequence charSequence, int i, int i2, UnicodeSet.SpanCondition spanCondition) {
        int i3;
        if (spanCondition == UnicodeSet.SpanCondition.CONTAINED) {
            i3 = this.maxLength16;
        } else {
            i3 = 0;
        }
        this.offsets.setMaxLength(i3);
        int length = charSequence.length();
        int i4 = length - i2;
        int i5 = i2 - i;
        int size = this.strings.size();
        int i6 = i2;
        while (true) {
            UnicodeSet.SpanCondition spanCondition2 = UnicodeSet.SpanCondition.CONTAINED;
            short s = LONG_SPAN;
            if (spanCondition == spanCondition2) {
                for (int i7 = 0; i7 < size; i7++) {
                    short s2 = this.spanLengths[i7];
                    if (s2 != 255) {
                        String str = this.strings.get(i7);
                        int length2 = str.length();
                        int iOffsetByCodePoints = s2;
                        if (s2 >= 254) {
                            iOffsetByCodePoints = str.offsetByCodePoints(length2, -1);
                        }
                        if (iOffsetByCodePoints > i5) {
                            iOffsetByCodePoints = i5;
                        }
                        int i8 = length2 - iOffsetByCodePoints;
                        int i9 = iOffsetByCodePoints;
                        while (i8 <= i4) {
                            if (!this.offsets.containsOffset(i8) && matches16CPB(charSequence, i6 - i9, length, str, length2)) {
                                if (i8 == i4) {
                                    return length;
                                }
                                this.offsets.addOffset(i8);
                            }
                            if (i9 == 0) {
                                break;
                            }
                            i8++;
                            i9--;
                        }
                    }
                }
            } else {
                int i10 = 0;
                int i11 = 0;
                int i12 = 0;
                while (i10 < size) {
                    short s3 = this.spanLengths[i10];
                    String str2 = this.strings.get(i10);
                    int length3 = str2.length();
                    int i13 = s3;
                    if (s3 >= s) {
                        i13 = length3;
                    }
                    if (i13 > i5) {
                        i13 = i5;
                    }
                    int i14 = i13;
                    int i15 = length3 - i13;
                    while (true) {
                        if (i15 <= i4 && i14 >= i12) {
                            if ((i14 > i12 || i15 > i11) && matches16CPB(charSequence, i6 - i14, length, str2, length3)) {
                                i12 = i14;
                                i11 = i15;
                                break;
                            }
                            i15++;
                            i14--;
                        }
                    }
                    i10++;
                    s = LONG_SPAN;
                    i12 = i12;
                }
                if (i11 != 0 || i12 != 0) {
                    i6 += i11;
                    i4 -= i11;
                    if (i4 == 0) {
                        return length;
                    }
                }
                i5 = 0;
            }
            if (i5 != 0 || i6 == 0) {
                if (this.offsets.isEmpty()) {
                    return i6;
                }
            } else if (this.offsets.isEmpty()) {
                int iSpan = this.spanSet.span(charSequence, i6, UnicodeSet.SpanCondition.CONTAINED);
                i5 = iSpan - i6;
                if (i5 == i4 || i5 == 0) {
                    break;
                }
                i6 += i5;
                i4 -= i5;
            } else {
                int iSpanOne = spanOne(this.spanSet, charSequence, i6, i4);
                if (iSpanOne > 0) {
                    if (iSpanOne == i4) {
                        return length;
                    }
                    i6 += iSpanOne;
                    i4 -= iSpanOne;
                    this.offsets.shift(iSpanOne);
                }
                i5 = 0;
            }
            int iPopMinimum = this.offsets.popMinimum(null);
            i6 += iPopMinimum;
            i4 -= iPopMinimum;
            i5 = 0;
        }
    }

    public int spanAndCount(java.lang.CharSequence r9, int r10, android.icu.text.UnicodeSet.SpanCondition r11, android.icu.util.OutputInt r12) {
        if (r11 == android.icu.text.UnicodeSet.SpanCondition.NOT_CONTAINED) {
            return spanNot(r9, r10, r12);
        }
        if (r11 == android.icu.text.UnicodeSet.SpanCondition.CONTAINED) {
            return spanContainedAndCount(r9, r10, r12);
        }
        r11 = r8.strings.size();
        r0 = r9.length();
        r1 = r0 - r10;
        r3 = 0;
        while (r1 != 0) {
            r4 = spanOne(r8.spanSet, r9, r10, r1);
            if (r4 <= 0) {
                r4 = 0;
            }
            r5 = r4;
            r4 = 0;
            while (r4 < r11) {
                r6 = r8.strings.get(r4);
                r7 = r6.length();
                if (r5 < r7 && r7 <= r1 && matches16CPB(r9, r10, r0, r6, r7)) {
                    r5 = r7;
                }
                r4 = r4 + 1;
            }
            if (r5 == 0) {
                r12.value = r3;
                return r10;
            }
            r3 = r3 + 1;
            r10 = r10 + r5;
            r1 = r1 - r5;
        }
        r12.value = r3;
        return r10;
    }

    private synchronized int spanContainedAndCount(CharSequence charSequence, int i, OutputInt outputInt) {
        this.offsets.setMaxLength(this.maxLength16);
        int size = this.strings.size();
        int length = charSequence.length();
        int i2 = length - i;
        int i3 = i;
        int i4 = 0;
        while (i2 != 0) {
            int iSpanOne = spanOne(this.spanSet, charSequence, i3, i2);
            if (iSpanOne > 0) {
                this.offsets.addOffsetAndCount(iSpanOne, i4 + 1);
            }
            for (int i5 = 0; i5 < size; i5++) {
                String str = this.strings.get(i5);
                int length2 = str.length();
                if (length2 <= i2) {
                    int i6 = i4 + 1;
                    if (!this.offsets.hasCountAtOffset(length2, i6) && matches16CPB(charSequence, i3, length, str, length2)) {
                        this.offsets.addOffsetAndCount(length2, i6);
                    }
                }
            }
            if (this.offsets.isEmpty()) {
                outputInt.value = i4;
                return i3;
            }
            int iPopMinimum = this.offsets.popMinimum(outputInt);
            i3 += iPopMinimum;
            i2 -= iPopMinimum;
            i4 = outputInt.value;
        }
        outputInt.value = i4;
        return i3;
    }

    public synchronized int spanBack(CharSequence charSequence, int i, UnicodeSet.SpanCondition spanCondition) {
        if (spanCondition == UnicodeSet.SpanCondition.NOT_CONTAINED) {
            return spanNotBack(charSequence, i);
        }
        int iSpanBack = this.spanSet.spanBack(charSequence, i, UnicodeSet.SpanCondition.CONTAINED);
        int i2 = 0;
        if (iSpanBack == 0) {
            return 0;
        }
        int i3 = i - iSpanBack;
        this.offsets.setMaxLength(spanCondition == UnicodeSet.SpanCondition.CONTAINED ? this.maxLength16 : 0);
        int size = this.strings.size();
        int i4 = this.all ? size : 0;
        while (true) {
            UnicodeSet.SpanCondition spanCondition2 = UnicodeSet.SpanCondition.CONTAINED;
            short s = LONG_SPAN;
            if (spanCondition == spanCondition2) {
                for (int i5 = i2; i5 < size; i5++) {
                    short s2 = this.spanLengths[i4 + i5];
                    if (s2 != 255) {
                        String str = this.strings.get(i5);
                        int length = str.length();
                        int iOffsetByCodePoints = s2;
                        if (s2 >= 254) {
                            iOffsetByCodePoints = length - str.offsetByCodePoints(i2, 1);
                        }
                        if (iOffsetByCodePoints > i3) {
                            iOffsetByCodePoints = i3;
                        }
                        int i6 = length - iOffsetByCodePoints;
                        int i7 = iOffsetByCodePoints;
                        while (i6 <= iSpanBack) {
                            if (!this.offsets.containsOffset(i6) && matches16CPB(charSequence, iSpanBack - i6, i, str, length)) {
                                if (i6 == iSpanBack) {
                                    return i2;
                                }
                                this.offsets.addOffset(i6);
                            }
                            if (i7 == 0) {
                                break;
                            }
                            i6++;
                            i7--;
                        }
                    }
                }
            } else {
                int i8 = i2;
                int i9 = i8;
                int i10 = i9;
                while (i8 < size) {
                    short s3 = this.spanLengths[i4 + i8];
                    String str2 = this.strings.get(i8);
                    int length2 = str2.length();
                    int i11 = s3;
                    if (s3 >= s) {
                        i11 = length2;
                    }
                    if (i11 > i3) {
                        i11 = i3;
                    }
                    int i12 = i11;
                    int i13 = length2 - i11;
                    while (true) {
                        if (i13 <= iSpanBack && i12 >= i10) {
                            if ((i12 > i10 || i13 > i9) && matches16CPB(charSequence, iSpanBack - i13, i, str2, length2)) {
                                i10 = i12;
                                i9 = i13;
                                break;
                            }
                            i13++;
                            i12--;
                        }
                    }
                    i8++;
                    s = LONG_SPAN;
                    i10 = i10;
                }
                if (i9 == 0 && i10 == 0) {
                }
                iSpanBack -= i9;
                if (iSpanBack == 0) {
                    return 0;
                }
                i2 = 0;
                i3 = 0;
            }
            if (i3 != 0 || iSpanBack == i) {
                if (this.offsets.isEmpty()) {
                    return iSpanBack;
                }
            } else if (this.offsets.isEmpty()) {
                int iSpanBack2 = this.spanSet.spanBack(charSequence, iSpanBack, UnicodeSet.SpanCondition.CONTAINED);
                i3 = iSpanBack - iSpanBack2;
                if (iSpanBack2 == 0 || i3 == 0) {
                    break;
                }
                iSpanBack = iSpanBack2;
                i2 = 0;
            } else {
                int iSpanOneBack = spanOneBack(this.spanSet, charSequence, iSpanBack);
                if (iSpanOneBack > 0) {
                    if (iSpanOneBack == iSpanBack) {
                        return 0;
                    }
                    iSpanBack -= iSpanOneBack;
                    this.offsets.shift(iSpanOneBack);
                }
                i2 = 0;
                i3 = 0;
            }
            iSpanBack -= this.offsets.popMinimum(null);
            i2 = 0;
            i3 = 0;
        }
    }

    private int spanNot(CharSequence charSequence, int i, OutputInt outputInt) {
        int iSpanAndCount;
        int i2;
        int iSpanOne;
        String str;
        int length;
        int length2 = charSequence.length();
        int size = this.strings.size();
        int i3 = 0;
        do {
            if (outputInt == null) {
                iSpanAndCount = this.spanNotSet.span(charSequence, i, UnicodeSet.SpanCondition.NOT_CONTAINED);
            } else {
                iSpanAndCount = this.spanNotSet.spanAndCount(charSequence, i, UnicodeSet.SpanCondition.NOT_CONTAINED, outputInt);
                i3 += outputInt.value;
                outputInt.value = i3;
            }
            if (iSpanAndCount == length2) {
                return length2;
            }
            i2 = length2 - iSpanAndCount;
            iSpanOne = spanOne(this.spanSet, charSequence, iSpanAndCount, i2);
            if (iSpanOne > 0) {
                return iSpanAndCount;
            }
            for (int i4 = 0; i4 < size; i4++) {
                if (this.spanLengths[i4] != 255 && (length = (str = this.strings.get(i4)).length()) <= i2 && matches16CPB(charSequence, iSpanAndCount, length2, str, length)) {
                    return iSpanAndCount;
                }
            }
            i = iSpanAndCount - iSpanOne;
            i3++;
        } while (i2 + iSpanOne != 0);
        if (outputInt != null) {
            outputInt.value = i3;
        }
        return length2;
    }

    private int spanNotBack(CharSequence charSequence, int i) {
        String str;
        int length;
        int size = this.strings.size();
        int i2 = i;
        do {
            int iSpanBack = this.spanNotSet.spanBack(charSequence, i2, UnicodeSet.SpanCondition.NOT_CONTAINED);
            if (iSpanBack == 0) {
                return 0;
            }
            int iSpanOneBack = spanOneBack(this.spanSet, charSequence, iSpanBack);
            if (iSpanOneBack > 0) {
                return iSpanBack;
            }
            for (int i3 = 0; i3 < size; i3++) {
                if (this.spanLengths[i3] != 255 && (length = (str = this.strings.get(i3)).length()) <= iSpanBack && matches16CPB(charSequence, iSpanBack - length, i, str, length)) {
                    return iSpanBack;
                }
            }
            i2 = iSpanBack + iSpanOneBack;
        } while (i2 != 0);
        return 0;
    }

    static short makeSpanLengthByte(int i) {
        return i < 254 ? (short) i : LONG_SPAN;
    }

    private static boolean matches16(CharSequence charSequence, int i, String str, int i2) {
        int i3 = i + i2;
        while (true) {
            int i4 = i2 - 1;
            if (i2 > 0) {
                i3--;
                if (charSequence.charAt(i3) == str.charAt(i4)) {
                    i2 = i4;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    static boolean matches16CPB(CharSequence charSequence, int i, int i2, String str, int i3) {
        int i4;
        if (matches16(charSequence, i, str, i3) && ((i <= 0 || !Character.isHighSurrogate(charSequence.charAt(i - 1)) || !Character.isLowSurrogate(charSequence.charAt(i))) && ((i4 = i + i3) >= i2 || !Character.isHighSurrogate(charSequence.charAt(i4 - 1)) || !Character.isLowSurrogate(charSequence.charAt(i4))))) {
            return true;
        }
        return false;
    }

    static int spanOne(UnicodeSet unicodeSet, CharSequence charSequence, int i, int i2) {
        char cCharAt = charSequence.charAt(i);
        if (cCharAt >= 55296 && cCharAt <= 56319 && i2 >= 2) {
            char cCharAt2 = charSequence.charAt(i + 1);
            if (UTF16.isTrailSurrogate(cCharAt2)) {
                return unicodeSet.contains(Character.toCodePoint(cCharAt, cCharAt2)) ? 2 : -2;
            }
        }
        return unicodeSet.contains(cCharAt) ? 1 : -1;
    }

    static int spanOneBack(UnicodeSet unicodeSet, CharSequence charSequence, int i) {
        char cCharAt = charSequence.charAt(i - 1);
        if (cCharAt >= 56320 && cCharAt <= 57343 && i >= 2) {
            char cCharAt2 = charSequence.charAt(i - 2);
            if (UTF16.isLeadSurrogate(cCharAt2)) {
                return unicodeSet.contains(Character.toCodePoint(cCharAt2, cCharAt)) ? 2 : -2;
            }
        }
        return unicodeSet.contains(cCharAt) ? 1 : -1;
    }

    private static final class OffsetList {
        static final boolean $assertionsDisabled = false;
        private int length;
        private int[] list = new int[16];
        private int start;

        public void setMaxLength(int i) {
            if (i > this.list.length) {
                this.list = new int[i];
            }
            clear();
        }

        public void clear() {
            int length = this.list.length;
            while (true) {
                int i = length - 1;
                if (length > 0) {
                    this.list[i] = 0;
                    length = i;
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

        public void shift(int i) {
            int length = this.start + i;
            if (length >= this.list.length) {
                length -= this.list.length;
            }
            if (this.list[length] != 0) {
                this.list[length] = 0;
                this.length--;
            }
            this.start = length;
        }

        public void addOffset(int i) {
            int length = this.start + i;
            if (length >= this.list.length) {
                length -= this.list.length;
            }
            this.list[length] = 1;
            this.length++;
        }

        public void addOffsetAndCount(int i, int i2) {
            int length = this.start + i;
            if (length >= this.list.length) {
                length -= this.list.length;
            }
            if (this.list[length] == 0) {
                this.list[length] = i2;
                this.length++;
            } else if (i2 < this.list[length]) {
                this.list[length] = i2;
            }
        }

        public boolean containsOffset(int i) {
            int length = this.start + i;
            if (length >= this.list.length) {
                length -= this.list.length;
            }
            return this.list[length] != 0;
        }

        public boolean hasCountAtOffset(int i, int i2) {
            int length = this.start + i;
            if (length >= this.list.length) {
                length -= this.list.length;
            }
            int i3 = this.list[length];
            return i3 != 0 && i3 <= i2;
        }

        public int popMinimum(OutputInt outputInt) {
            int i;
            int i2;
            int i3 = this.start;
            do {
                i3++;
                if (i3 < this.list.length) {
                    i2 = this.list[i3];
                } else {
                    int length = this.list.length - this.start;
                    int i4 = 0;
                    while (true) {
                        i = this.list[i4];
                        if (i != 0) {
                            break;
                        }
                        i4++;
                    }
                    this.list[i4] = 0;
                    this.length--;
                    this.start = i4;
                    if (outputInt != null) {
                        outputInt.value = i;
                    }
                    return length + i4;
                }
            } while (i2 == 0);
            this.list[i3] = 0;
            this.length--;
            int i5 = i3 - this.start;
            this.start = i3;
            if (outputInt != null) {
                outputInt.value = i2;
            }
            return i5;
        }
    }
}
