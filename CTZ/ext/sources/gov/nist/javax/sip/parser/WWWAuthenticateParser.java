package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.WWWAuthenticate;
import java.text.ParseException;

public class WWWAuthenticateParser extends ChallengeParser {
    public WWWAuthenticateParser(String str) {
        super(str);
    }

    protected WWWAuthenticateParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("parse");
        }
        try {
            headerName(TokenTypes.WWW_AUTHENTICATE);
            WWWAuthenticate wWWAuthenticate = new WWWAuthenticate();
            super.parse(wWWAuthenticate);
            return wWWAuthenticate;
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }
}
