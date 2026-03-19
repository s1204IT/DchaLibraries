package mf.org.apache.xerces.impl.xpath.regex;

import com.mediatek.plugin.preload.SoOperater;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import mf.org.apache.xerces.impl.xpath.XPath;

class RegexParser {
    protected static final int S_INBRACKETS = 1;
    protected static final int S_INXBRACKETS = 2;
    protected static final int S_NORMAL = 0;
    static final int T_BACKSOLIDUS = 10;
    static final int T_CARET = 11;
    static final int T_CHAR = 0;
    static final int T_COMMENT = 21;
    static final int T_CONDITION = 23;
    static final int T_DOLLAR = 12;
    static final int T_DOT = 8;
    static final int T_EOF = 1;
    static final int T_INDEPENDENT = 18;
    static final int T_LBRACKET = 9;
    static final int T_LOOKAHEAD = 14;
    static final int T_LOOKBEHIND = 16;
    static final int T_LPAREN = 6;
    static final int T_LPAREN2 = 13;
    static final int T_MODIFIERS = 22;
    static final int T_NEGATIVELOOKAHEAD = 15;
    static final int T_NEGATIVELOOKBEHIND = 17;
    static final int T_OR = 2;
    static final int T_PLUS = 4;
    static final int T_POSIX_CHARCLASS_START = 20;
    static final int T_QUESTION = 5;
    static final int T_RPAREN = 7;
    static final int T_SET_OPERATIONS = 19;
    static final int T_STAR = 3;
    static final int T_XMLSCHEMA_CC_SUBTRACTION = 24;
    int chardata;
    boolean hasBackReferences;
    int nexttoken;
    int offset;
    int options;
    String regex;
    int regexlen;
    ResourceBundle resources;
    int context = 0;
    int parenOpened = 1;
    int parennumber = 1;
    Vector references = null;

    static class ReferencePosition {
        int position;
        int refNumber;

        ReferencePosition(int n, int pos) {
            this.refNumber = n;
            this.position = pos;
        }
    }

    public RegexParser() {
        setLocale(Locale.getDefault());
    }

    public RegexParser(Locale locale) {
        setLocale(locale);
    }

    public void setLocale(Locale locale) {
        try {
            if (locale != null) {
                this.resources = ResourceBundle.getBundle("mf.org.apache.xerces.impl.xpath.regex.message", locale);
            } else {
                this.resources = ResourceBundle.getBundle("mf.org.apache.xerces.impl.xpath.regex.message");
            }
        } catch (MissingResourceException mre) {
            throw new RuntimeException("Installation Problem???  Couldn't load messages: " + mre.getMessage());
        }
    }

    final ParseException ex(String key, int loc) {
        return new ParseException(this.resources.getString(key), loc);
    }

    protected final boolean isSet(int flag) {
        return (this.options & flag) == flag;
    }

    synchronized Token parse(String regex, int options) throws ParseException {
        Token ret;
        this.options = options;
        this.offset = 0;
        setContext(0);
        this.parennumber = 1;
        this.parenOpened = 1;
        this.hasBackReferences = false;
        this.regex = regex;
        if (isSet(16)) {
            this.regex = REUtil.stripExtendedComment(this.regex);
        }
        this.regexlen = this.regex.length();
        next();
        ret = parseRegex();
        if (this.offset != this.regexlen) {
            throw ex("parser.parse.1", this.offset);
        }
        if (this.references != null) {
            for (int i = 0; i < this.references.size(); i++) {
                ReferencePosition position = (ReferencePosition) this.references.elementAt(i);
                if (this.parennumber <= position.refNumber) {
                    throw ex("parser.parse.2", position.position);
                }
            }
            this.references.removeAllElements();
        }
        return ret;
    }

    protected final void setContext(int con) {
        this.context = con;
    }

    final int read() {
        return this.nexttoken;
    }

    final void next() {
        if (this.offset >= this.regexlen) {
            this.chardata = -1;
            this.nexttoken = 1;
            return;
        }
        String str = this.regex;
        int i = this.offset;
        this.offset = i + 1;
        int ch = str.charAt(i);
        this.chardata = ch;
        int ret = 0;
        if (this.context == 1) {
            if (ch == 45) {
                if (this.offset < this.regexlen && this.regex.charAt(this.offset) == '[') {
                    this.offset++;
                    ret = 24;
                } else {
                    ret = 0;
                }
            } else {
                switch (ch) {
                    case 91:
                        if (!isSet(512) && this.offset < this.regexlen && this.regex.charAt(this.offset) == ':') {
                            this.offset++;
                            ret = 20;
                        } else if (REUtil.isHighSurrogate(ch) && this.offset < this.regexlen) {
                            int low = this.regex.charAt(this.offset);
                            if (REUtil.isLowSurrogate(low)) {
                                this.chardata = REUtil.composeFromSurrogates(ch, low);
                                this.offset++;
                            }
                        }
                        break;
                    case 92:
                        ret = 10;
                        if (this.offset >= this.regexlen) {
                            throw ex("parser.next.1", this.offset - 1);
                        }
                        String str2 = this.regex;
                        int i2 = this.offset;
                        this.offset = i2 + 1;
                        this.chardata = str2.charAt(i2);
                        break;
                        break;
                }
            }
            this.nexttoken = ret;
            return;
        }
        if (ch != 36) {
            if (ch != 46) {
                if (ch != 63) {
                    if (ch != 94) {
                        if (ch == 124) {
                            ret = 2;
                        } else {
                            switch (ch) {
                                case XPath.Tokens.EXPRTOKEN_AXISNAME_FOLLOWING_SIBLING:
                                    ret = 6;
                                    if (this.offset < this.regexlen && this.regex.charAt(this.offset) == '?') {
                                        int i3 = this.offset + 1;
                                        this.offset = i3;
                                        if (i3 >= this.regexlen) {
                                            throw ex("parser.next.2", this.offset - 1);
                                        }
                                        String str3 = this.regex;
                                        int i4 = this.offset;
                                        this.offset = i4 + 1;
                                        int ch2 = str3.charAt(i4);
                                        if (ch2 != 33) {
                                            if (ch2 != 35) {
                                                if (ch2 != 58) {
                                                    if (ch2 == 91) {
                                                        ret = 19;
                                                        break;
                                                    } else {
                                                        switch (ch2) {
                                                            case 60:
                                                                if (this.offset >= this.regexlen) {
                                                                    throw ex("parser.next.2", this.offset - 3);
                                                                }
                                                                String str4 = this.regex;
                                                                int i5 = this.offset;
                                                                this.offset = i5 + 1;
                                                                int ch3 = str4.charAt(i5);
                                                                if (ch3 != 61) {
                                                                    if (ch3 == 33) {
                                                                        ret = 17;
                                                                    } else {
                                                                        throw ex("parser.next.3", this.offset - 3);
                                                                    }
                                                                } else {
                                                                    ret = 16;
                                                                }
                                                                break;
                                                                break;
                                                            case 61:
                                                                ret = 14;
                                                                break;
                                                            case 62:
                                                                ret = 18;
                                                                break;
                                                            default:
                                                                if (ch2 == 45 || ((97 <= ch2 && ch2 <= 122) || (65 <= ch2 && ch2 <= 90))) {
                                                                    this.offset--;
                                                                    ret = 22;
                                                                } else if (ch2 == 40) {
                                                                    ret = 23;
                                                                } else {
                                                                    throw ex("parser.next.2", this.offset - 2);
                                                                }
                                                                break;
                                                        }
                                                    }
                                                } else {
                                                    ret = 13;
                                                    break;
                                                }
                                            } else {
                                                while (this.offset < this.regexlen) {
                                                    String str5 = this.regex;
                                                    int i6 = this.offset;
                                                    this.offset = i6 + 1;
                                                    ch2 = str5.charAt(i6);
                                                    if (ch2 == 41) {
                                                        if (ch2 == 41) {
                                                            throw ex("parser.next.4", this.offset - 1);
                                                        }
                                                        ret = 21;
                                                        break;
                                                    }
                                                }
                                                if (ch2 == 41) {
                                                }
                                            }
                                        } else {
                                            ret = 15;
                                            break;
                                        }
                                    }
                                    break;
                                case XPath.Tokens.EXPRTOKEN_AXISNAME_NAMESPACE:
                                    ret = 7;
                                    break;
                                case XPath.Tokens.EXPRTOKEN_AXISNAME_PARENT:
                                    ret = 3;
                                    break;
                                case XPath.Tokens.EXPRTOKEN_AXISNAME_PRECEDING:
                                    ret = 4;
                                    break;
                                default:
                                    switch (ch) {
                                        case 91:
                                            ret = 9;
                                            break;
                                        case 92:
                                            ret = 10;
                                            if (this.offset >= this.regexlen) {
                                                throw ex("parser.next.1", this.offset - 1);
                                            }
                                            String str6 = this.regex;
                                            int i7 = this.offset;
                                            this.offset = i7 + 1;
                                            this.chardata = str6.charAt(i7);
                                            break;
                                            break;
                                    }
                                    break;
                            }
                        }
                    } else {
                        ret = isSet(512) ? 0 : 11;
                    }
                } else {
                    ret = 5;
                }
            } else {
                ret = 8;
            }
        } else {
            ret = isSet(512) ? 0 : 12;
        }
        this.nexttoken = ret;
    }

    Token parseRegex() throws ParseException {
        Token tok = parseTerm();
        Token parent = null;
        while (read() == 2) {
            next();
            if (parent == null) {
                parent = Token.createUnion();
                parent.addChild(tok);
                tok = parent;
            }
            tok.addChild(parseTerm());
        }
        return tok;
    }

    Token parseTerm() throws ParseException {
        int ch = read();
        if (ch == 2 || ch == 7 || ch == 1) {
            return Token.createEmpty();
        }
        Token tok = parseFactor();
        Token concat = null;
        while (true) {
            int ch2 = read();
            if (ch2 == 2 || ch2 == 7 || ch2 == 1) {
                break;
            }
            if (concat == null) {
                concat = Token.createConcat();
                concat.addChild(tok);
                tok = concat;
            }
            concat.addChild(parseFactor());
        }
        return tok;
    }

    Token processCaret() throws ParseException {
        next();
        return Token.token_linebeginning;
    }

    Token processDollar() throws ParseException {
        next();
        return Token.token_lineend;
    }

    Token processLookahead() throws ParseException {
        next();
        Token tok = Token.createLook(20, parseRegex());
        if (read() != 7) {
            throw ex("parser.factor.1", this.offset - 1);
        }
        next();
        return tok;
    }

    Token processNegativelookahead() throws ParseException {
        next();
        Token tok = Token.createLook(21, parseRegex());
        if (read() != 7) {
            throw ex("parser.factor.1", this.offset - 1);
        }
        next();
        return tok;
    }

    Token processLookbehind() throws ParseException {
        next();
        Token tok = Token.createLook(22, parseRegex());
        if (read() != 7) {
            throw ex("parser.factor.1", this.offset - 1);
        }
        next();
        return tok;
    }

    Token processNegativelookbehind() throws ParseException {
        next();
        Token tok = Token.createLook(23, parseRegex());
        if (read() != 7) {
            throw ex("parser.factor.1", this.offset - 1);
        }
        next();
        return tok;
    }

    Token processBacksolidus_A() throws ParseException {
        next();
        return Token.token_stringbeginning;
    }

    Token processBacksolidus_Z() throws ParseException {
        next();
        return Token.token_stringend2;
    }

    Token processBacksolidus_z() throws ParseException {
        next();
        return Token.token_stringend;
    }

    Token processBacksolidus_b() throws ParseException {
        next();
        return Token.token_wordedge;
    }

    Token processBacksolidus_B() throws ParseException {
        next();
        return Token.token_not_wordedge;
    }

    Token processBacksolidus_lt() throws ParseException {
        next();
        return Token.token_wordbeginning;
    }

    Token processBacksolidus_gt() throws ParseException {
        next();
        return Token.token_wordend;
    }

    Token processStar(Token tok) throws ParseException {
        next();
        if (read() == 5) {
            next();
            return Token.createNGClosure(tok);
        }
        return Token.createClosure(tok);
    }

    Token processPlus(Token tok) throws ParseException {
        next();
        if (read() == 5) {
            next();
            return Token.createConcat(tok, Token.createNGClosure(tok));
        }
        return Token.createConcat(tok, Token.createClosure(tok));
    }

    Token processQuestion(Token tok) throws ParseException {
        next();
        Token par = Token.createUnion();
        if (read() == 5) {
            next();
            par.addChild(Token.createEmpty());
            par.addChild(tok);
        } else {
            par.addChild(tok);
            par.addChild(Token.createEmpty());
        }
        return par;
    }

    boolean checkQuestion(int off) {
        return off < this.regexlen && this.regex.charAt(off) == '?';
    }

    Token processParen() throws ParseException {
        next();
        int p = this.parenOpened;
        this.parenOpened = p + 1;
        Token tok = Token.createParen(parseRegex(), p);
        if (read() != 7) {
            throw ex("parser.factor.1", this.offset - 1);
        }
        this.parennumber++;
        next();
        return tok;
    }

    Token processParen2() throws ParseException {
        next();
        Token tok = Token.createParen(parseRegex(), 0);
        if (read() != 7) {
            throw ex("parser.factor.1", this.offset - 1);
        }
        next();
        return tok;
    }

    Token processCondition() throws ParseException {
        int ch;
        if (this.offset + 1 >= this.regexlen) {
            throw ex("parser.factor.4", this.offset);
        }
        int refno = -1;
        Token condition = null;
        int ch2 = this.regex.charAt(this.offset);
        if (49 <= ch2 && ch2 <= 57) {
            refno = ch2 - 48;
            int finalRefno = refno;
            if (this.parennumber <= refno) {
                throw ex("parser.parse.2", this.offset);
            }
            while (this.offset + 1 < this.regexlen && 48 <= (ch = this.regex.charAt(this.offset + 1)) && ch <= 57 && (refno = (refno * 10) + (ch - 48)) < this.parennumber) {
                finalRefno = refno;
                this.offset++;
            }
            this.hasBackReferences = true;
            if (this.references == null) {
                this.references = new Vector();
            }
            this.references.addElement(new ReferencePosition(finalRefno, this.offset));
            this.offset++;
            if (this.regex.charAt(this.offset) != ')') {
                throw ex("parser.factor.1", this.offset);
            }
            this.offset++;
        } else {
            if (ch2 == 63) {
                this.offset--;
            }
            next();
            condition = parseFactor();
            int i = condition.type;
            if (i == 8) {
                if (read() != 7) {
                    throw ex("parser.factor.1", this.offset - 1);
                }
            } else {
                switch (i) {
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                        break;
                    default:
                        throw ex("parser.factor.5", this.offset);
                }
            }
        }
        next();
        Token yesPattern = parseRegex();
        Token noPattern = null;
        if (yesPattern.type == 2) {
            if (yesPattern.size() != 2) {
                throw ex("parser.factor.6", this.offset);
            }
            noPattern = yesPattern.getChild(1);
            yesPattern = yesPattern.getChild(0);
        }
        if (read() != 7) {
            throw ex("parser.factor.1", this.offset - 1);
        }
        next();
        return Token.createCondition(refno, condition, yesPattern, noPattern);
    }

    Token processModifiers() throws ParseException {
        int v;
        int v2;
        int add = 0;
        int mask = 0;
        int ch = -1;
        while (this.offset < this.regexlen && (v2 = REUtil.getOptionValue((ch = this.regex.charAt(this.offset)))) != 0) {
            add |= v2;
            this.offset++;
        }
        int v3 = this.offset;
        if (v3 >= this.regexlen) {
            throw ex("parser.factor.2", this.offset - 1);
        }
        if (ch == 45) {
            this.offset++;
            while (this.offset < this.regexlen && (v = REUtil.getOptionValue((ch = this.regex.charAt(this.offset)))) != 0) {
                mask |= v;
                this.offset++;
            }
            int v4 = this.offset;
            if (v4 >= this.regexlen) {
                throw ex("parser.factor.2", this.offset - 1);
            }
        }
        if (ch == 58) {
            this.offset++;
            next();
            Token tok = Token.createModifierGroup(parseRegex(), add, mask);
            if (read() != 7) {
                throw ex("parser.factor.1", this.offset - 1);
            }
            next();
            return tok;
        }
        if (ch == 41) {
            this.offset++;
            next();
            Token tok2 = Token.createModifierGroup(parseRegex(), add, mask);
            return tok2;
        }
        throw ex("parser.factor.3", this.offset);
    }

    Token processIndependent() throws ParseException {
        next();
        Token tok = Token.createLook(24, parseRegex());
        if (read() != 7) {
            throw ex("parser.factor.1", this.offset - 1);
        }
        next();
        return tok;
    }

    Token processBacksolidus_c() throws ParseException {
        if (this.offset < this.regexlen) {
            String str = this.regex;
            int i = this.offset;
            this.offset = i + 1;
            int ch2 = str.charAt(i);
            if ((ch2 & 65504) == 64) {
                next();
                return Token.createChar(ch2 - 64);
            }
        }
        throw ex("parser.atom.1", this.offset - 1);
    }

    Token processBacksolidus_C() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    Token processBacksolidus_i() throws ParseException {
        Token tok = Token.createChar(105);
        next();
        return tok;
    }

    Token processBacksolidus_I() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    Token processBacksolidus_g() throws ParseException {
        next();
        return Token.getGraphemePattern();
    }

    Token processBacksolidus_X() throws ParseException {
        next();
        return Token.getCombiningCharacterSequence();
    }

    Token processBackreference() throws ParseException {
        int ch;
        int refnum = this.chardata - 48;
        int finalRefnum = refnum;
        if (this.parennumber <= refnum) {
            throw ex("parser.parse.2", this.offset - 2);
        }
        while (this.offset < this.regexlen && 48 <= (ch = this.regex.charAt(this.offset)) && ch <= 57 && (refnum = (refnum * 10) + (ch - 48)) < this.parennumber) {
            this.offset++;
            finalRefnum = refnum;
            this.chardata = ch;
        }
        Token tok = Token.createBackReference(finalRefnum);
        this.hasBackReferences = true;
        if (this.references == null) {
            this.references = new Vector();
        }
        this.references.addElement(new ReferencePosition(finalRefnum, this.offset - 2));
        next();
        return tok;
    }

    Token parseFactor() throws ParseException {
        switch (read()) {
            case 10:
                int i = this.chardata;
                if (i == 60) {
                    return processBacksolidus_lt();
                }
                if (i == 62) {
                    return processBacksolidus_gt();
                }
                if (i == 90) {
                    return processBacksolidus_Z();
                }
                if (i == 98) {
                    return processBacksolidus_b();
                }
                if (i != 122) {
                    switch (i) {
                        case 65:
                            return processBacksolidus_A();
                        case 66:
                            return processBacksolidus_B();
                    }
                }
                return processBacksolidus_z();
            case 11:
                return processCaret();
            case 12:
                return processDollar();
            case 14:
                return processLookahead();
            case 15:
                return processNegativelookahead();
            case 16:
                return processLookbehind();
            case 17:
                return processNegativelookbehind();
            case 21:
                next();
                return Token.createEmpty();
        }
        Token tok = parseAtom();
        int ch = read();
        if (ch != 0) {
            switch (ch) {
                case 3:
                    return processStar(tok);
                case 4:
                    return processPlus(tok);
                case 5:
                    return processQuestion(tok);
            }
        }
        if (this.chardata == 123 && this.offset < this.regexlen) {
            int off = this.offset;
            int off2 = off + 1;
            int off3 = this.regex.charAt(off);
            int ch2 = off3;
            if (off3 >= 48 && ch2 <= 57) {
                int min = ch2 - 48;
                while (off2 < this.regexlen) {
                    int off4 = off2 + 1;
                    int off5 = this.regex.charAt(off2);
                    ch2 = off5;
                    if (off5 >= 48 && ch2 <= 57) {
                        min = ((min * 10) + ch2) - 48;
                        if (min >= 0) {
                            off2 = off4;
                        } else {
                            throw ex("parser.quantifier.5", this.offset);
                        }
                    } else {
                        off2 = off4;
                        int max = min;
                        if (ch2 == 44) {
                            if (off2 >= this.regexlen) {
                                throw ex("parser.quantifier.3", this.offset);
                            }
                            int off6 = off2 + 1;
                            int off7 = this.regex.charAt(off2);
                            ch2 = off7;
                            if (off7 >= 48 && ch2 <= 57) {
                                max = ch2 - 48;
                                while (off6 < this.regexlen) {
                                    int off8 = off6 + 1;
                                    int iCharAt = this.regex.charAt(off6);
                                    ch2 = iCharAt;
                                    if (iCharAt >= 48 && ch2 <= 57) {
                                        max = ((max * 10) + ch2) - 48;
                                        if (max >= 0) {
                                            off6 = off8;
                                        } else {
                                            throw ex("parser.quantifier.5", this.offset);
                                        }
                                    } else {
                                        off2 = off8;
                                        if (min > max) {
                                            throw ex("parser.quantifier.4", this.offset);
                                        }
                                    }
                                }
                                off2 = off6;
                                if (min > max) {
                                }
                            } else {
                                max = -1;
                                off2 = off6;
                            }
                        }
                        if (ch2 == 125) {
                            throw ex("parser.quantifier.2", this.offset);
                        }
                        if (checkQuestion(off2)) {
                            tok = Token.createNGClosure(tok);
                            this.offset = off2 + 1;
                        } else {
                            tok = Token.createClosure(tok);
                            this.offset = off2;
                        }
                        tok.setMin(min);
                        tok.setMax(max);
                        next();
                    }
                }
                int max2 = min;
                if (ch2 == 44) {
                }
                if (ch2 == 125) {
                }
            } else {
                throw ex("parser.quantifier.1", this.offset);
            }
        }
        return tok;
    }

    Token parseAtom() throws ParseException {
        Token tok;
        int ch = read();
        if (ch == 0) {
            if (this.chardata == 93 || this.chardata == 123 || this.chardata == 125) {
                throw ex("parser.atom.4", this.offset - 1);
            }
            Token tok2 = Token.createChar(this.chardata);
            int high = this.chardata;
            next();
            if (REUtil.isHighSurrogate(high) && read() == 0 && REUtil.isLowSurrogate(this.chardata)) {
                char[] sur = {(char) high, (char) this.chardata};
                Token tok3 = Token.createParen(Token.createString(new String(sur)), 0);
                next();
                return tok3;
            }
            return tok2;
        }
        if (ch == 6) {
            return processParen();
        }
        if (ch == 13) {
            return processParen2();
        }
        switch (ch) {
            case 8:
                next();
                return Token.token_dot;
            case 9:
                return parseCharacterClass(true);
            case 10:
                int i = this.chardata;
                switch (i) {
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                        return processBackreference();
                    default:
                        switch (i) {
                            case 67:
                                return processBacksolidus_C();
                            case 68:
                                break;
                            default:
                                switch (i) {
                                    case 87:
                                        break;
                                    case 88:
                                        return processBacksolidus_X();
                                    default:
                                        switch (i) {
                                            case 99:
                                                return processBacksolidus_c();
                                            case 100:
                                                break;
                                            case 101:
                                            case 102:
                                                int ch2 = decodeEscaped();
                                                if (ch2 < 65536) {
                                                    tok = Token.createChar(ch2);
                                                } else {
                                                    tok = Token.createString(REUtil.decomposeToSurrogates(ch2));
                                                }
                                                next();
                                                return tok;
                                            case 103:
                                                return processBacksolidus_g();
                                            default:
                                                switch (i) {
                                                    case 114:
                                                    case 116:
                                                    case 117:
                                                    case 118:
                                                    case 120:
                                                        break;
                                                    case 115:
                                                    case 119:
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case 73:
                                                                return processBacksolidus_I();
                                                            case 80:
                                                            case 112:
                                                                int pstart = this.offset;
                                                                tok = processBacksolidus_pP(this.chardata);
                                                                if (tok == null) {
                                                                    throw ex("parser.atom.5", pstart);
                                                                }
                                                                break;
                                                            case 83:
                                                                break;
                                                            case 105:
                                                                return processBacksolidus_i();
                                                            case 110:
                                                                break;
                                                            default:
                                                                tok = Token.createChar(this.chardata);
                                                                break;
                                                        }
                                                        next();
                                                        return tok;
                                                }
                                                break;
                                        }
                                        break;
                                }
                                break;
                        }
                        Token tok4 = getTokenForShorthand(this.chardata);
                        next();
                        return tok4;
                }
            default:
                switch (ch) {
                    case 18:
                        return processIndependent();
                    case 19:
                        return parseSetOperations();
                    default:
                        switch (ch) {
                            case 22:
                                return processModifiers();
                            case 23:
                                return processCondition();
                            default:
                                throw ex("parser.atom.4", this.offset - 1);
                        }
                }
        }
    }

    protected RangeToken processBacksolidus_pP(int c) throws ParseException {
        next();
        if (read() != 0 || this.chardata != 123) {
            throw ex("parser.atom.2", this.offset - 1);
        }
        boolean positive = c == 112;
        int namestart = this.offset;
        int nameend = this.regex.indexOf(125, namestart);
        if (nameend < 0) {
            throw ex("parser.atom.3", this.offset);
        }
        String pname = this.regex.substring(namestart, nameend);
        this.offset = nameend + 1;
        return Token.getRange(pname, positive, isSet(512));
    }

    int processCIinCharacterClass(RangeToken tok, int c) {
        return decodeEscaped();
    }

    protected RangeToken parseCharacterClass(boolean useNrange) throws ParseException {
        RangeToken tok;
        boolean z;
        int i;
        int i2 = 1;
        setContext(1);
        next();
        boolean nrange = false;
        RangeToken base = null;
        char c = '^';
        boolean z2 = false;
        if (read() == 0 && this.chardata == 94) {
            nrange = true;
            next();
            if (useNrange) {
                tok = Token.createNRange();
            } else {
                base = Token.createRange();
                base.addRange(0, 1114111);
                tok = Token.createRange();
            }
        } else {
            tok = Token.createRange();
        }
        boolean firstloop = true;
        while (true) {
            int type = read();
            if (type != i2 && (type != 0 || this.chardata != 93 || firstloop)) {
                int c2 = this.chardata;
                boolean end = false;
                if (type == 10) {
                    switch (c2) {
                        case 67:
                        case 73:
                        case 99:
                        case 105:
                            c2 = processCIinCharacterClass(tok, c2);
                            if (c2 < 0) {
                                end = true;
                            }
                            break;
                        case 68:
                        case 83:
                        case 87:
                        case 100:
                        case 115:
                        case 119:
                            tok.mergeRanges(getTokenForShorthand(c2));
                            end = true;
                            break;
                        case 80:
                        case 112:
                            int pstart = this.offset;
                            Token tok2 = processBacksolidus_pP(c2);
                            if (tok2 == null) {
                                throw ex("parser.atom.5", pstart);
                            }
                            tok.mergeRanges(tok2);
                            end = true;
                            break;
                            break;
                        default:
                            c2 = decodeEscaped();
                            break;
                    }
                    z = z2;
                } else {
                    if (type == 20) {
                        int nameend = this.regex.indexOf(58, this.offset);
                        if (nameend < 0) {
                            throw ex("parser.cc.1", this.offset);
                        }
                        boolean positive = true;
                        if (this.regex.charAt(this.offset) == c) {
                            this.offset += i2;
                            positive = false;
                        }
                        String name = this.regex.substring(this.offset, nameend);
                        Token range = Token.getRange(name, positive, isSet(512));
                        if (range == null) {
                            throw ex("parser.cc.3", this.offset);
                        }
                        tok.mergeRanges(range);
                        end = true;
                        if (nameend + 1 < this.regexlen && this.regex.charAt(nameend + 1) == ']') {
                            this.offset = nameend + 2;
                        }
                    } else if (type == 24 && !firstloop) {
                        if (nrange) {
                            if (useNrange) {
                                tok = (RangeToken) Token.complementRanges(tok);
                            } else {
                                base.subtractRanges(tok);
                                tok = base;
                            }
                            nrange = false;
                        }
                        RangeToken range2 = parseCharacterClass(false);
                        tok.subtractRanges(range2);
                        if (read() != 0 || this.chardata != 93) {
                            throw ex("parser.cc.5", this.offset);
                        }
                    }
                    z = false;
                }
                next();
                if (!end) {
                    if (read() != 0 || this.chardata != 45) {
                        i = 1;
                        if (!isSet(2) || c2 > 65535) {
                            tok.addRange(c2, c2);
                        } else {
                            addCaseInsensitiveChar(tok, c2);
                        }
                    } else {
                        if (type == 24) {
                            throw ex("parser.cc.8", this.offset - 1);
                        }
                        next();
                        int type2 = read();
                        if (type2 == 1) {
                            throw ex("parser.cc.2", this.offset);
                        }
                        if (type2 == 0 && this.chardata == 93) {
                            if (!isSet(2) || c2 > 65535) {
                                tok.addRange(c2, c2);
                            } else {
                                addCaseInsensitiveChar(tok, c2);
                            }
                            tok.addRange(45, 45);
                        } else {
                            int rangeend = this.chardata;
                            if (type2 == 10) {
                                rangeend = decodeEscaped();
                            }
                            next();
                            if (c2 > rangeend) {
                                throw ex("parser.ope.3", this.offset - 1);
                            }
                            if (!isSet(2) || (c2 > 65535 && rangeend > 65535)) {
                                tok.addRange(c2, rangeend);
                            } else {
                                addCaseInsensitiveCharRange(tok, c2, rangeend);
                            }
                        }
                        i = 1;
                    }
                } else {
                    i = 1;
                }
                if (isSet(SoOperater.STEP) && read() == 0 && this.chardata == 44) {
                    next();
                }
                firstloop = false;
                z2 = z;
                i2 = i;
                c = '^';
            }
        }
    }

    protected RangeToken parseSetOperations() throws ParseException {
        RangeToken tok = parseCharacterClass(false);
        while (true) {
            int type = read();
            if (type != 7) {
                int ch = this.chardata;
                if ((type == 0 && (ch == 45 || ch == 38)) || type == 4) {
                    next();
                    if (read() != 9) {
                        throw ex("parser.ope.1", this.offset - 1);
                    }
                    RangeToken t2 = parseCharacterClass(false);
                    if (type == 4) {
                        tok.mergeRanges(t2);
                    } else if (ch == 45) {
                        tok.subtractRanges(t2);
                    } else if (ch == 38) {
                        tok.intersectRanges(t2);
                    } else {
                        throw new RuntimeException("ASSERT");
                    }
                } else {
                    throw ex("parser.ope.2", this.offset - 1);
                }
            } else {
                next();
                return tok;
            }
        }
    }

    Token getTokenForShorthand(int ch) {
        if (ch == 68) {
            if (isSet(32)) {
                Token tok = Token.getRange("Nd", false);
                return tok;
            }
            Token tok2 = Token.token_not_0to9;
            return tok2;
        }
        if (ch == 83) {
            if (isSet(32)) {
                Token tok3 = Token.getRange("IsSpace", false);
                return tok3;
            }
            Token tok4 = Token.token_not_spaces;
            return tok4;
        }
        if (ch == 87) {
            if (isSet(32)) {
                Token tok5 = Token.getRange("IsWord", false);
                return tok5;
            }
            Token tok6 = Token.token_not_wordchars;
            return tok6;
        }
        if (ch == 100) {
            if (isSet(32)) {
                Token tok7 = Token.getRange("Nd", true);
                return tok7;
            }
            Token tok8 = Token.token_0to9;
            return tok8;
        }
        if (ch == 115) {
            if (isSet(32)) {
                Token tok9 = Token.getRange("IsSpace", true);
                return tok9;
            }
            Token tok10 = Token.token_spaces;
            return tok10;
        }
        if (ch == 119) {
            if (isSet(32)) {
                Token tok11 = Token.getRange("IsWord", true);
                return tok11;
            }
            Token tok12 = Token.token_wordchars;
            return tok12;
        }
        throw new RuntimeException("Internal Error: shorthands: \\u" + Integer.toString(ch, 16));
    }

    int decodeEscaped() throws ParseException {
        int v1;
        int v12;
        int v13;
        int v14;
        int v15;
        int v16;
        int v17;
        int v18;
        int v19;
        int v110;
        int v111;
        int v112;
        if (read() != 10) {
            throw ex("parser.next.1", this.offset - 1);
        }
        int c = this.chardata;
        if (c != 65 && c != 90) {
            if (c == 110) {
                return 10;
            }
            if (c == 114) {
                return 13;
            }
            if (c == 120) {
                next();
                if (read() != 0) {
                    throw ex("parser.descape.1", this.offset - 1);
                }
                if (this.chardata == 123) {
                    int uv = 0;
                    while (true) {
                        next();
                        if (read() != 0) {
                            throw ex("parser.descape.1", this.offset - 1);
                        }
                        int v113 = hexChar(this.chardata);
                        if (v113 >= 0) {
                            if (uv > uv * 16) {
                                throw ex("parser.descape.2", this.offset - 1);
                            }
                            uv = (uv * 16) + v113;
                        } else {
                            if (this.chardata != 125) {
                                throw ex("parser.descape.3", this.offset - 1);
                            }
                            if (uv > 1114111) {
                                throw ex("parser.descape.4", this.offset - 1);
                            }
                            return uv;
                        }
                    }
                } else {
                    if (read() != 0 || (v1 = hexChar(this.chardata)) < 0) {
                        int uv2 = this.offset;
                        throw ex("parser.descape.1", uv2 - 1);
                    }
                    next();
                    if (read() != 0 || (v12 = hexChar(this.chardata)) < 0) {
                        throw ex("parser.descape.1", this.offset - 1);
                    }
                    int uv3 = (v1 * 16) + v12;
                    return uv3;
                }
            } else if (c != 122) {
                switch (c) {
                    case 101:
                        return 27;
                    case 102:
                        return 12;
                    default:
                        switch (c) {
                            case 116:
                                return 9;
                            case 117:
                                next();
                                if (read() != 0 || (v13 = hexChar(this.chardata)) < 0) {
                                    int uv4 = this.offset;
                                    throw ex("parser.descape.1", uv4 - 1);
                                }
                                next();
                                if (read() != 0 || (v14 = hexChar(this.chardata)) < 0) {
                                    int uv5 = this.offset;
                                    throw ex("parser.descape.1", uv5 - 1);
                                }
                                int uv6 = (v13 * 16) + v14;
                                next();
                                if (read() != 0 || (v15 = hexChar(this.chardata)) < 0) {
                                    int uv7 = this.offset;
                                    throw ex("parser.descape.1", uv7 - 1);
                                }
                                int uv8 = (uv6 * 16) + v15;
                                next();
                                if (read() != 0 || (v16 = hexChar(this.chardata)) < 0) {
                                    int uv9 = this.offset;
                                    throw ex("parser.descape.1", uv9 - 1);
                                }
                                return (uv8 * 16) + v16;
                            case 118:
                                next();
                                if (read() != 0 || (v17 = hexChar(this.chardata)) < 0) {
                                    int uv10 = this.offset;
                                    throw ex("parser.descape.1", uv10 - 1);
                                }
                                next();
                                if (read() != 0 || (v18 = hexChar(this.chardata)) < 0) {
                                    throw ex("parser.descape.1", this.offset - 1);
                                }
                                int uv11 = (v17 * 16) + v18;
                                next();
                                if (read() != 0 || (v19 = hexChar(this.chardata)) < 0) {
                                    int uv12 = this.offset;
                                    throw ex("parser.descape.1", uv12 - 1);
                                }
                                int uv13 = (uv11 * 16) + v19;
                                next();
                                if (read() != 0 || (v110 = hexChar(this.chardata)) < 0) {
                                    throw ex("parser.descape.1", this.offset - 1);
                                }
                                int uv14 = (uv13 * 16) + v110;
                                next();
                                if (read() != 0 || (v111 = hexChar(this.chardata)) < 0) {
                                    int uv15 = this.offset;
                                    throw ex("parser.descape.1", uv15 - 1);
                                }
                                int uv16 = (uv14 * 16) + v111;
                                next();
                                if (read() != 0 || (v112 = hexChar(this.chardata)) < 0) {
                                    throw ex("parser.descape.1", this.offset - 1);
                                }
                                int uv17 = (uv16 * 16) + v112;
                                if (uv17 > 1114111) {
                                    throw ex("parser.descappe.4", this.offset - 1);
                                }
                                return uv17;
                            default:
                                return c;
                        }
                }
            }
        }
        throw ex("parser.descape.5", this.offset - 2);
    }

    private static final int hexChar(int ch) {
        if (ch < 48 || ch > 102) {
            return -1;
        }
        if (ch <= 57) {
            return ch - 48;
        }
        if (ch < 65) {
            return -1;
        }
        if (ch <= 70) {
            return (ch - 65) + 10;
        }
        if (ch < 97) {
            return -1;
        }
        return (ch - 97) + 10;
    }

    protected static final void addCaseInsensitiveChar(RangeToken tok, int c) {
        int[] caseMap = CaseInsensitiveMap.get(c);
        tok.addRange(c, c);
        if (caseMap != null) {
            for (int i = 0; i < caseMap.length; i += 2) {
                tok.addRange(caseMap[i], caseMap[i]);
            }
        }
    }

    protected static final void addCaseInsensitiveCharRange(RangeToken tok, int start, int end) {
        int r1;
        int r2;
        if (start <= end) {
            r1 = start;
            r2 = end;
        } else {
            r1 = end;
            r2 = start;
        }
        tok.addRange(r1, r2);
        for (int ch = r1; ch <= r2; ch++) {
            int[] caseMap = CaseInsensitiveMap.get(ch);
            if (caseMap != null) {
                for (int i = 0; i < caseMap.length; i += 2) {
                    tok.addRange(caseMap[i], caseMap[i]);
                }
            }
        }
    }
}
