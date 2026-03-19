package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.Event;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class EventParser extends ParametersParser {
    public EventParser(String str) {
        super(str);
    }

    protected EventParser(Lexer lexer) {
        super(lexer);
    }

    @Override
    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("EventParser.parse");
        }
        try {
            try {
                headerName(TokenTypes.EVENT);
                this.lexer.SPorHT();
                Event event = new Event();
                this.lexer.match(4095);
                event.setEventType(this.lexer.getNextToken().getTokenValue());
                super.parse(event);
                this.lexer.SPorHT();
                this.lexer.match(10);
                return event;
            } catch (ParseException e) {
                throw createParseException(e.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("EventParser.parse");
            }
        }
    }
}
