package gov.nist.core;

import java.text.ParseException;
import javax.sip.header.WarningHeader;

public class HostNameParser extends ParserCore {
    private static LexerCore Lexer;
    private static final char[] VALID_DOMAIN_LABEL_CHAR = {65533, '-', '.'};
    private boolean stripAddressScopeZones;

    public HostNameParser(String str) {
        this.stripAddressScopeZones = false;
        this.lexer = new LexerCore("charLexer", str);
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
    }

    public HostNameParser(LexerCore lexerCore) {
        this.stripAddressScopeZones = false;
        this.lexer = lexerCore;
        lexerCore.selectLexer("charLexer");
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
    }

    protected void consumeDomainLabel() throws ParseException {
        if (debug) {
            dbg_enter("domainLabel");
        }
        try {
            this.lexer.consumeValidChars(VALID_DOMAIN_LABEL_CHAR);
        } finally {
            if (debug) {
                dbg_leave("domainLabel");
            }
        }
    }

    protected String ipv6Reference() throws ParseException {
        int iIndexOf;
        StringBuffer stringBuffer = new StringBuffer();
        if (debug) {
            dbg_enter("ipv6Reference");
        }
        try {
            if (!this.stripAddressScopeZones) {
                while (true) {
                    if (!this.lexer.hasMoreChars()) {
                        break;
                    }
                    char cLookAhead = this.lexer.lookAhead(0);
                    if (!LexerCore.isHexDigit(cLookAhead) && cLookAhead != '.' && cLookAhead != ':' && cLookAhead != '[') {
                        if (cLookAhead == ']') {
                            this.lexer.consume(1);
                            stringBuffer.append(cLookAhead);
                            String string = stringBuffer.toString();
                            if (debug) {
                                dbg_leave("ipv6Reference");
                            }
                            return string;
                        }
                    }
                    this.lexer.consume(1);
                    stringBuffer.append(cLookAhead);
                }
            } else {
                while (true) {
                    if (!this.lexer.hasMoreChars()) {
                        break;
                    }
                    char cLookAhead2 = this.lexer.lookAhead(0);
                    if (!LexerCore.isHexDigit(cLookAhead2) && cLookAhead2 != '.' && cLookAhead2 != ':' && cLookAhead2 != '[') {
                        if (cLookAhead2 == ']') {
                            this.lexer.consume(1);
                            stringBuffer.append(cLookAhead2);
                            return stringBuffer.toString();
                        }
                        if (cLookAhead2 == '%') {
                            this.lexer.consume(1);
                            String rest = this.lexer.getRest();
                            if (rest != null && rest.length() != 0 && (iIndexOf = rest.indexOf(93)) != -1) {
                                this.lexer.consume(iIndexOf + 1);
                                stringBuffer.append("]");
                                String string2 = stringBuffer.toString();
                                if (debug) {
                                    dbg_leave("ipv6Reference");
                                }
                                return string2;
                            }
                        }
                    }
                    this.lexer.consume(1);
                    stringBuffer.append(cLookAhead2);
                }
            }
            throw new ParseException(this.lexer.getBuffer() + ": Illegal Host name ", this.lexer.getPtr());
        } finally {
            if (debug) {
                dbg_leave("ipv6Reference");
            }
        }
    }

    public Host host() throws ParseException {
        String strSubstring;
        if (debug) {
            dbg_enter("host");
        }
        try {
            if (this.lexer.lookAhead(0) == '[') {
                strSubstring = ipv6Reference();
            } else if (isIPv6Address(this.lexer.getRest())) {
                int ptr = this.lexer.getPtr();
                this.lexer.consumeValidChars(new char[]{65533, ':'});
                StringBuffer stringBuffer = new StringBuffer("[");
                stringBuffer.append(this.lexer.getBuffer().substring(ptr, this.lexer.getPtr()));
                stringBuffer.append("]");
                strSubstring = stringBuffer.toString();
            } else {
                int ptr2 = this.lexer.getPtr();
                consumeDomainLabel();
                strSubstring = this.lexer.getBuffer().substring(ptr2, this.lexer.getPtr());
            }
            if (strSubstring.length() == 0) {
                throw new ParseException(this.lexer.getBuffer() + ": Missing host name", this.lexer.getPtr());
            }
            return new Host(strSubstring);
        } finally {
            if (debug) {
                dbg_leave("host");
            }
        }
    }

    private boolean isIPv6Address(String str) {
        LexerCore lexerCore = Lexer;
        int iIndexOf = str.indexOf(63);
        LexerCore lexerCore2 = Lexer;
        int iIndexOf2 = str.indexOf(59);
        if (iIndexOf == -1 || (iIndexOf2 != -1 && iIndexOf > iIndexOf2)) {
            iIndexOf = iIndexOf2;
        }
        if (iIndexOf == -1) {
            iIndexOf = str.length();
        }
        String strSubstring = str.substring(0, iIndexOf);
        LexerCore lexerCore3 = Lexer;
        int iIndexOf3 = strSubstring.indexOf(58);
        if (iIndexOf3 == -1) {
            return false;
        }
        LexerCore lexerCore4 = Lexer;
        return strSubstring.indexOf(58, iIndexOf3 + 1) != -1;
    }

    public HostPort hostPort(boolean z) throws ParseException {
        if (debug) {
            dbg_enter("hostPort");
        }
        try {
            Host host = host();
            HostPort hostPort = new HostPort();
            hostPort.setHost(host);
            if (z) {
                this.lexer.SPorHT();
            }
            if (this.lexer.hasMoreChars()) {
                switch (this.lexer.lookAhead(0)) {
                    case '\t':
                    case WarningHeader.ATTRIBUTE_NOT_UNDERSTOOD:
                    case '\r':
                    case ' ':
                    case ',':
                    case '/':
                    case ';':
                    case '>':
                    case '?':
                        break;
                    case '%':
                        if (!this.stripAddressScopeZones) {
                            if (z) {
                                throw new ParseException(this.lexer.getBuffer() + " Illegal character in hostname:" + this.lexer.lookAhead(0), this.lexer.getPtr());
                            }
                        }
                        break;
                    case ':':
                        this.lexer.consume(1);
                        if (z) {
                            this.lexer.SPorHT();
                        }
                        try {
                            hostPort.setPort(Integer.parseInt(this.lexer.number()));
                        } catch (NumberFormatException e) {
                            throw new ParseException(this.lexer.getBuffer() + " :Error parsing port ", this.lexer.getPtr());
                        }
                        break;
                    default:
                        if (z) {
                        }
                        break;
                }
            }
            return hostPort;
        } finally {
            if (debug) {
                dbg_leave("hostPort");
            }
        }
    }

    public static void main(String[] strArr) throws ParseException {
        for (String str : new String[]{"foo.bar.com:1234", "proxima.chaplin.bt.co.uk", "129.6.55.181:2345", ":1234", "foo.bar.com:         1234", "foo.bar.com     :      1234   ", "MIK_S:1234"}) {
            try {
                System.out.println("[" + new HostNameParser(str).hostPort(true).encode() + "]");
            } catch (ParseException e) {
                System.out.println("exception text = " + e.getMessage());
            }
        }
    }
}
