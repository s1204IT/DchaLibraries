package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.ContentEncoding;
import gov.nist.javax.sip.header.ContentEncodingList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ContentEncodingParser extends HeaderParser {
    public ContentEncodingParser(String contentEncoding) {
        super(contentEncoding);
    }

    protected ContentEncodingParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("ContentEncodingParser.parse");
        }
        ContentEncodingList list = new ContentEncodingList();
        try {
            try {
                headerName(TokenTypes.CONTENT_ENCODING);
                while (this.lexer.lookAhead(0) != '\n') {
                    ContentEncoding cl = new ContentEncoding();
                    cl.setHeaderName("Content-Encoding");
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    Token token = this.lexer.getNextToken();
                    cl.setEncoding(token.getTokenValue());
                    this.lexer.SPorHT();
                    list.add(cl);
                    while (this.lexer.lookAhead(0) == ',') {
                        ContentEncoding cl2 = new ContentEncoding();
                        this.lexer.match(44);
                        this.lexer.SPorHT();
                        this.lexer.match(4095);
                        this.lexer.SPorHT();
                        Token token2 = this.lexer.getNextToken();
                        cl2.setEncoding(token2.getTokenValue());
                        this.lexer.SPorHT();
                        list.add(cl2);
                    }
                }
                return list;
            } catch (ParseException ex) {
                throw createParseException(ex.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("ContentEncodingParser.parse");
            }
        }
    }
}
