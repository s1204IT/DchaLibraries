package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.RetryAfter;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class RetryAfterParser extends HeaderParser {
    public RetryAfterParser(String str) {
        super(str);
    }

    protected RetryAfterParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("RetryAfterParser.parse");
        }
        RetryAfter retryAfter = new RetryAfter();
        try {
            headerName(TokenTypes.RETRY_AFTER);
            try {
                try {
                    retryAfter.setRetryAfter(Integer.parseInt(this.lexer.number()));
                    this.lexer.SPorHT();
                    if (this.lexer.lookAhead(0) == '(') {
                        retryAfter.setComment(this.lexer.comment());
                    }
                    this.lexer.SPorHT();
                    while (this.lexer.lookAhead(0) == ';') {
                        this.lexer.match(59);
                        this.lexer.SPorHT();
                        this.lexer.match(4095);
                        String tokenValue = this.lexer.getNextToken().getTokenValue();
                        if (tokenValue.equalsIgnoreCase("duration")) {
                            this.lexer.match(61);
                            this.lexer.SPorHT();
                            try {
                                try {
                                    retryAfter.setDuration(Integer.parseInt(this.lexer.number()));
                                } catch (InvalidArgumentException e) {
                                    throw createParseException(e.getMessage());
                                }
                            } catch (NumberFormatException e2) {
                                throw createParseException(e2.getMessage());
                            }
                        } else {
                            this.lexer.SPorHT();
                            this.lexer.match(61);
                            this.lexer.SPorHT();
                            this.lexer.match(4095);
                            retryAfter.setParameter(tokenValue, this.lexer.getNextToken().getTokenValue());
                        }
                        this.lexer.SPorHT();
                    }
                    return retryAfter;
                } catch (NumberFormatException e3) {
                    throw createParseException(e3.getMessage());
                }
            } catch (InvalidArgumentException e4) {
                throw createParseException(e4.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("RetryAfterParser.parse");
            }
        }
    }
}
