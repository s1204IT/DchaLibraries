package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.ims.AddressHeaderIms;
import gov.nist.javax.sip.parser.AddressParser;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import java.text.ParseException;

abstract class AddressHeaderParser extends HeaderParser {
    protected AddressHeaderParser(Lexer lexer) {
        super(lexer);
    }

    protected AddressHeaderParser(String buffer) {
        super(buffer);
    }

    protected void parse(AddressHeaderIms addressHeader) throws ParseException {
        dbg_enter("AddressHeaderParser.parse");
        try {
            try {
                AddressParser addressParser = new AddressParser(getLexer());
                AddressImpl addr = addressParser.address(true);
                addressHeader.setAddress(addr);
            } catch (ParseException ex) {
                throw ex;
            }
        } finally {
            dbg_leave("AddressParametersParser.parse");
        }
    }
}
