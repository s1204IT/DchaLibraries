package android.icu.text;

import android.icu.impl.Utility;
import android.icu.text.RuleBasedTransliterator;
import android.icu.text.Transliterator;

class TransliterationRule {
    static final int ANCHOR_END = 2;
    static final int ANCHOR_START = 1;
    private StringMatcher anteContext;
    private int anteContextLength;
    private final RuleBasedTransliterator.Data data;
    byte flags;
    private StringMatcher key;
    private int keyLength;
    private UnicodeReplacer output;
    private String pattern;
    private StringMatcher postContext;
    UnicodeMatcher[] segments;

    public TransliterationRule(String str, int i, int i2, String str2, int i3, int i4, UnicodeMatcher[] unicodeMatcherArr, boolean z, boolean z2, RuleBasedTransliterator.Data data) {
        this.data = data;
        if (i < 0) {
            this.anteContextLength = 0;
        } else {
            if (i > str.length()) {
                throw new IllegalArgumentException("Invalid ante context");
            }
            this.anteContextLength = i;
        }
        if (i2 < 0) {
            this.keyLength = str.length() - this.anteContextLength;
        } else {
            if (i2 < this.anteContextLength || i2 > str.length()) {
                throw new IllegalArgumentException("Invalid post context");
            }
            this.keyLength = i2 - this.anteContextLength;
        }
        if (i3 < 0) {
            i3 = str2.length();
        } else if (i3 > str2.length()) {
            throw new IllegalArgumentException("Invalid cursor position");
        }
        this.segments = unicodeMatcherArr;
        this.pattern = str;
        this.flags = (byte) 0;
        if (z) {
            this.flags = (byte) (this.flags | 1);
        }
        if (z2) {
            this.flags = (byte) (this.flags | 2);
        }
        this.anteContext = null;
        if (this.anteContextLength > 0) {
            this.anteContext = new StringMatcher(this.pattern.substring(0, this.anteContextLength), 0, this.data);
        }
        this.key = null;
        if (this.keyLength > 0) {
            this.key = new StringMatcher(this.pattern.substring(this.anteContextLength, this.anteContextLength + this.keyLength), 0, this.data);
        }
        int length = (this.pattern.length() - this.keyLength) - this.anteContextLength;
        this.postContext = null;
        if (length > 0) {
            this.postContext = new StringMatcher(this.pattern.substring(this.anteContextLength + this.keyLength), 0, this.data);
        }
        this.output = new StringReplacer(str2, i3 + i4, this.data);
    }

    public int getAnteContextLength() {
        return this.anteContextLength + ((this.flags & 1) == 0 ? 0 : 1);
    }

    final int getIndexValue() {
        if (this.anteContextLength == this.pattern.length()) {
            return -1;
        }
        int iCharAt = UTF16.charAt(this.pattern, this.anteContextLength);
        if (this.data.lookupMatcher(iCharAt) == null) {
            return iCharAt & 255;
        }
        return -1;
    }

    final boolean matchesIndexValue(int i) {
        StringMatcher stringMatcher = this.key != null ? this.key : this.postContext;
        if (stringMatcher != null) {
            return stringMatcher.matchesIndexValue(i);
        }
        return true;
    }

    public boolean masks(TransliterationRule transliterationRule) {
        int length = this.pattern.length();
        int i = this.anteContextLength;
        int i2 = transliterationRule.anteContextLength;
        int length2 = this.pattern.length() - i;
        int length3 = transliterationRule.pattern.length() - i2;
        if (i != i2 || length2 != length3 || this.keyLength > transliterationRule.keyLength || !transliterationRule.pattern.regionMatches(0, this.pattern, 0, length)) {
            return i <= i2 && (length2 < length3 || (length2 == length3 && this.keyLength <= transliterationRule.keyLength)) && transliterationRule.pattern.regionMatches(i2 - i, this.pattern, 0, length);
        }
        if (this.flags != transliterationRule.flags) {
            if ((this.flags & 1) == 0 && (this.flags & 2) == 0) {
                return true;
            }
            return ((transliterationRule.flags & 1) == 0 || (transliterationRule.flags & 2) == 0) ? false : true;
        }
        return true;
    }

    static final int posBefore(Replaceable replaceable, int i) {
        if (i > 0) {
            return i - UTF16.getCharCount(replaceable.char32At(i - 1));
        }
        return i - 1;
    }

    static final int posAfter(Replaceable replaceable, int i) {
        if (i >= 0 && i < replaceable.length()) {
            return i + UTF16.getCharCount(replaceable.char32At(i));
        }
        return i + 1;
    }

    public int matchAndReplace(Replaceable replaceable, Transliterator.Position position, boolean z) {
        int iMatches;
        if (this.segments != null) {
            for (int i = 0; i < this.segments.length; i++) {
                ((StringMatcher) this.segments[i]).resetMatch();
            }
        }
        int iPosBefore = posBefore(replaceable, position.contextStart);
        int[] iArr = {posBefore(replaceable, position.start)};
        if (this.anteContext != null && this.anteContext.matches(replaceable, iArr, iPosBefore, false) != 2) {
            return 0;
        }
        int i2 = iArr[0];
        int iPosAfter = posAfter(replaceable, i2);
        if ((this.flags & 1) != 0 && i2 != iPosBefore) {
            return 0;
        }
        iArr[0] = position.start;
        if (this.key != null && (iMatches = this.key.matches(replaceable, iArr, position.limit, z)) != 2) {
            return iMatches;
        }
        int i3 = iArr[0];
        if (this.postContext != null) {
            if (z && i3 == position.limit) {
                return 1;
            }
            int iMatches2 = this.postContext.matches(replaceable, iArr, position.contextLimit, z);
            if (iMatches2 != 2) {
                return iMatches2;
            }
        }
        int i4 = iArr[0];
        if ((this.flags & 2) != 0) {
            if (i4 != position.contextLimit) {
                return 0;
            }
            if (z) {
                return 1;
            }
        }
        int iReplace = this.output.replace(replaceable, position.start, i3, iArr) - (i3 - position.start);
        int i5 = iArr[0];
        position.limit += iReplace;
        position.contextLimit += iReplace;
        position.start = Math.max(iPosAfter, Math.min(Math.min(i4 + iReplace, position.limit), i5));
        return 2;
    }

    public String toRule(boolean z) {
        StringBuffer stringBuffer = new StringBuffer();
        StringBuffer stringBuffer2 = new StringBuffer();
        boolean z2 = (this.anteContext == null && this.postContext == null) ? false : true;
        if ((this.flags & 1) != 0) {
            stringBuffer.append('^');
        }
        Utility.appendToRule(stringBuffer, this.anteContext, z, stringBuffer2);
        if (z2) {
            Utility.appendToRule(stringBuffer, 123, true, z, stringBuffer2);
        }
        Utility.appendToRule(stringBuffer, this.key, z, stringBuffer2);
        if (z2) {
            Utility.appendToRule(stringBuffer, 125, true, z, stringBuffer2);
        }
        Utility.appendToRule(stringBuffer, this.postContext, z, stringBuffer2);
        if ((this.flags & 2) != 0) {
            stringBuffer.append(SymbolTable.SYMBOL_REF);
        }
        Utility.appendToRule(stringBuffer, " > ", true, z, stringBuffer2);
        Utility.appendToRule(stringBuffer, this.output.toReplacerPattern(z), true, z, stringBuffer2);
        Utility.appendToRule(stringBuffer, 59, true, z, stringBuffer2);
        return stringBuffer.toString();
    }

    public String toString() {
        return '{' + toRule(true) + '}';
    }

    void addSourceTargetSet(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3, UnicodeSet unicodeSet4) {
        int i = this.anteContextLength + this.keyLength;
        UnicodeSet unicodeSet5 = new UnicodeSet();
        UnicodeSet unicodeSet6 = new UnicodeSet();
        int charCount = this.anteContextLength;
        while (charCount < i) {
            int iCharAt = UTF16.charAt(this.pattern, charCount);
            charCount += UTF16.getCharCount(iCharAt);
            UnicodeMatcher unicodeMatcherLookupMatcher = this.data.lookupMatcher(iCharAt);
            if (unicodeMatcherLookupMatcher == null) {
                if (!unicodeSet.contains(iCharAt)) {
                    return;
                } else {
                    unicodeSet5.add(iCharAt);
                }
            } else {
                try {
                    if (!unicodeSet.containsSome((UnicodeSet) unicodeMatcherLookupMatcher)) {
                        return;
                    } else {
                        unicodeMatcherLookupMatcher.addMatchSetTo(unicodeSet5);
                    }
                } catch (ClassCastException e) {
                    unicodeSet6.clear();
                    unicodeMatcherLookupMatcher.addMatchSetTo(unicodeSet6);
                    if (!unicodeSet.containsSome(unicodeSet6)) {
                        return;
                    } else {
                        unicodeSet5.addAll(unicodeSet6);
                    }
                }
            }
        }
        unicodeSet2.addAll(unicodeSet5);
        this.output.addReplacementSetTo(unicodeSet3);
    }
}
