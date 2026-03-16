package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import java.util.Calendar;

public class DateParser extends HeaderParser {
    public DateParser(String date) {
        super(date);
    }

    protected DateParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("DateParser.parse");
        }
        try {
            headerName(TokenTypes.DATE);
            wkday();
            this.lexer.match(44);
            this.lexer.match(32);
            Calendar cal = date();
            this.lexer.match(32);
            time(cal);
            this.lexer.match(32);
            String tzone = this.lexer.ttoken().toLowerCase();
            if (!"gmt".equals(tzone)) {
                throw createParseException("Bad Time Zone " + tzone);
            }
            this.lexer.match(10);
            SIPDateHeader retval = new SIPDateHeader();
            retval.setDate(cal);
            return retval;
        } finally {
            if (debug) {
                dbg_leave("DateParser.parse");
            }
        }
    }
}
