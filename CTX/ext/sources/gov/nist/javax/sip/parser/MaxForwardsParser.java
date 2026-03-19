package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class MaxForwardsParser extends HeaderParser {
    public MaxForwardsParser(String str) {
        super(str);
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
                    MaxForwards maxForwards = new MaxForwards();
                    headerName(TokenTypes.MAX_FORWARDS);
                    maxForwards.setMaxForwards(Integer.parseInt(this.lexer.number()));
                    this.lexer.SPorHT();
                    this.lexer.match(10);
                    return maxForwards;
                } catch (InvalidArgumentException e) {
                    throw createParseException(e.getMessage());
                }
            } catch (NumberFormatException e2) {
                throw createParseException(e2.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("MaxForwardsParser.leave");
            }
        }
    }
}
