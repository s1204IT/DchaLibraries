package android.icu.impl.number;

import android.icu.impl.PatternTokenizer;
import android.icu.lang.UCharacter;
import android.icu.text.NumberFormat;

public class AffixUtils {
    static final boolean $assertionsDisabled = false;
    private static final int STATE_AFTER_QUOTE = 3;
    private static final int STATE_BASE = 0;
    private static final int STATE_FIFTH_CURR = 8;
    private static final int STATE_FIRST_CURR = 4;
    private static final int STATE_FIRST_QUOTE = 1;
    private static final int STATE_FOURTH_CURR = 7;
    private static final int STATE_INSIDE_QUOTE = 2;
    private static final int STATE_OVERFLOW_CURR = 9;
    private static final int STATE_SECOND_CURR = 5;
    private static final int STATE_THIRD_CURR = 6;
    private static final int TYPE_CODEPOINT = 0;
    public static final int TYPE_CURRENCY_DOUBLE = -6;
    public static final int TYPE_CURRENCY_OVERFLOW = -15;
    public static final int TYPE_CURRENCY_QUAD = -8;
    public static final int TYPE_CURRENCY_QUINT = -9;
    public static final int TYPE_CURRENCY_SINGLE = -5;
    public static final int TYPE_CURRENCY_TRIPLE = -7;
    public static final int TYPE_MINUS_SIGN = -1;
    public static final int TYPE_PERCENT = -3;
    public static final int TYPE_PERMILLE = -4;
    public static final int TYPE_PLUS_SIGN = -2;

    public interface SymbolProvider {
        CharSequence getSymbol(int i);
    }

    public static int estimateLength(CharSequence charSequence) {
        if (charSequence == null) {
            return 0;
        }
        int iCharCount = 0;
        char c = 0;
        int i = 0;
        while (iCharCount < charSequence.length()) {
            int iCodePointAt = Character.codePointAt(charSequence, iCharCount);
            switch (c) {
                case 0:
                    if (iCodePointAt == 39) {
                        c = 1;
                    } else {
                        i++;
                        continue;
                    }
                    iCharCount += Character.charCount(iCodePointAt);
                    break;
                case 1:
                    if (iCodePointAt != 39) {
                        i++;
                    } else {
                        i++;
                        c = 0;
                        iCharCount += Character.charCount(iCodePointAt);
                    }
                    break;
                case 2:
                    if (iCodePointAt == 39) {
                        c = 3;
                        continue;
                    } else {
                        i++;
                    }
                    iCharCount += Character.charCount(iCodePointAt);
                    break;
                case 3:
                    if (iCodePointAt != 39) {
                        i++;
                        iCharCount += Character.charCount(iCodePointAt);
                    } else {
                        i++;
                    }
                    break;
                default:
                    throw new AssertionError();
            }
            c = 2;
            iCharCount += Character.charCount(iCodePointAt);
        }
        switch (c) {
            case 1:
            case 2:
                throw new IllegalArgumentException("Unterminated quote: \"" + ((Object) charSequence) + "\"");
            default:
                return i;
        }
    }

    public static int escape(CharSequence charSequence, StringBuilder sb) {
        if (charSequence == null) {
            return 0;
        }
        int length = sb.length();
        int iCharCount = 0;
        char c = 0;
        while (iCharCount < charSequence.length()) {
            int iCodePointAt = Character.codePointAt(charSequence, iCharCount);
            if (iCodePointAt == 37) {
                if (c == 0) {
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                    sb.appendCodePoint(iCodePointAt);
                    c = 2;
                } else {
                    sb.appendCodePoint(iCodePointAt);
                }
            } else if (iCodePointAt == 39) {
                sb.append("''");
            } else if (iCodePointAt != 43 && iCodePointAt != 45 && iCodePointAt != 164 && iCodePointAt != 8240) {
                if (c == 2) {
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                    sb.appendCodePoint(iCodePointAt);
                    c = 0;
                } else {
                    sb.appendCodePoint(iCodePointAt);
                }
            }
            iCharCount += Character.charCount(iCodePointAt);
        }
        if (c == 2) {
            sb.append(PatternTokenizer.SINGLE_QUOTE);
        }
        return sb.length() - length;
    }

    public static String escape(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        escape(charSequence, sb);
        return sb.toString();
    }

    public static final NumberFormat.Field getFieldForType(int i) {
        if (i != -15) {
            switch (i) {
                case TYPE_CURRENCY_QUINT:
                    return NumberFormat.Field.CURRENCY;
                case TYPE_CURRENCY_QUAD:
                    return NumberFormat.Field.CURRENCY;
                case TYPE_CURRENCY_TRIPLE:
                    return NumberFormat.Field.CURRENCY;
                case TYPE_CURRENCY_DOUBLE:
                    return NumberFormat.Field.CURRENCY;
                case TYPE_CURRENCY_SINGLE:
                    return NumberFormat.Field.CURRENCY;
                case TYPE_PERMILLE:
                    return NumberFormat.Field.PERMILLE;
                case TYPE_PERCENT:
                    return NumberFormat.Field.PERCENT;
                case -2:
                    return NumberFormat.Field.SIGN;
                case -1:
                    return NumberFormat.Field.SIGN;
                default:
                    throw new AssertionError();
            }
        }
        return NumberFormat.Field.CURRENCY;
    }

    public static int unescape(CharSequence charSequence, NumberStringBuilder numberStringBuilder, int i, SymbolProvider symbolProvider) {
        int iInsertCodePoint = 0;
        long jNextToken = 0;
        while (hasNext(jNextToken, charSequence)) {
            jNextToken = nextToken(jNextToken, charSequence);
            int typeOrCp = getTypeOrCp(jNextToken);
            if (typeOrCp == -15) {
                iInsertCodePoint += numberStringBuilder.insertCodePoint(i + iInsertCodePoint, UCharacter.REPLACEMENT_CHAR, NumberFormat.Field.CURRENCY);
            } else if (typeOrCp < 0) {
                iInsertCodePoint += numberStringBuilder.insert(i + iInsertCodePoint, symbolProvider.getSymbol(typeOrCp), getFieldForType(typeOrCp));
            } else {
                iInsertCodePoint += numberStringBuilder.insertCodePoint(i + iInsertCodePoint, typeOrCp, null);
            }
        }
        return iInsertCodePoint;
    }

    public static int unescapedCodePointCount(CharSequence charSequence, SymbolProvider symbolProvider) {
        long jNextToken = 0;
        int iCodePointCount = 0;
        while (hasNext(jNextToken, charSequence)) {
            jNextToken = nextToken(jNextToken, charSequence);
            int typeOrCp = getTypeOrCp(jNextToken);
            if (typeOrCp == -15) {
                iCodePointCount++;
            } else if (typeOrCp < 0) {
                CharSequence symbol = symbolProvider.getSymbol(typeOrCp);
                iCodePointCount += Character.codePointCount(symbol, 0, symbol.length());
            } else {
                iCodePointCount++;
            }
        }
        return iCodePointCount;
    }

    public static boolean containsType(CharSequence charSequence, int i) {
        if (charSequence == null || charSequence.length() == 0) {
            return false;
        }
        long jNextToken = 0;
        while (hasNext(jNextToken, charSequence)) {
            jNextToken = nextToken(jNextToken, charSequence);
            if (getTypeOrCp(jNextToken) == i) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasCurrencySymbols(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) {
            return false;
        }
        long jNextToken = 0;
        while (hasNext(jNextToken, charSequence)) {
            jNextToken = nextToken(jNextToken, charSequence);
            int typeOrCp = getTypeOrCp(jNextToken);
            if (typeOrCp < 0 && getFieldForType(typeOrCp) == NumberFormat.Field.CURRENCY) {
                return true;
            }
        }
        return false;
    }

    public static String replaceType(CharSequence charSequence, int i, char c) {
        if (charSequence == null || charSequence.length() == 0) {
            return "";
        }
        char[] charArray = charSequence.toString().toCharArray();
        long jNextToken = 0;
        while (hasNext(jNextToken, charSequence)) {
            jNextToken = nextToken(jNextToken, charSequence);
            if (getTypeOrCp(jNextToken) == i) {
                charArray[getOffset(jNextToken) - 1] = c;
            }
        }
        return new String(charArray);
    }

    public static long nextToken(long j, CharSequence charSequence) {
        int offset = getOffset(j);
        int state = getState(j);
        while (offset < charSequence.length()) {
            int iCodePointAt = Character.codePointAt(charSequence, offset);
            int iCharCount = Character.charCount(iCodePointAt);
            switch (state) {
                case 0:
                    if (iCodePointAt == 37) {
                        return makeTag(offset + iCharCount, -3, 0, 0);
                    }
                    if (iCodePointAt == 39) {
                        state = 1;
                        offset += iCharCount;
                    } else {
                        if (iCodePointAt == 43) {
                            return makeTag(offset + iCharCount, -2, 0, 0);
                        }
                        if (iCodePointAt == 45) {
                            return makeTag(offset + iCharCount, -1, 0, 0);
                        }
                        if (iCodePointAt == 164) {
                            state = 4;
                            offset += iCharCount;
                        } else {
                            if (iCodePointAt == 8240) {
                                return makeTag(offset + iCharCount, -4, 0, 0);
                            }
                            return makeTag(offset + iCharCount, 0, 0, iCodePointAt);
                        }
                    }
                    break;
                    break;
                case 1:
                    if (iCodePointAt != 39) {
                        return makeTag(offset + iCharCount, 0, 2, iCodePointAt);
                    }
                    return makeTag(offset + iCharCount, 0, 0, iCodePointAt);
                case 2:
                    if (iCodePointAt != 39) {
                        return makeTag(offset + iCharCount, 0, 2, iCodePointAt);
                    }
                    state = 3;
                    offset += iCharCount;
                    break;
                    break;
                case 3:
                    if (iCodePointAt == 39) {
                        return makeTag(offset + iCharCount, 0, 2, iCodePointAt);
                    }
                    state = 0;
                    break;
                    break;
                case 4:
                    if (iCodePointAt == 164) {
                        state = 5;
                        offset += iCharCount;
                    } else {
                        return makeTag(offset, -5, 0, 0);
                    }
                    break;
                case 5:
                    if (iCodePointAt == 164) {
                        state = 6;
                        offset += iCharCount;
                    } else {
                        return makeTag(offset, -6, 0, 0);
                    }
                    break;
                case 6:
                    if (iCodePointAt == 164) {
                        state = 7;
                        offset += iCharCount;
                    } else {
                        return makeTag(offset, -7, 0, 0);
                    }
                    break;
                case 7:
                    if (iCodePointAt == 164) {
                        state = 8;
                        offset += iCharCount;
                    } else {
                        return makeTag(offset, -8, 0, 0);
                    }
                    break;
                case 8:
                    if (iCodePointAt == 164) {
                        state = 9;
                        offset += iCharCount;
                    } else {
                        return makeTag(offset, -9, 0, 0);
                    }
                    break;
                case 9:
                    if (iCodePointAt == 164) {
                        offset += iCharCount;
                    } else {
                        return makeTag(offset, -15, 0, 0);
                    }
                    break;
                default:
                    throw new AssertionError();
            }
        }
        switch (state) {
            case 0:
                return -1L;
            case 1:
            case 2:
                throw new IllegalArgumentException("Unterminated quote in pattern affix: \"" + ((Object) charSequence) + "\"");
            case 3:
                return -1L;
            case 4:
                return makeTag(offset, -5, 0, 0);
            case 5:
                return makeTag(offset, -6, 0, 0);
            case 6:
                return makeTag(offset, -7, 0, 0);
            case 7:
                return makeTag(offset, -8, 0, 0);
            case 8:
                return makeTag(offset, -9, 0, 0);
            case 9:
                return makeTag(offset, -15, 0, 0);
            default:
                throw new AssertionError();
        }
    }

    public static boolean hasNext(long j, CharSequence charSequence) {
        int state = getState(j);
        int offset = getOffset(j);
        if (state == 2 && offset == charSequence.length() - 1 && charSequence.charAt(offset) == '\'') {
            return false;
        }
        return state != 0 || offset < charSequence.length();
    }

    public static int getTypeOrCp(long j) {
        int type = getType(j);
        return type == 0 ? getCodePoint(j) : -type;
    }

    private static long makeTag(int i, int i2, int i3, int i4) {
        return ((-i2) << 32) | ((long) i) | 0 | (((long) i3) << 36) | (((long) i4) << 40);
    }

    static int getOffset(long j) {
        return (int) (j & (-1));
    }

    static int getType(long j) {
        return (int) ((j >>> 32) & 15);
    }

    static int getState(long j) {
        return (int) ((j >>> 36) & 15);
    }

    static int getCodePoint(long j) {
        return (int) (j >>> 40);
    }
}
