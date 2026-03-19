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

    static final boolean f101assertionsDisabled;
    public static final int ADD_CASE_MAPPINGS = 4;
    public static final UnicodeSet ALL_CODE_POINTS;
    private static final String ANY_ID = "ANY";
    private static final String ASCII_ID = "ASCII";
    private static final String ASSIGNED = "Assigned";
    public static final int CASE = 2;
    public static final int CASE_INSENSITIVE = 2;
    public static final UnicodeSet EMPTY;
    private static final int GROW_EXTRA = 16;
    private static final int HIGH = 1114112;
    public static final int IGNORE_SPACE = 1;
    private static UnicodeSet[] INCLUSIONS = null;
    private static final int LAST0_START = 0;
    private static final int LAST1_RANGE = 1;
    private static final int LAST2_SET = 2;
    private static final int LOW = 0;
    public static final int MAX_VALUE = 1114111;
    public static final int MIN_VALUE = 0;
    private static final int MODE0_NONE = 0;
    private static final int MODE1_INBRACKET = 1;
    private static final int MODE2_OUTBRACKET = 2;
    private static final VersionInfo NO_VERSION;
    private static final int SETMODE0_NONE = 0;
    private static final int SETMODE1_UNICODESET = 1;
    private static final int SETMODE2_PROPERTYPAT = 2;
    private static final int SETMODE3_PREPARSED = 3;
    private static final int START_EXTRA = 16;
    private static XSymbolTable XSYMBOL_TABLE;
    private volatile BMPSet bmpSet;
    private int[] buffer;
    private int len;
    private int[] list;
    private String pat;
    private int[] rangeList;
    private volatile UnicodeSetStringSpan stringSpan;
    TreeSet<String> strings;

    private interface Filter {
        boolean contains(int i);
    }

    static {
        f101assertionsDisabled = !UnicodeSet.class.desiredAssertionStatus();
        EMPTY = new UnicodeSet().freeze();
        ALL_CODE_POINTS = new UnicodeSet(0, 1114111).freeze();
        XSYMBOL_TABLE = null;
        INCLUSIONS = null;
        NO_VERSION = VersionInfo.getInstance(0, 0, 0, 0);
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

    public UnicodeSet(UnicodeSet other) {
        this.strings = new TreeSet<>();
        this.pat = null;
        set(other);
    }

    public UnicodeSet(int start, int end) {
        this();
        complement(start, end);
    }

    public UnicodeSet(int... pairs) {
        this.strings = new TreeSet<>();
        this.pat = null;
        if ((pairs.length & 1) != 0) {
            throw new IllegalArgumentException("Must have even number of integers");
        }
        this.list = new int[pairs.length + 1];
        this.len = this.list.length;
        int last = -1;
        int i = 0;
        while (i < pairs.length) {
            int start = pairs[i];
            if (last >= start) {
                throw new IllegalArgumentException("Must be monotonically increasing.");
            }
            int i2 = i + 1;
            this.list[i] = start;
            int end = pairs[i2] + 1;
            if (start >= end) {
                throw new IllegalArgumentException("Must be monotonically increasing.");
            }
            i = i2 + 1;
            last = end;
            this.list[i2] = end;
        }
        this.list[i] = 1114112;
    }

    public UnicodeSet(String pattern) {
        this();
        applyPattern(pattern, (ParsePosition) null, (SymbolTable) null, 1);
    }

    public UnicodeSet(String pattern, boolean ignoreWhitespace) {
        this();
        applyPattern(pattern, (ParsePosition) null, (SymbolTable) null, ignoreWhitespace ? 1 : 0);
    }

    public UnicodeSet(String pattern, int options) {
        this();
        applyPattern(pattern, (ParsePosition) null, (SymbolTable) null, options);
    }

    public UnicodeSet(String pattern, ParsePosition pos, SymbolTable symbols) {
        this();
        applyPattern(pattern, pos, symbols, 1);
    }

    public UnicodeSet(String pattern, ParsePosition pos, SymbolTable symbols, int options) {
        this();
        applyPattern(pattern, pos, symbols, options);
    }

    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        UnicodeSet result = new UnicodeSet(this);
        result.bmpSet = this.bmpSet;
        result.stringSpan = this.stringSpan;
        return result;
    }

    public UnicodeSet set(int start, int end) {
        checkFrozen();
        clear();
        complement(start, end);
        return this;
    }

    public UnicodeSet set(UnicodeSet other) {
        checkFrozen();
        this.list = (int[]) other.list.clone();
        this.len = other.len;
        this.pat = other.pat;
        this.strings = new TreeSet<>((SortedSet) other.strings);
        return this;
    }

    public final UnicodeSet applyPattern(String pattern) {
        checkFrozen();
        return applyPattern(pattern, (ParsePosition) null, (SymbolTable) null, 1);
    }

    public UnicodeSet applyPattern(String pattern, boolean ignoreWhitespace) {
        checkFrozen();
        return applyPattern(pattern, (ParsePosition) null, (SymbolTable) null, ignoreWhitespace ? 1 : 0);
    }

    public UnicodeSet applyPattern(String pattern, int options) {
        checkFrozen();
        return applyPattern(pattern, (ParsePosition) null, (SymbolTable) null, options);
    }

    public static boolean resemblesPattern(String pattern, int pos) {
        if (pos + 1 < pattern.length() && pattern.charAt(pos) == '[') {
            return true;
        }
        return resemblesPropertyPattern(pattern, pos);
    }

    private static void appendCodePoint(Appendable app, int c) {
        boolean z = false;
        if (!f101assertionsDisabled) {
            if (c >= 0 && c <= 1114111) {
                z = true;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
        try {
            if (c <= 65535) {
                app.append((char) c);
            } else {
                app.append(UTF16.getLeadSurrogate(c)).append(UTF16.getTrailSurrogate(c));
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static void append(Appendable app, CharSequence s) {
        try {
            app.append(s);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static <T extends Appendable> T _appendToPat(T buf, String s, boolean escapeUnprintable) {
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            _appendToPat(buf, cp, escapeUnprintable);
            i += Character.charCount(cp);
        }
        return buf;
    }

    private static <T extends Appendable> T _appendToPat(T buf, int c, boolean escapeUnprintable) {
        if (escapeUnprintable) {
            try {
                if (Utility.isUnprintable(c) && Utility.escapeUnprintable(buf, c)) {
                    return buf;
                }
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
        switch (c) {
            case 36:
            case 38:
            case 45:
            case 58:
            case 91:
            case 92:
            case 93:
            case 94:
            case 123:
            case 125:
                buf.append(PatternTokenizer.BACK_SLASH);
                break;
            default:
                if (PatternProps.isWhiteSpace(c)) {
                    buf.append(PatternTokenizer.BACK_SLASH);
                }
                break;
        }
        appendCodePoint(buf, c);
        return buf;
    }

    @Override
    public String toPattern(boolean escapeUnprintable) {
        if (this.pat != null && !escapeUnprintable) {
            return this.pat;
        }
        StringBuilder result = new StringBuilder();
        return ((StringBuilder) _toPattern(result, escapeUnprintable)).toString();
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
            boolean z2 = false;
            int iCharCount = 0;
            while (iCharCount < this.pat.length()) {
                int iCodePointAt = this.pat.codePointAt(iCharCount);
                iCharCount += Character.charCount(iCodePointAt);
                if (Utility.isUnprintable(iCodePointAt)) {
                    Utility.escapeUnprintable(t, iCodePointAt);
                    z2 = false;
                } else if (!z2 && iCodePointAt == 92) {
                    z2 = true;
                } else {
                    if (z2) {
                        t.append(PatternTokenizer.BACK_SLASH);
                    }
                    appendCodePoint(t, iCodePointAt);
                    z2 = false;
                }
            }
            if (z2) {
                t.append(PatternTokenizer.BACK_SLASH);
            }
            return t;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public StringBuffer _generatePattern(StringBuffer result, boolean escapeUnprintable) {
        return _generatePattern(result, escapeUnprintable, true);
    }

    public StringBuffer _generatePattern(StringBuffer result, boolean escapeUnprintable, boolean includeStrings) {
        return (StringBuffer) appendNewPattern(result, escapeUnprintable, includeStrings);
    }

    private <T extends Appendable> T appendNewPattern(T result, boolean escapeUnprintable, boolean includeStrings) {
        try {
            result.append('[');
            int count = getRangeCount();
            if (count > 1 && getRangeStart(0) == 0 && getRangeEnd(count - 1) == 1114111) {
                result.append('^');
                for (int i = 1; i < count; i++) {
                    int start = getRangeEnd(i - 1) + 1;
                    int end = getRangeStart(i) - 1;
                    _appendToPat(result, start, escapeUnprintable);
                    if (start != end) {
                        if (start + 1 != end) {
                            result.append('-');
                        }
                        _appendToPat(result, end, escapeUnprintable);
                    }
                }
            } else {
                for (int i2 = 0; i2 < count; i2++) {
                    int start2 = getRangeStart(i2);
                    int end2 = getRangeEnd(i2);
                    _appendToPat(result, start2, escapeUnprintable);
                    if (start2 != end2) {
                        if (start2 + 1 != end2) {
                            result.append('-');
                        }
                        _appendToPat(result, end2, escapeUnprintable);
                    }
                }
            }
            if (includeStrings && this.strings.size() > 0) {
                for (String s : this.strings) {
                    result.append('{');
                    _appendToPat(result, s, escapeUnprintable);
                    result.append('}');
                }
            }
            result.append(']');
            return result;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public int size() {
        int n = 0;
        int count = getRangeCount();
        for (int i = 0; i < count; i++) {
            n += (getRangeEnd(i) - getRangeStart(i)) + 1;
        }
        return this.strings.size() + n;
    }

    public boolean isEmpty() {
        return this.len == 1 && this.strings.size() == 0;
    }

    @Override
    public boolean matchesIndexValue(int v) {
        for (int i = 0; i < getRangeCount(); i++) {
            int low = getRangeStart(i);
            int high = getRangeEnd(i);
            if ((low & (-256)) == (high & (-256))) {
                if ((low & 255) <= v && v <= (high & 255)) {
                    return true;
                }
            } else if ((low & 255) <= v || v <= (high & 255)) {
                return true;
            }
        }
        if (this.strings.size() != 0) {
            for (String s : this.strings) {
                int c = UTF16.charAt(s, 0);
                if ((c & 255) == v) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int matches(Replaceable text, int[] offset, int limit, boolean incremental) {
        if (offset[0] == limit) {
            if (contains(65535)) {
                return incremental ? 1 : 2;
            }
            return 0;
        }
        if (this.strings.size() != 0) {
            boolean forward = offset[0] < limit;
            char firstChar = text.charAt(offset[0]);
            int highWaterLength = 0;
            for (String trial : this.strings) {
                char c = trial.charAt(forward ? 0 : trial.length() - 1);
                if (forward && c > firstChar) {
                    break;
                }
                if (c == firstChar) {
                    int length = matchRest(text, offset[0], limit, trial);
                    if (incremental) {
                        int maxLen = forward ? limit - offset[0] : offset[0] - limit;
                        if (length == maxLen) {
                            return 1;
                        }
                    }
                    if (length == trial.length()) {
                        if (length > highWaterLength) {
                            highWaterLength = length;
                        }
                        if (forward && length < highWaterLength) {
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            }
            if (highWaterLength != 0) {
                int i = offset[0];
                if (!forward) {
                    highWaterLength = -highWaterLength;
                }
                offset[0] = i + highWaterLength;
                return 2;
            }
        }
        return super.matches(text, offset, limit, incremental);
    }

    private static int matchRest(Replaceable text, int start, int limit, String s) {
        int maxLen;
        int slen = s.length();
        if (start < limit) {
            maxLen = limit - start;
            if (maxLen > slen) {
                maxLen = slen;
            }
            for (int i = 1; i < maxLen; i++) {
                if (text.charAt(start + i) != s.charAt(i)) {
                    return 0;
                }
            }
        } else {
            maxLen = start - limit;
            if (maxLen > slen) {
                maxLen = slen;
            }
            int slen2 = slen - 1;
            for (int i2 = 1; i2 < maxLen; i2++) {
                if (text.charAt(start - i2) != s.charAt(slen2 - i2)) {
                    return 0;
                }
            }
        }
        return maxLen;
    }

    @Deprecated
    public int matchesAt(CharSequence text, int offset) {
        int lastLen = -1;
        if (this.strings.size() != 0) {
            char firstChar = text.charAt(offset);
            String trial = null;
            Iterator<String> it = this.strings.iterator();
            while (true) {
                if (!it.hasNext()) {
                    while (true) {
                        int tempLen = matchesAt(text, offset, trial);
                        if (lastLen > tempLen) {
                            break;
                        }
                        lastLen = tempLen;
                        if (!it.hasNext()) {
                            break;
                        }
                        trial = it.next();
                    }
                } else {
                    trial = it.next();
                    char firstStringChar = trial.charAt(0);
                    if (firstStringChar >= firstChar && firstStringChar > firstChar) {
                        break;
                    }
                }
            }
        }
        if (lastLen < 2) {
            int cp = UTF16.charAt(text, offset);
            if (contains(cp)) {
                lastLen = UTF16.getCharCount(cp);
            }
        }
        return offset + lastLen;
    }

    private static int matchesAt(CharSequence text, int offsetInText, CharSequence substring) {
        int len = substring.length();
        int textLength = text.length();
        if (textLength + offsetInText > len) {
            return -1;
        }
        int i = 0;
        int j = offsetInText;
        while (i < len) {
            char pc = substring.charAt(i);
            char tc = text.charAt(j);
            if (pc != tc) {
                return -1;
            }
            i++;
            j++;
        }
        return i;
    }

    @Override
    public void addMatchSetTo(UnicodeSet toUnionTo) {
        toUnionTo.addAll(this);
    }

    public int indexOf(int c) {
        if (c < 0 || c > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(c, 6));
        }
        int i = 0;
        int n = 0;
        while (true) {
            int i2 = i + 1;
            int start = this.list[i];
            if (c < start) {
                return -1;
            }
            i = i2 + 1;
            int limit = this.list[i2];
            if (c < limit) {
                return (n + c) - start;
            }
            n += limit - start;
        }
    }

    public int charAt(int index) {
        if (index >= 0) {
            int len2 = this.len & (-2);
            int i = 0;
            while (i < len2) {
                int i2 = i + 1;
                int start = this.list[i];
                i = i2 + 1;
                int count = this.list[i2] - start;
                if (index < count) {
                    return start + index;
                }
                index -= count;
            }
            return -1;
        }
        return -1;
    }

    public UnicodeSet add(int start, int end) {
        checkFrozen();
        return add_unchecked(start, end);
    }

    public UnicodeSet addAll(int start, int end) {
        checkFrozen();
        return add_unchecked(start, end);
    }

    private UnicodeSet add_unchecked(int start, int end) {
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start < end) {
            add(range(start, end), 2, 0);
        } else if (start == end) {
            add(start);
        }
        return this;
    }

    public final UnicodeSet add(int c) {
        checkFrozen();
        return add_unchecked(c);
    }

    private final UnicodeSet add_unchecked(int c) {
        if (c < 0 || c > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(c, 6));
        }
        int i = findCodePoint(c);
        if ((i & 1) != 0) {
            return this;
        }
        if (c == this.list[i] - 1) {
            this.list[i] = c;
            if (c == 1114111) {
                ensureCapacity(this.len + 1);
                int[] iArr = this.list;
                int i2 = this.len;
                this.len = i2 + 1;
                iArr[i2] = 1114112;
            }
            if (i > 0 && c == this.list[i - 1]) {
                System.arraycopy(this.list, i + 1, this.list, i - 1, (this.len - i) - 1);
                this.len -= 2;
            }
        } else if (i > 0 && c == this.list[i - 1]) {
            int[] iArr2 = this.list;
            int i3 = i - 1;
            iArr2[i3] = iArr2[i3] + 1;
        } else {
            if (this.len + 2 > this.list.length) {
                int[] temp = new int[this.len + 2 + 16];
                if (i != 0) {
                    System.arraycopy(this.list, 0, temp, 0, i);
                }
                System.arraycopy(this.list, i, temp, i + 2, this.len - i);
                this.list = temp;
            } else {
                System.arraycopy(this.list, i, this.list, i + 2, this.len - i);
            }
            this.list[i] = c;
            this.list[i + 1] = c + 1;
            this.len += 2;
        }
        this.pat = null;
        return this;
    }

    public final UnicodeSet add(CharSequence s) {
        checkFrozen();
        int cp = getSingleCP(s);
        if (cp < 0) {
            this.strings.add(s.toString());
            this.pat = null;
        } else {
            add_unchecked(cp, cp);
        }
        return this;
    }

    private static int getSingleCP(CharSequence s) {
        if (s.length() < 1) {
            throw new IllegalArgumentException("Can't use zero-length strings in UnicodeSet");
        }
        if (s.length() > 2) {
            return -1;
        }
        if (s.length() == 1) {
            return s.charAt(0);
        }
        int cp = UTF16.charAt(s, 0);
        if (cp > 65535) {
            return cp;
        }
        return -1;
    }

    public final UnicodeSet addAll(CharSequence s) {
        checkFrozen();
        int i = 0;
        while (i < s.length()) {
            int cp = UTF16.charAt(s, i);
            add_unchecked(cp, cp);
            i += UTF16.getCharCount(cp);
        }
        return this;
    }

    public final UnicodeSet retainAll(CharSequence s) {
        return retainAll(fromAll(s));
    }

    public final UnicodeSet complementAll(CharSequence s) {
        return complementAll(fromAll(s));
    }

    public final UnicodeSet removeAll(CharSequence s) {
        return removeAll(fromAll(s));
    }

    public final UnicodeSet removeAllStrings() {
        checkFrozen();
        if (this.strings.size() != 0) {
            this.strings.clear();
            this.pat = null;
        }
        return this;
    }

    public static UnicodeSet from(CharSequence s) {
        return new UnicodeSet().add(s);
    }

    public static UnicodeSet fromAll(CharSequence s) {
        return new UnicodeSet().addAll(s);
    }

    public UnicodeSet retain(int start, int end) {
        checkFrozen();
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start <= end) {
            retain(range(start, end), 2, 0);
        } else {
            clear();
        }
        return this;
    }

    public final UnicodeSet retain(int c) {
        return retain(c, c);
    }

    public final UnicodeSet retain(CharSequence cs) {
        int cp = getSingleCP(cs);
        if (cp < 0) {
            String s = cs.toString();
            boolean isIn = this.strings.contains(s);
            if (isIn && size() == 1) {
                return this;
            }
            clear();
            this.strings.add(s);
            this.pat = null;
        } else {
            retain(cp, cp);
        }
        return this;
    }

    public UnicodeSet remove(int start, int end) {
        checkFrozen();
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start <= end) {
            retain(range(start, end), 2, 2);
        }
        return this;
    }

    public final UnicodeSet remove(int c) {
        return remove(c, c);
    }

    public final UnicodeSet remove(CharSequence s) {
        int cp = getSingleCP(s);
        if (cp < 0) {
            this.strings.remove(s.toString());
            this.pat = null;
        } else {
            remove(cp, cp);
        }
        return this;
    }

    public UnicodeSet complement(int start, int end) {
        checkFrozen();
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start <= end) {
            xor(range(start, end), 2, 0);
        }
        this.pat = null;
        return this;
    }

    public final UnicodeSet complement(int c) {
        return complement(c, c);
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

    public final UnicodeSet complement(CharSequence s) {
        checkFrozen();
        int cp = getSingleCP(s);
        if (cp < 0) {
            String s2 = s.toString();
            if (this.strings.contains(s2)) {
                this.strings.remove(s2);
            } else {
                this.strings.add(s2);
            }
            this.pat = null;
        } else {
            complement(cp, cp);
        }
        return this;
    }

    @Override
    public boolean contains(int c) {
        if (c < 0 || c > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(c, 6));
        }
        if (this.bmpSet != null) {
            return this.bmpSet.contains(c);
        }
        if (this.stringSpan != null) {
            return this.stringSpan.contains(c);
        }
        int i = findCodePoint(c);
        return (i & 1) != 0;
    }

    private final int findCodePoint(int c) {
        if (c < this.list[0]) {
            return 0;
        }
        if (this.len >= 2 && c >= this.list[this.len - 2]) {
            return this.len - 1;
        }
        int lo = 0;
        int hi = this.len - 1;
        while (true) {
            int i = (lo + hi) >>> 1;
            if (i == lo) {
                return hi;
            }
            if (c < this.list[i]) {
                hi = i;
            } else {
                lo = i;
            }
        }
    }

    public boolean contains(int start, int end) {
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        int i = findCodePoint(start);
        return (i & 1) != 0 && end < this.list[i];
    }

    public final boolean contains(CharSequence s) {
        int cp = getSingleCP(s);
        if (cp < 0) {
            return this.strings.contains(s.toString());
        }
        return contains(cp);
    }

    public boolean containsAll(UnicodeSet b) {
        int aPtr;
        int bPtr;
        int[] listB = b.list;
        boolean needA = true;
        boolean needB = true;
        int aLen = this.len - 1;
        int bLen = b.len - 1;
        int startA = 0;
        int startB = 0;
        int limitA = 0;
        int limitB = 0;
        int bPtr2 = 0;
        int aPtr2 = 0;
        while (true) {
            if (!needA) {
                aPtr = aPtr2;
            } else if (aPtr2 >= aLen) {
                if (!needB || bPtr2 < bLen) {
                    return false;
                }
            } else {
                int aPtr3 = aPtr2 + 1;
                startA = this.list[aPtr2];
                limitA = this.list[aPtr3];
                aPtr = aPtr3 + 1;
            }
            if (!needB) {
                bPtr = bPtr2;
            } else {
                if (bPtr2 >= bLen) {
                    break;
                }
                int bPtr3 = bPtr2 + 1;
                startB = listB[bPtr2];
                limitB = listB[bPtr3];
                bPtr = bPtr3 + 1;
            }
            if (startB >= limitA) {
                needA = true;
                needB = false;
                bPtr2 = bPtr;
                aPtr2 = aPtr;
            } else if (startB >= startA && limitB <= limitA) {
                needA = false;
                needB = true;
                bPtr2 = bPtr;
                aPtr2 = aPtr;
            } else {
                return false;
            }
        }
    }

    public boolean containsAll(String s) {
        int i = 0;
        while (i < s.length()) {
            int cp = UTF16.charAt(s, i);
            if (contains(cp)) {
                i += UTF16.getCharCount(cp);
            } else {
                if (this.strings.size() == 0) {
                    return false;
                }
                return containsAll(s, 0);
            }
        }
        return true;
    }

    private boolean containsAll(String s, int i) {
        if (i >= s.length()) {
            return true;
        }
        int cp = UTF16.charAt(s, i);
        if (contains(cp) && containsAll(s, UTF16.getCharCount(cp) + i)) {
            return true;
        }
        for (String setStr : this.strings) {
            if (s.startsWith(setStr, i) && containsAll(s, setStr.length() + i)) {
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
        StringBuilder result = new StringBuilder("(?:");
        appendNewPattern(result, true, false);
        for (String s : this.strings) {
            result.append('|');
            _appendToPat(result, s, true);
        }
        return result.append(")").toString();
    }

    public boolean containsNone(int start, int end) {
        if (start < 0 || start > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < 0 || end > 1114111) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        int i = -1;
        do {
            i++;
        } while (start >= this.list[i]);
        return (i & 1) == 0 && end < this.list[i];
    }

    public boolean containsNone(UnicodeSet b) {
        int aPtr;
        int bPtr;
        int[] listB = b.list;
        boolean needA = true;
        boolean needB = true;
        int aLen = this.len - 1;
        int bLen = b.len - 1;
        int startA = 0;
        int startB = 0;
        int limitA = 0;
        int limitB = 0;
        int bPtr2 = 0;
        int aPtr2 = 0;
        while (true) {
            if (!needA) {
                aPtr = aPtr2;
            } else {
                if (aPtr2 >= aLen) {
                    break;
                }
                int aPtr3 = aPtr2 + 1;
                startA = this.list[aPtr2];
                limitA = this.list[aPtr3];
                aPtr = aPtr3 + 1;
            }
            if (!needB) {
                bPtr = bPtr2;
            } else {
                if (bPtr2 >= bLen) {
                    break;
                }
                int bPtr3 = bPtr2 + 1;
                startB = listB[bPtr2];
                limitB = listB[bPtr3];
                bPtr = bPtr3 + 1;
            }
            if (startB >= limitA) {
                needA = true;
                needB = false;
                bPtr2 = bPtr;
                aPtr2 = aPtr;
            } else if (startA >= limitB) {
                needA = false;
                needB = true;
                bPtr2 = bPtr;
                aPtr2 = aPtr;
            } else {
                return false;
            }
        }
    }

    public boolean containsNone(CharSequence s) {
        return span(s, SpanCondition.NOT_CONTAINED) == s.length();
    }

    public final boolean containsSome(int start, int end) {
        return !containsNone(start, end);
    }

    public final boolean containsSome(UnicodeSet s) {
        return !containsNone(s);
    }

    public final boolean containsSome(CharSequence s) {
        return !containsNone(s);
    }

    public UnicodeSet addAll(UnicodeSet c) {
        checkFrozen();
        add(c.list, c.len, 0);
        this.strings.addAll(c.strings);
        return this;
    }

    public UnicodeSet retainAll(UnicodeSet c) {
        checkFrozen();
        retain(c.list, c.len, 0);
        this.strings.retainAll(c.strings);
        return this;
    }

    public UnicodeSet removeAll(UnicodeSet c) {
        checkFrozen();
        retain(c.list, c.len, 2);
        this.strings.removeAll(c.strings);
        return this;
    }

    public UnicodeSet complementAll(UnicodeSet c) {
        checkFrozen();
        xor(c.list, c.len, 0);
        SortedSetRelation.doOperation(this.strings, 5, c.strings);
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

    public int getRangeStart(int index) {
        return this.list[index * 2];
    }

    public int getRangeEnd(int index) {
        return this.list[(index * 2) + 1] - 1;
    }

    public UnicodeSet compact() {
        checkFrozen();
        if (this.len != this.list.length) {
            int[] temp = new int[this.len];
            System.arraycopy(this.list, 0, temp, 0, this.len);
            this.list = temp;
        }
        this.rangeList = null;
        this.buffer = null;
        return this;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        try {
            UnicodeSet that = (UnicodeSet) o;
            if (this.len != that.len) {
                return false;
            }
            for (int i = 0; i < this.len; i++) {
                if (this.list[i] != that.list[i]) {
                    return false;
                }
            }
            return this.strings.equals(that.strings);
        } catch (Exception e) {
            return false;
        }
    }

    public int hashCode() {
        int result = this.len;
        for (int i = 0; i < this.len; i++) {
            result = (result * 1000003) + this.list[i];
        }
        return result;
    }

    public String toString() {
        return toPattern(true);
    }

    @Deprecated
    public UnicodeSet applyPattern(String pattern, ParsePosition pos, SymbolTable symbols, int options) {
        boolean parsePositionWasNull = pos == null;
        if (parsePositionWasNull) {
            pos = new ParsePosition(0);
        }
        StringBuilder rebuiltPat = new StringBuilder();
        RuleCharacterIterator chars = new RuleCharacterIterator(pattern, symbols, pos);
        applyPattern(chars, symbols, rebuiltPat, options);
        if (chars.inVariable()) {
            syntaxError(chars, "Extra chars in variable value");
        }
        this.pat = rebuiltPat.toString();
        if (parsePositionWasNull) {
            int i = pos.getIndex();
            if ((options & 1) != 0) {
                i = PatternProps.skipWhiteSpace(pattern, i);
            }
            if (i != pattern.length()) {
                throw new IllegalArgumentException("Parse of \"" + pattern + "\" failed at " + i);
            }
        }
        return this;
    }

    private void applyPattern(RuleCharacterIterator chars, SymbolTable symbols, Appendable rebuiltPat, int options) {
        UnicodeMatcher m;
        int opts = 3;
        if ((options & 1) != 0) {
            opts = 7;
        }
        StringBuilder patBuf = new StringBuilder();
        StringBuilder buf = null;
        boolean usePat = false;
        UnicodeSet scratch = null;
        Object backup = null;
        int lastItem = 0;
        int lastChar = 0;
        int mode = 0;
        char op = 0;
        boolean invert = false;
        clear();
        String lastString = null;
        while (true) {
            if (mode != 2 && !chars.atEnd()) {
                int c = 0;
                boolean literal = false;
                UnicodeSet nested = null;
                int setMode = 0;
                if (resemblesPropertyPattern(chars, opts)) {
                    setMode = 2;
                } else {
                    backup = chars.getPos(backup);
                    c = chars.next(opts);
                    literal = chars.isEscaped();
                    if (c == 91 && !literal) {
                        if (mode == 1) {
                            chars.setPos(backup);
                            setMode = 1;
                        } else {
                            mode = 1;
                            patBuf.append('[');
                            backup = chars.getPos(backup);
                            c = chars.next(opts);
                            boolean literal2 = chars.isEscaped();
                            if (c == 94 && !literal2) {
                                invert = true;
                                patBuf.append('^');
                                backup = chars.getPos(backup);
                                c = chars.next(opts);
                                chars.isEscaped();
                            }
                            if (c == 45) {
                                literal = true;
                            } else {
                                chars.setPos(backup);
                            }
                        }
                    } else if (symbols != null && (m = symbols.lookupMatcher(c)) != null) {
                        try {
                            nested = (UnicodeSet) m;
                            setMode = 3;
                        } catch (ClassCastException e) {
                            syntaxError(chars, "Syntax error");
                        }
                    }
                }
                if (setMode != 0) {
                    if (lastItem == 1) {
                        if (op != 0) {
                            syntaxError(chars, "Char expected after operator");
                        }
                        add_unchecked(lastChar, lastChar);
                        _appendToPat(patBuf, lastChar, false);
                        op = 0;
                    }
                    if (op == '-' || op == '&') {
                        patBuf.append(op);
                    }
                    if (nested == null) {
                        if (scratch == null) {
                            scratch = new UnicodeSet();
                        }
                        nested = scratch;
                    }
                    switch (setMode) {
                        case 1:
                            nested.applyPattern(chars, symbols, patBuf, options);
                            break;
                        case 2:
                            chars.skipIgnored(opts);
                            nested.applyPropertyPattern(chars, patBuf, symbols);
                            break;
                        case 3:
                            nested._toPattern(patBuf, false);
                            break;
                    }
                    usePat = true;
                    if (mode == 0) {
                        set(nested);
                        mode = 2;
                    } else {
                        switch (op) {
                            case 0:
                                addAll(nested);
                                break;
                            case '&':
                                retainAll(nested);
                                break;
                            case '-':
                                removeAll(nested);
                                break;
                        }
                        op = 0;
                        lastItem = 2;
                    }
                } else {
                    if (mode == 0) {
                        syntaxError(chars, "Missing '['");
                    }
                    if (!literal) {
                        switch (c) {
                            case 36:
                                backup = chars.getPos(backup);
                                c = chars.next(opts);
                                boolean literal3 = chars.isEscaped();
                                boolean anchor = c == 93 && !literal3;
                                if (symbols == null && !anchor) {
                                    c = 36;
                                    chars.setPos(backup);
                                } else if (anchor && op == 0) {
                                    if (lastItem == 1) {
                                        add_unchecked(lastChar, lastChar);
                                        _appendToPat(patBuf, lastChar, false);
                                    }
                                    add_unchecked(65535);
                                    usePat = true;
                                    patBuf.append(SymbolTable.SYMBOL_REF).append(']');
                                    mode = 2;
                                } else {
                                    syntaxError(chars, "Unquoted '$'");
                                }
                                break;
                            case 38:
                                if (lastItem == 2 && op == 0) {
                                    op = (char) c;
                                } else {
                                    syntaxError(chars, "'&' not after set");
                                }
                                break;
                            case 45:
                                if (op == 0) {
                                    if (lastItem != 0) {
                                        op = (char) c;
                                        break;
                                    } else if (lastString != null) {
                                        op = (char) c;
                                        break;
                                    } else {
                                        add_unchecked(c, c);
                                        c = chars.next(opts);
                                        boolean literal4 = chars.isEscaped();
                                        if (c == 93 && !literal4) {
                                            patBuf.append("-]");
                                            mode = 2;
                                            break;
                                        }
                                    }
                                }
                                syntaxError(chars, "'-' not after char, string, or set");
                                break;
                            case 93:
                                if (lastItem == 1) {
                                    add_unchecked(lastChar, lastChar);
                                    _appendToPat(patBuf, lastChar, false);
                                }
                                if (op == '-') {
                                    add_unchecked(op, op);
                                    patBuf.append(op);
                                } else if (op == '&') {
                                    syntaxError(chars, "Trailing '&'");
                                }
                                patBuf.append(']');
                                mode = 2;
                                continue;
                            case 94:
                                syntaxError(chars, "'^' not after '['");
                                break;
                            case 123:
                                if (op != 0 && op != '-') {
                                    syntaxError(chars, "Missing operand after operator");
                                }
                                if (lastItem == 1) {
                                    add_unchecked(lastChar, lastChar);
                                    _appendToPat(patBuf, lastChar, false);
                                }
                                lastItem = 0;
                                if (buf == null) {
                                    buf = new StringBuilder();
                                } else {
                                    buf.setLength(0);
                                }
                                boolean ok = false;
                                while (true) {
                                    if (!chars.atEnd()) {
                                        int c2 = chars.next(opts);
                                        boolean literal5 = chars.isEscaped();
                                        if (c2 == 125 && !literal5) {
                                            ok = true;
                                        } else {
                                            appendCodePoint(buf, c2);
                                        }
                                    }
                                }
                                if (buf.length() < 1 || !ok) {
                                    syntaxError(chars, "Invalid multicharacter string");
                                }
                                String curString = buf.toString();
                                if (op == '-') {
                                    int lastSingle = CharSequences.getSingleCodePoint(lastString == null ? "" : lastString);
                                    int curSingle = CharSequences.getSingleCodePoint(curString);
                                    if (lastSingle != Integer.MAX_VALUE && curSingle != Integer.MAX_VALUE) {
                                        add(lastSingle, curSingle);
                                    } else {
                                        try {
                                            StringRange.expand(lastString, curString, true, this.strings);
                                        } catch (Exception e2) {
                                            syntaxError(chars, e2.getMessage());
                                        }
                                    }
                                    lastString = null;
                                    op = 0;
                                } else {
                                    add(curString);
                                    lastString = curString;
                                }
                                patBuf.append('{');
                                _appendToPat(patBuf, curString, false);
                                patBuf.append('}');
                                continue;
                        }
                    }
                    switch (lastItem) {
                        case 0:
                            if (op == '-' && lastString != null) {
                                syntaxError(chars, "Invalid range");
                            }
                            lastItem = 1;
                            lastChar = c;
                            lastString = null;
                            break;
                        case 1:
                            if (op == '-') {
                                if (lastString != null) {
                                    syntaxError(chars, "Invalid range");
                                }
                                if (lastChar >= c) {
                                    syntaxError(chars, "Invalid range");
                                }
                                add_unchecked(lastChar, c);
                                _appendToPat(patBuf, lastChar, false);
                                patBuf.append(op);
                                _appendToPat(patBuf, c, false);
                                lastItem = 0;
                                op = 0;
                            } else {
                                add_unchecked(lastChar, lastChar);
                                _appendToPat(patBuf, lastChar, false);
                                lastChar = c;
                            }
                            break;
                        case 2:
                            if (op != 0) {
                                syntaxError(chars, "Set expected after operator");
                            }
                            lastChar = c;
                            lastItem = 1;
                            break;
                    }
                }
            }
        }
        if (mode != 2) {
            syntaxError(chars, "Missing ']'");
        }
        chars.skipIgnored(opts);
        if ((options & 2) != 0) {
            closeOver(2);
        }
        if (invert) {
            complement();
        }
        if (usePat) {
            append(rebuiltPat, patBuf.toString());
        } else {
            appendNewPattern(rebuiltPat, false, true);
        }
    }

    private static void syntaxError(RuleCharacterIterator chars, String msg) {
        throw new IllegalArgumentException("Error: " + msg + " at \"" + Utility.escape(chars.toString()) + '\"');
    }

    public <T extends Collection<String>> T addAllTo(T t) {
        return (T) addAllTo(this, t);
    }

    public String[] addAllTo(String[] target) {
        return (String[]) addAllTo(this, target);
    }

    public static String[] toArray(UnicodeSet set) {
        return (String[]) addAllTo(set, new String[set.size()]);
    }

    public UnicodeSet add(Iterable<?> source) {
        return addAll(source);
    }

    public UnicodeSet addAll(Iterable<?> source) {
        checkFrozen();
        for (Object o : source) {
            add(o.toString());
        }
        return this;
    }

    private void ensureCapacity(int newLen) {
        if (newLen <= this.list.length) {
            return;
        }
        int[] temp = new int[newLen + 16];
        System.arraycopy(this.list, 0, temp, 0, this.len);
        this.list = temp;
    }

    private void ensureBufferCapacity(int newLen) {
        if (this.buffer == null || newLen > this.buffer.length) {
            this.buffer = new int[newLen + 16];
        }
    }

    private int[] range(int start, int end) {
        if (this.rangeList == null) {
            this.rangeList = new int[]{start, end + 1, 1114112};
        } else {
            this.rangeList[0] = start;
            this.rangeList[1] = end + 1;
        }
        return this.rangeList;
    }

    private UnicodeSet xor(int[] other, int otherLen, int polarity) {
        int b;
        int k;
        int j;
        int i;
        int i2;
        int j2;
        int k2;
        ensureBufferCapacity(this.len + otherLen);
        int j3 = 0;
        int a = this.list[0];
        if (polarity == 1 || polarity == 2) {
            b = 0;
            if (other[0] == 0) {
                j3 = 1;
                b = other[1];
            }
            k = 0;
            j = j3;
            i = 1;
        } else {
            b = other[0];
            k = 0;
            j = 1;
            i = 1;
        }
        while (true) {
            if (a < b) {
                k2 = k + 1;
                this.buffer[k] = a;
                i2 = i + 1;
                a = this.list[i];
                j2 = j;
            } else if (b < a) {
                k2 = k + 1;
                this.buffer[k] = b;
                j2 = j + 1;
                b = other[j];
                i2 = i;
            } else if (a != 1114112) {
                i2 = i + 1;
                a = this.list[i];
                j2 = j + 1;
                b = other[j];
                k2 = k;
            } else {
                this.buffer[k] = 1114112;
                this.len = k + 1;
                int[] temp = this.list;
                this.list = this.buffer;
                this.buffer = temp;
                this.pat = null;
                return this;
            }
            k = k2;
            j = j2;
            i = i2;
        }
    }

    private UnicodeSet add(int[] other, int otherLen, int polarity) {
        int k;
        ensureBufferCapacity(this.len + otherLen);
        int k2 = 0;
        int i = 1;
        int a = this.list[0];
        int j = 1;
        int b = other[0];
        while (true) {
            switch (polarity) {
                case 0:
                    if (a < b) {
                        if (k > 0 && a <= this.buffer[k - 1]) {
                            k2 = k - 1;
                            a = max(this.list[i], this.buffer[k2]);
                        } else {
                            k2 = k + 1;
                            this.buffer[k] = a;
                            a = this.list[i];
                        }
                        i = i + 1;
                        polarity ^= 1;
                        j = j;
                    } else if (b < a) {
                        if (k > 0 && b <= this.buffer[k - 1]) {
                            k2 = k - 1;
                            b = max(other[j], this.buffer[k2]);
                        } else {
                            k2 = k + 1;
                            this.buffer[k] = b;
                            b = other[j];
                        }
                        j = j + 1;
                        polarity ^= 2;
                        i = i;
                    } else if (a != 1114112) {
                        if (k > 0 && a <= this.buffer[k - 1]) {
                            k2 = k - 1;
                            a = max(this.list[i], this.buffer[k2]);
                        } else {
                            k2 = k + 1;
                            this.buffer[k] = a;
                            a = this.list[i];
                        }
                        i = i + 1;
                        j = j + 1;
                        b = other[j];
                        polarity = (polarity ^ 1) ^ 2;
                    }
                    break;
                case 1:
                    if (a < b) {
                        k2 = k + 1;
                        this.buffer[k] = a;
                        i = i + 1;
                        a = this.list[i];
                        polarity ^= 1;
                        j = j;
                    } else if (b < a) {
                        j = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        k2 = k;
                        i = i;
                    } else if (a != 1114112) {
                        i = i + 1;
                        a = this.list[i];
                        j = j + 1;
                        b = other[j];
                        polarity = (polarity ^ 1) ^ 2;
                        k2 = k;
                    }
                    break;
                case 2:
                    if (b < a) {
                        k2 = k + 1;
                        this.buffer[k] = b;
                        j = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        i = i;
                    } else if (a < b) {
                        i = i + 1;
                        a = this.list[i];
                        polarity ^= 1;
                        k2 = k;
                        j = j;
                    } else if (a != 1114112) {
                        i = i + 1;
                        a = this.list[i];
                        j = j + 1;
                        b = other[j];
                        polarity = (polarity ^ 1) ^ 2;
                        k2 = k;
                    }
                    break;
                case 3:
                    if (b <= a) {
                        if (a != 1114112) {
                            k2 = k + 1;
                            this.buffer[k] = a;
                            i = i + 1;
                            a = this.list[i];
                            j = j + 1;
                            b = other[j];
                            polarity = (polarity ^ 1) ^ 2;
                        }
                    } else if (b != 1114112) {
                        k2 = k + 1;
                        this.buffer[k] = b;
                        i = i + 1;
                        a = this.list[i];
                        j = j + 1;
                        b = other[j];
                        polarity = (polarity ^ 1) ^ 2;
                    }
                    break;
                default:
                    k2 = k;
                    j = j;
                    i = i;
                    break;
            }
        }
        this.buffer[k] = 1114112;
        this.len = k + 1;
        int[] temp = this.list;
        this.list = this.buffer;
        this.buffer = temp;
        this.pat = null;
        return this;
    }

    private UnicodeSet retain(int[] other, int otherLen, int polarity) {
        int k;
        ensureBufferCapacity(this.len + otherLen);
        int k2 = 0;
        int i = 1;
        int a = this.list[0];
        int j = 1;
        int b = other[0];
        while (true) {
            switch (polarity) {
                case 0:
                    if (a < b) {
                        i = i + 1;
                        a = this.list[i];
                        polarity ^= 1;
                        k2 = k;
                        j = j;
                    } else if (b < a) {
                        j = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        k2 = k;
                        i = i;
                    } else if (a != 1114112) {
                        k2 = k + 1;
                        this.buffer[k] = a;
                        i = i + 1;
                        a = this.list[i];
                        j = j + 1;
                        b = other[j];
                        polarity = (polarity ^ 1) ^ 2;
                    }
                    break;
                case 1:
                    if (a < b) {
                        i = i + 1;
                        a = this.list[i];
                        polarity ^= 1;
                        k2 = k;
                        j = j;
                    } else if (b < a) {
                        k2 = k + 1;
                        this.buffer[k] = b;
                        j = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        i = i;
                    } else if (a != 1114112) {
                        i = i + 1;
                        a = this.list[i];
                        j = j + 1;
                        b = other[j];
                        polarity = (polarity ^ 1) ^ 2;
                        k2 = k;
                    }
                    break;
                case 2:
                    if (b < a) {
                        j = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        k2 = k;
                        i = i;
                    } else if (a < b) {
                        k2 = k + 1;
                        this.buffer[k] = a;
                        i = i + 1;
                        a = this.list[i];
                        polarity ^= 1;
                        j = j;
                    } else if (a != 1114112) {
                        i = i + 1;
                        a = this.list[i];
                        j = j + 1;
                        b = other[j];
                        polarity = (polarity ^ 1) ^ 2;
                        k2 = k;
                    }
                    break;
                case 3:
                    if (a < b) {
                        k2 = k + 1;
                        this.buffer[k] = a;
                        i = i + 1;
                        a = this.list[i];
                        polarity ^= 1;
                        j = j;
                    } else if (b < a) {
                        k2 = k + 1;
                        this.buffer[k] = b;
                        j = j + 1;
                        b = other[j];
                        polarity ^= 2;
                        i = i;
                    } else if (a != 1114112) {
                        k2 = k + 1;
                        this.buffer[k] = a;
                        i = i + 1;
                        a = this.list[i];
                        j = j + 1;
                        b = other[j];
                        polarity = (polarity ^ 1) ^ 2;
                    }
                    break;
                default:
                    k2 = k;
                    j = j;
                    i = i;
                    break;
            }
        }
        this.buffer[k] = 1114112;
        this.len = k + 1;
        int[] temp = this.list;
        this.list = this.buffer;
        this.buffer = temp;
        this.pat = null;
        return this;
    }

    private static final int max(int a, int b) {
        return a > b ? a : b;
    }

    private static class NumericValueFilter implements Filter {
        double value;

        NumericValueFilter(double value) {
            this.value = value;
        }

        @Override
        public boolean contains(int ch) {
            return UCharacter.getUnicodeNumericValue(ch) == this.value;
        }
    }

    private static class GeneralCategoryMaskFilter implements Filter {
        int mask;

        GeneralCategoryMaskFilter(int mask) {
            this.mask = mask;
        }

        @Override
        public boolean contains(int ch) {
            return ((1 << UCharacter.getType(ch)) & this.mask) != 0;
        }
    }

    private static class IntPropertyFilter implements Filter {
        int prop;
        int value;

        IntPropertyFilter(int prop, int value) {
            this.prop = prop;
            this.value = value;
        }

        @Override
        public boolean contains(int ch) {
            return UCharacter.getIntPropertyValue(ch, this.prop) == this.value;
        }
    }

    private static class ScriptExtensionsFilter implements Filter {
        int script;

        ScriptExtensionsFilter(int script) {
            this.script = script;
        }

        @Override
        public boolean contains(int c) {
            return UScript.hasScript(c, this.script);
        }
    }

    private static class VersionFilter implements Filter {
        VersionInfo version;

        VersionFilter(VersionInfo version) {
            this.version = version;
        }

        @Override
        public boolean contains(int ch) {
            VersionInfo v = UCharacter.getAge(ch);
            return v != UnicodeSet.NO_VERSION && v.compareTo(this.version) <= 0;
        }
    }

    private static synchronized UnicodeSet getInclusions(int src) {
        if (INCLUSIONS == null) {
            INCLUSIONS = new UnicodeSet[12];
        }
        if (INCLUSIONS[src] == null) {
            UnicodeSet incl = new UnicodeSet();
            switch (src) {
                case 1:
                    UCharacterProperty.INSTANCE.addPropertyStarts(incl);
                    break;
                case 2:
                    UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(incl);
                    break;
                case 3:
                default:
                    throw new IllegalStateException("UnicodeSet.getInclusions(unknown src " + src + ")");
                case 4:
                    UCaseProps.INSTANCE.addPropertyStarts(incl);
                    break;
                case 5:
                    UBiDiProps.INSTANCE.addPropertyStarts(incl);
                    break;
                case 6:
                    UCharacterProperty.INSTANCE.addPropertyStarts(incl);
                    UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(incl);
                    break;
                case 7:
                    Norm2AllModes.getNFCInstance().impl.addPropertyStarts(incl);
                    UCaseProps.INSTANCE.addPropertyStarts(incl);
                    break;
                case 8:
                    Norm2AllModes.getNFCInstance().impl.addPropertyStarts(incl);
                    break;
                case 9:
                    Norm2AllModes.getNFKCInstance().impl.addPropertyStarts(incl);
                    break;
                case 10:
                    Norm2AllModes.getNFKC_CFInstance().impl.addPropertyStarts(incl);
                    break;
                case 11:
                    Norm2AllModes.getNFCInstance().impl.addCanonIterPropertyStarts(incl);
                    break;
            }
            INCLUSIONS[src] = incl;
        }
        return INCLUSIONS[src];
    }

    private UnicodeSet applyFilter(Filter filter, int src) {
        clear();
        int startHasProperty = -1;
        UnicodeSet inclusions = getInclusions(src);
        int limitRange = inclusions.getRangeCount();
        for (int j = 0; j < limitRange; j++) {
            int start = inclusions.getRangeStart(j);
            int end = inclusions.getRangeEnd(j);
            for (int ch = start; ch <= end; ch++) {
                if (filter.contains(ch)) {
                    if (startHasProperty < 0) {
                        startHasProperty = ch;
                    }
                } else if (startHasProperty >= 0) {
                    add_unchecked(startHasProperty, ch - 1);
                    startHasProperty = -1;
                }
            }
        }
        if (startHasProperty >= 0) {
            add_unchecked(startHasProperty, 1114111);
        }
        return this;
    }

    private static String mungeCharName(String source) {
        String source2 = PatternProps.trimWhiteSpace(source);
        StringBuilder buf = null;
        for (int i = 0; i < source2.length(); i++) {
            char ch = source2.charAt(i);
            if (PatternProps.isWhiteSpace(ch)) {
                if (buf == null) {
                    buf = new StringBuilder().append((CharSequence) source2, 0, i);
                } else if (buf.charAt(buf.length() - 1) == ' ') {
                }
                ch = ' ';
                if (buf == null) {
                }
            } else if (buf == null) {
                buf.append(ch);
            }
        }
        if (buf == null) {
            return source2;
        }
        String source3 = buf.toString();
        return source3;
    }

    public UnicodeSet applyIntPropertyValue(int prop, int value) {
        checkFrozen();
        if (prop == 8192) {
            applyFilter(new GeneralCategoryMaskFilter(value), 1);
        } else if (prop == 28672) {
            applyFilter(new ScriptExtensionsFilter(value), 2);
        } else {
            applyFilter(new IntPropertyFilter(prop, value), UCharacterProperty.INSTANCE.getSource(prop));
        }
        return this;
    }

    public UnicodeSet applyPropertyAlias(String propertyAlias, String valueAlias) {
        return applyPropertyAlias(propertyAlias, valueAlias, null);
    }

    public UnicodeSet applyPropertyAlias(String propertyAlias, String valueAlias, SymbolTable symbols) {
        int p;
        int v;
        checkFrozen();
        boolean invert = false;
        if (symbols != null && (symbols instanceof XSymbolTable) && ((XSymbolTable) symbols).applyPropertyAlias(propertyAlias, valueAlias, this)) {
            return this;
        }
        if (XSYMBOL_TABLE != null && XSYMBOL_TABLE.applyPropertyAlias(propertyAlias, valueAlias, this)) {
            return this;
        }
        if (valueAlias.length() > 0) {
            p = UCharacter.getPropertyEnum(propertyAlias);
            if (p == 4101) {
                p = 8192;
            }
            if ((p >= 0 && p < 57) || ((p >= 4096 && p < 4118) || (p >= 8192 && p < 8193))) {
                try {
                    v = UCharacter.getPropertyValueEnum(p, valueAlias);
                } catch (IllegalArgumentException e) {
                    if ((p != 4098 && p != 4112 && p != 4113) || (v = Integer.parseInt(PatternProps.trimWhiteSpace(valueAlias))) < 0 || v > 255) {
                        throw e;
                    }
                }
            } else {
                switch (p) {
                    case 12288:
                        double value = Double.parseDouble(PatternProps.trimWhiteSpace(valueAlias));
                        applyFilter(new NumericValueFilter(value), 1);
                        return this;
                    case 16384:
                        VersionInfo version = VersionInfo.getInstance(mungeCharName(valueAlias));
                        applyFilter(new VersionFilter(version), 2);
                        return this;
                    case UProperty.NAME:
                        String buf = mungeCharName(valueAlias);
                        int ch = UCharacter.getCharFromExtendedName(buf);
                        if (ch == -1) {
                            throw new IllegalArgumentException("Invalid character name");
                        }
                        clear();
                        add_unchecked(ch);
                        return this;
                    case UProperty.UNICODE_1_NAME:
                        throw new IllegalArgumentException("Unicode_1_Name (na1) not supported");
                    case 28672:
                        v = UCharacter.getPropertyValueEnum(UProperty.SCRIPT, valueAlias);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported property");
                }
            }
        } else {
            UPropertyAliases pnames = UPropertyAliases.INSTANCE;
            p = 8192;
            v = pnames.getPropertyValueEnum(8192, propertyAlias);
            if (v == -1) {
                p = UProperty.SCRIPT;
                v = pnames.getPropertyValueEnum(UProperty.SCRIPT, propertyAlias);
                if (v == -1) {
                    p = pnames.getPropertyEnum(propertyAlias);
                    if (p == -1) {
                        p = -1;
                    }
                    if (p >= 0 && p < 57) {
                        v = 1;
                    } else if (p == -1) {
                        if (UPropertyAliases.compare(ANY_ID, propertyAlias) == 0) {
                            set(0, 1114111);
                            return this;
                        }
                        if (UPropertyAliases.compare(ASCII_ID, propertyAlias) == 0) {
                            set(0, 127);
                            return this;
                        }
                        if (UPropertyAliases.compare(ASSIGNED, propertyAlias) == 0) {
                            p = 8192;
                            v = 1;
                            invert = true;
                        } else {
                            throw new IllegalArgumentException("Invalid property alias: " + propertyAlias + "=" + valueAlias);
                        }
                    } else {
                        throw new IllegalArgumentException("Missing property value");
                    }
                }
            }
        }
        applyIntPropertyValue(p, v);
        if (invert) {
            complement();
        }
        if (0 != 0 && isEmpty()) {
            throw new IllegalArgumentException("Invalid property value");
        }
        return this;
    }

    private static boolean resemblesPropertyPattern(String pattern, int pos) {
        if (pos + 5 > pattern.length()) {
            return false;
        }
        if (pattern.regionMatches(pos, "[:", 0, 2) || pattern.regionMatches(true, pos, "\\p", 0, 2)) {
            return true;
        }
        return pattern.regionMatches(pos, "\\N", 0, 2);
    }

    private static boolean resemblesPropertyPattern(RuleCharacterIterator chars, int iterOpts) {
        boolean result = false;
        int iterOpts2 = iterOpts & (-3);
        Object pos = chars.getPos(null);
        int c = chars.next(iterOpts2);
        if (c == 91 || c == 92) {
            int d = chars.next(iterOpts2 & (-5));
            if (c == 91) {
                result = d == 58;
            } else {
                result = d == 78 || d == 112 || d == 80;
            }
        }
        chars.setPos(pos);
        return result;
    }

    private UnicodeSet applyPropertyPattern(String pattern, ParsePosition ppos, SymbolTable symbols) {
        int pos;
        String propName;
        String valueName;
        int pos2 = ppos.getIndex();
        if (pos2 + 5 > pattern.length()) {
            return null;
        }
        boolean posix = false;
        boolean isName = false;
        boolean invert = false;
        if (pattern.regionMatches(pos2, "[:", 0, 2)) {
            posix = true;
            pos = PatternProps.skipWhiteSpace(pattern, pos2 + 2);
            if (pos < pattern.length() && pattern.charAt(pos) == '^') {
                pos++;
                invert = true;
            }
        } else if (pattern.regionMatches(true, pos2, "\\p", 0, 2) || pattern.regionMatches(pos2, "\\N", 0, 2)) {
            char c = pattern.charAt(pos2 + 1);
            invert = c == 'P';
            isName = c == 'N';
            int pos3 = PatternProps.skipWhiteSpace(pattern, pos2 + 2);
            if (pos3 == pattern.length()) {
                return null;
            }
            int pos4 = pos3 + 1;
            if (pattern.charAt(pos3) != '{') {
                return null;
            }
            pos = pos4;
        } else {
            return null;
        }
        int close = pattern.indexOf(posix ? ":]" : "}", pos);
        if (close < 0) {
            return null;
        }
        int equals = pattern.indexOf(61, pos);
        if (equals >= 0 && equals < close && !isName) {
            propName = pattern.substring(pos, equals);
            valueName = pattern.substring(equals + 1, close);
        } else {
            propName = pattern.substring(pos, close);
            valueName = "";
            if (isName) {
                valueName = propName;
                propName = "na";
            }
        }
        applyPropertyAlias(propName, valueName, symbols);
        if (invert) {
            complement();
        }
        ppos.setIndex((posix ? 2 : 1) + close);
        return this;
    }

    private void applyPropertyPattern(RuleCharacterIterator chars, Appendable rebuiltPat, SymbolTable symbols) {
        String patStr = chars.lookahead();
        ParsePosition pos = new ParsePosition(0);
        applyPropertyPattern(patStr, pos, symbols);
        if (pos.getIndex() == 0) {
            syntaxError(chars, "Invalid property pattern");
        }
        chars.jumpahead(pos.getIndex());
        append(rebuiltPat, patStr.substring(0, pos.getIndex()));
    }

    private static final void addCaseMapping(UnicodeSet set, int result, StringBuilder full) {
        if (result < 0) {
            return;
        }
        if (result > 31) {
            set.add(result);
        } else {
            set.add(full.toString());
            full.setLength(0);
        }
    }

    public UnicodeSet closeOver(int attribute) {
        checkFrozen();
        if ((attribute & 6) != 0) {
            UCaseProps csp = UCaseProps.INSTANCE;
            UnicodeSet foldSet = new UnicodeSet(this);
            ULocale root = ULocale.ROOT;
            if ((attribute & 2) != 0) {
                foldSet.strings.clear();
            }
            int n = getRangeCount();
            StringBuilder full = new StringBuilder();
            int[] locCache = new int[1];
            for (int i = 0; i < n; i++) {
                int start = getRangeStart(i);
                int end = getRangeEnd(i);
                if ((attribute & 2) != 0) {
                    for (int cp = start; cp <= end; cp++) {
                        csp.addCaseClosure(cp, foldSet);
                    }
                } else {
                    for (int cp2 = start; cp2 <= end; cp2++) {
                        int result = csp.toFullLower(cp2, null, full, root, locCache);
                        addCaseMapping(foldSet, result, full);
                        int result2 = csp.toFullTitle(cp2, null, full, root, locCache);
                        addCaseMapping(foldSet, result2, full);
                        int result3 = csp.toFullUpper(cp2, null, full, root, locCache);
                        addCaseMapping(foldSet, result3, full);
                        int result4 = csp.toFullFolding(cp2, full, 0);
                        addCaseMapping(foldSet, result4, full);
                    }
                }
            }
            if (!this.strings.isEmpty()) {
                if ((attribute & 2) != 0) {
                    for (String s : this.strings) {
                        String str = UCharacter.foldCase(s, 0);
                        if (!csp.addStringCaseClosure(str, foldSet)) {
                            foldSet.add(str);
                        }
                    }
                } else {
                    BreakIterator bi = BreakIterator.getWordInstance(root);
                    for (String str2 : this.strings) {
                        foldSet.add(UCharacter.toLowerCase(root, str2));
                        foldSet.add(UCharacter.toTitleCase(root, str2, bi));
                        foldSet.add(UCharacter.toUpperCase(root, str2));
                        foldSet.add(UCharacter.foldCase(str2, 0));
                    }
                }
            }
            set(foldSet);
        }
        return this;
    }

    public static abstract class XSymbolTable implements SymbolTable {
        @Override
        public UnicodeMatcher lookupMatcher(int i) {
            return null;
        }

        public boolean applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
            return false;
        }

        @Override
        public char[] lookup(String s) {
            return null;
        }

        @Override
        public String parseReference(String text, ParsePosition pos, int limit) {
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
                int capacity = this.len == 0 ? 1 : this.len;
                int[] oldList = this.list;
                this.list = new int[capacity];
                int i = capacity;
                while (true) {
                    int i2 = i;
                    i = i2 - 1;
                    if (i2 <= 0) {
                        break;
                    }
                    this.list[i] = oldList[i];
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

    public int span(CharSequence s, SpanCondition spanCondition) {
        return span(s, 0, spanCondition);
    }

    public int span(CharSequence s, int start, SpanCondition spanCondition) {
        int end = s.length();
        if (start < 0) {
            start = 0;
        } else if (start >= end) {
            return end;
        }
        if (this.bmpSet != null) {
            return this.bmpSet.span(s, start, spanCondition, null);
        }
        if (this.stringSpan != null) {
            return this.stringSpan.span(s, start, spanCondition);
        }
        if (!this.strings.isEmpty()) {
            int which = spanCondition == SpanCondition.NOT_CONTAINED ? 33 : 34;
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList(this.strings), which);
            if (strSpan.needsStringSpanUTF16()) {
                return strSpan.span(s, start, spanCondition);
            }
        }
        return spanCodePointsAndCount(s, start, spanCondition, null);
    }

    @Deprecated
    public int spanAndCount(CharSequence s, int start, SpanCondition spanCondition, OutputInt outCount) {
        if (outCount == null) {
            throw new IllegalArgumentException("outCount must not be null");
        }
        int end = s.length();
        if (start < 0) {
            start = 0;
        } else if (start >= end) {
            return end;
        }
        if (this.stringSpan != null) {
            return this.stringSpan.spanAndCount(s, start, spanCondition, outCount);
        }
        if (this.bmpSet != null) {
            return this.bmpSet.span(s, start, spanCondition, outCount);
        }
        if (!this.strings.isEmpty()) {
            int which = spanCondition == SpanCondition.NOT_CONTAINED ? 33 : 34;
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList(this.strings), which | 64);
            return strSpan.spanAndCount(s, start, spanCondition, outCount);
        }
        return spanCodePointsAndCount(s, start, spanCondition, outCount);
    }

    private int spanCodePointsAndCount(CharSequence s, int start, SpanCondition spanCondition, OutputInt outCount) {
        boolean spanContained = spanCondition != SpanCondition.NOT_CONTAINED;
        int next = start;
        int length = s.length();
        int count = 0;
        do {
            int c = Character.codePointAt(s, next);
            if (spanContained != contains(c)) {
                break;
            }
            count++;
            next += Character.charCount(c);
        } while (next < length);
        if (outCount != null) {
            outCount.value = count;
        }
        return next;
    }

    public int spanBack(CharSequence s, SpanCondition spanCondition) {
        return spanBack(s, s.length(), spanCondition);
    }

    public int spanBack(CharSequence s, int fromIndex, SpanCondition spanCondition) {
        int which;
        if (fromIndex <= 0) {
            return 0;
        }
        if (fromIndex > s.length()) {
            fromIndex = s.length();
        }
        if (this.bmpSet != null) {
            return this.bmpSet.spanBack(s, fromIndex, spanCondition);
        }
        if (this.stringSpan != null) {
            return this.stringSpan.spanBack(s, fromIndex, spanCondition);
        }
        if (!this.strings.isEmpty()) {
            if (spanCondition == SpanCondition.NOT_CONTAINED) {
                which = 17;
            } else {
                which = 18;
            }
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList(this.strings), which);
            if (strSpan.needsStringSpanUTF16()) {
                return strSpan.spanBack(s, fromIndex, spanCondition);
            }
        }
        boolean spanContained = spanCondition != SpanCondition.NOT_CONTAINED;
        int prev = fromIndex;
        do {
            int c = Character.codePointBefore(s, prev);
            if (spanContained != contains(c)) {
                break;
            }
            prev -= Character.charCount(c);
        } while (prev > 0);
        return prev;
    }

    @Override
    public UnicodeSet cloneAsThawed() {
        UnicodeSet result = new UnicodeSet(this);
        if (!f101assertionsDisabled) {
            if (!(!result.isFrozen())) {
                throw new AssertionError();
            }
        }
        return result;
    }

    private void checkFrozen() {
        if (!isFrozen()) {
        } else {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
    }

    public static class EntryRange {
        public int codepoint;
        public int codepointEnd;

        EntryRange() {
        }

        public String toString() {
            StringBuilder b = new StringBuilder();
            return (this.codepoint == this.codepointEnd ? (StringBuilder) UnicodeSet._appendToPat(b, this.codepoint, false) : (StringBuilder) UnicodeSet._appendToPat(((StringBuilder) UnicodeSet._appendToPat(b, this.codepoint, false)).append('-'), this.codepointEnd, false)).toString();
        }
    }

    public Iterable<EntryRange> ranges() {
        return new EntryRangeIterable(this, null);
    }

    private class EntryRangeIterable implements Iterable<EntryRange> {
        EntryRangeIterable(UnicodeSet this$0, EntryRangeIterable entryRangeIterable) {
            this();
        }

        private EntryRangeIterable() {
        }

        @Override
        public Iterator<EntryRange> iterator() {
            return new EntryRangeIterator(UnicodeSet.this, null);
        }
    }

    private class EntryRangeIterator implements Iterator<EntryRange> {
        int pos;
        EntryRange result;

        EntryRangeIterator(UnicodeSet this$0, EntryRangeIterator entryRangeIterator) {
            this();
        }

        private EntryRangeIterator() {
            this.result = new EntryRange();
        }

        @Override
        public boolean hasNext() {
            return this.pos < UnicodeSet.this.len + (-1);
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

        UnicodeSetIterator2(UnicodeSet source) {
            this.len = source.len - 1;
            if (this.len > 0) {
                this.sourceStrings = source.strings;
                this.sourceList = source.list;
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
            this.stringIterator = source.strings.iterator();
            this.sourceList = null;
        }

        @Override
        public boolean hasNext() {
            if (this.sourceList == null) {
                return this.stringIterator.hasNext();
            }
            return true;
        }

        @Override
        public String next() {
            if (this.sourceList == null) {
                return this.stringIterator.next();
            }
            int codepoint = this.current;
            this.current = codepoint + 1;
            if (this.current >= this.limit) {
                if (this.item >= this.len) {
                    this.stringIterator = this.sourceStrings.iterator();
                    this.sourceList = null;
                } else {
                    int[] iArr = this.sourceList;
                    int i = this.item;
                    this.item = i + 1;
                    this.current = iArr[i];
                    int[] iArr2 = this.sourceList;
                    int i2 = this.item;
                    this.item = i2 + 1;
                    this.limit = iArr2[i2];
                }
            }
            if (codepoint <= 65535) {
                return String.valueOf((char) codepoint);
            }
            if (this.buffer == null) {
                this.buffer = new char[2];
            }
            int offset = codepoint - 65536;
            this.buffer[0] = (char) ((offset >>> 10) + 55296);
            this.buffer[1] = (char) ((offset & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE);
            return String.valueOf(this.buffer);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public <T extends CharSequence> boolean containsAll(Iterable<T> collection) {
        for (T o : collection) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    public <T extends CharSequence> boolean containsNone(Iterable<T> collection) {
        for (T o : collection) {
            if (contains(o)) {
                return false;
            }
        }
        return true;
    }

    public final <T extends CharSequence> boolean containsSome(Iterable<T> collection) {
        return !containsNone(collection);
    }

    public <T extends CharSequence> UnicodeSet addAll(T... collection) {
        checkFrozen();
        for (T str : collection) {
            add(str);
        }
        return this;
    }

    public <T extends CharSequence> UnicodeSet removeAll(Iterable<T> collection) {
        checkFrozen();
        for (T o : collection) {
            remove(o);
        }
        return this;
    }

    public <T extends CharSequence> UnicodeSet retainAll(Iterable<T> collection) {
        checkFrozen();
        UnicodeSet toRetain = new UnicodeSet();
        toRetain.addAll((Iterable<?>) collection);
        retainAll(toRetain);
        return this;
    }

    public enum ComparisonStyle {
        SHORTER_FIRST,
        LEXICOGRAPHIC,
        LONGER_FIRST;

        public static ComparisonStyle[] valuesCustom() {
            return values();
        }
    }

    @Override
    public int compareTo(UnicodeSet o) {
        return compareTo(o, ComparisonStyle.SHORTER_FIRST);
    }

    public int compareTo(UnicodeSet o, ComparisonStyle style) {
        int diff;
        if (style != ComparisonStyle.LEXICOGRAPHIC && (diff = size() - o.size()) != 0) {
            return (diff < 0) == (style == ComparisonStyle.SHORTER_FIRST) ? -1 : 1;
        }
        int i = 0;
        while (true) {
            int result = this.list[i] - o.list[i];
            if (result != 0) {
                if (this.list[i] == 1114112) {
                    if (this.strings.isEmpty()) {
                        return 1;
                    }
                    String item = this.strings.first();
                    return compare(item, o.list[i]);
                }
                if (o.list[i] != 1114112) {
                    return (i & 1) == 0 ? result : -result;
                }
                if (o.strings.isEmpty()) {
                    return -1;
                }
                String item2 = o.strings.first();
                return -compare(item2, this.list[i]);
            }
            if (this.list[i] != 1114112) {
                i++;
            } else {
                return compare(this.strings, o.strings);
            }
        }
    }

    public int compareTo(Iterable<String> other) {
        return compare(this, other);
    }

    public static int compare(CharSequence string, int codePoint) {
        return CharSequences.compare(string, codePoint);
    }

    public static int compare(int codePoint, CharSequence string) {
        return -CharSequences.compare(string, codePoint);
    }

    public static <T extends Comparable<T>> int compare(Iterable<T> collection1, Iterable<T> collection2) {
        return compare(collection1.iterator(), collection2.iterator());
    }

    @Deprecated
    public static <T extends Comparable<T>> int compare(Iterator<T> first, Iterator<T> other) {
        while (first.hasNext()) {
            if (!other.hasNext()) {
                return 1;
            }
            T item1 = first.next();
            T item2 = other.next();
            int result = item1.compareTo(item2);
            if (result != 0) {
                return result;
            }
        }
        return other.hasNext() ? -1 : 0;
    }

    public static <T extends Comparable<T>> int compare(Collection<T> collection1, Collection<T> collection2, ComparisonStyle style) {
        int diff;
        if (style == ComparisonStyle.LEXICOGRAPHIC || (diff = collection1.size() - collection2.size()) == 0) {
            return compare(collection1, collection2);
        }
        return (diff < 0) == (style == ComparisonStyle.SHORTER_FIRST) ? -1 : 1;
    }

    public static <T, U extends Collection<T>> U addAllTo(Iterable<T> source, U target) {
        for (T item : source) {
            target.add(item);
        }
        return target;
    }

    public static <T> T[] addAllTo(Iterable<T> source, T[] target) {
        int i = 0;
        for (T item : source) {
            target[i] = item;
            i++;
        }
        return target;
    }

    public Collection<String> strings() {
        return Collections.unmodifiableSortedSet(this.strings);
    }

    @Deprecated
    public static int getSingleCodePoint(CharSequence s) {
        return CharSequences.getSingleCodePoint(s);
    }

    @Deprecated
    public UnicodeSet addBridges(UnicodeSet dontCare) {
        UnicodeSet notInInput = new UnicodeSet(this).complement();
        UnicodeSetIterator it = new UnicodeSetIterator(notInInput);
        while (it.nextRange()) {
            if (it.codepoint != 0 && it.codepoint != UnicodeSetIterator.IS_STRING && it.codepointEnd != 1114111 && dontCare.contains(it.codepoint, it.codepointEnd)) {
                add(it.codepoint, it.codepointEnd);
            }
        }
        return this;
    }

    @Deprecated
    public int findIn(CharSequence value, int fromIndex, boolean findNot) {
        while (fromIndex < value.length()) {
            int cp = UTF16.charAt(value, fromIndex);
            if (contains(cp) != findNot) {
                break;
            }
            fromIndex += UTF16.getCharCount(cp);
        }
        return fromIndex;
    }

    @Deprecated
    public int findLastIn(CharSequence value, int fromIndex, boolean findNot) {
        int fromIndex2 = fromIndex - 1;
        while (fromIndex2 >= 0) {
            int cp = UTF16.charAt(value, fromIndex2);
            if (contains(cp) != findNot) {
                break;
            }
            fromIndex2 -= UTF16.getCharCount(cp);
        }
        if (fromIndex2 < 0) {
            return -1;
        }
        return fromIndex2;
    }

    @Deprecated
    public String stripFrom(CharSequence source, boolean matches) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (pos < source.length()) {
            int inside = findIn(source, pos, !matches);
            result.append(source.subSequence(pos, inside));
            pos = findIn(source, inside, matches);
        }
        return result.toString();
    }

    public enum SpanCondition {
        NOT_CONTAINED,
        CONTAINED,
        SIMPLE,
        CONDITION_COUNT;

        public static SpanCondition[] valuesCustom() {
            return values();
        }
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
