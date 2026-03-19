package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.ContentLanguage;
import gov.nist.javax.sip.header.ContentLanguageList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ContentLanguageParser extends HeaderParser {
    public ContentLanguageParser(String str) {
        super(str);
    }

    protected ContentLanguageParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("ContentLanguageParser.parse");
        }
        ContentLanguageList contentLanguageList = new ContentLanguageList();
        try {
            try {
                headerName(TokenTypes.CONTENT_LANGUAGE);
                while (this.lexer.lookAhead(0) != '\n') {
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    ContentLanguage contentLanguage = new ContentLanguage(this.lexer.getNextToken().getTokenValue());
                    this.lexer.SPorHT();
                    contentLanguageList.add(contentLanguage);
                    while (this.lexer.lookAhead(0) == ',') {
                        this.lexer.match(44);
                        this.lexer.SPorHT();
                        this.lexer.match(4095);
                        this.lexer.SPorHT();
                        ContentLanguage contentLanguage2 = new ContentLanguage(this.lexer.getNextToken().getTokenValue());
                        this.lexer.SPorHT();
                        contentLanguageList.add(contentLanguage2);
                    }
                }
                return contentLanguageList;
            } catch (ParseException e) {
                throw createParseException(e.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("ContentLanguageParser.parse");
            }
        }
    }
}
