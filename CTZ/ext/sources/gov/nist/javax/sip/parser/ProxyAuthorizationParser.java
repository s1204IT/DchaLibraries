package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.ProxyAuthorization;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ProxyAuthorizationParser extends ChallengeParser {
    public ProxyAuthorizationParser(String str) {
        super(str);
    }

    protected ProxyAuthorizationParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        headerName(TokenTypes.PROXY_AUTHORIZATION);
        ProxyAuthorization proxyAuthorization = new ProxyAuthorization();
        super.parse(proxyAuthorization);
        return proxyAuthorization;
    }
}
