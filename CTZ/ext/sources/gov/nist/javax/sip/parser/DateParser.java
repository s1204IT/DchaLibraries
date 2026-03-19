package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import java.util.Calendar;

public class DateParser extends HeaderParser {
    public DateParser(String str) {
        super(str);
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
            Calendar calendarDate = date();
            this.lexer.match(32);
            time(calendarDate);
            this.lexer.match(32);
            String lowerCase = this.lexer.ttoken().toLowerCase();
            if (!"gmt".equals(lowerCase)) {
                throw createParseException("Bad Time Zone " + lowerCase);
            }
            this.lexer.match(10);
            SIPDateHeader sIPDateHeader = new SIPDateHeader();
            sIPDateHeader.setDate(calendarDate);
            return sIPDateHeader;
        } finally {
            if (debug) {
                dbg_leave("DateParser.parse");
            }
        }
    }
}
