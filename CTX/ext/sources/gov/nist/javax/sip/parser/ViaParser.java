package gov.nist.javax.sip.parser;

import gov.nist.core.HostNameParser;
import gov.nist.core.NameValue;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.Protocol;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import java.text.ParseException;

public class ViaParser extends HeaderParser {
    public ViaParser(String str) {
        super(str);
    }

    public ViaParser(Lexer lexer) {
        super(lexer);
    }

    private void parseVia(Via via) throws ParseException {
        this.lexer.match(4095);
        Token nextToken = this.lexer.getNextToken();
        this.lexer.SPorHT();
        this.lexer.match(47);
        this.lexer.SPorHT();
        this.lexer.match(4095);
        this.lexer.SPorHT();
        Token nextToken2 = this.lexer.getNextToken();
        this.lexer.SPorHT();
        this.lexer.match(47);
        this.lexer.SPorHT();
        this.lexer.match(4095);
        this.lexer.SPorHT();
        Token nextToken3 = this.lexer.getNextToken();
        this.lexer.SPorHT();
        Protocol protocol = new Protocol();
        protocol.setProtocolName(nextToken.getTokenValue());
        protocol.setProtocolVersion(nextToken2.getTokenValue());
        protocol.setTransport(nextToken3.getTokenValue());
        via.setSentProtocol(protocol);
        via.setSentBy(new HostNameParser(getLexer()).hostPort(true));
        this.lexer.SPorHT();
        while (this.lexer.lookAhead(0) == ';') {
            this.lexer.consume(1);
            this.lexer.SPorHT();
            NameValue nameValue = nameValue();
            if (nameValue.getName().equals("branch") && ((String) nameValue.getValueAsObject()) == null) {
                throw new ParseException("null branch Id", this.lexer.getPtr());
            }
            via.setParameter(nameValue);
            this.lexer.SPorHT();
        }
        if (this.lexer.lookAhead(0) == '(') {
            this.lexer.selectLexer("charLexer");
            this.lexer.consume(1);
            StringBuffer stringBuffer = new StringBuffer();
            while (true) {
                char cLookAhead = this.lexer.lookAhead(0);
                if (cLookAhead == ')') {
                    this.lexer.consume(1);
                    break;
                }
                if (cLookAhead == '\\') {
                    stringBuffer.append(this.lexer.getNextToken().getTokenValue());
                    this.lexer.consume(1);
                    stringBuffer.append(this.lexer.getNextToken().getTokenValue());
                    this.lexer.consume(1);
                } else {
                    if (cLookAhead == '\n') {
                        break;
                    }
                    stringBuffer.append(cLookAhead);
                    this.lexer.consume(1);
                }
            }
            via.setComment(stringBuffer.toString());
        }
    }

    @Override
    protected NameValue nameValue() throws ParseException {
        String tokenValue;
        if (debug) {
            dbg_enter("nameValue");
        }
        try {
            this.lexer.match(4095);
            Token nextToken = this.lexer.getNextToken();
            this.lexer.SPorHT();
            try {
                boolean z = false;
                if (this.lexer.lookAhead(0) != '=') {
                    NameValue nameValue = new NameValue(nextToken.getTokenValue().toLowerCase(), null);
                    if (debug) {
                        dbg_leave("nameValue");
                    }
                    return nameValue;
                }
                this.lexer.consume(1);
                this.lexer.SPorHT();
                if (nextToken.getTokenValue().compareToIgnoreCase("received") == 0) {
                    tokenValue = this.lexer.byteStringNoSemicolon();
                } else if (this.lexer.lookAhead(0) == '\"') {
                    tokenValue = this.lexer.quotedString();
                    z = true;
                } else {
                    this.lexer.match(4095);
                    tokenValue = this.lexer.getNextToken().getTokenValue();
                }
                NameValue nameValue2 = new NameValue(nextToken.getTokenValue().toLowerCase(), tokenValue);
                if (z) {
                    nameValue2.setQuotedValue();
                }
                if (debug) {
                    dbg_leave("nameValue");
                }
                return nameValue2;
            } catch (ParseException e) {
                NameValue nameValue3 = new NameValue(nextToken.getTokenValue(), null);
                if (debug) {
                    dbg_leave("nameValue");
                }
                return nameValue3;
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
                Via via = new Via();
                parseVia(via);
                viaList.add(via);
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
