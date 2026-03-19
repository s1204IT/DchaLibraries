package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ContentTypeParser extends ParametersParser {
    public ContentTypeParser(String str) {
        super(str);
    }

    protected ContentTypeParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        ContentType contentType = new ContentType();
        if (debug) {
            dbg_enter("ContentTypeParser.parse");
        }
        try {
            headerName(TokenTypes.CONTENT_TYPE);
            this.lexer.match(4095);
            Token nextToken = this.lexer.getNextToken();
            this.lexer.SPorHT();
            contentType.setContentType(nextToken.getTokenValue());
            this.lexer.match(47);
            this.lexer.match(4095);
            Token nextToken2 = this.lexer.getNextToken();
            this.lexer.SPorHT();
            contentType.setContentSubType(nextToken2.getTokenValue());
            super.parse(contentType);
            this.lexer.match(10);
            return contentType;
        } finally {
            if (debug) {
                dbg_leave("ContentTypeParser.parse");
            }
        }
    }
}
