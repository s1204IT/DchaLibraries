package gov.nist.core;

import java.text.ParseException;
import javax.sip.header.WarningHeader;

public class HostNameParser extends ParserCore {
    private static LexerCore Lexer;
    private static final char[] VALID_DOMAIN_LABEL_CHAR = {65533, '-', '.'};
    private boolean stripAddressScopeZones;

    public HostNameParser(String hname) {
        this.stripAddressScopeZones = false;
        this.lexer = new LexerCore("charLexer", hname);
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
    }

    public HostNameParser(LexerCore lexer) {
        this.stripAddressScopeZones = false;
        this.lexer = lexer;
        lexer.selectLexer("charLexer");
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
        int stripLen;
        StringBuffer retval = new StringBuffer();
        if (debug) {
            dbg_enter("ipv6Reference");
        }
        try {
            if (!this.stripAddressScopeZones) {
                while (true) {
                    if (!this.lexer.hasMoreChars()) {
                        break;
                    }
                    char la = this.lexer.lookAhead(0);
                    if (LexerCore.isHexDigit(la) || la == '.' || la == ':' || la == '[') {
                        this.lexer.consume(1);
                        retval.append(la);
                    } else if (la == ']') {
                        this.lexer.consume(1);
                        retval.append(la);
                        String string = retval.toString();
                        if (debug) {
                            dbg_leave("ipv6Reference");
                        }
                        return string;
                    }
                }
            } else {
                while (true) {
                    if (!this.lexer.hasMoreChars()) {
                        break;
                    }
                    char la2 = this.lexer.lookAhead(0);
                    if (LexerCore.isHexDigit(la2) || la2 == '.' || la2 == ':' || la2 == '[') {
                        this.lexer.consume(1);
                        retval.append(la2);
                    } else {
                        if (la2 == ']') {
                            this.lexer.consume(1);
                            retval.append(la2);
                            return retval.toString();
                        }
                        if (la2 == '%') {
                            this.lexer.consume(1);
                            String rest = this.lexer.getRest();
                            if (rest != null && rest.length() != 0 && (stripLen = rest.indexOf(93)) != -1) {
                                this.lexer.consume(stripLen + 1);
                                retval.append("]");
                                String string2 = retval.toString();
                                if (debug) {
                                    dbg_leave("ipv6Reference");
                                }
                                return string2;
                            }
                        }
                    }
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
        String hostname;
        if (debug) {
            dbg_enter("host");
        }
        try {
            if (this.lexer.lookAhead(0) == '[') {
                hostname = ipv6Reference();
            } else if (isIPv6Address(this.lexer.getRest())) {
                int startPtr = this.lexer.getPtr();
                this.lexer.consumeValidChars(new char[]{65533, ':'});
                hostname = new StringBuffer("[").append(this.lexer.getBuffer().substring(startPtr, this.lexer.getPtr())).append("]").toString();
            } else {
                int startPtr2 = this.lexer.getPtr();
                consumeDomainLabel();
                hostname = this.lexer.getBuffer().substring(startPtr2, this.lexer.getPtr());
            }
            if (hostname.length() == 0) {
                throw new ParseException(this.lexer.getBuffer() + ": Missing host name", this.lexer.getPtr());
            }
            return new Host(hostname);
        } finally {
            if (debug) {
                dbg_leave("host");
            }
        }
    }

    private boolean isIPv6Address(String uriHeader) {
        int hostEnd = uriHeader.indexOf(63);
        int semiColonIndex = uriHeader.indexOf(59);
        if (hostEnd == -1 || (semiColonIndex != -1 && hostEnd > semiColonIndex)) {
            hostEnd = semiColonIndex;
        }
        if (hostEnd == -1) {
            hostEnd = uriHeader.length();
        }
        String host = uriHeader.substring(0, hostEnd);
        int firstColonIndex = host.indexOf(58);
        if (firstColonIndex == -1) {
            return false;
        }
        int secondColonIndex = host.indexOf(58, firstColonIndex + 1);
        return secondColonIndex != -1;
    }

    public HostPort hostPort(boolean allowWS) throws ParseException {
        if (debug) {
            dbg_enter("hostPort");
        }
        try {
            Host host = host();
            HostPort hp = new HostPort();
            hp.setHost(host);
            if (allowWS) {
                this.lexer.SPorHT();
            }
            if (this.lexer.hasMoreChars()) {
                char la = this.lexer.lookAhead(0);
                switch (la) {
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
                            if (!allowWS) {
                                throw new ParseException(this.lexer.getBuffer() + " Illegal character in hostname:" + this.lexer.lookAhead(0), this.lexer.getPtr());
                            }
                        }
                        break;
                    case ':':
                        this.lexer.consume(1);
                        if (allowWS) {
                            this.lexer.SPorHT();
                        }
                        try {
                            String port = this.lexer.number();
                            hp.setPort(Integer.parseInt(port));
                        } catch (NumberFormatException e) {
                            throw new ParseException(this.lexer.getBuffer() + " :Error parsing port ", this.lexer.getPtr());
                        }
                        break;
                }
            }
            return hp;
        } finally {
            if (debug) {
                dbg_leave("hostPort");
            }
        }
    }

    public static void main(String[] args) throws ParseException {
        String[] hostNames = {"foo.bar.com:1234", "proxima.chaplin.bt.co.uk", "129.6.55.181:2345", ":1234", "foo.bar.com:         1234", "foo.bar.com     :      1234   ", "MIK_S:1234"};
        for (String str : hostNames) {
            try {
                HostNameParser hnp = new HostNameParser(str);
                HostPort hp = hnp.hostPort(true);
                System.out.println("[" + hp.encode() + "]");
            } catch (ParseException ex) {
                System.out.println("exception text = " + ex.getMessage());
            }
        }
    }
}
