package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.ProxyRequire;
import gov.nist.javax.sip.header.ProxyRequireList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class ProxyRequireParser extends HeaderParser {
    public ProxyRequireParser(String str) {
        super(str);
    }

    protected ProxyRequireParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        ProxyRequireList proxyRequireList = new ProxyRequireList();
        if (debug) {
            dbg_enter("ProxyRequireParser.parse");
        }
        try {
            headerName(TokenTypes.PROXY_REQUIRE);
            while (this.lexer.lookAhead(0) != '\n') {
                ProxyRequire proxyRequire = new ProxyRequire();
                proxyRequire.setHeaderName("Proxy-Require");
                this.lexer.match(4095);
                proxyRequire.setOptionTag(this.lexer.getNextToken().getTokenValue());
                this.lexer.SPorHT();
                proxyRequireList.add(proxyRequire);
                while (this.lexer.lookAhead(0) == ',') {
                    this.lexer.match(44);
                    this.lexer.SPorHT();
                    ProxyRequire proxyRequire2 = new ProxyRequire();
                    this.lexer.match(4095);
                    proxyRequire2.setOptionTag(this.lexer.getNextToken().getTokenValue());
                    this.lexer.SPorHT();
                    proxyRequireList.add(proxyRequire2);
                }
            }
            return proxyRequireList;
        } finally {
            if (debug) {
                dbg_leave("ProxyRequireParser.parse");
            }
        }
    }
}
