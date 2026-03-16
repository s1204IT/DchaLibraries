package gov.nist.javax.sip.parser;

import gov.nist.core.Token;
import gov.nist.javax.sip.header.Event;
import gov.nist.javax.sip.header.SIPHeader;
import java.text.ParseException;

public class EventParser extends ParametersParser {
    public EventParser(String event) {
        super(event);
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
                Token token = this.lexer.getNextToken();
                String value = token.getTokenValue();
                event.setEventType(value);
                super.parse(event);
                this.lexer.SPorHT();
                this.lexer.match(10);
                return event;
            } catch (ParseException ex) {
                throw createParseException(ex.getMessage());
            }
        } finally {
            if (debug) {
                dbg_leave("EventParser.parse");
            }
        }
    }
}
