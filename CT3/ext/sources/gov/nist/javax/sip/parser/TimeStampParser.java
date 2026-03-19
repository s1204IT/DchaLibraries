package gov.nist.javax.sip.parser;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.TimeStamp;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class TimeStampParser extends HeaderParser {
    public TimeStampParser(String timeStamp) {
        super(timeStamp);
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
            String firstNumber = this.lexer.number();
            try {
                try {
                    if (this.lexer.lookAhead(0) == '.') {
                        this.lexer.match(46);
                        String secondNumber = this.lexer.number();
                        String s = firstNumber + Separators.DOT + secondNumber;
                        float ts = Float.parseFloat(s);
                        timeStamp.setTimeStamp(ts);
                    } else {
                        long ts2 = Long.parseLong(firstNumber);
                        timeStamp.setTime(ts2);
                    }
                    this.lexer.SPorHT();
                    if (this.lexer.lookAhead(0) != '\n') {
                        String firstNumber2 = this.lexer.number();
                        try {
                            try {
                                if (this.lexer.lookAhead(0) == '.') {
                                    this.lexer.match(46);
                                    String secondNumber2 = this.lexer.number();
                                    String s2 = firstNumber2 + Separators.DOT + secondNumber2;
                                    float ts3 = Float.parseFloat(s2);
                                    timeStamp.setDelay(ts3);
                                } else {
                                    int ts4 = Integer.parseInt(firstNumber2);
                                    timeStamp.setDelay(ts4);
                                }
                            } catch (NumberFormatException ex) {
                                throw createParseException(ex.getMessage());
                            }
                        } catch (InvalidArgumentException ex2) {
                            throw createParseException(ex2.getMessage());
                        }
                    }
                    return timeStamp;
                } catch (NumberFormatException ex3) {
                    throw createParseException(ex3.getMessage());
                }
            } catch (InvalidArgumentException ex4) {
                throw createParseException(ex4.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("TimeStampParser.parse");
            }
        }
    }
}
