package gov.nist.javax.sip.parser;

import gov.nist.core.HostNameParser;
import gov.nist.core.HostPort;
import gov.nist.core.NameValue;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.Protocol;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import java.text.ParseException;

public class ViaParser extends HeaderParser {
    public ViaParser(String via) {
        super(via);
    }

    public ViaParser(Lexer lexer) {
        super(lexer);
    }

    private void parseVia(Via v) throws ParseException {
        this.lexer.match(4095);
        Token protocolName = this.lexer.getNextToken();
        this.lexer.SPorHT();
        this.lexer.match(47);
        this.lexer.SPorHT();
        this.lexer.match(4095);
        this.lexer.SPorHT();
        Token protocolVersion = this.lexer.getNextToken();
        this.lexer.SPorHT();
        this.lexer.match(47);
        this.lexer.SPorHT();
        this.lexer.match(4095);
        this.lexer.SPorHT();
        Token transport = this.lexer.getNextToken();
        this.lexer.SPorHT();
        Protocol protocol = new Protocol();
        protocol.setProtocolName(protocolName.getTokenValue());
        protocol.setProtocolVersion(protocolVersion.getTokenValue());
        protocol.setTransport(transport.getTokenValue());
        v.setSentProtocol(protocol);
        HostNameParser hnp = new HostNameParser(getLexer());
        HostPort hostPort = hnp.hostPort(true);
        v.setSentBy(hostPort);
        this.lexer.SPorHT();
        while (this.lexer.lookAhead(0) == ';') {
            this.lexer.consume(1);
            this.lexer.SPorHT();
            NameValue nameValue = nameValue();
            String name = nameValue.getName();
            if (name.equals("branch")) {
                String branchId = (String) nameValue.getValueAsObject();
                if (branchId == null) {
                    throw new ParseException("null branch Id", this.lexer.getPtr());
                }
            }
            v.setParameter(nameValue);
            this.lexer.SPorHT();
        }
        if (this.lexer.lookAhead(0) != '(') {
            return;
        }
        this.lexer.selectLexer("charLexer");
        this.lexer.consume(1);
        StringBuffer comment = new StringBuffer();
        while (true) {
            char ch = this.lexer.lookAhead(0);
            if (ch == ')') {
                this.lexer.consume(1);
                break;
            }
            if (ch == '\\') {
                Token tok = this.lexer.getNextToken();
                comment.append(tok.getTokenValue());
                this.lexer.consume(1);
                Token tok2 = this.lexer.getNextToken();
                comment.append(tok2.getTokenValue());
                this.lexer.consume(1);
            } else {
                if (ch == '\n') {
                    break;
                }
                comment.append(ch);
                this.lexer.consume(1);
            }
        }
        v.setComment(comment.toString());
    }

    @Override
    protected NameValue nameValue() throws ParseException {
        String str;
        if (debug) {
            dbg_enter("nameValue");
        }
        try {
            this.lexer.match(4095);
            Token name = this.lexer.getNextToken();
            this.lexer.SPorHT();
            boolean quoted = false;
            try {
                char la = this.lexer.lookAhead(0);
                if (la != '=') {
                    NameValue nameValue = new NameValue(name.getTokenValue().toLowerCase(), null);
                    if (debug) {
                        dbg_leave("nameValue");
                    }
                    return nameValue;
                }
                this.lexer.consume(1);
                this.lexer.SPorHT();
                if (name.getTokenValue().compareToIgnoreCase("received") == 0) {
                    str = this.lexer.byteStringNoSemicolon();
                } else if (this.lexer.lookAhead(0) == '\"') {
                    str = this.lexer.quotedString();
                    quoted = true;
                } else {
                    this.lexer.match(4095);
                    Token value = this.lexer.getNextToken();
                    str = value.getTokenValue();
                }
                NameValue nv = new NameValue(name.getTokenValue().toLowerCase(), str);
                if (quoted) {
                    nv.setQuotedValue();
                }
                if (debug) {
                    dbg_leave("nameValue");
                }
                return nv;
            } catch (ParseException e) {
                NameValue nameValue2 = new NameValue(name.getTokenValue(), null);
                if (debug) {
                    dbg_leave("nameValue");
                }
                return nameValue2;
            }
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("nameValue");
            }
            throw th;
        }
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("parse");
        }
        try {
            ViaList viaList = new ViaList();
            this.lexer.match(TokenTypes.VIA);
            this.lexer.SPorHT();
            this.lexer.match(58);
            this.lexer.SPorHT();
            do {
                Via v = new Via();
                parseVia(v);
                viaList.add(v);
                this.lexer.SPorHT();
                if (this.lexer.lookAhead(0) == ',') {
                    this.lexer.consume(1);
                    this.lexer.SPorHT();
                }
            } while (this.lexer.lookAhead(0) != '\n');
            this.lexer.match(10);
            return viaList;
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }
}
