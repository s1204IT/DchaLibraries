package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.ContentDisposition;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ContentDispositionParser extends ParametersParser {
    public ContentDispositionParser(String str) {
        super(str);
    }

    protected ContentDispositionParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("ContentDispositionParser.parse");
        }
        try {
            try {
                headerName(TokenTypes.CONTENT_DISPOSITION);
                ContentDisposition contentDisposition = new ContentDisposition();
                contentDisposition.setHeaderName("Content-Disposition");
                this.lexer.SPorHT();
                this.lexer.match(4095);
                contentDisposition.setDispositionType(this.lexer.getNextToken().getTokenValue());
                this.lexer.SPorHT();
                super.parse(contentDisposition);
                this.lexer.SPorHT();
                this.lexer.match(10);
                return contentDisposition;
            } catch (ParseException e) {
                throw createParseException(e.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("ContentDispositionParser.parse");
            }
        }
    }
}
