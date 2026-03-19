package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.ContentDisposition;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ContentDispositionParser extends ParametersParser {
    public ContentDispositionParser(String contentDisposition) {
        super(contentDisposition);
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
                ContentDisposition cd = new ContentDisposition();
                cd.setHeaderName("Content-Disposition");
                this.lexer.SPorHT();
                this.lexer.match(4095);
                Token token = this.lexer.getNextToken();
                cd.setDispositionType(token.getTokenValue());
                this.lexer.SPorHT();
                super.parse(cd);
                this.lexer.SPorHT();
                this.lexer.match(10);
                return cd;
            } catch (ParseException ex) {
                throw createParseException(ex.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("ContentDispositionParser.parse");
            }
        }
    }
}
