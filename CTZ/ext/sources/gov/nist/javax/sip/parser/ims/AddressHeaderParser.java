package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.ims.AddressHeaderIms;
import gov.nist.javax.sip.parser.AddressParser;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.Lexer;
import java.text.ParseException;

abstract class AddressHeaderParser extends HeaderParser {
    protected AddressHeaderParser(Lexer lexer) {
        super(lexer);
    }

    protected AddressHeaderParser(String str) {
        super(str);
    }

    protected void parse(AddressHeaderIms addressHeaderIms) throws ParseException {
        dbg_enter("AddressHeaderParser.parse");
        try {
            try {
                addressHeaderIms.setAddress(new AddressParser(getLexer()).address(true));
            } catch (ParseException e) {
                throw e;
            }
        } finally {
            dbg_leave("AddressParametersParser.parse");
        }
    }
}
