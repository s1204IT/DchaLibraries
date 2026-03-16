package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.ContentLanguage;
import gov.nist.javax.sip.header.ContentLanguageList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ContentLanguageParser extends HeaderParser {
    public ContentLanguageParser(String contentLanguage) {
        super(contentLanguage);
    }

    protected ContentLanguageParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("ContentLanguageParser.parse");
        }
        ContentLanguageList list = new ContentLanguageList();
        try {
            try {
                headerName(TokenTypes.CONTENT_LANGUAGE);
                while (this.lexer.lookAhead(0) != '\n') {
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    Token token = this.lexer.getNextToken();
                    ContentLanguage cl = new ContentLanguage(token.getTokenValue());
                    this.lexer.SPorHT();
                    list.add(cl);
                    while (this.lexer.lookAhead(0) == ',') {
                        this.lexer.match(44);
                        this.lexer.SPorHT();
                        this.lexer.match(4095);
                        this.lexer.SPorHT();
                        Token token2 = this.lexer.getNextToken();
                        ContentLanguage cl2 = new ContentLanguage(token2.getTokenValue());
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
                dbg_leave("ContentLanguageParser.parse");
            }
        }
    }
}
