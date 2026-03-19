package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.RAck;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class RAckParser extends HeaderParser {
    public RAckParser(String str) {
        super(str);
    }

    protected RAckParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("RAckParser.parse");
        }
        RAck rAck = new RAck();
        try {
            headerName(TokenTypes.RACK);
            rAck.setHeaderName("RAck");
            try {
                rAck.setRSequenceNumber(Long.parseLong(this.lexer.number()));
                this.lexer.SPorHT();
                rAck.setCSequenceNumber(Long.parseLong(this.lexer.number()));
                this.lexer.SPorHT();
                this.lexer.match(4095);
                rAck.setMethod(this.lexer.getNextToken().getTokenValue());
                this.lexer.SPorHT();
                this.lexer.match(10);
                return rAck;
            } catch (InvalidArgumentException e) {
                throw createParseException(e.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("RAckParser.parse");
            }
        }
    }
}
