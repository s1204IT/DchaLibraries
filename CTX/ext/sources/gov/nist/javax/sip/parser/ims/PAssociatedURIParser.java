package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.AddressParametersHeader;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PAssociatedURI;
import gov.nist.javax.sip.header.ims.PAssociatedURIList;
import gov.nist.javax.sip.parser.AddressParametersParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class PAssociatedURIParser extends AddressParametersParser {
    public PAssociatedURIParser(String str) {
        super(str);
    }

    protected PAssociatedURIParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("PAssociatedURIParser.parse");
        }
        PAssociatedURIList pAssociatedURIList = new PAssociatedURIList();
        try {
            headerName(TokenTypes.P_ASSOCIATED_URI);
            PAssociatedURI pAssociatedURI = new PAssociatedURI();
            pAssociatedURI.setHeaderName("P-Associated-URI");
            super.parse((AddressParametersHeader) pAssociatedURI);
            pAssociatedURIList.add(pAssociatedURI);
            this.lexer.SPorHT();
            while (this.lexer.lookAhead(0) == ',') {
                this.lexer.match(44);
                this.lexer.SPorHT();
                PAssociatedURI pAssociatedURI2 = new PAssociatedURI();
                super.parse((AddressParametersHeader) pAssociatedURI2);
                pAssociatedURIList.add(pAssociatedURI2);
                this.lexer.SPorHT();
            }
            this.lexer.SPorHT();
            this.lexer.match(10);
            return pAssociatedURIList;
        } finally {
            if (debug) {
                dbg_leave("PAssociatedURIParser.parse");
            }
        }
    }
}
