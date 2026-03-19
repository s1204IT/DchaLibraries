package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.AcceptLanguage;
import gov.nist.javax.sip.header.AcceptLanguageList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class AcceptLanguageParser extends HeaderParser {
    public AcceptLanguageParser(String acceptLanguage) {
        super(acceptLanguage);
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
                    Token value = this.lexer.getNextToken();
                    acceptLanguage.setLanguageRange(value.getTokenValue());
                }
                while (this.lexer.lookAhead(0) == ';') {
                    this.lexer.match(59);
                    this.lexer.SPorHT();
                    this.lexer.match(113);
                    this.lexer.SPorHT();
                    this.lexer.match(61);
                    this.lexer.SPorHT();
                    this.lexer.match(4095);
                    Token value2 = this.lexer.getNextToken();
                    try {
                        try {
                            float fl = Float.parseFloat(value2.getTokenValue());
                            acceptLanguage.setQValue(fl);
                            this.lexer.SPorHT();
                        } catch (InvalidArgumentException ex) {
                            throw createParseException(ex.getMessage());
                        }
                    } catch (NumberFormatException ex2) {
                        throw createParseException(ex2.getMessage());
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
