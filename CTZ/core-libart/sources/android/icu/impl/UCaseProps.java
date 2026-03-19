package android.icu.impl;

import android.icu.impl.ICUBinary;
import android.icu.impl.Trie2;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.ULocale;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

public final class UCaseProps {
    private static final int ABOVE = 64;
    private static final int CLOSURE_MAX_LENGTH = 15;
    private static final String DATA_FILE_NAME = "ucase.icu";
    private static final String DATA_NAME = "ucase";
    private static final String DATA_TYPE = "icu";
    private static final int DELTA_SHIFT = 7;
    private static final int DOT_MASK = 96;
    private static final int EXCEPTION = 16;
    private static final int EXC_CLOSURE = 6;
    private static final int EXC_CONDITIONAL_FOLD = 32768;
    private static final int EXC_CONDITIONAL_SPECIAL = 16384;
    private static final int EXC_DOT_SHIFT = 7;
    private static final int EXC_DOUBLE_SLOTS = 256;
    private static final int EXC_FOLD = 1;
    private static final int EXC_FULL_MAPPINGS = 7;
    private static final int EXC_LOWER = 0;
    private static final int EXC_SHIFT = 5;
    private static final int EXC_TITLE = 3;
    private static final int EXC_UPPER = 2;
    private static final int FMT = 1665225541;
    private static final int FOLD_CASE_OPTIONS_MASK = 7;
    private static final int FULL_LOWER = 15;
    static final int IGNORABLE = 4;
    public static final UCaseProps INSTANCE;
    private static final int IX_EXC_LENGTH = 3;
    private static final int IX_TOP = 16;
    private static final int IX_TRIE_SIZE = 2;
    private static final int IX_UNFOLD_LENGTH = 4;
    public static final int LOC_DUTCH = 5;
    static final int LOC_GREEK = 4;
    private static final int LOC_LITHUANIAN = 3;
    public static final int LOC_ROOT = 1;
    private static final int LOC_TURKISH = 2;
    public static final int LOWER = 1;
    public static final int MAX_STRING_LENGTH = 31;
    public static final int NONE = 0;
    private static final int OTHER_ACCENT = 96;
    private static final int SENSITIVE = 8;
    private static final int SOFT_DOTTED = 32;
    public static final int TITLE = 3;
    public static final int TYPE_MASK = 3;
    private static final int UNFOLD_ROWS = 0;
    private static final int UNFOLD_ROW_WIDTH = 1;
    private static final int UNFOLD_STRING_WIDTH = 2;
    public static final int UPPER = 2;
    private static final String iDot = "i̇";
    private static final String iDotAcute = "i̇́";
    private static final String iDotGrave = "i̇̀";
    private static final String iDotTilde = "i̇̃";
    private static final String iOgonekDot = "į̇";
    private static final String jDot = "j̇";
    private String exceptions;
    private int[] indexes;
    private Trie2_16 trie;
    private char[] unfold;
    private static final byte[] flagsOffset = {0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8};
    public static final StringBuilder dummyStringBuilder = new StringBuilder();

    public interface ContextIterator {
        int next();

        void reset(int i);
    }

    private UCaseProps() throws IOException {
        readData(ICUBinary.getRequiredData(DATA_FILE_NAME));
    }

    private final void readData(ByteBuffer byteBuffer) throws IOException {
        ICUBinary.readHeader(byteBuffer, FMT, new IsAcceptable());
        int i = byteBuffer.getInt();
        if (i < 16) {
            throw new IOException("indexes[0] too small in ucase.icu");
        }
        this.indexes = new int[i];
        this.indexes[0] = i;
        for (int i2 = 1; i2 < i; i2++) {
            this.indexes[i2] = byteBuffer.getInt();
        }
        this.trie = Trie2_16.createFromSerialized(byteBuffer);
        int i3 = this.indexes[2];
        int serializedLength = this.trie.getSerializedLength();
        if (serializedLength > i3) {
            throw new IOException("ucase.icu: not enough bytes for the trie");
        }
        ICUBinary.skipBytes(byteBuffer, i3 - serializedLength);
        int i4 = this.indexes[3];
        if (i4 > 0) {
            this.exceptions = ICUBinary.getString(byteBuffer, i4, 0);
        }
        int i5 = this.indexes[4];
        if (i5 > 0) {
            this.unfold = ICUBinary.getChars(byteBuffer, i5, 0);
        }
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] bArr) {
            return bArr[0] == 3;
        }
    }

    public final void addPropertyStarts(UnicodeSet unicodeSet) {
        for (Trie2.Range range : this.trie) {
            if (!range.leadSurrogate) {
                unicodeSet.add(range.startCodePoint);
            } else {
                return;
            }
        }
    }

    private static final int getExceptionsOffset(int i) {
        return i >> 5;
    }

    private static final boolean propsHasException(int i) {
        return (i & 16) != 0;
    }

    static {
        try {
            INSTANCE = new UCaseProps();
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static final boolean hasSlot(int i, int i2) {
        return (i & (1 << i2)) != 0;
    }

    private static final byte slotOffset(int i, int i2) {
        return flagsOffset[i & ((1 << i2) - 1)];
    }

    private final long getSlotValueAndOffset(int i, int i2, int i3) {
        int iSlotOffset;
        long jCharAt;
        if ((i & 256) == 0) {
            iSlotOffset = i3 + slotOffset(i, i2);
            jCharAt = this.exceptions.charAt(iSlotOffset);
        } else {
            int iSlotOffset2 = i3 + (2 * slotOffset(i, i2));
            int i4 = iSlotOffset2 + 1;
            long jCharAt2 = (((long) this.exceptions.charAt(iSlotOffset2)) << 16) | ((long) this.exceptions.charAt(i4));
            iSlotOffset = i4;
            jCharAt = jCharAt2;
        }
        return jCharAt | (((long) iSlotOffset) << 32);
    }

    private final int getSlotValue(int i, int i2, int i3) {
        if ((i & 256) == 0) {
            return this.exceptions.charAt(i3 + slotOffset(i, i2));
        }
        int iSlotOffset = i3 + (2 * slotOffset(i, i2));
        return (this.exceptions.charAt(iSlotOffset) << 16) | this.exceptions.charAt(iSlotOffset + 1);
    }

    public final int tolower(int i) {
        int i2 = this.trie.get(i);
        if (!propsHasException(i2)) {
            if (getTypeFromProps(i2) >= 2) {
                return i + getDelta(i2);
            }
            return i;
        }
        int exceptionsOffset = getExceptionsOffset(i2);
        int i3 = exceptionsOffset + 1;
        char cCharAt = this.exceptions.charAt(exceptionsOffset);
        if (hasSlot(cCharAt, 0)) {
            return getSlotValue(cCharAt, 0, i3);
        }
        return i;
    }

    public final int toupper(int i) {
        int i2 = this.trie.get(i);
        if (!propsHasException(i2)) {
            if (getTypeFromProps(i2) == 1) {
                return i + getDelta(i2);
            }
            return i;
        }
        int exceptionsOffset = getExceptionsOffset(i2);
        int i3 = exceptionsOffset + 1;
        char cCharAt = this.exceptions.charAt(exceptionsOffset);
        if (hasSlot(cCharAt, 2)) {
            return getSlotValue(cCharAt, 2, i3);
        }
        return i;
    }

    public final int totitle(int i) {
        int i2 = this.trie.get(i);
        if (!propsHasException(i2)) {
            if (getTypeFromProps(i2) == 1) {
                return i + getDelta(i2);
            }
            return i;
        }
        int exceptionsOffset = getExceptionsOffset(i2);
        int i3 = exceptionsOffset + 1;
        char cCharAt = this.exceptions.charAt(exceptionsOffset);
        int i4 = 3;
        if (!hasSlot(cCharAt, 3)) {
            if (!hasSlot(cCharAt, 2)) {
                return i;
            }
            i4 = 2;
        }
        return getSlotValue(cCharAt, i4, i3);
    }

    public final void addCaseClosure(int i, UnicodeSet unicodeSet) {
        int i2;
        int delta;
        if (i != 73) {
            if (i == 105) {
                unicodeSet.add(73);
                return;
            }
            switch (i) {
                case 304:
                    unicodeSet.add(iDot);
                    break;
                case 305:
                    break;
                default:
                    int i3 = this.trie.get(i);
                    if (!propsHasException(i3)) {
                        if (getTypeFromProps(i3) != 0 && (delta = getDelta(i3)) != 0) {
                            unicodeSet.add(i + delta);
                            break;
                        }
                    } else {
                        int exceptionsOffset = getExceptionsOffset(i3);
                        int i4 = exceptionsOffset + 1;
                        char cCharAt = this.exceptions.charAt(exceptionsOffset);
                        int charCount = 0;
                        for (int i5 = 0; i5 <= 3; i5++) {
                            if (hasSlot(cCharAt, i5)) {
                                unicodeSet.add(getSlotValue(cCharAt, i5, i4));
                            }
                        }
                        if (hasSlot(cCharAt, 6)) {
                            long slotValueAndOffset = getSlotValueAndOffset(cCharAt, 6, i4);
                            int i6 = ((int) slotValueAndOffset) & 15;
                            int i7 = ((int) (slotValueAndOffset >> 32)) + 1;
                            i2 = i6;
                            charCount = i7;
                        } else {
                            i2 = 0;
                        }
                        if (hasSlot(cCharAt, 7)) {
                            long slotValueAndOffset2 = getSlotValueAndOffset(cCharAt, 7, i4);
                            int i8 = ((int) slotValueAndOffset2) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                            int i9 = ((int) (slotValueAndOffset2 >> 32)) + 1 + (i8 & 15);
                            int i10 = i8 >> 4;
                            int i11 = i10 & 15;
                            if (i11 != 0) {
                                int i12 = i11 + i9;
                                unicodeSet.add(this.exceptions.substring(i9, i12));
                                i9 = i12;
                            }
                            int i13 = i10 >> 4;
                            charCount = i9 + (i13 & 15) + (i13 >> 4);
                        }
                        int i14 = i2 + charCount;
                        while (charCount < i14) {
                            int iCodePointAt = this.exceptions.codePointAt(charCount);
                            unicodeSet.add(iCodePointAt);
                            charCount += UTF16.getCharCount(iCodePointAt);
                        }
                        break;
                    }
                    break;
            }
            return;
        }
        unicodeSet.add(105);
    }

    private final int strcmpMax(String str, int i, int i2) {
        int length = str.length();
        int i3 = i2 - length;
        int i4 = length;
        int i5 = i;
        int i6 = 0;
        while (true) {
            int i7 = i6 + 1;
            char cCharAt = str.charAt(i6);
            int i8 = i5 + 1;
            char c = this.unfold[i5];
            if (c == 0) {
                return 1;
            }
            int i9 = cCharAt - c;
            if (i9 != 0) {
                return i9;
            }
            i4--;
            if (i4 > 0) {
                i6 = i7;
                i5 = i8;
            } else {
                if (i3 == 0 || this.unfold[i8] == 0) {
                    return 0;
                }
                return -i3;
            }
        }
    }

    public final boolean addStringCaseClosure(String str, UnicodeSet unicodeSet) {
        int length;
        if (this.unfold == null || str == null || (length = str.length()) <= 1) {
            return false;
        }
        char c = this.unfold[0];
        char c2 = this.unfold[1];
        char charCount = this.unfold[2];
        if (length > charCount) {
            return false;
        }
        int i = 0;
        while (i < c) {
            int i2 = (i + (c == true ? 1 : 0)) / 2;
            int i3 = i2 + 1;
            int i4 = i3 * c2;
            int iStrcmpMax = strcmpMax(str, i4, charCount);
            if (iStrcmpMax == 0) {
                while (charCount < c2 && this.unfold[i4 + charCount] != 0) {
                    int iCharAt = UTF16.charAt(this.unfold, i4, this.unfold.length, charCount);
                    unicodeSet.add(iCharAt);
                    addCaseClosure(iCharAt, unicodeSet);
                    charCount += UTF16.getCharCount(iCharAt);
                }
                return true;
            }
            if (iStrcmpMax < 0) {
                c = i2;
            } else {
                i = i3;
            }
        }
        return false;
    }

    public final int getType(int i) {
        return getTypeFromProps(this.trie.get(i));
    }

    public final int getTypeOrIgnorable(int i) {
        return getTypeAndIgnorableFromProps(this.trie.get(i));
    }

    public final int getDotType(int i) {
        int i2 = this.trie.get(i);
        if (!propsHasException(i2)) {
            return i2 & 96;
        }
        return (this.exceptions.charAt(getExceptionsOffset(i2)) >> 7) & 96;
    }

    public final boolean isSoftDotted(int i) {
        return getDotType(i) == 32;
    }

    public final boolean isCaseSensitive(int i) {
        return (this.trie.get(i) & 8) != 0;
    }

    public static final int getCaseLocale(Locale locale) {
        return getCaseLocale(locale.getLanguage());
    }

    public static final int getCaseLocale(ULocale uLocale) {
        return getCaseLocale(uLocale.getLanguage());
    }

    private static final int getCaseLocale(String str) {
        if (str.length() == 2) {
            if (str.equals("en") || str.charAt(0) > 't') {
                return 1;
            }
            if (str.equals("tr") || str.equals("az")) {
                return 2;
            }
            if (str.equals("el")) {
                return 4;
            }
            if (str.equals("lt")) {
                return 3;
            }
            if (str.equals("nl")) {
                return 5;
            }
        } else if (str.length() == 3) {
            if (str.equals("tur") || str.equals("aze")) {
                return 2;
            }
            if (str.equals("ell")) {
                return 4;
            }
            if (str.equals("lit")) {
                return 3;
            }
            if (str.equals("nld")) {
                return 5;
            }
        }
        return 1;
    }

    private final boolean isFollowedByCasedLetter(ContextIterator contextIterator, int i) {
        int typeOrIgnorable;
        if (contextIterator == null) {
            return false;
        }
        contextIterator.reset(i);
        do {
            int next = contextIterator.next();
            if (next < 0) {
                return false;
            }
            typeOrIgnorable = getTypeOrIgnorable(next);
        } while ((typeOrIgnorable & 4) != 0);
        if (typeOrIgnorable == 0) {
            return false;
        }
        return true;
    }

    private final boolean isPrecededBySoftDotted(ContextIterator contextIterator) {
        int dotType;
        if (contextIterator == null) {
            return false;
        }
        contextIterator.reset(-1);
        do {
            int next = contextIterator.next();
            if (next < 0) {
                return false;
            }
            dotType = getDotType(next);
            if (dotType == 32) {
                return true;
            }
        } while (dotType == 96);
        return false;
    }

    private final boolean isPrecededBy_I(ContextIterator contextIterator) {
        int next;
        if (contextIterator == null) {
            return false;
        }
        contextIterator.reset(-1);
        do {
            next = contextIterator.next();
            if (next < 0) {
                return false;
            }
            if (next == 73) {
                return true;
            }
        } while (getDotType(next) == 96);
        return false;
    }

    private final boolean isFollowedByMoreAbove(ContextIterator contextIterator) {
        int dotType;
        if (contextIterator == null) {
            return false;
        }
        contextIterator.reset(1);
        do {
            int next = contextIterator.next();
            if (next < 0) {
                return false;
            }
            dotType = getDotType(next);
            if (dotType == 64) {
                return true;
            }
        } while (dotType == 96);
        return false;
    }

    private final boolean isFollowedByDotAbove(ContextIterator contextIterator) {
        int next;
        if (contextIterator == null) {
            return false;
        }
        contextIterator.reset(1);
        do {
            next = contextIterator.next();
            if (next < 0) {
                return false;
            }
            if (next == 775) {
                return true;
            }
        } while (getDotType(next) == 96);
        return false;
    }

    public final int toFullLower(int i, ContextIterator contextIterator, Appendable appendable, int i2) {
        int slotValue;
        int i3 = this.trie.get(i);
        if (!propsHasException(i3)) {
            if (getTypeFromProps(i3) >= 2) {
                slotValue = getDelta(i3) + i;
            } else {
                slotValue = i;
            }
        } else {
            int exceptionsOffset = getExceptionsOffset(i3);
            int i4 = exceptionsOffset + 1;
            char cCharAt = this.exceptions.charAt(exceptionsOffset);
            if ((cCharAt & 16384) != 0) {
                if (i2 == 3 && (((i == 73 || i == 74 || i == 302) && isFollowedByMoreAbove(contextIterator)) || i == 204 || i == 205 || i == 296)) {
                    try {
                        switch (i) {
                            case 73:
                                appendable.append(iDot);
                                return 2;
                            case 74:
                                appendable.append(jDot);
                                return 2;
                            case 204:
                                appendable.append(iDotGrave);
                                return 3;
                            case 205:
                                appendable.append(iDotAcute);
                                return 3;
                            case 296:
                                appendable.append(iDotTilde);
                                return 3;
                            case 302:
                                appendable.append(iOgonekDot);
                                return 2;
                            default:
                                return 0;
                        }
                    } catch (IOException e) {
                        throw new ICUUncheckedIOException(e);
                    }
                }
                if (i2 == 2 && i == 304) {
                    return 105;
                }
                if (i2 == 2 && i == 775 && isPrecededBy_I(contextIterator)) {
                    return 0;
                }
                if (i2 == 2 && i == 73 && !isFollowedByDotAbove(contextIterator)) {
                    return 305;
                }
                if (i == 304) {
                    try {
                        appendable.append(iDot);
                        return 2;
                    } catch (IOException e2) {
                        throw new ICUUncheckedIOException(e2);
                    }
                }
                if (i == 931 && !isFollowedByCasedLetter(contextIterator, 1) && isFollowedByCasedLetter(contextIterator, -1)) {
                    return 962;
                }
            } else if (hasSlot(cCharAt, 7)) {
                long slotValueAndOffset = getSlotValueAndOffset(cCharAt, 7, i4);
                int i5 = ((int) slotValueAndOffset) & 15;
                if (i5 != 0) {
                    int i6 = ((int) (slotValueAndOffset >> 32)) + 1;
                    try {
                        appendable.append(this.exceptions, i6, i6 + i5);
                        return i5;
                    } catch (IOException e3) {
                        throw new ICUUncheckedIOException(e3);
                    }
                }
            }
            if (hasSlot(cCharAt, 0)) {
                slotValue = getSlotValue(cCharAt, 0, i4);
            }
        }
        return slotValue == i ? ~slotValue : slotValue;
    }

    private final int toUpperOrTitle(int i, ContextIterator contextIterator, Appendable appendable, int i2, boolean z) {
        int i3;
        int slotValue;
        int i4 = this.trie.get(i);
        if (!propsHasException(i4)) {
            if (getTypeFromProps(i4) == 1) {
                slotValue = getDelta(i4) + i;
            } else {
                slotValue = i;
            }
        } else {
            int exceptionsOffset = getExceptionsOffset(i4);
            int i5 = exceptionsOffset + 1;
            char cCharAt = this.exceptions.charAt(exceptionsOffset);
            int i6 = 3;
            if ((cCharAt & 16384) != 0) {
                if (i2 == 2 && i == 105) {
                    return 304;
                }
                if (i2 == 3 && i == 775 && isPrecededBySoftDotted(contextIterator)) {
                    return 0;
                }
            } else if (hasSlot(cCharAt, 7)) {
                long slotValueAndOffset = getSlotValueAndOffset(cCharAt, 7, i5);
                int i7 = ((int) slotValueAndOffset) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                int i8 = ((int) (slotValueAndOffset >> 32)) + 1 + (i7 & 15);
                int i9 = i7 >> 4;
                int i10 = i8 + (i9 & 15);
                int i11 = i9 >> 4;
                if (z) {
                    i3 = i11 & 15;
                } else {
                    i10 += i11 & 15;
                    i3 = (i11 >> 4) & 15;
                }
                if (i3 != 0) {
                    try {
                        appendable.append(this.exceptions, i10, i10 + i3);
                        return i3;
                    } catch (IOException e) {
                        throw new ICUUncheckedIOException(e);
                    }
                }
            }
            if (z || !hasSlot(cCharAt, 3)) {
                if (!hasSlot(cCharAt, 2)) {
                    return ~i;
                }
                i6 = 2;
            }
            slotValue = getSlotValue(cCharAt, i6, i5);
        }
        return slotValue == i ? ~slotValue : slotValue;
    }

    public final int toFullUpper(int i, ContextIterator contextIterator, Appendable appendable, int i2) {
        return toUpperOrTitle(i, contextIterator, appendable, i2, true);
    }

    public final int toFullTitle(int i, ContextIterator contextIterator, Appendable appendable, int i2) {
        return toUpperOrTitle(i, contextIterator, appendable, i2, false);
    }

    public final int fold(int i, int i2) {
        int i3 = this.trie.get(i);
        if (!propsHasException(i3)) {
            if (getTypeFromProps(i3) >= 2) {
                return i + getDelta(i3);
            }
            return i;
        }
        int exceptionsOffset = getExceptionsOffset(i3);
        int i4 = exceptionsOffset + 1;
        char cCharAt = this.exceptions.charAt(exceptionsOffset);
        if ((32768 & cCharAt) != 0) {
            if ((i2 & 7) == 0) {
                if (i == 73) {
                    return 105;
                }
                if (i == 304) {
                    return i;
                }
            } else {
                if (i == 73) {
                    return 305;
                }
                if (i == 304) {
                    return 105;
                }
            }
        }
        int i5 = 1;
        if (!hasSlot(cCharAt, 1)) {
            if (!hasSlot(cCharAt, 0)) {
                return i;
            }
            i5 = 0;
        }
        return getSlotValue(cCharAt, i5, i4);
    }

    public final int toFullFolding(int i, Appendable appendable, int i2) {
        int slotValue;
        int i3 = this.trie.get(i);
        if (!propsHasException(i3)) {
            if (getTypeFromProps(i3) >= 2) {
                slotValue = getDelta(i3) + i;
            } else {
                slotValue = i;
            }
        } else {
            int exceptionsOffset = getExceptionsOffset(i3);
            int i4 = exceptionsOffset + 1;
            char cCharAt = this.exceptions.charAt(exceptionsOffset);
            if ((32768 & cCharAt) != 0) {
                if ((i2 & 7) == 0) {
                    if (i == 73) {
                        return 105;
                    }
                    if (i == 304) {
                        try {
                            appendable.append(iDot);
                            return 2;
                        } catch (IOException e) {
                            throw new ICUUncheckedIOException(e);
                        }
                    }
                } else {
                    if (i == 73) {
                        return 305;
                    }
                    if (i == 304) {
                        return 105;
                    }
                }
            } else if (hasSlot(cCharAt, 7)) {
                long slotValueAndOffset = getSlotValueAndOffset(cCharAt, 7, i4);
                int i5 = ((int) slotValueAndOffset) & DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH;
                int i6 = ((int) (slotValueAndOffset >> 32)) + 1 + (i5 & 15);
                int i7 = (i5 >> 4) & 15;
                if (i7 != 0) {
                    try {
                        appendable.append(this.exceptions, i6, i6 + i7);
                        return i7;
                    } catch (IOException e2) {
                        throw new ICUUncheckedIOException(e2);
                    }
                }
            }
            int i8 = 0;
            if (!hasSlot(cCharAt, 1)) {
                if (!hasSlot(cCharAt, 0)) {
                    return ~i;
                }
            } else {
                i8 = 1;
            }
            slotValue = getSlotValue(cCharAt, i8, i4);
        }
        return slotValue == i ? ~slotValue : slotValue;
    }

    public final boolean hasBinaryProperty(int i, int i2) {
        if (i2 == 22) {
            return 1 == getType(i);
        }
        if (i2 == 27) {
            return isSoftDotted(i);
        }
        if (i2 == 30) {
            return 2 == getType(i);
        }
        if (i2 == 34) {
            return isCaseSensitive(i);
        }
        if (i2 != 55) {
            switch (i2) {
                case 49:
                    return getType(i) != 0;
                case 50:
                    return (getTypeOrIgnorable(i) >> 2) != 0;
                case 51:
                    dummyStringBuilder.setLength(0);
                    return toFullLower(i, null, dummyStringBuilder, 1) >= 0;
                case 52:
                    dummyStringBuilder.setLength(0);
                    return toFullUpper(i, null, dummyStringBuilder, 1) >= 0;
                case 53:
                    dummyStringBuilder.setLength(0);
                    return toFullTitle(i, null, dummyStringBuilder, 1) >= 0;
                default:
                    return false;
            }
        }
        dummyStringBuilder.setLength(0);
        return toFullLower(i, null, dummyStringBuilder, 1) >= 0 || toFullUpper(i, null, dummyStringBuilder, 1) >= 0 || toFullTitle(i, null, dummyStringBuilder, 1) >= 0;
    }

    private static final int getTypeFromProps(int i) {
        return i & 3;
    }

    private static final int getTypeAndIgnorableFromProps(int i) {
        return i & 7;
    }

    private static final int getDelta(int i) {
        return ((short) i) >> 7;
    }
}
