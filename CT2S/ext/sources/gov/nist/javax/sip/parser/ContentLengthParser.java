package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class ContentLengthParser extends HeaderParser {
    public ContentLengthParser(String contentLength) {
        super(contentLength);
    }

    protected ContentLengthParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("ContentLengthParser.enter");
        }
        try {
            try {
                try {
                    ContentLength contentLength = new ContentLength();
                    headerName(TokenTypes.CONTENT_LENGTH);
                    String number = this.lexer.number();
                    contentLength.setContentLength(Integer.parseInt(number));
                    this.lexer.SPorHT();
                    this.lexer.match(10);
                    return contentLength;
                } catch (InvalidArgumentException ex) {
                    throw createParseException(ex.getMessage());
                }
            } catch (NumberFormatException ex2) {
                throw createParseException(ex2.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("ContentLengthParser.leave");
            }
        }
    }
}
