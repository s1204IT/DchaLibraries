package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class ContentLengthParser extends HeaderParser {
    public ContentLengthParser(String str) {
        super(str);
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
                    contentLength.setContentLength(Integer.parseInt(this.lexer.number()));
                    this.lexer.SPorHT();
                    this.lexer.match(10);
                    return contentLength;
                } catch (InvalidArgumentException e) {
                    throw createParseException(e.getMessage());
                }
            } catch (NumberFormatException e2) {
                throw createParseException(e2.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("ContentLengthParser.leave");
            }
        }
    }
}
