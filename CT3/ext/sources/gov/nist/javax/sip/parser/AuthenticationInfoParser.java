package gov.nist.javax.sip.parser;

import gov.nist.core.NameValue;
import gov.nist.javax.sip.header.AuthenticationInfo;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class AuthenticationInfoParser extends ParametersParser {
    public AuthenticationInfoParser(String authenticationInfo) {
        super(authenticationInfo);
    }

    protected AuthenticationInfoParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("AuthenticationInfoParser.parse");
        }
        try {
            headerName(TokenTypes.AUTHENTICATION_INFO);
            AuthenticationInfo authenticationInfo = new AuthenticationInfo();
            authenticationInfo.setHeaderName("Authentication-Info");
            this.lexer.SPorHT();
            NameValue nv = super.nameValue();
            authenticationInfo.setParameter(nv);
            this.lexer.SPorHT();
            while (this.lexer.lookAhead(0) == ',') {
                this.lexer.match(44);
                this.lexer.SPorHT();
                NameValue nv2 = super.nameValue();
                authenticationInfo.setParameter(nv2);
                this.lexer.SPorHT();
            }
            this.lexer.SPorHT();
            return authenticationInfo;
        } finally {
            if (debug) {
                dbg_leave("AuthenticationInfoParser.parse");
            }
        }
    }
}
