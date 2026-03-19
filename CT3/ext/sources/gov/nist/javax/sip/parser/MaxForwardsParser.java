package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class MaxForwardsParser extends HeaderParser {
    public MaxForwardsParser(String contentLength) {
        super(contentLength);
    }

    protected MaxForwardsParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("MaxForwardsParser.enter");
        }
        try {
            try {
                try {
                    MaxForwards contentLength = new MaxForwards();
                    headerName(TokenTypes.MAX_FORWARDS);
                    String number = this.lexer.number();
                    contentLength.setMaxForwards(Integer.parseInt(number));
                    this.lexer.SPorHT();
                    this.lexer.match(10);
                    return contentLength;
                } catch (NumberFormatException ex) {
                    throw createParseException(ex.getMessage());
                }
            } catch (InvalidArgumentException ex2) {
                throw createParseException(ex2.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("MaxForwardsParser.leave");
            }
        }
    }
}
