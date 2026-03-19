package gov.nist.javax.sip.parser;

import gov.nist.core.HostNameParser;
import gov.nist.core.HostPort;
import gov.nist.core.NameValue;
import gov.nist.core.NameValueList;
import gov.nist.core.Separators;
import gov.nist.core.Token;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.address.TelURLImpl;
import gov.nist.javax.sip.address.TelephoneNumber;
import java.text.ParseException;

public class URLParser extends Parser {
    public URLParser(String url) {
        this.lexer = new Lexer("sip_urlLexer", url);
    }

    public URLParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("sip_urlLexer");
    }

    protected static boolean isMark(char next) {
        switch (next) {
            case '!':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '-':
            case '.':
            case '_':
            case '~':
                return true;
            default:
                return false;
        }
    }

    protected static boolean isUnreserved(char next) {
        if (Lexer.isAlphaDigit(next)) {
            return true;
        }
        return isMark(next);
    }

    protected static boolean isReservedNoSlash(char next) {
        switch (next) {
            case '$':
            case '&':
            case '+':
            case ',':
            case ':':
            case ';':
            case '?':
            case '@':
                return true;
            default:
                return false;
        }
    }

    protected static boolean isUserUnreserved(char la) {
        switch (la) {
            case '#':
            case '$':
            case '&':
            case '+':
            case ',':
            case '/':
            case ';':
            case '=':
            case '?':
                return true;
            default:
                return false;
        }
    }

    protected String unreserved() throws ParseException {
        char next = this.lexer.lookAhead(0);
        if (isUnreserved(next)) {
            this.lexer.consume(1);
            return String.valueOf(next);
        }
        throw createParseException("unreserved");
    }

    protected String paramNameOrValue() throws ParseException {
        int startIdx = this.lexer.getPtr();
        while (this.lexer.hasMoreChars()) {
            char next = this.lexer.lookAhead(0);
            boolean isValidChar = false;
            switch (next) {
                case '$':
                case '&':
                case '+':
                case '/':
                case ':':
                case '[':
                case ']':
                    isValidChar = true;
                    break;
            }
            if (isValidChar || isUnreserved(next)) {
                this.lexer.consume(1);
            } else if (isEscaped()) {
                this.lexer.consume(3);
            } else {
                return this.lexer.getBuffer().substring(startIdx, this.lexer.getPtr());
            }
        }
        return this.lexer.getBuffer().substring(startIdx, this.lexer.getPtr());
    }

    private NameValue uriParam() throws ParseException {
        if (debug) {
            dbg_enter("uriParam");
        }
        try {
            String pvalue = "";
            String pname = paramNameOrValue();
            char next = this.lexer.lookAhead(0);
            boolean isFlagParam = true;
            if (next == '=') {
                this.lexer.consume(1);
                pvalue = paramNameOrValue();
                isFlagParam = false;
            }
            if (pname.length() == 0 && (pvalue == null || pvalue.length() == 0)) {
                return null;
            }
            NameValue nameValue = new NameValue(pname, pvalue, isFlagParam);
            if (debug) {
                dbg_leave("uriParam");
            }
            return nameValue;
        } finally {
            if (debug) {
                dbg_leave("uriParam");
            }
        }
    }

    protected static boolean isReserved(char next) {
        switch (next) {
            case '$':
            case '&':
            case '+':
            case ',':
            case '/':
            case ':':
            case ';':
            case '=':
            case '?':
            case '@':
                return true;
            default:
                return false;
        }
    }

    protected String reserved() throws ParseException {
        char next = this.lexer.lookAhead(0);
        if (isReserved(next)) {
            this.lexer.consume(1);
            return new StringBuffer().append(next).toString();
        }
        throw createParseException("reserved");
    }

    protected boolean isEscaped() {
        try {
            if (this.lexer.lookAhead(0) == '%' && Lexer.isHexDigit(this.lexer.lookAhead(1))) {
                return Lexer.isHexDigit(this.lexer.lookAhead(2));
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    protected String escaped() throws ParseException {
        if (debug) {
            dbg_enter("escaped");
        }
        try {
            StringBuffer retval = new StringBuffer();
            char next = this.lexer.lookAhead(0);
            char next1 = this.lexer.lookAhead(1);
            char next2 = this.lexer.lookAhead(2);
            if (next == '%' && Lexer.isHexDigit(next1) && Lexer.isHexDigit(next2)) {
                this.lexer.consume(3);
                retval.append(next);
                retval.append(next1);
                retval.append(next2);
                return retval.toString();
            }
            throw createParseException("escaped");
        } finally {
            if (debug) {
                dbg_leave("escaped");
            }
        }
    }

    protected String mark() throws ParseException {
        if (debug) {
            dbg_enter("mark");
        }
        try {
            char next = this.lexer.lookAhead(0);
            if (isMark(next)) {
                this.lexer.consume(1);
                return new String(new char[]{next});
            }
            throw createParseException("mark");
        } finally {
            if (debug) {
                dbg_leave("mark");
            }
        }
    }

    protected String uric() {
        if (debug) {
            dbg_enter("uric");
        }
        try {
            char la = this.lexer.lookAhead(0);
            if (isUnreserved(la)) {
                this.lexer.consume(1);
                String strCharAsString = Lexer.charAsString(la);
                if (debug) {
                    dbg_leave("uric");
                }
                return strCharAsString;
            }
            if (isReserved(la)) {
                this.lexer.consume(1);
                String strCharAsString2 = Lexer.charAsString(la);
                if (debug) {
                    dbg_leave("uric");
                }
                return strCharAsString2;
            }
            if (!isEscaped()) {
                if (debug) {
                    dbg_leave("uric");
                }
                return null;
            }
            String retval = this.lexer.charAsString(3);
            this.lexer.consume(3);
            if (debug) {
                dbg_leave("uric");
            }
            return retval;
        } catch (Exception e) {
            if (debug) {
                dbg_leave("uric");
            }
            return null;
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("uric");
            }
            throw th;
        }
    }

    protected String uricNoSlash() {
        if (debug) {
            dbg_enter("uricNoSlash");
        }
        try {
            char la = this.lexer.lookAhead(0);
            if (isEscaped()) {
                String retval = this.lexer.charAsString(3);
                this.lexer.consume(3);
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return retval;
            }
            if (isUnreserved(la)) {
                this.lexer.consume(1);
                String strCharAsString = Lexer.charAsString(la);
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return strCharAsString;
            }
            if (!isReservedNoSlash(la)) {
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return null;
            }
            this.lexer.consume(1);
            String strCharAsString2 = Lexer.charAsString(la);
            if (debug) {
                dbg_leave("uricNoSlash");
            }
            return strCharAsString2;
        } catch (ParseException e) {
            if (debug) {
                dbg_leave("uricNoSlash");
            }
            return null;
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("uricNoSlash");
            }
            throw th;
        }
    }

    protected String uricString() throws ParseException {
        StringBuffer retval = new StringBuffer();
        while (true) {
            String next = uric();
            if (next == null) {
                char la = this.lexer.lookAhead(0);
                if (la == '[') {
                    HostNameParser hnp = new HostNameParser(getLexer());
                    HostPort hp = hnp.hostPort(false);
                    retval.append(hp.toString());
                } else {
                    return retval.toString();
                }
            } else {
                retval.append(next);
            }
        }
    }

    public GenericURI uriReference(boolean inBrackets) throws ParseException {
        GenericURI retval;
        if (debug) {
            dbg_enter("uriReference");
        }
        Token[] tokens = this.lexer.peekNextToken(2);
        Token t1 = tokens[0];
        Token t2 = tokens[1];
        try {
            if (t1.getTokenType() == 2051 || t1.getTokenType() == 2136) {
                if (t2.getTokenType() == 58) {
                    retval = sipURL(inBrackets);
                } else {
                    throw createParseException("Expecting ':'");
                }
            } else if (t1.getTokenType() == 2105) {
                if (t2.getTokenType() == 58) {
                    retval = telURL(inBrackets);
                } else {
                    throw createParseException("Expecting ':'");
                }
            } else {
                String urlString = uricString();
                try {
                    retval = new GenericURI(urlString);
                } catch (ParseException ex) {
                    throw createParseException(ex.getMessage());
                }
            }
            return retval;
        } finally {
            if (debug) {
                dbg_leave("uriReference");
            }
        }
    }

    private String base_phone_number() throws ParseException {
        StringBuffer s = new StringBuffer();
        if (debug) {
            dbg_enter("base_phone_number");
        }
        int lc = 0;
        while (true) {
            try {
                if (!this.lexer.hasMoreChars()) {
                    break;
                }
                char w = this.lexer.lookAhead(0);
                if (Lexer.isDigit(w) || w == '-' || w == '.' || w == '(' || w == ')') {
                    this.lexer.consume(1);
                    s.append(w);
                    lc++;
                } else if (lc <= 0) {
                    throw createParseException("unexpected " + w);
                }
            } finally {
                if (debug) {
                    dbg_leave("base_phone_number");
                }
            }
        }
    }

    private String local_number() throws ParseException {
        StringBuffer s = new StringBuffer();
        if (debug) {
            dbg_enter("local_number");
        }
        int lc = 0;
        while (true) {
            try {
                if (!this.lexer.hasMoreChars()) {
                    break;
                }
                char la = this.lexer.lookAhead(0);
                if (la == '*' || la == '#' || la == '-' || la == '.' || la == '(' || la == ')' || Lexer.isHexDigit(la)) {
                    this.lexer.consume(1);
                    s.append(la);
                    lc++;
                } else if (lc <= 0) {
                    throw createParseException("unexepcted " + la);
                }
            } finally {
                if (debug) {
                    dbg_leave("local_number");
                }
            }
        }
    }

    public final TelephoneNumber parseTelephoneNumber(boolean inBrackets) throws ParseException {
        TelephoneNumber tn;
        if (debug) {
            dbg_enter("telephone_subscriber");
        }
        this.lexer.selectLexer("charLexer");
        try {
            char c = this.lexer.lookAhead(0);
            if (c == '+') {
                tn = global_phone_number(inBrackets);
            } else if (Lexer.isHexDigit(c) || c == '#' || c == '*' || c == '-' || c == '.' || c == '(' || c == ')') {
                tn = local_phone_number(inBrackets);
            } else {
                throw createParseException("unexpected char " + c);
            }
            return tn;
        } finally {
            if (debug) {
                dbg_leave("telephone_subscriber");
            }
        }
    }

    private final TelephoneNumber global_phone_number(boolean inBrackets) throws ParseException {
        if (debug) {
            dbg_enter("global_phone_number");
        }
        try {
            TelephoneNumber tn = new TelephoneNumber();
            tn.setGlobal(true);
            this.lexer.match(43);
            String b = base_phone_number();
            tn.setPhoneNumber(b);
            if (this.lexer.hasMoreChars()) {
                char tok = this.lexer.lookAhead(0);
                if (tok == ';' && inBrackets) {
                    this.lexer.consume(1);
                    NameValueList nv = tel_parameters();
                    tn.setParameters(nv);
                }
            }
            return tn;
        } finally {
            if (debug) {
                dbg_leave("global_phone_number");
            }
        }
    }

    private TelephoneNumber local_phone_number(boolean inBrackets) throws ParseException {
        if (debug) {
            dbg_enter("local_phone_number");
        }
        TelephoneNumber tn = new TelephoneNumber();
        tn.setGlobal(false);
        try {
            String b = local_number();
            tn.setPhoneNumber(b);
        } finally {
            if (debug) {
            }
        }
        if (this.lexer.hasMoreChars()) {
            Token tok = this.lexer.peekNextToken();
            switch (tok.getTokenType()) {
                case 59:
                    if (inBrackets) {
                        this.lexer.consume(1);
                        NameValueList nv = tel_parameters();
                        tn.setParameters(nv);
                        break;
                    }
                default:
                    return tn;
            }
            if (debug) {
                dbg_leave("local_phone_number");
            }
        }
        return tn;
    }

    private NameValueList tel_parameters() throws ParseException {
        NameValue nv;
        NameValueList nvList = new NameValueList();
        while (true) {
            String pname = paramNameOrValue();
            if (pname.equalsIgnoreCase("phone-context")) {
                nv = phone_context();
            } else if (this.lexer.lookAhead(0) == '=') {
                this.lexer.consume(1);
                String value = paramNameOrValue();
                nv = new NameValue(pname, value, false);
            } else {
                nv = new NameValue(pname, "", true);
            }
            nvList.set(nv);
            if (this.lexer.lookAhead(0) == ';') {
                this.lexer.consume(1);
            } else {
                return nvList;
            }
        }
    }

    private NameValue phone_context() throws ParseException {
        Object value;
        this.lexer.match(61);
        char la = this.lexer.lookAhead(0);
        if (la == '+') {
            this.lexer.consume(1);
            value = "+" + base_phone_number();
        } else if (Lexer.isAlphaDigit(la)) {
            Token t = this.lexer.match(4095);
            value = t.getTokenValue();
        } else {
            throw new ParseException("Invalid phone-context:" + la, -1);
        }
        return new NameValue("phone-context", value, false);
    }

    public TelURLImpl telURL(boolean inBrackets) throws ParseException {
        this.lexer.match(TokenTypes.TEL);
        this.lexer.match(58);
        TelephoneNumber tn = parseTelephoneNumber(inBrackets);
        TelURLImpl telUrl = new TelURLImpl();
        telUrl.setTelephoneNumber(tn);
        return telUrl;
    }

    public SipUri sipURL(boolean inBrackets) throws ParseException {
        if (debug) {
            dbg_enter("sipURL");
        }
        SipUri retval = new SipUri();
        Token nextToken = this.lexer.peekNextToken();
        int sipOrSips = TokenTypes.SIP;
        String scheme = "sip";
        if (nextToken.getTokenType() == 2136) {
            sipOrSips = TokenTypes.SIPS;
            scheme = "sips";
        }
        try {
            try {
                this.lexer.match(sipOrSips);
                this.lexer.match(58);
                retval.setScheme(scheme);
                int startOfUser = this.lexer.markInputPosition();
                String userOrHost = user();
                String passOrPort = null;
                if (this.lexer.lookAhead() == ':') {
                    this.lexer.consume(1);
                    passOrPort = password();
                }
                if (this.lexer.lookAhead() == '@') {
                    this.lexer.consume(1);
                    retval.setUser(userOrHost);
                    if (passOrPort != null) {
                        retval.setUserPassword(passOrPort);
                    }
                } else {
                    this.lexer.rewindInputPosition(startOfUser);
                }
                HostNameParser hnp = new HostNameParser(getLexer());
                HostPort hp = hnp.hostPort(false);
                retval.setHostPort(hp);
                this.lexer.selectLexer("charLexer");
                while (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) == ';' && inBrackets) {
                    this.lexer.consume(1);
                    NameValue parms = uriParam();
                    if (parms != null) {
                        retval.setUriParameter(parms);
                    }
                }
                if (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) == '?') {
                    this.lexer.consume(1);
                    while (this.lexer.hasMoreChars()) {
                        retval.setQHeader(qheader());
                        if (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) != '&') {
                            break;
                        }
                        this.lexer.consume(1);
                    }
                }
                return retval;
            } catch (RuntimeException e) {
                throw new ParseException("Invalid URL: " + this.lexer.getBuffer(), -1);
            }
        } finally {
            if (debug) {
                dbg_leave("sipURL");
            }
        }
    }

    public String peekScheme() throws ParseException {
        Token[] tokens = this.lexer.peekNextToken(1);
        if (tokens.length == 0) {
            return null;
        }
        String scheme = tokens[0].getTokenValue();
        return scheme;
    }

    protected NameValue qheader() throws ParseException {
        String name = this.lexer.getNextToken('=');
        this.lexer.consume(1);
        String value = hvalue();
        return new NameValue(name, value, false);
    }

    protected String hvalue() throws ParseException {
        StringBuffer retval = new StringBuffer();
        while (this.lexer.hasMoreChars()) {
            char la = this.lexer.lookAhead(0);
            boolean isValidChar = false;
            switch (la) {
                case '!':
                case '\"':
                case '$':
                case '(':
                case ')':
                case '*':
                case '+':
                case '-':
                case '.':
                case '/':
                case ':':
                case '?':
                case '[':
                case ']':
                case '_':
                case '~':
                    isValidChar = true;
                    break;
            }
            if (isValidChar || Lexer.isAlphaDigit(la)) {
                this.lexer.consume(1);
                retval.append(la);
            } else if (la == '%') {
                retval.append(escaped());
            } else {
                return retval.toString();
            }
        }
        return retval.toString();
    }

    protected String urlString() throws ParseException {
        char la;
        StringBuffer retval = new StringBuffer();
        this.lexer.selectLexer("charLexer");
        while (this.lexer.hasMoreChars() && (la = this.lexer.lookAhead(0)) != ' ' && la != '\t' && la != '\n' && la != '>' && la != '<') {
            this.lexer.consume(0);
            retval.append(la);
        }
        return retval.toString();
    }

    protected String user() throws ParseException {
        if (debug) {
            dbg_enter("user");
        }
        try {
            int startIdx = this.lexer.getPtr();
            while (this.lexer.hasMoreChars()) {
                char la = this.lexer.lookAhead(0);
                if (isUnreserved(la) || isUserUnreserved(la)) {
                    this.lexer.consume(1);
                } else {
                    if (!isEscaped()) {
                        break;
                    }
                    this.lexer.consume(3);
                }
            }
            return this.lexer.getBuffer().substring(startIdx, this.lexer.getPtr());
        } finally {
            if (debug) {
                dbg_leave("user");
            }
        }
    }

    protected String password() throws ParseException {
        int startIdx = this.lexer.getPtr();
        while (true) {
            char la = this.lexer.lookAhead(0);
            boolean isValidChar = false;
            switch (la) {
                case '$':
                case '&':
                case '+':
                case ',':
                case '=':
                    isValidChar = true;
                    break;
            }
            if (isValidChar || isUnreserved(la)) {
                this.lexer.consume(1);
            } else if (isEscaped()) {
                this.lexer.consume(3);
            } else {
                return this.lexer.getBuffer().substring(startIdx, this.lexer.getPtr());
            }
        }
    }

    public GenericURI parse() throws ParseException {
        return uriReference(true);
    }

    public static void main(String[] args) throws ParseException {
        String[] test = {"sip:alice@example.com", "sips:alice@examples.com", "sip:3Zqkv5dajqaaas0tCjCxT0xH2ZEuEMsFl0xoasip%3A%2B3519116786244%40siplab.domain.com@213.0.115.163:7070"};
        for (int i = 0; i < test.length; i++) {
            URLParser p = new URLParser(test[i]);
            GenericURI uri = p.parse();
            System.out.println("uri type returned " + uri.getClass().getName());
            System.out.println(test[i] + " is SipUri? " + uri.isSipURI() + Separators.GREATER_THAN + uri.encode());
        }
    }
}
