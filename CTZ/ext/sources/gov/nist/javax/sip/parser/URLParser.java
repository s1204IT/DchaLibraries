package gov.nist.javax.sip.parser;

import gov.nist.core.HostNameParser;
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
    public URLParser(String str) {
        this.lexer = new Lexer("sip_urlLexer", str);
    }

    public URLParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("sip_urlLexer");
    }

    protected static boolean isMark(char c) {
        if (c == '!' || c == '_' || c == '~') {
            return true;
        }
        switch (c) {
            case '\'':
            case '(':
            case ')':
            case '*':
                return true;
            default:
                switch (c) {
                    case '-':
                    case '.':
                        return true;
                    default:
                        return false;
                }
        }
    }

    protected static boolean isUnreserved(char c) {
        return Lexer.isAlphaDigit(c) || isMark(c);
    }

    protected static boolean isReservedNoSlash(char c) {
        switch (c) {
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

    protected static boolean isUserUnreserved(char c) {
        switch (c) {
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
        char cLookAhead = this.lexer.lookAhead(0);
        if (isUnreserved(cLookAhead)) {
            this.lexer.consume(1);
            return String.valueOf(cLookAhead);
        }
        throw createParseException("unreserved");
    }

    protected String paramNameOrValue() throws ParseException {
        int ptr = this.lexer.getPtr();
        while (this.lexer.hasMoreChars()) {
            char cLookAhead = this.lexer.lookAhead(0);
            if ((cLookAhead == '$' || cLookAhead == '&' || cLookAhead == '+' || cLookAhead == '/' || cLookAhead == ':' || cLookAhead == '[' || cLookAhead == ']') || isUnreserved(cLookAhead)) {
                this.lexer.consume(1);
            } else {
                if (!isEscaped()) {
                    break;
                }
                this.lexer.consume(3);
            }
        }
        return this.lexer.getBuffer().substring(ptr, this.lexer.getPtr());
    }

    private NameValue uriParam() throws ParseException {
        if (debug) {
            dbg_enter("uriParam");
        }
        try {
            String strParamNameOrValue = "";
            String strParamNameOrValue2 = paramNameOrValue();
            boolean z = false;
            if (this.lexer.lookAhead(0) == '=') {
                this.lexer.consume(1);
                strParamNameOrValue = paramNameOrValue();
            } else {
                z = true;
            }
            if (strParamNameOrValue2.length() != 0 || (strParamNameOrValue != null && strParamNameOrValue.length() != 0)) {
                NameValue nameValue = new NameValue(strParamNameOrValue2, strParamNameOrValue, z);
                if (debug) {
                    dbg_leave("uriParam");
                }
                return nameValue;
            }
            return null;
        } finally {
            if (debug) {
                dbg_leave("uriParam");
            }
        }
    }

    protected static boolean isReserved(char c) {
        switch (c) {
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
        char cLookAhead = this.lexer.lookAhead(0);
        if (isReserved(cLookAhead)) {
            this.lexer.consume(1);
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(cLookAhead);
            return stringBuffer.toString();
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
            StringBuffer stringBuffer = new StringBuffer();
            char cLookAhead = this.lexer.lookAhead(0);
            char cLookAhead2 = this.lexer.lookAhead(1);
            char cLookAhead3 = this.lexer.lookAhead(2);
            if (cLookAhead == '%' && Lexer.isHexDigit(cLookAhead2) && Lexer.isHexDigit(cLookAhead3)) {
                this.lexer.consume(3);
                stringBuffer.append(cLookAhead);
                stringBuffer.append(cLookAhead2);
                stringBuffer.append(cLookAhead3);
                return stringBuffer.toString();
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
            char cLookAhead = this.lexer.lookAhead(0);
            if (isMark(cLookAhead)) {
                this.lexer.consume(1);
                return new String(new char[]{cLookAhead});
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
            char cLookAhead = this.lexer.lookAhead(0);
            if (isUnreserved(cLookAhead)) {
                this.lexer.consume(1);
                String strCharAsString = Lexer.charAsString(cLookAhead);
                if (debug) {
                    dbg_leave("uric");
                }
                return strCharAsString;
            }
            if (isReserved(cLookAhead)) {
                this.lexer.consume(1);
                String strCharAsString2 = Lexer.charAsString(cLookAhead);
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
            String strCharAsString3 = this.lexer.charAsString(3);
            this.lexer.consume(3);
            if (debug) {
                dbg_leave("uric");
            }
            return strCharAsString3;
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
            char cLookAhead = this.lexer.lookAhead(0);
            if (isEscaped()) {
                String strCharAsString = this.lexer.charAsString(3);
                this.lexer.consume(3);
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return strCharAsString;
            }
            if (isUnreserved(cLookAhead)) {
                this.lexer.consume(1);
                String strCharAsString2 = Lexer.charAsString(cLookAhead);
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return strCharAsString2;
            }
            if (!isReservedNoSlash(cLookAhead)) {
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return null;
            }
            this.lexer.consume(1);
            String strCharAsString3 = Lexer.charAsString(cLookAhead);
            if (debug) {
                dbg_leave("uricNoSlash");
            }
            return strCharAsString3;
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
        StringBuffer stringBuffer = new StringBuffer();
        while (true) {
            String strUric = uric();
            if (strUric == null) {
                if (this.lexer.lookAhead(0) == '[') {
                    stringBuffer.append(new HostNameParser(getLexer()).hostPort(false).toString());
                } else {
                    return stringBuffer.toString();
                }
            } else {
                stringBuffer.append(strUric);
            }
        }
    }

    public GenericURI uriReference(boolean z) throws ParseException {
        GenericURI genericURISipURL;
        if (debug) {
            dbg_enter("uriReference");
        }
        Token[] tokenArrPeekNextToken = this.lexer.peekNextToken(2);
        Token token = tokenArrPeekNextToken[0];
        Token token2 = tokenArrPeekNextToken[1];
        try {
            if (token.getTokenType() == 2051 || token.getTokenType() == 2136) {
                if (token2.getTokenType() == 58) {
                    genericURISipURL = sipURL(z);
                } else {
                    throw createParseException("Expecting ':'");
                }
            } else if (token.getTokenType() == 2105) {
                if (token2.getTokenType() == 58) {
                    genericURISipURL = telURL(z);
                } else {
                    throw createParseException("Expecting ':'");
                }
            } else {
                try {
                    genericURISipURL = new GenericURI(uricString());
                } catch (ParseException e) {
                    throw createParseException(e.getMessage());
                }
            }
            return genericURISipURL;
        } finally {
            if (debug) {
                dbg_leave("uriReference");
            }
        }
    }

    private String base_phone_number() throws ParseException {
        StringBuffer stringBuffer = new StringBuffer();
        if (debug) {
            dbg_enter("base_phone_number");
        }
        int i = 0;
        while (true) {
            try {
                if (!this.lexer.hasMoreChars()) {
                    break;
                }
                char cLookAhead = this.lexer.lookAhead(0);
                if (!Lexer.isDigit(cLookAhead) && cLookAhead != '-' && cLookAhead != '.' && cLookAhead != '(' && cLookAhead != ')') {
                    if (i <= 0) {
                        throw createParseException("unexpected " + cLookAhead);
                    }
                }
                this.lexer.consume(1);
                stringBuffer.append(cLookAhead);
                i++;
            } finally {
                if (debug) {
                    dbg_leave("base_phone_number");
                }
            }
        }
    }

    private String local_number() throws ParseException {
        StringBuffer stringBuffer = new StringBuffer();
        if (debug) {
            dbg_enter("local_number");
        }
        int i = 0;
        while (true) {
            try {
                if (!this.lexer.hasMoreChars()) {
                    break;
                }
                char cLookAhead = this.lexer.lookAhead(0);
                if (cLookAhead != '*' && cLookAhead != '#' && cLookAhead != '-' && cLookAhead != '.' && cLookAhead != '(' && cLookAhead != ')' && !Lexer.isHexDigit(cLookAhead)) {
                    if (i <= 0) {
                        throw createParseException("unexepcted " + cLookAhead);
                    }
                }
                this.lexer.consume(1);
                stringBuffer.append(cLookAhead);
                i++;
            } finally {
                if (debug) {
                    dbg_leave("local_number");
                }
            }
        }
    }

    public final TelephoneNumber parseTelephoneNumber(boolean z) throws ParseException {
        TelephoneNumber telephoneNumberLocal_phone_number;
        if (debug) {
            dbg_enter("telephone_subscriber");
        }
        this.lexer.selectLexer("charLexer");
        try {
            char cLookAhead = this.lexer.lookAhead(0);
            if (cLookAhead == '+') {
                telephoneNumberLocal_phone_number = global_phone_number(z);
            } else {
                if (!Lexer.isHexDigit(cLookAhead) && cLookAhead != '#' && cLookAhead != '*' && cLookAhead != '-' && cLookAhead != '.' && cLookAhead != '(' && cLookAhead != ')') {
                    throw createParseException("unexpected char " + cLookAhead);
                }
                telephoneNumberLocal_phone_number = local_phone_number(z);
            }
            return telephoneNumberLocal_phone_number;
        } finally {
            if (debug) {
                dbg_leave("telephone_subscriber");
            }
        }
    }

    private final TelephoneNumber global_phone_number(boolean z) throws ParseException {
        if (debug) {
            dbg_enter("global_phone_number");
        }
        try {
            TelephoneNumber telephoneNumber = new TelephoneNumber();
            telephoneNumber.setGlobal(true);
            this.lexer.match(43);
            telephoneNumber.setPhoneNumber(base_phone_number());
            if (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) == ';' && z) {
                this.lexer.consume(1);
                telephoneNumber.setParameters(tel_parameters());
            }
            return telephoneNumber;
        } finally {
            if (debug) {
                dbg_leave("global_phone_number");
            }
        }
    }

    private TelephoneNumber local_phone_number(boolean z) throws ParseException {
        if (debug) {
            dbg_enter("local_phone_number");
        }
        TelephoneNumber telephoneNumber = new TelephoneNumber();
        telephoneNumber.setGlobal(false);
        try {
            telephoneNumber.setPhoneNumber(local_number());
            if (this.lexer.hasMoreChars() && this.lexer.peekNextToken().getTokenType() == 59 && z) {
                this.lexer.consume(1);
                telephoneNumber.setParameters(tel_parameters());
            }
            return telephoneNumber;
        } finally {
            if (debug) {
                dbg_leave("local_phone_number");
            }
        }
    }

    private NameValueList tel_parameters() throws ParseException {
        NameValue nameValue;
        NameValueList nameValueList = new NameValueList();
        while (true) {
            String strParamNameOrValue = paramNameOrValue();
            if (strParamNameOrValue.equalsIgnoreCase("phone-context")) {
                nameValue = phone_context();
            } else if (this.lexer.lookAhead(0) == '=') {
                this.lexer.consume(1);
                nameValue = new NameValue(strParamNameOrValue, paramNameOrValue(), false);
            } else {
                nameValue = new NameValue(strParamNameOrValue, "", true);
            }
            nameValueList.set(nameValue);
            if (this.lexer.lookAhead(0) == ';') {
                this.lexer.consume(1);
            } else {
                return nameValueList;
            }
        }
    }

    private NameValue phone_context() throws ParseException {
        String tokenValue;
        this.lexer.match(61);
        char cLookAhead = this.lexer.lookAhead(0);
        if (cLookAhead == '+') {
            this.lexer.consume(1);
            tokenValue = "+" + base_phone_number();
        } else if (Lexer.isAlphaDigit(cLookAhead)) {
            tokenValue = this.lexer.match(4095).getTokenValue();
        } else {
            throw new ParseException("Invalid phone-context:" + cLookAhead, -1);
        }
        return new NameValue("phone-context", tokenValue, false);
    }

    public TelURLImpl telURL(boolean z) throws ParseException {
        this.lexer.match(TokenTypes.TEL);
        this.lexer.match(58);
        TelephoneNumber telephoneNumber = parseTelephoneNumber(z);
        TelURLImpl telURLImpl = new TelURLImpl();
        telURLImpl.setTelephoneNumber(telephoneNumber);
        return telURLImpl;
    }

    public SipUri sipURL(boolean z) throws ParseException {
        if (debug) {
            dbg_enter("sipURL");
        }
        SipUri sipUri = new SipUri();
        Token tokenPeekNextToken = this.lexer.peekNextToken();
        int i = TokenTypes.SIP;
        String str = "sip";
        if (tokenPeekNextToken.getTokenType() == 2136) {
            str = "sips";
            i = 2136;
        }
        try {
            try {
                this.lexer.match(i);
                this.lexer.match(58);
                sipUri.setScheme(str);
                int iMarkInputPosition = this.lexer.markInputPosition();
                String strUser = user();
                String strPassword = null;
                if (this.lexer.lookAhead() == ':') {
                    this.lexer.consume(1);
                    strPassword = password();
                }
                if (this.lexer.lookAhead() == '@') {
                    this.lexer.consume(1);
                    sipUri.setUser(strUser);
                    if (strPassword != null) {
                        sipUri.setUserPassword(strPassword);
                    }
                } else {
                    this.lexer.rewindInputPosition(iMarkInputPosition);
                }
                sipUri.setHostPort(new HostNameParser(getLexer()).hostPort(false));
                this.lexer.selectLexer("charLexer");
                while (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) == ';' && z) {
                    this.lexer.consume(1);
                    NameValue nameValueUriParam = uriParam();
                    if (nameValueUriParam != null) {
                        sipUri.setUriParameter(nameValueUriParam);
                    }
                }
                if (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) == '?') {
                    this.lexer.consume(1);
                    while (this.lexer.hasMoreChars()) {
                        sipUri.setQHeader(qheader());
                        if (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) != '&') {
                            break;
                        }
                        this.lexer.consume(1);
                    }
                }
                return sipUri;
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
        Token[] tokenArrPeekNextToken = this.lexer.peekNextToken(1);
        if (tokenArrPeekNextToken.length == 0) {
            return null;
        }
        return tokenArrPeekNextToken[0].getTokenValue();
    }

    protected NameValue qheader() throws ParseException {
        String nextToken = this.lexer.getNextToken('=');
        this.lexer.consume(1);
        return new NameValue(nextToken, hvalue(), false);
    }

    protected String hvalue() throws ParseException {
        StringBuffer stringBuffer = new StringBuffer();
        while (this.lexer.hasMoreChars()) {
            boolean z = false;
            char cLookAhead = this.lexer.lookAhead(0);
            if (cLookAhead == '$' || cLookAhead == ':' || cLookAhead == '?' || cLookAhead == '[' || cLookAhead == ']' || cLookAhead == '_' || cLookAhead == '~') {
                z = true;
            } else {
                switch (cLookAhead) {
                    case '!':
                    case '\"':
                        z = true;
                        break;
                    default:
                        switch (cLookAhead) {
                            case '(':
                            case ')':
                            case '*':
                            case '+':
                                z = true;
                                break;
                            default:
                                switch (cLookAhead) {
                                    case '-':
                                    case '.':
                                    case '/':
                                        z = true;
                                        break;
                                }
                                break;
                        }
                        break;
                }
            }
            if (z || Lexer.isAlphaDigit(cLookAhead)) {
                this.lexer.consume(1);
                stringBuffer.append(cLookAhead);
            } else if (cLookAhead == '%') {
                stringBuffer.append(escaped());
            } else {
                return stringBuffer.toString();
            }
        }
        return stringBuffer.toString();
    }

    protected String urlString() throws ParseException {
        char cLookAhead;
        StringBuffer stringBuffer = new StringBuffer();
        this.lexer.selectLexer("charLexer");
        while (this.lexer.hasMoreChars() && (cLookAhead = this.lexer.lookAhead(0)) != ' ' && cLookAhead != '\t' && cLookAhead != '\n' && cLookAhead != '>' && cLookAhead != '<') {
            this.lexer.consume(0);
            stringBuffer.append(cLookAhead);
        }
        return stringBuffer.toString();
    }

    protected String user() throws ParseException {
        if (debug) {
            dbg_enter("user");
        }
        try {
            int ptr = this.lexer.getPtr();
            while (this.lexer.hasMoreChars()) {
                char cLookAhead = this.lexer.lookAhead(0);
                if (isUnreserved(cLookAhead) || isUserUnreserved(cLookAhead)) {
                    this.lexer.consume(1);
                } else {
                    if (!isEscaped()) {
                        break;
                    }
                    this.lexer.consume(3);
                }
            }
            return this.lexer.getBuffer().substring(ptr, this.lexer.getPtr());
        } finally {
            if (debug) {
                dbg_leave("user");
            }
        }
    }

    protected String password() throws ParseException {
        int ptr = this.lexer.getPtr();
        while (true) {
            boolean z = false;
            char cLookAhead = this.lexer.lookAhead(0);
            if (cLookAhead == '$' || cLookAhead == '&' || cLookAhead == '=') {
                z = true;
            } else {
                switch (cLookAhead) {
                    case '+':
                    case ',':
                        z = true;
                        break;
                }
            }
            if (z || isUnreserved(cLookAhead)) {
                this.lexer.consume(1);
            } else if (isEscaped()) {
                this.lexer.consume(3);
            } else {
                return this.lexer.getBuffer().substring(ptr, this.lexer.getPtr());
            }
        }
    }

    public GenericURI parse() throws ParseException {
        return uriReference(true);
    }

    public static void main(String[] strArr) throws ParseException {
        String[] strArr2 = {"sip:alice@example.com", "sips:alice@examples.com", "sip:3Zqkv5dajqaaas0tCjCxT0xH2ZEuEMsFl0xoasip%3A%2B3519116786244%40siplab.domain.com@213.0.115.163:7070"};
        for (int i = 0; i < strArr2.length; i++) {
            GenericURI genericURI = new URLParser(strArr2[i]).parse();
            System.out.println("uri type returned " + genericURI.getClass().getName());
            System.out.println(strArr2[i] + " is SipUri? " + genericURI.isSipURI() + Separators.GREATER_THAN + genericURI.encode());
        }
    }
}
