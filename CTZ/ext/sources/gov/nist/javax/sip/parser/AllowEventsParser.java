package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.AllowEvents;
import gov.nist.javax.sip.header.AllowEventsList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class AllowEventsParser extends HeaderParser {
    public AllowEventsParser(String str) {
        super(str);
    }

    protected AllowEventsParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("AllowEventsParser.parse");
        }
        AllowEventsList allowEventsList = new AllowEventsList();
        try {
            headerName(TokenTypes.ALLOW_EVENTS);
            AllowEvents allowEvents = new AllowEvents();
            allowEvents.setHeaderName("Allow-Events");
            this.lexer.SPorHT();
            this.lexer.match(4095);
            allowEvents.setEventType(this.lexer.getNextToken().getTokenValue());
            allowEventsList.add(allowEvents);
            this.lexer.SPorHT();
            while (this.lexer.lookAhead(0) == ',') {
                this.lexer.match(44);
                this.lexer.SPorHT();
                AllowEvents allowEvents2 = new AllowEvents();
                this.lexer.match(4095);
                allowEvents2.setEventType(this.lexer.getNextToken().getTokenValue());
                allowEventsList.add(allowEvents2);
                this.lexer.SPorHT();
            }
            this.lexer.SPorHT();
            this.lexer.match(10);
            return allowEventsList;
        } finally {
            if (debug) {
                dbg_leave("AllowEventsParser.parse");
            }
        }
    }
}
