package android.icu.impl;

import android.icu.impl.ICUBinary;
import android.icu.impl.Trie2;
import android.icu.text.UTF16;
import android.icu.text.UnicodeSet;
import android.icu.util.ICUUncheckedIOException;
import android.icu.util.ULocale;
import java.io.IOException;
import java.nio.ByteBuffer;

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
    private static final int FOLD_CASE_OPTIONS_MASK = 255;
    private static final int FULL_LOWER = 15;
    public static final UCaseProps INSTANCE;
    private static final int IX_EXC_LENGTH = 3;
    private static final int IX_TOP = 16;
    private static final int IX_TRIE_SIZE = 2;
    private static final int IX_UNFOLD_LENGTH = 4;
    private static final int LOC_LITHUANIAN = 3;
    private static final int LOC_ROOT = 1;
    private static final int LOC_TURKISH = 2;
    private static final int LOC_UNKNOWN = 0;
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
    private char[] exceptions;
    private int[] indexes;
    private Trie2_16 trie;
    private char[] unfold;
    private static final byte[] flagsOffset = {0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8};
    private static final int[] rootLocCache = {1};
    public static final StringBuilder dummyStringBuilder = new StringBuilder();

    public interface ContextIterator {
        int next();

        void reset(int i);
    }

    private UCaseProps() throws IOException {
        ByteBuffer bytes = ICUBinary.getRequiredData(DATA_FILE_NAME);
        readData(bytes);
    }

    private final void readData(ByteBuffer bytes) throws IOException {
        ICUBinary.readHeader(bytes, FMT, new IsAcceptable(null));
        int count = bytes.getInt();
        if (count < 16) {
            throw new IOException("indexes[0] too small in ucase.icu");
        }
        this.indexes = new int[count];
        this.indexes[0] = count;
        for (int i = 1; i < count; i++) {
            this.indexes[i] = bytes.getInt();
        }
        this.trie = Trie2_16.createFromSerialized(bytes);
        int expectedTrieLength = this.indexes[2];
        int trieLength = this.trie.getSerializedLength();
        if (trieLength > expectedTrieLength) {
            throw new IOException("ucase.icu: not enough bytes for the trie");
        }
        ICUBinary.skipBytes(bytes, expectedTrieLength - trieLength);
        int count2 = this.indexes[3];
        if (count2 > 0) {
            this.exceptions = ICUBinary.getChars(bytes, count2, 0);
        }
        int count3 = this.indexes[4];
        if (count3 <= 0) {
            return;
        }
        this.unfold = ICUBinary.getChars(bytes, count3, 0);
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        IsAcceptable(IsAcceptable isAcceptable) {
            this();
        }

        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] version) {
            return version[0] == 3;
        }
    }

    public final void addPropertyStarts(UnicodeSet set) {
        for (Trie2.Range range : this.trie) {
            if (range.leadSurrogate) {
                return;
            } else {
                set.add(range.startCodePoint);
            }
        }
    }

    private static final int getExceptionsOffset(int props) {
        return props >> 5;
    }

    private static final boolean propsHasException(int props) {
        return (props & 16) != 0;
    }

    static {
        try {
            INSTANCE = new UCaseProps();
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static final boolean hasSlot(int flags, int index) {
        return ((1 << index) & flags) != 0;
    }

    private static final byte slotOffset(int flags, int index) {
        return flagsOffset[((1 << index) - 1) & flags];
    }

    private final long getSlotValueAndOffset(int excWord, int index, int excOffset) {
        long value;
        int excOffset2;
        if ((excWord & 256) == 0) {
            excOffset2 = excOffset + slotOffset(excWord, index);
            value = this.exceptions[excOffset2];
        } else {
            int excOffset3 = excOffset + (slotOffset(excWord, index) * 2);
            int excOffset4 = excOffset3 + 1;
            long value2 = this.exceptions[excOffset3];
            value = (value2 << 16) | ((long) this.exceptions[excOffset4]);
            excOffset2 = excOffset4;
        }
        return (((long) excOffset2) << 32) | value;
    }

    private final int getSlotValue(int excWord, int index, int excOffset) {
        if ((excWord & 256) == 0) {
            return this.exceptions[excOffset + slotOffset(excWord, index)];
        }
        int excOffset2 = excOffset + (slotOffset(excWord, index) * 2);
        int value = (this.exceptions[excOffset2] << 16) | this.exceptions[excOffset2 + 1];
        return value;
    }

    public final int tolower(int c) {
        int props = this.trie.get(c);
        if (!propsHasException(props)) {
            if (getTypeFromProps(props) >= 2) {
                return c + getDelta(props);
            }
            return c;
        }
        int excOffset = getExceptionsOffset(props);
        int excOffset2 = excOffset + 1;
        char c2 = this.exceptions[excOffset];
        if (hasSlot(c2, 0)) {
            return getSlotValue(c2, 0, excOffset2);
        }
        return c;
    }

    public final int toupper(int c) {
        int props = this.trie.get(c);
        if (!propsHasException(props)) {
            if (getTypeFromProps(props) == 1) {
                return c + getDelta(props);
            }
            return c;
        }
        int excOffset = getExceptionsOffset(props);
        int excOffset2 = excOffset + 1;
        char c2 = this.exceptions[excOffset];
        if (hasSlot(c2, 2)) {
            return getSlotValue(c2, 2, excOffset2);
        }
        return c;
    }

    public final int totitle(int c) {
        int index;
        int props = this.trie.get(c);
        if (!propsHasException(props)) {
            if (getTypeFromProps(props) == 1) {
                return c + getDelta(props);
            }
            return c;
        }
        int excOffset = getExceptionsOffset(props);
        int excOffset2 = excOffset + 1;
        char c2 = this.exceptions[excOffset];
        if (hasSlot(c2, 3)) {
            index = 3;
        } else if (hasSlot(c2, 2)) {
            index = 2;
        } else {
            return c;
        }
        return getSlotValue(c2, index, excOffset2);
    }

    public final void addCaseClosure(int c, UnicodeSet set) {
        int closureLength;
        int closureOffset;
        int delta;
        switch (c) {
            case 73:
                set.add(105);
                break;
            case 105:
                set.add(73);
                break;
            case 304:
                set.add(iDot);
                break;
            case 305:
                break;
            default:
                int props = this.trie.get(c);
                if (!propsHasException(props)) {
                    if (getTypeFromProps(props) != 0 && (delta = getDelta(props)) != 0) {
                        set.add(c + delta);
                        break;
                    }
                } else {
                    int excOffset = getExceptionsOffset(props);
                    int excOffset2 = excOffset + 1;
                    char c2 = this.exceptions[excOffset];
                    for (int index = 0; index <= 3; index++) {
                        if (hasSlot(c2, index)) {
                            set.add(getSlotValue(c2, index, excOffset2));
                        }
                    }
                    if (hasSlot(c2, 6)) {
                        long value = getSlotValueAndOffset(c2, 6, excOffset2);
                        closureLength = ((int) value) & 15;
                        closureOffset = ((int) (value >> 32)) + 1;
                    } else {
                        closureLength = 0;
                        closureOffset = 0;
                    }
                    if (hasSlot(c2, 7)) {
                        long value2 = getSlotValueAndOffset(c2, 7, excOffset2);
                        int fullLength = ((int) value2) & 65535;
                        int excOffset3 = ((int) (value2 >> 32)) + 1 + (fullLength & 15);
                        int fullLength2 = fullLength >> 4;
                        int length = fullLength2 & 15;
                        if (length != 0) {
                            set.add(new String(this.exceptions, excOffset3, length));
                            excOffset3 += length;
                        }
                        int fullLength3 = fullLength2 >> 4;
                        closureOffset = excOffset3 + (fullLength3 & 15) + (fullLength3 >> 4);
                    }
                    int index2 = 0;
                    while (index2 < closureLength) {
                        int c3 = UTF16.charAt(this.exceptions, closureOffset, this.exceptions.length, index2);
                        set.add(c3);
                        index2 += UTF16.getCharCount(c3);
                    }
                    break;
                }
                break;
        }
    }

    private final int strcmpMax(String s, int unfoldOffset, int max) {
        int length = s.length();
        int max2 = max - length;
        int i1 = 0;
        while (true) {
            int i12 = i1 + 1;
            int c1 = s.charAt(i1);
            int unfoldOffset2 = unfoldOffset + 1;
            char c = this.unfold[unfoldOffset];
            if (c == 0) {
                return 1;
            }
            int c12 = c1 - c;
            if (c12 != 0) {
                return c12;
            }
            length--;
            if (length <= 0) {
                if (max2 == 0 || this.unfold[unfoldOffset2] == 0) {
                    return 0;
                }
                return -max2;
            }
            i1 = i12;
            unfoldOffset = unfoldOffset2;
        }
    }

    public final boolean addStringCaseClosure(String s, UnicodeSet set) {
        int length;
        if (this.unfold == null || s == null || (length = s.length()) <= 1) {
            return false;
        }
        char c = this.unfold[0];
        char c2 = this.unfold[1];
        char c3 = this.unfold[2];
        if (length > c3) {
            return false;
        }
        int start = 0;
        int limit = c;
        while (start < limit) {
            int i = (start + limit) / 2;
            int unfoldOffset = (i + 1) * c2;
            int result = strcmpMax(s, unfoldOffset, c3);
            if (result == 0) {
                int i2 = c3;
                while (i2 < c2 && this.unfold[unfoldOffset + i2] != 0) {
                    int c4 = UTF16.charAt(this.unfold, unfoldOffset, this.unfold.length, i2);
                    set.add(c4);
                    addCaseClosure(c4, set);
                    i2 += UTF16.getCharCount(c4);
                }
                return true;
            }
            if (result < 0) {
                limit = i;
            } else {
                start = i + 1;
            }
        }
        return false;
    }

    public final int getType(int c) {
        return getTypeFromProps(this.trie.get(c));
    }

    public final int getTypeOrIgnorable(int c) {
        return getTypeAndIgnorableFromProps(this.trie.get(c));
    }

    public final int getDotType(int c) {
        int props = this.trie.get(c);
        if (!propsHasException(props)) {
            return props & 96;
        }
        return (this.exceptions[getExceptionsOffset(props)] >> 7) & 96;
    }

    public final boolean isSoftDotted(int c) {
        return getDotType(c) == 32;
    }

    public final boolean isCaseSensitive(int c) {
        return (this.trie.get(c) & 8) != 0;
    }

    private static final int getCaseLocale(ULocale locale, int[] locCache) {
        int result;
        if (locCache != null && (result = locCache[0]) != 0) {
            return result;
        }
        int result2 = 1;
        String language = locale.getLanguage();
        if (language.equals("tr") || language.equals("tur") || language.equals("az") || language.equals("aze")) {
            result2 = 2;
        } else if (language.equals("lt") || language.equals("lit")) {
            result2 = 3;
        }
        if (locCache != null) {
            locCache[0] = result2;
        }
        return result2;
    }

    private final boolean isFollowedByCasedLetter(ContextIterator iter, int dir) {
        int type;
        if (iter == null) {
            return false;
        }
        iter.reset(dir);
        do {
            int c = iter.next();
            if (c < 0) {
                return false;
            }
            type = getTypeOrIgnorable(c);
        } while ((type & 4) != 0);
        return type != 0;
    }

    private final boolean isPrecededBySoftDotted(ContextIterator iter) {
        int dotType;
        if (iter == null) {
            return false;
        }
        iter.reset(-1);
        do {
            int c = iter.next();
            if (c < 0) {
                return false;
            }
            dotType = getDotType(c);
            if (dotType == 32) {
                return true;
            }
        } while (dotType == 96);
        return false;
    }

    private final boolean isPrecededBy_I(ContextIterator iter) {
        int dotType;
        if (iter == null) {
            return false;
        }
        iter.reset(-1);
        do {
            int c = iter.next();
            if (c < 0) {
                return false;
            }
            if (c == 73) {
                return true;
            }
            dotType = getDotType(c);
        } while (dotType == 96);
        return false;
    }

    private final boolean isFollowedByMoreAbove(ContextIterator iter) {
        int dotType;
        if (iter == null) {
            return false;
        }
        iter.reset(1);
        do {
            int c = iter.next();
            if (c < 0) {
                return false;
            }
            dotType = getDotType(c);
            if (dotType == 64) {
                return true;
            }
        } while (dotType == 96);
        return false;
    }

    private final boolean isFollowedByDotAbove(ContextIterator iter) {
        int dotType;
        if (iter == null) {
            return false;
        }
        iter.reset(1);
        do {
            int c = iter.next();
            if (c < 0) {
                return false;
            }
            if (c == 775) {
                return true;
            }
            dotType = getDotType(c);
        } while (dotType == 96);
        return false;
    }

    public final int toFullLower(int c, ContextIterator iter, StringBuilder out, ULocale locale, int[] locCache) {
        int result = c;
        int props = this.trie.get(c);
        if (!propsHasException(props)) {
            if (getTypeFromProps(props) >= 2) {
                result = c + getDelta(props);
            }
        } else {
            int excOffset = getExceptionsOffset(props);
            int excOffset2 = excOffset + 1;
            char c2 = this.exceptions[excOffset];
            if ((c2 & 16384) != 0) {
                int loc = getCaseLocale(locale, locCache);
                if (loc == 3 && (((c == 73 || c == 74 || c == 302) && isFollowedByMoreAbove(iter)) || c == 204 || c == 205 || c == 296)) {
                    switch (c) {
                        case 73:
                            out.append(iDot);
                            return 2;
                        case 74:
                            out.append(jDot);
                            return 2;
                        case 204:
                            out.append(iDotGrave);
                            return 3;
                        case 205:
                            out.append(iDotAcute);
                            return 3;
                        case 296:
                            out.append(iDotTilde);
                            return 3;
                        case 302:
                            out.append(iOgonekDot);
                            return 2;
                        default:
                            return 0;
                    }
                }
                if (loc == 2 && c == 304) {
                    return 105;
                }
                if (loc == 2 && c == 775 && isPrecededBy_I(iter)) {
                    return 0;
                }
                if (loc == 2 && c == 73 && !isFollowedByDotAbove(iter)) {
                    return 305;
                }
                if (c == 304) {
                    out.append(iDot);
                    return 2;
                }
                if (c == 931 && !isFollowedByCasedLetter(iter, 1) && isFollowedByCasedLetter(iter, -1)) {
                    return 962;
                }
            } else if (hasSlot(c2, 7)) {
                long value = getSlotValueAndOffset(c2, 7, excOffset2);
                int full = ((int) value) & 15;
                if (full != 0) {
                    out.append(this.exceptions, ((int) (value >> 32)) + 1, full);
                    return full;
                }
            }
            if (hasSlot(c2, 0)) {
                result = getSlotValue(c2, 0, excOffset2);
            }
        }
        return result == c ? ~result : result;
    }

    private final int toUpperOrTitle(int c, ContextIterator iter, StringBuilder out, ULocale locale, int[] locCache, boolean upperNotTitle) {
        int full;
        int index;
        int result = c;
        int props = this.trie.get(c);
        if (!propsHasException(props)) {
            if (getTypeFromProps(props) == 1) {
                result = c + getDelta(props);
            }
        } else {
            int excOffset = getExceptionsOffset(props);
            int excOffset2 = excOffset + 1;
            char c2 = this.exceptions[excOffset];
            if ((c2 & 16384) != 0) {
                int loc = getCaseLocale(locale, locCache);
                if (loc == 2 && c == 105) {
                    return 304;
                }
                if (loc == 3 && c == 775 && isPrecededBySoftDotted(iter)) {
                    return 0;
                }
                if (upperNotTitle && hasSlot(c2, 3)) {
                    index = 3;
                } else if (!hasSlot(c2, 2)) {
                    index = 2;
                } else {
                    return ~c;
                }
                result = getSlotValue(c2, index, excOffset2);
            } else {
                if (hasSlot(c2, 7)) {
                    long value = getSlotValueAndOffset(c2, 7, excOffset2);
                    int full2 = ((int) value) & 65535;
                    int excOffset3 = ((int) (value >> 32)) + 1 + (full2 & 15);
                    int full3 = full2 >> 4;
                    int excOffset4 = excOffset3 + (full3 & 15);
                    int full4 = full3 >> 4;
                    if (upperNotTitle) {
                        full = full4 & 15;
                    } else {
                        excOffset4 += full4 & 15;
                        full = (full4 >> 4) & 15;
                    }
                    if (full != 0) {
                        out.append(this.exceptions, excOffset4, full);
                        return full;
                    }
                }
                if (upperNotTitle) {
                    if (!hasSlot(c2, 2)) {
                    }
                }
            }
        }
        return result == c ? ~result : result;
    }

    public final int toFullUpper(int c, ContextIterator iter, StringBuilder out, ULocale locale, int[] locCache) {
        return toUpperOrTitle(c, iter, out, locale, locCache, true);
    }

    public final int toFullTitle(int c, ContextIterator iter, StringBuilder out, ULocale locale, int[] locCache) {
        return toUpperOrTitle(c, iter, out, locale, locCache, false);
    }

    public final int fold(int c, int options) {
        int index;
        int props = this.trie.get(c);
        if (!propsHasException(props)) {
            if (getTypeFromProps(props) >= 2) {
                return c + getDelta(props);
            }
            return c;
        }
        int excOffset = getExceptionsOffset(props);
        int excOffset2 = excOffset + 1;
        char c2 = this.exceptions[excOffset];
        if ((32768 & c2) != 0) {
            if ((options & 255) == 0) {
                if (c == 73) {
                    return 105;
                }
                if (c == 304) {
                    return c;
                }
            } else {
                if (c == 73) {
                    return 305;
                }
                if (c == 304) {
                    return 105;
                }
            }
        }
        if (hasSlot(c2, 1)) {
            index = 1;
        } else if (hasSlot(c2, 0)) {
            index = 0;
        } else {
            return c;
        }
        return getSlotValue(c2, index, excOffset2);
    }

    public final int toFullFolding(int c, StringBuilder out, int options) {
        int index;
        int result = c;
        int props = this.trie.get(c);
        if (!propsHasException(props)) {
            if (getTypeFromProps(props) >= 2) {
                result = c + getDelta(props);
            }
        } else {
            int excOffset = getExceptionsOffset(props);
            int excOffset2 = excOffset + 1;
            char c2 = this.exceptions[excOffset];
            if ((32768 & c2) != 0) {
                if ((options & 255) == 0) {
                    if (c == 73) {
                        return 105;
                    }
                    if (c == 304) {
                        out.append(iDot);
                        return 2;
                    }
                } else {
                    if (c == 73) {
                        return 305;
                    }
                    if (c == 304) {
                        return 105;
                    }
                }
            } else {
                if (hasSlot(c2, 7)) {
                    long value = getSlotValueAndOffset(c2, 7, excOffset2);
                    int full = ((int) value) & 65535;
                    int excOffset3 = ((int) (value >> 32)) + 1 + (full & 15);
                    int full2 = (full >> 4) & 15;
                    if (full2 != 0) {
                        out.append(this.exceptions, excOffset3, full2);
                        return full2;
                    }
                }
                if (!hasSlot(c2, 1)) {
                    index = 1;
                } else if (hasSlot(c2, 0)) {
                    index = 0;
                } else {
                    return ~c;
                }
                result = getSlotValue(c2, index, excOffset2);
            }
            if (!hasSlot(c2, 1)) {
            }
            result = getSlotValue(c2, index, excOffset2);
        }
        return result == c ? ~result : result;
    }

    public final boolean hasBinaryProperty(int c, int which) {
        switch (which) {
            case 22:
                return 1 == getType(c);
            case 27:
                return isSoftDotted(c);
            case 30:
                return 2 == getType(c);
            case 34:
                return isCaseSensitive(c);
            case 49:
                return getType(c) != 0;
            case 50:
                return (getTypeOrIgnorable(c) >> 2) != 0;
            case 51:
                dummyStringBuilder.setLength(0);
                return toFullLower(c, null, dummyStringBuilder, ULocale.ROOT, rootLocCache) >= 0;
            case 52:
                dummyStringBuilder.setLength(0);
                return toFullUpper(c, null, dummyStringBuilder, ULocale.ROOT, rootLocCache) >= 0;
            case 53:
                dummyStringBuilder.setLength(0);
                return toFullTitle(c, null, dummyStringBuilder, ULocale.ROOT, rootLocCache) >= 0;
            case 55:
                dummyStringBuilder.setLength(0);
                return toFullLower(c, null, dummyStringBuilder, ULocale.ROOT, rootLocCache) >= 0 || toFullUpper(c, null, dummyStringBuilder, ULocale.ROOT, rootLocCache) >= 0 || toFullTitle(c, null, dummyStringBuilder, ULocale.ROOT, rootLocCache) >= 0;
            default:
                return false;
        }
    }

    private static final int getTypeFromProps(int props) {
        return props & 3;
    }

    private static final int getTypeAndIgnorableFromProps(int props) {
        return props & 7;
    }

    private static final int getDelta(int props) {
        return ((short) props) >> 7;
    }
}
