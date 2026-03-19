package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.AcceptLanguage;
import gov.nist.javax.sip.header.AcceptLanguageList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class AcceptLanguageParser extends HeaderParser {
    public AcceptLanguageParser(String str) {
        super(str);
    }

    protected AcceptLanguageParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        AcceptLanguageList acceptLanguageList = new AcceptLanguageList();
        if (debug) {
            dbg_enter("AcceptLanguageParser.parse");
        }
        try {
            headerName(TokenTypes.ACCEPT_LANGUAGE);
            while (this.lexer.lookAhead(0) != '\n') {
                AcceptLanguage acceptLanguage = new AcceptLanguage();
                acceptLanguage.setHeaderName("Accept-Language");
                if (this.lexer.lookAhead(0) != ';') {
                    this.lexer.match(4095);
                    acceptLanguage.setLanguageRange(this.lexer.getNextToken().getTokenValue());
                }
                while (this.lexer.lookAhead(0) == ';') {
                    this.lexer.match(59);
                    this.lexer.SPorHT();
                    this.lexer.match(113);
                    this.lexer.SPorHT();
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    try {
                        acceptLanguage.setQValue(Float.parseFloat(this.lexer.getNextToken().getTokenValue()));
                        this.lexer.SPorHT();
                    } catch (NumberFormatException e) {
                        throw createParseException(e.getMessage());
                    } catch (InvalidArgumentException e2) {
                        throw createParseException(e2.getMessage());
                    }
                }
                acceptLanguageList.add(acceptLanguage);
                if (this.lexer.lookAhead(0) == ',') {
                    this.lexer.match(44);
                    this.lexer.SPorHT();
                } else {
                    this.lexer.SPorHT();
                }
            }
            return acceptLanguageList;
        } finally {
            if (debug) {
                dbg_leave("AcceptLanguageParser.parse");
            }
        }
    }
}
