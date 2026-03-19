package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.AddressParametersHeader;
import gov.nist.javax.sip.header.RecordRoute;
import gov.nist.javax.sip.header.RecordRouteList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class RecordRouteParser extends AddressParametersParser {
    public RecordRouteParser(String str) {
        super(str);
    }

    protected RecordRouteParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        char cLookAhead;
        RecordRouteList recordRouteList = new RecordRouteList();
        if (debug) {
            dbg_enter("RecordRouteParser.parse");
        }
        try {
            this.lexer.match(TokenTypes.RECORD_ROUTE);
            this.lexer.SPorHT();
            this.lexer.match(58);
            this.lexer.SPorHT();
            while (true) {
                RecordRoute recordRoute = new RecordRoute();
                super.parse((AddressParametersHeader) recordRoute);
                recordRouteList.add(recordRoute);
                this.lexer.SPorHT();
                cLookAhead = this.lexer.lookAhead(0);
                if (cLookAhead != ',') {
                    break;
                }
                this.lexer.match(44);
                this.lexer.SPorHT();
            }
            if (cLookAhead != '\n') {
                throw createParseException("unexpected char");
            }
            return recordRouteList;
        } finally {
            if (debug) {
                dbg_leave("RecordRouteParser.parse");
            }
        }
    }
}
