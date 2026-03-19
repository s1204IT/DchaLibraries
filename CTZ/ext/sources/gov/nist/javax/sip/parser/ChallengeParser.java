package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.AuthenticationHeader;
import java.text.ParseException;

public abstract class ChallengeParser extends HeaderParser {
    protected ChallengeParser(String str) {
        super(str);
    }

    protected ChallengeParser(Lexer lexer) {
        super(lexer);
    }

    protected void parseParameter(AuthenticationHeader authenticationHeader) throws ParseException {
        if (debug) {
            dbg_enter("parseParameter");
        }
        try {
            authenticationHeader.setParameter(nameValue('='));
        } finally {
            if (debug) {
                dbg_leave("parseParameter");
            }
        }
    }

    public void parse(AuthenticationHeader authenticationHeader) throws ParseException {
        this.lexer.SPorHT();
        this.lexer.match(4095);
        Token nextToken = this.lexer.getNextToken();
        this.lexer.SPorHT();
        authenticationHeader.setScheme(nextToken.getTokenValue());
        while (this.lexer.lookAhead(0) != '\n') {
            try {
                parseParameter(authenticationHeader);
                this.lexer.SPorHT();
                char cLookAhead = this.lexer.lookAhead(0);
                if (cLookAhead != '\n' && cLookAhead != 0) {
                    this.lexer.match(44);
                    this.lexer.SPorHT();
                }
                return;
            } catch (ParseException e) {
                throw e;
            }
        }
    }
}
