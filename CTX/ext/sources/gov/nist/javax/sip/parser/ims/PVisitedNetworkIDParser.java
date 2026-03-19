package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PVisitedNetworkID;
import gov.nist.javax.sip.header.ims.PVisitedNetworkIDList;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class PVisitedNetworkIDParser extends ParametersParser implements TokenTypes {
    public PVisitedNetworkIDParser(String str) {
        super(str);
    }

    protected PVisitedNetworkIDParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        char cLookAhead;
        PVisitedNetworkIDList pVisitedNetworkIDList = new PVisitedNetworkIDList();
        if (debug) {
            dbg_enter("VisitedNetworkIDParser.parse");
        }
        try {
            this.lexer.match(TokenTypes.P_VISITED_NETWORK_ID);
            this.lexer.SPorHT();
            this.lexer.match(58);
            this.lexer.SPorHT();
            while (true) {
                PVisitedNetworkID pVisitedNetworkID = new PVisitedNetworkID();
                if (this.lexer.lookAhead(0) == '\"') {
                    parseQuotedString(pVisitedNetworkID);
                } else {
                    parseToken(pVisitedNetworkID);
                }
                pVisitedNetworkIDList.add(pVisitedNetworkID);
                this.lexer.SPorHT();
                cLookAhead = this.lexer.lookAhead(0);
                if (cLookAhead != ',') {
                    break;
                }
                this.lexer.match(44);
                this.lexer.SPorHT();
            }
            if (cLookAhead != '\n') {
                throw createParseException("unexpected char = " + cLookAhead);
            }
            return pVisitedNetworkIDList;
        } finally {
            if (debug) {
                dbg_leave("VisitedNetworkIDParser.parse");
            }
        }
    }

    protected void parseQuotedString(PVisitedNetworkID pVisitedNetworkID) throws ParseException {
        boolean z;
        if (debug) {
            dbg_enter("parseQuotedString");
        }
        try {
            StringBuffer stringBuffer = new StringBuffer();
            if (this.lexer.lookAhead(0) != '\"') {
                throw createParseException("unexpected char");
            }
            this.lexer.consume(1);
            while (true) {
                char nextChar = this.lexer.getNextChar();
                if (nextChar != '\"') {
                    if (nextChar == 0) {
                        throw new ParseException("unexpected EOL", 1);
                    }
                    if (nextChar == '\\') {
                        stringBuffer.append(nextChar);
                        stringBuffer.append(this.lexer.getNextChar());
                    } else {
                        stringBuffer.append(nextChar);
                    }
                } else {
                    pVisitedNetworkID.setVisitedNetworkID(stringBuffer.toString());
                    super.parse(pVisitedNetworkID);
                    if (z) {
                        return;
                    } else {
                        return;
                    }
                }
            }
        } finally {
            if (debug) {
                dbg_leave("parseQuotedString.parse");
            }
        }
    }

    protected void parseToken(PVisitedNetworkID pVisitedNetworkID) throws ParseException {
        this.lexer.match(4095);
        pVisitedNetworkID.setVisitedNetworkID(this.lexer.getNextToken());
        super.parse(pVisitedNetworkID);
    }
}
