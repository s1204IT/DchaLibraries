package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.AddressParametersHeader;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;

public class AddressParametersParser extends ParametersParser {
    protected AddressParametersParser(Lexer lexer) {
        super(lexer);
    }

    protected AddressParametersParser(String buffer) {
        super(buffer);
    }

    protected void parse(AddressParametersHeader addressParametersHeader) throws ParseException {
        dbg_enter("AddressParametersParser.parse");
        try {
            try {
                AddressParser addressParser = new AddressParser(getLexer());
                AddressImpl addr = addressParser.address(false);
                addressParametersHeader.setAddress(addr);
                this.lexer.SPorHT();
                char la = this.lexer.lookAhead(0);
                if (this.lexer.hasMoreChars() && la != 0 && la != '\n' && this.lexer.startsId()) {
                    super.parseNameValueList(addressParametersHeader);
                } else {
                    super.parse((ParametersHeader) addressParametersHeader);
                }
            } catch (ParseException ex) {
                throw ex;
            }
        } finally {
            dbg_leave("AddressParametersParser.parse");
        }
    }
}
