package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.AddressParametersHeader;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class RouteParser extends AddressParametersParser {
    public RouteParser(String str) {
        super(str);
    }

    protected RouteParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        char cLookAhead;
        RouteList routeList = new RouteList();
        if (debug) {
            dbg_enter("parse");
        }
        try {
            this.lexer.match(TokenTypes.ROUTE);
            this.lexer.SPorHT();
            this.lexer.match(58);
            this.lexer.SPorHT();
            while (true) {
                Route route = new Route();
                super.parse((AddressParametersHeader) route);
                routeList.add(route);
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
            return routeList;
        } finally {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }
}
