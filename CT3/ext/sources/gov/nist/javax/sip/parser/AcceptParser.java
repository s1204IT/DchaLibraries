package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.Accept;
import gov.nist.javax.sip.header.AcceptList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class AcceptParser extends ParametersParser {
    public AcceptParser(String accept) {
        super(accept);
    }

    protected AcceptParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("AcceptParser.parse");
        }
        AcceptList list = new AcceptList();
        try {
            headerName(2068);
            Accept accept = new Accept();
            accept.setHeaderName("Accept");
            this.lexer.SPorHT();
            this.lexer.match(4095);
            Token token = this.lexer.getNextToken();
            accept.setContentType(token.getTokenValue());
            this.lexer.match(47);
            this.lexer.match(4095);
            Token token2 = this.lexer.getNextToken();
            accept.setContentSubType(token2.getTokenValue());
            this.lexer.SPorHT();
            super.parse(accept);
            list.add(accept);
            while (this.lexer.lookAhead(0) == ',') {
                this.lexer.match(44);
                this.lexer.SPorHT();
                Accept accept2 = new Accept();
                this.lexer.match(4095);
                Token token3 = this.lexer.getNextToken();
                accept2.setContentType(token3.getTokenValue());
                this.lexer.match(47);
                this.lexer.match(4095);
                Token token4 = this.lexer.getNextToken();
                accept2.setContentSubType(token4.getTokenValue());
                this.lexer.SPorHT();
                super.parse(accept2);
                list.add(accept2);
            }
            return list;
        } finally {
            if (debug) {
                dbg_leave("AcceptParser.parse");
            }
        }
    }
}
