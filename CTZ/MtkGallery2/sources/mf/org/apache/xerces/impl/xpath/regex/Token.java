package mf.org.apache.xerces.impl.xpath.regex;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;
import mf.org.apache.xerces.impl.xpath.XPath;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

class Token implements Serializable {
    static final int ANCHOR = 8;
    static final int BACKREFERENCE = 12;
    static final int CHAR = 0;
    static final int CHAR_FINAL_QUOTE = 30;
    static final int CHAR_INIT_QUOTE = 29;
    static final int CHAR_LETTER = 31;
    static final int CHAR_MARK = 32;
    static final int CHAR_NUMBER = 33;
    static final int CHAR_OTHER = 35;
    static final int CHAR_PUNCTUATION = 36;
    static final int CHAR_SEPARATOR = 34;
    static final int CHAR_SYMBOL = 37;
    static final int CLOSURE = 3;
    static final int CONCAT = 1;
    static final int CONDITION = 26;
    static final boolean COUNTTOKENS = true;
    static final int DOT = 11;
    static final int EMPTY = 7;
    static final int FC_ANY = 2;
    static final int FC_CONTINUE = 0;
    static final int FC_TERMINAL = 1;
    static final int INDEPENDENT = 24;
    static final int LOOKAHEAD = 20;
    static final int LOOKBEHIND = 22;
    static final int MODIFIERGROUP = 25;
    static final int NEGATIVELOOKAHEAD = 21;
    static final int NEGATIVELOOKBEHIND = 23;
    private static final int NONBMP_BLOCK_START = 84;
    static final int NONGREEDYCLOSURE = 9;
    static final int NRANGE = 5;
    static final int PAREN = 6;
    static final int RANGE = 4;
    static final int STRING = 10;
    static final int UNION = 2;
    static final int UTF16_MAX = 1114111;
    private static final String[] blockNames;
    static final String blockRanges = "\u0000\u007f\u0080ÿĀſƀɏɐʯʰ˿̀ͯͰϿЀӿ\u0530֏\u0590\u05ff\u0600ۿ܀ݏހ\u07bfऀॿঀ\u09ff\u0a00\u0a7f\u0a80૿\u0b00\u0b7f\u0b80\u0bffఀ౿ಀ\u0cffഀൿ\u0d80\u0dff\u0e00\u0e7f\u0e80\u0effༀ\u0fffက႟Ⴀჿᄀᇿሀ\u137fᎠ\u13ff᐀ᙿ\u1680\u169fᚠ\u16ffក\u17ff᠀\u18afḀỿἀ\u1fff\u2000\u206f⁰\u209f₠\u20cf⃐\u20ff℀⅏⅐\u218f←⇿∀⋿⌀⏿␀\u243f⑀\u245f①⓿─╿▀▟■◿☀⛿✀➿⠀⣿⺀\u2eff⼀\u2fdf⿰\u2fff\u3000〿\u3040ゟ゠ヿ\u3100ㄯ\u3130\u318f㆐㆟ㆠㆿ㈀㋿㌀㏿㐀䶵一鿿ꀀ\ua48f꒐\ua4cf가힣\ue000\uf8ff豈\ufaffﬀﭏﭐ﷿︠︯︰﹏﹐\ufe6fﹰ\ufefe\ufeff\ufeff\uff00\uffef";
    private static final Hashtable categories;
    private static final Hashtable categories2;
    private static final String[] categoryNames;
    static final int[] nonBMPBlockRanges;
    static Hashtable nonxs = null;
    private static final long serialVersionUID = 8484976002585487481L;
    private static Token token_ccs = null;
    private static Token token_grapheme = null;
    static Token token_not_0to9 = null;
    static Token token_not_spaces = null;
    static Token token_not_wordchars = null;
    static Token token_spaces = null;
    static Token token_wordchars = null;
    static final String viramaString = "्্੍્୍்్್്ฺ྄";
    final int type;
    static int tokens = 0;
    static Token token_empty = new Token(7);
    static Token token_linebeginning = createAnchor(94);
    static Token token_linebeginning2 = createAnchor(64);
    static Token token_lineend = createAnchor(36);
    static Token token_stringbeginning = createAnchor(65);
    static Token token_stringend = createAnchor(122);
    static Token token_stringend2 = createAnchor(90);
    static Token token_wordedge = createAnchor(98);
    static Token token_not_wordedge = createAnchor(66);
    static Token token_wordbeginning = createAnchor(60);
    static Token token_wordend = createAnchor(62);
    static Token token_dot = new Token(11);
    static Token token_0to9 = createRange();

    static {
        token_0to9.addRange(48, 57);
        token_wordchars = createRange();
        token_wordchars.addRange(48, 57);
        token_wordchars.addRange(65, 90);
        token_wordchars.addRange(95, 95);
        token_wordchars.addRange(97, 122);
        token_spaces = createRange();
        token_spaces.addRange(9, 9);
        token_spaces.addRange(10, 10);
        token_spaces.addRange(12, 12);
        token_spaces.addRange(13, 13);
        token_spaces.addRange(32, 32);
        token_not_0to9 = complementRanges(token_0to9);
        token_not_wordchars = complementRanges(token_wordchars);
        token_not_spaces = complementRanges(token_spaces);
        categories = new Hashtable();
        categories2 = new Hashtable();
        String[] strArr = new String[38];
        strArr[0] = "Cn";
        strArr[1] = "Lu";
        strArr[2] = "Ll";
        strArr[3] = "Lt";
        strArr[4] = "Lm";
        strArr[5] = "Lo";
        strArr[6] = "Mn";
        strArr[7] = "Me";
        strArr[8] = "Mc";
        strArr[9] = "Nd";
        strArr[10] = "Nl";
        strArr[11] = "No";
        strArr[12] = "Zs";
        strArr[13] = "Zl";
        strArr[14] = "Zp";
        strArr[15] = "Cc";
        strArr[16] = "Cf";
        strArr[18] = "Co";
        strArr[19] = "Cs";
        strArr[20] = "Pd";
        strArr[21] = "Ps";
        strArr[22] = "Pe";
        strArr[23] = "Pc";
        strArr[24] = "Po";
        strArr[25] = "Sm";
        strArr[26] = "Sc";
        strArr[27] = "Sk";
        strArr[28] = "So";
        strArr[29] = "Pi";
        strArr[30] = "Pf";
        strArr[31] = "L";
        strArr[32] = "M";
        strArr[33] = "N";
        strArr[34] = "Z";
        strArr[35] = "C";
        strArr[36] = "P";
        strArr[37] = "S";
        categoryNames = strArr;
        blockNames = new String[]{"Basic Latin", "Latin-1 Supplement", "Latin Extended-A", "Latin Extended-B", "IPA Extensions", "Spacing Modifier Letters", "Combining Diacritical Marks", "Greek", "Cyrillic", "Armenian", "Hebrew", "Arabic", "Syriac", "Thaana", "Devanagari", "Bengali", "Gurmukhi", "Gujarati", "Oriya", "Tamil", "Telugu", "Kannada", "Malayalam", "Sinhala", "Thai", "Lao", "Tibetan", "Myanmar", "Georgian", "Hangul Jamo", "Ethiopic", "Cherokee", "Unified Canadian Aboriginal Syllabics", "Ogham", "Runic", "Khmer", "Mongolian", "Latin Extended Additional", "Greek Extended", "General Punctuation", "Superscripts and Subscripts", "Currency Symbols", "Combining Marks for Symbols", "Letterlike Symbols", "Number Forms", "Arrows", "Mathematical Operators", "Miscellaneous Technical", "Control Pictures", "Optical Character Recognition", "Enclosed Alphanumerics", "Box Drawing", "Block Elements", "Geometric Shapes", "Miscellaneous Symbols", "Dingbats", "Braille Patterns", "CJK Radicals Supplement", "Kangxi Radicals", "Ideographic Description Characters", "CJK Symbols and Punctuation", "Hiragana", "Katakana", "Bopomofo", "Hangul Compatibility Jamo", "Kanbun", "Bopomofo Extended", "Enclosed CJK Letters and Months", "CJK Compatibility", "CJK Unified Ideographs Extension A", "CJK Unified Ideographs", "Yi Syllables", "Yi Radicals", "Hangul Syllables", "Private Use", "CJK Compatibility Ideographs", "Alphabetic Presentation Forms", "Arabic Presentation Forms-A", "Combining Half Marks", "CJK Compatibility Forms", "Small Form Variants", "Arabic Presentation Forms-B", "Specials", "Halfwidth and Fullwidth Forms", "Old Italic", "Gothic", "Deseret", "Byzantine Musical Symbols", "Musical Symbols", "Mathematical Alphanumeric Symbols", "CJK Unified Ideographs Extension B", "CJK Compatibility Ideographs Supplement", "Tags"};
        nonBMPBlockRanges = new int[]{66304, 66351, 66352, 66383, 66560, 66639, 118784, 119039, 119040, 119295, 119808, 120831, 131072, 173782, 194560, 195103, 917504, 917631};
        nonxs = null;
        token_grapheme = null;
        token_ccs = null;
    }

    static ParenToken createLook(int type, Token child) {
        tokens++;
        return new ParenToken(type, child, 0);
    }

    static ParenToken createParen(Token child, int pnumber) {
        tokens++;
        return new ParenToken(6, child, pnumber);
    }

    static ClosureToken createClosure(Token tok) {
        tokens++;
        return new ClosureToken(3, tok);
    }

    static ClosureToken createNGClosure(Token tok) {
        tokens++;
        return new ClosureToken(9, tok);
    }

    static ConcatToken createConcat(Token tok1, Token tok2) {
        tokens++;
        return new ConcatToken(tok1, tok2);
    }

    static UnionToken createConcat() {
        tokens++;
        return new UnionToken(1);
    }

    static UnionToken createUnion() {
        tokens++;
        return new UnionToken(2);
    }

    static Token createEmpty() {
        return token_empty;
    }

    static RangeToken createRange() {
        tokens++;
        return new RangeToken(4);
    }

    static RangeToken createNRange() {
        tokens++;
        return new RangeToken(5);
    }

    static CharToken createChar(int ch) {
        tokens++;
        return new CharToken(0, ch);
    }

    private static CharToken createAnchor(int ch) {
        tokens++;
        return new CharToken(8, ch);
    }

    static StringToken createBackReference(int refno) {
        tokens++;
        return new StringToken(12, null, refno);
    }

    static StringToken createString(String str) {
        tokens++;
        return new StringToken(10, str, 0);
    }

    static ModifierToken createModifierGroup(Token child, int add, int mask) {
        tokens++;
        return new ModifierToken(child, add, mask);
    }

    static ConditionToken createCondition(int refno, Token condition, Token yespat, Token nopat) {
        tokens++;
        return new ConditionToken(refno, condition, yespat, nopat);
    }

    protected Token(int type) {
        this.type = type;
    }

    int size() {
        return 0;
    }

    Token getChild(int index) {
        return null;
    }

    void addChild(Token tok) {
        throw new RuntimeException("Not supported.");
    }

    protected void addRange(int start, int end) {
        throw new RuntimeException("Not supported.");
    }

    protected void sortRanges() {
        throw new RuntimeException("Not supported.");
    }

    protected void compactRanges() {
        throw new RuntimeException("Not supported.");
    }

    protected void mergeRanges(Token tok) {
        throw new RuntimeException("Not supported.");
    }

    protected void subtractRanges(Token tok) {
        throw new RuntimeException("Not supported.");
    }

    protected void intersectRanges(Token tok) {
        throw new RuntimeException("Not supported.");
    }

    static Token complementRanges(Token tok) {
        return RangeToken.complementRanges(tok);
    }

    void setMin(int min) {
    }

    void setMax(int max) {
    }

    int getMin() {
        return -1;
    }

    int getMax() {
        return -1;
    }

    int getReferenceNumber() {
        return 0;
    }

    String getString() {
        return null;
    }

    int getParenNumber() {
        return 0;
    }

    int getChar() {
        return -1;
    }

    public String toString() {
        return toString(0);
    }

    public String toString(int options) {
        return this.type == 11 ? "." : "";
    }

    final int getMinLength() {
        int i = this.type;
        switch (i) {
            case 0:
            case 4:
            case 5:
            case 11:
                return 1;
            case 1:
                int sum = 0;
                for (int i2 = 0; i2 < size(); i2++) {
                    sum += getChild(i2).getMinLength();
                }
                return sum;
            case 2:
                if (size() == 0) {
                    return 0;
                }
                int ret = getChild(0).getMinLength();
                for (int i3 = 1; i3 < size(); i3++) {
                    int min = getChild(i3).getMinLength();
                    if (min < ret) {
                        ret = min;
                    }
                }
                return ret;
            case 3:
            case 9:
                if (getMin() >= 0) {
                    return getMin() * getChild(0).getMinLength();
                }
                return 0;
            case 6:
                return getChild(0).getMinLength();
            case 7:
            case 8:
                return 0;
            case 10:
                return getString().length();
            case 12:
                return 0;
            default:
                switch (i) {
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                        return 0;
                    case 24:
                    case 25:
                        break;
                    case 26:
                        break;
                    default:
                        throw new RuntimeException("Token#getMinLength(): Invalid Type: " + this.type);
                }
                break;
        }
    }

    final int getMaxLength() {
        int i = this.type;
        switch (i) {
            case 0:
                return 1;
            case 1:
                int sum = 0;
                for (int i2 = 0; i2 < size(); i2++) {
                    int d = getChild(i2).getMaxLength();
                    if (d < 0) {
                        return -1;
                    }
                    sum += d;
                }
                return sum;
            case 2:
                if (size() == 0) {
                    return 0;
                }
                int ret = getChild(0).getMaxLength();
                for (int i3 = 1; ret >= 0 && i3 < size(); i3++) {
                    int max = getChild(i3).getMaxLength();
                    if (max < 0) {
                        return -1;
                    }
                    if (max > ret) {
                        ret = max;
                    }
                }
                return ret;
            case 3:
            case 9:
                if (getMax() >= 0) {
                    return getMax() * getChild(0).getMaxLength();
                }
                return -1;
            case 4:
            case 5:
            case 11:
                return 2;
            case 6:
                return getChild(0).getMaxLength();
            case 7:
            case 8:
                return 0;
            case 10:
                return getString().length();
            case 12:
                return -1;
            default:
                switch (i) {
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                        return 0;
                    case 24:
                    case 25:
                        break;
                    case 26:
                        break;
                    default:
                        throw new RuntimeException("Token#getMaxLength(): Invalid Type: " + this.type);
                }
                break;
        }
    }

    private static final boolean isSet(int options, int flag) {
        if ((options & flag) == flag) {
            return COUNTTOKENS;
        }
        return false;
    }

    final int analyzeFirstCharacter(RangeToken result, int options) {
        int i = this.type;
        switch (i) {
            case 0:
                int ch = getChar();
                result.addRange(ch, ch);
                if (ch < 65536 && isSet(options, 2)) {
                    int ch2 = Character.toUpperCase((char) ch);
                    result.addRange(ch2, ch2);
                    int ch3 = Character.toLowerCase((char) ch2);
                    result.addRange(ch3, ch3);
                }
                return 1;
            case 1:
                int ret = 0;
                for (int i2 = 0; i2 < size(); i2++) {
                    int iAnalyzeFirstCharacter = getChild(i2).analyzeFirstCharacter(result, options);
                    ret = iAnalyzeFirstCharacter;
                    if (iAnalyzeFirstCharacter != 0) {
                        return ret;
                    }
                }
                return ret;
            case 2:
                if (size() == 0) {
                    return 0;
                }
                int ret2 = 0;
                boolean hasEmpty = false;
                for (int i3 = 0; i3 < size() && (ret2 = getChild(i3).analyzeFirstCharacter(result, options)) != 2; i3++) {
                    if (ret2 == 0) {
                        hasEmpty = COUNTTOKENS;
                    }
                }
                if (hasEmpty) {
                    return 0;
                }
                return ret2;
            case 3:
            case 9:
                getChild(0).analyzeFirstCharacter(result, options);
                return 0;
            case 4:
                result.mergeRanges(this);
                return 1;
            case 5:
                result.mergeRanges(complementRanges(this));
                return 1;
            case 6:
                break;
            case 7:
            case 8:
                return 0;
            case 10:
                int cha = getString().charAt(0);
                if (REUtil.isHighSurrogate(cha) && getString().length() >= 2) {
                    int ch22 = getString().charAt(1);
                    if (REUtil.isLowSurrogate(ch22)) {
                        cha = REUtil.composeFromSurrogates(cha, ch22);
                    }
                }
                result.addRange(cha, cha);
                if (cha < 65536 && isSet(options, 2)) {
                    int cha2 = Character.toUpperCase((char) cha);
                    result.addRange(cha2, cha2);
                    int cha3 = Character.toLowerCase((char) cha2);
                    result.addRange(cha3, cha3);
                }
                return 1;
            case 11:
                return 2;
            case 12:
                result.addRange(0, UTF16_MAX);
                return 2;
            default:
                switch (i) {
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                        return 0;
                    case 24:
                        break;
                    case 25:
                        return getChild(0).analyzeFirstCharacter(result, (options | ((ModifierToken) this).getOptions()) & (~((ModifierToken) this).getOptionsMask()));
                    case 26:
                        int ret3 = getChild(0).analyzeFirstCharacter(result, options);
                        if (size() == 1) {
                            return 0;
                        }
                        if (ret3 == 2) {
                            return ret3;
                        }
                        int ret4 = getChild(1).analyzeFirstCharacter(result, options);
                        return ret4 == 2 ? ret4 : (ret3 == 0 || ret4 == 0) ? 0 : 1;
                    default:
                        throw new RuntimeException("Token#analyzeHeadCharacter(): Invalid Type: " + this.type);
                }
                break;
        }
        return getChild(0).analyzeFirstCharacter(result, options);
    }

    private final boolean isShorterThan(Token tok) {
        if (tok == null) {
            return false;
        }
        if (this.type != 10) {
            throw new RuntimeException("Internal Error: Illegal type: " + this.type);
        }
        int mylength = getString().length();
        if (tok.type != 10) {
            throw new RuntimeException("Internal Error: Illegal type: " + tok.type);
        }
        int otherlength = tok.getString().length();
        if (mylength < otherlength) {
            return COUNTTOKENS;
        }
        return false;
    }

    static class FixedStringContainer {
        Token token = null;
        int options = 0;

        FixedStringContainer() {
        }
    }

    final void findFixedString(FixedStringContainer container, int options) {
        int i = this.type;
        switch (i) {
            case 0:
                container.token = null;
                return;
            case 1:
                Token prevToken = null;
                int prevOptions = 0;
                for (int i2 = 0; i2 < size(); i2++) {
                    getChild(i2).findFixedString(container, options);
                    if (prevToken == null || prevToken.isShorterThan(container.token)) {
                        prevToken = container.token;
                        prevOptions = container.options;
                    }
                }
                container.token = prevToken;
                container.options = prevOptions;
                return;
            case 2:
            case 3:
            case 4:
            case 5:
            case 7:
            case 8:
            case 9:
            case 11:
            case 12:
                container.token = null;
                return;
            case 6:
                getChild(0).findFixedString(container, options);
                return;
            case 10:
                container.token = this;
                container.options = options;
                return;
            default:
                switch (i) {
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 26:
                        break;
                    case 24:
                        break;
                    case 25:
                        getChild(0).findFixedString(container, (options | ((ModifierToken) this).getOptions()) & (~((ModifierToken) this).getOptionsMask()));
                        return;
                    default:
                        throw new RuntimeException("Token#findFixedString(): Invalid Type: " + this.type);
                }
                break;
        }
    }

    boolean match(int ch) {
        throw new RuntimeException("NFAArrow#match(): Internal error: " + this.type);
    }

    protected static RangeToken getRange(String str, boolean z) {
        int i;
        if (categories.size() == 0) {
            synchronized (categories) {
                Token[] tokenArr = new Token[categoryNames.length];
                for (int i2 = 0; i2 < tokenArr.length; i2++) {
                    tokenArr[i2] = createRange();
                }
                for (int i3 = 0; i3 < 65536; i3++) {
                    int type = Character.getType((char) i3);
                    if (type == 21 || type == 22) {
                        if (i3 == 171 || i3 == 8216 || i3 == 8219 || i3 == 8220 || i3 == 8223 || i3 == 8249) {
                            type = 29;
                        }
                        if (i3 == 187 || i3 == 8217 || i3 == 8221 || i3 == 8250) {
                            type = 30;
                        }
                    }
                    tokenArr[type].addRange(i3, i3);
                    switch (type) {
                        case 0:
                        case 15:
                        case 16:
                        case 18:
                        case XPath.Tokens.EXPRTOKEN_OPERATOR_DIV:
                            i = 35;
                            break;
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            i = 31;
                            break;
                        case 6:
                        case 7:
                        case 8:
                            i = 32;
                            break;
                        case 9:
                        case 10:
                        case 11:
                            i = 33;
                            break;
                        case 12:
                        case 13:
                        case 14:
                            i = 34;
                            break;
                        case 17:
                        default:
                            throw new RuntimeException("mf.org.apache.xerces.utils.regex.Token#getRange(): Unknown Unicode category: " + type);
                        case 20:
                        case 21:
                        case 22:
                        case 23:
                        case 24:
                        case 29:
                        case 30:
                            i = 36;
                            break;
                        case 25:
                        case 26:
                        case XPath.Tokens.EXPRTOKEN_OPERATOR_NOT_EQUAL:
                        case XPath.Tokens.EXPRTOKEN_OPERATOR_LESS:
                            i = 37;
                            break;
                    }
                    tokenArr[i].addRange(i3, i3);
                }
                boolean z2 = false;
                tokenArr[0].addRange(65536, UTF16_MAX);
                int i4 = 0;
                while (i4 < tokenArr.length) {
                    boolean z3 = z2 ? 1 : 0;
                    if (categoryNames[i4] != null) {
                        if (i4 == 0) {
                            tokenArr[i4].addRange(65536, UTF16_MAX);
                        }
                        categories.put(categoryNames[i4], tokenArr[i4]);
                        categories2.put(categoryNames[i4], complementRanges(tokenArr[i4]));
                    }
                    i4++;
                    z2 = z3 ? 1 : 0;
                }
                StringBuffer stringBuffer = new StringBuffer(50);
                int i5 = 0;
                ?? r3 = z2;
                while (i5 < blockNames.length) {
                    RangeToken rangeTokenCreateRange = createRange();
                    if (i5 < NONBMP_BLOCK_START) {
                        int i6 = i5 * 2;
                        rangeTokenCreateRange.addRange(blockRanges.charAt(i6), blockRanges.charAt(i6 + 1));
                    } else {
                        int i7 = (i5 - 84) * 2;
                        rangeTokenCreateRange.addRange(nonBMPBlockRanges[i7], nonBMPBlockRanges[i7 + 1]);
                    }
                    String str2 = blockNames[i5];
                    if (str2.equals("Specials")) {
                        rangeTokenCreateRange.addRange(65520, 65533);
                    }
                    if (str2.equals("Private Use")) {
                        rangeTokenCreateRange.addRange(983040, 1048573);
                        rangeTokenCreateRange.addRange(1048576, 1114109);
                    }
                    categories.put(str2, rangeTokenCreateRange);
                    categories2.put(str2, complementRanges(rangeTokenCreateRange));
                    stringBuffer.setLength(0);
                    stringBuffer.append("Is");
                    if (str2.indexOf(32) >= 0) {
                        for (int i8 = 0; i8 < str2.length(); i8++) {
                            if (str2.charAt(i8) != ' ') {
                                stringBuffer.append(str2.charAt(i8));
                            }
                        }
                    } else {
                        stringBuffer.append(str2);
                    }
                    setAlias(stringBuffer.toString(), str2, COUNTTOKENS);
                    i5++;
                    r3 = 0;
                }
                setAlias("ASSIGNED", "Cn", r3);
                setAlias("UNASSIGNED", "Cn", COUNTTOKENS);
                RangeToken rangeTokenCreateRange2 = createRange();
                rangeTokenCreateRange2.addRange(r3, UTF16_MAX);
                categories.put("ALL", rangeTokenCreateRange2);
                categories2.put("ALL", complementRanges(rangeTokenCreateRange2));
                registerNonXS("ASSIGNED");
                registerNonXS("UNASSIGNED");
                registerNonXS("ALL");
                RangeToken rangeTokenCreateRange3 = createRange();
                rangeTokenCreateRange3.mergeRanges(tokenArr[1]);
                rangeTokenCreateRange3.mergeRanges(tokenArr[2]);
                rangeTokenCreateRange3.mergeRanges(tokenArr[5]);
                categories.put("IsAlpha", rangeTokenCreateRange3);
                categories2.put("IsAlpha", complementRanges(rangeTokenCreateRange3));
                registerNonXS("IsAlpha");
                RangeToken rangeTokenCreateRange4 = createRange();
                rangeTokenCreateRange4.mergeRanges(rangeTokenCreateRange3);
                rangeTokenCreateRange4.mergeRanges(tokenArr[9]);
                categories.put("IsAlnum", rangeTokenCreateRange4);
                categories2.put("IsAlnum", complementRanges(rangeTokenCreateRange4));
                registerNonXS("IsAlnum");
                RangeToken rangeTokenCreateRange5 = createRange();
                rangeTokenCreateRange5.mergeRanges(token_spaces);
                rangeTokenCreateRange5.mergeRanges(tokenArr[34]);
                categories.put("IsSpace", rangeTokenCreateRange5);
                categories2.put("IsSpace", complementRanges(rangeTokenCreateRange5));
                registerNonXS("IsSpace");
                RangeToken rangeTokenCreateRange6 = createRange();
                rangeTokenCreateRange6.mergeRanges(rangeTokenCreateRange4);
                rangeTokenCreateRange6.addRange(95, 95);
                categories.put("IsWord", rangeTokenCreateRange6);
                categories2.put("IsWord", complementRanges(rangeTokenCreateRange6));
                registerNonXS("IsWord");
                RangeToken rangeTokenCreateRange7 = createRange();
                rangeTokenCreateRange7.addRange(r3, 127);
                categories.put("IsASCII", rangeTokenCreateRange7);
                categories2.put("IsASCII", complementRanges(rangeTokenCreateRange7));
                registerNonXS("IsASCII");
                RangeToken rangeTokenCreateRange8 = createRange();
                rangeTokenCreateRange8.mergeRanges(tokenArr[35]);
                rangeTokenCreateRange8.addRange(32, 32);
                categories.put("IsGraph", complementRanges(rangeTokenCreateRange8));
                categories2.put("IsGraph", rangeTokenCreateRange8);
                registerNonXS("IsGraph");
                RangeToken rangeTokenCreateRange9 = createRange();
                rangeTokenCreateRange9.addRange(48, 57);
                rangeTokenCreateRange9.addRange(65, 70);
                rangeTokenCreateRange9.addRange(97, 102);
                categories.put("IsXDigit", complementRanges(rangeTokenCreateRange9));
                categories2.put("IsXDigit", rangeTokenCreateRange9);
                registerNonXS("IsXDigit");
                setAlias("IsDigit", "Nd", COUNTTOKENS);
                setAlias("IsUpper", "Lu", COUNTTOKENS);
                setAlias("IsLower", "Ll", COUNTTOKENS);
                setAlias("IsCntrl", "C", COUNTTOKENS);
                setAlias("IsPrint", "C", false);
                setAlias("IsPunct", "P", COUNTTOKENS);
                registerNonXS("IsDigit");
                registerNonXS("IsUpper");
                registerNonXS("IsLower");
                registerNonXS("IsCntrl");
                registerNonXS("IsPrint");
                registerNonXS("IsPunct");
                setAlias("alpha", "IsAlpha", COUNTTOKENS);
                setAlias("alnum", "IsAlnum", COUNTTOKENS);
                setAlias("ascii", "IsASCII", COUNTTOKENS);
                setAlias("cntrl", "IsCntrl", COUNTTOKENS);
                setAlias("digit", "IsDigit", COUNTTOKENS);
                setAlias("graph", "IsGraph", COUNTTOKENS);
                setAlias("lower", "IsLower", COUNTTOKENS);
                setAlias("print", "IsPrint", COUNTTOKENS);
                setAlias("punct", "IsPunct", COUNTTOKENS);
                setAlias("space", "IsSpace", COUNTTOKENS);
                setAlias("upper", "IsUpper", COUNTTOKENS);
                setAlias("word", "IsWord", COUNTTOKENS);
                setAlias("xdigit", "IsXDigit", COUNTTOKENS);
                registerNonXS("alpha");
                registerNonXS("alnum");
                registerNonXS("ascii");
                registerNonXS("cntrl");
                registerNonXS("digit");
                registerNonXS("graph");
                registerNonXS("lower");
                registerNonXS("print");
                registerNonXS("punct");
                registerNonXS("space");
                registerNonXS("upper");
                registerNonXS("word");
                registerNonXS("xdigit");
            }
        }
        return z ? (RangeToken) categories.get(str) : (RangeToken) categories2.get(str);
    }

    protected static RangeToken getRange(String name, boolean positive, boolean xs) {
        RangeToken range = getRange(name, positive);
        if (xs && range != null && isRegisterNonXS(name)) {
            return null;
        }
        return range;
    }

    protected static void registerNonXS(String name) {
        if (nonxs == null) {
            nonxs = new Hashtable();
        }
        nonxs.put(name, name);
    }

    protected static boolean isRegisterNonXS(String name) {
        if (nonxs == null) {
            return false;
        }
        return nonxs.containsKey(name);
    }

    private static void setAlias(String newName, String name, boolean positive) {
        Token t1 = (Token) categories.get(name);
        Token t2 = (Token) categories2.get(name);
        if (positive) {
            categories.put(newName, t1);
            categories2.put(newName, t2);
        } else {
            categories2.put(newName, t1);
            categories.put(newName, t2);
        }
    }

    static synchronized Token getGraphemePattern() {
        if (token_grapheme != null) {
            return token_grapheme;
        }
        Token base_char = createRange();
        base_char.mergeRanges(getRange("ASSIGNED", COUNTTOKENS));
        base_char.subtractRanges(getRange("M", COUNTTOKENS));
        base_char.subtractRanges(getRange("C", COUNTTOKENS));
        Token virama = createRange();
        for (int i = 0; i < viramaString.length(); i++) {
            virama.addRange(i, i);
        }
        Token combiner_wo_virama = createRange();
        combiner_wo_virama.mergeRanges(getRange("M", COUNTTOKENS));
        combiner_wo_virama.addRange(4448, 4607);
        combiner_wo_virama.addRange(65438, 65439);
        Token left = createUnion();
        left.addChild(base_char);
        left.addChild(token_empty);
        Token foo = createUnion();
        foo.addChild(createConcat(virama, getRange("L", COUNTTOKENS)));
        foo.addChild(combiner_wo_virama);
        token_grapheme = createConcat(left, createClosure(foo));
        return token_grapheme;
    }

    static synchronized Token getCombiningCharacterSequence() {
        if (token_ccs != null) {
            return token_ccs;
        }
        Token foo = createClosure(getRange("M", COUNTTOKENS));
        token_ccs = createConcat(getRange("M", false), foo);
        return token_ccs;
    }

    static class StringToken extends Token implements Serializable {
        private static final long serialVersionUID = -4614366944218504172L;
        final int refNumber;
        String string;

        StringToken(int type, String str, int n) {
            super(type);
            this.string = str;
            this.refNumber = n;
        }

        @Override
        int getReferenceNumber() {
            return this.refNumber;
        }

        @Override
        String getString() {
            return this.string;
        }

        @Override
        public String toString(int options) {
            if (this.type == 12) {
                return "\\" + this.refNumber;
            }
            return REUtil.quoteMeta(this.string);
        }
    }

    static class ConcatToken extends Token implements Serializable {
        private static final long serialVersionUID = 8717321425541346381L;
        final Token child;
        final Token child2;

        ConcatToken(Token t1, Token t2) {
            super(1);
            this.child = t1;
            this.child2 = t2;
        }

        @Override
        int size() {
            return 2;
        }

        @Override
        Token getChild(int index) {
            return index == 0 ? this.child : this.child2;
        }

        @Override
        public String toString(int options) {
            if (this.child2.type == 3 && this.child2.getChild(0) == this.child) {
                String ret = String.valueOf(this.child.toString(options)) + "+";
                return ret;
            }
            if (this.child2.type == 9 && this.child2.getChild(0) == this.child) {
                String ret2 = String.valueOf(this.child.toString(options)) + "+?";
                return ret2;
            }
            String ret3 = String.valueOf(this.child.toString(options)) + this.child2.toString(options);
            return ret3;
        }
    }

    static class CharToken extends Token implements Serializable {
        private static final long serialVersionUID = -4394272816279496989L;
        final int chardata;

        CharToken(int type, int ch) {
            super(type);
            this.chardata = ch;
        }

        @Override
        int getChar() {
            return this.chardata;
        }

        @Override
        public String toString(int options) {
            int i = this.type;
            if (i != 0) {
                if (i == 8) {
                    if (this == Token.token_linebeginning || this == Token.token_lineend) {
                        StringBuilder sb = new StringBuilder();
                        sb.append((char) this.chardata);
                        String ret = sb.toString();
                        return ret;
                    }
                    String ret2 = "\\" + ((char) this.chardata);
                    return ret2;
                }
                return null;
            }
            switch (this.chardata) {
                case 9:
                    return "\\t";
                case 10:
                    return "\\n";
                case 12:
                    return "\\f";
                case 13:
                    return "\\r";
                case XPath.Tokens.EXPRTOKEN_OPERATOR_NOT_EQUAL:
                    return "\\e";
                case XPath.Tokens.EXPRTOKEN_AXISNAME_FOLLOWING_SIBLING:
                case XPath.Tokens.EXPRTOKEN_AXISNAME_NAMESPACE:
                case XPath.Tokens.EXPRTOKEN_AXISNAME_PARENT:
                case XPath.Tokens.EXPRTOKEN_AXISNAME_PRECEDING:
                case XPath.Tokens.EXPRTOKEN_LITERAL:
                case 63:
                case 91:
                case 92:
                case 123:
                case 124:
                    String ret3 = "\\" + ((char) this.chardata);
                    return ret3;
                default:
                    if (this.chardata >= 65536) {
                        String pre = SchemaSymbols.ATTVAL_FALSE_0 + Integer.toHexString(this.chardata);
                        return "\\v" + pre.substring(pre.length() - 6, pre.length());
                    }
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append((char) this.chardata);
                    String ret4 = sb2.toString();
                    return ret4;
            }
        }

        @Override
        boolean match(int ch) {
            if (this.type == 0) {
                if (ch == this.chardata) {
                    return Token.COUNTTOKENS;
                }
                return false;
            }
            throw new RuntimeException("NFAArrow#match(): Internal error: " + this.type);
        }
    }

    static class ClosureToken extends Token implements Serializable {
        private static final long serialVersionUID = 1308971930673997452L;
        final Token child;
        int max;
        int min;

        ClosureToken(int type, Token tok) {
            super(type);
            this.child = tok;
            setMin(-1);
            setMax(-1);
        }

        @Override
        int size() {
            return 1;
        }

        @Override
        Token getChild(int index) {
            return this.child;
        }

        @Override
        final void setMin(int min) {
            this.min = min;
        }

        @Override
        final void setMax(int max) {
            this.max = max;
        }

        @Override
        final int getMin() {
            return this.min;
        }

        @Override
        final int getMax() {
            return this.max;
        }

        @Override
        public String toString(int options) {
            if (this.type == 3) {
                if (getMin() < 0 && getMax() < 0) {
                    String ret = String.valueOf(this.child.toString(options)) + "*";
                    return ret;
                }
                if (getMin() == getMax()) {
                    String ret2 = String.valueOf(this.child.toString(options)) + "{" + getMin() + "}";
                    return ret2;
                }
                if (getMin() >= 0 && getMax() >= 0) {
                    String ret3 = String.valueOf(this.child.toString(options)) + "{" + getMin() + "," + getMax() + "}";
                    return ret3;
                }
                if (getMin() >= 0 && getMax() < 0) {
                    String ret4 = String.valueOf(this.child.toString(options)) + "{" + getMin() + ",}";
                    return ret4;
                }
                throw new RuntimeException("Token#toString(): CLOSURE " + getMin() + ", " + getMax());
            }
            if (getMin() < 0 && getMax() < 0) {
                String ret5 = String.valueOf(this.child.toString(options)) + "*?";
                return ret5;
            }
            if (getMin() == getMax()) {
                String ret6 = String.valueOf(this.child.toString(options)) + "{" + getMin() + "}?";
                return ret6;
            }
            if (getMin() >= 0 && getMax() >= 0) {
                String ret7 = String.valueOf(this.child.toString(options)) + "{" + getMin() + "," + getMax() + "}?";
                return ret7;
            }
            if (getMin() >= 0 && getMax() < 0) {
                String ret8 = String.valueOf(this.child.toString(options)) + "{" + getMin() + ",}?";
                return ret8;
            }
            throw new RuntimeException("Token#toString(): NONGREEDYCLOSURE " + getMin() + ", " + getMax());
        }
    }

    static class ParenToken extends Token implements Serializable {
        private static final long serialVersionUID = -5938014719827987704L;
        final Token child;
        final int parennumber;

        ParenToken(int type, Token tok, int paren) {
            super(type);
            this.child = tok;
            this.parennumber = paren;
        }

        @Override
        int size() {
            return 1;
        }

        @Override
        Token getChild(int index) {
            return this.child;
        }

        @Override
        int getParenNumber() {
            return this.parennumber;
        }

        @Override
        public String toString(int options) {
            int i = this.type;
            if (i == 6) {
                if (this.parennumber == 0) {
                    String ret = "(?:" + this.child.toString(options) + ")";
                    return ret;
                }
                String ret2 = "(" + this.child.toString(options) + ")";
                return ret2;
            }
            switch (i) {
                case 20:
                    String ret3 = "(?=" + this.child.toString(options) + ")";
                    return ret3;
                case 21:
                    String ret4 = "(?!" + this.child.toString(options) + ")";
                    return ret4;
                case 22:
                    String ret5 = "(?<=" + this.child.toString(options) + ")";
                    return ret5;
                case 23:
                    String ret6 = "(?<!" + this.child.toString(options) + ")";
                    return ret6;
                case 24:
                    String ret7 = "(?>" + this.child.toString(options) + ")";
                    return ret7;
                default:
                    return null;
            }
        }
    }

    static class ConditionToken extends Token implements Serializable {
        private static final long serialVersionUID = 4353765277910594411L;
        final Token condition;
        final Token no;
        final int refNumber;
        final Token yes;

        ConditionToken(int refno, Token cond, Token yespat, Token nopat) {
            super(26);
            this.refNumber = refno;
            this.condition = cond;
            this.yes = yespat;
            this.no = nopat;
        }

        @Override
        int size() {
            return this.no == null ? 1 : 2;
        }

        @Override
        Token getChild(int index) {
            if (index == 0) {
                return this.yes;
            }
            if (index == 1) {
                return this.no;
            }
            throw new RuntimeException("Internal Error: " + index);
        }

        @Override
        public String toString(int options) {
            String ret;
            if (this.refNumber > 0) {
                ret = "(?(" + this.refNumber + ")";
            } else if (this.condition.type == 8) {
                ret = "(?(" + this.condition + ")";
            } else {
                ret = "(?" + this.condition;
            }
            if (this.no == null) {
                return String.valueOf(ret) + this.yes + ")";
            }
            return String.valueOf(ret) + this.yes + "|" + this.no + ")";
        }
    }

    static class ModifierToken extends Token implements Serializable {
        private static final long serialVersionUID = -9114536559696480356L;
        final int add;
        final Token child;
        final int mask;

        ModifierToken(Token tok, int add, int mask) {
            super(25);
            this.child = tok;
            this.add = add;
            this.mask = mask;
        }

        @Override
        int size() {
            return 1;
        }

        @Override
        Token getChild(int index) {
            return this.child;
        }

        int getOptions() {
            return this.add;
        }

        int getOptionsMask() {
            return this.mask;
        }

        @Override
        public String toString(int options) {
            StringBuilder sb = new StringBuilder("(?");
            sb.append(this.add == 0 ? "" : REUtil.createOptionString(this.add));
            sb.append(this.mask == 0 ? "" : REUtil.createOptionString(this.mask));
            sb.append(":");
            sb.append(this.child.toString(options));
            sb.append(")");
            return sb.toString();
        }
    }

    static class UnionToken extends Token implements Serializable {
        private static final long serialVersionUID = -2568843945989489861L;
        Vector children;

        UnionToken(int type) {
            super(type);
        }

        @Override
        void addChild(Token tok) {
            StringBuffer buffer;
            if (tok == null) {
                return;
            }
            if (this.children == null) {
                this.children = new Vector();
            }
            if (this.type == 2) {
                this.children.addElement(tok);
                return;
            }
            if (tok.type == 1) {
                for (int i = 0; i < tok.size(); i++) {
                    addChild(tok.getChild(i));
                }
                return;
            }
            int size = this.children.size();
            if (size == 0) {
                this.children.addElement(tok);
                return;
            }
            Token previous = (Token) this.children.elementAt(size - 1);
            if ((previous.type != 0 && previous.type != 10) || (tok.type != 0 && tok.type != 10)) {
                this.children.addElement(tok);
                return;
            }
            int nextMaxLength = tok.type == 0 ? 2 : tok.getString().length();
            if (previous.type == 0) {
                buffer = new StringBuffer(2 + nextMaxLength);
                int ch = previous.getChar();
                if (ch >= 65536) {
                    buffer.append(REUtil.decomposeToSurrogates(ch));
                } else {
                    buffer.append((char) ch);
                }
                previous = Token.createString(null);
                this.children.setElementAt(previous, size - 1);
            } else {
                buffer = new StringBuffer(previous.getString().length() + nextMaxLength);
                buffer.append(previous.getString());
            }
            if (tok.type == 0) {
                int ch2 = tok.getChar();
                if (ch2 >= 65536) {
                    buffer.append(REUtil.decomposeToSurrogates(ch2));
                } else {
                    buffer.append((char) ch2);
                }
            } else {
                buffer.append(tok.getString());
            }
            ((StringToken) previous).string = new String(buffer);
        }

        @Override
        int size() {
            if (this.children == null) {
                return 0;
            }
            return this.children.size();
        }

        @Override
        Token getChild(int index) {
            return (Token) this.children.elementAt(index);
        }

        @Override
        public String toString(int options) {
            String ret;
            if (this.type == 1) {
                if (this.children.size() == 2) {
                    Token ch = getChild(0);
                    Token ch2 = getChild(1);
                    if (ch2.type == 3 && ch2.getChild(0) == ch) {
                        ret = String.valueOf(ch.toString(options)) + "+";
                    } else if (ch2.type == 9 && ch2.getChild(0) == ch) {
                        ret = String.valueOf(ch.toString(options)) + "+?";
                    } else {
                        ret = String.valueOf(ch.toString(options)) + ch2.toString(options);
                    }
                } else {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < this.children.size(); i++) {
                        sb.append(((Token) this.children.elementAt(i)).toString(options));
                    }
                    ret = new String(sb);
                }
                return ret;
            }
            if (this.children.size() == 2 && getChild(1).type == 7) {
                String ret2 = String.valueOf(getChild(0).toString(options)) + "?";
                return ret2;
            }
            if (this.children.size() == 2 && getChild(0).type == 7) {
                String ret3 = String.valueOf(getChild(1).toString(options)) + "??";
                return ret3;
            }
            StringBuffer sb2 = new StringBuffer();
            sb2.append(((Token) this.children.elementAt(0)).toString(options));
            for (int i2 = 1; i2 < this.children.size(); i2++) {
                sb2.append('|');
                sb2.append(((Token) this.children.elementAt(i2)).toString(options));
            }
            String ret4 = new String(sb2);
            return ret4;
        }
    }
}
