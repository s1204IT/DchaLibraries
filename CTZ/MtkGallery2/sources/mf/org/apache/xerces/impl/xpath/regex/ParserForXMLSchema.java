package mf.org.apache.xerces.impl.xpath.regex;

import java.util.Hashtable;
import java.util.Locale;
import mf.org.apache.xerces.impl.xpath.XPath;

class ParserForXMLSchema extends RegexParser {
    private static final String DIGITS = "09٠٩۰۹०९০৯੦੯૦૯୦୯௧௯౦౯೦೯൦൯๐๙໐໙༠༩";
    private static final String LETTERS = "AZazÀÖØöøıĴľŁňŊžƀǃǍǰǴǵǺȗɐʨʻˁΆΆΈΊΌΌΎΡΣώϐϖϚϚϜϜϞϞϠϠϢϳЁЌЎяёќўҁҐӄӇӈӋӌӐӫӮӵӸӹԱՖՙՙաֆאתװײءغفيٱڷںھۀێېۓەەۥۦअहऽऽक़ॡঅঌএঐওনপরললশহড়ঢ়য়ৡৰৱਅਊਏਐਓਨਪਰਲਲ਼ਵਸ਼ਸਹਖ਼ੜਫ਼ਫ਼ੲੴઅઋઍઍએઑઓનપરલળવહઽઽૠૠଅଌଏଐଓନପରଲଳଶହଽଽଡ଼ଢ଼ୟୡஅஊஎஐஒகஙசஜஜஞடணதநபமவஷஹఅఌఎఐఒనపళవహౠౡಅಌಎಐಒನಪಳವಹೞೞೠೡഅഌഎഐഒനപഹൠൡกฮะะาำเๅກຂຄຄງຈຊຊຍຍດທນຟມຣລລວວສຫອຮະະາຳຽຽເໄཀཇཉཀྵႠჅაჶᄀᄀᄂᄃᄅᄇᄉᄉᄋᄌᄎᄒᄼᄼᄾᄾᅀᅀᅌᅌᅎᅎᅐᅐᅔᅕᅙᅙᅟᅡᅣᅣᅥᅥᅧᅧᅩᅩᅭᅮᅲᅳᅵᅵᆞᆞᆨᆨᆫᆫᆮᆯᆷᆸᆺᆺᆼᇂᇫᇫᇰᇰᇹᇹḀẛẠỹἀἕἘἝἠὅὈὍὐὗὙὙὛὛὝὝὟώᾀᾴᾶᾼιιῂῄῆῌῐΐῖΊῠῬῲῴῶῼΩΩKÅ℮℮ↀↂ〇〇〡〩ぁゔァヺㄅㄬ一龥가힣";
    private static final String NAMECHARS = "-.0:AZ__az··ÀÖØöøıĴľŁňŊžƀǃǍǰǴǵǺȗɐʨʻˁːˑ̀͠͡ͅΆΊΌΌΎΡΣώϐϖϚϚϜϜϞϞϠϠϢϳЁЌЎяёќўҁ҃҆ҐӄӇӈӋӌӐӫӮӵӸӹԱՖՙՙաֆֹֻֽֿֿׁׂ֑֣֡ׄׄאתװײءغـْ٠٩ٰڷںھۀێېۓە۪ۭۨ۰۹ँःअह़्॑॔क़ॣ०९ঁঃঅঌএঐওনপরললশহ়়াৄেৈো্ৗৗড়ঢ়য়ৣ০ৱਂਂਅਊਏਐਓਨਪਰਲਲ਼ਵਸ਼ਸਹ਼਼ਾੂੇੈੋ੍ਖ਼ੜਫ਼ਫ਼੦ੴઁઃઅઋઍઍએઑઓનપરલળવહ઼ૅેૉો્ૠૠ૦૯ଁଃଅଌଏଐଓନପରଲଳଶହ଼ୃେୈୋ୍ୖୗଡ଼ଢ଼ୟୡ୦୯ஂஃஅஊஎஐஒகஙசஜஜஞடணதநபமவஷஹாூெைொ்ௗௗ௧௯ఁఃఅఌఎఐఒనపళవహాౄెైొ్ౕౖౠౡ౦౯ಂಃಅಌಎಐಒನಪಳವಹಾೄೆೈೊ್ೕೖೞೞೠೡ೦೯ംഃഅഌഎഐഒനപഹാൃെൈൊ്ൗൗൠൡ൦൯กฮะฺเ๎๐๙ກຂຄຄງຈຊຊຍຍດທນຟມຣລລວວສຫອຮະູົຽເໄໆໆ່ໍ໐໙༘༙༠༩༹༹༵༵༷༷༾ཇཉཀྵ྄ཱ྆ྋྐྕྗྗྙྭྱྷྐྵྐྵႠჅაჶᄀᄀᄂᄃᄅᄇᄉᄉᄋᄌᄎᄒᄼᄼᄾᄾᅀᅀᅌᅌᅎᅎᅐᅐᅔᅕᅙᅙᅟᅡᅣᅣᅥᅥᅧᅧᅩᅩᅭᅮᅲᅳᅵᅵᆞᆞᆨᆨᆫᆫᆮᆯᆷᆸᆺᆺᆼᇂᇫᇫᇰᇰᇹᇹḀẛẠỹἀἕἘἝἠὅὈὍὐὗὙὙὛὛὝὝὟώᾀᾴᾶᾼιιῂῄῆῌῐΐῖΊῠῬῲῴῶῼ⃐⃜⃡⃡ΩΩKÅ℮℮ↀↂ々々〇〇〡〯〱〵ぁゔ゙゚ゝゞァヺーヾㄅㄬ一龥가힣";
    private static final String SPACES = "\t\n\r\r  ";
    private static Hashtable ranges = null;
    private static Hashtable ranges2 = null;

    public ParserForXMLSchema() {
    }

    public ParserForXMLSchema(Locale locale) {
        super(locale);
    }

    @Override
    Token processCaret() throws ParseException {
        next();
        return Token.createChar(94);
    }

    @Override
    Token processDollar() throws ParseException {
        next();
        return Token.createChar(36);
    }

    @Override
    Token processLookahead() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processNegativelookahead() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processLookbehind() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processNegativelookbehind() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processBacksolidus_A() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processBacksolidus_Z() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processBacksolidus_z() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processBacksolidus_b() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processBacksolidus_B() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processBacksolidus_lt() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processBacksolidus_gt() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processStar(Token tok) throws ParseException {
        next();
        return Token.createClosure(tok);
    }

    @Override
    Token processPlus(Token tok) throws ParseException {
        next();
        return Token.createConcat(tok, Token.createClosure(tok));
    }

    @Override
    Token processQuestion(Token tok) throws ParseException {
        next();
        Token par = Token.createUnion();
        par.addChild(tok);
        par.addChild(Token.createEmpty());
        return par;
    }

    @Override
    boolean checkQuestion(int off) {
        return false;
    }

    @Override
    Token processParen() throws ParseException {
        next();
        Token tok = Token.createParen(parseRegex(), 0);
        if (read() != 7) {
            throw ex("parser.factor.1", this.offset - 1);
        }
        next();
        return tok;
    }

    @Override
    Token processParen2() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processCondition() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processModifiers() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processIndependent() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token processBacksolidus_c() throws ParseException {
        next();
        return getTokenForShorthand(99);
    }

    @Override
    Token processBacksolidus_C() throws ParseException {
        next();
        return getTokenForShorthand(67);
    }

    @Override
    Token processBacksolidus_i() throws ParseException {
        next();
        return getTokenForShorthand(105);
    }

    @Override
    Token processBacksolidus_I() throws ParseException {
        next();
        return getTokenForShorthand(73);
    }

    @Override
    Token processBacksolidus_g() throws ParseException {
        throw ex("parser.process.1", this.offset - 2);
    }

    @Override
    Token processBacksolidus_X() throws ParseException {
        throw ex("parser.process.1", this.offset - 2);
    }

    @Override
    Token processBackreference() throws ParseException {
        throw ex("parser.process.1", this.offset - 4);
    }

    @Override
    int processCIinCharacterClass(RangeToken tok, int c) {
        tok.mergeRanges(getTokenForShorthand(c));
        return -1;
    }

    @Override
    protected RangeToken parseCharacterClass(boolean useNrange) throws ParseException {
        RangeToken tok;
        setContext(1);
        next();
        boolean nrange = false;
        RangeToken base = null;
        ?? r6 = 0;
        if (read() == 0 && this.chardata == 94) {
            nrange = true;
            next();
            base = Token.createRange();
            base.addRange(0, 1114111);
            tok = Token.createRange();
        } else {
            tok = Token.createRange();
        }
        boolean firstloop = true;
        while (true) {
            int type = read();
            if (type != 1) {
                if (type == 0 && this.chardata == 93 && !firstloop) {
                    if (nrange) {
                        base.subtractRanges(tok);
                        tok = base;
                    }
                } else {
                    int c = this.chardata;
                    boolean end = false;
                    if (type == 10) {
                        switch (c) {
                            case XPath.Tokens.EXPRTOKEN_AXISNAME_SELF:
                                c = decodeEscaped();
                                break;
                            case 67:
                            case 73:
                            case 99:
                            case 105:
                                c = processCIinCharacterClass(tok, c);
                                if (c < 0) {
                                    end = true;
                                }
                                break;
                            case 68:
                            case 83:
                            case 87:
                            case 100:
                            case 115:
                            case 119:
                                tok.mergeRanges(getTokenForShorthand(c));
                                end = true;
                                break;
                            case 80:
                            case 112:
                                int pstart = this.offset;
                                RangeToken tok2 = processBacksolidus_pP(c);
                                if (tok2 == null) {
                                    throw ex("parser.atom.5", pstart);
                                }
                                tok.mergeRanges(tok2);
                                end = true;
                                break;
                                break;
                            default:
                                c = decodeEscaped();
                                break;
                        }
                    } else if (type == 24 && !firstloop) {
                        if (nrange) {
                            base.subtractRanges(tok);
                            tok = base;
                        }
                        RangeToken range2 = parseCharacterClass(r6);
                        tok.subtractRanges(range2);
                        if (read() != 0 || this.chardata != 93) {
                            throw ex("parser.cc.5", this.offset);
                        }
                    }
                    next();
                    if (!end) {
                        if (type == 0) {
                            if (c == 91) {
                                throw ex("parser.cc.6", this.offset - 2);
                            }
                            if (c == 93) {
                                throw ex("parser.cc.7", this.offset - 2);
                            }
                            if (c == 45 && this.chardata != 93 && !firstloop) {
                                throw ex("parser.cc.8", this.offset - 2);
                            }
                        }
                        if (read() != 0 || this.chardata != 45 || (c == 45 && firstloop)) {
                            if (!isSet(2) || c > 65535) {
                                tok.addRange(c, c);
                            } else {
                                addCaseInsensitiveChar(tok, c);
                            }
                        } else {
                            next();
                            int type2 = read();
                            if (type2 == 1) {
                                throw ex("parser.cc.2", this.offset);
                            }
                            if (type2 == 0 && this.chardata == 93) {
                                if (!isSet(2) || c > 65535) {
                                    tok.addRange(c, c);
                                } else {
                                    addCaseInsensitiveChar(tok, c);
                                }
                                tok.addRange(45, 45);
                            } else {
                                if (type2 == 24) {
                                    throw ex("parser.cc.8", this.offset - 1);
                                }
                                int rangeend = this.chardata;
                                if (type2 == 0) {
                                    if (rangeend == 91) {
                                        throw ex("parser.cc.6", this.offset - 1);
                                    }
                                    if (rangeend == 93) {
                                        throw ex("parser.cc.7", this.offset - 1);
                                    }
                                    if (rangeend == 45) {
                                        throw ex("parser.cc.8", this.offset - 2);
                                    }
                                } else if (type2 == 10) {
                                    rangeend = decodeEscaped();
                                }
                                next();
                                if (c > rangeend) {
                                    throw ex("parser.ope.3", this.offset - 1);
                                }
                                if (!isSet(2) || (c > 65535 && rangeend > 65535)) {
                                    tok.addRange(c, rangeend);
                                } else {
                                    addCaseInsensitiveCharRange(tok, c, rangeend);
                                }
                            }
                        }
                    }
                    firstloop = false;
                    r6 = 0;
                }
            }
        }
    }

    @Override
    protected RangeToken parseSetOperations() throws ParseException {
        throw ex("parser.process.1", this.offset);
    }

    @Override
    Token getTokenForShorthand(int ch) {
        switch (ch) {
            case 67:
                return getRange("xml:isNameChar", false);
            case 68:
                return getRange("xml:isDigit", false);
            case 73:
                return getRange("xml:isInitialNameChar", false);
            case 83:
                return getRange("xml:isSpace", false);
            case 87:
                return getRange("xml:isWord", false);
            case 99:
                return getRange("xml:isNameChar", true);
            case 100:
                return getRange("xml:isDigit", true);
            case 105:
                return getRange("xml:isInitialNameChar", true);
            case 115:
                return getRange("xml:isSpace", true);
            case 119:
                return getRange("xml:isWord", true);
            default:
                throw new RuntimeException("Internal Error: shorthands: \\u" + Integer.toString(ch, 16));
        }
    }

    @Override
    int decodeEscaped() throws ParseException {
        if (read() != 10) {
            throw ex("parser.next.1", this.offset - 1);
        }
        int c = this.chardata;
        if (c == 63) {
            return c;
        }
        if (c == 110) {
            return 10;
        }
        if (c == 114) {
            return 13;
        }
        if (c == 116) {
            return 9;
        }
        switch (c) {
            case XPath.Tokens.EXPRTOKEN_AXISNAME_FOLLOWING_SIBLING:
            case XPath.Tokens.EXPRTOKEN_AXISNAME_NAMESPACE:
            case XPath.Tokens.EXPRTOKEN_AXISNAME_PARENT:
            case XPath.Tokens.EXPRTOKEN_AXISNAME_PRECEDING:
                return c;
            default:
                switch (c) {
                    case XPath.Tokens.EXPRTOKEN_AXISNAME_SELF:
                    case XPath.Tokens.EXPRTOKEN_LITERAL:
                        return c;
                    default:
                        switch (c) {
                            case 91:
                            case 92:
                            case 93:
                            case 94:
                                return c;
                            default:
                                switch (c) {
                                    case 123:
                                    case 124:
                                    case 125:
                                        return c;
                                    default:
                                        throw ex("parser.process.1", this.offset - 2);
                                }
                        }
                }
        }
    }

    protected static synchronized RangeToken getRange(String name, boolean positive) {
        RangeToken tok;
        if (ranges == null) {
            ranges = new Hashtable();
            ranges2 = new Hashtable();
            RangeToken rangeTokenCreateRange = Token.createRange();
            setupRange(rangeTokenCreateRange, SPACES);
            ranges.put("xml:isSpace", rangeTokenCreateRange);
            ranges2.put("xml:isSpace", Token.complementRanges(rangeTokenCreateRange));
            RangeToken rangeTokenCreateRange2 = Token.createRange();
            setupRange(rangeTokenCreateRange2, DIGITS);
            ranges.put("xml:isDigit", rangeTokenCreateRange2);
            ranges2.put("xml:isDigit", Token.complementRanges(rangeTokenCreateRange2));
            RangeToken rangeTokenCreateRange3 = Token.createRange();
            setupRange(rangeTokenCreateRange3, DIGITS);
            ranges.put("xml:isDigit", rangeTokenCreateRange3);
            ranges2.put("xml:isDigit", Token.complementRanges(rangeTokenCreateRange3));
            RangeToken rangeTokenCreateRange4 = Token.createRange();
            setupRange(rangeTokenCreateRange4, LETTERS);
            rangeTokenCreateRange4.mergeRanges((Token) ranges.get("xml:isDigit"));
            ranges.put("xml:isWord", rangeTokenCreateRange4);
            ranges2.put("xml:isWord", Token.complementRanges(rangeTokenCreateRange4));
            RangeToken rangeTokenCreateRange5 = Token.createRange();
            setupRange(rangeTokenCreateRange5, NAMECHARS);
            ranges.put("xml:isNameChar", rangeTokenCreateRange5);
            ranges2.put("xml:isNameChar", Token.complementRanges(rangeTokenCreateRange5));
            RangeToken rangeTokenCreateRange6 = Token.createRange();
            setupRange(rangeTokenCreateRange6, LETTERS);
            rangeTokenCreateRange6.addRange(95, 95);
            rangeTokenCreateRange6.addRange(58, 58);
            ranges.put("xml:isInitialNameChar", rangeTokenCreateRange6);
            ranges2.put("xml:isInitialNameChar", Token.complementRanges(rangeTokenCreateRange6));
        }
        tok = positive ? (RangeToken) ranges.get(name) : (RangeToken) ranges2.get(name);
        return tok;
    }

    static void setupRange(Token range, String src) {
        int len = src.length();
        for (int i = 0; i < len; i += 2) {
            range.addRange(src.charAt(i), src.charAt(i + 1));
        }
    }
}
