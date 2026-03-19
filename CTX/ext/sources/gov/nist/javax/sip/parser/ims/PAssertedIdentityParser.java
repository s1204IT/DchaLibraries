package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.AddressParametersHeader;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PAssertedIdentity;
import gov.nist.javax.sip.header.ims.PAssertedIdentityList;
import gov.nist.javax.sip.parser.AddressParametersParser;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class PAssertedIdentityParser extends AddressParametersParser implements TokenTypes {
    public PAssertedIdentityParser(String str) {
        super(str);
    }

    protected PAssertedIdentityParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("AssertedIdentityParser.parse");
        }
        PAssertedIdentityList pAssertedIdentityList = new PAssertedIdentityList();
        try {
            headerName(TokenTypes.P_ASSERTED_IDENTITY);
            PAssertedIdentity pAssertedIdentity = new PAssertedIdentity();
            pAssertedIdentity.setHeaderName("P-Asserted-Identity");
            super.parse((AddressParametersHeader) pAssertedIdentity);
            pAssertedIdentityList.add(pAssertedIdentity);
            this.lexer.SPorHT();
            while (this.lexer.lookAhead(0) == ',') {
                this.lexer.match(44);
                this.lexer.SPorHT();
                PAssertedIdentity pAssertedIdentity2 = new PAssertedIdentity();
                super.parse((AddressParametersHeader) pAssertedIdentity2);
                pAssertedIdentityList.add(pAssertedIdentity2);
                this.lexer.SPorHT();
            }
            this.lexer.SPorHT();
            this.lexer.match(10);
            return pAssertedIdentityList;
        } finally {
            if (debug) {
                dbg_leave("AssertedIdentityParser.parse");
            }
        }
    }
}
