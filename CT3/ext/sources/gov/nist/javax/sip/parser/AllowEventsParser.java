package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.AllowEvents;
import gov.nist.javax.sip.header.AllowEventsList;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class AllowEventsParser extends HeaderParser {
    public AllowEventsParser(String allowEvents) {
        super(allowEvents);
    }

    protected AllowEventsParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("AllowEventsParser.parse");
        }
        AllowEventsList list = new AllowEventsList();
        try {
            headerName(TokenTypes.ALLOW_EVENTS);
            AllowEvents allowEvents = new AllowEvents();
            allowEvents.setHeaderName("Allow-Events");
            this.lexer.SPorHT();
            this.lexer.match(4095);
            Token token = this.lexer.getNextToken();
            allowEvents.setEventType(token.getTokenValue());
            list.add(allowEvents);
            this.lexer.SPorHT();
            while (this.lexer.lookAhead(0) == ',') {
                this.lexer.match(44);
                this.lexer.SPorHT();
                AllowEvents allowEvents2 = new AllowEvents();
                this.lexer.match(4095);
                Token token2 = this.lexer.getNextToken();
                allowEvents2.setEventType(token2.getTokenValue());
                list.add(allowEvents2);
                this.lexer.SPorHT();
            }
            this.lexer.SPorHT();
            this.lexer.match(10);
            return list;
        } finally {
            if (debug) {
                dbg_leave("AllowEventsParser.parse");
            }
        }
    }
}
