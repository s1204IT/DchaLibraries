package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.Require;
import gov.nist.javax.sip.header.RequireList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class RequireParser extends HeaderParser {
    public RequireParser(String str) {
        super(str);
    }

    protected RequireParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        RequireList requireList = new RequireList();
        if (debug) {
            dbg_enter("RequireParser.parse");
        }
        try {
            headerName(TokenTypes.REQUIRE);
            while (this.lexer.lookAhead(0) != '\n') {
                Require require = new Require();
                require.setHeaderName("Require");
                this.lexer.match(4095);
                require.setOptionTag(this.lexer.getNextToken().getTokenValue());
                this.lexer.SPorHT();
                requireList.add(require);
                while (this.lexer.lookAhead(0) == ',') {
                    this.lexer.match(44);
                    this.lexer.SPorHT();
                    Require require2 = new Require();
                    this.lexer.match(4095);
                    require2.setOptionTag(this.lexer.getNextToken().getTokenValue());
                    this.lexer.SPorHT();
                    requireList.add(require2);
                }
            }
            return requireList;
        } finally {
            if (debug) {
                dbg_leave("RequireParser.parse");
            }
        }
    }
}
