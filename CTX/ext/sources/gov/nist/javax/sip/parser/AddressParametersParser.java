package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.AddressParametersHeader;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;

public class AddressParametersParser extends ParametersParser {
    protected AddressParametersParser(Lexer lexer) {
        super(lexer);
    }

    protected AddressParametersParser(String str) {
        super(str);
    }

    protected void parse(AddressParametersHeader addressParametersHeader) throws ParseException {
        dbg_enter("AddressParametersParser.parse");
        try {
            try {
                addressParametersHeader.setAddress(new AddressParser(getLexer()).address(false));
                this.lexer.SPorHT();
                char cLookAhead = this.lexer.lookAhead(0);
                if (this.lexer.hasMoreChars() && cLookAhead != 0 && cLookAhead != '\n' && this.lexer.startsId()) {
                    super.parseNameValueList(addressParametersHeader);
                } else {
                    super.parse((ParametersHeader) addressParametersHeader);
                }
            } catch (ParseException e) {
                throw e;
            }
        } finally {
            dbg_leave("AddressParametersParser.parse");
        }
    }
}
