package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.Allow;
import gov.nist.javax.sip.header.AllowList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class AllowParser extends HeaderParser {
    public AllowParser(String str) {
        super(str);
    }

    protected AllowParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("AllowParser.parse");
        }
        AllowList allowList = new AllowList();
        try {
            headerName(TokenTypes.ALLOW);
            Allow allow = new Allow();
            allow.setHeaderName("Allow");
            this.lexer.SPorHT();
            this.lexer.match(4095);
            allow.setMethod(this.lexer.getNextToken().getTokenValue());
            allowList.add(allow);
            this.lexer.SPorHT();
            while (this.lexer.lookAhead(0) == ',') {
                this.lexer.match(44);
                this.lexer.SPorHT();
                Allow allow2 = new Allow();
                this.lexer.match(4095);
                allow2.setMethod(this.lexer.getNextToken().getTokenValue());
                allowList.add(allow2);
                this.lexer.SPorHT();
            }
            this.lexer.SPorHT();
            this.lexer.match(10);
            return allowList;
        } finally {
            if (debug) {
                dbg_leave("AllowParser.parse");
            }
        }
    }
}
