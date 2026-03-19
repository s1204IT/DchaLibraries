package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.Authorization;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class AuthorizationParser extends ChallengeParser {
    public AuthorizationParser(String str) {
        super(str);
    }

    protected AuthorizationParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        dbg_enter("parse");
        try {
            headerName(TokenTypes.AUTHORIZATION);
            Authorization authorization = new Authorization();
            super.parse(authorization);
            return authorization;
        } finally {
            dbg_leave("parse");
        }
    }
}
