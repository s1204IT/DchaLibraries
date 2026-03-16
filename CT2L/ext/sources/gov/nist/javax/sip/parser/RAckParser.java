package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.RAck;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class RAckParser extends HeaderParser {
    public RAckParser(String rack) {
        super(rack);
    }

    protected RAckParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("RAckParser.parse");
        }
        RAck rack = new RAck();
        try {
            headerName(TokenTypes.RACK);
            rack.setHeaderName("RAck");
            try {
                String number = this.lexer.number();
                rack.setRSequenceNumber(Long.parseLong(number));
                this.lexer.SPorHT();
                String number2 = this.lexer.number();
                rack.setCSequenceNumber(Long.parseLong(number2));
                this.lexer.SPorHT();
                this.lexer.match(4095);
                Token token = this.lexer.getNextToken();
                rack.setMethod(token.getTokenValue());
                this.lexer.SPorHT();
                this.lexer.match(10);
                return rack;
            } catch (InvalidArgumentException ex) {
                throw createParseException(ex.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("RAckParser.parse");
            }
        }
    }
}
