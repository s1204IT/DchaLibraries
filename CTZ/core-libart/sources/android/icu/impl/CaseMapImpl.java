package android.icu.impl;

import android.icu.impl.UCaseProps;
import android.icu.impl.coll.CollationSettings;
import android.icu.text.BreakIterator;
import android.icu.text.Edits;
import android.icu.text.UTF16;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.ULocale;
import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.text.CharacterIterator;
import java.util.Locale;

public final class CaseMapImpl {
    static final boolean $assertionsDisabled = false;
    private static final int LNS = 251792942;
    public static final int OMIT_UNCHANGED_TEXT = 16384;
    private static final int TITLECASE_ADJUSTMENT_MASK = 1536;
    public static final int TITLECASE_ADJUST_TO_CASED = 1024;
    private static final int TITLECASE_ITERATOR_MASK = 224;
    public static final int TITLECASE_SENTENCES = 64;
    public static final int TITLECASE_WHOLE_STRING = 32;

    public static final class StringContextIterator implements UCaseProps.ContextIterator {
        protected int limit;
        protected CharSequence s;
        protected int index = 0;
        protected int cpLimit = 0;
        protected int cpStart = 0;
        protected int dir = 0;

        public StringContextIterator(CharSequence charSequence) {
            this.s = charSequence;
            this.limit = charSequence.length();
        }

        public void setLimit(int i) {
            if (i >= 0 && i <= this.s.length()) {
                this.limit = i;
            } else {
                this.limit = this.s.length();
            }
        }

        public void moveToLimit() {
            int i = this.limit;
            this.cpLimit = i;
            this.cpStart = i;
        }

        public int nextCaseMapCP() {
            this.cpStart = this.cpLimit;
            if (this.cpLimit < this.limit) {
                int iCodePointAt = Character.codePointAt(this.s, this.cpLimit);
                this.cpLimit += Character.charCount(iCodePointAt);
                return iCodePointAt;
            }
            return -1;
        }

        public int getCPStart() {
            return this.cpStart;
        }

        public int getCPLimit() {
            return this.cpLimit;
        }

        public int getCPLength() {
            return this.cpLimit - this.cpStart;
        }

        @Override
        public void reset(int i) {
            if (i > 0) {
                this.dir = 1;
                this.index = this.cpLimit;
            } else if (i < 0) {
                this.dir = -1;
                this.index = this.cpStart;
            } else {
                this.dir = 0;
                this.index = 0;
            }
        }

        @Override
        public int next() {
            if (this.dir > 0 && this.index < this.s.length()) {
                int iCodePointAt = Character.codePointAt(this.s, this.index);
                this.index += Character.charCount(iCodePointAt);
                return iCodePointAt;
            }
            if (this.dir < 0 && this.index > 0) {
                int iCodePointBefore = Character.codePointBefore(this.s, this.index);
                this.index -= Character.charCount(iCodePointBefore);
                return iCodePointBefore;
            }
            return -1;
        }
    }

    public static int addTitleAdjustmentOption(int i, int i2) {
        int i3 = i & TITLECASE_ADJUSTMENT_MASK;
        if (i3 != 0 && i3 != i2) {
            throw new IllegalArgumentException("multiple titlecasing index adjustment options");
        }
        return i | i2;
    }

    private static boolean isLNS(int i) {
        int type = UCharacterProperty.INSTANCE.getType(i);
        if (((1 << type) & LNS) == 0) {
            return type == 4 && UCaseProps.INSTANCE.getType(i) != 0;
        }
        return true;
    }

    public static int addTitleIteratorOption(int i, int i2) {
        int i3 = i & 224;
        if (i3 != 0 && i3 != i2) {
            throw new IllegalArgumentException("multiple titlecasing iterator options");
        }
        return i | i2;
    }

    public static BreakIterator getTitleBreakIterator(Locale locale, int i, BreakIterator breakIterator) {
        int i2 = i & 224;
        if (i2 != 0 && breakIterator != null) {
            throw new IllegalArgumentException("titlecasing iterator option together with an explicit iterator");
        }
        if (breakIterator == null) {
            if (i2 == 0) {
                return BreakIterator.getWordInstance(locale);
            }
            if (i2 == 32) {
                return new WholeStringBreakIterator();
            }
            if (i2 == 64) {
                return BreakIterator.getSentenceInstance(locale);
            }
            throw new IllegalArgumentException("unknown titlecasing iterator option");
        }
        return breakIterator;
    }

    public static BreakIterator getTitleBreakIterator(ULocale uLocale, int i, BreakIterator breakIterator) {
        int i2 = i & 224;
        if (i2 != 0 && breakIterator != null) {
            throw new IllegalArgumentException("titlecasing iterator option together with an explicit iterator");
        }
        if (breakIterator == null) {
            if (i2 == 0) {
                return BreakIterator.getWordInstance(uLocale);
            }
            if (i2 == 32) {
                return new WholeStringBreakIterator();
            }
            if (i2 == 64) {
                return BreakIterator.getSentenceInstance(uLocale);
            }
            throw new IllegalArgumentException("unknown titlecasing iterator option");
        }
        return breakIterator;
    }

    private static final class WholeStringBreakIterator extends BreakIterator {
        private int length;

        private WholeStringBreakIterator() {
        }

        private static void notImplemented() {
            throw new UnsupportedOperationException("should not occur");
        }

        @Override
        public int first() {
            return 0;
        }

        @Override
        public int last() {
            notImplemented();
            return 0;
        }

        @Override
        public int next(int i) {
            notImplemented();
            return 0;
        }

        @Override
        public int next() {
            return this.length;
        }

        @Override
        public int previous() {
            notImplemented();
            return 0;
        }

        @Override
        public int following(int i) {
            notImplemented();
            return 0;
        }

        @Override
        public int current() {
            notImplemented();
            return 0;
        }

        @Override
        public CharacterIterator getText() {
            notImplemented();
            return null;
        }

        @Override
        public void setText(CharacterIterator characterIterator) {
            this.length = characterIterator.getEndIndex();
        }

        @Override
        public void setText(CharSequence charSequence) {
            this.length = charSequence.length();
        }

        @Override
        public void setText(String str) {
            this.length = str.length();
        }
    }

    private static int appendCodePoint(Appendable appendable, int i) throws IOException {
        if (i <= 65535) {
            appendable.append((char) i);
            return 1;
        }
        appendable.append((char) (55232 + (i >> 10)));
        appendable.append((char) (UTF16.TRAIL_SURROGATE_MIN_VALUE + (i & Opcodes.OP_NEW_INSTANCE_JUMBO)));
        return 2;
    }

    private static void appendResult(int i, Appendable appendable, int i2, int i3, Edits edits) throws IOException {
        if (i < 0) {
            if (edits != null) {
                edits.addUnchanged(i2);
            }
            if ((i3 & 16384) != 0) {
                return;
            }
            appendCodePoint(appendable, ~i);
            return;
        }
        if (i <= 31) {
            if (edits != null) {
                edits.addReplace(i2, i);
            }
        } else {
            int iAppendCodePoint = appendCodePoint(appendable, i);
            if (edits != null) {
                edits.addReplace(i2, iAppendCodePoint);
            }
        }
    }

    private static final void appendUnchanged(CharSequence charSequence, int i, int i2, Appendable appendable, int i3, Edits edits) throws IOException {
        if (i2 > 0) {
            if (edits != null) {
                edits.addUnchanged(i2);
            }
            if ((i3 & 16384) != 0) {
                return;
            }
            appendable.append(charSequence, i, i2 + i);
        }
    }

    private static String applyEdits(CharSequence charSequence, StringBuilder sb, Edits edits) {
        if (!edits.hasChanges()) {
            return charSequence.toString();
        }
        StringBuilder sb2 = new StringBuilder(charSequence.length() + edits.lengthDelta());
        Edits.Iterator coarseIterator = edits.getCoarseIterator();
        while (coarseIterator.next()) {
            if (coarseIterator.hasChange()) {
                int iReplacementIndex = coarseIterator.replacementIndex();
                sb2.append((CharSequence) sb, iReplacementIndex, coarseIterator.newLength() + iReplacementIndex);
            } else {
                int iSourceIndex = coarseIterator.sourceIndex();
                sb2.append(charSequence, iSourceIndex, coarseIterator.oldLength() + iSourceIndex);
            }
        }
        return sb2.toString();
    }

    private static void internalToLower(int i, int i2, StringContextIterator stringContextIterator, Appendable appendable, Edits edits) throws IOException {
        while (true) {
            int iNextCaseMapCP = stringContextIterator.nextCaseMapCP();
            if (iNextCaseMapCP >= 0) {
                appendResult(UCaseProps.INSTANCE.toFullLower(iNextCaseMapCP, stringContextIterator, appendable, i), appendable, stringContextIterator.getCPLength(), i2, edits);
            } else {
                return;
            }
        }
    }

    public static String toLower(int i, int i2, CharSequence charSequence) {
        if (charSequence.length() <= 100 && (i2 & 16384) == 0) {
            if (charSequence.length() == 0) {
                return charSequence.toString();
            }
            Edits edits = new Edits();
            return applyEdits(charSequence, (StringBuilder) toLower(i, i2 | 16384, charSequence, new StringBuilder(), edits), edits);
        }
        return ((StringBuilder) toLower(i, i2, charSequence, new StringBuilder(charSequence.length()), null)).toString();
    }

    public static <A extends Appendable> A toLower(int i, int i2, CharSequence charSequence, A a, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        internalToLower(i, i2, new StringContextIterator(charSequence), a, edits);
        return a;
    }

    public static String toUpper(int i, int i2, CharSequence charSequence) {
        if (charSequence.length() <= 100 && (i2 & 16384) == 0) {
            if (charSequence.length() == 0) {
                return charSequence.toString();
            }
            Edits edits = new Edits();
            return applyEdits(charSequence, (StringBuilder) toUpper(i, i2 | 16384, charSequence, new StringBuilder(), edits), edits);
        }
        return ((StringBuilder) toUpper(i, i2, charSequence, new StringBuilder(charSequence.length()), null)).toString();
    }

    public static <A extends Appendable> A toUpper(int i, int i2, CharSequence charSequence, A a, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        if (i != 4) {
            StringContextIterator stringContextIterator = new StringContextIterator(charSequence);
            while (true) {
                int iNextCaseMapCP = stringContextIterator.nextCaseMapCP();
                if (iNextCaseMapCP >= 0) {
                    appendResult(UCaseProps.INSTANCE.toFullUpper(iNextCaseMapCP, stringContextIterator, a, i), a, stringContextIterator.getCPLength(), i2, edits);
                } else {
                    return a;
                }
            }
        } else {
            return (A) GreekUpper.toUpper(i2, charSequence, a, edits);
        }
    }

    public static String toTitle(int i, int i2, BreakIterator breakIterator, CharSequence charSequence) {
        if (charSequence.length() <= 100 && (i2 & 16384) == 0) {
            if (charSequence.length() == 0) {
                return charSequence.toString();
            }
            Edits edits = new Edits();
            return applyEdits(charSequence, (StringBuilder) toTitle(i, i2 | 16384, breakIterator, charSequence, new StringBuilder(), edits), edits);
        }
        return ((StringBuilder) toTitle(i, i2, breakIterator, charSequence, new StringBuilder(charSequence.length()), null)).toString();
    }

    public static <A extends Appendable> A toTitle(int i, int i2, BreakIterator breakIterator, CharSequence charSequence, A a, Edits edits) {
        boolean z;
        int next;
        int i3;
        char cCharAt;
        int i4;
        if (edits != null) {
            try {
                edits.reset();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        StringContextIterator stringContextIterator = new StringContextIterator(charSequence);
        int length = charSequence.length();
        boolean z2 = true;
        int i5 = 0;
        while (i5 < length) {
            if (z2) {
                next = breakIterator.first();
                z = false;
            } else {
                z = z2;
                next = breakIterator.next();
            }
            int i6 = (next == -1 || next > length) ? length : next;
            if (i5 < i6) {
                stringContextIterator.setLimit(i6);
                int iNextCaseMapCP = stringContextIterator.nextCaseMapCP();
                if ((i2 & 512) == 0) {
                    boolean z3 = (i2 & 1024) != 0;
                    do {
                        if (z3) {
                            if (UCaseProps.INSTANCE.getType(iNextCaseMapCP) != 0) {
                                break;
                            }
                            iNextCaseMapCP = stringContextIterator.nextCaseMapCP();
                        } else {
                            if (isLNS(iNextCaseMapCP)) {
                                break;
                            }
                            iNextCaseMapCP = stringContextIterator.nextCaseMapCP();
                        }
                    } while (iNextCaseMapCP >= 0);
                    int i7 = iNextCaseMapCP;
                    int cPStart = stringContextIterator.getCPStart();
                    if (i5 < cPStart) {
                        i4 = cPStart;
                        i3 = i6;
                        appendUnchanged(charSequence, i5, cPStart - i5, a, i2, edits);
                    } else {
                        i4 = cPStart;
                        i3 = i6;
                    }
                    iNextCaseMapCP = i7;
                    i5 = i4;
                } else {
                    i3 = i6;
                }
                if (i5 < i3) {
                    int cPLimit = stringContextIterator.getCPLimit();
                    appendResult(UCaseProps.INSTANCE.toFullTitle(iNextCaseMapCP, stringContextIterator, a, i), a, stringContextIterator.getCPLength(), i2, edits);
                    int i8 = i5 + 1;
                    if (i8 < i3 && i == 5 && ((cCharAt = charSequence.charAt(i5)) == 'i' || cCharAt == 'I')) {
                        char cCharAt2 = charSequence.charAt(i8);
                        if (cCharAt2 == 'j') {
                            a.append('J');
                            if (edits != null) {
                                edits.addReplace(1, 1);
                            }
                            stringContextIterator.nextCaseMapCP();
                            cPLimit++;
                        } else if (cCharAt2 == 'J') {
                            appendUnchanged(charSequence, i8, 1, a, i2, edits);
                            stringContextIterator.nextCaseMapCP();
                            cPLimit++;
                        }
                    }
                    int i9 = cPLimit;
                    if (i9 < i3) {
                        if ((i2 & 256) != 0) {
                            appendUnchanged(charSequence, i9, i3 - i9, a, i2, edits);
                            stringContextIterator.moveToLimit();
                        } else {
                            internalToLower(i, i2, stringContextIterator, a, edits);
                        }
                    }
                }
            } else {
                i3 = i6;
            }
            i5 = i3;
            z2 = z;
        }
        return a;
    }

    public static String fold(int i, CharSequence charSequence) {
        if (charSequence.length() <= 100 && (i & 16384) == 0) {
            if (charSequence.length() == 0) {
                return charSequence.toString();
            }
            Edits edits = new Edits();
            return applyEdits(charSequence, (StringBuilder) fold(i | 16384, charSequence, new StringBuilder(), edits), edits);
        }
        return ((StringBuilder) fold(i, charSequence, new StringBuilder(charSequence.length()), null)).toString();
    }

    public static <A extends Appendable> A fold(int i, CharSequence charSequence, A a, Edits edits) {
        if (edits != null) {
            try {
                edits.reset();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        int length = charSequence.length();
        int i2 = 0;
        while (i2 < length) {
            int iCodePointAt = Character.codePointAt(charSequence, i2);
            int iCharCount = Character.charCount(iCodePointAt);
            i2 += iCharCount;
            appendResult(UCaseProps.INSTANCE.toFullFolding(iCodePointAt, a, i), a, iCharCount, i, edits);
        }
        return a;
    }

    private static final class GreekUpper {
        private static final int AFTER_CASED = 1;
        private static final int AFTER_VOWEL_WITH_ACCENT = 2;
        private static final int HAS_ACCENT = 16384;
        private static final int HAS_COMBINING_DIALYTIKA = 65536;
        private static final int HAS_DIALYTIKA = 32768;
        private static final int HAS_EITHER_DIALYTIKA = 98304;
        private static final int HAS_OTHER_GREEK_DIACRITIC = 131072;
        private static final int HAS_VOWEL = 4096;
        private static final int HAS_VOWEL_AND_ACCENT = 20480;
        private static final int HAS_VOWEL_AND_ACCENT_AND_DIALYTIKA = 53248;
        private static final int HAS_YPOGEGRAMMENI = 8192;
        private static final int UPPER_MASK = 1023;
        private static final char data2126 = 5033;
        private static final char[] data0370 = {880, 880, 882, 882, 0, 0, 886, 886, 0, 0, 890, 1021, 1022, 1023, 0, 895, 0, 0, 0, 0, 0, 0, 21393, 0, 21397, 21399, 21401, 0, 21407, 0, 21413, 21417, 54169, 5009, 914, 915, 916, 5013, 918, 5015, 920, 5017, 922, 923, 924, 925, 926, 5023, 928, 929, 0, 931, 932, 5029, 934, 935, 936, data2126, 37785, 37797, 21393, 21397, 21399, 21401, 54181, 5009, 914, 915, 916, 5013, 918, 5015, 920, 5017, 922, 923, 924, 925, 926, 5023, 928, 929, 931, 931, 932, 5029, 934, 935, 936, data2126, 37785, 37797, 21407, 21413, 21417, 975, 914, 920, 978, 17362, 33746, 934, 928, 975, 984, 984, 986, 986, 988, 988, 990, 990, 992, 992, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 922, 929, 1017, 895, 1012, 5013, 0, 1015, 1015, 1017, 1018, 1018, 1020, 1021, 1022, 1023};
        private static final char[] data1F00 = {5009, 5009, 21393, 21393, 21393, 21393, 21393, 21393, 5009, 5009, 21393, 21393, 21393, 21393, 21393, 21393, 5013, 5013, 21397, 21397, 21397, 21397, 0, 0, 5013, 5013, 21397, 21397, 21397, 21397, 0, 0, 5015, 5015, 21399, 21399, 21399, 21399, 21399, 21399, 5015, 5015, 21399, 21399, 21399, 21399, 21399, 21399, 5017, 5017, 21401, 21401, 21401, 21401, 21401, 21401, 5017, 5017, 21401, 21401, 21401, 21401, 21401, 21401, 5023, 5023, 21407, 21407, 21407, 21407, 0, 0, 5023, 5023, 21407, 21407, 21407, 21407, 0, 0, 5029, 5029, 21413, 21413, 21413, 21413, 21413, 21413, 0, 5029, 0, 21413, 0, 21413, 0, 21413, data2126, data2126, 21417, 21417, 21417, 21417, 21417, 21417, data2126, data2126, 21417, 21417, 21417, 21417, 21417, 21417, 21393, 21393, 21397, 21397, 21399, 21399, 21401, 21401, 21407, 21407, 21413, 21413, 21417, 21417, 0, 0, 13201, 13201, 29585, 29585, 29585, 29585, 29585, 29585, 13201, 13201, 29585, 29585, 29585, 29585, 29585, 29585, 13207, 13207, 29591, 29591, 29591, 29591, 29591, 29591, 13207, 13207, 29591, 29591, 29591, 29591, 29591, 29591, 13225, 13225, 29609, 29609, 29609, 29609, 29609, 29609, 13225, 13225, 29609, 29609, 29609, 29609, 29609, 29609, 5009, 5009, 29585, 13201, 29585, 0, 21393, 29585, 5009, 5009, 21393, 21393, 13201, 0, 5017, 0, 0, 0, 29591, 13207, 29591, 0, 21399, 29591, 21397, 21397, 21399, 21399, 13207, 0, 0, 0, 5017, 5017, 54169, 54169, 0, 0, 21401, 54169, 5017, 5017, 21401, 21401, 0, 0, 0, 0, 5029, 5029, 54181, 54181, 929, 929, 21413, 54181, 5029, 5029, 21413, 21413, 929, 0, 0, 0, 0, 0, 29609, 13225, 29609, 0, 21417, 29609, 21407, 21407, 21417, 21417, 13225, 0, 0, 0};

        private GreekUpper() {
        }

        private static final int getLetterData(int i) {
            if (i < 880 || 8486 < i || (1023 < i && i < 7936)) {
                return 0;
            }
            if (i <= 1023) {
                return data0370[i - 880];
            }
            if (i <= 8191) {
                return data1F00[i - 7936];
            }
            if (i != 8486) {
                return 0;
            }
            return 5033;
        }

        private static final int getDiacriticData(int i) {
            if (i == 774) {
                return 131072;
            }
            if (i == 776) {
                return 65536;
            }
            if (i != 785) {
                switch (i) {
                    case CollationSettings.CASE_FIRST_AND_UPPER_MASK:
                    case 769:
                    case 770:
                    case 771:
                        return 16384;
                    case 772:
                        return 131072;
                    default:
                        switch (i) {
                            case 787:
                            case 788:
                                return 131072;
                            default:
                                switch (i) {
                                    case 834:
                                        return 16384;
                                    case 835:
                                        return 131072;
                                    case 836:
                                        return 81920;
                                    case 837:
                                        return 8192;
                                    default:
                                        return 0;
                                }
                        }
                }
            }
            return 16384;
        }

        private static boolean isFollowedByCasedLetter(CharSequence charSequence, int i) {
            while (i < charSequence.length()) {
                int iCodePointAt = Character.codePointAt(charSequence, i);
                int typeOrIgnorable = UCaseProps.INSTANCE.getTypeOrIgnorable(iCodePointAt);
                if ((typeOrIgnorable & 4) == 0) {
                    return typeOrIgnorable != 0;
                }
                i += Character.charCount(iCodePointAt);
            }
            return false;
        }

        private static <A extends Appendable> A toUpper(int i, CharSequence charSequence, A a, Edits edits) throws IOException {
            int i2;
            boolean z;
            boolean z2;
            boolean z3;
            int diacriticData;
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            while (i4 < charSequence.length()) {
                int iCodePointAt = Character.codePointAt(charSequence, i4);
                int iCharCount = Character.charCount(iCodePointAt) + i4;
                int typeOrIgnorable = UCaseProps.INSTANCE.getTypeOrIgnorable(iCodePointAt);
                if ((typeOrIgnorable & 4) != 0) {
                    i2 = (i5 & 1) | i3;
                } else {
                    i2 = typeOrIgnorable != 0 ? 1 : i3;
                }
                int letterData = getLetterData(iCodePointAt);
                if (letterData <= 0) {
                    CaseMapImpl.appendResult(UCaseProps.INSTANCE.toFullUpper(iCodePointAt, null, a, 4), a, iCharCount - i4, i, edits);
                } else {
                    int i6 = letterData & 1023;
                    if ((letterData & 4096) != 0 && (i5 & 2) != 0 && (i6 == 921 || i6 == 933)) {
                        letterData |= 32768;
                    }
                    int i7 = (letterData & 8192) != 0 ? 1 : i3;
                    while (iCharCount < charSequence.length() && (diacriticData = getDiacriticData(charSequence.charAt(iCharCount))) != 0) {
                        letterData |= diacriticData;
                        if ((diacriticData & 8192) != 0) {
                            i7++;
                        }
                        iCharCount++;
                    }
                    if ((HAS_VOWEL_AND_ACCENT_AND_DIALYTIKA & letterData) == HAS_VOWEL_AND_ACCENT) {
                        i2 |= 2;
                    }
                    if (i6 == 919 && (letterData & 16384) != 0 && i7 == 0 && (i5 & 1) == 0 && !isFollowedByCasedLetter(charSequence, iCharCount)) {
                        if (i4 == iCharCount) {
                            i6 = 905;
                        } else {
                            z = true;
                            if (edits == null || (i & 16384) != 0) {
                                boolean z4 = charSequence.charAt(i4) == i6 || i7 > 0;
                                int i8 = i4 + 1;
                                if ((letterData & HAS_EITHER_DIALYTIKA) != 0) {
                                    z4 |= i8 >= iCharCount || charSequence.charAt(i8) != 776;
                                    i8++;
                                }
                                if (z) {
                                    z4 |= i8 >= iCharCount || charSequence.charAt(i8) != 769;
                                    i8++;
                                }
                                int i9 = iCharCount - i4;
                                int i10 = (i8 - i4) + i7;
                                z2 = (i9 == i10) | z4;
                                if (!z2) {
                                    if (edits != null) {
                                        edits.addReplace(i9, i10);
                                    }
                                    z3 = z2;
                                } else {
                                    if (edits != null) {
                                        edits.addUnchanged(i9);
                                    }
                                    z3 = (i & 16384) == 0;
                                }
                                if (!z3) {
                                    a.append((char) i6);
                                    if ((HAS_EITHER_DIALYTIKA & letterData) != 0) {
                                        a.append((char) 776);
                                    }
                                    if (z) {
                                        a.append((char) 769);
                                    }
                                    while (i7 > 0) {
                                        a.append((char) 921);
                                        i7--;
                                    }
                                }
                            }
                        }
                    } else if ((letterData & 32768) != 0) {
                        if (i6 == 921) {
                            i6 = 938;
                            letterData &= -98305;
                        } else if (i6 == 933) {
                            i6 = 939;
                            letterData &= -98305;
                        }
                    }
                    z = false;
                    if (edits == null) {
                        if (charSequence.charAt(i4) == i6) {
                            int i82 = i4 + 1;
                            if ((letterData & HAS_EITHER_DIALYTIKA) != 0) {
                            }
                            if (z) {
                            }
                            int i92 = iCharCount - i4;
                            int i102 = (i82 - i4) + i7;
                            z2 = (i92 == i102) | z4;
                            if (!z2) {
                            }
                            if (!z3) {
                            }
                        }
                    }
                }
                i4 = iCharCount;
                i5 = i2;
                i3 = 0;
            }
            return a;
        }
    }
}
