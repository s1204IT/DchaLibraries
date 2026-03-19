package android.icu.text;

import android.icu.impl.BMPSet;
import android.icu.impl.Norm2AllModes;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.RuleCharacterIterator;
import android.icu.impl.SortedSetRelation;
import android.icu.impl.StringRange;
import android.icu.impl.UBiDiProps;
import android.icu.impl.UCaseProps;
import android.icu.impl.UCharacterProperty;
import android.icu.impl.UPropertyAliases;
import android.icu.impl.UnicodeSetStringSpan;
import android.icu.impl.Utility;
import android.icu.lang.CharSequences;
import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;
import android.icu.lang.UScript;
import android.icu.util.Freezable;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.OutputInt;
import android.icu.util.ULocale;
import android.icu.util.VersionInfo;
import dalvik.bytecode.Opcodes;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

public class UnicodeSet extends UnicodeFilter implements Iterable<String>, Comparable<UnicodeSet>, Freezable<UnicodeSet> {
    static final boolean $assertionsDisabled = false;
    public static final int ADD_CASE_MAPPINGS = 4;
    private static final String ANY_ID = "ANY";
    private static final String ASCII_ID = "ASCII";
    private static final String ASSIGNED = "Assigned";
    public static final int CASE = 2;
    public static final int CASE_INSENSITIVE = 2;
    private static final int GROW_EXTRA = 16;
    private static final int HIGH = 1114112;
    public static final int IGNORE_SPACE = 1;
    private static final int LAST0_START = 0;
    private static final int LAST1_RANGE = 1;
    private static final int LAST2_SET = 2;
    private static final int LOW = 0;
    public static final int MAX_VALUE = 1114111;
    public static final int MIN_VALUE = 0;
    private static final int MODE0_NONE = 0;
    private static final int MODE1_INBRACKET = 1;
    private static final int MODE2_OUTBRACKET = 2;
    private static final int SETMODE0_NONE = 0;
    private static final int SETMODE1_UNICODESET = 1;
    private static final int SETMODE2_PROPERTYPAT = 2;
    private static final int SETMODE3_PREPARSED = 3;
    private static final int START_EXTRA = 16;
    private volatile BMPSet bmpSet;
    private int[] buffer;
    private int len;
    private int[] list;
    private String pat;
    private int[] rangeList;
    private volatile UnicodeSetStringSpan stringSpan;
    TreeSet<String> strings;
    public static final UnicodeSet EMPTY = new UnicodeSet().freeze();
    public static final UnicodeSet ALL_CODE_POINTS = new UnicodeSet(0, 1114111).freeze();
    private static XSymbolTable XSYMBOL_TABLE = null;
    private static UnicodeSet[] INCLUSIONS = null;
    private static final VersionInfo NO_VERSION = VersionInfo.getInstance(0, 0, 0, 0);

    public enum ComparisonStyle {
        SHORTER_FIRST,
        LEXICOGRAPHIC,
        LONGER_FIRST
    }

    private interface Filter {
        boolean contains(int i);
    }

    public enum SpanCondition {
        NOT_CONTAINED,
        CONTAINED,
        SIMPLE,
        CONDITION_COUNT
    }

    public UnicodeSet() {
        this.strings = new TreeSet<>();
        this.pat = null;
        this.list = new int[17];
        int[] iArr = this.list;
        int i = this.len;
        this.len = i + 1;
        iArr[i] = 1114112;
    }

    public UnicodeSet(UnicodeSet unicodeSet) {
        this.strings = new TreeSet<>();
        this.pat = null;
        set(unicodeSet);
    }

    public UnicodeSet(int i, int i2) {
        this();
        complement(i, i2);
    }

    public UnicodeSet(int... iArr) {
        this.strings = new TreeSet<>();
        this.pat = null;
        if ((iArr.length & 1) != 0) {
            throw new IllegalArgumentException("Must have even number of integers");
        }
        this.list = new int[iArr.length + 1];
        this.len = this.list.length;
        int i = -1;
        int i2 = 0;
        while (i2 < iArr.length) {
            int i3 = iArr[i2];
            if (i >= i3) {
                throw new IllegalArgumentException("Must be monotonically increasing.");
            }
            int i4 = i2 + 1;
            this.list[i2] = i3;
            i = iArr[i4] + 1;
            if (i3 >= i) {
                throw new IllegalArgumentException("Must be monotonically increasing.");
            }
            this.list[i4] = i;
            i2 = i4 + 1;
        }
        this.list[i2] = 1114112;
    }

    public UnicodeSet(String str) {
        this();
        applyPattern(str, (ParsePosition) null, (SymbolTable) null, 1);
    }

    public UnicodeSet(String str, boolean z) {
        this();
        applyPattern(str, (ParsePosition) null, (SymbolTable) null, z ? 1 : 0);
    }

    public UnicodeSet(String str, int i) {
        this();
        applyPattern(str, (ParsePosition) null, (SymbolTable) null, i);
    }

    public UnicodeSet(String str, ParsePosition parsePosition, SymbolTable symbolTable) {
        this();
        applyPattern(str, parsePosition, symbolTable, 1);
    }

    public UnicodeSet(String str, ParsePosition parsePosition, SymbolTable symbolTable, int i) {
        this();
        applyPattern(str, parsePosition, symbolTable, i);
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        UnicodeSet unicodeSet = new UnicodeSet(this);
        unicodeSet.bmpSet = this.bmpSet;
        unicodeSet.stringSpan = this.stringSpan;
        return unicodeSet;
    }

    public UnicodeSet set(int i, int i2) {
        checkFrozen();
        clear();
        complement(i, i2);
        return this;
    }

    public UnicodeSet set(UnicodeSet unicodeSet) {
        checkFrozen();
        this.list = (int[]) unicodeSet.list.clone();
        this.len = unicodeSet.len;
        this.pat = unicodeSet.pat;
        this.strings = new TreeSet<>((SortedSet) unicodeSet.strings);
        return this;
    }

    public final UnicodeSet applyPattern(String str) {
        checkFrozen();
        return applyPattern(str, (ParsePosition) null, (SymbolTable) null, 1);
    }

    public UnicodeSet applyPattern(String str, boolean z) {
        checkFrozen();
        return applyPattern(str, (ParsePosition) null, (SymbolTable) null, z ? 1 : 0);
    }

    public UnicodeSet applyPattern(String str, int i) {
        checkFrozen();
        return applyPattern(str, (ParsePosition) null, (SymbolTable) null, i);
    }

    public static boolean resemblesPattern(String str, int i) {
        if ((i + 1 < str.length() && str.charAt(i) == '[') || resemblesPropertyPattern(str, i)) {
            return true;
        }
        return false;
    }

    private static void appendCodePoint(Appendable appendable, int i) {
        try {
            if (i <= 65535) {
                appendable.append((char) i);
            } else {
                appendable.append(UTF16.getLeadSurrogate(i)).append(UTF16.getTrailSurrogate(i));
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static void append(Appendable appendable, CharSequence charSequence) {
        try {
            appendable.append(charSequence);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static <T extends Appendable> T _appendToPat(T t, String str, boolean z) {
        int iCharCount = 0;
        while (iCharCount < str.length()) {
            int iCodePointAt = str.codePointAt(iCharCount);
            _appendToPat(t, iCodePointAt, z);
            iCharCount += Character.charCount(iCodePointAt);
        }
        return t;
    }

    private static <T extends Appendable> T _appendToPat(T t, int i, boolean z) {
        if (z) {
            try {
                if (Utility.isUnprintable(i) && Utility.escapeUnprintable(t, i)) {
                    return t;
                }
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        if (i == 36 || i == 38 || i == 45 || i == 58 || i == 123 || i == 125) {
            t.append(PatternTokenizer.BACK_SLASH);
        } else {
            switch (i) {
                case 91:
                case 92:
                case 93:
                case 94:
                    break;
                default:
                    if (PatternProps.isWhiteSpace(i)) {
                        t.append(PatternTokenizer.BACK_SLASH);
                    }
                    break;
            }
        }
        appendCodePoint(t, i);
        return t;
    }

    @Override
    public String toPattern(boolean z) {
        if (this.pat != null && !z) {
            return this.pat;
        }
        return ((StringBuilder) _toPattern(new StringBuilder(), z)).toString();
    }

    private <T extends Appendable> T _toPattern(T t, boolean z) {
        if (this.pat == null) {
            return (T) appendNewPattern(t, z, true);
        }
        try {
            if (!z) {
                t.append(this.pat);
                return t;
            }
            int iCharCount = 0;
            boolean z2 = false;
            while (iCharCount < this.pat.length()) {
                int iCodePointAt = this.pat.codePointAt(iCharCount);
                iCharCount += Character.charCount(iCodePointAt);
                if (Utility.isUnprintable(iCodePointAt)) {
                    Utility.escapeUnprintable(t, iCodePointAt);
                } else if (z2 || iCodePointAt != 92) {
                    if (z2) {
                        t.append(PatternTokenizer.BACK_SLASH);
                    }
                    appendCodePoint(t, iCodePointAt);
                } else {
                    z2 = true;
                }
                z2 = false;
            }
            if (z2) {
                t.append(PatternTokenizer.BACK_SLASH);
            }
            return t;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public StringBuffer _generatePattern(StringBuffer stringBuffer, boolean z) {
        return _generatePattern(stringBuffer, z, true);
    }

    public StringBuffer _generatePattern(StringBuffer stringBuffer, boolean z, boolean z2) {
        return (StringBuffer) appendNewPattern(stringBuffer, z, z2);
    }

    private <T extends Appendable> T appendNewPattern(T t, boolean z, boolean z2) {
        try {
            t.append('[');
            int rangeCount = getRangeCount();
            if (rangeCount > 1 && getRangeStart(0) == 0 && getRangeEnd(rangeCount - 1) == 1114111) {
                t.append('^');
                for (int i = 1; i < rangeCount; i++) {
                    int rangeEnd = getRangeEnd(i - 1) + 1;
                    int rangeStart = getRangeStart(i) - 1;
                    _appendToPat(t, rangeEnd, z);
                    if (rangeEnd != rangeStart) {
                        if (rangeEnd + 1 != rangeStart) {
                            t.append('-');
                        }
                        _appendToPat(t, rangeStart, z);
                    }
                }
            } else {
                for (int i2 = 0; i2 < rangeCount; i2++) {
                    int rangeStart2 = getRangeStart(i2);
                    int rangeEnd2 = getRangeEnd(i2);
                    _appendToPat(t, rangeStart2, z);
                    if (rangeStart2 != rangeEnd2) {
                        if (rangeStart2 + 1 != rangeEnd2) {
                            t.append('-');
                        }
                        _appendToPat(t, rangeEnd2, z);
                    }
                }
            }
            if (z2 && this.strings.size() > 0) {
                for (String str : this.strings) {
                    t.append('{');
                    _appendToPat(t, str, z);
                    t.append('}');
                }
            }
            t.append(']');
            return t;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public int size() {
        int rangeCount = getRangeCount();
        int rangeEnd = 0;
        for (int i = 0; i < rangeCount; i++) {
            rangeEnd += (getRangeEnd(i) - getRangeStart(i)) + 1;
        }
        return rangeEnd + this.strings.size();
    }

    public boolean isEmpty() {
        return this.len == 1 && this.strings.size() == 0;
    }

    @Override
    public boolean matchesIndexValue(int i) {
        for (int i2 = 0; i2 < getRangeCount(); i2++) {
            int rangeStart = getRangeStart(i2);
            int rangeEnd = getRangeEnd(i2);
            if ((rangeStart & (-256)) == (rangeEnd & (-256))) {
                if ((rangeStart & 255) <= i && i <= (rangeEnd & 255)) {
                    return true;
                }
            } else if ((rangeStart & 255) <= i || i <= (rangeEnd & 255)) {
                return true;
            }
        }
        if (this.strings.size() != 0) {
            Iterator<String> it = this.strings.iterator();
            while (it.hasNext()) {
                if ((UTF16.charAt(it.next(), 0) & 255) == i) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int matches(Replaceable replaceable, int[] iArr, int i, boolean z) {
        if (iArr[0] == i) {
            if (contains(DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH)) {
                return z ? 1 : 2;
            }
            return 0;
        }
        if (this.strings.size() != 0) {
            boolean z2 = iArr[0] < i;
            char cCharAt = replaceable.charAt(iArr[0]);
            int i2 = 0;
            for (String str : this.strings) {
                char cCharAt2 = str.charAt(z2 ? 0 : str.length() - 1);
                if (z2 && cCharAt2 > cCharAt) {
                    break;
                }
                if (cCharAt2 == cCharAt) {
                    int iMatchRest = matchRest(replaceable, iArr[0], i, str);
                    if (z) {
                        if (iMatchRest == (z2 ? i - iArr[0] : iArr[0] - i)) {
                            return 1;
                        }
                    }
                    if (iMatchRest == str.length()) {
                        if (iMatchRest > i2) {
                            i2 = iMatchRest;
                        }
                        if (z2 && iMatchRest < i2) {
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            }
            if (i2 != 0) {
                int i3 = iArr[0];
                if (!z2) {
                    i2 = -i2;
                }
                iArr[0] = i3 + i2;
                return 2;
            }
        }
        return super.matches(replaceable, iArr, i, z);
    }

    private static int matchRest(Replaceable replaceable, int i, int i2, String str) {
        int i3;
        int length = str.length();
        int i4 = 1;
        if (i < i2) {
            i3 = i2 - i;
            if (i3 > length) {
                i3 = length;
            }
            while (i4 < i3) {
                if (replaceable.charAt(i + i4) != str.charAt(i4)) {
                    return 0;
                }
                i4++;
            }
        } else {
            i3 = i - i2;
            if (i3 > length) {
                i3 = length;
            }
            int i5 = length - 1;
            while (i4 < i3) {
                if (replaceable.charAt(i - i4) != str.charAt(i5 - i4)) {
                    return 0;
                }
                i4++;
            }
        }
        return i3;
    }

    @Deprecated
    public int matchesAt(CharSequence charSequence, int i) {
        int charCount = -1;
        if (this.strings.size() != 0) {
            char cCharAt = charSequence.charAt(i);
            String next = null;
            Iterator<String> it = this.strings.iterator();
            while (true) {
                if (!it.hasNext()) {
                    while (true) {
                        int iMatchesAt = matchesAt(charSequence, i, next);
                        if (charCount > iMatchesAt) {
                            break;
                        }
                        if (it.hasNext()) {
                            next = it.next();
                            charCount = iMatchesAt;
                        } else {
                            charCount = iMatchesAt;
                            break;
                        }
                    }
                } else {
                    next = it.next();
                    char cCharAt2 = next.charAt(0);
                    if (cCharAt2 >= cCharAt && cCharAt2 > cCharAt) {
                        break;
                    }
                }
            }
        }
        if (charCount < 2) {
            int iCharAt = UTF16.charAt(charSequence, i);
            if (contains(iCharAt)) {
                charCount = UTF16.getCharCount(iCharAt);
            }
        }
        return i + charCount;
    }

    private static int matchesAt(CharSequence charSequence, int i, CharSequence charSequence2) {
        int length = charSequence2.length();
        if (charSequence.length() + i > length) {
            return -1;
        }
        int i2 = 0;
        while (i2 < length) {
            if (charSequence2.charAt(i2) != charSequence.charAt(i)) {
                return -1;
            }
            i2++;
            i++;
        }
        return i2;
    }

    @Override
    public void addMatchSetTo(UnicodeSet unicodeSet) {
        unicodeSet.addAll(this);
    }

    public int indexOf(int i) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i, 6));
        }
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int i4 = i2 + 1;
            int i5 = this.list[i2];
            if (i < i5) {
                return -1;
            }
            int i6 = i4 + 1;
            int i7 = this.list[i4];
            if (i < i7) {
                return (i3 + i) - i5;
            }
            i3 += i7 - i5;
            i2 = i6;
        }
    }

    public int charAt(int i) {
        if (i >= 0) {
            int i2 = this.len & (-2);
            int i3 = 0;
            while (i3 < i2) {
                int i4 = i3 + 1;
                int i5 = this.list[i3];
                int i6 = i4 + 1;
                int i7 = this.list[i4] - i5;
                if (i < i7) {
                    return i5 + i;
                }
                i -= i7;
                i3 = i6;
            }
            return -1;
        }
        return -1;
    }

    public UnicodeSet add(int i, int i2) {
        checkFrozen();
        return add_unchecked(i, i2);
    }

    public UnicodeSet addAll(int i, int i2) {
        checkFrozen();
        return add_unchecked(i, i2);
    }

    private UnicodeSet add_unchecked(int i, int i2) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i, 6));
        }
        if (i2 < 0 || i2 > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i2, 6));
        }
        if (i < i2) {
            add(range(i, i2), 2, 0);
        } else if (i == i2) {
            add(i);
        }
        return this;
    }

    public final UnicodeSet add(int i) {
        checkFrozen();
        return add_unchecked(i);
    }

    private final UnicodeSet add_unchecked(int i) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i, 6));
        }
        int iFindCodePoint = findCodePoint(i);
        if ((iFindCodePoint & 1) != 0) {
            return this;
        }
        if (i == this.list[iFindCodePoint] - 1) {
            this.list[iFindCodePoint] = i;
            if (i == 1114111) {
                ensureCapacity(this.len + 1);
                int[] iArr = this.list;
                int i2 = this.len;
                this.len = i2 + 1;
                iArr[i2] = 1114112;
            }
            if (iFindCodePoint > 0) {
                int i3 = iFindCodePoint - 1;
                if (i == this.list[i3]) {
                    System.arraycopy(this.list, iFindCodePoint + 1, this.list, i3, (this.len - iFindCodePoint) - 1);
                    this.len -= 2;
                }
            }
        } else if (iFindCodePoint > 0) {
            int i4 = iFindCodePoint - 1;
            if (i == this.list[i4]) {
                int[] iArr2 = this.list;
                iArr2[i4] = iArr2[i4] + 1;
            } else {
                if (this.len + 2 > this.list.length) {
                    int[] iArr3 = new int[this.len + 2 + 16];
                    if (iFindCodePoint != 0) {
                        System.arraycopy(this.list, 0, iArr3, 0, iFindCodePoint);
                    }
                    System.arraycopy(this.list, iFindCodePoint, iArr3, iFindCodePoint + 2, this.len - iFindCodePoint);
                    this.list = iArr3;
                } else {
                    System.arraycopy(this.list, iFindCodePoint, this.list, iFindCodePoint + 2, this.len - iFindCodePoint);
                }
                this.list[iFindCodePoint] = i;
                this.list[iFindCodePoint + 1] = i + 1;
                this.len += 2;
            }
        }
        this.pat = null;
        return this;
    }

    public final UnicodeSet add(CharSequence charSequence) {
        checkFrozen();
        int singleCP = getSingleCP(charSequence);
        if (singleCP < 0) {
            this.strings.add(charSequence.toString());
            this.pat = null;
        } else {
            add_unchecked(singleCP, singleCP);
        }
        return this;
    }

    private static int getSingleCP(CharSequence charSequence) {
        if (charSequence.length() < 1) {
            throw new IllegalArgumentException("Can't use zero-length strings in UnicodeSet");
        }
        if (charSequence.length() > 2) {
            return -1;
        }
        if (charSequence.length() == 1) {
            return charSequence.charAt(0);
        }
        int iCharAt = UTF16.charAt(charSequence, 0);
        if (iCharAt > 65535) {
            return iCharAt;
        }
        return -1;
    }

    public final UnicodeSet addAll(CharSequence charSequence) {
        checkFrozen();
        int charCount = 0;
        while (charCount < charSequence.length()) {
            int iCharAt = UTF16.charAt(charSequence, charCount);
            add_unchecked(iCharAt, iCharAt);
            charCount += UTF16.getCharCount(iCharAt);
        }
        return this;
    }

    public final UnicodeSet retainAll(CharSequence charSequence) {
        return retainAll(fromAll(charSequence));
    }

    public final UnicodeSet complementAll(CharSequence charSequence) {
        return complementAll(fromAll(charSequence));
    }

    public final UnicodeSet removeAll(CharSequence charSequence) {
        return removeAll(fromAll(charSequence));
    }

    public final UnicodeSet removeAllStrings() {
        checkFrozen();
        if (this.strings.size() != 0) {
            this.strings.clear();
            this.pat = null;
        }
        return this;
    }

    public static UnicodeSet from(CharSequence charSequence) {
        return new UnicodeSet().add(charSequence);
    }

    public static UnicodeSet fromAll(CharSequence charSequence) {
        return new UnicodeSet().addAll(charSequence);
    }

    public UnicodeSet retain(int i, int i2) {
        checkFrozen();
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i, 6));
        }
        if (i2 < 0 || i2 > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i2, 6));
        }
        if (i <= i2) {
            retain(range(i, i2), 2, 0);
        } else {
            clear();
        }
        return this;
    }

    public final UnicodeSet retain(int i) {
        return retain(i, i);
    }

    public final UnicodeSet retain(CharSequence charSequence) {
        int singleCP = getSingleCP(charSequence);
        if (singleCP < 0) {
            String string = charSequence.toString();
            if (this.strings.contains(string) && size() == 1) {
                return this;
            }
            clear();
            this.strings.add(string);
            this.pat = null;
        } else {
            retain(singleCP, singleCP);
        }
        return this;
    }

    public UnicodeSet remove(int i, int i2) {
        checkFrozen();
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i, 6));
        }
        if (i2 < 0 || i2 > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i2, 6));
        }
        if (i <= i2) {
            retain(range(i, i2), 2, 2);
        }
        return this;
    }

    public final UnicodeSet remove(int i) {
        return remove(i, i);
    }

    public final UnicodeSet remove(CharSequence charSequence) {
        int singleCP = getSingleCP(charSequence);
        if (singleCP < 0) {
            this.strings.remove(charSequence.toString());
            this.pat = null;
        } else {
            remove(singleCP, singleCP);
        }
        return this;
    }

    public UnicodeSet complement(int i, int i2) {
        checkFrozen();
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i, 6));
        }
        if (i2 < 0 || i2 > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i2, 6));
        }
        if (i <= i2) {
            xor(range(i, i2), 2, 0);
        }
        this.pat = null;
        return this;
    }

    public final UnicodeSet complement(int i) {
        return complement(i, i);
    }

    public UnicodeSet complement() {
        checkFrozen();
        if (this.list[0] == 0) {
            System.arraycopy(this.list, 1, this.list, 0, this.len - 1);
            this.len--;
        } else {
            ensureCapacity(this.len + 1);
            System.arraycopy(this.list, 0, this.list, 1, this.len);
            this.list[0] = 0;
            this.len++;
        }
        this.pat = null;
        return this;
    }

    public final UnicodeSet complement(CharSequence charSequence) {
        checkFrozen();
        int singleCP = getSingleCP(charSequence);
        if (singleCP < 0) {
            String string = charSequence.toString();
            if (this.strings.contains(string)) {
                this.strings.remove(string);
            } else {
                this.strings.add(string);
            }
            this.pat = null;
        } else {
            complement(singleCP, singleCP);
        }
        return this;
    }

    @Override
    public boolean contains(int i) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i, 6));
        }
        if (this.bmpSet != null) {
            return this.bmpSet.contains(i);
        }
        if (this.stringSpan != null) {
            return this.stringSpan.contains(i);
        }
        return (findCodePoint(i) & 1) != 0;
    }

    private final int findCodePoint(int i) {
        int i2 = 0;
        if (i < this.list[0]) {
            return 0;
        }
        if (this.len >= 2 && i >= this.list[this.len - 2]) {
            return this.len - 1;
        }
        int i3 = this.len - 1;
        while (true) {
            int i4 = (i2 + i3) >>> 1;
            if (i4 == i2) {
                return i3;
            }
            if (i < this.list[i4]) {
                i3 = i4;
            } else {
                i2 = i4;
            }
        }
    }

    public boolean contains(int i, int i2) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i, 6));
        }
        if (i2 < 0 || i2 > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i2, 6));
        }
        int iFindCodePoint = findCodePoint(i);
        return (iFindCodePoint & 1) != 0 && i2 < this.list[iFindCodePoint];
    }

    public final boolean contains(CharSequence charSequence) {
        int singleCP = getSingleCP(charSequence);
        if (singleCP < 0) {
            return this.strings.contains(charSequence.toString());
        }
        return contains(singleCP);
    }

    public boolean containsAll(UnicodeSet unicodeSet) {
        int[] iArr = unicodeSet.list;
        int i = this.len - 1;
        int i2 = unicodeSet.len - 1;
        boolean z = true;
        boolean z2 = true;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        while (true) {
            if (!z) {
                if (z2) {
                    if (i4 >= i2) {
                        break;
                    }
                    int i9 = i4 + 1;
                    int i10 = iArr[i4];
                    i4 = i9 + 1;
                    i8 = iArr[i9];
                    i5 = i10;
                }
                if (i5 < i6) {
                    z = true;
                    z2 = false;
                } else {
                    if (i5 < i7 || i8 > i6) {
                        break;
                    }
                    z2 = true;
                    z = false;
                }
            } else if (i3 >= i) {
                if (!z2 || i4 < i2) {
                    return false;
                }
            } else {
                int i11 = i3 + 1;
                int i12 = this.list[i3];
                int i13 = i11 + 1;
                i6 = this.list[i11];
                i3 = i13;
                i7 = i12;
                if (z2) {
                }
                if (i5 < i6) {
                }
            }
        }
        return false;
    }

    public boolean containsAll(String str) {
        int charCount = 0;
        while (charCount < str.length()) {
            int iCharAt = UTF16.charAt(str, charCount);
            if (contains(iCharAt)) {
                charCount += UTF16.getCharCount(iCharAt);
            } else {
                if (this.strings.size() == 0) {
                    return false;
                }
                return containsAll(str, 0);
            }
        }
        return true;
    }

    private boolean containsAll(String str, int i) {
        if (i >= str.length()) {
            return true;
        }
        int iCharAt = UTF16.charAt(str, i);
        if (contains(iCharAt) && containsAll(str, UTF16.getCharCount(iCharAt) + i)) {
            return true;
        }
        for (String str2 : this.strings) {
            if (str.startsWith(str2, i) && containsAll(str, str2.length() + i)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public String getRegexEquivalent() {
        if (this.strings.size() == 0) {
            return toString();
        }
        StringBuilder sb = new StringBuilder("(?:");
        appendNewPattern(sb, true, false);
        for (String str : this.strings) {
            sb.append('|');
            _appendToPat(sb, str, true);
        }
        sb.append(")");
        return sb.toString();
    }

    public boolean containsNone(int i, int i2) {
        if (i < 0 || i > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i, 6));
        }
        if (i2 < 0 || i2 > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(i2, 6));
        }
        int i3 = -1;
        do {
            i3++;
        } while (i >= this.list[i3]);
        return (i3 & 1) == 0 && i2 < this.list[i3];
    }

    public boolean containsNone(UnicodeSet unicodeSet) {
        int[] iArr = unicodeSet.list;
        int i = this.len - 1;
        int i2 = unicodeSet.len - 1;
        boolean z = true;
        boolean z2 = true;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        while (true) {
            if (!z) {
                if (z2) {
                    if (i4 >= i2) {
                        break;
                    }
                    int i9 = i4 + 1;
                    int i10 = iArr[i4];
                    i4 = i9 + 1;
                    i8 = iArr[i9];
                    i5 = i10;
                }
                if (i5 < i6) {
                    z = true;
                    z2 = false;
                } else if (i7 >= i8) {
                    z2 = true;
                    z = false;
                } else {
                    return false;
                }
            } else {
                if (i3 >= i) {
                    break;
                }
                int i11 = i3 + 1;
                int i12 = this.list[i3];
                int i13 = i11 + 1;
                i6 = this.list[i11];
                i3 = i13;
                i7 = i12;
                if (z2) {
                }
                if (i5 < i6) {
                }
            }
        }
    }

    public boolean containsNone(CharSequence charSequence) {
        return span(charSequence, SpanCondition.NOT_CONTAINED) == charSequence.length();
    }

    public final boolean containsSome(int i, int i2) {
        return !containsNone(i, i2);
    }

    public final boolean containsSome(UnicodeSet unicodeSet) {
        return !containsNone(unicodeSet);
    }

    public final boolean containsSome(CharSequence charSequence) {
        return !containsNone(charSequence);
    }

    public UnicodeSet addAll(UnicodeSet unicodeSet) {
        checkFrozen();
        add(unicodeSet.list, unicodeSet.len, 0);
        this.strings.addAll(unicodeSet.strings);
        return this;
    }

    public UnicodeSet retainAll(UnicodeSet unicodeSet) {
        checkFrozen();
        retain(unicodeSet.list, unicodeSet.len, 0);
        this.strings.retainAll(unicodeSet.strings);
        return this;
    }

    public UnicodeSet removeAll(UnicodeSet unicodeSet) {
        checkFrozen();
        retain(unicodeSet.list, unicodeSet.len, 2);
        this.strings.removeAll(unicodeSet.strings);
        return this;
    }

    public UnicodeSet complementAll(UnicodeSet unicodeSet) {
        checkFrozen();
        xor(unicodeSet.list, unicodeSet.len, 0);
        SortedSetRelation.doOperation(this.strings, 5, unicodeSet.strings);
        return this;
    }

    public UnicodeSet clear() {
        checkFrozen();
        this.list[0] = 1114112;
        this.len = 1;
        this.pat = null;
        this.strings.clear();
        return this;
    }

    public int getRangeCount() {
        return this.len / 2;
    }

    public int getRangeStart(int i) {
        return this.list[i * 2];
    }

    public int getRangeEnd(int i) {
        return this.list[(i * 2) + 1] - 1;
    }

    public UnicodeSet compact() {
        checkFrozen();
        if (this.len != this.list.length) {
            int[] iArr = new int[this.len];
            System.arraycopy(this.list, 0, iArr, 0, this.len);
            this.list = iArr;
        }
        this.rangeList = null;
        this.buffer = null;
        return this;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        try {
            UnicodeSet unicodeSet = (UnicodeSet) obj;
            if (this.len != unicodeSet.len) {
                return false;
            }
            for (int i = 0; i < this.len; i++) {
                if (this.list[i] != unicodeSet.list[i]) {
                    return false;
                }
            }
            if (!this.strings.equals(unicodeSet.strings)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int hashCode() {
        int i = this.len;
        for (int i2 = 0; i2 < this.len; i2++) {
            i = (i * 1000003) + this.list[i2];
        }
        return i;
    }

    public String toString() {
        return toPattern(true);
    }

    @Deprecated
    public UnicodeSet applyPattern(String str, ParsePosition parsePosition, SymbolTable symbolTable, int i) {
        boolean z = parsePosition == null;
        if (z) {
            parsePosition = new ParsePosition(0);
        }
        StringBuilder sb = new StringBuilder();
        RuleCharacterIterator ruleCharacterIterator = new RuleCharacterIterator(str, symbolTable, parsePosition);
        applyPattern(ruleCharacterIterator, symbolTable, sb, i);
        if (ruleCharacterIterator.inVariable()) {
            syntaxError(ruleCharacterIterator, "Extra chars in variable value");
        }
        this.pat = sb.toString();
        if (z) {
            int index = parsePosition.getIndex();
            if ((i & 1) != 0) {
                index = PatternProps.skipWhiteSpace(str, index);
            }
            if (index != str.length()) {
                throw new IllegalArgumentException("Parse of \"" + str + "\" failed at " + index);
            }
        }
        return this;
    }

    private void applyPattern(RuleCharacterIterator ruleCharacterIterator, SymbolTable symbolTable, Appendable appendable, int i) {
        Object obj;
        char c;
        int next;
        UnicodeSet unicodeSet;
        ?? r20;
        StringBuilder sb;
        int i2;
        boolean z;
        UnicodeMatcher unicodeMatcherLookupMatcher;
        ?? r202;
        ?? r203;
        int i3;
        int i4 = (i & 1) != 0 ? 7 : 3;
        StringBuilder sb2 = new StringBuilder();
        clear();
        char c2 = 2;
        char c3 = 1;
        Object pos = null;
        char c4 = 0;
        String str = null;
        char c5 = 0;
        char c6 = 0;
        int i5 = 0;
        UnicodeSet unicodeSet2 = null;
        StringBuilder sb3 = null;
        boolean z2 = false;
        char c7 = 0;
        while (true) {
            if (c5 != c2 && !ruleCharacterIterator.atEnd()) {
                if (resemblesPropertyPattern(ruleCharacterIterator, i4)) {
                    obj = pos;
                    c = 2;
                    next = 0;
                    unicodeSet = null;
                    r20 = 0;
                } else {
                    Object pos2 = ruleCharacterIterator.getPos(pos);
                    next = ruleCharacterIterator.next(i4);
                    boolean zIsEscaped = ruleCharacterIterator.isEscaped();
                    if (next != 91 || zIsEscaped) {
                        if (symbolTable != null && (unicodeMatcherLookupMatcher = symbolTable.lookupMatcher(next)) != null) {
                            try {
                                obj = pos2;
                                unicodeSet = (UnicodeSet) unicodeMatcherLookupMatcher;
                                c = 3;
                                r20 = zIsEscaped;
                            } catch (ClassCastException e) {
                                syntaxError(ruleCharacterIterator, "Syntax error");
                                obj = pos2;
                                r203 = zIsEscaped;
                                c = 0;
                                r202 = r203;
                                unicodeSet = null;
                                r20 = r202;
                            }
                        }
                        obj = pos2;
                        r203 = zIsEscaped;
                        c = 0;
                        r202 = r203;
                        unicodeSet = null;
                        r20 = r202;
                    } else if (c5 == c3) {
                        ruleCharacterIterator.setPos(pos2);
                        c = c3;
                        obj = pos2;
                        r202 = zIsEscaped;
                        unicodeSet = null;
                        r20 = r202;
                    } else {
                        sb2.append('[');
                        pos = ruleCharacterIterator.getPos(pos2);
                        int next2 = ruleCharacterIterator.next(i4);
                        boolean zIsEscaped2 = ruleCharacterIterator.isEscaped();
                        if (next2 != 94 || zIsEscaped2) {
                            i3 = next2;
                        } else {
                            sb2.append('^');
                            pos = ruleCharacterIterator.getPos(pos);
                            int next3 = ruleCharacterIterator.next(i4);
                            ruleCharacterIterator.isEscaped();
                            i3 = next3;
                            c7 = c3;
                        }
                        if (i3 == 45) {
                            obj = pos;
                            c5 = c3;
                            r203 = c5;
                            next = i3;
                            c = 0;
                            r202 = r203;
                            unicodeSet = null;
                            r20 = r202;
                        } else {
                            ruleCharacterIterator.setPos(pos);
                            c5 = c3;
                            c2 = 2;
                        }
                    }
                }
                if (c != 0) {
                    if (c6 == 1) {
                        if (c4 != 0) {
                            syntaxError(ruleCharacterIterator, "Char expected after operator");
                        }
                        add_unchecked(i5, i5);
                        _appendToPat(sb2, i5, false);
                        c4 = 0;
                    }
                    if (c4 == '-' || c4 == '&') {
                        sb2.append(c4);
                    }
                    if (unicodeSet == null) {
                        unicodeSet = unicodeSet2 == null ? new UnicodeSet() : unicodeSet2;
                        unicodeSet2 = unicodeSet;
                    }
                    switch (c) {
                        case 1:
                            unicodeSet.applyPattern(ruleCharacterIterator, symbolTable, sb2, i);
                            break;
                        case 2:
                            ruleCharacterIterator.skipIgnored(i4);
                            unicodeSet.applyPropertyPattern(ruleCharacterIterator, sb2, symbolTable);
                            break;
                        case 3:
                            unicodeSet._toPattern(sb2, false);
                            break;
                    }
                    if (c5 == 0) {
                        set(unicodeSet);
                        c5 = 2;
                        z2 = true;
                    } else {
                        if (c4 == 0) {
                            addAll(unicodeSet);
                        } else if (c4 == '&') {
                            retainAll(unicodeSet);
                        } else if (c4 == '-') {
                            removeAll(unicodeSet);
                        }
                        pos = obj;
                        c4 = 0;
                        c2 = 2;
                        c3 = 1;
                        c6 = 2;
                    }
                } else {
                    if (c5 == 0) {
                        syntaxError(ruleCharacterIterator, "Missing '['");
                    }
                    if (r20 == 0) {
                        if (next != 36) {
                            if (next != 38) {
                                if (next == 45) {
                                    sb = sb3;
                                    if (c4 == 0) {
                                        if (c6 != 0) {
                                            c4 = (char) next;
                                        } else if (str != null) {
                                            c4 = (char) next;
                                        } else {
                                            add_unchecked(next, next);
                                            int next4 = ruleCharacterIterator.next(i4);
                                            boolean zIsEscaped3 = ruleCharacterIterator.isEscaped();
                                            if (next4 != 93 || zIsEscaped3) {
                                                next = next4;
                                            } else {
                                                sb2.append("-]");
                                                sb3 = sb;
                                            }
                                        }
                                    }
                                    syntaxError(ruleCharacterIterator, "'-' not after char, string, or set");
                                    pos = obj;
                                } else if (next != 123) {
                                    switch (next) {
                                        case 93:
                                            if (c6 == 1) {
                                                add_unchecked(i5, i5);
                                                _appendToPat(sb2, i5, false);
                                            }
                                            if (c4 == '-') {
                                                add_unchecked(c4, c4);
                                                sb2.append(c4);
                                            } else if (c4 == '&') {
                                                syntaxError(ruleCharacterIterator, "Trailing '&'");
                                            }
                                            sb2.append(']');
                                            break;
                                        case 94:
                                            syntaxError(ruleCharacterIterator, "'^' not after '['");
                                            break;
                                    }
                                } else {
                                    if (c4 != 0 && c4 != '-') {
                                        syntaxError(ruleCharacterIterator, "Missing operand after operator");
                                    }
                                    if (c6 == 1) {
                                        add_unchecked(i5, i5);
                                        i2 = 0;
                                        _appendToPat(sb2, i5, false);
                                    } else {
                                        i2 = 0;
                                    }
                                    StringBuilder sb4 = sb3;
                                    if (sb4 == null) {
                                        sb4 = new StringBuilder();
                                    } else {
                                        sb4.setLength(i2);
                                    }
                                    while (true) {
                                        if (ruleCharacterIterator.atEnd()) {
                                            z = false;
                                        } else {
                                            int next5 = ruleCharacterIterator.next(i4);
                                            boolean zIsEscaped4 = ruleCharacterIterator.isEscaped();
                                            if (next5 != 125 || zIsEscaped4) {
                                                appendCodePoint(sb4, next5);
                                            } else {
                                                z = true;
                                            }
                                        }
                                    }
                                    if (sb4.length() < 1 || !z) {
                                        syntaxError(ruleCharacterIterator, "Invalid multicharacter string");
                                    }
                                    String string = sb4.toString();
                                    if (c4 == '-') {
                                        int singleCodePoint = CharSequences.getSingleCodePoint(str == null ? "" : str);
                                        int singleCodePoint2 = CharSequences.getSingleCodePoint(string);
                                        if (singleCodePoint == Integer.MAX_VALUE || singleCodePoint2 == Integer.MAX_VALUE) {
                                            try {
                                                StringRange.expand(str, string, true, this.strings);
                                            } catch (Exception e2) {
                                                syntaxError(ruleCharacterIterator, e2.getMessage());
                                            }
                                        } else {
                                            add(singleCodePoint, singleCodePoint2);
                                        }
                                        c4 = 0;
                                        str = null;
                                    } else {
                                        add(string);
                                        str = string;
                                    }
                                    sb2.append('{');
                                    _appendToPat(sb2, string, false);
                                    sb2.append('}');
                                    sb3 = sb4;
                                    pos = obj;
                                    c2 = 2;
                                    c3 = 1;
                                    c6 = 0;
                                }
                                pos = obj;
                                c2 = 2;
                                c3 = 1;
                                c5 = 2;
                            } else {
                                sb = sb3;
                                if (c6 == 2 && c4 == 0) {
                                    c4 = (char) next;
                                } else {
                                    syntaxError(ruleCharacterIterator, "'&' not after set");
                                    pos = obj;
                                }
                            }
                            sb3 = sb;
                            pos = obj;
                            c2 = 2;
                            c3 = 1;
                        } else {
                            sb = sb3;
                            Object pos3 = ruleCharacterIterator.getPos(obj);
                            int next6 = ruleCharacterIterator.next(i4);
                            boolean z3 = next6 == 93 && !ruleCharacterIterator.isEscaped();
                            if (symbolTable == null && !z3) {
                                ruleCharacterIterator.setPos(pos3);
                                pos = pos3;
                                next = 36;
                            } else if (z3 && c4 == 0) {
                                if (c6 == 1) {
                                    add_unchecked(i5, i5);
                                    _appendToPat(sb2, i5, false);
                                }
                                add_unchecked(DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH);
                                sb2.append(SymbolTable.SYMBOL_REF);
                                sb2.append(']');
                                pos = pos3;
                                sb3 = sb;
                                c2 = 2;
                                c3 = 1;
                                c5 = 2;
                            } else {
                                syntaxError(ruleCharacterIterator, "Unquoted '$'");
                                pos = pos3;
                                next = next6;
                            }
                        }
                        switch (c6) {
                            case 0:
                                if (c4 == '-' && str != null) {
                                    syntaxError(ruleCharacterIterator, "Invalid range");
                                }
                                i5 = next;
                                str = null;
                                c6 = 1;
                                break;
                            case 1:
                                if (c4 != '-') {
                                    add_unchecked(i5, i5);
                                    _appendToPat(sb2, i5, false);
                                    i5 = next;
                                } else {
                                    if (str != null) {
                                        syntaxError(ruleCharacterIterator, "Invalid range");
                                    }
                                    if (i5 >= next) {
                                        syntaxError(ruleCharacterIterator, "Invalid range");
                                    }
                                    add_unchecked(i5, next);
                                    _appendToPat(sb2, i5, false);
                                    sb2.append(c4);
                                    _appendToPat(sb2, next, false);
                                    c4 = 0;
                                    c6 = 0;
                                }
                                break;
                            case 2:
                                if (c4 != 0) {
                                    syntaxError(ruleCharacterIterator, "Set expected after operator");
                                }
                                i5 = next;
                                c6 = 1;
                                break;
                        }
                        sb3 = sb;
                        c2 = 2;
                        c3 = 1;
                    }
                    sb = sb3;
                    pos = obj;
                    switch (c6) {
                    }
                    sb3 = sb;
                    c2 = 2;
                    c3 = 1;
                }
                z2 = true;
            }
        }
        if (c5 != 2) {
            syntaxError(ruleCharacterIterator, "Missing ']'");
        }
        ruleCharacterIterator.skipIgnored(i4);
        if ((i & 2) != 0) {
            closeOver(2);
        }
        if (c7 != 0) {
            complement();
        }
        if (z2) {
            append(appendable, sb2.toString());
        } else {
            appendNewPattern(appendable, false, true);
        }
    }

    private static void syntaxError(RuleCharacterIterator ruleCharacterIterator, String str) {
        throw new IllegalArgumentException("Error: " + str + " at \"" + Utility.escape(ruleCharacterIterator.toString()) + '\"');
    }

    public <T extends Collection<String>> T addAllTo(T t) {
        return (T) addAllTo(this, t);
    }

    public String[] addAllTo(String[] strArr) {
        return (String[]) addAllTo(this, strArr);
    }

    public static String[] toArray(UnicodeSet unicodeSet) {
        return (String[]) addAllTo(unicodeSet, new String[unicodeSet.size()]);
    }

    public UnicodeSet add(Iterable<?> iterable) {
        return addAll(iterable);
    }

    public UnicodeSet addAll(Iterable<?> iterable) {
        checkFrozen();
        Iterator<?> it = iterable.iterator();
        while (it.hasNext()) {
            add(it.next().toString());
        }
        return this;
    }

    private void ensureCapacity(int i) {
        if (i <= this.list.length) {
            return;
        }
        int[] iArr = new int[i + 16];
        System.arraycopy(this.list, 0, iArr, 0, this.len);
        this.list = iArr;
    }

    private void ensureBufferCapacity(int i) {
        if (this.buffer == null || i > this.buffer.length) {
            this.buffer = new int[i + 16];
        }
    }

    private int[] range(int i, int i2) {
        if (this.rangeList == null) {
            this.rangeList = new int[]{i, i2 + 1, 1114112};
        } else {
            this.rangeList[0] = i;
            this.rangeList[1] = i2 + 1;
        }
        return this.rangeList;
    }

    private UnicodeSet xor(int[] iArr, int i, int i2) {
        int i3;
        int i4;
        int i5;
        int i6;
        ensureBufferCapacity(this.len + i);
        int i7 = 0;
        int i8 = this.list[0];
        int i9 = 1;
        if (i2 != 1 && i2 != 2) {
            i5 = iArr[0];
        } else if (iArr[0] == 0) {
            i5 = iArr[1];
        } else {
            i3 = 0;
            i4 = 0;
            while (true) {
                if (i8 >= i7) {
                    i6 = i3 + 1;
                    this.buffer[i3] = i8;
                    i8 = this.list[i9];
                    i9++;
                } else if (i7 < i8) {
                    i6 = i3 + 1;
                    this.buffer[i3] = i7;
                    i7 = iArr[i4];
                    i4++;
                } else {
                    if (i8 == 1114112) {
                        this.buffer[i3] = 1114112;
                        this.len = i3 + 1;
                        int[] iArr2 = this.list;
                        this.list = this.buffer;
                        this.buffer = iArr2;
                        this.pat = null;
                        return this;
                    }
                    i8 = this.list[i9];
                    i9++;
                    i7 = iArr[i4];
                    i4++;
                }
                i3 = i6;
            }
        }
        i4 = 1;
        i7 = i5;
        i3 = 0;
        while (true) {
            if (i8 >= i7) {
            }
            i3 = i6;
        }
    }

    private UnicodeSet add(int[] iArr, int i, int i2) {
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        ensureBufferCapacity(this.len + i);
        int i11 = 0;
        int iMax = this.list[0];
        int iMax2 = iArr[0];
        int i12 = 1;
        int i13 = 1;
        while (true) {
            switch (i2) {
                case 0:
                    if (iMax < iMax2) {
                        if (i11 <= 0 || iMax > this.buffer[i11 - 1]) {
                            this.buffer[i11] = iMax;
                            iMax = this.list[i12];
                            i11++;
                        } else {
                            i11--;
                            iMax = max(this.list[i12], this.buffer[i11]);
                        }
                        i12++;
                        i2 ^= 1;
                    } else if (iMax2 < iMax) {
                        if (i11 <= 0 || iMax2 > this.buffer[i11 - 1]) {
                            this.buffer[i11] = iMax2;
                            iMax2 = iArr[i13];
                            i11++;
                        } else {
                            i11--;
                            iMax2 = max(iArr[i13], this.buffer[i11]);
                        }
                        i13++;
                        i2 ^= 2;
                    } else if (iMax != 1114112) {
                        if (i11 <= 0 || iMax > this.buffer[i11 - 1]) {
                            this.buffer[i11] = iMax;
                            iMax = this.list[i12];
                            i11++;
                        } else {
                            i11--;
                            iMax = max(this.list[i12], this.buffer[i11]);
                        }
                        i12++;
                        i2 ^= 1;
                        i3 = i13 + 1;
                        i4 = iArr[i13];
                        i2 ^= 2;
                        int i14 = i4;
                        i13 = i3;
                        iMax2 = i14;
                    }
                    break;
                case 1:
                    if (iMax < iMax2) {
                        i5 = i11 + 1;
                        this.buffer[i11] = iMax;
                        iMax = this.list[i12];
                        i2 ^= 1;
                        i12++;
                        i11 = i5;
                    } else if (iMax2 < iMax) {
                        i3 = i13 + 1;
                        i4 = iArr[i13];
                        i2 ^= 2;
                        int i142 = i4;
                        i13 = i3;
                        iMax2 = i142;
                    } else if (iMax != 1114112) {
                        i6 = i12 + 1;
                        iMax = this.list[i12];
                        i7 = i2 ^ 1;
                        i8 = i13 + 1;
                        i9 = iArr[i13];
                        i2 = i7 ^ 2;
                        int i15 = i8;
                        i12 = i6;
                        iMax2 = i9;
                        i13 = i15;
                    }
                    break;
                case 2:
                    if (iMax2 < iMax) {
                        i5 = i11 + 1;
                        this.buffer[i11] = iMax2;
                        iMax2 = iArr[i13];
                        i2 ^= 2;
                        i13++;
                        i11 = i5;
                    } else if (iMax < iMax2) {
                        iMax = this.list[i12];
                        i2 ^= 1;
                        i12++;
                    } else if (iMax != 1114112) {
                        i6 = i12 + 1;
                        iMax = this.list[i12];
                        i7 = i2 ^ 1;
                        i8 = i13 + 1;
                        i9 = iArr[i13];
                        i2 = i7 ^ 2;
                        int i152 = i8;
                        i12 = i6;
                        iMax2 = i9;
                        i13 = i152;
                    }
                    break;
                case 3:
                    if (iMax2 <= iMax) {
                        if (iMax != 1114112) {
                            i10 = i11 + 1;
                            this.buffer[i11] = iMax;
                            int i16 = i12 + 1;
                            iMax = this.list[i12];
                            int i17 = iArr[i13];
                            i2 = (i2 ^ 1) ^ 2;
                            i13++;
                            iMax2 = i17;
                            i12 = i16;
                            i11 = i10;
                        }
                    } else if (iMax2 != 1114112) {
                        i10 = i11 + 1;
                        this.buffer[i11] = iMax2;
                        int i162 = i12 + 1;
                        iMax = this.list[i12];
                        int i172 = iArr[i13];
                        i2 = (i2 ^ 1) ^ 2;
                        i13++;
                        iMax2 = i172;
                        i12 = i162;
                        i11 = i10;
                    }
                    break;
            }
        }
        this.buffer[i11] = 1114112;
        this.len = i11 + 1;
        int[] iArr2 = this.list;
        this.list = this.buffer;
        this.buffer = iArr2;
        this.pat = null;
        return this;
    }

    private UnicodeSet retain(int[] iArr, int i, int i2) {
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        ensureBufferCapacity(this.len + i);
        int i13 = this.list[0];
        int i14 = iArr[0];
        int i15 = 1;
        int i16 = 0;
        int i17 = 1;
        while (true) {
            switch (i2) {
                case 0:
                    if (i13 < i14) {
                        i9 = i15 + 1;
                        i13 = this.list[i15];
                        i2 ^= 1;
                        i15 = i9;
                    } else if (i14 < i13) {
                        i3 = i17 + 1;
                        i4 = iArr[i17];
                        i2 ^= 2;
                        int i18 = i3;
                        i14 = i4;
                        i17 = i18;
                    } else if (i13 != 1114112) {
                        i5 = i16 + 1;
                        this.buffer[i16] = i13;
                        i6 = i15 + 1;
                        i13 = this.list[i15];
                        i7 = i17 + 1;
                        i8 = iArr[i17];
                        i2 = (i2 ^ 1) ^ 2;
                        i16 = i5;
                        int i19 = i6;
                        i14 = i8;
                        i17 = i7;
                        i15 = i19;
                    }
                    break;
                case 1:
                    if (i13 < i14) {
                        i9 = i15 + 1;
                        i13 = this.list[i15];
                        i2 ^= 1;
                        i15 = i9;
                    } else if (i14 < i13) {
                        i10 = i16 + 1;
                        this.buffer[i16] = i14;
                        i3 = i17 + 1;
                        i4 = iArr[i17];
                        i2 ^= 2;
                        i16 = i10;
                        int i182 = i3;
                        i14 = i4;
                        i17 = i182;
                    } else if (i13 != 1114112) {
                        i6 = i15 + 1;
                        i13 = this.list[i15];
                        i7 = i17 + 1;
                        i8 = iArr[i17];
                        i2 = (i2 ^ 1) ^ 2;
                        int i192 = i6;
                        i14 = i8;
                        i17 = i7;
                        i15 = i192;
                    }
                    break;
                case 2:
                    if (i14 < i13) {
                        i3 = i17 + 1;
                        i4 = iArr[i17];
                        i2 ^= 2;
                        int i1822 = i3;
                        i14 = i4;
                        i17 = i1822;
                    } else if (i13 < i14) {
                        i11 = i16 + 1;
                        this.buffer[i16] = i13;
                        i12 = i15 + 1;
                        i13 = this.list[i15];
                        i2 ^= 1;
                        i15 = i12;
                        i16 = i11;
                    } else if (i13 != 1114112) {
                        i6 = i15 + 1;
                        i13 = this.list[i15];
                        i7 = i17 + 1;
                        i8 = iArr[i17];
                        i2 = (i2 ^ 1) ^ 2;
                        int i1922 = i6;
                        i14 = i8;
                        i17 = i7;
                        i15 = i1922;
                    }
                    break;
                case 3:
                    if (i13 < i14) {
                        i11 = i16 + 1;
                        this.buffer[i16] = i13;
                        i12 = i15 + 1;
                        i13 = this.list[i15];
                        i2 ^= 1;
                        i15 = i12;
                        i16 = i11;
                    } else if (i14 < i13) {
                        i10 = i16 + 1;
                        this.buffer[i16] = i14;
                        i3 = i17 + 1;
                        i4 = iArr[i17];
                        i2 ^= 2;
                        i16 = i10;
                        int i18222 = i3;
                        i14 = i4;
                        i17 = i18222;
                    } else if (i13 != 1114112) {
                        i5 = i16 + 1;
                        this.buffer[i16] = i13;
                        i6 = i15 + 1;
                        i13 = this.list[i15];
                        i7 = i17 + 1;
                        i8 = iArr[i17];
                        i2 = (i2 ^ 1) ^ 2;
                        i16 = i5;
                        int i19222 = i6;
                        i14 = i8;
                        i17 = i7;
                        i15 = i19222;
                    }
                    break;
            }
        }
        this.buffer[i16] = 1114112;
        this.len = i16 + 1;
        int[] iArr2 = this.list;
        this.list = this.buffer;
        this.buffer = iArr2;
        this.pat = null;
        return this;
    }

    private static final int max(int i, int i2) {
        return i > i2 ? i : i2;
    }

    private static class NumericValueFilter implements Filter {
        double value;

        NumericValueFilter(double d) {
            this.value = d;
        }

        @Override
        public boolean contains(int i) {
            return UCharacter.getUnicodeNumericValue(i) == this.value;
        }
    }

    private static class GeneralCategoryMaskFilter implements Filter {
        int mask;

        GeneralCategoryMaskFilter(int i) {
            this.mask = i;
        }

        @Override
        public boolean contains(int i) {
            return ((1 << UCharacter.getType(i)) & this.mask) != 0;
        }
    }

    private static class IntPropertyFilter implements Filter {
        int prop;
        int value;

        IntPropertyFilter(int i, int i2) {
            this.prop = i;
            this.value = i2;
        }

        @Override
        public boolean contains(int i) {
            return UCharacter.getIntPropertyValue(i, this.prop) == this.value;
        }
    }

    private static class ScriptExtensionsFilter implements Filter {
        int script;

        ScriptExtensionsFilter(int i) {
            this.script = i;
        }

        @Override
        public boolean contains(int i) {
            return UScript.hasScript(i, this.script);
        }
    }

    private static class VersionFilter implements Filter {
        VersionInfo version;

        VersionFilter(VersionInfo versionInfo) {
            this.version = versionInfo;
        }

        @Override
        public boolean contains(int i) {
            VersionInfo age = UCharacter.getAge(i);
            return !Utility.sameObjects(age, UnicodeSet.NO_VERSION) && age.compareTo(this.version) <= 0;
        }
    }

    private static synchronized UnicodeSet getInclusions(int i) {
        if (INCLUSIONS == null) {
            INCLUSIONS = new UnicodeSet[12];
        }
        if (INCLUSIONS[i] == null) {
            UnicodeSet unicodeSet = new UnicodeSet();
            switch (i) {
                case 1:
                    UCharacterProperty.INSTANCE.addPropertyStarts(unicodeSet);
                    break;
                case 2:
                    UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(unicodeSet);
                    break;
                case 3:
                default:
                    throw new IllegalStateException("UnicodeSet.getInclusions(unknown src " + i + ")");
                case 4:
                    UCaseProps.INSTANCE.addPropertyStarts(unicodeSet);
                    break;
                case 5:
                    UBiDiProps.INSTANCE.addPropertyStarts(unicodeSet);
                    break;
                case 6:
                    UCharacterProperty.INSTANCE.addPropertyStarts(unicodeSet);
                    UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(unicodeSet);
                    break;
                case 7:
                    Norm2AllModes.getNFCInstance().impl.addPropertyStarts(unicodeSet);
                    UCaseProps.INSTANCE.addPropertyStarts(unicodeSet);
                    break;
                case 8:
                    Norm2AllModes.getNFCInstance().impl.addPropertyStarts(unicodeSet);
                    break;
                case 9:
                    Norm2AllModes.getNFKCInstance().impl.addPropertyStarts(unicodeSet);
                    break;
                case 10:
                    Norm2AllModes.getNFKC_CFInstance().impl.addPropertyStarts(unicodeSet);
                    break;
                case 11:
                    Norm2AllModes.getNFCInstance().impl.addCanonIterPropertyStarts(unicodeSet);
                    break;
            }
            INCLUSIONS[i] = unicodeSet;
        }
        return INCLUSIONS[i];
    }

    private UnicodeSet applyFilter(Filter filter, int i) {
        clear();
        UnicodeSet inclusions = getInclusions(i);
        int rangeCount = inclusions.getRangeCount();
        int i2 = -1;
        for (int i3 = 0; i3 < rangeCount; i3++) {
            int rangeEnd = inclusions.getRangeEnd(i3);
            for (int rangeStart = inclusions.getRangeStart(i3); rangeStart <= rangeEnd; rangeStart++) {
                if (filter.contains(rangeStart)) {
                    if (i2 < 0) {
                        i2 = rangeStart;
                    }
                } else if (i2 >= 0) {
                    add_unchecked(i2, rangeStart - 1);
                    i2 = -1;
                }
            }
        }
        if (i2 >= 0) {
            add_unchecked(i2, 1114111);
        }
        return this;
    }

    private static String mungeCharName(String str) {
        String strTrimWhiteSpace = PatternProps.trimWhiteSpace(str);
        StringBuilder sb = null;
        for (int i = 0; i < strTrimWhiteSpace.length(); i++) {
            char cCharAt = strTrimWhiteSpace.charAt(i);
            if (PatternProps.isWhiteSpace(cCharAt)) {
                if (sb == null) {
                    sb = new StringBuilder();
                    sb.append((CharSequence) strTrimWhiteSpace, 0, i);
                } else if (sb.charAt(sb.length() - 1) == ' ') {
                }
                cCharAt = ' ';
                if (sb == null) {
                }
            } else if (sb == null) {
                sb.append(cCharAt);
            }
        }
        return sb == null ? strTrimWhiteSpace : sb.toString();
    }

    public UnicodeSet applyIntPropertyValue(int i, int i2) {
        checkFrozen();
        if (i == 8192) {
            applyFilter(new GeneralCategoryMaskFilter(i2), 1);
        } else if (i == 28672) {
            applyFilter(new ScriptExtensionsFilter(i2), 2);
        } else {
            applyFilter(new IntPropertyFilter(i, i2), UCharacterProperty.INSTANCE.getSource(i));
        }
        return this;
    }

    public UnicodeSet applyPropertyAlias(String str, String str2) {
        return applyPropertyAlias(str, str2, null);
    }

    public UnicodeSet applyPropertyAlias(String str, String str2, SymbolTable symbolTable) {
        int propertyEnum;
        checkFrozen();
        if (symbolTable != null && (symbolTable instanceof XSymbolTable) && ((XSymbolTable) symbolTable).applyPropertyAlias(str, str2, this)) {
            return this;
        }
        if (XSYMBOL_TABLE != null && XSYMBOL_TABLE.applyPropertyAlias(str, str2, this)) {
            return this;
        }
        boolean z = false;
        int propertyValueEnum = 1;
        if (str2.length() > 0) {
            propertyEnum = UCharacter.getPropertyEnum(str);
            if (propertyEnum == 4101) {
                propertyEnum = 8192;
            }
            if ((propertyEnum >= 0 && propertyEnum < 64) || ((propertyEnum >= 4096 && propertyEnum < 4118) || (propertyEnum >= 8192 && propertyEnum < 8193))) {
                try {
                    propertyValueEnum = UCharacter.getPropertyValueEnum(propertyEnum, str2);
                } catch (IllegalArgumentException e) {
                    if ((propertyEnum != 4098 && propertyEnum != 4112 && propertyEnum != 4113) || (propertyValueEnum = Integer.parseInt(PatternProps.trimWhiteSpace(str2))) < 0 || propertyValueEnum > 255) {
                        throw e;
                    }
                }
            } else {
                if (propertyEnum == 12288) {
                    applyFilter(new NumericValueFilter(Double.parseDouble(PatternProps.trimWhiteSpace(str2))), 1);
                    return this;
                }
                if (propertyEnum == 16384) {
                    applyFilter(new VersionFilter(VersionInfo.getInstance(mungeCharName(str2))), 2);
                    return this;
                }
                if (propertyEnum == 16389) {
                    int charFromExtendedName = UCharacter.getCharFromExtendedName(mungeCharName(str2));
                    if (charFromExtendedName == -1) {
                        throw new IllegalArgumentException("Invalid character name");
                    }
                    clear();
                    add_unchecked(charFromExtendedName);
                    return this;
                }
                if (propertyEnum == 16395) {
                    throw new IllegalArgumentException("Unicode_1_Name (na1) not supported");
                }
                if (propertyEnum == 28672) {
                    propertyValueEnum = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, str2);
                } else {
                    throw new IllegalArgumentException("Unsupported property");
                }
            }
        } else {
            UPropertyAliases uPropertyAliases = UPropertyAliases.INSTANCE;
            int propertyValueEnum2 = uPropertyAliases.getPropertyValueEnum(8192, str);
            if (propertyValueEnum2 == -1) {
                propertyValueEnum2 = uPropertyAliases.getPropertyValueEnum(UProperty.SCRIPT, str);
                if (propertyValueEnum2 == -1) {
                    int propertyEnum2 = uPropertyAliases.getPropertyEnum(str);
                    int i = propertyEnum2 == -1 ? -1 : propertyEnum2;
                    if (i < 0 || i >= 64) {
                        if (i == -1) {
                            if (UPropertyAliases.compare(ANY_ID, str) == 0) {
                                set(0, 1114111);
                                return this;
                            }
                            if (UPropertyAliases.compare(ASCII_ID, str) == 0) {
                                set(0, 127);
                                return this;
                            }
                            if (UPropertyAliases.compare(ASSIGNED, str) != 0) {
                                throw new IllegalArgumentException("Invalid property alias: " + str + "=" + str2);
                            }
                            z = true;
                            propertyEnum = 8192;
                        } else {
                            throw new IllegalArgumentException("Missing property value");
                        }
                    } else {
                        propertyEnum = i;
                    }
                } else {
                    propertyEnum = 4106;
                }
            } else {
                propertyEnum = 8192;
            }
            propertyValueEnum = propertyValueEnum2;
        }
        applyIntPropertyValue(propertyEnum, propertyValueEnum);
        if (z) {
            complement();
        }
        return this;
    }

    private static boolean resemblesPropertyPattern(String str, int i) {
        if (i + 5 > str.length()) {
            return false;
        }
        return str.regionMatches(i, "[:", 0, 2) || str.regionMatches(true, i, "\\p", 0, 2) || str.regionMatches(i, "\\N", 0, 2);
    }

    private static boolean resemblesPropertyPattern(RuleCharacterIterator ruleCharacterIterator, int i) {
        int i2 = i & (-3);
        Object pos = ruleCharacterIterator.getPos(null);
        int next = ruleCharacterIterator.next(i2);
        boolean z = false;
        if (next == 91 || next == 92) {
            int next2 = ruleCharacterIterator.next(i2 & (-5));
            if (next != 91 ? next2 == 78 || next2 == 112 || next2 == 80 : next2 == 58) {
                z = true;
            }
        }
        ruleCharacterIterator.setPos(pos);
        return z;
    }

    private UnicodeSet applyPropertyPattern(String str, ParsePosition parsePosition, SymbolTable symbolTable) {
        boolean z;
        boolean z2;
        int iSkipWhiteSpace;
        String strSubstring;
        String strSubstring2;
        int index = parsePosition.getIndex();
        if (index + 5 > str.length()) {
            return null;
        }
        boolean z3 = false;
        if (str.regionMatches(index, "[:", 0, 2)) {
            iSkipWhiteSpace = PatternProps.skipWhiteSpace(str, index + 2);
            if (iSkipWhiteSpace >= str.length() || str.charAt(iSkipWhiteSpace) != '^') {
                z2 = false;
                z = false;
                z3 = true;
            } else {
                iSkipWhiteSpace++;
                z2 = false;
                z = true;
                z3 = true;
            }
        } else {
            if (!str.regionMatches(true, index, "\\p", 0, 2) && !str.regionMatches(index, "\\N", 0, 2)) {
                return null;
            }
            char cCharAt = str.charAt(index + 1);
            boolean z4 = cCharAt == 'P';
            boolean z5 = cCharAt == 'N';
            int iSkipWhiteSpace2 = PatternProps.skipWhiteSpace(str, index + 2);
            if (iSkipWhiteSpace2 != str.length()) {
                int i = iSkipWhiteSpace2 + 1;
                if (str.charAt(iSkipWhiteSpace2) == '{') {
                    z = z4;
                    z2 = z5;
                    iSkipWhiteSpace = i;
                }
            }
            return null;
        }
        int iIndexOf = str.indexOf(z3 ? ":]" : "}", iSkipWhiteSpace);
        if (iIndexOf < 0) {
            return null;
        }
        int iIndexOf2 = str.indexOf(61, iSkipWhiteSpace);
        if (iIndexOf2 >= 0 && iIndexOf2 < iIndexOf && !z2) {
            strSubstring = str.substring(iSkipWhiteSpace, iIndexOf2);
            strSubstring2 = str.substring(iIndexOf2 + 1, iIndexOf);
        } else {
            strSubstring = str.substring(iSkipWhiteSpace, iIndexOf);
            strSubstring2 = "";
            if (z2) {
                strSubstring = "na";
                strSubstring2 = strSubstring;
            }
        }
        applyPropertyAlias(strSubstring, strSubstring2, symbolTable);
        if (z) {
            complement();
        }
        parsePosition.setIndex(iIndexOf + (z3 ? 2 : 1));
        return this;
    }

    private void applyPropertyPattern(RuleCharacterIterator ruleCharacterIterator, Appendable appendable, SymbolTable symbolTable) {
        String strLookahead = ruleCharacterIterator.lookahead();
        ParsePosition parsePosition = new ParsePosition(0);
        applyPropertyPattern(strLookahead, parsePosition, symbolTable);
        if (parsePosition.getIndex() == 0) {
            syntaxError(ruleCharacterIterator, "Invalid property pattern");
        }
        ruleCharacterIterator.jumpahead(parsePosition.getIndex());
        append(appendable, strLookahead.substring(0, parsePosition.getIndex()));
    }

    private static final void addCaseMapping(UnicodeSet unicodeSet, int i, StringBuilder sb) {
        if (i >= 0) {
            if (i > 31) {
                unicodeSet.add(i);
            } else {
                unicodeSet.add(sb.toString());
                sb.setLength(0);
            }
        }
    }

    public UnicodeSet closeOver(int i) {
        checkFrozen();
        if ((i & 6) != 0) {
            UCaseProps uCaseProps = UCaseProps.INSTANCE;
            UnicodeSet unicodeSet = new UnicodeSet(this);
            ULocale uLocale = ULocale.ROOT;
            int i2 = i & 2;
            if (i2 != 0) {
                unicodeSet.strings.clear();
            }
            int rangeCount = getRangeCount();
            StringBuilder sb = new StringBuilder();
            for (int i3 = 0; i3 < rangeCount; i3++) {
                int rangeStart = getRangeStart(i3);
                int rangeEnd = getRangeEnd(i3);
                if (i2 != 0) {
                    while (rangeStart <= rangeEnd) {
                        uCaseProps.addCaseClosure(rangeStart, unicodeSet);
                        rangeStart++;
                    }
                } else {
                    while (rangeStart <= rangeEnd) {
                        addCaseMapping(unicodeSet, uCaseProps.toFullLower(rangeStart, null, sb, 1), sb);
                        addCaseMapping(unicodeSet, uCaseProps.toFullTitle(rangeStart, null, sb, 1), sb);
                        addCaseMapping(unicodeSet, uCaseProps.toFullUpper(rangeStart, null, sb, 1), sb);
                        addCaseMapping(unicodeSet, uCaseProps.toFullFolding(rangeStart, sb, 0), sb);
                        rangeStart++;
                    }
                }
            }
            if (!this.strings.isEmpty()) {
                if (i2 != 0) {
                    Iterator<String> it = this.strings.iterator();
                    while (it.hasNext()) {
                        String strFoldCase = UCharacter.foldCase(it.next(), 0);
                        if (!uCaseProps.addStringCaseClosure(strFoldCase, unicodeSet)) {
                            unicodeSet.add(strFoldCase);
                        }
                    }
                } else {
                    BreakIterator wordInstance = BreakIterator.getWordInstance(uLocale);
                    for (String str : this.strings) {
                        unicodeSet.add(UCharacter.toLowerCase(uLocale, str));
                        unicodeSet.add(UCharacter.toTitleCase(uLocale, str, wordInstance));
                        unicodeSet.add(UCharacter.toUpperCase(uLocale, str));
                        unicodeSet.add(UCharacter.foldCase(str, 0));
                    }
                }
            }
            set(unicodeSet);
        }
        return this;
    }

    public static abstract class XSymbolTable implements SymbolTable {
        @Override
        public UnicodeMatcher lookupMatcher(int i) {
            return null;
        }

        public boolean applyPropertyAlias(String str, String str2, UnicodeSet unicodeSet) {
            return false;
        }

        @Override
        public char[] lookup(String str) {
            return null;
        }

        @Override
        public String parseReference(String str, ParsePosition parsePosition, int i) {
            return null;
        }
    }

    @Override
    public boolean isFrozen() {
        return (this.bmpSet == null && this.stringSpan == null) ? false : true;
    }

    @Override
    public UnicodeSet freeze() {
        if (!isFrozen()) {
            this.buffer = null;
            if (this.list.length > this.len + 16) {
                int i = this.len == 0 ? 1 : this.len;
                int[] iArr = this.list;
                this.list = new int[i];
                while (true) {
                    int i2 = i - 1;
                    if (i <= 0) {
                        break;
                    }
                    this.list[i2] = iArr[i2];
                    i = i2;
                }
            }
            if (!this.strings.isEmpty()) {
                this.stringSpan = new UnicodeSetStringSpan(this, new ArrayList(this.strings), 127);
            }
            if (this.stringSpan == null || !this.stringSpan.needsStringSpanUTF16()) {
                this.bmpSet = new BMPSet(this.list, this.len);
            }
        }
        return this;
    }

    public int span(CharSequence charSequence, SpanCondition spanCondition) {
        return span(charSequence, 0, spanCondition);
    }

    public int span(CharSequence charSequence, int i, SpanCondition spanCondition) {
        int length = charSequence.length();
        if (i < 0) {
            i = 0;
        } else if (i >= length) {
            return length;
        }
        if (this.bmpSet != null) {
            return this.bmpSet.span(charSequence, i, spanCondition, null);
        }
        if (this.stringSpan != null) {
            return this.stringSpan.span(charSequence, i, spanCondition);
        }
        if (!this.strings.isEmpty()) {
            UnicodeSetStringSpan unicodeSetStringSpan = new UnicodeSetStringSpan(this, new ArrayList(this.strings), spanCondition == SpanCondition.NOT_CONTAINED ? 33 : 34);
            if (unicodeSetStringSpan.needsStringSpanUTF16()) {
                return unicodeSetStringSpan.span(charSequence, i, spanCondition);
            }
        }
        return spanCodePointsAndCount(charSequence, i, spanCondition, null);
    }

    @Deprecated
    public int spanAndCount(CharSequence charSequence, int i, SpanCondition spanCondition, OutputInt outputInt) {
        if (outputInt == null) {
            throw new IllegalArgumentException("outCount must not be null");
        }
        int length = charSequence.length();
        if (i < 0) {
            i = 0;
        } else if (i >= length) {
            return length;
        }
        if (this.stringSpan != null) {
            return this.stringSpan.spanAndCount(charSequence, i, spanCondition, outputInt);
        }
        if (this.bmpSet != null) {
            return this.bmpSet.span(charSequence, i, spanCondition, outputInt);
        }
        if (!this.strings.isEmpty()) {
            return new UnicodeSetStringSpan(this, new ArrayList(this.strings), (spanCondition == SpanCondition.NOT_CONTAINED ? 33 : 34) | 64).spanAndCount(charSequence, i, spanCondition, outputInt);
        }
        return spanCodePointsAndCount(charSequence, i, spanCondition, outputInt);
    }

    private int spanCodePointsAndCount(CharSequence charSequence, int i, SpanCondition spanCondition, OutputInt outputInt) {
        int i2 = 0;
        boolean z = spanCondition != SpanCondition.NOT_CONTAINED;
        int length = charSequence.length();
        do {
            int iCodePointAt = Character.codePointAt(charSequence, i);
            if (z != contains(iCodePointAt)) {
                break;
            }
            i2++;
            i += Character.charCount(iCodePointAt);
        } while (i < length);
        if (outputInt != null) {
            outputInt.value = i2;
        }
        return i;
    }

    public int spanBack(CharSequence charSequence, SpanCondition spanCondition) {
        return spanBack(charSequence, charSequence.length(), spanCondition);
    }

    public int spanBack(CharSequence charSequence, int i, SpanCondition spanCondition) {
        int i2;
        if (i <= 0) {
            return 0;
        }
        if (i > charSequence.length()) {
            i = charSequence.length();
        }
        if (this.bmpSet != null) {
            return this.bmpSet.spanBack(charSequence, i, spanCondition);
        }
        if (this.stringSpan != null) {
            return this.stringSpan.spanBack(charSequence, i, spanCondition);
        }
        if (!this.strings.isEmpty()) {
            if (spanCondition == SpanCondition.NOT_CONTAINED) {
                i2 = 17;
            } else {
                i2 = 18;
            }
            UnicodeSetStringSpan unicodeSetStringSpan = new UnicodeSetStringSpan(this, new ArrayList(this.strings), i2);
            if (unicodeSetStringSpan.needsStringSpanUTF16()) {
                return unicodeSetStringSpan.spanBack(charSequence, i, spanCondition);
            }
        }
        boolean z = spanCondition != SpanCondition.NOT_CONTAINED;
        do {
            int iCodePointBefore = Character.codePointBefore(charSequence, i);
            if (z != contains(iCodePointBefore)) {
                break;
            }
            i -= Character.charCount(iCodePointBefore);
        } while (i > 0);
        return i;
    }

    @Override
    public UnicodeSet cloneAsThawed() {
        return new UnicodeSet(this);
    }

    private void checkFrozen() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
    }

    public static class EntryRange {
        public int codepoint;
        public int codepointEnd;

        EntryRange() {
        }

        public String toString() {
            StringBuilder sb;
            StringBuilder sb2 = new StringBuilder();
            if (this.codepoint == this.codepointEnd) {
                sb = (StringBuilder) UnicodeSet._appendToPat(sb2, this.codepoint, false);
            } else {
                StringBuilder sb3 = (StringBuilder) UnicodeSet._appendToPat(sb2, this.codepoint, false);
                sb3.append('-');
                sb = (StringBuilder) UnicodeSet._appendToPat(sb3, this.codepointEnd, false);
            }
            return sb.toString();
        }
    }

    public Iterable<EntryRange> ranges() {
        return new EntryRangeIterable();
    }

    private class EntryRangeIterable implements Iterable<EntryRange> {
        private EntryRangeIterable() {
        }

        @Override
        public Iterator<EntryRange> iterator() {
            return new EntryRangeIterator();
        }
    }

    private class EntryRangeIterator implements Iterator<EntryRange> {
        int pos;
        EntryRange result;

        private EntryRangeIterator() {
            this.result = new EntryRange();
        }

        @Override
        public boolean hasNext() {
            return this.pos < UnicodeSet.this.len - 1;
        }

        @Override
        public EntryRange next() {
            if (this.pos < UnicodeSet.this.len - 1) {
                EntryRange entryRange = this.result;
                int[] iArr = UnicodeSet.this.list;
                int i = this.pos;
                this.pos = i + 1;
                entryRange.codepoint = iArr[i];
                EntryRange entryRange2 = this.result;
                int[] iArr2 = UnicodeSet.this.list;
                this.pos = this.pos + 1;
                entryRange2.codepointEnd = iArr2[r2] - 1;
                return this.result;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new UnicodeSetIterator2(this);
    }

    private static class UnicodeSetIterator2 implements Iterator<String> {
        private char[] buffer;
        private int current;
        private int item;
        private int len;
        private int limit;
        private int[] sourceList;
        private TreeSet<String> sourceStrings;
        private Iterator<String> stringIterator;

        UnicodeSetIterator2(UnicodeSet unicodeSet) {
            this.len = unicodeSet.len - 1;
            if (this.len > 0) {
                this.sourceStrings = unicodeSet.strings;
                this.sourceList = unicodeSet.list;
                int[] iArr = this.sourceList;
                int i = this.item;
                this.item = i + 1;
                this.current = iArr[i];
                int[] iArr2 = this.sourceList;
                int i2 = this.item;
                this.item = i2 + 1;
                this.limit = iArr2[i2];
                return;
            }
            this.stringIterator = unicodeSet.strings.iterator();
            this.sourceList = null;
        }

        @Override
        public boolean hasNext() {
            return this.sourceList != null || this.stringIterator.hasNext();
        }

        @Override
        public String next() {
            if (this.sourceList == null) {
                return this.stringIterator.next();
            }
            int i = this.current;
            this.current = i + 1;
            if (this.current >= this.limit) {
                if (this.item >= this.len) {
                    this.stringIterator = this.sourceStrings.iterator();
                    this.sourceList = null;
                } else {
                    int[] iArr = this.sourceList;
                    int i2 = this.item;
                    this.item = i2 + 1;
                    this.current = iArr[i2];
                    int[] iArr2 = this.sourceList;
                    int i3 = this.item;
                    this.item = i3 + 1;
                    this.limit = iArr2[i3];
                }
            }
            if (i <= 65535) {
                return String.valueOf((char) i);
            }
            if (this.buffer == null) {
                this.buffer = new char[2];
            }
            int i4 = i - 65536;
            this.buffer[0] = (char) ((i4 >>> 10) + 55296);
            this.buffer[1] = (char) ((i4 & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE);
            return String.valueOf(this.buffer);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public <T extends CharSequence> boolean containsAll(Iterable<T> iterable) {
        Iterator<T> it = iterable.iterator();
        while (it.hasNext()) {
            if (!contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    public <T extends CharSequence> boolean containsNone(Iterable<T> iterable) {
        Iterator<T> it = iterable.iterator();
        while (it.hasNext()) {
            if (contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    public final <T extends CharSequence> boolean containsSome(Iterable<T> iterable) {
        return !containsNone(iterable);
    }

    public <T extends CharSequence> UnicodeSet addAll(T... tArr) {
        checkFrozen();
        for (T t : tArr) {
            add(t);
        }
        return this;
    }

    public <T extends CharSequence> UnicodeSet removeAll(Iterable<T> iterable) {
        checkFrozen();
        Iterator<T> it = iterable.iterator();
        while (it.hasNext()) {
            remove(it.next());
        }
        return this;
    }

    public <T extends CharSequence> UnicodeSet retainAll(Iterable<T> iterable) {
        checkFrozen();
        UnicodeSet unicodeSet = new UnicodeSet();
        unicodeSet.addAll((Iterable<?>) iterable);
        retainAll(unicodeSet);
        return this;
    }

    @Override
    public int compareTo(UnicodeSet unicodeSet) {
        return compareTo(unicodeSet, ComparisonStyle.SHORTER_FIRST);
    }

    public int compareTo(UnicodeSet unicodeSet, ComparisonStyle comparisonStyle) {
        int iCompare;
        int size;
        if (comparisonStyle != ComparisonStyle.LEXICOGRAPHIC && (size = size() - unicodeSet.size()) != 0) {
            return (size < 0) == (comparisonStyle == ComparisonStyle.SHORTER_FIRST) ? -1 : 1;
        }
        int i = 0;
        while (true) {
            int i2 = this.list[i] - unicodeSet.list[i];
            if (i2 != 0) {
                if (this.list[i] == 1114112) {
                    if (this.strings.isEmpty()) {
                        return 1;
                    }
                    return compare(this.strings.first(), unicodeSet.list[i]);
                }
                if (unicodeSet.list[i] != 1114112) {
                    return (i & 1) == 0 ? i2 : -i2;
                }
                if (!unicodeSet.strings.isEmpty() && (iCompare = compare(unicodeSet.strings.first(), this.list[i])) <= 0) {
                    return iCompare < 0 ? 1 : 0;
                }
                return -1;
            }
            if (this.list[i] != 1114112) {
                i++;
            } else {
                return compare(this.strings, unicodeSet.strings);
            }
        }
    }

    public int compareTo(Iterable<String> iterable) {
        return compare(this, iterable);
    }

    public static int compare(CharSequence charSequence, int i) {
        return CharSequences.compare(charSequence, i);
    }

    public static int compare(int i, CharSequence charSequence) {
        return -CharSequences.compare(charSequence, i);
    }

    public static <T extends Comparable<T>> int compare(Iterable<T> iterable, Iterable<T> iterable2) {
        return compare(iterable.iterator(), iterable2.iterator());
    }

    @Deprecated
    public static <T extends Comparable<T>> int compare(Iterator<T> it, Iterator<T> it2) {
        while (it.hasNext()) {
            if (!it2.hasNext()) {
                return 1;
            }
            int iCompareTo = it.next().compareTo(it2.next());
            if (iCompareTo != 0) {
                return iCompareTo;
            }
        }
        return it2.hasNext() ? -1 : 0;
    }

    public static <T extends Comparable<T>> int compare(Collection<T> collection, Collection<T> collection2, ComparisonStyle comparisonStyle) {
        int size;
        if (comparisonStyle == ComparisonStyle.LEXICOGRAPHIC || (size = collection.size() - collection2.size()) == 0) {
            return compare(collection, collection2);
        }
        return (size < 0) == (comparisonStyle == ComparisonStyle.SHORTER_FIRST) ? -1 : 1;
    }

    public static <T, U extends Collection<T>> U addAllTo(Iterable<T> iterable, U u) {
        Iterator<T> it = iterable.iterator();
        while (it.hasNext()) {
            u.add(it.next());
        }
        return u;
    }

    public static <T> T[] addAllTo(Iterable<T> iterable, T[] tArr) {
        Iterator<T> it = iterable.iterator();
        int i = 0;
        while (it.hasNext()) {
            tArr[i] = it.next();
            i++;
        }
        return tArr;
    }

    public Collection<String> strings() {
        return Collections.unmodifiableSortedSet(this.strings);
    }

    @Deprecated
    public static int getSingleCodePoint(CharSequence charSequence) {
        return CharSequences.getSingleCodePoint(charSequence);
    }

    @Deprecated
    public UnicodeSet addBridges(UnicodeSet unicodeSet) {
        UnicodeSetIterator unicodeSetIterator = new UnicodeSetIterator(new UnicodeSet(this).complement());
        while (unicodeSetIterator.nextRange()) {
            if (unicodeSetIterator.codepoint != 0 && unicodeSetIterator.codepoint != UnicodeSetIterator.IS_STRING && unicodeSetIterator.codepointEnd != 1114111 && unicodeSet.contains(unicodeSetIterator.codepoint, unicodeSetIterator.codepointEnd)) {
                add(unicodeSetIterator.codepoint, unicodeSetIterator.codepointEnd);
            }
        }
        return this;
    }

    @Deprecated
    public int findIn(CharSequence charSequence, int i, boolean z) {
        while (i < charSequence.length()) {
            int iCharAt = UTF16.charAt(charSequence, i);
            if (contains(iCharAt) != z) {
                break;
            }
            i += UTF16.getCharCount(iCharAt);
        }
        return i;
    }

    @Deprecated
    public int findLastIn(CharSequence charSequence, int i, boolean z) {
        int charCount = i - 1;
        while (charCount >= 0) {
            int iCharAt = UTF16.charAt(charSequence, charCount);
            if (contains(iCharAt) != z) {
                break;
            }
            charCount -= UTF16.getCharCount(iCharAt);
        }
        if (charCount < 0) {
            return -1;
        }
        return charCount;
    }

    @Deprecated
    public String stripFrom(CharSequence charSequence, boolean z) {
        StringBuilder sb = new StringBuilder();
        int iFindIn = 0;
        while (iFindIn < charSequence.length()) {
            int iFindIn2 = findIn(charSequence, iFindIn, !z);
            sb.append(charSequence.subSequence(iFindIn, iFindIn2));
            iFindIn = findIn(charSequence, iFindIn2, z);
        }
        return sb.toString();
    }

    @Deprecated
    public static XSymbolTable getDefaultXSymbolTable() {
        return XSYMBOL_TABLE;
    }

    @Deprecated
    public static void setDefaultXSymbolTable(XSymbolTable xSymbolTable) {
        INCLUSIONS = null;
        XSYMBOL_TABLE = xSymbolTable;
    }
}
