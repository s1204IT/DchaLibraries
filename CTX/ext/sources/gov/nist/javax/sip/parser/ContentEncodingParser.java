package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.ContentEncoding;
import gov.nist.javax.sip.header.ContentEncodingList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ContentEncodingParser extends HeaderParser {
    public ContentEncodingParser(String str) {
        super(str);
    }

    protected ContentEncodingParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("ContentEncodingParser.parse");
        }
        ContentEncodingList contentEncodingList = new ContentEncodingList();
        try {
            try {
                headerName(TokenTypes.CONTENT_ENCODING);
                while (this.lexer.lookAhead(0) != '\n') {
                    ContentEncoding contentEncoding = new ContentEncoding();
                    contentEncoding.setHeaderName("Content-Encoding");
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    contentEncoding.setEncoding(this.lexer.getNextToken().getTokenValue());
                    this.lexer.SPorHT();
                    contentEncodingList.add(contentEncoding);
                    while (this.lexer.lookAhead(0) == ',') {
                        ContentEncoding contentEncoding2 = new ContentEncoding();
                        this.lexer.match(44);
                        this.lexer.SPorHT();
                        this.lexer.match(4095);
                        this.lexer.SPorHT();
                        contentEncoding2.setEncoding(this.lexer.getNextToken().getTokenValue());
                        this.lexer.SPorHT();
                        contentEncodingList.add(contentEncoding2);
                    }
                }
                return contentEncodingList;
            } catch (ParseException e) {
                throw createParseException(e.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("ContentEncodingParser.parse");
            }
        }
    }
}
