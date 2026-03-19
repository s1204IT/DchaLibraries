package gov.nist.javax.sip.parser;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.TimeStamp;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class TimeStampParser extends HeaderParser {
    public TimeStampParser(String str) {
        super(str);
    }

    protected TimeStampParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("TimeStampParser.parse");
        }
        TimeStamp timeStamp = new TimeStamp();
        try {
            headerName(TokenTypes.TIMESTAMP);
            timeStamp.setHeaderName("Timestamp");
            this.lexer.SPorHT();
            String strNumber = this.lexer.number();
            try {
                if (this.lexer.lookAhead(0) == '.') {
                    this.lexer.match(46);
                    timeStamp.setTimeStamp(Float.parseFloat(strNumber + Separators.DOT + this.lexer.number()));
                } else {
                    timeStamp.setTime(Long.parseLong(strNumber));
                }
                this.lexer.SPorHT();
                if (this.lexer.lookAhead(0) != '\n') {
                    String strNumber2 = this.lexer.number();
                    try {
                        if (this.lexer.lookAhead(0) == '.') {
                            this.lexer.match(46);
                            timeStamp.setDelay(Float.parseFloat(strNumber2 + Separators.DOT + this.lexer.number()));
                        } else {
                            timeStamp.setDelay(Integer.parseInt(strNumber2));
                        }
                    } catch (NumberFormatException e) {
                        throw createParseException(e.getMessage());
                    } catch (InvalidArgumentException e2) {
                        throw createParseException(e2.getMessage());
                    }
                }
                return timeStamp;
            } catch (NumberFormatException e3) {
                throw createParseException(e3.getMessage());
            } catch (InvalidArgumentException e4) {
                throw createParseException(e4.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("TimeStampParser.parse");
            }
        }
    }
}
